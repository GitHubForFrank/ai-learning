package com.gitgui.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单个文件变更（commit diff 一行）
 * <p>对应 LogMessagesDialog「Path」面板的一行记录：</p>
 * <ul>
 *   <li>{@code changeType}：ADD / MODIFY / DELETE / RENAME / COPY</li>
 *   <li>{@code oldPath} / {@code newPath}：删除时 newPath 为空，添加时 oldPath 为空</li>
 * </ul>
 *
 * @author FrankKang
 * @since 2026-07-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileChange {

    /** 主显示路径（删除用 oldPath，其他用 newPath） */
    private String path;

    /** 变更类型：ADD / MODIFY / DELETE / RENAME / COPY */
    private String changeType;

    /** 旧路径（删除 / 重命名时使用） */
    private String oldPath;

    /** 新路径（新增 / 修改 / 重命名时使用） */
    private String newPath;

    /**
     * 用于表格展示：「[ADD] path/newPath」
     */
    public String getDisplayPath() {
        if ("RENAME".equals(changeType) && oldPath != null && newPath != null) {
            return oldPath + " → " + newPath;
        }
        return path == null ? "" : path;
    }
}