# Markdown 规范

> 本文是项目中所有 Markdown 编写规范的唯一权威来源。

---

## 1. Markdown 代码块规范

### 1.1 强制规则：代码块必须指定语言

所有 Markdown 代码块（```` ``` ``` ````）必须显式指定语言标签，禁止使用空标签 ` ``` `。

原因：

- 避免 IDE / Markdown 预览器告警
- 保证语法高亮一致性
- 便于代码块自动检测和工具处理

### 1.2 常用语言标签速查表

| 代码类型 | 标签 | 说明 |
| --- | --- | --- |
| 普通文本、目录树、ASCII 图 | `plaintext` | 最通用的无语法高亮标签 |
| Shell 命令 | `bash` 或 `sh` | 命令行脚本 |
| Python | `python` | Python 代码 |
| TypeScript | `typescript` 或 `ts` | TS 代码 |
| JavaScript | `javascript` 或 `js` | JS 代码 |
| Java | `java` | Java 代码 |
| SQL | `sql` | SQL 语句 |
| HTML | `html` | HTML 代码 |
| CSS | `css` | CSS 代码 |
| JSON (纯) | `json` | 见 1.3 |
| JSON (含注释) | `jsonc` | 见 1.3，推荐 90% 场景 |
| JSON5 | `json5` | 见 1.3 |
| Markdown | `markdown` | Markdown 代码本身 |
| HTTP | `http` | HTTP 请求/响应示例 |
| YAML | `yaml` 或 `yml` | YAML 配置 |
| Nginx | `nginx` | Nginx 配置 |

#### 反例

- ❌ ` ``` `（空标签，IDE 告警）
- ✅ ` ```plaintext `（目录树、ASCII 图）
- ✅ ` ```jsonc `（配置示例，含注释）

### 1.3 JSON 三档分类

| 标签 | 何时用 |
| --- | --- |
| ` ```json ` | 纯 RFC 8259 JSON：无注释、无尾逗号、无单引号 |
| ` ```jsonc ` | 含 `//` 或 `/* */` 注释的配置示例（**默认推荐**，90% 场景） |
| ` ```json5 ` | 需要单引号 / 尾逗号 / 十六进制等 JSON5 扩展语法 |

### 1.4 一致性规则

- 同一文档内同类示例标签一致（配置示例全用 `jsonc`，协议消息全用 `json`）
- 拼写严格：`json` / `jsonc` / `json5`（大写或错拼导致高亮失效）

### 1.5 决策口诀

> 加注释？→ `jsonc`。用单引号/尾逗号？→ `json5`。都没有？→ `json` 或 `jsonc`（`jsonc` 更安全）。

#### 反例

- ❌ 在 ` ```json ` 块写 `//` 注释 → IDE 爆红
- ❌ 因"json 不支持注释"一律改 `json5` → 应改 `jsonc`

### 1.7 代码块结构：仅开头标注语言，结尾不重复

代码块采用 **语言标识开头**、` ``` ` **结尾** 的单边标注方式：

- **开头**：`` ```语言标识 ``（如 `` ```java ``、`` ```jsonc ``）
- **结尾**：仅 ` ``` `，**不重复**语言标识
- 这是**非成对**设置——语言标签只出现在开头围栏，不在结尾围栏重复

#### 正确示例（代码块结构）

````plaintext
```java
public class Hello {
    public static void main(String[] args) {
        System.out.println("Hello");
    }
}
```
````

````plaintext
```python
print("hello")
```
````

#### 错误示例（结尾重复语言标签）

````plaintext
```java
public class Hello { }
```
```java
```
````

````plaintext
```python
print("hello")
```python
```
````

---

## 2. Markdown 排版规范（markdownlint）

### 2.1 适用范围

本规范适用于项目中输出的所有 Markdown 内容，必须严格遵循 markdownlint 主流规范，零警告。

> 本节规则基于 [markdownlint v0.41.0][mdlint-doc] 官方规则文档整理。

### 2.2 规则总览（MD001 ~ MD060）

