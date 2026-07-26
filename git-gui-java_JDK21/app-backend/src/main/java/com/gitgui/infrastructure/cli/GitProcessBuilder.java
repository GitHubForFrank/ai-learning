package com.gitgui.infrastructure.cli;

import com.gitgui.core.async.ProgressCallback;
import com.gitgui.core.exception.ErrorCode;
import com.gitgui.core.exception.GitGuiException;
import com.gitgui.core.util.GitEncodingUtil;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Git CLI 进程构造器
 * <p>强制 UTF-8 编码并设置 core.quotepath=false（BR-42）。</p>
 * <p>使用 {@link ProcessBuilder} 参数数组，禁止字符串拼接 shell（D1 安全约束）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class GitProcessBuilder {

    private static final Logger log = LoggerFactory.getLogger(GitProcessBuilder.class);
    @Setter
    private static String gitExecutable;

    /**
     * 执行 Git 命令（参数数组形式，禁止字符串拼接），非零退出码抛异常。
     *
     * @param repoPath 工作目录
     * @param args     Git 命令参数数组
     * @param callback 进度回调（可空）
     * @return 命令输出
     * @throws GitGuiException 执行失败或非零退出码
     */
    public static String execute(String repoPath, List<String> args, ProgressCallback callback) {
        return executeInternal(repoPath, args, callback, false);
    }

    /**
     * 执行 Git 命令，允许非零退出码（查询类命令推荐）。
     *
     * @param repoPath 工作目录
     * @param args     Git 命令参数数组
     * @param callback 进度回调（可空）
     * @param allowNonZero 是否允许非零退出码
     * @return 命令输出（即使退出码非零也返回）
     * @throws GitGuiException 执行异常（IO / 中断 / 超时）
     */
    public static String executeQuietly(String repoPath, List<String> args, ProgressCallback callback,
                                         boolean allowNonZero) {
        return executeInternal(repoPath, args, callback, allowNonZero);
    }

    private static String executeInternal(String repoPath, List<String> args, ProgressCallback callback,
                                           boolean allowNonZero) {
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
        pb.environment().putAll(GitEncodingUtil.gitEnvironment());
        pb.redirectErrorStream(true);

        log.info("执行 Git 命令：{} (dir={})", String.join(" ", command), repoPath);
        Process process = null;
        // 取消监听器：每 250ms 检查 callback.isCancelled()，命中立即强杀进程，
        // 解决 git 在网络/认证阶段 long-running 无输出导致 readLine 永远阻塞的问题。
        Thread cancelWatcher = null;
        try {
            process = pb.start();
            cancelWatcher = startCancelWatcher(process, callback);
            StringBuilder output = new StringBuilder();
            try (Reader reader = new InputStreamReader(process.getInputStream(),
                    Charset.forName(GitEncodingUtil.ENCODING))) {
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
                            output.append(line).append('\n');
                            if (callback != null) {
                                callback.onOutput(line);
                                if (callback.isCancelled()) {
                                    process.destroyForcibly();
                                    throw new GitGuiException(ErrorCode.TASK_CANCELED, "任务已取消");
                                }
                            }
                            lineBuf.setLength(0);
                        }
                    } else {
                        lineBuf.append((char) ch);
                    }
                }
                // 进程结束时若还有残余内容（无行终止符结尾），也输出
                if (!lineBuf.isEmpty()) {
                    String line = lineBuf.toString();
                    output.append(line).append('\n');
                    if (callback != null) {
                        callback.onOutput(line);
                    }
                }
            }
            boolean finished = process.waitFor(60, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, "Git 命令执行超时");
            }
            int exit = process.exitValue();
            if (exit != 0 && !allowNonZero) {
                throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED,
                        "Git 命令执行失败（exit=" + exit + "）：" + output);
            }
            return output.toString();
        } catch (IOException e) {
            throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, "Git 命令执行异常：" + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GitGuiException(ErrorCode.TASK_CANCELED, "任务被中断");
        } finally {
            if (cancelWatcher != null && cancelWatcher.isAlive()) {
                cancelWatcher.interrupt();
            }
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * 启动取消监听线程：每 250ms 检查一次 callback.isCancelled()，命中立即强制销毁进程。
     * <p>解决 git 在认证阶段 / 网络超时 / 长时间无输出时 cancel 按钮失效的问题。</p>
     */
    private static Thread startCancelWatcher(Process process, ProgressCallback callback) {
        if (callback == null) return null;
        Thread watcher = new Thread(() -> {
            try {
                while (process.isAlive()) {
                    if (callback.isCancelled()) {
                        log.info("CancelWatcher: 检测到取消请求，强杀 git 进程 pid={}", process);
                        process.destroyForcibly();
                        return;
                    }
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            } catch (Throwable t) {
                log.debug("CancelWatcher 异常退出：{}", t.getMessage());
            }
        }, "git-cancel-watcher");
        watcher.setDaemon(true);
        watcher.start();
        return watcher;
    }

    /**
     * 解析 Git 可执行文件路径（未配置时从 PATH 检测）。
     */
    private static String resolveGit() {
        if (gitExecutable != null && !gitExecutable.isBlank()) {
            return gitExecutable;
        }
        String osName = System.getProperty("os.name", "").toLowerCase();
        // 简化：依赖 PATH 查找
        return osName.contains("win") ? "git.exe" : "git";
    }
}
