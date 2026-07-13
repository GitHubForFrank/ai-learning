# mcp-office-toolbox

> 基于 Spring AI MCP Server Starter 的办公文件操作 MCP Server，**stdio 传输**版本。
> 提供文件系统、TXT、Markdown、PDF、Word、Excel、PPT、CSV、ZIP、网页抓取、文档查询等全面的文件操作能力。

---

## 1. 能力概览

| 模块 | Handler | 工具数量 | 说明 |
|------|---------|---------|------|
| 命令行执行 | `CommandHandler` | 1 | 执行系统命令，返回原文输出，支持超时控制 |
| 通用文件系统 | `FileSystemHandler` | 19 | 文件读写、复制、移动、删除、搜索、替换、目录遍历等 |
| TXT 文本 | `TxtHandler` | 9 | 创建、全文/按行/指定行读取、覆盖/追加写入、清空、查找、替换 |
| Markdown | `MarkdownHandler` | 13 | 创建、读取、追加、插入、标题/列表/代码块/表格/引用块生成、修改、保存 |
| PDF 文档 | `PdfHandler` | 12 | 全文读取、按页读取、元信息、PDF转TXT、合并、拆分、从文本创建、提取页面、水印、转图片、旋转页面、提取图片 |
| Word 文档 | `WordHandler` | 33 | 创建/读取、文字/标题/段落/表格/图片插入、页面设置、对齐/行间距、页眉页脚/页码/水印、列表/超链接/目录、PDF转换、文档合并、图片提取、批注、字数统计、结构化读取、Word转Markdown、删除段落、关键词定位、统一字体、段前段后间距 |
| Excel 读取 | `ExcelReadHandler` | 6 | 工作表描述、分页读取、截图、数据分析、工作表对比、预览 |
| Excel 写入 | `ExcelWriteHandler` | 14 | 工作簿创建、数据写入、工作表复制、行列增删/插入、工作表管理、按列拆分、插入图片、批量操作、Excel转PDF |
| Excel 格式化 | `ExcelFormatHandler` | 11 | 单元格格式化、表格创建、列宽/行高/冻结/合并、条件格式、图表创建、命名区域、打印设置 |
| Excel 分析 | `ExcelAnalyzeHandler` | 12 | 排序、筛选、查找替换、去重、公式、数据验证、CSV互转、工作簿合并、透视表、保护工作表、分类汇总、行分组 |
| PPT 演示 | `PptHandler` | 16 | 创建、增删幻灯片、读取文本、修改文字、另存、文本框/图片/表格插入、复制幻灯片、演讲者备注、幻灯片排序、PPT转PDF、PPT合并、设置版式、设置背景 |
| CSV 文件 | `CsvHandler` | 6 | 创建、读取（含表头分离）、写入、追加、信息查看 |
| ZIP 压缩 | `ZipHandler` | 4 | 多文件/目录压缩、解压、内容列表查看 |
| 网页抓取 | `FetchHandler` | 4 | 抓取 HTML/JSON/TXT/Markdown 四种格式 |
| 文档查询 | `Context7Handler` | 2 | 库 ID 解析、最新文档查询（基于 Context7 API） |
| 图片处理 | `ImageHandler` | 3 | 图片压缩、缩放、格式转换 |
| 跨格式转换 | `ConvertHandler` | 3 | HTML转PDF、Markdown转PDF、Markdown转Word |
| 二维码 | `QrCodeHandler` | 2 | 二维码生成、二维码解析 |
| 通用工具 | `ToolHandler` | 2 | 文件编码转换、正则表达式测试 |
| 文本处理 | `TextToolHandler` | 10 | 文本统计、排序、大小写转换、信息提取、编解码、哈希计算 |
| 结构化数据 | `JsonDataHandler` | 12 | JSON/XML/YAML/CSV 格式的解析、校验、查询和互转 |
| 日期时间 | `DateTimeHandler` | 7 | 日期计算、差异比较、格式转换、时区转换、工作日计算、日历生成 |
| 文档对比 | `DocumentDiffHandler` | 4 | 文本/Word/PDF 文档及目录的差异对比 |
| HTTP 客户端 | `HttpClientHandler` | 7 | HTTP GET/POST/PUT/DELETE/PATCH/HEAD 请求及文件下载，支持 SSL 证书配置 |
| 图表生成 | `ChartHandler` | 3 | 柱状图、饼图、折线图生成（PNG 输出） |
| 批量文件 | `BatchFileHandler` | 4 | 批量重命名、批量文本替换、批量编码转换、重复文件查找 |
| 高级图片 | `ImageAdvancedHandler` | 8 | 裁剪、旋转、水印、元信息、拼接、文字标注、Base64 转换 |
| 加密解密 | `CryptoHandler` | 5 | AES 加解密、文件哈希、密码生成、Base64 编解码 |
| 系统诊断 | `SystemInfoHandler` | 5 | 系统信息、磁盘信息、环境变量、端口检测、系统属性 |
| 高级 PDF | `PdfAdvancedHandler` | 5 | PDF 压缩、加密、解密、书签读取、页面重排 |
| 高级 Word | `WordAdvancedHandler` | 4 | 模板生成、关键词高亮、标题提取、默认字体设置 |
| 高级 PPT | `PptAdvancedHandler` | 4 | 幻灯片导出为图片、嵌入图表、添加图形、幻灯片统计 |
| 媒体文件 | `MediaHandler` | 3 | 音频/视频/通用媒体文件元信息获取 |
| 高级归档 | `ArchiveAdvancedHandler` | 3 | ZIP/tar.gz/gzip 压缩、解压和信息查看 |
| 条形码 | `BarcodeHandler` | 2 | 一维条形码生成（EAN-13/Code128/Code39）和解析 |
| 国际化 | `I18nHandler` | 3 | 单位换算、货币汇率换算、数字格式化 |
| 操作审计 | `AuditLogger` | — | 异步记录所有 Tool 调用日志，每日轮转备份 |

**总计：37 个 Handler，约 260+ 个 @Tool 方法**

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
    │   │   │   ├── BaseHandler.java                     · 抽象基类（统一异常处理、审计日志）
    │   │   │   ├── ThrowingSupplier.java                · 函数式接口
    │   │   │   ├── excel/                               · Excel 操作（5个）
    │   │   │   ├── word/                                · Word 操作（3个）
    │   │   │   ├── pdf/                                 · PDF 操作（2个）
    │   │   │   ├── image/                               · 图片处理（2个）
    │   │   │   ├── ppt/                                 · PPT 操作（2个）
    │   │   │   ├── file/                                · 文件操作（4个）
    │   │   │   ├── convert/                             · 格式转换（2个）
    │   │   │   ├── tool/                                · 通用工具（6个）
    │   │   │   ├── system/                              · 系统诊断（2个）
    │   │   │   ├── fetch/                               · HTTP 请求（2个）
    │   │   │   └── ...                                  · 其他专项 Handler
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

