package com.gitgui.di;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.gitgui.application.redline.CommandInterceptor;
import com.gitgui.application.redline.CommandRedLineServiceImpl;
import com.gitgui.core.redline.rule.AmendPushedRule;
import com.gitgui.core.redline.rule.CleanFdxRule;
import com.gitgui.core.redline.rule.DeleteProtectedBranchRule;
import com.gitgui.core.redline.rule.FilterBranchRule;
import com.gitgui.core.redline.rule.ForcePushRule;
import com.gitgui.core.redline.rule.LargeFileRule;
import com.gitgui.core.redline.rule.NoVerifyRule;
import com.gitgui.core.redline.rule.ProtectedBranchRule;
import com.gitgui.core.redline.rule.RebasePushedRule;
import com.gitgui.core.redline.rule.RedLineToggleRule;
import com.gitgui.core.redline.rule.RemoteWhitelistRule;
import com.gitgui.core.redline.rule.ResetHardRule;
import com.gitgui.core.redline.rule.SensitiveFileRule;
import com.gitgui.domain.redline.RedLineRule;
import com.gitgui.domain.service.CommandRedLineService;

/**
 * 红线 Module
 * <p>绑定 {@link CommandRedLineService} 与 {@link CommandInterceptor}，
 * 通过 {@link Multibinder} 收集 13 个 {@link RedLineRule} 实现类。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class RedLineModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(CommandRedLineService.class).to(CommandRedLineServiceImpl.class).in(Singleton.class);
        bind(CommandInterceptor.class).in(Singleton.class);

        // 收集 13 个红线规则到 Multibinder
        Multibinder<RedLineRule> ruleBinder = Multibinder.newSetBinder(binder(), RedLineRule.class);
        // 阻断类（BLOCK，BR-26）
        ruleBinder.addBinding().to(ForcePushRule.class);
        ruleBinder.addBinding().to(ProtectedBranchRule.class);
        ruleBinder.addBinding().to(DeleteProtectedBranchRule.class);
        ruleBinder.addBinding().to(SensitiveFileRule.class);
        ruleBinder.addBinding().to(RemoteWhitelistRule.class);
        ruleBinder.addBinding().to(NoVerifyRule.class);
        // 二次确认类（CONFIRM，BR-29）
        ruleBinder.addBinding().to(ResetHardRule.class);
        ruleBinder.addBinding().to(CleanFdxRule.class);
        ruleBinder.addBinding().to(AmendPushedRule.class);
        ruleBinder.addBinding().to(RebasePushedRule.class);
        ruleBinder.addBinding().to(FilterBranchRule.class);
        ruleBinder.addBinding().to(LargeFileRule.class);
        ruleBinder.addBinding().to(RedLineToggleRule.class);
    }
}
