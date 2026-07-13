package com.example.mcp.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.mcp.handler.tool.ToolHandler;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * ToolHandler 单元测试
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@SpringBootTest
class ToolHandlerTest {

    @TempDir
    Path tempDir;
    @Autowired
    private ToolHandler toolHandler;
    private Path testFile;

    @BeforeEach
    void setUp() {
        testFile = tempDir.resolve("test.txt");
    }

    // ==================== encodeConvert 测试 ====================

    @Test
    void testEncodeConvertValid() throws Exception {
        // 用 GBK 编码写入中文内容
        String content = "你好，世界！";
        Files.writeString(testFile, content, Charset.forName("GBK"));

        Path targetFile = tempDir.resolve("output.txt");
        String result = toolHandler.encodeConvert(testFile.toString(), "GBK", "UTF-8", targetFile.toString());

        assertTrue(result.contains("编码转换成功"));
        assertTrue(Files.exists(targetFile));
        String converted = Files.readString(targetFile, Charset.forName("UTF-8"));
        assertEquals(content, converted);
    }

    @Test
    void testEncodeConvertSameEncoding() throws Exception {
        Files.writeString(testFile, "Hello World", Charset.forName("UTF-8"));

        String result = toolHandler.encodeConvert(testFile.toString(), "UTF-8", "UTF-8", null);

        assertTrue(result.contains("源编码与目标编码相同"));
        assertTrue(result.contains("无需转换"));
    }

    @Test
    void testEncodeConvertInvalidEncoding() throws Exception {
        Files.writeString(testFile, "Hello World");

        String result = toolHandler.encodeConvert(testFile.toString(), "INVALID_ENCODING", "UTF-8", null);

        assertTrue(result.contains("不支持的编码格式"));
    }

    @Test
    void testEncodeConvertNullFilePath() {
        String result = toolHandler.encodeConvert(null, "GBK", "UTF-8", null);

        assertTrue(result.contains("文件路径不能为空"));
    }

    @Test
    void testEncodeConvertNullFromEncoding() {
        String result = toolHandler.encodeConvert(testFile.toString(), null, "UTF-8", null);

        assertTrue(result.contains("源编码不能为空"));
    }

    @Test
    void testEncodeConvertNullToEncoding() {
        String result = toolHandler.encodeConvert(testFile.toString(), "GBK", null, null);

        assertTrue(result.contains("目标编码不能为空"));
    }

    @Test
    void testEncodeConvertFileNotExist() {
        String result = toolHandler.encodeConvert(tempDir.resolve("nonexistent.txt")
                                                         .toString(), "GBK", "UTF-8", null);

        assertTrue(result.contains("文件不存在"));
    }

    @Test
    void testEncodeConvertOverwriteSource() throws Exception {
        // 用 GBK 编码写入，转换后覆盖原文件
        String content = "编码转换测试";
        Files.writeString(testFile, content, Charset.forName("GBK"));

        String result = toolHandler.encodeConvert(testFile.toString(), "GBK", "UTF-8", null);

        assertTrue(result.contains("编码转换成功"));
        String converted = Files.readString(testFile, Charset.forName("UTF-8"));
        assertEquals(content, converted);
    }

    // ==================== regexTest 测试 ====================

    @Test
    void testRegexTestMatchSuccess() {
        String result = toolHandler.regexTest("Hello World", "Hello World", "match", null, null);

        assertTrue(result.contains("\"matches\":true"));
        assertTrue(result.contains("\"groups\""));
        assertTrue(result.contains("\"Hello World\""));
    }

    @Test
    void testRegexTestMatchFail() {
        String result = toolHandler.regexTest("Hello World", "Goodbye", "match", null, null);

        assertTrue(result.contains("\"matches\":false"));
    }

    @Test
    void testRegexTestMatchWithGroups() {
        String result = toolHandler.regexTest("2024-01-15", "(\\d{4})-(\\d{2})-(\\d{2})", "match", null, null);

        assertTrue(result.contains("\"matches\":true"));
        assertTrue(result.contains("\"groups\""));
        assertTrue(result.contains("\"2024\""));
        assertTrue(result.contains("\"01\""));
        assertTrue(result.contains("\"15\""));
    }

    @Test
    void testRegexTestFindMode() {
        String result = toolHandler.regexTest("abc123def456ghi789", "\\d+", "find", null, null);

        assertTrue(result.contains("\"count\":3"));
        assertTrue(result.contains("\"123\""));
        assertTrue(result.contains("\"456\""));
        assertTrue(result.contains("\"789\""));
        assertTrue(result.contains("\"matches\""));
        assertTrue(result.contains("\"positions\""));
    }

    @Test
    void testRegexTestFindModeNoMatch() {
        String result = toolHandler.regexTest("abcdef", "\\d+", "find", null, null);

        assertTrue(result.contains("\"count\":0"));
    }

    @Test
    void testRegexTestReplaceMode() {
        String result = toolHandler.regexTest("Hello World, World is big", "World", "replace", "Java", null);

        assertTrue(result.contains("\"result\""));
        assertTrue(result.contains("Hello Java, Java is big"));
        assertTrue(result.contains("\"replacements\":2"));
    }

    @Test
    void testRegexTestReplaceModeSingle() {
        String result = toolHandler.regexTest("Hello World", "World", "replace", "地球", null);

        assertTrue(result.contains("\"result\""));
        assertTrue(result.contains("Hello 地球"));
        assertTrue(result.contains("\"replacements\":1"));
    }

    @Test
    void testRegexTestReplaceModeNoReplacement() {
        String result = toolHandler.regexTest("Hello World", "World", "replace", null, null);

        assertTrue(result.contains("replace 模式需要提供 replacement 参数"));
    }

    @Test
    void testRegexTestInvalidPattern() {
        String result = toolHandler.regexTest("Hello World", "[invalid(", "match", null, null);

        assertTrue(result.contains("Unclosed character class"));
    }

    @Test
    void testRegexTestNullText() {
        String result = toolHandler.regexTest(null, ".*", "match", null, null);

        assertTrue(result.contains("文本内容不能为空"));
    }

    @Test
    void testRegexTestNullPattern() {
        String result = toolHandler.regexTest("Hello World", null, "match", null, null);

        assertTrue(result.contains("正则表达式不能为空"));
    }

    @Test
    void testRegexTestDefaultMode() {
        // 不传 mode，默认使用 match 模式
        String result = toolHandler.regexTest("Hello", "Hello", null, null, null);

        assertTrue(result.contains("\"matches\":true"));
    }

    @Test
    void testRegexTestWithFlags() {
        // 使用 CASE_INSENSITIVE 标志
        String result = toolHandler.regexTest("Hello World", "hello world", "match", null, "CASE_INSENSITIVE");

        assertTrue(result.contains("\"matches\":true"));
    }

    @Test
    void testRegexTestMatchWithEmptyText() {
        String result = toolHandler.regexTest("", "^$", "match", null, null);

        assertTrue(result.contains("\"matches\":true"));
    }

    @Test
    void testRegexTestFindModeWithPositions() {
        String result = toolHandler.regexTest("abc123def456", "\\d{3}", "find", null, null);

        assertTrue(result.contains("\"count\":2"));
        assertTrue(result.contains("\"start\""));
        assertTrue(result.contains("\"end\""));
        assertTrue(result.contains("\"123\""));
        assertTrue(result.contains("\"456\""));
    }
}