package com.example.mcp.handler.tool;

import com.alibaba.fastjson2.JSON;
import com.example.mcp.handler.BaseHandler;
import com.example.mcp.util.LogUtil;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP 通用工具实现，提供文件编码转换和正则表达式测试功能。
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@Service
public class ToolHandler extends BaseHandler {

    // --- 1. encode_convert ---

    /**
     * 文件编码转换，将文本文件从一种编码转换为另一种编码。
     * 如果未指定目标路径，则覆盖原文件。
     *
     * @param filePath     源文件路径
     * @param fromEncoding 源文件编码，如 GBK、UTF-8、ISO-8859-1
     * @param toEncoding   目标编码，如 UTF-8、GBK
     * @param targetPath   输出文件路径（可选，默认覆盖原文件）
     * @return 转换结果消息
     */
    @Tool(name = "tool_encode_convert", description = "文件编码转换。将文本文件从一种编码转换为另一种编码，支持 GBK、UTF-8、ISO-8859-1 等常见编码。")
    public String encodeConvert(@ToolParam(description = "源文件路径") String filePath,
        @ToolParam(description = "源文件编码，如 GBK、UTF-8、ISO-8859-1") String fromEncoding,
        @ToolParam(description = "目标编码，如 UTF-8、GBK") String toEncoding,
        @ToolParam(description = "输出文件路径（可选，默认覆盖原文件）", required = false) String targetPath) {
        return execute("tool_encode_convert", () -> {
            if (filePath == null || filePath.isBlank()) {
                return "错误: 文件路径不能为空";
            }
            if (fromEncoding == null || fromEncoding.isBlank()) {
                return "错误: 源编码不能为空";
            }
            if (toEncoding == null || toEncoding.isBlank()) {
                return "错误: 目标编码不能为空";
            }

            Path source = Path.of(filePath);
            if (!Files.exists(source)) {
                return "错误: 文件不存在 - " + filePath;
            }

            Charset fromCharset;
            Charset toCharset;
            try {
                fromCharset = Charset.forName(fromEncoding);
                toCharset = Charset.forName(toEncoding);
            } catch (Exception e) {
                return "错误: 不支持的编码格式 - " + e.getMessage();
            }

            if (fromCharset.equals(toCharset)) {
                LogUtil.info("encodeConvert 跳过: 源编码与目标编码相同 ({})", fromEncoding);
                return "源编码与目标编码相同，无需转换: " + fromEncoding;
            }

            String outputPath = (targetPath != null && !targetPath.isBlank()) ? targetPath : filePath;
            Path output = Path.of(outputPath);

            byte[] bytes = Files.readAllBytes(source);
            String content = new String(bytes, fromCharset);
            byte[] converted = content.getBytes(toCharset);
            Files.write(output, converted);

            LogUtil.info("编码转换成功: {} ({} -> {}), 输出: {}", filePath, fromEncoding, toEncoding, outputPath);
            return String.format("编码转换成功: %s (%s -> %s), 输出: %s", filePath, fromEncoding, toEncoding, outputPath);
        });
    }

    // --- 2. regex_test ---

    /**
     * 正则表达式测试工具，支持匹配、查找和替换三种模式。
     * <ul>
     *   <li>match 模式：测试整个文本是否完全匹配正则表达式，返回匹配结果和分组信息</li>
     *   <li>find 模式：在文本中查找所有匹配项，返回匹配数量和位置信息</li>
     *   <li>replace 模式：替换所有匹配项，返回替换后的文本和替换次数</li>
     * </ul>
     *
     * @param text        要测试的文本内容
     * @param pattern     正则表达式
     * @param mode        匹配模式：match（完全匹配）、find（查找）、replace（替换），默认 match
     * @param replacement 替换文本（mode 为 replace 时使用）
     * @param flags       正则标志，多个用逗号分隔，如 "DOTALL,CASE_INSENSITIVE,MULTILINE"
     * @return JSON 格式的测试结果
     */
    @Tool(name = "tool_regex_test", description = "正则表达式测试工具。支持匹配（match）、查找（find）和替换（replace）三种模式，返回 JSON 格式结果。")
    public String regexTest(@ToolParam(description = "要测试的文本内容") String text, @ToolParam(description = "正则表达式") String pattern,
        @ToolParam(description = "匹配模式：match（完全匹配）、find（查找）、replace（替换），默认 match", required = false) String mode,
        @ToolParam(description = "替换文本（mode 为 replace 时使用）", required = false) String replacement,
        @ToolParam(description = "正则标志，多个用逗号分隔，如 DOTALL,CASE_INSENSITIVE,MULTILINE", required = false) String flags) {
        return execute("tool_regex_test", () -> {
            if (text == null) {
                return "{\"error\": \"文本内容不能为空\"}";
            }
            if (pattern == null || pattern.isBlank()) {
                return "{\"error\": \"正则表达式不能为空\"}";
            }

            String m = (mode != null && !mode.isBlank()) ? mode.trim()
                                                               .toLowerCase() : "match";

            int flagBits = 0;
            if (flags != null && !flags.isBlank()) {
                for (String flag : flags.split(",")) {
                    switch (flag.trim()
                                .toUpperCase()) {
                        case "DOTALL":
                            flagBits |= Pattern.DOTALL;
                            break;
                        case "CASE_INSENSITIVE":
                            flagBits |= Pattern.CASE_INSENSITIVE;
                            break;
                        case "MULTILINE":
                            flagBits |= Pattern.MULTILINE;
                            break;
                        case "UNIX_LINES":
                            flagBits |= Pattern.UNIX_LINES;
                            break;
                        case "LITERAL":
                            flagBits |= Pattern.LITERAL;
                            break;
                        case "COMMENTS":
                            flagBits |= Pattern.COMMENTS;
                            break;
                        default:
                            LogUtil.warn("regexTest 不支持的标志: {}", flag.trim());
                    }
                }
            }

            Pattern p = Pattern.compile(pattern, flagBits);
            Matcher matcher = p.matcher(text);

            Map<String, Object> result = new LinkedHashMap<>();

            switch (m) {
                case "find" -> {
                    List<String> matches = new ArrayList<>();
                    List<Map<String, Object>> positions = new ArrayList<>();
                    while (matcher.find()) {
                        matches.add(matcher.group());
                        Map<String, Object> pos = new LinkedHashMap<>();
                        pos.put("start", matcher.start());
                        pos.put("end", matcher.end());
                        positions.add(pos);
                    }
                    result.put("count", matches.size());
                    result.put("matches", matches);
                    result.put("positions", positions);
                }
                case "replace" -> {
                    if (replacement == null) {
                        return "{\"error\": \"replace 模式需要提供 replacement 参数\"}";
                    }
                    int count = 0;
                    Matcher replaceMatcher = matcher;
                    StringBuffer sb = new StringBuffer();
                    while (replaceMatcher.find()) {
                        count++;
                        replaceMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                    }
                    replaceMatcher.appendTail(sb);
                    result.put("result", sb.toString());
                    result.put("replacements", count);
                }
                default -> { // match
                    boolean matches = matcher.matches();
                    result.put("matches", matches);
                    if (matches) {
                        List<String> groups = new ArrayList<>();
                        for (int i = 0; i <= matcher.groupCount(); i++) {
                            groups.add(matcher.group(i));
                        }
                        result.put("groups", groups);
                    }
                }
            }

            LogUtil.info("regexTest 完成: pattern={}, mode={}, text长度={}", pattern, m, text.length());
            return JSON.toJSONString(result);
        });
    }
}