package com.gitgui.domain.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gitgui.core.constant.OperationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志领域模型
 * <p>状态机：追加写入，不可修改/不可删除。每次 Git 操作完成（成功/失败/取消）后立即写一条。</p>
 * <p>遵循 BR-35（任务结果记录）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
@Data
@Builder
@TableName("operation_log")
public class OperationLog {

    /** 主键 UUID */
    @TableId
    private String id;

    /** 目标仓库路径 */
    private String repoPath;

    /** 操作类型（COMMIT/PULL/PUSH/...） */
    private OperationType operation;

    /** 实际执行的 git 命令或 JGit API 描述 */
    private String command;

    /** 参数 JSON */
    private String args;

    /** 是否成功 */
    private boolean success;

    /** 耗时（毫秒） */
    private long durationMs;

    /** 错误信息（中文友好提示） */
    private String errorMessage;

    /** 关联异步任务 ID（异步操作时） */
    private String taskId;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
