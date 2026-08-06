package com.gitgui.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gitgui.core.constant.OperationType;
import com.gitgui.core.util.IdUtil;
import com.gitgui.domain.model.OperationLog;
import com.gitgui.domain.repository.OperationLogRepository;
import com.gitgui.infrastructure.persistence.mapper.OperationLogMapper;
import com.gitgui.infrastructure.persistence.mybatis.MyBatisSqlSessionManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 操作日志仓储 MyBatis-Plus 实现
 *
 * @author FrankKang
 * @since 2026-05-27
 */
@Singleton
public class OperationLogRepositoryImpl implements OperationLogRepository {

    private static final Logger log = LoggerFactory.getLogger(OperationLogRepositoryImpl.class);

    private final MyBatisSqlSessionManager sessionManager;

    @Inject
    public OperationLogRepositoryImpl(MyBatisSqlSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void save(OperationLog logEntry) {
        if (logEntry.getId() == null) {
            logEntry.setId(IdUtil.newId());
        }
        logEntry.setCreatedAt(LocalDateTime.now());
        try (var session = sessionManager.openSession()) {
            OperationLogMapper mapper = session.getMapper(OperationLogMapper.class);
            mapper.insert(logEntry);
        } catch (Exception e) {
            log.error("保存操作日志失败", e);
        }
    }

    @Override
    public List<OperationLog> findAll() {
        try (var session = sessionManager.openSession()) {
            OperationLogMapper mapper = session.getMapper(OperationLogMapper.class);
            return mapper.selectList(new LambdaQueryWrapper<OperationLog>().orderByDesc(OperationLog::getCreatedAt));
        }
    }

    @Override
    public List<OperationLog> findByRepoPath(String repoPath) {
        try (var session = sessionManager.openSession()) {
            OperationLogMapper mapper = session.getMapper(OperationLogMapper.class);
            return mapper.selectList(new LambdaQueryWrapper<OperationLog>().eq(OperationLog::getRepoPath, repoPath)
                                                                           .orderByDesc(OperationLog::getCreatedAt));
        }
    }

    @Override
    public List<OperationLog> findByOperation(OperationType operation) {
        try (var session = sessionManager.openSession()) {
            OperationLogMapper mapper = session.getMapper(OperationLogMapper.class);
            return mapper.selectList(new LambdaQueryWrapper<OperationLog>().eq(OperationLog::getOperation, operation)
                                                                           .orderByDesc(OperationLog::getCreatedAt));
        }
    }
}
