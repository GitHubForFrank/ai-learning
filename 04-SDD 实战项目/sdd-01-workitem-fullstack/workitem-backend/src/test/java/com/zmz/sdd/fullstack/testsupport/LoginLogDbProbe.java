package com.zmz.sdd.fullstack.testsupport;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * [SDD-TASK: Task001]
 * [SDD-SPEC: conventions/regression-conventions.md §3 + BR-14 物理验证]
 */
@Component
@RequiredArgsConstructor
public class LoginLogDbProbe {

    private final JdbcTemplate jdbc;

    public long countByUsername(String username) {
        Long c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM login_log WHERE username_attempted = ?", Long.class, username);
        return c == null ? 0 : c;
    }

    public List<Map<String, Object>> findAllByUsername(String username) {
        return jdbc.queryForList(
                "SELECT * FROM login_log WHERE username_attempted = ? ORDER BY id ASC", username);
    }
}
