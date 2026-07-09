package com.example.mcp.config;

import com.example.mcp.handler.Context7Handler;
import com.example.mcp.handler.CsvHandler;
import com.example.mcp.handler.ExcelHandler;
import com.example.mcp.handler.FetchHandler;
import com.example.mcp.handler.FileSystemHandler;
import com.example.mcp.handler.MarkdownHandler;
import com.example.mcp.handler.PdfHandler;
import com.example.mcp.handler.PptHandler;
import com.example.mcp.handler.TxtHandler;
import com.example.mcp.handler.WordHandler;
import com.example.mcp.handler.ZipHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class McpConfiguration {

    /**
     * 注册文件系统工具（FileSystemHandler），提供文件读取、写入、编辑、搜索、目录浏览等 MCP 工具功能
     *
     * @param fileSystemHandler 文件系统处理器实例
     * @return ToolCallbackProvider 对象，提供文件系统相关的工具功能
     */
    @Bean
    public ToolCallbackProvider fileSystemTools(FileSystemHandler fileSystemHandler) {
        return MethodToolCallbackProvider.builder()
                                         .toolObjects(fileSystemHandler)
                                         .build();
    }

    /**
     * 注册 Fetch 工具（FetchHandler），提供 URL 抓取、HTML 转 Markdown 等 MCP 工具功能
     *
     * @param fetchHandler Fetch 处理器实例
     * @return ToolCallbackProvider 对象，提供网页抓取相关的工具功能
     */
    @Bean
    public ToolCallbackProvider fetchTools(FetchHandler fetchHandler) {
        return MethodToolCallbackProvider.builder()
                                         .toolObjects(fetchHandler)
                                         .build();
    }

    /**
     * 注册 Excel 工具（ExcelHandler），提供 Excel 读取、写入、格式化、截图等 MCP 工具功能
     *
     * @param excelHandler Excel 处理器实例
     * @return ToolCallbackProvider 对象，提供 Excel 相关的工具功能
     */
    @Bean
    public ToolCallbackProvider excelTools(ExcelHandler excelHandler) {
        return MethodToolCallbackProvider.builder()
                                         .toolObjects(excelHandler)
                                         .build();
    }

    /**
     * 注册 TXT 工具（TxtHandler），提供 TXT 文本文件的创建、读取、写入、查找、替换等 MCP 工具功能
     *
     * @param txtHandler TXT 处理器实例
     * @return ToolCallbackProvider 对象，提供 TXT 相关的工具功能
     */
    @Bean
    public ToolCallbackProvider txtTools(TxtHandler txtHandler) {
        return MethodToolCallbackProvider.builder()
                                         .toolObjects(txtHandler)
                                         .build();
    }

    /**
     * 注册 Markdown 工具（MarkdownHandler），提供 MD 文件的创建、读取、追加、组件生成、修改等 MCP 工具功能
     *
     * @param markdownHandler Markdown 处理器实例
     * @return ToolCallbackProvider 对象，提供 Markdown 相关的工具功能
     */
    @Bean
    public ToolCallbackProvider markdownTools(MarkdownHandler markdownHandler) {
        return MethodToolCallbackProvider.builder()
                                         .toolObjects(markdownHandler)
                                         .build();
    }

    /**
     * 注册 PDF 工具（PdfHandler），提供 PDF 文档的读取、解析、元信息获取和文本转换等 MCP 工具功能
     *
     * @param pdfHandler PDF 处理器实例
     * @return ToolCallbackProvider 对象，提供 PDF 相关的工具功能
     */
    @Bean
    public ToolCallbackProvider pdfTools(PdfHandler pdfHandler) {
        return MethodToolCallbackProvider.builder()
                                         .toolObjects(pdfHandler)
                                         .build();
    }

    /**
     * 注册 Word 工具（WordHandler），提供 Word 文档的创建、读取、写入、修改和保存等 MCP 工具功能
     *
     * @param wordHandler Word 处理器实例
     * @return ToolCallbackProvider 对象，提供 Word 相关的工具功能
     */
    @Bean
    public ToolCallbackProvider wordTools(WordHandler wordHandler) {
        return MethodToolCallbackProvider.builder()
                                         .toolObjects(wordHandler)
                                         .build();
    }

    /**
     * 注册 PPT 工具（PptHandler），提供 PPT 演示文稿的创建、幻灯片管理、文本读取和修改等 MCP 工具功能
     *
     * @param pptHandler PPT 处理器实例
     * @return ToolCallbackProvider 对象，提供 PPT 相关的工具功能
     */
    @Bean
    public ToolCallbackProvider pptTools(PptHandler pptHandler) {
        return MethodToolCallbackProvider.builder()
                                         .toolObjects(pptHandler)
                                         .build();
    }

    /**
     * 注册 ZIP 工具（ZipHandler），提供文件与目录的压缩打包、解压还原及内容查看等 MCP 工具功能
     *
     * @param zipHandler ZIP 处理器实例
     * @return ToolCallbackProvider 对象，提供 ZIP 相关的工具功能
     */
    @Bean
    public ToolCallbackProvider zipTools(ZipHandler zipHandler) {
        return MethodToolCallbackProvider.builder()
                                         .toolObjects(zipHandler)
                                         .build();
    }

    /**
     * 注册 CSV 工具（CsvHandler），提供 CSV 文件的创建、读取、写入、追加及信息查看等 MCP 工具功能
     *
     * @param csvHandler CSV 处理器实例
     * @return ToolCallbackProvider 对象，提供 CSV 相关的工具功能
     */
    @Bean
    public ToolCallbackProvider csvTools(CsvHandler csvHandler) {
        return MethodToolCallbackProvider.builder()
                                         .toolObjects(csvHandler)
                                         .build();
    }

    /**
     * 注册 Context7 工具（Context7Handler），提供库文档解析和查询等 MCP 工具功能
     *
     * @param context7Handler Context7 处理器实例
     * @return ToolCallbackProvider 对象，提供 Context7 相关的工具功能
     */
    @Bean
    public ToolCallbackProvider context7Tools(Context7Handler context7Handler) {
        return MethodToolCallbackProvider.builder()
                                         .toolObjects(context7Handler)
                                         .build();
    }

}