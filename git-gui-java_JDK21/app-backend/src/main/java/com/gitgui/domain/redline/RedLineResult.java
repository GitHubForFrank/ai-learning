package com.gitgui.domain.redline;

import com.gitgui.core.constant.RedLineCode;
import lombok.Builder;
import lombok.Data;

/**
 * 红线校验结果
 * <p>三态：{@link Action#PASS} 放行 / {@link Action#CONFIRM} UI 弹窗确认 / {@link Action#BLOCK} 直接阻断。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
@Data
@Builder(toBuilder = true)
public class RedLineResult {

    /**
     * 拦截动作
     */
    private Action action;

    /**
     * 命中的规则代码（PASS 时为 null）
     */
    private RedLineCode ruleCode;

    /**
     * 中文风险提示（含安全等价命令建议）
     */
    private String message;

    /**
     * 详情 JSON（如命中敏感文件清单、保护分支名、文件大小）
     */
    private String detail;

    /**
     * 构造放行结果。
     *
     * @return PASS 结果
     */
    public static RedLineResult pass() {
        return RedLineResult.builder()
                            .action(Action.PASS)
                            .build();
    }

    /**
     * 构造阻断结果。
     *
     * @param code    规则代码
     * @param message 中文提示
     * @return BLOCK 结果
     */
    public static RedLineResult block(RedLineCode code, String message) {
        return RedLineResult.builder()
                            .action(Action.BLOCK)
                            .ruleCode(code)
                            .message(message)
                            .build();
    }

    /**
     * 构造二次确认结果。
     *
     * @param code    规则代码
     * @param message 中文提示
     * @return CONFIRM 结果
     */
    public static RedLineResult confirm(RedLineCode code, String message) {
        return RedLineResult.builder()
                            .action(Action.CONFIRM)
                            .ruleCode(code)
                            .message(message)
                            .build();
    }

    /**
     * 拦截动作枚举。
     */
    public enum Action {
        /**
         * 放行执行
         */
        PASS,
        /**
         * UI 弹窗二次确认
         */
        CONFIRM,
        /**
         * 直接阻断拒绝
         */
        BLOCK
    }
}
