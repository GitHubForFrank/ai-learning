package com.gitgui.infrastructure.jgit;

import com.gitgui.domain.model.request.PushRequest;
import com.gitgui.infrastructure.jgit.JGitOperationExecutor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Push 集成测试 —— 用来 reproduce ClassCastException / 其他 push 异常
 * <p>在 Windows / macOS / Linux 上创建本地 git 仓库 + file:// 远端，</p>
 * <p>尝试 push，验证 JGitOperationExecutor.push() 的错误处理和异常信息。</p>
 */
class PushTest {

    private Path workDir;
    private Path remoteDir;
    private JGitOperationExecutor executor;

    @BeforeEach
    void setUp() throws Exception {
        workDir = Files.createTempDirectory("pushit-source-");
        remoteDir = Files.createTempDirectory("pushit-remote-");
        // 初始化本地仓库
        try (Git git = Git.init().setDirectory(workDir.toFile()).call()) {
            // 配 user.name / user.email
            git.getRepository().getConfig().setString("user", null, "name", "Test User");
            git.getRepository().getConfig().setString("user", null, "email", "test@test.com");
            // 配 remote
            git.getRepository().getConfig().setString("remote", "origin", "url",
                    remoteDir.toUri().toString());
            git.getRepository().getConfig().setString("remote", "origin", "fetch",
                    "+refs/heads/*:refs/remotes/origin/*");
            git.getRepository().getConfig().save();
            // 提交一个文件
            Files.writeString(workDir.resolve("README.md"), "Hello World\n");
            git.add().addFilepattern("README.md").call();
            git.commit().setMessage("initial commit").call();
        }
        // 初始化远端裸仓库
        try (Git ignored = Git.init().setDirectory(remoteDir.toFile()).setBare(true).call()) {
            // bare repo 初始化
        }
        executor = new JGitOperationExecutor();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (workDir != null) {
            deleteRecursive(workDir.toFile());
        }
        if (remoteDir != null) {
            deleteRecursive(remoteDir.toFile());
        }
    }

    @Test
    @DisplayName("Push 到 file:// 远端 — happy path")
    @EnabledOnOs({OS.WINDOWS, OS.LINUX, OS.MAC})
    void testPushToFileRemote() {
        PushRequest req = PushRequest.builder()
                .repoPath(workDir.toString())
                .remote("origin")
                .force(false)
                .build();
        try {
            executor.push(req, null);
            System.out.println("[OK] Push 成功");
        } catch (Exception e) {
            // 打印完整异常链（用以诊断 ClassCastException）
            System.out.println("[FAILED] Push 失败，异常链：");
            Throwable cur = e;
            int depth = 0;
            while (cur != null && depth < 6) {
                System.out.println("  ".repeat(depth) + cur.getClass().getName()
                        + ": " + cur.getMessage());
                cur = cur.getCause();
                depth++;
            }
            // 不断言失败——这只是个诊断测试，看看实际抛什么
        }
    }

    @Test
    @DisplayName("Push 到不存在的远端 — 异常信息应该清晰")
    @EnabledOnOs({OS.WINDOWS, OS.LINUX, OS.MAC})
    void testPushToNonExistentRemote() {
        PushRequest req = PushRequest.builder()
                .repoPath(workDir.toString())
                .remote("non-existent-remote")
                .force(false)
                .build();
        try {
            executor.push(req, null);
            System.out.println("[UNEXPECTED] Push 居然成功了");
        } catch (Exception e) {
            // 打印完整异常链
            System.out.println("[EXPECTED] Push 失败，异常链：");
            Throwable cur = e;
            int depth = 0;
            while (cur != null && depth < 6) {
                System.out.println("  ".repeat(depth) + cur.getClass().getName()
                        + ": " + cur.getMessage());
                cur = cur.getCause();
                depth++;
            }
        }
    }

