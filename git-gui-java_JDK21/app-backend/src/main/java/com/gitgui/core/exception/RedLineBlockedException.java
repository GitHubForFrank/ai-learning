package com.gitgui.core.exception;

import lombok.Getter;

/**
 * 命令红线阻断异常
 * <p>当 {@code CommandInterceptor} 检测到 {@code RedLineResult.Action.BLOCK} 时抛出。</p>
 * <p>UI 据此展示安全等价命令提示（如「请改用 --force-with-lease」）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
@Getter
public class RedLineBlockedException extends GitGuiException {

    private final String ruleCode;

    /**
     * 构造阻断异常。
     *
     * @param ruleCode 红线规则代码
     * @param message  阻断原因与安全等价命令建议
     */
    public RedLineBlockedException(String ruleCode, String message) {
        super(ErrorCode.RED_LINE_BLOCKED, message);
        this.ruleCode = ruleCode;
    }

}
