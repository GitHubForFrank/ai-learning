package com.gitgui.ui.main;

import com.gitgui.core.async.ProgressCallback;
import com.gitgui.core.async.TaskHandle;
import com.gitgui.core.config.AppConfig;
import com.gitgui.core.exception.GitGuiException;
import com.gitgui.domain.constant.TaskType;
import com.gitgui.domain.model.FileStatus;
import com.gitgui.domain.model.Favorite;
import com.gitgui.domain.model.LogEntry;
import com.gitgui.domain.model.RepoScanRoot;
import com.gitgui.domain.model.RepositoryMeta;
import com.gitgui.domain.service.AsyncTaskService;
import com.gitgui.domain.service.FavoriteService;
import com.gitgui.domain.service.GitOperationService;
import com.gitgui.domain.service.RemoteConfigService;
import com.gitgui.domain.service.RepoScanRootService;
import com.gitgui.domain.service.RepositoryService;
import com.gitgui.domain.service.SettingsService;
import com.gitgui.domain.service.StatusService;
import com.gitgui.ui.AsyncUiLoader;
import com.gitgui.ui.dialog.CloneDialog;
import com.gitgui.ui.dialog.CommitDialog;
import com.gitgui.ui.dialog.ExternalToolsDialog;
import com.gitgui.ui.dialog.ProgressDialog;
import com.gitgui.ui.dialog.PullDialog;
import com.gitgui.ui.dialog.PushDialog;
import com.gitgui.ui.dialog.RemoteConfigDialog;
import com.gitgui.ui.dialog.SettingsDialog;
import com.gitgui.ui.dialog.SwitchDialog;
import com.gitgui.ui.i18n.I18nUtil;
import com.gitgui.ui.theme.ThemeManager;
import com.google.inject.Inject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;

