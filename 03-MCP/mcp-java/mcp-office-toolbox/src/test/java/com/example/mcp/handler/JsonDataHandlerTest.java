package com.example.mcp.handler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.mcp.handler.json.JsonDataHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * JsonDataHandler 单元测试
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@SpringBootTest
class JsonDataHandlerTest {

    @Autowired
    private JsonDataHandler jsonDataHandler;

    @Test
    void testJsonFormat() {
        String result = jsonDataHandler.jsonFormat("{\"name\":\"张三\",\"age\":25}", null);
        assertNotNull(result);
        assertTrue(result.contains("name"));
    }

    @Test
    void testJsonValidate() {
        String result = jsonDataHandler.jsonValidate("{\"key\":\"value\"}");
        assertNotNull(result);
        assertTrue(result.contains("合法"));
    }

    @Test
    void testJsonValidateInvalid() {
        String result = jsonDataHandler.jsonValidate("{invalid");
        assertNotNull(result);
        assertTrue(result.contains("错误"));
    }

    @Test
    void testJsonQuery() {
        String result = jsonDataHandler.jsonQuery("{\"data\":{\"items\":[{\"name\":\"test\"}]}}", "data.items[0].name");
        assertNotNull(result);
        assertTrue(result.contains("test"));
    }

    @Test
    void testJsonToCsv() {
        String result = jsonDataHandler.jsonToCsv("[{\"姓名\":\"张三\",\"年龄\":25}]", null, null);
        assertNotNull(result);
        assertTrue(result.contains("张三"));
    }

    @Test
    void testJsonToYaml() {
        String result = jsonDataHandler.jsonToYaml("{\"name\":\"test\",\"value\":123}");
        assertNotNull(result);
        assertTrue(result.contains("name"));
    }

    @Test
    void testYamlToJson() {
        String result = jsonDataHandler.yamlToJson("name: test\nvalue: 123");
        assertNotNull(result);
        assertTrue(result.contains("test"));
    }

    @Test
    void testXmlValidate() {
        String result = jsonDataHandler.xmlValidate("<root><name>test</name></root>");
        assertNotNull(result);
        assertTrue(result.contains("合法"));
    }

    @Test
    void testXmlFormat() {
        String result = jsonDataHandler.xmlFormat("<root><a>1</a><b>2</b></root>");
        assertNotNull(result);
    }

    @Test
    void testCsvToJson() {
        String result = jsonDataHandler.csvToJson("姓名,年龄\n张三,25", null, null);
        assertNotNull(result);
        assertTrue(result.contains("张三"));
    }
}
