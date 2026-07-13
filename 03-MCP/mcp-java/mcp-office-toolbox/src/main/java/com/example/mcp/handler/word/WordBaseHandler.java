package com.example.mcp.handler.word;

import com.example.mcp.handler.BaseHandler;
import com.example.mcp.util.FileValidateUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

/**
 * MCP Word 文档操作 Handler 的抽象基类，提供 Word 文档校验、打开、保存等公共方法。
 * <p>
 * 供 {@link WordHandler} 和 {@link WordAdvancedHandler} 复用，避免重复代码。
 * </p>
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
public abstract class WordBaseHandler extends BaseHandler {

    /**
     * 校验 Word 文件路径是否存在且为 .docx 格式
     *
     * @param fileAbsolutePath 文件绝对路径
     * @return 解析后的 Path 对象
     */
    protected Path validateDocxFile(String fileAbsolutePath) {
        return FileValidateUtil.validateFile(fileAbsolutePath, ".docx");
    }

    /**
     * 打开 Word 文档
     * <p>
     * 调用方必须使用 try-with-resources 管理返回的 XWPFDocument 对象。
     * </p>
     *
     * @param filePath 文件路径
     * @return XWPFDocument 实例
     * @throws IOException 如果文件读取失败
     */
    protected XWPFDocument openDocument(Path filePath) throws IOException {
        try (InputStream is = Files.newInputStream(filePath)) {
            return new XWPFDocument(is);
        }
    }

    /**
     * 保存 Word 文档
     *
     * @param document 文档实例
     * @param filePath 目标文件路径
     * @throws IOException 如果写入失败
     */
    protected void saveDocument(XWPFDocument document, Path filePath) throws IOException {
        try (OutputStream os = Files.newOutputStream(filePath)) {
            document.write(os);
        }
    }

    /**
     * 遍历文档中所有段落和表格中的所有 Run，对每个 Run 执行指定的操作。
     * <p>
     * 遍历顺序：先遍历文档正文所有段落，再遍历所有表格中的所有单元格中的所有段落。
     * 供 {@link WordHandler} 和 {@link WordAdvancedHandler} 复用，避免重复的嵌套循环代码。
     * </p>
     *
     * @param document Word 文档对象
     * @param consumer 对每个 Run 及其文本内容执行的操作（第一个参数为 Run，第二个参数为文本内容，可能为 null）
     */
    protected void forEachRun(XWPFDocument document, BiConsumer<XWPFRun, String> consumer) {
        // 遍历文档正文段落
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            List<XWPFRun> runs = paragraph.getRuns();
            if (runs != null) {
                for (XWPFRun run : runs) {
                    consumer.accept(run, run.getText(0));
                }
            }
        }
        // 遍历表格
        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        List<XWPFRun> runs = paragraph.getRuns();
                        if (runs != null) {
                            for (XWPFRun run : runs) {
                                consumer.accept(run, run.getText(0));
                            }
                        }
                    }
                }
            }
        }
    }
}