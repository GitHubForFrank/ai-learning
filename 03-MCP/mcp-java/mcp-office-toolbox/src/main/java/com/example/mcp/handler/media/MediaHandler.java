package com.example.mcp.handler.media;

import com.example.mcp.handler.BaseHandler;

import com.example.mcp.util.LogUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP 音视频媒体文件元信息工具，提供音频、视频和通用媒体文件的基本信息获取功能。
 * <p>
 * 注意：由于不依赖外部媒体解析库（如 JAVE2、FFmpeg 等），
 * 本工具主要通过文件扩展名判断媒体类型，通过文件大小估算时长。
 * 比特率、采样率等详细信息无法精确获取，仅提供基于文件大小的粗略估算。
 * 对于精确的媒体元数据，建议使用专业工具（如 FFprobe）。
 * </p>
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@Service
public class MediaHandler extends BaseHandler {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long KB = 1024L;
    private static final long MB = 1024L * KB;
    private static final long GB = 1024L * MB;

    // 常见格式的近似比特率（bps），用于估算时长
    private static final Map<String, Long> AUDIO_BITRATE_ESTIMATE = Map.ofEntries(Map.entry("mp3", 128_000L), Map.entry("wav", 1_411_200L),
                                                                                  Map.entry("flac", 900_000L), Map.entry("aac", 192_000L),
                                                                                  Map.entry("ogg", 192_000L), Map.entry("wma", 128_000L),
                                                                                  Map.entry("m4a", 192_000L), Map.entry("opus", 96_000L),
                                                                                  Map.entry("ape", 800_000L), Map.entry("aiff", 1_411_200L));

    private static final Map<String, Long> VIDEO_BITRATE_ESTIMATE = Map.ofEntries(Map.entry("mp4", 2_500_000L), Map.entry("avi", 5_000_000L),
                                                                                  Map.entry("mkv", 3_000_000L), Map.entry("mov", 4_000_000L),
                                                                                  Map.entry("wmv", 2_000_000L), Map.entry("flv", 1_000_000L),
                                                                                  Map.entry("webm", 2_000_000L), Map.entry("m4v", 2_500_000L),
                                                                                  Map.entry("3gp", 500_000L), Map.entry("ts", 3_000_000L));

    private static final Map<String, String> MEDIA_DESCRIPTIONS = Map.ofEntries(Map.entry("mp3", "MPEG Audio Layer III (MP3)"),
                                                                                Map.entry("wav", "Waveform Audio (WAV)"),
                                                                                Map.entry("flac", "Free Lossless Audio Codec (FLAC)"),
                                                                                Map.entry("aac", "Advanced Audio Coding (AAC)"),
                                                                                Map.entry("ogg", "Ogg Vorbis"),
                                                                                Map.entry("wma", "Windows Media Audio (WMA)"),
                                                                                Map.entry("m4a", "MPEG-4 Audio (M4A)"),
                                                                                Map.entry("opus", "Opus Audio"),
                                                                                Map.entry("ape", "Monkey's Audio (APE)"),
                                                                                Map.entry("aiff", "Audio Interchange File Format (AIFF)"),
                                                                                Map.entry("mp4", "MPEG-4 Video (MP4)"),
                                                                                Map.entry("avi", "Audio Video Interleave (AVI)"),
                                                                                Map.entry("mkv", "Matroska Video (MKV)"),
                                                                                Map.entry("mov", "QuickTime Movie (MOV)"),
                                                                                Map.entry("wmv", "Windows Media Video (WMV)"),
                                                                                Map.entry("flv", "Flash Video (FLV)"),
                                                                                Map.entry("webm", "WebM Video"),
                                                                                Map.entry("m4v", "MPEG-4 Video (M4V)"),
                                                                                Map.entry("3gp", "3GPP Multimedia"),
                                                                                Map.entry("ts", "MPEG Transport Stream (TS)"));

