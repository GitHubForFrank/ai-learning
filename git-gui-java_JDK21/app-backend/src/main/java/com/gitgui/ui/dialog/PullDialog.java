package com.gitgui.ui.dialog;

import com.gitgui.domain.constant.TaskType;
import com.gitgui.domain.model.RemoteConfig;
import com.gitgui.domain.model.request.PullRequest;
import com.gitgui.domain.service.GitOperationService;
import com.gitgui.domain.service.RemoteConfigService;
import com.gitgui.infrastructure.cli.CliGitExecutor;
import com.gitgui.ui.AsyncUiLoader;
import com.gitgui.ui.i18n.I18nUtil;
import java.text.MessageFormat;
import java.util.List;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 拉取对话框（精简版）—— 参数收集器
 * <p>对应 PRD 4.5.1：收集 Remote / 分支 两个必选项后返回 {@link PullRequest}，
 * 由 {@link com.gitgui.ui.main.MainController#onPull()} 在 MainController 栈上
 * 走完整的 {@link ProgressDialog} 流程（与 {@code onFetch} 行为一致）。</p>
 * <p>对比旧版：</p>
 * <ul>
 *   <li>去掉了 AutoStash / 变基 / 拉取标签 / 所有分支 / 更新子模块 / 预演 6 个低频/进阶选项</li>
 *   <li>去掉了拉取执行逻辑（移至 MainController.onPull，避免从模态 Dialog 内部开 ProgressDialog
 *       导致窗口被 modality 屏蔽掉的问题）</li>
 * </ul>
 * <p>标题动态注入当前分支：{@code pull.titleWithBranch}（i18n 提供占位符 {0}）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class PullDialog extends Dialog<PullRequest> {

    private static final Logger log = LoggerFactory.getLogger(PullDialog.class);

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
     * 远程配置服务
     */
    private RemoteConfigService remoteConfigService;
    /**
     * CliGitExecutor（用于同步取当前分支，作为对话框标题显示）
     */
    private CliGitExecutor gitExecutor;

    /**
     * 构造拉取对话框。
     *
     * @param gitOperationService Git 操作服务（保留以备后续扩展）
     * @param repoPath            仓库路径
     */
    public PullDialog(GitOperationService gitOperationService, String repoPath) {
        this.gitOperationService = gitOperationService;
        this.repoPath = repoPath;

        initModality(Modality.APPLICATION_MODAL);

        // 从 Guice 获取 RemoteConfigService、CliGitExecutor
        try {
            remoteConfigService = com.gitgui.GitGuiApp.getInjector()
                                                      .getInstance(RemoteConfigService.class);
        } catch (Exception e) {
            log.warn("无法获取 RemoteConfigService，使用默认 origin", e);
        }
        try {
            gitExecutor = com.gitgui.GitGuiApp.getInjector()
                                              .getInstance(CliGitExecutor.class);
        } catch (Exception e) {
            log.warn("无法获取 CliGitExecutor，对话框标题回退为不带分支名", e);
        }
        // 标题：拉取 [当前分支]；若取不到分支则仅显示"拉取"
        applyDialogTitle();

        DialogPane pane = getDialogPane();
        pane.setContent(buildContent());
        ButtonType okType = new ButtonType(I18nUtil.get("button.pull"), ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes()
            .addAll(ButtonType.CANCEL, okType);

        loadRemotes();

        // 校验逻辑放在 OK 按钮的 onAction：参数不合法时调用 event.consume()，
        // JavaFX 会中止后续关闭流程（包括不再调用 setResultConverter），
        // 让对话框保持打开，用户重新选择。
        Button okButton = (Button) pane.lookupButton(okType);
        if (okButton != null) {
            okButton.setOnAction(e -> {
                String remote = remoteCombo.getValue();
                String branch = branchCombo.getValue();
                if (remote == null || remote.isEmpty()) {
                    new Alert(Alert.AlertType.WARNING, I18nUtil.get("pull.selectRemote")).showAndWait();
                    e.consume();
                    return;
                }
                if (branch == null || branch.isEmpty()) {
                    new Alert(Alert.AlertType.WARNING, I18nUtil.get("pull.selectBranch")).showAndWait();
                    e.consume();
                    return;
                }
                // 参数有效：不 consume，让 JavaFX 继续调用 setResultConverter
            });
        }

        // setResultConverter 在按钮点击 + onAction 不 consume 时被调用，
        // 收集最终参数构造 PullRequest 作为 showAndWait() 的返回值。
        setResultConverter(buttonType -> {
            if (buttonType != okType) {
                return null;
            }
            String remote = remoteCombo.getValue();
            String branch = branchCombo.getValue();
            if (remote == null || remote.isEmpty() || branch == null || branch.isEmpty()) {
                // 理论上 onAction 已经校验过，这里是兜底
                return null;
            }
            return PullRequest.builder()
                              .repoPath(repoPath)
                              .remote(remote)
                              .branch(branch)
                              .build();
        });
    }

    /**
     * 应用对话框标题：尝试用 {@code pull.titleWithBranch} 注入当前分支名。
     * 当前分支未知时回退到 {@code pull.title}。
     */
    private void applyDialogTitle() {
        String branch = resolveCurrentBranchSafe();
        if (branch == null || branch.isBlank()) {
            setTitle(I18nUtil.get("pull.title"));
            return;
        }
        String template = I18nUtil.get("pull.titleWithBranch");
        try {
            setTitle(MessageFormat.format(template, branch));
        } catch (Exception e) {
            setTitle(I18nUtil.get("pull.title") + "  " + branch);
        }
    }

    /**
     * 同步读取当前分支（仅用于标题展示，失败返回空串，不影响功能）。
     */
    private String resolveCurrentBranchSafe() {
        if (gitExecutor == null) {
            return "";
        }
        try {
            return gitExecutor.getCurrentBranch(repoPath);
        } catch (Exception e) {
            log.debug("取当前分支失败：repoPath={}", repoPath, e);
            return "";
        }
    }

    /**
     * 构建对话框内容（精简版：只保留 Remote + Branch）。
     */
    private VBox buildContent() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(12));
        vbox.setPrefWidth(420);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label(I18nUtil.get("pull.remote")), 0, 0);
        grid.add(remoteCombo, 1, 0);
        grid.add(new Label(I18nUtil.get("pull.branch")), 0, 1);
        grid.add(branchCombo, 1, 1);

        vbox.getChildren()
            .addAll(grid);
        return vbox;
    }

    /**
     * 加载 Remote 列表（异步）。完成后默认选第一个 + 加载分支。
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
     * 加载分支列表（异步）。完成后默认选中当前分支，并刷新标题。
     */
    private void loadBranches(String remote) {
        AsyncUiLoader.submitRead(repoPath, TaskType.STATUS, () -> {
            try {
                CliGitExecutor exec = this.gitExecutor;
                if (exec == null) {
                    exec = com.gitgui.GitGuiApp.getInjector()
                                               .getInstance(CliGitExecutor.class);
                }
                List<String> branches = exec.listBranches(repoPath);
                String current = exec.getCurrentBranch(repoPath);
                String curShort = current == null ? "" : current.replace("refs/heads/", "");
                Platform.runLater(() -> {
                    branchCombo.getItems()
                               .clear();
                    for (String b : branches) {
                        String name = b.replace("refs/heads/", "")
                                       .replace("refs/remotes/" + remote + "/", "");
                        if (!name.equals("HEAD")) {
                            branchCombo.getItems()
                                       .add(name);
                        }
                    }
                    if (!curShort.isEmpty()) {
                        branchCombo.getSelectionModel()
                                   .select(curShort);
                    } else if (!branches.isEmpty()) {
                        branchCombo.getSelectionModel()
                                   .select(0);
                    }
                    // 用异步拿到的当前分支更新对话框标题
                    if (!curShort.isEmpty()) {
                        String template = I18nUtil.get("pull.titleWithBranch");
                        try {
                            setTitle(MessageFormat.format(template, curShort));
                        } catch (Exception ignored) {
                            setTitle(I18nUtil.get("pull.title") + "  " + curShort);
                        }
                    }
                });
            } catch (Exception e) {
                log.error("加载分支列表失败", e);
            }
        });
    }

}