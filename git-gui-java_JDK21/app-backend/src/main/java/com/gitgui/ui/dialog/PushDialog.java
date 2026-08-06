package com.gitgui.ui.dialog;

import com.gitgui.core.async.ProgressCallback;
import com.gitgui.core.async.TaskHandle;
import com.gitgui.core.exception.RedLineBlockedException;
import com.gitgui.domain.constant.TaskType;
import com.gitgui.domain.model.LogEntry;
import com.gitgui.domain.model.RemoteConfig;
import com.gitgui.domain.model.request.PushRequest;
import com.gitgui.domain.service.GitOperationService;
import com.gitgui.domain.service.RemoteConfigService;
import com.gitgui.ui.AsyncUiLoader;
import com.gitgui.ui.i18n.I18nUtil;
import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 推送对话框（简化版）。
 * <p>仅暴露 Remote + 分支两个核心选择，commit 列表实时刷新（待推送 / 待拉取），</p>
 * <p>其他高级选项（Force / Tags / Push All Branches / 临时 URL 等）已下线，符合 BR-09 + 极简原则。</p>
 *
 * <p>v3 改进（用户反馈）：</p>
 * <ol>
 *   <li>删除全部 7 个 CheckBox + 高级模式 + 临时 URL — 推送弹窗极简</li>
 *   <li>修#2：移除顶部冗余 syncStatusLabel（与 outgoingHeader 重复显示），outgoingHeader 单一表达状态</li>
 *   <li>修#3：保存 pushButtonType 引用，setPushButtonEnabled 用同一引用 lookup，
 *       避免每次 new ButtonType 引用不等导致按钮无法 setDisable(false)，永久置灰</li>
 * </ol>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class PushDialog extends Dialog<Void> {

    private static final Logger log = LoggerFactory.getLogger(PushDialog.class);

    /**
     * 待展示的 commit 数上限
     */
    private static final int OUTGOING_LIMIT = 50;
    private static final int INCOMING_LIMIT = 50;
    /**
     * 时间格式
     */
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final GitOperationService gitOperationService;
    private final String repoPath;

    /**
     * Remote 下拉
     */
    private final ComboBox<String> remoteCombo = new ComboBox<>();
    /**
     * 分支下拉
     */
    private final ComboBox<String> branchCombo = new ComboBox<>();

    /**
     * 待推送 commit 表格
     */
    private final TableView<LogEntry> outgoingTable = new TableView<>();
    /**
     * 远端领先 commit 表格
     */
    private final TableView<LogEntry> incomingTable = new TableView<>();
    /**
     * 当前 outgoing 列表
     */
    private final ObservableList<LogEntry> outgoingItems = FXCollections.observableArrayList();
    /**
     * 当前 incoming 列表
     */
    private final ObservableList<LogEntry> incomingItems = FXCollections.observableArrayList();
    /**
     * outgoing 区标题（合并了原 syncStatusLabel 的全部状态展示）
     */
    private final Label outgoingHeader = new Label();
    /**
     * incoming 区标题
     */
    private final Label incomingHeader = new Label();
    /**
     * "刷新"按钮
     */
    private final Button refreshButton = new Button(I18nUtil.get("push.refreshCommits"));

    /**
     * "推送"按钮类型引用——必须在构造时保存，setPushButtonEnabled 用同一引用 lookup
     */
    private final ButtonType pushButtonType = new ButtonType(I18nUtil.get("button.push"), ButtonBar.ButtonData.OK_DONE);

    private RemoteConfigService remoteConfigService;
    private com.gitgui.infrastructure.cli.CliGitExecutor gitExecutor;

    /**
     * 当前选择触发查询去抖的 timer
     */
    private volatile Timer queryDebounceTimer;

    /**
     * 构造推送对话框。
     */
    public PushDialog(GitOperationService gitOperationService, String repoPath) {
        this.gitOperationService = gitOperationService;
        this.repoPath = repoPath;
        setTitle(I18nUtil.get("push.title"));
        setHeaderText(null);
        initModality(Modality.APPLICATION_MODAL);

        DialogPane pane = getDialogPane();
        pane.setContent(buildContent());
        pane.getButtonTypes()
            .addAll(ButtonType.CANCEL, pushButtonType);

        // Guice 取远端配置 + git 执行器
        try {
            remoteConfigService = com.gitgui.GitGuiApp.getInjector()
                                                      .getInstance(RemoteConfigService.class);
        } catch (Exception e) {
            log.warn("无法获取 RemoteConfigService，使用默认 origin", e);
        }
        try {
            gitExecutor = com.gitgui.GitGuiApp.getInjector()
                                              .getInstance(com.gitgui.infrastructure.cli.CliGitExecutor.class);
        } catch (Exception e) {
            log.warn("无法获取 CliGitExecutor，commit 列表禁用", e);
        }

        // 初次状态文案
        outgoingHeader.setText(I18nUtil.get("push.status.fallback"));
        incomingHeader.setVisible(false);
        incomingTable.setVisible(false);

        loadRemotes();

        // OK 按钮触发推送
        Button okButton = (Button) pane.lookupButton(pushButtonType);
        if (okButton != null) {
            okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                event.consume();
                doPush();
            });
            // 初次禁用直到 commit 列表加载完毕
            okButton.setDisable(true);
        }

        // 切换 Remote / 分支触发 commit 列表刷新（去抖 250ms）
        remoteCombo.valueProperty()
                   .addListener(this::onRemoteOrBranchChanged);
        branchCombo.valueProperty()
                   .addListener(this::onRemoteOrBranchChanged);

        setResultConverter(buttonType -> null);
    }

    /**
     * i18n 占位符封装。
     */
    private static String formatI18n(String key, Object... params) {
        String template = I18nUtil.get(key);
        if (params == null || params.length == 0) {
            return template;
        }
        try {
            return MessageFormat.format(template, params);
        } catch (Exception e) {
            log.warn("i18n 占位符格式化失败：key={}", key, e);
            return template;
        }
    }

    /**
     * 构建对话框内容：Remote + 分支两行 + 双 commit 表格。
     */
    private VBox buildContent() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(12));
        vbox.setPrefWidth(640);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label(I18nUtil.get("push.remote")), 0, 0);
        grid.add(remoteCombo, 1, 0);
        grid.add(new Label(I18nUtil.get("push.branch")), 0, 1);
        grid.add(branchCombo, 1, 1);

        // commit 面板：单 refreshButton 放在 outgoing 标题右侧
        outgoingHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #4ec9b0;");
        incomingHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #f0a070;");

        outgoingTable.setItems(outgoingItems);
        outgoingTable.setPlaceholder(new Label(I18nUtil.get("push.status.fallback")));
        configureCommitsTable(outgoingTable);

        incomingTable.setItems(incomingItems);
        incomingTable.setPlaceholder(new Label(""));
        configureCommitsTable(incomingTable);

        // 顶部 "outgoing 标题 + 刷新" 一行
        HBox outgoingTitleBar = new HBox(8);
        outgoingTitleBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        outgoingTitleBar.getChildren()
                        .addAll(outgoingHeader, spacer, refreshButton);
        refreshButton.setOnAction(e -> queryOutgoingAndIncoming());

        // 双表：outgoing 优先占更大空间，incoming 较小
        VBox.setVgrow(outgoingTable, Priority.ALWAYS);
        VBox.setVgrow(incomingTable, Priority.ALWAYS);
        outgoingTable.setMinHeight(120);
        outgoingTable.setPrefHeight(220);
        incomingTable.setMinHeight(60);
        incomingTable.setPrefHeight(120);

        VBox commitsPanel = new VBox(6, outgoingTitleBar, outgoingTable, incomingHeader, incomingTable);
        commitsPanel.setPadding(new Insets(4, 0, 0, 0));
        commitsPanel.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 4;");

        vbox.getChildren()
            .addAll(grid, commitsPanel);
        return vbox;
    }

    private void configureCommitsTable(TableView<LogEntry> table) {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<LogEntry, String> shaCol = new TableColumn<>(I18nUtil.get("push.col.shortSha"));
        shaCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue() == null ? "" : cd.getValue()
                                                                                                 .getShortId()));
        shaCol.setPrefWidth(80);

        TableColumn<LogEntry, String> authorCol = new TableColumn<>(I18nUtil.get("push.col.author"));
        authorCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue() == null ? "" : cd.getValue()
                                                                                                    .getAuthor()));
        authorCol.setPrefWidth(120);

        TableColumn<LogEntry, String> dateCol = new TableColumn<>(I18nUtil.get("push.col.date"));
        dateCol.setCellValueFactory(cd -> {
            if (cd.getValue() == null || cd.getValue()
                                           .getCommitTime() == null) {
                return new SimpleStringProperty("");
            }
            return new SimpleStringProperty(cd.getValue()
                                              .getCommitTime()
                                              .format(TS_FMT));
        });
        dateCol.setPrefWidth(130);

        TableColumn<LogEntry, String> msgCol = new TableColumn<>(I18nUtil.get("push.col.message"));
        msgCol.setCellValueFactory(cd -> {
            if (cd.getValue() == null) {
                return new SimpleStringProperty("");
            }
            String firstLine = cd.getValue()
                                 .getMessage() == null ? "" : cd.getValue()
                                                                .getMessage();
            int nl = firstLine.indexOf('\n');
            return new SimpleStringProperty(nl > 0 ? firstLine.substring(0, nl) : firstLine);
        });
        msgCol.setPrefWidth(280);

        table.getColumns()
             .setAll(shaCol, authorCol, dateCol, msgCol);
        table.setRowFactory(tv -> new javafx.scene.control.TableRow<>() {
            @Override
            protected void updateItem(LogEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setTooltip(null);
                } else {
                    setTooltip(new Tooltip(item.getMessage()));
                }
            }
        });
    }

    /**
     * 加载 Remote 列表。
     */
    private void loadRemotes() {
        AsyncUiLoader.submitRead(repoPath, TaskType.STATUS, () -> {
            try {
                if (remoteConfigService != null) {
                    List<RemoteConfig> remotes = remoteConfigService.list(repoPath);
                    Platform.runLater(() -> {
                        remoteCombo.getItems()
                                   .clear();
                        for (RemoteConfig r : remotes) {
                            remoteCombo.getItems()
                                       .add(r.getName());
                        }
                        if (!remotes.isEmpty()) {
                            remoteCombo.getSelectionModel()
                                       .select(0);
                            loadBranches(remotes.get(0)
                                                .getName());
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        remoteCombo.getItems()
                                   .add("origin");
                        remoteCombo.getSelectionModel()
                                   .select(0);
                        loadBranches("origin");
                    });
                }
            } catch (Exception e) {
                log.error("加载 Remote 列表失败", e);
                Platform.runLater(() -> {
                    remoteCombo.getItems()
                               .add("origin");
                    remoteCombo.getSelectionModel()
                               .select(0);
                    loadBranches("origin");
                });
            }
        });
    }

    /**
     * 加载本地分支列表。
     */
    private void loadBranches(String remote) {
        AsyncUiLoader.submitRead(repoPath, TaskType.STATUS, () -> {
            try {
                com.gitgui.infrastructure.cli.CliGitExecutor executor = gitExecutor;
                if (executor == null) {
                    executor = com.gitgui.GitGuiApp.getInjector()
                                                   .getInstance(com.gitgui.infrastructure.cli.CliGitExecutor.class);
                    gitExecutor = executor;
                }
                List<String> branches = executor.listBranches(repoPath);
                String current = executor.getCurrentBranch(repoPath);
                Platform.runLater(() -> {
                    branchCombo.getItems()
                               .clear();
                    for (String b : branches) {
                        String name = b.replace("refs/heads/", "");
                        branchCombo.getItems()
                                   .add(name);
                    }
                    if (current != null && !current.equals("UNKNOWN") && !current.equals("DETACHED_HEAD")) {
                        branchCombo.getSelectionModel()
                                   .select(current.replace("refs/heads/", ""));
                    } else if (!branches.isEmpty()) {
                        branchCombo.getSelectionModel()
                                   .select(0);
                    }
                });
            } catch (Exception e) {
                log.error("加载分支列表失败", e);
            }
        });
    }

    /**
     * Remote 或分支变化时触发 commit 列表刷新。
     */
    private void onRemoteOrBranchChanged(ObservableValue<? extends String> obs, String oldVal, String newVal) {
        if (newVal == null || newVal.isBlank()) {
            return;
        }
        debounceQueryCommits();
    }

    /**
     * 去抖 250ms 后触发查询。
     */
    private void debounceQueryCommits() {
        Timer prev = queryDebounceTimer;
        if (prev != null) {
            prev.cancel();
        }
        Timer timer = new Timer("push-dialog-query-debounce", true);
        queryDebounceTimer = timer;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> queryOutgoingAndIncoming());
            }
        }, 250);
    }

    /**
     * 异步查询 outgoing / incoming commit。
     */
    private void queryOutgoingAndIncoming() {
        final String remote = remoteCombo.getValue();
        final String branch = branchCombo.getValue();
        if (remote == null || remote.isBlank() || branch == null || branch.isBlank()) {
            return;
        }
        if (gitExecutor == null) {
            try {
                gitExecutor = com.gitgui.GitGuiApp.getInjector()
                                                  .getInstance(com.gitgui.infrastructure.cli.CliGitExecutor.class);
            } catch (Exception e) {
                log.warn("CliGitExecutor 不可用，跳过 commit 列表查询", e);
                return;
            }
        }

        // 加载中态
        Platform.runLater(() -> {
            outgoingHeader.setText(I18nUtil.get("push.status.fallback"));
            refreshButton.setDisable(true);
        });

        final com.gitgui.infrastructure.cli.CliGitExecutor executor = gitExecutor;
        AsyncUiLoader.submitRead(repoPath, TaskType.STATUS, () -> {
            try {
                boolean remoteExists = executor.remoteRefExists(repoPath, remote, branch);
                String fromRef = remoteExists ? (remote + "/" + branch) : null;

                List<LogEntry> outgoing = executor.getCommitsBetween(repoPath, fromRef, branch, OUTGOING_LIMIT);
                List<LogEntry> incoming = executor.getCommitsBetween(repoPath, branch, fromRef, INCOMING_LIMIT);

                Platform.runLater(() -> renderCommitsResult(outgoing, incoming, remoteExists, remote, branch));
            } catch (Exception e) {
                log.error("查询 commit 列表失败", e);
                Platform.runLater(() -> renderCommitsError(e));
            }
        });
    }

    /**
     * 渲染查询结果到 UI（修复#2：单一 outgoingHeader 表达全部状态，去掉冗余顶部 Label）。
     */
    private void renderCommitsResult(List<LogEntry> outgoing, List<LogEntry> incoming, boolean remoteExists, String remote, String branch) {
        refreshButton.setDisable(false);

        outgoingItems.setAll(outgoing);
        boolean showIncoming = !remoteExists || !incoming.isEmpty();
        incomingItems.setAll(incoming);
        incomingHeader.setVisible(showIncoming);
        incomingTable.setVisible(showIncoming);

        int outgoingCount = outgoing.size();
        int incomingCount = incoming.size();

        boolean pushEnabled;
        if (outgoingCount > 0) {
            // 有待推 → 显示 "待推送 commit（X 个）" + 按钮可用
            outgoingHeader.setText(formatI18n("push.section.outgoing", outgoingCount));
            if (showIncoming) {
                incomingHeader.setText(formatI18n("push.section.incoming", incomingCount));
            }
            pushEnabled = true;
        } else if (incomingCount > 0) {
            // 没待推但远端有 → 不显示 outgoing 标题（用一个空态文案），禁用推送
            outgoingHeader.setText(I18nUtil.get("push.emptyReason.needPull")
                                           .replace("{0}", String.valueOf(incomingCount)));
            incomingHeader.setText(formatI18n("push.section.incoming", incomingCount));
            pushEnabled = false;
        } else {
            // 完全同步
            if (remoteExists) {
                outgoingHeader.setText(I18nUtil.get("push.status.upToDate"));
            } else {
                outgoingHeader.setText(I18nUtil.get("push.status.notFound")
                                               .replace("{branch}", branch) + "（" + formatI18n("push.section.outgoing", outgoingCount) + "）");
            }
            pushEnabled = true;
        }
        outgoingTable.setPlaceholder(
                new Label(outgoingCount == 0 && remoteExists ? I18nUtil.get("push.emptyReason.noCommits") : I18nUtil.get("push.status.fallback")));
        setPushButtonEnabled(pushEnabled);
    }

    /**
     * 加载失败时的状态。
     */
    private void renderCommitsError(Exception e) {
        refreshButton.setDisable(false);
        outgoingHeader.setText(I18nUtil.get("push.status.failed") + e.getMessage());
        incomingHeader.setVisible(false);
        incomingTable.setVisible(false);
        // 加载失败保守起见禁用推送，避免静默推空
        setPushButtonEnabled(false);
    }

    /**
     * 设置"推送"按钮的可用性。
     * <p>修复#3：用 {@link #pushButtonType} 字段引用 lookup，避免每次 new ButtonType 引用不等。</p>
     */
    private void setPushButtonEnabled(boolean enabled) {
        DialogPane pane = getDialogPane();
        Node btn = pane.lookupButton(pushButtonType);
        if (btn instanceof Button) {
            ((Button) btn).setDisable(!enabled);
        }
    }

    /**
     * 执行推送。
     */
    private void doPush() {
        String remote = remoteCombo.getValue();
        String branch = branchCombo.getValue();
        if (remote == null || remote.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, I18nUtil.get("push.selectRemote")).showAndWait();
            return;
        }
        if (branch == null || branch.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, I18nUtil.get("push.selectBranch")).showAndWait();
            return;
        }

        PushRequest req = PushRequest.builder()
                                     .repoPath(repoPath)
                                     .remote(remote)
                                     .branch(branch)
                                     .build();

        Stage owner = (getDialogPane().getScene() == null) ? null : (Stage) getDialogPane().getScene()
                                                                                           .getWindow();
        close();

        Platform.runLater(() -> {
            ProgressDialog progress = new ProgressDialog(owner, I18nUtil.get("push.title") + "  →  " + remote + "/" + branch,
                                                         I18nUtil.get("progress.headerOperating"), ProgressDialog.OpKind.PUSH);
            ProgressCallback sharedCb = progress.asCallback();
            try {
                TaskHandle handle = gitOperationService.push(req, sharedCb);
                progress.attach(handle);
                progress.showAndWaitForTask();
            } catch (RedLineBlockedException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(I18nUtil.get("redline.blocked.title"));
                alert.setHeaderText(I18nUtil.get("redline.blocked.hitRule") + e.getRuleCode());
                alert.setContentText(e.getMessage());
                alert.showAndWait();
                Platform.runLater(progress::close);
            } catch (Exception e) {
                log.error("推送异常", e);
                new Alert(Alert.AlertType.ERROR, I18nUtil.get("push.error") + e.getMessage()).showAndWait();
                Platform.runLater(progress::close);
            }
        });
    }
}
