package com.gitgui.core.constant;

/**
 * 命令红线规则代码枚举
 * <p>对应 13 个 {@code RedLineRule} 实现类，作为 {@code audit_log.rule_code} 字段值。</p>
 * <p>遵循 BR-26~BR-32 红线闭环约束。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public enum RedLineCode {

    /** 裸 --force push（BLOCK，BR-26） */
    RED_FORCE_PUSH("裸强制推送，请改用 --force-with-lease"),
    /** 向保护分支 force push（BLOCK，BR-26/BR-27） */
    RED_PROTECTED_BRANCH("向保护分支强制推送"),
    /** push 删除保护分支（BLOCK，BR-26） */
    RED_DELETE_PROTECTED_BRANCH("推送删除保护分支"),
    /** 推送含敏感信息文件（BLOCK，BR-26/BR-32） */
    RED_SENSITIVE_FILE("推送含敏感信息文件"),
    /** 推送到非授权远程（BLOCK，BR-26/BR-28） */
    RED_REMOTE_WHITELIST("推送到非授权远程"),
    /** --no-verify 跳过 hook（BLOCK，BR-26） */
    RED_NO_VERIFY("跳过 hook 校验"),
    /** reset --hard（CONFIRM，BR-29） */
    RED_RESET_HARD("硬重置将丢失本地修改"),
    /** clean -fdx（CONFIRM，BR-29） */
    RED_CLEAN_FDX("清理含忽略文件"),
    /** amend 已推送提交（CONFIRM，BR-29） */
    RED_AMEND_PUSHED("修补已推送提交将篡改共享历史"),
    /** rebase 已推送提交（CONFIRM，BR-29/BR-30） */
    RED_REBASE_PUSHED("变基已推送提交将篡改共享历史"),
    /** filter-branch / filter-repo（CONFIRM，BR-29） */
    RED_FILTER_BRANCH("重写历史"),
    /** 推送超大文件（CONFIRM，BR-29） */
    RED_LARGE_FILE("推送超大文件，建议走 LFS"),
    /** 红线开关切换（CONFIRM，BR-30） */
    RED_LINE_TOGGLE("切换命令红线总开关");

    /** 中文风险描述，用于 UI 提示与审计日志 */
    private final String description;

    RedLineCode(String description) {
        this.description = description;
    }

    /**
     * 获取风险描述。
     *
     * @return 中文风险描述
     */
    public String getDescription() {
        return description;
    }
}
