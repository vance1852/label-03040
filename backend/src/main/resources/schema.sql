CREATE DATABASE IF NOT EXISTS apk_signer DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE apk_signer;

CREATE TABLE IF NOT EXISTS sign_history (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_filename VARCHAR(255) NOT NULL COMMENT '原始文件名',
    signed_filename VARCHAR(255) COMMENT '签名后文件名',
    file_path       VARCHAR(500) NOT NULL COMMENT '上传文件路径',
    signed_file_path VARCHAR(500) COMMENT '签名文件路径',
    sign_type       VARCHAR(20) NOT NULL COMMENT '签名类型: DEBUG/RELEASE/V1/V2/V3',
    key_alias       VARCHAR(100) DEFAULT 'apk-key' COMMENT '密钥别名',
    validity_years  INT DEFAULT 25 COMMENT '有效期(年)',
    store_password  VARCHAR(100) DEFAULT 'android' COMMENT 'KeyStore密码',
    key_password    VARCHAR(100) DEFAULT 'android' COMMENT 'Key密码',
    file_size       BIGINT NOT NULL COMMENT '文件大小(bytes)',
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/PROCESSING/SUCCESS/FAILED',
    error_message   TEXT COMMENT '错误信息',
    download_code   VARCHAR(64) COMMENT '下载码',
    batch_id        VARCHAR(64) COMMENT '批量签名批次ID',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_download_code (download_code),
    INDEX idx_batch_id (batch_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='签名历史记录';

CREATE TABLE IF NOT EXISTS sign_config (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key      VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value    VARCHAR(500) NOT NULL COMMENT '配置值',
    description     VARCHAR(255) COMMENT '描述',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='签名配置';

INSERT IGNORE INTO sign_config (config_key, config_value, description) VALUES
('max_file_size_mb', '200', '最大文件大小(MB)'),
('file_retention_hours', '24', '文件保留时间(小时)'),
('default_key_alias', 'apk-key', '默认密钥别名'),
('default_validity_years', '25', '默认有效期(年)');
