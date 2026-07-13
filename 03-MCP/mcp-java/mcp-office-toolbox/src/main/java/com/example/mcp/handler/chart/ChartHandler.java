package com.example.mcp.handler.chart;

import com.example.mcp.handler.BaseHandler;

import com.example.mcp.pojo.chart.ChartGenerateRequest;
import com.example.mcp.util.LogUtil;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP 图表生成工具实现，基于 Java 2D 生成柱状图、饼图和折线图，输出为 PNG 图片文件。
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@Service
public class ChartHandler extends BaseHandler {

    private static final int DEFAULT_WIDTH = 800;
    private static final int DEFAULT_HEIGHT = 500;
    private static final Font TITLE_FONT = new Font("Microsoft YaHei", Font.BOLD, 18);
    private static final Font LABEL_FONT = new Font("Microsoft YaHei", Font.PLAIN, 12);
    private static final Font LEGEND_FONT = new Font("Microsoft YaHei", Font.PLAIN, 11);
    private static final Font VALUE_FONT = new Font("Microsoft YaHei", Font.BOLD, 12);
    private static final Color[] COLORS = {new Color(66, 133, 244), new Color(219, 68, 55), new Color(244, 180, 0), new Color(15, 157, 88),
        new Color(142, 68, 173), new Color(52, 152, 219), new Color(230, 126, 34), new Color(149, 165, 166), new Color(26, 188, 156),
        new Color(241, 196, 15)};

    /**
     * 解析 JSON 数据字符串。格式: {"labels":["A","B","C"],"values":[10,20,30]}
     */
    @SuppressWarnings("unchecked")
    private ChartData parseData(String dataJson) {
        com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSON.parseObject(dataJson);
        ChartData data = new ChartData();
        data.title = json.getString("title");
        if (data.title == null) {
            data.title = "图表";
        }

        if (json.containsKey("categories")) {
            data.labels = json.getJSONArray("categories")
                              .toList(String.class);
        } else if (json.containsKey("labels")) {
            data.labels = json.getJSONArray("labels")
                              .toList(String.class);
        }

        if (json.containsKey("series")) {
            var seriesArr = json.getJSONArray("series");
            for (int i = 0; i < seriesArr.size(); i++) {
                var s = seriesArr.getJSONObject(i);
                Series series = new Series();
                series.name = s.getString("name");
                series.values = new ArrayList<>();
                var vals = s.getJSONArray("values");
                for (int j = 0; j < vals.size(); j++) {
                    series.values.add(vals.getDoubleValue(j));
                }
                data.series.add(series);
            }
        } else if (json.containsKey("values")) {
            Series series = new Series();
            series.name = json.containsKey("name") ? json.getString("name") : "数据";
            series.values = new ArrayList<>();
            var vals = json.getJSONArray("values");
            for (int j = 0; j < vals.size(); j++) {
                series.values.add(vals.getDoubleValue(j));
            }
            data.series.add(series);
        }

        if (data.labels == null && !data.series.isEmpty()) {
            data.labels = new ArrayList<>();
            int count = data.series.get(0).values.size();
            for (int i = 0; i < count; i++) {
                data.labels.add("项目" + (i + 1));
            }
        }
        return data;
    }

    // --- 1. chart_bar ---

