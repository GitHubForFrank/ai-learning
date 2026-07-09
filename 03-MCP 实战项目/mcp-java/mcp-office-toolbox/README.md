# mcp-office-toolbox

> 基于 Spring AI MCP Server Starter 的办公文件操作 MCP Server，**stdio 传输**版本。
> 提供文件系统、TXT、Markdown、PDF、Word、Excel、PPT、CSV、ZIP、网页抓取、文档查询等全面的文件操作能力。

---

## 1. 能力概览

| 模块 | Handler | 工具数量 | 说明 |
|------|---------|---------|------|
| 通用文件系统 | `FileSystemHandler` | 20 | 文件读写、复制、移动、删除、搜索、替换、目录遍历等 |
| TXT 文本 | `TxtHandler` | 9 | 创建、全文/按行/指定行读取、覆盖/追加写入、清空、查找、替换 |
| Markdown | `MarkdownHandler` | 13 | 创建、读取、追加、插入、标题/列表/代码块/表格/引用块生成、修改、保存 |
| PDF 文档 | `PdfHandler` | 4 | 全文读取、按页读取、元信息获取、PDF 转 TXT |
| Word 文档 | `WordHandler` | 8 | 创建、全文/分段读取、插入文字/标题、修改、批量替换、另存 |
| Excel 表格 | `ExcelHandler` | 11 | 创建工作簿、读写单元格、格式化、删除行列、清空、截图等 |
| PPT 演示 | `PptHandler` | 6 | 创建、增删幻灯片、读取文本、修改文字、另存 |
| CSV 文件 | `CsvHandler` | 6 | 创建、读取（含表头分离）、写入、追加、信息查看 |
| ZIP 压缩 | `ZipHandler` | 3 | 多文件/目录压缩、解压、内容列表查看 |
| 网页抓取 | `FetchHandler` | 4 | 抓取 HTML/JSON/TXT/Markdown 四种格式 |
| 文档查询 | `Context7Handler` | 2 | 库 ID 解析、最新文档查询（基于 Context7 API） |

**总计：11 个 Handler，86 个 @Tool 方法**

---

## 2. 目录结构

```
mcp-office-toolbox/
├── pom.xml                              · 依赖与构建声明
└── src/
    ├── main/
    │   ├── java/com/example/mcp/
    │   │   ├── McpOfficeToolboxApplication.java      · Spring Boot 入口
    │   │   ├── config/
    │   │   │   └── McpConfiguration.java             · MCP 工具注册配置
    │   │   ├── handler/
    │   │   │   ├── FileSystemHandler.java            · 通用文件系统操作
    │   │   │   ├── TxtHandler.java                   · TXT 文本文件操作
    │   │   │   ├── MarkdownHandler.java              · Markdown 文件操作
    │   │   │   ├── PdfHandler.java                   · PDF 文档操作
    │   │   │   ├── WordHandler.java                  · Word 文档操作
    │   │   │   ├── ExcelHandler.java                 · Excel 表格操作
    │   │   │   ├── PptHandler.java                   · PPT 演示文稿操作
    │   │   │   ├── CsvHandler.java                   · CSV 文件操作
    │   │   │   ├── ZipHandler.java                   · ZIP 压缩/解压操作
    │   │   │   ├── FetchHandler.java                 · 网页抓取操作
    │   │   │   └── Context7Handler.java              · Context7 文档查询操作
    │   │   ├── pojo/                                 · 数据实体类
    │   │   │   ├── excel/                            · Excel 样式相关 POJO
    │   │   │   ├── fetch/                            · Fetch 请求 POJO
    │   │   │   ├── context7/                         · Context7 请求 POJO
    │   │   │   └── filesystem/                       · 文件系统编辑 POJO
    │   │   └── util/                                 · 公共工具类
    │   │       ├── FileValidateUtil.java             · 文件校验工具
    │   │       ├── PathUtil.java                     · 路径操作工具
    │   │       └── ZipUtil.java                      · ZIP 压缩/解压工具
    │   └── resources/
    │       └── application.properties                · 应用配置
    └── test/java/                                    · 测试代码
```

---

## 3. 工具列表与使用方式

