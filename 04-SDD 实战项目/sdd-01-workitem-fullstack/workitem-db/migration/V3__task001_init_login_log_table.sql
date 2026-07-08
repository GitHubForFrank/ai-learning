-- [SDD-TASK: Task001]
-- [SDD-SPEC: 02-功能规范.md §8 DB-05 + §2.6 + 03-技术方案.md §5.1bis] login_log 表

CREATE TABLE `login_log` (
    `id`                   BIGINT        NOT NULL AUTO_INCREMENT,
    `app_user_id`          BIGINT        DEFAULT NULL,
    `username_attempted`   VARCHAR(50)   NOT NULL,
    `login_type`           VARCHAR(16)   NOT NULL,
    `login_ip`             VARCHAR(64)   NOT NULL,
    `user_agent`           VARCHAR(512)  DEFAULT NULL,
    `traceparent`          VARCHAR(128)  DEFAULT NULL,
    `created_at`           DATETIME      NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_login_log_user_created` (`app_user_id`, `created_at`),
    KEY `idx_login_log_ip_created`   (`login_ip`,    `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
