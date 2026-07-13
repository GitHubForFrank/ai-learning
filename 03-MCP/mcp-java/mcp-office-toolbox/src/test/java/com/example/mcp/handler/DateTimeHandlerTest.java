package com.example.mcp.handler;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.example.mcp.handler.tool.DateTimeHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * DateTimeHandler 单元测试
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@SpringBootTest
class DateTimeHandlerTest {

    @Autowired
    private DateTimeHandler dateTimeHandler;

    @Test
    void testDateCalc() {
        String result = dateTimeHandler.dateCalc("2026-01-01", 30, 0, 0);
        assertNotNull(result);
    }

    @Test
    void testDateDiff() {
        String result = dateTimeHandler.dateDiff("2026-01-01", "2026-01-31", "days");
        assertNotNull(result);
    }

    @Test
    void testDateFormat() {
        String result = dateTimeHandler.dateFormat("2026-01-15 10:30:00", "datetime", "yyyy/MM/dd");
        assertNotNull(result);
    }

    @Test
    void testTimezoneConvert() {
        String result = dateTimeHandler.timezoneConvert("2026-01-15 10:00:00", null, "UTC");
        assertNotNull(result);
    }

    @Test
    void testWorkdayCalc() {
        String result = dateTimeHandler.workdayCalc("2026-01-05", "2026-01-09");
        assertNotNull(result);
    }

    @Test
    void testCalendarGenerate() {
        String result = dateTimeHandler.calendarGenerate(2026, 7);
        assertNotNull(result);
    }

    @Test
    void testDateWeekday() {
        String result = dateTimeHandler.dateWeekday("2026-01-01");
        assertNotNull(result);
    }
}
