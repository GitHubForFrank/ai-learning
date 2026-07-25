package com.gitgui.domain.repository;

import com.gitgui.core.constant.OperationType;
import com.gitgui.domain.model.OperationLog;

import java.util.List;

/**
 * 操作日志仓储接口
 * <p>遵循 BR-35（任务结果记录，追加写入）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public interface OperationLogRepository {

    /**
     * 追加写入操作日志。
     *
     * @param log 操作日志
     */
    void save(OperationLog log);

    /**
     * 列出全部操作日志（按 createdAt 倒序）。
     *
     * @return 操作日志列表
     */
    List<OperationLog> findAll();

    /**
     * 按仓库路径查询操作日志。
     *
     * @param repoPath 仓库路径
     * @return 操作日志列表
     */
    List<OperationLog> findByRepoPath(String repoPath);

    /**
     * 按操作类型查询。
     *
     * @param operation 操作类型
     * @return 操作日志列表
     */
    List<OperationLog> findByOperation(OperationType operation);
}
