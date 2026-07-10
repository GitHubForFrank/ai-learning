package com.example.mcp.handler;

import com.example.mcp.util.FileValidateUtil;
import com.example.mcp.util.PathUtil;
import com.example.mcp.util.ZipUtil;
import com.example.mcp.util.ZipUtil.ZipEntryInfo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import com.example.mcp.util.LogUtil;
import org.springframework.ai.tool.annotation.Tool;
import com.example.mcp.util.LogUtil;
import org.springframework.ai.tool.annotation.ToolParam;
import com.example.mcp.util.LogUtil;
import org.springframework.stereotype.Service;

/**
 * MCP ZIP 压缩/解压工具实现，提供文件与目录的压缩打包、解压还原及内容查看功能。
 * 基于 JDK 内置 java.util.zip，无需额外依赖。支持递归压缩目录，内置 Zip Slip 安全防护。
 *
 * @author Frank Kang
 * @since 2026-07-09
 */
@Service
public class ZipHandler {

    /**
     * 格式化文件大小为可读字符串
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        }
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    // --- 1. zip_compress ---

    /**
     * 将多个文件或目录压缩打包为一个 ZIP 文件
     */
    @Tool(name = "zip_compress", description = "压缩多个文件或目录为 ZIP 文件。支持递归压缩目录结构。"
        + "Source paths 以逗号分隔，如 \"path1,path2,path3\"。")
    public String zipCompress(@ToolParam(description = "源文件或目录路径，多个以逗号分隔") String sourcePaths,
        @ToolParam(description = "输出的 ZIP 文件路径") String zipOutputPath) {
        try {
            String[] parts = sourcePaths.split(",");
            List<Path> paths = new ArrayList<>();
            for (String part : parts) {
                Path p = PathUtil.resolvePath(part.trim());
                if (Files.exists(p)) {
                    paths.add(p);
                } else {
                    return "错误：源路径不存在 - " + p;
                }
            }

            if (paths.isEmpty()) {
                return "错误：没有有效的源路径";
            }

            Path outputPath = PathUtil.resolvePath(zipOutputPath);
            ZipUtil.compress(paths, outputPath);

            long zipSize = Files.size(outputPath);
            return "压缩成功：" + zipOutputPath + "，文件大小 " + formatSize(zipSize);
        } catch (Exception e) {
            LogUtil.error("zipCompress 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 2. zip_decompress ---

    /**
     * 将 ZIP 文件解压到指定目录
     */
    @Tool(name = "zip_decompress", description = "解压 ZIP 文件到指定目录。内置 Zip Slip 安全防护，防止路径穿越攻击。")
    public String zipDecompress(@ToolParam(description = "ZIP 文件路径") String zipFilePath,
        @ToolParam(description = "解压目标目录路径") String destDirPath) {
        try {
            Path zipPath = FileValidateUtil.validateFile(zipFilePath, ".zip");
            Path destPath = PathUtil.resolvePath(destDirPath);

            ZipUtil.decompress(zipPath, destPath);

            // 统计解压结果
            try (var entries = Files.list(destPath)) {
                long count = entries.count();
                return "解压成功：" + destDirPath + "，共解压到 " + count + " 个顶级条目";
            }
        } catch (Exception e) {
            LogUtil.error("zipDecompress 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 3. zip_list ---

    /**
     * 查看 ZIP 文件中的内容列表
     */
    @Tool(name = "zip_list", description = "列出 ZIP 文件中的所有条目，包含名称、大小、是否目录、压缩大小等信息。")
    public String zipList(@ToolParam(description = "ZIP 文件路径") String zipFilePath) {
        try {
            Path zipPath = FileValidateUtil.validateFile(zipFilePath, ".zip");
            List<ZipEntryInfo> entries = ZipUtil.listEntries(zipPath);

            if (entries.isEmpty()) {
                return "ZIP 文件为空: " + zipFilePath;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("ZIP 文件内容列表: ")
              .append(zipFilePath)
              .append("\n");
            sb.append(String.format("共 %d 个条目\n\n", entries.size()));
            sb.append(String.format("%-50s %-10s %-10s %s\n", "名称", "大小", "压缩后", "类型"));

            long totalSize = 0;
            long totalCompressed = 0;
            for (ZipEntryInfo entry : entries) {
                String type = entry.isDirectory() ? "[DIR]" : "[FILE]";
                sb.append(String.format("%-50s %-10s %-10s %s\n", entry.name()
                                                                       .length() > 48 ? entry.name()
                                                                                             .substring(0, 45) + "..." : entry.name(),
                                        formatSize(entry.size()), formatSize(entry.compressedSize()), type));
                totalSize += entry.size();
                totalCompressed += entry.compressedSize();
            }

            sb.append("\n");
            sb.append("原始总大小: ")
              .append(formatSize(totalSize))
              .append("\n");
            sb.append("压缩后总大小: ")
              .append(formatSize(totalCompressed))
              .append("\n");
            if (totalSize > 0) {
                double ratio = (1 - (double) totalCompressed / totalSize) * 100;
                sb.append(String.format("压缩率: %.1f%%", ratio));
            }

            return sb.toString();
        } catch (Exception e) {
            LogUtil.error("zipList 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 4. zip_compress_directory ---

    /**
     * 便捷方法：快速压缩单个目录
     */
    @Tool(description = "快速压缩单个目录为 ZIP 文件。便捷方法，等同于 zip_compress 传入单个目录。")
    public String zipCompressDirectory(@ToolParam(description = "要压缩的目录路径") String dirPath,
        @ToolParam(description = "输出的 ZIP 文件路径（可选，默认为同目录下同名 .zip）", required = false) String zipOutputPath) {
        try {
            Path dir = FileValidateUtil.validateExists(dirPath);
            if (!Files.isDirectory(dir)) {
                return "错误：路径不是目录 - " + dirPath;
            }

            Path outputPath;
            if (zipOutputPath != null && !zipOutputPath.isBlank()) {
                outputPath = PathUtil.resolvePath(zipOutputPath);
            } else {
                // 默认输出到同目录下同名 .zip
                String dirName = dir.getFileName()
                                    .toString();
                outputPath = dir.getParent()
                                .resolve(dirName + ".zip");
            }

            List<Path> paths = List.of(dir);
            ZipUtil.compress(paths, outputPath);

            long zipSize = Files.size(outputPath);
            return "目录压缩成功：" + dirPath + " → " + outputPath + "，文件大小 " + formatSize(zipSize);
        } catch (Exception e) {
            LogUtil.error("zipCompressDirectory 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }
}