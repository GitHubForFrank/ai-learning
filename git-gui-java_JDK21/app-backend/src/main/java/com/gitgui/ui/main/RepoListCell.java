package com.gitgui.ui.main;

import com.gitgui.ui.i18n.I18nUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * 侧边栏仓库列表项渲染 Cell
 * <p>渲染 {@link RepoListItem}，包含：</p>
 * <ul>
 *   <li>左侧：仓库名 + 副标题（分支/路径）</li>
 *   <li>右侧：⭐ 收藏切换按钮</li>
 *   <li>未提交修改指示：副标题前缀「●」</li>
 * </ul>
 *
 * @author FrankKang
 * @since 2026-07-24
 */
public class RepoListCell extends ListCell<RepoListItem> {

    /** 收藏切换回调（点击星标时触发，参数为当前项） */
    private final Consumer<RepoListItem> onToggleFavorite;

    private final HBox root;
    private final Label nameLabel;
    private final Label subtitleLabel;
    private final Button favoriteButton;

    public RepoListCell(Consumer<RepoListItem> onToggleFavorite) {
        this.onToggleFavorite = onToggleFavorite;

        nameLabel = new Label();
        nameLabel.getStyleClass().add("repo-cell-name");

        subtitleLabel = new Label();
        subtitleLabel.getStyleClass().add("repo-cell-subtitle");

        VBox textBox = new VBox(2, nameLabel, subtitleLabel);
        textBox.getStyleClass().add("repo-cell-text");
        HBox.setHgrow(textBox, Priority.ALWAYS);

        favoriteButton = new Button();
        favoriteButton.getStyleClass().add("repo-cell-favorite-button");
        favoriteButton.setMinWidth(28);
        favoriteButton.setPrefWidth(28);
        favoriteButton.setMaxWidth(28);
        favoriteButton.setFocusTraversable(false);
        // 直接在 button 上监听 MOUSE_CLICKED 并消费事件，避免依赖 Button.onAction 的
        // MOUSE_RELEASED -> fire() 链路。某些 cell 复用/重绘场景下，Button.onAction 不会触发，
        // 导致点击星标后既不切换收藏状态、也未阻止事件冒泡到 ListView 触发 openRepository。
        favoriteButton.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            // 关键：消费事件，阻止冒泡到父 ListView 触发 openRepository。
            e.consume();
            RepoListItem item = getItem();
            if (item != null && onToggleFavorite != null) {
                onToggleFavorite.accept(item);
            }
        });

        root = new HBox(6, textBox, favoriteButton);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(4, 6, 4, 6));
        root.getStyleClass().add("repo-cell");
    }

    @Override
    protected void updateItem(RepoListItem item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            return;
        }
        // 仓库名
        nameLabel.setText(item.getDisplayName());
        // 副标题：未提交修改加「●」前缀
        String subtitle = item.getSubtitle();
        if (item.getRepoMeta().isHasUncommittedChanges()) {
            subtitle = "● " + subtitle;
        }
        subtitleLabel.setText(subtitle);

        // 收藏按钮：根据当前状态显示 ★ / ☆
        if (item.isFavorite()) {
            favoriteButton.setText("★");
            favoriteButton.setTooltip(new Tooltip(I18nUtil.get("sidebar.repo.unfavorite")));
        } else {
            favoriteButton.setText("☆");
            favoriteButton.setTooltip(new Tooltip(I18nUtil.get("sidebar.repo.favorite")));
        }
        // Tooltip 显示完整路径便于识别
        setTooltip(new Tooltip(item.getRepoMeta().getRepoPath()));

        setGraphic(root);
    }

    /**
     * 占位 Cell（空数据时由 ListView 调用以填充空间）。
     */
    public static class PlaceholderCell extends ListCell<String> {
        private final Label label = new Label();

        public PlaceholderCell(String message) {
            label.setText(message);
            label.setWrapText(true);
            label.getStyleClass().add("repo-cell-placeholder");
            label.setMaxWidth(Double.MAX_VALUE);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                label.setText(item);
                setGraphic(label);
            }
        }
    }
}