package com.gitgui.ui.dialog;

import com.gitgui.domain.constant.TaskType;
import com.gitgui.domain.model.FileChange;
import com.gitgui.domain.model.LogEntry;
import com.gitgui.domain.service.StatusService;
import com.gitgui.ui.AsyncUiLoader;
import com.gitgui.ui.i18n.I18nUtil;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志消息对话框（Log Messages）
 * <p>参照 TortoiseGit Log Messages 对话框，用于按 commit 切换分支 / 浏览历史 / 挑选 commit。</p>
 * <pre>
 * ┌─ &lt;repo&gt; - Log Messages ─────────────────────────────────────┐
 * │ From: [2025/10/16] To: [2025/10/16] [🔍 Auth] Author Email ▼  │
 * │ ┌ Graph | Message | Author | Date ─────────────────────────┐ │
 * │ │ ●     Reorganize the example skills (#1)  Keith L  2025/10│ │
 * │ │ ●     init repo                          Keith L  2025/10│ │
 * │ └─────────────────────────────────────────────────────────┘ │
 * │ SHA-1: 83291af582d2f15418854fa628a76686203c2f7a              │
 * │ * Reorganize the example skills (#1)                         │
 * │ Path:                                                         │
 * │ [+] algorithmic-art/LICENSE.txt                              │
 * │ [M] algorithmic-art/SKILL.md                                 │
 * │ ☐ All Branches                                                │
 * │ [Refresh] [Statistics] [Walk Behaviour ▼] [View ▼] [Help]   │
 * │                                  [OK]  [Cancel]              │
 * └──────────────────────────────────────────────────────────────┘
 * </pre>
 * <p>关键能力：</p>
 * <ul>
 *   <li>顶部过滤：From / To 日期、Author / Email 关键字、上下导航</li>
 *   <li>Commit 表格：Graph / Message / Author / Date 四列</li>
 *   <li>详情区：完整 SHA + commit message + 文件变更列表</li>
 *   <li>双击 commit 行 → 自动选中并返回</li>
 *   <li>底部按钮：Refresh / OK / Cancel</li>
 * </ul>
 *
 * @author FrankKang
 * @since 2026-07-25
 */
public class LogMessagesDialog extends Dialog<LogEntry> {

    private static final Logger log = LoggerFactory.getLogger(LogMessagesDialog.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    /**
     * 默认 commit 条数上限
     */
    private static final int DEFAULT_LIMIT = 500;
    private final StatusService statusService;
    private final String repoPath;
    // 顶部过滤
    private final DatePicker fromPicker = new DatePicker();
    private final DatePicker toPicker = new DatePicker();
    private final ComboBox<String> authorFilter = new ComboBox<>();
    // Commit 表格
    private final ObservableList<LogEntry> commitData = FXCollections.observableArrayList();
    private final TableView<LogEntry> commitTable = new TableView<>();
    // 详情
    private final Label shaLabel = new Label();
    private final TextArea messageArea = new TextArea();
    private final ListView<FileChange> fileListView = new ListView<>();
    private final ObservableList<FileChange> fileChangesData = FXCollections.observableArrayList();
    // 过滤缓存
    private String authorKeyword = "";
    private String messageKeyword = "";

    public LogMessagesDialog(StatusService statusService, String repoPath) {
        this.statusService = statusService;
        this.repoPath = repoPath;
        setTitle(repoPath + " - " + I18nUtil.get("log.title"));
        setHeaderText(null);
        initModality(Modality.APPLICATION_MODAL);

        DialogPane pane = getDialogPane();
        pane.setContent(buildContent());
        pane.setPrefSize(920, 660);

        ButtonType okType = new ButtonType(I18nUtil.get("button.ok"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType(I18nUtil.get("button.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType helpType = new ButtonType(I18nUtil.get("button.help"), ButtonBar.ButtonData.HELP);
        pane.getButtonTypes()
            .addAll(okType, cancelType, helpType);

        Button okButton = (Button) pane.lookupButton(okType);
        if (okButton != null) {
            okButton.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
                e.consume();
                confirmSelection();
            });
        }
        Button cancelButton = (Button) pane.lookupButton(cancelType);
        if (cancelButton != null) {
            cancelButton.addEventFilter(javafx.event.ActionEvent.ACTION, e -> setResult(null));
        }
        Button helpButton = (Button) pane.lookupButton(helpType);
        if (helpButton != null) {
            helpButton.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
                e.consume();
                showHelp();
            });
        }
        setResultConverter(buttonType -> buttonType == okType ? getSelectedCommit() : null);

        // 默认日期：From / To 都设为今天（与 TortoiseGit 默认一致：只展示今天 commit）
        fromPicker.setValue(LocalDate.now());
        toPicker.setValue(LocalDate.now());

        // 异步加载
        loadCommits();
    }

    /**
     * 取 message 第一行（避免撑爆表格行）。
     */
    private static String firstLine(String message) {
        if (message == null) {
            return "";
        }
        int nl = message.indexOf('\n');
        return nl >= 0 ? message.substring(0, nl) : message;
    }

    /**
     * 构建对话框主体。
     */
    private BorderPane buildContent() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(8));

        // === 顶部过滤 ===
        HBox topBar = new HBox(6);
        topBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        topBar.getChildren()
              .addAll(new Label(I18nUtil.get("log.from")), fromPicker, new Label(I18nUtil.get("log.to")), toPicker, buildNavigationBox(),
                      authorFilter);
        HBox.setHgrow(authorFilter, Priority.ALWAYS);
        root.setTop(topBar);
        BorderPane.setMargin(topBar, new Insets(0, 0, 6, 0));

        // === 中部：表格 + 详情 ===
        SplitPane split = new SplitPane();
        split.setOrientation(javafx.geometry.Orientation.VERTICAL);
        split.setDividerPositions(0.55);
        split.getItems()
             .addAll(buildCommitTable(), buildDetailPane());
        root.setCenter(split);

        // === 底部 ===
        HBox bottomBar = new HBox(8);
        bottomBar.setPadding(new Insets(6, 0, 0, 0));
        bottomBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Button refreshBtn = new Button(I18nUtil.get("log.btn.refresh"));
        refreshBtn.setOnAction(e -> loadCommits());
        Button statsBtn = new Button(I18nUtil.get("log.btn.statistics"));
        statsBtn.setOnAction(e -> showStatistics());
        bottomBar.getChildren()
                 .addAll(refreshBtn, statsBtn);
        root.setBottom(bottomBar);

        return root;
    }

    /**
     * 构建上下导航按钮（↓ 下一天 / ↑ 上一天）。
     */
    private HBox buildNavigationBox() {
        Button prevDay = new Button("↑");
        prevDay.setTooltip(new Tooltip(I18nUtil.get("log.nav.prevDay")));
        prevDay.setOnAction(e -> adjustDate(-1));
        Button nextDay = new Button("↓");
        nextDay.setTooltip(new Tooltip(I18nUtil.get("log.nav.nextDay")));
        nextDay.setOnAction(e -> adjustDate(1));
        Button refresh = new Button("🔍");
        refresh.setTooltip(new Tooltip(I18nUtil.get("log.nav.refresh")));
        refresh.setOnAction(e -> loadCommits());
        return new HBox(2, prevDay, nextDay, refresh);
    }

    /**
     * 日期前后调整一天并触发刷新。
     */
    private void adjustDate(int deltaDays) {
        LocalDate from = fromPicker.getValue();
        LocalDate to = toPicker.getValue();
        if (from != null) {
            fromPicker.setValue(from.plusDays(deltaDays));
        }
        if (to != null) {
            toPicker.setValue(to.plusDays(deltaDays));
        }
        loadCommits();
    }

    /**
     * 构建 commit 表格。
     */
    private TableView<LogEntry> buildCommitTable() {
        commitTable.setItems(commitData);
        commitTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        commitTable.setPlaceholder(new Label(I18nUtil.get("log.loading")));

        // Graph 列（图形 + 节点标记）
        TableColumn<LogEntry, String> colGraph = new TableColumn<>(I18nUtil.get("log.col.graph"));
        colGraph.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(renderGraph(cd.getValue())));
        colGraph.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
            }
        });
        colGraph.setPrefWidth(60);

        // Message 列
        TableColumn<LogEntry, String> colMsg = new TableColumn<>(I18nUtil.get("log.col.message"));
        colMsg.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(firstLine(cd.getValue()
                                                                                                    .getMessage())));
        colMsg.setPrefWidth(420);

        // Author 列
        TableColumn<LogEntry, String> colAuthor = new TableColumn<>(I18nUtil.get("log.col.author"));
        colAuthor.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue()
                                                                                             .getAuthor() == null ? "" : cd.getValue()
                                                                                                                           .getAuthor()));
        colAuthor.setPrefWidth(120);

        // Date 列
        TableColumn<LogEntry, String> colDate = new TableColumn<>(I18nUtil.get("log.col.date"));
        colDate.setCellValueFactory(cd -> {
            LocalDateTime t = cd.getValue()
                                .getCommitTime();
            return new javafx.beans.property.SimpleStringProperty(t == null ? "" : t.format(DATE_FORMATTER));
        });
        colDate.setPrefWidth(90);

        commitTable.getColumns()
                   .addAll(colGraph, colMsg, colAuthor, colDate);
        commitTable.setRowFactory(tv -> {
            TableRow<LogEntry> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 1) {
                    // 单击：刷新详情面板
                    updateDetailPanel(row.getItem());
                } else if (e.getClickCount() == 2 && !row.isEmpty()) {
                    // 双击：确认选择
                    setResult(row.getItem());
                }
            });
            return row;
        });

        // 监听选中变化（键盘上下键）
        commitTable.getSelectionModel()
                   .selectedItemProperty()
                   .addListener((obs, o, n) -> updateDetailPanel(n));

        return commitTable;
    }

    /**
     * 构建详情面板：SHA-1 + message + 文件变更列表。
     */
    private VBox buildDetailPane() {
        VBox detail = new VBox(6);
        detail.setPadding(new Insets(8));
        VBox.setVgrow(detail, Priority.ALWAYS);

        // SHA-1 行
        HBox shaRow = new HBox(8);
        Label shaTitle = new Label(I18nUtil.get("log.sha"));
        shaTitle.setMinWidth(60);
        shaLabel.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace;");
        shaRow.getChildren()
              .addAll(shaTitle, shaLabel);
        detail.getChildren()
              .add(shaRow);

        // message
        Label msgTitle = new Label(I18nUtil.get("log.message"));
        messageArea.setEditable(false);
        messageArea.setWrapText(true);
        messageArea.setPrefHeight(80);
        detail.getChildren()
              .addAll(msgTitle, messageArea);

        // 文件变更
        Label pathTitle = new Label(I18nUtil.get("log.path"));
        fileListView.setItems(fileChangesData);
        fileListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(FileChange item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String prefix = changeTypePrefix(item.getChangeType());
                    setText(prefix + " " + item.getDisplayPath());
                }
            }
        });
        VBox.setVgrow(fileListView, Priority.ALWAYS);
        detail.getChildren()
              .addAll(pathTitle, fileListView);

        return detail;
    }

    /**
     * 异步加载 commit 列表（按当前过滤条件）。
     */
    private void loadCommits() {
        LocalDate fromDate = fromPicker.getValue();
        LocalDate toDate = toPicker.getValue();
        LocalDateTime fromDt = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime toDt = toDate == null ? null : toDate.atTime(LocalTime.MAX);

        final String author = authorKeyword;
        final String message = messageKeyword;
        commitTable.setPlaceholder(new Label(I18nUtil.get("log.loading")));

        AsyncUiLoader.submitRead(repoPath, TaskType.STATUS, () -> {
            try {
                List<LogEntry> list = statusService.listLogEntries(repoPath, fromDt, toDt, author, message, DEFAULT_LIMIT);
                Platform.runLater(() -> {
                    commitData.setAll(list);
                    commitTable.setPlaceholder(new Label(I18nUtil.get("log.empty")));
                    if (!list.isEmpty()) {
                        commitTable.getSelectionModel()
                                   .select(0);
                    } else {
                        clearDetailPanel();
                    }
                });
            } catch (Exception e) {
                log.error("加载 commit 列表失败：{}", repoPath, e);
                Platform.runLater(() -> commitTable.setPlaceholder(new Label(I18nUtil.get("log.loadFailed") + "：" + e.getMessage())));
            }
        });
    }

    /**
     * 更新详情面板（commit 选中时调用）。
     */
    private void updateDetailPanel(LogEntry entry) {
        if (entry == null) {
            clearDetailPanel();
            return;
        }
        shaLabel.setText(entry.getCommitId());
        String msg = entry.getMessage() == null ? "" : entry.getMessage();
        messageArea.setText(msg);
        fileChangesData.clear();

        // 异步加载文件变更
        AsyncUiLoader.submitRead(repoPath, TaskType.STATUS, () -> {
            try {
                List<FileChange> changes = statusService.getCommitChanges(repoPath, entry.getCommitId());
                Platform.runLater(() -> fileChangesData.setAll(changes));
            } catch (Exception e) {
                log.warn("加载 commit 文件变更失败：{}", entry.getCommitId(), e);
                Platform.runLater(() -> fileChangesData.clear());
            }
        });
    }

    /**
     * 清空详情面板。
     */
    private void clearDetailPanel() {
        shaLabel.setText("");
        messageArea.clear();
        fileChangesData.clear();
    }

    /**
     * 确认选择。
     */
    private void confirmSelection() {
        LogEntry sel = commitTable.getSelectionModel()
                                  .getSelectedItem();
        if (sel == null) {
            new Alert(Alert.AlertType.WARNING, I18nUtil.get("log.selectRequired")).showAndWait();
            return;
        }
        setResult(sel);
    }

    /**
     * 获取当前选中的 commit。
     */
    private LogEntry getSelectedCommit() {
        return commitTable.getSelectionModel()
                          .getSelectedItem();
    }

    /**
     * 显示帮助信息。
     */
    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18nUtil.get("log.help.title"));
        alert.setHeaderText(I18nUtil.get("log.help.header"));
        alert.setContentText(I18nUtil.get("log.help.content"));
        alert.showAndWait();
    }

    /**
     * 显示统计信息（commit 数 + 作者分布）。
     */
    private void showStatistics() {
        int total = commitData.size();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18nUtil.get("log.stats.title"));
        alert.setHeaderText(I18nUtil.get("log.stats.header"));
        alert.setContentText(String.format(I18nUtil.get("log.stats.content"), total));
        alert.showAndWait();
    }

    /**
     * 渲染 commit graph 节点（TortoiseGit 风格简版）：
     * <ul>
     *   <li>merge commit：M（多 parent）</li>
     *   <li>普通 commit：●</li>
     *   <li>初始 commit：★</li>
     * </ul>
     */
    private String renderGraph(LogEntry entry) {
        if (entry == null) {
            return "";
        }
        if (entry.getParents() == null || entry.getParents()
                                               .isEmpty()) {
            return "★";
        }
        if (entry.getParents()
                 .size() > 1) {
            return "M";
        }
        return "●";
    }

    /**
     * 文件变更类型 → 显示前缀字符。
     */
    private String changeTypePrefix(String changeType) {
        if (changeType == null) {
            return "?";
        }
        switch (changeType) {
            case "ADD":
                return "[+]";
            case "DELETE":
                return "[-]";
            case "MODIFY":
                return "[M]";
            case "RENAME":
                return "[→]";
            case "COPY":
                return "[C]";
            default:
                return "[?]";
        }
    }
}