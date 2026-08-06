package com.gitgui.ui.dialog;

import com.gitgui.domain.constant.TaskType;
import com.gitgui.domain.model.LogEntry;
import com.gitgui.domain.model.RefInfo;
import com.gitgui.domain.model.request.CheckoutRequest;
import com.gitgui.domain.service.GitOperationService;
import com.gitgui.domain.service.StatusService;
import com.gitgui.ui.AsyncUiLoader;
import com.gitgui.ui.i18n.I18nUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 切换/Checkout 对话框（TortoiseGit 风格）
 * <p>参照 TortoiseGit Switch/Checkout 对话框布局：</p>
 * <pre>
 * ┌─ &lt;repoPath&gt; - Switch/Checkout ─────────────────────────┐
 * │ Switch To                                                 │
 * │   ○ Branch     [main                       ▼] [..]       │
 * │   ○ Tag        [                            ▼] [..]      │
 * │   ○ Commit     [                            ▼] [..]      │
 * │                                                          │
 * │ Option                                                    │
 * │   ☐ Create New Branch        [Branch_main      ]         │
 * │   ☐ Overwrite working tree changes (force)  ☐ Merge      │
 * │   ☐ Track                                                 │
 * │   ☐ Override branch if exists                              │
 * │                                                          │
 * │                                  [OK]  [Cancel]  [Help]   │
 * └──────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p>三单选（Branch / Tag / Commit）互斥，激活对应 ComboBox；</p>
 * <p>每个 ComboBox 旁有「...」按钮，点击弹出 {@link BrowseReferencesDialog} 让用户从 refs 树中挑选；</p>
 * <p>支持 TortoiseGit 风格的完整 Option：创建新分支、强制切换、跟踪、覆盖分支、merge。</p>
 *
 * @author FrankKang
 * @since 2026-07-24
 */
public class SwitchDialog extends Dialog<Void> {

    private static final Logger log = LoggerFactory.getLogger(SwitchDialog.class);

    private final GitOperationService gitOperationService;
    private final StatusService statusService;
    private final String repoPath;

    // ===== Switch To =====
    private final ToggleGroup switchToGroup = new ToggleGroup();
    private final RadioButton branchRadio = new RadioButton(I18nUtil.get("switch.target.branch"));
    private final RadioButton tagRadio = new RadioButton(I18nUtil.get("switch.target.tag"));
    private final RadioButton commitRadio = new RadioButton(I18nUtil.get("switch.target.commit"));

    private final ComboBox<String> branchCombo = new ComboBox<>();
    private final ComboBox<String> tagCombo = new ComboBox<>();
    private final ComboBox<CommitItem> commitCombo = new ComboBox<>();
    private final Button branchBrowse = new Button("...");
    private final Button tagBrowse = new Button("...");
    private final Button commitBrowse = new Button("...");

    // ===== Option =====
    private final CheckBox createCheck = new CheckBox(I18nUtil.get("switch.option.create"));
    private final TextField newBranchField = new TextField();
    private final CheckBox forceCheck = new CheckBox(I18nUtil.get("switch.option.force"));
    private final CheckBox mergeCheck = new CheckBox(I18nUtil.get("switch.option.merge"));
    private final CheckBox trackCheck = new CheckBox(I18nUtil.get("switch.option.track"));
    private final CheckBox overrideCheck = new CheckBox(I18nUtil.get("switch.option.override"));
    /**
     * branch combo 数据源（含本地 + 远程，分组显示）
     */
    private final ObservableList<String> branches = FXCollections.observableArrayList();
    /**
     * tag combo 数据源
     */
    private final ObservableList<String> tags = FXCollections.observableArrayList();
    /**
     * commit combo 数据源
     */
    private final ObservableList<CommitItem> commits = FXCollections.observableArrayList();
    /**
     * 当前分支名（用于显示默认 placeholder）
     */
    private String currentBranch = "";

