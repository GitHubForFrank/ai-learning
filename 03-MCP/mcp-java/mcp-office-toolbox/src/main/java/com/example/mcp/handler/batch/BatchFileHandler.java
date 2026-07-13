package com.example.mcp.handler.batch;

import com.example.mcp.handler.BaseHandler;

import com.example.mcp.pojo.batch.BatchConvertEncodingRequest;
import com.example.mcp.pojo.batch.BatchRenameRequest;
import com.example.mcp.pojo.batch.BatchReplaceTextRequest;
import com.example.mcp.util.LogUtil;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * 批量文件操作处理器，提供批量重命名、批量文本替换、批量编码转换和重复文件查找 MCP 工具。
 * 基于 JDK 21 NIO (java.nio.file) 和 java.security.MessageDigest 实现。
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@Service
public class BatchFileHandler extends BaseHandler {

    /**
     * 批量重命名文件：按规则（前缀/后缀/序号/替换）重命名目录中的文件。
     * <p>
     * 支持四种重命名模式：
     * <ul>
     *   <li>prefix - 添加前缀</li>
     *   <li>suffix - 添加后缀</li>
     *   <li>index - 按序号重命名（如 file_001.txt, file_002.txt）</li>
     *   <li>replace - 替换文件名中的文本</li>
     * </ul>
     *
     * @param dirPath        目标目录的绝对路径
     * @param mode           重命名模式：prefix / suffix / index / replace
     * @param value          重命名参数值：prefix/suffix 模式为要添加的文本，index 模式为基础名称，replace 模式为 "旧文本|新文本"
     * @param pattern        文件名匹配模式（支持通配符 * 和 ?），如 "*.txt" 匹配所有 txt 文件，可选，默认匹配所有文件
     * @param includeSubdirs 是否包含子目录中的文件，可选，默认 false
     * @return 重命名结果报告
     */
    @Tool(name = "batch_rename", description = "批量重命名文件：按规则（前缀/后缀/序号/替换）重命名目录中的文件")
    public String batchRename(BatchRenameRequest request) {

        if (request.dirPath() == null || request.dirPath()
                                                .isBlank()) {
            return "错误: 目录路径不能为空";
        }
        if (request.mode() == null || request.mode()
                                             .isBlank()) {
            return "错误: 重命名模式不能为空";
        }
        if (request.value() == null || request.value()
                                              .isBlank()) {
            return "错误: 重命名参数值不能为空";
        }

        LogUtil.info("批量重命名: 目录={}, 模式={}, 值={}", request.dirPath(), request.mode(), request.value());

        return execute("batchRename", () -> {
            Path dir = Path.of(request.dirPath());
            if (!Files.isDirectory(dir)) {
                return "错误: 路径不是目录 - " + request.dirPath();
            }

            List<Path> files = listFiles(dir, request.pattern(), request.includeSubdirs() != null && request.includeSubdirs());
            if (files.isEmpty()) {
                return "未找到匹配的文件。";
            }

            // 按文件名排序，确保 index 模式序号稳定
            files.sort(Comparator.comparing(p -> p.getFileName()
                                                  .toString()));

            int successCount = 0;
            int skipCount = 0;
            List<String> details = new ArrayList<>();

            for (int i = 0; i < files.size(); i++) {
                Path file = files.get(i);
                String oldName = file.getFileName()
                                     .toString();
                String newName = generateNewName(oldName, request.mode(), request.value(), i, files.size());

                if (newName == null || newName.equals(oldName)) {
                    skipCount++;
                    continue;
                }

                Path newPath = file.resolveSibling(newName);
                if (Files.exists(newPath)) {
                    details.add(String.format("  跳过（目标已存在）: %s -> %s", oldName, newName));
                    skipCount++;
                    continue;
                }

                try {
                    Files.move(file, newPath);
                    details.add(String.format("  已重命名: %s -> %s", oldName, newName));
                    successCount++;
                } catch (IOException e) {
                    details.add(String.format("  重命名失败: %s -> %s (%s)", oldName, newName, e.getMessage()));
                    skipCount++;
                }
            }

            StringBuilder report = new StringBuilder();
            report.append("=== 批量重命名报告 ===\n");
            report.append("目录: ")
                  .append(request.dirPath())
                  .append("\n");
            report.append("模式: ")
                  .append(request.mode())
                  .append(", 参数: ")
                  .append(request.value())
                  .append("\n");
            report.append("匹配文件数: ")
                  .append(files.size())
                  .append("\n");
            report.append("成功: ")
                  .append(successCount)
                  .append(" 个, 跳过: ")
                  .append(skipCount)
                  .append(" 个\n\n");

            for (String detail : details) {
                report.append(detail)
                      .append("\n");
            }

            LogUtil.info("批量重命名完成: 成功 {} 个, 跳过 {} 个", successCount, skipCount);
            return report.toString();
        });
    }

