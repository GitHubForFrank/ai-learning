package com.zmz.sdd.fullstack.core.config;

import com.zmz.sdd.fullstack.app.infrastructure.dao.entity.AppUserEntity;
import com.zmz.sdd.fullstack.app.infrastructure.dao.mapper.AppUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * [SDD-TASK: Task001]
 * [SDD-SPEC: 02-功能规范.md §8 DB-06 + 03-技术方案.md §5.1bis V4]
 *
 * V4 INSERT 写入占位符 __BCRYPT_PLACEHOLDER__,本初始化器在启动时检测并替换为真实 BCrypt 哈希。
 * 这样:
 *   - Flyway 仍然版本化种子(`workitem-db/migration/V4`)
 *   - SQL 不嵌入实际哈希值(secret 派生物)
 *   - 启动后该用户已可用,密码 = application.properties 的 auth.default-admin.password
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultAdminInitializer implements CommandLineRunner {

    public static final String PLACEHOLDER = "__BCRYPT_PLACEHOLDER__";

    private final AppUserMapper appUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${auth.default-admin.username}")
    private String defaultUsername;

    @Value("${auth.default-admin.password}")
    private String defaultPassword;

    @Override
    public void run(String... args) {
        AppUserEntity admin = appUserMapper.findByUsernameIncludingPlaceholder(defaultUsername);
        if (admin == null) {
            log.info("default admin user '{}' not found; V4 seed missing or schema not migrated yet", defaultUsername);
            return;
        }
        if (PLACEHOLDER.equals(admin.getPasswordHash())) {
            String hash = passwordEncoder.encode(defaultPassword);
            appUserMapper.updatePasswordHashById(admin.getId(), hash);
            log.info("default admin '{}' password initialized (BCrypt hash applied)", defaultUsername);
        } else {
            log.debug("default admin '{}' already initialized; skip", defaultUsername);
        }
    }
}
