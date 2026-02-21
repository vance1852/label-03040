package com.apksigner.scheduler;

import com.apksigner.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanupScheduler {

    private final FileStorageService fileStorageService;

    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    public void cleanExpiredFiles() {
        log.info("开始执行过期文件清理任务");
        fileStorageService.cleanExpiredFiles();
    }
}
