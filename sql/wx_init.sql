-- ============================================================
-- LifeArchive 微信小程序：独立新增表
-- 说明：本文件【不修改】现有 init.sql / user 表，单独执行即可。
-- 执行方式（任选其一）：
--   1) 手动在 MySQL 客户端 source 本文件；
--   2) 挂载到 docker mysql 初始化卷（不改动 docker-compose.yml）。
-- ============================================================

CREATE TABLE IF NOT EXISTS wx_account (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL COMMENT '关联现有 user.id',
    openid      VARCHAR(64)  NOT NULL COMMENT '微信小程序唯一标识',
    unionid     VARCHAR(64)  DEFAULT NULL COMMENT '微信开放平台 unionid',
    session_key VARCHAR(128) DEFAULT NULL COMMENT '微信会话密钥（建议加密存储）',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_openid (openid),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信账号映射表';
