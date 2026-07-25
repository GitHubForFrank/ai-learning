package com.gitgui.domain.service;

import com.gitgui.domain.model.AuditLog;
import com.gitgui.domain.redline.RedLineContext;
import com.gitgui.domain.redline.RedLineResult;

import java.util.List;

/**
 * 命令红线服务接口（闭环核心）
 * <p>关联 BR：BR-26~BR-32。</p>
 * <p>校验返回 PASS / CONFIRM / BLOCK + ruleCode + message；PASS 放行，CONFIRM 由 UI 弹窗，
 * BLOCK 直接拒绝并提示安全等价命令。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public interface CommandRedLineService {

    /**
     * 校验红线，返回三态结果。
     * <p>任一规则返回 BLOCK 则整体 BLOCK；任一返回 CONFIRM 则收集确认项；全部 PASS 则放行。</p>
     *
     * @param ctx 红线上下文
     * @return 校验结果
     */
    RedLineResult check(RedLineContext ctx);

    /**
     * 持久化审计日志（BR-31）。
     *
     * @param auditLog 审计日志
     */
    void recordAudit(AuditLog auditLog);

    /**
     * 查询审计日志列表。
     *
     * @param repoPath 仓库路径（null 表示全部）
     * @return 审计日志列表
     */
    List<AuditLog> queryAudit(String repoPath);
}
