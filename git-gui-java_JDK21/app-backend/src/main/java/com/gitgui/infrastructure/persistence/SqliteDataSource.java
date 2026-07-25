package com.gitgui.infrastructure.persistence;

import com.gitgui.core.config.AppConfig;
import com.gitgui.core.exception.ErrorCode;
import com.gitgui.core.exception.GitGuiException;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

/**
 * SQLite 数据源与 Flyway 迁移管理，同时实现 {@link javax.sql.DataSource} 供 MyBatis-Plus 使用。
 * <p>负责 SQLite 连接获取与 Flyway 自动迁移（V1~V7）。</p>
 * <p>遵循 BR-40 单实例与 04-database.md SQLite 语法约束。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class SqliteDataSource implements DataSource {

    private static final Logger log = LoggerFactory.getLogger(SqliteDataSource.class);

    /** SQLite JDBC 连接串 */
    private final String jdbcUrl;

    /**
     * 构造数据源，初始化数据库目录并执行 Flyway 迁移。
     */
    public SqliteDataSource() {
        String dbPath = AppConfig.dbPath();
        // 确保数据库目录存在
        try {
            Path dbDir = Paths.get(dbPath).getParent();
            if (dbDir != null) {
                Files.createDirectories(dbDir);
            }
        } catch (Exception e) {
            throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED,
                    "无法创建数据库目录：" + dbPath, e);
        }
        this.jdbcUrl = "jdbc:sqlite:" + dbPath;
        // 启用 WAL 模式提升并发读
        initPragma();
        // 执行 Flyway 迁移
        migrate();
        log.info("SQLite 数据源初始化完成：{}", dbPath);
    }

    /**
     * 获取数据库连接。
     *
     * @return JDBC 连接
     * @throws SQLException 获取失败
     */
    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(jdbcUrl);
        // 启用外键约束
        try (var stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    /**
     * 初始化 SQLite PRAGMA（WAL 模式 + UTF-8 编码）。
     */
    private void initPragma() {
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             var stmt = conn.createStatement()) {
            // 单写多读，提升并发读性能
            stmt.execute("PRAGMA journal_mode = WAL");
            // UTF-8 编码
            stmt.execute("PRAGMA encoding = 'UTF-8'");
        } catch (SQLException e) {
            log.error("初始化 SQLite PRAGMA 失败", e);
        }
    }

    /**
     * 执行 Flyway 迁移（V1~V7）。
     */
    private void migrate() {
        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, "", null)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
        log.info("Flyway 迁移完成");
    }

    // ========== javax.sql.DataSource 实现 ==========

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return getConnection();
    }

    @Override
    public PrintWriter getLogWriter() {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        // no-op
    }

    @Override
    public void setLoginTimeout(int seconds) {
        // no-op
    }

    @Override
    public int getLoginTimeout() {
        return 0;
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("Cannot unwrap to " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface.isInstance(this);
    }
}
