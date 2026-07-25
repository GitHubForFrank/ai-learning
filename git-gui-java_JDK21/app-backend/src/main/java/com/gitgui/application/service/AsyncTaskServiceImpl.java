package com.gitgui.application.service;

import com.gitgui.core.async.ProgressCallback;
import com.gitgui.core.async.TaskHandle;
import com.gitgui.core.constant.TaskStatus;
import com.gitgui.core.async.TaskManager;
import com.gitgui.domain.constant.TaskType;
import com.gitgui.domain.model.TaskRecord;
import com.gitgui.domain.repository.TaskRecordRepository;
import com.gitgui.domain.service.AsyncTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;
import com.google.inject.Inject;

/**
 * 异步任务服务实现
 * <p>关联 BR：BR-33~BR-36。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class AsyncTaskServiceImpl implements AsyncTaskService {

    private static final Logger log = LoggerFactory.getLogger(AsyncTaskServiceImpl.class);

    private final TaskManager taskManager;
    /** 任务记录仓储（用于查询历史任务，BR-35） */
    private final TaskRecordRepository taskRecordRepository;
    /** 任务完成事件处理器（UI 通过此刷新，BR-36） */
    private volatile Consumer<String> taskFinishedHandler;

    @Inject
    public AsyncTaskServiceImpl(TaskManager taskManager, TaskRecordRepository taskRecordRepository) {
        this.taskManager = taskManager;
        this.taskRecordRepository = taskRecordRepository;
    }

    @Override
    public TaskHandle submitWrite(String repoPath, TaskType taskType, Runnable task, ProgressCallback cb) {
        // BR-34：同仓库写串行
        TaskHandle handle = taskManager.submitWrite(repoPath, taskType, task, cb);
        handle.onSuccess(r -> notifyFinished(handle.getTaskId()));
        handle.onFailure(e -> notifyFinished(handle.getTaskId()));
        return handle;
    }

    @Override
    public TaskHandle submitRead(String repoPath, TaskType taskType, Runnable task, ProgressCallback cb) {
        // BR-34：读操作可并发
        TaskHandle handle = taskManager.submitRead(repoPath, taskType, task, cb);
        handle.onSuccess(r -> notifyFinished(handle.getTaskId()));
        handle.onFailure(e -> notifyFinished(handle.getTaskId()));
        return handle;
    }

    @Override
    public void cancel(String taskId) {
        // BR-33：可取消
        taskManager.cancel(taskId);
    }

    @Override
    public TaskRecord get(String taskId) {
        TaskHandle handle = taskManager.getHandle(taskId);
        if (handle != null) {
            return TaskRecord.builder()
                    .id(handle.getTaskId())
                    .status(handle.getStatus())
                    .build();
        }
        return null;
    }

    @Override
    public List<TaskRecord> listActive(String repoPath) {
        return taskManager.listActive(repoPath);
    }

    @Override
    public List<TaskRecord> listHistory(String repoPath) {
        // BR-35：历史任务从持久化仓储查询（TaskManager 仅维护内存中的活跃任务）
        return taskRecordRepository.findHistoryByRepoPath(repoPath);
    }

    @Override
    public void onTaskFinished(Consumer<String> handler) {
        this.taskFinishedHandler = handler;
    }

    /**
     * 通知任务完成事件（BR-36，UI 通过此刷新）。
     */
    private void notifyFinished(String taskId) {
        if (taskFinishedHandler != null) {
            try {
                taskFinishedHandler.accept(taskId);
            } catch (Exception e) {
                log.warn("任务完成事件处理失败：taskId={}", taskId, e);
            }
        }
    }
}
