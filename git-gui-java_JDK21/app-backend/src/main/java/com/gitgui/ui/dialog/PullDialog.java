package com.gitgui.ui.dialog;

import com.gitgui.core.async.ProgressCallback;
import com.gitgui.core.async.TaskHandle;
import com.gitgui.core.exception.RedLineBlockedException;
import com.gitgui.domain.constant.TaskType;
import com.gitgui.domain.model.RemoteConfig;
import com.gitgui.domain.model.request.PullRequest;
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
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 拉取对话框
 * <p>对应 PRD 4.5.1，收集 Remote/分支/AutoStash/变基等参数后调用 {@code GitOperationService.pull}。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class PullDialog extends Dialog<Void> {

    private static final Logger log = LoggerFactory.getLogger(PullDialog.class);

    private final GitOperationService gitOperationService;
    private final String repoPath;

    /** Remote 下拉 */
    private final ComboBox<String> remoteCombo = new ComboBox<>();
    /** 分支下拉 */
    private final ComboBox<String> branchCombo = new ComboBox<>();
    /** AutoStash 选项 */
    private final CheckBox autoStashCheck = new CheckBox(I18nUtil.get("pull.autoStash"));
    /** 变基而非合并 */
    private final CheckBox rebaseCheck = new CheckBox(I18nUtil.get("pull.rebase"));
    /** 拉取标签 */
    private final CheckBox fetchTagsCheck = new CheckBox(I18nUtil.get("pull.fetchTags"));
    /** 所有分支 */
    private final CheckBox allBranchesCheck = new CheckBox(I18nUtil.get("pull.allBranches"));
    /** 更新子模块 */
    private final CheckBox updateSubmodulesCheck = new CheckBox(I18nUtil.get("pull.updateSubmodules"));
    /** 预演 */
    private final CheckBox dryRunCheck = new CheckBox(I18nUtil.get("pull.dryRun"));

    /** 进度条 */
    private final ProgressBar progressBar = new ProgressBar(0);
    /** 进度信息 */
    private final Label progressLabel = new Label();

    /** 远程配置服务 */
    private RemoteConfigService remoteConfigService;

    /**
     * 构造拉取对话框。
     *
     * @param gitOperationService Git 操作服务
     * @param repoPath 仓库路径
     */
    public PullDialog(GitOperationService gitOperationService, String repoPath) {
        this.gitOperationService = gitOperationService;
        this.repoPath = repoPath;
        setTitle(I18nUtil.get("pull.title"));
        setHeaderText(null);
        initModality(Modality.APPLICATION_MODAL);

        DialogPane pane = getDialogPane();
        pane.setContent(buildContent());
        pane.getButtonTypes().addAll(ButtonType.CANCEL, new ButtonType(I18nUtil.get("button.pull"), ButtonBar.ButtonData.OK_DONE));

        // 从 Guice 获取 RemoteConfigService
        try {
            remoteConfigService = com.gitgui.GitGuiApp.getInjector().getInstance(RemoteConfigService.class);
        } catch (Exception e) {
            log.warn("无法获取 RemoteConfigService，使用默认 origin", e);
        }

        loadRemotes();

        // OK 按钮触发拉取
        Button okButton = (Button) pane.lookupButton(new ButtonType(I18nUtil.get("button.pull"), ButtonBar.ButtonData.OK_DONE));
        if (okButton != null) {
            okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                event.consume();
                doPull();
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

        grid.add(new Label(I18nUtil.get("pull.remote")), 0, 0);
        grid.add(remoteCombo, 1, 0);
        grid.add(new Label(I18nUtil.get("pull.branch")), 0, 1);
        grid.add(branchCombo, 1, 1);

        HBox optionsRow1 = new HBox(15, autoStashCheck, rebaseCheck, fetchTagsCheck);
        HBox optionsRow2 = new HBox(15, allBranchesCheck, updateSubmodulesCheck, dryRunCheck);
        grid.add(optionsRow1, 1, 2);
        grid.add(optionsRow2, 1, 3);

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
     * 加载远程分支列表。
     * <p>通过 {@link AsyncUiLoader} 提交读任务（BR-33），完成后在 UI 线程填充下拉框并选中当前分支。</p>
     *
     * @param remote 远程名
     */
    private void loadBranches(String remote) {
        AsyncUiLoader.submitRead(repoPath, TaskType.STATUS, () -> {
            try {
                com.gitgui.infrastructure.cli.CliGitExecutor gitExecutor =
                        com.gitgui.GitGuiApp.getInjector().getInstance(com.gitgui.infrastructure.cli.CliGitExecutor.class);
                // 先 fetch 一次（避免远程分支列表过时），这里简化：直接列本地 + 远程分支
                List<String> branches = gitExecutor.listBranches(repoPath);
                String current = gitExecutor.getCurrentBranch(repoPath);
                Platform.runLater(() -> {
                    branchCombo.getItems().clear();
                    for (String b : branches) {
                        String name = b.replace("refs/heads/", "").replace("refs/remotes/" + remote + "/", "");
                        if (!name.equals("HEAD")) {
                            branchCombo.getItems().add(name);
                        }
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
     * 执行拉取。
     * <p>校验 Remote/分支非空后，通过 {@link GitOperationService#pull} 提交异步写任务（BR-33），
     * 注册进度/成功/失败回调，成功后弹出提示并关闭对话框，失败时弹错。</p>
     */
    private void doPull() {
        String remote = remoteCombo.getValue();
        String branch = branchCombo.getValue();
        if (remote == null || remote.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, I18nUtil.get("pull.selectRemote")).showAndWait();
            return;
        }
        if (branch == null || branch.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, I18nUtil.get("pull.selectBranch")).showAndWait();
            return;
        }

        PullRequest req = PullRequest.builder()
                .repoPath(repoPath)
                .remote(remote)
                .branch(branch)
                .autoStash(autoStashCheck.isSelected())
                .rebaseInsteadOfMerge(rebaseCheck.isSelected())
                .fetchTags(fetchTagsCheck.isSelected())
                .allBranches(allBranchesCheck.isSelected())
                .updateSubmodules(updateSubmodulesCheck.isSelected())
                .dryRun(dryRunCheck.isSelected())
                .build();

        // 先拿到 stage 引用，再关闭（与 Commit push 模式一致）
        Stage owner = (getDialogPane().getScene() == null) ? null
                : (Stage) getDialogPane().getScene().getWindow();
        close();

        // 延迟一帧打开 ProgressDialog，确保 PullDialog 完全关闭后再渲染进度窗口
        Platform.runLater(() -> {
            ProgressDialog progress = new ProgressDialog(
                    owner,
                    I18nUtil.get("pull.title") + "  ←  " + remote + "/" + branch,
                    I18nUtil.get("progress.headerOperating")
            );
            ProgressCallback sharedCb = progress.asCallback();
            try {
                TaskHandle handle = gitOperationService.pull(req, sharedCb);
                progress.attach(handle);
                // 拉取完成后进度条到 100%，console 显示结果，用户手动关闭
                progress.showAndWaitForTask();
            } catch (RedLineBlockedException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(I18nUtil.get("redline.blocked.title"));
                alert.setHeaderText(I18nUtil.get("redline.blocked.hitRule") + e.getRuleCode());
                alert.setContentText(e.getMessage());
                alert.showAndWait();
                Platform.runLater(progress::close);
            } catch (Exception e) {
                log.error("拉取异常", e);
                new Alert(Alert.AlertType.ERROR, I18nUtil.get("pull.error") + e.getMessage()).showAndWait();
                Platform.runLater(progress::close);
            }
        });
    }
}
