package com.gitgui.ui;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.gitgui.domain.service.SettingsService;
import com.gitgui.ui.i18n.I18nUtil;
import com.gitgui.ui.main.MainController;
import com.gitgui.ui.theme.ThemeManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * 主窗口启动器
 * <p>负责加载 {@code MainView.fxml}、注入 Controller、绑定主题、显示 Stage。</p>
 * <p>遵循 BR-37：启动时加载默认设置（主题、语言）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
@Singleton
public class MainAppLauncher {

    private static final Logger log = LoggerFactory.getLogger(MainAppLauncher.class);

    /** 主题管理器 */
    private final ThemeManager themeManager;
    /** 设置服务 */
    private final SettingsService settingsService;
    /** 主窗口 Controller */
    private final MainController mainController;

    /**
     * 构造启动器（Guice 注入）。
     *
     * @param themeManager 主题管理器
     * @param settingsService 设置服务
     * @param mainController 主窗口 Controller
     */
    @Inject
    public MainAppLauncher(ThemeManager themeManager,
                           SettingsService settingsService,
                           MainController mainController) {
        this.themeManager = themeManager;
        this.settingsService = settingsService;
        this.mainController = mainController;
    }

    /**
     * 启动主窗口。
     *
     * @param primaryStage JavaFX 主舞台
     */
    public void launch(Stage primaryStage) {
        try {
            // 加载默认设置：语言与主题
            String language = settingsService.get("ui.language");
            if (language != null && !language.isEmpty()) {
                I18nUtil.switchLanguage(language);
            }

            // 加载 FXML
            FXMLLoader loader = new FXMLLoader();
            loader.setControllerFactory(c -> mainController);
            loader.setResources(I18nUtil.getBundle());
            try (InputStream fxmlStream = MainAppLauncher.class.getResourceAsStream("/fxml/MainView.fxml")) {
                if (fxmlStream == null) {
                    throw new IllegalStateException("找不到 /fxml/MainView.fxml");
                }
                Parent root = loader.load(fxmlStream);
                Scene scene = new Scene(root, 1280, 800);

                // 应用主题
                String theme = settingsService.get("ui.theme");
                if (theme == null || theme.isEmpty()) {
                    theme = "DARK";
                }
                themeManager.bindScene(scene);
                themeManager.applyTheme(theme);

                primaryStage.setTitle(I18nUtil.get("app.title"));
                primaryStage.setScene(scene);
                primaryStage.setMinWidth(960);
                primaryStage.setMinHeight(640);
                primaryStage.show();
                log.info("主窗口已显示");
            }
        } catch (Exception e) {
            log.error("加载主窗口失败", e);
            throw new RuntimeException("加载主窗口失败", e);
        }
    }
}
