package com.zmz.sdd.fullstack.app.infrastructure.repository.translator;

import com.zmz.sdd.fullstack.app.domain.model.LoginLog;
import com.zmz.sdd.fullstack.app.domain.model.LoginType;
import com.zmz.sdd.fullstack.app.infrastructure.dao.entity.LoginLogEntity;
import org.springframework.stereotype.Component;

@Component
public class LoginLogTranslator {
    public LoginLogEntity toEntity(LoginLog l) {
        LoginLogEntity e = new LoginLogEntity();
        e.setAppUserId(l.getAppUserId());
        e.setUsernameAttempted(l.getUsernameAttempted());
        e.setLoginType(l.getLoginType() == null ? null : l.getLoginType().name());
        e.setLoginIp(l.getLoginIp());
        e.setUserAgent(l.getUserAgent());
        e.setTraceparent(l.getTraceparent());
        return e;
    }

    public LoginLog toModel(LoginLogEntity e) {
        if (e == null) return null;
        return LoginLog.builder()
                .id(e.getId())
                .appUserId(e.getAppUserId())
                .usernameAttempted(e.getUsernameAttempted())
                .loginType(e.getLoginType() == null ? null : LoginType.valueOf(e.getLoginType()))
                .loginIp(e.getLoginIp())
                .userAgent(e.getUserAgent())
                .traceparent(e.getTraceparent())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