## 3. 项目架构

### 3.1 Handler 继承体系

```
BaseHandler（抽象基类）
  ├── execute()          · 统一异常处理，区分业务异常和系统异常
  ├── executeWithAudit() · 统一异常处理 + 审计日志 + 耗时统计
  │
  ├── WordBaseHandler（Word 文档操作基类）
  │     ├── validateDocxFile()  · 校验 .docx 文件
  │     ├── openDocument()      · 打开 Word 文档
  │     ├── saveDocument()      · 保存 Word 文档
  │     └── 子类: WordHandler、WordAdvancedHandler
  │
  ├── ExcelBaseHandler（Excel 共享基类）
  │     └── 子类: ExcelReadHandler、ExcelWriteHandler、ExcelFormatHandler、ExcelAnalyzeHandler
  │
  └── 其他直接子类: PptHandler、PdfHandler、TxtHandler、MarkdownHandler 等
```

**BaseHandler** 是项目所有 Handler 的抽象基类，提供以下核心能力：

| 方法 | 说明 |
|------|------|
| `execute(String toolName, ThrowingSupplier<String> action)` | 执行业务逻辑，自动捕获异常并记录日志，区分业务异常和系统异常 |
| `executeWithAudit(String toolName, String params, ThrowingSupplier<String> action)` | 在 execute 基础上增加审计日志记录和耗时统计 |

**WordBaseHandler** 继承 BaseHandler，为 Word 文档操作提供公共方法，供 `WordHandler` 和 `WordAdvancedHandler` 复用，避免重复代码。

**ExcelBaseHandler** 为 Excel 操作提供共享辅助方法（如工作表渲染为图片等），供 4 个 Excel 子 Handler 复用。

### 3.2 日志基础设施

| 组件 | 说明 |
|------|------|
| `LogUtil` | 异步文件日志工具，基于 BlockingQueue + daemon 线程实现。日志写入 `logs/mcp-office-toolbox.log`，每日自动轮转备份为 `mcp-office-toolbox_yyyy_MM_dd.log`。支持 info / error / warn / debug 四个级别 |
| `AuditLogger` | 异步操作审计日志工具，记录每次 Tool 调用的工具名、参数、结果和耗时。日志写入 `logs/audit.log`，每日自动轮转备份为 `audit_yyyy_MM_dd.log`。通过 `BaseHandler.executeWithAudit()` 自动集成，无需手动调用 |

### 3.3 工具类

| 组件 | 说明 |
|------|------|
| `FileValidateUtil` | 文件校验工具，校验文件是否存在且为指定扩展名 |
| `PathUtil` | 路径操作工具 |
| `ZipUtil` | ZIP 压缩/解压工具 |
| `SslUtil` | SSL/TLS 工具，支持自定义信任证书、客户端证书（mTLS）和不安全模式 |
| `FormatUtil` | 通用格式化工具，字节数、运行时间等格式化方法 |
| `CsvUtil` | CSV 解析工具，提供 CSV 行解析方法 |

---

## 4. 工具列表与使用方式

### 3.0 CommandHandler（命令行执行）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `command_execute` | 执行系统命令并返回原文输出，支持超时 | `command_execute(command="git status", workDir="/path/to/project", timeout=30)` |

### 3.1 FileSystemHandler（通用文件系统）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `read_text_file` | 读取文本文件内容，支持 head/tail 参数 | `fs_read_text_file(path="test.txt", head=10)` |
| `read_media_file` | 读取图片/音频文件，返回 base64 | `fs_read_media_file(path="image.png")` |
| `read_multiple_files` | 批量读取多个文件 | `fs_read_multiple_files(paths=["a.txt", "b.txt"])` |
| `write_file` | 创建或覆盖文件 | `fs_write_file(path="test.txt", content="Hello")` |
| `edit_file` | 基于行的精确编辑 | `fs_edit_file(request={path, edits, dryRun})` |
| `create_directory` | 创建目录（支持嵌套） | `fs_create_directory(path="newdir")` |
| `list_directory` | 列出目录内容 | `fs_list_directory(path=".")` |
| `list_directory_with_sizes` | 列出目录内容（含文件大小） | `fs_list_directory_with_sizes(path=".", sortBy="size")` |
| `directory_tree` | 递归获取目录树（JSON 格式） | `fs_directory_tree(path=".", excludePatterns=["node_modules"])` |
| `move_file` | 移动或重命名文件/目录 | `fs_move_file(source="a.txt", destination="b.txt")` |
| `search_files` | 按 glob 模式搜索文件 | `fs_search_files(path=".", pattern="**/*.java")` |
| `get_file_info` | 获取文件/目录元数据 | `fs_get_file_info(path="test.txt")` |
| `list_allowed_directories` | 查看允许访问的目录 | `fs_list_allowed_directories()` |
| `copy_file` | 复制文件或目录 | `fs_copy_file(source="a.txt", destination="b.txt")` |
| `delete_file` | 删除文件或目录（递归） | `fs_delete_file(path="test.txt")` |
| `append_to_file` | 向文件末尾追加内容 | `fs_append_to_file(path="test.txt", content="World")` |
| `clear_file` | 清空文件内容 | `fs_clear_file(path="test.txt")` |
| `search_in_file` | 在文件中搜索关键词 | `fs_search_in_file(path="test.txt", keyword="hello")` |
| `replace_in_file` | 替换文件中的文本 | `fs_replace_in_file(path="test.txt", oldText="hello", newText="world")` |

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
| `pdf_merge` | 合并多个 PDF 文件 | `pdf_merge(sourceFilePaths="a.pdf,b.pdf", targetFilePath="merged.pdf")` |
| `pdf_split` | 按页码范围拆分 PDF | `pdf_split(fileAbsolutePath="doc.pdf", pageRanges="1-3,5,7-9", outputDir="./output")` |
| `pdf_create_from_text` | 从文本内容生成 PDF | `pdf_create_from_text(textContent="Hello", targetFilePath="out.pdf", title="标题", fontSize=12)` |
| `pdf_extract_pages` | 从 PDF 中提取指定页面 | `pdf_extract_pages(fileAbsolutePath="doc.pdf", pageNumbers="1,3-5", targetFilePath="extracted.pdf")` |
| `pdf_add_watermark` | 为 PDF 添加水印文字 | `pdf_add_watermark(fileAbsolutePath="doc.pdf", watermarkText="机密", opacity=0.3, rotation=45)` |
| `pdf_to_images` | 将 PDF 页面转换为图片 | `pdf_to_images(fileAbsolutePath="doc.pdf", outputDir="./images", imageFormat="png", resolution=150)` |
| `pdf_rotate_page` | 旋转 PDF 指定页面（90/180/270度） | `pdf_rotate_page(fileAbsolutePath="doc.pdf", pageNumber=1, rotation=90)` |
| `pdf_extract_images` | 提取 PDF 中嵌入的图片 | `pdf_extract_images(fileAbsolutePath="doc.pdf", outputDir="./images")` |

