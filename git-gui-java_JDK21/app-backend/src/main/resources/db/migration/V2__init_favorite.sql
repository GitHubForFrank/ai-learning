-- ============================================================
-- V2__init_favorite.sql
-- 收藏表
-- 创建时间：2026-07-23
-- 服务 BR：BR-03（唯一性）、BR-04（置顶与排序）
-- @author FrankKang
-- @since 2026-05-27
-- ============================================================

-- 收藏表：常用仓库标记收藏，置顶展示
CREATE TABLE IF NOT EXISTS favorite (
    id          TEXT PRIMARY KEY NOT NULL,                                  -- 主键 UUID
    repo_path   TEXT NOT NULL UNIQUE,                                       -- 仓库绝对路径，全局唯一（BR-03）
    alias       TEXT DEFAULT '',                                            -- 别名（便于识别，最大 100 字符）
    "group"     TEXT DEFAULT '',                                            -- 分组（如项目/团队，最大 50 字符）
    pinned      INTEGER NOT NULL DEFAULT 0,                                 -- 是否置顶（0=否，1=是）
    sort_order  INTEGER NOT NULL DEFAULT 0,                                 -- 排序权重
    remote_url  TEXT DEFAULT '',                                            -- 远程 URL（缓存便于列表展示）
    created_at  TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),      -- 创建时间
    updated_at  TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))       -- 更新时间
);

CREATE INDEX IF NOT EXISTS idx_favorite_group ON favorite("group");
CREATE INDEX IF NOT EXISTS idx_favorite_pinned ON favorite(pinned);
CREATE INDEX IF NOT EXISTS idx_favorite_sort_order ON favorite(sort_order);
