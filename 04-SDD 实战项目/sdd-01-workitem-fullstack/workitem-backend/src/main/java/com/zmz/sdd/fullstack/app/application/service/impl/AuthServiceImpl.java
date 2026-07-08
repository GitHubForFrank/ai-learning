package com.zmz.sdd.fullstack.app.application.service.impl;

import com.zmz.sdd.fullstack.app.application.service.AuthService;
import com.zmz.sdd.fullstack.app.application.service.LoginLogService;
import com.zmz.sdd.fullstack.app.domain.model.AppUser;
import com.zmz.sdd.fullstack.app.domain.model.LoginType;
import com.zmz.sdd.fullstack.app.domain.model.UserStatus;
import com.zmz.sdd.fullstack.app.domain.repository.AppUserRepository;
import com.zmz.sdd.fullstack.core.common.BizException;
import com.zmz.sdd.fullstack.core.common.ErrorCode;
import com.zmz.sdd.fullstack.core.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * [SDD-TASK: Task001]
 * [SDD-SPEC: 02-功能规范.md §3.6 + §4 BR-09 ~ BR-14]
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AppUserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginLogService loginLogService;

    @Value("${auth.lockout.max-failures}")
    private int maxFailures;

    @Value("${auth.lockout.duration-minutes}")
    private int lockMinutes;

    @Override
    @Transactional
    public LoginResult login(String username, String password, String ip, String userAgent, String traceparent) {
        AppUser user = userRepo.findByUsername(username).orElse(null);

        // 用户名不存在:写 FAIL_INVALID,统一 3001(防用户名枚举)
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            loginLogService.record(null, username, LoginType.FAIL_INVALID, ip, userAgent, traceparent);
            throw new BizException(ErrorCode.AUTH_INVALID_CREDENTIAL,
                    ErrorCode.AUTH_INVALID_CREDENTIAL.getDefaultMessage());
        }

        // [SDD-SPEC: §4 BR-11] 锁定中,直接拒绝,不进入密码校验
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            loginLogService.record(user.getId(), username, LoginType.FAIL_LOCKED, ip, userAgent, traceparent);
            throw new BizException(ErrorCode.AUTH_LOCKED,
                    "Account locked. Try again after " + user.getLockedUntil());
        }

        // 密码校验
        boolean matches = passwordEncoder.matches(password, user.getPasswordHash());
        if (!matches) {
            // [SDD-SPEC: §4 BR-10] 计数 + 满 3 次锁 15 分钟
            int newCount = user.getFailedLoginCount() + 1;
            if (newCount >= maxFailures) {
                userRepo.lockUntil(user.getId(), LocalDateTime.now().plusMinutes(lockMinutes));
                log.info("user '{}' locked for {} minutes due to {} failures", username, lockMinutes, newCount);
            } else {
                userRepo.incrementFailedCount(user.getId());
            }
            loginLogService.record(user.getId(), username, LoginType.FAIL_INVALID, ip, userAgent, traceparent);
            throw new BizException(ErrorCode.AUTH_INVALID_CREDENTIAL,
                    ErrorCode.AUTH_INVALID_CREDENTIAL.getDefaultMessage());
        }

        // 登录成功:[SDD-SPEC: §4 BR-12] 重置失败计数 + 解锁
        userRepo.resetFailedCountAndUnlock(user.getId());
        loginLogService.record(user.getId(), username, LoginType.SUCCESS, ip, userAgent, traceparent);

        String token = jwtService.issue(user.getId(), user.getUsername());
        return new LoginResult(token, jwtService.getExpiresSeconds(), user.getUsername());
    }

    @Override
    public AppUser getById(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.AUTH_REQUIRED, "User not found"));
    }
}
