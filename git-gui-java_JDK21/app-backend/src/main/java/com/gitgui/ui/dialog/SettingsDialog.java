package com.gitgui.ui.dialog;

import com.gitgui.core.util.JsonUtil;
import com.gitgui.domain.constant.TaskType;
import com.gitgui.domain.model.SensitiveFileRule;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 执行红线设置对话框
 * <p>对应 PRD 4.17 命令红线配置：总开关、保护分支清单、远程白名单、敏感文件规则、超大文件阈值。</p>
 * <p>界面（主题/语言）与外部工具配置已迁移到外层「设置」菜单的独立入口，
 * 本对话框仅保留命令红线相关配置。</p>
 * <p>遵循 BR-27、BR-28、BR-29、BR-30、BR-32、BR-37、BR-38、BR-39。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class SettingsDialog extends Dialog<Void> {

    private static final Logger log = LoggerFactory.getLogger(SettingsDialog.class);

    private final SettingsService settingsService;

    /** 红线总开关 */
    private final CheckBox redlineEnabledCheck = new CheckBox(I18nUtil.get("settings.redline.enabled"));
    /** 保护分支清单 */
    private final TextArea protectedBranchesArea = new TextArea();
    /** 远程白名单 */
    private final TextArea remoteWhitelistArea = new TextArea();
    /** 敏感文件规则 */
    private final TextArea sensitiveFileRulesArea = new TextArea();
    /** 超大文件阈值 */
    private final TextField largeFileThresholdField = new TextField();
    /** 已加载的敏感文件规则（保存时保留其 description，避免覆盖丢失） */
    private List<SensitiveFileRule> loadedSensitiveRules = Collections.emptyList();

    /**
     * 构造执行红线设置对话框。
     *
     * @param settingsService 设置服务
     */
    public SettingsDialog(SettingsService settingsService) {
        this.settingsService = settingsService;
        setTitle(I18nUtil.get("settings.title"));
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
     * 构建对话框内容（命令红线配置表单）。
     * <p>移除原 TabPane 容器，直接展示红线表单（界面/外部工具 Tab 已迁出）。</p>
     *
     * @return VBox
     */
    private VBox buildContent() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(12));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(redlineEnabledCheck, 0, 0, 2, 1);

        grid.add(new Label(I18nUtil.get("settings.redline.protectedBranches")), 0, 1);
        protectedBranchesArea.setPrefRowCount(4);
        protectedBranchesArea.setWrapText(true);
        protectedBranchesArea.setPromptText("每行一个分支名或通配符，如：\nmain\nmaster\nrelease/*");
        grid.add(protectedBranchesArea, 1, 1);

        grid.add(new Label(I18nUtil.get("settings.redline.remoteWhitelist")), 0, 2);
        remoteWhitelistArea.setPrefRowCount(4);
        remoteWhitelistArea.setWrapText(true);
        remoteWhitelistArea.setPromptText("每行一个 host，如：\ngithub.com\ngitee.com\ngitlab.com");
        grid.add(remoteWhitelistArea, 1, 2);

        grid.add(new Label(I18nUtil.get("settings.redline.sensitiveFileRules")), 0, 3);
        sensitiveFileRulesArea.setPrefRowCount(4);
        sensitiveFileRulesArea.setWrapText(true);
        sensitiveFileRulesArea.setPromptText("每行一个正则表达式，如：\n.*\\.env\n.*credentials.*\n.*\\.pem$");
        grid.add(sensitiveFileRulesArea, 1, 3);

        grid.add(new Label(I18nUtil.get("settings.redline.largeFileThreshold")), 0, 4);
        grid.add(largeFileThresholdField, 1, 4);

        vbox.getChildren().add(grid);
        return vbox;
    }

    /**
     * 加载当前红线设置到 UI。
     * <p>通过 {@link AsyncUiLoader} 提交读任务（BR-33），完成后在 UI 线程回填表单。</p>
     */
    private void loadSettings() {
        AsyncUiLoader.submitRead(null, TaskType.STATUS, () -> {
            try {
                boolean enabled = settingsService.isRedLineEnabled();
                List<String> protectedBranches = settingsService.getProtectedBranches();
                List<String> remoteWhitelist = settingsService.getRemoteWhitelist();
                List<SensitiveFileRule> sensitiveRules = settingsService.getSensitiveFileRules();
                // 缓存已加载规则，保存时按 pattern 保留 description，避免覆盖丢失
                loadedSensitiveRules = sensitiveRules == null ? Collections.emptyList() : sensitiveRules;
                int largeFileThreshold = settingsService.getLargeFileThresholdMb();

                Platform.runLater(() -> {
                    redlineEnabledCheck.setSelected(enabled);
                    protectedBranchesArea.setText(String.join("\n", protectedBranches));
                    remoteWhitelistArea.setText(String.join("\n", remoteWhitelist));
                    StringBuilder sb = new StringBuilder();
                    for (SensitiveFileRule rule : loadedSensitiveRules) {
                        sb.append(rule.getPattern()).append("\n");
                    }
                    sensitiveFileRulesArea.setText(sb.toString().trim());
                    largeFileThresholdField.setText(String.valueOf(largeFileThreshold));
                });
            } catch (Exception e) {
                log.error("加载设置失败", e);
            }
        });
    }

    /**
     * 保存 UI 设置到服务。
     */
    private void saveSettings() {
        try {
            // 红线总开关（BR-30，切换本身写入 audit_log）
            settingsService.setRedLineEnabled(redlineEnabledCheck.isSelected());

            // 保护分支清单
            List<String> branches = parseLines(protectedBranchesArea.getText());
            settingsService.setProtectedBranches(branches);

            // 远程白名单
            List<String> whitelist = parseLines(remoteWhitelistArea.getText());
            settingsService.setRemoteWhitelist(whitelist);

            // 敏感文件规则：保存为 JSON 数组到 red_line.sensitive_file_rules（BR-32）
            // 配置键名与 V1 SQL/SettingsServiceImpl 保持一致；保留已加载规则的 description
            String sensitiveText = sensitiveFileRulesArea.getText().trim();
            List<SensitiveFileRule> rulesToSave = new ArrayList<>();
            if (!sensitiveText.isEmpty()) {
                // 以 pattern 为键建立已加载规则的索引，便于保留 description
                Map<String, String> existingDesc = new HashMap<>();
                for (SensitiveFileRule r : loadedSensitiveRules) {
                    if (r.getPattern() != null) {
                        existingDesc.put(r.getPattern(), r.getDescription());
                    }
                }
                for (String line : sensitiveText.split("\n")) {
                    String pattern = line.trim();
                    if (!pattern.isEmpty()) {
                        String desc = existingDesc.getOrDefault(pattern, "");
                        rulesToSave.add(SensitiveFileRule.builder().pattern(pattern).description(desc).build());
                    }
                }
            }
            settingsService.set("red_line.sensitive_file_rules", JsonUtil.toJson(rulesToSave));

            // 超大文件阈值（键名与 V1 SQL 一致）
            int threshold;
            try {
                threshold = Integer.parseInt(largeFileThresholdField.getText().trim());
            } catch (NumberFormatException e) {
                threshold = 100;
            }
            settingsService.set("red_line.large_file_threshold_mb", String.valueOf(threshold));

            new Alert(Alert.AlertType.INFORMATION, I18nUtil.get("settings.saved")).showAndWait();
            close();
        } catch (Exception e) {
            log.error("保存设置失败", e);
            new Alert(Alert.AlertType.ERROR, I18nUtil.get("settings.saveFailed") + e.getMessage()).showAndWait();
        }
    }

    /**
     * 将多行文本解析为列表。
     *
     * @param text 多行文本
     * @return 非空行列表
     */
    private List<String> parseLines(String text) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return result;
        }
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
