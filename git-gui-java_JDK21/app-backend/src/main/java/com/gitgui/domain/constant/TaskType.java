package com.gitgui.domain.constant;

/**
 * 异步任务类型枚举
 * <p>对应 {@code task_record.task_type} 字段，标识任务来源 Git 操作。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public enum TaskType {

    /**
     * 克隆仓库（PRD 4.1）
     */
    CLONE,
    /**
     * 拉取（PRD 4.5.1）
     */
    PULL,
    /**
     * 推送（PRD 4.5.2）
     */
    PUSH,
    /**
     * 获取（PRD 4.5.3）
     */
    FETCH,
    /**
     * 提交（PRD 4.4）
     */
    COMMIT,
    /**
     * 合并（PRD 4.6.3）
     */
    MERGE,
    /**
     * 变基（PRD 4.11）
     */
    REBASE,
    /**
     * 多仓库检索（PRD 4.2.1）
     */
    MULTI_REPO_SCAN,
    /**
     * 垃圾回收（PRD 4.9.5）
     */
    GC,
    /**
     * 切换/检出（PRD 4.6.1）
     */
    CHECKOUT,
    /**
     * 暂存（PRD 4.9.1）
     */
    STASH,
    /**
     * 状态查询/界面刷新（UI 层异步加载，如刷新文件状态、加载分支列表等）
     */
    STATUS
}
