package com.example.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Frank Kang
 * @since 2023-09-01 09:05
 */
@Slf4j
@SpringBootApplication
public class McpServerDemoSseWebfluxApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerDemoSseWebfluxApplication.class, args);
        log.info("McpServerDemoSseWebfluxApplication.main.completed");
    }


}