### 3.5 WordHandler（Word 文档）

**读写操作**

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `create_word_doc` | 创建空白 Word 文档 | `create_word_doc(fileAbsolutePath="doc.docx")` |
| `read_word_full` | 读取 Word 全文 | `read_word_full(fileAbsolutePath="doc.docx")` |
| `read_word_paragraphs` | 逐段读取 Word（含样式和对齐） | `read_word_paragraphs(fileAbsolutePath="doc.docx")` |
| `read_word_structured` | 结构化读取（标题/段落/格式/表格/图片），适用于 AI 转 Markdown | `read_word_structured(fileAbsolutePath="doc.docx")` |
| `save_word_as` | 另存为新文件 | `save_word_as(sourcePath="doc.docx", targetPath="doc2.docx")` |
| `word_count` | 字数/段落/表格/图片统计 | `word_count(fileAbsolutePath="doc.docx")` |
| `word_find` | 在文档中搜索关键词，返回匹配位置 | `word_find(fileAbsolutePath="doc.docx", keyword="搜索词", caseSensitive=false)` |

**内容编辑**

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `insert_word_text` | 插入文字 | `insert_word_text(fileAbsolutePath="doc.docx", text="新内容")` |
| `insert_word_heading` | 插入标题 | `insert_word_heading(fileAbsolutePath="doc.docx", level=2, text="标题")` |
| `insert_word_paragraph` | 插入自定义段落（支持粗体/斜体/字号/颜色） | `insert_word_paragraph(fileAbsolutePath="doc.docx", text="段落", bold=true, fontSize=14, color="#FF0000")` |
| `modify_word_content` | 修改文档内容 | `modify_word_content(fileAbsolutePath="doc.docx", oldText="旧", newText="新")` |
| `replace_word_keywords` | 批量替换关键词 | `replace_word_keywords(fileAbsolutePath="doc.docx", replacements={"旧":"新"})` |
| `word_delete_paragraph` | 删除指定索引的段落 | `word_delete_paragraph(fileAbsolutePath="doc.docx", paragraphIndex=2)` |

**表格与图片**

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `insert_word_table` | 插入表格 | `insert_word_table(fileAbsolutePath="doc.docx", rows="姓名,年龄\n张三,25")` |
| `read_word_tables` | 读取所有表格数据 | `read_word_tables(fileAbsolutePath="doc.docx")` |
| `insert_word_image` | 插入图片（支持 PNG/JPG/GIF/BMP） | `insert_word_image(fileAbsolutePath="doc.docx", imagePath="pic.png", width=200, height=150)` |

**排版与页面设置**

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `word_page_setup` | 页面设置（纸张/方向/页边距） | `word_page_setup(fileAbsolutePath="doc.docx", paperSize="A4", orientation="portrait")` |
| `word_add_page_break` | 添加分页符 | `word_add_page_break(fileAbsolutePath="doc.docx")` |
| `word_set_paragraph_alignment` | 设置段落对齐 | `word_set_paragraph_alignment(fileAbsolutePath="doc.docx", paragraphIndex=0, alignment="CENTER")` |
| `word_set_line_spacing` | 设置行间距（倍数） | `word_set_line_spacing(fileAbsolutePath="doc.docx", lineSpacing=1.5)` |
| `word_set_paragraph_spacing` | 设置段前段后间距和行距 | `word_set_paragraph_spacing(fileAbsolutePath="doc.docx", paragraphIndex=-1, beforeSpacing=6, afterSpacing=6, lineSpacing=1.5)` |
| `word_set_font` | 统一设置文档字体和字号 | `word_set_font(fileAbsolutePath="doc.docx", fontName="宋体", fontSize=12, scope="all")` |
| `word_add_header_footer` | 添加页眉页脚 | `word_add_header_footer(fileAbsolutePath="doc.docx", headerText="页眉", footerText="页脚")` |
| `word_add_page_number` | 添加页码 | `word_add_page_number(fileAbsolutePath="doc.docx")` |

**文档元素**

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `word_add_bullet_list` | 添加项目符号/编号列表 | `word_add_bullet_list(fileAbsolutePath="doc.docx", items="第一项\n第二项", numbered=false)` |
| `word_add_hyperlink` | 添加超链接 | `word_add_hyperlink(fileAbsolutePath="doc.docx", text="点击访问", url="https://example.com")` |
| `word_add_table_of_contents` | 插入目录域（需在 Word 中刷新） | `word_add_table_of_contents(fileAbsolutePath="doc.docx")` |
| `word_add_watermark` | 添加水印文字 | `word_add_watermark(fileAbsolutePath="doc.docx", watermarkText="机密")` |
| `word_add_comment` | 添加批注 | `word_add_comment(fileAbsolutePath="doc.docx", paragraphIndex=0, commentText="批注内容", author="Reviewer")` |

**转换与高级操作**

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `word_convert_to_pdf` | Word 转 PDF | `word_convert_to_pdf(sourcePath="doc.docx", targetPath="output.pdf")` |
| `word_convert_to_markdown` | Word 文档转换为 Markdown 格式 | `word_convert_to_markdown(fileAbsolutePath="doc.docx", targetFilePath="output.md")` |
| `word_merge_documents` | 合并多个 Word 文档 | `word_merge_documents(targetPath="merged.docx", sourceFilePaths="doc1.docx,doc2.docx")` |
| `word_extract_images` | 提取文档中所有图片 | `word_extract_images(fileAbsolutePath="doc.docx", outputDir="./images")` |

### 3.6.1 ExcelReadHandler（Excel 读取）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `excel_read_describe_sheets` | 列出所有工作表信息 | `excel_read_describe_sheets(fileAbsolutePath="book.xlsx")` |
| `excel_read_sheet` | 读取工作表数据（支持分页/公式/样式） | `excel_read_sheet(fileAbsolutePath="book.xlsx", sheetName="Sheet1", range="A1:C10")` |
| `excel_read_screen_capture` | 截取工作表截图（base64 PNG） | `excel_read_screen_capture(fileAbsolutePath="book.xlsx", sheetName="Sheet1")` |
| `excel_read_analyze` | 数据摘要分析（自动识别数值列并计算汇总统计） | `excel_read_analyze(fileAbsolutePath="book.xlsx", sheetName="Sheet1", range="A1:C50")` |
| `excel_read_compare` | 对比两个工作表的差异 | `excel_read_compare(filePath1="a.xlsx", sheetName1="Sheet1", filePath2="b.xlsx", sheetName2="Sheet1", keyColumnIndex=0)` |
| `excel_read_preview` | 预览工作表内容（前 N 行数据） | `excel_read_preview(fileAbsolutePath="book.xlsx", sheetName="Sheet1", rows=10)` |

