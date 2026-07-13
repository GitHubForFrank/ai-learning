package com.example.mcp.handler.system;

import com.example.mcp.handler.BaseHandler;

import com.example.mcp.util.FormatUtil;
import com.example.mcp.util.LogUtil;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP 系统诊断工具实现，提供系统信息、磁盘空间、环境变量、端口检测和 Java 系统属性查询功能。
 * 基于 JDK 21 内置 API（System、Runtime、ManagementFactory、java.io.File、java.net.Socket）。
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@Service
public class SystemInfoHandler extends BaseHandler {

    /**
     * 获取系统信息，包括操作系统、Java 版本、处理器数、内存使用情况等。
     *
     * @return 系统信息字符串
     */
    @Tool(name = "system_info", description = "获取系统信息。包括操作系统名称/版本/架构、Java版本/供应商、可用处理器数、内存使用情况。")
    public String systemInfo() {
        return execute("systemInfo", () -> {
            Map<String, Object> info = new LinkedHashMap<>();

            // 操作系统信息
            info.put("操作系统名称", System.getProperty("os.name"));
            info.put("操作系统版本", System.getProperty("os.version"));
            info.put("操作系统架构", System.getProperty("os.arch"));
            info.put("用户名称", System.getProperty("user.name"));
            info.put("用户目录", System.getProperty("user.home"));
            info.put("当前工作目录", System.getProperty("user.dir"));
            info.put("文件分隔符", System.getProperty("file.separator"));
            info.put("换行符", System.getProperty("line.separator")
                                     .replace("\r", "\\r")
                                     .replace("\n", "\\n"));

            // Java 信息
            info.put("Java 版本", System.getProperty("java.version"));
            info.put("Java 运行时版本", System.getProperty("java.runtime.version"));
            info.put("Java 供应商", System.getProperty("java.vendor"));
            info.put("Java 供应商 URL", System.getProperty("java.vendor.url"));
            info.put("Java 安装目录", System.getProperty("java.home"));

            // JVM 信息
            RuntimeMXBean runtimeMX = ManagementFactory.getRuntimeMXBean();
            info.put("JVM 名称", runtimeMX.getVmName());
            info.put("JVM 供应商", runtimeMX.getVmVendor());
            info.put("JVM 版本", runtimeMX.getVmVersion());
            info.put("JVM 启动时间", runtimeMX.getStartTime());
            info.put("JVM 运行时间", FormatUtil.formatUptime(runtimeMX.getUptime()));

            // 处理器信息
            Runtime runtime = Runtime.getRuntime();
            info.put("可用处理器数", runtime.availableProcessors() + " 核");

            // 内存信息
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            long usedMemory = totalMemory - freeMemory;
            info.put("最大堆内存", FormatUtil.formatBytes(maxMemory));
            info.put("已分配堆内存", FormatUtil.formatBytes(totalMemory));
            info.put("已使用堆内存", FormatUtil.formatBytes(usedMemory));
            info.put("空闲堆内存", FormatUtil.formatBytes(freeMemory));

            // 内存使用率
            double usagePercent = maxMemory > 0 ? (double) usedMemory / maxMemory * 100 : 0;
            info.put("堆内存使用率", String.format("%.1f%%", usagePercent));

            // OperatingSystemMXBean 扩展信息
            OperatingSystemMXBean osMX = ManagementFactory.getOperatingSystemMXBean();
            info.put("系统平均负载", osMX.getAvailableProcessors() > 0 ? String.format("%.2f", osMX.getSystemLoadAverage()) : "不可用");

            StringBuilder sb = new StringBuilder();
            sb.append("系统信息：\n");
            for (Map.Entry<String, Object> entry : info.entrySet()) {
                sb.append("  ")
                  .append(entry.getKey())
                  .append(": ")
                  .append(entry.getValue())
                  .append("\n");
            }

            LogUtil.info("获取系统信息完成");
            return sb.toString()
                     .trim();
        });
    }

