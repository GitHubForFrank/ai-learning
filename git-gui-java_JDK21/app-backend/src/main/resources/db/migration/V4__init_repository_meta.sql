-- ============================================================
-- V4__init_repository_meta.sql
-- 仓库元信息缓存表
-- 创建时间：2026-07-23
-- 服务 BR：BR-01（检索缓存）、BR-02（异步刷新）
-- @author FrankKang
-- @since 2026-05-27
-- ============================================================

-- 仓库元信息缓存表：随每次打开/刷新更新
CREATE TABLE IF NOT EXISTS repository_meta (
    id                       TEXT PRIMARY KEY NOT NULL,                     -- 主键 UUID
    repo_path                TEXT NOT NULL UNIQUE,                          -- 仓库绝对路径，唯一
    current_branch           TEXT DEFAULT '',                               -- 当前分支
    head_commit              TEXT DEFAULT '',                               -- HEAD 提交哈希
    remote_url               TEXT DEFAULT '',                               -- 主远程 URL
    has_uncommitted_changes  INTEGER NOT NULL DEFAULT 0,                    -- 是否有未提交修改（0=否，1=是）
    last_synced_at           TEXT NOT NULL DEFAULT (datetime('now', 'localtime')), -- 元信息最后刷新时间
    created_at               TEXT NOT NULL DEFAULT (datetime('now', 'localtime')), -- 创建时间
    updated_at               TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))  -- 更新时间
);

CREATE INDEX IF NOT EXISTS idx_repository_meta_last_synced_at ON repository_meta(last_synced_at);
