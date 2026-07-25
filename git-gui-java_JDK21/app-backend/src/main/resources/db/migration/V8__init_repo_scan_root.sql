-- ============================================================
-- V8__init_repo_scan_root.sql
-- 多仓库扫描根目录表：记忆用户选择的根目录，下次启动自动恢复列表
-- 创建时间：2026-07-24
-- 服务 BR：BR-01（多仓库检索）
-- @author FrankKang
-- @since 2026-07-24
-- ============================================================

-- 多仓库扫描根目录表
-- 同一根路径 upsert（已存在则更新 lastScannedAt / scanDepth）
-- 用于下次启动自动恢复左侧仓库列表
CREATE TABLE IF NOT EXISTS repo_scan_root (
    id              TEXT PRIMARY KEY NOT NULL,                               -- 主键 UUID
    root_path       TEXT NOT NULL UNIQUE,                                    -- 根目录绝对路径，唯一
    alias           TEXT DEFAULT '',                                         -- 用户自定义别名（便于识别）
    scan_depth      INTEGER NOT NULL DEFAULT 3,                              -- 扫描深度（1~10，BR-01）
    last_scanned_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),   -- 最后一次扫描时间
    enabled         INTEGER NOT NULL DEFAULT 1,                              -- 是否启用（0=禁用，1=启用）
    sort_order      INTEGER NOT NULL DEFAULT 0,                              -- 排序权重
    created_at      TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),   -- 创建时间
    updated_at      TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))    -- 更新时间
);

-- last_scanned_at 倒序索引：用于按时间倒序展示
CREATE INDEX IF NOT EXISTS idx_repo_scan_root_last_scanned_at ON repo_scan_root(last_scanned_at DESC);
-- sort_order 升序索引：用于排序展示
CREATE INDEX IF NOT EXISTS idx_repo_scan_root_sort_order ON repo_scan_root(sort_order);