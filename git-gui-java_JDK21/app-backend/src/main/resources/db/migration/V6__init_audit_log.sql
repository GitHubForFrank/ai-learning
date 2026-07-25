-- ============================================================
-- V6__init_audit_log.sql
-- 红线审计日志表
-- 创建时间：2026-07-23
-- 服务 BR：BR-31（红线审计追加写入不可删除）
-- @author FrankKang
-- @since 2026-05-27
-- ============================================================

-- 红线审计日志表：所有命中红线操作追加写入，不可修改/删除（BR-31）
CREATE TABLE IF NOT EXISTS audit_log (
    id            TEXT PRIMARY KEY NOT NULL,                                -- 主键 UUID
    rule_code     TEXT NOT NULL,                                            -- 红线规则代码（如 RED_FORCE_PUSH）
    command       TEXT DEFAULT '',                                          -- 命中红线的命令
    repo_path     TEXT NOT NULL,                                            -- 仓库路径
    branch        TEXT DEFAULT '',                                          -- 涉及分支
    remote_url    TEXT DEFAULT '',                                          -- 涉及远程 URL
    action        TEXT NOT NULL,                                            -- 红线类型（BLOCK/CONFIRM）
    action_result TEXT NOT NULL,                                            -- 处理结果（BLOCKED/CONFIRMED/CANCELLED）
    detail        TEXT DEFAULT '',                                          -- 详情 JSON（如命中敏感文件清单）
    created_at    TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))      -- 创建时间
);

CREATE INDEX IF NOT EXISTS idx_audit_log_rule_code ON audit_log(rule_code);
CREATE INDEX IF NOT EXISTS idx_audit_log_repo_path ON audit_log(repo_path);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_action_result ON audit_log(action_result);
