package com.gitgui.ui.dialog;

import com.gitgui.core.async.ProgressCallback;
import com.gitgui.core.async.TaskHandle;
import com.gitgui.core.constant.TaskStatus;
import com.gitgui.ui.i18n.I18nUtil;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Git 操作实时输出对话框
 * <p>替代原有"弹窗显示一行结果"的设计，实时滚屏展示 git CLI 的所有输出行（commit/pull/push/fetch/clone 等），
 * 让用户看到完整过程（远程进度、文件计数、错误日志等）。</p>
 *
 * <p>标准使用流程：
 * <ol>
 *   <li>{@code new ProgressDialog(owner, title, header, OpKind.XXX)} 创建对话框（仅初始化 UI）</li>
 *   <li>{@code .asCallback()} 拿到可复用的 ProgressCallback 实例</li>
 *   <li>把 callback 传给 git 服务（如 {@code gitOperationService.push(req, cb)}）</li>
 *   <li>{@code .attach(taskHandle)} 把 TaskHandle 绑到对话框（注册 onSuccess/onFailure）</li>
 *   <li>{@code .showAndWaitForTask()} 显示并等待任务结束</li>
 * </ol>
 * </p>
 *
 * @author FrankKang
 * @since 2026-07-26
 */
public class ProgressDialog extends Stage {

