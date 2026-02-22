package com.apksigner.config;

import com.apksigner.util.CryptoUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 启动时校验安全相关配置，缺失关键配置则阻止启动
 */
@Slf4j
@Component
public class SecurityConfig {

    @PostConstruct
    public void validateEncryptionKey() {
        try {
            CryptoUtil.getKey();
            log.info("加密密钥校验通过 (ENCRYPT_KEY 已配置)");
        } catch (IllegalStateException e) {
            log.error("安全配置校验失败: {}", e.getMessage());
            throw e; // 阻止 Spring Boot 启动
        }
    }
}
