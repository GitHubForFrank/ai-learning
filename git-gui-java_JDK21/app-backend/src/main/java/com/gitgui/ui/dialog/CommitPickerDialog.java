package com.gitgui.ui.dialog;

import com.gitgui.domain.model.LogEntry;
import com.gitgui.ui.i18n.I18nUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

/**
 * Commit 选择对话框（TortoiseGit 风格）。
 * <p>用于 CommitDialog 右键菜单的以下场景：</p>
 * <ul>
 *   <li>Pick commit hash：从最近 commit 中选一个，把 short id 插入 message</li>
 *   <li>Pick commit message：从最近 commit 中选一个，把 message 插入 message</li>
 *   <li>Paste recent message...：从最近 commit message 列表中选一个插入</li>
 * </ul>
 *
 * <p>支持双击直接确认 + OK 按钮确认两种交互方式。</p>
 *
 * @author FrankKang
 * @since 2026-07-25
 */
public class CommitPickerDialog extends Dialog<LogEntry> {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ListView<LogEntry> commitList = new ListView<>();
    /** 每行的展示文案生成器（默认 "shortId author date firstLine"） */
    private final Function<LogEntry, String> labelProvider;

    /**
     * @param commits      候选 commit 列表
     * @param labelProvider 自定义每行展示文案
     */
    public CommitPickerDialog(List<LogEntry> commits, Function<LogEntry, String> labelProvider) {
        this.labelProvider = labelProvider == null ? this::defaultLabel : labelProvider;
        setTitle(I18nUtil.get("commit.contextMenu.pickCommitTitle"));
        setHeaderText(I18nUtil.get("commit.contextMenu.pickCommitPrompt"));

        // 应用模态
        initModality(Modality.APPLICATION_MODAL);

        DialogPane pane = getDialogPane();
        pane.setContent(buildContent(commits));
        pane.setPrefSize(640, 380);

        ButtonType okType = new ButtonType(I18nUtil.get("commit.contextMenu.pickCommitOk"),
                ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType(I18nUtil.get("commit.contextMenu.pickCommitCancel"),
                ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(okType, cancelType);

        // OK 按钮：返回当前选中项
        Button okButton = (Button) pane.lookupButton(okType);
        if (okButton != null) {
            okButton.setDefaultButton(true);
            okButton.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
                LogEntry selected = commitList.getSelectionModel().getSelectedItem();
                if (selected == null) {
                    e.consume(); // 没选中时不允许关闭
                } else {
                    setResult(selected);
                }
            });
        }
        // 取消按钮
        Button cancelButton = (Button) pane.lookupButton(cancelType);
        if (cancelButton != null) {
            cancelButton.setCancelButton(true);
        }

        setResultConverter(buttonType -> {
            if (buttonType == okType) {
                return commitList.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        // 双击直接确认
        commitList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && !commitList.getSelectionModel().isEmpty()) {
                LogEntry selected = commitList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    setResult(selected);
                    // 主动关闭（result 设置后，Dialog 不会自动关闭，需要 hide）
                    Platform.runLater(this::hide);
                }
            }
        });
    }

    private VBox buildContent(List<LogEntry> commits) {
        Label promptLabel = new Label(I18nUtil.get("commit.contextMenu.pickCommitPrompt"));
        promptLabel.setStyle("-fx-text-fill: #757575; -fx-font-size: 11px;");

        ObservableList<LogEntry> items = FXCollections.observableArrayList(
                commits == null ? List.of() : commits);
        commitList.setItems(items);
        commitList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(LogEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(labelProvider.apply(item));
                }
            }
        });
        // 默认选中第一条
        if (!items.isEmpty()) {
            commitList.getSelectionModel().select(0);
        }
        commitList.setPlaceholder(new Label(I18nUtil.get("commit.contextMenu.noRecentCommit")));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox promptRow = new HBox(spacer, new Label(String.valueOf(items.size()) + " commits"));
        promptRow.setStyle("-fx-text-fill: #757575; -fx-font-size: 11px;");

        VBox box = new VBox(6, promptLabel, commitList, promptRow);
        VBox.setVgrow(commitList, Priority.ALWAYS);
        box.setPadding(new Insets(0, 0, 8, 0));
        return box;
    }

    private String defaultLabel(LogEntry e) {
        if (e == null) return "";
        String shortId = e.getShortId() == null ? "" : e.getShortId();
        String author = e.getAuthor() == null ? "" : e.getAuthor();
        String date = e.getCommitTime() == null ? "" : e.getCommitTime().format(DATE_FMT);
        String msg = e.getMessage() == null ? "" : e.getMessage();
        int nl = msg.indexOf('\n');
        String firstLine = nl >= 0 ? msg.substring(0, nl) : msg;
        return String.format("%-8s %-15s %-16s %s", shortId, truncate(author, 15), date, firstLine);
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
