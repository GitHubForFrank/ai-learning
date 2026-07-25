package com.gitgui.domain.model;

import lombok.Builder;
import lombok.Data;

/**
 * 远程仓库配置领域模型
 * <p>对应 PRD 4.14，仓库所有 Remote 名称及对应 URL。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
@Data
@Builder
public class RemoteConfig {

    /** Remote 名称（如 origin） */
    private String name;

    /** Fetch URL */
    private String fetchUrl;

    /** Push URL（可空，与 fetchUrl 相同） */
    private String pushUrl;

    /** 是否默认 push remote */
    private boolean defaultPush;

    /** 是否默认 pull remote */
    private boolean defaultPull;

    /**
     * ListView 显示用的友好格式：「name  fetchUrl」（替换默认 Lombok toString）。
     * <p>修复 issue：「RemoteConfig(name=origin, fet...)」对用户不友好。</p>
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name == null ? "" : name);
        if (fetchUrl != null && !fetchUrl.isEmpty()) {
            sb.append("  ").append(fetchUrl);
        }
        return sb.toString();
    }
}
