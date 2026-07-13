package com.example.mcp.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 异步日志公共基类，提供日志目录解析、文件轮转、Writer 管理等公共基础设施。
 * 子类（LogUtil、AuditLogger）通过调用静态方法复用这些公共逻辑。
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
public abstract class AbstractAsyncLogger {

    /**
     * 日志根目录名称
     */
    protected static final String LOG_DIR = "logs";

    /**
     * 日期格式化器（用于日志文件备份命名）
     */
    protected static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy_MM_dd");

    /**
     * 时间格式化器（用于日志行时间戳）
     */
    protected static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * 异步队列容量
     */
    protected static final int QUEUE_CAPACITY = 10000;

    /**
     * 禁止实例化。
     */
    protected AbstractAsyncLogger() {
    }

    // ==================== 日志目录解析 ====================

    /**
     * 解析日志根目录：IDE 开发时定位到项目根目录；jar 包运行时定位到 jar 所在目录。
     *
     * @param callerClass 调用方的 Class 对象，用于获取 protection domain
     * @return 日志根目录路径
     */
    protected static Path resolveLogDir(Class<?> callerClass) {
        try {
            URI uri = callerClass.getProtectionDomain()
                                 .getCodeSource()
                                 .getLocation()
                                 .toURI();
            Path codePath = Paths.get(uri);

            if (codePath.toString()
                        .endsWith(".jar")) {
                // jar 包运行：logs 放在 jar 同级目录
                return codePath.getParent()
                               .resolve(LOG_DIR);
            } else {
                // IDE 开发：从 classes 目录向上查找项目根目录（包含 pom.xml 或 src 目录）
                Path current = codePath;
                while (current != null) {
                    if (Files.exists(current.resolve("pom.xml")) || Files.exists(current.resolve("src"))) {
                        return current.resolve(LOG_DIR);
                    }
                    current = current.getParent();
                }
                // 兜底：user.dir/logs
                return Paths.get(System.getProperty("user.dir"))
                            .resolve(LOG_DIR);
            }
        } catch (Exception e) {
            return Paths.get(System.getProperty("user.dir"))
                        .resolve(LOG_DIR);
        }
    }

    /**
     * 解析并创建日志目录。
     *
     * @param callerClass 调用方的 Class 对象
     * @param loggerName  日志器名称（用于错误输出前缀）
     * @return 日志目录路径
     */
    protected static Path initLogDir(Class<?> callerClass, String loggerName) {
        Path dir = resolveLogDir(callerClass);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            System.err.println("[" + loggerName + "] 无法创建日志目录: " + dir + " - " + e.getMessage());
        }
        return dir;
    }

    // ==================== Writer 管理 ====================

    /**
     * 确保 BufferedWriter 已打开，若未打开则创建新的。
     *
     * @param writer      当前 writer（可能为 null）
     * @param logDir      日志目录
     * @param logFileName 日志文件名
     * @return 已打开的 BufferedWriter
     * @throws IOException 创建 writer 失败时抛出
     */
    protected static synchronized BufferedWriter ensureWriter(BufferedWriter writer, Path logDir, String logFileName) throws IOException {
        if (writer == null) {
            return Files.newBufferedWriter(logDir.resolve(logFileName), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        return writer;
    }

    /**
     * 关闭 BufferedWriter。
     *
     * @param writer     当前 writer
     * @param writerLock 写入锁对象
     * @return 始终返回 {@code null}，便于调用方置空 writer 引用
     */
    protected static BufferedWriter closeWriter(BufferedWriter writer, Object writerLock) {
        synchronized (writerLock) {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                }
                return null;
            }
            return null;
        }
    }

    // ==================== 日志轮转 ====================

    /**
     * 检查并执行日志文件轮转。如果日期变更，则关闭当前 writer、备份日志文件，并返回更新后的状态。
     *
     * @param currentDate 当前记录的日期
     * @param logFileName 日志文件名
     * @param loggerName  日志器名称（用于错误输出前缀）
     * @param dateFmt     日期格式化器
     * @param logDir      日志目录
     * @param writerLock  写入锁对象
     * @param writer      当前 writer
     * @return 轮转结果，包含更新后的日期和 writer（若发生轮转则 writer 为 null）
     */
    protected static RotationResult checkAndRotate(LocalDate currentDate, String logFileName, String loggerName, DateTimeFormatter dateFmt,
        Path logDir, Object writerLock, BufferedWriter writer) {
        LocalDate today = LocalDate.now();
        if (!today.equals(currentDate)) {
            synchronized (writerLock) {
                if (!today.equals(currentDate)) {
                    String backupName = logFileName.replace(".log", "_" + currentDate.format(dateFmt) + ".log");
                    Path logPath = logDir.resolve(logFileName);
                    Path backupPath = logDir.resolve(backupName);
                    try {
                        closeWriter(writer, writerLock);
                        if (Files.exists(logPath) && Files.size(logPath) > 0) {
                            Files.move(logPath, backupPath);
                        }
                    } catch (IOException e) {
                        System.err.println("[" + loggerName + "] 日志轮转异常: " + e.getMessage());
                    }
                    return new RotationResult(today, null);
                }
            }
        }
        return new RotationResult(currentDate, writer);
    }

    /**
     * 日志轮转结果，包含更新后的日期和 writer。
     */
    protected record RotationResult(LocalDate newDate, BufferedWriter newWriter) {

    }
}