### 3.6.2 ExcelWriteHandler（Excel 写入）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `excel_write_create_workbook` | 创建空白工作簿 | `excel_write_create_workbook(fileAbsolutePath="book.xlsx")` |
| `excel_write_data` | 写入工作表数据 | `excel_write_data(fileAbsolutePath="book.xlsx", sheetName="Sheet1", newSheet=false, range="A1", values=[["姓名","年龄"],["张三",25]])` |
| `excel_write_copy_sheet` | 复制工作表 | `excel_write_copy_sheet(fileAbsolutePath="book.xlsx", srcSheetName="Sheet1", dstSheetName="Sheet2")` |
| `excel_write_delete_row` | 删除指定行 | `excel_write_delete_row(fileAbsolutePath="book.xlsx", sheetName="Sheet1", rowIndex=0)` |
| `excel_write_delete_column` | 删除指定列 | `excel_write_delete_column(fileAbsolutePath="book.xlsx", sheetName="Sheet1", columnIndex=0)` |
| `excel_write_clear_sheet` | 清空工作表数据 | `excel_write_clear_sheet(fileAbsolutePath="book.xlsx", sheetName="Sheet1")` |
| `excel_write_insert_row` | 插入空行 | `excel_write_insert_row(fileAbsolutePath="book.xlsx", sheetName="Sheet1", rowIndex=1)` |
| `excel_write_insert_column` | 插入空列 | `excel_write_insert_column(fileAbsolutePath="book.xlsx", sheetName="Sheet1", columnIndex=1)` |
| `excel_write_delete_sheet` | 删除工作表 | `excel_write_delete_sheet(fileAbsolutePath="book.xlsx", sheetName="Sheet2")` |
| `excel_write_rename_sheet` | 重命名工作表 | `excel_write_rename_sheet(fileAbsolutePath="book.xlsx", oldName="Sheet1", newName="数据表")` |
| `excel_write_split_by_column` | 按指定列的值拆分为多个独立文件 | `excel_write_split_by_column(fileAbsolutePath="book.xlsx", sheetName="Sheet1", columnIndex=0, outputDir="./output")` |
| `excel_write_add_image` | 在工作表中插入图片 | `excel_write_add_image(fileAbsolutePath="book.xlsx", sheetName="Sheet1", imagePath="chart.png", row=0, col=0)` |
| `excel_write_batch` | 批量写入多个工作表数据 | `excel_write_batch(fileAbsolutePath="book.xlsx", sheetData=...)` |
| `excel_write_export_to_pdf` | Excel 工作表导出为 PDF | `excel_write_export_to_pdf(fileAbsolutePath="book.xlsx", sheetName="Sheet1", targetPath="output.pdf")` |

### 3.6.3 ExcelFormatHandler（Excel 格式化）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `excel_format_range` | 格式化单元格（字体/填充/边框） | `excel_format_range(fileAbsolutePath="book.xlsx", sheetName="Sheet1", range="A1:C3", styles=[...])` |
| `excel_format_create_table` | 创建表格（XLSX） | `excel_format_create_table(fileAbsolutePath="book.xlsx", sheetName="Sheet1", tableName="MyTable", range="A1:C10")` |
| `excel_format_auto_fit_columns` | 自动调整列宽 | `excel_format_auto_fit_columns(fileAbsolutePath="book.xlsx", sheetName="Sheet1", columnIndices="0,1,2")` |
| `excel_format_set_column_width` | 设置列宽（字符数） | `excel_format_set_column_width(fileAbsolutePath="book.xlsx", sheetName="Sheet1", columnIndex=0, width=20)` |
| `excel_format_set_row_height` | 设置行高（磅值） | `excel_format_set_row_height(fileAbsolutePath="book.xlsx", sheetName="Sheet1", rowIndex=0, height=30)` |
| `excel_format_freeze_panes` | 冻结窗格 | `excel_format_freeze_panes(fileAbsolutePath="book.xlsx", sheetName="Sheet1", colSplit=1, rowSplit=1)` |
| `excel_format_merge_cells` | 合并/取消合并单元格 | `excel_format_merge_cells(fileAbsolutePath="book.xlsx", sheetName="Sheet1", range="A1:C1", merge=true)` |
| `excel_format_conditional` | 添加条件格式 | `excel_format_conditional(fileAbsolutePath="book.xlsx", sheetName="Sheet1", range="A2:A10", type="cellValue", operator=">", value="10", fillColor="RED")` |
| `excel_format_chart_create` | 创建图表（柱状图/折线图/饼图/面积图/散点图） | `excel_format_chart_create(fileAbsolutePath="book.xlsx", sheetName="Sheet1", chartType="bar", categoryRange="A2:A10", valueRange="B2:B10", chartTitle="销售统计")` |
| `excel_format_named_range` | 创建命名区域 | `excel_format_named_range(fileAbsolutePath="book.xlsx", sheetName="Sheet1", name="MyRange", range="A1:C10")` |
| `excel_format_print_setup` | 设置打印区域和打印选项 | `excel_format_print_setup(fileAbsolutePath="book.xlsx", sheetName="Sheet1", printArea="A1:F50", orientation="landscape")` |

