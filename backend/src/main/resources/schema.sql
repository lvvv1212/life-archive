-- H2 初始化脚本（兼容 MySQL 模式）
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `nickname` VARCHAR(100),
    `avatar` VARCHAR(500),
    `email` VARCHAR(100),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `deleted` INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS `memory` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `description` VARCHAR(1000),
    `content` TEXT,
    `file_url` VARCHAR(500),
    `file_type` VARCHAR(50),
    `memory_type` VARCHAR(50),
    `location` VARCHAR(200),
    `emotion` VARCHAR(50),
    `tags` VARCHAR(500),
    `ai_summary` TEXT,
    `event_time` TIMESTAMP,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `deleted` INT DEFAULT 0
);