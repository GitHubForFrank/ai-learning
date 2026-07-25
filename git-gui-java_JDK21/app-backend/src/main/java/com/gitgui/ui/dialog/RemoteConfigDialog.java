package com.gitgui.ui.dialog;

import com.gitgui.core.exception.GitGuiException;
import com.gitgui.domain.constant.TaskType;
import com.gitgui.domain.model.RemoteConfig;
import com.gitgui.domain.service.RemoteConfigService;
import com.gitgui.ui.AsyncUiLoader;
import com.gitgui.ui.i18n.I18nUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * 远程配置对话框（Remote）
 * <p>参照 TortoiseGit Settings → Git → Remote 双列布局：</p>
 * <pre>
 * ┌─ &lt;repo&gt; - Remote ─────────────────────────────┐
 * │ ┌────────────┐ ┌────────────────────────────────┐│
 * │ │ Remote:    │ │ Remote: [origin        ] [Rename]││
 * │ │ ┌────────┐ │ │                                ││
 * │ │ │ origin │ │ │ URL:       [https://...    ]   ││
 * │ │ │        │ │ │ Push URL:  [                ]  ││
 * │ │ │        │ │ │ Putty Key: [                ][]││
 * │ │ │        │ │ │ Tags: [Reachable ▼]  ☐ Push Def. ││
 * │ │ └────────┘ │ │ ☑ Prune                       ││
 * │ │            │ │                                ││
 * │ │            │ │ ┌──────────────────────────┐  ││
 * │ │            │ │ │ Add New/Save             │  ││
 * │ │            │ │ │ Remove                   │  ││
 * │ │            │ │ └──────────────────────────┘  ││
 * │ └────────────┘ └────────────────────────────────┘│
 * │                                                  │
 * │                            [确定]  [取消]         │
 * └──────────────────────────────────────────────────┘
 * </pre>
 * <p>功能：</p>
 * <ul>
 *   <li>左侧 Remote 列表：选中 → 右侧显示详情</li>
 *   <li>右侧编辑 URL / Push URL 后点击 Save → 写入配置</li>
 *   <li>Add New：创建新 Remote</li>
 *   <li>Remove：删除当前 Remote</li>
 *   <li>Rename：重命名当前 Remote</li>
 *   <li>Putty Key、Tags、Prune、Push Default 仅展示 + 持久化（V1 简化版仅 URL/Push URL/Name）</li>
 * </ul>
 *
 * @author FrankKang
 * @since 2026-07-25
 */
public class RemoteConfigDialog extends Dialog<Void> {

    private static final Logger log = LoggerFactory.getLogger(RemoteConfigDialog.class);

    private final RemoteConfigService remoteConfigService;
    private final String repoPath;

    /** Remote 列表数据源 */
    private final ObservableList<RemoteConfig> remotes = FXCollections.observableArrayList();
    /** Remote 列表视图 */
    private final ListView<RemoteConfig> remoteList = new ListView<>();

    /** 防止保存重复触发的并发控制标志 */
    private volatile boolean savingInProgress = false;
    /** 当前选中 Remote 的详细信息（编辑区） */
    private final TextField nameField = new TextField();
    private final TextField urlField = new TextField();
    private final TextField pushUrlField = new TextField();
    /** push URL 行标签，用于切换可见性 */
    private Label pushUrlLabel = new Label();
    /** 当前 push URL 是否可见 */
    private boolean pushUrlVisible = false;
    /** 切换 push URL 显示/隐藏的按钮 */
    private Button togglePushBtn;
    private final TextField puttyKeyField = new TextField();
    private final ComboBox<String> tagsCombo = new ComboBox<>();
    private final CheckBox pushDefaultCheck = new CheckBox();
    private final CheckBox pruneCheck = new CheckBox();

    /** 标记当前是否在「新建」模式（未持久化的临时 Remote） */
    private boolean creatingNew = false;

    public RemoteConfigDialog(RemoteConfigService remoteConfigService, String repoPath) {
        this.remoteConfigService = remoteConfigService;
        this.repoPath = repoPath;
        setTitle(repoPath + " - " + I18nUtil.get("remote.title"));
        setHeaderText(null);
        initModality(Modality.APPLICATION_MODAL);

        DialogPane pane = getDialogPane();
        pane.setContent(buildContent());
        pane.setPrefSize(720, 520);

        ButtonType okType = new ButtonType(I18nUtil.get("button.ok"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType(I18nUtil.get("button.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(okType, cancelType);

        Button okButton = (Button) pane.lookupButton(okType);
        if (okButton != null) {
            // OK 按钮：保存 + 关闭（TortoiseGit 风格语义，与 Save 按钮共享保存逻辑）
            okButton.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
                e.consume();
                doSave(true);
            });
        }
        setResultConverter(buttonType -> null);

        // 列表选中变化 → 右侧详情
        remoteList.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> fillDetails(newVal));

        // 异步加载 remotes
        loadRemotes();
    }

    /**
     * 构建对话框主体。
     */
    private BorderPane buildContent() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));

        // === 中部：左列表 + 右详情 ===
        SplitPane split = new SplitPane();
        split.setDividerPositions(0.3);
        root.setCenter(split);

        // 左侧：Remote 列表
        VBox leftBox = new VBox(6);
        Label leftTitle = new Label(I18nUtil.get("remote.label.remote") + ":");
        remoteList.setItems(remotes);
        remoteList.setPlaceholder(new Label(I18nUtil.get("remote.empty")));
        VBox.setVgrow(remoteList, Priority.ALWAYS);
        leftBox.getChildren().addAll(leftTitle, remoteList);
        leftBox.setPadding(new Insets(0, 6, 0, 0));
        split.getItems().add(leftBox);

        // 右侧：详情编辑区
        GridPane rightGrid = buildDetailGrid();
        split.getItems().add(rightGrid);

        // === 底部：按钮区 ===
        HBox bottomBar = new HBox(10);
        bottomBar.setPadding(new Insets(12, 0, 0, 0));
        bottomBar.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        // 左侧：Add New / Save / Remove / Rename
        Button addNewBtn = new Button(I18nUtil.get("remote.btn.addNew"));
        addNewBtn.setOnAction(e -> onAddNew());
        Button saveBtn = new Button(I18nUtil.get("remote.btn.save"));
        saveBtn.setOnAction(e -> doSave(false));
        Button removeBtn = new Button(I18nUtil.get("remote.btn.remove"));
        removeBtn.setOnAction(e -> onRemove());
        Button renameBtn = new Button(I18nUtil.get("remote.btn.rename"));
        renameBtn.setOnAction(e -> onRename());

        HBox leftButtons = new HBox(6, addNewBtn, saveBtn, removeBtn, renameBtn);
        HBox.setHgrow(leftButtons, Priority.ALWAYS);
        bottomBar.getChildren().add(leftButtons);

        root.setBottom(bottomBar);

        return root;
    }

    /**
     * 构建右侧详情编辑区。
     */
    private GridPane buildDetailGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(0, 0, 0, 6));

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(22);
        col1.setHalignment(HPos.RIGHT);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(78);
        grid.getColumnConstraints().addAll(col1, col2);

        int row = 0;

        // Remote 名称 + Rename 按钮（同行）
        HBox nameRow = new HBox(6, nameField, new Button(I18nUtil.get("remote.btn.renameInline")));
        HBox.setHgrow(nameField, Priority.ALWAYS);
        Button renameInline = (Button) nameRow.getChildren().get(1);
        renameInline.setOnAction(e -> onRename());
        grid.add(new Label(I18nUtil.get("remote.label.name")), 0, row);
        grid.add(nameRow, 1, row++);

        // URL
        urlField.setPromptText(I18nUtil.get("remote.url.prompt"));
        grid.add(new Label(I18nUtil.get("remote.label.url")), 0, row);
        grid.add(urlField, 1, row++);

        // Push URL（TortoiseGit 风格：默认不显示，仅在已配置 push URL 时回显；可手动切换）
        pushUrlLabel.setText(I18nUtil.get("remote.label.pushUrl"));
        pushUrlField.setPromptText(I18nUtil.get("remote.pushUrl.prompt"));
        // 默认隐藏：setManaged(false) 让 GridPane 不为该行分配空间
        pushUrlLabel.setVisible(false);
        pushUrlLabel.setManaged(false);
        pushUrlField.setVisible(false);
        pushUrlField.setManaged(false);
        grid.add(pushUrlLabel, 0, row);
        grid.add(pushUrlField, 1, row++);
        // 切换按钮：Show / Hide Push URL
        togglePushBtn = new Button(I18nUtil.get("remote.btn.showPush"));
        togglePushBtn.getStyleClass().add("remote-toggle-btn");
        togglePushBtn.setOnAction(e -> showPushUrlRow(!pushUrlVisible));
        grid.add(togglePushBtn, 1, row++);

        // Putty Key
        HBox puttyRow = new HBox(6, puttyKeyField, new Button("..."));
        HBox.setHgrow(puttyKeyField, Priority.ALWAYS);
        puttyKeyField.setPromptText(I18nUtil.get("remote.putty.prompt"));
        grid.add(new Label(I18nUtil.get("remote.label.puttyKey")), 0, row);
        grid.add(puttyRow, 1, row++);

        // Tags: Reachable / All / None
        tagsCombo.getItems().addAll("Reachable", "All", "None");
        tagsCombo.setValue("Reachable");
        HBox tagsRow = new HBox(6, tagsCombo, pushDefaultCheck);
        HBox.setHgrow(tagsCombo, Priority.ALWAYS);
        pushDefaultCheck.setText(I18nUtil.get("remote.label.pushDefault"));
        grid.add(new Label(I18nUtil.get("remote.label.tags")), 0, row);
        grid.add(tagsRow, 1, row++);

        // Prune
        pruneCheck.setText(I18nUtil.get("remote.label.prune"));
        pruneCheck.setSelected(true);
        grid.add(new Label(""), 0, row);
        grid.add(pruneCheck, 1, row++);

        return grid;
    }

    /**
     * 异步加载所有 Remote。
     */
    private void loadRemotes() {
        AsyncUiLoader.submitRead(repoPath, TaskType.STATUS, () -> {
            try {
                List<RemoteConfig> list = remoteConfigService.list(repoPath);
                Platform.runLater(() -> {
                    remotes.setAll(list);
                    if (!list.isEmpty()) {
                        remoteList.getSelectionModel().select(0);
                    } else {
                        // 没有 Remote，自动进入新建模式
                        onAddNew();
                    }
                });
            } catch (Exception e) {
                log.error("加载 Remote 列表失败：{}", repoPath, e);
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR,
                            I18nUtil.get("remote.loadFailed") + "：" + e.getMessage());
                    alert.showAndWait();
                });
            }
        });
    }

    /**
     * 右侧详情区显示选中 Remote 的信息。
     */
    private void fillDetails(RemoteConfig rc) {
        if (rc == null) {
            nameField.clear();
            urlField.clear();
            pushUrlField.clear();
            puttyKeyField.clear();
            tagsCombo.setValue("Reachable");
            pushDefaultCheck.setSelected(false);
            pruneCheck.setSelected(true);
            return;
        }
        nameField.setText(rc.getName() == null ? "" : rc.getName());
        String fetchUrl = rc.getFetchUrl() == null ? "" : rc.getFetchUrl();
        urlField.setText(fetchUrl);
        String existingPush = rc.getPushUrl() == null ? "" : rc.getPushUrl();
        // TortoiseGit 风格：仅在 pushUrl 确实存在且与 fetchUrl 不同时才回显
        boolean hasPushUrl = !existingPush.isEmpty() && !existingPush.equals(fetchUrl);
        if (hasPushUrl) {
            pushUrlField.setText(existingPush);
            if (!pushUrlVisible) {
                showPushUrlRow(true);
            }
        } else {
            // 无 pushUrl 或相同 → 不显示
            pushUrlField.clear();
            if (pushUrlVisible) {
                showPushUrlRow(false);
            }
        }
        puttyKeyField.clear();
        pushDefaultCheck.setSelected(rc.isDefaultPush());
        pruneCheck.setSelected(true);
        creatingNew = false;
    }

    /**
     * 新建 Remote（清空详情，等待用户输入）。
     */
    private void onAddNew() {
        creatingNew = true;
        nameField.clear();
        urlField.clear();
        showPushUrlRow(false);
        pushUrlField.clear();
        nameField.requestFocus();
        remoteList.getSelectionModel().clearSelection();
    }

    /**
     * 切换 Push URL 行的可见性。
     *
     * @param visible true 显示，false 隐藏
     */
    private void showPushUrlRow(boolean visible) {
        pushUrlVisible = visible;
        pushUrlLabel.setVisible(visible);
        pushUrlLabel.setManaged(visible);
        pushUrlField.setVisible(visible);
        pushUrlField.setManaged(visible);
        if (!visible) {
            pushUrlField.clear();
        }
        if (togglePushBtn != null) {
            togglePushBtn.setText(visible
                    ? I18nUtil.get("remote.btn.hidePush")
                    : I18nUtil.get("remote.btn.showPush"));
        }
    }

    /**
     * 保存当前编辑内容（新建 / 更新 URL），并根据 {@code closeAfter} 决定是否在保存成功后关闭对话框。
     * <p>被 OK 按钮（{@code closeAfter=true}）和 Save 按钮（{@code closeAfter=false}）共用。</p>
     *
     * @param closeAfter true：保存成功后关闭对话框（TortoiseGit 风格 OK 按钮语义）；<br>
     *                   false：保存成功后保持对话框打开以便继续编辑其他 Remote（Save 按钮语义）
     */
    private void doSave(boolean closeAfter) {
        if (savingInProgress) {
            return;
        }
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        String url = urlField.getText() == null ? "" : urlField.getText().trim();
        String pushUrl = pushUrlField.getText() == null ? "" : pushUrlField.getText().trim();

        if (name.isEmpty()) {
            warn(I18nUtil.get("remote.error.nameRequired"));
            return;
        }
        if (url.isEmpty()) {
            warn(I18nUtil.get("remote.error.urlRequired"));
            return;
        }

        // pushUrl 可见时的处理：
        //   与 url 相同 → 删除 push URL 配置
        //   与 url 不同 → 更新 push URL
        // pushUrl 不可见 → 不动原配置（保持现状）
        final String fName = name;
        final String fUrl = url;
        final String fPushUrl = pushUrl;
        final boolean isPushRowVisible = pushUrlVisible;

        savingInProgress = true;

        AsyncUiLoader.submitRead(repoPath, TaskType.STATUS, () -> {
            try {
                if (creatingNew) {
                    remoteConfigService.add(repoPath, fName, fUrl);
                    if (isPushRowVisible) {
                        handlePushUrlSave(repoPath, fName, fPushUrl, fUrl);
                    }
                } else {
                    // 更新 fetch URL
                    RemoteConfig selected = remoteList.getSelectionModel().getSelectedItem();
                    String oldName = selected != null ? selected.getName() : fName;
                    if (!oldName.equals(fName)) {
                        // 名称变化：先 rename 再 set-url
                        remoteConfigService.rename(repoPath, oldName, fName);
                    }
                    remoteConfigService.update(repoPath, fName, fUrl);
                    if (isPushRowVisible) {
                        handlePushUrlSave(repoPath, fName, fPushUrl, fUrl);
                    }
                }
                // 保存成功 → 重置新建模式
                creatingNew = false;
                Platform.runLater(() -> {
                    savingInProgress = false;
                    // 重新加载 Remote 列表（确保 list 中显示最新的 URL）
                    loadRemotes();
                    if (closeAfter) {
                        // 关闭对话框（与 TortoiseGit OK 按钮语义一致）：
                        // OK 按钮事件已被 consume() 拦截，需要显式关闭
                        close();
                    } else {
                        info(I18nUtil.get("remote.saved"));
                    }
                });
            } catch (GitGuiException ex) {
                log.error("保存 Remote 失败", ex);
                Platform.runLater(() -> {
                    savingInProgress = false;
                    error(I18nUtil.get("remote.saveFailed") + "：" + ex.getMessage());
                });
            } catch (Exception ex) {
                log.error("保存 Remote 失败", ex);
                Platform.runLater(() -> {
                    savingInProgress = false;
                    error(I18nUtil.get("remote.saveFailed") + "：" + ex.getMessage());
                });
            }
        });
    }

    /**
     * 处理 push URL 保存逻辑（仅在 push URL 行可见时调用）：
     * <ul>
     *   <li>pushUrl != fetchUrl → 写入 push URL config</li>
     *   <li>pushUrl == fetchUrl 或为空 → 删除 push URL config（因为只有不同时才需要显式配置）</li>
     * </ul>
     */
    private void handlePushUrlSave(String repoPath, String name, String pushUrl, String fetchUrl) {
        if (!pushUrl.isEmpty() && !pushUrl.equals(fetchUrl)) {
            setPushUrl(repoPath, name, pushUrl);
        } else {
            // pushUrl == fetchUrl 或为空 → 删除 pushurl 配置，与 TortoiseGit 行为一致
            unsetPushUrl(repoPath, name);
        }
    }

    /**
     * 写入 push URL 配置（{@code git remote set-url --push <name> <newUrl>}）。
     */
    private void setPushUrl(String repoPath, String name, String pushUrl) {
        try {
            com.gitgui.infrastructure.cli.GitProcessBuilder.execute(repoPath,
                    java.util.List.of("remote", "set-url", "--push", name, pushUrl),
                    null);
        } catch (Exception e) {
            throw new GitGuiException(com.gitgui.core.exception.ErrorCode.GIT_EXECUTION_FAILED,
                    "设置 push URL 失败：" + e.getMessage(), e);
        }
    }

    /**
     * 删除 push URL 配置（{@code git config --unset-all remote.<name>.pushurl}）。
     * <p>与 {@code setPushUrl} 配对：当用户清空 push URL 或将其设为与 fetch URL 相同时，
     * 需要删除 pushurl config 键。</p>
     */
    private void unsetPushUrl(String repoPath, String name) {
        try {
            // 先检查是否存在，避免 unset 不存在时报错
            com.gitgui.infrastructure.cli.GitProcessBuilder.execute(repoPath,
                    java.util.List.of("config", "--unset-all", "remote." + name + ".pushurl"),
                    null);
        } catch (Exception e) {
            // unset 失败不是致命错误（可能本来就不存在），仅记录日志
            log.warn("删除 push URL 配置失败（非致命）：name={}, repo={}", name, repoPath, e);
        }
    }

    /**
     * 删除当前 Remote。
     */
    private void onRemove() {
        RemoteConfig selected = remoteList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            warn(I18nUtil.get("remote.error.selectFirst"));
            return;
        }
        if (!confirm(I18nUtil.get("remote.confirm.remove") + "：" + selected.getName())) {
            return;
        }
        String name = selected.getName();
        AsyncUiLoader.submitRead(repoPath, TaskType.STATUS, () -> {
            try {
                remoteConfigService.delete(repoPath, name);
                Platform.runLater(this::loadRemotes);
            } catch (Exception e) {
                Platform.runLater(() -> error(I18nUtil.get("remote.removeFailed") + "：" + e.getMessage()));
            }
        });
    }

    /**
     * 重命名当前 Remote。
     */
    private void onRename() {
        RemoteConfig selected = remoteList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            warn(I18nUtil.get("remote.error.selectFirst"));
            return;
        }
        TextInputDialog dialog = new TextInputDialog(selected.getName());
        dialog.setTitle(I18nUtil.get("remote.btn.rename"));
        dialog.setHeaderText(I18nUtil.get("remote.rename.header"));
        dialog.setContentText(I18nUtil.get("remote.rename.prompt"));
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().trim().isEmpty() || result.get().trim().equals(selected.getName())) {
            return;
        }
        String oldName = selected.getName();
        String newName = result.get().trim();
        AsyncUiLoader.submitRead(repoPath, TaskType.STATUS, () -> {
            try {
                remoteConfigService.rename(repoPath, oldName, newName);
                Platform.runLater(this::loadRemotes);
            } catch (Exception e) {
                Platform.runLater(() -> error(I18nUtil.get("remote.renameFailed") + "：" + e.getMessage()));
            }
        });
    }

    /**
     * 警告对话框。
     */
    private void warn(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /**
     * 信息对话框。
     */
    private void info(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /**
     * 错误对话框。
     */
    private void error(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /**
     * 确认对话框。
     */
    private boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message);
        alert.setHeaderText(null);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}