### 3.6.4 ExcelAnalyzeHandler（Excel 分析）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `excel_analyze_sort_range` | 按列排序（升序/降序） | `excel_analyze_sort_range(fileAbsolutePath="book.xlsx", sheetName="Sheet1", range="A1:C10", sortColumnIndex=0, ascending=true)` |
| `excel_analyze_filter_range` | 添加自动筛选 | `excel_analyze_filter_range(fileAbsolutePath="book.xlsx", sheetName="Sheet1", range="A1:C10")` |
| `excel_analyze_find_replace` | 查找并替换文本 | `excel_analyze_find_replace(fileAbsolutePath="book.xlsx", sheetName="Sheet1", findText="旧", replaceText="新")` |
| `excel_analyze_remove_duplicates` | 按列去重 | `excel_analyze_remove_duplicates(fileAbsolutePath="book.xlsx", sheetName="Sheet1", columnIndices="0,1")` |
| `excel_analyze_apply_formula` | 批量应用公式 | `excel_analyze_apply_formula(fileAbsolutePath="book.xlsx", sheetName="Sheet1", target="D2", formula="=SUM(A2:A10)")` |
| `excel_analyze_data_validation` | 添加下拉列表验证 | `excel_analyze_data_validation(fileAbsolutePath="book.xlsx", sheetName="Sheet1", range="A2:A10", allowedValues="是,否")` |
| `excel_analyze_convert_csv` | CSV ↔ Excel 互转 | `excel_analyze_convert_csv(sourceFilePath="data.csv", targetFilePath="data.xlsx")` |
| `excel_analyze_merge_workbooks` | 合并多个工作簿 | `excel_analyze_merge_workbooks(targetFilePath="merged.xlsx", sourceFilePaths="a.xlsx,b.xlsx")` |
| `excel_analyze_pivot_table` | 创建数据透视表 | `excel_analyze_pivot_table(fileAbsolutePath="book.xlsx", sheetName="Sheet1", sourceRange="A1:C50", targetSheetName="透视表", targetCell="A1", rowField="姓名", dataField="金额", operation="SUM")` |
| `excel_analyze_protect_sheet` | 保护工作表或工作簿结构 | `excel_analyze_protect_sheet(fileAbsolutePath="book.xlsx", sheetName="Sheet1", password="123456", protectSheet=true)` |
| `excel_analyze_subtotal` | 按列分组并汇总计算 | `excel_analyze_subtotal(fileAbsolutePath="book.xlsx", sheetName="Sheet1", groupByColumnIndex=0, subtotalColumnIndex=1, operation="SUM")` |
| `excel_analyze_group_rows` | 将指定行范围分组折叠 | `excel_analyze_group_rows(fileAbsolutePath="book.xlsx", sheetName="Sheet1", startRow=1, endRow=5)` |

### 3.7 PptHandler（PPT 演示文稿）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `create_ppt` | 创建空白 PPT | `create_ppt(fileAbsolutePath="slide.pptx")` |
| `add_ppt_slide` | 添加新幻灯片 | `add_ppt_slide(fileAbsolutePath="slide.pptx", title="标题")` |
| `delete_ppt_slide` | 删除幻灯片 | `delete_ppt_slide(fileAbsolutePath="slide.pptx", slideIndex=0)` |
| `read_ppt_text` | 读取所有幻灯片文本 | `read_ppt_text(fileAbsolutePath="slide.pptx")` |
| `modify_ppt_slide_text` | 修改幻灯片文本 | `modify_ppt_slide_text(fileAbsolutePath="slide.pptx", slideIndex=0, oldText="旧", newText="新")` |
| `save_ppt_as` | 另存为新文件 | `save_ppt_as(sourcePath="slide.pptx", targetPath="slide2.pptx")` |
| `ppt_add_text_box` | 在幻灯片上添加文本框 | `ppt_add_text_box(fileAbsolutePath="slide.pptx", slideIndex=0, text="内容", x=100, y=100, width=500, height=200, fontSize=18, bold=true, color="#333333")` |
| `ppt_add_image` | 在幻灯片上插入图片 | `ppt_add_image(fileAbsolutePath="slide.pptx", slideIndex=0, imagePath="image.png", x=100, y=100, width=400, height=300)` |
| `ppt_add_table` | 在幻灯片上添加表格 | `ppt_add_table(fileAbsolutePath="slide.pptx", slideIndex=0, rows=3, cols=2, data="姓名,年龄;张三,25;李四,30")` |
| `ppt_duplicate_slide` | 复制幻灯片到指定位置 | `ppt_duplicate_slide(fileAbsolutePath="slide.pptx", slideIndex=0, targetIndex=-1)` |
| `ppt_add_notes` | 为幻灯片添加演讲者备注 | `ppt_add_notes(fileAbsolutePath="slide.pptx", slideIndex=0, notes="演讲要点")` |
| `ppt_reorder_slides` | 调整幻灯片顺序 | `ppt_reorder_slides(fileAbsolutePath="slide.pptx", slideOrder="2,0,1,3")` |
| `ppt_convert_to_pdf` | 将 PPT 转换为 PDF | `ppt_convert_to_pdf(fileAbsolutePath="slide.pptx", targetFilePath="output.pdf")` |
| `ppt_merge` | 合并多个 PPT 文件 | `ppt_merge(sourceFilePaths="a.pptx,b.pptx", targetFilePath="merged.pptx")` |
| `ppt_set_slide_layout` | 设置幻灯片版式 | `ppt_set_slide_layout(fileAbsolutePath="slide.pptx", slideIndex=0, layoutName="标题和内容")` |
| `ppt_set_background` | 设置幻灯片背景（纯色/图片/渐变） | `ppt_set_background(fileAbsolutePath="slide.pptx", slideIndex=0, backgroundType="solid", color="#FFFFFF")` |

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
| `zip_compress_directory` | 快速压缩单个目录为 ZIP | `zip_compress_directory(dirPath="./mydir", zipOutputPath="archive.zip")` |
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
| `resolve-library-id` | 解析库名称为标准 ID | `context7_resolve_library_id(request={query:"I need to manage state", libraryName:"react"})` |
| `query-docs` | 查询最新文档 | `context7_query_docs(request={libraryId:"/facebook/react", query:"useState hook", tokens:5000})` |

### 3.12 ImageHandler（图片处理）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `image_compress` | 压缩图片，支持自定义质量 | `image_compress(fileAbsolutePath="photo.jpg", quality=0.7, targetPath="compressed.jpg")` |
| `image_resize` | 缩放图片尺寸 | `image_resize(fileAbsolutePath="photo.jpg", width=800, height=600, targetPath="resized.jpg")` |
| `image_convert` | 图片格式转换（PNG/JPG/WEBP/BMP） | `image_convert(fileAbsolutePath="photo.png", targetFormat="jpg", targetPath="converted.jpg")` |

### 3.13 ConvertHandler（跨格式转换）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `html_to_pdf` | 将 HTML 文件或内容转换为 PDF | `convert_html_to_pdf(htmlFilePath="report.html", targetPath="report.pdf")` |
| `md_to_pdf` | 将 Markdown 文件或内容转换为 PDF | `convert_md_to_pdf(mdFilePath="doc.md", targetPath="doc.pdf")` |
| `md_to_docx` | 将 Markdown 文件或内容转换为 Word | `convert_md_to_docx(mdFilePath="doc.md", targetPath="doc.docx")` |

### 3.14 QrCodeHandler（二维码工具）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `qr_code_generate` | 生成二维码图片 | `qr_code_generate(content="https://example.com", targetPath="qr.png", width=300, height=300)` |
| `qr_code_read` | 解析/读取二维码内容 | `qr_code_read(imagePath="qr.png")` |

### 3.15 ToolHandler（通用工具）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `encode_convert` | 文件编码转换（GBK↔UTF-8等） | `tool_encode_convert(filePath="doc.txt", fromEncoding="GBK", toEncoding="UTF-8")` |
| `regex_test` | 正则表达式测试（匹配/查找/替换） | `tool_regex_test(text="Hello 123", pattern="\\d+", mode="find")` |

