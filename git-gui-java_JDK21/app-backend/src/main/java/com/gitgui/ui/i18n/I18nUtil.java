package com.gitgui.ui.i18n;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * 国际化工具
 * <p>通过 {@link ResourceBundle} 加载 {@code messages_zh/en.properties}。</p>
 * <p>遵循 BR-38：语言切换立即生效，无需重启。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public final class I18nUtil {

    private static final Logger log = LoggerFactory.getLogger(I18nUtil.class);

    /** 资源包基础名 */
    private static final String BASE_NAME = "i18n.messages";

    @Getter
    private static volatile ResourceBundle bundle = ResourceBundle.getBundle(BASE_NAME, Locale.SIMPLIFIED_CHINESE);

    private I18nUtil() {
        // 工具类禁止实例化
    }

    /**
     * 获取当前语言对应的国际化文本。
     *
     * @param key 资源键
     * @return 国际化文本，找不到返回 key
     */
    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            log.warn("国际化资源缺失：key={}", key);
            return key;
        }
    }

    /**
     * 切换语言（BR-38 立即生效）。
     *
     * @param language 语言代码（zh/en）
     */
    public static void switchLanguage(String language) {
        Locale locale = "en".equalsIgnoreCase(language)
                ? Locale.ENGLISH
                : Locale.SIMPLIFIED_CHINESE;
        bundle = ResourceBundle.getBundle(BASE_NAME, locale);
        log.info("语言切换：{}", language);
    }

}
