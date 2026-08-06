package com.gitgui.infrastructure.credential;

import com.gitgui.infrastructure.cli.GitProcessBuilder;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 系统 credential helper 适配
 * <p>遵循 BR-39：通过系统 credential helper（osxkeychain/wincred/manager-core）管理凭证，
 * 应用不明文存储密码；SSH 密钥仅存储路径不存密钥内容。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class SystemCredentialHelper {

    private static final Logger log = LoggerFactory.getLogger(SystemCredentialHelper.class);

    /**
     * 自动检测系统 credential helper。
     * <p>Windows: manager-core；macOS: osxkeychain；Linux: store。</p>
     *
     * @return helper 名称
     */
    public String detectHelper() {
        String osName = System.getProperty("os.name", "")
                              .toLowerCase();
        if (osName.contains("win")) {
            return "manager-core";
        } else if (osName.contains("mac")) {
            return "osxkeychain";
        } else {
            return "store";
        }
    }

    /**
     * 配置 credential helper（git config --global credential.helper <helper>）。
     *
     * @param repoPath 仓库路径（null 表示全局配置）
     * @param helper   helper 名称
     */
    public void configure(String repoPath, String helper) {
        try {
            GitProcessBuilder.execute(repoPath, List.of("config", "--global", "credential.helper", helper), null);
            log.info("credential helper 已配置：{}", helper);
        } catch (Exception e) {
            log.warn("配置 credential helper 失败：{}", e.getMessage());
        }
    }
}
