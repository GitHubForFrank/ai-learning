package com.gitgui.ui;

import com.gitgui.GitGuiApp;
import com.gitgui.core.async.ProgressCallback;
import com.gitgui.core.async.TaskHandle;
import com.gitgui.domain.constant.TaskType;
import com.gitgui.domain.service.AsyncTaskService;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UI 异步加载工具
 * <p>封装 UI 层"提交读/写任务到 {@link AsyncTaskService} + Platform.runLater 回调 UI + 异常时
 * Platform.runLater 弹错"的统一模式，替代散落的裸 {@code new Thread().start()}，遵循 BR-33/BR-36/E2。</p>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * // 读任务（如加载分支列表、刷新文件状态）
 * AsyncUiLoader.submitRead(repoPath, TaskType.STATUS, () -> {
 *     List<String> branches = gitExecutor.listBranches(repoPath);
 *     Platform.runLater(() -> branchCombo.getItems().setAll(branches));
 * });
 *
 * // 写任务（如提交）
 * AsyncUiLoader.submitWrite(repoPath, TaskType.COMMIT, () -> {
 *     String commitId = gitOperationService.commit(req);
 *     Platform.runLater(() -> showSuccess(commitId));
 * });
 * }</pre>
 *
 * <p>注意：任务体内部仍需自行调用 {@link Platform#runLater(Runnable)} 刷新 UI；
 * 未捕获异常由 {@link AsyncTaskService} 统一记录到 task_record 并通过日志输出。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public final class AsyncUiLoader {

    private static final Logger log = LoggerFactory.getLogger(AsyncUiLoader.class);

    /** 无仓库上下文时使用的占位路径（如全局设置加载，对应 task_record.repo_path 默认空串） */
    private static final String EMPTY_REPO_PATH = "";

    /**
     * 工具类禁止实例化。
     */
    private AsyncUiLoader() {
    }

    /**
     * 获取 AsyncTaskService 实例（从 Guice 注入器，参考 Dialog 获取 RemoteConfigService 的方式）。
     *
     * @return AsyncTaskService 实例
     * @throws IllegalStateException 注入器未就绪时抛出
     */
    private static AsyncTaskService getAsyncTaskService() {
        AsyncTaskService service = GitGuiApp.getInjector().getInstance(AsyncTaskService.class);
        if (service == null) {
            throw new IllegalStateException("AsyncTaskService 未注入，无法提交异步任务");
        }
        return service;
    }

    /**
     * 规范化仓库路径：null 转为空串（task_record.repo_path 允许空，对应多仓库检索/全局设置场景）。
     *
     * @param repoPath 原始仓库路径
     * @return 规范化后的仓库路径
     */
    private static String normalizeRepoPath(String repoPath) {
        return repoPath == null ? EMPTY_REPO_PATH : repoPath;
    }

    /**
     * 提交读任务到异步任务体系（BR-33/BR-36）。
     * <p>读任务走并发池（BR-34），任务体内需自行调用 {@link Platform#runLater(Runnable)} 刷新 UI。</p>
     *
     * @param repoPath 仓库路径（null 视为无仓库上下文，如全局设置加载）
     * @param taskType 任务类型
     * @param task     任务体
     * @return 任务句柄
     */
    public static TaskHandle submitRead(String repoPath, TaskType taskType, Runnable task) {
        try {
            return getAsyncTaskService().submitRead(normalizeRepoPath(repoPath), taskType, task, null);
        } catch (Exception e) {
            log.error("提交读任务失败：taskType={}", taskType, e);
            throw e;
        }
    }

    /**
     * 提交写任务到异步任务体系（BR-33/BR-34/BR-36）。
     * <p>写任务走同仓库串行队列（BR-34），任务体内需自行调用 {@link Platform#runLater(Runnable)} 刷新 UI。</p>
     *
     * @param repoPath 仓库路径
     * @param taskType 任务类型
     * @param task     任务体
     * @return 任务句柄
     */
    public static TaskHandle submitWrite(String repoPath, TaskType taskType, Runnable task) {
        try {
            return getAsyncTaskService().submitWrite(normalizeRepoPath(repoPath), taskType, task, null);
        } catch (Exception e) {
            log.error("提交写任务失败：taskType={}", taskType, e);
            throw e;
        }
    }
}