### 3.1 FileSystemHandler（通用文件系统）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `read_text_file` | 读取文本文件内容，支持 head/tail 参数 | `read_text_file(path="test.txt", head=10)` |
| `read_media_file` | 读取图片/音频文件，返回 base64 | `read_media_file(path="image.png")` |
| `read_multiple_files` | 批量读取多个文件 | `read_multiple_files(paths=["a.txt", "b.txt"])` |
| `write_file` | 创建或覆盖文件 | `write_file(path="test.txt", content="Hello")` |
| `edit_file` | 基于行的精确编辑 | `edit_file(request={path, edits, dryRun})` |
| `create_directory` | 创建目录（支持嵌套） | `create_directory(path="newdir")` |
| `list_directory` | 列出目录内容 | `list_directory(path=".")` |
| `list_directory_with_sizes` | 列出目录内容（含文件大小） | `list_directory_with_sizes(path=".", sortBy="size")` |
| `directory_tree` | 递归获取目录树（JSON 格式） | `directory_tree(path=".", excludePatterns=["node_modules"])` |
| `move_file` | 移动或重命名文件/目录 | `move_file(source="a.txt", destination="b.txt")` |
| `search_files` | 按 glob 模式搜索文件 | `search_files(path=".", pattern="**/*.java")` |
| `get_file_info` | 获取文件/目录元数据 | `get_file_info(path="test.txt")` |
| `list_allowed_directories` | 查看允许访问的目录 | `list_allowed_directories()` |
| `copy_file` | 复制文件或目录 | `copy_file(source="a.txt", destination="b.txt")` |
| `delete_file` | 删除文件或目录（递归） | `delete_file(path="test.txt")` |
| `append_to_file` | 向文件末尾追加内容 | `append_to_file(path="test.txt", content="World")` |
| `clear_file` | 清空文件内容 | `clear_file(path="test.txt")` |
| `search_in_file` | 在文件中搜索关键词 | `search_in_file(path="test.txt", keyword="hello")` |
| `replace_in_file` | 替换文件中的文本 | `replace_in_file(path="test.txt", oldText="hello", newText="world")` |

### 3.2 TxtHandler（TXT 文本文件）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `create_txt_file` | 创建空白 TXT 文件 | `create_txt_file(filePath="test.txt")` |
| `read_txt_full` | 读取 TXT 全文 | `read_txt_full(filePath="test.txt")` |
| `read_txt_lines` | 按行读取 TXT | `read_txt_lines(filePath="test.txt", startLine=1, endLine=10)` |
| `read_txt_specific_lines` | 读取指定行 | `read_txt_specific_lines(filePath="test.txt", lineNumbers=[1,3,5])` |
| `write_txt` | 覆盖写入 TXT | `write_txt(filePath="test.txt", content="Hello")` |
| `append_txt` | 追加写入 TXT | `append_txt(filePath="test.txt", content="World")` |
| `clear_txt` | 清空 TXT 文件 | `clear_txt(filePath="test.txt")` |
| `search_txt` | 在 TXT 中搜索 | `search_txt(filePath="test.txt", keyword="hello")` |
| `replace_txt` | 替换 TXT 中的文本 | `replace_txt(filePath="test.txt", oldText="hello", newText="world")` |

### 3.3 MarkdownHandler（Markdown 文件）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `create_md_file` | 创建空白 MD 文件 | `create_md_file(filePath="doc.md")` |
| `read_md` | 读取 MD 文件源码 | `read_md(filePath="doc.md")` |
| `append_md` | 向 MD 文件追加内容 | `append_md(filePath="doc.md", content="## 标题")` |
| `insert_md` | 在指定行插入内容 | `insert_md(filePath="doc.md", lineNumber=5, content="新内容")` |
| `md_generate_heading` | 生成 Markdown 标题 | `md_generate_heading(headingText="标题", level=2)` |
| `md_generate_list` | 生成 Markdown 列表 | `md_generate_list(items=["项目 1","项目 2"], ordered=true)` |
| `md_generate_code_block` | 生成代码块 | `md_generate_code_block(code="print('hi')", language="python")` |
| `md_generate_table` | 生成数据表格 | `md_generate_table(headers=["姓名","年龄"], rows=[["张三","25"]])` |
| `md_generate_blockquote` | 生成引用块 | `md_generate_blockquote(text="这是一段引用")` |
| `modify_md_paragraph` | 修改指定段落 | `modify_md_paragraph(filePath="doc.md", oldText="旧文本", newText="新文本")` |
| `replace_md_content` | 全局替换文本 | `replace_md_content(filePath="doc.md", oldText="旧", newText="新")` |
| `save_md` | 保存内容到 MD 文件 | `save_md(filePath="doc.md", content="# 标题")` |
| `save_md_as` | 另存为新文件 | `save_md_as(sourcePath="doc.md", targetPath="doc2.md")` |

