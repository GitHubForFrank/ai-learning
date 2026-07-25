package com.gitgui.core.redline.rule;

import com.gitgui.core.constant.RedLineCode;
import com.gitgui.core.redline.AbstractRedLineRule;
import com.gitgui.core.util.WildcardUtil;
import com.gitgui.domain.redline.RedLineContext;
import com.gitgui.domain.redline.RedLineResult;
import com.gitgui.domain.service.SettingsService;
import com.google.inject.Inject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * 推送到非授权远程规则（BLOCK，BR-26/BR-28）
 * <p>远程白名单在 Settings 配置，支持域名通配；空列表表示不限制。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class RemoteWhitelistRule extends AbstractRedLineRule {

    @Inject
    public RemoteWhitelistRule(SettingsService settingsService) {
        super(settingsService);
    }

    @Override
    protected RedLineResult doCheck(RedLineContext ctx) {
        // 命中条件：Push 操作且远程 URL 不在白名单
        if (ctx.getRemoteUrl() == null || ctx.getRemoteUrl().isBlank()) {
            return RedLineResult.pass();
        }
        List<String> whitelist = settingsService.getRemoteWhitelist();
        if (whitelist == null || whitelist.isEmpty()) {
            // 空白名单表示不限制（BR-28）
            return RedLineResult.pass();
        }
        String host = extractHost(ctx.getRemoteUrl());
        if (host == null) {
            return RedLineResult.pass();
        }
        for (String pattern : whitelist) {
            if (WildcardUtil.match(pattern, host) || WildcardUtil.match(pattern, ctx.getRemoteUrl())) {
                return RedLineResult.pass();
            }
        }
        return RedLineResult.block(RedLineCode.RED_REMOTE_WHITELIST,
                "禁止推送到非授权远程：" + ctx.getRemoteUrl() + "（不在白名单），请检查配置或改用授权远程");
    }

    /**
     * 从 URL 提取 host（支持 https/ssh 协议）。
     */
    private String extractHost(String url) {
        try {
            if (url.startsWith("git@") || url.startsWith("ssh://")) {
                // SSH 格式：git@host:path 或 ssh://user@host:port/path
                String rest = url.startsWith("git@") ? url.substring("git@".length()) : url.substring("ssh://".length());
                int at = rest.indexOf('@');
                if (at >= 0) {
                    rest = rest.substring(at + 1);
                }
                int colon = rest.indexOf(':');
                int slash = rest.indexOf('/');
                int end = -1;
                if (colon >= 0 && (slash < 0 || colon < slash)) {
                    end = colon;
                } else if (slash >= 0) {
                    end = slash;
                }
                return end >= 0 ? rest.substring(0, end) : rest;
            }
            return new URI(url).getHost();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    public String ruleCode() {
        return RedLineCode.RED_REMOTE_WHITELIST.name();
    }
}
