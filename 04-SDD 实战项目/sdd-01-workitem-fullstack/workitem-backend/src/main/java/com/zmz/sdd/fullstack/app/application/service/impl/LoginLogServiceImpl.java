package com.zmz.sdd.fullstack.app.application.service.impl;

import com.zmz.sdd.fullstack.app.application.service.LoginLogService;
import com.zmz.sdd.fullstack.app.domain.model.LoginLog;
import com.zmz.sdd.fullstack.app.domain.model.LoginType;
import com.zmz.sdd.fullstack.app.domain.repository.LoginLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** [SDD-TASK: Task001][SDD-SPEC: 02-功能规范.md §4 BR-14] */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLogServiceImpl implements LoginLogService {

    private final LoginLogRepository repo;

    @Override
    public void record(Long appUserId, String usernameAttempted, LoginType type,
                       String ip, String userAgent, String traceparent) {
        try {
            LoginLog l = LoginLog.builder()
                    .appUserId(appUserId)
                    .usernameAttempted(usernameAttempted)
                    .loginType(type)
                    .loginIp(ip)
                    .userAgent(userAgent != null && userAgent.length() > 512
                            ? userAgent.substring(0, 512) : userAgent)
                    .traceparent(traceparent)
                    .build();
            repo.save(l);
        } catch (Exception e) {
            // 写日志失败不应阻塞登录主流程
            log.error("Failed to write login_log username={} type={}", usernameAttempted, type, e);
        }
    }
}