    /**
     * 生成柱状图 PNG 图片。
     *
     * @param request 图表生成请求参数（dataJson + outputPath + width + height）
     * @return 生成结果消息
     */
    @Tool(name = "chart_bar", description = "生成柱状图 PNG 图片，输入 JSON 格式的图表数据。")
    public String chartBar(@ToolParam(description = "图表生成请求参数，包含图表数据JSON、输出路径和尺寸") ChartGenerateRequest request) {
        return execute("chartBar", () -> {
            if (request.dataJson() == null || request.dataJson()
                                                     .isBlank()) {
                return "错误: 图表数据不能为空";
            }
            if (request.outputPath() == null || request.outputPath()
                                                       .isBlank()) {
                return "错误: 输出路径不能为空";
            }

            ChartData data = parseData(request.dataJson());
            int w = request.width() != null && request.width() > 0 ? request.width() : DEFAULT_WIDTH;
            int h = request.height() != null && request.height() > 0 ? request.height() : DEFAULT_HEIGHT;

            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);

            int margin = 60;
            int plotX = margin + 30;
            int plotY = margin;
            int plotW = w - plotX - margin;
            int plotH = h - plotY - margin - 40;

            g.setColor(Color.BLACK);
            g.setFont(TITLE_FONT);
            g.drawString(data.title, w / 2 - g.getFontMetrics()
                                              .stringWidth(data.title) / 2, 30);

            if (data.series.isEmpty() || data.labels == null || data.labels.isEmpty()) {
                g.dispose();
                return "错误: 缺少有效的图表数据";
            }

            Series series = data.series.get(0);
            int n = Math.min(data.labels.size(), series.values.size());
            double maxVal = 0;
            for (int i = 0; i < n; i++) {
                maxVal = Math.max(maxVal, series.values.get(i));
            }
            if (maxVal == 0) {
                maxVal = 1;
            }
            maxVal *= 1.1;

            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(1.5f));
            g.drawLine(plotX, plotY, plotX, plotY + plotH);
            g.drawLine(plotX, plotY + plotH, plotX + plotW, plotY + plotH);

            g.setFont(LABEL_FONT);
            for (int i = 0; i <= 5; i++) {
                double val = maxVal * i / 5;
                int y = (int) (plotY + plotH - (val / maxVal) * plotH);
                g.drawString(String.format("%.0f", val), plotX - 40, y + 5);
                g.setColor(Color.LIGHT_GRAY);
                g.setStroke(new BasicStroke(0.5f));
                g.drawLine(plotX + 1, y, plotX + plotW, y);
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke(1.5f));
            }

            int barW = Math.max(10, plotW / n / 2);
            for (int i = 0; i < n; i++) {
                int barH = (int) (series.values.get(i) / maxVal * plotH);
                int x = plotX + (i + 1) * plotW / (n + 1) - barW / 2;
                int y = plotY + plotH - barH;
                g.setColor(COLORS[i % COLORS.length]);
                g.fillRect(x, y, barW, barH);
                g.setColor(Color.BLACK);
                g.drawRect(x, y, barW, barH);
                g.setFont(VALUE_FONT);
                String valStr = formatValue(series.values.get(i));
                int sw = g.getFontMetrics()
                          .stringWidth(valStr);
                g.drawString(valStr, x + barW / 2 - sw / 2, y - 5);
                g.setFont(LABEL_FONT);
                String label = data.labels.get(i);
                if (label.length() > 8) {
                    label = label.substring(0, 7) + "..";
                }
                sw = g.getFontMetrics()
                      .stringWidth(label);
                g.drawString(label, x + barW / 2 - sw / 2, plotY + plotH + 15);
            }

