package com.apksigner.service;

import com.apksigner.config.AppConfig;
import com.apksigner.exception.BizException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final AppConfig appConfig;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Path.of(appConfig.getUploadDir()));
            Files.createDirectories(Path.of(appConfig.getSignedDir()));
            Files.createDirectories(Path.of(appConfig.getKeystoreDir()));
            log.info("文件存储目录初始化完成");
        } catch (IOException e) {
            throw new RuntimeException("无法创建存储目录", e);
        }
    }

    public String storeUploadFile(MultipartFile file) {
        validateApkFile(file);
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path targetPath = Path.of(appConfig.getUploadDir(), filename);
        try {
            file.transferTo(targetPath.toFile());
            log.info("文件上传成功: {}, 大小: {} bytes", filename, file.getSize());
            return targetPath.toString();
        } catch (IOException e) {
            log.error("文件存储失败: {}", e.getMessage());
            throw new BizException("文件上传失败: " + e.getMessage());
        }
    }

    public void validateApkFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "请选择要上传的文件");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".apk")) {
            throw new BizException(400, "仅支持 .apk 格式文件");
        }
        long maxSize = 200L * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new BizException(400, "文件大小超过限制(最大200MB)");
        }
    }

    public Path getSignedFilePath(String originalPath) {
        String originalName = Path.of(originalPath).getFileName().toString();
        String signedName = "signed_" + originalName;
        return Path.of(appConfig.getSignedDir(), signedName);
    }

    public void deleteFile(String filePath) {
        try {
            Files.deleteIfExists(Path.of(filePath));
            log.info("文件已删除: {}", filePath);
        } catch (IOException e) {
            log.warn("文件删除失败: {}", filePath);
        }
    }

    public void cleanExpiredFiles() {
        int hours = appConfig.getFileRetentionHours();
        Instant cutoff = Instant.now().minus(hours, ChronoUnit.HOURS);
        cleanDirectory(appConfig.getUploadDir(), cutoff);
        cleanDirectory(appConfig.getSignedDir(), cutoff);
        log.info("过期文件清理完成, 保留时间: {}小时", hours);
    }

    private void cleanDirectory(String dir, Instant cutoff) {
        try {
            Path dirPath = Path.of(dir);
            if (!Files.exists(dirPath)) return;
            Files.walkFileTree(dirPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (attrs.lastModifiedTime().toInstant().isBefore(cutoff)) {
                        Files.delete(file);
                        log.debug("清理过期文件: {}", file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("清理目录失败: {}", dir, e);
        }
    }
}
