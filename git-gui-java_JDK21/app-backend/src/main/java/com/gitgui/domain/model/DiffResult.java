package com.gitgui.domain.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Diff 对比结果领域模型
 * <p>对应 PRD 4.10 双栏 Compare revisions 与 4.3 双击文件 Diff 对比。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
@Data
@Builder
public class DiffResult {

    /**
     * 文件路径
     */
    private String path;

    /**
     * 旧版本哈希
     */
    private String oldRev;

    /**
     * 新版本哈希
     */
    private String newRev;

    /**
     * Diff 文本（unified diff 格式）
     */
    private String diffText;

    /**
     * 变更文件块列表
     */
    private List<DiffHunk> hunks;

    /**
     * Diff 块。
     */
    @Data
    @Builder
    public static class DiffHunk {

        /**
         * 旧文件起始行
         */
        private int oldStart;
        /**
         * 旧文件行数
         */
        private int oldLines;
        /**
         * 新文件起始行
         */
        private int newStart;
        /**
         * 新文件行数
         */
        private int newLines;
        /**
         * 块内容
         */
        private String content;
    }
}
