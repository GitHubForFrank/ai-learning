package com.gitgui.domain.redline;

/**
 * 命令红线规则接口
 * <p>13 个实现类通过 Guice {@code Multibindings} 收集，由 {@code CommandRedLineService.check} 遍历执行。</p>
 * <p>遵循 BR-26~BR-32 红线闭环约束。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public interface RedLineRule {

    /**
     * 校验是否命中红线。
     * <p>任一规则返回 BLOCK 则整体 BLOCK；任一返回 CONFIRM 则收集确认项；全部 PASS 则放行。</p>
     *
     * @param ctx 红线上下文
     * @return 校验结果（PASS/CONFIRM/BLOCK）
     */
    RedLineResult check(RedLineContext ctx);

    /**
     * 获取规则代码（用于审计日志 rule_code 字段）。
     *
     * @return 规则代码字符串
     */
    String ruleCode();
}
