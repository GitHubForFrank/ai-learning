package com.example.mcp.handler;

import com.example.mcp.pojo.EditFileRequest;
import com.example.mcp.pojo.EditItem;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP Filesystem 工具实现，提供文件系统的读取、写入、编辑、搜索、目录浏览等操作。
 * 所有操作仅限在允许的目录范围内执行。
 *
 * @author FrankKang
 * @since 2026-07-09
 */
@Service
public class FileSystemHandler {

    /** 允许访问的基础目录，默认为当前工作目录 */
    private final Path basePath;

    /** 日期时间格式化器 */
    private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public FileSystemHandler() {
        // 优先使用环境变量 MCP_FILESYSTEM_BASE_PATH，否则使用当前工作目录
        String envPath = System.getenv("MCP_FILESYSTEM_BASE_PATH");
        this.basePath = Paths.get(envPath != null ? envPath : System.getProperty("user.dir"))
                .toAbsolutePath()
                .normalize();
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 将用户输入的路径解析为绝对路径，并校验是否在允许的目录范围内
     */
    private Path resolvePath(String userPath) {
        Path resolved = basePath.resolve(userPath).toAbsolutePath().normalize();
        if (!resolved.startsWith(basePath)) {
            throw new SecurityException("路径越权：'" + userPath + "' 不在允许的目录范围内 [" + basePath + "]");
        }
        return resolved;
    }

    /**
     * 格式化文件大小为可读字符串
     */
    private String formatSize(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024)
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * 根据文件扩展名获取 MIME 类型
     */
    private String getMimeType(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".png"))
            return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg"))
            return "image/jpeg";
        if (name.endsWith(".gif"))
            return "image/gif";
        if (name.endsWith(".bmp"))
            return "image/bmp";
        if (name.endsWith(".webp"))
            return "image/webp";
        if (name.endsWith(".svg"))
            return "image/svg+xml";
        if (name.endsWith(".mp3"))
            return "audio/mpeg";
        if (name.endsWith(".wav"))
            return "audio/wav";
        if (name.endsWith(".ogg"))
            return "audio/ogg";
        if (name.endsWith(".mp4"))
            return "video/mp4";
        if (name.endsWith(".pdf"))
            return "application/pdf";
        return "application/octet-stream";
    }

    /**
     * 生成简单的行级 diff（统一差异格式）
     */
    private String generateDiff(String path, String oldContent, String newContent) {
        StringBuilder sb = new StringBuilder();
        sb.append("--- a/").append(path).append("\n");
        sb.append("+++ b/").append(path).append("\n");

        String[] oldLines = oldContent.split("\n", -1);
        String[] newLines = newContent.split("\n", -1);

        // 简单的逐行比较
        int maxLen = Math.max(oldLines.length, newLines.length);
        for (int i = 0; i < maxLen; i++) {
            String oldLine = i < oldLines.length ? oldLines[i] : null;
            String newLine = i < newLines.length ? newLines[i] : null;
            if (oldLine == null && newLine != null) {
                sb.append("+").append(newLine).append("\n");
            } else if (newLine == null && oldLine != null) {
                sb.append("-").append(oldLine).append("\n");
            } else if (!oldLine.equals(newLine)) {
                sb.append("-").append(oldLine).append("\n");
                sb.append("+").append(newLine).append("\n");
            }
        }
        return sb.toString();
    }

    // ==================== MCP 工具方法 ====================

    // --- 1. read_text_file ---

    /**
     * 读取文本文件的完整内容。支持通过 head/tail 参数只读取文件的前 N 行或后 N 行。
     */
    @Tool(description = "Read the complete contents of a file from the file system as text. "
            + "Handles various text encodings and provides detailed error messages if the file cannot be read. "
            + "Use the 'head' parameter to read only the first N lines of a file, or the 'tail' parameter "
            + "to read only the last N lines of a file. Only works within allowed directories.")
    public String readTextFile(
            @ToolParam(description = "Path to the file to read") String path,
            @ToolParam(description = "If provided, returns only the first N lines of the file", required = false) Integer head,
            @ToolParam(description = "If provided, returns only the last N lines of the file", required = false) Integer tail) {
        try {
            Path filePath = resolvePath(path);
            if (!Files.exists(filePath)) {
                return "错误：文件不存在 - " + filePath;
            }
            if (!Files.isRegularFile(filePath)) {
                return "错误：路径不是文件 - " + filePath;
            }
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            if (head != null && head > 0) {
                lines = lines.subList(0, Math.min(head, lines.size()));
            }
            if (tail != null && tail > 0) {
                lines = lines.subList(Math.max(0, lines.size() - tail), lines.size());
            }
            return String.join("\n", lines);
        } catch (SecurityException e) {
            return "安全错误：" + e.getMessage();
        } catch (IOException e) {
            return "读取文件失败：" + e.getMessage();
        }
    }

    // --- 2. read_file (已废弃，功能同 read_text_file) ---

    /**
     * 【已废弃】读取文件内容，建议使用 read_text_file 替代。
     *
     * @deprecated 使用 {@link #readTextFile(String, Integer, Integer)} 替代
     */
    @Deprecated
    @Tool(description = "Read the complete contents of a file as text. DEPRECATED: Use read_text_file instead.")
    public String readFile(
            @ToolParam(description = "Path to the file to read") String path,
            @ToolParam(description = "If provided, returns only the first N lines of the file", required = false) Integer head,
            @ToolParam(description = "If provided, returns only the last N lines of the file", required = false) Integer tail) {
        return readTextFile(path, head, tail);
    }

    // --- 3. read_media_file ---

    /**
     * 读取图片或音频文件，返回 base64 编码数据和 MIME 类型。
     */
    @Tool(description = "Read an image or audio file. Returns the base64 encoded data and MIME type. "
            + "Only works within allowed directories.")
    public String readMediaFile(
            @ToolParam(description = "Path to the media file to read") String path) {
        try {
            Path filePath = resolvePath(path);
            if (!Files.exists(filePath)) {
                return "错误：文件不存在 - " + filePath;
            }
            if (!Files.isRegularFile(filePath)) {
                return "错误：路径不是文件 - " + filePath;
            }
            byte[] bytes = Files.readAllBytes(filePath);
            String base64Data = Base64.getEncoder().encodeToString(bytes);
            String mimeType = getMimeType(filePath);
            return String.format("{\"mimeType\": \"%s\", \"size\": %d, \"data\": \"%s\"}", mimeType, bytes.length,
                    base64Data);
        } catch (SecurityException e) {
            return "安全错误：" + e.getMessage();
        } catch (IOException e) {
            return "读取媒体文件失败：" + e.getMessage();
        }
    }

    // --- 4. read_multiple_files ---

    /**
     * 批量读取多个文件的内容，返回每个文件的路径和内容。
     * 单个文件读取失败不会影响其他文件的读取。
     */
    @Tool(description = "Read the contents of multiple files simultaneously. This is more efficient than "
            + "reading files one by one when you need to analyze or compare multiple files. Each file's content "
            + "is returned with its path as a reference. Failed reads for individual files won't stop the entire "
            + "operation. Only works within allowed directories.")
    public String readMultipleFiles(
            @ToolParam(description = "Array of file paths to read") List<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return "错误：paths 参数不能为空";
        }
        StringBuilder sb = new StringBuilder();
        for (String p : paths) {
            sb.append("=== ").append(p).append(" ===\n");
            try {
                Path filePath = resolvePath(p);
                if (!Files.exists(filePath)) {
                    sb.append("[错误] 文件不存在\n");
                } else if (!Files.isRegularFile(filePath)) {
                    sb.append("[错误] 路径不是文件\n");
                } else {
                    String content = Files.readString(filePath, StandardCharsets.UTF_8);
                    sb.append(content).append("\n");
                }
            } catch (SecurityException e) {
                sb.append("[安全错误] ").append(e.getMessage()).append("\n");
            } catch (IOException e) {
                sb.append("[读取失败] ").append(e.getMessage()).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // --- 5. write_file ---

    /**
     * 创建新文件或完全覆盖已有文件的内容。
     */
    @Tool(description = "Create a new file or completely overwrite an existing file with new content. "
            + "Use with caution as it will overwrite existing files without warning. "
            + "Handles text content with proper encoding. Only works within allowed directories.")
    public String writeFile(
            @ToolParam(description = "Path to the file to write") String path,
            @ToolParam(description = "Content to write to the file") String content) {
        try {
            Path filePath = resolvePath(path);
            // 确保父目录存在
            Path parent = filePath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            boolean existed = Files.exists(filePath);
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
            return existed ? "文件已覆盖写入：" + filePath : "文件已创建：" + filePath;
        } catch (SecurityException e) {
            return "安全错误：" + e.getMessage();
        } catch (IOException e) {
            return "写入文件失败：" + e.getMessage();
        }
    }

    // --- 6. edit_file ---

    /**
     * 对文本文件进行基于行的精确编辑。每次编辑将精确匹配的旧文本替换为新文本。
     * 支持 dryRun 模式预览变更（返回 git-style diff），不实际修改文件。
     */
    @Tool(description = "Make line-based edits to a text file. Each edit replaces exact line sequences "
            + "with new content. Returns a git-style diff showing the changes made. "
            + "Only works within allowed directories.")
    public String editFile(
            @ToolParam(description = "编辑文件请求参数") EditFileRequest request) {
        try {
            Path filePath = resolvePath(request.path());
            if (!Files.exists(filePath)) {
                return "错误：文件不存在 - " + filePath;
            }
            if (!Files.isRegularFile(filePath)) {
                return "错误：路径不是文件 - " + filePath;
            }

            String originalContent = Files.readString(filePath, StandardCharsets.UTF_8);
            String modifiedContent = originalContent;

            for (EditItem edit : request.edits()) {
                String oldText = edit.oldText();
                String newText = edit.newText();
                if (!modifiedContent.contains(oldText)) {
                    return String.format("错误：未找到要替换的文本 '%s'",
                            oldText.length() > 100 ? oldText.substring(0, 100) + "..." : oldText);
                }
                // 只替换第一次出现
                modifiedContent = modifiedContent.replaceFirst(java.util.regex.Pattern.quote(oldText),
                        java.util.regex.Matcher.quoteReplacement(newText));
            }

            String diff = generateDiff(request.path(), originalContent, modifiedContent);

            if (request.dryRun()) {
                return "[DRY RUN 预览] 以下为变更差异，文件未被实际修改：\n" + diff;
            }

            Files.writeString(filePath, modifiedContent, StandardCharsets.UTF_8);
            return "文件已编辑：" + filePath + "\n" + diff;
        } catch (SecurityException e) {
            return "安全错误：" + e.getMessage();
        } catch (IOException e) {
            return "编辑文件失败：" + e.getMessage();
        }
    }

    // --- 7. create_directory ---

    /**
     * 创建目录，支持嵌套创建。如果目录已存在则静默成功。
     */
    @Tool(description = "Create a new directory or ensure a directory exists. Can create multiple "
            + "nested directories in one operation. If the directory already exists, this operation "
            + "will succeed silently. Only works within allowed directories.")
    public String createDirectory(
            @ToolParam(description = "Path to the directory to create") String path) {
        try {
            Path dirPath = resolvePath(path);
            if (Files.exists(dirPath)) {
                if (Files.isDirectory(dirPath)) {
                    return "目录已存在：" + dirPath;
                }
                return "错误：路径已存在但不是目录 - " + dirPath;
            }
            Files.createDirectories(dirPath);
            return "目录已创建：" + dirPath;
        } catch (SecurityException e) {
            return "安全错误：" + e.getMessage();
        } catch (IOException e) {
            return "创建目录失败：" + e.getMessage();
        }
    }

    // --- 8. list_directory ---

    /**
     * 列出指定目录下的所有文件和子目录，使用 [FILE] 和 [DIR] 前缀区分。
     */
    @Tool(description = "Get a detailed listing of all files and directories in a specified path. "
            + "Results clearly distinguish between files and directories with [FILE] and [DIR] prefixes. "
            + "Only works within allowed directories.")
    public String listDirectory(
            @ToolParam(description = "Path to the directory to list") String path) {
        try {
            Path dirPath = resolvePath(path);
            if (!Files.exists(dirPath)) {
                return "错误：目录不存在 - " + dirPath;
            }
            if (!Files.isDirectory(dirPath)) {
                return "错误：路径不是目录 - " + dirPath;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Directory listing for: ").append(dirPath).append("\n");
            try (Stream<Path> entries = Files.list(dirPath)) {
                entries.sorted(Comparator.comparing(Path::getFileName)).forEach(entry -> {
                    if (Files.isDirectory(entry)) {
                        sb.append("[DIR]  ").append(entry.getFileName()).append("\n");
                    } else {
                        sb.append("[FILE] ").append(entry.getFileName()).append("\n");
                    }
                });
            }
            return sb.toString();
        } catch (SecurityException e) {
            return "安全错误：" + e.getMessage();
        } catch (IOException e) {
            return "列出目录失败：" + e.getMessage();
        }
    }

    // --- 9. list_directory_with_sizes ---

    /**
     * 列出指定目录下的所有文件和子目录，包含文件大小，支持按名称或大小排序。
     */
    @Tool(description = "Get a detailed listing of all files and directories in a specified path, "
            + "including sizes. Results clearly distinguish between files and directories with "
            + "[FILE] and [DIR] prefixes. Only works within allowed directories.")
    public String listDirectoryWithSizes(
            @ToolParam(description = "Path to the directory to list") String path,
            @ToolParam(description = "Sort entries by name or size", required = false) String sortBy) {
        try {
            Path dirPath = resolvePath(path);
            if (!Files.exists(dirPath)) {
                return "错误：目录不存在 - " + dirPath;
            }
            if (!Files.isDirectory(dirPath)) {
                return "错误：路径不是目录 - " + dirPath;
            }

            // 收集条目信息
            List<Map.Entry<String, Long>> entries = new ArrayList<>();
            try (Stream<Path> stream = Files.list(dirPath)) {
                stream.forEach(entry -> {
                    String prefix = Files.isDirectory(entry) ? "[DIR]  " : "[FILE] ";
                    String name = prefix + entry.getFileName();
                    long size = 0;
                    try {
                        if (Files.isRegularFile(entry)) {
                            size = Files.size(entry);
                        } else if (Files.isDirectory(entry)) {
                            // 目录大小：递归计算
                            size = calculateDirSize(entry);
                        }
                    } catch (IOException ignored) {
                    }
                    entries.add(new java.util.AbstractMap.SimpleEntry<>(name, size));
                });
            }

            // 排序
            if ("size".equalsIgnoreCase(sortBy)) {
                entries.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
            } else {
                entries.sort(Map.Entry.comparingByKey());
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Directory listing (with sizes) for: ").append(dirPath).append("\n");
            for (Map.Entry<String, Long> entry : entries) {
                sb.append(entry.getKey());
                if (entry.getValue() > 0 || entry.getKey().startsWith("[FILE]")) {
                    sb.append("  (").append(formatSize(entry.getValue())).append(")");
                }
                sb.append("\n");
            }
            return sb.toString();
        } catch (SecurityException e) {
            return "安全错误：" + e.getMessage();
        } catch (IOException e) {
            return "列出目录失败：" + e.getMessage();
        }
    }

    /**
     * 递归计算目录大小
     */
    private long calculateDirSize(Path dir) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .sum();
        }
    }

    // --- 10. directory_tree ---

    /**
     * 递归获取目录树结构，返回 JSON 格式的树形数据，支持排除模式。
     */
    @Tool(description = "Get a recursive tree view of files and directories as a JSON structure. "
            + "Each entry includes 'name', 'type' (file/directory), and 'children' for directories. "
            + "Only works within allowed directories.")
    public String directoryTree(
            @ToolParam(description = "Path to the directory") String path,
            @ToolParam(description = "Glob patterns to exclude from the tree", required = false) List<String> excludePatterns) {
        try {
            Path dirPath = resolvePath(path);
            if (!Files.exists(dirPath)) {
                return "错误：目录不存在 - " + dirPath;
            }
            if (!Files.isDirectory(dirPath)) {
                return "错误：路径不是目录 - " + dirPath;
            }

            List<PathMatcher> matchers = new ArrayList<>();
            if (excludePatterns != null) {
                for (String pattern : excludePatterns) {
                    matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + pattern));
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            buildTreeJson(sb, dirPath, matchers, "  ");
            sb.append("}\n");
            return sb.toString();
        } catch (SecurityException e) {
            return "安全错误：" + e.getMessage();
        } catch (IOException e) {
            return "获取目录树失败：" + e.getMessage();
        }
    }

    /**
     * 递归构建目录树的 JSON 字符串
     */
    private void buildTreeJson(StringBuilder sb, Path dir, List<PathMatcher> matchers, String indent)
            throws IOException {
        sb.append(indent).append("\"name\": \"").append(escapeJson(dir.getFileName().toString())).append("\",\n");
        sb.append(indent).append("\"type\": \"directory\",\n");
        sb.append(indent).append("\"children\": [\n");

        try (Stream<Path> entries = Files.list(dir)) {
            List<Path> sorted = entries.sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .collect(Collectors.toList());
            boolean first = true;
            for (Path entry : sorted) {
                // 检查排除模式
                boolean excluded = false;
                for (PathMatcher matcher : matchers) {
                    if (matcher.matches(entry.getFileName())) {
                        excluded = true;
                        break;
                    }
                }
                if (excluded)
                    continue;

                if (!first) {
                    sb.append(",\n");
                }
                first = false;

                if (Files.isDirectory(entry)) {
                    sb.append(indent).append("  {\n");
                    buildTreeJson(sb, entry, matchers, indent + "    ");
                    sb.append(indent).append("  }");
                } else {
                    sb.append(indent).append("  {\n");
                    sb.append(indent).append("    \"name\": \"").append(escapeJson(entry.getFileName().toString()))
                            .append("\",\n");
                    sb.append(indent).append("    \"type\": \"file\"\n");
                    sb.append(indent).append("  }");
                }
            }
        }
        sb.append("\n").append(indent).append("]\n");
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // --- 11. move_file ---

    /**
     * 移动或重命名文件/目录。如果目标已存在则操作失败。
     */
    @Tool(description = "Move or rename files and directories. Can move files between directories "
            + "and rename them in a single operation. If the destination exists, the operation will fail. "
            + "Both source and destination must be within allowed directories.")
    public String moveFile(
            @ToolParam(description = "Source path of the file or directory") String source,
            @ToolParam(description = "Destination path for the file or directory") String destination) {
        try {
            Path sourcePath = resolvePath(source);
            Path destPath = resolvePath(destination);

            if (!Files.exists(sourcePath)) {
                return "错误：源文件不存在 - " + sourcePath;
            }
            if (Files.exists(destPath)) {
                return "错误：目标已存在，操作失败 - " + destPath;
            }

            // 确保目标父目录存在
            Path destParent = destPath.getParent();
            if (destParent != null && !Files.exists(destParent)) {
                Files.createDirectories(destParent);
            }

            Files.move(sourcePath, destPath, StandardCopyOption.ATOMIC_MOVE);
            return "移动成功：" + sourcePath + " → " + destPath;
        } catch (SecurityException e) {
            return "安全错误：" + e.getMessage();
        } catch (IOException e) {
            return "移动文件失败：" + e.getMessage();
        }
    }

    // --- 12. search_files ---

    /**
     * 按 glob 模式递归搜索文件和目录，返回匹配的完整路径列表。
     */
    @Tool(description = "Recursively search for files and directories matching a pattern. "
            + "The pattern should be a glob-style pattern that matches paths relative to the working directory. "
            + "Use pattern like '*.ext' to match files in current directory, and '**/*.ext' to match files "
            + "in all subdirectories. Returns full paths to all matching items. Only searches within allowed directories.")
    public String searchFiles(
            @ToolParam(description = "Path to the directory to search in") String path,
            @ToolParam(description = "Glob pattern to match (e.g., '*.java', '**/*.xml')") String pattern,
            @ToolParam(description = "Glob patterns to exclude from results", required = false) List<String> excludePatterns) {
        try {
            Path dirPath = resolvePath(path);
            if (!Files.exists(dirPath)) {
                return "错误：目录不存在 - " + dirPath;
            }
            if (!Files.isDirectory(dirPath)) {
                return "错误：路径不是目录 - " + dirPath;
            }

            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            List<PathMatcher> excludeMatchers = new ArrayList<>();
            if (excludePatterns != null) {
                for (String ep : excludePatterns) {
                    excludeMatchers.add(FileSystems.getDefault().getPathMatcher("glob:" + ep));
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("搜索结果 (pattern: ").append(pattern).append("):\n");
            try (Stream<Path> walk = Files.walk(dirPath)) {
                walk.filter(p -> {
                    // 检查排除模式
                    for (PathMatcher em : excludeMatchers) {
                        if (em.matches(p.getFileName())) {
                            return false;
                        }
                    }
                    return true;
                })
                        .filter(p -> matcher.matches(p.getFileName()) || matcher.matches(dirPath.relativize(p)))
                        .forEach(p -> sb.append(p.toString()).append("\n"));
            }
            return sb.toString();
        } catch (SecurityException e) {
            return "安全错误：" + e.getMessage();
        } catch (IOException e) {
            return "搜索文件失败：" + e.getMessage();
        }
    }

    // --- 13. get_file_info ---

    /**
     * 获取文件或目录的详细元数据：大小、创建时间、修改时间、权限、类型等。
     */
    @Tool(description = "Retrieve detailed metadata about a file or directory. Returns comprehensive "
            + "information including size, creation time, last modified time, permissions, and type. "
            + "Only works within allowed directories.")
    public String getFileInfo(
            @ToolParam(description = "Path to the file or directory") String path) {
        try {
            Path filePath = resolvePath(path);
            if (!Files.exists(filePath)) {
                return "错误：文件或目录不存在 - " + filePath;
            }

            BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
            boolean isDirectory = attrs.isDirectory();
            boolean isRegularFile = attrs.isRegularFile();

            StringBuilder sb = new StringBuilder();
            sb.append("文件信息：").append(filePath).append("\n");
            sb.append("  路径:     ").append(filePath).append("\n");
            sb.append("  名称:     ").append(filePath.getFileName()).append("\n");
            sb.append("  类型:     ").append(isDirectory ? "目录" : isRegularFile ? "文件" : "其他").append("\n");
            sb.append("  大小:     ").append(formatSize(attrs.size())).append(" (").append(attrs.size())
                    .append(" bytes)\n");
            sb.append("  创建时间: ").append(DT_FORMATTER.format(attrs.creationTime().toInstant())).append("\n");
            sb.append("  修改时间: ").append(DT_FORMATTER.format(attrs.lastModifiedTime().toInstant())).append("\n");
            sb.append("  访问时间: ").append(DT_FORMATTER.format(attrs.lastAccessTime().toInstant())).append("\n");
            sb.append("  可读:     ").append(Files.isReadable(filePath)).append("\n");
            sb.append("  可写:     ").append(Files.isWritable(filePath)).append("\n");
            sb.append("  可执行:   ").append(Files.isExecutable(filePath)).append("\n");

            if (isDirectory) {
                try (Stream<Path> entries = Files.list(filePath)) {
                    long count = entries.count();
                    sb.append("  子项数量: ").append(count).append("\n");
                }
            } else {
                try {
                    String mimeType = Files.probeContentType(filePath);
                    sb.append("  MIME类型: ").append(mimeType != null ? mimeType : "未知").append("\n");
                } catch (IOException ignored) {
                }
            }
            return sb.toString();
        } catch (SecurityException e) {
            return "安全错误：" + e.getMessage();
        } catch (IOException e) {
            return "获取文件信息失败：" + e.getMessage();
        }
    }

    // --- 14. list_allowed_directories ---

    /**
     * 返回当前服务器允许访问的目录列表。
     */
    @Tool(description = "Returns the list of directories that this server is allowed to access. "
            + "Subdirectories within these allowed directories are also accessible. "
            + "Use this to understand which directories and their nested paths are available "
            + "before trying to access files.")
    public String listAllowedDirectories() {
        return "允许访问的目录：\n  " + basePath.toString();
    }
}