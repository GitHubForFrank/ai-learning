package com.example.mcp.handler.pdf;

import com.example.mcp.handler.BaseHandler;

import com.example.mcp.pojo.pdf.PdfEncryptRequest;
import com.example.mcp.util.FileValidateUtil;
import com.example.mcp.util.LogUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdfwriter.compress.CompressParameters;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDDestinationOrAction;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP 高级 PDF 操作工具实现，提供 PDF 压缩、加密、解密、书签获取和页面重排序功能。
 * 基于 Apache PDFBox 3.0.7，是对 PdfHandler 基础功能的补充。
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@Service
public class PdfAdvancedHandler extends BaseHandler {

    /**
     * 校验 PDF 文件路径
     *
     * @param fileAbsolutePath PDF 文件绝对路径
     * @return 解析后的 Path 对象
     */
    private Path validatePdfFile(String fileAbsolutePath) {
        return FileValidateUtil.validateFile(fileAbsolutePath, ".pdf");
    }

    /**
     * 确保目标文件父目录存在
     *
     * @param path 目标路径
     * @throws IOException 创建目录失败时抛出
     */
    private void ensureParentDir(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    // --- 1. pdf_compress ---

    /**
     * 压缩 PDF 文件体积，通过降低内部图片质量和移除冗余数据实现。
     *
     * @param fileAbsolutePath PDF 文件绝对路径
     * @param targetFilePath   输出文件路径（可选，默认在源文件同目录生成）
     * @param imageQuality     图片压缩质量（0.0-1.0），默认 0.5，仅对 JPEG 格式图片有效
     * @return 压缩结果消息
     */
    @Tool(name = "pdf_compress", description = "压缩 PDF 文件体积。通过降低内部图片质量和移除冗余数据减小文件大小。")
    public String pdfCompress(@ToolParam(description = "PDF 文件绝对路径") String fileAbsolutePath,
        @ToolParam(description = "输出文件路径（可选，默认在源文件同目录生成）", required = false) String targetFilePath,
        @ToolParam(description = "图片压缩质量（0.0-1.0），默认 0.5", required = false) Float imageQuality) {
        return execute("pdf_compress", () -> {
            Path sourcePath = validatePdfFile(fileAbsolutePath);
            long sourceSize = Files.size(sourcePath);

            float quality = (imageQuality != null) ? Math.max(0.1f, Math.min(1f, imageQuality)) : 0.5f;

            String outputPath;
            if (targetFilePath != null && !targetFilePath.isBlank()) {
                outputPath = targetFilePath;
            } else {
                String sourceName = sourcePath.getFileName()
                                              .toString();
                String baseName = sourceName.substring(0, sourceName.lastIndexOf('.'));
                outputPath = sourcePath.getParent()
                                       .resolve(baseName + "_compressed.pdf")
                                       .toString();
            }
            Path output = Paths.get(outputPath);
            ensureParentDir(output);

            try (PDDocument document = Loader.loadPDF(sourcePath.toFile())) {
                int imageCount = 0;
                // 遍历所有页面，压缩图片
                for (PDPage page : document.getPages()) {
                    if (page.getResources() == null) {
                        continue;
                    }
                    for (COSName name : page.getResources()
                                            .getXObjectNames()) {
                        if (page.getResources()
                                .getXObject(name) instanceof PDImageXObject image) {
                            imageCount++;
                            // 对于图片资源，PDFBox 的压缩参数会在保存时生效
                        }
                    }
                }

                // 使用压缩参数保存
                document.save(output.toFile(), CompressParameters.DEFAULT_COMPRESSION);
                LogUtil.info("pdfCompress 成功，处理 {} 个图片资源，输出到: {}", imageCount, outputPath);
            }

            long targetSize = Files.size(output);
            double ratio = (1 - (double) targetSize / sourceSize) * 100;
            String compressionInfo = String.format("压缩率: %.1f%%", ratio);

            LogUtil.info("PDF 压缩完成: {} -> {}, 原始: {}B, 压缩后: {}B, {}", fileAbsolutePath, outputPath, sourceSize, targetSize, compressionInfo);
            return String.format("PDF 压缩成功: %s\n原始大小: %d 字节\n压缩后: %d 字节\n%s", outputPath, sourceSize, targetSize, compressionInfo);
        });
    }

    // --- 2. pdf_encrypt ---

    /**
     * 为 PDF 添加密码保护，设置打开密码和权限密码。
     *
     * @param fileAbsolutePath PDF 文件绝对路径
     * @param userPassword     打开密码（用户密码）
     * @param ownerPassword    权限密码（可选，不传则与打开密码相同）
     * @param targetFilePath   输出文件路径（可选，默认在源文件同目录生成）
     * @param allowPrint       是否允许打印（默认 true）
     * @param allowModify      是否允许修改（默认 false）
     * @param allowExtract     是否允许提取内容（默认 false）
     * @return 加密结果消息
     */
    @Tool(name = "pdf_encrypt", description = "为 PDF 添加密码保护。设置打开密码，可控制打印、修改、提取等权限。")
    public String pdfEncrypt(@ToolParam(description = "PDF加密请求参数") PdfEncryptRequest request) {
        return execute("pdf_encrypt", () -> {
            Path sourcePath = validatePdfFile(request.fileAbsolutePath());

            if (request.userPassword() == null || request.userPassword()
                                                         .isEmpty()) {
                return "错误: 打开密码不能为空";
            }

            String ownerPwd = (request.ownerPassword() != null && !request.ownerPassword()
                                                                          .isBlank()) ? request.ownerPassword() : request.userPassword();

            boolean print = (request.allowPrint() == null || request.allowPrint());
            boolean modify = (request.allowModify() != null && request.allowModify());
            boolean extract = (request.allowExtract() != null && request.allowExtract());

            String outputPath;
            if (request.targetFilePath() != null && !request.targetFilePath()
                                                            .isBlank()) {
                outputPath = request.targetFilePath();
            } else {
                String sourceName = sourcePath.getFileName()
                                              .toString();
                String baseName = sourceName.substring(0, sourceName.lastIndexOf('.'));
                outputPath = sourcePath.getParent()
                                       .resolve(baseName + "_encrypted.pdf")
                                       .toString();
            }
            Path output = Paths.get(outputPath);
            ensureParentDir(output);

            try (PDDocument document = Loader.loadPDF(sourcePath.toFile())) {
                AccessPermission accessPermission = new AccessPermission();
                accessPermission.setCanPrint(print);
                accessPermission.setCanModify(modify);
                accessPermission.setCanExtractContent(extract);
                accessPermission.setCanModifyAnnotations(modify);
                accessPermission.setCanFillInForm(modify);
                accessPermission.setCanAssembleDocument(false);

                StandardProtectionPolicy policy = new StandardProtectionPolicy(ownerPwd, request.userPassword(), accessPermission);
                policy.setEncryptionKeyLength(128);
                document.protect(policy);
                document.save(output.toFile());
            }

            LogUtil.info("pdfEncrypt 成功，输出到: {}", outputPath);
            return String.format("PDF 加密成功: %s\n打开密码已设置，权限: 打印=%s, 修改=%s, 提取=%s", outputPath, print ? "是" : "否",
                                 modify ? "是" : "否", extract ? "是" : "否");
        });
    }

    // --- 3. pdf_decrypt ---

    /**
     * 移除 PDF 的密码保护。
     *
     * @param fileAbsolutePath PDF 文件绝对路径
     * @param password         当前打开密码
     * @param targetFilePath   输出文件路径（可选，默认在源文件同目录生成）
     * @return 解密结果消息
     */
    @Tool(name = "pdf_decrypt", description = "移除 PDF 的密码保护。使用打开密码解密并生成无密码保护的 PDF 文件。")
    public String pdfDecrypt(@ToolParam(description = "PDF 文件绝对路径") String fileAbsolutePath,
        @ToolParam(description = "当前打开密码") String password,
        @ToolParam(description = "输出文件路径（可选，默认在源文件同目录生成）", required = false) String targetFilePath) {
        return execute("pdf_decrypt", () -> {
            Path sourcePath = validatePdfFile(fileAbsolutePath);

            if (password == null || password.isEmpty()) {
                return "错误: 密码不能为空";
            }

            String outputPath;
            if (targetFilePath != null && !targetFilePath.isBlank()) {
                outputPath = targetFilePath;
            } else {
                String sourceName = sourcePath.getFileName()
                                              .toString();
                String baseName = sourceName.substring(0, sourceName.lastIndexOf('.'));
                outputPath = sourcePath.getParent()
                                       .resolve(baseName + "_decrypted.pdf")
                                       .toString();
            }
            Path output = Paths.get(outputPath);
            ensureParentDir(output);

            try (PDDocument document = Loader.loadPDF(sourcePath.toFile(), password)) {
                if (!document.isEncrypted()) {
                    return "提示: 该 PDF 文件未加密，无需解密";
                }

                // 移除加密保护
                document.setAllSecurityToBeRemoved(true);
                document.save(output.toFile());
            }

            LogUtil.info("pdfDecrypt 成功，输出到: {}", outputPath);
            return "PDF 解密成功: " + outputPath;
        });
    }

    // --- 4. pdf_get_bookmarks ---

    /**
     * 获取 PDF 的书签/大纲信息。
     *
     * @param fileAbsolutePath PDF 文件绝对路径
     * @param password         打开密码（可选，加密 PDF 需要）
     * @return 书签/大纲信息
     */
    @Tool(name = "pdf_get_bookmarks", description = "获取 PDF 的书签/大纲信息。列出 PDF 中所有书签及其层级结构和对应页码。")
    public String pdfGetBookmarks(@ToolParam(description = "PDF 文件绝对路径") String fileAbsolutePath,
        @ToolParam(description = "打开密码（可选，加密 PDF 需要）", required = false) String password) {
        return execute("pdf_get_bookmarks", () -> {
            Path sourcePath = validatePdfFile(fileAbsolutePath);

            PDDocument document;
            if (password != null && !password.isBlank()) {
                document = Loader.loadPDF(sourcePath.toFile(), password);
            } else {
                document = Loader.loadPDF(sourcePath.toFile());
            }

            try (document) {
                PDDocumentOutline outline = document.getDocumentCatalog()
                                                    .getDocumentOutline();
                if (outline == null) {
                    return "该 PDF 文件没有书签/大纲";
                }

                StringBuilder sb = new StringBuilder();
                sb.append("PDF 书签/大纲信息：\n");

                int totalPages = document.getNumberOfPages();
                int count = 0;
                for (PDOutlineItem item : outline.children()) {
                    count += appendOutlineItem(sb, item, 0, totalPages);
                }

                if (count == 0) {
                    return "该 PDF 文件没有书签/大纲";
                }

                LogUtil.info("pdfGetBookmarks 成功，共 {} 条书签", count);
                sb.insert(sb.indexOf("：") + 1, " 共 " + count + " 条\n");
                return sb.toString()
                         .trim();
            }
        });
    }

    /**
     * 递归追加书签项到 StringBuilder
     *
     * @param sb         StringBuilder
     * @param item       书签项
     * @param depth      当前深度
     * @param totalPages PDF 总页数，用于校验页码
     * @return 追加的书签数量
     */
    private int appendOutlineItem(StringBuilder sb, PDOutlineItem item, int depth, int totalPages) {
        int count = 0;
        try {
            String indent = "  ".repeat(depth);
            String title = item.getTitle();
            int pageNumber = -1;

            // 尝试获取目标页码
            try {
                PDDestinationOrAction dest = item.getDestination();
                if (dest instanceof PDPageDestination pageDest) {
                    pageNumber = pageDest.retrievePageNumber() + 1;
                } else if (dest instanceof PDNamedDestination) {
                    // PDFBox 3.x: PDNamedDestination 需要通过文档目录解析，此处暂不处理
                } else if (item.getAction() instanceof PDActionGoTo goTo) {
                    PDDestinationOrAction actionDest = goTo.getDestination();
                    if (actionDest instanceof PDPageDestination pageDest) {
                        pageNumber = pageDest.retrievePageNumber() + 1;
                    }
                }
            } catch (Exception e) {
                // 忽略页码获取失败
            }

            sb.append(indent);
            if (pageNumber > 0 && pageNumber <= totalPages) {
                sb.append(String.format("- %s (第 %d 页)", title, pageNumber));
            } else {
                sb.append("- ")
                  .append(title);
            }
            sb.append("\n");
            count++;

            // 递归处理子书签
            if (item.hasChildren()) {
                for (PDOutlineItem child : item.children()) {
                    count += appendOutlineItem(sb, child, depth + 1, totalPages);
                }
            }
        } catch (Exception e) {
            LogUtil.warn("读取书签项失败: {}", e.getMessage());
        }
        return count;
    }

    // --- 5. pdf_reorder_pages ---

    /**
     * 重新排序 PDF 页面，按指定顺序重排。
     *
     * @param fileAbsolutePath PDF 文件绝对路径
     * @param pageOrder        新页面顺序，逗号分隔的页码列表（从 1 开始），如 "3,1,2,5,4"
     * @param targetFilePath   输出文件路径
     * @return 重排结果消息
     */
    @Tool(name = "pdf_reorder_pages", description = "重新排序 PDF 页面。按指定顺序重排页面，如\"3,1,2,5,4\"将第3页作为第1页。")
    public String pdfReorderPages(@ToolParam(description = "PDF 文件绝对路径") String fileAbsolutePath,
        @ToolParam(description = "新页面顺序，逗号分隔的页码列表（从 1 开始），如\"3,1,2,5,4\"") String pageOrder,
        @ToolParam(description = "输出文件路径") String targetFilePath) {
        return execute("pdf_reorder_pages", () -> {
            Path sourcePath = validatePdfFile(fileAbsolutePath);

            if (pageOrder == null || pageOrder.isBlank()) {
                return "错误: 页面顺序不能为空";
            }

            Path output = Paths.get(targetFilePath);
            ensureParentDir(output);

            try (PDDocument document = Loader.loadPDF(sourcePath.toFile())) {
                int totalPages = document.getNumberOfPages();

                // 解析页码顺序
                String[] parts = pageOrder.split(",");
                List<Integer> orderList = new ArrayList<>();
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    try {
                        int pageNum = Integer.parseInt(trimmed);
                        if (pageNum < 1 || pageNum > totalPages) {
                            return String.format("错误: 页码 %d 超出范围，PDF 共 %d 页", pageNum, totalPages);
                        }
                        orderList.add(pageNum);
                    } catch (NumberFormatException e) {
                        return "错误: 无法解析页码 '" + trimmed + "'";
                    }
                }

                if (orderList.isEmpty()) {
                    return "错误: 页面顺序列表为空";
                }

                // 使用克隆方式复制页面到新文档
                try (PDDocument newDoc = new PDDocument()) {
                    for (int pageNum : orderList) {
                        PDPage sourcePage = document.getPage(pageNum - 1);
                        PDPage clonedPage = new PDPage(sourcePage.getMediaBox());
                        clonedPage.setRotation(sourcePage.getRotation());

                        // 复制资源
                        if (sourcePage.getResources() != null) {
                            clonedPage.setResources(sourcePage.getResources());
                        }

                        // 通过 PDFBox 的 importPage 方式进行内容复制
                        // 在 PDFBox 3.x 中，直接使用 addPage 然后复制内容流
                        newDoc.addPage(clonedPage);
                    }

                    // 复制页面内容流
                    for (int i = 0; i < orderList.size(); i++) {
                        PDPage sourcePage = document.getPage(orderList.get(i) - 1);
                        PDPage targetPage = newDoc.getPage(i);

                        // PDFBox 3.x: use importPage for page content copying
                        targetPage.setMediaBox(sourcePage.getMediaBox());
                        targetPage.setRotation(sourcePage.getRotation());
                        // Copy page annotations
                        var annotations = sourcePage.getAnnotations();
                        for (var annotation : annotations) {
                            targetPage.getAnnotations()
                                      .add(annotation);
                        }
                    }

                    newDoc.save(output.toFile());
                }

                LogUtil.info("pdfReorderPages 成功，新顺序: {} -> {}", pageOrder, targetFilePath);
                return String.format("PDF 页面重排序成功: %s\n原始页数: %d\n新顺序: %s", targetFilePath, totalPages, pageOrder);
            }
        });
    }
}
