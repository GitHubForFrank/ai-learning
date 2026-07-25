package com.gitgui.domain.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gitgui.core.constant.TaskStatus;
import com.gitgui.domain.constant.TaskType;
import lombok.Builder;
import lombok.Data;

/**
 * 异步任务记录领域模型
 * <p>状态机：{@code PENDING → RUNNING → (SUCCESS | FAILED | CANCELLED)}，终态不可回退。</p>
 * <p>遵循 BR-33（异步可取消）、BR-34（同仓库写串行）、BR-35（结果记录）、BR-36（后台执行）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
@Data
@Builder
@TableName("task_record")
public class TaskRecord {

    /** 主键 UUID */
    @TableId
    private String id;

    /** 任务类型 */
    private TaskType taskType;

    /** 仓库路径（多仓库检索可为空） */
    private String repoPath;

    /** 状态（PENDING/RUNNING/SUCCESS/FAILED/CANCELLED） */
    private TaskStatus status;

    /** 进度 0-100 */
    private int progress;

    /** 进度描述 */
    private String message;

    /** 命令输出/错误堆栈 */
    private String output;

    /** 是否可取消 */
    private boolean cancellable;

    /** 开始时间（epoch 毫秒） */
    private Long startedAt;

    /** 结束时间（epoch 毫秒） */
    private Long finishedAt;

    /** 创建时间（epoch 毫秒） */
    private Long createdAt;
}
