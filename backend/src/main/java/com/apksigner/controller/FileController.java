package com.apksigner.controller;

import com.apksigner.dto.R;
import com.apksigner.entity.SignHistory;
import com.apksigner.exception.BizException;
import com.apksigner.service.QrCodeService;
import com.apksigner.service.SignHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final SignHistoryService signHistoryService;
    private final QrCodeService qrCodeService;

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        SignHistory history = signHistoryService.getById(id);
        if (history == null) {
            throw new BizException(404, "记录不存在");
        }
        if (!"SUCCESS".equals(history.getStatus()) || history.getSignedFilePath() == null) {
            throw new BizException("文件尚未签名完成");
        }

        Path filePath = Path.of(history.getSignedFilePath());
        if (!Files.exists(filePath)) {
            throw new BizException("签名文件已过期或不存在");
        }

        String encodedFilename = URLEncoder.encode(history.getSignedFilename(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        log.info("文件下载: id={}, filename={}", id, history.getSignedFilename());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFilename)
                .body(new FileSystemResource(filePath));
    }

    @GetMapping("/download/code/{code}")
    public ResponseEntity<Resource> downloadByCode(@PathVariable String code) {
        SignHistory history = signHistoryService.getByDownloadCode(code);
        return download(history.getId());
    }

    @GetMapping("/qrcode/{id}")
    public R<String> getQrCode(@PathVariable Long id) {
        SignHistory history = signHistoryService.getById(id);
        if (history == null) {
            throw new BizException(404, "记录不存在");
        }
        String qrCode = qrCodeService.generateDownloadQrCode(id);
        return R.ok(qrCode);
    }
}
