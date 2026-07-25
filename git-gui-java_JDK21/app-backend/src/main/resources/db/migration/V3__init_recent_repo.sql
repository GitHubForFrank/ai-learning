-- ============================================================
-- V3__init_recent_repo.sql
-- 最近仓库表
-- 创建时间：2026-07-23
-- 服务 BR：BR-05（倒序与淘汰）
-- @author FrankKang
-- @since 2026-05-27
-- ============================================================

-- 最近仓库表：打开仓库时 upsert，超限淘汰最旧
CREATE TABLE IF NOT EXISTS recent_repo (
    id             TEXT PRIMARY KEY NOT NULL,                               -- 主键 UUID
    repo_path      TEXT NOT NULL UNIQUE,                                    -- 仓库绝对路径，唯一
    last_branch    TEXT DEFAULT '',                                         -- 上次打开的分支
    last_opened_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),   -- 最后打开时间
    open_count     INTEGER NOT NULL DEFAULT 1,                              -- 累计打开次数
    created_at     TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),   -- 创建时间
    updated_at     TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))    -- 更新时间
);

-- last_opened_at 倒序索引：用于按时间倒序展示与淘汰最旧
CREATE INDEX IF NOT EXISTS idx_recent_repo_last_opened_at ON recent_repo(last_opened_at DESC);
