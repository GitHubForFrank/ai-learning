package com.gitgui.ui.dialog;

import com.gitgui.ui.i18n.I18nUtil;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * 文件 Diff 查看器（TortoiseGit 风格）
 * <p>对应 PRD 4.3 "双击文件查看 diff" 场景：</p>
 * <ul>
 *   <li>由 CommitDialog 双击文件行触发；</li>
 *   <li>展示 unified diff 文本（只读 TextArea）；</li>
 *   <li>对于 Untracked 文件（HEAD 中无此文件）展示全文件内容并标记为新增；</li>
 *   <li>对于纯删除文件（工作区已无此文件）展示全文件内容并标记为删除；</li>
 * </ul>
 *
 * <p>参照 TortoiseGit "Unified Diff Viewer" 设计：顶部文件名 + 状态，中间 diff 文本，底部 Close 按钮。</p>
 *
 * @author FrankKang
 * @since 2026-07-25
 */
public class DiffViewerDialog {

    private DiffViewerDialog() {
        // 工具类，不实例化
    }

    /**
     * 弹出 Diff 查看窗口（异步：在工作线程算 diff，主线程显示）。
     *
     * @param owner          父窗口（用于置顶），可为 null
     * @param repoPath       仓库路径
     * @param filePath       文件相对路径
     * @param fileState      文件状态（决定如何处理：untracked / modified / deleted / staged）
     * @param oldRev         旧版本（null 表示与 HEAD 对比；untracked 时不使用）
     * @param newRev         新版本（null 表示工作区；deleted 时不使用）
     * @param diffProvider   diff 文本提供者（调用方负责读 git/文件系统）
     */
    public static void show(Stage owner, String repoPath, String filePath,
                            com.gitgui.domain.model.FileStatus.FileState fileState,
                            String oldRev, String newRev, DiffProvider diffProvider) {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.UTILITY);
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(Modality.NONE);
        stage.setTitle(filePath + " - " + I18nUtil.get("commit.diffTitle"));

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // ===== 顶部：文件名 + 状态标签 =====
        Label fileLabel = new Label(I18nUtil.get("commit.diffFileLabel") + " " + filePath);
        fileLabel.setStyle("-fx-font-weight: bold;");
        Label stateLabel = new Label(formatState(fileState));
        stateLabel.setStyle(stateStyle(fileState));
        HBox top = new HBox(10, fileLabel, stateLabel);
        top.setAlignment(Pos.CENTER_LEFT);
        root.setTop(top);

        // ===== 中间：diff 文本 =====
        TextArea diffArea = new TextArea();
        diffArea.setEditable(false);
        diffArea.setWrapText(false);
        diffArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 12px;");
        diffArea.setText(I18nUtil.get("common.loading"));
        root.setCenter(diffArea);
        BorderPane.setMargin(diffArea, new Insets(8, 0, 8, 0));

        // ===== 底部：Close 按钮 =====
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button closeBtn = new Button(I18nUtil.get("button.close"));
        closeBtn.setDefaultButton(true);
        closeBtn.setOnAction(e -> stage.close());
        HBox bottom = new HBox(spacer, closeBtn);
        bottom.setAlignment(Pos.CENTER_RIGHT);
        root.setBottom(bottom);

        Scene scene = new Scene(root, 900, 600);
        stage.setScene(scene);
        stage.show();

        // 异步加载 diff
        Thread t = new Thread(() -> {
            String text;
            String header;
            try {
                if (fileState == com.gitgui.domain.model.FileStatus.FileState.UNTRACKED) {
                    // 未跟踪：直接读工作区文件全文
                    text = diffProvider.readWorkingFile(repoPath, filePath);
                    header = I18nUtil.get("commit.diffNewFile");
                } else if (fileState == com.gitgui.domain.model.FileStatus.FileState.DELETED) {
                    // 删除：读 HEAD 中该文件的内容
                    text = diffProvider.readHeadFile(repoPath, filePath);
                    header = "Deleted file";
                } else {
                    // Modified / Staged：走 git diff
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
                StringBuilder sb = new StringBuilder();
                if (finalHeader != null && !finalHeader.isEmpty()) {
                    sb.append("=== ").append(finalHeader).append(" ===\n\n");
                }
                if (finalText == null || finalText.isEmpty()) {
                    sb.append("(").append(I18nUtil.get("commit.diffNoChange")).append(")");
                } else {
                    sb.append(finalText);
                }
                diffArea.setText(sb.toString());
                diffArea.positionCaret(0);
            });
        }, "DiffViewerLoader");
        t.setDaemon(true);
        t.start();
    }

    private static String formatState(com.gitgui.domain.model.FileStatus.FileState s) {
        if (s == null) return "";
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
        if (s == null) return "";
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
     * Diff 文本提供者（由 CommitDialog 注入具体的实现）。
     * <p>三种来源：</p>
     * <ul>
     *   <li>{@link #getDiff} - 通过 StatusService.getDiff 拿 unified diff</li>
     *   <li>{@link #readWorkingFile} - 读工作区文件全文（用于 untracked）</li>
     *   <li>{@link #readHeadFile} - 读 HEAD 中该文件的内容（用于 deleted）</li>
     * </ul>
     */
    public interface DiffProvider {
        String getDiff(String repoPath, String path, String oldRev, String newRev);

        String readWorkingFile(String repoPath, String path);

        String readHeadFile(String repoPath, String path);
    }
}
