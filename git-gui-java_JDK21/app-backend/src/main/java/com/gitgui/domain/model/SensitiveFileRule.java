package com.gitgui.domain.model;

import lombok.Builder;
import lombok.Data;

/**
 * 敏感文件规则领域模型
 * <p>内置默认规则集（BR-32）：.env/credentials/*.pem/*.key/id_rsa/.npmrc。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
@Data
@Builder
public class SensitiveFileRule {

    /** 正则表达式模式 */
    private String pattern;

    /** 中文描述 */
    private String description;
}