### 3.16 AuditLogger（操作审计）

| 功能 | 说明 |
|------|------|
| 异步日志记录 | 所有 Tool 调用自动记录到 `logs/audit.log`，格式：`[时间戳] [耗时ms] 工具名 参数 结果` |
| 每日轮转 | 每天自动备份为 `audit_yyyy_MM_dd.log` |
| 集成方式 | 每个 @Tool 方法调用前后自动记录，无需手动配置 |

---

## 5. 构建

```bash
cd mcp-office-toolbox

# 编译（仅编译，不运行测试）
mvn compile

# 运行测试
mvn test

# 打包（跳过测试）
mvn clean package -DskipTests
# 产物：target/mcp-office-toolbox-*.jar
```

---

## 6. 开发规范

### 6.1 Handler 开发规范

- 新增 Handler 必须继承 `BaseHandler` 或 `WordBaseHandler`（Word 文档操作场景）
- 使用 `LogUtil` 进行日志输出，**禁止使用 `System.out.println`**
- 使用 `BaseHandler.execute()` 或 `executeWithAudit()` 模板方法包装业务逻辑
- Handler 必须是**无状态**的（`@Service` 单例），不能声明实例变量

### 6.2 注解规范

- `@ToolParam` 的 `description` 必须使用**中文**
- `@Tool` 的 `name` 使用 **snake_case** 命名，遵循 `模块前缀_动作_目标` 模式
- `@Tool` 的 `description` 使用**中文**描述

### 6.3 代码规范

- 所有 Java 类必须符合 **JavaDoc 规范**（类级别、公共方法级别）
- 使用 `@author` 和 `@since` 标注作者和创建日期
- 项目基于 **JDK 21**，可使用虚拟线程、Record 类、Switch 表达式等特性
- 所有 AutoCloseable 资源必须使用 **try-with-resources** 管理

### 6.4 资源管理

- 文件操作必须使用 try-with-resources 确保资源释放
- 优先使用 `java.nio.file.Files` 和 `Path` API（而非 `java.io.File`）
- 使用 `FileValidateUtil` 校验文件路径和扩展名

---

## 7. 作为 MCP Server 挂载

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

## 8. 调试

```bash
npx @modelcontextprotocol/inspector java -jar target/mcp-office-toolbox-*.jar
```

浏览器 UI 可手动调用工具、查看 JSON-RPC 流量。

---

## 9. 工具命名规范

所有 MCP 工具采用 **snake_case 命名**，遵循 `模块前缀_动作_目标` 的命名模式：

| 模块 | 前缀 | 示例 |
|------|------|------|
| 命令行 | `command` | `command_execute` |
| 文件系统 | `fs` | `fs_read_text_file`, `fs_write_file`, `fs_list_directory` |
| Word | `word` | `create_word_doc`, `word_page_setup`, `read_word_structured` |
| Excel 读取 | `excel_read` | `excel_read_describe_sheets`, `excel_read_sheet`, `excel_read_preview` |
| Excel 写入 | `excel_write` | `excel_write_create_workbook`, `excel_write_data`, `excel_write_batch` |
| Excel 格式化 | `excel_format` | `excel_format_range`, `excel_format_chart_create`, `excel_format_print_setup` |
| Excel 分析 | `excel_analyze` | `excel_analyze_sort_range`, `excel_analyze_pivot_table`, `excel_analyze_subtotal` |
| PDF | `pdf` | `read_pdf_text`, `get_pdf_info`, `pdf_merge`, `pdf_rotate_page` |
| PPT | `ppt` | `create_ppt`, `read_ppt_text`, `ppt_set_slide_layout` |
| TXT | `txt` | `create_txt_file`, `read_txt_full` |
| Markdown | `md` | `create_md_file`, `md_generate_heading` |
| CSV | `csv` | `csv_create`, `csv_read`, `csv_write` |
| ZIP | `zip` | `zip_compress`, `zip_decompress`, `zip_list` |
| Fetch | `fetch` | `fetch_html`, `fetch_json`, `fetch_txt`, `fetch_markdown` |
| Context7 | `context7` | `context7_resolve_library_id`, `context7_query_docs` |
| Image | `image` | `image_compress`, `image_resize`, `image_convert` |
| Convert | `convert` | `convert_html_to_pdf`, `convert_md_to_pdf`, `convert_md_to_docx` |
| QrCode | `qr_code` | `qr_code_generate`, `qr_code_read` |
| Tool | `tool` | `tool_encode_convert`, `tool_regex_test` |

---

### 3.16 TextToolHandler（文本处理）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `text_count` | 统计文本字数/行数/段落/字符数 | `text_count(text="Hello World")` |
| `text_sort_lines` | 文本按行排序/去重/反转 | `text_sort_lines(text="c\na\nb", order="asc", unique=true)` |
| `text_case_convert` | 大小写转换（upper/lower/camel/snake/kebab） | `text_case_convert(text="hello world", mode="camel")` |
| `text_extract` | 提取邮箱/手机号/URL/IP | `text_extract(text="邮箱:test@ex.com", extractType="email")` |
| `text_trim` | 去除空行/首尾空格/重复行 | `text_trim(text="a\n\nb\nb", trimMode="dedup")` |
| `text_wrap` | 按指定宽度自动换行 | `text_wrap(text="长文本", width=40)` |
| `text_generate` | 生成UUID/随机字符串 | `text_generate(generateType="uuid")` |
| `text_url_encode_decode` | URL 编解码 | `text_url_encode_decode(content="测试", mode="encode")` |
| `text_base64_encode_decode` | Base64 编解码 | `text_base64_encode_decode(content="hello", mode="encode")` |
| `text_hash_calculate` | 计算 SHA-256/SHA-512/MD5 哈希 | `text_hash_calculate(text="hello", algorithm="sha256")` |

