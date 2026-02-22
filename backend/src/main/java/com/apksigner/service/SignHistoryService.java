package com.apksigner.service;

import com.apksigner.dto.BatchSignRequest;
import com.apksigner.dto.SignRequest;
import com.apksigner.entity.SignHistory;
import com.apksigner.exception.BizException;
import com.apksigner.mapper.SignHistoryMapper;
import com.apksigner.util.CryptoUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        return sanitizeForResponse(history);
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
        // 加密后存储密码
        history.setStorePassword(CryptoUtil.encrypt(request.getStorePassword()));
        history.setKeyPassword(CryptoUtil.encrypt(request.getKeyPassword()));
        updateById(history);

        try {
            String outputPath = fileStorageService.getSignedFilePath(history.getFilePath()).toString();
            // 使用原始明文密码传给签名引擎
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
            return sanitizeForResponse(history);
        } catch (Exception e) {
            // 内部详细错误只记日志，存储和返回给前端的是脱敏后的通用提示
            log.error("签名失败: id={}, error={}", history.getId(), e.getMessage(), e);
            history.setStatus("FAILED");
            history.setErrorMessage(sanitizeErrorMessage(e.getMessage()));
            updateById(history);
            throw new BizException("签名处理失败，请检查文件格式或签名参数后重试");
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
        results.forEach(this::sanitizeForResponse);
        return results;
    }

    public Page<SignHistory> getHistoryPage(int page, int size) {
        Page<SignHistory> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SignHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SignHistory::getCreatedAt);
        Page<SignHistory> result = page(pageParam, wrapper);
        result.getRecords().forEach(this::sanitizeForResponse);
        return result;
    }

    public SignHistory getByDownloadCode(String code) {
        LambdaQueryWrapper<SignHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SignHistory::getDownloadCode, code);
        SignHistory history = getOne(wrapper);
        if (history == null) {
            throw new BizException(404, "下载码无效");
        }
        return sanitizeForResponse(history);
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

    public SignHistory getSanitizedById(Long id) {
        SignHistory history = getById(id);
        return sanitizeForResponse(history);
    }

    /**
     * 脱敏处理：API 返回前清除/遮盖敏感字段
     * - 密码字段用 mask 替代，不返回密文也不返回明文
     * - 文件系统路径不暴露给前端
     */
    private SignHistory sanitizeForResponse(SignHistory history) {
        if (history == null) return null;
        history.setStorePassword(history.getStorePassword() != null ? "******" : null);
        history.setKeyPassword(history.getKeyPassword() != null ? "******" : null);
        history.setFilePath(null);
        history.setSignedFilePath(null);
        return history;
    }

    /**
     * 错误信息脱敏：移除内部路径、命令输出等敏感信息
     */
    private String sanitizeErrorMessage(String rawMessage) {
        if (rawMessage == null) return "未知错误";
        // 移除文件路径信息
        String sanitized = rawMessage.replaceAll("/data/[\\w/\\-_.]+", "[path]");
        // 移除命令输出细节
        sanitized = sanitized.replaceAll("Cannot run program.*", "签名工具执行异常");
        sanitized = sanitized.replaceAll("error=\\d+,?\\s*", "");
        // 截断过长的错误信息
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 200) + "...";
        }
        return sanitized;
    }
}