    // --- 2. system_disk_info ---

    /**
     * 获取磁盘空间信息，包括各盘符的总空间、可用空间、已用空间。
     *
     * @param path 指定路径（可选，不传则列出所有根目录的磁盘信息）
     * @return 磁盘空间信息字符串
     */
    @Tool(name = "system_disk_info", description = "获取磁盘空间信息。包括各盘符的总空间、可用空间、已用空间和使用率。")
    public String systemDiskInfo(@ToolParam(description = "指定路径（可选，不传则列出所有根目录）", required = false) String path) {
        return execute("systemDiskInfo", () -> {
            List<File> roots = new ArrayList<>();
            if (path != null && !path.isBlank()) {
                File dir = new File(path);
                if (dir.exists()) {
                    roots.add(dir);
                } else {
                    return "错误: 指定路径不存在: " + path;
                }
            } else {
                File[] fileRoots = File.listRoots();
                if (fileRoots != null) {
                    for (File root : fileRoots) {
                        roots.add(root);
                    }
                }
            }

            if (roots.isEmpty()) {
                return "未找到磁盘信息";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("磁盘空间信息：\n");

            for (File root : roots) {
                long totalSpace = root.getTotalSpace();
                long freeSpace = root.getFreeSpace();
                long usableSpace = root.getUsableSpace();
                long usedSpace = totalSpace - freeSpace;

                sb.append("  盘符: ")
                  .append(root.getPath())
                  .append("\n");
                sb.append("    总空间: ")
                  .append(FormatUtil.formatBytes(totalSpace))
                  .append("\n");
                sb.append("    已用空间: ")
                  .append(FormatUtil.formatBytes(usedSpace))
                  .append("\n");
                sb.append("    可用空间: ")
                  .append(FormatUtil.formatBytes(usableSpace))
                  .append("\n");

                if (totalSpace > 0) {
                    double usagePercent = (double) usedSpace / totalSpace * 100;
                    sb.append("    使用率: ")
                      .append(String.format("%.1f%%", usagePercent))
                      .append("\n");

                    // 添加警告信息
                    if (usagePercent > 90) {
                        sb.append("    警告: 磁盘空间即将用尽！\n");
                    } else if (usagePercent > 75) {
                        sb.append("    提示: 磁盘使用率较高\n");
                    }
                }
                sb.append("\n");
            }

            LogUtil.info("获取磁盘信息完成，共 {} 个盘符", roots.size());
            return sb.toString()
                     .trim();
        });
    }

    // --- 3. system_env_get ---

    /**
     * 获取指定环境变量值或列出所有环境变量。
     *
     * @param name 环境变量名称（可选，不传则列出所有环境变量）
     * @return 环境变量值或列表
     */
    @Tool(name = "system_env_get", description = "获取环境变量值。可获取指定环境变量（如 JAVA_HOME、PATH）或列出所有环境变量。")
    public String systemEnvGet(
        @ToolParam(description = "环境变量名称（可选，如 JAVA_HOME、PATH，不传则列出所有环境变量）", required = false) String name) {
        return execute("systemEnvGet", () -> {
            if (name != null && !name.isBlank()) {
                String value = System.getenv(name.trim());
                if (value == null) {
                    return "环境变量 '" + name + "' 未设置";
                }
                LogUtil.info("获取环境变量: {} = {}", name, value);
                return name + " = " + value;
            }

            // 列出所有环境变量
            Map<String, String> env = System.getenv();
            List<Map.Entry<String, String>> sorted = new ArrayList<>(env.entrySet());
            sorted.sort(Comparator.comparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER));

            StringBuilder sb = new StringBuilder();
            sb.append("所有环境变量（共 ")
              .append(sorted.size())
              .append(" 个）：\n");
            for (Map.Entry<String, String> entry : sorted) {
                sb.append("  ")
                  .append(entry.getKey())
                  .append(" = ")
                  .append(entry.getValue())
                  .append("\n");
            }

            LogUtil.info("列出所有环境变量，共 {} 个", sorted.size());
            return sb.toString()
                     .trim();
        });
    }