    /**
     * 批量替换多个文件中的文本内容。
     * <p>
     * 在匹配的文件中查找并替换指定的文本，支持正则表达式匹配。
     * 修改会直接写入原文件。
     *
     * @param dirPath        目标目录的绝对路径
     * @param searchText     要查找的文本（支持正则表达式）
     * @param replaceText    替换后的文本
     * @param pattern        文件名匹配模式（支持通配符 * 和 ?），可选，默认匹配所有文件
     * @param useRegex       是否使用正则表达式匹配，可选，默认 false（普通文本替换）
     * @param includeSubdirs 是否包含子目录中的文件，可选，默认 false
     * @return 替换结果报告
     */
    @Tool(name = "batch_replace_text", description = "批量替换多个文件中的文本内容")
    public String batchReplaceText(BatchReplaceTextRequest request) {

        if (request.dirPath() == null || request.dirPath()
                                                .isBlank()) {
            return "错误: 目录路径不能为空";
        }
        if (request.searchText() == null || request.searchText()
                                                   .isBlank()) {
            return "错误: 查找文本不能为空";
        }
        String replaceText = request.replaceText() != null ? request.replaceText() : "";

        LogUtil.info("批量替换文本: 目录={}, 查找={}, 替换={}", request.dirPath(), request.searchText(), replaceText);

        return execute("batchReplaceText", () -> {
            Path dir = Path.of(request.dirPath());
            if (!Files.isDirectory(dir)) {
                return "错误: 路径不是目录 - " + request.dirPath();
            }

            List<Path> files = listFiles(dir, request.pattern(), request.includeSubdirs() != null && request.includeSubdirs());
            if (files.isEmpty()) {
                return "未找到匹配的文件。";
            }

            boolean regexMode = request.useRegex() != null && request.useRegex();
            Pattern regexPattern = null;
            if (regexMode) {
                regexPattern = Pattern.compile(request.searchText());
            }

            int modifiedCount = 0;
            int totalReplacements = 0;
            List<String> details = new ArrayList<>();

            for (Path file : files) {
                try {
                    String content = Files.readString(file);

                    String newContent;
                    int count;
                    if (regexMode) {
                        Matcher matcher = regexPattern.matcher(content);
                        StringBuilder sb = new StringBuilder();
                        count = 0;
                        while (matcher.find()) {
                            matcher.appendReplacement(sb, Matcher.quoteReplacement(replaceText));
                            count++;
                        }
                        matcher.appendTail(sb);
                        newContent = sb.toString();
                    } else {
                        count = countOccurrences(content, request.searchText());
                        newContent = content.replace(request.searchText(), replaceText);
                    }

                    if (count > 0) {
                        Files.writeString(file, newContent);
                        modifiedCount++;
                        totalReplacements += count;
                        details.add(String.format("  %s: %d 处替换", file.getFileName(), count));
                    }
                } catch (IOException e) {
                    details.add(String.format("  %s: 读取/写入失败 - %s", file.getFileName(), e.getMessage()));
                }
            }

            StringBuilder report = new StringBuilder();
            report.append("=== 批量文本替换报告 ===\n");
            report.append("目录: ")
                  .append(request.dirPath())
                  .append("\n");
            report.append("查找: \"")
                  .append(request.searchText())
                  .append("\", 替换为: \"")
                  .append(replaceText)
                  .append("\"\n");
            report.append("正则模式: ")
                  .append(regexMode ? "是" : "否")
                  .append("\n");
            report.append("扫描文件数: ")
                  .append(files.size())
                  .append("\n");
            report.append("修改文件数: ")
                  .append(modifiedCount)
                  .append(", 总替换次数: ")
                  .append(totalReplacements)
                  .append("\n\n");

            for (String detail : details) {
                report.append(detail)
                      .append("\n");
            }

            LogUtil.info("批量文本替换完成: 修改 {} 个文件, {} 处替换", modifiedCount, totalReplacements);
            return report.toString();
        });
    }

