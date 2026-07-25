package com.gitgui.core.async;

import com.gitgui.core.constant.TaskStatus;
import com.gitgui.core.exception.ErrorCode;
import com.gitgui.core.exception.GitGuiException;
import com.gitgui.domain.model.TaskRecord;
import com.gitgui.domain.repository.TaskRecordRepository;
import com.gitgui.domain.constant.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 异步任务管理器（单例）
 * <p>维护仓库级写任务队列 + 全局读任务并发池，调度异步任务执行。</p>
 * <p>遵循 BR-33（可取消）、BR-34（同仓库写串行，读并发）、BR-35（结果记录）、BR-36（后台执行）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class TaskManager {

    private static final Logger log = LoggerFactory.getLogger(TaskManager.class);

    /** 写任务队列最大长度（同仓库写任务排队超限抛 TASK_QUEUE_FULL，BR-34） */
    private static final int WRITE_QUEUE_MAX = 50;

    /** 写任务队列：repoPath → 单线程串行执行器 */
    private final Map<String, ExecutorService> writeExecutors = new ConcurrentHashMap<>();

    /** 读任务并发池（读操作可并发，BR-34） */
    private final ExecutorService readExecutor;

    /** 任务句柄注册表：taskId → TaskHandle */
    private final Map<String, TaskHandle> handles = new ConcurrentHashMap<>();

    /** 任务记录仓储（持久化任务状态，BR-35） */
    private final TaskRecordRepository taskRecordRepository;

    /**
     * 构造任务管理器。
     *
     * @param taskRecordRepository 任务记录仓储
     */
    public TaskManager(TaskRecordRepository taskRecordRepository) {
        this.taskRecordRepository = taskRecordRepository;
        // 读任务并发池：核心 4 线程，最大 16 线程，队列 200
        this.readExecutor = new ThreadPoolExecutor(
                4, 16, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(200),
                r -> {
                    Thread t = new Thread(r, "git-gui-read-task");
                    t.setDaemon(true);
                    return t;
                });
    }

    /**
     * 提交写任务（同仓库写串行，BR-34）。
     *
     * @param repoPath 仓库路径
     * @param taskType 任务类型
     * @param task     任务体
     * @param callback 进度回调
     * @return 任务句柄
     */
    public TaskHandle submitWrite(String repoPath, TaskType taskType, Runnable task, ProgressCallback callback) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        TaskRecord record = TaskRecord.builder()
                .id(taskId)
                .taskType(taskType)
                .repoPath(repoPath)
                .status(TaskStatus.PENDING)
                .progress(0)
                .cancellable(true)
                .build();
        taskRecordRepository.save(record);

        TaskHandle handle = new TaskHandle(taskId, () -> {
            log.info("任务取消请求：taskId={}, repoPath={}", taskId, repoPath);
            if (callback != null) {
                // callback.isCancelled 由执行体轮询
            }
        });
        handle.onProgress(callback == null ? ProgressCallback.NOOP : callback);
        handles.put(taskId, handle);

        ExecutorService executor = writeExecutors.computeIfAbsent(repoPath, k -> createWriteExecutor());
        if (((ThreadPoolExecutor) executor).getQueue().size() >= WRITE_QUEUE_MAX) {
            throw new GitGuiException(ErrorCode.TASK_QUEUE_FULL);
        }
        executor.submit(() -> executeTask(handle, record, task, callback));
        return handle;
    }

    /**
     * 提交读任务（并发执行，BR-34）。
     *
     * @param repoPath 仓库路径
     * @param taskType 任务类型
     * @param task     任务体
     * @param callback 进度回调
     * @return 任务句柄
     */
    public TaskHandle submitRead(String repoPath, TaskType taskType, Runnable task, ProgressCallback callback) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        TaskRecord record = TaskRecord.builder()
                .id(taskId)
                .taskType(taskType)
                .repoPath(repoPath)
                .status(TaskStatus.PENDING)
                .progress(0)
                .cancellable(true)
                .build();
        taskRecordRepository.save(record);

        TaskHandle handle = new TaskHandle(taskId, () -> log.info("读任务取消：{}", taskId));
        handle.onProgress(callback == null ? ProgressCallback.NOOP : callback);
        handles.put(taskId, handle);

        readExecutor.submit(() -> executeTask(handle, record, task, callback));
        return handle;
    }

    /**
     * 取消指定任务（BR-33）。
     *
     * @param taskId 任务 ID
     */
    public void cancel(String taskId) {
        TaskHandle handle = handles.get(taskId);
        if (handle != null) {
            handle.cancel();
            TaskRecord record = taskRecordRepository.findById(taskId);
            if (record != null) {
                record.setStatus(TaskStatus.CANCELLED);
                taskRecordRepository.update(record);
            }
        }
    }

    /**
     * 获取任务句柄。
     *
     * @param taskId 任务 ID
     * @return 句柄，不存在返回 null
     */
    public TaskHandle getHandle(String taskId) {
        return handles.get(taskId);
    }

    /**
     * 列出仓库的活跃任务（PENDING/RUNNING）。
     *
     * @param repoPath 仓库路径
     * @return 活跃任务记录列表
     */
    public List<TaskRecord> listActive(String repoPath) {
        return taskRecordRepository.findActiveByRepoPath(repoPath);
    }

    /**
     * 关闭所有执行器，释放资源。
     */
    public void shutdown() {
        writeExecutors.values().forEach(ExecutorService::shutdownNow);
        readExecutor.shutdownNow();
        log.info("TaskManager 已关闭");
    }

    /**
     * 创建仓库级写任务执行器（单线程串行，保证同仓库写串行，BR-34）。
     */
    private ExecutorService createWriteExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "git-gui-write-task");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 执行任务体并更新状态。
     */
    private void executeTask(TaskHandle handle, TaskRecord record, Runnable task, ProgressCallback callback) {
        try {
            handle.setStatus(TaskStatus.RUNNING);
            record.setStatus(TaskStatus.RUNNING);
            record.setStartedAt(System.currentTimeMillis());
            taskRecordRepository.update(record);
            task.run();
            if (callback != null && callback.isCancelled()) {
                handle.setStatus(TaskStatus.CANCELLED);
                record.setStatus(TaskStatus.CANCELLED);
            } else {
                handle.completeSuccess(null);
                record.setStatus(TaskStatus.SUCCESS);
                record.setProgress(100);
            }
        } catch (Exception e) {
            log.error("任务执行失败：taskId={}", handle.getTaskId(), e);
            handle.completeFailure(e);
            record.setStatus(TaskStatus.FAILED);
            record.setOutput(e.getMessage());
        } finally {
            record.setFinishedAt(System.currentTimeMillis());
            taskRecordRepository.update(record);
            handles.remove(handle.getTaskId());
        }
    }
}
