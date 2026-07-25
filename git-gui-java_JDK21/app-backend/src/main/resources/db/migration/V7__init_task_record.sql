-- ============================================================
-- V7__init_task_record.sql
-- 异步任务记录表
-- 创建时间：2026-07-23
-- 服务 BR：BR-33（异步可取消）、BR-34（同仓库写串行）、BR-35（结果记录）、BR-36（后台执行）
-- @author FrankKang
-- @since 2026-05-27
-- ============================================================

-- 异步任务记录表：任务状态持久化，重启后可恢复未完成任务状态展示
-- 状态机：PENDING → RUNNING → (SUCCESS | FAILED | CANCELLED)，终态不可回退
CREATE TABLE IF NOT EXISTS task_record (
    id           TEXT PRIMARY KEY NOT NULL,                                 -- 主键 UUID
    task_type    TEXT NOT NULL,                                             -- 任务类型（CLONE/PULL/PUSH/FETCH/COMMIT/MERGE/REBASE/MULTI_REPO_SCAN/GC/CHECKOUT/STASH/STATUS）
    repo_path    TEXT DEFAULT '',                                           -- 仓库路径（多仓库检索可为空）
    status       TEXT NOT NULL DEFAULT 'PENDING',                           -- 状态（PENDING/RUNNING/SUCCESS/FAILED/CANCELLED）
    progress     INTEGER NOT NULL DEFAULT 0,                                -- 进度 0-100
    message      TEXT DEFAULT '',                                           -- 进度描述
    output       TEXT DEFAULT '',                                           -- 命令输出/错误堆栈
    cancellable  INTEGER NOT NULL DEFAULT 1,                                -- 是否可取消（0=否，1=是）
    started_at   TEXT DEFAULT NULL,                                         -- 开始时间
    finished_at  TEXT DEFAULT NULL,                                         -- 结束时间
    created_at   TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))       -- 创建时间
);

CREATE INDEX IF NOT EXISTS idx_task_record_repo_path ON task_record(repo_path);
CREATE INDEX IF NOT EXISTS idx_task_record_status ON task_record(status);
CREATE INDEX IF NOT EXISTS idx_task_record_task_type ON task_record(task_type);
CREATE INDEX IF NOT EXISTS idx_task_record_created_at ON task_record(created_at);