    /**
     * 批量转换文件编码。
     * <p>
     * 将目录中文件的编码从一种格式转换为另一种格式（如 GBK -> UTF-8），
     * 修改会直接写入原文件。
     *
     * @param dirPath        目标目录的绝对路径
     * @param sourceEncoding 源编码名称（如 GBK、ISO-8859-1）
     * @param targetEncoding 目标编码名称（如 UTF-8）
     * @param pattern        文件名匹配模式（支持通配符 * 和 ?），可选，默认匹配所有文件
     * @param includeSubdirs 是否包含子目录中的文件，可选，默认 false
     * @return 转换结果报告
     */
    @Tool(name = "batch_convert_encoding", description = "批量转换文件编码")
    public String batchConvertEncoding(BatchConvertEncodingRequest request) {

        if (request.dirPath() == null || request.dirPath()
                                                .isBlank()) {
            return "错误: 目录路径不能为空";
        }
        if (request.sourceEncoding() == null || request.sourceEncoding()
                                                       .isBlank()) {
            return "错误: 源编码不能为空";
        }
        if (request.targetEncoding() == null || request.targetEncoding()
                                                       .isBlank()) {
            return "错误: 目标编码不能为空";
        }

        LogUtil.info("批量转换编码: 目录={}, {} -> {}", request.dirPath(), request.sourceEncoding(), request.targetEncoding());

        return execute("batchConvertEncoding", () -> {
            // 验证编码是否有效
            if (!Charset.isSupported(request.sourceEncoding())) {
                return "错误: 不支持的源编码 - " + request.sourceEncoding();
            }
            if (!Charset.isSupported(request.targetEncoding())) {
                return "错误: 不支持的目标编码 - " + request.targetEncoding();
            }

            Path dir = Path.of(request.dirPath());
            if (!Files.isDirectory(dir)) {
                return "错误: 路径不是目录 - " + request.dirPath();
            }

            List<Path> files = listFiles(dir, request.pattern(), request.includeSubdirs() != null && request.includeSubdirs());
            if (files.isEmpty()) {
                return "未找到匹配的文件。";
            }

            int successCount = 0;
            int failCount = 0;
            List<String> details = new ArrayList<>();

            for (Path file : files) {
                try {
                    // 以源编码读取文件内容
                    byte[] rawBytes = Files.readAllBytes(file);
                    String content = new String(rawBytes, Charset.forName(request.sourceEncoding()));

                    // 以目标编码写入
                    byte[] newBytes = content.getBytes(Charset.forName(request.targetEncoding()));
                    Files.write(file, newBytes);

                    details.add(String.format("  已转换: %s (%s -> %s)", file.getFileName(), request.sourceEncoding(), request.targetEncoding()));
                    successCount++;
                } catch (IOException e) {
                    details.add(String.format("  转换失败: %s (%s)", file.getFileName(), e.getMessage()));
                    failCount++;
                }
            }

            StringBuilder report = new StringBuilder();
            report.append("=== 批量编码转换报告 ===\n");
            report.append("目录: ")
                  .append(request.dirPath())
                  .append("\n");
            report.append("源编码: ")
                  .append(request.sourceEncoding())
                  .append(", 目标编码: ")
                  .append(request.targetEncoding())
                  .append("\n");
            report.append("扫描文件数: ")
                  .append(files.size())
                  .append("\n");
            report.append("成功: ")
                  .append(successCount)
                  .append(" 个, 失败: ")
                  .append(failCount)
                  .append(" 个\n\n");

            for (String detail : details) {
                report.append(detail)
                      .append("\n");
            }

            LogUtil.info("批量编码转换完成: 成功 {} 个, 失败 {} 个", successCount, failCount);
            return report.toString();
        });
    }

