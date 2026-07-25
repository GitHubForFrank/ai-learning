package com.gitgui.core.async;

import com.gitgui.core.constant.TaskStatus;

import java.util.function.Consumer;

/**
 * 异步任务句柄
 * <p>包装任务执行句柄，提供取消、进度/成功/失败回调注册能力。</p>
 * <p>遵循 BR-33（可取消）与 BR-36（后台执行，UI 通过事件总线刷新）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class TaskHandle {

    /** 任务 ID（对应 task_record.id） */
    private final String taskId;
    /** 取消函数 */
    private final Runnable cancelAction;
    /** 任务状态（volatile 保证多线程可见性） */
    private volatile TaskStatus status = TaskStatus.PENDING;
    /** 成功回调 */
    private Consumer<Object> successHandler;
    /** 失败回调 */
    private Consumer<Throwable> failureHandler;
    /** 进度回调 */
    private ProgressCallback progressCallback;

    /**
     * 构造任务句柄。
     *
     * @param taskId       任务 ID
     * @param cancelAction 取消动作
     */
    public TaskHandle(String taskId, Runnable cancelAction) {
        this.taskId = taskId;
        this.cancelAction = cancelAction;
    }

    /**
     * 获取任务 ID。
     *
     * @return 任务 ID
     */
    public String getTaskId() {
        return taskId;
    }

    /**
     * 获取任务状态。
     *
     * @return 任务状态
     */
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * 设置任务状态。
     *
     * @param status 新状态
     */
    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    /**
     * 请求取消任务（BR-33）。
     */
    public void cancel() {
        if (cancelAction != null) {
            cancelAction.run();
        }
        this.status = TaskStatus.CANCELLED;
    }

    /**
     * 注册成功回调。
     *
     * @param handler 成功处理器
     * @return this
     */
    public TaskHandle onSuccess(Consumer<Object> handler) {
        this.successHandler = handler;
        return this;
    }

    /**
     * 注册失败回调。
     *
     * @param handler 失败处理器
     * @return this
     */
    public TaskHandle onFailure(Consumer<Throwable> handler) {
        this.failureHandler = handler;
        return this;
    }

    /**
     * 注册进度回调。
     *
     * @param callback 进度回调
     * @return this
     */
    public TaskHandle onProgress(ProgressCallback callback) {
        this.progressCallback = callback;
        return this;
    }

    /**
     * 触发成功回调。
     *
     * @param result 结果
     */
    public void completeSuccess(Object result) {
        this.status = TaskStatus.SUCCESS;
        if (successHandler != null) {
            successHandler.accept(result);
        }
    }

    /**
     * 触发失败回调。
     *
     * @param error 错误
     */
    public void completeFailure(Throwable error) {
        this.status = TaskStatus.FAILED;
        if (failureHandler != null) {
            failureHandler.accept(error);
        }
    }

    /**
     * 通知进度更新。
     *
     * @param percent 百分比
     * @param message 描述
     */
    public void notifyProgress(int percent, String message) {
        if (progressCallback != null) {
            progressCallback.onProgress(percent, message);
        }
    }
}
