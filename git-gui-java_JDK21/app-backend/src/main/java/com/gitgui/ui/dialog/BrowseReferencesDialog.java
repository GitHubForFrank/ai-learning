package com.gitgui.ui.dialog;

import com.gitgui.domain.constant.TaskType;
import com.gitgui.domain.model.RefInfo;
import com.gitgui.domain.service.StatusService;
import com.gitgui.ui.AsyncUiLoader;
import com.gitgui.ui.i18n.I18nUtil;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 浏览引用对话框（Browse References）
 * <p>参照 TortoiseGit Switch/Checkout 对话框中「...」按钮弹出的子对话框，</p>
 * <p>用于从 refs 树中挑选分支 / tag / commit：</p>
 * <ul>
 *   <li>左侧：refs 树（refs/heads / refs/remotes/origin / refs/tags）</li>
 *   <li>右侧：表格（Branch Name / Tracked branch / Date Last Commit / Last Commit / Last Author / SHA-1）</li>
 *   <li>顶部：Filter 输入框（按 refname / 提交信息 / 作者 / SHA-1 过滤）</li>
 *   <li>底部：「Show nested refs」勾选框 + OK / Cancel / Current Branch 按钮</li>
 * </ul>
 *
 * @author FrankKang
 * @since 2026-07-25
 */
public class BrowseReferencesDialog extends Dialog<RefInfo> {

    private static final Logger log = LoggerFactory.getLogger(BrowseReferencesDialog.class);

    private final StatusService statusService;
    private final String repoPath;

    /**
     * 表格数据源
     */
    private final ObservableList<ReferenceRow> allRefs = FXCollections.observableArrayList();
    /**
     * refs 树（用于按目录分组）
     */
    private final TreeView<String> refsTree = new TreeView<>();
    /**
     * 主表格
     */
    private final TableView<ReferenceRow> refTable = new TableView<>();
    /**
     * 过滤输入框
     */
    private final TextField filterField = new TextField();
    /**
     * 是否显示嵌套 refs（默认折叠远程分支组）
     */
    private final CheckBox showNestedCheck = new CheckBox(I18nUtil.get("browse.showNested"));
    private FilteredList<ReferenceRow> filteredRefs;
    /**
     * OK / 双击后回填的完整 ref 信息（含 refName / displayName / kind / remoteName）
     */
    private RefInfo selectedReference;

