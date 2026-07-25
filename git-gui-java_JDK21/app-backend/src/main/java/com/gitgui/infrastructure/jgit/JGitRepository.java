package com.gitgui.infrastructure.jgit;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * JGit 仓库工具
 * <p>封装 JGit {@link Repository} 的创建与缓存，统一仓库打开逻辑。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public final class JGitRepository {

    private static final Logger log = LoggerFactory.getLogger(JGitRepository.class);

    private JGitRepository() {
        // 工具类禁止实例化
    }

    /**
     * 打开指定路径的 Git 仓库。
     *
     * @param repoPath 仓库绝对路径
     * @return JGit Repository 实例
     * @throws IOException 仓库打开失败
     */
    public static Repository open(String repoPath) throws IOException {
        File gitDir = new File(repoPath, ".git");
        return new FileRepositoryBuilder()
                .setGitDir(gitDir)
                .readEnvironment()
                .findGitDir()
                .build();
    }

    /**
     * 初始化新仓库（git init）。
     *
     * @param dir  目标目录
     * @param bare 是否裸仓库
     * @return JGit Repository 实例
     * @throws IOException 初始化失败
     */
    public static Repository init(String dir, boolean bare) throws IOException {
        File gitDir = bare ? new File(dir) : new File(dir, ".git");
        try (Repository repo = new FileRepositoryBuilder().setGitDir(gitDir).build()) {
            repo.create(bare);
            log.info("仓库已初始化：{}", dir);
            return repo;
        }
    }
}
