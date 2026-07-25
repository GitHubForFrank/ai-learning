package com.gitgui.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gitgui.core.util.IdUtil;
import com.gitgui.domain.model.RepoScanRoot;
import com.gitgui.domain.repository.RepoScanRootRepository;
import com.gitgui.infrastructure.persistence.mapper.RepoScanRootMapper;
import com.gitgui.infrastructure.persistence.mybatis.MyBatisSqlSessionManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 扫描根目录仓储 MyBatis-Plus 实现。
 *
 * @author FrankKang
 * @since 2026-07-24
 */
@Singleton
public class RepoScanRootRepositoryImpl implements RepoScanRootRepository {

    private static final Logger log = LoggerFactory.getLogger(RepoScanRootRepositoryImpl.class);

    private final MyBatisSqlSessionManager sessionManager;

    @Inject
    public RepoScanRootRepositoryImpl(MyBatisSqlSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void save(RepoScanRoot root) {
        if (root.getId() == null) {
            root.setId(IdUtil.newId());
        }
        LocalDateTime now = LocalDateTime.now();
        if (root.getCreatedAt() == null) {
            root.setCreatedAt(now);
        }
        root.setUpdatedAt(now);
        try (var session = sessionManager.openSession()) {
            RepoScanRootMapper mapper = session.getMapper(RepoScanRootMapper.class);
            // INSERT OR REPLACE 语义：同 rootPath 已存在则更新并保留原 ID
            RepoScanRoot existing = findByRootPath(root.getRootPath());
            if (existing != null) {
                root.setId(existing.getId());
                root.setCreatedAt(existing.getCreatedAt());
                mapper.updateById(root);
            } else {
                mapper.insert(root);
            }
        }
    }

    @Override
    public RepoScanRoot findById(String id) {
        try (var session = sessionManager.openSession()) {
            RepoScanRootMapper mapper = session.getMapper(RepoScanRootMapper.class);
            return mapper.selectById(id);
        }
    }

    @Override
    public RepoScanRoot findByRootPath(String rootPath) {
        try (var session = sessionManager.openSession()) {
            RepoScanRootMapper mapper = session.getMapper(RepoScanRootMapper.class);
            return mapper.selectOne(
                    new LambdaQueryWrapper<RepoScanRoot>()
                            .eq(RepoScanRoot::getRootPath, rootPath)
            );
        }
    }

    @Override
    public List<RepoScanRoot> findEnabled() {
        try (var session = sessionManager.openSession()) {
            RepoScanRootMapper mapper = session.getMapper(RepoScanRootMapper.class);
            return mapper.selectList(
                    new LambdaQueryWrapper<RepoScanRoot>()
                            .eq(RepoScanRoot::isEnabled, true)
                            .orderByAsc(RepoScanRoot::getSortOrder)
                            .orderByDesc(RepoScanRoot::getLastScannedAt)
            );
        }
    }

    @Override
    public List<RepoScanRoot> findAll() {
        try (var session = sessionManager.openSession()) {
            RepoScanRootMapper mapper = session.getMapper(RepoScanRootMapper.class);
            return mapper.selectList(
                    new LambdaQueryWrapper<RepoScanRoot>()
                            .orderByAsc(RepoScanRoot::getSortOrder)
                            .orderByDesc(RepoScanRoot::getLastScannedAt)
            );
        }
    }

    @Override
    public void deleteById(String id) {
        try (var session = sessionManager.openSession()) {
            RepoScanRootMapper mapper = session.getMapper(RepoScanRootMapper.class);
            mapper.deleteById(id);
        }
    }

    @Override
    public void deleteByRootPath(String rootPath) {
        try (var session = sessionManager.openSession()) {
            RepoScanRootMapper mapper = session.getMapper(RepoScanRootMapper.class);
            mapper.delete(
                    new LambdaQueryWrapper<RepoScanRoot>()
                            .eq(RepoScanRoot::getRootPath, rootPath)
            );
        }
    }

    @Override
    public boolean existsByRootPath(String rootPath) {
        try (var session = sessionManager.openSession()) {
            RepoScanRootMapper mapper = session.getMapper(RepoScanRootMapper.class);
            return mapper.selectCount(
                    new LambdaQueryWrapper<RepoScanRoot>()
                            .eq(RepoScanRoot::getRootPath, rootPath)
            ) > 0;
        }
    }
}