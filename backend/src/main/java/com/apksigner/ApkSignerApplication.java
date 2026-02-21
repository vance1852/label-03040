package com.apksigner;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.apksigner.mapper")
@EnableScheduling
public class ApkSignerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApkSignerApplication.class, args);
    }
}
