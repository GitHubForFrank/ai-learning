package com.gitgui;

import com.gitgui.core.async.TaskManager;
import com.gitgui.core.config.AppConfig;
import com.gitgui.di.AppModule;
import com.gitgui.di.AsyncModule;
import com.gitgui.di.DatabaseModule;
import com.gitgui.di.GitModule;
import com.gitgui.di.RedLineModule;
import com.gitgui.di.ServiceModule;
import com.gitgui.infrastructure.cli.GitExecutableDetector;
import com.gitgui.infrastructure.cli.GitProcessBuilder;
import com.gitgui.ui.MainAppLauncher;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javafx.application.Application;
import javafx.stage.Stage;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JavaFX 应用入口
 * <p>启动顺序（遵循 02-架构设计.md）：</p>
 * <ol>
 *   <li>锁文件检测（BR-40 单实例）</li>
 *   <li>Guice Injector 创建</li>
 *   <li>Flyway 迁移（在 SqliteDataSource 构造时执行）</li>
 *   <li>Git 可执行文件检测（BR-41）</li>
 *   <li>加载默认设置</li>
 *   <li>显示主窗口</li>
 * </ol>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class GitGuiApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(GitGuiApp.class);
    @Getter
    private static Injector injector;

    /**
     * 单实例锁文件句柄
     */
    private static FileChannel lockChannel;

    /**
     * main 方法入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        log.info("git-gui 启动中...");
        try {
            // 1. 锁文件检测（BR-40 单实例）
            acquireSingleInstanceLock();

            // 2. 创建 Guice Injector（Flyway 迁移在 SqliteDataSource 构造时触发）
            injector = Guice.createInjector(new AppModule(), new DatabaseModule(), new GitModule(), new ServiceModule(), new RedLineModule(),
                                            new AsyncModule());

            // 3. Git 可执行文件检测（BR-41）
            String gitPath = GitExecutableDetector.detect(null);
            if (gitPath != null) {
                GitProcessBuilder.setGitExecutable(gitPath);
                log.info("本地 Git 检测成功：{}", gitPath);
            } else {
                log.warn("未检测到本地 Git，Git 操作将不可用（LFS/Hook 等场景受限）");
            }

            // 4. 加载默认设置（V1 迁移已初始化内置默认值）
            // 5. 显示主窗口
            MainAppLauncher launcher = injector.getInstance(MainAppLauncher.class);
            launcher.launch(primaryStage);
        } catch (Exception e) {
            log.error("git-gui 启动失败", e);
            System.exit(1);
        }
    }

    @Override
    public void stop() {
        // 主窗口关闭时关闭 Guice 注入器并释放资源、释放锁文件
        log.info("git-gui 关闭中...");
        if (injector != null) {
            try {
                TaskManager taskManager = injector.getInstance(TaskManager.class);
                taskManager.shutdown();
            } catch (Exception e) {
                log.warn("关闭 TaskManager 失败", e);
            }
        }
        releaseSingleInstanceLock();
        log.info("git-gui 已退出");
    }

    /**
     * 获取单实例锁（BR-40）。
     * <p>二次启动时抛异常退出。</p>
     */
    private void acquireSingleInstanceLock() {
        try {
            Path lockPath = Paths.get(AppConfig.lockFilePath());
            Files.createDirectories(lockPath.getParent());
            lockChannel = java.nio.channels.FileChannel.open(lockPath, java.nio.file.StandardOpenOption.CREATE,
                                                             java.nio.file.StandardOpenOption.WRITE);
            // 尝试获取独占锁
            java.nio.channels.FileLock lock = lockChannel.tryLock();
            if (lock == null) {
                log.error("应用已在运行（BR-40 单实例），请勿重复启动");
                System.exit(2);
            }
        } catch (IOException e) {
            log.error("获取单实例锁失败", e);
            System.exit(3);
        }
    }

    /**
     * 释放单实例锁（BR-40）。
     */
    private void releaseSingleInstanceLock() {
        try {
            if (lockChannel != null) {
                lockChannel.close();
                Files.deleteIfExists(Paths.get(AppConfig.lockFilePath()));
            }
        } catch (IOException e) {
            log.warn("释放单实例锁失败", e);
        }
    }
}
