package com.example.mcp.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.mcp.handler.tool.TextToolHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * TextToolHandler 单元测试
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@SpringBootTest
class TextToolHandlerTest {

    @Autowired
    private TextToolHandler textToolHandler;

    @Test
    void testTextCount() {
        String result = textToolHandler.textCount("Hello World\n这是测试文本\n\n第三段", null);
        assertNotNull(result);
        assertTrue(result.contains("totalChars"));
    }

    @Test
    void testTextSortLines() {
        String result = textToolHandler.textSortLines("banana\napple\ncherry\nbanana", "asc", true, true);
        assertNotNull(result);
        assertEquals("apple\nbanana\ncherry", result);
    }

    @Test
    void testTextCaseConvert() {
        String result = textToolHandler.textCaseConvert("hello world", "upper");
        assertNotNull(result);
        assertTrue(result.contains("HELLO"));
    }

    @Test
    void testTextExtract() {
        String result = textToolHandler.textExtract("联系邮箱 test@example.com 或 13800138000", "email");
        assertNotNull(result);
        assertTrue(result.contains("test@example.com"));
    }

    @Test
    void testTextTrim() {
        String result = textToolHandler.textTrim("第一行\n\n第二行\n第二行", "dedup");
        assertNotNull(result);
    }

    @Test
    void testTextWrap() {
        String result = textToolHandler.textWrap("这是一段很长的文本需要自动换行处理测试内容", 10);
        assertNotNull(result);
    }

    @Test
    void testTextGenerate() {
        String result = textToolHandler.textGenerate("uuid", null, null);
        assertNotNull(result);
        assertTrue(result.length() > 30);
    }

    @Test
    void testTextUrlEncodeDecode() {
        String result = textToolHandler.textUrlEncodeDecode("https://example.com?q=测试", "encode");
        assertNotNull(result);
    }

    @Test
    void testTextBase64EncodeDecode() {
        String encoded = textToolHandler.textBase64EncodeDecode("测试文本", "encode");
        assertNotNull(encoded);
        String decoded = textToolHandler.textBase64EncodeDecode(encoded, "decode");
        assertTrue(decoded.contains("测试文本"));
    }

    @Test
    void testTextHashCalculate() {
        String result = textToolHandler.textHashCalculate("hello", "sha256");
        assertNotNull(result);
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", result);
    }

    /**
     * 测试空字符串哈希计算，预期返回错误信息
     */
    @Test
    void testTextHashCalculateEmpty() {
        String result = textToolHandler.textHashCalculate("", "sha256");
        assertNotNull(result);
        assertTrue(result.contains("错误"));
    }

    /**
     * 测试空字符串排序，预期返回错误信息
     */
    @Test
    void testTextSortLinesEmpty() {
        String result = textToolHandler.textSortLines("", "asc", null, null);
        assertNotNull(result);
        assertTrue(result.contains("错误"));
    }

    /**
     * 测试空字符串大小写转换，预期返回错误信息
     */
    @Test
    void testTextCaseConvertEmpty() {
        String result = textToolHandler.textCaseConvert("", "upper");
        assertNotNull(result);
        assertTrue(result.contains("错误"));
    }
}
