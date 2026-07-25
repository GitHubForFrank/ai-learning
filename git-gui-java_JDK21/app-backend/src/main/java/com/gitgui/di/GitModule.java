package com.gitgui.di;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.gitgui.infrastructure.cli.CliGitExecutor;
import com.gitgui.infrastructure.credential.SystemCredentialHelper;
import com.gitgui.infrastructure.jgit.JGitOperationExecutor;

/**
 * Git Module
 * <p>绑定 JGit 适配器主实现、CLI 兜底适配器、Credential Helper。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class GitModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(JGitOperationExecutor.class).in(Singleton.class);
        bind(CliGitExecutor.class).in(Singleton.class);
        bind(SystemCredentialHelper.class).in(Singleton.class);
    }
}
