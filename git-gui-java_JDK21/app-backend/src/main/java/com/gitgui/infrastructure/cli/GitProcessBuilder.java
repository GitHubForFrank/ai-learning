package com.gitgui.infrastructure.cli;

import com.gitgui.core.async.ProgressCallback;
import com.gitgui.core.exception.ErrorCode;
import com.gitgui.core.exception.GitGuiException;
import com.gitgui.core.util.GitEncodingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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

    /** Git 可执行文件路径（null 表示从 PATH 检测） */
    private static String gitExecutable;

    /**
     * 设置 Git 可执行文件路径。
     *
     * @param path Git 路径
     */
    public static void setGitExecutable(String path) {
        gitExecutable = path;
    }

    /**
     * 执行 Git 命令（参数数组形式，禁止字符串拼接）。
     *
     * @param repoPath 工作目录
     * @param args     Git 命令参数数组（如 ["push", "--force-with-lease", "origin", "main"]）
     * @param callback 进度回调（可空）
     * @return 命令输出
     * @throws GitGuiException 执行失败
     */
    public static String execute(String repoPath, List<String> args, ProgressCallback callback) {
        // 命令构造：git -c core.quotepath=false <args>
        List<String> command = new ArrayList<>();
        command.add(resolveGit());
        // BR-42：强制 core.quotepath=false，避免中文路径乱码
        command.add("-c");
        command.add("core.quotepath=false");
        command.addAll(args);

        ProcessBuilder pb = new ProcessBuilder(command);
        if (repoPath != null && !repoPath.isBlank()) {
            pb.directory(new File(repoPath));
        }
        // 强制 UTF-8 编码
        pb.environment().putAll(GitEncodingUtil.gitEnvironment());
        pb.redirectErrorStream(true);

        log.info("执行 Git 命令：{} (dir={})", String.join(" ", command), repoPath);
        Process process = null;
        try {
            process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), Charset.forName(GitEncodingUtil.ENCODING)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                    if (callback != null) {
                        callback.onOutput(line);
                        if (callback.isCancelled()) {
                            process.destroyForcibly();
                            throw new GitGuiException(ErrorCode.TASK_CANCELED, "任务已取消");
                        }
                    }
                }
            }
            boolean finished = process.waitFor(60, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, "Git 命令执行超时");
            }
            int exit = process.exitValue();
            if (exit != 0) {
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
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * 解析 Git 可执行文件路径（未配置时从 PATH 检测）。
     */
    private static String resolveGit() {
        if (gitExecutable != null && !gitExecutable.isBlank()) {
            return gitExecutable;
        }
        String osName = System.getProperty("os.name", "").toLowerCase();
        String cmd = osName.contains("win") ? "git.exe" : "git";
        // 简化：依赖 PATH 查找
        return cmd;
    }
}
