package com.zmz.sdd.fullstack.app.application.service;

import com.zmz.sdd.fullstack.app.domain.model.LoginType;

public interface LoginLogService {
    /** [SDD-SPEC: 02-功能规范.md §4 BR-14] */
    void record(Long appUserId, String usernameAttempted, LoginType type,
                String ip, String userAgent, String traceparent);
}
