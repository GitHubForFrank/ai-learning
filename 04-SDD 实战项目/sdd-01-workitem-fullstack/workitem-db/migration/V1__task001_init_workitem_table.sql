-- [SDD-TASK: Task001]
-- [SDD-SPEC: 02-功能规范.md §8 DB-01/DB-02 + 03-技术方案.md §5.1] workitem 表 + 2 个索引

CREATE TABLE `workitem` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT,
    `title`       VARCHAR(100)  NOT NULL,
    `description` VARCHAR(500)  DEFAULT NULL,
    `status`      VARCHAR(16)   NOT NULL DEFAULT 'TODO',
    `priority`    VARCHAR(16)   NOT NULL DEFAULT 'MEDIUM',
    `due_date`    DATE          DEFAULT NULL,
    `created_at`  DATETIME      NOT NULL,
    `updated_at`  DATETIME      NOT NULL,
    `deleted`     TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_workitem_status_deleted`  (`status`, `deleted`),
    KEY `idx_workitem_deleted_created` (`deleted`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
