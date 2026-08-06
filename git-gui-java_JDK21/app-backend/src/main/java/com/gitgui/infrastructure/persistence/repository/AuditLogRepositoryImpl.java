package com.gitgui.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gitgui.core.util.IdUtil;
import com.gitgui.domain.model.AuditLog;
import com.gitgui.domain.repository.AuditLogRepository;
import com.gitgui.infrastructure.persistence.mapper.AuditLogMapper;
import com.gitgui.infrastructure.persistence.mybatis.MyBatisSqlSessionManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 红线审计日志仓储 MyBatis-Plus 实现
 * <p>遵循 BR-31（追加写入不可删除）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
@Singleton
public class AuditLogRepositoryImpl implements AuditLogRepository {

    private static final Logger log = LoggerFactory.getLogger(AuditLogRepositoryImpl.class);

    private final MyBatisSqlSessionManager sessionManager;

    @Inject
    public AuditLogRepositoryImpl(MyBatisSqlSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void save(AuditLog auditLog) {
        if (auditLog.getId() == null) {
            auditLog.setId(IdUtil.newId());
        }
        auditLog.setCreatedAt(LocalDateTime.now());
        try (var session = sessionManager.openSession()) {
            AuditLogMapper mapper = session.getMapper(AuditLogMapper.class);
            // 追加写入，不可修改/删除（BR-31）
            mapper.insert(auditLog);
        }
    }

    @Override
    public List<AuditLog> findAll() {
        try (var session = sessionManager.openSession()) {
            AuditLogMapper mapper = session.getMapper(AuditLogMapper.class);
            return mapper.selectList(new LambdaQueryWrapper<AuditLog>().orderByDesc(AuditLog::getCreatedAt));
        }
    }

    @Override
    public List<AuditLog> findByRepoPath(String repoPath) {
        try (var session = sessionManager.openSession()) {
            AuditLogMapper mapper = session.getMapper(AuditLogMapper.class);
            return mapper.selectList(new LambdaQueryWrapper<AuditLog>().eq(AuditLog::getRepoPath, repoPath)
                                                                       .orderByDesc(AuditLog::getCreatedAt));
        }
    }

    @Override
    public List<AuditLog> findByRuleCode(String ruleCode) {
        try (var session = sessionManager.openSession()) {
            AuditLogMapper mapper = session.getMapper(AuditLogMapper.class);
            return mapper.selectList(new LambdaQueryWrapper<AuditLog>().eq(AuditLog::getRuleCode, ruleCode)
                                                                       .orderByDesc(AuditLog::getCreatedAt));
        }
    }
}
