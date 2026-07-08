-- [SDD-TASK: Task001]
-- [SDD-SPEC: 02-功能规范.md §8 DB-06 + 03-技术方案.md §5.1bis] 默认管理员种子
-- 默认账号:admin / 默认密码:12346#@&
--
-- 安全说明:
-- - password_hash 列存放 BCrypt(strength=12) 占位字符串 __BCRYPT_PLACEHOLDER__
-- - Spring Boot 启动时 DefaultAdminInitializer (CommandLineRunner) 检测占位符并替换为真实 BCrypt 哈希
-- - 这避免了把哈希值写死在 SQL 里(virgin DB 的可重现性 + 不嵌入 secret 派生物)
-- - 默认密码仅教学用,生产场景必须强制首次登录改密码 + 走 secrets manager
--   (见 conventions/security-conventions.md §13.1)

INSERT INTO `app_user`
    (`username`, `password_hash`, `status`, `failed_login_count`, `created_at`, `updated_at`, `deleted`)
VALUES
    ('admin', '__BCRYPT_PLACEHOLDER__', 'ACTIVE', 0, NOW(), NOW(), 0);
