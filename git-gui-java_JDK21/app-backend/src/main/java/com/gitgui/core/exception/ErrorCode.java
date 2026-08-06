package com.gitgui.core.exception;

import lombok.Getter;

/**
 * 应用错误码枚举
 * <p>对应 02-api.md 错误码注册表，{@code GitGuiException} 携带的错误代码。</p>
 * <p>错误信息对用户友好（中文），不暴露技术细节（栈/SQL/内网路径）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
@Getter
public enum ErrorCode {

    /**
     * 参数校验失败（BR-03/BR-06/BR-07/BR-21）
     */
    VALIDATION_FAILED("参数校验失败"),
    /**
     * 选定目录非 Git 仓库（BR-41）
     */
    REPO_NOT_GIT("选定目录不是 Git 仓库"),
    /**
     * 命中阻断类红线（BR-26~BR-28/BR-32）
     */
    RED_LINE_BLOCKED("命令被红线拦截"),
    /**
     * 二次确认被用户取消（BR-29/BR-30）
     */
    RED_LINE_CONFIRM_CANCELED("用户取消二次确认"),
    /**
     * 工作区不干净且操作不允许（BR-12）
     */
    WORKTREE_DIRTY("工作区不干净，请先提交或暂存修改"),
    /**
     * Git CLI 执行失败（BE-02~BE-09）
     */
    GIT_EXECUTION_FAILED("Git 操作执行失败"),
    /**
     * 异步任务被取消（BR-33）
     */
    TASK_CANCELED("任务已取消"),
    /**
     * 同仓库写任务排队超限（BR-34）
     */
    TASK_QUEUE_FULL("任务队列已满"),
    /**
     * 未配置外部 Merge 工具
     */
    MERGE_TOOL_NOT_CONFIGURED("未配置外部合并工具"),
    /**
     * 收藏路径重复（BR-03）
     */
    DUPLICATE_FAVORITE("该仓库已收藏"),
    /**
     * 资源不存在
     */
    NOT_FOUND("资源不存在"),
    /**
     * 单实例已运行（BR-40）
     */
    SINGLE_INSTANCE_RUNNING("应用已在运行"),
    /**
     * 本地 Git 未安装（BR-41）
     */
    GIT_NOT_FOUND("未检测到本地 Git 可执行文件");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

}
