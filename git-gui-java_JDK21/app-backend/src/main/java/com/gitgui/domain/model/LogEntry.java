package com.gitgui.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 提交日志条目领域模型
 * <p>对应 PRD 4.10 日志与回溯，主窗口日志区域每行展示一条。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
@Data
@Builder
public class LogEntry {

    /**
     * 提交哈希
     */
    private String commitId;

    /**
     * 简短哈希（展示用）
     */
    private String shortId;

    /**
     * 作者
     */
    private String author;

    /**
     * 作者邮箱
     */
    private String authorEmail;

    /**
     * 提交时间
     */
    private LocalDateTime commitTime;

    /**
     * 提交信息
     */
    private String message;

    /**
     * 分支/标签引用列表
     */
    private List<String> refs;

    /**
     * 父提交哈希列表
     */
    private List<String> parents;
}
