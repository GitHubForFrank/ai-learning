package com.zmz.sdd.fullstack.app.domain.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * [SDD-SPEC: 02-功能规范.md §2.4]
 * 领域模型(passwordHash 不对外暴露,但 domain 层保留以供 AuthService 校验)
 */
@Data
@Builder
public class AppUser {
    private Long id;
    private String username;
    private String passwordHash;
    private UserStatus status;
    private int failedLoginCount;
    private LocalDateTime lockedUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
