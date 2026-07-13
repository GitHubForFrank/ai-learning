package com.example.mcp.handler;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.example.mcp.handler.i18n.I18nHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * I18nHandler 单元测试
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@SpringBootTest
class I18nHandlerTest {

    @Autowired
    private I18nHandler i18nHandler;

    @Test
    void testUnitConvertLength() {
        String result = i18nHandler.i18nUnitConvert(1.0, "km", "m", "length");
        assertNotNull(result);
    }

    @Test
    void testUnitConvertTemperature() {
        String result = i18nHandler.i18nUnitConvert(0.0, "celsius", "fahrenheit", "temperature");
        assertNotNull(result);
    }

    @Test
    void testCurrencyConvert() {
        String result = i18nHandler.i18nCurrencyConvert(100.0, "CNY", "USD", null);
        assertNotNull(result);
    }

    @Test
    void testNumberFormat() {
        String result = i18nHandler.i18nNumberFormat(12345L, "thousands", "zh_CN");
        assertNotNull(result);
    }

    @Test
    void testNumberFormatChinese() {
        String result = i18nHandler.i18nNumberFormat(100L, "chinese_upper", null);
        assertNotNull(result);
    }
}
