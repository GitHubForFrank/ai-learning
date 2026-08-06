package com.gitgui.di;

import com.gitgui.infrastructure.cli.CliGitExecutor;
import com.gitgui.infrastructure.credential.SystemCredentialHelper;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/**
 * Git Module
 * <p>绑定 CLI Git 适配器与 Credential Helper。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class GitModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(CliGitExecutor.class).in(Singleton.class);
        bind(SystemCredentialHelper.class).in(Singleton.class);
    }
}
