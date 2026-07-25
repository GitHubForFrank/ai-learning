package com.gitgui.application.service;

import com.gitgui.core.constant.RedLineCode;
import com.gitgui.core.exception.ErrorCode;
import com.gitgui.core.exception.GitGuiException;
import com.gitgui.domain.constant.SettingsCategory;
import com.gitgui.domain.model.AppSettings;
import com.gitgui.domain.model.AuditLog;
import com.gitgui.domain.model.SensitiveFileRule;
import com.gitgui.domain.repository.AppSettingsRepository;
import com.gitgui.domain.repository.AuditLogRepository;
import com.gitgui.domain.service.SettingsService;
import com.gitgui.core.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import com.google.inject.Inject;

/**
 * 设置服务实现
 * <p>关联 BR：BR-27、BR-28、BR-29、BR-30、BR-32、BR-37、BR-38、BR-39。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class SettingsServiceImpl implements SettingsService {

    private static final Logger log = LoggerFactory.getLogger(SettingsServiceImpl.class);

    private final AppSettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;

    @Inject
    public SettingsServiceImpl(AppSettingsRepository settingsRepository,
                               AuditLogRepository auditLogRepository) {
        this.settingsRepository = settingsRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public String get(String key) {
        return settingsRepository.getValue(key);
    }

    @Override
    public void set(String key, String value) {
        AppSettings existing = settingsRepository.findByKey(key);
        AppSettings settings = AppSettings.builder()
                .id(existing == null ? null : existing.getId())
                .key(key)
                .value(value)
                .category(existing == null ? SettingsCategory.UI : existing.getCategory())
                .description(existing == null ? "" : existing.getDescription())
                .build();
        settingsRepository.save(settings);
    }

    @Override
    public List<String> getProtectedBranches() {
        // BR-27：保护分支清单，默认含 main/master/develop
        String value = settingsRepository.getValue("red_line.protected_branches");
        if (value == null || value.isBlank()) {
            return Arrays.asList("main", "master", "develop");
        }
        String[] arr = JsonUtil.fromJson(value, String[].class);
        return arr == null ? Collections.emptyList() : Arrays.asList(arr);
    }

    @Override
    public List<String> getRemoteWhitelist() {
        // BR-28：远程白名单，空列表表示不限制
        String value = settingsRepository.getValue("red_line.remote_whitelist");
        if (value == null || value.isBlank()) {
            return Collections.emptyList();
        }
        String[] arr = JsonUtil.fromJson(value, String[].class);
        return arr == null ? Collections.emptyList() : Arrays.asList(arr);
    }

    @Override
    public List<SensitiveFileRule> getSensitiveFileRules() {
        // BR-32：敏感文件规则
        String value = settingsRepository.getValue("red_line.sensitive_file_rules");
        if (value == null || value.isBlank()) {
            return Collections.emptyList();
        }
        SensitiveFileRule[] arr = JsonUtil.fromJson(value, SensitiveFileRule[].class);
        return arr == null ? Collections.emptyList() : Arrays.asList(arr);
    }

    @Override
    public int getLargeFileThresholdMb() {
        // BR-29：超大文件阈值，默认 50MB
        String value = settingsRepository.getValue("red_line.large_file_threshold_mb");
        if (value == null || value.isBlank()) {
            return 50;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 50;
        }
    }

    @Override
    public boolean isRedLineEnabled() {
        // BR-30：红线总开关，默认开启
        String value = settingsRepository.getValue("red_line.enabled");
        return !"false".equalsIgnoreCase(value);
    }

    @Override
    public void setRedLineEnabled(boolean enabled) {
        // BR-30：切换操作本身写入 audit_log
        String oldValue = settingsRepository.getValue("red_line.enabled");
        set("red_line.enabled", String.valueOf(enabled));
        AuditLog auditLog = AuditLog.builder()
                .ruleCode(RedLineCode.RED_LINE_TOGGLE.name())
                .command("RED_LINE_TOGGLE")
                .repoPath("")
                .action("CONFIRM")
                .actionResult("CONFIRMED")
                .detail("切换红线总开关：" + oldValue + " → " + enabled)
                .build();
        auditLogRepository.save(auditLog);
        log.info("红线总开关切换：{} → {}", oldValue, enabled);
    }

    @Override
    public void setProtectedBranches(List<String> branches) {
        set("red_line.protected_branches", JsonUtil.toJson(branches));
    }

    @Override
    public void setRemoteWhitelist(List<String> whitelist) {
        set("red_line.remote_whitelist", JsonUtil.toJson(whitelist));
    }
}
