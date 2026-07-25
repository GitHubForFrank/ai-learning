package com.gitgui.domain.repository;

import com.gitgui.domain.model.AuditLog;

import java.util.List;

/**
 * 红线审计日志仓储接口
 * <p>遵循 BR-31（追加写入不可删除）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public interface AuditLogRepository {

    /**
     * 追加写入审计日志（BR-31，不可修改/删除）。
     *
     * @param auditLog 审计日志
     */
    void save(AuditLog auditLog);

    /**
     * 列出全部审计日志（按 createdAt 倒序）。
     *
     * @return 审计日志列表
     */
    List<AuditLog> findAll();

    /**
     * 按仓库路径查询审计日志。
     *
     * @param repoPath 仓库路径
     * @return 审计日志列表
     */
    List<AuditLog> findByRepoPath(String repoPath);

    /**
     * 按规则代码查询。
     *
     * @param ruleCode 规则代码
     * @return 审计日志列表
     */
    List<AuditLog> findByRuleCode(String ruleCode);
}