### 3.4 PdfHandler（PDF 文档）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `read_pdf_text` | 读取 PDF 全文 | `read_pdf_text(fileAbsolutePath="doc.pdf")` |
| `read_pdf_page` | 读取指定页 | `read_pdf_page(fileAbsolutePath="doc.pdf", pageNumber=1)` |
| `get_pdf_info` | 获取 PDF 元信息 | `get_pdf_info(fileAbsolutePath="doc.pdf")` |
| `convert_pdf_to_txt` | PDF 转 TXT | `convert_pdf_to_txt(fileAbsolutePath="doc.pdf", outputFilePath="out.txt")` |

### 3.5 WordHandler（Word 文档）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `create_word_doc` | 创建空白 Word 文档 | `create_word_doc(fileAbsolutePath="doc.docx")` |
| `read_word_full` | 读取 Word 全文 | `read_word_full(fileAbsolutePath="doc.docx")` |
| `read_word_paragraphs` | 逐段读取 Word | `read_word_paragraphs(fileAbsolutePath="doc.docx")` |
| `insert_word_text` | 插入文字 | `insert_word_text(fileAbsolutePath="doc.docx", text="新内容")` |
| `insert_word_heading` | 插入标题 | `insert_word_heading(fileAbsolutePath="doc.docx", text="标题", level=2)` |
| `modify_word_content` | 修改文档内容 | `modify_word_content(fileAbsolutePath="doc.docx", oldText="旧", newText="新")` |
| `replace_word_keywords` | 批量替换关键词 | `replace_word_keywords(fileAbsolutePath="doc.docx", replacements={"旧 1":"新 1","旧 2":"新 2"})` |
| `save_word_as` | 另存为新文件 | `save_word_as(sourcePath="doc.docx", targetPath="doc2.docx")` |

### 3.6 ExcelHandler（Excel 表格）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `excel_create_workbook` | 创建空白工作簿 | `excel_create_workbook(fileAbsolutePath="book.xlsx")` |
| `excel_describe_sheets` | 列出所有工作表 | `excel_describe_sheets(fileAbsolutePath="book.xlsx")` |
| `excel_read_sheet` | 读取工作表数据 | `excel_read_sheet(fileAbsolutePath="book.xlsx", sheetName="Sheet1")` |
| `excel_write_to_sheet` | 写入工作表 | `excel_write_to_sheet(fileAbsolutePath="book.xlsx", sheetName="Sheet1", values=[["姓名","年龄"],["张三",25]])` |
| `excel_copy_sheet` | 复制工作表 | `excel_copy_sheet(fileAbsolutePath="book.xlsx", sourceSheet="Sheet1", targetSheet="Sheet2")` |
| `excel_create_table` | 创建表格 | `excel_create_table(fileAbsolutePath="book.xlsx", sheetName="Sheet1", range="A1:C10")` |
| `excel_format_range` | 格式化单元格 | `excel_format_range(fileAbsolutePath="book.xlsx", sheetName="Sheet1", range="A1:C1", styles=[...])` |
| `excel_screen_capture` | 截取工作表截图 | `excel_screen_capture(fileAbsolutePath="book.xlsx", sheetName="Sheet1")` |
| `excel_delete_row` | 删除行 | `excel_delete_row(fileAbsolutePath="book.xlsx", sheetName="Sheet1", rowIndex=0)` |
| `excel_delete_column` | 删除列 | `excel_delete_column(fileAbsolutePath="book.xlsx", sheetName="Sheet1", columnIndex=0)` |
| `excel_clear_sheet` | 清空工作表 | `excel_clear_sheet(fileAbsolutePath="book.xlsx", sheetName="Sheet1")` |

### 3.7 PptHandler（PPT 演示文稿）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `create_ppt` | 创建空白 PPT | `create_ppt(fileAbsolutePath="slide.pptx")` |
| `add_ppt_slide` | 添加新幻灯片 | `add_ppt_slide(fileAbsolutePath="slide.pptx", title="标题")` |
| `delete_ppt_slide` | 删除幻灯片 | `delete_ppt_slide(fileAbsolutePath="slide.pptx", slideIndex=0)` |
| `read_ppt_text` | 读取所有幻灯片文本 | `read_ppt_text(fileAbsolutePath="slide.pptx")` |
| `modify_ppt_slide_text` | 修改幻灯片文本 | `modify_ppt_slide_text(fileAbsolutePath="slide.pptx", slideIndex=0, oldText="旧", newText="新")` |
| `save_ppt_as` | 另存为新文件 | `save_ppt_as(sourcePath="slide.pptx", targetPath="slide2.pptx")` |