    public BrowseReferencesDialog(StatusService statusService, String repoPath) {
        this.statusService = statusService;
        this.repoPath = repoPath;
        setTitle(repoPath + " - " + I18nUtil.get("browse.title"));
        setHeaderText(null);
        initModality(Modality.APPLICATION_MODAL);

        DialogPane pane = getDialogPane();
        pane.setContent(buildContent());
        pane.setPrefSize(960, 560);

        // 按钮：OK / Cancel / Current Branch
        ButtonType okType = new ButtonType(I18nUtil.get("button.ok"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType(I18nUtil.get("button.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType currentBranchType = new ButtonType(I18nUtil.get("browse.currentBranch"), ButtonBar.ButtonData.LEFT);
        pane.getButtonTypes()
            .addAll(okType, cancelType, currentBranchType);

        // OK：选中行 → 回填
        Button okButton = (Button) pane.lookupButton(okType);
        if (okButton != null) {
            okButton.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
                e.consume();
                confirmSelection();
            });
        }
        // Cancel：清空选择
        Button cancelButton = (Button) pane.lookupButton(cancelType);
        if (cancelButton != null) {
            cancelButton.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
                selectedReference = null;
            });
        }
        // Current Branch：选择当前分支
        Button currentBranchButton = (Button) pane.lookupButton(currentBranchType);
        if (currentBranchButton != null) {
            currentBranchButton.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
                e.consume();
                // 构造一个 RefInfo 表示当前分支（仅 displayName 字段，其他为空）
                String current = statusService.getCurrentBranch(repoPath);
                if (current == null || current.isEmpty()) {
                    new Alert(Alert.AlertType.WARNING, I18nUtil.get("browse.selectRequired")).showAndWait();
                    return;
                }
                selectedReference = RefInfo.builder()
                                           .refName("refs/heads/" + current)
                                           .displayName(current)
                                           .kind("BRANCH")
                                           .remoteName("")
                                           .build();
                setResult(selectedReference);
            });
        }

        setResultConverter(buttonType -> {
            if (buttonType == okType) {
                return selectedReference;
            }
            return null;
        });

        // 双击表格行 → 立即确认
        refTable.setRowFactory(tv -> {
            TableRow<ReferenceRow> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    ReferenceRow item = row.getItem();
                    selectedReference = RefInfo.builder()
                                               .refName(item.getKind()
                                                            .equals("TAG") ? "refs/tags/" + item.getDisplayRef() : (item.getKind()
                                                                                                                        .equals("REMOTE") ?
                                                       "refs/remotes/" + item.getRemoteName() + "/" + item.getDisplayRef()
                                                       : "refs/heads/" + item.getDisplayRef()))
                                               .displayName(item.getDisplayRef())
                                               .kind(item.getKind())
                                               .remoteName(item.getRemoteName())
                                               .build();
                    setResult(selectedReference);
                }
            });
            return row;
        });

        // 异步加载 refs
        loadReferences();
    }

    /**
     * 构建对话框内容。
     */
    private BorderPane buildContent() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(8));

        // === 顶部：Filter ===
        HBox topBar = new HBox(8);
        topBar.setPadding(new Insets(0, 0, 8, 0));
        Label filterLabel = new Label(I18nUtil.get("browse.filter") + ":");
        filterField.setPromptText(I18nUtil.get("browse.filterPrompt"));
        filterField.textProperty()
                   .addListener((obs, o, n) -> applyFilter(n));
        HBox.setHgrow(filterField, Priority.ALWAYS);
        topBar.getChildren()
              .addAll(filterLabel, filterField);
        root.setTop(topBar);

        // === 左侧：refs 树 ===
        refsTree.setPrefWidth(220);
        refsTree.setShowRoot(true);
        // 选中树节点 → 过滤右侧表格
        refsTree.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> applyFilter(filterField.getText()));
        root.setLeft(refsTree);

        // === 右侧：表格 ===
        buildRefTable();
        root.setCenter(refTable);

        // === 底部：Show nested refs + 状态 ===
        HBox bottomBar = new HBox(8);
        bottomBar.setPadding(new Insets(8, 0, 0, 0));
        showNestedCheck.setSelected(false);
        // 切换 nested 时重新分组树
        showNestedCheck.selectedProperty()
                       .addListener((obs, o, n) -> rebuildRefsTree());
        bottomBar.getChildren()
                 .addAll(showNestedCheck);
        root.setBottom(bottomBar);

        return root;
    }

    /**
     * 构造右侧表格（列定义）。
     */
    private void buildRefTable() {
        TableColumn<ReferenceRow, String> colBranch = new TableColumn<>(I18nUtil.get("browse.col.branch"));
        colBranch.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()
                                                                       .getDisplayRef()));
        colBranch.setPrefWidth(220);

        TableColumn<ReferenceRow, String> colTracked = new TableColumn<>(I18nUtil.get("browse.col.tracked"));
        colTracked.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()
                                                                        .getTrackedBranch()));
        colTracked.setPrefWidth(140);

        TableColumn<ReferenceRow, String> colDate = new TableColumn<>(I18nUtil.get("browse.col.date"));
        colDate.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()
                                                                     .getDate()));
        colDate.setPrefWidth(140);

        TableColumn<ReferenceRow, String> colMessage = new TableColumn<>(I18nUtil.get("browse.col.message"));
        colMessage.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()
                                                                        .getMessage()));
        colMessage.setPrefWidth(260);

        TableColumn<ReferenceRow, String> colAuthor = new TableColumn<>(I18nUtil.get("browse.col.author"));
        colAuthor.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()
                                                                       .getAuthor()));
        colAuthor.setPrefWidth(120);

        TableColumn<ReferenceRow, String> colSha = new TableColumn<>(I18nUtil.get("browse.col.sha"));
        colSha.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()
                                                                    .getShortSha()));
        colSha.setPrefWidth(110);

        refTable.getColumns()
                .addAll(colBranch, colTracked, colDate, colMessage, colAuthor, colSha);
        refTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        refTable.setPlaceholder(new Label(I18nUtil.get("browse.loading")));

        filteredRefs = new FilteredList<>(allRefs, p -> true);
        refTable.setItems(filteredRefs);
    }

    /**
     * 异步加载 refs 与对应 commit 元信息。
     * <p>使用 {@link StatusService#batchListRefs(String)} 一次性获取所有 ref + 对应 commit 元信息，</p>
     * <p>避免每个 ref 单独打开仓库造成性能瓶颈（原实现 50 个 ref 需打开 50 次仓库）。</p>
     */
    private void loadReferences() {
        AsyncUiLoader.submitRead(repoPath, TaskType.STATUS, () -> {
            try {
                List<RefInfo> refInfos = statusService.batchListRefs(repoPath);
                List<ReferenceRow> rows = new ArrayList<>(refInfos.size());
                for (RefInfo info : refInfos) {
                    rows.add(new ReferenceRow(info.getDisplayName(), info.getTrackedBranch() == null ? "" : info.getTrackedBranch(),
                                              info.getCommitDate() == null ? "" : info.getCommitDate(),
                                              info.getMessage() == null ? "" : info.getMessage(), info.getAuthor() == null ? "" : info.getAuthor(),
                                              info.getSha() == null ? "" : info.getSha(), info.getKind(),
                                              info.getRemoteName() == null ? "" : info.getRemoteName()));
                }
                Platform.runLater(() -> {
                    allRefs.setAll(rows);
                    rebuildRefsTree();
                    applyFilter(filterField.getText());
                });
            } catch (Exception e) {
                log.error("加载 refs 失败：{}", repoPath, e);
                Platform.runLater(() -> refTable.setPlaceholder(new Label(I18nUtil.get("browse.loadFailed") + "：" + e.getMessage())));
            }
        });
    }

    /**
     * 构造左侧 refs 树（TortoiseGit 风格）。
     * <p>层级结构（折叠模式，{@code Show nested refs} 取消勾选）：</p>
     * <pre>
     * refs
     * ├── heads
     * │   ├── main
     * │   └── develop
     * ├── remotes
     * │   └── origin
     * │       ├── andibrae        ← 第一路径段作为子分组节点
     * │       ├── klazuka
     * │       └── ...
     * └── tags
     *     ├── v1.0.0
     *     └── ...
     * </pre>
     * <p>嵌套模式（{@code Show nested refs} 勾选）：</p>
     * <pre>
     * remotes
     * └── origin
     *     └── andibrae
     *         ├── create-top-level-namespace
     *         └── ...
     * </pre>
     * <p>设计要点：</p>
     * <ul>
     *   <li>每个 tree node 的 value 即其显示路径段；用户选中时由 {@link #getTreeFilterText} 拼接为完整过滤前缀</li>
     *   <li>中间分组节点（如 andibrae）也是「虚拟」的，不会出现在 table 中</li>
     *   <li>叶子节点（实际 ref）才对应 table 中的 ReferenceRow</li>
     * </ul>
     */
    private void rebuildRefsTree() {
        TreeItem<String> root = new TreeItem<>(I18nUtil.get("browse.refs"));
        root.setExpanded(true);

        TreeItem<String> headsNode = new TreeItem<>(I18nUtil.get("browse.heads"));
        TreeItem<String> remotesNode = new TreeItem<>(I18nUtil.get("browse.remotes"));
        TreeItem<String> tagsNode = new TreeItem<>(I18nUtil.get("browse.tags"));

        boolean nested = showNestedCheck.isSelected();

        for (ReferenceRow row : allRefs) {
            String kind = row.getKind();
            if ("TAG".equals(kind)) {
                // tag 直接作为叶子挂在 tags 下
                tagsNode.getChildren()
                        .add(new TreeItem<>(row.getDisplayRef()));
                continue;
            }
            if ("BRANCH".equals(kind)) {
                // 本地分支直接作为叶子挂在 heads 下
                headsNode.getChildren()
                         .add(new TreeItem<>(row.getDisplayRef()));
                continue;
            }
            if ("REMOTE".equals(kind)) {
                // 远程分支：remotes/<remote>/<displayRef 剩余部分>
                TreeItem<String> remoteRoot = findOrCreateChild(remotesNode, row.getRemoteName());
                String remaining = row.getDisplayRef();  // 已剥离 <remote>/ 前缀
                if (remaining == null || remaining.isEmpty()) {
                    continue;
                }
                if (!remaining.contains("/")) {
                    // 简单分支：origin/main → 直接挂在 origin 下
                    remoteRoot.getChildren()
                              .add(new TreeItem<>(remaining));
                } else if (nested) {
                    // 嵌套模式：递归创建中间节点，最终叶子是完整 remaining
                    buildNestedPath(remoteRoot, remaining);
                } else {
                    // 折叠模式：只取第一段作为分组节点（不递归挂叶子）
                    String firstSeg = remaining.substring(0, remaining.indexOf('/'));
                    findOrCreateChild(remoteRoot, firstSeg);
                }
            }
        }

        root.getChildren()
            .addAll(headsNode, remotesNode, tagsNode);
        refsTree.setRoot(root);
        refsTree.refresh();
    }

    /**
     * 在父节点下按 {@code path/segments} 递归创建中间节点（嵌套模式），最终叶子为 {@code path} 整体。
     *
     * @param parent 父节点（如 origin）
     * @param path   完整剩余路径（如 andibrae/create-top-level-namespace）
     */
    private void buildNestedPath(TreeItem<String> parent, String path) {
        int idx = path.indexOf('/');
        if (idx < 0) {
            // 叶子：直接挂上完整路径作为节点
            parent.getChildren()
                  .add(new TreeItem<>(path));
            return;
        }
        String segment = path.substring(0, idx);
        String rest = path.substring(idx + 1);
        TreeItem<String> child = findOrCreateChild(parent, segment);
        buildNestedPath(child, rest);
    }

    /**
     * 在指定父节点下查找或创建子节点（同名）。
     */
    private TreeItem<String> findOrCreateChild(TreeItem<String> parent, String name) {
        for (TreeItem<String> child : parent.getChildren()) {
            if (child.getValue()
                     .equals(name)) {
                return child;
            }
        }
        TreeItem<String> node = new TreeItem<>(name);
        parent.getChildren()
              .add(node);
        return node;
    }

    /**
     * 过滤过滤框 + refs 树选中状态（联合过滤）。
     */
    private void applyFilter(String keyword) {
        if (filteredRefs == null) {
            return;
        }
        final String lower = keyword == null ? "" : keyword.toLowerCase();
        final String treeFilter = getTreeFilterText(refsTree.getSelectionModel()
                                                            .getSelectedItem());
        filteredRefs.setPredicate(row -> {
            // 树过滤优先
            if (!treeFilter.isEmpty()) {
                if (!matchesTree(row, treeFilter)) {
                    return false;
                }
            }
            if (lower.isEmpty()) {
                return true;
            }
            // 多字段模糊匹配
            return row.getDisplayRef()
                      .toLowerCase()
                      .contains(lower) || (row.getAuthor() != null && row.getAuthor()
                                                                         .toLowerCase()
                                                                         .contains(lower)) || (row.getMessage() != null && row.getMessage()
                                                                                                                              .toLowerCase()
                                                                                                                              .contains(lower)) || (
                    row.getShortSha() != null && row.getShortSha()
                                                    .toLowerCase()
                                                    .contains(lower));
        });
    }

    /**
     * 获取树节点的过滤前缀文本（root 返回空串，否则按层级拼接）。
     */
    private String getTreeFilterText(TreeItem<String> node) {
        if (node == null || node.getParent() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(node.getValue());
        TreeItem<String> cur = node.getParent();
        while (cur != null && cur.getParent() != null) {
            sb.insert(0, cur.getValue() + "/");
            cur = cur.getParent();
        }
        return sb.toString();
    }

    /**
     * 判断行是否匹配树节点前缀。
     * <p>树节点路径规则：</p>
     * <ul>
     *   <li>顶部大类：{@code heads} / {@code remotes} / {@code tags}（已国际化）</li>
     *   <li>远程名段：{@code remotes/<remote>}</li>
     *   <li>远程分支段：{@code remotes/<remote>/<seg1>/<seg2>/...}</li>
     *   <li>注意：{@code tags} 下直接挂叶子 tag；{@code heads} 下直接挂叶子分支；{@code remotes/<remote>} 下挂「第一路径段」作为分组（折叠模式）或「完整路径段」作为分组（嵌套模式）</li>
     * </ul>
     *
     * @param row        表格行
     * @param treeFilter 树节点路径（按层级用 {@code /} 拼接）
     * @return true 表示行匹配该树节点
     */
    private boolean matchesTree(ReferenceRow row, String treeFilter) {
        if (treeFilter == null || treeFilter.isEmpty()) {
            return true;
        }
        String[] segs = treeFilter.split("/", -1);
        if (segs.length == 0) {
            return true;
        }
        String headsLabel = I18nUtil.get("browse.heads");
        String remotesLabel = I18nUtil.get("browse.remotes");
        String tagsLabel = I18nUtil.get("browse.tags");
        String first = segs[0];
        if (first.equals(headsLabel)) {
            return "BRANCH".equals(row.getKind()) && (row.getDisplayRef() == null || !row.getDisplayRef()
                                                                                         .contains("/"));
        }
        if (first.equals(tagsLabel)) {
            return "TAG".equals(row.getKind());
        }
        if (first.equals(remotesLabel)) {
            if (!"REMOTE".equals(row.getKind())) {
                return false;
            }
            if (segs.length == 1) {
                // 选 remotes 顶层：所有远程分支
                return true;
            }
            if (segs.length == 2) {
                // 选 remotes/<remote>：匹配该 remote 下所有分支
                return segs[1].equals(row.getRemoteName());
            }
            // remotes/<remote>/<seg1>/<seg2>/...
            // 拼接剩余路径段（不含 segs[0]=remotes 与 segs[1]=<remote>）
            StringBuilder prefix = new StringBuilder();
            for (int i = 2; i < segs.length; i++) {
                if (prefix.length() > 0) {
                    prefix.append('/');
                }
                prefix.append(segs[i]);
            }
            String displayRef = row.getDisplayRef() == null ? "" : row.getDisplayRef();
            // 折叠模式下的「虚拟分组节点」如 andibrae：匹配所有以 andibrae/ 开头的剩余路径
            // 嵌套模式下的叶子节点：精确匹配
            return displayRef.equals(prefix.toString()) || displayRef.startsWith(prefix.toString() + "/");
        }
        return true;
    }

    /**
     * 确认选择（点击 OK 时调用）。
     */
    private void confirmSelection() {
        ReferenceRow sel = refTable.getSelectionModel()
                                   .getSelectedItem();
        if (sel == null) {
            new Alert(Alert.AlertType.WARNING, I18nUtil.get("browse.selectRequired")).showAndWait();
            return;
        }
        // 构造 RefInfo：把显示名按 kind 拼回完整 ref 路径
        String fullRef;
        if ("TAG".equals(sel.getKind())) {
            fullRef = "refs/tags/" + sel.getDisplayRef();
        } else if ("REMOTE".equals(sel.getKind())) {
            fullRef = "refs/remotes/" + sel.getRemoteName() + "/" + sel.getDisplayRef();
        } else {
            fullRef = "refs/heads/" + sel.getDisplayRef();
        }
        selectedReference = RefInfo.builder()
                                   .refName(fullRef)
                                   .displayName(sel.getDisplayRef())
                                   .kind(sel.getKind())
                                   .remoteName(sel.getRemoteName())
                                   .build();
        setResult(selectedReference);
    }

    /**
     * 表格行模型（不可变 POJO）。
     * <p>字段语义：</p>
     * <ul>
     *   <li>{@code displayRef}：已剥离 {@code refs/remotes/<remote>/} / {@code refs/heads/} / {@code refs/tags/} 前缀</li>
     *   <li>{@code remoteName}：仅 REMOTE 类型有值（如 origin），其他为空串</li>
     *   <li>{@code kind}：BRANCH（本地分支）/ REMOTE（远程分支）/ TAG</li>
     * </ul>
     */
    public static class ReferenceRow {

        private final String displayRef;
        private final String trackedBranch;
        private final String date;
        private final String message;
        private final String author;
        private final String sha;
        private final String kind;
        /**
         * 远程名（仅 REMOTE 类型有值）
         */
        private final String remoteName;

        public ReferenceRow(String displayRef, String trackedBranch, String date, String message, String author, String sha, String kind,
                String remoteName) {
            this.displayRef = displayRef;
            this.trackedBranch = trackedBranch;
            this.date = date;
            this.message = message;
            this.author = author;
            this.sha = sha;
            this.kind = kind;
            this.remoteName = remoteName == null ? "" : remoteName;
        }

        public String getDisplayRef() {
            return displayRef;
        }

        public String getTrackedBranch() {
            return trackedBranch;
        }

        public String getDate() {
            return date;
        }

        public String getMessage() {
            return message;
        }

        public String getAuthor() {
            return author;
        }

        public String getSha() {
            return sha;
        }

        public String getShortSha() {
            return sha == null || sha.length() < 8 ? sha : sha.substring(0, 8);
        }

        public String getKind() {
            return kind;
        }

        public String getRemoteName() {
            return remoteName;
        }
    }
}