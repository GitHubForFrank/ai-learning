package com.gitgui.core.constant;

/**
 * Git 操作类型枚举
 * <p>对应 PRD 第四章各 Git 操作，用于操作日志 {@code operation_log.operation} 字段。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public enum OperationType {

    /** 克隆仓库（PRD 4.1） */
    CLONE,
    /** 初始化仓库（PRD 4.1） */
    INIT,
    /** 多仓库检索（PRD 4.2.1） */
    SCAN,
    /** 提交（PRD 4.4） */
    COMMIT,
    /** 拉取（PRD 4.5.1） */
    PULL,
    /** 推送（PRD 4.5.2） */
    PUSH,
    /** 获取（PRD 4.5.3） */
    FETCH,
    /** 同步（PRD 4.5.4） */
    SYNC,
    /** 切换/检出（PRD 4.6.1） */
    CHECKOUT,
    /** 合并（PRD 4.6.3） */
    MERGE,
    /** 变基（PRD 4.11） */
    REBASE,
    /** 遴选（PRD 4.8） */
    CHERRY_PICK,
    /** 重置（PRD 4.10） */
    RESET,
    /** 反向提交（PRD 4.10） */
    REVERT,
    /** 暂存（PRD 4.9.1） */
    STASH,
    /** 清理未跟踪文件（PRD 4.9.4） */
    CLEAN,
    /** 垃圾回收（PRD 4.9.5） */
    GC,
    /** 创建分支（PRD 4.6.2） */
    CREATE_BRANCH,
    /** 删除分支（PRD 4.6.4） */
    DELETE_BRANCH,
    /** 重命名分支（PRD 4.6.4） */
    RENAME_BRANCH,
    /** 创建标签（PRD 4.7） */
    CREATE_TAG,
    /** 删除标签（PRD 4.7） */
    DELETE_TAG,
    /** 推送标签（PRD 4.7） */
    PUSH_TAG
}