    public SwitchDialog(GitOperationService gitOperationService, StatusService statusService, String repoPath) {
        this.gitOperationService = gitOperationService;
        this.statusService = statusService;
        this.repoPath = repoPath;
        setTitle(repoPath + " - " + I18nUtil.get("switch.title"));
        setHeaderText(null);
        initModality(Modality.APPLICATION_MODAL);

        DialogPane pane = getDialogPane();
        pane.setContent(buildContent());
        pane.setPrefSize(620, 540);

        ButtonType okType = new ButtonType(I18nUtil.get("button.ok"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType(I18nUtil.get("button.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType helpType = new ButtonType(I18nUtil.get("button.help"), ButtonBar.ButtonData.HELP);
        pane.getButtonTypes()
            .addAll(okType, cancelType, helpType);

        Button okButton = (Button) pane.lookupButton(okType);
        if (okButton != null) {
            okButton.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
                e.consume();
                doCheckout();
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

        setResultConverter(buttonType -> null);

        // 异步加载分支 / tag / commit
        loadAllData();
    }

    /**
     * 规范化分支名：剥离 refs/heads/ 与 refs/remotes/<remote>/ 前缀。
     */
    private static String normalizeBranch(String raw) {
        if (raw == null) {
            return "";
        }
        if (raw.startsWith("refs/heads/")) {
            return raw.substring("refs/heads/".length());
        }
        if (raw.startsWith("refs/remotes/")) {
            String rest = raw.substring("refs/remotes/".length());
            int slash = rest.indexOf('/');
            return slash > 0 ? rest.substring(slash + 1) : rest;
        }
        return raw;
    }

    /**
     * 规范化 tag：剥离 refs/tags/ 前缀。
     */
    private static String normalizeTag(String raw) {
        if (raw == null) {
            return "";
        }
        if (raw.startsWith("refs/tags/")) {
            return raw.substring("refs/tags/".length());
        }
        return raw;
    }

    /**
     * 判断是否为远程分支（含 / 字符，如 origin/main）。
     */
    private static boolean isRemoteBranch(String name) {
        return name.contains("/");
    }

    /**
     * 取 message 第一行。
     */
    private static String firstLine(String message) {
        if (message == null) {
            return "";
        }
        int nl = message.indexOf('\n');
        return nl >= 0 ? message.substring(0, nl) : message;
    }

    /**
     * 构建对话框内容。
     */
    private VBox buildContent() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(12));
        root.setPrefWidth(600);

        // ===== Switch To =====
        TitledPane switchToPane = new TitledPane();
        switchToPane.setText(I18nUtil.get("switch.section.switchTo"));
        switchToPane.setCollapsible(false);
        switchToPane.setContent(buildSwitchToBox());
        VBox.setVgrow(switchToPane, Priority.ALWAYS);

        // ===== Option =====
        TitledPane optionPane = new TitledPane();
        optionPane.setText(I18nUtil.get("switch.section.option"));
        optionPane.setCollapsible(false);
        optionPane.setContent(buildOptionBox());

        root.getChildren()
            .addAll(switchToPane, optionPane);
        return root;
    }

    /**
     * 构建「Switch To」三行区域（Branch / Tag / Commit 单选 + ComboBox + 浏览按钮）。
     */
    private GridPane buildSwitchToBox() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(8));

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(20);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(70);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(10);
        grid.getColumnConstraints()
            .addAll(col1, col2, col3);

        // Row 0: Branch
        branchRadio.setToggleGroup(switchToGroup);
        branchRadio.setUserData("BRANCH");
        branchCombo.setEditable(true);
        branchCombo.setItems(branches);
        branchCombo.setMaxWidth(Double.MAX_VALUE);
        branchBrowse.setOnAction(e -> openBrowser("BRANCH"));
        GridPane.setHgrow(branchCombo, Priority.ALWAYS);
        grid.add(branchRadio, 0, 0);
        grid.add(branchCombo, 1, 0);
        grid.add(branchBrowse, 2, 0);

        // Row 1: Tag
        tagRadio.setToggleGroup(switchToGroup);
        tagRadio.setUserData("TAG");
        tagCombo.setEditable(true);
        tagCombo.setItems(tags);
        tagCombo.setMaxWidth(Double.MAX_VALUE);
        tagBrowse.setOnAction(e -> openBrowser("TAG"));
        GridPane.setHgrow(tagCombo, Priority.ALWAYS);
        grid.add(tagRadio, 0, 1);
        grid.add(tagCombo, 1, 1);
        grid.add(tagBrowse, 2, 1);

        // Row 2: Commit
        commitRadio.setToggleGroup(switchToGroup);
        commitRadio.setUserData("COMMIT");
        commitCombo.setEditable(true);
        commitCombo.setItems(commits);
        commitCombo.setMaxWidth(Double.MAX_VALUE);
        commitCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(CommitItem c) {
                return c == null ? "" : c.toDisplayString();
            }

            @Override
            public CommitItem fromString(String s) {
                // 用户手动输入 → 构造临时 CommitItem（仅含 commitId 字段）
                return s == null || s.isBlank() ? null : new CommitItem(s, "", "", "");
            }
        });
        commitBrowse.setOnAction(e -> openBrowser("COMMIT"));
        GridPane.setHgrow(commitCombo, Priority.ALWAYS);
        grid.add(commitRadio, 0, 2);
        grid.add(commitCombo, 1, 2);
        grid.add(commitBrowse, 2, 2);