    private static final Logger log = LoggerFactory.getLogger(ProgressDialog.class);
    /**
     * 剥离 git CLI 输出中的 ANSI 转义序列（含颜色、光标移动等），避免控制台出现 □@i;33m 这类乱码。
     * <pre>
     * 匹配模式：ESC（\u001B / 0x1B）+ '[' + 任意数字 / 分号 + 字母结束
     * 例如："\u001B[1;33mGITEE.COM\u001B[0m" → "GITEE.COM"
     * </pre>
     */
    private static final java.util.regex.Pattern ANSI_ESCAPE_PATTERN = java.util.regex.Pattern.compile("\u001B\\[[0-9;]*[a-zA-Z]");
    private final ProgressBar progressBar = new ProgressBar();
    private final Label statusLabel = new Label();
    /**
     * 输出区：改用 {@link ListView} 替代 TextArea，原因是 TextArea.appendText 在
     * Stage.showAndWait 嵌套事件循环下经常不刷新（OpenJFX 已知问题），
     * ListView 各 cell 在 add 后会强制 refresh，渲染稳定得多。
     */
    private final ListView<String> outputList = new ListView<>();
    private final ObservableList<String> outputLines = FXCollections.observableArrayList();
    private final Button cancelButton = new Button(I18nUtil.get("button.cancel"));
    /**
     * 当前操作类型（用于输出区首行 hint）；构造时确定，构造后不再变更
     */
    private final OpKind opKind;
    private final ProgressCallback callback;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);
    private final long startMillis = System.currentTimeMillis();
    private final AtomicBoolean firstOutput = new AtomicBoolean(false);
    /**
     * 心跳动画（替代裸 Thread，P1-009）
     */
    private javafx.animation.Timeline heartbeatTimeline;
    private TaskHandle taskHandle;
    /**
     * 累计 git 实际产出的行数（不含 buildContent 中的 firstHint 首行），用于 onSuccess 判断是否需要 "[无输出]" 兜底。
     */
    private volatile int realOutputLines = 0;
    /**
     * 任务完成后的回调（用于 commit+push 场景，push 完成后显示 commit done alert）。
     * 在 onSuccess/onFailure 中都会调用，确保无论成功失败都能通知调用方。
     */
    private Runnable onTaskFinished;
    /**
     * 创建进度对话框（仅初始化 UI，不绑定 TaskHandle）。
     *
     * @param owner  父窗口（modality 用，可为 null）
     * @param title  对话框标题（如 "推送"）
     * @param header 描述性副标题（如 "推送到 origin/main"），可为 null
     * @param opKind 当前 git 操作类型（用于输出区首行 hint，正确传入能避免 hint 文案与实际命令不匹配）
     */
    public ProgressDialog(Stage owner, String title, String header, OpKind opKind) {
        if (owner != null) {
            initOwner(owner);
        }
        initModality(Modality.APPLICATION_MODAL);
        setTitle(title);
        setResizable(true);
        // 设为"初始尺寸"——很多 Scene/Stage 会忽略 minWidth/minHeight，直接以 Scene 实际尺寸显示，
        // 这里同时设置 min + pref，确保布局计算给 TextArea 留足空间，不被挤压到看不见。
        setMinWidth(720);
        setMinHeight(500);
        setWidth(720);
        setHeight(500);
        // 兼容 null：如果调用方没传 opKind（老代码），fallback 到 PUSH，文案兼容性最好（旧逻辑就当成 push）
        this.opKind = opKind == null ? OpKind.PUSH : opKind;

        Scene scene = new Scene(buildContent(header));
        setScene(scene);

        // 取消按钮
        cancelButton.setOnAction(e -> {
            if (taskHandle != null && (taskHandle.getStatus() == TaskStatus.PENDING || taskHandle.getStatus() == TaskStatus.RUNNING)) {
                // 第一次按下：标记请求 + 立即给用户反馈
                if (cancelRequested.compareAndSet(false, true)) {
                    cancelled.set(true);
                    taskHandle.cancel();
                    cancelButton.setDisable(true);
                    statusLabel.setText("⚠ 取消中… 等待当前操作退出…");
                    appendLineAndScroll("");
                    appendLineAndScroll(I18nUtil.get("progress.cancelRequest") + "（已请求终止 git 进程）");
                }
                // 后续点击不响应
            } else {
                close();
            }
        });

        // 内部 callback（共享给 gitExecutor.push、TaskHandle.onProgress）
        this.callback = new ProgressCallback() {
            @Override
            public void onProgress(int percent, String message) {
                Platform.runLater(() -> {
                    progressBar.setProgress(Math.max(0, Math.min(1, percent / 100.0)));
                    if (message != null && !message.isEmpty()) {
                        statusLabel.setText(message);
                    }
                });
            }

            @Override
            public void onOutput(String line) {
                if (!firstOutput.get() && line != null && !line.isBlank()) {
                    firstOutput.set(true);
                    Platform.runLater(() -> statusLabel.setText("执行中…"));
                }
                if (line != null) {
                    realOutputLines++;
                }
                // 去掉 ANSI 转义（如 "\u001B[1;33m"）避免控制台乱码
                appendLineAndScroll(stripAnsi(line));
            }

            @Override
            public boolean isCancelled() {
                return cancelled.get();
            }
        };

        // 启动心跳线程：每 1 秒更新一次 "已等待 N 秒" 状态（当任务还没结束且没收到任何输出时）
        startHeartbeat();
    }

    /**
     * 把传入字符串中的 ANSI 控制字符清掉。null 直接返回。
     */
    public static String stripAnsi(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return ANSI_ESCAPE_PATTERN.matcher(s)
                                  .replaceAll("");
    }

    private static String safeErrorMessage(Throwable t) {
        if (t == null) {
            return "";
        }
        String msg = t.getMessage();
        if (msg != null && !msg.isBlank()) {
            return msg;
        }
        return t.getClass()
                .getSimpleName();
    }

    /**
     * 设置任务完成后的回调。
     * <p>在 onSuccess/onFailure 中都会调用，确保无论成功失败都能通知调用方。</p>
     * <p>用于 commit+push 场景：push 完成后显示 commit done alert。</p>
     *
     * @param callback 任务完成后的回调（可为 null）
     */
    public void setOnTaskFinished(Runnable callback) {
        this.onTaskFinished = callback;
    }

    /**
     * 拿到 ProgressDialog 内部的 ProgressCallback。<br>
     * 必须把这个实例同时传给 {@link com.gitgui.domain.service.GitOperationService} 的 git 操作方法，
     * 这样 CLI 输出才会实时滚到本对话框的输出区中。
     */
    public ProgressCallback asCallback() {
        return callback;
    }

    /**
     * 把异步任务句柄绑到对话框：onProgress 复用同一个 callback + 注册成功/失败处理。
     *
     * @param handle 异步任务句柄（通常由 {@code gitOperationService.xxx(req, cb)} 返回）
     */
    public void attach(TaskHandle handle) {
        if (handle == null) {
            return;
        }
        this.taskHandle = handle;
        handle.onProgress(callback);
        handle.onSuccess(result -> Platform.runLater(() -> {
            progressBar.setProgress(1.0);
            statusLabel.setText(I18nUtil.get("progress.completed"));
            long elapsed = (System.currentTimeMillis() - startMillis) / 1000;
            // 若 git 没有任何输出（推送已同步、获取无变更等场景），给一行可视反馈避免黑屏
            if (realOutputLines == 0) {
                appendLineAndScroll("[无输出] git 命令已执行成功，输出为空（可能：远程已同步、无变更可推）");
            }
            appendLineAndScroll("");
            appendLineAndScroll("═══════════════════════════════════════");
            appendLineAndScroll(I18nUtil.get("progress.completed") + " (耗时 " + elapsed + " 秒 · git 输出 " + realOutputLines + " 行)");
            appendLineAndScroll("═══════════════════════════════════════");
            cancelButton.setText(I18nUtil.get("button.close"));
            cancelButton.setDisable(false);
            if (onTaskFinished != null) {
                try {
                    onTaskFinished.run();
                } catch (Exception ignored) {
                }
            }
        }));
        handle.onFailure(error -> Platform.runLater(() -> {
            progressBar.setProgress(0);
            progressBar.setStyle("-fx-accent: #d32f2f;");
            statusLabel.setText(I18nUtil.get("progress.failed"));
            long elapsed = (System.currentTimeMillis() - startMillis) / 1000;
            appendLineAndScroll("");
            appendLineAndScroll("═══════════════════════════════════════");
            appendLineAndScroll(I18nUtil.get("progress.failedPrefix") + safeErrorMessage(error) + " (耗时 " + elapsed + " 秒)");
            appendLineAndScroll("═══════════════════════════════════════");
            cancelButton.setText(I18nUtil.get("button.close"));
            cancelButton.setDisable(false);
            if (onTaskFinished != null) {
                try {
                    onTaskFinished.run();
                } catch (Exception ignored) {
                }
            }
        }));
    }

    /**
     * 显示对话框并异步等待任务结束（不阻塞 JavaFX 事件循环）。
     * <p>必须使用 show() 而非 showAndWait()：因为 callback.onOutput() 通过 Platform.runLater()
     * 更新 UI，若用 showAndWait() 阻塞主线程，runLater 事件无法及时处理，日志区会卡住。</p>
     */
    public void showAndWaitForTask() {
        // 注册窗口关闭时自动取消未完成任务 + 停止心跳动画
        setOnCloseRequest(e -> {
            if (heartbeatTimeline != null) {
                heartbeatTimeline.stop();
            }
            closed.set(true);
            if (taskHandle != null && (taskHandle.getStatus() == TaskStatus.PENDING || taskHandle.getStatus() == TaskStatus.RUNNING)) {
                cancelled.set(true);
                taskHandle.cancel();
            }
        });
        show();  // 非模态显示，让 JavaFX 事件循环继续处理 Platform.runLater()
    }

    private VBox buildContent(String header) {
        VBox root = new VBox(8);
        root.setPadding(new Insets(14));
        root.setFillWidth(true);

        Label headerLabel = new Label(header == null ? I18nUtil.get("progress.headerOperating") : header);
        headerLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1976d2; -fx-font-size: 13px;");
        headerLabel.setMaxWidth(Double.MAX_VALUE);

        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(20);
        progressBar.setProgress(0);

        statusLabel.setText(I18nUtil.get("progress.started"));
        statusLabel.setStyle("-fx-text-fill: #555555; -fx-font-size: 12px;");
        statusLabel.setMaxWidth(Double.MAX_VALUE);

        // 输出区：ListView<String>，避免 TextArea 在 showAndWait 嵌套事件循环下不刷新的问题
        outputList.setEditable(false);
        outputList.setPrefHeight(250);
        outputList.setMinHeight(140);
        outputList.setItems(outputLines);
        outputList.setStyle("-fx-control-inner-background: #1e1e1e; " + "-fx-background: #1e1e1e; " + "-fx-base: #1e1e1e;");
        outputList.setFixedCellSize(-1);   // 自动调整行高
        outputList.setSelectionModel(null);  // 不用选中
        outputList.setFocusModel(null);     // 不接受焦点，避免选择状态干扰显示
        // 自定义单元格：深底浅字 + 等宽字体（仿 console）
        outputList.setCellFactory(lv -> new ListCell<>() {
            private final Label label = new Label();

            {
                label.setStyle("-fx-text-fill: #d4d4d4; " + "-fx-font-family: 'Consolas','Courier New',monospace; " + "-fx-font-size: 12.5px;");
                label.setWrapText(false);
                label.setMaxWidth(Double.MAX_VALUE);
                setStyle("-fx-background-color: #1e1e1e;");
                setGraphic(label);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                label.setText(empty ? "" : (item == null ? "" : item));
            }
        });
        // 立即可见的首行：让用户知道"命令已启动"
        // 改用 opKind 枚举判断，避免之前用 title.contains("拉取")/contains("获取") 把 commit 误判为 push 的 bug。
        String firstHint = switch (opKind) {
            case COMMIT -> "▸ git commit 已启动，等待输出…";
            case PUSH -> "▸ git push 已启动，等待输出…";
            case PULL -> "▸ git pull 已启动，等待输出…";
            case FETCH -> "▸ git fetch 已启动，等待输出…";
            case CLONE -> "▸ git clone 已启动，等待输出…";
            case GC -> "▸ git gc 已启动，等待输出…";
        };
        outputLines.add(firstHint);
        VBox.setVgrow(outputList, Priority.ALWAYS);

        cancelButton.setPrefWidth(100);
        cancelButton.setCancelButton(true);
        cancelButton.setMinHeight(28);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bottom = new HBox(10, spacer, cancelButton);

        root.getChildren()
            .addAll(headerLabel, progressBar, statusLabel, outputList, bottom);
        return root;
    }

    /**
     * 心跳线程：每 1 秒更新一次状态标签（无输出 / 长时间等待时让用户知道 dialog 没卡死）
     * </br>
     * 显示规则：
     * <ul>
     *   <li>刚启动还没任何输出：显示 "正在启动…（已等待 N 秒）"</li>
     *   <li>已有输出但任务还在跑：显示 "执行中…（已等待 N 秒）"</li>
     *   <li>取消请求中：维持 "取消中…"（由 cancel 按钮 handler 设定，不被心跳覆盖）</li>
     * </ul>
     */
    /**
     * P1-009: 裸 Thread 心跳 → javafx.animation.Timeline
     */
    private void startHeartbeat() {
        heartbeatTimeline = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
            if (closed.get()) {
                heartbeatTimeline.stop();
                return;
            }
            long sec = (System.currentTimeMillis() - startMillis) / 1000;
            // 取消中/已完成/失败 → 不覆盖状态
            if (cancelRequested.get() || (taskHandle != null && taskHandle.getStatus() != TaskStatus.RUNNING)) {
                return;
            }
            String elapsed = "（已等待 " + sec + " 秒）";
            if (!firstOutput.get()) {
                statusLabel.setText("正在启动… " + elapsed);
            }
        }));
        heartbeatTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        heartbeatTimeline.play();
    }

    private void appendLineAndScroll(String line) {
        if (line == null) {
            return;
        }
        Platform.runLater(() -> {
            if (closed.get()) {
                return;
            }
            outputLines.add(line);
            // 自动滚动到底部
            try {
                outputList.scrollTo(outputLines.size() - 1);
            } catch (Exception ignored) {
            }
        });
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            super.close();
        }
    }

    /**
     * 进度对话框正在执行的 git 操作类型，用于决定：
     * <ul>
     *   <li>输出区首行 hint（"▸ git XXX 已启动，等待输出…"）</li>
     *   <li>后续 i18n 文案标签</li>
     * </ul>
     * <p>以前通过 title 字符串做 substring 匹配（如 {@code contains("拉取")}），但 commit/title
     * "仅提交" / "提交" 不命中任一分支，会被误判为 push —— 已废弃此方案，全量改用枚举。</p>
     */
    public enum OpKind {
        /**
         * git commit
         */
        COMMIT,
        /**
         * git push
         */
        PUSH,
        /**
         * git pull
         */
        PULL,
        /**
         * git fetch
         */
        FETCH,
        /**
         * git clone
         */
        CLONE,
        /**
         * git gc
         */
        GC
    }
}
