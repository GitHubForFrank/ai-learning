package com.gitgui.core.constant;

/**
 * 异步任务状态枚举
 * <p>状态机：{@code PENDING → RUNNING → (SUCCESS | FAILED | CANCELLED)}，终态不可回退。</p>
 * <p>对应 {@code task_record.status} 字段，遵循 BR-33~BR-36。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public enum TaskStatus {

    /**
     * 待执行：已提交到队列，等待 TaskManager 调度
     */
    PENDING,
    /**
     * 执行中：TaskManager 已取出并开始执行
     */
    RUNNING,
    /**
     * 成功：任务正常完成
     */
    SUCCESS,
    /**
     * 失败：任务执行异常（不自动重试写操作，BR-35）
     */
    FAILED,
    /**
     * 已取消：用户主动取消（BR-33 可取消）
     */
    CANCELLED
}
