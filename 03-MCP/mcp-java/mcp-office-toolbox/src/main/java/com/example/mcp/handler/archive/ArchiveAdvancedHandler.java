package com.example.mcp.handler.archive;

import com.example.mcp.handler.BaseHandler;

import com.example.mcp.util.LogUtil;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP 归档文件高级操作工具，提供文件压缩（ZIP/tar.gz）、解压和归档信息查看功能。
 * ZIP 使用 JDK java.util.zip，TAR 使用 Apache Commons Compress 库。
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@Service
public class ArchiveAdvancedHandler extends BaseHandler {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int BUFFER_SIZE = 8192;
    private static final long KB = 1024L;
    private static final long MB = 1024L * KB;

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long bytes) {
        if (bytes >= MB) {
            return String.format("%.2f MB", bytes / (double) MB);
        } else if (bytes >= KB) {
            return String.format("%.2f KB", bytes / (double) KB);
        } else {
            return bytes + " B";
        }
    }

    /**
     * 确保目标目录存在
     */
    private void ensureParentDir(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    // --- 1. archive_compress ---

    /**
     * 将指定文件或目录压缩为 ZIP 或 tar.gz 格式的归档文件。
     * 如果是目录，则递归压缩目录下所有文件和子目录。
     *
     * @param sourcePath 要压缩的文件或目录的绝对路径
     * @param targetPath 输出归档文件的绝对路径（扩展名决定格式：.zip 或 .tar.gz / .tgz）
     * @return 操作结果消息
     */
    @Tool(name = "archive_compress", description = "压缩文件/目录为 ZIP 或 tar.gz 格式。根据目标文件扩展名自动选择格式。")
    public String archiveCompress(@ToolParam(description = "要压缩的文件或目录的绝对路径") String sourcePath,
        @ToolParam(description = "输出归档文件的绝对路径（扩展名决定格式：.zip 或 .tar.gz/.tgz）") String targetPath) {
        return execute("archiveCompress", () -> {
            Path srcPath = Paths.get(sourcePath);
            if (!Files.exists(srcPath)) {
                return "错误: 源文件/目录不存在 - " + sourcePath;
            }

            Path tgtPath = Paths.get(targetPath);
            ensureParentDir(tgtPath);

            String targetName = tgtPath.getFileName()
                                       .toString()
                                       .toLowerCase();
            if (targetName.endsWith(".tar.gz") || targetName.endsWith(".tgz")) {
                return compressToTarGz(srcPath, tgtPath);
            } else if (targetName.endsWith(".zip")) {
                return compressToZip(srcPath, tgtPath);
            } else {
                return "错误: 不支持的目标格式。请使用 .zip 或 .tar.gz / .tgz 扩展名";
            }
        });
    }

    /**
     * 压缩为 ZIP 格式
     */
    private String compressToZip(Path srcPath, Path targetPath) throws IOException {
        List<Path> fileList = new ArrayList<>();
        if (Files.isDirectory(srcPath)) {
            try (var stream = Files.walk(srcPath)) {
                stream.filter(p -> !Files.isDirectory(p))
                      .forEach(fileList::add);
            }
        } else {
            fileList.add(srcPath);
        }

        int totalFiles = fileList.size();
        long totalOriginalSize = 0;
        for (Path p : fileList) {
            totalOriginalSize += Files.size(p);
        }

        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(targetPath.toFile())))) {
            for (Path file : fileList) {
                String entryName;
                if (Files.isDirectory(srcPath)) {
                    entryName = srcPath.relativize(file)
                                       .toString()
                                       .replace("\\", "/");
                } else {
                    entryName = file.getFileName()
                                    .toString();
                }
                ZipEntry entry = new ZipEntry(entryName);
                entry.setTime(Files.getLastModifiedTime(file)
                                   .toMillis());
                zos.putNextEntry(entry);
                Files.copy(file, zos);
                zos.closeEntry();
            }
        }

        long compressedSize = Files.size(targetPath);
        double ratio = totalOriginalSize > 0 ? (1.0 - (double) compressedSize / totalOriginalSize) * 100 : 0;
        LogUtil.info("archiveCompress ZIP 完成，{} 个文件，原始: {}，压缩后: {}，压缩率: {:.1f}%", totalFiles, formatFileSize(totalOriginalSize),
                     formatFileSize(compressedSize), ratio);
        return String.format("ZIP 压缩完成: %s（共 %d 个文件，原始 %s -> 压缩后 %s，压缩率 %.1f%%）", targetPath, totalFiles,
                             formatFileSize(totalOriginalSize), formatFileSize(compressedSize), ratio);
    }

    /**
     * 压缩为 tar.gz 格式
     */
    private String compressToTarGz(Path srcPath, Path targetPath) throws IOException {
        List<Path> fileList = new ArrayList<>();
        if (Files.isDirectory(srcPath)) {
            try (var stream = Files.walk(srcPath)) {
                stream.filter(p -> !Files.isDirectory(p))
                      .forEach(fileList::add);
            }
        } else {
            fileList.add(srcPath);
        }

        int totalFiles = fileList.size();
        long totalOriginalSize = 0;
        for (Path p : fileList) {
            totalOriginalSize += Files.size(p);
        }

        try (FileOutputStream fos = new FileOutputStream(targetPath.toFile()); BufferedOutputStream bos = new BufferedOutputStream(
            fos); GZIPOutputStream gzos = new GZIPOutputStream(bos); TarArchiveOutputStream tos = new TarArchiveOutputStream(gzos)) {

            tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

            for (Path file : fileList) {
                String entryName;
                if (Files.isDirectory(srcPath)) {
                    entryName = srcPath.relativize(file)
                                       .toString()
                                       .replace("\\", "/");
                } else {
                    entryName = file.getFileName()
                                    .toString();
                }
                TarArchiveEntry entry = new TarArchiveEntry(file.toFile(), entryName);
                tos.putArchiveEntry(entry);
                Files.copy(file, tos);
                tos.closeArchiveEntry();
            }
            tos.finish();
        }

        long compressedSize = Files.size(targetPath);
        double ratio = totalOriginalSize > 0 ? (1.0 - (double) compressedSize / totalOriginalSize) * 100 : 0;
        LogUtil.info("archiveCompress tar.gz 完成，{} 个文件，原始: {}，压缩后: {}，压缩率: {:.1f}%", totalFiles, formatFileSize(totalOriginalSize),
                     formatFileSize(compressedSize), ratio);
        return String.format("tar.gz 压缩完成: %s（共 %d 个文件，原始 %s -> 压缩后 %s，压缩率 %.1f%%）", targetPath, totalFiles,
                             formatFileSize(totalOriginalSize), formatFileSize(compressedSize), ratio);
    }

    // --- 2. archive_decompress ---

    /**
     * 解压归档文件，自动检测格式（ZIP / tar.gz / gzip）。
     * 根据文件扩展名或内容自动判断归档类型并解压到指定目录。
     *
     * @param sourcePath 归档文件的绝对路径（支持 .zip / .tar.gz / .tgz / .gz）
     * @param outputDir  解压输出目录的绝对路径
     * @return 操作结果消息
     */
    @Tool(name = "archive_decompress", description = "解压归档文件到指定目录。自动检测格式：ZIP/tar.gz/gzip。")
    public String archiveDecompress(@ToolParam(description = "归档文件的绝对路径（支持 .zip / .tar.gz / .tgz / .gz）") String sourcePath,
        @ToolParam(description = "解压输出目录的绝对路径") String outputDir) {
        return execute("archiveDecompress", () -> {
            Path srcPath = Paths.get(sourcePath);
            if (!Files.exists(srcPath)) {
                return "错误: 归档文件不存在 - " + sourcePath;
            }

            Path outDir = Paths.get(outputDir);
            if (!Files.exists(outDir)) {
                Files.createDirectories(outDir);
            }

            String fileName = srcPath.getFileName()
                                     .toString()
                                     .toLowerCase();
            if (fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz")) {
                return decompressTarGz(srcPath, outDir);
            } else if (fileName.endsWith(".zip")) {
                return decompressZip(srcPath, outDir);
            } else if (fileName.endsWith(".gz") && !fileName.endsWith(".tar.gz")) {
                return decompressGzip(srcPath, outDir);
            } else {
                return "错误: 无法识别归档格式。支持的文件格式: .zip, .tar.gz, .tgz, .gz";
            }
        });
    }

    /**
     * 解压 ZIP 文件
     */
    private String decompressZip(Path srcPath, Path outDir) throws IOException {
        int fileCount = 0;
        long totalSize = 0;
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(srcPath.toFile())))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    Files.createDirectories(outDir.resolve(entry.getName()));
                    zis.closeEntry();
                    continue;
                }
                Path outputPath = outDir.resolve(entry.getName());
                ensureParentDir(outputPath);
                Files.copy(zis, outputPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                totalSize += Files.size(outputPath);
                fileCount++;
                zis.closeEntry();
            }
        }
        LogUtil.info("archiveDecompress ZIP 完成，解压 {} 个文件，总大小: {}，输出: {}", fileCount, formatFileSize(totalSize), outDir);
        return String.format("ZIP 解压完成: 共 %d 个文件（%s）-> %s", fileCount, formatFileSize(totalSize), outDir);
    }

    /**
     * 解压 tar.gz 文件
     */
    private String decompressTarGz(Path srcPath, Path outDir) throws IOException {
        int fileCount = 0;
        long totalSize = 0;
        try (FileInputStream fis = new FileInputStream(srcPath.toFile()); BufferedInputStream bis = new BufferedInputStream(
            fis); GzipCompressorInputStream gzis = new GzipCompressorInputStream(bis); TarArchiveInputStream tis = new TarArchiveInputStream(gzis)) {

            TarArchiveEntry entry;
            while ((entry = tis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    Files.createDirectories(outDir.resolve(entry.getName()));
                    continue;
                }
                Path outputPath = outDir.resolve(entry.getName());
                ensureParentDir(outputPath);
                Files.copy(tis, outputPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                totalSize += Files.size(outputPath);
                fileCount++;
            }
        }
        LogUtil.info("archiveDecompress tar.gz 完成，解压 {} 个文件，总大小: {}，输出: {}", fileCount, formatFileSize(totalSize), outDir);
        return String.format("tar.gz 解压完成: 共 %d 个文件（%s）-> %s", fileCount, formatFileSize(totalSize), outDir);
    }

    /**
     * 解压单个 gzip 文件
     */
    private String decompressGzip(Path srcPath, Path outDir) throws IOException {
        String fileName = srcPath.getFileName()
                                 .toString();
        // 去掉 .gz 扩展名
        String outputName = fileName.endsWith(".gz") ? fileName.substring(0, fileName.length() - 3) : fileName + ".decompressed";
        Path outputPath = outDir.resolve(outputName);

        try (FileInputStream fis = new FileInputStream(srcPath.toFile()); GZIPInputStream gzis = new GZIPInputStream(
            fis); FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = gzis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        }

        long outputSize = Files.size(outputPath);
        LogUtil.info("archiveDecompress GZIP 完成，输出: {} ({}), 解压到: {}", outputName, formatFileSize(outputSize), outDir);
        return String.format("GZIP 解压完成: %s (%s) -> %s", outputName, formatFileSize(outputSize), outDir);
    }

    // --- 3. archive_info ---

    /**
     * 查看归档文件的信息，包括格式、文件列表、原始大小、压缩后大小和压缩率。
     * 支持 ZIP / tar.gz / gzip 格式的归档文件。
     *
     * @param fileAbsolutePath 归档文件的绝对路径
     * @return 归档文件的详细信息
     */
    @Tool(name = "archive_info", description = "查看归档文件信息（格式/文件列表/大小/压缩率）。支持 ZIP/tar.gz/gzip 格式。")
    public String archiveInfo(@ToolParam(description = "归档文件的绝对路径") String fileAbsolutePath) {
        return execute("archiveInfo", () -> {
            Path path = Paths.get(fileAbsolutePath);
            if (!Files.exists(path)) {
                return "错误: 归档文件不存在 - " + fileAbsolutePath;
            }

            String fileName = path.getFileName()
                                  .toString()
                                  .toLowerCase();
            if (fileName.endsWith(".zip")) {
                return getZipInfo(path);
            } else if (fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz")) {
                return getTarGzInfo(path);
            } else if (fileName.endsWith(".gz") && !fileName.endsWith(".tar.gz")) {
                return getGzipInfo(path);
            } else {
                return "错误: 无法识别归档格式。支持的文件格式: .zip, .tar.gz, .tgz, .gz";
            }
        });
    }

    /**
     * 获取 ZIP 文件信息
     */
    private String getZipInfo(Path path) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("========== 归档文件信息 ==========\n");
        sb.append("文件名    : ")
          .append(path.getFileName())
          .append("\n");
        sb.append("格式      : ZIP\n");
        sb.append("文件大小  : ")
          .append(formatFileSize(Files.size(path)))
          .append("\n");

        long compressedSize = Files.size(path);
        long totalOriginalSize = 0;
        int fileCount = 0;
        int dirCount = 0;
        List<String> fileList = new ArrayList<>();

        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(path.toFile())))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    dirCount++;
                } else {
                    fileCount++;
                    totalOriginalSize += entry.getSize();
                    if (fileList.size() < 50) {
                        fileList.add(String.format("  %s (%s)", entry.getName(), formatFileSize(entry.getSize())));
                    }
                }
                zis.closeEntry();
            }
        }

        double ratio = totalOriginalSize > 0 ? (1.0 - (double) compressedSize / totalOriginalSize) * 100 : 0;
        sb.append("文件数    : ")
          .append(fileCount)
          .append(" 个文件，")
          .append(dirCount)
          .append(" 个目录\n");
        sb.append("原始大小  : ")
          .append(formatFileSize(totalOriginalSize))
          .append("\n");
        sb.append("压缩率    : ")
          .append(String.format("%.1f%%", ratio))
          .append("\n");
        sb.append("------------------------------------\n");
        sb.append("文件列表（最多显示50个）:\n");
        for (String f : fileList) {
            sb.append(f)
              .append("\n");
        }
        if (fileCount > 50) {
            sb.append("  ... 还有 ")
              .append(fileCount - 50)
              .append(" 个文件未显示\n");
        }
        sb.append("====================================\n");

        LogUtil.info("archiveInfo ZIP 完成: {}, {} 个文件", path.getFileName(), fileCount);
        return sb.toString();
    }

    /**
     * 获取 tar.gz 文件信息
     */
    private String getTarGzInfo(Path path) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("========== 归档文件信息 ==========\n");
        sb.append("文件名    : ")
          .append(path.getFileName())
          .append("\n");
        sb.append("格式      : tar.gz (GZIP 压缩的 TAR)\n");
        sb.append("文件大小  : ")
          .append(formatFileSize(Files.size(path)))
          .append("\n");

        long compressedSize = Files.size(path);
        long totalOriginalSize = 0;
        int fileCount = 0;
        int dirCount = 0;
        List<String> fileList = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(path.toFile()); BufferedInputStream bis = new BufferedInputStream(
            fis); GzipCompressorInputStream gzis = new GzipCompressorInputStream(bis); TarArchiveInputStream tis = new TarArchiveInputStream(gzis)) {

            TarArchiveEntry entry;
            while ((entry = tis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    dirCount++;
                } else {
                    fileCount++;
                    totalOriginalSize += entry.getSize();
                    if (fileList.size() < 50) {
                        fileList.add(String.format("  %s (%s)", entry.getName(), formatFileSize(entry.getSize())));
                    }
                }
            }
        }

        double ratio = totalOriginalSize > 0 ? (1.0 - (double) compressedSize / totalOriginalSize) * 100 : 0;
        sb.append("文件数    : ")
          .append(fileCount)
          .append(" 个文件，")
          .append(dirCount)
          .append(" 个目录\n");
        sb.append("原始大小  : ")
          .append(formatFileSize(totalOriginalSize))
          .append("\n");
        sb.append("压缩率    : ")
          .append(String.format("%.1f%%", ratio))
          .append("\n");
        sb.append("------------------------------------\n");
        sb.append("文件列表（最多显示50个）:\n");
        for (String f : fileList) {
            sb.append(f)
              .append("\n");
        }
        if (fileCount > 50) {
            sb.append("  ... 还有 ")
              .append(fileCount - 50)
              .append(" 个文件未显示\n");
        }
        sb.append("====================================\n");

        LogUtil.info("archiveInfo tar.gz 完成: {}, {} 个文件", path.getFileName(), fileCount);
        return sb.toString();
    }

    /**
     * 获取 gzip 文件信息
     */
    private String getGzipInfo(Path path) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("========== 归档文件信息 ==========\n");
        sb.append("文件名    : ")
          .append(path.getFileName())
          .append("\n");
        sb.append("格式      : GZIP 单文件压缩\n");
        sb.append("文件大小  : ")
          .append(formatFileSize(Files.size(path)))
          .append("\n");

        long compressedSize = Files.size(path);

        // 尝试从 gzip 尾部读取原始大小
        long originalSize = 0;
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(path.toFile(), "r")) {
            raf.seek(raf.length() - 4);
            originalSize = raf.readInt() & 0xFFFFFFFFL;
        } catch (Exception e) {
            originalSize = compressedSize * 2; // 估算
        }

        double ratio = originalSize > 0 ? (1.0 - (double) compressedSize / originalSize) * 100 : 0;
        sb.append("原始大小  : ")
          .append(formatFileSize(originalSize))
          .append("\n");
        sb.append("压缩率    : ")
          .append(String.format("%.1f%%", ratio))
          .append("\n");
        sb.append("------------------------------------\n");
        String originalName = path.getFileName()
                                  .toString();
        if (originalName.endsWith(".gz")) {
            originalName = originalName.substring(0, originalName.length() - 3);
        }
        sb.append("原始文件  : ")
          .append(originalName)
          .append("（可能）\n");
        sb.append("====================================\n");

        LogUtil.info("archiveInfo GZIP 完成: {}", path.getFileName());
        return sb.toString();
    }
}
