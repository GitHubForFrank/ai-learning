package com.gitgui.application.service;

import com.gitgui.core.constant.OperationType;
import com.gitgui.core.util.IdUtil;
import com.gitgui.domain.model.OperationLog;
import com.gitgui.domain.repository.OperationLogRepository;
import com.gitgui.domain.service.OperationLogService;
import com.google.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 操作日志服务实现
 * <p>关联 BR：BR-35。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class OperationLogServiceImpl implements OperationLogService {

    private static final Logger log = LoggerFactory.getLogger(OperationLogServiceImpl.class);

    private final OperationLogRepository operationLogRepository;

    @Inject
    public OperationLogServiceImpl(OperationLogRepository operationLogRepository) {
        this.operationLogRepository = operationLogRepository;
    }

    @Override
    public void record(String repoPath, OperationType operation, String command, boolean success, long durationMs, String errorMessage,
            String taskId) {
        // BR-35：任务结果记录（追加写入不可删除）
        OperationLog logEntry = OperationLog.builder()
                                            .id(IdUtil.newId())
                                            .repoPath(repoPath)
                                            .operation(operation)
                                            .command(command)
                                            .success(success)
                                            .durationMs(durationMs)
                                            .errorMessage(errorMessage == null ? "" : errorMessage)
                                            .taskId(taskId == null ? "" : taskId)
                                            .createdAt(LocalDateTime.now())
                                            .build();
        operationLogRepository.save(logEntry);
    }

    @Override
    public List<OperationLog> query(String repoPath) {
        if (repoPath == null) {
            return operationLogRepository.findAll();
        }
        return operationLogRepository.findByRepoPath(repoPath);
    }
}
