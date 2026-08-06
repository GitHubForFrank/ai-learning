package com.gitgui.ui.dialog;

import com.gitgui.domain.constant.TaskType;
import com.gitgui.infrastructure.cli.CliGitExecutor;
import com.gitgui.ui.AsyncUiLoader;
import com.gitgui.ui.i18n.I18nUtil;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文件 Diff 查看器（TortoiseGit 风格）
 * <p>对应 PRD 4.3 "双击文件查看 diff" 场景。</p>
 * <p>v2 增强：提供 3 种查看方式：</p>
 * <ul>
 *   <li>① <b>Git Diff</b>：原始 unified diff 文本（保留）</li>
 *   <li>② <b>Side by Side</b>：左右双栏对比（解析 unified diff）</li>
 *   <li>③ <b>External Tool</b>：调用配置的 Beyond Compare 等外部对比工具</li>
 * </ul>
 *
 * @author FrankKang
 * @since 2026-07-25
 */
public class DiffViewerDialog {

    private static final Logger log = LoggerFactory.getLogger(DiffViewerDialog.class);

    /**
     * git config 键：外部 diff 工具名（必填）
     */
    private static final String GIT_CONFIG_DIFF_TOOL = "diff.tool";
    /**
     * git config 键：外部 diff 工具路径（必填）
     */
    private static final String GIT_CONFIG_DIFFTOOL_NAME = "difftool.";

    /**
     * 旧版本临时文件名前缀（删除时用）
     */
    private static final String OLD_TEMP_PREFIX = "git-gui-diff-old-";
    /**
     * 当前 UI 上保留的旧版本临时文件（删除时使用），在窗口关闭时清理
     */
    private static File currentOldTempFile;

    private DiffViewerDialog() {
        // 工具类，不实例化
    }

    /**
     * 弹出 Diff 查看窗口（异步：在工作线程算 diff，主线程显示）。
     */
    public static void show(Stage owner, String repoPath, String filePath, com.gitgui.domain.model.FileStatus.FileState fileState, String oldRev,
            String newRev, DiffProvider diffProvider) {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.UTILITY);
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(Modality.NONE);
        stage.setTitle(filePath + " - " + I18nUtil.get("commit.diffTitle"));

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // ===== 顶部：文件名 + 状态 + 模式选择器 =====
        Label fileLabel = new Label(I18nUtil.get("commit.diffFileLabel") + " " + filePath);
        fileLabel.setStyle("-fx-font-weight: bold;");
        Label stateLabel = new Label(formatState(fileState));
        stateLabel.setStyle(stateStyle(fileState));

        ToggleGroup modeGroup = new ToggleGroup();
        ToggleButton btnGitDiff = new ToggleButton(I18nUtil.get("commit.diffModeGitDiff"));
        ToggleButton btnSideBySide = new ToggleButton(I18nUtil.get("commit.diffModeSideBySide"));
        ToggleButton btnExternal = new ToggleButton(I18nUtil.get("commit.diffModeExternalTool"));
        btnGitDiff.setToggleGroup(modeGroup);
        btnSideBySide.setToggleGroup(modeGroup);
        btnExternal.setToggleGroup(modeGroup);
        btnGitDiff.setUserData(DiffMode.GIT_DIFF);
        btnSideBySide.setUserData(DiffMode.SIDE_BY_SIDE);
        btnExternal.setUserData(DiffMode.EXTERNAL_TOOL);
        btnGitDiff.setSelected(true);