    @Test
    @DisplayName("Push 到 https 但没配 credentials — 异常信息应该清晰")
    @EnabledOnOs({OS.WINDOWS, OS.LINUX, OS.MAC})
    void testPushToHttpsNoCredentials() {
        PushRequest req = PushRequest.builder()
                .repoPath(workDir.toString())
                .remote("https://github.com/non-existent-user-12345/non-existent-repo.git")
                .force(false)
                .build();
        try {
            executor.push(req, null);
            System.out.println("[UNEXPECTED] Push 居然成功了");
        } catch (Exception e) {
            // 打印完整异常链
            System.out.println("[EXPECTED] Push 失败，异常链：");
            Throwable cur = e;
            int depth = 0;
            while (cur != null && depth < 6) {
                System.out.println("  ".repeat(depth) + cur.getClass().getName()
                        + ": " + cur.getMessage());
                cur = cur.getCause();
                depth++;
            }
        }
    }

    @Test
    @DisplayName("Push 到 Windows 本地路径（无 file:// 前缀）— 模拟用户场景")
    @EnabledOnOs(OS.WINDOWS)
    void testPushToWindowsLocalPath() {
        // Windows 本地路径（不用 file://），模拟用户可能的场景
        String winPath = remoteDir.toString().replace('\\', '/');
        PushRequest req = PushRequest.builder()
                .repoPath(workDir.toString())
                .remote(winPath)
                .force(false)
                .build();
        try {
            executor.push(req, null);
            System.out.println("[OK] Push 到 Windows 本地路径成功");
        } catch (Exception e) {
            System.out.println("[EXPECTED] Push 失败，异常链：");
            Throwable cur = e;
            int depth = 0;
            while (cur != null && depth < 6) {
                System.out.println("  ".repeat(depth) + cur.getClass().getName()
                        + ": " + cur.getMessage());
                cur = cur.getCause();
                depth++;
            }
        }
    }

    @Test
    @DisplayName("CLI 推送回退 — happy path（file:// remote）")
    @EnabledOnOs({OS.WINDOWS, OS.LINUX, OS.MAC})
    void testPushViaCliHappy() {
        PushRequest req = PushRequest.builder()
                .repoPath(workDir.toString())
                .remote("origin")
                .force(false)
                .build();
        try {
            String output = executor.pushViaCli(req);
            System.out.println("[OK] CLI Push 成功，输出：\n" + output);
        } catch (Exception e) {
            System.out.println("[FAILED] CLI Push 失败（环境可能没装 git）：");
            System.out.println("  " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @Test
    @DisplayName("CLI 推送回退 — 不存在的 remote（应失败且信息清晰）")
    @EnabledOnOs({OS.WINDOWS, OS.LINUX, OS.MAC})
    void testPushViaCliBadRemote() {
        PushRequest req = PushRequest.builder()
                .repoPath(workDir.toString())
                .remote("non-existent-remote")
                .force(false)
                .build();
        try {
            executor.pushViaCli(req);
            System.out.println("[UNEXPECTED] CLI Push 居然成功了");
        } catch (Exception e) {
            System.out.println("[EXPECTED] CLI Push 失败，异常链：");
            Throwable cur = e;
            int depth = 0;
            while (cur != null && depth < 6) {
                System.out.println("  ".repeat(depth) + cur.getClass().getName()
                        + ": " + cur.getMessage());
                cur = cur.getCause();
                depth++;
            }
        }
    }

    @Test
    @DisplayName("CLI 推送回退 + 已成功 push 后再次 push（不应 CCE）")
    @EnabledOnOs({OS.WINDOWS, OS.LINUX, OS.MAC})
    void testPushViaCliAfterFirstSuccess() {
        // 场景：先 push 一次成功，验证不会因为第二次调用而 CCE
        try {
            executor.push(PushRequest.builder()
                    .repoPath(workDir.toString()).remote("origin").build(), null);
            System.out.println("[OK] 第一次 Push 成功");
            // 第二次用 CLI 推（验证 CLI 不会 CCE）
            String output = executor.pushViaCli(PushRequest.builder()
                    .repoPath(workDir.toString()).remote("origin").build());
            System.out.println("[OK] 第二次 CLI Push 成功：\n" + output);
        } catch (Exception e) {
            System.out.println("[FAILED] 异常链：");
            Throwable cur = e;
            int depth = 0;
            while (cur != null && depth < 6) {
                System.out.println("  ".repeat(depth) + cur.getClass().getName()
                        + ": " + cur.getMessage());
                cur = cur.getCause();
                depth++;
            }
        }
    }

    private static void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }
}