/**
 * 主窗口 Controller
 * <p>对应 PRD 第五章 UI 架构：菜单栏 + 左侧侧边栏（收藏 / 仓库）+ 内容区 + 状态栏。</p>
 * <p>侧边栏功能：</p>
 * <ul>
 *   <li>收藏区（顶部）：展示已收藏仓库，支持 ★ 切换</li>
 *   <li>仓库区（底部）：按扫描根目录分组展示仓库列表，支持右键菜单（打开/提交/拉取/推送/获取/设置/在文件管理器中打开/复制路径/设置别名/刷新/移除）</li>
 *   <li>记忆：扫描根目录持久化到 {@code repo_scan_root} 表，下次启动自动恢复并重新扫描</li>
 * </ul>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class MainController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    /** 仓库服务 */
    @Inject private RepositoryService repositoryService;
    /** 状态查询服务 */
    @Inject private StatusService statusService;
    /** Git 操作服务 */
    @Inject private GitOperationService gitOperationService;
    /** 设置服务 */
    @Inject private SettingsService settingsService;
    /** 主题管理器 */
    @Inject private ThemeManager themeManager;
    /** 收藏服务 */
    @Inject private FavoriteService favoriteService;
    /** 远程配置服务 */
    @Inject private RemoteConfigService remoteConfigService;
    /** 扫描根目录服务 */
    @Inject private RepoScanRootService repoScanRootService;
    /** 异步任务服务（用于订阅扫描结果刷新） */
    @Inject private AsyncTaskService asyncTaskService;

    @FXML private MenuBar menuBar;
    @FXML private TitledPane favoritesPane;
    @FXML private ListView<RepoListItem> favoritesList;
    @FXML private Label repositoriesTitleLabel;
    /** 仓库列表的折叠/展开切换按钮（▲=展开，▼=折叠） */
    @FXML private Button repositoriesToggleButton;
    @FXML private ListView<RepoListItem> repositoriesList;
    @FXML private Button addRootButton;
    @FXML private Button rescanAllButton;
    @FXML private TabPane contentTabs;
    @FXML private TableView<FileStatus> fileStatusTable;
    @FXML private TableColumn<FileStatus, String> fileColStatus;
    @FXML private TableColumn<FileStatus, String> fileColPath;
    @FXML private TableColumn<FileStatus, String> fileColExt;
    @FXML private TableColumn<FileStatus, Number> fileColAdded;
    @FXML private TableColumn<FileStatus, Number> fileColRemoved;
    @FXML private TableView<LogEntry> logTable;
    @FXML private TableColumn<LogEntry, String> logColHash;
    @FXML private TableColumn<LogEntry, String> logColAuthor;
    @FXML private TableColumn<LogEntry, String> logColDate;
    @FXML private TableColumn<LogEntry, String> logColMessage;
    @FXML private Label repoLabel;
    @FXML private Label branchLabel;
    @FXML private Label taskStatusLabel;
    /** 主题单选组 + 三个主题项（与外层「设置」菜单绑定，ToggleGroup 保证互斥） */
    @FXML private ToggleGroup themeToggleGroup;
    @FXML private RadioMenuItem themeLightItem;
    @FXML private RadioMenuItem themeDarkItem;
    @FXML private RadioMenuItem themeSystemItem;
    /** 语言单选组 + 两个语言项 */
    @FXML private ToggleGroup languageToggleGroup;
    @FXML private RadioMenuItem languageZhItem;
    @FXML private RadioMenuItem languageEnItem;

    /** 当前打开的仓库路径 */
    private String currentRepoPath;

    /** 收藏仓库列表（侧边栏顶部） */
    private final ObservableList<RepoListItem> favorites = FXCollections.observableArrayList();
    /** 扫描到的仓库列表（侧边栏底部） */
    private final ObservableList<RepoListItem> repositories = FXCollections.observableArrayList();

    /** 仓库区段折叠状态：true = 展开（默认），false = 折叠 */
    private boolean repositoriesPaneExpanded = true;

    /** 收藏仓库路径 → 收藏对象（含 alias） 映射，用于快速判定 isFavorite 与 alias */
    private final Map<String, Favorite> favoriteIndex = new HashMap<>();
    /** 扫描根目录缓存，避免重复查询 */
    private final Map<String, RepoScanRoot> scanRootIndex = new HashMap<>();

    /** 共享 ContextMenu：避免快速右键时多个菜单实例叠加（修复 issue #1） */
    private final ContextMenu sharedContextMenu = new ContextMenu();

    /** 当前右键命中的仓库项（供菜单回调访问） */
    private RepoListItem contextMenuTargetItem;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化状态栏
        repoLabel.setText(I18nUtil.get("main.status.currentRepo") + ": -");
        branchLabel.setText(I18nUtil.get("main.status.currentBranch") + ": -");
        taskStatusLabel.setText(I18nUtil.get("main.status.ready"));

        // 初始化提交日志 TableView 列（Hash / Refs / Author / Date / Message）
        setupLogTableColumns();
        // 初始化文件状态 TableView 列（Status / Path / Extension / + / -）
        setupFileStatusTableColumns();

        // 初始化侧边栏：标题（含数量）+ CellFactory + 右键菜单
        updateSidebarTitles();
        addRootButton.setText(I18nUtil.get("sidebar.repositories.scanRoot"));
        rescanAllButton.setText(I18nUtil.get("sidebar.repositories.rescanAll"));

        favoritesList.setItems(favorites);
        favoritesList.setCellFactory(lv -> new RepoListCell(this::onToggleFavoriteFromCell));
        favoritesList.setPlaceholder(new Label(I18nUtil.get("sidebar.favorites.empty")));

        repositoriesList.setItems(repositories);
        repositoriesList.setCellFactory(lv -> new RepoListCell(this::onToggleFavoriteFromCell));
        repositoriesList.setPlaceholder(new Label(I18nUtil.get("sidebar.repositories.empty")));

        // 右键菜单：监听鼠标按下事件，命中仓库条目时弹出
        attachContextMenu(favoritesList);
        attachContextMenu(repositoriesList);

        // 单击切换仓库（修复 issue #3：左侧切换仓库后右侧未及时更换）
        // 注：收藏按钮自身 consume 鼠标事件，不会触发此处回调
        attachSingleClickOpen(favoritesList);
        attachSingleClickOpen(repositoriesList);

        // 加载索引并恢复侧边栏列表
        reloadFavoriteIndex();
        reloadScanRootIndex();

        // 订阅扫描结果完成事件，刷新仓库列表
        asyncTaskService.onTaskFinished(taskId -> {
            // 仅响应 MULTI_REPO_SCAN 任务完成事件
            Platform.runLater(this::refreshRepositoriesList);
        });

        // 应用启动时自动恢复列表：重新扫描全部已启用根目录
        repoScanRootService.rescanAll();
        refreshRepositoriesList();

        // 同步主题/语言 RadioMenuItem 选中状态（与持久化设置一致）
        syncThemeSelection();
        syncLanguageSelection();
    }

    /**
     * 根据 settingsService 中的 ui.theme 值，选中对应的主题 RadioMenuItem。
     * <p>RadioMenuItem 已通过 FXML 绑定到 {@link #themeToggleGroup}，选中一项会自动取消选中其它项。</p>
     */
    private void syncThemeSelection() {
        String theme = settingsService.get("ui.theme");
        RadioMenuItem target = switch (theme == null ? "" : theme) {
            case "LIGHT" -> themeLightItem;
            case "SYSTEM" -> themeSystemItem;
            default -> themeDarkItem;
        };
        target.setSelected(true);
    }

    /**
     * 根据 settingsService 中的 ui.language 值，选中对应的语言 RadioMenuItem。
     */
    private void syncLanguageSelection() {
        String language = settingsService.get("ui.language");
        RadioMenuItem target = "en".equals(language) ? languageEnItem : languageZhItem;
        target.setSelected(true);
    }

    /**
     * 设置主舞台（用于绑定主题）。
     *
     * @param stage 主舞台
     */
    public void setStage(Stage stage) {
        // 主题绑定在 MainAppLauncher 中完成
    }

    /**
     * 初始化文件状态 TableView 的列 cellValueFactory。
     * <p>对应 PRD 4.5 主窗口文件状态 Tab：Status / Path / Extension / + / - 五列。</p>
     */
    private void setupFileStatusTableColumns() {
        fileStatusTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        fileStatusTable.setPlaceholder(new Label(I18nUtil.get("file.status.empty")));

        // Status：状态文本（Modified / Untracked / ...），按 state 上色
        fileColStatus.setCellValueFactory(cd -> {
            FileStatus f = cd.getValue();
            return new javafx.beans.property.SimpleStringProperty(formatFileState(f));
        });
        fileColStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    FileStatus f = getTableRow() == null ? null : (FileStatus) getTableRow().getItem();
                    setStyle(stateStyle(f == null ? null : f.getState()));
                }
            }
        });

        // Path：相对路径
        fileColPath.setCellValueFactory(cd -> {
            FileStatus f = cd.getValue();
            return new javafx.beans.property.SimpleStringProperty(f == null ? "" : (f.getPath() == null ? "" : f.getPath()));
        });

        // Extension：文件扩展名
        fileColExt.setCellValueFactory(cd -> {
            FileStatus f = cd.getValue();
            String ext = "";
            if (f != null && f.getPath() != null) {
                int dot = f.getPath().lastIndexOf('.');
                if (dot > 0 && dot < f.getPath().length() - 1) {
                    ext = f.getPath().substring(dot + 1);
                }
            }
            return new javafx.beans.property.SimpleStringProperty(ext);
        });

        // +：新增行数
        fileColAdded.setCellValueFactory(cd -> {
            FileStatus f = cd.getValue();
            return new javafx.beans.property.SimpleIntegerProperty(f == null ? 0 : f.getAddedLines());
        });
        fileColAdded.setStyle("-fx-alignment: CENTER-RIGHT;");

        // -：删除行数
        fileColRemoved.setCellValueFactory(cd -> {
            FileStatus f = cd.getValue();
            return new javafx.beans.property.SimpleIntegerProperty(f == null ? 0 : f.getDeletedLines());
        });
        fileColRemoved.setStyle("-fx-alignment: CENTER-RIGHT;");
    }

    private String formatFileState(FileStatus f) {
        if (f == null || f.getState() == null) {
            return "";
        }
        return switch (f.getState()) {
            case MODIFIED -> I18nUtil.get("file.state.modified");
            case UNTRACKED -> I18nUtil.get("file.state.untracked");
            case DELETED -> I18nUtil.get("file.state.deleted");
            case STAGED -> I18nUtil.get("file.state.staged");
            case CONFLICT -> I18nUtil.get("file.state.conflict");
            case IGNORED -> I18nUtil.get("file.state.ignored");
            default -> f.getState().name();
        };
    }

    private String stateStyle(FileStatus.FileState s) {
        if (s == null) return "";
        return switch (s) {
            case MODIFIED -> "-fx-text-fill: #1976d2; -fx-font-weight: bold;";
            case UNTRACKED -> "-fx-text-fill: #388e3c;";
            case DELETED -> "-fx-text-fill: #d32f2f;";
            case STAGED -> "-fx-text-fill: #f57c00; -fx-font-weight: bold;";
            case CONFLICT -> "-fx-text-fill: #d32f2f; -fx-font-weight: bold;";
            default -> "";
        };
    }

    /**
     * 初始化提交日志 TableView 的列 cellValueFactory。
     * <p>对应 PRD 4.5 主窗口日志 Tab：Hash / Author / Date / Message 四列。</p>
     * <p>布局策略：Hash/Author/Date 三列为固定宽度（贴近内容，禁用拖拽改变宽度），
     * Message 列弹性占满剩余空间 + 超长 ellipsis。</p>
     */
    private void setupLogTableColumns() {
        // 使用 CONSTRAINED_RESIZE_POLICY：让 Message 列自动填满剩余空间，消除右侧空白区域
        logTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        logTable.setPlaceholder(new Label(I18nUtil.get("log.loading")));

        // ====== Hash 列：固定 86px = 7 字符（Consolas 13px）+ 左右内边距 22px ======
        logColHash.setMinWidth(86);
        logColHash.setPrefWidth(86);
        logColHash.setMaxWidth(86);
        logColHash.setResizable(false);
        logColHash.setStyle("-fx-alignment: CENTER-LEFT;");
        logColHash.setCellValueFactory(cd -> {
            LogEntry e = cd.getValue();
            String shortId = e == null || e.getShortId() == null ? "" : e.getShortId();
            return new javafx.beans.property.SimpleStringProperty(shortId);
        });
        logColHash.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    LogEntry e = getTableRow() == null ? null : (LogEntry) getTableRow().getItem();
                    if (e != null && e.getCommitId() != null) {
                        setTooltip(new Tooltip(e.getCommitId()));
                    }
                    setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace;");
                }
            }
        });

        // ====== Author 列：固定宽度 140，超长 ellipsis ======
        logColAuthor.setMinWidth(140);
        logColAuthor.setPrefWidth(140);
        logColAuthor.setMaxWidth(140);
        logColAuthor.setCellValueFactory(cd -> {
            LogEntry e = cd.getValue();
            String author = e == null || e.getAuthor() == null ? "" : e.getAuthor();
            return new javafx.beans.property.SimpleStringProperty(author);
        });
        logColAuthor.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    LogEntry e = getTableRow() == null ? null : (LogEntry) getTableRow().getItem();
                    if (e != null && e.getAuthorEmail() != null) {
                        setTooltip(new Tooltip(item + " <" + e.getAuthorEmail() + ">"));
                    }
                }
            }
        });

        // ====== Date 列：固定宽度 160，完整格式 yyyy-MM-dd HH:mm:ss ======
        java.time.format.DateTimeFormatter DATE_FMT = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        logColDate.setMinWidth(160);
        logColDate.setPrefWidth(160);
        logColDate.setMaxWidth(160);
        logColDate.setStyle("-fx-alignment: CENTER-LEFT;");
        logColDate.setCellValueFactory(cd -> {
            LogEntry e = cd.getValue();
            if (e == null || e.getCommitTime() == null) {
                return new javafx.beans.property.SimpleStringProperty("");
            }
            return new javafx.beans.property.SimpleStringProperty(e.getCommitTime().format(DATE_FMT));
        });
        // 显示完整时间（tooltip 给能看到秒数的入口）
        logColDate.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    LogEntry e = getTableRow() == null ? null : (LogEntry) getTableRow().getItem();
                    if (e != null && e.getCommitTime() != null) {
                        setTooltip(new Tooltip(e.getCommitTime()
                                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
                    }
                }
            }
        });

        // ====== Message 列：弹性空间 + 多行 ellipsis ======
        logColMessage.setMinWidth(150);
        logColMessage.setPrefWidth(400);
        logColMessage.setMaxWidth(Double.MAX_VALUE);  // 弹性宽度，填满剩余空间
        logColMessage.setCellValueFactory(cd -> {
            LogEntry e = cd.getValue();
            String msg = e == null || e.getMessage() == null ? "" : e.getMessage();
            int nl = msg.indexOf('\n');
            String firstLine = nl >= 0 ? msg.substring(0, nl) : msg;
            return new javafx.beans.property.SimpleStringProperty(firstLine);
        });
        logColMessage.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    LogEntry e = getTableRow() == null ? null : (LogEntry) getTableRow().getItem();
                    if (e != null && e.getMessage() != null && e.getMessage().contains("\n")) {
                        setTooltip(new Tooltip(e.getMessage()));
                    }
                }
            }
        });
    }

    /**
     * 设置当前仓库路径并刷新状态。
     *
     * @param repoPath 仓库路径
     */
    public void setCurrentRepo(String repoPath) {
        this.currentRepoPath = repoPath;
        repoLabel.setText(I18nUtil.get("main.status.currentRepo") + ": " + repoPath);
        refreshStatus();
    }

    /**
     * 刷新文件状态列表与分支信息。
     */
    public void refreshStatus() {
        if (currentRepoPath == null) {
            return;
        }
        // 异步加载，避免阻塞 UI 线程（BR-33），统一走 AsyncTaskService（BR-36/E2）
        AsyncUiLoader.submitRead(currentRepoPath, TaskType.STATUS, () -> {
            try {
                RepositoryMeta meta = repositoryService.refreshMeta(currentRepoPath);
                Platform.runLater(() -> {
                    branchLabel.setText(I18nUtil.get("main.status.currentBranch") + ": " + meta.getCurrentBranch());
                });
                List<FileStatus> files = statusService.getStatus(currentRepoPath, true, false);
                // 过滤掉 UNMODIFIED / null（与 CommitDialog 一致）
                List<FileStatus> filteredFiles = new java.util.ArrayList<>();
                for (FileStatus f : files) {
                    if (f.getState() != null
                            && f.getState() != FileStatus.FileState.UNMODIFIED
                            && f.getState() != FileStatus.FileState.IGNORED) {
                        filteredFiles.add(f);
                    }
                }
                // 排序：状态 → 路径
                java.util.Comparator<FileStatus> byState = java.util.Comparator.comparingInt(f -> {
                    if (f.getState() == null) return 99;
                    return switch (f.getState()) {
                        case CONFLICT -> 0;
                        case MODIFIED -> 1;
                        case STAGED -> 2;
                        case DELETED -> 3;
                        case UNTRACKED -> 4;
                        default -> 50;
                    };
                });
                java.util.Comparator<FileStatus> byPath = java.util.Comparator
                        .comparing(FileStatus::getPath, String.CASE_INSENSITIVE_ORDER);
                filteredFiles.sort(byState.thenComparing(byPath));
                ObservableList<FileStatus> items = FXCollections.observableArrayList(filteredFiles);
                Platform.runLater(() -> fileStatusTable.setItems(items));

                // 加载日志
                // 单页条数走常量配置（BR-18 分页加载），不再硬编码
                List<LogEntry> logs = statusService.getLog(currentRepoPath, 1, AppConfig.LOG_DEFAULT_PAGE_SIZE);
                ObservableList<LogEntry> logItems = FXCollections.observableArrayList(logs);
                Platform.runLater(() -> logTable.setItems(logItems));
            } catch (Exception e) {
                log.error("刷新状态失败", e);
                Platform.runLater(() -> showError(I18nUtil.get("error.gitExecutionFailed"), e.getMessage()));
            }
        });
    }

    /**
     * 选择根目录并触发自动检索。
     * <p>打开 DirectoryChooser 让用户选择任意目录（含 git 仓库或非 git 目录均可），</p>
     * <p>将该目录登记为扫描根目录，并异步扫描其下所有 git 仓库，扫描完成后自动列在侧边栏。</p>
     */
    @FXML
    public void onOpenRepository() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(I18nUtil.get("menu.file.open"));
        File selected = chooser.showDialog(getMainStage());
        if (selected == null) {
            return;
        }
        String rootPath = selected.getAbsolutePath();
        registerAndScanRoot(rootPath, null, AppConfig.DEFAULT_SCAN_DEPTH, true);
    }

    /**
     * 添加新的扫描根目录（侧边栏底部工具栏「+ 选择根目录」按钮）。
     */
    @FXML
    public void onAddScanRoot() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(I18nUtil.get("sidebar.repositories.scanRoot"));
        File selected = chooser.showDialog(getMainStage());
        if (selected == null) {
            return;
        }
        String rootPath = selected.getAbsolutePath();
        registerAndScanRoot(rootPath, null, AppConfig.DEFAULT_SCAN_DEPTH, false);
    }

    /**
     * 重新扫描全部已启用根目录（侧边栏底部工具栏「重新扫描全部」按钮）。
     */
    @FXML
    public void onRescanAll() {
        repoScanRootService.rescanAll();
        Platform.runLater(() -> {
            refreshRepositoriesList();
            taskStatusLabel.setText(I18nUtil.get("main.status.ready"));
        });
    }

    /**
     * 切换仓库区段的展开/折叠状态（侧边栏底部自定义折叠按钮）。
     * <p>折叠时隐藏 ListView 并取消其占用空间（{@code setManaged(false)}）；展开时恢复显示。</p>
     */
    @FXML
    public void onToggleRepositoriesPane() {
        repositoriesPaneExpanded = !repositoriesPaneExpanded;
        repositoriesList.setVisible(repositoriesPaneExpanded);
        repositoriesList.setManaged(repositoriesPaneExpanded);
        // 折叠→▶，展开→▼
        repositoriesToggleButton.setText(repositoriesPaneExpanded ? "▼" : "▶");
    }

    /**
     * 登记根目录并触发扫描。
     *
     * @param rootPath   根目录绝对路径
     * @param alias      别名（可空）
     * @param scanDepth  扫描深度
     * @param openFirstRepo 是否在扫描完成后自动打开第一个发现的仓库
     */
    private void registerAndScanRoot(String rootPath, String alias, int scanDepth, boolean openFirstRepo) {
        // 登记根目录（upsert），add() 内部会触发异步扫描
        try {
            repoScanRootService.add(rootPath, alias, scanDepth);
        } catch (GitGuiException e) {
            showError(I18nUtil.get("error.title"), e.getMessage());
            return;
        }
        reloadScanRootIndex();
        // 等待扫描完成（轮询 scanResults 变化），最多 30 秒
        final long deadline = System.currentTimeMillis() + 30_000L;
        AsyncUiLoader.submitRead(rootPath, TaskType.MULTI_REPO_SCAN, () -> {
            // 轮询 scanResults，等待扫描完成或超时
            try {
                List<RepositoryMeta> initial = new ArrayList<>(repositoryService.getScanResults());
                int initialSize = initial.size();
                while (System.currentTimeMillis() < deadline) {
                    Thread.sleep(300);
                    List<RepositoryMeta> current = repositoryService.getScanResults();
                    if (current.size() > initialSize) {
                        // 扫描有新进展，认为完成（粗略判断，实际以 taskRecord 为准）
                        break;
                    }
                    if (current.size() == initialSize) {
                        // 也可能没有新增，连续 2 次不变视为稳定
                        Thread.sleep(300);
                        List<RepositoryMeta> again = repositoryService.getScanResults();
                        if (again.size() == current.size()) {
                            break;
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Platform.runLater(() -> {
                refreshRepositoriesList();
                // 如果是「打开仓库」触发且要求自动打开第一个
                if (openFirstRepo) {
                    List<RepositoryMeta> results = repositoryService.getScanResults();
                    if (!results.isEmpty()) {
                        // 优先打开被选目录自身（如果它就是仓库），否则打开第一个子仓库
                        RepositoryMeta target = null;
                        for (RepositoryMeta m : results) {
                            if (rootPath.equals(m.getRepoPath())) {
                                target = m;
                                break;
                            }
                        }
                        if (target == null) {
                            target = results.get(0);
                        }
                        try {
                            RepositoryMeta meta = repositoryService.openRepository(target.getRepoPath());
                            setCurrentRepo(meta.getRepoPath());
                        } catch (Exception ex) {
                            log.warn("自动打开仓库失败：{}", target.getRepoPath(), ex);
                        }
                    } else {
                        showInfo(I18nUtil.get("sidebar.scan.completed").replace("{0}", "0"));
                    }
                }
            });
        });
    }

    /**
     * 刷新侧边栏底部仓库列表（从 RepositoryService.scanResults + RepoScanRoot + FavoriteIndex 组合）。
     */
    private void refreshRepositoriesList() {
        // 1. 重新加载收藏索引
        reloadFavoriteIndex();
        // 2. 加载所有扫描结果，按所属扫描根目录分组排序
        List<RepositoryMeta> metas = repositoryService.getScanResults();
        List<RepoListItem> items = new ArrayList<>();
        // rootPath → 该 root 下的仓库列表
        Map<String, List<RepositoryMeta>> grouped = new LinkedHashMap<>();
        // 未归属任何已知根目录的仓库单列一组
        List<RepositoryMeta> orphans = new ArrayList<>();
        for (RepositoryMeta m : metas) {
            String matchedRoot = findMatchedRoot(m.getRepoPath());
            if (matchedRoot != null) {
                grouped.computeIfAbsent(matchedRoot, k -> new ArrayList<>()).add(m);
            } else {
                orphans.add(m);
            }
        }
        // 排序：先按 rootPath，再按仓库名
        grouped.forEach((root, list) -> {
            list.sort(Comparator.comparing(m -> repoDisplayName(m.getRepoPath())));
            for (RepositoryMeta m : list) {
                Favorite fav = favoriteIndex.get(m.getRepoPath());
                items.add(new RepoListItem(m, root, fav != null, fav == null ? "" : fav.getAlias()));
            }
        });
        if (!orphans.isEmpty()) {
            orphans.sort(Comparator.comparing(m -> repoDisplayName(m.getRepoPath())));
            for (RepositoryMeta m : orphans) {
                Favorite fav = favoriteIndex.get(m.getRepoPath());
                items.add(new RepoListItem(m, null, fav != null, fav == null ? "" : fav.getAlias()));
            }
        }
        // 保留当前选中项：setAll 会同时清空 selection 和 focus，必须再补回去
        String prevRepoPath = currentRepoPath;
        String prevSelectedPath = selectedRepoPath();
        repositories.setAll(items);
        // 仓库列表变化 → 刷新侧边栏标题上的数量
        updateSidebarTitles();

        // 还原选中：先试当前打开的 repoPath，再 fallback 到刷新前的列表选中项
        if (prevRepoPath != null) {
            selectRepoByPath(repositoriesList, prevRepoPath);
        }
        if (repositoriesList.getSelectionModel().getSelectedItem() == null
                && prevSelectedPath != null) {
            selectRepoByPath(repositoriesList, prevSelectedPath);
        }

        // 3. 刷新收藏区
        refreshFavoritesList();
    }

    /**
     * 刷新收藏列表（去重，按 pinned 降序、sortOrder 升序、createdAt 降序）。
     */
    private void refreshFavoritesList() {
        reloadFavoriteIndex();
        List<Favorite> all = favoriteService.list();
        List<RepoListItem> items = new ArrayList<>();
        for (Favorite fav : all) {
            RepositoryMeta meta = repositoryService.getScanResults().stream()
                    .filter(m -> m.getRepoPath().equals(fav.getRepoPath()))
                    .findFirst()
                    .orElseGet(() -> {
                        // 收藏的仓库可能不在本次扫描结果中（被禁用 / 根目录已删除），构造一个轻量 meta 用于显示
                        return RepositoryMeta.builder()
                                .repoPath(fav.getRepoPath())
                                .currentBranch("")
                                .hasUncommittedChanges(false)
                                .build();
                    });
            items.add(new RepoListItem(meta, null, true, fav.getAlias()));
        }
        // 保留收藏列表中的当前选中项
        String prevFavPath = selectedFavoritePath();
        favorites.setAll(items);
        // 收藏列表变化 → 刷新侧边栏标题上的数量
        updateSidebarTitles();
        // 还原选中 + 焦点（refresh 可能来自扫描完成/收藏切换的回调）
        if (prevFavPath != null) {
            selectFavoriteByPath(prevFavPath);
        }
    }

    /**
     * 查找仓库路径隶属的扫描根目录（最长前缀匹配）。
     */
    private String findMatchedRoot(String repoPath) {
        String best = null;
        for (String root : scanRootIndex.keySet()) {
            if (repoPath.equals(root) || repoPath.startsWith(root + File.separator)) {
                if (best == null || root.length() > best.length()) {
                    best = root;
                }
            }
        }
        return best;
    }

    /**
     * 从完整路径提取目录名（用于排序）。
     */
    private static String repoDisplayName(String path) {
        if (path == null) return "";
        int winIdx = path.lastIndexOf('\\');
        int unixIdx = path.lastIndexOf('/');
        int idx = Math.max(winIdx, unixIdx);
        if (idx >= 0 && idx < path.length() - 1) {
            return path.substring(idx + 1).toLowerCase(Locale.ROOT);
        }
        return path.toLowerCase(Locale.ROOT);
    }

    /**
     * 重新加载收藏索引（favoriteIndex: repoPath → Favorite）。
     */
    private void reloadFavoriteIndex() {
        favoriteIndex.clear();
        for (Favorite fav : favoriteService.list()) {
            favoriteIndex.put(fav.getRepoPath(), fav);
        }
    }

    /**
     * 重新加载扫描根目录索引（scanRootIndex: rootPath → RepoScanRoot）。
     */
    private void reloadScanRootIndex() {
        scanRootIndex.clear();
        for (RepoScanRoot root : repoScanRootService.listAll()) {
            scanRootIndex.put(root.getRootPath(), root);
        }
    }

    /**
     * 给 ListView 附加右键上下文菜单。
     * <p>使用单例共享 {@link #sharedContextMenu}：每次右键重建菜单项，避免多个菜单实例叠加（修复 issue #1）。</p>
     * <p>每次右键触发流程：</p>
     * <ol>
     *   <li>先隐藏当前已显示的菜单（如果存在）</li>
     *   <li>记录当前右键命中的 {@link RepoListItem} 到 {@link #contextMenuTargetItem}</li>
     *   <li>重建菜单项并显示在鼠标位置</li>
     * </ol>
     *
     * @param listView 目标 ListView（收藏区 / 仓库区共用）
     */
    private void attachContextMenu(ListView<RepoListItem> listView) {
        // 共享 ContextMenu：键盘 ESC 立即关闭
        sharedContextMenu.setHideOnEscape(true);
        sharedContextMenu.setAutoHide(true);

        listView.setOnContextMenuRequested(event -> {
            // 根据鼠标坐标找出目标行（命中列表项则弹出菜单，否则关闭）
            RepoListItem item = pickItemAt(listView, event.getX(), event.getY());
            if (item == null) {
                sharedContextMenu.hide();
                return;
            }
            // 选中该行以便视觉一致
            listView.getSelectionModel().select(item);
            // 重建菜单项（每次重建避免状态污染，例：favorite 切换后旧 MenuItem 文案过时）
            rebuildRepoContextMenu(item);
            // 先隐藏再显示（修复 issue #1：快速连续右键时多个菜单实例叠加）
            sharedContextMenu.hide();
            // 显示在鼠标位置（使用 screen 坐标）
            sharedContextMenu.show(listView, event.getScreenX(), event.getScreenY());
            event.consume();
        });

        // 当用户点击菜单自身外部时强制隐藏，避免持续驻留
        listView.setOnMousePressed(event -> {
            // 仅在非右键时清理（右键由 setOnContextMenuRequested 处理）
            if (event.getButton() != MouseButton.SECONDARY) {
                sharedContextMenu.hide();
            }
        });
    }

    /**
     * 单击切换仓库（修复 issue #3：左侧切换仓库后右侧未及时更换；
     * 修复 issue #5：左击收藏列表项时总是打开第一个仓库）。
     * <p>主按钮单击（含单击、双击）即触发；星标按钮事件自身 consume 不会被冒泡触发此处。</p>
     * <p>不依赖 {@code getSelectedItem()}，因为自定义 cell 含 Button 子节点时，cell 的
     * {@code CellBehaviorBase} 更新 selectedIndex 与 ListView 的 {@code MOUSE_CLICKED} 回调之间
     * 存在时序差，会导致 getSelectedItem 仍指向之前的选中项。改为直接通过
     * {@code event.getTarget()} 找到被点击的 cell，从 cell.getItem() 取数据，最准确。</p>
     *
     * @param listView 目标 ListView
     */
    private void attachSingleClickOpen(ListView<RepoListItem> listView) {
        listView.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY) return;
            // 双重保障：事件被 consume（RepoListCell 星标按钮 setOnMouseClicked）或 target 在 Button 内 → 跳过
            if (event.isConsumed() || isInsideButton(event.getTarget())) return;

            sharedContextMenu.hide();
            // 通过事件 target 找到对应的 cell，从 cell.getItem() 取出当前被点击的 RepoListItem
            RepoListItem item = findItemFromTarget(event.getTarget(), listView);
            if (item != null) {
                openRepository(item.getRepoPath());
            }
        });
    }

    /**
     * 从事件 target 向上查找所属的 {@link ListCell}，返回该 cell 当前的 item。
     * <p>比 {@code selectionModel.getSelectedItem()} 更可靠：selection model 的
     * {@code selectedIndex} 在 {@code MOUSE_CLICKED} 触发的瞬间可能尚未同步更新
     * （特别是 cell 含子节点、cell 复用等场景），而 cell.getItem() 始终反映当前 cell
     * 正在渲染的真实数据。</p>
     *
     * @param target   事件的 target（最深节点）
     * @param listView 用于在未找到 cell 时回退的 ListView（兜底）
     * @return 命中的 cell 正在渲染的 item；未命中返回 null
     */
    @SuppressWarnings("unchecked")
    private RepoListItem findItemFromTarget(javafx.event.EventTarget target, ListView<RepoListItem> listView) {
        if (target instanceof javafx.scene.Node) {
            for (javafx.scene.Node cur = (javafx.scene.Node) target; cur != null; cur = cur.getParent()) {
                if (cur instanceof ListCell) {
                    Object cellItem = ((ListCell<?>) cur).getItem();
                    if (cellItem instanceof RepoListItem) {
                        return (RepoListItem) cellItem;
                    }
                }
            }
        }
        // 兜底：未找到 cell 时使用 selection model（理论上不应进入这里）
        return listView.getSelectionModel().getSelectedItem();
    }

    /**
     * 判断事件目标是否位于某个 Button 内部（包含 Button 自身或其 Text 子节点）。
     * <p>{@code event.getTarget()} 返回 {@code EventTarget}，需先判断是否为 {@code Node} 再遍历父链。</p>
     */
    private boolean isInsideButton(javafx.event.EventTarget target) {
        if (target == null) return false;
        if (!(target instanceof javafx.scene.Node)) return false;
        for (javafx.scene.Node cur = (javafx.scene.Node) target; cur != null; cur = cur.getParent()) {
            if (cur instanceof Button) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据 ListView 内的坐标挑选对应行（命中区域为 Cell 高度内即可）。
     *
     * @param listView 目标 ListView
     * @param x        ListView 局部坐标 X
     * @param y        ListView 局部坐标 Y
     * @return 命中行；未命中返回 null
     */
    private RepoListItem pickItemAt(ListView<RepoListItem> listView, double x, double y) {
        // ListView 内坐标 → 行索引
        int index = -1;
        if (y >= 0) {
            // 采用 floor 计算行索引（每行高度为固定 cell 高度）
            double cellHeight = listView.getFixedCellSize() > 0
                    ? listView.getFixedCellSize()
                    : 26.0;
            index = (int) Math.floor(y / cellHeight);
            if (index >= listView.getItems().size()) {
                index = -1;
            }
        }
        if (index >= 0 && index < listView.getItems().size()) {
            return listView.getItems().get(index);
        }
        return null;
    }

    /**
     * 获取仓库列表当前选中项的仓库路径，无选中时返回 null。
     */
    private String selectedRepoPath() {
        RepoListItem item = repositoriesList.getSelectionModel().getSelectedItem();
        return item == null ? null : item.getRepoPath();
    }

    /**
     * 获取收藏列表当前选中项的仓库路径，无选中时返回 null。
     */
    private String selectedFavoritePath() {
        RepoListItem item = favoritesList.getSelectionModel().getSelectedItem();
        return item == null ? null : item.getRepoPath();
    }

    /**
     * 按仓库路径选中 + 聚焦仓库列表中的项。
     * <p>修复扫描/异步回调导致的 selection + focus 丢失：{@code setAll()} 同时清空
     * selectionModel 和 focusModel，本方法在 setAll 之后调用以恢复用户原始焦点。</p>
     */
    private void selectRepoByPath(ListView<RepoListItem> listView, String repoPath) {
        if (repoPath == null) return;
        for (int i = 0; i < listView.getItems().size(); i++) {
            RepoListItem item = listView.getItems().get(i);
            if (item != null && repoPath.equals(item.getRepoPath())) {
                listView.getSelectionModel().select(i);
                listView.getFocusModel().focus(i);
                // scroll 一下保证可见（如果之前已被滚动到屏幕外）
                listView.scrollTo(i);
                return;
            }
        }
    }

    /**
     * 按仓库路径选中 + 聚焦收藏列表中的项。
     */
    private void selectFavoriteByPath(String repoPath) {
        selectRepoByPath(favoritesList, repoPath);
    }

    /**
     * 重建共享 ContextMenu 的菜单项（每次右键触发前调用，避免旧状态）。
     * <p>修复 issue #1：连续右键时菜单不会叠加；修复 issue #2：补充「切换分支」选项。</p>
     * <p>所有 MenuItem 的 action 在触发后立即显式 {@code sharedContextMenu.hide()}，确保即时关闭。</p>
     *
     * @param item 当前右键命中的仓库项
     */
    private void rebuildRepoContextMenu(RepoListItem item) {
        contextMenuTargetItem = item;
        sharedContextMenu.getItems().clear();
        String path = item.getRepoPath();

        // 提交 / 拉取 / 推送 / 获取
        MenuItem commitItem = new MenuItem(I18nUtil.get("sidebar.repo.contextMenu.commit"));
        commitItem.setOnAction(e -> {
            sharedContextMenu.hide();
            setCurrentRepo(path);
            onCommit();
        });
        sharedContextMenu.getItems().add(commitItem);

        MenuItem pullItem = new MenuItem(I18nUtil.get("sidebar.repo.contextMenu.pull"));
        pullItem.setOnAction(e -> {
            sharedContextMenu.hide();
            setCurrentRepo(path);
            onPull();
        });
        sharedContextMenu.getItems().add(pullItem);

        MenuItem pushItem = new MenuItem(I18nUtil.get("sidebar.repo.contextMenu.push"));
        pushItem.setOnAction(e -> {
            sharedContextMenu.hide();
            setCurrentRepo(path);
            onPush();
        });
        sharedContextMenu.getItems().add(pushItem);

        MenuItem fetchItem = new MenuItem(I18nUtil.get("sidebar.repo.contextMenu.fetch"));
        fetchItem.setOnAction(e -> {
            sharedContextMenu.hide();
            setCurrentRepo(path);
            onFetch();
        });
        sharedContextMenu.getItems().add(fetchItem);

        // 切换分支（修复 issue #2）
        MenuItem switchBranchItem = new MenuItem(I18nUtil.get("sidebar.repo.contextMenu.switchBranch"));
        switchBranchItem.setOnAction(e -> {
            sharedContextMenu.hide();
            setCurrentRepo(path);
            onSwitchBranch();
        });
        sharedContextMenu.getItems().add(switchBranchItem);

        sharedContextMenu.getItems().add(new SeparatorMenuItem());

        // 收藏 / 取消收藏
        MenuItem favoriteItem = new MenuItem(item.isFavorite()
                ? I18nUtil.get("sidebar.repo.unfavorite")
                : I18nUtil.get("sidebar.repo.favorite"));
        favoriteItem.setOnAction(e -> {
            sharedContextMenu.hide();
            toggleFavorite(item);
        });
        sharedContextMenu.getItems().add(favoriteItem);

        // 设置别名
        MenuItem aliasItem = new MenuItem(I18nUtil.get("sidebar.repo.contextMenu.renameAlias"));
        aliasItem.setOnAction(e -> {
            sharedContextMenu.hide();
            renameAlias(item);
        });
        sharedContextMenu.getItems().add(aliasItem);

        sharedContextMenu.getItems().add(new SeparatorMenuItem());

        // 修改远程 URL
        MenuItem remoteItem = new MenuItem(I18nUtil.get("sidebar.repo.contextMenu.remoteConfig"));
        remoteItem.setOnAction(e -> {
            sharedContextMenu.hide();
            setCurrentRepo(path);
            onRemoteConfig();
        });
        sharedContextMenu.getItems().add(remoteItem);

        // 在文件管理器中打开
        MenuItem openFolderItem = new MenuItem(I18nUtil.get("sidebar.repo.contextMenu.openInExplorer"));
        openFolderItem.setOnAction(e -> {
            sharedContextMenu.hide();
            openInFileManager(path);
        });
        sharedContextMenu.getItems().add(openFolderItem);

        // 复制路径
        MenuItem copyPathItem = new MenuItem(I18nUtil.get("sidebar.repo.contextMenu.copyPath"));
        copyPathItem.setOnAction(e -> {
            sharedContextMenu.hide();
            copyToClipboard(path);
        });
        sharedContextMenu.getItems().add(copyPathItem);

        // 刷新此仓库元信息（与工具栏「重新扫描全部」区分：仅刷新单个仓库的分支/HEAD/是否干净，不重新发现仓库）
        MenuItem refreshItem = new MenuItem(I18nUtil.get("sidebar.repo.contextMenu.refresh"));
        refreshItem.setOnAction(e -> {
            sharedContextMenu.hide();
            refreshSingleRepo(path);
        });
        sharedContextMenu.getItems().add(refreshItem);
    }

    /**
     * 切换收藏状态（来自右键菜单或 Cell 星标按钮）。
     * <p>取消收藏时直接从 ObservableList 移除条目（即时 UI 反馈）+ DB 删除；
     * 添加收藏时 DB 写入 + full refresh 收藏列表。</p>
     * <p>两个分支都必须同步更新仓库列表（repositories）中对应项的 favorite 状态，
     * 否则用户在仓库区点击 ☆ 添加收藏后，cell 的 item 仍为 favorite=false，
     * 再次点击 ⭐ 时会误走"添加收藏"分支，抛出"仓库已收藏"异常（issue #6）。</p>
     * <p>catch Throwable 而非 Exception：MyBatis-Plus 的 deleteById 在缺少 spring-core 时
     * 抛 NoClassDefFoundError（Error 子类），用 catch(Exception) 无法捕获，会被 JavaFX 吞掉
     * 导致 UI 无任何反馈（issue #6 真正根因）。</p>
     */
    private void toggleFavorite(RepoListItem item) {
        final String repoPath = item.getRepoPath();
        try {
            if (item.isFavorite()) {
                Favorite fav = favoriteIndex.get(repoPath);
                if (fav != null) {
                    favoriteService.remove(fav.getId());
                } else {
                    log.warn("收藏索引中找不到 repoPath={}，将全量重建索引", repoPath);
                }
                // 直接从收藏列表移除（即时 UI 反馈，不依赖 DB 刷新）
                favorites.removeIf(li -> repoPath.equals(li.getRepoPath()));
                // 仓库列表中该条目从 favorited → unfavorited 状态切换
                for (int i = 0; i < repositories.size(); i++) {
                    RepoListItem ri = repositories.get(i);
                    if (repoPath.equals(ri.getRepoPath())) {
                        repositories.set(i, ri.withFavorite(false, ""));
                        break;
                    }
                }
            } else {
                Favorite added = favoriteService.add(repoPath, "", "");
                // 添加收藏：完整刷新收藏列表（按 pinned/sortOrder/createdAt 排序）
                refreshFavoritesList();
                // 仓库列表中该条目从 unfavorited → favorited 状态切换（issue #6 修复关键点）
                // 必须在 refreshFavoritesList 之后执行，确保 favoriteIndex 已重建
                String alias = added != null ? added.getAlias() : "";
                for (int i = 0; i < repositories.size(); i++) {
                    RepoListItem ri = repositories.get(i);
                    if (repoPath.equals(ri.getRepoPath())) {
                        repositories.set(i, ri.withFavorite(true, alias));
                        break;
                    }
                }
            }
        } catch (Throwable ex) {
            log.error("收藏操作失败：repoPath={}", repoPath, ex);
            showError(I18nUtil.get("error.title"), ex.getMessage());
            showStatusError(I18nUtil.get("error.favoriteFailed") + "：" + ex.getMessage());
        } finally {
            reloadFavoriteIndex();
            updateSidebarTitles();
        }
    }

    /**
     * 来自 Cell 的星标点击事件。
     */
    private void onToggleFavoriteFromCell(RepoListItem item) {
        toggleFavorite(item);
    }

    /**
     * 弹出输入框重命名别名。
     */
    private void renameAlias(RepoListItem item) {
        TextInputDialog dialog = new TextInputDialog(item.getAlias());
        dialog.setTitle(I18nUtil.get("sidebar.alias.title"));
        dialog.setHeaderText(I18nUtil.get("sidebar.alias.prompt"));
        dialog.setContentText(item.getRepoPath());
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String newAlias = result.get().trim();
            try {
                if (item.isFavorite()) {
                    Favorite fav = favoriteIndex.get(item.getRepoPath());
                    if (fav != null) {
                        favoriteService.update(fav.getId(), newAlias, fav.getGroup());
                    }
                } else {
                    // 未收藏时直接收藏并写入别名
                    favoriteService.add(item.getRepoPath(), newAlias, "");
                }
                reloadFavoriteIndex();
                refreshRepositoriesList();
            } catch (GitGuiException ex) {
                showError(I18nUtil.get("error.title"), ex.getMessage());
            }
        }
    }

    /**
     * 在系统文件管理器中打开目录（Windows 资源管理器 / macOS Finder / Linux xdg-open）。
     */
    private void openInFileManager(String path) {
        try {
            File dir = new File(path);
            if (!dir.exists()) {
                showError(I18nUtil.get("error.title"), "目录不存在：" + path);
                return;
            }
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(dir);
            } else {
                // 兜底：Windows 直接调用 explorer
                if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
                    Runtime.getRuntime().exec(new String[]{"explorer.exe", path});
                } else {
                    showError(I18nUtil.get("error.title"), "当前系统不支持 Desktop.open");
                }
            }
        } catch (Exception ex) {
            log.error("打开文件管理器失败：{}", path, ex);
            showError(I18nUtil.get("error.title"), ex.getMessage());
        }
    }

    /**
     * 复制路径到剪贴板。
     */
    private void copyToClipboard(String text) {
        try {
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            Clipboard.getSystemClipboard().setContent(content);
        } catch (Exception ex) {
            log.warn("复制到剪贴板失败：{}", text, ex);
        }
    }

    /**
     * 刷新单个仓库的元信息（异步）。
     */
    private void refreshSingleRepo(String repoPath) {
        AsyncUiLoader.submitRead(repoPath, TaskType.STATUS, () -> {
            try {
                repositoryService.refreshMeta(repoPath);
                Platform.runLater(this::refreshRepositoriesList);
            } catch (Exception ex) {
                log.warn("刷新仓库元信息失败：{}", repoPath, ex);
                Platform.runLater(() -> showError(I18nUtil.get("error.gitExecutionFailed"), ex.getMessage()));
            }
        });
    }

    /**
     * 从侧边栏移除仓库（仅移除列表项，不删除本地文件）。
     * <p>实现思路：将该仓库所属的扫描根目录整组移除；如该仓库独立（不在任何根目录下），
     * 则从 RepositoryMeta 缓存移除对应记录。</p>
     */
    private void removeRepo(RepoListItem item) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18nUtil.get("sidebar.confirm.remove.title"));
        alert.setHeaderText(I18nUtil.get("sidebar.confirm.remove.message"));
        alert.setContentText(MessageFormat.format(
                I18nUtil.get("sidebar.confirm.remove.detail"), item.getRepoPath()));
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        try {
            // 若是收藏，先取消收藏
            if (item.isFavorite()) {
                Favorite fav = favoriteIndex.get(item.getRepoPath());
                if (fav != null) {
                    favoriteService.remove(fav.getId());
                }
            }
            // 若该仓库匹配某个扫描根目录 → 询问是否一并移除根目录（这里直接静默移除根目录）
            String rootPath = item.getScanRootPath();
            if (rootPath != null) {
                RepoScanRoot root = scanRootIndex.get(rootPath);
                if (root != null) {
                    repoScanRootService.remove(root.getId());
                    reloadScanRootIndex();
                }
            }
            refreshRepositoriesList();
        } catch (GitGuiException ex) {
            showError(I18nUtil.get("error.title"), ex.getMessage());
        }
    }

    /**
     * 打开指定路径的仓库（含最近仓库登记与 UI 刷新）。
     */
    private void openRepository(String repoPath) {
        try {
            RepositoryMeta meta = repositoryService.openRepository(repoPath);
            setCurrentRepo(meta.getRepoPath());
        } catch (GitGuiException ex) {
            showError(I18nUtil.get("error.repoNotGit"), ex.getMessage());
        }
    }

    /**
     * 克隆仓库（菜单：文件 → 克隆）。
     */
    @FXML
    public void onClone() {
        CloneDialog dialog = new CloneDialog(repositoryService);
        dialog.showAndWait();
        if (dialog.getClonedPath() != null) {
            setCurrentRepo(dialog.getClonedPath());
            // 克隆成功后将所在目录登记为扫描根目录
            File cloned = new File(dialog.getClonedPath());
            File parent = cloned.getParentFile();
            if (parent != null) {
                repoScanRootService.add(parent.getAbsolutePath(), "", AppConfig.DEFAULT_SCAN_DEPTH);
                reloadScanRootIndex();
                Platform.runLater(this::refreshRepositoriesList);
            }
        }
    }

    /**
     * 初始化仓库（菜单：文件 → 初始化）。
     */
    @FXML
    public void onInit() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(I18nUtil.get("menu.file.init"));
        File selected = chooser.showDialog(getMainStage());
        if (selected == null) {
            return;
        }
        repositoryService.initRepository(selected.getAbsolutePath(), false);
        // 初始化后登记所在根目录
        File parent = selected.getParentFile();
        if (parent != null) {
            repoScanRootService.add(parent.getAbsolutePath(), "", AppConfig.DEFAULT_SCAN_DEPTH);
            reloadScanRootIndex();
        }
        setCurrentRepo(selected.getAbsolutePath());
        Platform.runLater(this::refreshRepositoriesList);
    }

    /**
     * 提交（菜单：操作 → 提交）。
     */
    @FXML
    public void onCommit() {
        if (requireRepo()) {
            CommitDialog dialog = new CommitDialog(gitOperationService, statusService,
                    remoteConfigService, currentRepoPath);
            dialog.showAndWait();
            refreshStatus();
        }
    }

    /**
     * 拉取（菜单：操作 → 拉取）。
     */
    @FXML
    public void onPull() {
        if (requireRepo()) {
            PullDialog dialog = new PullDialog(gitOperationService, currentRepoPath);
            dialog.showAndWait();
            refreshStatus();
        }
    }

    /**
     * 推送（菜单：操作 → 推送）。
     */
    @FXML
    public void onPush() {
        if (requireRepo()) {
            PushDialog dialog = new PushDialog(gitOperationService, currentRepoPath);
            dialog.showAndWait();
            refreshStatus();
        }
    }

    /**
     * 获取（菜单：操作 → 获取）。
     * <p>直接调用 fetch 服务 + ProgressDialog 实时展示 git fetch 输出。</p>
     */
    @FXML
    public void onFetch() {
        if (requireRepo()) {
            Stage owner = getMainStage();
            ProgressDialog progress = new ProgressDialog(
                    owner,
                    "获取 Fetch  →  origin",
                    I18nUtil.get("progress.headerOperating")
            );
            ProgressCallback sharedCb = progress.asCallback();
            try {
                TaskHandle handle = gitOperationService.fetch(currentRepoPath, "origin", "", true, sharedCb);
                progress.attach(handle);
                progress.showAndWaitForTask();
            } catch (Exception e) {
                log.error("获取失败", e);
                new Alert(Alert.AlertType.ERROR, "获取失败：" + e.getMessage()).showAndWait();
            }
            refreshStatus();
        }
    }

    /**
     * 切换分支（右键菜单 + 操作 → 切换分支共用入口）。
     * <p>打开 SwitchDialog 让用户选择/创建分支，切换成功后刷新状态面板。</p>
     */
    @FXML
    public void onSwitchBranch() {
        if (requireRepo()) {
            SwitchDialog dialog = new SwitchDialog(gitOperationService, statusService, currentRepoPath);
            dialog.showAndWait();
            // 切换成功后刷新状态栏、列表（当前分支可能变化）
            refreshStatus();
            refreshRepositoriesList();
        }
    }

    /**
     * 远程配置（操作 → 远程 URL 菜单共用入口 + 右键仓库菜单「修改远程 URL」共用）。
     * <p>打开 RemoteConfigDialog 让用户新增 / 修改 / 删除 / 重命名仓库的远程配置。</p>
     */
    @FXML
    public void onRemoteConfig() {
        if (requireRepo()) {
            RemoteConfigDialog dialog = new RemoteConfigDialog(remoteConfigService, currentRepoPath);
            dialog.showAndWait();
        }
    }

    /**
     * 切换浅色主题。
     */
    @FXML
    public void onThemeLight() {
        themeManager.applyTheme("LIGHT");
        settingsService.set("ui.theme", "LIGHT");
    }

    /**
     * 切换深色主题。
     */
    @FXML
    public void onThemeDark() {
        themeManager.applyTheme("DARK");
        settingsService.set("ui.theme", "DARK");
    }

    /**
     * 切换跟随系统主题。
     */
    @FXML
    public void onThemeSystem() {
        themeManager.applyTheme("SYSTEM");
        settingsService.set("ui.theme", "SYSTEM");
    }

    /**
     * 切换中文。
     */
    @FXML
    public void onLanguageZh() {
        I18nUtil.switchLanguage("zh");
        settingsService.set("ui.language", "zh");
        // 刷新侧边栏 i18n 文案
        applyI18n();
    }

    /**
     * 切换英文。
     */
    @FXML
    public void onLanguageEn() {
        I18nUtil.switchLanguage("en");
        settingsService.set("ui.language", "en");
        // 刷新侧边栏 i18n 文案
        applyI18n();
    }

    /**
     * 重新应用 i18n 文案（语言切换后调用）。
     */
    private void applyI18n() {
        // i18n 切换时重新设置所有侧边栏文案 + 数量
        updateSidebarTitles();
        addRootButton.setText(I18nUtil.get("sidebar.repositories.scanRoot"));
        rescanAllButton.setText(I18nUtil.get("sidebar.repositories.rescanAll"));
        favoritesList.setPlaceholder(new Label(I18nUtil.get("sidebar.favorites.empty")));
        repositoriesList.setPlaceholder(new Label(I18nUtil.get("sidebar.repositories.empty")));
    }

    /**
     * 更新两个侧边栏标题（含仓库数量）：
     * <pre>
     * ⭐ 收藏 (3)
     * 📁 仓库 (12)
     * </pre>
     * <p>每次数据刷新或语言切换时调用，确保标题上的数量与 {@link #favorites} / {@link #repositories} ObservableList 同步。</p>
     */
    private void updateSidebarTitles() {
        String favTitle = I18nUtil.get("sidebar.favorites") + " (" + favorites.size() + ")";
        String repoTitle = I18nUtil.get("sidebar.repositories") + " (" + repositories.size() + ")";
        favoritesPane.setText(favTitle);
        repositoriesTitleLabel.setText(repoTitle);
    }

    /**
     * 打开执行红线设置对话框（仅命令红线配置；界面/外部工具已迁移到外层菜单独立入口）。
     */
    @FXML
    public void onSettings() {
        SettingsDialog dialog = new SettingsDialog(settingsService);
        dialog.showAndWait();
    }

    /**
     * 打开扩展工具配置对话框（Diff 工具 / 合并工具 / 外部编辑器）。
     * <p>原属 SettingsDialog 的「外部工具」Tab，现迁移为独立入口，挂靠在「设置」顶级菜单下。</p>
     */
    @FXML
    public void onExternalTools() {
        ExternalToolsDialog dialog = new ExternalToolsDialog(settingsService);
        dialog.showAndWait();
    }

    /**
     * 关于对话框。
     */
    @FXML
    public void onAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18nUtil.get("menu.help.about"));
        alert.setHeaderText(I18nUtil.get("about.version"));
        alert.setContentText(I18nUtil.get("about.description"));
        alert.showAndWait();
    }

    /**
     * 退出应用。
     */
    @FXML
    public void onExit() {
        Platform.exit();
    }

    /**
     * 检查是否已打开仓库。
     *
     * @return true 表示已打开
     */
    private boolean requireRepo() {
        if (currentRepoPath == null) {
            showError(I18nUtil.get("error.title"), I18nUtil.get("error.repoNotOpened"));
            return false;
        }
        return true;
    }

    /**
     * 显示错误对话框。
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 显示信息对话框。
     */
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18nUtil.get("main.status.ready"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 在状态栏显示错误消息（非阻塞），并自动 5 秒后清除。
     * <p>用于后端异常即时反馈，避免阻塞用户操作；如需用户确认可再叠加 {@link #showError}。</p>
     *
     * @param message 错误消息
     */
    private void showStatusError(String message) {
        taskStatusLabel.setStyle("-fx-text-fill: #d9534f;");
        taskStatusLabel.setText("⚠ " + message);
        // 5 秒后自动清除（恢复就绪样式）
        javafx.animation.PauseTransition delay =
                new javafx.animation.PauseTransition(javafx.util.Duration.seconds(5));
        delay.setOnFinished(e -> {
            taskStatusLabel.setStyle("");
            taskStatusLabel.setText(I18nUtil.get("main.status.ready"));
        });
        delay.play();
    }

    private Stage getMainStage() {
        return menuBar.getScene() == null ? null
                : (Stage) menuBar.getScene().getWindow();
    }
}
