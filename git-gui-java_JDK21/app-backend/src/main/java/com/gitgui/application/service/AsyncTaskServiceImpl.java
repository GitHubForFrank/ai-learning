package com.gitgui.application.service;

import com.gitgui.core.async.ProgressCallback;
import com.gitgui.core.async.TaskHandle;
import com.gitgui.core.async.TaskManager;
import com.gitgui.domain.constant.TaskType;
import com.gitgui.domain.model.TaskRecord;
import com.gitgui.domain.repository.TaskRecordRepository;
import com.gitgui.domain.service.AsyncTaskService;
import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    /**
     * 任务记录仓储（用于查询历史任务，BR-35）
     */
    private final TaskRecordRepository taskRecordRepository;
    /**
     * 按任务类型过滤的完成事件处理器：{@code TaskType -> handler}。
     * <p>支持同时为不同任务类型注册独立回调（例如：MULTI_REPO_SCAN 完成时刷新侧边栏）。</p>
     */
    private final Map<TaskType, Consumer<String>> typedTaskFinishedHandlers = new ConcurrentHashMap<>();
    /**
     * 任务类型缓存：{@code taskId -> TaskType}（notifyFinished 时按 taskId 直接查询，
     * 避免再次访问 DB 查 task_record）。
     */
    private final Map<String, TaskType> taskTypeCache = new ConcurrentHashMap<>();
    /**
     * 任务完成事件处理器（UI 通过此刷新，BR-36）
     */
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
        registerTaskType(handle.getTaskId(), taskType);
        handle.onSuccess(r -> notifyFinished(handle.getTaskId()));
        handle.onFailure(e -> notifyFinished(handle.getTaskId()));
        return handle;
    }

    @Override
    public TaskHandle submitRead(String repoPath, TaskType taskType, Runnable task, ProgressCallback cb) {
        // BR-34：读操作可并发
        TaskHandle handle = taskManager.submitRead(repoPath, taskType, task, cb);
        registerTaskType(handle.getTaskId(), taskType);
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

    @Override
    public void onTaskFinished(TaskType taskType, Consumer<String> handler) {
        // 仅注册按类型过滤的事件处理器；null 入参视为无效，直接忽略
        if (taskType == null) {
            return;
        }
        typedTaskFinishedHandlers.put(taskType, handler);
    }

    /**
     * 注册 taskId → TaskType 映射（submitRead/submitWrite 时调用，notifyFinished 时查询）。
     * <p>避免在 UI 任务完成回调中再次访问 DB 查 task_record。</p>
     */
    private void registerTaskType(String taskId, TaskType taskType) {
        if (taskId == null || taskType == null) {
            return;
        }
        taskTypeCache.put(taskId, taskType);
    }

    /**
     * 通知任务完成事件（BR-36，UI 通过此刷新）。
     * <p>两路派发：</p>
     * <ol>
     *   <li>全局 {@link #taskFinishedHandler}：所有任务都触发。</li>
     *   <li>类型过滤 {@link #typedTaskFinishedHandlers}：仅当 taskType 命中已注册类型时触发，
     *       用于让 UI 仅对关心的任务类型做局部刷新（例如 MULTI_REPO_SCAN 完成时刷新仓库列表），
     *       避免 STATUS/FETCH 等无关任务误触发仓库列表重建与滚动置顶。</li>
     * </ol>
     */
    private void notifyFinished(String taskId) {
        TaskType type = taskTypeCache.remove(taskId);
        if (taskFinishedHandler != null) {
            try {
                taskFinishedHandler.accept(taskId);
            } catch (Exception e) {
                log.warn("任务完成事件处理失败：taskId={}", taskId, e);
            }
        }
        if (type != null) {
            Consumer<String> typed = typedTaskFinishedHandlers.get(type);
            if (typed != null) {
                try {
                    typed.accept(taskId);
                } catch (Exception e) {
                    log.warn("按类型任务完成事件处理失败：taskId={}, taskType={}", taskId, type, e);
                }
            }
        }
    }
}
