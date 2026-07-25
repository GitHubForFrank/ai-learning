package com.gitgui.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gitgui.core.util.IdUtil;
import com.gitgui.domain.model.RecentRepo;
import com.gitgui.domain.repository.RecentRepoRepository;
import com.gitgui.infrastructure.persistence.mapper.RecentRepoMapper;
import com.gitgui.infrastructure.persistence.mybatis.MyBatisSqlSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 最近仓库仓储 MyBatis-Plus 实现
 * <p>遵循 BR-05（倒序与淘汰）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
@Singleton
public class RecentRepoRepositoryImpl implements RecentRepoRepository {

    private static final Logger log = LoggerFactory.getLogger(RecentRepoRepositoryImpl.class);

    private final MyBatisSqlSessionManager sessionManager;

    @Inject
    public RecentRepoRepositoryImpl(MyBatisSqlSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void save(RecentRepo recentRepo) {
        if (recentRepo.getId() == null) {
            recentRepo.setId(IdUtil.newId());
        }
        LocalDateTime now = LocalDateTime.now();
        if (recentRepo.getCreatedAt() == null) {
            recentRepo.setCreatedAt(now);
        }
        recentRepo.setUpdatedAt(now);
        try (var session = sessionManager.openSession()) {
            RecentRepoMapper mapper = session.getMapper(RecentRepoMapper.class);
            // INSERT OR REPLACE 语义
            RecentRepo existing = findByRepoPath(recentRepo.getRepoPath());
            if (existing != null) {
                recentRepo.setId(existing.getId());
                mapper.updateById(recentRepo);
            } else {
                mapper.insert(recentRepo);
            }
        }
    }

    @Override
    public List<RecentRepo> findAll() {
        try (var session = sessionManager.openSession()) {
            RecentRepoMapper mapper = session.getMapper(RecentRepoMapper.class);
            return mapper.selectList(
                    new LambdaQueryWrapper<RecentRepo>()
                            .orderByDesc(RecentRepo::getLastOpenedAt)
            );
        }
    }

    @Override
    public RecentRepo findByRepoPath(String repoPath) {
        try (var session = sessionManager.openSession()) {
            RecentRepoMapper mapper = session.getMapper(RecentRepoMapper.class);
            return mapper.selectOne(
                    new LambdaQueryWrapper<RecentRepo>()
                            .eq(RecentRepo::getRepoPath, repoPath)
            );
        }
    }

    @Override
    public void deleteById(String id) {
        try (var session = sessionManager.openSession()) {
            RecentRepoMapper mapper = session.getMapper(RecentRepoMapper.class);
            mapper.deleteById(id);
        }
    }

    @Override
    public void deleteAll() {
        try (var session = sessionManager.openSession()) {
            RecentRepoMapper mapper = session.getMapper(RecentRepoMapper.class);
            mapper.delete(new LambdaQueryWrapper<>());
        }
    }

    @Override
    public long count() {
        try (var session = sessionManager.openSession()) {
            RecentRepoMapper mapper = session.getMapper(RecentRepoMapper.class);
            return mapper.selectCount(new LambdaQueryWrapper<>());
        }
    }

    @Override
    public void deleteOldest(int keep) {
        // 删除超出 keep 数量的最旧记录（BR-05 淘汰）
        try (var session = sessionManager.openSession()) {
            RecentRepoMapper mapper = session.getMapper(RecentRepoMapper.class);
            List<RecentRepo> all = mapper.selectList(
                    new LambdaQueryWrapper<RecentRepo>()
                            .orderByDesc(RecentRepo::getLastOpenedAt)
            );
            if (all.size() > keep) {
                for (int i = keep; i < all.size(); i++) {
                    mapper.deleteById(all.get(i).getId());
                }
            }
        }
    }
}
