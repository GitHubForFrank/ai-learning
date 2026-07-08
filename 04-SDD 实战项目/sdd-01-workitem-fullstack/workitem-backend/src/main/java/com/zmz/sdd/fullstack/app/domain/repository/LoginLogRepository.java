package com.zmz.sdd.fullstack.app.domain.repository;

import com.zmz.sdd.fullstack.app.domain.model.LoginLog;

/** [SDD-SPEC: 03-技术方案.md §4 + BR-14] */
public interface LoginLogRepository {
    void save(LoginLog log);
}
