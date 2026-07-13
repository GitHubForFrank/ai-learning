package com.example.mcp.handler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.mcp.handler.chart.ChartHandler;
import com.example.mcp.pojo.chart.ChartGenerateRequest;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * ChartHandler 单元测试
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@SpringBootTest
class ChartHandlerTest {

    @TempDir
    Path tempDir;
    @Autowired
    private ChartHandler chartHandler;

    @Test
    void testChartBar() {
        String data = "{\"title\":\"测试柱状图\",\"labels\":[\"A\",\"B\",\"C\"],\"values\":[10,20,30]}";
        ChartGenerateRequest req = new ChartGenerateRequest(data, tempDir.resolve("bar.png")
                                                                         .toString(), 400, 300);
        String result = chartHandler.chartBar(req);
        assertNotNull(result);
        assertTrue(result.contains("柱状图"));
    }

    @Test
    void testChartPie() {
        String data = "{\"title\":\"测试饼图\",\"labels\":[\"苹果\",\"香蕉\",\"橙子\"],\"values\":[30,25,45]}";
        ChartGenerateRequest req = new ChartGenerateRequest(data, tempDir.resolve("pie.png")
                                                                         .toString(), 400, 300);
        String result = chartHandler.chartPie(req);
        assertNotNull(result);
        assertTrue(result.contains("饼图"));
    }

    @Test
    void testChartLine() {
        String data = "{\"title\":\"测试折线图\",\"labels\":[\"1月\",\"2月\",\"3月\"],\"values\":[10,20,15]}";
        ChartGenerateRequest req = new ChartGenerateRequest(data, tempDir.resolve("line.png")
                                                                         .toString(), 400, 300);
        String result = chartHandler.chartLine(req);
        assertNotNull(result);
        assertTrue(result.contains("折线图"));
    }
}
