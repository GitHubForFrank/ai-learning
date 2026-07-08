package com.zmz.sdd.fullstack.app.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zmz.sdd.fullstack.app.api.controller.dto.LoginReq;
import com.zmz.sdd.fullstack.app.api.controller.dto.WorkitemCreateReq;
import com.zmz.sdd.fullstack.testsupport.WorkitemDbProbe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * [SDD-TASK: Task001]
 * [SDD-SPEC: 04-验收标准.md §4.2 (TC-IT-*) + §4.5.2 (TC-R-DB-*)]
 *
 * Testcontainers MySQL 8;Flyway 自动从 ../workitem-db/migration 加载脚本。
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WorkitemControllerIT {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("zmz_sdd_demo")
            .withUsername("root")
            .withPassword("test");

    @DynamicPropertySource
    static void overrideDb(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",      mysql::getJdbcUrl);
        r.add("spring.datasource.username", mysql::getUsername);
        r.add("spring.datasource.password", mysql::getPassword);
        r.add("auth.lockout.duration-minutes", () -> "0"); // E2E 快进
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired WorkitemDbProbe workitemDbProbe;

    private String jwt;

    @BeforeAll
    void loginAsAdmin() throws Exception {
        LoginReq req = new LoginReq();
        req.setUsername("admin");
        req.setPassword("12346#@&");
        MvcResult result = mvc.perform(post("/api/login")
                        .header("traceparent", "00-0123456789abcdef0123456789abcdef-0123456789abcdef-01")
                        .contentType("application/json")
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        // 简化解析:抽 token 字段
        int i = body.indexOf("\"token\":\"") + 9;
        int j = body.indexOf("\"", i);
        jwt = body.substring(i, j);
        assertThat(jwt).isNotBlank();
    }

    @Test
    @DisplayName("TC-IT-01 + TC-R-DB-01: post create returns 200, DB physical row deleted=0/status=TODO")
    void post_create_200() throws Exception {
        WorkitemCreateReq req = new WorkitemCreateReq();
        req.setTitle("integration test workitem");

        MvcResult result = mvc.perform(post("/api/workitems")
                        .header("Authorization", "Bearer " + jwt)
                        .header("traceparent", "00-0123456789abcdef0123456789abcdef-0123456789abcdef-01")
                        .contentType("application/json")
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("TODO"))
                .andExpect(jsonPath("$.data.priority").value("MEDIUM"))
                .andReturn();

        // 后台 DB Probe 物理断言(BR-01 + BR-02 + BR-03 物理真相)
        String body = result.getResponse().getContentAsString();
        int i = body.indexOf("\"id\":") + 5;
        int j = body.indexOf(",", i);
        Long id = Long.parseLong(body.substring(i, j).trim());
        var row = workitemDbProbe.findPhysicalById(id);
        assertThat(row).isNotNull();
        assertThat(row.get("deleted")).isEqualTo(0);
        assertThat(row.get("status")).isEqualTo("TODO");
    }

    @Test
    @DisplayName("TC-IT-17 BR-15: missing Authorization header should return 3003")
    void get_workitems_withoutAuth_returns3003() throws Exception {
        mvc.perform(get("/api/workitems")
                        .header("traceparent", "00-0123456789abcdef0123456789abcdef-0123456789abcdef-01"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(3003));
    }
}
