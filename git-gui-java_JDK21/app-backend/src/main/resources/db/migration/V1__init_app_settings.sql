-- ============================================================
-- V1__init_app_settings.sql
-- 应用设置表 + 内置默认设置数据
-- 创建时间：2026-07-23
-- 执行前提：无（首版迁移）
-- 服务 BR：BR-27（保护分支清单）、BR-28（远程白名单）、BR-32（敏感文件规则）、BR-38（主题/语言/快捷键）
-- @author FrankKang
-- @since 2026-05-27
-- ============================================================

-- 应用设置表：key-value 持久化，UI 修改即更新
CREATE TABLE IF NOT EXISTS app_settings (
    id          TEXT PRIMARY KEY NOT NULL,                                  -- 主键 UUID
    key         TEXT NOT NULL UNIQUE,                                       -- 设置键，唯一
    value       TEXT NOT NULL DEFAULT '',                                   -- 设置值（JSON 字符串承载复杂结构）
    category    TEXT NOT NULL DEFAULT 'UI',                                 -- 分类：RED_LINE/UI/GIT/EXTERNAL_TOOL/SSH/REPO_SCAN/RECENT
    description TEXT DEFAULT '',                                             -- 设置说明
    updated_at  TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))        -- 更新时间
);

CREATE INDEX IF NOT EXISTS idx_app_settings_category ON app_settings(category);

-- 内置默认设置数据（V1 必须插入，避免应用首次启动时红线配置缺失）
-- 红线总开关：默认开启（BR-30）
INSERT INTO app_settings (id, key, value, category, description) VALUES
('10000000-0000-0000-0000-000000000001', 'red_line.enabled', 'true', 'RED_LINE', '命令红线总开关，关闭时所有阻断降级为二次确认');

-- 保护分支清单：默认含 main/master/develop（BR-27）
INSERT INTO app_settings (id, key, value, category, description) VALUES
('10000000-0000-0000-0000-000000000002', 'red_line.protected_branches', '["main","master","develop","release/*"]', 'RED_LINE', '保护分支清单，支持通配符（如 release/*），force push 与删除均阻断');

-- 远程白名单：默认空（不限制），由用户在 Settings 中配置（BR-28）
INSERT INTO app_settings (id, key, value, category, description) VALUES
('10000000-0000-0000-0000-000000000003', 'red_line.remote_whitelist', '[]', 'RED_LINE', '允许推送的远程 URL 域名清单，支持通配；空列表表示不限制');

-- 敏感文件规则默认集（BR-32）：内置常见模式
INSERT INTO app_settings (id, key, value, category, description) VALUES
('10000000-0000-0000-0000-000000000004', 'red_line.sensitive_file_rules', '[{"pattern":"\\.env(\\..*)?$","description":"环境变量文件"},{"pattern":"credentials","description":"凭证文件"},{"pattern":"\\.pem$","description":"PEM 证书"},{"pattern":"\\.key$","description":"私钥文件"},{"pattern":"id_rsa","description":"SSH 私钥"},{"pattern":"\\.npmrc$","description":"npm 凭证文件"}]', 'RED_LINE', '敏感文件规则，命中即阻断推送并提示加入 .gitignore');

-- 推送超大文件阈值：默认 50MB（BR-29）
INSERT INTO app_settings (id, key, value, category, description) VALUES
('10000000-0000-0000-0000-000000000005', 'red_line.large_file_threshold_mb', '50', 'RED_LINE', '推送超大文件阈值（MB，非 LFS），超过走二次确认');

-- 主题：默认浅色（BR-38）
INSERT INTO app_settings (id, key, value, category, description) VALUES
('10000000-0000-0000-0000-000000000006', 'ui.theme', 'DARK', 'UI', '主题：LIGHT/DARK/SYSTEM');

-- 语言：默认中文（BR-38）
INSERT INTO app_settings (id, key, value, category, description) VALUES
('10000000-0000-0000-0000-000000000007', 'ui.language', 'zh', 'UI', '语言：zh/en');

-- 快捷键自定义（BR-38）
INSERT INTO app_settings (id, key, value, category, description) VALUES
('10000000-0000-0000-0000-000000000008', 'ui.shortcuts', '{}', 'UI', '快捷键自定义映射 JSON');

-- 多仓库检索默认深度：3 层（BR-01）
INSERT INTO app_settings (id, key, value, category, description) VALUES
('10000000-0000-0000-0000-000000000009', 'repo_scan.default_depth', '3', 'REPO_SCAN', '多仓库自动检索默认深度（1~10）');

-- 最近仓库列表最大保留：20 条（BR-05）
INSERT INTO app_settings (id, key, value, category, description) VALUES
('10000000-0000-0000-0000-000000000010', 'recent_repo.max_keep', '20', 'RECENT', '最近仓库列表最大保留条数，超限淘汰最旧');

-- 外部 Diff 工具（BR-37 外部工具关联）
INSERT INTO app_settings (id, key, value, category, description) VALUES
('10000000-0000-0000-0000-000000000011', 'external.diff_tool', '', 'EXTERNAL_TOOL', '外部 Diff 工具命令模板，支持 $BASE/$LOCAL/$REMOTE/$MERGED 占位符');

-- 外部 Merge 工具
INSERT INTO app_settings (id, key, value, category, description) VALUES
('10000000-0000-0000-0000-000000000012', 'external.merge_tool', '', 'EXTERNAL_TOOL', '外部合并工具命令模板');

-- 外部编辑器
INSERT INTO app_settings (id, key, value, category, description) VALUES
('10000000-0000-0000-0000-000000000013', 'external.editor', '', 'EXTERNAL_TOOL', '外部文本编辑器命令模板');

-- SSH 默认密钥路径（BR-39：仅存储路径不存密钥内容）
INSERT INTO app_settings (id, key, value, category, description) VALUES
('10000000-0000-0000-0000-000000000014', 'ssh.default_key_path', '', 'SSH', 'SSH 默认密钥文件路径，仅存路径不存内容');
