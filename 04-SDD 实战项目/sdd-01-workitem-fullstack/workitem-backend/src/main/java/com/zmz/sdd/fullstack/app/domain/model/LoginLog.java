package com.zmz.sdd.fullstack.app.domain.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** [SDD-SPEC: 02-功能规范.md §2.6] */
@Data
@Builder
public class LoginLog {
    private Long id;
    private Long appUserId;
    private String usernameAttempted;
    private LoginType loginType;
    private String loginIp;
    private String userAgent;
    private String traceparent;
    private LocalDateTime createdAt;
}