        // 默认选中 Branch
        branchRadio.setSelected(true);

        // 单选变化 → 启用对应 ComboBox，其他置灰
        switchToGroup.selectedToggleProperty()
                     .addListener((obs, o, n) -> updateComboEnableState());
        updateComboEnableState();

        return grid;
    }

    /**
     * 根据当前单选启用对应 ComboBox，其他置灰。
     */
    private void updateComboEnableState() {
        String sel = switchToGroup.getSelectedToggle() == null ? "BRANCH" : (String) switchToGroup.getSelectedToggle()
                                                                                                  .getUserData();
        branchCombo.setDisable(!"BRANCH".equals(sel));
        branchBrowse.setDisable(!"BRANCH".equals(sel));
        tagCombo.setDisable(!"TAG".equals(sel));
        tagBrowse.setDisable(!"TAG".equals(sel));
        commitCombo.setDisable(!"COMMIT".equals(sel));
        commitBrowse.setDisable(!"COMMIT".equals(sel));
    }

    /**
     * 构建 Option 区域（TortoiseGit 风格复选框）。
     */
    private GridPane buildOptionBox() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(8));

        // Row 0: Create New Branch + 新分支名输入框
        newBranchField.setDisable(true);
        newBranchField.setPromptText(I18nUtil.get("switch.option.newBranchPrompt"));
        HBox createRow = new HBox(8, createCheck, newBranchField);
        createRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(newBranchField, Priority.ALWAYS);
        newBranchField.setMaxWidth(Double.MAX_VALUE);
        grid.add(createRow, 0, 0, 2, 1);

        // Row 1: Overwrite working tree changes + Merge
        HBox forceRow = new HBox(20, forceCheck, mergeCheck);
        forceRow.setAlignment(Pos.CENTER_LEFT);
        grid.add(forceRow, 0, 1, 2, 1);

        // Row 2: Track
        grid.add(trackCheck, 0, 2, 2, 1);

        // Row 3: Override branch if exists
        grid.add(overrideCheck, 0, 3, 2, 1);

        // Option 联动：
        //  - 勾选 create → 启用 newBranchField，自动聚焦输入框；禁用 override、track
        //  - 勾选 override → 启用 newBranchField；禁用 create、track
        //  - 勾选 track → 仅在 BRANCH 模式下可用；disable force
        //  - commit / tag 模式下 disable create / track / override
        createCheck.selectedProperty()
                   .addListener((obs, o, n) -> {
                       if (n) {
                           newBranchField.setDisable(false);
                           overrideCheck.setSelected(false);
                           trackCheck.setSelected(false);
                           Platform.runLater(newBranchField::requestFocus);
                           // 默认填充 Branch_<当前分支名> 提示
                           if (newBranchField.getText() == null || newBranchField.getText()
                                                                                 .isEmpty()) {
                               newBranchField.setText(I18nUtil.get("switch.option.newBranchPrefix") + currentBranch);
                           }
                       } else {
                           newBranchField.setDisable(true);
                       }
                   });
        overrideCheck.selectedProperty()
                     .addListener((obs, o, n) -> {
                         if (n) {
                             newBranchField.setDisable(false);
                             createCheck.setSelected(false);
                             trackCheck.setSelected(false);
                             Platform.runLater(newBranchField::requestFocus);
                             if (newBranchField.getText() == null || newBranchField.getText()
                                                                                   .isEmpty()) {
                                 newBranchField.setText(I18nUtil.get("switch.option.newBranchPrefix") + currentBranch);
                             }
                         }
                     });
        trackCheck.selectedProperty()
                  .addListener((obs, o, n) -> {
                      if (n) {
                          forceCheck.setSelected(false);
                          createCheck.setSelected(false);
                          overrideCheck.setSelected(false);
                      }
                  });
        // Radio 切换时联动
        switchToGroup.selectedToggleProperty()
                     .addListener((obs, o, n) -> {
                         String sel = (String) (n == null ? null : n.getUserData());
                         boolean branchOnly = "BRANCH".equals(sel);
                         createCheck.setDisable(!branchOnly);
                         overrideCheck.setDisable(!branchOnly);
                         trackCheck.setDisable(!branchOnly);
                         if (!branchOnly) {
                             createCheck.setSelected(false);
                             overrideCheck.setSelected(false);
                             trackCheck.setSelected(false);
                         }
                     });

        return grid;
    }

    /**
     * 打开 refs / 日志浏览对话框并回填到对应 ComboBox。
     * <p>Branch / Tag 模式弹出 {@link BrowseReferencesDialog}；Commit 模式弹出 {@link LogMessagesDialog}。</p>
     * <p>接收 RefInfo（携带完整 ref 名），把完整路径存入 CheckoutRequest.refName
     * 以便 Service 层正确解析远程分支 ref（避免 Ref cannot be resolved 错误）。</p>
     */
    private void openBrowser(String targetType) {
        if ("COMMIT".equals(targetType)) {
            // Commit 模式：使用 LogMessagesDialog（支持日期/作者过滤 + 文件变更详情）
            LogMessagesDialog logDialog = new LogMessagesDialog(statusService, repoPath);
            Optional<LogEntry> result = logDialog.showAndWait();
            if (result.isPresent() && result.get() != null) {
                LogEntry entry = result.get();
                String fullSha = entry.getCommitId() == null ? "" : entry.getCommitId();
                String shortId = entry.getShortId() == null ? (fullSha.length() > 8 ? fullSha.substring(0, 8) : fullSha) : entry.getShortId();

                // 1. 回填完整 commitId 到输入框（不截断）
                commitCombo.getEditor()
                           .setText(fullSha);

                // 2. 同时构造 RefInfo 携带完整 commitId，让 Service 层能正确 checkout
                RefInfo refInfo = RefInfo.builder()
                                         .refName(fullSha)
                                         .displayName(fullSha)
                                         .kind("COMMIT")
                                         .build();
                commitCombo.setUserData(refInfo);
                commitRadio.setSelected(true);

                // 3. 自动勾选「Create New Branch」并预填分支名（TortoiseGit 默认行为：Branch_<7位shortId>）
                createCheck.setSelected(true);
                newBranchField.setText(I18nUtil.get("switch.option.newBranchPrefix") + shortId);
                // newBranchField 在 createCheck 联动中已 enabled
                Platform.runLater(newBranchField::requestFocus);
            }
            return;
        }

        // Branch / Tag 模式：使用 BrowseReferencesDialog
        BrowseReferencesDialog browser = new BrowseReferencesDialog(statusService, repoPath);
        Optional<RefInfo> result = browser.showAndWait();
        if (result.isPresent() && result.get() != null) {
            RefInfo refInfo = result.get();
            switch (targetType) {
                case "BRANCH":
                    // 把 refInfo 暂存到 combo 的 userData，doCheckout 时取出构造完整 refName
                    branchCombo.getSelectionModel()
                               .select(refInfo.getDisplayName());
                    branchCombo.setUserData(refInfo);
                    branchRadio.setSelected(true);
                    break;
                case "TAG":
                    tagCombo.getSelectionModel()
                            .select(refInfo.getDisplayName());
                    tagCombo.setUserData(refInfo);
                    tagRadio.setSelected(true);
                    break;
            }
        }
    }

    /**
     * 从 ComboBox 的 userData 中取出 RefInfo（用户从 BrowseReferencesDialog 选中的 ref）。
     * 若 userData 为 null，说明用户直接在 ComboBox 手动键入了值，回退到 branch/tag/commitId 字段。
     */
    private RefInfo getSelectedRefInfo(javafx.scene.control.ComboBox<?> combo) {
        Object data = combo.getUserData();
        if (data instanceof RefInfo) {
            return (RefInfo) data;
        }
        return null;
    }

    /**
     * 异步加载分支 / tag（并行互不阻塞）；commit 延迟加载，仅当 Commit 单选被选中时按需触发。
     * <p>原实现串行调用 listBranches → listTags → listRecentCommits，若 listRecentCommits 在大仓库
     * 耗时较长会导致 branches/tags 列表也卡住不显示。重构后三类数据各自独立异步加载，互不阻塞。</p>
     */
    private void loadAllData() {
        // 任务 1：加载分支
        AsyncUiLoader.submitRead(repoPath, TaskType.STATUS, () -> {
            try {
                List<String> raw = statusService.listBranches(repoPath);
                String current = statusService.getCurrentBranch(repoPath);
                List<String> norm = new ArrayList<>();
                for (String b : raw) {
                    norm.add(normalizeBranch(b));
                }
                norm.sort(Comparator.<String>comparingInt(b -> isRemoteBranch(b) ? 1 : 0)
                                    .thenComparing(b -> b.toLowerCase(java.util.Locale.ROOT)));
                Platform.runLater(() -> {
                    currentBranch = current == null ? "" : current.replace("refs/heads/", "");
                    branches.setAll(norm);
                    if (!currentBranch.isEmpty()) {
                        branchCombo.getSelectionModel()
                                   .select(currentBranch);
                    } else if (!norm.isEmpty()) {
                        branchCombo.getSelectionModel()
                                   .select(0);
                    }
                });
            } catch (Exception e) {
                log.error("加载分支列表失败：{}", repoPath, e);
                Platform.runLater(() -> branches.setAll(java.util.Collections.emptyList()));
            }
        });

        // 任务 2：加载 tag（独立异步，与分支并行）
        AsyncUiLoader.submitRead(repoPath, TaskType.STATUS, () -> {
            try {
                List<String> raw = statusService.listTags(repoPath);
                List<String> norm = new ArrayList<>();
                for (String t : raw) {
                    norm.add(normalizeTag(t));
                }
                norm.sort(String.CASE_INSENSITIVE_ORDER);
                Platform.runLater(() -> tags.setAll(norm));
            } catch (Exception e) {
                log.error("加载 tag 列表失败：{}", repoPath, e);
                Platform.runLater(() -> tags.setAll(java.util.Collections.emptyList()));
            }
        });

        // 任务 3：commit 延迟加载（首次不加载，等用户点击 Commit 单选时再触发）
        commitRadio.selectedProperty()
                   .addListener((obs, wasSelected, nowSelected) -> {
                       if (nowSelected && commits.isEmpty()) {
                           loadCommits();
                       }
                   });
    }

    /**
     * 异步加载 commit 列表（仅当用户首次选中 Commit 单选时触发）。
     * <p>大仓库 commit 历史可能很长，使用 200 条上限以平衡显示效果与性能。</p>
     */
    private void loadCommits() {
        AsyncUiLoader.submitRead(repoPath, TaskType.STATUS, () -> {
            try {
                List<LogEntry> recent = statusService.listRecentCommits(repoPath, 200);
                List<CommitItem> items = new ArrayList<>();
                for (LogEntry le : recent) {
                    items.add(new CommitItem(le.getShortId() == null ? "" : le.getShortId(), le.getAuthor() == null ? "" : le.getAuthor(),
                                             le.getMessage() == null ? "" : firstLine(le.getMessage()),
                                             le.getCommitId() == null ? "" : le.getCommitId()));
                }
                Platform.runLater(() -> commits.setAll(items));
            } catch (Exception e) {
                log.error("加载 commit 列表失败：{}", repoPath, e);
                Platform.runLater(() -> commits.setAll(java.util.Collections.emptyList()));
            }
        });
    }

    /**
     * 执行切换。
     */
    private void doCheckout() {
        CheckoutRequest.CheckoutRequestBuilder builder = CheckoutRequest.builder()
                                                                        .repoPath(repoPath);

        // 1. 目标类型
        if (branchRadio.isSelected()) {
            String branch = branchCombo.getEditor()
                                       .getText();
            if (branch == null || branch.isBlank()) {
                showWarn(I18nUtil.get("switch.selectBranchRequired"));
                return;
            }
            // 优先取 RefInfo 携带的完整 ref 名（来自 BrowseReferencesDialog）
            RefInfo refInfo = getSelectedRefInfo(branchCombo);
            if (refInfo != null && refInfo.getRefName() != null && !refInfo.getRefName()
                                                                           .isBlank()) {
                builder.refName(refInfo.getRefName());
            }
            builder.targetType(CheckoutRequest.TargetType.BRANCH)
                   .branch(branch.trim());
        } else if (tagRadio.isSelected()) {
            String tag = tagCombo.getEditor()
                                 .getText();
            if (tag == null || tag.isBlank()) {
                showWarn(I18nUtil.get("switch.selectTagRequired"));
                return;
            }
            RefInfo refInfo = getSelectedRefInfo(tagCombo);
            if (refInfo != null && refInfo.getRefName() != null && !refInfo.getRefName()
                                                                           .isBlank()) {
                builder.refName(refInfo.getRefName());
            }
            builder.targetType(CheckoutRequest.TargetType.TAG)
                   .tag(tag.trim());
        } else if (commitRadio.isSelected()) {
            String commitText = commitCombo.getEditor()
                                           .getText();
            if (commitText == null || commitText.isBlank()) {
                showWarn(I18nUtil.get("switch.selectCommitRequired"));
                return;
            }
            // 优先取 RefInfo 携带的完整 commitId（来自 LogMessagesDialog）
            String commitId = commitText.trim();
            RefInfo refInfo = getSelectedRefInfo(commitCombo);
            if (refInfo != null && refInfo.getRefName() != null && !refInfo.getRefName()
                                                                           .isBlank()) {
                commitId = refInfo.getRefName();
            } else {
                // 否则尝试从 ComboBox 已选项目取完整 commitId
                CommitItem sel = commitCombo.getSelectionModel()
                                            .getSelectedItem();
                if (sel != null && sel.shortId.equals(commitText.trim())) {
                    commitId = sel.commitId;
                }
            }
            builder.targetType(CheckoutRequest.TargetType.COMMIT)
                   .commitId(commitId);
        } else {
            showWarn(I18nUtil.get("switch.selectTargetRequired"));
            return;
        }

        // 2. Options
        if (createCheck.isSelected()) {
            String newBranch = newBranchField.getText();
            if (newBranch == null || newBranch.isBlank()) {
                showWarn(I18nUtil.get("switch.newBranchRequired"));
                return;
            }
            builder.create(true)
                   .newBranch(newBranch.trim());
        }
        if (overrideCheck.isSelected()) {
            String newBranch = newBranchField.getText();
            if (newBranch == null || newBranch.isBlank()) {
                showWarn(I18nUtil.get("switch.newBranchRequired"));
                return;
            }
            builder.overrideExisting(true)
                   .newBranch(newBranch.trim());
        }
        builder.force(forceCheck.isSelected())
               .track(trackCheck.isSelected())
               .merge(mergeCheck.isSelected());

        // 3. 执行（P1-002: git checkout 转到后台线程，避免冻结 JavaFX UI）
        CheckoutRequest checkoutReq = builder.build();
        AsyncUiLoader.submitWrite(repoPath, TaskType.CHECKOUT, () -> {
            try {
                gitOperationService.checkout(checkoutReq);
                Platform.runLater(this::close);
            } catch (Exception e) {
                log.error("Switch/Checkout 失败", e);
                Platform.runLater(() -> showError(I18nUtil.get("switch.failed") + "：" + e.getMessage()));
            }
        });
    }

    /**
     * 显示帮助信息（TortoiseGit 同位置）。
     */
    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18nUtil.get("switch.help.title"));
        alert.setHeaderText(I18nUtil.get("switch.help.header"));
        alert.setContentText(I18nUtil.get("switch.help.content"));
        alert.showAndWait();
    }

    /**
     * 警告对话框。
     */
    private void showWarn(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(I18nUtil.get("switch.title"));
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    /**
     * 错误对话框。
     */
    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(I18nUtil.get("switch.title"));
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    /**
     * Commit 下拉项（同时显示 shortId / 作者 / message 第一行）。
     */
    public static class CommitItem {

        final String shortId;
        final String author;
        final String message;
        final String commitId;

        public CommitItem(String shortId, String author, String message, String commitId) {
            this.shortId = shortId;
            this.author = author;
            this.message = message;
            this.commitId = commitId;
        }

        public String toDisplayString() {
            StringBuilder sb = new StringBuilder();
            if (!shortId.isEmpty()) {
                sb.append(shortId)
                  .append(' ');
            }
            if (!author.isEmpty()) {
                sb.append(author)
                  .append(' ');
            }
            sb.append(message);
            return sb.toString();
        }

        @Override
        public String toString() {
            return toDisplayString();
        }
    }
}