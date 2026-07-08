package com.zmz.sdd.fullstack.testsupport;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * [SDD-TASK: Task001]
 * [SDD-SPEC: conventions/regression-conventions.md §3]
 */
@Component
@RequiredArgsConstructor
public class AppUserDbProbe {

    private final JdbcTemplate jdbc;

    public Map<String, Object> findPhysicalByUsername(String username) {
        List<Map<String, Object>> rows =
                jdbc.queryForList("SELECT * FROM app_user WHERE username = ?", username);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Integer getFailedLoginCount(String username) {
        return jdbc.queryForObject(
                "SELECT failed_login_count FROM app_user WHERE username = ?", Integer.class, username);
    }
}
