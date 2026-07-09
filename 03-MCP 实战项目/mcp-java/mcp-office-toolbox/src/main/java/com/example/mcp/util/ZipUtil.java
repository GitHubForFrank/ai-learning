package com.example.mcp.util;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * ZIP 压缩/解压工具类，提供文件和目录的压缩、解压及内容查看功能。
 * 基于 JDK 内置 java.util.zip，无需额外依赖。
 *
 * @author Frank Kang
 * @since 2026-07-09
 */
public final class ZipUtil {

    /** 缓冲区大小：8KB */
    private static final int BUFFER_SIZE = 8192;

    private ZipUtil() {
        // 工具类，禁止实例化
    }

    /**
     * 压缩多个文件或目录到 ZIP 文件
     *
     * @param sourcePaths 源文件/目录路径列表
     * @param zipOutputPath 输出 ZIP 文件路径
     * @throws IOException 压缩失败时抛出
     */
    public static void compress(List<Path> sourcePaths, Path zipOutputPath) throws IOException {
        PathUtil.ensureParentDirectory(zipOutputPath);
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(zipOutputPath)))) {
            for (Path sourcePath : sourcePaths) {
                if (!Files.exists(sourcePath)) {
                    continue;
                }
                String baseName = sourcePath.getFileName().toString();
                if (Files.isDirectory(sourcePath)) {
                    // 递归压缩目录
                    Files.walkFileTree(sourcePath, new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            String entryName = baseName + "/" + sourcePath.relativize(file).toString().replace("\\", "/");
                            addToZip(zos, file, entryName);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                            if (!dir.equals(sourcePath)) {
                                String entryName = baseName + "/" + sourcePath.relativize(dir).toString().replace("\\", "/") + "/";
                                ZipEntry entry = new ZipEntry(entryName);
                                zos.putNextEntry(entry);
                                zos.closeEntry();
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } else {
                    addToZip(zos, sourcePath, baseName);
                }
            }
            zos.finish();
        }
    }

    /**
     * 解压 ZIP 文件到目标目录
     *
     * @param zipFilePath ZIP 文件路径
     * @param destDirPath 目标目录路径
     * @throws IOException 解压失败时抛出
     */
    public static void decompress(Path zipFilePath, Path destDirPath) throws IOException {
        if (!Files.exists(zipFilePath)) {
            throw new IllegalArgumentException("ZIP 文件不存在: " + zipFilePath);
        }
        if (!Files.exists(destDirPath)) {
            Files.createDirectories(destDirPath);
        }

        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipFilePath)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = destDirPath.resolve(entry.getName()).normalize();
                // 安全校验：防止 Zip Slip 攻击
                if (!entryPath.startsWith(destDirPath)) {
                    throw new SecurityException("ZIP 条目路径越权: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    PathUtil.ensureParentDirectory(entryPath);
                    try (OutputStream os = Files.newOutputStream(entryPath)) {
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            os.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    /**
     * 列出 ZIP 文件中的所有条目
     *
     * @param zipFilePath ZIP 文件路径
     * @return 条目信息列表，每项包含名称、大小、是否为目录
     * @throws IOException 读取失败时抛出
     */
    public static List<ZipEntryInfo> listEntries(Path zipFilePath) throws IOException {
        if (!Files.exists(zipFilePath)) {
            throw new IllegalArgumentException("ZIP 文件不存在: " + zipFilePath);
        }

        List<ZipEntryInfo> entries = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipFilePath)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries.add(new ZipEntryInfo(
                        entry.getName(),
                        entry.getSize(),
                        entry.isDirectory(),
                        entry.getCompressedSize(),
                        entry.getTime()));
                zis.closeEntry();
            }
        }
        return entries;
    }

    /**
     * 将单个文件添加到 ZIP 输出流
     */
    private static void addToZip(ZipOutputStream zos, Path file, String entryName) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        entry.setTime(Files.getLastModifiedTime(file).toMillis());
        zos.putNextEntry(entry);
        Files.copy(file, zos);
        zos.closeEntry();
    }

    /**
     * ZIP 条目信息 POJO
     *
     * @param name           条目名称（路径）
     * @param size           原始大小（字节）
     * @param isDirectory    是否为目录
     * @param compressedSize 压缩后大小（字节）
     * @param time           修改时间（毫秒时间戳）
     */
    public record ZipEntryInfo(String name, long size, boolean isDirectory, long compressedSize, long time) {
    }
}