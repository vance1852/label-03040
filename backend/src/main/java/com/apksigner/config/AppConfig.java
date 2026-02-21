package com.apksigner.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "apk-signer")
public class AppConfig {
    private String uploadDir = "/data/uploads";
    private String signedDir = "/data/signed";
    private String keystoreDir = "/data/keystores";
    private int fileRetentionHours = 24;
    private String baseUrl = "http://localhost:8080";
}
