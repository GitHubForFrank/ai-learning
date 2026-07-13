package com.example.mcp.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 异步日志工具类，将日志写入项目/logs目录下的 mcp-office-toolbox.log 文件。
 * 每天自动备份为 mcp-office-toolbox_yyyy_MM_dd.log。
 * 支持 info、error、warn、debug 等静态方法，所有日志异步写入，不阻塞主进程。
 *
 * @author Frank Kang
 * @since 2026-07-10
 */
public final class LogUtil {

    private static final String LOG_FILE_NAME = "mcp-office-toolbox.log";
    private static final String LOG_DIR = "logs";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy_MM_dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final int QUEUE_CAPACITY = 10000;

    private static final BlockingQueue<LogEntry> QUEUE = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private static final Object WRITER_LOCK = new Object();
    private static volatile boolean initialized;
    private static Path logDir;
    private static LocalDate currentDate;
    private static BufferedWriter writer;

    static {
        init();
    }

    private LogUtil() {
        // 工具类禁止实例化
    }

    // ==================== 初始化 ====================

    private static synchronized void init() {
        if (initialized) {
            return;
        }
        logDir = AbstractAsyncLogger.initLogDir(LogUtil.class, "LogUtil");
        currentDate = LocalDate.now();

        Thread writerThread = new Thread(LogUtil::writeLoop, "log-writer");
        writerThread.setDaemon(true);
        writerThread.start();

        initialized = true;
        info("LogUtil 初始化完成，日志目录: " + logDir.toAbsolutePath());
    }

    /**
     * 解析日志根目录，委托给 {@link AbstractAsyncLogger#resolveLogDir(Class)}。
     */
    private static Path resolveLogDir() {
        return AbstractAsyncLogger.resolveLogDir(LogUtil.class);
    }

    // ==================== 异步写循环 ====================

    private static void writeLoop() {
        while (!Thread.currentThread()
                      .isInterrupted()) {
            try {
                LogEntry entry = QUEUE.poll(1, TimeUnit.SECONDS);
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
                System.err.println("[LogUtil] 写入异常: " + e.getMessage());
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
        AbstractAsyncLogger.RotationResult result = AbstractAsyncLogger.checkAndRotate(currentDate, LOG_FILE_NAME, "LogUtil", DATE_FMT, logDir,
                                                                                       WRITER_LOCK, writer);
        currentDate = result.newDate();
        writer = result.newWriter();
    }

    // ==================== 格式化 ====================

    private static String formatEntry(LogEntry entry) {
        return String.format("%s [%s] [%s] %s", LocalDateTime.now()
                                                             .format(TIME_FMT), entry.level(), entry.caller(), entry.message());
    }

    /**
     * 获取调用 LogUtil 的类名，跳过 LogUtil 自身和 JDK 内部帧。
     */
    private static String getCallerClassName() {
        // 利用 StackWalker 高效获取调用栈
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                          .walk(frames -> frames.filter(f -> f.getDeclaringClass() != LogUtil.class)
                                                .findFirst()
                                                .map(f -> f.getDeclaringClass()
                                                           .getSimpleName())
                                                .orElse("Unknown"));
    }

    private static String getStackTraceString(Throwable t) {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            t.printStackTrace(pw);
        }
        return sw.toString();
    }

    // ==================== 公共 API ====================

    // --- info ---

    public static void info(String message) {
        enqueue("INFO", message);
    }

    public static void info(String format, Object... args) {
        enqueue("INFO", formatMessage(format, args));
    }

    // --- error ---

    public static void error(String message) {
        enqueue("ERROR", message);
    }

    public static void error(String format, Object... args) {
        enqueue("ERROR", formatMessage(format, args));
    }

    /**
     * 记录错误消息并附带异常堆栈。
     */
    public static void error(String message, Throwable t) {
        StringBuilder sb = new StringBuilder(message);
        if (t != null) {
            sb.append(System.lineSeparator());
            sb.append(getStackTraceString(t));
        }
        enqueue("ERROR", sb.toString());
    }

    // --- warn ---

    public static void warn(String message) {
        enqueue("WARN", message);
    }

    public static void warn(String format, Object... args) {
        enqueue("WARN", formatMessage(format, args));
    }

    // --- debug ---

    public static void debug(String message) {
        enqueue("DEBUG", message);
    }

    public static void debug(String format, Object... args) {
        enqueue("DEBUG", formatMessage(format, args));
    }

    // ==================== 内部方法 ====================

    private static void enqueue(String level, String message) {
        String caller = getCallerClassName();
        LogEntry entry = new LogEntry(level, caller, message);
        if (!QUEUE.offer(entry)) {
            // 队列满时丢弃最旧的
            QUEUE.poll();
            QUEUE.offer(entry);
        }
    }

    /**
     * 格式化消息，支持 SLF4J 风格的 {} 占位符。
     * 如果最后一个参数是 Throwable，则自动追加异常堆栈（兼容 SLF4J 行为）。
     */
    private static String formatMessage(String format, Object... args) {
        if (args == null || args.length == 0) {
            return format;
        }
        // 检测最后一个参数是否为 Throwable（兼容 SLF4J 的 error("msg {}", arg, e) 模式）
        Object lastArg = args[args.length - 1];
        if (lastArg instanceof Throwable) {
            Object[] formatArgs = new Object[args.length - 1];
            System.arraycopy(args, 0, formatArgs, 0, args.length - 1);
            String formatted = formatMessage(format, formatArgs);
            return formatted + System.lineSeparator() + getStackTraceString((Throwable) lastArg);
        }
        // 普通 {} 占位符替换
        StringBuilder sb = new StringBuilder(format);
        int argIdx = 0;
        int pos = 0;
        while ((pos = sb.indexOf("{}", pos)) != -1 && argIdx < args.length) {
            sb.replace(pos, pos + 2, String.valueOf(args[argIdx++]));
            pos += String.valueOf(args[argIdx - 1])
                         .length();
        }
        return sb.toString();
    }

    // ==================== 日志条目 ====================

    private record LogEntry(String level, String caller, String message) {

    }
}
