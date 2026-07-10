package com.example.mcp.handler;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.example.mcp.util.LogUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * 命令行执行处理器，提供命令执行 MCP 工具。
 * AI 插件可通过此工具在工作区内执行系统命令，工具将原文返回执行结果。
 *
 * @author FrankKang
 * @since 2026-07-10 07:51
 */
@Service
public class CommandHandler {

    /**
     * 默认命令执行超时时间（秒）
     */
    private static final int DEFAULT_TIMEOUT = 30;

    /**
     * 在工作区目录下执行系统命令，并返回原文输出结果。
     * <p>
     * 该工具允许 AI 插件传入任意的命令行指令，在当前工作区目录下执行。
     * 执行结果（包括标准输出和标准错误）将以原文形式返回给 AI 插件，
     * 从而避免 AI 插件每次执行命令时都需要用户手动确认。
     *
     * @param command 要执行的命令（例如 "git status", "mvn compile", "dir" 等）
     * @param workDir 命令执行的工作目录（绝对路径），可选，默认为当前项目工作区根目录
     * @param timeout 命令执行超时时间（秒），可选，默认 30 秒
     * @return 命令执行的原文字符串输出，如果执行失败则返回错误信息
     */
    @Tool(name = "command_execute", description = """
        执行系统命令并返回原文输出。AI 插件可传入命令行指令在当前工作目录下执行，
        无需用户手动确认。支持所有命令行指令，如 git、mvn、dir、echo 等。
        执行结果包括标准输出和标准错误，以原文形式返回。""")
    public String commandExecute(@ToolParam(description = "要执行的命令行指令，如 git status、mvn compile、dir 等") String command,
        @ToolParam(description = "命令执行的绝对路径工作目录，默认为当前项目工作区根目录", required = false) String workDir,
        @ToolParam(description = "执行超时时间（秒），默认 30 秒", required = false) Integer timeout) {

        if (command == null || command.isBlank()) {
            return "错误: 命令不能为空";
        }

        int effectiveTimeout = (timeout != null && timeout > 0) ? timeout : DEFAULT_TIMEOUT;
        File effectiveWorkDir = resolveWorkDir(workDir);

        LogUtil.info("执行命令: {} | 工作目录: {} | 超时: {}秒", command, effectiveWorkDir.getAbsolutePath(), effectiveTimeout);

        try {
            ProcessResult result = executeCommand(command, effectiveWorkDir, effectiveTimeout);
            return formatResult(result);
        } catch (Exception e) {
            LogUtil.error("命令执行异常: {}", e.getMessage(), e);
            return "命令执行异常: " + e.getMessage();
        }
    }

    /**
     * 解析工作目录，若未指定则使用当前项目工作区根目录
     */
    private File resolveWorkDir(String workDir) {
        if (workDir != null && !workDir.isBlank()) {
            File dir = new File(workDir);
            if (dir.exists() && dir.isDirectory()) {
                return dir;
            }
            LogUtil.warn("指定的工作目录不存在或不是目录: {}，回退到当前用户目录", workDir);
        }
        // 默认使用 user.dir（当前工作区根目录）
        return new File(System.getProperty("user.dir"));
    }

    /**
     * 使用 ProcessBuilder 执行系统命令
     */
    private ProcessResult executeCommand(String command, File workDir, int timeoutSeconds) throws Exception {
        ProcessBuilder processBuilder = buildProcess(command, workDir);
        Process process = processBuilder.start();

        // 分别在独立线程中读取标准输出和标准错误，避免阻塞
        CompletableFuture<String> stdoutFuture = CompletableFuture.supplyAsync(() -> readStream(process.getInputStream()));
        CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(() -> readStream(process.getErrorStream()));

        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            // 等待异步读取完成（最多1秒），然后关闭流
            try {
                CompletableFuture.allOf(stdoutFuture, stderrFuture)
                                 .get(1, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                // 忽略清理阶段的异常
            }
            String stdout = stdoutFuture.getNow("");
            String stderr = stderrFuture.getNow("");
            return new ProcessResult(-1, stdout, stderr, "命令执行超时（" + timeoutSeconds + "秒）");
        }

        // 进程正常结束，等待异步读取完成
        String stdout = stdoutFuture.get();
        String stderr = stderrFuture.get();
        int exitCode = process.exitValue();
        return new ProcessResult(exitCode, stdout, stderr, null);
    }

    /**
     * 根据操作系统构建 ProcessBuilder
     */
    private ProcessBuilder buildProcess(String command, File workDir) {
        ProcessBuilder builder = new ProcessBuilder();
        builder.directory(workDir);
        builder.redirectErrorStream(false);

        String osName = System.getProperty("os.name")
                              .toLowerCase();
        if (osName.contains("win")) {
            builder.command("cmd", "/c", command);
        } else {
            builder.command("sh", "-c", command);
        }
        return builder;
    }

    /**
     * 读取流内容为字符串
     */
    private String readStream(java.io.InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines()
                         .collect(Collectors.joining(System.lineSeparator()));
        } catch (Exception e) {
            return "读取输出流失败: " + e.getMessage();
        }
    }

    /**
     * 格式化最终返回结果
     */
    private String formatResult(ProcessResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("退出码: ")
          .append(result.exitCode())
          .append("\n");

        if (result.timeoutMessage() != null) {
            sb.append(result.timeoutMessage())
              .append("\n");
        }

        if (result.stdout() != null && !result.stdout()
                                              .isBlank()) {
            sb.append("--- 标准输出 ---\n");
            sb.append(result.stdout());
        }

        if (result.stderr() != null && !result.stderr()
                                              .isBlank()) {
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append("--- 标准错误 ---\n");
            sb.append(result.stderr());
        }

        return sb.isEmpty() ? "命令执行完成，无输出内容。" : sb.toString();
    }

    /**
     * 命令执行结果记录
     */
    private record ProcessResult(int exitCode, String stdout, String stderr, String timeoutMessage) {

    }
}
