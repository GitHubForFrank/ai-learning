-- [SDD-TASK: Task001]
-- [SDD-SPEC: 02-功能规范.md §8 DB-04 + §2.4 + 03-技术方案.md §5.1bis] app_user 表
-- 注:表名 app_user 而非 user(避 MySQL 保留字,见 conventions/db-conventions.md §2)

CREATE TABLE `app_user` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT,
    `username`            VARCHAR(50)  NOT NULL,
    `password_hash`       VARCHAR(100) NOT NULL,
    `status`              VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    `failed_login_count`  INT          NOT NULL DEFAULT 0,
    `locked_until`        DATETIME     DEFAULT NULL,
    `created_at`          DATETIME     NOT NULL,
    `updated_at`          DATETIME     NOT NULL,
    `deleted`             TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
