package com.gitgui.di;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.gitgui.application.service.AsyncTaskServiceImpl;
import com.gitgui.application.service.ConflictResolveServiceImpl;
import com.gitgui.application.service.FavoriteServiceImpl;
import com.gitgui.application.service.GitOperationServiceImpl;
import com.gitgui.application.service.OperationLogServiceImpl;
import com.gitgui.application.service.RecentRepoServiceImpl;
import com.gitgui.application.service.RemoteConfigServiceImpl;
import com.gitgui.application.service.RepoScanRootServiceImpl;
import com.gitgui.application.service.RepositoryServiceImpl;
import com.gitgui.application.service.SettingsServiceImpl;
import com.gitgui.application.service.StatusServiceImpl;
import com.gitgui.domain.service.AsyncTaskService;
import com.gitgui.domain.service.ConflictResolveService;
import com.gitgui.domain.service.FavoriteService;
import com.gitgui.domain.service.GitOperationService;
import com.gitgui.domain.service.OperationLogService;
import com.gitgui.domain.service.RecentRepoService;
import com.gitgui.domain.service.RemoteConfigService;
import com.gitgui.domain.service.RepoScanRootService;
import com.gitgui.domain.service.RepositoryService;
import com.gitgui.domain.service.SettingsService;
import com.gitgui.domain.service.StatusService;

/**
 * 服务 Module
 * <p>绑定 {@code domain/service/*} 接口 → {@code application/service/*} 实现。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class ServiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(AsyncTaskService.class).to(AsyncTaskServiceImpl.class).in(Singleton.class);
        bind(ConflictResolveService.class).to(ConflictResolveServiceImpl.class).in(Singleton.class);
        bind(FavoriteService.class).to(FavoriteServiceImpl.class).in(Singleton.class);
        bind(GitOperationService.class).to(GitOperationServiceImpl.class).in(Singleton.class);
        bind(OperationLogService.class).to(OperationLogServiceImpl.class).in(Singleton.class);
        bind(RecentRepoService.class).to(RecentRepoServiceImpl.class).in(Singleton.class);
        bind(RemoteConfigService.class).to(RemoteConfigServiceImpl.class).in(Singleton.class);
        bind(RepoScanRootService.class).to(RepoScanRootServiceImpl.class).in(Singleton.class);
        bind(RepositoryService.class).to(RepositoryServiceImpl.class).in(Singleton.class);
        bind(SettingsService.class).to(SettingsServiceImpl.class).in(Singleton.class);
        bind(StatusService.class).to(StatusServiceImpl.class).in(Singleton.class);
    }
}
