CREATE TABLE IF NOT EXISTS sign_history (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_filename VARCHAR(255) NOT NULL,
    signed_filename VARCHAR(255),
    file_path       VARCHAR(500) NOT NULL,
    signed_file_path VARCHAR(500),
    sign_type       VARCHAR(20) NOT NULL,
    key_alias       VARCHAR(100) DEFAULT 'apk-key',
    validity_years  INT DEFAULT 25,
    store_password  VARCHAR(100) DEFAULT 'android',
    key_password    VARCHAR(100) DEFAULT 'android',
    file_size       BIGINT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message   TEXT,
    download_code   VARCHAR(64),
    batch_id        VARCHAR(64),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sign_config (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key      VARCHAR(100) NOT NULL UNIQUE,
    config_value    VARCHAR(500) NOT NULL,
    description     VARCHAR(255),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