    /**
     * 查找目录中的重复文件（按 MD5 哈希值）。
     * <p>
     * 扫描目录中所有文件，计算每个文件的 MD5 哈希值，
     * 将哈希值相同的文件归为重复文件组。大文件会以流式方式计算 MD5。
     *
     * @param dirPath        目标目录的绝对路径
     * @param pattern        文件名匹配模式（支持通配符 * 和 ?），可选，默认匹配所有文件
     * @param includeSubdirs 是否包含子目录中的文件，可选，默认 false
     * @return 重复文件报告，按重复组列出文件
     */
    @Tool(name = "find_duplicate_files", description = "查找目录中的重复文件（按 MD5 哈希值）")
    public String findDuplicateFiles(@ToolParam(description = "目标目录的绝对路径") String dirPath,
        @ToolParam(description = "文件名匹配模式（支持通配符 * 和 ?），如 *.txt，可选", required = false) String pattern,
        @ToolParam(description = "是否包含子目录中的文件，可选，默认 false", required = false) Boolean includeSubdirs) {

        if (dirPath == null || dirPath.isBlank()) {
            return "错误: 目录路径不能为空";
        }

        LogUtil.info("查找重复文件: 目录={}", dirPath);

        return execute("findDuplicateFiles", () -> {
            Path dir = Path.of(dirPath);
            if (!Files.isDirectory(dir)) {
                return "错误: 路径不是目录 - " + dirPath;
            }

            List<Path> files = listFiles(dir, pattern, includeSubdirs != null && includeSubdirs);
            if (files.isEmpty()) {
                return "未找到匹配的文件。";
            }

            // 按文件大小分组（大小不同的文件不可能是重复的）
            Map<Long, List<Path>> sizeGroups = new LinkedHashMap<>();
            for (Path file : files) {
                try {
                    long size = Files.size(file);
                    sizeGroups.computeIfAbsent(size, k -> new ArrayList<>())
                              .add(file);
                } catch (IOException ignored) {
                    // 跳过无法读取的文件
                }
            }

            // 对大小相同的文件组计算 MD5
            Map<String, List<Path>> hashGroups = new LinkedHashMap<>();
            for (Map.Entry<Long, List<Path>> entry : sizeGroups.entrySet()) {
                if (entry.getValue()
                         .size() < 2) {
                    continue; // 大小唯一的文件不可能是重复的
                }
                for (Path file : entry.getValue()) {
                    try {
                        String md5 = computeMD5(file);
                        hashGroups.computeIfAbsent(md5, k -> new ArrayList<>())
                                  .add(file);
                    } catch (Exception ignored) {
                        // 跳过无法计算哈希的文件
                    }
                }
            }

            // 筛选出有重复的组
            List<Map.Entry<String, List<Path>>> duplicateGroups = hashGroups.entrySet()
                                                                            .stream()
                                                                            .filter(e -> e.getValue()
                                                                                          .size() >= 2)
                                                                            .sorted(Comparator.comparingInt(e -> -e.getValue()
                                                                                                                   .size()))
                                                                            .toList();

            StringBuilder report = new StringBuilder();
            report.append("=== 重复文件查找报告 ===\n");
            report.append("目录: ")
                  .append(dirPath)
                  .append("\n");
            report.append("扫描文件数: ")
                  .append(files.size())
                  .append("\n");

            if (duplicateGroups.isEmpty()) {
                report.append("未发现重复文件。\n");
            } else {
                int totalDupFiles = duplicateGroups.stream()
                                                   .mapToInt(e -> e.getValue()
                                                                   .size())
                                                   .sum();
                report.append("重复组数: ")
                      .append(duplicateGroups.size())
                      .append(", 重复文件总数: ")
                      .append(totalDupFiles)
                      .append("\n\n");

                int groupNum = 1;
                for (Map.Entry<String, List<Path>> group : duplicateGroups) {
                    List<Path> dupFiles = group.getValue();
                    try {
                        long fileSize = Files.size(dupFiles.get(0));
                        report.append("--- 重复组 ")
                              .append(groupNum)
                              .append(" (MD5: ")
                              .append(group.getKey())
                              .append(", 文件大小: ")
                              .append(formatFileSize(fileSize))
                              .append(", ")
                              .append(dupFiles.size())
                              .append(" 个文件) ---\n");

                        for (Path file : dupFiles) {
                            report.append("  ")
                                  .append(file.toAbsolutePath())
                                  .append("\n");
                        }
                        report.append("\n");
                        groupNum++;
                    } catch (IOException ignored) {
                        // 跳过
                    }
                }

                long wastedBytes = duplicateGroups.stream()
                                                  .mapToLong(g -> {
                                                      try {
                                                          long size = Files.size(g.getValue()
                                                                                  .get(0));
                                                          return size * (g.getValue()
                                                                          .size() - 1);
                                                      } catch (IOException e) {
                                                          return 0;
                                                      }
                                                  })
                                                  .sum();
                report.append("可回收空间（删除重复文件后）: ")
                      .append(formatFileSize(wastedBytes))
                      .append("\n");
            }

            LogUtil.info("重复文件查找完成: {} 个重复组", duplicateGroups.size());
            return report.toString();
        });
    }

