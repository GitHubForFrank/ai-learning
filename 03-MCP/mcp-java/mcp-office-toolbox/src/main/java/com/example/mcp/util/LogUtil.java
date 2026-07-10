package com.example.mcp.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
 * @author FrankKang
 * @since 2026-07-10
 */
public final class LogUtil {

    private static final String LOG_FILE_NAME = "mcp-office-toolbox.log";
    private static final String LOG_DIR = "logs";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy_MM_dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final int QUEUE_CAPACITY = 10000;

    private static final BlockingQueue<LogEntry> QUEUE = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private static volatile boolean initialized;
    private static Path logDir;
    private static LocalDate currentDate;
    private static BufferedWriter writer;
    private static final Object WRITER_LOCK = new Object();

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
        logDir = resolveLogDir();
        try {
            Files.createDirectories(logDir);
        } catch (IOException e) {
            System.err.println("[LogUtil] 无法创建日志目录: " + logDir + " - " + e.getMessage());
        }
        currentDate = LocalDate.now();

        Thread writerThread = new Thread(LogUtil::writeLoop, "log-writer");
        writerThread.setDaemon(true);
        writerThread.start();

        initialized = true;
        info("LogUtil 初始化完成，日志目录: " + logDir.toAbsolutePath());
    }

    /**
     * 解析日志根目录：IDE 开发时定位到项目根目录；jar 包运行时定位到 jar 所在目录。
     */
    private static Path resolveLogDir() {
        try {
            URI uri = LogUtil.class.getProtectionDomain()
                                   .getCodeSource()
                                   .getLocation()
                                   .toURI();
            Path codePath = Paths.get(uri);

            if (codePath.toString().endsWith(".jar")) {
                // jar 包运行：logs 放在 jar 同级目录
                return codePath.getParent().resolve(LOG_DIR);
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
                return Paths.get(System.getProperty("user.dir")).resolve(LOG_DIR);
            }
        } catch (Exception e) {
            return Paths.get(System.getProperty("user.dir")).resolve(LOG_DIR);
        }
    }

    // ==================== 异步写循环 ====================

    private static void writeLoop() {
        while (!Thread.currentThread().isInterrupted()) {
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
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[LogUtil] 写入异常: " + e.getMessage());
            }
        }
        closeWriter();
    }

    private static void ensureWriter() throws IOException {
        if (writer == null) {
            writer = Files.newBufferedWriter(
                    logDir.resolve(LOG_FILE_NAME),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }

    private static void closeWriter() {
        synchronized (WRITER_LOCK) {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                }
                writer = null;
            }
        }
    }

    // ==================== 日志轮转 ====================

    private static void checkAndRotate() {
        LocalDate today = LocalDate.now();
        if (!today.equals(currentDate)) {
            synchronized (WRITER_LOCK) {
                if (!today.equals(currentDate)) {
                    // 备份当前日志文件
                    String backupName = LOG_FILE_NAME.replace(".log", "_" + currentDate.format(DATE_FMT) + ".log");
                    Path logPath = logDir.resolve(LOG_FILE_NAME);
                    Path backupPath = logDir.resolve(backupName);
                    try {
                        closeWriter();
                        if (Files.exists(logPath) && Files.size(logPath) > 0) {
                            Files.move(logPath, backupPath);
                        }
                    } catch (IOException e) {
                        System.err.println("[LogUtil] 日志轮转异常: " + e.getMessage());
                    }
                    currentDate = today;
                }
            }
        }
    }

    // ==================== 格式化 ====================

    private static String formatEntry(LogEntry entry) {
        return String.format("%s [%s] [%s] %s",
                LocalDateTime.now().format(TIME_FMT),
                entry.level(),
                entry.caller(),
                entry.message());
    }

    /**
     * 获取调用 LogUtil 的类名，跳过 LogUtil 自身和 JDK 内部帧。
     */
    private static String getCallerClassName() {
        // 利用 StackWalker 高效获取调用栈
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                          .walk(frames -> frames
                                  .filter(f -> f.getDeclaringClass() != LogUtil.class)
                                  .findFirst()
                                  .map(f -> f.getDeclaringClass().getSimpleName())
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
            pos += String.valueOf(args[argIdx - 1]).length();
        }
        return sb.toString();
    }

    // ==================== 日志条目 ====================

    private record LogEntry(String level, String caller, String message) {
    }
}
