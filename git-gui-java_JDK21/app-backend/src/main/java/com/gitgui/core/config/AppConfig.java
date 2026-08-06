package com.gitgui.core.config;

/**
 * 应用全局配置常量
 * <p>集中定义应用数据目录、数据库路径、日志路径、锁文件路径等核心路径常量。</p>
 * <p>所有路径默认基于用户主目录 {@code ~/.git-gui}，遵循 BR-40 单实例与 BR-42 编码约束。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public final class AppConfig {

    /**
     * 应用数据根目录：{@code ~/.git-gui}
     */
    public static final String DATA_DIR_NAME = ".git-gui";

    /**
     * 数据库文件相对路径：{@code db/git-gui.db}
     */
    public static final String DB_RELATIVE_PATH = "db/git-gui.db";

    /**
     * 日志目录相对路径：{@code logs}
     */
    public static final String LOG_RELATIVE_PATH = "logs";

    /**
     * 单实例锁文件相对路径：{@code .git-gui.lock}（BR-40）
     */
    public static final String LOCK_FILE_NAME = ".git-gui.lock";

    /**
     * Flyway 迁移脚本 classpath 位置
     */
    public static final String FLYWAY_LOCATION = "db/migration";

    /**
     * 多仓库检索默认深度：3 层（BR-01）
     */
    public static final int DEFAULT_SCAN_DEPTH = 3;

    /**
     * 最近仓库列表最大保留条数：20（BR-05）
     */
    public static final int DEFAULT_RECENT_MAX_KEEP = 20;

    /**
     * 推送超大文件阈值：50MB（BR-29）
     */
    public static final int DEFAULT_LARGE_FILE_THRESHOLD_MB = 50;

    /**
     * 字符串最大长度校验：收藏别名 100 字符（BR-03）
     */
    public static final int ALIAS_MAX_LENGTH = 100;

    /**
     * 分组最大长度：50 字符（BR-03）
     */
    public static final int GROUP_MAX_LENGTH = 50;

    /**
     * 输入数组上限：100
     */
    public static final int INPUT_ARRAY_MAX_SIZE = 100;

    /**
     * 日志单页条数：200（BR-18 分页加载）
     */
    public static final int LOG_DEFAULT_PAGE_SIZE = 200;

    private AppConfig() {
        // 工具类禁止实例化
    }

    /**
     * 获取应用数据根目录绝对路径。
     *
     * @return {@code ~/.git-gui} 绝对路径
     */
    public static String dataDir() {
        return System.getProperty("user.home") + java.io.File.separator + DATA_DIR_NAME;
    }

    /**
     * 获取 SQLite 数据库文件绝对路径。
     *
     * @return 数据库文件绝对路径
     */
    public static String dbPath() {
        return dataDir() + java.io.File.separator + DB_RELATIVE_PATH;
    }

    /**
     * 获取日志目录绝对路径。
     *
     * @return 日志目录绝对路径
     */
    public static String logDir() {
        return dataDir() + java.io.File.separator + LOG_RELATIVE_PATH;
    }

    /**
     * 获取单实例锁文件绝对路径（BR-40）。
     *
     * @return 锁文件绝对路径
     */
    public static String lockFilePath() {
        return dataDir() + java.io.File.separator + LOCK_FILE_NAME;
    }
}
