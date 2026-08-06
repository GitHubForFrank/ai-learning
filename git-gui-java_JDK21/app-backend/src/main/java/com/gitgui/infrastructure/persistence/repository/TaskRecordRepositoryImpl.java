package com.gitgui.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gitgui.core.constant.TaskStatus;
import com.gitgui.core.util.IdUtil;
import com.gitgui.domain.model.TaskRecord;
import com.gitgui.domain.repository.TaskRecordRepository;
import com.gitgui.infrastructure.persistence.mapper.TaskRecordMapper;
import com.gitgui.infrastructure.persistence.mybatis.MyBatisSqlSessionManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 异步任务记录仓储 MyBatis-Plus 实现
 *
 * @author FrankKang
 * @since 2026-05-27
 */
@Singleton
public class TaskRecordRepositoryImpl implements TaskRecordRepository {

    private static final Logger log = LoggerFactory.getLogger(TaskRecordRepositoryImpl.class);

    private final MyBatisSqlSessionManager sessionManager;

    @Inject
    public TaskRecordRepositoryImpl(MyBatisSqlSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void save(TaskRecord record) {
        if (record.getId() == null) {
            record.setId(IdUtil.newId());
        }
        record.setCreatedAt(System.currentTimeMillis());
        try (var session = sessionManager.openSession()) {
            TaskRecordMapper mapper = session.getMapper(TaskRecordMapper.class);
            mapper.insert(record);
        }
    }

    @Override
    public void update(TaskRecord record) {
        try (var session = sessionManager.openSession()) {
            TaskRecordMapper mapper = session.getMapper(TaskRecordMapper.class);
            mapper.updateById(record);
        } catch (Exception e) {
            log.error("更新任务记录失败: id={}", record.getId(), e);
        }
    }

    @Override
    public TaskRecord findById(String id) {
        try (var session = sessionManager.openSession()) {
            TaskRecordMapper mapper = session.getMapper(TaskRecordMapper.class);
            return mapper.selectById(id);
        }
    }

    @Override
    public List<TaskRecord> findActiveByRepoPath(String repoPath) {
        try (var session = sessionManager.openSession()) {
            TaskRecordMapper mapper = session.getMapper(TaskRecordMapper.class);
            return mapper.selectList(new LambdaQueryWrapper<TaskRecord>().eq(TaskRecord::getRepoPath, repoPath)
                                                                         .in(TaskRecord::getStatus, TaskStatus.PENDING, TaskStatus.RUNNING)
                                                                         .orderByDesc(TaskRecord::getCreatedAt));
        }
    }

    @Override
    public List<TaskRecord> findHistoryByRepoPath(String repoPath) {
        try (var session = sessionManager.openSession()) {
            TaskRecordMapper mapper = session.getMapper(TaskRecordMapper.class);
            return mapper.selectList(new LambdaQueryWrapper<TaskRecord>().eq(TaskRecord::getRepoPath, repoPath)
                                                                         .orderByDesc(TaskRecord::getCreatedAt));
        }
    }

    @Override
    public List<TaskRecord> findByStatus(TaskStatus status) {
        try (var session = sessionManager.openSession()) {
            TaskRecordMapper mapper = session.getMapper(TaskRecordMapper.class);
            return mapper.selectList(new LambdaQueryWrapper<TaskRecord>().eq(TaskRecord::getStatus, status)
                                                                         .orderByDesc(TaskRecord::getCreatedAt));
        }
    }
}
