package com.zmz.sdd.fullstack.core.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * [SDD-TASK: Task001]
 * [SDD-SPEC: 03-技术方案.md §5.5 + §9.5]
 *
 * Spring Boot 4.0 不再自带 Flyway 自动配置(已从 spring-boot-autoconfigure 中移除),
 * 这里显式构造 Flyway Bean 并通过 init-method=migrate 在容器启动时跑迁移。
 *
 * 此 Bean 在 DefaultAdminInitializer 之前完成初始化(后者是 CommandLineRunner,
 * 上下文 ready 后才跑),所以表已存在。
 */
@Configuration
public class FlywayConfig {

    @Value("${spring.flyway.locations:filesystem:../workitem-db/migration}")
    private String[] locations;

    @Value("${spring.flyway.baseline-on-migrate:false}")
    private boolean baselineOnMigrate;

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .baselineOnMigrate(baselineOnMigrate)
                .load();
    }
}
