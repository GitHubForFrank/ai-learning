package com.gitgui.ui.dialog;

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
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 提交对话框（TortoiseGit 风格布局）
 * <p>对应 PRD 4.4，参照 TortoiseGit "Commit" 对话框设计：</p>
 * <pre>
 * ┌─ &lt;repoPath&gt; - Commit ─────────────────────────────────────┐
 * │ Commit to: master                       ☐ new branch        │
 * │ Message:                                                     │
 * │  [  multi-line message + signed-off-by line     ]            │
 * │                                                4/1            │
 * │                                                              │
 * │ ☐ Amend Last Commit                                          │
 * │ ☑ Set author date  [2026/7/25]  [14:34:19]                   │
 * │ ☑ Set author       [Frank <Frank-ZQ.Kang@iaia.com>] [+Add]   │
 * │                                                              │
 * │ Changes made (double-click on file for diff):                │
 * │  ☐ Check   All None Unversioned Versioned ...                │
 * │  ┌────────────────────────────────────────────────────────┐  │
 * │  │ Modified Files                                          │  │
 * │  │ ☑ CHANGELOG.md  md  Modified   1   0                   │  │
 * │  │ Not Versioned Files                                     │  │
 * │  │ ☑ 123.txt        .txt Unknown                           │  │
 * │  └────────────────────────────────────────────────────────┘  │
 * │ ☑ Show Unversioned Files                                     │
 * │ ☐ Do not autoselect submodules                               │
 * │ ☐ Show Whole Project                                         │
 * │ ☐ Message only                       2 files, ...   View     │
 * │                                                              │
 * │              [Commit & Push ▼]   [Cancel]   [Help]           │
 * └──────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p>遵循 BR-06（至少勾选一个文件）、BR-07（message 非空，Amend 复用上次 message 可为空）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class CommitDialog extends Dialog<Void> {

    private static final Logger log = LoggerFactory.getLogger(CommitDialog.class);

    /** 消息最大长度（用于 4/1 字符计数显示，TortoiseGit 默认 4 行展示） */
    private static final int MESSAGE_VISIBLE_ROWS = 4;
    /** 时间字段格式 */
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final GitOperationService gitOperationService;
    private final StatusService statusService;
    /** Remote 配置服务（用于「Commit & Push」时自动选 Remote） */
    private final com.gitgui.domain.service.RemoteConfigService remoteConfigService;
    private final String repoPath;

    // ====== 顶部：Commit to / new branch ======
    private final Label branchLabel = new Label();

    // ====== 消息输入 ======
    private final TextArea messageArea = new TextArea();
    private final Label charCounter = new Label();

    // ====== 选项 ======
    private final CheckBox amendCheck = new CheckBox(I18nUtil.get("commit.amend"));
    private final CheckBox setAuthorDateCheck = new CheckBox(I18nUtil.get("commit.setAuthorDate"));
    private final DatePicker authorDatePicker = new DatePicker(LocalDate.now());
    private final TextField authorTimeField = new TextField(LocalTime.now().withNano(0).format(TIME_FMT));
    private final CheckBox setAuthorCheck = new CheckBox(I18nUtil.get("commit.setAuthor"));
    private final TextField authorField = new TextField();
    private final Button addSignedOffByBtn = new Button(I18nUtil.get("commit.addSignedOffBy"));
    private final CheckBox signCheck = new CheckBox(I18nUtil.get("commit.signCommit"));

    // ====== 变更文件表格 ======
    /**
     * 「Check」列标题：纯文本标签（TortoiseGit 风格，列名就是 "Check"，无 checkbox）。
     * <p>文件行不显示 checkbox，而是通过行点击 / All / None 来控制勾选状态。</p>
     */
    private final Label checkAllCheck = new Label(I18nUtil.get("commit.check"));
    private final TableView<FileRow> fileTable = new TableView<>();
    private final ObservableList<FileRow> allRows = FXCollections.observableArrayList();
    /** 经过滤（按 Check / 状态过滤 / 显示未跟踪等）后真正展示的行 */
    private final ObservableList<FileRow> displayedRows = FXCollections.observableArrayList();

    // ====== 过滤按钮：All / None 是 action 按钮（控制可见文件勾选状态），其他是 visibility 过滤 ======
    private final ToggleGroup filterGroup = new ToggleGroup();
    /** action 按钮：点击后**全勾**当前可见文件（不影响 visibility） */
    private final Button checkAllActionBtn = makeActionButton("commit.filter.all");
    /** action 按钮：点击后**全不勾**当前可见文件（不影响 visibility） */
    private final Button checkNoneActionBtn = makeActionButton("commit.filter.none");
    /** visibility 过滤：仅显示 Unversioned 文件（未跟踪） */
    private final ToggleButton filterUnversioned = makeFilter("commit.filter.unversioned", "UNVERSIONED");
    /** visibility 过滤：仅显示已纳入版本控制的文件（排除 UNTRACKED） */
    private final ToggleButton filterVersioned = makeFilter("commit.filter.versioned", "VERSIONED");
    /** visibility 过滤：仅显示 Added（已暂存的新增） */
    private final ToggleButton filterAdded = makeFilter("commit.filter.added", "ADDED");
    /** visibility 过滤：仅显示 Deleted */
    private final ToggleButton filterDeleted = makeFilter("commit.filter.deleted", "DELETED");
    /** visibility 过滤：仅显示 Modified */
    private final ToggleButton filterModified = makeFilter("commit.filter.modified", "MODIFIED");
    /** visibility 过滤：仅显示普通文件（排除 submodule） */
    private final ToggleButton filterFiles = makeFilter("commit.filter.files", "FILES");

    // 底部选项
    private final CheckBox showUnversionedCheck = new CheckBox(I18nUtil.get("commit.showUnversioned"));
    // 默认勾选：与 TortoiseGit 一致，让 Untracked 文件默认可见
    {
        showUnversionedCheck.setSelected(true);
    }
    private final CheckBox noAutoselectSubmodulesCheck = new CheckBox(I18nUtil.get("commit.noAutoselectSubmodules"));
    private final CheckBox showWholeProjectCheck = new CheckBox(I18nUtil.get("commit.showWholeProject"));
    private final CheckBox messageOnlyCheck = new CheckBox(I18nUtil.get("commit.messageOnly"));

    private final Label fileCountLabel = new Label();
    private final Hyperlink viewPatchLink = new Hyperlink(I18nUtil.get("commit.viewPatch"));
    private int fileCountTotal = 0;

    // ====== 底部按钮 ======
    private final MenuButton commitMenuBtn = new MenuButton(I18nUtil.get("commit.action.commitAndPushMenu"));
    /** Cancel 按钮（commit 进行中禁用，避免中途取消） */
    private Button cancelButtonRef;
    /** Help 按钮（commit 进行中禁用） */
    private Button helpButtonRef;
    /** commit 进行中标记 */
    private boolean committing = false;

    // ====== TortoiseGit: 提交动作类型 ======
    private enum CommitAction { COMMIT, RE_COMMIT, COMMIT_AND_PUSH, COMMIT_AND_PUSH_TAGS }

    /**
     * 构造提交对话框。
     *
     * @param gitOperationService Git 操作服务
     * @param statusService       状态服务
     * @param repoPath            仓库路径
     */
    public CommitDialog(GitOperationService gitOperationService, StatusService statusService,
                        com.gitgui.domain.service.RemoteConfigService remoteConfigService, String repoPath) {
        this.gitOperationService = gitOperationService;
        this.statusService = statusService;
        this.remoteConfigService = remoteConfigService;
        this.repoPath = repoPath;
        setTitle(repoPath + " - " + I18nUtil.get("commit.title"));
        setHeaderText(null);
        initModality(Modality.APPLICATION_MODAL);

        DialogPane pane = getDialogPane();
        pane.setContent(buildContent());
        pane.setPrefSize(820, 700);

        // 关键修复：底部三个按钮（Commit & Push ▼ / Cancel / Help）统一作为内容的一部分，
        // 不再依赖 DialogPane 内部的 buttonBar 节点（JavaFX 21 的 buttonBar 类型是 ButtonBar，
        // 不是 Pane，原代码用 instanceof Pane 判断失败导致按钮不显示）。
        // 这里不调用 pane.getButtonTypes().addAll(...)，所有按钮都在 buildContent() 的 buildBottomActionBar() 中。

        setResultConverter(buttonType -> null);

        // 关键修复：由于去掉了 ButtonTypes，Dialog 自带的关闭机制（点 X / 按 Esc）会失效。
        // 必须手动绑定 Stage 的 onCloseRequest，否则用户点窗口 X 或 Cancel 按钮都无法关闭。
        setOnShowing(e -> {
            javafx.stage.Window win = getDialogPane().getScene().getWindow();
            if (win != null) {
                win.setOnCloseRequest(ev -> {
                    // commit 进行中时不允许点 X 关闭
                    if (committing) {
                        ev.consume();
                    } else {
                        setResult(null);
                    }
                });
            }
        });

        // 加载当前分支 + 变更文件
        loadBranchAndFiles();
    }

    // ================================================================
    //  整体布局
    // ================================================================

    private VBox buildContent() {
        VBox root = new VBox(8);
        root.setPadding(new Insets(10));

        VBox changesBox = buildChangesBox();
        root.getChildren().addAll(
                buildCommitToBox(),
                buildMessageBox(),
                buildOptionsBox(),
                changesBox,
                buildBottomOptionsBox(),
                buildBottomActionBar()  // 底部按钮行（Commit & Push ▼ / Cancel / Help）
        );
        VBox.setVgrow(changesBox, Priority.ALWAYS); // changes 区占满剩余空间
        // 「Message only」勾选时隐藏变更区
        messageOnlyCheck.selectedProperty().addListener((obs, o, n) -> {
            changesBox.setVisible(!n);
            changesBox.setManaged(!n);
            // 同时隐藏过滤行（changesBox 第一个子节点是 header+filter）
            for (Node child : changesBox.getChildren()) {
                child.setVisible(!n);
                child.setManaged(!n);
            }
            // 隐藏底部选项中的 Show Unversioned/Do not autoselect/Show Whole Project
            // （仅保留 messageOnly 自身 + 计数 + View Patch）
            showUnversionedCheck.setVisible(!n);
            showUnversionedCheck.setManaged(!n);
            noAutoselectSubmodulesCheck.setVisible(!n);
            noAutoselectSubmodulesCheck.setManaged(!n);
            showWholeProjectCheck.setVisible(!n);
            showWholeProjectCheck.setManaged(!n);
        });
        return root;
    }

    /**
     * 底部按钮行：Commit & Push ▼ / Cancel / Help，统一右对齐。
     * <p>不依赖 DialogPane 内部 buttonBar 节点类型，</p>
     * <p>避免 JavaFX 21 ButtonBar 不是 Pane 导致 instanceof 判断失败的问题。</p>
     */
    private HBox buildBottomActionBar() {
        // 构建 Commit & Push ▼ 菜单：3 项对齐 TortoiseGit（Commit / ReCommit / Commit & Push），
        // 不放分隔符，主按钮仍用蓝色保持 commit 操作的视觉权重。
        commitMenuBtn.getItems().clear();
        commitMenuBtn.getItems().addAll(
                makeActionMenuItem(CommitAction.COMMIT, I18nUtil.get("commit.action.commit")),
                makeActionMenuItem(CommitAction.RE_COMMIT, I18nUtil.get("commit.action.reCommit")),
                makeActionMenuItem(CommitAction.COMMIT_AND_PUSH, I18nUtil.get("commit.action.commitAndPush"))
        );
        commitMenuBtn.setStyle("-fx-base: #1976d2; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        commitMenuBtn.setPrefWidth(180);

        // Cancel 按钮
        Button cancelButton = new Button(I18nUtil.get("button.cancel"));
        cancelButton.setCancelButton(true);
        // 多层关闭保障：setResult + close + window.hide 三道防线
        cancelButton.setOnAction(e -> {
            if (committing) return;
            setResult(null);
            close();
        });
        cancelButton.setPrefWidth(90);
        this.cancelButtonRef = cancelButton;

        // Help 按钮
        Button helpButton = new Button(I18nUtil.get("button.help"));
        helpButton.setOnAction(e -> showHelp());
        helpButton.setPrefWidth(90);
        this.helpButtonRef = helpButton;

        Region spacer = new Region();
        HBox bar = new HBox(10, spacer, commitMenuBtn, cancelButton, helpButton);
        bar.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        // 顶部加一条分割线，跟上面的内容区分
        bar.setPadding(new Insets(8, 0, 0, 0));
        return bar;
    }

    /**
     * 顶部「Commit to: master  ☐ new branch」一行。
     */
    private HBox buildCommitToBox() {
        Label toLabel = new Label(I18nUtil.get("commit.to"));
        toLabel.setMinWidth(70);
        branchLabel.setText("...");
        branchLabel.setStyle("-fx-font-weight: bold;");

        CheckBox newBranchCheck = new CheckBox(I18nUtil.get("commit.newBranch"));
        TextField newBranchNameField = new TextField();
        newBranchNameField.setPromptText(I18nUtil.get("commit.newBranchName"));
        newBranchNameField.setDisable(true);
        newBranchNameField.setPrefWidth(220);
        // TortoiseGit：「new branch」勾选时允许输入新分支名
        newBranchCheck.selectedProperty().addListener((obs, o, n) -> {
            newBranchNameField.setDisable(!n);
            if (n) {
                Platform.runLater(newBranchNameField::requestFocus);
            }
        });
        newBranchNameField.setUserData(newBranchCheck);
        // 暂存到成员方便 doCommit 读取
        this.newBranchCheck = newBranchCheck;
        this.newBranchNameField = newBranchNameField;

        HBox box = new HBox(10, toLabel, branchLabel, spacer(), newBranchCheck, newBranchNameField);
        HBox.setHgrow(box.getChildren().get(2), Priority.ALWAYS);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private CheckBox newBranchCheck;
    private TextField newBranchNameField;

    /**
     * Message 输入区：Label + 多行 TextArea + 字符计数「4/1」。
     */
    private VBox buildMessageBox() {
        Label msgLabel = new Label(I18nUtil.get("commit.message"));
        messageArea.setPromptText(I18nUtil.get("commit.messageHelp"));
        messageArea.setWrapText(true);
        messageArea.setPrefRowCount(MESSAGE_VISIBLE_ROWS);
        VBox.setVgrow(messageArea, Priority.NEVER);
        // TortoiseGit 风格右键菜单（替换默认 JavaFX 菜单）
        messageArea.setContextMenu(buildMessageContextMenu());
        // 字符计数：第一行为标题（最多 60 字符），其余行累计 / 70 行
        updateCharCounter();
        messageArea.textProperty().addListener((obs, o, n) -> updateCharCounter());

        HBox counterRow = new HBox(spacer(), charCounter);
        HBox.setHgrow(counterRow.getChildren().get(0), Priority.ALWAYS);
        counterRow.setAlignment(Pos.CENTER_RIGHT);

        VBox box = new VBox(4, msgLabel, messageArea, counterRow);
        return box;
    }

    private void updateCharCounter() {
        String text = messageArea.getText() == null ? "" : messageArea.getText();
        // 第一行长度
        int firstLineLen = text.isEmpty() ? 0 : (text.indexOf('\n') < 0 ? text.length() : text.indexOf('\n'));
        // 累计总字符数
        int total = text.length();
        // 限制：第一行 60 字符，总字符数任意
        charCounter.setText(MessageFormat.format(I18nUtil.get("commit.charCounter"), firstLineLen, Math.max(1, total == 0 ? 1 : total)));
    }

    /**
     * 选项区：Amend + Set author date + Set author + Sign commit（按行排列）。
     */
    private VBox buildOptionsBox() {
        // Row 1: Amend Last Commit
        // Row 2: Set author date [date] [time]
        // Row 3: Set author [text] [+ Add Signed-off-by]
        // Row 4: Sign commit (GPG)

        HBox row1 = new HBox(10, amendCheck);
        row1.setAlignment(Pos.CENTER_LEFT);

        authorDatePicker.setDisable(true);
        authorTimeField.setDisable(true);
        authorTimeField.setPrefWidth(100);
        authorTimeField.setPromptText("HH:mm:ss");
        setAuthorDateCheck.selectedProperty().addListener((obs, o, n) -> {
            authorDatePicker.setDisable(!n);
            authorTimeField.setDisable(!n);
        });
        HBox row2 = new HBox(8, setAuthorDateCheck, authorDatePicker, authorTimeField);
        row2.setAlignment(Pos.CENTER_LEFT);

        authorField.setPromptText("Name <email@example.com>");
        authorField.setPrefWidth(280);
        authorField.setDisable(true);
        setAuthorCheck.selectedProperty().addListener((obs, o, n) -> authorField.setDisable(!n));
        // 默认填入当前 git 配置的作者（异步加载完成后回填）
        HBox row3 = new HBox(8, setAuthorCheck, authorField, addSignedOffByBtn);
        row3.setAlignment(Pos.CENTER_LEFT);
        addSignedOffByBtn.setOnAction(e -> insertSignedOffBy());

        HBox row4 = new HBox(10, signCheck);
        row4.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(4, row1, row2, row3, row4);
        return box;
    }

    /**
     * 变更文件区：表头 + 过滤按钮 + TableView（按状态分组）。
     */
    private VBox buildChangesBox() {
        Label header = new Label(I18nUtil.get("commit.changesMade"));
        header.setStyle("-fx-font-weight: bold;");

        // 「Check」是列标题（纯文本，TortoiseGit 风格），不再作为 master checkbox。
        // 全选 / 全不选由 All / None 按钮（action）控制。
        HBox checkRow = new HBox(10, checkAllCheck, makeFilterBar());
        checkRow.setAlignment(Pos.CENTER_LEFT);

        // TableView 列：Path / Extension / Status / + / -
        TableColumn<FileRow, String> pathCol = new TableColumn<>(I18nUtil.get("commit.col.path"));
        pathCol.setCellValueFactory(d -> d.getValue().pathProperty());
        pathCol.setPrefWidth(380);
        pathCol.setCellFactory(col -> new PathCell());

        TableColumn<FileRow, String> extCol = new TableColumn<>(I18nUtil.get("commit.col.extension"));
        extCol.setCellValueFactory(d -> d.getValue().extensionProperty());
        extCol.setPrefWidth(80);

        TableColumn<FileRow, String> statusCol = new TableColumn<>(I18nUtil.get("commit.col.status"));
        statusCol.setCellValueFactory(d -> d.getValue().statusProperty());
        statusCol.setPrefWidth(110);

        TableColumn<FileRow, Number> addCol = new TableColumn<>(I18nUtil.get("commit.col.added"));
        addCol.setCellValueFactory(d -> d.getValue().addedLinesProperty());
        addCol.setPrefWidth(60);
        addCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<FileRow, Number> delCol = new TableColumn<>(I18nUtil.get("commit.col.removed"));
        delCol.setCellValueFactory(d -> d.getValue().deletedLinesProperty());
        delCol.setPrefWidth(60);
        delCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        fileTable.getColumns().addAll(pathCol, extCol, statusCol, addCol, delCol);
        fileTable.setItems(displayedRows);
        fileTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        fileTable.setPrefHeight(220);
        fileTable.setPlaceholder(new Label(""));
        fileTable.getStyleClass().add("commit-table");
        // 自适应行：分组标题行不带 checkbox
        fileTable.setRowFactory(tv -> new FileRowTableRow());
        // 单击 / 双击处理：
        //   - 单击文件行 → 切换勾选状态（TortoiseGit 风格，文件行无 checkbox，靠行点击切换）
        //   - 双击文件行 → 打开 Diff 查看器
        //   - 单击 / 双击分组行 → 无操作
        fileTable.setOnMouseClicked(e -> {
            if (e.getButton() != javafx.scene.input.MouseButton.PRIMARY) {
                return;
            }
            FileRow row = (FileRow) fileTable.getSelectionModel().getSelectedItem();
            if (row == null || !row.isFile()) {
                return;
            }
            if (e.getClickCount() == 1) {
                // 单击：切换勾选状态
                row.setSelected(!row.isSelected());
                updateFileCount();
            } else if (e.getClickCount() == 2) {
                // 双击：打开 Diff 查看器
                openDiffForRow(row);
            }
        });
        VBox.setVgrow(fileTable, Priority.ALWAYS);

        VBox box = new VBox(4, header, checkRow, fileTable);
        return box;
    }

    /**
     * 双击文件行 → 弹出 DiffViewerDialog。
     * <p>按文件状态决定 diff 拉取方式：</p>
     * <ul>
     *   <li>UNTRACKED：直接读工作区文件</li>
     *   <li>DELETED：读 HEAD 中该文件内容</li>
     *   <li>其他：走 StatusService.getDiff(repo, path, HEAD, null)</li>
     * </ul>
     */
    private void openDiffForRow(FileRow row) {
        if (row == null || !row.isFile()) return;
        String path = row.getPath();
        FileStatus.FileState state = row.getState();
        Stage owner = (Stage) getDialogPane().getScene().getWindow();
        DiffViewerDialog.show(owner, repoPath, path, state, "HEAD", null, new DiffViewerDialog.DiffProvider() {
            @Override
            public String getDiff(String repoPath, String path, String oldRev, String newRev) {
                DiffResult r = statusService.getDiff(repoPath, path, oldRev, newRev);
                return r == null ? "" : (r.getDiffText() == null ? "" : r.getDiffText());
            }

            @Override
            public String readWorkingFile(String repoPath, String path) {
                // 读工作区文件全文
                java.io.File f = new java.io.File(repoPath, path);
                if (!f.exists() || !f.isFile()) {
                    return "";
                }
                try {
                    return new String(java.nio.file.Files.readAllBytes(f.toPath()),
                            java.nio.charset.StandardCharsets.UTF_8);
                } catch (Exception e) {
                    return "(读取失败：" + e.getMessage() + ")";
                }
            }

            @Override
            public String readHeadFile(String repoPath, String path) {
                // 读 HEAD 中该文件的内容
                try (org.eclipse.jgit.api.Git git = new org.eclipse.jgit.api.Git(
                        new org.eclipse.jgit.storage.file.FileRepositoryBuilder()
                                .setGitDir(new java.io.File(repoPath, ".git"))
                                .readEnvironment().findGitDir().build())) {
                    org.eclipse.jgit.lib.Repository repo = git.getRepository();
                    org.eclipse.jgit.lib.ObjectId headTree = repo.resolve("HEAD^{tree}");
                    if (headTree == null) {
                        return "";
                    }
                    try (org.eclipse.jgit.treewalk.TreeWalk tw = new org.eclipse.jgit.treewalk.TreeWalk(repo)) {
                        tw.addTree(headTree);
                        tw.setRecursive(true);
                        while (tw.next()) {
                            if (path.equals(tw.getPathString())) {
                                org.eclipse.jgit.lib.ObjectLoader loader = repo.open(tw.getObjectId(0));
                                return new String(loader.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
                            }
                        }
                    }
                } catch (Exception e) {
                    return "(读取失败：" + e.getMessage() + ")";
                }
                return "";
            }
        });
    }

    /**
     * 构造过滤按钮组：
     * <ul>
     *   <li>左侧两个 action 按钮：All / None（控制可见文件的勾选状态，不影响 visibility）</li>
     *   <li>右侧 visibility 过滤：Unversioned / Versioned / Added / Deleted / Modified / Files</li>
     * </ul>
     */
    private HBox makeFilterBar() {
        HBox bar = new HBox(4);
        bar.getChildren().addAll(
                checkAllActionBtn,
                checkNoneActionBtn,
                filterUnversioned,
                filterVersioned,
                filterAdded,
                filterDeleted,
                filterModified,
                filterFiles
        );
        // 默认不选任何 visibility 过滤（显示所有类型的文件）
        // 选 All/None 才会触发 action（不影响 visibility 过滤的选中态）
        filterGroup.selectedToggleProperty().addListener((obs, o, n) -> applyFilter());
        // Show Unversioned Files 切换 → 重过滤
        showUnversionedCheck.selectedProperty().addListener((obs, o, n) -> applyFilter());
        // All / None action 按钮：批量设置可见文件的勾选状态
        checkAllActionBtn.setOnAction(e -> setAllVisibleSelected(true));
        checkNoneActionBtn.setOnAction(e -> setAllVisibleSelected(false));
        return bar;
    }

    /**
     * 把当前可见文件批量设为勾选 / 取消勾选（TortoiseGit「All / None」行为）。
     * <p>只影响 {@link #displayedRows} 中的文件行，visibility 过滤本身不变。</p>
     */
    private void setAllVisibleSelected(boolean selected) {
        for (FileRow r : displayedRows) {
            if (r.isFile() && r.isCheckable()) {
                r.setSelected(selected);
            }
        }
        // 「Check」复选框的 indeterminate / checked 状态需要更新
        updateCheckAllTriState();
        updateFileCount();
    }

    /**
     * 统计 displayedRows 的勾选状态（保留方法是为了不过度重写调用方）。
     * <p>「Check」已改为列标题纯文本，不再维护三态 UI，</p>
     * <p>全选 / 全不选由右侧 All / None action 按钮控制。</p>
     */
    private void updateCheckAllTriState() {
        // 无 UI 副作用：保留方法签名以兼容已有调用方
        // 文件计数由 updateFileCount() 单独维护
    }

    /**
     * 创建 action 按钮（All / None）：与 visibility 过滤按钮外观相同，但不属于 toggle group，
     * 点击后执行批量勾选/取消勾选动作。
     */
    private Button makeActionButton(String i18nKey) {
        Button btn = new Button(I18nUtil.get(i18nKey));
        btn.setFocusTraversable(false);
        btn.getStyleClass().add("filter-button");
        btn.getStyleClass().add("filter-action");
        return btn;
    }

    private ToggleButton makeFilter(String i18nKey, String userData) {
        ToggleButton tb = new ToggleButton(I18nUtil.get(i18nKey));
        tb.setUserData(userData);
        tb.setToggleGroup(filterGroup);
        tb.setFocusTraversable(false);
        tb.getStyleClass().add("filter-button");
        return tb;
    }

    /**
     * 底部选项 + 文件计数 + View Patch 链接。
     */
    private HBox buildBottomOptionsBox() {
        // 第一行：Show Unversioned Files
        HBox line1 = new HBox(20, showUnversionedCheck, noAutoselectSubmodulesCheck);
        line1.setAlignment(Pos.CENTER_LEFT);
        // 第二行：Show Whole Project + Message only + 计数 + View Patch
        HBox line2 = new HBox(20, showWholeProjectCheck, messageOnlyCheck, spacer(), fileCountLabel, viewPatchLink);
        HBox.setHgrow(line2.getChildren().get(2), Priority.ALWAYS);
        line2.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(2, line1, line2);
        // 把外层包成 HBox 让 VBox 充满宽度
        HBox wrapper = new HBox(box);
        HBox.setHgrow(box, Priority.ALWAYS);
        return wrapper;
    }

    /**
     * 创建可伸缩占位 Region（在 HBox/VBox 中充当弹簧）。
     */
    private static Region spacer() {
        Region r = new Region();
        // 容器布局中需要 HBox.setHgrow(r, Priority.ALWAYS) 才能撑开
        r.getStyleClass().add("spacer");
        return r;
    }

    private MenuItem makeActionMenuItem(CommitAction action, String label) {
        MenuItem item = new MenuItem(label);
        item.setOnAction(e -> doCommit(action));
        return item;
    }

    // ================================================================
    //  数据加载
    // ================================================================

    /**
     * 加载当前分支 + 工作区变更文件。
     */
    private void loadBranchAndFiles() {
        AsyncUiLoader.submitRead(repoPath, TaskType.STATUS, () -> {
            String branch = "UNKNOWN";
            List<FileStatus> files = List.of();
            try {
                branch = statusService.getCurrentBranch(repoPath);
                files = statusService.getStatus(repoPath, true, false);
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

    /**
     * 根据 FileStatus 列表构造 TableView 数据行（按状态分组 + 文件行）。
     */
    private void buildRows(List<FileStatus> files) {
        allRows.clear();
        if (files == null) {
            return;
        }
        // 按状态分组
        List<FileStatus> modified = new ArrayList<>();
        List<FileStatus> staged = new ArrayList<>();
        List<FileStatus> deleted = new ArrayList<>();
        List<FileStatus> untracked = new ArrayList<>();
        List<FileStatus> conflict = new ArrayList<>();
        List<FileStatus> others = new ArrayList<>();

        for (FileStatus f : files) {
            if (f.getState() == null || f.getState() == FileStatus.FileState.UNMODIFIED
                    || f.getState() == FileStatus.FileState.IGNORED) {
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

        // 排序：路径升序
        Comparator<FileStatus> byPath = Comparator.comparing(FileStatus::getPath, String.CASE_INSENSITIVE_ORDER);
        modified.sort(byPath);
        staged.sort(byPath);
        deleted.sort(byPath);
        untracked.sort(byPath);
        conflict.sort(byPath);
        others.sort(byPath);

        // 添加分组标题 + 文件行
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
            // 选中状态变化 → 更新文件计数
            row.selectedProperty().addListener((obs, o, n) -> updateFileCount());
            allRows.add(row);
        }
    }

    /**
     * 应用 visibility 过滤规则（默认显示所有文件 + 受 Show Unversioned Files 控制）。
     * <p>注意：All / None 已改为 action 按钮，<b>不再</b>作为 visibility 过滤，</p>
     * <p>本方法只处理 Unversioned / Versioned / Added / Deleted / Modified / Files 几种 visibility 过滤。</p>
     */
    private void applyFilter() {
        Toggle selected = filterGroup.getSelectedToggle();
        String filter = selected == null ? null : (String) selected.getUserData();
        boolean showUnversioned = showUnversionedCheck.isSelected();
        displayedRows.clear();
        int totalFileCount = 0;
        for (FileRow r : allRows) {
            if (r.isGroup()) {
                // 分组标题：当组内至少有一个文件通过过滤时显示
                long matchInGroup = countFileMatchInGroup(r.getGroupName(), filter, showUnversioned);
                if (matchInGroup > 0) {
                    displayedRows.add(r);
                }
            } else {
                totalFileCount++;
                if (matchesFilter(r, filter, showUnversioned)) {
                    displayedRows.add(r);
                }
            }
        }
        // 「Check」复选框三态
        updateCheckAllTriState();
        // 文件计数
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

    /**
     * 判定一个文件行是否通过当前 visibility 过滤。
     *
     * @param filter          visibility 过滤 ID（null = 不按类型过滤，显示所有；All/None 已移出这里）
     * @param showUnversioned 是否勾选「Show Unversioned Files」
     */
    private boolean matchesFilter(FileRow r, String filter, boolean showUnversioned) {
        if (!r.isFile()) {
            return false;
        }
        FileStatus.FileState s = r.getState();
        // 「Show Unversioned Files」未勾选时，UNTRACKED 隐藏
        if (!showUnversioned && s == FileStatus.FileState.UNTRACKED) {
            return false;
        }
        // 未指定 visibility 过滤 → 显示所有
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
    //  提交动作
    // ================================================================

    /**
     * 构造 message 区域右键菜单（TortoiseGit 风格）。
     * <p>替换 JavaFX TextArea 默认右键菜单，增加 5 个 TortoiseGit 专属动作：</p>
     * <ul>
     *   <li>Pick commit hash — 选一个 commit，把 short hash 插入</li>
     *   <li>Pick commit message — 选一个 commit，把 message 插入</li>
     *   <li>Paste filename list — 把当前勾选文件路径列表插入（每行一个）</li>
     *   <li>Paste last commit message — 插入最近一次 commit 的 message</li>
     *   <li>Paste recent message... — 弹窗选历史 message</li>
     * </ul>
     */
    private ContextMenu buildMessageContextMenu() {
        ContextMenu menu = new ContextMenu();

        // ===== 标准编辑操作 =====
        MenuItem undo = new MenuItem(I18nUtil.get("edit.undo"));
        undo.setOnAction(e -> messageArea.undo());
        undo.setDisable(!messageArea.isUndoable());

        MenuItem redo = new MenuItem(I18nUtil.get("edit.redo"));
        redo.setOnAction(e -> messageArea.redo());
        redo.setDisable(!messageArea.isRedoable());

        MenuItem cut = new MenuItem(I18nUtil.get("edit.cut"));
        cut.setOnAction(e -> messageArea.cut());
        cut.setDisable(messageArea.getSelection().getLength() == 0);

        MenuItem copy = new MenuItem(I18nUtil.get("edit.copy"));
        copy.setOnAction(e -> messageArea.copy());
        copy.setDisable(messageArea.getSelection().getLength() == 0);

        MenuItem paste = new MenuItem(I18nUtil.get("edit.paste"));
        paste.setOnAction(e -> messageArea.paste());

        MenuItem delete = new MenuItem(I18nUtil.get("edit.delete"));
        delete.setOnAction(e -> {
            IndexRange sel = messageArea.getSelection();
            if (sel.getLength() > 0) {
                messageArea.deleteText(sel);
            }
        });
        delete.setDisable(messageArea.getSelection().getLength() == 0);

        MenuItem selectAll = new MenuItem(I18nUtil.get("edit.selectAll"));
        selectAll.setOnAction(e -> messageArea.selectAll());
        selectAll.setDisable(messageArea.getLength() == 0);

        // ===== TortoiseGit 专属动作 =====
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

        // 右键菜单弹出时，根据当前状态更新 enable / disable
        menu.setOnShown(e -> {
            boolean hasSel = messageArea.getSelection().getLength() > 0;
            boolean hasText = messageArea.getLength() > 0;
            cut.setDisable(!hasSel);
            copy.setDisable(!hasSel);
            delete.setDisable(!hasSel);
            selectAll.setDisable(!hasText);
            undo.setDisable(!messageArea.isUndoable());
            redo.setDisable(!messageArea.isRedoable());
        });

        menu.getItems().addAll(
                undo, redo,
                new SeparatorMenuItem(),
                cut, copy, paste, delete,
                new SeparatorMenuItem(),
                selectAll,
                new SeparatorMenuItem(),
                pickHash, pickMsg,
                new SeparatorMenuItem(),
                pasteFilenames, pasteLastMsg, pasteRecentMsg
        );
        return menu;
    }

    /**
     * 在当前光标位置插入文本。
     */
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
        // 把光标移到插入内容之后
        int newPos = start + text.length();
        messageArea.positionCaret(newPos);
    }

    /**
     * 在光标处插入文本，并保证光标前后有换行分隔（避免粘连到现有内容）。
     */
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

    /**
     * TortoiseGit 「Pick commit hash」：选一个 commit，把 short hash 插入 message。
     */
    private void actionPickCommitHash() {
        List<LogEntry> commits = loadRecentCommits(50);
        if (commits.isEmpty()) {
            showInfo(I18nUtil.get("commit.contextMenu.noRecentCommit"));
            return;
        }
        CommitPickerDialog dialog = new CommitPickerDialog(commits, null);
        dialog.showAndWait().ifPresent(entry -> {
            if (entry.getShortId() != null) {
                insertAtCaretWithNewline(entry.getShortId());
            }
        });
    }

    /**
     * TortoiseGit 「Pick commit message」：选一个 commit，把 message 插入 message。
     */
    private void actionPickCommitMessage() {
        List<LogEntry> commits = loadRecentCommits(50);
        if (commits.isEmpty()) {
            showInfo(I18nUtil.get("commit.contextMenu.noRecentCommit"));
            return;
        }
        CommitPickerDialog dialog = new CommitPickerDialog(commits, null);
        dialog.showAndWait().ifPresent(entry -> {
            if (entry.getMessage() != null && !entry.getMessage().isEmpty()) {
                insertAtCaretWithNewline(entry.getMessage());
            }
        });
    }

    /**
     * TortoiseGit 「Paste filename list」：把当前勾选文件路径列表插入 message。
     */
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

    /**
     * TortoiseGit 「Paste last commit message」：把最近一次 commit 的 message 插入。
     */
    private void actionPasteLastCommitMessage() {
        List<LogEntry> commits = loadRecentCommits(1);
        if (commits.isEmpty()) {
            showInfo(I18nUtil.get("commit.contextMenu.noRecentCommit"));
            return;
        }
        LogEntry last = commits.get(0);
        if (last.getMessage() != null && !last.getMessage().isEmpty()) {
            insertAtCaretWithNewline(last.getMessage());
        }
    }

    /**
     * TortoiseGit 「Paste recent message...」：弹窗从最近 N 条 message 中选一个插入。
     */
    private void actionPasteRecentMessage() {
        List<LogEntry> commits = loadRecentCommits(50);
        if (commits.isEmpty()) {
            showInfo(I18nUtil.get("commit.contextMenu.noRecentCommit"));
            return;
        }
        CommitPickerDialog dialog = new CommitPickerDialog(commits, null);
        dialog.showAndWait().ifPresent(entry -> {
            if (entry.getMessage() != null && !entry.getMessage().isEmpty()) {
                insertAtCaretWithNewline(entry.getMessage());
            }
        });
    }

    /**
     * 同步加载最近 commit 列表（最多 {@code limit} 条），失败返回空列表。
     */
    private List<LogEntry> loadRecentCommits(int limit) {
        try {
            return statusService.getLog(repoPath, 1, limit);
        } catch (Exception e) {
            log.error("加载最近 commit 失败", e);
            return List.of();
        }
    }

    /**
     * 把「Signed-off-by: ...」插入到 message 末尾（若已存在则不重复添加）。
     * <p>作者获取优先级：</p>
     * <ol>
     *   <li>「Set author」文本框（用户显式填写）</li>
     *   <li>git config user.name / user.email（最常见来源）</li>
     *   <li>弹错提示用户填写（不能用分支名 master 兜底，否则会出现 "Signed-off-by: master" 的误用）</li>
     * </ol>
     */
    private void insertSignedOffBy() {
        String author = "";
        // 1) 优先用「Set author」文本框
        if (authorField.getText() != null && !authorField.getText().isBlank()) {
            author = authorField.getText().trim();
        }
        // 2) 兜底：从 git config 读
        if (author.isBlank()) {
            author = readGitAuthorFromConfig();
        }
        // 3) 拿不到时弹错（避免写入 "Signed-off-by: master" 这种垃圾值）
        if (author.isBlank()) {
            new Alert(Alert.AlertType.WARNING,
                    "无法获取作者信息，请先在「设置作者」中填写，或通过 git config 配置 user.name/user.email。"
                            + "\n\n(Signed-off-by 需要真实的作者签名，"
                            + "不能用分支名兜底，否则会写入无效的 Signed-off-by: master 之类的内容。)")
                    .showAndWait();
            return;
        }
        String line = "Signed-off-by: " + author;
        String current = messageArea.getText() == null ? "" : messageArea.getText();
        if (current.contains(line)) {
            return; // 已存在
        }
        String updated = current.isEmpty() ? line : (current.endsWith("\n") ? current + line : current + "\n" + line);
        messageArea.setText(updated);
        messageArea.positionCaret(updated.length());
    }

    /**
     * 从 git config 读取 user.name / user.email，格式化为 "Name <email>"。
     */
    private String readGitAuthorFromConfig() {
        try {
            String name = readGitConfig("user.name");
            String email = readGitConfig("user.email");
            if (name == null && email == null) {
                return "";
            }
            name = name == null ? "" : name.trim();
            email = email == null ? "" : email.trim();
            if (email.isEmpty()) {
                return name;
            }
            return name + " <" + email + ">";
        } catch (Exception e) {
            log.debug("读取 git author 失败：{}", e.getMessage());
            return "";
        }
    }

    private String readGitConfig(String key) {
        try (Git git = new Git(new org.eclipse.jgit.storage.file.FileRepositoryBuilder()
                .setGitDir(new File(repoPath, ".git"))
                .readEnvironment().findGitDir().build())) {
            return git.getRepository().getConfig().getString("user", null, key);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 执行提交。
     * <p>校验通过后通过 {@link AsyncUiLoader} 提交写任务（BR-33/BR-34 同仓库写串行），</p>
     * <p>提交过程中：commit 按钮文案改成「正在提交…」+ 禁用所有 action 按钮，避免双击；</p>
     * <p>成功后弹出提交哈希并关闭对话框，命中红线或异常时弹错并恢复按钮。</p>
     */
    private void doCommit(CommitAction action) {
        // 防止双击：committing 期间再次点 commit 按钮直接忽略
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
        // BR-06：至少勾选一个文件
        if (selectedFiles.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, I18nUtil.get("commit.selectAtLeastOne")).showAndWait();
            return;
        }
        // BR-07：提交信息非空（Amend 可复用上次）
        String message = messageArea.getText() == null ? "" : messageArea.getText().trim();
        boolean amend = amendCheck.isSelected();
        if (message.isEmpty() && !amend) {
            new Alert(Alert.AlertType.WARNING, I18nUtil.get("commit.messageRequired")).showAndWait();
            return;
        }

        // 组装 author
        boolean customAuthor = setAuthorCheck.isSelected()
                && authorField.getText() != null
                && !authorField.getText().trim().isEmpty();
        String author = customAuthor ? authorField.getText().trim() : null;

        // 组装 authorDate
        boolean setAuthorDate = setAuthorDateCheck.isSelected();
        LocalDateTime authorDate = null;
        if (setAuthorDate) {
            LocalDate d = authorDatePicker.getValue() == null ? LocalDate.now() : authorDatePicker.getValue();
            LocalTime t = parseTimeOrNow(authorTimeField.getText());
            authorDate = LocalDateTime.of(d, t);
        }

        // new branch
        boolean createNewBranch = newBranchCheck != null && newBranchCheck.isSelected();
        String newBranchName = (newBranchNameField != null) ? newBranchNameField.getText() : null;
        if (createNewBranch && (newBranchName == null || newBranchName.isBlank())) {
            new Alert(Alert.AlertType.WARNING, I18nUtil.get("switch.newBranchRequired")).showAndWait();
            return;
        }

        // ReCommit：使用上次 message
        if (action == CommitAction.RE_COMMIT) {
            message = "";
            // Amend 模式下 message 为空 → 走 git commit --amend 默认沿用上次信息
            amend = true;
        }

        boolean pushAfter = action == CommitAction.COMMIT_AND_PUSH || action == CommitAction.COMMIT_AND_PUSH_TAGS;
        boolean pushTags = action == CommitAction.COMMIT_AND_PUSH_TAGS;

        // Commit & Push 模式下，提前选好 Remote（先选，不阻塞 UI）
        // 优先 origin，没有 origin 就用第一个；都没有就弹友好错误让用户去配置
        // 用 final wrapper 数组绕过 lambda 变量必须是 effectively final 的限制
        final String[] pickedRemoteHolder = {null};
        if (pushAfter) {
            try {
                java.util.List<com.gitgui.domain.model.RemoteConfig> remotes =
                        remoteConfigService == null ? java.util.List.of() : remoteConfigService.list(repoPath);
                if (remotes == null || remotes.isEmpty()) {
                    // 无 Remote：弹友好错误，提供「去配置」入口
                    if (!showNoRemoteDialogAndReturn()) {
                        return;  // 用户取消
                    }
                } else {
                    pickedRemoteHolder[0] = pickBestRemote(remotes);
                }
            } catch (Exception e) {
                log.warn("读取 Remote 列表失败：{}", e.getMessage());
                // 读取失败也弹友好错误（不要让 push 时才暴露 BR-09 这种技术错误）
                if (!showNoRemoteDialogAndReturn()) {
                    return;
                }
            }
        }
        final String pickedRemote = pickedRemoteHolder[0];

        CommitRequest req = CommitRequest.builder()
                .repoPath(repoPath)
                .stagedFiles(selectedFiles)
                .message(message)
                .amend(amend)
                .signCommit(signCheck.isSelected())
                .customAuthor(customAuthor)
                .author(author)
                .setAuthorDate(setAuthorDate)
                .authorDate(authorDate)
                .pushAfterCommit(pushAfter)
                .pushWithTags(pushTags)
                .reuseLastMessage(action == CommitAction.RE_COMMIT)
                .createNewBranch(createNewBranch)
                .newBranchName(newBranchName)
                .build();

        // ====== 关键 UX 修复：commit 进行中显示进度状态 ======
        setCommittingState(true);

        // 使用独立 Thread 执行 commit（不依赖 AsyncUiLoader，避免任务队列阻塞导致卡住）。
        // commit 是同步的，push 是异步的（通过 TaskHandle.onSuccess/onFailure 回调拿到结果）。
        new Thread(() -> {
            String commitId;
            try {
                commitId = gitOperationService.commit(req);
            } catch (RedLineBlockedException e) {
                final String msg = e.getMessage();
                final String rule = e.getRuleCode();
                Platform.runLater(() -> {
                    setCommittingState(false);
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(I18nUtil.get("redline.blocked.title"));
                    alert.setHeaderText(I18nUtil.get("redline.blocked.hitRule") + rule);
                    alert.setContentText(msg);
                    alert.showAndWait();
                });
                return;
            } catch (Exception e) {
                log.error("提交失败", e);
                final String msg = formatExceptionMessage(e);
                Platform.runLater(() -> {
                    setCommittingState(false);
                    new Alert(Alert.AlertType.ERROR,
                            I18nUtil.get("commit.failed") + msg).showAndWait();
                });
                return;
            }

            // commit 成功
            final String cid = commitId;
            if (!req.isPushAfterCommit()) {
                // 不推送：直接显示提交成功
                Platform.runLater(() -> {
                    setCommittingState(false);
                    new Alert(Alert.AlertType.INFORMATION,
                            I18nUtil.get("commit.success") + cid.substring(0, Math.min(8, cid.length()))).showAndWait();
                    close();
                });
                return;
            }

            // commit&push 模式：push 是异步的，注册 TaskHandle 回调处理结果
            // （关键修复：之前没注册回调，push 异常被静默吞掉，错误信息丢失）
            com.gitgui.domain.model.request.PushRequest pushReq =
                    com.gitgui.domain.model.request.PushRequest.builder()
                            .repoPath(repoPath)
                            .remote(pickedRemote)
                            .force(false)
                            .forceWithLease(false)
                            .pushAllBranches(false)
                            .pushAllTags(req.isPushWithTags())
                            .includeTags(req.isPushWithTags())
                            .build();
            try {
                com.gitgui.core.async.TaskHandle pushHandle = gitOperationService.push(pushReq, null);
                if (pushHandle == null) {
                    // 防御性：service 返回 null 当作成功处理
                    Platform.runLater(() -> {
                        setCommittingState(false);
                        new Alert(Alert.AlertType.INFORMATION,
                                I18nUtil.get("commit.success") + cid.substring(0, Math.min(8, cid.length()))).showAndWait();
                        close();
                    });
                    return;
                }
                pushHandle.onSuccess(r -> Platform.runLater(() -> {
                    setCommittingState(false);
                    new Alert(Alert.AlertType.INFORMATION,
                            I18nUtil.get("commit.success") + cid.substring(0, Math.min(8, cid.length()))
                                    + "\n" + I18nUtil.get("commit.pushSuccess")).showAndWait();
                    close();
                }));
                pushHandle.onFailure(e -> {
                    // 打完整堆栈（这样从日志能看出 JGit 内部到底抛了啥）
                    log.error("Commit & Push 推送失败，提交已成功不回滚。commitId={}, remote={}",
                            cid, pickedRemote, e);
                    // 如果异常看起来是 JGit 内部 bug（CCE / 内部异常），自动回退到 CLI 推送
                    if (isJGitInternalBug(e)) {
                        log.warn("JGit 推送抛内部异常，自动回退到 git CLI 推送。commitId={}", cid);
                        tryFallbackToCliPush(cid, pickedRemote, req);
                    } else {
                        final String pushMsg = formatExceptionMessage(e);
                        Platform.runLater(() -> {
                            setCommittingState(false);
                            new Alert(Alert.AlertType.WARNING,
                                    I18nUtil.get("commit.success") + cid.substring(0, Math.min(8, cid.length()))
                                            + "\n" + I18nUtil.get("commit.pushFailed") + pushMsg).showAndWait();
                            close();
                        });
                    }
                });
            } catch (Exception pushEx) {
                // 同步校验失败（如 BR-09 选 Remote 失败）：直接弹错
                log.warn("Push 同步阶段失败：{}", pushEx.getMessage());
                // 如果异常是 JGit 内部 bug，自动回退到 CLI
                if (isJGitInternalBug(pushEx)) {
                    log.warn("Push 同步阶段抛 JGit 内部异常，自动回退到 git CLI 推送。commitId={}", cid);
                    tryFallbackToCliPush(cid, pickedRemote, req);
                } else {
                    final String pushMsg = formatExceptionMessage(pushEx);
                    Platform.runLater(() -> {
                        setCommittingState(false);
                        new Alert(Alert.AlertType.WARNING,
                                I18nUtil.get("commit.success") + cid.substring(0, Math.min(8, cid.length()))
                                        + "\n" + I18nUtil.get("commit.pushFailed") + pushMsg).showAndWait();
                        close();
                    });
                }
            }
        }, "CommitWorker").start();
    }

    /**
     * 设置 commit 进行中的 UI 状态。
     * <ul>
     *   <li>{@code committing=true}：commit 按钮文案改成「正在提交…」+ 禁用 commit/cancel/help + 弹窗标题前缀</li>
     *   <li>{@code committing=false}：恢复原状</li>
     * </ul>
     */
    private void setCommittingState(boolean committing) {
        this.committing = committing;
        if (committing) {
            commitMenuBtn.setText(I18nUtil.get("commit.committing"));
            commitMenuBtn.setDisable(true);
            for (MenuItem item : commitMenuBtn.getItems()) {
                item.setDisable(true);
            }
            if (cancelButtonRef != null) {
                cancelButtonRef.setDisable(true);
            }
            if (helpButtonRef != null) {
                helpButtonRef.setDisable(true);
            }
            // 改一下弹窗标题（让用户看到正在处理中）
            if (getDialogPane().getScene() != null
                    && getDialogPane().getScene().getWindow() instanceof Stage stage) {
                stage.setTitle(repoPath + " - " + I18nUtil.get("commit.title")
                        + " - " + I18nUtil.get("commit.committing"));
            }
        } else {
            commitMenuBtn.setText(I18nUtil.get("commit.action.commitAndPushMenu"));
            commitMenuBtn.setDisable(false);
            for (MenuItem item : commitMenuBtn.getItems()) {
                item.setDisable(false);
            }
            if (cancelButtonRef != null) {
                cancelButtonRef.setDisable(false);
            }
            if (helpButtonRef != null) {
                helpButtonRef.setDisable(false);
            }
            if (getDialogPane().getScene() != null
                    && getDialogPane().getScene().getWindow() instanceof Stage stage) {
                stage.setTitle(repoPath + " - " + I18nUtil.get("commit.title"));
            }
        }
    }

    private static LocalTime parseTimeOrNow(String text) {
        if (text == null || text.isBlank()) {
            return LocalTime.now().withNano(0);
        }
        try {
            // 支持 HH:mm:ss 与 HH:mm
            String[] parts = text.trim().split(":");
            int h = Integer.parseInt(parts[0]);
            int m = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int s = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return LocalTime.of(h, m, s);
        } catch (Exception e) {
            return LocalTime.now().withNano(0);
        }
    }

    /**
    /**
     * 轻量级 INFORMATION 弹窗（用于右键菜单操作的轻提示）。
     */
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setHeaderText(null);
        alert.setTitle(I18nUtil.get("commit.title"));
        alert.showAndWait();
    }

    /**
     * 判断异常是否是 JGit 内部 bug（用于自动回退到 CLI 推送）。
     * <ul>
     *   <li>{@link ClassCastException} — JGit 6.9 已知问题</li>
     *   <li>JGit 内部包（{@code org.eclipse.jgit.internal.*}）的异常</li>
     *   <li>其他裸 ClassCastException（message 为 null）</li>
     * </ul>
     * 这种情况网络/认证都不是问题，是 JGit 本身 bug，CLI 兜底成功率很高。
     */
    private boolean isJGitInternalBug(Throwable e) {
        if (e == null) return false;
        Throwable cur = e;
        int depth = 0;
        while (cur != null && depth < 5) {
            if (cur instanceof ClassCastException) {
                return true;
            }
            String cls = cur.getClass().getName();
            // JGit 内部包（如 org.eclipse.jgit.internal.transport.http.*）抛的任何异常
            if (cls.startsWith("org.eclipse.jgit.internal.")) {
                return true;
            }
            if (cur.getCause() == null || cur.getCause() == cur) break;
            cur = cur.getCause();
            depth++;
        }
        return false;
    }

    /**
     * CLI 推送回退：JGit 失败后用系统 {@code git push} 命令重试。
     * <p>必须在新线程中执行（CLI 推送是阻塞的），用 Platform.runLater 更新 UI。</p>
     */
    private void tryFallbackToCliPush(String commitId, String remote, CommitRequest req) {
        new Thread(() -> {
            try {
                com.gitgui.domain.model.request.PushRequest cliReq =
                        com.gitgui.domain.model.request.PushRequest.builder()
                                .repoPath(repoPath)
                                .remote(remote)
                                .force(false)
                                .build();
                String output = gitOperationService.pushViaCli(cliReq);
                log.info("CLI 推送回退成功：commitId={}, remote={}, output={}", commitId, remote, output);
                Platform.runLater(() -> {
                    setCommittingState(false);
                    new Alert(Alert.AlertType.INFORMATION,
                            I18nUtil.get("commit.success") + commitId.substring(0, Math.min(8, commitId.length()))
                                    + "\n" + I18nUtil.get("commit.pushSuccess")
                                    + "（JGit 失败后用 git CLI 兜底成功）").showAndWait();
                    close();
                });
            } catch (Exception cliEx) {
                log.error("CLI 推送回退也失败", cliEx);
                final String cliMsg = formatExceptionMessage(cliEx);
                Platform.runLater(() -> {
                    setCommittingState(false);
                    new Alert(Alert.AlertType.WARNING,
                            I18nUtil.get("commit.success") + commitId.substring(0, Math.min(8, commitId.length()))
                                    + "\n" + I18nUtil.get("commit.pushFailed") + cliMsg
                                    + "\n\n（JGit 和 git CLI 都已尝试，均失败，请检查 Remote 配置或网络。）").showAndWait();
                    close();
                });
            }
        }, "PushCliFallback").start();
    }

    /**
     * 把任意 {@link Throwable} 转成对用户友好的错误信息字符串。
     * <ul>
     *   <li>优先取 {@link Throwable#getMessage()}（去掉技术性 class 名）</li>
     *   <li>对常见网络 / Git 异常映射成业务化提示（连接超时、未知主机、认证失败等）</li>
     *   <li>对 ExecutionException / TimeoutException / ClassCastException 这类 message 为 null 的异常，
     *       剥出 cause 重新解析</li>
     *   <li>对 ClassCastException 等「无 message」异常，输出「类型 X 无法转换为 Y」+ 完整类名 + 完整 cause 链</li>
     *   <li>实在拿不到就返回「完整类名（包路径）」，而不是简化的 ClassCastException</li>
     * </ul>
     */
    private String formatExceptionMessage(Throwable e) {
        if (e == null) {
            return "未知错误";
        }
        // 先尝试最深的 cause（有意义的 message）
        Throwable deepestMeaningful = findDeepestWithMessage(e);
        if (deepestMeaningful != null) {
            return translateException(deepestMeaningful, deepestMeaningful.getMessage());
        }
        // 没有任何 cause 有 message（比如 ClassCastException）—— 走特殊路径
        return buildFallbackMessage(e);
    }

    /**
     * 在 cause 链中找最深的有 message 的异常。
     */
    private Throwable findDeepestWithMessage(Throwable e) {
        Throwable best = null;
        Throwable cur = e;
        // 最多剥 5 层
        for (int i = 0; i < 5 && cur != null; i++) {
            String msg = cur.getMessage();
            if (msg != null && !msg.isBlank()
                    && !msg.startsWith("class ")
                    && !msg.equals(cur.getClass().getName())) {
                best = cur;  // 越深越好
            }
            if (cur.getCause() == null || cur.getCause() == cur) {
                break;
            }
            cur = cur.getCause();
        }
        return best;
    }

    /**
     * 当所有 cause 都没有 message 时（例如裸 ClassCastException），构造一个包含完整类名 + 完整 cause 链的诊断消息。
     */
    private String buildFallbackMessage(Throwable e) {
        // 优先用业务化翻译（看异常类型）—— 例如 TimeoutException 即使没 message 也能翻译
        String translated = translateException(e, null);
        if (translated != null && !translated.equals(e.getClass().getSimpleName())) {
            return translated + "（" + describeExceptionChain(e) + "）";
        }
        // 翻译不出 → 返回 cause 链 + 完整类名
        return describeExceptionChain(e);
    }

    /**
     * 输出完整异常链：「A → B → C（完整类名）」
     */
    private String describeExceptionChain(Throwable e) {
        if (e == null) return "未知错误";
        StringBuilder sb = new StringBuilder();
        Throwable cur = e;
        int depth = 0;
        while (cur != null && depth < 6) {
            if (depth > 0) sb.append(" → ");
            String msg = cur.getMessage();
            sb.append(cur.getClass().getName());
            if (msg != null && !msg.isBlank() && !msg.equals(cur.getClass().getName())) {
                sb.append("(").append(msg).append(")");
            }
            if (cur.getCause() == null || cur.getCause() == cur) {
                break;
            }
            cur = cur.getCause();
            depth++;
        }
        return sb.toString();
    }

    /**
     * 根据异常类型返回对应的中文业务提示。
     */
    private String translateException(Throwable e, String msg) {
        if (e == null) {
            return "未知错误";
        }
        // ClassCastException 特殊处理：JGit 内部 CCE 经常 message 为 null，输出完整类名 + cause 链
        if (e instanceof ClassCastException) {
            String detail = msg;
            if (detail == null || detail.isBlank()) {
                detail = describeExceptionChain(e);
            }
            return "类型转换异常（ClassCastException）：" + detail
                    + "\n可能原因：JGit 6.9 内部 bug / 仓库 .git 目录异常 / 远端配置不兼容。"
                    + "\n建议：在终端用 `git push` 测试，或重试一次。";
        }
        // 网络类
        if (e instanceof java.net.UnknownHostException) {
            return "网络错误：无法解析主机名（" + (msg == null ? e.getClass().getName() : msg) + "）";
        }
        if (e instanceof java.net.SocketTimeoutException) {
            return "网络错误：连接超时（" + (msg == null ? "" : msg) + "）";
        }
        if (e instanceof java.net.ConnectException) {
            return "网络错误：无法连接到服务器（" + (msg == null ? "" : msg) + "）";
        }
        if (e instanceof java.net.NoRouteToHostException
                || e instanceof java.net.PortUnreachableException) {
            return "网络错误：无法到达目标主机（" + (msg == null ? "" : msg) + "）";
        }
        // 并发异常（异步任务包装）
        if (e instanceof java.util.concurrent.TimeoutException) {
            return "操作超时（" + (msg == null ? "" : msg) + "）";
        }
        if (e instanceof java.util.concurrent.ExecutionException) {
            return "异步执行失败（" + (msg == null ? "" : msg) + "）";
        }
        if (e instanceof java.util.concurrent.CancellationException) {
            return "操作被取消";
        }
        // JGit
        if (e instanceof org.eclipse.jgit.errors.TransportException) {
            return "Git 传输失败：" + (msg == null ? "请检查网络或认证信息" : msg);
        }
        if (e instanceof org.eclipse.jgit.api.errors.TransportException) {
            Throwable cause = e.getCause();
            String detail = cause == null ? "" : ("（" + formatExceptionMessage(cause) + "）");
            return "Git 传输失败：" + (msg == null ? "请检查网络或认证信息" : msg) + detail;
        }
        if (e instanceof org.eclipse.jgit.errors.MissingObjectException) {
            return "Git 对象缺失：" + (msg == null ? "文件或对象已被删除" : msg);
        }
        if (e instanceof org.eclipse.jgit.errors.AmbiguousObjectException) {
            return "Git 对象引用不明确：" + (msg == null ? "请使用完整 SHA" : msg);
        }
        if (e instanceof org.eclipse.jgit.errors.IncorrectObjectTypeException) {
            return "Git 对象类型错误：" + (msg == null ? "" : msg);
        }
        // GitGuiException（业务异常）
        if (e instanceof com.gitgui.core.exception.GitGuiException) {
            return msg == null ? e.getClass().getSimpleName() : msg;
        }
        // 兜底：返回 message（如果非空）或完整类名（带包路径）
        if (msg != null && !msg.isBlank()) {
            return msg;
        }
        return e.getClass().getName();  // 关键：返回完整类名（含包路径）而不是 SimpleName
    }

    /**
     * 从 Remote 列表中智能挑一个：优先 origin，没有 origin 就用第一个。
     *
     * @param remotes Remote 列表
     * @return 选中的 Remote 名称（始终非 null）
     */
    private String pickBestRemote(List<com.gitgui.domain.model.RemoteConfig> remotes) {
        if (remotes == null || remotes.isEmpty()) {
            return null;
        }
        for (com.gitgui.domain.model.RemoteConfig rc : remotes) {
            if ("origin".equalsIgnoreCase(rc.getName())) {
                return rc.getName();
            }
        }
        return remotes.get(0).getName();
    }

    /**
     * 弹出「没有 Remote」友好错误对话框，并提供「去配置 Remote」入口。
     * <p>点「去配置 Remote」会打开 RemoteConfigDialog（已存在的工具类），让用户当场配置；</p>
     * <p>点「取消」或关掉对话框时，本方法返回 false（调用方应放弃 commit 推送）。</p>
     *
     * @return true = 用户已配置好 Remote（继续 commit & push）；false = 用户取消
     */
    private boolean showNoRemoteDialogAndReturn() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(I18nUtil.get("commit.noRemote.title"));
        alert.setHeaderText(I18nUtil.get("commit.noRemote.title"));
        alert.setContentText(I18nUtil.get("commit.noRemote.content"));
        // 自定义按钮：取消 / 去配置 Remote
        ButtonType cancelBtn = new ButtonType(I18nUtil.get("commit.noRemote.cancel"),
                ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType openManagerBtn = new ButtonType(I18nUtil.get("commit.noRemote.openManager"),
                ButtonBar.ButtonData.APPLY);
        alert.getButtonTypes().setAll(cancelBtn, openManagerBtn);
        java.util.Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == openManagerBtn) {
            // 用户选择「去配置」：打开 RemoteConfigDialog
            try {
                com.gitgui.ui.dialog.RemoteConfigDialog remoteDlg =
                        new com.gitgui.ui.dialog.RemoteConfigDialog(remoteConfigService, repoPath);
                remoteDlg.showAndWait();
            } catch (Exception e) {
                log.error("打开 Remote 配置对话框失败", e);
            }
            // 配置完后，让用户重新点 commit：不再自动继续
            return false;
        }
        return false;  // 取消
    }

    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18nUtil.get("commit.title") + " - " + I18nUtil.get("button.help"));
        alert.setHeaderText(I18nUtil.get("commit.title"));
        alert.setContentText("""
                提交对话框使用说明：

                1. 顶部「Commit to:」展示当前分支，勾选 new branch 可在提交前创建并切换到新分支。
                2. Message 文本框输入提交信息，第一行建议 ≤ 60 字符，右下角显示字符计数。
                3. 勾选 Amend Last Commit 可修补上次提交（message 留空则复用上次）。
                4. Set author date / Set author 可自定义作者及时间。
                5. 「+ Add Signed-off-by」按钮自动在 Message 末尾追加 Signed-off-by 行。
                6. 下方表格展示变更文件，按状态分组；表头 Check 可全选/全不选当前显示的文件。
                7. 过滤按钮（All/None/Unversioned/...）按状态过滤文件。
                8. 底部「Show Unversioned Files」控制是否展示未跟踪文件。
                9. 「Commit & Push ▼」下拉选择 4 种动作：仅提交 / ReCommit / 提交并推送 / 提交并推送标签。
                10. 双击文件行可查看该文件的 diff。
                """);
        alert.showAndWait();
    }

    // ================================================================
    //  数据模型
    // ================================================================

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
        // 用于表格属性绑定
        private final javafx.beans.property.StringProperty pathPropertyObj;
        private final javafx.beans.property.StringProperty extensionPropertyObj;
        private final javafx.beans.property.StringProperty statusPropertyObj;
        private final javafx.beans.property.IntegerProperty addedLinesPropertyObj;
        private final javafx.beans.property.IntegerProperty deletedLinesPropertyObj;

        private FileRow(boolean group, String groupName, int groupSize,
                        String path, FileStatus.FileState state, String extension,
                        int addedLines, int deletedLines, boolean selected) {
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
                int dot = f.getPath().lastIndexOf('.');
                if (dot > 0 && dot < f.getPath().length() - 1) {
                    ext = f.getPath().substring(dot + 1);
                }
            }
            return new FileRow(false, null, 0, f.getPath(), f.getState(), ext,
                    f.getAddedLines(), f.getDeletedLines(), true);
        }

        private static String formatState(FileStatus.FileState s) {
            if (s == null) return "";
            return switch (s) {
                case MODIFIED -> "Modified";
                case UNTRACKED -> "Unknown"; // TortoiseGit 风格：未跟踪显示为 Unknown
                case DELETED -> "Deleted";
                case STAGED -> "Added";
                case CONFLICT -> "Conflict";
                case IGNORED -> "Ignored";
                case UNMODIFIED -> "Unmodified";
            };
        }

        public boolean isGroup() { return group; }
        public boolean isFile() { return !group; }
        public String getGroupName() { return groupName; }
        public int getGroupSize() { return groupSize; }
        public String getPath() { return path; }
        public FileStatus.FileState getState() { return state; }
        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean v) { selected.set(v); }
        public BooleanProperty selectedProperty() { return selected; }
        public boolean isCheckable() { return isFile(); }

        public javafx.beans.property.StringProperty pathProperty() { return pathPropertyObj; }
        public javafx.beans.property.StringProperty extensionProperty() { return extensionPropertyObj; }
        public javafx.beans.property.StringProperty statusProperty() { return statusPropertyObj; }
        public IntegerProperty addedLinesProperty() { return addedLinesPropertyObj; }
        public IntegerProperty deletedLinesProperty() { return deletedLinesPropertyObj; }
    }

    /**
     * Path 列 Cell：TortoiseGit 风格
     * <ul>
     *   <li>文件行：左侧 ☑ / ☐ 小图标（状态指示，不可点击）+ 右侧 path 文本；行点击切换勾选状态</li>
     *   <li>分组行：「分组名 (size)」加粗显示</li>
     * </ul>
     */
    private static class PathCell extends TableCell<FileRow, String> {
        /** 状态指示图标（☑ / ☐），不可点击，行点击才切换 */
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
                // 状态指示图标：☑ (U+2611) / ☐ (U+2610)
                checkIndicator.setText(row.isSelected() ? "\u2611" : "\u2610");
                checkIndicator.setStyle(row.isSelected()
                        ? "-fx-text-fill: #1976d2; -fx-font-size: 14px;"
                        : "-fx-text-fill: #bdbdbd; -fx-font-size: 14px;");
                // 第二个 Label 显示 path
                ((Label) graphic.getChildren().get(1)).setText(item);
                ((Label) graphic.getChildren().get(1)).setStyle(stateStyle(row.getState()));
                // 监听 row.selected 变化 → 更新图标
                row.selectedProperty().addListener(this::onRowSelectedChanged);
                setGraphic(graphic);
                setText(null);
                setStyle(stateStyle(row.getState()));
            }
        }

        private void onRowSelectedChanged(javafx.beans.value.ObservableValue<? extends Boolean> obs, Boolean o, Boolean n) {
            if (n == null) return;
            checkIndicator.setText(n ? "\u2611" : "\u2610");
            checkIndicator.setStyle(n
                    ? "-fx-text-fill: #1976d2; -fx-font-size: 14px;"
                    : "-fx-text-fill: #bdbdbd; -fx-font-size: 14px;");
        }

        private void unbindPrevious() {
            if (boundRow != null) {
                boundRow.selectedProperty().removeListener(this::onRowSelectedChanged);
            }
        }
    }

    private static String stateStyle(FileStatus.FileState s) {
        if (s == null) return "";
        return switch (s) {
            case MODIFIED -> "-fx-text-fill: #1976d2;";
            case STAGED, UNTRACKED -> "-fx-text-fill: #388e3c;";
            case DELETED -> "-fx-text-fill: #d32f2f;";
            case CONFLICT -> "-fx-text-fill: #d32f2f; -fx-font-weight: bold;";
            default -> "";
        };
    }

    /**
     * TableRow：分组行不可选 + 灰底；文件行可双击触发 diff 回调（外部可扩展）。
     */
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

    // 占位：在 MessageFormat.format 中需要 java.text.MessageFormat
    private static final class MessageFormat {
        static String format(String pattern, Object... args) {
            return java.text.MessageFormat.format(pattern, args);
        }
    }
}
