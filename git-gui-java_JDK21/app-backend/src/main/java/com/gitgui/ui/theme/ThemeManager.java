package com.gitgui.ui.theme;

import javafx.scene.Scene;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 主题管理器
 * <p>遵循 BR-38：支持浅色/深色/跟随系统主题切换，立即生效无需重启。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class ThemeManager {

    private static final Logger log = LoggerFactory.getLogger(ThemeManager.class);

    /** 当前主题场景引用 */
    private Scene scene;
    @Getter
    private String currentTheme = "DARK";

    /**
     * 绑定场景。
     *
     * @param scene JavaFX 场景
     */
    public void bindScene(Scene scene) {
        this.scene = scene;
        applyTheme(currentTheme);
    }

    /**
     * 应用主题。
     *
     * @param theme 主题名：LIGHT / DARK / SYSTEM
     */
    public void applyTheme(String theme) {
        this.currentTheme = theme;
        if (scene == null) {
            return;
        }
        // 清除旧主题样式表
        scene.getStylesheets().clear();
        String css;
        if ("SYSTEM".equalsIgnoreCase(theme)) {
            // 跟随系统：检测系统暗色模式（简化实现，默认浅色）
            css = "LIGHT".equalsIgnoreCase(detectSystemTheme()) ? "/css/light.css" : "/css/dark.css";
        } else if ("DARK".equalsIgnoreCase(theme)) {
            css = "/css/dark.css";
        } else {
            css = "/css/light.css";
        }
        try {
            scene.getStylesheets().add(ThemeManager.class.getResource(css).toExternalForm());
            log.info("主题应用：{}", theme);
        } catch (Exception e) {
            log.warn("加载主题样式表失败：{}", css, e);
        }
    }

    /**
     * 检测系统主题（简化实现，默认浅色）。
     *
     * @return LIGHT / DARK
     */
    private String detectSystemTheme() {
        // 简化实现：实际可通过系统属性或 JavaFX 平台 API 检测
        return "LIGHT";
    }
}