| 编号 | 别名 | 说明 | 状态 |
| --- | --- | --- | --- |
| MD001 | heading-increment | 标题层级必须逐级递增，禁止跳级 | 启用 |
| MD002 | - | 已废弃，由 MD041 替代 | 废弃 |
| MD003 | heading-style | 标题风格必须一致（ATX / Setext） | 启用 |
| MD004 | ul-style | 无序列表标记风格一致（- / * / +） | 启用 |
| MD005 | list-indent | 同级列表项缩进必须一致 | 启用 |
| MD006 | - | 已废弃，由 MD007 替代 | 废弃 |
| MD007 | ul-indent | 无序列表缩进（默认 2 空格） | 启用 |
| MD008 | - | 已废弃 | 废弃 |
| MD009 | no-trailing-spaces | 禁止行尾空格 | 启用 |
| MD010 | no-hard-tabs | 禁止硬制表符，使用空格缩进 | 启用 |
| MD011 | no-reversed-links | 禁止反向链接语法 ( )[] | 启用 |
| MD012 | no-multiple-blanks | 禁止连续多个空行（默认最多 1 个） | 启用 |
| MD013 | line-length | 行长度限制（默认 80 字符） | 启用 |
| MD014 | commands-show-output | 命令前 $ 需配合输出展示 | 启用 |
| MD015 | - | 已废弃 | 废弃 |
| MD016 | - | 已废弃 | 废弃 |
| MD017 | - | 已废弃 | 废弃 |
| MD018 | no-missing-space-atx | ATX 标题 # 后必须有空格 | 启用 |
| MD019 | no-multiple-space-atx | ATX 标题 # 后禁止多个空格 | 启用 |
| MD020 | no-missing-space-closed-atx | 闭合 ATX 标题 # 内侧必须有空格 | 启用 |
| MD021 | no-multiple-space-closed-atx | 闭合 ATX 标题 # 内侧禁止多个空格 | 启用 |
| MD022 | blanks-around-headings | 标题前后必须空行 | 启用 |
| MD023 | heading-start-left | 标题必须顶行开始，禁止缩进 | 启用 |
| MD024 | no-duplicate-heading | 禁止重复标题内容 | 启用 |
| MD025 | single-title | 单文件只允许一个顶级标题（h1） | 启用 |
| MD026 | no-trailing-punctuation | 标题末尾禁止标点符号 | 启用 |
| MD027 | no-multiple-space-blockquote | 引用 > 后禁止多个空格 | 启用 |
| MD028 | no-blanks-blockquote | 引用块内空行需有 > 前缀 | 启用 |
| MD029 | ol-prefix | 有序列表前缀规范（1./1./2. 等） | 启用 |
| MD030 | list-marker-space | 列表标记后空格数规范（默认 1 空格） | 启用 |
| MD031 | blanks-around-fences | 代码块前后必须空行 | 启用 |
| MD032 | blanks-around-lists | 列表前后必须空行 | 启用 |
| MD033 | no-inline-html | 禁止内联 HTML | 启用 |
| MD034 | no-bare-urls | 禁止裸 URL，需用 <> 包裹 | 启用 |
| MD035 | hr-style | 水平线风格一致 | 启用 |
| MD036 | no-emphasis-as-heading | 禁止用强调（粗体/斜体）代替标题 | 启用 |
| MD037 | no-space-in-emphasis | 强调标记内侧禁止空格 | 启用 |
| MD038 | no-space-in-code | 行内代码片段内禁止多余空格 | 启用 |
| MD039 | no-space-in-links | 链接文本内侧禁止空格 | 启用 |
| MD040 | fenced-code-language | 代码块必须指定语言 | 启用 |
| MD041 | first-line-heading | 文件首行必须为顶级标题 | 启用 |
| MD042 | no-empty-links | 禁止空链接 | 启用 |
| MD043 | required-headings | 必需标题结构校验 | 启用 |
| MD044 | proper-names | 专有名词大小写校验 | 启用 |
| MD045 | no-alt-text | 图片必须有替代文本（alt text） | 启用 |
| MD046 | code-block-style | 代码块风格一致（缩进 / 围栏） | 启用 |
| MD047 | single-trailing-newline | 文件末尾必须有且仅有一个换行符 | 启用 |
| MD048 | code-fence-style | 代码围栏风格一致（` 或 ~） | 启用 |
| MD049 | emphasis-style | 强调风格一致（* 或 _） | 启用 |
| MD050 | strong-style | 加粗风格一致（** 或 __） | 启用 |
| MD051 | link-fragments | 链接片段必须有效（锚点存在） | 启用 |
| MD052 | reference-links-images | 引用链接/图片标签必须已定义 | 启用 |
| MD053 | link-image-reference-definitions | 引用定义必须被使用 | 启用 |
| MD054 | link-image-style | 链接/图片风格规范 | 启用 |
| MD055 | table-pipe-style | 表格竖线风格一致 | 启用 |
| MD056 | table-column-count | 表格每行列数必须一致 | 启用 |
| MD057 | - | 未定义（v0.41.0 中暂无此规则） | 保留 |
| MD058 | blanks-around-tables | 表格前后必须空行 | 启用 |
| MD059 | descriptive-link-text | 链接文本必须有描述性 | 启用 |
| MD060 | table-column-style | 表格列风格一致（对齐/紧凑/紧密） | 启用 |

> **信息参考来源**：[markdownlint v0.41.0 官方规则文档][mdlint-doc]

[mdlint-doc]: https://github.com/DavidAnson/markdownlint/tree/v0.41.0/doc

### 2.3 本项目核心强制规则

以下为本项目明确要求强制遵守的规则，其余规则默认遵循 markdownlint 官方推荐配置。

| 规则编号 | 规则内容 |
| --- | --- |
| MD004 | 无序列表统一用 `-`，全文一致 |
| MD007 | 列表缩进 2 空格 |
| MD009 | 禁止行尾空格 |
| MD010 | 禁止硬制表符，使用空格缩进 |
| MD018 | ATX 标题 `#` 后必须有空格 |
| MD019 | ATX 标题 `#` 后禁止多个空格 |
| MD022 | 标题（`#` / `##` / `###`）前后各空一行，标题不顶正文写 |
| MD031 | 代码块（```` ``` ````）前后各空一行 |
| MD032 | 有序列表（`1.`）、无序列表（`-`）前后各空一行，标题与列表之间必须空一行，不能紧连 |
| MD040 | 代码块必须指定语言 |
| MD047 | 文件末尾必须有且仅有一个换行符 |
| MD055 | 表格竖线两侧各保留 1 个半角空格，分隔线与两侧竖线同样保留空格 |
| MD058 | 表格前后必须空行 |
| MD060 | 表格竖线对齐、分隔线列数与表头一致，禁止竖线错位或列数不匹配 |

### 2.4 排版原则

所有块级元素（标题、列表、代码块、段落、表格）之间必须有空行分隔，不得紧挨。

### 2.5 正确示例

````markdown
## 标题

这是一段正文。

- 列表项 1
- 列表项 2

```python
print("hello")
```

1. 有序列表项 1
2. 有序列表项 2

最后一段文字。
````

### 2.6 禁止

- 标题、列表、代码块、段落之间紧挨着无空行
- 列表前直接跟标题而不空行

### 2.7 文件末尾

文件末尾必须保留一个空行。

### 2.8 markdownlint MD055 + MD060：表格规范

#### MD055 竖线空格规则

所有 Markdown 表格的竖线 `|` 左右两侧必须保留 1 个半角空格：

1. 表头行、分隔线行、数据行统一遵循空格规则
2. 单元格内容与竖线禁止紧贴无空格
3. 分隔线 `----` 与两侧 `|` 同样保留空格

#### MD060 列对齐规则

所有 Markdown 表格必须严格符合 markdownlint MD060 规范：

1. 表头行、分隔线行、内容行的竖线 `|` 必须上下对齐
2. 分隔线行的列数必须与表头完全一致，不能多列、少列
3. 禁止出现竖线错位、分隔线列数不匹配的情况

#### 正确示例（表格规范）

```markdown
| 名称 | 说明 |
| ---- | ---- |
| A    | 测试 |
```

#### 错误示例（分隔线列数不匹配）

```markdown
| 名称 | 说明 |
| ---- |
| A    | 测试 |
```

#### 错误示例（竖线未对齐）

```markdown
| 名称 | 说明 |
| ---- | ---- |
| A | 测试 |
```

### 2.9 完成判定

- [ ] 所有 Markdown 文件通过 markdownlint 检查，零警告
- [ ] 无序列表统一使用 `-`
- [ ] 列表缩进为 2 空格
- [ ] 文件末尾有空行
- [ ] 所有表格竖线对齐，分隔线列数与表头一致
- [ ] 所有表格竖线 `|` 两侧保留 1 个半角空格，分隔线 `----` 与两侧 `|` 之间保留空格
