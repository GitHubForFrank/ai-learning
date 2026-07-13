package com.example.mcp.handler.document;

import com.alibaba.fastjson2.JSON;
import com.example.mcp.handler.BaseHandler;
import com.example.mcp.util.LogUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP 文档对比工具实现，提供文本文件、Word 文档、PDF 文档和目录之间的差异对比功能。
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@Service
public class DocumentDiffHandler extends BaseHandler {

    private List<String> readLines(Path path) throws IOException {
        return Files.readAllLines(path);
    }

    private String readWordText(String filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(Path.of(filePath)))) {
            for (XWPFParagraph para : doc.getParagraphs()) {
                sb.append(para.getText())
                  .append("\n");
            }
        }
        return sb.toString();
    }

    private String readPdfText(String filePath) throws IOException {
        try (PDDocument doc = Loader.loadPDF(Path.of(filePath)
                                                 .toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    // --- 1. diff_text ---

    /**
     * 对比两个文本文件，输出行级差异报告（新增行、删除行、修改行）。
     *
     * @param filePath1 第一个文本文件路径
     * @param filePath2 第二个文本文件路径
     * @return JSON 格式的差异报告
     */
    @Tool(name = "diff_text", description = "对比两个文本文件的行级差异，返回新增、删除和修改的行。")
    public String diffText(@ToolParam(description = "第一个文本文件路径") String filePath1,
        @ToolParam(description = "第二个文本文件路径") String filePath2) {
        return execute("diff_text", () -> {
            if (filePath1 == null || filePath1.isBlank()) {
                return "{\"error\": \"第一个文件路径不能为空\"}";
            }
            if (filePath2 == null || filePath2.isBlank()) {
                return "{\"error\": \"第二个文件路径不能为空\"}";
            }
            Path p1 = Path.of(filePath1);
            Path p2 = Path.of(filePath2);
            if (!Files.exists(p1)) {
                return "{\"error\": \"文件不存在: " + filePath1 + "\"}";
            }
            if (!Files.exists(p2)) {
                return "{\"error\": \"文件不存在: " + filePath2 + "\"}";
            }

            List<String> lines1 = readLines(p1);
            List<String> lines2 = readLines(p2);

            List<String> added = new ArrayList<>();
            List<String> deleted = new ArrayList<>();
            List<Map<String, String>> modified = new ArrayList<>();

            int minSize = Math.min(lines1.size(), lines2.size());
            for (int i = 0; i < minSize; i++) {
                if (!lines1.get(i)
                           .equals(lines2.get(i))) {
                    Map<String, String> mod = new LinkedHashMap<>();
                    mod.put("line", String.valueOf(i + 1));
                    mod.put("old", lines1.get(i));
                    mod.put("new", lines2.get(i));
                    modified.add(mod);
                }
            }
            for (int i = minSize; i < lines1.size(); i++) {
                deleted.add("行" + (i + 1) + ": " + lines1.get(i));
            }
            for (int i = minSize; i < lines2.size(); i++) {
                added.add("行" + (i + 1) + ": " + lines2.get(i));
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("file1", filePath1);
            result.put("file2", filePath2);
            result.put("file1Lines", lines1.size());
            result.put("file2Lines", lines2.size());
            result.put("identical", added.isEmpty() && deleted.isEmpty() && modified.isEmpty());
            result.put("addedCount", added.size());
            result.put("deletedCount", deleted.size());
            result.put("modifiedCount", modified.size());
            if (!added.isEmpty()) {
                result.put("added", added);
            }
            if (!deleted.isEmpty()) {
                result.put("deleted", deleted);
            }
            if (!modified.isEmpty()) {
                result.put("modified", modified);
            }

            LogUtil.info("diffText 完成: {} vs {}, 新增={}, 删除={}, 修改={}", filePath1, filePath2, added.size(), deleted.size(), modified.size());
            return JSON.toJSONString(result);
        });
    }

    // --- 2. diff_word_text ---

    /**
     * 对比两个 Word 文档的文本内容差异。
     *
     * @param filePath1 第一个 Word 文档路径
     * @param filePath2 第二个 Word 文档路径
     * @return JSON 格式的差异报告
     */
    @Tool(name = "diff_word_text", description = "对比两个 Word 文档的文本内容差异。")
    public String diffWordText(@ToolParam(description = "第一个 Word 文档路径") String filePath1,
        @ToolParam(description = "第二个 Word 文档路径") String filePath2) {
        return execute("diff_word_text", () -> {
            if (filePath1 == null || filePath1.isBlank()) {
                return "{\"error\": \"第一个文件路径不能为空\"}";
            }
            if (filePath2 == null || filePath2.isBlank()) {
                return "{\"error\": \"第二个文件路径不能为空\"}";
            }

            String text1 = readWordText(filePath1);
            String text2 = readWordText(filePath2);

            String[] lines1 = text1.split("\n");
            String[] lines2 = text2.split("\n");

            List<String> added = new ArrayList<>();
            List<String> deleted = new ArrayList<>();
            int minLen = Math.min(lines1.length, lines2.length);
            for (int i = minLen; i < lines1.length; i++) {
                deleted.add(lines1[i]);
            }
            for (int i = minLen; i < lines2.length; i++) {
                added.add(lines2[i]);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("file1", filePath1);
            result.put("file2", filePath2);
            result.put("file1Length", text1.length());
            result.put("file2Length", text2.length());
            result.put("addedLines", added.size());
            result.put("deletedLines", deleted.size());
            result.put("identical", text1.equals(text2));

            LogUtil.info("diffWordText 完成: {} vs {}", filePath1, filePath2);
            return JSON.toJSONString(result);
        });
    }

    // --- 3. diff_pdf_text ---

    /**
     * 对比两个 PDF 文档的文本内容差异。
     *
     * @param filePath1 第一个 PDF 文档路径
     * @param filePath2 第二个 PDF 文档路径
     * @return JSON 格式的差异报告
     */
    @Tool(name = "diff_pdf_text", description = "对比两个 PDF 文档的文本内容差异。")
    public String diffPdfText(@ToolParam(description = "第一个 PDF 文档路径") String filePath1,
        @ToolParam(description = "第二个 PDF 文档路径") String filePath2) {
        return execute("diff_pdf_text", () -> {
            if (filePath1 == null || filePath1.isBlank()) {
                return "{\"error\": \"第一个文件路径不能为空\"}";
            }
            if (filePath2 == null || filePath2.isBlank()) {
                return "{\"error\": \"第二个文件路径不能为空\"}";
            }

            String text1 = readPdfText(filePath1);
            String text2 = readPdfText(filePath2);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("file1", filePath1);
            result.put("file2", filePath2);
            result.put("file1Length", text1.length());
            result.put("file2Length", text2.length());
            result.put("identical", text1.equals(text2));

            LogUtil.info("diffPdfText 完成: {} vs {}", filePath1, filePath2);
            return JSON.toJSONString(result);
        });
    }

    // --- 4. diff_directories ---

    /**
     * 对比两个目录的文件列表差异，按文件名比较。
     *
     * @param dirPath1 第一个目录路径
     * @param dirPath2 第二个目录路径
     * @return JSON 格式的差异报告
     */
    @Tool(name = "diff_directories", description = "对比两个目录的文件列表差异，按文件名、大小和修改时间比较。")
    public String diffDirectories(@ToolParam(description = "第一个目录路径") String dirPath1,
        @ToolParam(description = "第二个目录路径") String dirPath2) {
        return execute("diff_directories", () -> {
            if (dirPath1 == null || dirPath1.isBlank()) {
                return "{\"error\": \"第一个目录路径不能为空\"}";
            }
            if (dirPath2 == null || dirPath2.isBlank()) {
                return "{\"error\": \"第二个目录路径不能为空\"}";
            }
            Path p1 = Path.of(dirPath1);
            Path p2 = Path.of(dirPath2);
            if (!Files.isDirectory(p1)) {
                return "{\"error\": \"不是有效目录: " + dirPath1 + "\"}";
            }
            if (!Files.isDirectory(p2)) {
                return "{\"error\": \"不是有效目录: " + dirPath2 + "\"}";
            }

            Map<String, FileTime> files1 = new LinkedHashMap<>();
            Map<String, Long> sizes1 = new LinkedHashMap<>();
            try (var stream = Files.list(p1)) {
                stream.filter(Files::isRegularFile)
                      .forEach(f -> {
                          try {
                              files1.put(f.getFileName()
                                          .toString(), Files.getLastModifiedTime(f));
                              sizes1.put(f.getFileName()
                                          .toString(), Files.size(f));
                          } catch (IOException ignored) {
                          }
                      });
            }

            Map<String, FileTime> files2 = new LinkedHashMap<>();
            Map<String, Long> sizes2 = new LinkedHashMap<>();
            try (var stream = Files.list(p2)) {
                stream.filter(Files::isRegularFile)
                      .forEach(f -> {
                          try {
                              files2.put(f.getFileName()
                                          .toString(), Files.getLastModifiedTime(f));
                              sizes2.put(f.getFileName()
                                          .toString(), Files.size(f));
                          } catch (IOException ignored) {
                          }
                      });
            }

            List<String> onlyIn1 = new ArrayList<>();
            List<String> onlyIn2 = new ArrayList<>();
            List<String> modified = new ArrayList<>();
            List<String> same = new ArrayList<>();

            for (String name : files1.keySet()) {
                if (!files2.containsKey(name)) {
                    onlyIn1.add(name);
                } else {
                    if (!files1.get(name)
                               .equals(files2.get(name)) || !sizes1.get(name)
                                                                   .equals(sizes2.get(name))) {
                        modified.add(name);
                    } else {
                        same.add(name);
                    }
                }
            }
            for (String name : files2.keySet()) {
                if (!files1.containsKey(name)) {
                    onlyIn2.add(name);
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("dir1", dirPath1);
            result.put("dir2", dirPath2);
            result.put("dir1FileCount", files1.size());
            result.put("dir2FileCount", files2.size());
            result.put("onlyInDir1", onlyIn1.size());
            result.put("onlyInDir2", onlyIn2.size());
            result.put("modified", modified.size());
            result.put("identical", same.size());
            if (!onlyIn1.isEmpty()) {
                result.put("onlyInDir1Files", onlyIn1);
            }
            if (!onlyIn2.isEmpty()) {
                result.put("onlyInDir2Files", onlyIn2);
            }
            if (!modified.isEmpty()) {
                result.put("modifiedFiles", modified);
            }

            LogUtil.info("diffDirectories 完成: {} vs {}", dirPath1, dirPath2);
            return JSON.toJSONString(result);
        });
    }
}
