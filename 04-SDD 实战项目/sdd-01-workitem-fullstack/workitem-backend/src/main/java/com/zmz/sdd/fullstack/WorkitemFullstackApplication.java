package com.zmz.sdd.fullstack;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * [SDD-TASK: Task001]
 * [SDD-SPEC: 03-技术方案.md §4.3] 启动类
 */
@SpringBootApplication
@MapperScan("com.zmz.sdd.fullstack.app.infrastructure.dao.mapper")
public class WorkitemFullstackApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkitemFullstackApplication.class, args);
    }
}