    /**
     * 获取文件的扩展名（小写）
     */
    private String getExtension(Path filePath) {
        String fileName = filePath.getFileName()
                                  .toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1)
                           .toLowerCase(Locale.ROOT);
        }
        return "";
    }

    /**
     * 获取文件的基本信息
     */
    private FileBasicInfo getFileBasicInfo(Path filePath) throws IOException {
        String fileName = filePath.getFileName()
                                  .toString();
        long fileSize = Files.size(filePath);
        String ext = getExtension(filePath);
        String formatDesc = MEDIA_DESCRIPTIONS.getOrDefault(ext, ext.isEmpty() ? "未知格式" : ext.toUpperCase() + " 格式");

        BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
        LocalDateTime lastModified = LocalDateTime.ofInstant(attrs.lastModifiedTime()
                                                                  .toInstant(), ZoneId.systemDefault());
        LocalDateTime creationTime = LocalDateTime.ofInstant(attrs.creationTime()
                                                                  .toInstant(), ZoneId.systemDefault());

        return new FileBasicInfo(fileName, fileSize, ext, formatDesc, lastModified, creationTime);
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long bytes) {
        if (bytes >= GB) {
            return String.format("%.2f GB", bytes / (double) GB);
        } else if (bytes >= MB) {
            return String.format("%.2f MB", bytes / (double) MB);
        } else if (bytes >= KB) {
            return String.format("%.2f KB", bytes / (double) KB);
        } else {
            return bytes + " B";
        }
    }

    /**
     * 估算媒体时长
     */
    private String estimateDuration(long fileSizeBytes, long bitrateBps) {
        if (bitrateBps <= 0) {
            return "无法估算";
        }
        long seconds = (fileSizeBytes * 8) / bitrateBps;
        Duration duration = Duration.ofSeconds(seconds);
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long secs = duration.getSeconds() % 60;
        if (hours > 0) {
            return String.format("%d小时%d分%d秒（估算）", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%d分%d秒（估算）", minutes, secs);
        } else {
            return String.format("%d秒（估算）", secs);
        }
    }

    /**
     * 判断文件是否为音频文件
     */
    private boolean isAudioFile(String extension) {
        return AUDIO_BITRATE_ESTIMATE.containsKey(extension);
    }

    /**
     * 判断文件是否为视频文件
     */
    private boolean isVideoFile(String extension) {
        return VIDEO_BITRATE_ESTIMATE.containsKey(extension);
    }

    /**
     * 获取音频文件的元信息，包括文件名、大小、格式、估算时长、比特率（估算）等。
     * <p>
     * 注意：由于不依赖外部媒体库，比特率和时长为基于文件大小的粗略估算值，
     * 并非从文件头部读取的实际值。如需精确数据请使用 FFprobe 等专业工具。
     * </p>
     *
     * @param fileAbsolutePath 音频文件的绝对路径
     * @return 音频文件的元信息
     */
    @Tool(name = "audio_info", description = "获取音频文件元信息（文件名/大小/格式/估算时长/比特率等）。注意：时长和比特率为估算值。")
    public String audioInfo(@ToolParam(description = "音频文件的绝对路径") String fileAbsolutePath) {
        return execute("audio_info", () -> {
            Path path = Paths.get(fileAbsolutePath);
            if (!Files.exists(path)) {
                return "错误: 文件不存在 - " + fileAbsolutePath;
            }

            String ext = getExtension(path);
            if (!isAudioFile(ext)) {
                return "警告：文件扩展名 '" + ext + "' 不在支持的音频格式列表中。" + "支持格式: " + String.join(", ", AUDIO_BITRATE_ESTIMATE.keySet())
                    + "\n" + "将尝试按音频文件处理，但数据可能不准确。";
            }

            FileBasicInfo info = getFileBasicInfo(path);
            long estimatedBitrate = AUDIO_BITRATE_ESTIMATE.getOrDefault(ext, 128_000L);
            String estimatedDuration = estimateDuration(info.fileSize(), estimatedBitrate);

            StringBuilder sb = new StringBuilder();
            sb.append("========== 音频文件信息 ==========\n");
            sb.append("文件名    : ")
              .append(info.fileName())
              .append("\n");
            sb.append("文件大小  : ")
              .append(formatFileSize(info.fileSize()))
              .append("\n");
            sb.append("格式      : ")
              .append(info.formatDesc())
              .append("\n");
            sb.append("扩展名    : .")
              .append(info.format())
              .append("\n");
            sb.append("估算比特率: ")
              .append(estimatedBitrate / 1000)
              .append(" kbps（估算值）\n");
            sb.append("估算时长  : ")
              .append(estimatedDuration)
              .append("\n");
            sb.append("修改时间  : ")
              .append(info.lastModified()
                          .format(DATE_FMT))
              .append("\n");
            sb.append("创建时间  : ")
              .append(info.creationTime()
                          .format(DATE_FMT))
              .append("\n");
            sb.append("====================================\n");
            sb.append("注意：比特率和时长均为基于文件大小的粗略估算，\n");
            sb.append("      并非从文件头部读取的实际值。\n");
            sb.append("      精确数据请使用 FFprobe 等专业工具。\n");

            LogUtil.info("audioInfo 完成: {}", fileAbsolutePath);
            return sb.toString();
        });
    }

    // --- 1. audio_info ---

    /**
     * 获取视频文件的元信息，包括文件名、大小、格式、估算时长、估算比特率等。
     * <p>
     * 注意：由于不依赖外部媒体库，时长和比特率为基于文件大小的粗略估算值，
     * 分辨率和帧率信息无法获取。如需精确数据请使用 FFprobe 等专业工具。
     * </p>
     *
     * @param fileAbsolutePath 视频文件的绝对路径
     * @return 视频文件的元信息
     */
    @Tool(name = "video_info", description = "获取视频文件元信息（文件名/大小/格式/估算时长等）。注意：时长和比特率为估算值，无法获取分辨率。")
    public String videoInfo(@ToolParam(description = "视频文件的绝对路径") String fileAbsolutePath) {
        return execute("video_info", () -> {
            Path path = Paths.get(fileAbsolutePath);
            if (!Files.exists(path)) {
                return "错误: 文件不存在 - " + fileAbsolutePath;
            }

            String ext = getExtension(path);
            if (!isVideoFile(ext)) {
                return "警告：文件扩展名 '" + ext + "' 不在支持的视频格式列表中。" + "支持格式: " + String.join(", ", VIDEO_BITRATE_ESTIMATE.keySet())
                    + "\n" + "将尝试按视频文件处理，但数据可能不准确。";
            }

            FileBasicInfo info = getFileBasicInfo(path);
            long estimatedBitrate = VIDEO_BITRATE_ESTIMATE.getOrDefault(ext, 2_500_000L);
            String estimatedDuration = estimateDuration(info.fileSize(), estimatedBitrate);

            StringBuilder sb = new StringBuilder();
            sb.append("========== 视频文件信息 ==========\n");
            sb.append("文件名    : ")
              .append(info.fileName())
              .append("\n");
            sb.append("文件大小  : ")
              .append(formatFileSize(info.fileSize()))
              .append("\n");
            sb.append("格式      : ")
              .append(info.formatDesc())
              .append("\n");
            sb.append("扩展名    : .")
              .append(info.format())
              .append("\n");
            sb.append("估算比特率: ")
              .append(String.format("%.1f", estimatedBitrate / 1_000_000.0))
              .append(" Mbps（估算值）\n");
            sb.append("估算时长  : ")
              .append(estimatedDuration)
              .append("\n");
            sb.append("分辨率    : 无法获取（需专业工具）\n");
            sb.append("帧率      : 无法获取（需专业工具）\n");
            sb.append("修改时间  : ")
              .append(info.lastModified()
                          .format(DATE_FMT))
              .append("\n");
            sb.append("创建时间  : ")
              .append(info.creationTime()
                          .format(DATE_FMT))
              .append("\n");
            sb.append("====================================\n");
            sb.append("注意：比特率和时长均为基于文件大小的粗略估算，\n");
            sb.append("      分辨率和帧率信息无法获取。\n");
            sb.append("      精确数据请使用 FFprobe 等专业工具。\n");

            LogUtil.info("videoInfo 完成: {}", fileAbsolutePath);
            return sb.toString();
        });
    }

    // --- 2. video_info ---

    /**
     * 获取通用媒体文件信息，自动判断文件类型（音频/视频/未知），返回基本文件信息。
     * 如果文件是已知的音频或视频格式，会提供估算的比特率和时长。
     *
     * @param fileAbsolutePath 媒体文件的绝对路径
     * @return 媒体文件的基本信息
     */
    @Tool(name = "media_file_info", description = "通用媒体文件信息。自动判断文件类型（音频/视频），返回基本文件信息和估算参数。")
    public String mediaFileInfo(@ToolParam(description = "媒体文件的绝对路径") String fileAbsolutePath) {
        return execute("media_file_info", () -> {
            Path path = Paths.get(fileAbsolutePath);
            if (!Files.exists(path)) {
                return "错误: 文件不存在 - " + fileAbsolutePath;
            }

            FileBasicInfo info = getFileBasicInfo(path);
            String ext = info.format();

            // 判断媒体类型
            String mediaType;
            Long estimatedBitrate = null;
            if (isAudioFile(ext)) {
                mediaType = "音频文件";
                estimatedBitrate = AUDIO_BITRATE_ESTIMATE.getOrDefault(ext, 128_000L);
            } else if (isVideoFile(ext)) {
                mediaType = "视频文件";
                estimatedBitrate = VIDEO_BITRATE_ESTIMATE.getOrDefault(ext, 2_500_000L);
            } else {
                mediaType = "未知媒体类型（非标准音视频扩展名）";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("========== 媒体文件信息 ==========\n");
            sb.append("文件名    : ")
              .append(info.fileName())
              .append("\n");
            sb.append("文件路径  : ")
              .append(fileAbsolutePath)
              .append("\n");
            sb.append("文件大小  : ")
              .append(formatFileSize(info.fileSize()))
              .append(" (")
              .append(info.fileSize())
              .append(" 字节)\n");
            sb.append("媒体类型  : ")
              .append(mediaType)
              .append("\n");
            if (!ext.isEmpty()) {
                sb.append("格式      : ")
                  .append(info.formatDesc())
                  .append("\n");
                sb.append("扩展名    : .")
                  .append(ext)
                  .append("\n");
            } else {
                sb.append("格式      : 无扩展名，无法判断\n");
            }

            if (estimatedBitrate != null) {
                String estimatedDuration = estimateDuration(info.fileSize(), estimatedBitrate);
                sb.append("估算比特率: ")
                  .append(estimatedBitrate / 1000)
                  .append(" kbps（估算值）\n");
                sb.append("估算时长  : ")
                  .append(estimatedDuration)
                  .append("\n");
            }

            sb.append("修改时间  : ")
              .append(info.lastModified()
                          .format(DATE_FMT))
              .append("\n");
            sb.append("创建时间  : ")
              .append(info.creationTime()
                          .format(DATE_FMT))
              .append("\n");

            // 计算文件年龄
            long daysSinceModified = Duration.between(info.lastModified(), LocalDateTime.now())
                                             .toDays();
            sb.append("文件年龄  : ")
              .append(daysSinceModified)
              .append(" 天前修改\n");
            sb.append("====================================\n");

            if (estimatedBitrate != null) {
                sb.append("注意：时长和比特率为基于文件大小的估算值，非实际读取值。\n");
            }

            LogUtil.info("mediaFileInfo 完成，类型: {}，文件: {}", mediaType, fileAbsolutePath);
            return sb.toString();
        });
    }

    // --- 3. media_file_info ---

    /**
     * 获取文件基本信息
     */
    private record FileBasicInfo(String fileName, long fileSize, String format, String formatDesc, LocalDateTime lastModified,
                                 LocalDateTime creationTime) {

    }
}
