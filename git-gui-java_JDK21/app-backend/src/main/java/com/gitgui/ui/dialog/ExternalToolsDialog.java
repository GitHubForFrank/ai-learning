package com.gitgui.ui.dialog;

import com.gitgui.domain.constant.TaskType;
import com.gitgui.domain.service.SettingsService;
import com.gitgui.ui.AsyncUiLoader;
import com.gitgui.ui.i18n.I18nUtil;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 扩展工具配置对话框
 * <p>配置外部 Diff 工具、合并工具、外部编辑器。</p>
 * <p>原属 {@link SettingsDialog} 的「外部工具」Tab，现迁移为独立入口，
 * 挂靠在「设置」顶级菜单下作为独立子项。</p>
 *
 * @author FrankKang
 * @since 2026-07-26
 */
public class ExternalToolsDialog extends Dialog<Void> {

    private static final Logger log = LoggerFactory.getLogger(ExternalToolsDialog.class);

    private final SettingsService settingsService;

    /** Diff 工具 */
    private final TextField diffToolField = new TextField();
    /** 合并工具 */
    private final TextField mergeToolField = new TextField();
    /** 外部编辑器 */
    private final TextField editorField = new TextField();

    /**
     * 构造扩展工具配置对话框。
     *
     * @param settingsService 设置服务
     */
    public ExternalToolsDialog(SettingsService settingsService) {
        this.settingsService = settingsService;
        setTitle(I18nUtil.get("settings.external"));
        setHeaderText(null);
        initModality(Modality.APPLICATION_MODAL);

        DialogPane pane = getDialogPane();
        pane.setContent(buildContent());
        pane.getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        loadSettings();

        // OK 按钮保存设置
        Button okButton = (Button) pane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                event.consume();
                saveSettings();
            });
        }

        setResultConverter(buttonType -> null);
    }

    /**
     * 构建对话框内容（Diff 工具 / 合并工具 / 外部编辑器表单）。
     *
     * @return VBox
     */
    private VBox buildContent() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(12));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        diffToolField.setPrefWidth(380);
        mergeToolField.setPrefWidth(380);
        editorField.setPrefWidth(380);

        grid.add(new Label(I18nUtil.get("settings.external.diffTool")), 0, 0);
        grid.add(diffToolField, 1, 0);
        grid.add(new Label(I18nUtil.get("settings.external.mergeTool")), 0, 1);
        grid.add(mergeToolField, 1, 1);
        grid.add(new Label(I18nUtil.get("settings.external.editor")), 0, 2);
        grid.add(editorField, 1, 2);

        vbox.getChildren().add(grid);
        return vbox;
    }

    /**
     * 加载当前扩展工具设置到 UI。
     * <p>通过 {@link AsyncUiLoader} 提交读任务（BR-33），完成后在 UI 线程回填表单。</p>
     */
    private void loadSettings() {
        AsyncUiLoader.submitRead(null, TaskType.STATUS, () -> {
            try {
                // 配置键名与 V1 SQL/SettingsServiceImpl 保持一致（下划线风格）
                String diffTool = settingsService.get("external.diff_tool");
                String mergeTool = settingsService.get("external.merge_tool");
                String editor = settingsService.get("external.editor");

                Platform.runLater(() -> {
                    diffToolField.setText(diffTool == null ? "" : diffTool);
                    mergeToolField.setText(mergeTool == null ? "" : mergeTool);
                    editorField.setText(editor == null ? "" : editor);
                });
            } catch (Exception e) {
                log.error("加载扩展工具设置失败", e);
            }
        });
    }

    /**
     * 保存 UI 设置到服务。
     */
    private void saveSettings() {
        try {
            // 配置键名与 V1 SQL 一致：下划线风格
            settingsService.set("external.diff_tool", diffToolField.getText().trim());
            settingsService.set("external.merge_tool", mergeToolField.getText().trim());
            settingsService.set("external.editor", editorField.getText().trim());

            new Alert(Alert.AlertType.INFORMATION, I18nUtil.get("settings.saved")).showAndWait();
            close();
        } catch (Exception e) {
            log.error("保存扩展工具设置失败", e);
            new Alert(Alert.AlertType.ERROR, I18nUtil.get("settings.saveFailed") + e.getMessage()).showAndWait();
        }
    }
}
