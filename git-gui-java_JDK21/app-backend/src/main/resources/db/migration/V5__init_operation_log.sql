-- ============================================================
-- V5__init_operation_log.sql
-- 操作日志表
-- 创建时间：2026-07-23
-- 服务 BR：BR-35（任务结果记录）
-- @author FrankKang
-- @since 2026-05-27
-- ============================================================

-- 操作日志表：每次 Git 操作完成（成功/失败/取消）后追加写入一条，不可修改/删除
CREATE TABLE IF NOT EXISTS operation_log (
    id            TEXT PRIMARY KEY NOT NULL,                                -- 主键 UUID
    repo_path     TEXT NOT NULL,                                            -- 目标仓库路径
    operation     TEXT NOT NULL,                                            -- 操作类型枚举（COMMIT/PULL/PUSH/...）
    command       TEXT DEFAULT '',                                          -- 实际执行的 git 命令或 JGit API 描述
    args          TEXT DEFAULT '',                                          -- 参数 JSON
    success       INTEGER NOT NULL DEFAULT 1,                               -- 是否成功（0=失败，1=成功）
    duration_ms   INTEGER DEFAULT 0,                                        -- 耗时（毫秒）
    error_message TEXT DEFAULT '',                                          -- 错误信息（中文友好提示）
    task_id       TEXT DEFAULT '',                                          -- 关联异步任务 ID
    created_at    TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))      -- 创建时间
);

CREATE INDEX IF NOT EXISTS idx_operation_log_repo_path ON operation_log(repo_path);
CREATE INDEX IF NOT EXISTS idx_operation_log_operation ON operation_log(operation);
CREATE INDEX IF NOT EXISTS idx_operation_log_created_at ON operation_log(created_at);
CREATE INDEX IF NOT EXISTS idx_operation_log_success ON operation_log(success);
