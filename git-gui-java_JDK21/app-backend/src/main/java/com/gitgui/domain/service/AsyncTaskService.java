package com.gitgui.domain.service;

import com.gitgui.core.async.ProgressCallback;
import com.gitgui.core.async.TaskHandle;
import com.gitgui.domain.constant.TaskType;
import com.gitgui.domain.model.TaskRecord;
import java.util.List;
import java.util.function.Consumer;

/**
 * 异步任务服务接口
 * <p>关联 BR：BR-33~BR-36。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public interface AsyncTaskService {

    /**
     * 提交写任务（同仓库写串行，BR-34）。
     *
     * @param repoPath 仓库路径
     * @param taskType 任务类型
     * @param task     任务体
     * @param cb       进度回调
     * @return 任务句柄
     */
    TaskHandle submitWrite(String repoPath, com.gitgui.domain.constant.TaskType taskType, Runnable task, ProgressCallback cb);

    /**
     * 提交读任务（并发，BR-34）。
     *
     * @param repoPath 仓库路径
     * @param taskType 任务类型
     * @param task     任务体
     * @param cb       进度回调
     * @return 任务句柄
     */
    TaskHandle submitRead(String repoPath, com.gitgui.domain.constant.TaskType taskType, Runnable task, ProgressCallback cb);

    /**
     * 取消任务（BR-33）。
     *
     * @param taskId 任务 ID
     */
    void cancel(String taskId);

    /**
     * 获取任务记录。
     *
     * @param taskId 任务 ID
     * @return 任务记录
     */
    TaskRecord get(String taskId);

    /**
     * 列出仓库的活跃任务。
     *
     * @param repoPath 仓库路径
     * @return 活跃任务列表
     */
    List<TaskRecord> listActive(String repoPath);

    /**
     * 列出仓库的历史任务。
     *
     * @param repoPath 仓库路径
     * @return 历史任务列表
     */
    List<TaskRecord> listHistory(String repoPath);

    /**
     * 注册任务完成事件处理器（BR-36，UI 通过此刷新）。
     * <p>所有任务完成时都会触发，包括读/写/扫描等。</p>
     *
     * @param handler 处理器（参数为完成的 taskId）
     */
    void onTaskFinished(Consumer<String> handler);

    /**
     * 注册按任务类型过滤的任务完成事件处理器（BR-36）。
     * <p>仅当任务类型匹配时触发回调，避免无关任务触发 UI 重绘。
     * 例如：仅关心 {@link TaskType#MULTI_REPO_SCAN} 完成事件、仅刷新仓库列表。</p>
     *
     * @param taskType 关注的任务类型
     * @param handler  处理器（参数为完成的 taskId）
     */
    void onTaskFinished(TaskType taskType, Consumer<String> handler);
}