### 3.17 JsonDataHandler（结构化数据处理）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `json_format` | JSON 格式化美化 | `json_format(jsonContent="{\"a\":1}")` |
| `json_validate` | JSON 格式校验 | `json_validate(jsonContent="{\"a\":1}")` |
| `json_query` | JSONPath 查询 | `json_query(jsonContent="{\"data\":[{\"n\":\"x\"}]}", jsonPath="data[0].n")` |
| `json_to_csv` | JSON 数组转 CSV | `json_to_csv(jsonContent="[{\"n\":\"a\"}]", delimiter=",")` |
| `json_to_yaml` | JSON 转 YAML | `json_to_yaml(jsonContent="{\"name\":\"test\"}")` |
| `yaml_to_json` | YAML 转 JSON | `yaml_to_json(yamlContent="name: test")` |
| `json_to_xml` | JSON 转 XML | `json_to_xml(jsonContent="{\"root\":{\"item\":1}}")` |
| `xml_to_json` | XML 转 JSON | `xml_to_json(xmlContent="<root><a>1</a></root>")` |
| `xml_validate` | XML 格式校验 | `xml_validate(xmlContent="<root></root>")` |
| `json_to_properties` | JSON 转 Properties 格式 | `json_to_properties(jsonContent="{\"db.host\":\"localhost\"}")` |
| `csv_to_json` | CSV 转 JSON 数组 | `csv_to_json(csvContent="name,age\n张三,25")` |
| `xml_format` | XML 格式化美化 | `xml_format(xmlContent="<root><a>1</a></root>")` |

### 3.18 DateTimeHandler（日期时间处理）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `date_calc` | 日期加减运算 | `date_calc(dateStr="2026-01-01", days=30)` |
| `date_diff` | 计算日期差异 | `date_diff(startDate="2026-01-01", endDate="2026-01-31")` |
| `date_format` | 日期格式转换（时间戳↔字符串） | `date_format(input="2026-01-15", inputType="datetime", outFormat="yyyy/MM/dd")` |
| `timezone_convert` | 时区转换 | `timezone_convert(dateTimeStr="2026-01-15 10:00:00", toZone="UTC")` |
| `workday_calc` | 工作日天数计算 | `workday_calc(startDate="2026-01-05", endDate="2026-01-09")` |
| `calendar_generate` | 生成文本日历 | `calendar_generate(year=2026, month=7)` |
| `date_weekday` | 查询日期星期 | `date_weekday(dateStr="2026-01-01")` |

### 3.19 DocumentDiffHandler（文档对比）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `diff_text` | 文本文件行级差异对比 | `diff_text(filePath1="a.txt", filePath2="b.txt")` |
| `diff_word_text` | Word 文档文本差异对比 | `diff_word_text(filePath1="a.docx", filePath2="b.docx")` |
| `diff_pdf_text` | PDF 文档文本差异对比 | `diff_pdf_text(filePath1="a.pdf", filePath2="b.pdf")` |
| `diff_directories` | 目录文件列表差异对比 | `diff_directories(dirPath1="./dir1", dirPath2="./dir2")` |

### 3.20 HttpClientHandler（HTTP 客户端）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `http_get` | HTTP GET 请求，支持自定义请求头 | `http_get(url="https://api.example.com/data", headers="{\"Authorization\":\"Bearer xxx\"}")` |
| `http_post` | HTTP POST 请求，支持 JSON 请求体和自定义请求头 | `http_post(url="https://api.example.com/create", body="{\"key\":\"val\"}", headers="{\"Authorization\":\"Bearer xxx\"}")` |
| `http_put` | HTTP PUT 请求，用于更新资源 | `http_put(url="https://api.example.com/resource/1", body="{\"name\":\"updated\"}", headers="{\"Authorization\":\"Bearer xxx\"}")` |
| `http_delete` | HTTP DELETE 请求，用于删除资源 | `http_delete(url="https://api.example.com/resource/1", headers="{\"Authorization\":\"Bearer xxx\"}")` |
| `http_patch` | HTTP PATCH 请求，用于部分更新资源 | `http_patch(url="https://api.example.com/resource/1", body="{\"status\":\"active\"}", headers="{\"Authorization\":\"Bearer xxx\"}")` |
| `http_head` | HTTP HEAD 请求，仅返回响应头和状态码 | `http_head(url="https://example.com")` |
| `http_download` | 下载文件到本地磁盘 | `http_download(url="https://example.com/file.pdf", targetPath="file.pdf")` |

**SSL/TLS 证书配置：** 所有 `http_*` 工具均支持通过 `sslConfig` 参数传入 SSL 配置，无需在配置文件中固定：

```json
{
  "insecure": true,                              // 跳过证书验证（仅测试环境）
  "trustStorePath": "/path/to/truststore.jks",   // 信任证书库路径（JKS/PKCS12）
  "trustStorePassword": "changeit",              // 信任证书库密码
  "keyStorePath": "/path/to/keystore.jks",       // 客户端证书路径（mTLS）
  "keyStorePassword": "changeit"                 // 客户端证书密码
}
```

所有字段均为可选，不传 `sslConfig` 则使用 JDK 默认证书验证。

### 3.21 ChartHandler（图表生成）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `chart_bar` | 生成柱状图 PNG | `chart_bar(dataJson="{\"labels\":[\"A\",\"B\"],\"values\":[10,20]}", outputPath="bar.png")` |
| `chart_pie` | 生成饼图 PNG | `chart_pie(dataJson="{\"labels\":[\"A\",\"B\"],\"values\":[30,70]}", outputPath="pie.png")` |
| `chart_line` | 生成折线图 PNG | `chart_line(dataJson="{\"labels\":[\"1月\",\"2月\"],\"values\":[10,20]}", outputPath="line.png")` |

### 3.22 BatchFileHandler（批量文件处理）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `batch_rename` | 批量重命名文件 | `batch_rename(dirPath="./output", oldPrefix="old_", newPrefix="new_")` |
| `batch_replace_text` | 批量文本替换 | `batch_replace_text(dirPath="./docs", searchText="旧词", replaceText="新词")` |
| `batch_convert_encoding` | 批量编码转换 | `batch_convert_encoding(dirPath="./files", fromEncoding="GBK", toEncoding="UTF-8")` |
| `find_duplicate_files` | 查找重复文件 | `find_duplicate_files(dirPath="./downloads")` |

### 3.23 ImageAdvancedHandler（高级图片处理）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `image_crop` | 裁剪图片区域 | `image_crop(fileAbsolutePath="photo.jpg", x=10, y=10, width=200, height=200)` |
| `image_rotate` | 旋转图片 | `image_rotate(fileAbsolutePath="photo.jpg", angle=90)` |
| `image_watermark` | 添加文字水印 | `image_watermark(fileAbsolutePath="photo.jpg", text="机密", position="center")` |
| `image_info` | 获取图片元信息 | `image_info(fileAbsolutePath="photo.jpg")` |
| `image_concat` | 拼接多张图片 | `image_concat(filePaths="a.jpg,b.jpg", direction="horizontal")` |
| `image_add_text` | 图片添加文字标注 | `image_add_text(fileAbsolutePath="photo.jpg", text="标注", x=50, y=50)` |
| `image_to_base64` | 图片转 Base64 | `image_to_base64(fileAbsolutePath="photo.jpg")` |
| `base64_to_image` | Base64 转图片 | `base64_to_image(base64Content="iVBORw...", outputPath="out.png")` |

