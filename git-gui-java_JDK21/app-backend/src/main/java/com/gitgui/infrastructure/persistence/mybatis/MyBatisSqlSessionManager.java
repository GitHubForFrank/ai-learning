package com.gitgui.infrastructure.persistence.mybatis;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.gitgui.infrastructure.persistence.SqliteDataSource;
import com.gitgui.infrastructure.persistence.mapper.AppSettingsMapper;
import com.gitgui.infrastructure.persistence.mapper.AuditLogMapper;
import com.gitgui.infrastructure.persistence.mapper.FavoriteMapper;
import com.gitgui.infrastructure.persistence.mapper.OperationLogMapper;
import com.gitgui.infrastructure.persistence.mapper.RecentRepoMapper;
import com.gitgui.infrastructure.persistence.mapper.RepoScanRootMapper;
import com.gitgui.infrastructure.persistence.mapper.RepositoryMetaMapper;
import com.gitgui.infrastructure.persistence.mapper.TaskRecordMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.LocalDateTime;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MyBatis-Plus SqlSessionFactory 管理器。
 * <p>基于 SqliteDataSource 构建 MyBatis 环境，注册 Mapper 与自定义 TypeHandler。
 * 每次数据库操作通过 {@link #openSession()} 获取新会话，用完即关。</p>
 *
 * @author FrankKang
 * @since 2026-07-23
 */
@Singleton
public class MyBatisSqlSessionManager {

    private static final Logger log = LoggerFactory.getLogger(MyBatisSqlSessionManager.class);

    private final SqlSessionFactory sqlSessionFactory;

    @Inject
    public MyBatisSqlSessionManager(SqliteDataSource dataSource) {
        this.sqlSessionFactory = buildSqlSessionFactory(dataSource);
        log.info("MyBatis-Plus SqlSessionFactory 初始化完成");
    }

    /**
     * 打开一个新的 SqlSession。
     *
     * @return 新 SqlSession
     */
    public SqlSession openSession() {
        return sqlSessionFactory.openSession(true); // auto-commit
    }

    /**
     * 获取 Mapper 代理实例。
     *
     * @param mapperClass Mapper 接口类
     * @param <T>         Mapper 类型
     * @return Mapper 代理
     */
    public <T> T getMapper(Class<T> mapperClass) {
        return sqlSessionFactory.getConfiguration()
                                .getMapper(mapperClass, openSession());
    }

    private SqlSessionFactory buildSqlSessionFactory(SqliteDataSource dataSource) {
        MybatisConfiguration configuration = new MybatisConfiguration();

        // 环境配置
        org.apache.ibatis.mapping.Environment environment = new org.apache.ibatis.mapping.Environment("git-gui", new JdbcTransactionFactory(),
                                                                                                      dataSource);
        configuration.setEnvironment(environment);

        // 驼峰命名自动映射（repo_path ↔ repoPath）
        configuration.setMapUnderscoreToCamelCase(true);

        // 注册自定义 TypeHandler — LocalDateTime ↔ SQLite TEXT
        configuration.getTypeHandlerRegistry()
                     .register(LocalDateTime.class, SqliteLocalDateTimeTypeHandler.class);

        // MyBatis-Plus 全局配置
        GlobalConfig globalConfig = GlobalConfigUtils.defaults();
        GlobalConfigUtils.setGlobalConfig(configuration, globalConfig);

        // 注册 Mapper 接口
        configuration.addMapper(AppSettingsMapper.class);
        configuration.addMapper(AuditLogMapper.class);
        configuration.addMapper(FavoriteMapper.class);
        configuration.addMapper(OperationLogMapper.class);
        configuration.addMapper(RecentRepoMapper.class);
        configuration.addMapper(RepoScanRootMapper.class);
        configuration.addMapper(RepositoryMetaMapper.class);
        configuration.addMapper(TaskRecordMapper.class);

        // 必须使用 MybatisSqlSessionFactoryBuilder 而非原生 SqlSessionFactoryBuilder，
        // 否则 IdentifierGenerator / ISqlInjector 等 MyBatis-Plus 组件不会初始化，
        // insert 时 populateKeys 会因 identifierGenerator 为 null 抛 NPE
        return new MybatisSqlSessionFactoryBuilder().build(configuration);
    }
}