            g.dispose();
            writeImage(image, request.outputPath());
            LogUtil.info("chartBar 完成: {} -> {}", data.title, request.outputPath());
            return String.format("柱状图已生成: %s (尺寸: %dx%d)", request.outputPath(), w, h);
        });
    }

    // --- 2. chart_pie ---

    /**
     * 生成饼图 PNG 图片。
     *
     * @param request 图表生成请求参数
     * @return 生成结果消息
     */
    @Tool(name = "chart_pie", description = "生成饼图 PNG 图片，输入 JSON 格式的图表数据。")
    public String chartPie(@ToolParam(description = "图表生成请求参数") ChartGenerateRequest request) {
        return execute("chartPie", () -> {
            if (request.dataJson() == null || request.dataJson()
                                                     .isBlank()) {
                return "错误: 图表数据不能为空";
            }
            if (request.outputPath() == null || request.outputPath()
                                                       .isBlank()) {
                return "错误: 输出路径不能为空";
            }

            ChartData data = parseData(request.dataJson());
            int w = request.width() != null && request.width() > 0 ? request.width() : 600;
            int h = request.height() != null && request.height() > 0 ? request.height() : 500;

            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);

            g.setColor(Color.BLACK);
            g.setFont(TITLE_FONT);
            g.drawString(data.title, w / 2 - g.getFontMetrics()
                                              .stringWidth(data.title) / 2, 30);

            if (data.series.isEmpty() || data.labels == null) {
                g.dispose();
                return "错误: 缺少有效的图表数据";
            }

            Series series = data.series.get(0);
            int n = Math.min(data.labels.size(), series.values.size());
            double total = 0;
            for (int i = 0; i < n; i++) {
                total += Math.max(0, series.values.get(i));
            }
            if (total <= 0) {
                g.dispose();
                return "错误: 所有值之和必须大于零";
            }

            int cx = w / 2;
            int cy = h / 2 + 20;
            int radius = Math.min(w, h) / 2 - 80;

            double startAngle = 0;
            for (int i = 0; i < n; i++) {
                double angle = series.values.get(i) / total * 360;
                g.setColor(COLORS[i % COLORS.length]);
                g.fillArc(cx - radius, cy - radius, radius * 2, radius * 2, (int) Math.round(startAngle), (int) Math.ceil(angle));
                g.setColor(Color.BLACK);
                g.drawArc(cx - radius, cy - radius, radius * 2, radius * 2, (int) Math.round(startAngle), (int) Math.ceil(angle));
                startAngle += angle;
            }

            int legendX = w - 130;
            int legendY = 60;
            g.setFont(LEGEND_FONT);
            for (int i = 0; i < n; i++) {
                int y = legendY + i * 20;
                if (y > h - 20) {
                    break;
                }
                g.setColor(COLORS[i % COLORS.length]);
                g.fillRect(legendX, y, 12, 12);
                g.setColor(Color.BLACK);
                g.drawRect(legendX, y, 12, 12);
                String label = data.labels.get(i);
                if (label.length() > 8) {
                    label = label.substring(0, 7) + "..";
                }
                double pct = Math.round(series.values.get(i) / total * 1000) / 10.0;
                g.drawString(label + " (" + pct + "%)", legendX + 16, y + 11);
            }

            g.dispose();
            writeImage(image, request.outputPath());
            LogUtil.info("chartPie 完成: {} -> {}", data.title, request.outputPath());
            return String.format("饼图已生成: %s (尺寸: %dx%d)", request.outputPath(), w, h);
        });
    }

    // --- 3. chart_line ---

    /**
     * 生成折线图 PNG 图片。
     *
     * @param request 图表生成请求参数
     * @return 生成结果消息
     */
    @Tool(name = "chart_line", description = "生成折线图 PNG 图片，输入 JSON 格式的图表数据。")
    public String chartLine(@ToolParam(description = "图表生成请求参数") ChartGenerateRequest request) {
        return execute("chartLine", () -> {
            if (request.dataJson() == null || request.dataJson()
                                                     .isBlank()) {
                return "错误: 图表数据不能为空";
            }
            if (request.outputPath() == null || request.outputPath()
                                                       .isBlank()) {
                return "错误: 输出路径不能为空";
            }

            ChartData data = parseData(request.dataJson());
            int w = request.width() != null && request.width() > 0 ? request.width() : DEFAULT_WIDTH;
            int h = request.height() != null && request.height() > 0 ? request.height() : DEFAULT_HEIGHT;

            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);

            int margin = 60;
            int plotX = margin + 30;
            int plotY = margin;
            int plotW = w - plotX - margin;
            int plotH = h - plotY - margin - 40;

            g.setColor(Color.BLACK);
            g.setFont(TITLE_FONT);
            g.drawString(data.title, w / 2 - g.getFontMetrics()
                                              .stringWidth(data.title) / 2, 30);

            if (data.series.isEmpty() || data.labels == null || data.labels.isEmpty()) {
                g.dispose();
                return "错误: 缺少有效的图表数据";
            }

            double maxVal = 0;
            int n = data.labels.size();
            for (Series series : data.series) {
                for (int i = 0; i < Math.min(n, series.values.size()); i++) {
                    maxVal = Math.max(maxVal, series.values.get(i));
                }
            }
            if (maxVal == 0) {
                maxVal = 1;
            }
            maxVal *= 1.1;

            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(1.5f));
            g.drawLine(plotX, plotY, plotX, plotY + plotH);
            g.drawLine(plotX, plotY + plotH, plotX + plotW, plotY + plotH);

            g.setFont(LABEL_FONT);
            for (int i = 0; i <= 5; i++) {
                double val = maxVal * i / 5;
                int y = (int) (plotY + plotH - (val / maxVal) * plotH);
                g.drawString(String.format("%.0f", val), plotX - 40, y + 5);
                g.setColor(Color.LIGHT_GRAY);
                g.setStroke(new BasicStroke(0.5f));
                g.drawLine(plotX + 1, y, plotX + plotW, y);
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke(1.5f));
            }

            for (int s = 0; s < data.series.size(); s++) {
                Series series = data.series.get(s);
                int points = Math.min(n, series.values.size());
                if (points == 0) {
                    continue;
                }

                g.setColor(COLORS[s % COLORS.length]);
                g.setStroke(new BasicStroke(2.5f));

                int[] xs = new int[points];
                int[] ys = new int[points];
                for (int i = 0; i < points; i++) {
                    xs[i] = plotX + (i + 1) * plotW / (n + 1);
                    ys[i] = (int) (plotY + plotH - (series.values.get(i) / maxVal) * plotH);
                }

                for (int i = 0; i < points - 1; i++) {
                    g.drawLine(xs[i], ys[i], xs[i + 1], ys[i + 1]);
                }

                for (int i = 0; i < points; i++) {
                    g.fillOval(xs[i] - 4, ys[i] - 4, 8, 8);
                }
            }

            g.setColor(Color.BLACK);
            g.setFont(LABEL_FONT);
            for (int i = 0; i < n; i++) {
                int x = plotX + (i + 1) * plotW / (n + 1);
                String label = data.labels.get(i);
                if (label.length() > 8) {
                    label = label.substring(0, 7) + "..";
                }
                int sw = g.getFontMetrics()
                          .stringWidth(label);
                g.drawString(label, x - sw / 2, plotY + plotH + 18);
            }

            if (data.series.size() > 1) {
                g.setFont(LEGEND_FONT);
                int legendY = 38;
                for (int s = 0; s < data.series.size(); s++) {
                    int lx = w - 150;
                    g.setColor(COLORS[s % COLORS.length]);
                    g.fillRect(lx, legendY + s * 16, 10, 10);
                    g.setColor(Color.BLACK);
                    g.drawString(data.series.get(s).name, lx + 14, legendY + s * 16 + 10);
                }
            }

            g.dispose();
            writeImage(image, request.outputPath());
            LogUtil.info("chartLine 完成: {} -> {}", data.title, request.outputPath());
            return String.format("折线图已生成: %s (尺寸: %dx%d)", request.outputPath(), w, h);
        });
    }

    private void writeImage(BufferedImage image, String outputPath) throws Exception {
        Path p = Path.of(outputPath);
        if (p.getParent() != null) {
            Files.createDirectories(p.getParent());
        }
        ImageIO.write(image, "PNG", p.toFile());
    }

    private String formatValue(double val) {
        if (val == (long) val) {
            return String.valueOf((long) val);
        }
        return String.format("%.1f", val);
    }

    /**
     * 内部图表数据结构，包含标题、标签和数据系列。
     */
    static class ChartData {

        String title;
        List<String> labels;
        List<Series> series = new ArrayList<>();
    }

    /**
     * 内部图表数据系列，包含系列名称和数据值列表。
     */
    static class Series {

        String name;
        List<Double> values;
    }
}