    // --- 4. system_port_check ---

    /**
     * 检查指定端口是否被占用（TCP 端口检测）。
     *
     * @param port    端口号
     * @param host    主机地址（默认 localhost）
     * @param timeout 连接超时时间（毫秒，默认 3000）
     * @return 端口占用状态信息
     */
    @Tool(name = "system_port_check", description = "检查指定端口是否被占用。通过 TCP 连接检测端口占用状态。")
    public String systemPortCheck(@ToolParam(description = "端口号") int port,
        @ToolParam(description = "主机地址，默认 localhost", required = false) String host,
        @ToolParam(description = "连接超时时间（毫秒），默认 3000", required = false) Integer timeout) {
        return execute("systemPortCheck", () -> {
            if (port < 1 || port > 65535) {
                return "错误: 端口号必须在 1-65535 之间，当前值: " + port;
            }

            String hostname = (host != null && !host.isBlank()) ? host.trim() : "localhost";
            int timeoutMs = (timeout != null && timeout > 0) ? timeout : 3000;

            boolean occupied = false;
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(hostname, port), timeoutMs);
                occupied = true;
            } catch (Exception e) {
                // 连接失败说明端口未占用
                occupied = false;
            }

            String result;
            if (occupied) {
                result = String.format("端口 %d 已被占用（主机: %s, 超时: %dms）", port, hostname, timeoutMs);
                LogUtil.info("端口检测: {}:{} 已占用", hostname, port);
            } else {
                result = String.format("端口 %d 未被占用（主机: %s, 超时: %dms）", port, hostname, timeoutMs);
                LogUtil.info("端口检测: {}:{} 未占用", hostname, port);
            }
            return result;
        });
    }

    // --- 5. system_properties ---

    /**
     * 获取 Java 系统属性，可获取指定属性或列出所有属性。
     *
     * @param key 系统属性键名（可选，如 java.version、user.dir、file.encoding，不传则列出所有属性）
     * @return 系统属性值或列表
     */
    @Tool(name = "system_properties", description = "获取 Java 系统属性。可获取指定属性（如 java.version、user.dir、file.encoding）或列出所有属性。")
    public String systemProperties(
        @ToolParam(description = "系统属性键名（可选，如 java.version、user.dir、file.encoding，不传则列出所有属性）", required = false) String key) {
        return execute("systemProperties", () -> {
            if (key != null && !key.isBlank()) {
                String value = System.getProperty(key.trim());
                if (value == null) {
                    return "系统属性 '" + key + "' 未设置";
                }
                LogUtil.info("获取系统属性: {} = {}", key, value);
                return key + " = " + value;
            }

            // 列出所有系统属性（按 key 排序）
            java.util.Properties props = System.getProperties();
            List<String> keys = new ArrayList<>();
            for (Object k : props.keySet()) {
                keys.add((String) k);
            }
            keys.sort(String.CASE_INSENSITIVE_ORDER);

            StringBuilder sb = new StringBuilder();
            sb.append("所有 Java 系统属性（共 ")
              .append(keys.size())
              .append(" 个）：\n");
            for (String k : keys) {
                String v = System.getProperty(k);
                // 对敏感信息（如密码相关）进行部分遮蔽
                String displayValue = v;
                if (k.toLowerCase()
                     .contains("password") || k.toLowerCase()
                                               .contains("secret") || k.toLowerCase()
                                                                       .contains("token")) {
                    displayValue = "******";
                }
                sb.append("  ")
                  .append(k)
                  .append(" = ")
                  .append(displayValue)
                  .append("\n");
            }

            LogUtil.info("列出所有 Java 系统属性，共 {} 个", keys.size());
            return sb.toString()
                     .trim();
        });
    }
}