### 3.8 CsvHandler（CSV 文件）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `csv_create` | 创建空白 CSV | `csv_create(filePath="data.csv", headers="姓名，年龄，城市")` |
| `csv_read` | 读取 CSV 全部内容 | `csv_read(filePath="data.csv", hasHeader=true, limit=100)` |
| `csv_read_headers` | 仅读取表头 | `csv_read_headers(filePath="data.csv")` |
| `csv_write` | 覆盖写入 CSV | `csv_write(filePath="data.csv", values=[["张三",25,"北京"]])` |
| `csv_append` | 追加数据行 | `csv_append(filePath="data.csv", values=[["李四",30,"上海"]])` |
| `csv_info` | 获取 CSV 基本信息 | `csv_info(filePath="data.csv")` |

### 3.9 ZipHandler（ZIP 压缩/解压）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `zip_compress` | 压缩文件/目录 | `zip_compress(sourcePaths="file1.txt,file2.txt", zipOutputPath="archive.zip")` |
| `zip_decompress` | 解压 ZIP 文件 | `zip_decompress(zipFilePath="archive.zip", destDirPath="output")` |
| `zip_list` | 查看 ZIP 内容列表 | `zip_list(zipFilePath="archive.zip")` |

### 3.10 FetchHandler（网页抓取）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `fetch_html` | 抓取原始 HTML | `fetch_html(request={url:"https://example.com", headers:{}})` |
| `fetch_json` | 抓取并格式化 JSON | `fetch_json(request={url:"https://api.example.com/data", headers:{}})` |
| `fetch_txt` | 抓取并提取纯文本 | `fetch_txt(request={url:"https://example.com", headers:{}})` |
| `fetch_markdown` | 抓取并转换为 Markdown | `fetch_markdown(request={url:"https://example.com", headers:{}})` |

### 3.11 Context7Handler（文档查询）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `resolve-library-id` | 解析库名称为标准 ID | `resolve_library_id(request={query:"I need to manage state", libraryName:"react"})` |
| `query-docs` | 查询最新文档 | `query_docs(request={libraryId:"/facebook/react", query:"useState hook", tokens:5000})` |

---

## 4. 构建

```bash
cd mcp-office-toolbox
mvn clean package -DskipTests
# 产物：target/mcp-office-toolbox-*.jar
```

---

## 5. 作为 MCP Server 挂载

stdio 模式下 Server 由 Client 拉起，不手动启动。在 `.mcp.json` 配置：

```jsonc
{
  "mcpServers": {
    "office-toolbox": {
      "command": "java",
      "args": ["-jar", "/absolute/path/to/target/mcp-office-toolbox-*.jar"]
    }
  }
}
```

---

## 6. 调试

```bash
npx @modelcontextprotocol/inspector java -jar target/mcp-office-toolbox-*.jar
```

浏览器 UI 可手动调用工具、查看 JSON-RPC 流量。

---

## 7. 工具命名规范

所有 MCP 工具采用 **snake_case 命名**，遵循 `模块前缀_动作_目标` 的命名模式：

| 模块 | 前缀 | 示例 |
|------|------|------|
| 文件系统 | 无前缀（通用） | `read_text_file`, `write_file`, `delete_file` |
| Excel | `excel` | `excel_read_sheet`, `excel_create_workbook` |
| PDF | `pdf` / `read_pdf` | `read_pdf_text`, `get_pdf_info` |
| Word | `word` | `create_word_doc`, `read_word_full` |
| PPT | `ppt` | `create_ppt`, `read_ppt_text` |
| TXT | `txt` | `create_txt_file`, `read_txt_full` |
| Markdown | `md` | `create_md_file`, `md_generate_heading` |
| CSV | `csv` | `csv_create`, `csv_read`, `csv_write` |
| ZIP | `zip` | `zip_compress`, `zip_decompress`, `zip_list` |
| Fetch | `fetch` | `fetch_html`, `fetch_json`, `fetch_txt`, `fetch_markdown` |
| Context7 | 无固定前缀 | `resolve-library-id`, `query-docs` |

---

## 8. 依赖

| 依赖 | 用途 |
|------|------|
| Spring AI MCP Server Starter | MCP Server 框架 |
| Apache POI (poi-ooxml) | Excel / Word / PPT 文档操作 |
| Apache PDFBox | PDF 文档解析 |
| Hutool | CSV 文件操作、通用工具 |
| Fastjson2 | JSON 序列化 |
| Lombok | 简化代码 |

---

## 9. 更多

- 功能模块总览：`../docs/文件操作功能模块总览.md`
- Spring AI MCP 官方文档：<https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html>
- Context7 API 文档：<https://context7.com/docs/api-guide>
