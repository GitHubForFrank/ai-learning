package com.gitgui.domain.model;

import lombok.Builder;
import lombok.Data;

/**
 * 文件状态领域模型
 * <p>对应 PRD 4.3 仓库状态检查，标识工作区每个文件的 Git 状态。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
@Data
@Builder
public class FileStatus {

    /** 文件相对仓库路径 */
    private String path;

    /** 文件状态 */
    private FileState state;

    /** 新增行数（diff 统计） */
    private int addedLines;

    /** 删除行数（diff 统计） */
    private int deletedLines;

    /**
     * 文件 Git 状态枚举。
     */
    public enum FileState {
        /** 已修改（未暂存） */
        MODIFIED,
        /** 已新增（未跟踪） */
        UNTRACKED,
        /** 已删除 */
        DELETED,
        /** 已暂存 */
        STAGED,
        /** 冲突 */
        CONFLICT,
        /** 已忽略 */
        IGNORED,
        /** 未修改 */
        UNMODIFIED
    }
}
