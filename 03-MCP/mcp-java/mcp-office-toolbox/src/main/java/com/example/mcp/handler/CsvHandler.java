package com.example.mcp.handler;

import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.text.csv.CsvWriter;
import com.example.mcp.util.FileValidateUtil;
import com.example.mcp.util.PathUtil;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP CSV 文件专属操作工具实现。
 * 提供 CSV 文件的创建、读取、写入、元信息查看及与 Excel 的互转功能。
 * 基于 Hutool CSV 工具实现，支持自定义分隔符和字符编码。
 *
 * @author Frank Kang
 * @since 2026-07-09
 */
@Service
public class CsvHandler {

    private static final Logger log = LoggerFactory.getLogger(CsvHandler.class);

    // --- 1. csv_create ---

    /**
     * 新建空白 CSV 文件
     */
    @Tool(name = "csv_create", description = "创建空白 CSV 文件。如果文件已存在则覆盖为空文件。可选指定表头行。")
    public String csvCreate(@ToolParam(description = "CSV 文件路径") String filePath,
        @ToolParam(description = "逗号分隔的表头列名（可选，如 \"姓名,年龄,城市\"）", required = false) String headers) {
        try {
            Path path = PathUtil.resolvePath(filePath);
            PathUtil.ensureParentDirectory(path);

            if (headers != null && !headers.isBlank()) {
                // 写入表头
                CsvWriter writer = CsvUtil.getWriter(path.toFile(), StandardCharsets.UTF_8);
                writer.write(headers.split(","));
                writer.close();
                return "CSV 文件已创建（含表头）: " + path;
            } else {
                Files.writeString(path, "", StandardCharsets.UTF_8);
                return "空白 CSV 文件已创建: " + path;
            }
        } catch (Exception e) {
            log.error("csvCreate 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 2. csv_read ---

    /**
     * 读取 CSV 文件全部内容
     */
    @Tool(name = "csv_read", description = "读取 CSV 文件全部内容，返回 JSON 格式的行列数据。支持自定义分隔符和字符编码。")
    public String csvRead(@ToolParam(description = "CSV 文件路径") String filePath,
        @ToolParam(description = "列分隔符（默认为逗号）", required = false) String delimiter,
        @ToolParam(description = "字符编码（默认为 UTF-8）", required = false) String encoding,
        @ToolParam(description = "是否包含表头行（默认为 true）", required = false) Boolean hasHeader,
        @ToolParam(description = "限制读取行数（可选，不指定则读取全部）", required = false) Integer limit) {
        try {
            Path path = FileValidateUtil.validateFile(filePath, ".csv");
            Charset charset = (encoding != null && !encoding.isBlank()) ? Charset.forName(encoding) : StandardCharsets.UTF_8;
            boolean header = hasHeader == null || hasHeader;

            CsvReader reader = CsvUtil.getReader();
            // 设置分隔符
            if (delimiter != null && !delimiter.isBlank()) {
                reader.setFieldSeparator(delimiter.charAt(0));
            }

            CsvData data = reader.read(path.toFile(), charset);
            List<List<String>> rows = new ArrayList<>();

            // 确定读取范围
            int startRow = header ? 1 : 0;
            int totalRows = data.getRowCount();
            int endRow = (limit != null && limit > 0) ? Math.min(startRow + limit, totalRows) : totalRows;

            for (int i = startRow; i < endRow; i++) {
                rows.add(new ArrayList<>(data.getRow(i)
                                             .getRawList()));
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("filePath", filePath);
            result.put("totalRows", totalRows);
            result.put("returnedRows", rows.size());

            if (header && data.getRowCount() > 0) {
                result.put("headers", data.getRow(0)
                                          .getRawList());
            }
            result.put("data", rows);

            return com.alibaba.fastjson2.JSON.toJSONString(result);
        } catch (Exception e) {
            log.error("csvRead 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 3. csv_read_headers ---

    /**
     * 仅读取 CSV 表头
     */
    @Tool(name = "csv_read_headers", description = "仅读取 CSV 文件的表头行（第一行），返回列名列表。")
    public String csvReadHeaders(@ToolParam(description = "CSV 文件路径") String filePath,
        @ToolParam(description = "列分隔符（默认为逗号）", required = false) String delimiter) {
        try {
            Path path = FileValidateUtil.validateFile(filePath, ".csv");

            CsvReader reader = CsvUtil.getReader();
            if (delimiter != null && !delimiter.isBlank()) {
                reader.setFieldSeparator(delimiter.charAt(0));
            }

            CsvData data = reader.read(path.toFile(), StandardCharsets.UTF_8);
            if (data.getRowCount() == 0) {
                return "CSV 文件为空，无表头: " + filePath;
            }

            List<String> headers = data.getRow(0)
                                       .getRawList();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("filePath", filePath);
            result.put("columnCount", headers.size());
            result.put("headers", headers);

            return com.alibaba.fastjson2.JSON.toJSONString(result);
        } catch (Exception e) {
            log.error("csvReadHeaders 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 4. csv_write ---

    /**
     * 写入数据到 CSV 文件（覆盖写入）
     */
    @Tool(name = "csv_write", description = "将数据写入 CSV 文件（覆盖写入）。支持自定义分隔符。"
        + "Data 格式：每行数据用换行分隔，每行内列用逗号分隔，如 \"张三,25,北京\\n李四,30,上海\"。")
    public String csvWrite(@ToolParam(description = "CSV 文件路径") String filePath,
        @ToolParam(description = "数据内容，每行换行分隔，行内列逗号分隔") String data,
        @ToolParam(description = "逗号分隔的表头列名（可选）", required = false) String headers,
        @ToolParam(description = "列分隔符（默认为逗号）", required = false) String delimiter) {
        try {
            Path path = PathUtil.resolvePath(filePath);
            PathUtil.ensureParentDirectory(path);

            CsvWriter writer = CsvUtil.getWriter(path.toFile(), StandardCharsets.UTF_8);

            // 写入表头
            if (headers != null && !headers.isBlank()) {
                writer.write(headers.split(","));
            }

            // 写入数据行
            String[] rows = data.split("\n");
            for (String row : rows) {
                String trimmed = row.trim();
                if (!trimmed.isEmpty()) {
                    // 使用自定义分隔符或逗号分割
                    String sep = (delimiter != null && !delimiter.isBlank()) ? delimiter : ",";
                    writer.write(trimmed.split(sep));
                }
            }

            writer.close();
            return "CSV 文件写入成功: " + path;
        } catch (Exception e) {
            log.error("csvWrite 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 5. csv_append ---

    /**
     * 追加数据到 CSV 文件末尾
     */
    @Tool(name = "csv_append", description = "追加数据行到 CSV 文件末尾。如果文件不存在则创建。")
    public String csvAppend(@ToolParam(description = "CSV 文件路径") String filePath,
        @ToolParam(description = "要追加的数据行，每行换行分隔，行内列逗号分隔") String data) {
        try {
            Path path = PathUtil.resolvePath(filePath);
            PathUtil.ensureParentDirectory(path);

            boolean exists = Files.exists(path) && Files.size(path) > 0;
            CsvWriter writer = CsvUtil.getWriter(path.toFile(), StandardCharsets.UTF_8, true);

            String[] rows = data.split("\n");
            for (String row : rows) {
                String trimmed = row.trim();
                if (!trimmed.isEmpty()) {
                    writer.write(trimmed.split(","));
                }
            }

            writer.close();
            return "CSV 数据追加成功: " + path;
        } catch (Exception e) {
            log.error("csvAppend 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 6. csv_info ---

    /**
     * 获取 CSV 文件基本信息
     */
    @Tool(name = "csv_info", description = "获取 CSV 文件的基本信息：行数、列数、表头、文件大小等。")
    public String csvInfo(@ToolParam(description = "CSV 文件路径") String filePath,
        @ToolParam(description = "列分隔符（默认为逗号）", required = false) String delimiter) {
        try {
            Path path = FileValidateUtil.validateFile(filePath, ".csv");

            CsvReader reader = CsvUtil.getReader();
            if (delimiter != null && !delimiter.isBlank()) {
                reader.setFieldSeparator(delimiter.charAt(0));
            }

            CsvData data = reader.read(path.toFile(), StandardCharsets.UTF_8);
            long fileSize = Files.size(path);

            Map<String, Object> info = new LinkedHashMap<>();
            info.put("filePath", filePath);
            info.put("fileSize", fileSize);
            info.put("totalRows", data.getRowCount());
            info.put("totalColumns", data.getRowCount() > 0 ? data.getRow(0)
                                                                  .size() : 0);
            if (data.getRowCount() > 0) {
                info.put("headers", data.getRow(0)
                                        .getRawList());
            }

            StringBuilder sb = new StringBuilder();
            sb.append("CSV 文件信息：\n");
            for (Map.Entry<String, Object> entry : info.entrySet()) {
                sb.append("  ")
                  .append(entry.getKey())
                  .append(": ")
                  .append(entry.getValue())
                  .append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("csvInfo 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }
}