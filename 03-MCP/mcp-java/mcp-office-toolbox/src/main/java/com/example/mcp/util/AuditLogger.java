package com.example.mcp.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 操作审计日志工具类，异步记录每次工具调用的审计信息。
 * 日志写入项目/logs目录下的 audit.log 文件，每天自动轮转备份。
 * 使用 BlockingQueue + daemon 线程实现异步写入，不阻塞主进程。
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
public final class AuditLogger {

    private static final String LOG_FILE_NAME = "audit.log";
    private static final String LOG_DIR = "logs";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy_MM_dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final int QUEUE_CAPACITY = 10000;
    private static final int MAX_PARAM_LENGTH = 200;
    private static final int MAX_RESULT_LENGTH = 200;

    private static final BlockingQueue<AuditEntry> QUEUE = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private static final Object WRITER_LOCK = new Object();
    private static volatile boolean initialized;
    private static Path logDir;
    private static LocalDate currentDate;
    private static BufferedWriter writer;

    static {
        init();
    }

    private AuditLogger() {
        // 工具类禁止实例化
    }

    // ==================== 初始化 ====================

    private static synchronized void init() {
        if (initialized) {
            return;
        }
        logDir = AbstractAsyncLogger.initLogDir(AuditLogger.class, "AuditLogger");
        currentDate = LocalDate.now();

        Thread writerThread = new Thread(AuditLogger::writeLoop, "audit-log-writer");
        writerThread.setDaemon(true);
        writerThread.start();

        initialized = true;
    }

    /**
     * 解析日志根目录，委托给 {@link AbstractAsyncLogger#resolveLogDir(Class)}。
     */
    private static Path resolveLogDir() {
        return AbstractAsyncLogger.resolveLogDir(AuditLogger.class);
    }

    // ==================== 异步写循环 ====================

    private static void writeLoop() {
        while (!Thread.currentThread()
                      .isInterrupted()) {
            try {
                AuditEntry entry = QUEUE.poll(1, TimeUnit.SECONDS);
                if (entry != null) {
                    checkAndRotate();
                    synchronized (WRITER_LOCK) {
                        ensureWriter();
                        writer.write(formatEntry(entry));
                        writer.newLine();
                        writer.flush();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread()
                      .interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[AuditLogger] 写入异常: " + e.getMessage());
            }
        }
        closeWriter();
    }

    private static void ensureWriter() throws IOException {
        writer = AbstractAsyncLogger.ensureWriter(writer, logDir, LOG_FILE_NAME);
    }

    private static void closeWriter() {
        writer = AbstractAsyncLogger.closeWriter(writer, WRITER_LOCK);
    }

    // ==================== 日志轮转 ====================

    private static void checkAndRotate() {
        AbstractAsyncLogger.RotationResult result = AbstractAsyncLogger.checkAndRotate(currentDate, LOG_FILE_NAME, "AuditLogger", DATE_FMT, logDir,
                                                                                       WRITER_LOCK, writer);
        currentDate = result.newDate();
        writer = result.newWriter();
    }

    // ==================== 格式化 ====================

    private static String formatEntry(AuditEntry entry) {
        return String.format("[%s] [%dms] %s %s %s", LocalDateTime.now()
                                                                  .format(TIME_FMT), entry.durationMs(), entry.toolName(), entry.params(),
                             entry.result());
    }

    /**
     * 截断过长的字符串，避免日志行过长。
     */
    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return "null";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    // ==================== 公共 API ====================

    /**
     * 记录一次工具调用审计日志。
     *
     * @param toolName   工具名称
     * @param params     参数摘要
     * @param result     结果摘要
     * @param durationMs 耗时（毫秒）
     */
    public static void log(String toolName, String params, String result, long durationMs) {
        AuditEntry entry = new AuditEntry(truncate(toolName, 50), truncate(params, MAX_PARAM_LENGTH), truncate(result, MAX_RESULT_LENGTH),
                                          durationMs);
        if (!QUEUE.offer(entry)) {
            // 队列满时丢弃最旧的
            QUEUE.poll();
            QUEUE.offer(entry);
        }
    }

    // ==================== 审计条目 ====================

    private record AuditEntry(String toolName, String params, String result, long durationMs) {

    }
}