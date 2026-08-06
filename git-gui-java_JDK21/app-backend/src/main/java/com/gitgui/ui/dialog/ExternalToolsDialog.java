package com.gitgui.ui.dialog;

import com.gitgui.domain.constant.TaskType;
import com.gitgui.domain.service.SettingsService;
import com.gitgui.ui.AsyncUiLoader;
import com.gitgui.ui.i18n.I18nUtil;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 扩展工具配置对话框
 * <p>v3 简化：</p>
 * <ul>
 *   <li><b>外部 Diff 工具 / 合并工具</b> ── 配置入口已下线。
 *       用户直接修改 {@code ~/.gitconfig} 中的 {@code [diff]tool} + {@code [difftool "..."]path} 即可。
 *       DiffViewerDialog 会自动读取 .gitconfig 并调用 {@code git difftool}。</li>
 *   <li><b>外部编辑器</b> ── 保留，git-gui 用于「在编辑器中打开文件」的场景。</li>
 * </ul>
 *
 * @author FrankKang
 * @since 2026-07-26
 */
public class ExternalToolsDialog extends Dialog<Void> {

    private static final Logger log = LoggerFactory.getLogger(ExternalToolsDialog.class);
    /**
     * 设置键：外部编辑器
     */
    private static final String SETTING_EXTERNAL_EDITOR = "external.editor";
    /**
     * Diff / Merge 工具设置键 —— 已废弃，保留只为兼容旧 SettingsService 中可能存在的数据。
     * 新版不再维护，写入也无意义。
     */
    private static final String[] LEGACY_TOOL_KEYS = {"external.diff_tool", "external.merge_tool"};
    private final SettingsService settingsService;
    /**
     * 外部编辑器（保留：用于「在编辑器中打开文件」）
     */
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
        pane.getButtonTypes()
            .addAll(ButtonType.CANCEL, ButtonType.OK);

        loadSettings();

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
     * 构建对话框内容：仅"外部编辑器"一项 + Diff/Merge 工具说明（指向 .gitconfig）。
     */
    private VBox buildContent() {
        VBox vbox = new VBox(12);
        vbox.setPadding(new Insets(12));

        // Diff / Merge 工具：提示改用 .gitconfig
        Label diffInfo = new Label(I18nUtil.get("settings.external.diffToolGitconfigHint"));
        diffInfo.setWrapText(true);
        diffInfo.setStyle("-fx-text-fill: #1976d2; -fx-padding: 0 0 4 0;");

        Label externalEditorLabel = new Label(I18nUtil.get("settings.external.editor"));
        editorField.setPrefWidth(380);
        editorField.setPromptText(I18nUtil.get("settings.external.editorPrompt"));

        vbox.getChildren()
            .addAll(diffInfo, externalEditorLabel, editorField);
        return vbox;
    }

    /**
     * 加载当前扩展工具设置到 UI（仅外部编辑器）。
     */
    private void loadSettings() {
        AsyncUiLoader.submitRead(null, TaskType.STATUS, () -> {
            try {
                String editor = settingsService.get(SETTING_EXTERNAL_EDITOR);
                Platform.runLater(() -> editorField.setText(editor == null ? "" : editor));
            } catch (Exception e) {
                log.error("加载外部编辑器设置失败", e);
            }
        });
    }

    /**
     * 保存 UI 设置到服务（仅外部编辑器）。
     * <p>Diff / Merge 工具的旧设置数据（external.diff_tool / external.merge_tool）保留不动，
     * 让现有用户的数据不被强制擦除；后续新版本可加一键清理。</p>
     */
    private void saveSettings() {
        try {
            settingsService.set(SETTING_EXTERNAL_EDITOR, editorField.getText()
                                                                    .trim());

            new Alert(Alert.AlertType.INFORMATION, I18nUtil.get("settings.saved")).showAndWait();
            close();
        } catch (Exception e) {
            log.error("保存扩展工具设置失败", e);
            new Alert(Alert.AlertType.ERROR, I18nUtil.get("settings.saveFailed") + e.getMessage()).showAndWait();
        }
    }

    /**
     * 清理已废弃的旧设置键（在「设置」页面右上角加清理按钮时调用）。
     * <p>当前未触发，保留为后续扩展用。</p>
     */
    @SuppressWarnings("unused")
    private void clearLegacySettings() {
        for (String key : LEGACY_TOOL_KEYS) {
            try {
                settingsService.set(key, "");
            } catch (Exception ignored) {
                // ignore
            }
        }
    }
}
