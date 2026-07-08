package com.zmz.sdd.fullstack.app.application.service;

import com.zmz.sdd.fullstack.app.domain.model.AppUser;

public interface AuthService {

    /** [SDD-SPEC: 02-功能规范.md §3.6 + BR-09~12, 14] */
    LoginResult login(String username, String password, String ip, String userAgent, String traceparent);

    AppUser getById(Long id);

    record LoginResult(String token, long expiresInSeconds, String username) {}
}
