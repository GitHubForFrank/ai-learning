package com.gitgui.ui.dialog;

import com.gitgui.domain.redline.RedLineResult;
import com.gitgui.ui.i18n.I18nUtil;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 红线二次确认对话框
 * <p>当 {@code CommandInterceptor.intercept()} 返回 CONFIRM 时由 UI 弹出。</p>
 * <p>遵循 BR-31：用户确认/取消均记录到 audit_log（CONFIRMED/CANCELLED）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class RedLineConfirmDialog extends Dialog<Boolean> {

    private static final Logger log = LoggerFactory.getLogger(RedLineConfirmDialog.class);

    /**
     * 构造确认对话框。
     *
     * @param result 红线校验结果
     */
    public RedLineConfirmDialog(RedLineResult result) {
        setTitle(I18nUtil.get("redline.confirm.title"));
        setHeaderText(null);
        initModality(Modality.APPLICATION_MODAL);

        DialogPane pane = getDialogPane();
        pane.setContent(buildContent(result));
        pane.getButtonTypes().addAll(
                new ButtonType(I18nUtil.get("redline.confirm.abort"), ButtonBar.ButtonData.CANCEL_CLOSE),
                new ButtonType(I18nUtil.get("redline.confirm.proceed"), ButtonBar.ButtonData.OK_DONE)
        );

        setResultConverter(buttonType -> {
            if (buttonType == null) {
                return false;
            }
            return buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE;
        });
    }

    /**
     * 构建对话框内容。
     *
     * @param result 校验结果
     * @return 内容节点
     */
    private VBox buildContent(RedLineResult result) {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(15));
        vbox.setPrefWidth(520);

        // 风险提示信息
        Label messageLabel = new Label(I18nUtil.get("redline.confirm.message"));
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        // 命中规则代码
        Label ruleCodeLabel = new Label(result.getRuleCode() == null ? "-" : result.getRuleCode().name());
        grid.add(new Label(I18nUtil.get("redline.confirm.hitRule")), 0, 0);
        grid.add(ruleCodeLabel, 1, 0);

        // 风险提示
        TextArea riskArea = new TextArea(result.getMessage());
        riskArea.setWrapText(true);
        riskArea.setPrefRowCount(5);
        riskArea.setEditable(false);
        grid.add(new Label(I18nUtil.get("redline.confirm.risk")), 0, 1);
        grid.add(riskArea, 1, 1);

        // 详情
        if (result.getDetail() != null && !result.getDetail().isEmpty()) {
            TextArea detailArea = new TextArea(result.getDetail());
            detailArea.setWrapText(true);
            detailArea.setPrefRowCount(3);
            detailArea.setEditable(false);
            grid.add(new Label(I18nUtil.get("redline.confirm.detail")), 0, 2);
            grid.add(detailArea, 1, 2);
        }

        vbox.getChildren().addAll(messageLabel, grid);
        return vbox;
    }

    /**
     * 弹窗并等待用户确认。
     *
     * @param result 校验结果
     * @return true 表示用户确认继续，false 表示取消
     */
    public static boolean confirm(RedLineResult result) {
        RedLineConfirmDialog dialog = new RedLineConfirmDialog(result);
        return dialog.showAndWait().orElse(false);
    }
}