        Label viewModeLabel = new Label(I18nUtil.get("commit.diffViewLabel"));
        HBox modeBar = new HBox(8, viewModeLabel, btnGitDiff, btnSideBySide, btnExternal);

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);
        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        top.getChildren()
           .addAll(fileLabel, stateLabel, topSpacer, modeBar);
        root.setTop(top);

        // ===== 中间：3 个 pane，根据模式切换 =====
        // ① Git Diff pane（原有）
        TextArea diffArea = new TextArea();
        diffArea.setEditable(false);
        diffArea.setWrapText(false);
        diffArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 12px;");
        diffArea.setText(I18nUtil.get("common.loading"));
        BorderPane.setMargin(diffArea, new Insets(8, 0, 8, 0));

        // ② Side by Side pane
        TextArea oldArea = new TextArea();
        oldArea.setEditable(false);
        oldArea.setWrapText(false);
        oldArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 12px;");
        oldArea.setPromptText(I18nUtil.get("commit.diffOldLabel"));
        TextArea newArea = new TextArea();
        newArea.setEditable(false);
        newArea.setWrapText(false);
        newArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 12px;");
        newArea.setPromptText(I18nUtil.get("commit.diffNewLabel"));
        VBox oldBox = new VBox(2, new Label(I18nUtil.get("commit.diffOldLabel")), oldArea);
        VBox newBox = new VBox(2, new Label(I18nUtil.get("commit.diffNewLabel")), newArea);
        oldBox.setPrefWidth(450);
        newBox.setPrefWidth(450);
        HBox sideBySidePane = new HBox(8, oldBox, newBox);
        HBox.setHgrow(oldBox, Priority.ALWAYS);
        HBox.setHgrow(newBox, Priority.ALWAYS);
        oldArea.setPrefHeight(800);
        newArea.setPrefHeight(800);
        sideBySidePane.setVisible(false);
        sideBySidePane.setManaged(false);

        // ③ External Tool pane
        Label externalStatusLabel = new Label(I18nUtil.get("common.loading"));
        externalStatusLabel.setWrapText(true);
        Button launchExternalBtn = new Button(I18nUtil.get("commit.diffModeExternalTool"));
        launchExternalBtn.setDefaultButton(false);
        VBox externalPane = new VBox(8, externalStatusLabel, launchExternalBtn);
        externalPane.setAlignment(Pos.CENTER);
        externalPane.setVisible(false);
        externalPane.setManaged(false);

        // 中心切换：根据模式显示对应 pane
        BorderPane centerContainer = new BorderPane();
        centerContainer.setCenter(diffArea);
        root.setCenter(centerContainer);

        // ===== 底部：Close 按钮 =====
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button closeBtn = new Button(I18nUtil.get("button.close"));
        closeBtn.setDefaultButton(true);
        closeBtn.setOnAction(e -> {
            cleanupTempFiles();
            stage.close();
        });
        HBox bottom = new HBox(spacer, closeBtn);
        bottom.setAlignment(Pos.CENTER_RIGHT);
        root.setBottom(bottom);

        Scene scene = new Scene(root, 1000, 640);
        stage.setScene(scene);
        stage.show();

        // 切换 mode 的事件处理
        modeGroup.selectedToggleProperty()
                 .addListener((obs, old, newv) -> {
                     if (newv == null) {
                         return;  // 不允许空选
                     }
                     Toggle t = newv;
                     DiffMode mode = (DiffMode) t.getUserData();
                     switch (mode) {
                         case GIT_DIFF -> {
                             centerContainer.setCenter(diffArea);
                             diffArea.setVisible(true);
                             diffArea.setManaged(true);
                             sideBySidePane.setVisible(false);
                             sideBySidePane.setManaged(false);
                             externalPane.setVisible(false);
                             externalPane.setManaged(false);
                         }
                         case SIDE_BY_SIDE -> {
                             centerContainer.setCenter(sideBySidePane);
                             diffArea.setVisible(false);
                             diffArea.setManaged(false);
                             sideBySidePane.setVisible(true);
                             sideBySidePane.setManaged(true);
                             externalPane.setVisible(false);
                             externalPane.setManaged(false);
                         }
                         case EXTERNAL_TOOL -> {
                             centerContainer.setCenter(externalPane);
                             diffArea.setVisible(false);
                             diffArea.setManaged(false);
                             sideBySidePane.setVisible(false);
                             sideBySidePane.setManaged(false);
                             externalPane.setVisible(true);
                             externalPane.setManaged(true);
                         }
                         default -> log.warn("未知的 DiffMode: {}", mode);
                     }
                 });

        // launchExternalBtn 启动外部工具
        launchExternalBtn.setOnAction(e -> launchExternalTool(repoPath, filePath, fileState, diffProvider, externalStatusLabel));

        // 异步加载 diff（P2-037: 裸 Thread → AsyncUiLoader 后台读取）
        AsyncUiLoader.submitRead(repoPath, TaskType.STATUS, () -> {
            String text;
            String header;
            try {
                if (fileState == com.gitgui.domain.model.FileStatus.FileState.UNTRACKED) {
                    text = diffProvider.readWorkingFile(repoPath, filePath);
                    header = I18nUtil.get("commit.diffNewFile");
                } else if (fileState == com.gitgui.domain.model.FileStatus.FileState.DELETED) {
                    text = diffProvider.readHeadFile(repoPath, filePath);
                    header = "Deleted file";
                } else {
                    text = diffProvider.getDiff(repoPath, filePath, oldRev, newRev);
                    header = text.isEmpty() ? I18nUtil.get("commit.diffNoChange") : "";
                }
            } catch (Exception ex) {
                text = I18nUtil.get("commit.diffFailed") + ex.getMessage();
                header = "";
            }
            final String finalText = text;
            final String finalHeader = header;
            Platform.runLater(() -> {
                // ① Git Diff pane
                StringBuilder sb = new StringBuilder();
                if (finalHeader != null && !finalHeader.isEmpty()) {
                    sb.append("=== ")
                      .append(finalHeader)
                      .append(" ===\n\n");
                }
                if (finalText == null || finalText.isEmpty()) {
                    sb.append("(")
                      .append(I18nUtil.get("commit.diffNoChange"))
                      .append(")");
                } else {
                    sb.append(finalText);
                }
                diffArea.setText(sb.toString());
                diffArea.positionCaret(0);

                // ② Side by Side pane：解析 unified diff
                SideBySideContent sides = parseUnifiedDiff(finalText);
                oldArea.setText(sides.oldText);
                newArea.setText(sides.newText);

                // ③ External Tool pane 状态：从 .gitconfig 读取 diff.tool + difftool.<name>.path
                // （git-gui 不再自行持有配置入口；配置由用户在 ~/.gitconfig / <repo>/.git/config 设置）
                ExternalToolInfo info = readExternalToolInfo(repoPath);
                externalStatusLabel.setText(info.statusText);
                launchExternalBtn.setDisable(!info.configured);
            });
        });
    }


    /**
     * 读取 git config（按 git 解析顺序：本仓库 → 用户全局 → 系统级）。
     * <p>gitconfig 是外部工具配置的事实标准，git-gui 不再单独维护配置入口。</p>
     *
     * @param repoPath 仓库路径
     * @param key      配置键（如 diff.tool）
     * @return 配置值，未设置则返回 null
     */
    private static String readGitConfig(String repoPath, String key) {
        try {
            CliGitExecutor executor = getCliGitExecutor();
            if (executor == null) {
                return null;
            }
            return executor.getConfig(repoPath, key);
        } catch (Exception e) {
            log.warn("无法读取 git config {}：{}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 取 CliGitExecutor（容错）。
     */
    private static CliGitExecutor getCliGitExecutor() {
        try {
            return com.gitgui.GitGuiApp.getInjector()
                                       .getInstance(CliGitExecutor.class);
        } catch (Exception e) {
            log.warn("无法获取 CliGitExecutor：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 .gitconfig 读取 [diff] / [difftool "..."] 配置。
     * <p>用户在 ~/.gitconfig 提供的写法：</p>
     * <pre>
     * [diff]
     *     tool = bc4
     * [difftool "bc4"]
     *     path = C:\\Program Files\\Beyond Compare 4\\BCompare.exe
     * </pre>
     */
    private static ExternalToolInfo readExternalToolInfo(String repoPath) {
        String toolName = readGitConfig(repoPath, GIT_CONFIG_DIFF_TOOL);
        if (toolName == null || toolName.isBlank()) {
            return new ExternalToolInfo(false, I18nUtil.get("commit.diffExternalToolDiffToolMissing"), null, null);
        }
        String exePath = readGitConfig(repoPath, GIT_CONFIG_DIFFTOOL_NAME + toolName + ".path");
        if (exePath == null || exePath.isBlank()) {
            return new ExternalToolInfo(false, I18nUtil.get("commit.diffExternalToolPathMissing")
                                                       .replace("{tool}", toolName), toolName, null);
        }
        File exeFile = new File(exePath);
        if (!exeFile.isAbsolute()) {
            // 用户写的是相对路径，相对当前仓库目录计算
            exeFile = new File(repoPath, exePath);
        }
        if (!exeFile.exists()) {
            return new ExternalToolInfo(false, I18nUtil.get("commit.diffExternalToolExeNotFound")
                                                       .replace("{0}", exeFile.getAbsolutePath()), toolName, exeFile.getAbsolutePath());
        }
        return new ExternalToolInfo(true, I18nUtil.get("commit.diffExternalToolConfigured")
                                                  .replace("{tool}", toolName)
                                                  .replace("{path}", exeFile.getAbsolutePath()), toolName, exeFile.getAbsolutePath());
    }

    /**
     * 解析 unified diff 文本为左右两栏内容。
     *
     * @param diffText unified diff 文本（含 hunk 头 + - / + / 空格 前缀）
     * @return SideBySideContent 双栏文本
     */
    private static SideBySideContent parseUnifiedDiff(String diffText) {
        StringBuilder left = new StringBuilder();
        StringBuilder right = new StringBuilder();
        if (diffText == null || diffText.isEmpty()) {
            return new SideBySideContent(left.toString(), right.toString());
        }
        // hunk 头: @@ -X,Y +A,B @@
        Pattern hunkHeader = Pattern.compile("^@@ -(\\d+)(?:,\\d+)? \\+(\\d+)(?:,\\d+)? @@.*$");
        for (String rawLine : diffText.split("\\R", -1)) {
            if (rawLine.isEmpty()) {
                continue;
            }
            // 文件元信息行（diff --git / index / --- / +++）跳过
            if (rawLine.startsWith("diff ") || rawLine.startsWith("index ") || rawLine.startsWith("--- ") || rawLine.startsWith("+++ ")) {
                continue;
            }
            if (rawLine.startsWith("@@")) {
                // hunk header：把上下版本范围写到两侧
                Matcher m = hunkHeader.matcher(rawLine);
                if (m.matches()) {
                    left.append("[... hunk ")
                        .append(m.group(1))
                        .append(" ...]\n");
                    right.append("[... hunk ")
                         .append(m.group(2))
                         .append(" ...]\n");
                }
                continue;
            }
            char prefix = rawLine.charAt(0);
            String body = rawLine.substring(1);  // 去掉前缀 (-/+/space)，保留原缩进
            switch (prefix) {
                case '-' -> {
                    left.append(body)
                        .append('\n');
                    // 右侧对应位置留空（占位，同步对齐）
                    right.append('\n');
                }
                case '+' -> {
                    left.append('\n');
                    right.append(body)
                         .append('\n');
                }
                case ' ' -> {
                    left.append(body)
                        .append('\n');
                    right.append(body)
                         .append('\n');
                }
                default -> {
                    // 元数据行（"@@..." 之外），忽略
                }
            }
        }
        return new SideBySideContent(left.toString(), right.toString());
    }

    private static void cleanupTempFiles() {
        if (currentOldTempFile != null && currentOldTempFile.exists()) {
            if (!currentOldTempFile.delete()) {
                log.warn("临时文件清理失败：{}", currentOldTempFile.getAbsolutePath());
            }
            currentOldTempFile = null;
        }
    }

    /**
     * 启动外部对比工具（参考 git difftool 命令的标准用法）：
     * <ul>
     *   <li>MODIFIED / DELETED：{@code git difftool --no-prompt -- <file>}（git 自动准备 temp + 调 exe）</li>
     *   <li>STAGED：{@code git difftool --no-prompt --cached -- <file>}（对比 index vs HEAD）</li>
     *   <li>UNTRACKED：git difftool 不识别（新文件不在 git 索引里），降级为 git-gui 准备 temp + 直接调 exe</li>
     * </ul>
     * <p>这样跟用户在 bash 用 {@code git difftool <file>} 完全等价。</p>
     */
    private static void launchExternalTool(String repoPath, String filePath, com.gitgui.domain.model.FileStatus.FileState fileState,
            DiffProvider diffProvider, Label statusLabel) {
        cleanupTempFiles();
        // 重新读取 .gitconfig（用户可能在对话框打开后修改了配置）
        ExternalToolInfo info = readExternalToolInfo(repoPath);
        if (!info.configured || info.exePath == null) {
            statusLabel.setText(info.statusText);
            return;
        }
        File exeFile = new File(info.exePath);
        if (!exeFile.exists()) {
            statusLabel.setText(I18nUtil.get("commit.diffExternalToolExeNotFound")
                                        .replace("{0}", exeFile.getAbsolutePath()));
            return;
        }

        ProcessBuilder pb;
        File workDir = new File(repoPath);

        try {
            if (fileState == com.gitgui.domain.model.FileStatus.FileState.UNTRACKED) {
                // UNTRACKED：git difftool 不识别这个新文件 —— 降级为 git-gui 自己准备 temp
                File oldTemp = createEmptyTemp();
                File newFile = new File(repoPath, filePath);
                pb = new ProcessBuilder(exeFile.getAbsolutePath(), oldTemp.getAbsolutePath(), newFile.getAbsolutePath());
            } else {
                // MODIFIED / STAGED / DELETED / OTHER ── 用 git difftool（用户资料给的实现）
                pb = new ProcessBuilder("git", "difftool", "--no-prompt", "--", filePath);
                if (fileState == com.gitgui.domain.model.FileStatus.FileState.STAGED) {
                    // 对比 staging vs HEAD
                    pb = new ProcessBuilder("git", "difftool", "--no-prompt", "--cached", "--", filePath);
                }
            }
            pb.directory(workDir);
            Process p = pb.start();
            statusLabel.setText(I18nUtil.get("commit.diffExternalToolLaunched")
                                        .replace("{0}", String.valueOf(p.pid())));
        } catch (IOException ioEx) {
            log.error("启动外部对比工具失败", ioEx);
            statusLabel.setText(I18nUtil.get("commit.diffExternalToolLaunchFailed") + ioEx.getMessage());
        }
    }

    /**
     * 写内容到临时文件，返回 File。
     */
    private static File writeToTemp(String content, String originalName) throws IOException {
        File tmp = Files.createTempFile(OLD_TEMP_PREFIX + sanitize(originalName), ".tmp")
                        .toFile();
        try (FileWriter w = new FileWriter(tmp, StandardCharsets.UTF_8)) {
            w.write(content);
        }
        // 仅保留最近一个临时文件（窗口未关闭期间）
        File prev = currentOldTempFile;
        currentOldTempFile = tmp;
        if (prev != null && prev.exists()) {
            // noinspection ResultOfMethodCallIgnored
            prev.delete();
        }
        // 注册 JVM 关闭时的最终清理
        tmp.deleteOnExit();
        return tmp;
    }

    private static File createEmptyTemp() throws IOException {
        return writeToTemp("", "empty");
    }

    private static String sanitize(String name) {
        if (name == null || name.isEmpty()) {
            return "x";
        }
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    /**
     * 格式化文件状态为可读字符串。
     */
    private static String formatState(com.gitgui.domain.model.FileStatus.FileState s) {
        if (s == null) {
            return "";
        }
        return switch (s) {
            case MODIFIED -> "Modified";
            case UNTRACKED -> "Untracked";
            case DELETED -> "Deleted";
            case STAGED -> "Staged";
            case CONFLICT -> "Conflict";
            case IGNORED -> "Ignored";
            case UNMODIFIED -> "Unmodified";
        };
    }

    private static String stateStyle(com.gitgui.domain.model.FileStatus.FileState s) {
        if (s == null) {
            return "";
        }
        return switch (s) {
            case MODIFIED -> "-fx-text-fill: #1976d2; -fx-font-style: italic;";
            case UNTRACKED -> "-fx-text-fill: #388e3c; -fx-font-style: italic;";
            case DELETED -> "-fx-text-fill: #d32f2f; -fx-font-style: italic;";
            case STAGED -> "-fx-text-fill: #f57c00; -fx-font-style: italic;";
            case CONFLICT -> "-fx-text-fill: #d32f2f; -fx-font-weight: bold;";
            default -> "-fx-font-style: italic;";
        };
    }

    /**
     * 3 种查看模式枚举
     */
    private enum DiffMode {
        GIT_DIFF,
        SIDE_BY_SIDE,
        EXTERNAL_TOOL
    }

    /**
     * Diff 文本提供者（由 CommitDialog 注入具体的实现）。
     */
    public interface DiffProvider {

        String getDiff(String repoPath, String path, String oldRev, String newRev);

        String readWorkingFile(String repoPath, String path);

        String readHeadFile(String repoPath, String path);
    }

    /**
     * 外部工具配置读出结果（用于主线程显示状态）
     */
    private record ExternalToolInfo(boolean configured, String statusText, String toolName, String exePath) {

    }

    /**
     * Side by Side 双栏内容包装
     */
    private record SideBySideContent(String oldText, String newText) {

    }
}
