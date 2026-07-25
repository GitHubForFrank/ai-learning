package com.gitgui.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gitgui.core.util.IdUtil;
import com.gitgui.domain.model.RepositoryMeta;
import com.gitgui.domain.repository.RepositoryMetaRepository;
import com.gitgui.infrastructure.persistence.mapper.RepositoryMetaMapper;
import com.gitgui.infrastructure.persistence.mybatis.MyBatisSqlSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 仓库元信息仓储 MyBatis-Plus 实现
 *
 * @author FrankKang
 * @since 2026-05-27
 */
@Singleton
public class RepositoryMetaRepositoryImpl implements RepositoryMetaRepository {

    private static final Logger log = LoggerFactory.getLogger(RepositoryMetaRepositoryImpl.class);

    private final MyBatisSqlSessionManager sessionManager;

    @Inject
    public RepositoryMetaRepositoryImpl(MyBatisSqlSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void save(RepositoryMeta meta) {
        if (meta.getId() == null) {
            meta.setId(IdUtil.newId());
        }
        LocalDateTime now = LocalDateTime.now();
        meta.setLastSyncedAt(now);
        if (meta.getCreatedAt() == null) {
            meta.setCreatedAt(now);
        }
        meta.setUpdatedAt(now);
        try (var session = sessionManager.openSession()) {
            RepositoryMetaMapper mapper = session.getMapper(RepositoryMetaMapper.class);
            // INSERT OR REPLACE 语义
            RepositoryMeta existing = findByRepoPath(meta.getRepoPath());
            if (existing != null) {
                meta.setId(existing.getId());
                mapper.updateById(meta);
            } else {
                mapper.insert(meta);
            }
        }
    }

    @Override
    public RepositoryMeta findByRepoPath(String repoPath) {
        try (var session = sessionManager.openSession()) {
            RepositoryMetaMapper mapper = session.getMapper(RepositoryMetaMapper.class);
            return mapper.selectOne(
                    new LambdaQueryWrapper<RepositoryMeta>()
                            .eq(RepositoryMeta::getRepoPath, repoPath)
            );
        }
    }

    @Override
    public List<RepositoryMeta> findAll() {
        try (var session = sessionManager.openSession()) {
            RepositoryMetaMapper mapper = session.getMapper(RepositoryMetaMapper.class);
            return mapper.selectList(
                    new LambdaQueryWrapper<RepositoryMeta>()
                            .orderByDesc(RepositoryMeta::getLastSyncedAt)
            );
        }
    }

    @Override
    public void deleteByRepoPath(String repoPath) {
        try (var session = sessionManager.openSession()) {
            RepositoryMetaMapper mapper = session.getMapper(RepositoryMetaMapper.class);
            mapper.delete(
                    new LambdaQueryWrapper<RepositoryMeta>()
                            .eq(RepositoryMeta::getRepoPath, repoPath)
            );
        }
    }
}
