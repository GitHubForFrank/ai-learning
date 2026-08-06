package com.gitgui.di;

import com.gitgui.domain.repository.AppSettingsRepository;
import com.gitgui.domain.repository.AuditLogRepository;
import com.gitgui.domain.repository.FavoriteRepository;
import com.gitgui.domain.repository.OperationLogRepository;
import com.gitgui.domain.repository.RecentRepoRepository;
import com.gitgui.domain.repository.RepoScanRootRepository;
import com.gitgui.domain.repository.RepositoryMetaRepository;
import com.gitgui.domain.repository.TaskRecordRepository;
import com.gitgui.infrastructure.persistence.SqliteDataSource;
import com.gitgui.infrastructure.persistence.mybatis.MyBatisSqlSessionManager;
import com.gitgui.infrastructure.persistence.repository.AppSettingsRepositoryImpl;
import com.gitgui.infrastructure.persistence.repository.AuditLogRepositoryImpl;
import com.gitgui.infrastructure.persistence.repository.FavoriteRepositoryImpl;
import com.gitgui.infrastructure.persistence.repository.OperationLogRepositoryImpl;
import com.gitgui.infrastructure.persistence.repository.RecentRepoRepositoryImpl;
import com.gitgui.infrastructure.persistence.repository.RepoScanRootRepositoryImpl;
import com.gitgui.infrastructure.persistence.repository.RepositoryMetaRepositoryImpl;
import com.gitgui.infrastructure.persistence.repository.TaskRecordRepositoryImpl;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/**
 * 数据库 Module（MyBatis-Plus）
 * <p>绑定 SQLite DataSource、MyBatisSqlSessionManager、各 Repository 接口 → 实现。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class DatabaseModule extends AbstractModule {

    @Override
    protected void configure() {
        // MyBatis-Plus SqlSession 管理器（含 SqlSessionFactory 构建）
        bind(MyBatisSqlSessionManager.class).in(Singleton.class);

        // Repository 接口 → MyBatis-Plus 实现
        bind(AppSettingsRepository.class).to(AppSettingsRepositoryImpl.class)
                                         .in(Singleton.class);
        bind(AuditLogRepository.class).to(AuditLogRepositoryImpl.class)
                                      .in(Singleton.class);
        bind(FavoriteRepository.class).to(FavoriteRepositoryImpl.class)
                                      .in(Singleton.class);
        bind(OperationLogRepository.class).to(OperationLogRepositoryImpl.class)
                                          .in(Singleton.class);
        bind(RecentRepoRepository.class).to(RecentRepoRepositoryImpl.class)
                                        .in(Singleton.class);
        bind(RepoScanRootRepository.class).to(RepoScanRootRepositoryImpl.class)
                                          .in(Singleton.class);
        bind(RepositoryMetaRepository.class).to(RepositoryMetaRepositoryImpl.class)
                                            .in(Singleton.class);
        bind(TaskRecordRepository.class).to(TaskRecordRepositoryImpl.class)
                                        .in(Singleton.class);

        // SQLite DataSource（实现 javax.sql.DataSource，供 MyBatis 环境使用）
        bind(SqliteDataSource.class).in(Singleton.class);
    }
}
