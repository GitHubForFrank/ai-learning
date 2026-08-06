package com.gitgui.application.redline;

import com.gitgui.domain.model.AuditLog;
import com.gitgui.domain.redline.RedLineContext;
import com.gitgui.domain.redline.RedLineResult;
import com.gitgui.domain.redline.RedLineRule;
import com.gitgui.domain.repository.AuditLogRepository;
import com.gitgui.domain.service.CommandRedLineService;
import com.gitgui.domain.service.SettingsService;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 命令红线服务实现（闭环核心）
 * <p>遍历 Multibindings 收集的 13 个 {@link RedLineRule}，汇总结果并持久化审计日志。</p>
 * <p>遵循 BR-26~BR-32 红线闭环约束。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class CommandRedLineServiceImpl implements CommandRedLineService {

    private static final Logger log = LoggerFactory.getLogger(CommandRedLineServiceImpl.class);

    /**
     * 13 个红线规则集合（由 Guice Multibindings 收集）
     */
    private final Set<RedLineRule> rules;
    /**
     * 审计日志仓储
     */
    private final AuditLogRepository auditLogRepository;
    /**
     * 设置服务（用于读取红线开关与配置）
     */
    private final SettingsService settingsService;

    @Inject
    public CommandRedLineServiceImpl(Set<RedLineRule> rules, AuditLogRepository auditLogRepository, SettingsService settingsService) {
        this.rules = rules;
        this.auditLogRepository = auditLogRepository;
        this.settingsService = settingsService;
    }

    @Override
    public RedLineResult check(RedLineContext ctx) {
        // 遍历所有规则，任一 BLOCK 则整体 BLOCK；任一 CONFIRM 则收集；全部 PASS 则放行
        List<RedLineResult> confirms = new ArrayList<>();
        for (RedLineRule rule : rules) {
            try {
                RedLineResult result = rule.check(ctx);
                if (result.getAction() == RedLineResult.Action.BLOCK) {
                    log.info("红线阻断：rule={}, repoPath={}", rule.ruleCode(), ctx.getRepoPath());
                    return result;
                }
                if (result.getAction() == RedLineResult.Action.CONFIRM) {
                    confirms.add(result);
                }
            } catch (Exception e) {
                // 规则执行异常不阻断主流程，记录日志
                log.error("红线规则执行异常：rule={}", rule.ruleCode(), e);
            }
        }
        // 收集到 CONFIRM 项则返回首个（UI 弹窗逐个确认）
        if (!confirms.isEmpty()) {
            return confirms.get(0);
        }
        return RedLineResult.pass();
    }

    @Override
    public void recordAudit(AuditLog auditLog) {
        // BR-31：追加写入不可删除
        auditLogRepository.save(auditLog);
        log.info("审计日志已记录：rule={}, result={}", auditLog.getRuleCode(), auditLog.getActionResult());
    }

    @Override
    public List<AuditLog> queryAudit(String repoPath) {
        if (repoPath == null) {
            return auditLogRepository.findAll();
        }
        return auditLogRepository.findByRepoPath(repoPath);
    }
}
