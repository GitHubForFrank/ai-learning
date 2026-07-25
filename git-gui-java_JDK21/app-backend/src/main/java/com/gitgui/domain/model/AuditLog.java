package com.gitgui.domain.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 红线审计日志领域模型
 * <p>状态机：追加写入，不可修改/不可删除（BR-31）。</p>
 * <p>所有命中红线的操作记录到本表，含时间、规则代码、命令、仓库、分支、远程URL、处理结果。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
@Data
@Builder
@TableName("audit_log")
public class AuditLog {

    /** 主键 UUID */
    @TableId
    private String id;

    /** 红线规则代码（如 RED_FORCE_PUSH） */
    private String ruleCode;

    /** 命中红线的命令 */
    private String command;

    /** 仓库路径 */
    private String repoPath;

    /** 涉及分支 */
    private String branch;

    /** 涉及远程 URL */
    private String remoteUrl;

    /** 红线类型（BLOCK/CONFIRM） */
    private String action;

    /** 处理结果（BLOCKED/CONFIRMED/CANCELLED） */
    private String actionResult;

    /** 详情 JSON（如命中敏感文件清单、保护分支名、文件大小） */
    private String detail;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
