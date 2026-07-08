package com.zmz.sdd.fullstack.testsupport;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * [SDD-TASK: Task001]
 * [SDD-SPEC: conventions/regression-conventions.md §3 + 04-验收标准.md §4.5.2]
 *
 * 测试专用 DB Probe,绕开 MP 逻辑删除拦截器,直接用 JdbcTemplate 看物理状态。
 * 仅 src/test/ 可见,禁止 src/main/ 引用。
 */
@Component
@RequiredArgsConstructor
public class WorkitemDbProbe {

    private final JdbcTemplate jdbc;

    /** 物理查单行(不论 deleted) */
    public Map<String, Object> findPhysicalById(Long id) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM workitem WHERE id = ?", id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 按 deleted 值计数 */
    public long countPhysicalByDeleted(int deleted) {
        Long c = jdbc.queryForObject("SELECT COUNT(*) FROM workitem WHERE deleted = ?", Long.class, deleted);
        return c == null ? 0 : c;
    }

    /** 全表计数(不论 deleted) */
    public long countPhysicalAll() {
        Long c = jdbc.queryForObject("SELECT COUNT(*) FROM workitem", Long.class);
        return c == null ? 0 : c;
    }

    /** 是否物理存在(不论 deleted) */
    public boolean existsPhysicalById(Long id) {
        Long c = jdbc.queryForObject("SELECT COUNT(*) FROM workitem WHERE id = ?", Long.class, id);
        return c != null && c > 0;
    }
}