    // ==================== 内部方法 ====================

    /**
     * 根据模式生成新文件名
     */
    private String generateNewName(String oldName, String mode, String value, int index, int total) {
        return switch (mode.toLowerCase()) {
            case "prefix" -> value + oldName;
            case "suffix" -> {
                int dotIdx = oldName.lastIndexOf('.');
                if (dotIdx > 0) {
                    yield oldName.substring(0, dotIdx) + value + oldName.substring(dotIdx);
                } else {
                    yield oldName + value;
                }
            }
            case "index" -> {
                int dotIdx = oldName.lastIndexOf('.');
                String ext = dotIdx > 0 ? oldName.substring(dotIdx) : "";
                String fmt = "%0" + String.valueOf(total)
                                          .length() + "d";
                yield value + "_" + String.format(fmt, index + 1) + ext;
            }
            case "replace" -> {
                if (value.contains("|")) {
                    String[] parts = value.split("\\|", 2);
                    yield oldName.replace(parts[0], parts.length > 1 ? parts[1] : "");
                } else {
                    yield oldName;
                }
            }
            default -> {
                LogUtil.warn("未知的重命名模式: {}", mode);
                yield null;
            }
        };
    }

    /**
     * 列出目录中匹配模式的文件
     */
    private List<Path> listFiles(Path dir, String pattern, boolean includeSubdirs) throws IOException {
        List<Path> files = new ArrayList<>();
        PathMatcher matcher = createPathMatcher(pattern);

        try (Stream<Path> stream = includeSubdirs ? Files.walk(dir) : Files.list(dir)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> matcher.matches(p.getFileName()))
                  .forEach(files::add);
        }
        return files;
    }

    /**
     * 简单的路径匹配器（支持 * 和 ? 通配符）
     */
    private PathMatcher createPathMatcher(String pattern) {
        if (pattern == null || pattern.isBlank() || "*".equals(pattern) || "*.*".equals(pattern)) {
            return name -> true;
        }
        String regex = pattern.replace(".", "\\.")
                              .replace("*", ".*")
                              .replace("?", ".");
        Pattern p = Pattern.compile("^" + regex + "$", Pattern.CASE_INSENSITIVE);
        return name -> p.matcher(name.toString())
                        .matches();
    }

    /**
     * 计算字符串中指定子串的出现次数
     */
    private int countOccurrences(String text, String search) {
        if (search.isEmpty()) {
            return 0;
        }
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(search, idx)) != -1) {
            count++;
            idx += search.length();
        }
        return count;
    }

    /**
     * 计算文件的 MD5 哈希值（流式读取，支持大文件）
     */
    private String computeMD5(Path file) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] buffer = new byte[8192];
        try (java.io.InputStream in = Files.newInputStream(file)) {
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        }
        return HexFormat.of()
                        .formatHex(md.digest());
    }

    /**
     * 格式化文件大小为可读字符串
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 路径匹配器函数式接口
     */
    @FunctionalInterface
    private interface PathMatcher {

        boolean matches(Path name);
    }
}
