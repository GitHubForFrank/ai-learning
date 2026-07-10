package com.example.mcp.handler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * CommandHandler 单元测试
 *
 * @author FrankKang
 * @since 2026-07-10
 */
@SpringBootTest
class CommandHandlerTest {

    @TempDir
    Path tempDir;

    @Autowired
    private CommandHandler commandHandler;

    @BeforeEach
    void setUp() {
        // 无需额外初始化
    }

    @Test
    void testEchoCommand() {
        String result = commandHandler.commandExecute("echo HelloWorld", tempDir.toString(), null);
        assertTrue(result.contains("HelloWorld"));
        assertTrue(result.contains("退出码: 0"));
    }

    @Test
    void testDirCommand() {
        String result = commandHandler.commandExecute("dir", tempDir.toString(), null);
        assertTrue(result.contains("退出码: 0"));
    }

    @Test
    void testCustomWorkDir() throws Exception {
        // 在临时目录下创建一个标记文件
        Path marker = tempDir.resolve("marker.txt");
        Files.writeString(marker, "test");

        String result = commandHandler.commandExecute("dir", tempDir.toString(), null);
        assertTrue(result.contains("marker.txt"));
    }

    @Test
    void testEmptyCommandShouldReturnError() {
        String result = commandHandler.commandExecute("", null, null);
        assertTrue(result.contains("错误: 命令不能为空"));
    }

    @Test
    void testNullCommandShouldReturnError() {
        String result = commandHandler.commandExecute(null, null, null);
        assertTrue(result.contains("错误: 命令不能为空"));
    }

    @Test
    void testNonExistentCommandShouldHaveNonZeroExit() {
        String result = commandHandler.commandExecute("nonexistent_cmd_xyz_123", tempDir.toString(), null);
        assertFalse(result.contains("退出码: 0"));
    }

    @Test
    void testDefaultWorkDir() {
        String result = commandHandler.commandExecute("echo testDefault", null, null);
        assertTrue(result.contains("退出码: 0"));
        // 不指定 workDir 时默认使用 user.dir
    }

    @Test
    void testTimeoutShouldWork() {
        // 使用 ping 命令模拟长时间执行（ping 6次约5秒，超时2秒必然触发）
        String osName = System.getProperty("os.name").toLowerCase();
        String sleepCmd;
        if (osName.contains("win")) {
            sleepCmd = "ping -n 6 127.0.0.1";
        } else {
            sleepCmd = "sleep 10";
        }
        // 设置超时 2 秒，workDir 用 null 避免 tempDir 被进程锁定导致清理失败
        String result = commandHandler.commandExecute(sleepCmd, null, 2);
        assertTrue(result.contains("超时") || result.contains("退出码: -1"),
                "Expected timeout but got: " + result);
    }

    @Test
    void testNonExistentWorkDirFallback() {
        File nonExistent = tempDir.resolve("nonExistentDir").toFile();
        String result = commandHandler.commandExecute("echo fallback", nonExistent.getAbsolutePath(), null);
        assertTrue(result.contains("退出码: 0"));
    }

    @Test
    void testGitStatusInProjectDir() {
        String result = commandHandler.commandExecute("git status", null, 10);
        // 只要能执行（不管当前目录是不是 git 仓库），不应该抛异常
        assertFalse(result.contains("命令执行异常"));
    }

    @Test
    void testMultiLineOutput() {
        String result = commandHandler.commandExecute("echo line1 && echo line2 && echo line3", tempDir.toString(), null);
        assertTrue(result.contains("line1"));
        assertTrue(result.contains("line2"));
        assertTrue(result.contains("line3"));
    }
}
