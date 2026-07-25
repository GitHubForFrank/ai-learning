package com.gitgui.domain.service;

import com.gitgui.core.constant.OperationType;
import com.gitgui.domain.model.OperationLog;

import java.util.List;

/**
 * 操作日志服务接口
 * <p>关联 BR：BR-35。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public interface OperationLogService {

    /**
     * 记录操作日志（BR-35）。
     *
     * @param repoPath    仓库路径
     * @param operation   操作类型
     * @param command     命令
     * @param success     是否成功
     * @param durationMs  耗时
     * @param errorMessage 错误信息
     * @param taskId      关联任务 ID
     */
    void record(String repoPath, OperationType operation, String command,
                boolean success, long durationMs, String errorMessage, String taskId);

    /**
     * 查询操作日志。
     *
     * @param repoPath 仓库路径（null 表示全部）
     * @return 操作日志列表
     */
    List<OperationLog> query(String repoPath);
}