### 3.24 CryptoHandler（加密解密）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `crypto_aes_encrypt` | AES 加密 | `crypto_aes_encrypt(plainText="敏感数据", password="mypassword")` |
| `crypto_aes_decrypt` | AES 解密 | `crypto_aes_decrypt(cipherText="...", password="mypassword")` |
| `crypto_hash_file` | 计算文件哈希值 | `crypto_hash_file(filePath="data.zip", algorithm="SHA-256")` |
| `crypto_password_generate` | 生成随机安全密码 | `crypto_password_generate(length=16, includeLetters=true, includeNumbers=true, includeSymbols=true)` |
| `crypto_base64` | Base64 编解码 | `crypto_base64(content="hello", mode="encode")` |

### 3.25 SystemInfoHandler（系统诊断）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `system_info` | 获取系统信息 | `system_info()` |
| `system_disk_info` | 获取磁盘空间信息 | `system_disk_info(drive="C:")` |
| `system_env_get` | 获取环境变量 | `system_env_get(variableName="JAVA_HOME")` |
| `system_port_check` | 检查端口占用 | `system_port_check(port=8080)` |
| `system_properties` | 获取 Java 系统属性 | `system_properties(propertyName="java.version")` |

### 3.26 PdfAdvancedHandler（高级 PDF 操作）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `pdf_compress` | 压缩 PDF 文件 | `pdf_compress(fileAbsolutePath="doc.pdf", targetPath="compressed.pdf")` |
| `pdf_encrypt` | PDF 添加密码保护 | `pdf_encrypt(fileAbsolutePath="doc.pdf", password="mypassword", targetPath="encrypted.pdf")` |
| `pdf_decrypt` | PDF 移除密码保护 | `pdf_decrypt(fileAbsolutePath="encrypted.pdf", password="mypassword", targetPath="decrypted.pdf")` |
| `pdf_get_bookmarks` | 获取 PDF 书签 | `pdf_get_bookmarks(fileAbsolutePath="doc.pdf")` |
| `pdf_reorder_pages` | 重排 PDF 页面顺序 | `pdf_reorder_pages(fileAbsolutePath="doc.pdf", pageOrder="3,1,2,5,4", targetPath="reordered.pdf")` |

### 3.27 WordAdvancedHandler（高级 Word 操作）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `word_create_from_template` | 基于模板生成文档 | `word_create_from_template(templatePath="template.docx", replacements="{\"{{name}}\":\"张三\"}", targetPath="output.docx")` |
| `word_find_and_highlight` | 搜索并高亮关键词 | `word_find_and_highlight(fileAbsolutePath="doc.docx", keyword="重要", color="YELLOW")` |
| `word_extract_headings` | 提取文档标题结构 | `word_extract_headings(fileAbsolutePath="doc.docx")` |
| `word_set_default_font` | 设置文档默认字体 | `word_set_default_font(fileAbsolutePath="doc.docx", fontName="宋体", fontSize=12)` |

### 3.28 PptAdvancedHandler（高级 PPT 操作）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `ppt_export_slides_as_images` | 幻灯片导出为图片 | `ppt_export_slides_as_images(fileAbsolutePath="slide.pptx", outputDir="./images")` |
| `ppt_add_chart` | 幻灯片嵌入图表 | `ppt_add_chart(fileAbsolutePath="slide.pptx", slideIndex=0, chartType="BAR")` |
| `ppt_add_shape` | 幻灯片添加图形 | `ppt_add_shape(fileAbsolutePath="slide.pptx", slideIndex=0, shapeType="rectangle", x=100, y=100, width=200, height=100)` |
| `ppt_get_slide_count` | 获取幻灯片页数和标题 | `ppt_get_slide_count(fileAbsolutePath="slide.pptx")` |

### 3.29 MediaHandler（媒体文件）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `audio_info` | 获取音频文件元信息 | `audio_info(fileAbsolutePath="music.mp3")` |
| `video_info` | 获取视频文件元信息 | `video_info(fileAbsolutePath="video.mp4")` |
| `media_file_info` | 获取通用媒体文件信息 | `media_file_info(fileAbsolutePath="file.mp4")` |

### 3.30 ArchiveAdvancedHandler（高级归档）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `archive_compress` | 压缩为 ZIP/tar.gz | `archive_compress(sourcePaths="file1.txt,file2.txt", targetPath="archive.zip", format="zip")` |
| `archive_decompress` | 解压归档文件 | `archive_decompress(sourcePath="archive.tar.gz", outputDir="./out")` |
| `archive_info` | 查看归档文件信息 | `archive_info(sourcePath="archive.zip")` |

### 3.31 BarcodeHandler（条形码）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `barcode_generate` | 生成条形码图片 | `barcode_generate(content="12345678", format="CODE_128", targetPath="barcode.png")` |
| `barcode_read` | 解析/读取条形码 | `barcode_read(imagePath="barcode.png")` |

### 3.32 I18nHandler（国际化工具）

| Tool Name | 功能说明 | 使用示例 |
|-----------|---------|---------|
| `i18n_unit_convert` | 单位换算 | `i18n_unit_convert(fromUnit="km", toUnit="m", value=1.0)` |
| `i18n_currency_convert` | 货币汇率换算 | `i18n_currency_convert(amount=100.0, fromCurrency="CNY", toCurrency="USD")` |
| `i18n_number_format` | 数字格式化 | `i18n_number_format(value=12345.67, formatType="grouped")` |

---

## 10. 依赖

| 依赖 | 用途 |
|------|------|
| Spring AI MCP Server Starter | MCP Server 框架 |
| Apache POI (poi-ooxml + poi-ooxml-full) | Excel / Word / PPT 文档操作 |
| Apache PDFBox | PDF 文档解析与生成 |
| Hutool | CSV 文件操作、通用工具 |
| Fastjson2 | JSON 序列化 |
| Lombok | 简化代码 |
| OpenHTMLToPDF | HTML 转 PDF（ConvertHandler） |
| Flexmark | Markdown 转 HTML（ConvertHandler） |
| ZXing | 二维码/条形码生成与解析（QrCodeHandler/BarcodeHandler） |
| Apache Commons Compress | TAR/GZIP 压缩支持（ArchiveAdvancedHandler） |
| SnakeYAML | YAML 格式支持（JsonDataHandler，由 Spring Boot 提供） |
| LogUtil（内置） | 异步文件日志，日志写入 `logs/mcp-office-toolbox.log`，每日备份 |
| AuditLogger（内置） | 异步操作审计日志，日志写入 `logs/audit.log`，每日轮转 |

---

## 11. 更多

- 功能模块总览：`../docs/文件操作功能模块总览.md`
- Spring AI MCP 官方文档：<https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html>
- Context7 API 文档：<https://context7.com/docs/api-guide>
