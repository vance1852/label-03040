package com.apksigner.service;

import com.apksigner.dto.BatchSignRequest;
import com.apksigner.dto.SignRequest;
import com.apksigner.entity.SignHistory;
import com.apksigner.exception.BizException;
import com.apksigner.mapper.SignHistoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignHistoryService extends ServiceImpl<SignHistoryMapper, SignHistory> {

    private final FileStorageService fileStorageService;
    private final ApkSigningService apkSigningService;

    public SignHistory uploadFile(MultipartFile file) {
        String filePath = fileStorageService.storeUploadFile(file);

        SignHistory history = new SignHistory();
        history.setOriginalFilename(file.getOriginalFilename());
        history.setFilePath(filePath);
        history.setFileSize(file.getSize());
        history.setStatus("PENDING");
        history.setSignType("RELEASE");
        history.setDownloadCode(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        save(history);

        log.info("文件上传记录创建: id={}, filename={}", history.getId(), file.getOriginalFilename());
        return history;
    }

    public SignHistory executeSign(SignRequest request) {
        SignHistory history = getById(request.getHistoryId());
        if (history == null) {
            throw new BizException(404, "签名记录不存在");
        }
        if ("PROCESSING".equals(history.getStatus())) {
            throw new BizException("签名正在处理中，请勿重复提交");
        }

        history.setStatus("PROCESSING");
        history.setSignType(request.getSignType());
        history.setKeyAlias(request.getKeyAlias());
        history.setValidityYears(request.getValidityYears());
        history.setStorePassword(request.getStorePassword());
        history.setKeyPassword(request.getKeyPassword());
        updateById(history);

        try {
            String outputPath = fileStorageService.getSignedFilePath(history.getFilePath()).toString();
            apkSigningService.signApk(
                    history.getFilePath(), outputPath,
                    request.getSignType(), request.getKeyAlias(),
                    request.getValidityYears(),
                    request.getStorePassword(), request.getKeyPassword()
            );

            String signedFilename = "signed_" + history.getOriginalFilename();
            history.setSignedFilename(signedFilename);
            history.setSignedFilePath(outputPath);
            history.setStatus("SUCCESS");
            history.setErrorMessage(null);
            updateById(history);

            log.info("签名成功: id={}, type={}", history.getId(), request.getSignType());
            return history;
        } catch (Exception e) {
            history.setStatus("FAILED");
            history.setErrorMessage(e.getMessage());
            updateById(history);
            log.error("签名失败: id={}, error={}", history.getId(), e.getMessage());
            throw e;
        }
    }

    public List<SignHistory> executeBatchSign(BatchSignRequest request) {
        String batchId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        List<SignHistory> results = new ArrayList<>();

        for (Long historyId : request.getHistoryIds()) {
            try {
                SignRequest signRequest = new SignRequest();
                signRequest.setHistoryId(historyId);
                signRequest.setSignType(request.getSignType());
                signRequest.setKeyAlias(request.getKeyAlias());
                signRequest.setValidityYears(request.getValidityYears());
                signRequest.setStorePassword(request.getStorePassword());
                signRequest.setKeyPassword(request.getKeyPassword());

                SignHistory history = executeSign(signRequest);
                history.setBatchId(batchId);
                updateById(history);
                results.add(history);
            } catch (Exception e) {
                log.error("批量签名中单个文件失败: historyId={}, error={}", historyId, e.getMessage());
                SignHistory failed = getById(historyId);
                if (failed != null) {
                    failed.setBatchId(batchId);
                    updateById(failed);
                    results.add(failed);
                }
            }
        }

        log.info("批量签名完成: batchId={}, total={}, success={}",
                batchId, results.size(), results.stream().filter(h -> "SUCCESS".equals(h.getStatus())).count());
        return results;
    }

    public Page<SignHistory> getHistoryPage(int page, int size) {
        Page<SignHistory> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SignHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SignHistory::getCreatedAt);
        return page(pageParam, wrapper);
    }

    public SignHistory getByDownloadCode(String code) {
        LambdaQueryWrapper<SignHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SignHistory::getDownloadCode, code);
        SignHistory history = getOne(wrapper);
        if (history == null) {
            throw new BizException(404, "下载码无效");
        }
        return history;
    }

    public void deleteHistory(Long id) {
        SignHistory history = getById(id);
        if (history == null) {
            throw new BizException(404, "记录不存在");
        }
        if (history.getFilePath() != null) {
            fileStorageService.deleteFile(history.getFilePath());
        }
        if (history.getSignedFilePath() != null) {
            fileStorageService.deleteFile(history.getSignedFilePath());
        }
        removeById(id);
        log.info("删除签名记录: id={}", id);
    }

    public void batchDelete(List<Long> ids) {
        ids.forEach(this::deleteHistory);
    }
}
