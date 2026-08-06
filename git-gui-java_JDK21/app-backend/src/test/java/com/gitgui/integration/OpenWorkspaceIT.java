package com.gitgui.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gitgui.di.AppModule;
import com.gitgui.di.AsyncModule;
import com.gitgui.di.DatabaseModule;
import com.gitgui.di.GitModule;
import com.gitgui.di.RedLineModule;
import com.gitgui.di.ServiceModule;
import com.gitgui.domain.model.FileStatus;
import com.gitgui.domain.model.LogEntry;
import com.gitgui.domain.model.RepositoryMeta;
import com.gitgui.domain.service.RepositoryService;
import com.gitgui.domain.service.StatusService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 打开工作空间集成测试
 * <p>验证：</p>
 * <ol>
 *   <li>全新环境下 db 自动创建（Flyway 迁移，无需打包 db 文件）</li>
 *   <li>RepositoryService.openRepository 能正常打开真实 Git 仓库</li>
 *   <li>StatusService 能加载文件状态与提交日志</li>
 * </ol>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
class OpenWorkspaceIT {

    /**
     * 测试用真实 Git 仓库路径
     */
    private static final String TEST_REPO = "D:\\workspaces\\workspace_github\\skills";

    @Test
    @DisplayName("全新环境自动建库 + 打开真实 Git 仓库并加载状态与日志")
    void testOpenWorkspace() {
        // 测试仓库不存在时跳过（保证可移植性）
        Assumptions.assumeTrue(Files.isDirectory(Path.of(TEST_REPO)), "测试仓库不存在，跳过：" + TEST_REPO);

        // 1. 创建 Guice 注入器 —— SqliteDataSource 构造时触发 Flyway 自动迁移（验证 db 机制）
        Injector injector = Guice.createInjector(new AppModule(), new DatabaseModule(), new GitModule(), new ServiceModule(), new RedLineModule(),
                                                 new AsyncModule());
        System.out.println("[OK] Guice 注入器创建成功（db 已由 Flyway 自动迁移）");

        // 2. 打开真实 Git 仓库
        RepositoryService repositoryService = injector.getInstance(RepositoryService.class);
        RepositoryMeta meta = repositoryService.openRepository(TEST_REPO);

        assertNotNull(meta, "RepositoryMeta 不应为 null");
        assertEquals("main", meta.getCurrentBranch(), "当前分支应为 main");
        assertTrue(meta.getRemoteUrl()
                       .contains("github.com"), "远程地址应含 github.com");
        System.out.println("[OK] 打开仓库成功：branch=" + meta.getCurrentBranch() + ", remote=" + meta.getRemoteUrl() + ", clean="
                                   + !meta.isHasUncommittedChanges());

        // 3. 加载文件状态
        StatusService statusService = injector.getInstance(StatusService.class);
        List<FileStatus> files = statusService.getStatus(TEST_REPO, true, false);
        System.out.println("[OK] 文件状态加载成功：共 " + files.size() + " 个文件变更");
        for (FileStatus f : files) {
            System.out.println("  [" + f.getState() + "] " + f.getPath());
        }

        // 4. 加载提交日志
        List<LogEntry> logs = statusService.getLog(TEST_REPO, 1, 5);
        assertFalse(logs.isEmpty(), "提交日志不应为空");
        System.out.println("[OK] 提交日志加载成功：共 " + logs.size() + " 条");
        for (LogEntry log : logs) {
            System.out.println("  " + log.getShortId() + " " + log.getAuthor() + " " + log.getMessage());
        }
    }
}
