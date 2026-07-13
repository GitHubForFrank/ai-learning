package com.example.mcp.handler;

import com.example.mcp.handler.archive.ArchiveAdvancedHandler;
import com.example.mcp.handler.batch.BatchFileHandler;
import com.example.mcp.handler.chart.ChartHandler;
import com.example.mcp.handler.document.DocumentDiffHandler;
import com.example.mcp.handler.fetch.HttpClientHandler;
import com.example.mcp.handler.i18n.I18nHandler;
import com.example.mcp.handler.image.ImageAdvancedHandler;
import com.example.mcp.handler.json.JsonDataHandler;
import com.example.mcp.handler.media.MediaHandler;
import com.example.mcp.handler.pdf.PdfAdvancedHandler;
import com.example.mcp.handler.ppt.PptAdvancedHandler;
import com.example.mcp.handler.system.SystemInfoHandler;
import com.example.mcp.handler.tool.BarcodeHandler;
import com.example.mcp.handler.tool.CryptoHandler;
import com.example.mcp.handler.tool.DateTimeHandler;
import com.example.mcp.handler.tool.TextToolHandler;
import com.example.mcp.handler.word.WordAdvancedHandler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 确保所有新 Handler 能正确注入 Spring 上下文
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@SpringBootTest
class HandlerInjectionTest {

    @Autowired
    private TextToolHandler textToolHandler;
    @Autowired
    private JsonDataHandler jsonDataHandler;
    @Autowired
    private DateTimeHandler dateTimeHandler;
    @Autowired
    private DocumentDiffHandler documentDiffHandler;
    @Autowired
    private HttpClientHandler httpClientHandler;
    @Autowired
    private ChartHandler chartHandler;
    @Autowired
    private BatchFileHandler batchFileHandler;
    @Autowired
    private ImageAdvancedHandler imageAdvancedHandler;
    @Autowired
    private CryptoHandler cryptoHandler;
    @Autowired
    private SystemInfoHandler systemInfoHandler;
    @Autowired
    private PdfAdvancedHandler pdfAdvancedHandler;
    @Autowired
    private WordAdvancedHandler wordAdvancedHandler;
    @Autowired
    private PptAdvancedHandler pptAdvancedHandler;
    @Autowired
    private MediaHandler mediaHandler;
    @Autowired
    private ArchiveAdvancedHandler archiveAdvancedHandler;
    @Autowired
    private BarcodeHandler barcodeHandler;
    @Autowired
    private I18nHandler i18nHandler;

    @Test
    void testAllHandlersInjected() {
        // 所有 17 个 Handler 都能正确注入
        org.junit.jupiter.api.Assertions.assertNotNull(textToolHandler);
        org.junit.jupiter.api.Assertions.assertNotNull(jsonDataHandler);
        org.junit.jupiter.api.Assertions.assertNotNull(dateTimeHandler);
        org.junit.jupiter.api.Assertions.assertNotNull(documentDiffHandler);
        org.junit.jupiter.api.Assertions.assertNotNull(httpClientHandler);
        org.junit.jupiter.api.Assertions.assertNotNull(chartHandler);
        org.junit.jupiter.api.Assertions.assertNotNull(batchFileHandler);
        org.junit.jupiter.api.Assertions.assertNotNull(imageAdvancedHandler);
        org.junit.jupiter.api.Assertions.assertNotNull(cryptoHandler);
        org.junit.jupiter.api.Assertions.assertNotNull(systemInfoHandler);
        org.junit.jupiter.api.Assertions.assertNotNull(pdfAdvancedHandler);
        org.junit.jupiter.api.Assertions.assertNotNull(wordAdvancedHandler);
        org.junit.jupiter.api.Assertions.assertNotNull(pptAdvancedHandler);
        org.junit.jupiter.api.Assertions.assertNotNull(mediaHandler);
        org.junit.jupiter.api.Assertions.assertNotNull(archiveAdvancedHandler);
        org.junit.jupiter.api.Assertions.assertNotNull(barcodeHandler);
        org.junit.jupiter.api.Assertions.assertNotNull(i18nHandler);
    }
}
