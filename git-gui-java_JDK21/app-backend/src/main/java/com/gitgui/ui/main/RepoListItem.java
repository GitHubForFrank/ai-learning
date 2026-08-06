package com.gitgui.ui.main;

import com.gitgui.domain.model.RepositoryMeta;

import java.util.Objects;
import lombok.Getter;

/**
 * 侧边栏仓库列表项模型
 * <p>包装 {@link RepositoryMeta} + 收藏标记 + 扫描根目录路径，作为 ListView 的数据载体。</p>
 * <p>作为侧边栏唯一显示项，承载：</p>
 * <ul>
 *   <li>{@code repoMeta}：仓库元信息（路径、分支、HEAD、是否干净）</li>
 *   <li>{@code scanRootPath}：所属扫描根目录（用于侧边栏分组展示，可空）</li>
 *   <li>{@code favorite}：是否已收藏（与 favorite 表联动）</li>
 *   <li>{@code alias}：用户自定义别名（来自 favorite.alias，可空）</li>
 * </ul>
 *
 * @author FrankKang
 * @since 2026-07-24
 */
@Getter
public class RepoListItem {

    /**
     * 仓库元信息
     */
    private final RepositoryMeta repoMeta;
    /**
     * 所属扫描根目录路径，可空
     */
    private final String scanRootPath;
    /**
     * 是否已收藏
     */
    private final boolean favorite;
    /**
     * 用户自定义别名（来自 favorite 表，可空）
     */
    private final String alias;

    public RepoListItem(RepositoryMeta repoMeta, String scanRootPath, boolean favorite, String alias) {
        this.repoMeta = Objects.requireNonNull(repoMeta, "repoMeta 不能为空");
        this.scanRootPath = scanRootPath;
        this.favorite = favorite;
        this.alias = alias;
    }

    /**
     * 获取仓库绝对路径。
     *
     * @return 仓库绝对路径
     */
    public String getRepoPath() {
        return repoMeta.getRepoPath();
    }

    /**
     * 获取展示名称：优先 alias，其次目录名，最后回退路径。
     *
     * @return 展示名称
     */
    public String getDisplayName() {
        if (alias != null && !alias.isBlank()) {
            return alias;
        }
        String path = repoMeta.getRepoPath();
        if (path == null || path.isEmpty()) {
            return "?";
        }
        // 取最后一段路径分隔符之后的目录名（兼容 Windows / Unix）
        int winIdx = path.lastIndexOf('\\');
        int unixIdx = path.lastIndexOf('/');
        int idx = Math.max(winIdx, unixIdx);
        if (idx >= 0 && idx < path.length() - 1) {
            return path.substring(idx + 1);
        }
        return path;
    }

    /**
     * 获取副标题（路径或分支信息，用于二级文本）。
     *
     * @return 副标题
     */
    public String getSubtitle() {
        String branch = repoMeta.getCurrentBranch();
        if (branch != null && !branch.isEmpty()) {
            return branch + " · " + repoMeta.getRepoPath();
        }
        return repoMeta.getRepoPath();
    }

    /**
     * 切换收藏状态后生成新实例（不可变对象，便于 ObservableList diff）。
     */
    public RepoListItem withFavorite(boolean newFavorite, String newAlias) {
        return new RepoListItem(this.repoMeta, this.scanRootPath, newFavorite, newAlias);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RepoListItem)) {
            return false;
        }
        RepoListItem that = (RepoListItem) o;
        return Objects.equals(repoMeta.getRepoPath(), that.repoMeta.getRepoPath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(repoMeta.getRepoPath());
    }
}