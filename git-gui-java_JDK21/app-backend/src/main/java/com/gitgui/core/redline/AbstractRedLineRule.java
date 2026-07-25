package com.gitgui.core.redline;

import com.gitgui.domain.redline.RedLineContext;
import com.gitgui.domain.redline.RedLineResult;
import com.gitgui.domain.redline.RedLineRule;
import com.gitgui.domain.service.SettingsService;
import com.gitgui.core.util.WildcardUtil;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 红线规则抽象基类
 * <p>提供规则代码与命中构造的通用方法，子类只需实现 {@link #doCheck}。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public abstract class AbstractRedLineRule implements RedLineRule {

    /** 设置服务（用于读取保护分支、远程白名单、敏感文件规则等配置） */
    protected final SettingsService settingsService;

    protected AbstractRedLineRule(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Override
    public RedLineResult check(RedLineContext ctx) {
        // BR-30：红线总开关关闭时，所有 BLOCK 降级为 CONFIRM
        RedLineResult result = doCheck(ctx);
        if (result.getAction() == RedLineResult.Action.BLOCK
                && settingsService != null
                && !settingsService.isRedLineEnabled()) {
            return RedLineResult.confirm(result.getRuleCode(), result.getMessage());
        }
        return result;
    }

    /**
     * 子类实现的命中判定逻辑。
     *
     * @param ctx 上下文
     * @return 校验结果
     */
    protected abstract RedLineResult doCheck(RedLineContext ctx);

    /**
     * 判断分支是否匹配保护分支清单（支持通配符，BR-27）。
     *
     * @param branch             待判断的分支名
     * @param protectedBranches  保护分支清单
     * @return true 表示命中
     */
    protected boolean matchesProtected(String branch, List<String> protectedBranches) {
        if (branch == null || protectedBranches == null) {
            return false;
        }
        // 去除 refs/heads/ 前缀
        String shortName = branch.replace("refs/heads/", "");
        for (String pattern : protectedBranches) {
            if (WildcardUtil.match(pattern, shortName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断文件路径是否匹配敏感文件规则（BR-32）。
     *
     * @param path    文件路径
     * @param rules   敏感文件规则列表
     * @return 命中的规则描述，未命中返回 null
     */
    protected String matchSensitiveFile(String path, List<com.gitgui.domain.model.SensitiveFileRule> rules) {
        if (path == null || rules == null) {
            return null;
        }
        for (com.gitgui.domain.model.SensitiveFileRule rule : rules) {
            try {
                if (Pattern.compile(rule.getPattern()).matcher(path).find()) {
                    return rule.getDescription();
                }
            } catch (RuntimeException e) {
                // 正则非法跳过
            }
        }
        return null;
    }
}
