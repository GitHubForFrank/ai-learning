package com.example.mcp.handler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.mcp.handler.system.SystemInfoHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * SystemInfoHandler 单元测试
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@SpringBootTest
class SystemInfoHandlerTest {

    @Autowired
    private SystemInfoHandler systemInfoHandler;

    @Test
    void testSystemInfo() {
        String result = systemInfoHandler.systemInfo();
        assertNotNull(result);
    }

    @Test
    void testSystemDiskInfo() {
        String result = systemInfoHandler.systemDiskInfo(null);
        assertNotNull(result);
    }

    @Test
    void testSystemEnvGet() {
        String result = systemInfoHandler.systemEnvGet("JAVA_HOME");
        assertNotNull(result);
    }

    @Test
    void testSystemProperties() {
        String result = systemInfoHandler.systemProperties("java.version");
        assertNotNull(result);
        assertTrue(result.contains("21"));
    }

    @Test
    void testSystemPortCheck() {
        String result = systemInfoHandler.systemPortCheck(9999, "localhost", 500);
        assertNotNull(result);
    }
}
