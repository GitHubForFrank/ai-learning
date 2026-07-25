package com.gitgui.ui.dialog;

import com.gitgui.core.async.ProgressCallback;
import com.gitgui.core.async.TaskHandle;
import com.gitgui.domain.model.request.CloneRequest;
import com.gitgui.domain.service.RepositoryService;
import com.gitgui.ui.i18n.I18nUtil;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * 克隆仓库对话框
 * <p>对应 PRD 4.1，收集 URL/目标目录/分支/浅克隆深度等参数后调用 {@code RepositoryService.clone}。</p>
 * <p>遵循 BR-33：异步克隆、进度实时反馈、可取消。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class CloneDialog extends Dialog<String> {

    private static final Logger log = LoggerFactory.getLogger(CloneDialog.class);

    private final RepositoryService repositoryService;

    /** 远程 URL 输入框 */
    private final TextField urlField = new TextField();
    /** 本地目录输入框 */
    private final TextField targetDirField = new TextField();
    /** 分支输入框 */
    private final TextField branchField = new TextField();
    /** 浅克隆深度输入框 */
    private final TextField depthField = new TextField("0");
    /** SSH 密钥输入框 */
    private final TextField sshKeyField = new TextField();
    /** 裸仓库选项 */
    private final CheckBox bareCheck = new CheckBox(I18nUtil.get("clone.bare"));
    /** 稀疏检出选项 */
    private final CheckBox sparseCheck = new CheckBox(I18nUtil.get("clone.sparse"));
    /** 子目录选项 */
    private final CheckBox subdirectoryCheck = new CheckBox(I18nUtil.get("clone.subdirectory"));

    /** 进度条 */
    private final ProgressBar progressBar = new ProgressBar(0);
    /** 进度信息 */
    private final Label progressLabel = new Label();
    /** 克隆成功后的仓库路径 */
    private String clonedPath;

    /** 当前任务句柄 */
    private TaskHandle currentHandle;

    /**
     * 构造克隆对话框。
     *
     * @param repositoryService 仓库服务
     */
    public CloneDialog(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
        setTitle(I18nUtil.get("clone.title"));
        setHeaderText(null);
        initModality(Modality.APPLICATION_MODAL);

        DialogPane pane = getDialogPane();
        pane.setContent(buildContent());
        pane.getButtonTypes().addAll(ButtonType.CANCEL, new ButtonType(I18nUtil.get("button.clone"), ButtonBar.ButtonData.OK_DONE));

        // OK 按钮触发克隆
        Button okButton = (Button) pane.lookupButton(new ButtonType(I18nUtil.get("button.clone"), ButtonBar.ButtonData.OK_DONE));
        if (okButton != null) {
            okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                event.consume();
                doClone();
            });
        }
        // CANCEL 按钮触发取消
        Button cancelButton = (Button) pane.lookupButton(ButtonType.CANCEL);
        if (cancelButton != null) {
            cancelButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                if (currentHandle != null && currentHandle.getStatus() == com.gitgui.core.constant.TaskStatus.RUNNING) {
                    currentHandle.cancel();
                }
            });
        }

        setResultConverter(buttonType -> {
            if (clonedPath != null) {
                return clonedPath;
            }
            return null;
        });
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

        urlField.setPrefWidth(400);
        targetDirField.setPrefWidth(320);
        Button browseBtn = new Button(I18nUtil.get("button.browse"));
        browseBtn.setOnAction(e -> browseTargetDir());

        grid.add(new Label(I18nUtil.get("clone.url")), 0, 0);
        grid.add(urlField, 1, 0, 2, 1);
        grid.add(new Label(I18nUtil.get("clone.targetDir")), 0, 1);
        grid.add(targetDirField, 1, 1);
        grid.add(browseBtn, 2, 1);
        grid.add(new Label(I18nUtil.get("clone.branch")), 0, 2);
        grid.add(branchField, 1, 2, 2, 1);
        grid.add(new Label(I18nUtil.get("clone.depth")), 0, 3);
        grid.add(depthField, 1, 3);
        grid.add(new Label(I18nUtil.get("clone.sshKey")), 0, 4);
        grid.add(sshKeyField, 1, 4, 2, 1);

        HBox optionsBox = new HBox(15, bareCheck, sparseCheck, subdirectoryCheck);
        grid.add(optionsBox, 1, 5, 2, 1);

        progressBar.setPrefWidth(540);
        progressBar.setVisible(false);
        progressLabel.setVisible(false);

        vbox.getChildren().addAll(grid, progressBar, progressLabel);
        return vbox;
    }

    /**
     * 选择本地目录。
     */
    private void browseTargetDir() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(I18nUtil.get("clone.targetDir"));
        File selected = chooser.showDialog(getOwner());
        if (selected != null) {
            targetDirField.setText(selected.getAbsolutePath());
        }
    }

    /**
     * 执行克隆。
     * <p>校验 URL/目标目录非空后，通过 {@link RepositoryService#clone} 提交异步写任务（BR-33），
     * 注册进度/成功/失败回调，成功后记录克隆路径并自动关闭对话框，失败时显示错误信息。</p>
     */
    private void doClone() {
        String url = urlField.getText().trim();
        String target = targetDirField.getText().trim();
        if (url.isEmpty() || target.isEmpty()) {
            progressLabel.setText(I18nUtil.get("clone.urlAndTargetRequired"));
            progressLabel.setVisible(true);
            return;
        }

        int depth;
        try {
            depth = Integer.parseInt(depthField.getText().trim());
        } catch (NumberFormatException e) {
            depth = 0;
        }

        CloneRequest req = CloneRequest.builder()
                .remoteUrl(url)
                .targetDir(target)
                .branch(branchField.getText().trim().isEmpty() ? null : branchField.getText().trim())
                .sshKey(sshKeyField.getText().trim().isEmpty() ? null : sshKeyField.getText().trim())
                .depth(depth)
                .bare(bareCheck.isSelected())
                .sparseCheckout(sparseCheck.isSelected())
                .subdirectory(subdirectoryCheck.isSelected())
                .build();

        progressBar.setVisible(true);
        progressLabel.setVisible(true);
        progressBar.setProgress(0);
        progressLabel.setText(I18nUtil.get("button.clone") + "...");

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
                return currentHandle != null && currentHandle.getStatus() == com.gitgui.core.constant.TaskStatus.CANCELLED;
            }
        };

        try {
            currentHandle = repositoryService.clone(req, cb);
            currentHandle.onSuccess(result -> Platform.runLater(() -> {
                progressBar.setProgress(1.0);
                progressLabel.setText(I18nUtil.get("clone.success"));
                clonedPath = req.getTargetDir();
                // 自动关闭对话框
                close();
            }));
            currentHandle.onFailure(error -> Platform.runLater(() -> {
                progressBar.setProgress(0);
                progressLabel.setText(I18nUtil.get("clone.failed") + error.getMessage());
                log.error("克隆失败", error);
            }));
        } catch (Exception e) {
            log.error("克隆异常", e);
            progressLabel.setText(I18nUtil.get("clone.error") + e.getMessage());
        }
    }

    /**
     * 获取克隆成功的本地路径。
     *
     * @return 路径，未成功克隆返回 null
     */
    public String getClonedPath() {
        return clonedPath;
    }
}
