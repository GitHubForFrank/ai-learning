package com.gitgui.ui.dialog;

import com.gitgui.core.async.ProgressCallback;
import com.gitgui.core.async.TaskHandle;
import com.gitgui.core.exception.RedLineBlockedException;
import com.gitgui.domain.constant.TaskType;
import com.gitgui.domain.model.DiffResult;
import com.gitgui.domain.model.FileStatus;
import com.gitgui.domain.model.LogEntry;
import com.gitgui.domain.model.request.CommitRequest;
import com.gitgui.domain.service.GitOperationService;
import com.gitgui.domain.service.StatusService;
import com.gitgui.ui.AsyncUiLoader;
import com.gitgui.ui.i18n.I18nUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 提交对话框（精简版）
 * <p>对应 PRD 4.4。本版相比 TortoiseGit 风格布局做了大幅简化：</p>
 * <ul>
 *   <li>移除 Amend Last Commit / GPG 签名 / 自定义作者等高级选项</li>
 *   <li>移除「new branch」分支（属于提交副作用，简洁起见不混合到提交请求里）</li>
 *   <li>移除「显示未跟踪文件」「不自动选中子模块」「显示整个项目」「仅信息」四个底部选项</li>
 *   <li>移除「查看补丁」「Help」按钮</li>
 * </ul>
 * <p>保留：提交目标分支展示、Message、字符计数、变更文件表格（行点击切换勾选）、过滤按钮、
 * 双击查看 diff、底部 3 个按钮（仅提交 / 提交并推送 / 取消）。</p>
 * <p><b>提交过程进度</b>：通过 {@link com.gitgui.core.async.ProgressCallback} 把 git commit
 * 进程输出实时推到 {@link ProgressDialog}，实现「像 fetch 一样输出过程」的体验。</p>
 *
 * <p>遵循 BR-06（至少勾选一个文件）、BR-07（message 非空）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class CommitDialog extends Dialog<Void> {

    private static final Logger log = LoggerFactory.getLogger(CommitDialog.class);

    /**
     * 消息最大行数（用于 TextArea 渲染高度，TortoiseGit 风格 4 行）
     */
    private static final int MESSAGE_VISIBLE_ROWS = 4;
    /**
     * 默认显示所有类型的文件（含 Untracked）—— 简化版无 UI 切换
     */
    private static final boolean DEFAULT_SHOW_UNVERSIONED = true;
    private final GitOperationService gitOperationService;
    private final StatusService statusService;
    /**
     * Remote 配置服务（用于「Commit & Push」时自动选 Remote）
     */
    private final com.gitgui.domain.service.RemoteConfigService remoteConfigService;
    private final String repoPath;
    // ====== 顶部：Commit to ======
    private final Label branchLabel = new Label();
    // ====== 消息输入 ======
    private final TextArea messageArea = new TextArea();
    private final Label charCounter = new Label();
    // ====== 变更文件表格 ======
    private final Label checkAllCheck = new Label(I18nUtil.get("commit.check"));
    private final TableView<FileRow> fileTable = new TableView<>();
    private final ObservableList<FileRow> allRows = FXCollections.observableArrayList();
    /**
     * 经过滤（按 Check / 状态过滤）后真正展示的行
     */
    private final ObservableList<FileRow> displayedRows = FXCollections.observableArrayList();
    // ====== 过滤按钮：All / None 是 action 按钮，其他是 visibility 过滤 ======
    private final ToggleGroup filterGroup = new ToggleGroup();
    private final Button checkAllActionBtn = makeActionButton("commit.filter.all");
    private final Button checkNoneActionBtn = makeActionButton("commit.filter.none");
    private final ToggleButton filterUnversioned = makeFilter("commit.filter.unversioned", "UNVERSIONED");
    private final ToggleButton filterVersioned = makeFilter("commit.filter.versioned", "VERSIONED");
    private final ToggleButton filterAdded = makeFilter("commit.filter.added", "ADDED");
    private final ToggleButton filterDeleted = makeFilter("commit.filter.deleted", "DELETED");
    private final ToggleButton filterModified = makeFilter("commit.filter.modified", "MODIFIED");
    private final ToggleButton filterFiles = makeFilter("commit.filter.files", "FILES");
    private final Label fileCountLabel = new Label();
    // ====== 底部按钮 ======
    private final Button commitBtn = new Button(I18nUtil.get("commit.action.commit"));
    private final Button commitAndPushBtn = new Button(I18nUtil.get("commit.action.commitAndPush"));
    private int fileCountTotal = 0;
    private Button cancelButtonRef;
    /**
     * commit 进行中标记
     */
    private boolean committing = false;

    public CommitDialog(GitOperationService gitOperationService, StatusService statusService,
            com.gitgui.domain.service.RemoteConfigService remoteConfigService, String repoPath) {
        this.gitOperationService = gitOperationService;
        this.statusService = statusService;
        this.remoteConfigService = remoteConfigService;
        this.repoPath = repoPath;
        setTitle(repoPath + " - " + I18nUtil.get("commit.title"));
        setHeaderText(null);
        // 使用 APPLICATION_MODAL：commit/push 进行中阻塞主窗口的所有点击，
        // 防止用户误点仓库触发同步 git CLI 调用导致 UI 卡顿。
        // 注：ProgressDialog 用 owner=null 跳过模态作用域，能正常接收事件。
        initModality(Modality.APPLICATION_MODAL);

        DialogPane pane = getDialogPane();
        pane.setContent(buildContent());
        pane.setPrefSize(760, 620);

        setResultConverter(buttonType -> null);

        // 兜底关闭：点 X 或 Esc 都能退出（commit 进行中不允许关）
        setOnShown(e -> {
            javafx.stage.Window win = getDialogPane().getScene()
                                                     .getWindow();
            if (win != null) {
                win.setOnCloseRequest(ev -> {
                    if (committing) {
                        ev.consume();
                    } else {
                        closeDialogSafely();
                    }
                });
            }
        });

        // 加载当前分支 + 变更文件
        loadBranchAndFiles();
    }

    private static Region spacer() {
        Region r = new Region();
        r.getStyleClass()
         .add("spacer");
        return r;
    }

    // ================================================================
    //  整体布局
    // ================================================================

    private static String stateStyle(FileStatus.FileState s) {
        if (s == null) {
            return "";
        }
        return switch (s) {
            case MODIFIED -> "-fx-text-fill: #1976d2;";
            case STAGED, UNTRACKED -> "-fx-text-fill: #388e3c;";
            case DELETED -> "-fx-text-fill: #d32f2f;";
            case CONFLICT -> "-fx-text-fill: #d32f2f; -fx-font-weight: bold;";
            default -> "";
        };
    }

    private VBox buildContent() {
        VBox root = new VBox(8);
        root.setPadding(new Insets(10));

        VBox changesBox = buildChangesBox();
        root.getChildren()
            .addAll(buildCommitToBox(), buildMessageBox(), changesBox, buildFileCountBox(), buildBottomActionBar());
        VBox.setVgrow(changesBox, Priority.ALWAYS); // changes 区占满剩余空间
        return root;
    }

    /**
     * 底部按钮行：仅提交 / 提交并推送 / 取消，统一右对齐。
     */
    private HBox buildBottomActionBar() {
        String primaryStyle = "-fx-base: #1976d2; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;";

        commitBtn.setStyle(primaryStyle);
        commitBtn.setPrefWidth(120);
        commitBtn.setDefaultButton(true);
        commitBtn.setOnAction(e -> doCommit(CommitAction.COMMIT));

        commitAndPushBtn.setStyle(primaryStyle);
        commitAndPushBtn.setPrefWidth(140);
        commitAndPushBtn.setOnAction(e -> doCommit(CommitAction.COMMIT_AND_PUSH));

        Button cancelButton = new Button(I18nUtil.get("button.cancel"));
        cancelButton.setCancelButton(true);
        cancelButton.setDefaultButton(false);
        cancelButton.setPrefWidth(90);
        cancelButton.setOnAction(e -> {
            if (committing) {
                e.consume();
                return;
            }
            closeDialogSafely();
        });
        this.cancelButtonRef = cancelButton;

        Region spacer = new Region();
        HBox bar = new HBox(10, spacer, commitBtn, commitAndPushBtn, cancelButton);
        bar.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        bar.setPadding(new Insets(8, 0, 0, 0));
        return bar;
    }

    /**
     * 顶部「Commit to: master」一行（精简版：只显示当前分支，不再提供 new branch 选择）。
     */
    private HBox buildCommitToBox() {
        Label toLabel = new Label(I18nUtil.get("commit.to"));
        toLabel.setMinWidth(70);
        branchLabel.setText("...");
        branchLabel.setStyle("-fx-font-weight: bold;");

        HBox box = new HBox(10, toLabel, branchLabel, spacer());
        HBox.setHgrow(box.getChildren()
                         .get(2), Priority.ALWAYS);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    /**
     * Message 输入区：Label + 多行 TextArea + 字符计数。
     */
    private VBox buildMessageBox() {
        Label msgLabel = new Label(I18nUtil.get("commit.message"));
        messageArea.setPromptText(I18nUtil.get("commit.messageHelp"));
        messageArea.setWrapText(true);
        messageArea.setPrefRowCount(MESSAGE_VISIBLE_ROWS);
        VBox.setVgrow(messageArea, Priority.NEVER);
        // 保留右键菜单（TortoiseGit 风格：pick commit hash / paste filename list 等）
        messageArea.setContextMenu(buildMessageContextMenu());
        updateCharCounter();
        messageArea.textProperty()
                   .addListener((obs, o, n) -> updateCharCounter());

        HBox counterRow = new HBox(spacer(), charCounter);
        HBox.setHgrow(counterRow.getChildren()
                                .get(0), Priority.ALWAYS);
        counterRow.setAlignment(Pos.CENTER_RIGHT);

        VBox box = new VBox(4, msgLabel, messageArea, counterRow);
        return box;
    }

    /**
     * 变更文件区：表头 + 过滤按钮 + TableView（按状态分组）。
     */
    private VBox buildChangesBox() {
        Label header = new Label(I18nUtil.get("commit.changesMade"));
        header.setStyle("-fx-font-weight: bold;");

        HBox checkRow = new HBox(10, checkAllCheck, makeFilterBar());
        checkRow.setAlignment(Pos.CENTER_LEFT);

        TableColumn<FileRow, String> pathCol = new TableColumn<>(I18nUtil.get("commit.col.path"));
        pathCol.setCellValueFactory(d -> d.getValue()
                                          .pathProperty());
        pathCol.setPrefWidth(360);
        pathCol.setCellFactory(col -> new PathCell());

        TableColumn<FileRow, String> extCol = new TableColumn<>(I18nUtil.get("commit.col.extension"));
        extCol.setCellValueFactory(d -> d.getValue()
                                         .extensionProperty());
        extCol.setPrefWidth(80);

        TableColumn<FileRow, String> statusCol = new TableColumn<>(I18nUtil.get("commit.col.status"));
        statusCol.setCellValueFactory(d -> d.getValue()
                                            .statusProperty());
        statusCol.setPrefWidth(110);

        TableColumn<FileRow, Number> addCol = new TableColumn<>(I18nUtil.get("commit.col.added"));
        addCol.setCellValueFactory(d -> d.getValue()
                                         .addedLinesProperty());
        addCol.setPrefWidth(60);
        addCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<FileRow, Number> delCol = new TableColumn<>(I18nUtil.get("commit.col.removed"));
        delCol.setCellValueFactory(d -> d.getValue()
                                         .deletedLinesProperty());
        delCol.setPrefWidth(60);
        delCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        fileTable.getColumns()
                 .addAll(pathCol, extCol, statusCol, addCol, delCol);
        fileTable.setItems(displayedRows);
        fileTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        fileTable.setPrefHeight(220);
        fileTable.setPlaceholder(new Label(""));
        fileTable.getStyleClass()
                 .add("commit-table");
        fileTable.setRowFactory(tv -> new FileRowTableRow());
        fileTable.setOnMouseClicked(e -> {
            if (e.getButton() != javafx.scene.input.MouseButton.PRIMARY) {
                return;
            }
            FileRow row = (FileRow) fileTable.getSelectionModel()
                                             .getSelectedItem();
            if (row == null || !row.isFile()) {
                return;
            }
            if (e.getClickCount() == 1) {
                row.setSelected(!row.isSelected());
                updateFileCount();
            } else if (e.getClickCount() == 2) {
                openDiffForRow(row);
            }
        });
        VBox.setVgrow(fileTable, Priority.ALWAYS);

        VBox box = new VBox(4, header, checkRow, fileTable);
        return box;
    }

    /**
     * 文件计数行（保留为一行，右对齐，紧挨底部按钮上方）。
     */
    private HBox buildFileCountBox() {
        HBox row = new HBox(fileCountLabel);
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setPadding(new Insets(4, 0, 0, 0));
        return row;
    }

    private void updateCharCounter() {
        String text = messageArea.getText() == null ? "" : messageArea.getText();
        int firstLineLen = text.isEmpty() ? 0 : (text.indexOf('\n') < 0 ? text.length() : text.indexOf('\n'));
        int total = text.length();
        charCounter.setText(MessageFormat.format(I18nUtil.get("commit.charCounter"), firstLineLen, Math.max(1, total == 0 ? 1 : total)));
    }

    private void closeDialogSafely() {
        try {
            setResult(null);
        } catch (Exception ignored) {
        }
        Platform.runLater(() -> {
            try {
                close();
            } catch (Exception ex) {
                log.warn("Dialog.close() 失败，fallback 到 window.hide()：{}", ex.getMessage());
            }
            try {
                javafx.stage.Window win = getDialogPane().getScene() == null ? null : getDialogPane().getScene()
                                                                                                     .getWindow();
                if (win != null && win.isShowing()) {
                    win.hide();
                }
            } catch (Exception ignored) {
            }
        });
    }

    // ================================================================
    //  Diff / 双击 / 表格
    // ================================================================

    private void openDiffForRow(FileRow row) {
        if (row == null || !row.isFile()) {
            return;
        }
        String path = row.getPath();
        FileStatus.FileState state = row.getState();
        Stage owner = (Stage) getDialogPane().getScene()
                                             .getWindow();
        DiffViewerDialog.show(owner, repoPath, path, state, "HEAD", null, new DiffViewerDialog.DiffProvider() {
            @Override
            public String getDiff(String repoPath, String path, String oldRev, String newRev) {
                DiffResult r = statusService.getDiff(repoPath, path, oldRev, newRev);
                return r == null ? "" : (r.getDiffText() == null ? "" : r.getDiffText());
            }

            @Override
            public String readWorkingFile(String repoPath, String path) {
                java.io.File f = new java.io.File(repoPath, path);
                if (!f.exists() || !f.isFile()) {
                    return "";
                }
                try {
                    return new String(java.nio.file.Files.readAllBytes(f.toPath()), java.nio.charset.StandardCharsets.UTF_8);
                } catch (Exception e) {
                    return "(读取失败：" + e.getMessage() + ")";
                }
            }

            @Override
            public String readHeadFile(String repoPath, String path) {
                try {
                    com.gitgui.infrastructure.cli.CliGitExecutor executor = com.gitgui.GitGuiApp.getInjector()
                                                                                                .getInstance(
                                                                                                        com.gitgui.infrastructure.cli.CliGitExecutor.class);
                    return executor.readFileFromHead(repoPath, path);
                } catch (Exception e) {
                    return "(读取失败：" + e.getMessage() + ")";
                }
            }
        });
    }

    /**
     * 构造过滤按钮组：左侧两个 action 按钮（All / None），右侧 6 个 visibility 过滤。
     */
    private HBox makeFilterBar() {
        HBox bar = new HBox(4);
        bar.getChildren()
           .addAll(checkAllActionBtn, checkNoneActionBtn, filterUnversioned, filterVersioned, filterAdded, filterDeleted, filterModified,
                   filterFiles);
        filterGroup.selectedToggleProperty()
                   .addListener((obs, o, n) -> applyFilter());
        checkAllActionBtn.setOnAction(e -> setAllVisibleSelected(true));
        checkNoneActionBtn.setOnAction(e -> setAllVisibleSelected(false));
        return bar;
    }

    private void setAllVisibleSelected(boolean selected) {
        for (FileRow r : displayedRows) {
            if (r.isFile() && r.isCheckable()) {
                r.setSelected(selected);
            }
        }
        updateFileCount();
    }

    private Button makeActionButton(String i18nKey) {
        Button btn = new Button(I18nUtil.get(i18nKey));
        btn.setFocusTraversable(false);
        btn.getStyleClass()
           .add("filter-button");
        btn.getStyleClass()
           .add("filter-action");
        return btn;
    }

    private ToggleButton makeFilter(String i18nKey, String userData) {
        ToggleButton tb = new ToggleButton(I18nUtil.get(i18nKey));
        tb.setUserData(userData);
        tb.setToggleGroup(filterGroup);
        tb.setFocusTraversable(false);
        tb.getStyleClass()
          .add("filter-button");
        return tb;
    }

    // ================================================================
    //  数据加载
    // ================================================================

    private void loadBranchAndFiles() {
        AsyncUiLoader.submitRead(repoPath, TaskType.STATUS, () -> {
            String branch = "UNKNOWN";
            List<FileStatus> files = List.of();
            try {
                branch = statusService.getCurrentBranch(repoPath);
                files = statusService.getStatus(repoPath, DEFAULT_SHOW_UNVERSIONED, false);
            } catch (Exception e) {
                log.error("加载分支/文件失败", e);
            }
            String finalBranch = branch;
            List<FileStatus> finalFiles = files;
            Platform.runLater(() -> {
                branchLabel.setText(finalBranch);
                buildRows(finalFiles);
                applyFilter();
                updateFileCount();
            });
        });
    }

    private void buildRows(List<FileStatus> files) {
        allRows.clear();
        if (files == null) {
            updateFilterButtonStates();  // 空列表：禁用所有类别按钮
            return;
        }
        List<FileStatus> modified = new ArrayList<>();
        List<FileStatus> staged = new ArrayList<>();
        List<FileStatus> deleted = new ArrayList<>();
        List<FileStatus> untracked = new ArrayList<>();
        List<FileStatus> conflict = new ArrayList<>();
        List<FileStatus> others = new ArrayList<>();

        for (FileStatus f : files) {
            if (f.getState() == null || f.getState() == FileStatus.FileState.UNMODIFIED || f.getState() == FileStatus.FileState.IGNORED) {
                continue;
            }
            switch (f.getState()) {
                case MODIFIED -> modified.add(f);
                case STAGED -> staged.add(f);
                case DELETED -> deleted.add(f);
                case UNTRACKED -> untracked.add(f);
                case CONFLICT -> conflict.add(f);
                default -> others.add(f);
            }
        }
        Comparator<FileStatus> byPath = Comparator.comparing(FileStatus::getPath, String.CASE_INSENSITIVE_ORDER);
        modified.sort(byPath);
        staged.sort(byPath);
        deleted.sort(byPath);
        untracked.sort(byPath);
        conflict.sort(byPath);
        others.sort(byPath);

        addGroup(I18nUtil.get("commit.group.modified"), modified);
        addGroup(I18nUtil.get("commit.group.staged"), staged);
        addGroup(I18nUtil.get("commit.group.deleted"), deleted);
        addGroup(I18nUtil.get("commit.group.untracked"), untracked);
        addGroup(I18nUtil.get("commit.group.conflict"), conflict);
        if (!others.isEmpty()) {
            addGroup(I18nUtil.get("commit.group.notVersioned"), others);
        }
    }

    private void addGroup(String groupName, List<FileStatus> files) {
        if (files.isEmpty()) {
            return;
        }
        allRows.add(FileRow.group(groupName, files.size()));
        for (FileStatus f : files) {
            FileRow row = FileRow.file(f);
            row.selectedProperty()
               .addListener((obs, o, n) -> updateFileCount());
            allRows.add(row);
        }
        // 文件列表重建后，根据当前类别刷新 Check 栏 toggle 按钮的禁用状态
        updateFilterButtonStates();
    }

    /**
     * 根据当前 allRows 中实际存在的文件类别，启用/禁用 filter 按钮。
     * <p>当某类别（如 DELETED、STAGED）为 0 时，对应的 toggle 按钮置灰禁用；</p>
     * <p>避免用户点了没反应或误以为列表为空。FILES（"全部文件"）始终可用。</p>
     */
    private void updateFilterButtonStates() {
        boolean hasVersioned = false;
        boolean hasUnversioned = false;
        boolean hasAdded = false;
        boolean hasDeleted = false;
        boolean hasModified = false;
        for (FileRow r : allRows) {
            if (!r.isFile()) {
                continue;
            }
            FileStatus.FileState s = r.getState();
            if (s == null) {
                continue;
            }
            if (s == FileStatus.FileState.UNTRACKED) {
                hasUnversioned = true;
            } else {
                hasVersioned = true;
                switch (s) {
                    case STAGED -> hasAdded = true;
                    case DELETED -> hasDeleted = true;
                    case MODIFIED -> hasModified = true;
                    default -> { /* 其他类别（CONFLICT 等）不单独展示按钮 */ }
                }
            }
        }
        filterVersioned.setDisable(!hasVersioned);
        filterUnversioned.setDisable(!hasUnversioned);
        filterAdded.setDisable(!hasAdded);
        filterDeleted.setDisable(!hasDeleted);
        filterModified.setDisable(!hasModified);
        // filterFiles（"Files" 全部分类）始终可用，作为兜底过滤
    }

    /**
     * 应用 visibility 过滤规则（默认显示所有文件 + 受 {@link #DEFAULT_SHOW_UNVERSIONED} 控制）。
     */
    private void applyFilter() {
        Toggle selected = filterGroup.getSelectedToggle();
        String filter = selected == null ? null : (String) selected.getUserData();
        displayedRows.clear();
        int totalFileCount = 0;
        for (FileRow r : allRows) {
            if (r.isGroup()) {
                long matchInGroup = countFileMatchInGroup(r.getGroupName(), filter, DEFAULT_SHOW_UNVERSIONED);
                if (matchInGroup > 0) {
                    displayedRows.add(r);
                }
            } else {
                totalFileCount++;
                if (matchesFilter(r, filter, DEFAULT_SHOW_UNVERSIONED)) {
                    displayedRows.add(r);
                }
            }
        }
        fileCountTotal = totalFileCount;
        updateFileCount();
    }

    private void updateFileCount() {
        int selected = 0;
        for (FileRow r : displayedRows) {
            if (r.isFile() && r.isSelected()) {
                selected++;
            }
        }
        fileCountLabel.setText(MessageFormat.format(I18nUtil.get("commit.fileCount"), selected, fileCountTotal));
    }

    private long countFileMatchInGroup(String groupName, String filter, boolean showUnversioned) {
        return allRows.stream()
                      .filter(r -> r.isFile() && groupName.equals(r.getGroupName()))
                      .filter(r -> matchesFilter(r, filter, showUnversioned))
                      .count();
    }

    private boolean matchesFilter(FileRow r, String filter, boolean showUnversioned) {
        if (!r.isFile()) {
            return false;
        }
        FileStatus.FileState s = r.getState();
        if (!showUnversioned && s == FileStatus.FileState.UNTRACKED) {
            return false;
        }
        if (filter == null) {
            return true;
        }
        return switch (filter) {
            case "UNVERSIONED" -> s == FileStatus.FileState.UNTRACKED;
            case "VERSIONED" -> s != FileStatus.FileState.UNTRACKED;
            case "ADDED" -> s == FileStatus.FileState.STAGED;
            case "DELETED" -> s == FileStatus.FileState.DELETED;
            case "MODIFIED" -> s == FileStatus.FileState.MODIFIED;
            case "FILES" -> true;
            default -> true;
        };
    }

    // ================================================================
    //  Message 右键菜单（TortoiseGit 风格）
    // ================================================================

    private ContextMenu buildMessageContextMenu() {
        ContextMenu menu = new ContextMenu();

        MenuItem undo = new MenuItem(I18nUtil.get("edit.undo"));
        undo.setOnAction(e -> messageArea.undo());
        undo.setDisable(!messageArea.isUndoable());

        MenuItem redo = new MenuItem(I18nUtil.get("edit.redo"));
        redo.setOnAction(e -> messageArea.redo());
        redo.setDisable(!messageArea.isRedoable());

        MenuItem cut = new MenuItem(I18nUtil.get("edit.cut"));
        cut.setOnAction(e -> messageArea.cut());
        cut.setDisable(messageArea.getSelection()
                                  .getLength() == 0);

        MenuItem copy = new MenuItem(I18nUtil.get("edit.copy"));
        copy.setOnAction(e -> messageArea.copy());
        copy.setDisable(messageArea.getSelection()
                                   .getLength() == 0);

        MenuItem paste = new MenuItem(I18nUtil.get("edit.paste"));
        paste.setOnAction(e -> messageArea.paste());

        MenuItem delete = new MenuItem(I18nUtil.get("edit.delete"));
        delete.setOnAction(e -> {
            IndexRange sel = messageArea.getSelection();
            if (sel.getLength() > 0) {
                messageArea.deleteText(sel);
            }
        });
        delete.setDisable(messageArea.getSelection()
                                     .getLength() == 0);

        MenuItem selectAll = new MenuItem(I18nUtil.get("edit.selectAll"));
        selectAll.setOnAction(e -> messageArea.selectAll());
        selectAll.setDisable(messageArea.getLength() == 0);

        MenuItem pickHash = new MenuItem(I18nUtil.get("commit.contextMenu.pickCommitHash"));
        pickHash.setOnAction(e -> actionPickCommitHash());

        MenuItem pickMsg = new MenuItem(I18nUtil.get("commit.contextMenu.pickCommitMessage"));
        pickMsg.setOnAction(e -> actionPickCommitMessage());

        MenuItem pasteFilenames = new MenuItem(I18nUtil.get("commit.contextMenu.pasteFilenameList"));
        pasteFilenames.setOnAction(e -> actionPasteFilenameList());

        MenuItem pasteLastMsg = new MenuItem(I18nUtil.get("commit.contextMenu.pasteLastCommitMessage"));
        pasteLastMsg.setOnAction(e -> actionPasteLastCommitMessage());

        MenuItem pasteRecentMsg = new MenuItem(I18nUtil.get("commit.contextMenu.pasteRecentMessage"));
        pasteRecentMsg.setOnAction(e -> actionPasteRecentMessage());

        menu.setOnShown(e -> {
            boolean hasSel = messageArea.getSelection()
                                        .getLength() > 0;
            boolean hasText = messageArea.getLength() > 0;
            cut.setDisable(!hasSel);
            copy.setDisable(!hasSel);
            delete.setDisable(!hasSel);
            selectAll.setDisable(!hasText);
            undo.setDisable(!messageArea.isUndoable());
            redo.setDisable(!messageArea.isRedoable());
        });

        menu.getItems()
            .addAll(undo, redo, new SeparatorMenuItem(), cut, copy, paste, delete, new SeparatorMenuItem(), selectAll, new SeparatorMenuItem(),
                    pickHash, pickMsg, new SeparatorMenuItem(), pasteFilenames, pasteLastMsg, pasteRecentMsg);
        return menu;
    }

    private void insertAtCaret(String text) {
        if (text == null) {
            return;
        }
        IndexRange sel = messageArea.getSelection();
        int start = sel.getStart();
        int end = sel.getEnd();
        if (sel.getLength() > 0) {
            messageArea.replaceText(sel, text);
        } else {
            messageArea.insertText(messageArea.getCaretPosition(), text);
        }
        int newPos = start + text.length();
        messageArea.positionCaret(newPos);
    }

    private void insertAtCaretWithNewline(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        String current = messageArea.getText() == null ? "" : messageArea.getText();
        int caret = messageArea.getCaretPosition();
        String before = current.substring(0, caret);
        String after = current.substring(caret);
        String prefix = "";
        if (!before.isEmpty() && !before.endsWith("\n")) {
            prefix = "\n";
        }
        String suffix = "";
        if (!after.isEmpty() && !after.startsWith("\n")) {
            suffix = "\n";
        }
        String toInsert = prefix + text + suffix;
        messageArea.insertText(caret, toInsert);
        int newCaret = caret + toInsert.length() - (suffix.isEmpty() ? 0 : suffix.length());
        messageArea.positionCaret(newCaret);
    }

    private void actionPickCommitHash() {
        List<LogEntry> commits = loadRecentCommits(50);
        if (commits.isEmpty()) {
            showInfo(I18nUtil.get("commit.contextMenu.noRecentCommit"));
            return;
        }
        CommitPickerDialog dialog = new CommitPickerDialog(commits, null);
        dialog.showAndWait()
              .ifPresent(entry -> {
                  if (entry.getShortId() != null) {
                      insertAtCaretWithNewline(entry.getShortId());
                  }
              });
    }

    private void actionPickCommitMessage() {
        List<LogEntry> commits = loadRecentCommits(50);
        if (commits.isEmpty()) {
            showInfo(I18nUtil.get("commit.contextMenu.noRecentCommit"));
            return;
        }
        CommitPickerDialog dialog = new CommitPickerDialog(commits, null);
        dialog.showAndWait()
              .ifPresent(entry -> {
                  if (entry.getMessage() != null && !entry.getMessage()
                                                          .isEmpty()) {
                      insertAtCaretWithNewline(entry.getMessage());
                  }
              });
    }

    private void actionPasteFilenameList() {
        List<String> selectedFiles = new ArrayList<>();
        for (FileRow r : allRows) {
            if (r.isFile() && r.isSelected()) {
                selectedFiles.add(r.getPath());
            }
        }
        if (selectedFiles.isEmpty()) {
            showInfo(I18nUtil.get("commit.contextMenu.pasteFilenameEmpty"));
            return;
        }
        insertAtCaretWithNewline(String.join("\n", selectedFiles));
    }

    private void actionPasteLastCommitMessage() {
        List<LogEntry> commits = loadRecentCommits(1);
        if (commits.isEmpty()) {
            showInfo(I18nUtil.get("commit.contextMenu.noRecentCommit"));
            return;
        }
        LogEntry last = commits.get(0);
        if (last.getMessage() != null && !last.getMessage()
                                              .isEmpty()) {
            insertAtCaretWithNewline(last.getMessage());
        }
    }

    private void actionPasteRecentMessage() {
        List<LogEntry> commits = loadRecentCommits(50);
        if (commits.isEmpty()) {
            showInfo(I18nUtil.get("commit.contextMenu.noRecentCommit"));
            return;
        }
        CommitPickerDialog dialog = new CommitPickerDialog(commits, null);
        dialog.showAndWait()
              .ifPresent(entry -> {
                  if (entry.getMessage() != null && !entry.getMessage()
                                                          .isEmpty()) {
                      insertAtCaretWithNewline(entry.getMessage());
                  }
              });
    }

    private List<LogEntry> loadRecentCommits(int limit) {
        try {
            return statusService.getLog(repoPath, 1, limit);
        } catch (Exception e) {
            log.error("加载最近 commit 失败", e);
            return List.of();
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setHeaderText(null);
        alert.setTitle(I18nUtil.get("commit.title"));
        alert.showAndWait();
    }

    // ================================================================
    //  提交动作：像 fetch 一样在 ProgressDialog 中输出过程
    // ================================================================

    /**
     * 执行提交。
     * <p>步骤：</p>
     * <ol>
     *   <li>校验（BR-06 至少一个文件、BR-07 message 非空）</li>
     *   <li>点击按钮后立即关闭 CommitDialog，然后打开 ProgressDialog（与 fetch 同模式）</li>
     *   <li>通过 {@link GitOperationService#commit(CommitRequest, ProgressCallback)} 提交异步任务，
     *       把 git commit 的输出（进度、hook 输出）实时推到 ProgressDialog</li>
     *   <li>任务完成后弹「提交成功 / 失败」alert（不自动关闭，让用户看清结果）</li>
     * </ol>
     */
    private void doCommit(CommitAction action) {
        if (committing) {
            log.debug("commit 任务进行中，忽略重复点击");
            return;
        }
        List<String> selectedFiles = new ArrayList<>();
        for (FileRow r : allRows) {
            if (r.isFile() && r.isSelected()) {
                selectedFiles.add(r.getPath());
            }
        }
        if (selectedFiles.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, I18nUtil.get("commit.selectAtLeastOne")).showAndWait();
            return;
        }
        String message = messageArea.getText() == null ? "" : messageArea.getText()
                                                                         .trim();
        if (message.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, I18nUtil.get("commit.messageRequired")).showAndWait();
            return;
        }

        boolean pushAfter = action == CommitAction.COMMIT_AND_PUSH;

        // 选 Remote（Commit & Push 模式）
        String pickedRemote = null;
        if (pushAfter) {
            // 读取 Remote 列表（异常独立处理，不要和正常空列表合并到一处）
            List<com.gitgui.domain.model.RemoteConfig> remotes = List.of();
            try {
                if (remoteConfigService != null) {
                    remotes = remoteConfigService.list(repoPath);
                }
            } catch (Exception e) {
                // 读取异常：弹"无 Remote"对话框，让用户选择"打开管理器"或"取消"
                log.warn("读取 Remote 列表失败：{}", e.getMessage());
                if (!showNoRemoteDialogAndReturn()) {
                    return;
                }
                // 再次尝试拿 Remote
                try {
                    remotes = remoteConfigService == null ? List.of() : remoteConfigService.list(repoPath);
                    pickedRemote = pickBestRemote(remotes);
                } catch (Exception e2) {
                    log.warn("重新读取 Remote 列表仍失败：{}", e2.getMessage());
                    // 列表拿不到又没有 Remote → 退化为仅 commit
                    pushAfter = false;
                }
                // 若重新读到的 remotes 仍为空，pickedRemote 保持 null；外层会按 pushAfter=false 处理
            }
            // 正常路径（首次 list 没抛异常）
            if (pickedRemote == null && pushAfter) {
                if (remotes == null || remotes.isEmpty()) {
                    if (!showNoRemoteDialogAndReturn()) {
                        return;
                    }
                    try {
                        remotes = remoteConfigService.list(repoPath);
                        pickedRemote = pickBestRemote(remotes);
                    } catch (Exception e) {
                        log.warn("打开配置后再次读取 Remote 失败：{}", e.getMessage());
                        pushAfter = false;
                    }
                } else {
                    pickedRemote = pickBestRemote(remotes);
                }
            }
        }
        final String finalPickedRemote = pickedRemote;
        final boolean finalPushAfter = pushAfter;

        // 立即设置 committing 标记，禁用按钮（commit 进行中不允许反复点击）
        setCommittingState(true);

        // 先关闭 CommitDialog（释放 UI 焦点，让 ProgressDialog 弹出来抢焦点）
        Stage commitStage = (getDialogPane().getScene() == null) ? null : (Stage) getDialogPane().getScene()
                                                                                                 .getWindow();
        try {
            setResult(null);
        } catch (Exception ignored) {
        }
        try {
            close();
        } catch (Exception ex) {
            log.warn("close CommitDialog 失败：{}", ex.getMessage());
        }
        if (commitStage != null && commitStage.isShowing()) {
            commitStage.hide();
        }

        // 打开 ProgressDialog（同 PullDialog 优化：先创建 sharedCb，但延迟到下一帧再 show 窗口）
        Platform.runLater(() -> {
            try {
                openCommitProgressDialog(selectedFiles, message, finalPushAfter, finalPickedRemote);
            } catch (Exception ex) {
                log.error("打开 ProgressDialog 失败", ex);
                Platform.runLater(() -> {
                    if (commitStage != null) {
                        commitStage.show();
                    }
                });
            }
        });
    }

    /**
     * 打开 Commit 专用的 ProgressDialog，提交异步任务，并实时展示 git commit 输出。
     */
    private void openCommitProgressDialog(List<String> selectedFiles, String message, boolean pushAfter, String pickedRemote) {
        ProgressDialog progress = new ProgressDialog(
                /* owner */ null, pushAfter ? I18nUtil.get("commit.action.commitAndPush") : I18nUtil.get("commit.action.commit"),
                            I18nUtil.get("commit.title") + " - " + branchLabel.getText(),
                            com.gitgui.ui.dialog.ProgressDialog.OpKind.COMMIT   // 修复 hint 推断：commit 模式不再误判为 push
        );
        ProgressCallback sharedCb = progress.asCallback();

        CommitRequest req = CommitRequest.builder()
                                         .repoPath(repoPath)
                                         .stagedFiles(selectedFiles)
                                         .message(message)
                                         .pushAfterCommit(pushAfter)
                                         .pushWithTags(false)
                                         .build();

        TaskHandle handle;
        try {
            // 关键：commit 也走 async + cb 路径，把 git 输出实时推到 ProgressDialog，
            // 实现「像 fetch 一样显示过程」的体验
            handle = gitOperationService.commit(req, sharedCb);
        } catch (RedLineBlockedException rbe) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(I18nUtil.get("redline.blocked.title"));
            alert.setHeaderText(I18nUtil.get("redline.blocked.hitRule") + rbe.getRuleCode());
            alert.setContentText(rbe.getMessage());
            alert.showAndWait();
            Platform.runLater(progress::close);
            return;
        } catch (Exception e) {
            log.error("提交任务提交失败", e);
            new Alert(Alert.AlertType.ERROR, I18nUtil.get("commit.failed") + e.getMessage()).showAndWait();
            Platform.runLater(progress::close);
            return;
        }

        progress.attach(handle);

        // commit 完成后统一处理：弹"成功"提示（如有 SHA）/ 切 push / 复位提交状态。
        // 修复：原来这里的 onSuccess 注册了两次（一次弹 success 提示，一次切 push 流程），
        // 造成逻辑分散且 onFailure 回调也重复注册；现合并为单个 onSuccess/onFailure。
        handle.onSuccess(r -> Platform.runLater(() -> {
            String shortCid;
            if (r instanceof String s && !s.isBlank()) {
                // 服务端带回了 commitId（前 8 位足够辨识）
                shortCid = s.length() > 8 ? s.substring(0, 8) : s;
            } else if (r != null) {
                shortCid = r.toString();
            } else {
                shortCid = req.getStagedFiles()
                              .size() + " files committed";
            }
            new Alert(Alert.AlertType.INFORMATION, I18nUtil.get("commit.success") + shortCid).showAndWait();
            // 如果勾选了提交并推送，串接到 push 流程
            if (pushAfter && pickedRemote != null) {
                doPushAfterCommit(handle, progress, pickedRemote);
            } else {
                // 不推送：仅 commit，让用户从 ProgressDialog 中点关闭
                setCommittingState(false);
            }
        }));
        handle.onFailure(e -> {
            log.error("commit 任务失败", e);
            final String msg = e == null ? "" : e.getMessage();
            Platform.runLater(() -> {
                new Alert(Alert.AlertType.WARNING, I18nUtil.get("commit.failed") + msg).showAndWait();
                setCommittingState(false);
            });
        });

        progress.showAndWaitForTask();
    }

    /**
     * Commit & Push 模式下的 push 切换：
     * <p>commit 成功后，构造 PushRequest 并打开 push 用的 ProgressDialog，</p>
     * <p>复用现有的 {@code ProgressDialog} + {@code ProgressCallback} 流程。</p>
     */
    private void doPushAfterCommit(TaskHandle commitHandle, ProgressDialog parentProgress, String pickedRemote) {
        try {
            // 关闭上一个 commit 的 ProgressDialog，开启 push 用的
            parentProgress.close();

            // 拿到 commitId：如果可能的话从 commitHandle 读取
            // 这里没有 commitHandle 的 commitId getter，做一个简单 fallback：直接拿当前分支 SHA
            String committedCid;
            try {
                com.gitgui.infrastructure.cli.CliGitExecutor exec = com.gitgui.GitGuiApp.getInjector()
                                                                                        .getInstance(
                                                                                                com.gitgui.infrastructure.cli.CliGitExecutor.class);
                committedCid = exec.getCurrentBranch(repoPath);
            } catch (Exception ex) {
                committedCid = "HEAD";
            }

            String branchTmp;
            try {
                com.gitgui.infrastructure.cli.CliGitExecutor exec = com.gitgui.GitGuiApp.getInjector()
                                                                                        .getInstance(
                                                                                                com.gitgui.infrastructure.cli.CliGitExecutor.class);
                branchTmp = exec.getCurrentBranch(repoPath);
            } catch (Exception ex) {
                branchTmp = null;
            }
            final String currentBranch = branchTmp != null ? branchTmp : "current";

            com.gitgui.domain.model.request.PushRequest pushReq = com.gitgui.domain.model.request.PushRequest.builder()
                                                                                                             .repoPath(repoPath)
                                                                                                             .remote(pickedRemote)
                                                                                                             .force(false)
                                                                                                             .forceWithLease(false)
                                                                                                             .pushAllBranches(false)
                                                                                                             .pushAllTags(false)
                                                                                                             .includeTags(false)
                                                                                                             .build();

            ProgressDialog pushProgress = new ProgressDialog(null, I18nUtil.get("push.title") + "  →  " + pickedRemote + "/" + currentBranch,
                                                             I18nUtil.get("commit.pushHeaderPrefix") + " " + committedCid.substring(0, Math.min(8,
                                                                                                                                                committedCid.length())),
                                                             com.gitgui.ui.dialog.ProgressDialog.OpKind.PUSH);
            ProgressCallback sharedCb = pushProgress.asCallback();

            try {
                TaskHandle pushHandle = gitOperationService.push(pushReq, sharedCb);
                if (pushHandle == null) {
                    pushProgress.close();
                    return;
                }
                pushProgress.attach(pushHandle);
                pushHandle.onSuccess(p -> Platform.runLater(() -> {
                    setCommittingState(false);
                }));
                pushHandle.onFailure(pushEx -> {
                    log.warn("Push 异常", pushEx);
                    final String pushMsg = formatExceptionMessage(pushEx);
                    Platform.runLater(() -> {
                        new Alert(Alert.AlertType.WARNING, I18nUtil.get("commit.pushFailed") + pushMsg).showAndWait();
                    });
                });
                pushProgress.showAndWaitForTask();
            } catch (RedLineBlockedException rbe) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(I18nUtil.get("redline.blocked.title"));
                alert.setHeaderText(I18nUtil.get("redline.blocked.hitRule") + rbe.getRuleCode());
                alert.setContentText(rbe.getMessage());
                alert.showAndWait();
                Platform.runLater(pushProgress::close);
                setCommittingState(false);
            }
        } finally {
            setCommittingState(false);
        }
    }

    /**
     * 设置 commit 进行中的 UI 状态。
     */
    private void setCommittingState(boolean committing) {
        this.committing = committing;
        if (committing) {
            commitBtn.setText(I18nUtil.get("commit.committing"));
            commitBtn.setDisable(true);
            commitAndPushBtn.setText(I18nUtil.get("commit.committing"));
            commitAndPushBtn.setDisable(true);
            if (cancelButtonRef != null) {
                cancelButtonRef.setDisable(true);
            }
        } else {
            commitBtn.setText(I18nUtil.get("commit.action.commit"));
            commitBtn.setDisable(false);
            commitAndPushBtn.setText(I18nUtil.get("commit.action.commitAndPush"));
            commitAndPushBtn.setDisable(false);
            if (cancelButtonRef != null) {
                cancelButtonRef.setDisable(false);
            }
        }
    }

    private String pickBestRemote(List<com.gitgui.domain.model.RemoteConfig> remotes) {
        if (remotes == null || remotes.isEmpty()) {
            return null;
        }
        for (com.gitgui.domain.model.RemoteConfig rc : remotes) {
            if ("origin".equalsIgnoreCase(rc.getName())) {
                return rc.getName();
            }
        }
        return remotes.get(0)
                      .getName();
    }

    /**
     * 「当前仓库没有配置 Remote」的提示对话框。
     * <p>用户选项：</p>
     * <ul>
     *   <li>「取消」→ 直接返回 false，调用方中断 commit & push 流程</li>
     *   <li>「去配置 Remote」→ 弹出 Remote 管理对话框让用户配置；用户关闭后再次读取 Remote，
     *       若此时存在 Remote 则返回 true（让 commit & push 继续），否则返回 false</li>
     * </ul>
     * <p><b>修复：</b>原实现两个分支都返回 false，导致"配 Remote 后无法继续 commit & push"。</p>
     *
     * @return true=用户已配置好 Remote 可继续；false=用户取消或仍未配置
     */
    private boolean showNoRemoteDialogAndReturn() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(I18nUtil.get("commit.noRemote.title"));
        alert.setHeaderText(I18nUtil.get("commit.noRemote.title"));
        alert.setContentText(I18nUtil.get("commit.noRemote.content"));
        ButtonType cancelBtn = new ButtonType(I18nUtil.get("commit.noRemote.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType openManagerBtn = new ButtonType(I18nUtil.get("commit.noRemote.openManager"), ButtonBar.ButtonData.APPLY);
        alert.getButtonTypes()
             .setAll(cancelBtn, openManagerBtn);
        java.util.Optional<ButtonType> result = alert.showAndWait();
        // 取消分支：明确返回 false
        if (result.isEmpty() || result.get() == cancelBtn) {
            return false;
        }
        // 用户选"去配置 Remote" → 打开管理器，关闭后重新读取 Remote
        try {
            com.gitgui.ui.dialog.RemoteConfigDialog remoteDlg = new com.gitgui.ui.dialog.RemoteConfigDialog(remoteConfigService, repoPath);
            remoteDlg.showAndWait();
        } catch (Exception e) {
            log.error("打开 Remote 配置对话框失败", e);
            return false;
        }
        // 重新探查 Remote：用户可能已经在管理器里加了
        try {
            if (remoteConfigService != null) {
                List<com.gitgui.domain.model.RemoteConfig> remotes = remoteConfigService.list(repoPath);
                if (remotes != null && !remotes.isEmpty()) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("重新读取 Remote 列表失败：{}", e.getMessage());
        }
        return false;
    }

    private String formatExceptionMessage(Throwable e) {
        if (e == null) {
            return "未知错误";
        }
        Throwable cur = e;
        for (int i = 0; i < 5 && cur != null; i++) {
            String msg = cur.getMessage();
            if (msg != null && !msg.isBlank()) {
                return msg;
            }
            if (cur.getCause() == null || cur.getCause() == cur) {
                break;
            }
            cur = cur.getCause();
        }
        return e.getClass()
                .getName();
    }

    // ================================================================
    //  数据模型
    // ================================================================

    // ====== 提交动作类型 ======
    private enum CommitAction {
        COMMIT,
        COMMIT_AND_PUSH
    }

    /**
     * 表格行模型：分组标题行（{@code isFile=false}）+ 文件行（{@code isFile=true}）。
     */
    public static class FileRow {

        private final boolean group;
        private final String groupName;
        private final int groupSize;
        // file 字段
        private final String path;
        private final FileStatus.FileState state;
        private final String extension;
        private final int addedLines;
        private final int deletedLines;
        private final BooleanProperty selected;
        private final javafx.beans.property.StringProperty pathPropertyObj;
        private final javafx.beans.property.StringProperty extensionPropertyObj;
        private final javafx.beans.property.StringProperty statusPropertyObj;
        private final javafx.beans.property.IntegerProperty addedLinesPropertyObj;
        private final javafx.beans.property.IntegerProperty deletedLinesPropertyObj;

        private FileRow(boolean group, String groupName, int groupSize, String path, FileStatus.FileState state, String extension, int addedLines,
                int deletedLines, boolean selected) {
            this.group = group;
            this.groupName = groupName;
            this.groupSize = groupSize;
            this.path = path;
            this.state = state;
            this.extension = extension;
            this.addedLines = addedLines;
            this.deletedLines = deletedLines;
            this.selected = new SimpleBooleanProperty(selected);
            this.pathPropertyObj = new javafx.beans.property.SimpleStringProperty(path == null ? "" : path);
            this.extensionPropertyObj = new javafx.beans.property.SimpleStringProperty(extension == null ? "" : extension);
            this.statusPropertyObj = new javafx.beans.property.SimpleStringProperty(formatState(state));
            this.addedLinesPropertyObj = new SimpleIntegerProperty(addedLines);
            this.deletedLinesPropertyObj = new SimpleIntegerProperty(deletedLines);
        }

        public static FileRow group(String groupName, int size) {
            return new FileRow(true, groupName, size, null, null, null, 0, 0, false);
        }

        public static FileRow file(FileStatus f) {
            String ext = "";
            if (f.getPath() != null) {
                int dot = f.getPath()
                           .lastIndexOf('.');
                if (dot > 0 && dot < f.getPath()
                                      .length() - 1) {
                    ext = f.getPath()
                           .substring(dot + 1);
                }
            }
            return new FileRow(false, null, 0, f.getPath(), f.getState(), ext, f.getAddedLines(), f.getDeletedLines(), true);
        }

        private static String formatState(FileStatus.FileState s) {
            if (s == null) {
                return "";
            }
            return switch (s) {
                case MODIFIED -> "Modified";
                case UNTRACKED -> "Unknown";
                case DELETED -> "Deleted";
                case STAGED -> "Added";
                case CONFLICT -> "Conflict";
                case IGNORED -> "Ignored";
                case UNMODIFIED -> "Unmodified";
            };
        }

        public boolean isGroup() {
            return group;
        }

        public boolean isFile() {
            return !group;
        }

        public String getGroupName() {
            return groupName;
        }

        public int getGroupSize() {
            return groupSize;
        }

        public String getPath() {
            return path;
        }

        public FileStatus.FileState getState() {
            return state;
        }

        public boolean isSelected() {
            return selected.get();
        }

        public void setSelected(boolean v) {
            selected.set(v);
        }

        public BooleanProperty selectedProperty() {
            return selected;
        }

        public boolean isCheckable() {
            return isFile();
        }

        public javafx.beans.property.StringProperty pathProperty() {
            return pathPropertyObj;
        }

        public javafx.beans.property.StringProperty extensionProperty() {
            return extensionPropertyObj;
        }

        public javafx.beans.property.StringProperty statusProperty() {
            return statusPropertyObj;
        }

        public IntegerProperty addedLinesProperty() {
            return addedLinesPropertyObj;
        }

        public IntegerProperty deletedLinesProperty() {
            return deletedLinesPropertyObj;
        }
    }

    /**
     * Path 列 Cell：状态指示器 + path 文本
     */
    private static class PathCell extends TableCell<FileRow, String> {

        private final Label checkIndicator = new Label();
        private final HBox graphic = new HBox(6, checkIndicator, new Label());
        private FileRow boundRow;

        PathCell() {
            checkIndicator.setStyle("-fx-font-size: 14px; -fx-min-width: 16px; -fx-alignment: center;");
            graphic.setAlignment(Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            unbindPrevious();
            if (empty) {
                setGraphic(null);
                setText(null);
                boundRow = null;
                return;
            }
            FileRow row = getTableRow() == null ? null : (FileRow) getTableRow().getItem();
            if (row == null) {
                setGraphic(null);
                setText(item);
                return;
            }
            if (row.isGroup()) {
                setGraphic(null);
                setText(row.getGroupName() + " (" + row.getGroupSize() + ")");
                setStyle("-fx-font-weight: bold; -fx-text-fill: #1976d2;");
            } else {
                boundRow = row;
                checkIndicator.setText(row.isSelected() ? "\u2611" : "\u2610");
                checkIndicator.setStyle(
                        row.isSelected() ? "-fx-text-fill: #1976d2; -fx-font-size: 14px;" : "-fx-text-fill: #bdbdbd; -fx-font-size: 14px;");
                ((Label) graphic.getChildren()
                                .get(1)).setText(item);
                ((Label) graphic.getChildren()
                                .get(1)).setStyle(stateStyle(row.getState()));
                row.selectedProperty()
                   .addListener(this::onRowSelectedChanged);
                setGraphic(graphic);
                setText(null);
                setStyle(stateStyle(row.getState()));
            }
        }

        private void onRowSelectedChanged(javafx.beans.value.ObservableValue<? extends Boolean> obs, Boolean o, Boolean n) {
            if (n == null) {
                return;
            }
            checkIndicator.setText(n ? "\u2611" : "\u2610");
            checkIndicator.setStyle(n ? "-fx-text-fill: #1976d2; -fx-font-size: 14px;" : "-fx-text-fill: #bdbdbd; -fx-font-size: 14px;");
        }

        private void unbindPrevious() {
            if (boundRow != null) {
                boundRow.selectedProperty()
                        .removeListener(this::onRowSelectedChanged);
            }
        }
    }

    private static class FileRowTableRow extends TableRow<FileRow> {

        @Override
        protected void updateItem(FileRow item, boolean empty) {
            super.updateItem(item, empty);
            getStyleClass().removeAll("group-row", "file-row");
            if (empty || item == null) {
                setStyle("");
                return;
            }
            if (item.isGroup()) {
                getStyleClass().add("group-row");
                setStyle("");
            } else {
                getStyleClass().add("file-row");
                setStyle("");
            }
        }
    }

    private static final class MessageFormat {

        static String format(String pattern, Object... args) {
            return java.text.MessageFormat.format(pattern, args);
        }
    }
}
