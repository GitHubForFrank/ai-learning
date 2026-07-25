package com.gitgui.di;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.gitgui.core.config.AppConfig;

/**
 * 应用核心 Module
 * <p>绑定核心常量、路径、异常处理器。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class AppModule extends AbstractModule {

    @Override
    protected void configure() {
        // 核心常量绑定（AppConfig 为静态工具类，无需绑定）
        // 异常处理器绑定可在此扩展
    }
}
