package com.gitgui.application.service;

import com.gitgui.core.exception.ErrorCode;
import com.gitgui.core.exception.GitGuiException;
import com.gitgui.domain.model.RemoteConfig;
import com.gitgui.domain.service.RemoteConfigService;
import com.gitgui.infrastructure.cli.GitProcessBuilder;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 远程配置服务实现
 * <p>关联 BR：BR-09。通过 {@code git remote} 命令直接修改本地仓库配置。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class RemoteConfigServiceImpl implements RemoteConfigService {

    private static final Logger log = LoggerFactory.getLogger(RemoteConfigServiceImpl.class);

    @Override
    public List<RemoteConfig> list(String repoPath) {
        // 通过 git remote -v 查询
        try {
            String output = GitProcessBuilder.execute(repoPath, List.of("remote", "-v"), null);
            return parseRemoteOutput(output);
        } catch (Exception e) {
            log.warn("查询 Remote 失败：{}", e.getMessage());
            throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, "查询 Remote 失败：" + e.getMessage(), e);
        }
    }

    @Override
    public void add(String repoPath, String name, String url) {
        if (name == null || name.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "Remote 名称不能为空");
        }
        if (url == null || url.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "Remote URL 不能为空");
        }
        GitProcessBuilder.execute(repoPath, List.of("remote", "add", name, url), null);
        log.info("新增 Remote：repoPath={}, name={}", repoPath, name);
    }

    @Override
    public void update(String repoPath, String name, String url) {
        GitProcessBuilder.execute(repoPath, List.of("remote", "set-url", name, url), null);
        log.info("更新 Remote URL：repoPath={}, name={}", repoPath, name);
    }

    @Override
    public void delete(String repoPath, String name) {
        GitProcessBuilder.execute(repoPath, List.of("remote", "remove", name), null);
        log.info("删除 Remote：repoPath={}, name={}", repoPath, name);
    }

    @Override
    public void rename(String repoPath, String oldName, String newName) {
        GitProcessBuilder.execute(repoPath, List.of("remote", "rename", oldName, newName), null);
        log.info("重命名 Remote：repoPath={}, {} → {}", repoPath, oldName, newName);
    }

    /**
     * 解析 git remote -v 输出。
     */
    private List<RemoteConfig> parseRemoteOutput(String output) {
        java.util.Map<String, RemoteConfig.RemoteConfigBuilder> builders = new java.util.LinkedHashMap<>();
        for (String line : output.split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            // 格式：origin\tgit@github.com:foo/bar.git (fetch)
            String[] parts = line.split("\\s+");
            if (parts.length < 2) {
                continue;
            }
            String name = parts[0];
            String url = parts[1];
            String type = parts.length >= 3 ? parts[2] : "";
            RemoteConfig.RemoteConfigBuilder b = builders.computeIfAbsent(name, k -> RemoteConfig.builder()
                                                                                                 .name(k));
            if (type.contains("fetch")) {
                b.fetchUrl(url);
            } else if (type.contains("push")) {
                b.pushUrl(url);
            }
        }
        return builders.values()
                       .stream()
                       .map(RemoteConfig.RemoteConfigBuilder::build)
                       .toList();
    }
}
