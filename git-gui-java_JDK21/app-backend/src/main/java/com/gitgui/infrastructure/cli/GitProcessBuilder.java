package com.gitgui.infrastructure.cli;

import com.gitgui.core.async.ProgressCallback;
import com.gitgui.core.exception.ErrorCode;
import com.gitgui.core.exception.GitGuiException;
import com.gitgui.core.util.GitEncodingUtil;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Git CLI 进程构造器
 * <p>强制 UTF-8 编码并设置 core.quotepath=false（BR-42）。</p>
 * <p>使用 {@link ProcessBuilder} 参数数组，禁止字符串拼接 shell（D1 安全约束）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class GitProcessBuilder {

    /**
     * push/pull/fetch/clone 等长耗时操作的空闲超时（毫秒）：
     * 若连续无新输出超过此阈值且 git 进程仍存活，视为远端 hang，强制终止等待。
     * <p>仅对带 {@link ProgressCallback} 的异步命令启用；同步查询命令不受此限制。</p>
     */
    static final long DEFAULT_IDLE_TIMEOUT_MS = 30_000;
    private static final Logger log = LoggerFactory.getLogger(GitProcessBuilder.class);
    /**
     * P1-005: watcher 线程池（Daemon 线程，避免每次 push/pull 裸 new Thread）
     */
    private static final ExecutorService WATCHER_POOL = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "git-cancel-watcher");
        t.setDaemon(true);
        return t;
    });
    @Setter
    private static String gitExecutable;

    /**
     * 执行 Git 命令（参数数组形式，禁止字符串拼接），非零退出码抛异常。
     * <p>当 callback 非空时启用空闲超时保护（默认 30 秒无输出即终止，防止 Gitee/GitHub 远端 hang）。</p>
     *
     * @param repoPath 工作目录
     * @param args     Git 命令参数数组
     * @param callback 进度回调（可空）
     * @return 命令输出
     * @throws GitGuiException 执行失败或非零退出码
     */
    public static String execute(String repoPath, List<String> args, ProgressCallback callback) {
        long idleTimeoutMs = callback != null ? DEFAULT_IDLE_TIMEOUT_MS : 0;
        return executeInternal(repoPath, args, callback, false, idleTimeoutMs);
    }

    /**
     * 执行 Git 命令，允许非零退出码（查询类命令推荐）。
     * <p>不启用空闲超时（查询类命令通常瞬时完成）。</p>
     *
     * @param repoPath     工作目录
     * @param args         Git 命令参数数组
     * @param callback     进度回调（可空）
     * @param allowNonZero 是否允许非零退出码
     * @return 命令输出（即使退出码非零也返回）
     * @throws GitGuiException 执行异常（IO / 中断 / 超时）
     */
    public static String executeQuietly(String repoPath, List<String> args, ProgressCallback callback, boolean allowNonZero) {
        return executeInternal(repoPath, args, callback, allowNonZero, 0);
    }

    private static String executeInternal(String repoPath, List<String> args, ProgressCallback callback, boolean allowNonZero, long idleTimeoutMs) {
        // 命令构造：git -c core.quotepath=false <args>
        List<String> command = new ArrayList<>();
        command.add(resolveGit());
        command.add("-c");
        command.add("core.quotepath=false");
        command.addAll(args);

        ProcessBuilder pb = new ProcessBuilder(command);
        if (repoPath != null && !repoPath.isBlank()) {
            pb.directory(new File(repoPath));
        }
        pb.environment()
          .putAll(GitEncodingUtil.gitEnvironment());
        pb.redirectErrorStream(true);

        log.info("执行 Git 命令：{} (dir={})", String.join(" ", command), repoPath);
        Process process = null;
        // 取消 + 空闲超时监听器：每 250ms 轮询取消标记与最后输出时间，
        // 解决 git 在网络/认证/Gitee post-receive 阶段 long-running 无输出导致永久阻塞的问题。
        Future<?> cancelWatcher = null;
        AtomicLong lastOutputMs = new AtomicLong(System.currentTimeMillis());
        AtomicBoolean idleTimedOut = new AtomicBoolean(false);
        try {
            process = pb.start();
            cancelWatcher = startCancelWatcher(process, callback, idleTimeoutMs, lastOutputMs, idleTimedOut);
            StringBuilder output = new StringBuilder();
            try (Reader reader = new InputStreamReader(process.getInputStream(), Charset.forName(GitEncodingUtil.ENCODING))) {
                // 逐字符读取：git push/pull/fetch 的 --progress 输出以 \r 分隔（原地覆盖），
                // BufferedReader.readLine() 只认 \n，导致进度行被 OS 管道缓冲后一次性输出，
                // 表现为 UI 一直卡在"等待输出…"直到进程结束。
                // 改为逐字符读取，\r 和 \n 都视为行终止符，实时回调到 UI。
                StringBuilder lineBuf = new StringBuilder();
                int ch;
                while ((ch = reader.read()) != -1) {
                    if (ch == '\r' || ch == '\n') {
                        // 连续 \r\n 只产生一行，避免空行
                        if (!lineBuf.isEmpty()) {
                            String line = lineBuf.toString();
                            output.append(line)
                                  .append('\n');
                            if (callback != null) {
                                callback.onOutput(line);
                                if (callback.isCancelled()) {
                                    process.destroyForcibly();
                                    throw new GitGuiException(ErrorCode.TASK_CANCELED, "任务已取消");
                                }
                            }
                            lineBuf.setLength(0);
                            // 每输出完整一行后更新心跳时间戳，供空闲超时监听器使用
                            lastOutputMs.set(System.currentTimeMillis());
                        }
                    } else {
                        lineBuf.append((char) ch);
                    }
                }
                // 进程结束时若还有残余内容（无行终止符结尾），也输出
                if (!lineBuf.isEmpty()) {
                    String line = lineBuf.toString();
                    output.append(line)
                          .append('\n');
                    if (callback != null) {
                        callback.onOutput(line);
                    }
                }
            }

            // reader 自然结束后（EOF）或由于 process.destroyForcibly() 导致流关闭后，检查是否空闲超时触发
            if (idleTimedOut.get()) {
                throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, "远端服务无响应：已超过 " + (idleTimeoutMs / 1000)
                        + " 秒未收到新输出，推送可能实际已成功，但远端响应超时，已终止等待");
            }

            boolean finished = process.waitFor(60, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, "Git 命令执行超时（60 分钟）");
            }
            int exit = process.exitValue();
            if (exit != 0 && !allowNonZero) {
                throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, "Git 命令执行失败（exit=" + exit + "）：" + output);
            }
            return output.toString();
        } catch (IOException e) {
            // 若是因为 idle timeout 导致 process.destroyForcibly() 后流关闭，用空闲超时消息
            if (idleTimedOut.get()) {
                throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, "远端服务无响应：已超过 " + (idleTimeoutMs / 1000)
                        + " 秒未收到新输出，推送可能实际已成功，但远端响应超时，已终止等待");
            }
            throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, "Git 命令执行异常：" + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread()
                  .interrupt();
            throw new GitGuiException(ErrorCode.TASK_CANCELED, "任务被中断");
        } finally {
            if (cancelWatcher != null && !cancelWatcher.isDone()) {
                cancelWatcher.cancel(true);
            }
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * 启动取消 + 空闲超时监听线程：每 250ms 轮询。
     * <ul>
     *   <li>检测到 {@link ProgressCallback#isCancelled()} → 立即强杀进程</li>
     *   <li>检测到 idleTimeoutMs 毫秒内无新输出且进程仍存活 → 标记 idleTimedOut 并强杀进程</li>
     * </ul>
     * <p>解决 git 在认证阶段 / 网络超时 / Gitee post-receive hook 长时间无输出时 cancel 按钮失效的问题。</p>
     *
     * @param process       被监控的 git 进程
     * @param callback      进度回调（用于取消检测，可为 null）
     * @param idleTimeoutMs 空闲超时阈值（毫秒），≤0 表示不启用
     * @param lastOutputMs  最后一次输出的时间戳（原子变量，由 reader 线程更新）
     * @param idleTimedOut  空闲超时标记（原子变量，命中后置 true）
     * @return 守护线程实例，无需 callback 且无超时需求时返回 null
     */
    /**
     * P1-005: 返回 Future 以支持 cancel(true)，线程来自 Daemon 缓存池
     */
    private static Future<?> startCancelWatcher(Process process, ProgressCallback callback, long idleTimeoutMs, AtomicLong lastOutputMs,
            AtomicBoolean idleTimedOut) {
        boolean needIdleWatch = idleTimeoutMs > 0;
        if (callback == null && !needIdleWatch) {
            return null;
        }

        return WATCHER_POOL.submit(() -> {
            try {
                while (process.isAlive()) {
                    if (callback != null && callback.isCancelled()) {
                        log.info("CancelWatcher: 检测到取消请求，强杀 git 进程");
                        process.destroyForcibly();
                        return;
                    }
                    if (needIdleWatch) {
                        long idle = System.currentTimeMillis() - lastOutputMs.get();
                        if (idle > idleTimeoutMs) {
                            log.warn("IdleWatcher: 空闲超时 {}ms（最后输出距今 {}ms），强杀 git 进程", idleTimeoutMs, idle);
                            idleTimedOut.set(true);
                            process.destroyForcibly();
                            return;
                        }
                    }
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException ie) {
                        Thread.currentThread()
                              .interrupt();
                        return;
                    }
                }
            } catch (Throwable t) {
                log.debug("Watcher 异常退出：{}", t.getMessage());
            }
        });
    }

    /**
     * 解析 Git 可执行文件路径（未配置时从 PATH 检测）。
     */
    private static String resolveGit() {
        if (gitExecutable != null && !gitExecutable.isBlank()) {
            return gitExecutable;
        }
        String osName = System.getProperty("os.name", "")
                              .toLowerCase();
        // 简化：依赖 PATH 查找
        return osName.contains("win") ? "git.exe" : "git";
    }
}
