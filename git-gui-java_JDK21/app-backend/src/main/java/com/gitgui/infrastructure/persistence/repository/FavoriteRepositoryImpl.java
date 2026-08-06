package com.gitgui.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gitgui.core.util.IdUtil;
import com.gitgui.domain.model.Favorite;
import com.gitgui.domain.repository.FavoriteRepository;
import com.gitgui.infrastructure.persistence.mapper.FavoriteMapper;
import com.gitgui.infrastructure.persistence.mybatis.MyBatisSqlSessionManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 收藏仓储 MyBatis-Plus 实现
 * <p>遵循 BR-03（唯一性）、BR-04（置顶与排序）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
@Singleton
public class FavoriteRepositoryImpl implements FavoriteRepository {

    private static final Logger log = LoggerFactory.getLogger(FavoriteRepositoryImpl.class);

    private final MyBatisSqlSessionManager sessionManager;

    @Inject
    public FavoriteRepositoryImpl(MyBatisSqlSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void save(Favorite favorite) {
        if (favorite.getId() == null) {
            favorite.setId(IdUtil.newId());
        }
        LocalDateTime now = LocalDateTime.now();
        if (favorite.getCreatedAt() == null) {
            favorite.setCreatedAt(now);
        }
        favorite.setUpdatedAt(now);
        try (var session = sessionManager.openSession()) {
            FavoriteMapper mapper = session.getMapper(FavoriteMapper.class);
            // INSERT OR REPLACE 语义
            Favorite existing = findByRepoPath(favorite.getRepoPath());
            if (existing != null) {
                favorite.setId(existing.getId());
                mapper.updateById(favorite);
            } else {
                mapper.insert(favorite);
            }
        }
    }

    @Override
    public Favorite findById(String id) {
        try (var session = sessionManager.openSession()) {
            FavoriteMapper mapper = session.getMapper(FavoriteMapper.class);
            return mapper.selectById(id);
        }
    }

    @Override
    public Favorite findByRepoPath(String repoPath) {
        try (var session = sessionManager.openSession()) {
            FavoriteMapper mapper = session.getMapper(FavoriteMapper.class);
            return mapper.selectOne(new LambdaQueryWrapper<Favorite>().eq(Favorite::getRepoPath, repoPath));
        }
    }

    @Override
    public List<Favorite> findAll() {
        try (var session = sessionManager.openSession()) {
            FavoriteMapper mapper = session.getMapper(FavoriteMapper.class);
            return mapper.selectList(new LambdaQueryWrapper<Favorite>().orderByDesc(Favorite::isPinned)
                                                                       .orderByAsc(Favorite::getSortOrder)
                                                                       .orderByDesc(Favorite::getCreatedAt));
        }
    }

    @Override
    public List<Favorite> findByGroup(String group) {
        try (var session = sessionManager.openSession()) {
            FavoriteMapper mapper = session.getMapper(FavoriteMapper.class);
            return mapper.selectList(new LambdaQueryWrapper<Favorite>().eq(Favorite::getGroup, group)
                                                                       .orderByDesc(Favorite::isPinned)
                                                                       .orderByAsc(Favorite::getSortOrder));
        }
    }

    @Override
    public void deleteById(String id) {
        try (var session = sessionManager.openSession()) {
            FavoriteMapper mapper = session.getMapper(FavoriteMapper.class);
            mapper.deleteById(id);
        }
    }

    @Override
    public boolean existsByRepoPath(String repoPath) {
        try (var session = sessionManager.openSession()) {
            FavoriteMapper mapper = session.getMapper(FavoriteMapper.class);
            return mapper.selectCount(new LambdaQueryWrapper<Favorite>().eq(Favorite::getRepoPath, repoPath)) > 0;
        }
    }
}
