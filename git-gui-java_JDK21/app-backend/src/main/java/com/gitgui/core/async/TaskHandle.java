package com.gitgui.core.async;

import com.gitgui.core.constant.TaskStatus;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 异步任务句柄
 * <p>包装任务执行句柄，提供取消、进度/成功/失败回调注册能力。</p>
 * <p>遵循 BR-33（可取消）与 BR-36（后台执行，UI 通过事件总线刷新）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class TaskHandle {

    private static final Logger log = LoggerFactory.getLogger(TaskHandle.class);

    @Getter
    private final String taskId;
    private final Runnable cancelAction;
    /**
     * 成功回调列表（替代旧版单字段）。
     * <p>使用 {@link CopyOnWriteArrayList} 因为注册 handler 罕见（任务执行前几次）但
     * completeSuccess 在任务完成时遍历调用是热路径；CoW 在"读多写少"场景无锁性能最好。</p>
     */
    private final List<Consumer<Object>> successHandlers = new CopyOnWriteArrayList<>();
    /**
     * 失败回调列表。
     */
    private final List<Consumer<Throwable>> failureHandlers = new CopyOnWriteArrayList<>();
    /**
     * 进度回调列表（保留 List 而非单字段，对称语义，且允许多个消费方订阅进度）。
     */
    private final List<ProgressCallback> progressCallbacks = new CopyOnWriteArrayList<>();
    @Setter
    @Getter
    private volatile TaskStatus status = TaskStatus.PENDING;

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
     * 请求取消任务（BR-33）。
     */
    public void cancel() {
        if (cancelAction != null) {
            cancelAction.run();
        }
        this.status = TaskStatus.CANCELLED;
    }

    /**
     * 注册成功回调（append 模式：可多次注册，所有注册的回调都会在 {@link #completeSuccess(Object)} 时触发）。
     * <p><b>修复：</b>原版用单字段，后注册的会覆盖前注册的，导致 ProgressDialog.attach 注册的
     * 状态更新 handler 被调用方覆盖丢失，UI 永远停在"执行中…"。</p>
     *
     * @param handler 成功处理器
     * @return this
     */
    public TaskHandle onSuccess(Consumer<Object> handler) {
        if (handler != null) {
            successHandlers.add(handler);
        }
        return this;
    }

    /**
     * 注册失败回调（append 模式：可多次注册，所有注册的回调都会在 {@link #completeFailure(Throwable)} 时触发）。
     *
     * @param handler 失败处理器
     * @return this
     */
    public TaskHandle onFailure(Consumer<Throwable> handler) {
        if (handler != null) {
            failureHandlers.add(handler);
        }
        return this;
    }

    /**
     * 注册进度回调（append 模式：可多次注册）。
     *
     * @param callback 进度回调
     * @return this
     */
    public TaskHandle onProgress(ProgressCallback callback) {
        if (callback != null) {
            progressCallbacks.add(callback);
        }
        return this;
    }

    /**
     * 触发所有成功回调：依次调用，handler 内异常不中断后续调用（每个 handler try/catch）。
     *
     * @param result 结果
     */
    public void completeSuccess(Object result) {
        this.status = TaskStatus.SUCCESS;
        for (Consumer<Object> h : successHandlers) {
            try {
                h.accept(result);
            } catch (Throwable t) {
                // 单个 handler 抛异常不影响其他 handler（典型场景：UI handler 内部 Platform.runLater 抛错）
                log.warn("successHandler 执行异常", t);
            }
        }
    }

    /**
     * 触发所有失败回调：依次调用，handler 内异常不中断后续调用。
     *
     * @param error 错误
     */
    public void completeFailure(Throwable error) {
        this.status = TaskStatus.FAILED;
        for (Consumer<Throwable> h : failureHandlers) {
            try {
                h.accept(error);
            } catch (Throwable t) {
                log.warn("failureHandler 执行异常", t);
            }
        }
    }

    /**
     * 通知进度更新。依次调用所有已注册的 progressCallback。
     *
     * @param percent 百分比
     * @param message 描述
     */
    public void notifyProgress(int percent, String message) {
        for (ProgressCallback cb : progressCallbacks) {
            try {
                cb.onProgress(percent, message);
            } catch (Throwable t) {
                log.warn("progressCallback 执行异常", t);
            }
        }
    }
}
