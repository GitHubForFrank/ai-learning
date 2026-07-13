package com.example.mcp.handler.tool;

import com.alibaba.fastjson2.JSON;
import com.example.mcp.handler.BaseHandler;
import com.example.mcp.util.LogUtil;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP 文本处理工具实现，提供文本统计、排序、大小写转换、信息提取、编解码和哈希计算等功能。
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@Service
public class TextToolHandler extends BaseHandler {

    private static final Random RANDOM = new Random();

    // --- 1. text_count ---

    /**
     * 统计文本的基本信息，包括字符数、字数、行数、段落数等。
     *
     * @param text        要统计的文本内容
     * @param countSpaces 是否将空格计入字符统计，默认为 true
     * @return JSON 格式的统计结果
     */
    @Tool(name = "text_count", description = "统计文本的字数、行数、段落数、字符数（含/不含空格）等基本信息。")
    public String textCount(@ToolParam(description = "要统计的文本内容") String text,
        @ToolParam(description = "是否将空格计入字符统计，默认为 true", required = false) Boolean countSpaces) {
        return execute("textCount", () -> {
            if (text == null || text.isEmpty()) {
                return "{\"error\": \"文本内容不能为空\"}";
            }
            boolean includeSpaces = countSpaces == null || countSpaces;

            int totalChars = includeSpaces ? text.length() : text.replaceAll("\\s", "")
                                                                 .length();
            int lineCount = text.split("\\r?\\n", -1).length;
            // 段落按连续空行分割
            int paragraphCount = text.trim()
                                     .isEmpty() ? 0 : text.trim()
                                                          .split("\\n\\s*\\n").length;
            // 中文字数
            int chineseChars = 0;
            for (char c : text.toCharArray()) {
                if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                    chineseChars++;
                }
            }
            // 英文单词数
            String[] words = text.replaceAll("[\\p{Punct}&&[^']]", " ")
                                 .trim()
                                 .split("\\s+");
            int wordCount = text.trim()
                                .isEmpty() ? 0 : words.length;

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("totalChars", totalChars);
            result.put("lineCount", lineCount);
            result.put("paragraphCount", paragraphCount);
            result.put("chineseChars", chineseChars);
            result.put("wordCount", wordCount);
            if (!includeSpaces) {
                result.put("note", "空格未计入字符统计");
            }

            LogUtil.info("textCount 完成: 总字符数={}, 行数={}, 段落数={}, 中文字数={}, 单词数={}", totalChars, lineCount, paragraphCount,
                         chineseChars, wordCount);
            return JSON.toJSONString(result);
        });
    }

    // --- 2. text_sort_lines ---

    /**
     * 对文本按行进行排序，支持升序/降序、去重和反转。
     *
     * @param text      要排序的文本内容
     * @param order     排序方式：asc（升序）、desc（降序）、reverse（反转）
     * @param unique    是否去重，默认为 false
     * @param trimEmpty 是否忽略空行，默认为 true
     * @return 排序后的文本
     */
    @Tool(name = "text_sort_lines", description = "对文本按行进行排序，支持升序/降序、去重和反转。")
    public String textSortLines(@ToolParam(description = "要排序的文本内容") String text,
        @ToolParam(description = "排序方式：asc（升序）、desc（降序）、reverse（反转），默认 asc", required = false) String order,
        @ToolParam(description = "是否去重，默认 false", required = false) Boolean unique,
        @ToolParam(description = "是否忽略空行，默认 true", required = false) Boolean trimEmpty) {
        return execute("textSortLines", () -> {
            if (text == null || text.isEmpty()) {
                return "错误: 文本内容不能为空";
            }
            String orderMode = (order != null && !order.isBlank()) ? order.trim()
                                                                          .toLowerCase() : "asc";
            boolean doUnique = unique != null && unique;
            boolean doTrimEmpty = trimEmpty == null || trimEmpty;

            String[] lines = text.split("\\r?\\n", -1);
            List<String> lineList = new ArrayList<>();
            for (String line : lines) {
                if (doTrimEmpty && line.trim()
                                       .isEmpty()) {
                    continue;
                }
                lineList.add(line);
            }

            if (doUnique) {
                lineList = new ArrayList<>(new java.util.LinkedHashSet<>(lineList));
            }

            switch (orderMode) {
                case "desc":
                    lineList.sort(Comparator.reverseOrder());
                    break;
                case "reverse":
                    java.util.Collections.reverse(lineList);
                    break;
                default:
                    lineList.sort(Comparator.naturalOrder());
                    break;
            }

            String result = String.join("\n", lineList);
            LogUtil.info("textSortLines 完成: 模式={}, 去重={}, 行数={}", orderMode, doUnique, lineList.size());
            return result;
        });
    }

    // --- 3. text_case_convert ---

    /**
     * 文本大小写转换，支持大写、小写、驼峰、蛇形、短横线等多种格式互转。
     *
     * @param text 要转换的文本
     * @param mode 转换模式：upper（全大写）、lower（全小写）、camel（驼峰命名）、snake（蛇形命名）、kebab（短横线命名）
     * @return 转换后的文本
     */
    @Tool(name = "text_case_convert", description = "文本大小写格式转换，支持全大写、全小写、驼峰命名、蛇形命名、短横线命名。")
    public String textCaseConvert(@ToolParam(description = "要转换的文本") String text,
        @ToolParam(description = "转换模式：upper（全大写）、lower（全小写）、camel（驼峰命名）、snake（蛇形命名）、kebab（短横线命名）") String mode) {
        return execute("textCaseConvert", () -> {
            if (text == null || text.isEmpty()) {
                return "错误: 文本内容不能为空";
            }
            if (mode == null || mode.isBlank()) {
                return "错误: 转换模式不能为空";
            }
            String m = mode.trim()
                           .toLowerCase();
            String result;
            switch (m) {
                case "upper":
                    result = text.toUpperCase();
                    break;
                case "lower":
                    result = text.toLowerCase();
                    break;
                case "camel":
                    result = toCamelCase(text);
                    break;
                case "snake":
                    result = toSnakeCase(text);
                    break;
                case "kebab":
                    result = toKebabCase(text);
                    break;
                default:
                    return "错误: 不支持的模式 '" + mode + "'，支持: upper, lower, camel, snake, kebab";
            }
            LogUtil.info("textCaseConvert 完成: 模式={}, 原长度={}", m, text.length());
            return result;
        });
    }

    private String toCamelCase(String text) {
        String[] words = text.split("[\\s_\\-]+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (words[i].isEmpty()) {
                continue;
            }
            if (i == 0) {
                sb.append(words[i].toLowerCase());
            } else {
                sb.append(Character.toUpperCase(words[i].charAt(0)));
                if (words[i].length() > 1) {
                    sb.append(words[i].substring(1)
                                      .toLowerCase());
                }
            }
        }
        return sb.toString();
    }

    private String toSnakeCase(String text) {
        return text.replaceAll("([a-z])([A-Z])", "$1_$2")
                   .replaceAll("[\\s\\-]+", "_")
                   .toLowerCase();
    }

    private String toKebabCase(String text) {
        return text.replaceAll("([a-z])([A-Z])", "$1-$2")
                   .replaceAll("[\\s_]+", "-")
                   .toLowerCase();
    }

    // --- 4. text_extract ---

    /**
     * 从文本中提取特定模式的信息，如邮箱、手机号、URL、IP 地址等。
     *
     * @param text        要提取信息的文本
     * @param extractType 提取类型：email（邮箱）、phone（手机号）、url（URL）、ip（IP地址）
     * @return JSON 格式的提取结果
     */
    @Tool(name = "text_extract", description = "从文本中提取特定模式的信息，支持邮箱、手机号、URL、IP地址等。")
    public String textExtract(@ToolParam(description = "要提取信息的文本") String text,
        @ToolParam(description = "提取类型：email（邮箱）、phone（手机号）、url（URL）、ip（IP地址）") String extractType) {
        return execute("textExtract", () -> {
            if (text == null || text.isEmpty()) {
                return "{\"error\": \"文本内容不能为空\"}";
            }
            if (extractType == null || extractType.isBlank()) {
                return "{\"error\": \"提取类型不能为空\"}";
            }
            String type = extractType.trim()
                                     .toLowerCase();
            String patternStr;
            switch (type) {
                case "email":
                    patternStr = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}";
                    break;
                case "phone":
                    patternStr = "1[3-9]\\d{9}";
                    break;
                case "url":
                    patternStr = "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+";
                    break;
                case "ip":
                    patternStr = "\\b(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\b";
                    break;
                default:
                    return "{\"error\": \"不支持的提取类型: " + extractType + "，支持: email, phone, url, ip\"}";
            }

            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(text);
            List<String> matches = new ArrayList<>();
            while (matcher.find()) {
                matches.add(matcher.group());
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("type", type);
            result.put("count", matches.size());
            result.put("matches", matches);

            LogUtil.info("textExtract 完成: 类型={}, 匹配数={}", type, matches.size());
            return JSON.toJSONString(result);
        });
    }

    // --- 5. text_trim ---

    /**
     * 去除文本中的空行、首尾空格或重复行。
     *
     * @param text     要处理的文本
     * @param trimMode 处理模式：emptyLines（去除空行）、whitespace（去除每行首尾空格）、dedup（去除重复行）
     * @return 处理后的文本
     */
    @Tool(name = "text_trim", description = "去除文本中的空行、每行首尾空格或重复行。")
    public String textTrim(@ToolParam(description = "要处理的文本") String text,
        @ToolParam(description = "处理模式：emptyLines（去除空行）、whitespace（去除每行首尾空格）、dedup（去除重复行）") String trimMode) {
        return execute("textTrim", () -> {
            if (text == null || text.isEmpty()) {
                return "错误: 文本内容不能为空";
            }
            if (trimMode == null || trimMode.isBlank()) {
                return "错误: 处理模式不能为空";
            }
            String mode = trimMode.trim()
                                  .toLowerCase();
            String[] lines = text.split("\\r?\\n", -1);
            StringBuilder sb = new StringBuilder();

            switch (mode) {
                case "emptylines":
                    for (String line : lines) {
                        if (!line.trim()
                                 .isEmpty()) {
                            if (sb.length() > 0) {
                                sb.append("\n");
                            }
                            sb.append(line);
                        }
                    }
                    break;
                case "whitespace":
                    for (int i = 0; i < lines.length; i++) {
                        if (i > 0) {
                            sb.append("\n");
                        }
                        sb.append(lines[i].replaceFirst("^\\s+", "")
                                          .replaceFirst("\\s+$", ""));
                    }
                    break;
                case "dedup":
                    java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
                    for (String line : lines) {
                        if (seen.add(line)) {
                            if (sb.length() > 0) {
                                sb.append("\n");
                            }
                            sb.append(line);
                        }
                    }
                    break;
                default:
                    return "错误: 不支持的模式 '" + trimMode + "', 支持: emptyLines, whitespace, dedup";
            }

            LogUtil.info("textTrim 完成: 模式={}, 原行数={}", mode, lines.length);
            return sb.toString();
        });
    }

    // --- 6. text_wrap ---

    /**
     * 按指定宽度对文本进行自动换行。
     *
     * @param text  要换行的文本
     * @param width 每行最大字符宽度
     * @return 换行后的文本
     */
    @Tool(name = "text_wrap", description = "按指定宽度对文本进行自动换行处理。")
    public String textWrap(@ToolParam(description = "要换行的文本") String text, @ToolParam(description = "每行最大字符宽度，默认 80") Integer width) {
        return execute("textWrap", () -> {
            if (text == null || text.isEmpty()) {
                return "错误: 文本内容不能为空";
            }
            int w = (width != null && width > 0) ? width : 80;
            StringBuilder sb = new StringBuilder();
            String[] paragraphs = text.split("\\n", -1);
            for (int p = 0; p < paragraphs.length; p++) {
                if (p > 0) {
                    sb.append("\n");
                }
                String para = paragraphs[p];
                if (para.length() <= w) {
                    sb.append(para);
                } else {
                    int start = 0;
                    while (start < para.length()) {
                        if (sb.length() > 0 && start == 0 && p > 0) {
                            // already added newline
                        } else if (start > 0) {
                            sb.append("\n");
                        }
                        int end = Math.min(start + w, para.length());
                        sb.append(para, start, end);
                        start = end;
                    }
                }
            }
            LogUtil.info("textWrap 完成: 宽度={}, 原长度={}", w, text.length());
            return sb.toString();
        });
    }

    // --- 7. text_generate ---

    /**
     * 生成随机文本内容，支持 UUID、随机数字、随机字母、随机混合字符串。
     *
     * @param generateType 生成类型：uuid（UUID）、number（纯数字）、letter（纯字母）、mixed（数字+字母混合）
     * @param length       生成内容的长度（uuid 模式忽略此参数）
     * @param count        生成数量，默认 1
     * @return 生成的文本内容，多个结果用换行分隔
     */
    @Tool(name = "text_generate", description = "生成随机文本内容，支持UUID、随机数字、字母和混合字符串。")
    public String textGenerate(@ToolParam(description = "生成类型：uuid（UUID）、number（纯数字）、letter（纯字母）、mixed（数字+字母混合）") String generateType,
        @ToolParam(description = "生成内容的长度（uuid模式忽略），默认 16", required = false) Integer length,
        @ToolParam(description = "生成数量，默认 1", required = false) Integer count) {
        return execute("textGenerate", () -> {
            String type = (generateType != null && !generateType.isBlank()) ? generateType.trim()
                                                                                          .toLowerCase() : "uuid";
            int len = (length != null && length > 0) ? length : 16;
            int cnt = (count != null && count > 0) ? count : 1;
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < cnt; i++) {
                if (i > 0) {
                    sb.append("\n");
                }
                String generated;
                switch (type) {
                    case "uuid":
                        generated = UUID.randomUUID()
                                        .toString();
                        break;
                    case "number":
                        generated = generateRandomString("0123456789", len);
                        break;
                    case "letter":
                        generated = generateRandomString("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ", len);
                        break;
                    case "mixed":
                        generated = generateRandomString("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ", len);
                        break;
                    default:
                        return "错误: 不支持的类型 '" + generateType + "', 支持: uuid, number, letter, mixed";
                }
                sb.append(generated);
            }

            LogUtil.info("textGenerate 完成: 类型={}, 长度={}, 数量={}", type, len, cnt);
            return sb.toString();
        });
    }

    private String generateRandomString(String chars, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // --- 8. text_url_encode_decode ---

    /**
     * URL 编码或解码操作。
     *
     * @param content 要编码/解码的文本内容
     * @param mode    操作模式：encode（编码）、decode（解码）
     * @return 编码/解码后的文本
     */
    @Tool(name = "text_url_encode_decode", description = "对文本进行 URL 编码（encode）或 URL 解码（decode）。")
    public String textUrlEncodeDecode(@ToolParam(description = "要编码/解码的文本内容") String content,
        @ToolParam(description = "操作模式：encode（编码）、decode（解码）") String mode) {
        return execute("textUrlEncodeDecode", () -> {
            if (content == null || content.isEmpty()) {
                return "错误: 文本内容不能为空";
            }
            if (mode == null || mode.isBlank()) {
                return "错误: 操作模式不能为空";
            }
            String m = mode.trim()
                           .toLowerCase();
            String result;
            if ("encode".equals(m)) {
                result = URLEncoder.encode(content, StandardCharsets.UTF_8);
            } else if ("decode".equals(m)) {
                result = URLDecoder.decode(content, StandardCharsets.UTF_8);
            } else {
                return "错误: 不支持的模式 '" + mode + "', 支持: encode, decode";
            }
            LogUtil.info("textUrlEncodeDecode 完成: 模式={}, 长度={}", m, content.length());
            return result;
        });
    }

    // --- 9. text_base64_encode_decode ---

    /**
     * Base64 编码或解码操作。
     *
     * @param content 要编码/解码的文本内容
     * @param mode    操作模式：encode（编码）、decode（解码）
     * @return 编码/解码后的文本
     */
    @Tool(name = "text_base64_encode_decode", description = "对文本进行 Base64 编码（encode）或 Base64 解码（decode）。")
    public String textBase64EncodeDecode(@ToolParam(description = "要编码/解码的文本内容") String content,
        @ToolParam(description = "操作模式：encode（编码）、decode（解码）") String mode) {
        return execute("textBase64EncodeDecode", () -> {
            if (content == null || content.isEmpty()) {
                return "错误: 文本内容不能为空";
            }
            if (mode == null || mode.isBlank()) {
                return "错误: 操作模式不能为空";
            }
            String m = mode.trim()
                           .toLowerCase();
            String result;
            if ("encode".equals(m)) {
                result = Base64.getEncoder()
                               .encodeToString(content.getBytes(StandardCharsets.UTF_8));
            } else if ("decode".equals(m)) {
                byte[] decoded = Base64.getDecoder()
                                       .decode(content);
                result = new String(decoded, StandardCharsets.UTF_8);
            } else {
                return "错误: 不支持的模式 '" + mode + "', 支持: encode, decode";
            }
            LogUtil.info("textBase64EncodeDecode 完成: 模式={}", m);
            return result;
        });
    }

    // --- 10. text_hash_calculate ---

    /**
     * 计算文本的哈希值，支持 SHA-256、SHA-512 和 MD5。
     *
     * @param text      要计算哈希的文本内容
     * @param algorithm 哈希算法：sha256、sha512、md5，默认 sha256
     * @return 十六进制格式的哈希值
     */
    @Tool(name = "text_hash_calculate", description = "计算文本的 SHA-256、SHA-512 或 MD5 哈希值。")
    public String textHashCalculate(@ToolParam(description = "要计算哈希的文本内容") String text,
        @ToolParam(description = "哈希算法：sha256、sha512、md5，默认 sha256", required = false) String algorithm) {
        return execute("textHashCalculate", () -> {
            if (text == null || text.isEmpty()) {
                return "错误: 文本内容不能为空";
            }
            String algo = (algorithm != null && !algorithm.isBlank()) ? algorithm.trim()
                                                                                 .toLowerCase() : "sha256";
            String javaAlgo;
            switch (algo) {
                case "sha256":
                    javaAlgo = "SHA-256";
                    break;
                case "sha512":
                    javaAlgo = "SHA-512";
                    break;
                case "md5":
                    javaAlgo = "MD5";
                    break;
                default:
                    return "错误: 不支持的算法 '" + algorithm + "', 支持: sha256, sha512, md5";
            }

            MessageDigest md = MessageDigest.getInstance(javaAlgo);
            byte[] hashBytes = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            String result = hexString.toString();

            LogUtil.info("textHashCalculate 完成: 算法={}, 结果长度={}", algo, result.length());
            return result;
        });
    }
}
