package com.gitgui.domain.repository;

import com.gitgui.core.constant.TaskStatus;
import com.gitgui.domain.model.TaskRecord;

import java.util.List;

/**
 * 异步任务记录仓储接口
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public interface TaskRecordRepository {

    /**
     * 保存任务记录（新增或更新）。
     *
     * @param record 任务记录
     */
    void save(TaskRecord record);

    /**
     * 更新任务记录。
     *
     * @param record 任务记录
     */
    void update(TaskRecord record);

    /**
     * 根据 ID 查找。
     *
     * @param id 任务 ID
     * @return 任务记录，不存在返回 null
     */
    TaskRecord findById(String id);

    /**
     * 列出仓库的活跃任务（PENDING/RUNNING）。
     *
     * @param repoPath 仓库路径
     * @return 活跃任务列表
     */
    List<TaskRecord> findActiveByRepoPath(String repoPath);

    /**
     * 列出仓库的历史任务（按 createdAt 倒序）。
     *
     * @param repoPath 仓库路径
     * @return 历史任务列表
     */
    List<TaskRecord> findHistoryByRepoPath(String repoPath);

    /**
     * 按状态查询。
     *
     * @param status 任务状态
     * @return 任务记录列表
     */
    List<TaskRecord> findByStatus(TaskStatus status);
}
