package com.gitgui.ui.dialog;

import com.gitgui.core.async.ProgressCallback;
import com.gitgui.core.async.TaskHandle;
import com.gitgui.core.exception.RedLineBlockedException;
import com.gitgui.domain.constant.TaskType;
import com.gitgui.domain.model.RemoteConfig;
import com.gitgui.domain.model.request.PushRequest;
import com.gitgui.domain.service.GitOperationService;
import com.gitgui.domain.service.RemoteConfigService;
import com.gitgui.ui.AsyncUiLoader;
import com.gitgui.ui.i18n.I18nUtil;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 推送对话框
 * <p>对应 PRD 4.5.2，收集 Remote/分支/Force 选项后调用 {@code GitOperationService.push}。</p>
 * <p>遵循 BR-09（必选 Remote 与分支）、BR-11（默认仅暴露 Force with lease）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class PushDialog extends Dialog<Void> {

    private static final Logger log = LoggerFactory.getLogger(PushDialog.class);

    private final GitOperationService gitOperationService;
    private final String repoPath;

    /** Remote 下拉 */
    private final ComboBox<String> remoteCombo = new ComboBox<>();
    /** 分支下拉 */
    private final ComboBox<String> branchCombo = new ComboBox<>();
    /** 设置上游跟踪 */
    private final CheckBox setUpstreamCheck = new CheckBox(I18nUtil.get("push.setUpstream"));
    /** Force with lease（默认可见） */
    private final CheckBox forceWithLeaseCheck = new CheckBox(I18nUtil.get("push.forceWithLease"));
    /** 裸 Force（高级模式） */
    private final CheckBox forceCheck = new CheckBox(I18nUtil.get("push.force"));
    /** 包含标签 */
    private final CheckBox includeTagsCheck = new CheckBox(I18nUtil.get("push.includeTags"));
    /** 推送所有分支 */
    private final CheckBox pushAllBranchesCheck = new CheckBox(I18nUtil.get("push.pushAllBranches"));
    /** 推送所有标签 */
    private final CheckBox pushAllTagsCheck = new CheckBox(I18nUtil.get("push.pushAllTags"));
    /** 删除远程分支 */
    private final CheckBox deleteRemoteBranchCheck = new CheckBox(I18nUtil.get("push.deleteRemoteBranch"));
    /** 临时目标 URL */
    private final TextField pushToUrlField = new TextField();
    /** 高级模式开关 */
    private final CheckBox advancedCheck = new CheckBox(I18nUtil.get("push.advancedMode"));

    /** 进度条 */
    private final ProgressBar progressBar = new ProgressBar(0);
    /** 进度信息 */
    private final Label progressLabel = new Label();

    /** 远程配置服务（懒加载，从 Guice 获取） */
    private RemoteConfigService remoteConfigService;

    /**
     * 构造推送对话框。
     *
     * @param gitOperationService Git 操作服务
     * @param repoPath 仓库路径
     */
    public PushDialog(GitOperationService gitOperationService, String repoPath) {
        this.gitOperationService = gitOperationService;
        this.repoPath = repoPath;
        setTitle(I18nUtil.get("push.title"));
        setHeaderText(null);
        initModality(Modality.APPLICATION_MODAL);

        DialogPane pane = getDialogPane();
        pane.setContent(buildContent());
        pane.getButtonTypes().addAll(ButtonType.CANCEL, new ButtonType(I18nUtil.get("button.push"), ButtonBar.ButtonData.OK_DONE));

        // 从 Guice 获取 RemoteConfigService（若获取不到则降级为只用 origin）
        try {
            remoteConfigService = com.gitgui.GitGuiApp.getInjector().getInstance(RemoteConfigService.class);
        } catch (Exception e) {
            log.warn("无法获取 RemoteConfigService，使用默认 origin", e);
        }

        loadRemotes();

        // 高级模式切换：控制裸 Force 选项可见性
        forceCheck.visibleProperty().bind(advancedCheck.selectedProperty());
        pushToUrlField.visibleProperty().bind(advancedCheck.selectedProperty());
        deleteRemoteBranchCheck.visibleProperty().bind(advancedCheck.selectedProperty());

        // OK 按钮触发推送
        Button okButton = (Button) pane.lookupButton(new ButtonType(I18nUtil.get("button.push"), ButtonBar.ButtonData.OK_DONE));
        if (okButton != null) {
            okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                event.consume();
                doPush();
            });
        }

        setResultConverter(buttonType -> null);
    }

    /**
     * 构建对话框内容。
     *
     * @return 内容节点
     */
    private VBox buildContent() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(12));
        vbox.setPrefWidth(560);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label(I18nUtil.get("push.remote")), 0, 0);
        grid.add(remoteCombo, 1, 0);
        grid.add(new Label(I18nUtil.get("push.branch")), 0, 1);
        grid.add(branchCombo, 1, 1);

        HBox optionsRow1 = new HBox(15, setUpstreamCheck, forceWithLeaseCheck, includeTagsCheck);
        HBox optionsRow2 = new HBox(15, pushAllBranchesCheck, pushAllTagsCheck);
        grid.add(optionsRow1, 1, 2);
        grid.add(optionsRow2, 1, 3);

        grid.add(advancedCheck, 0, 4);
        grid.add(forceCheck, 1, 5);
        grid.add(deleteRemoteBranchCheck, 1, 6);
        grid.add(new Label(I18nUtil.get("push.pushToUrl")), 0, 7);
        grid.add(pushToUrlField, 1, 7);

        progressBar.setPrefWidth(540);
        progressBar.setVisible(false);
        progressLabel.setVisible(false);

        vbox.getChildren().addAll(grid, progressBar, progressLabel);
        return vbox;
    }

    /**
     * 加载 Remote 列表。
     * <p>通过 {@link AsyncUiLoader} 提交读任务（BR-33），完成后在 UI 线程填充下拉框，
     * 失败时降级为默认 origin。</p>
     */
    private void loadRemotes() {
        AsyncUiLoader.submitRead(repoPath, TaskType.STATUS, () -> {
            try {
                if (remoteConfigService != null) {
                    List<RemoteConfig> remotes = remoteConfigService.list(repoPath);
                    Platform.runLater(() -> {
                        remoteCombo.getItems().clear();
                        for (RemoteConfig r : remotes) {
                            remoteCombo.getItems().add(r.getName());
                        }
                        if (!remotes.isEmpty()) {
                            remoteCombo.getSelectionModel().select(0);
                            loadBranches(remotes.get(0).getName());
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        remoteCombo.getItems().add("origin");
                        remoteCombo.getSelectionModel().select(0);
                        loadBranches("origin");
                    });
                }
            } catch (Exception e) {
                log.error("加载 Remote 列表失败", e);
                Platform.runLater(() -> {
                    remoteCombo.getItems().add("origin");
                    remoteCombo.getSelectionModel().select(0);
                    loadBranches("origin");
                });
            }
        });
    }

    /**
     * 加载本地分支列表。
     * <p>通过 {@link AsyncUiLoader} 提交读任务（BR-33），完成后在 UI 线程填充下拉框并选中当前分支。</p>
     *
     * @param remote 选中远程名（仅用于触发刷新，分支为本地分支）
     */
    private void loadBranches(String remote) {
        AsyncUiLoader.submitRead(repoPath, TaskType.STATUS, () -> {
            try {
                com.gitgui.infrastructure.jgit.JGitOperationExecutor jgit =
                        com.gitgui.GitGuiApp.getInjector().getInstance(com.gitgui.infrastructure.jgit.JGitOperationExecutor.class);
                List<String> branches = jgit.listBranches(repoPath);
                String current = jgit.getCurrentBranch(repoPath);
                Platform.runLater(() -> {
                    branchCombo.getItems().clear();
                    for (String b : branches) {
                        // 去掉 refs/heads/ 前缀
                        String name = b.replace("refs/heads/", "");
                        branchCombo.getItems().add(name);
                    }
                    if (current != null) {
                        branchCombo.getSelectionModel().select(current.replace("refs/heads/", ""));
                    } else if (!branches.isEmpty()) {
                        branchCombo.getSelectionModel().select(0);
                    }
                });
            } catch (Exception e) {
                log.error("加载分支列表失败", e);
            }
        });
    }

    /**
     * 执行推送。
     * <p>校验 Remote/分支非空后（BR-09），通过 {@link GitOperationService#push} 提交异步写任务（BR-33），
     * 注册进度/成功/失败回调，成功后弹出提示并关闭对话框；命中红线或异常时弹错。</p>
     */
    private void doPush() {
        String remote = remoteCombo.getValue();
        String branch = branchCombo.getValue();
        // BR-09：必选 Remote 与分支
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
                .setUpstream(setUpstreamCheck.isSelected())
                .forceWithLease(forceWithLeaseCheck.isSelected())
                .force(forceCheck.isSelected())
                .includeTags(includeTagsCheck.isSelected())
                .pushAllBranches(pushAllBranchesCheck.isSelected())
                .pushAllTags(pushAllTagsCheck.isSelected())
                .pushToUrl(pushToUrlField.getText().trim().isEmpty() ? null : pushToUrlField.getText().trim())
                .deleteRemoteBranch(deleteRemoteBranchCheck.isSelected())
                .build();

        progressBar.setVisible(true);
        progressLabel.setVisible(true);
        progressBar.setProgress(0);
        progressLabel.setText(I18nUtil.get("button.push") + "...");

        ProgressCallback cb = new ProgressCallback() {
            @Override
            public void onProgress(int percent, String message) {
                Platform.runLater(() -> {
                    progressBar.setProgress(percent / 100.0);
                    progressLabel.setText(message);
                });
            }

            @Override
            public void onOutput(String line) {
                Platform.runLater(() -> progressLabel.setText(line));
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        };

        try {
            TaskHandle handle = gitOperationService.push(req, cb);
            handle.onSuccess(result -> Platform.runLater(() -> {
                progressBar.setProgress(1.0);
                progressLabel.setText(I18nUtil.get("push.success"));
                new Alert(Alert.AlertType.INFORMATION, I18nUtil.get("push.success")).showAndWait();
                close();
            }));
            handle.onFailure(error -> Platform.runLater(() -> {
                progressBar.setProgress(0);
                if (error instanceof RedLineBlockedException rbe) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(I18nUtil.get("redline.blocked.title"));
                    alert.setHeaderText(I18nUtil.get("redline.blocked.hitRule") + rbe.getRuleCode());
                    alert.setContentText(rbe.getMessage());
                    alert.showAndWait();
                } else {
                    progressLabel.setText(I18nUtil.get("push.failed") + error.getMessage());
                    new Alert(Alert.AlertType.ERROR, I18nUtil.get("push.failed") + error.getMessage()).showAndWait();
                }
                log.error("推送失败", error);
            }));
        } catch (RedLineBlockedException e) {
            progressBar.setVisible(false);
            progressLabel.setVisible(false);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(I18nUtil.get("redline.blocked.title"));
            alert.setHeaderText(I18nUtil.get("redline.blocked.hitRule") + e.getRuleCode());
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        } catch (Exception e) {
            log.error("推送异常", e);
            progressBar.setVisible(false);
            progressLabel.setVisible(false);
            new Alert(Alert.AlertType.ERROR, I18nUtil.get("push.error") + e.getMessage()).showAndWait();
        }
    }
}
