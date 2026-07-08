<!--
本文件为 Java 专用版 MCP 开发指引。

- 通用流程步骤（Step 0 ~ Step 7 骨架）与其他 5 种语言版保持一致；
  变更请同步改其他 5 份。
- 提示词模板内的 <LANG> / config 文件名 / 测试命令已固化为 Java。
-->

# MCP 项目开发指引（提示词驱动版）

> **前提**：本仓库的 MCP Server 由**大模型（Claude 等）**作为开发者实现，人负责**给指令 + 审核产出**。
> **本指引作用**：给出每一步的**提示词模板**，复制改占位符即可交给大模型执行。
> **与其他文档的关系**：
> - `mcp_develop_rule.md` → 约束（硬规则）
> - `mcp_develop_guideline.md`（本文件）→ 步骤 + 提示词模板
> - `./guide/` → 本语言的具体 SDK / 样板（4 个编号文件）
> - 本语言现有样板：见 `./mcp-<feature>/`（Java 实现）

---

## 人机分工速查

| 人做的事 | 大模型做的事 |
|---|---|
| 想清楚"MCP Server 要提供什么工具" | 把需求扩写为完整实现 |
| 提供调用样例、审核产出 | 写代码、配依赖、写 README |
| 决定版本升位档（MAJOR/MINOR/PATCH） | 执行升版、同步改文档 |
| 决策要不要支持 HTTP 传输 | 按规则实现双传输入口 |

**人要守住的底线**：
1. 每段提示词首行都写 `【必读】所有开发必须遵循 mcp_develop_rule.md ！！！`（见 Step 0）
2. 审核 README / GUIDE / 源码三者的功能描述是否同步
3. stdio 模式要亲自跑一次 `npx @modelcontextprotocol/inspector <cmd>` 验证，防"stdout 污染"

---

## 总览：开发流水线

```
⓪ 开工前缀（每次对话必带）
① 需求草稿（人）→ ② 选语言 + 扩写 README 草稿 → ③ 搭骨架 + 实现核心
                → ④ 补双传输（stdio + http）→ ⑤ Inspector 联调
                → ⑥ 补单测 → ⑦ 交付前清单
```

占位符（已按 Java 固化）：
- 当前语言：`java`（目录 `06-MCP 实战项目/mcp-java/`）
- `<FEATURE>` — 功能名（`case-converter` / `log-summarizer` / …）
- `<PROJECT_ROOT>` — `06-MCP 实战项目/mcp-java/mcp-<FEATURE>/`
- `<TOOLS>` — 待暴露的 MCP 工具列表

---

## Step 0 · 开工前缀（每次对话必带）

**一句话规则**：每段提示词的第一行都写：

```
【必读】所有开发必须遵循 mcp_develop_rule.md ！！！
```

**为什么必须要**：`mcp_develop_rule.md` 是硬约束（目录结构、stdio 不污染、版本同步、三文档同步）。大模型没读，十有八九会违反。

**额外建议**：
- 迭代 / 改 bug：加一句"先读当前 README 和 GUIDE，再动代码"
- 新功能：加一句"确认是否需要升 MINOR 版本"

**审核要点**：大模型回复开头应能看出它读了 rule（如明确引用 §X 条款）。

---

## Step 1 · 需求草稿（人自己想，0.5~2 小时）

**目标**：用大白话写清楚"MCP Server 要暴露哪些工具、解决谁的问题"。

**人写下来的内容（5 行以内）**：
1. 工具名 + 做什么（例：`convert_case(text, mode)` → 大小写/命名风格转换）
2. 参数签名（type + 必填/可选 + 示例值）
3. 输出形态（纯文本 / 结构化 JSON / 资源引用）
4. 目标语言 + 首选 SDK（不确定留空，让大模型建议）
5. 部署形态（只需 stdio / 也要 HTTP / 容器化）

这一步**不用**写提示词，写完进入 Step 2。

---

## Step 2 · 选语言 + 扩写 README 草稿（1 次对话）

**目标**：大模型给出选型建议 + 生成 README v0.1 草稿。

**提示词模板**：

```
【必读】所有开发必须遵循 mcp_develop_rule.md ！！！

我想实现一个 MCP Server：<一句话描述>。

工具清单：
  - <工具 1>(参数) → 返回
  - <工具 2>(参数) → 返回

部署场景：<本地 Claude Code / 团队内网 / SaaS / ...>

请：
  1. 参照 `05-MCP 学习笔记/03-开发指南/01-MCP-Server-语言选型.md` 推荐技术栈，给出理由
  2. 按 rule §4.2 生成 README.md 草稿到 <PROJECT_ROOT>/README.md
  3. 不要写代码；先对齐契约和目录结构
```

**审核要点**：
- 工具 description 是否说清"何时使用"（不止是"做什么"）
- 目录结构是否符合 rule §3.1
- 版本号是否用指针式（指向 config 文件）而非硬编码

---

## Step 3 · 搭骨架 + 实现核心（1 次对话）

**目标**：按 README 搭最小可运行 MCP Server，覆盖 stdio 传输即可。

**提示词模板**：

```
【必读】所有开发必须遵循 mcp_develop_rule.md ！！！

按 <PROJECT_ROOT>/README.md 实现 MCP Server：

  1. 按 rule §3.1 搭目录结构；语言特定布局参考 `./guide/02-源代码结构.md`
  2. 纯逻辑（如 converter）和 MCP 接入层分文件；前者不 import MCP
  3. 暴露工具：<TOOLS 列表>
  4. 工具 description 写清"何时使用"，给调用示例
  5. 先只做 stdio 传输；双传输留 Step 4
  6. stdio 日志**强制 stderr**（rule §7.2），不允许任何 stdout 写入
  7. 初始化 pom.xml（`pom.xml` 根 `<version>` 标签（或 Gradle 的 `version = '...'`）），version = 0.1.0

跑最简单的 case 验证端到端能通。
```

**审核要点**：
- `grep -rn 'System.out.print' src/` 在 stdio 入口路径应为 0 条
- 工具参数校验有没有（Schema 校验优先于手写 if）
- 纯逻辑模块是不是真的"无 MCP 依赖"（能独立 import）

---

## Step 4 · 补双传输（stdio + http）

**目标**：加 `--transport stdio|http` CLI 开关，支持 Streamable HTTP。

**提示词模板**：

```
【必读】所有开发必须遵循 mcp_develop_rule.md ！！！

给 <PROJECT_ROOT> 加 Streamable HTTP 传输：

  1. CLI 入口支持 --transport stdio|http，默认 stdio
  2. http 模式接 --host（默认 127.0.0.1）和 --port（默认 8000）
  3. http 端点 POST /mcp（rule §7.1 要求，Streamable HTTP 规范）
  4. 同步更新 README 的"运行"章节，给出两种模式的启动命令 + curl 验证脚本
  5. 同步更新 Client 挂载配置示例（stdio 版 + http 版）
  6. 升 MINOR 版本号

不要动核心工具逻辑。
```

**审核要点**：
- curl 验证脚本是否能真的跑通（手动 curl 一次）
- http 模式是否也能通过 Inspector 连接

---

## Step 5 · Inspector 联调

**目标**：用官方 Inspector 手工跑一遍所有工具，发现 Schema 错漏。

**人手工做**：

```
# stdio 模式
npx @modelcontextprotocol/inspector <启动命令>

# http 模式（先启动 Server）
npx @modelcontextprotocol/inspector
# UI 里填 URL http://127.0.0.1:8000/mcp
```

**提示词模板（发现问题后）**：

```
【必读】所有开发必须遵循 mcp_develop_rule.md ！！！

Inspector 联调发现以下问题：
  1. <工具名> 参数 <X> 应是 enum 不是自由 string
  2. <工具名> 的 description 没说"何时使用"，只写了"做什么"
  3. ...

请逐条修复，升 PATCH 版本；改完重新标记哪些需要我手动验证。
```

---

## Step 6 · 补单测

**目标**：纯逻辑（converter）覆盖到（rule §8）。

**提示词模板**：

```
【必读】所有开发必须遵循 mcp_develop_rule.md ！！！

给 <PROJECT_ROOT> 的纯逻辑模块补单元测试：
  - 用 JUnit 5（`@SpringBootTest` 集成测试、`@Tool` 方法可作普通 Bean 单测）
  - 覆盖：正常输入、边界（空串 / 首字母缩写 / 全大写）、所有转换模式各至少 1 条

跑通后在 README 的"调试"章节加一条测试命令。
```

---

## Step 7 · 交付前检查清单

**提示词模板**：

```
【必读】所有开发必须遵循 mcp_develop_rule.md ！！！

对照 rule §2 交付前检查清单，逐项核查 <PROJECT_ROOT>：

[ ] README 目录结构与 `ls` 输出一致
[ ] GUIDE 如有结构变化已同步
[ ] 语言 config 文件 version 字段已按 §6 升位
[ ] --transport stdio|http 两种模式都能跑通
[ ] stdio 入口路径 `grep` 不到 stdout 写入
[ ] README 无硬编码版本号 / 最后更新日期

每条不过的给出具体文件路径 + 问题描述 + 修复方案。
```

---

## 附录 A · 提示词使用技巧

1. **首行必写** `【必读】所有开发必须遵循 mcp_develop_rule.md ！！！`
2. **占位符替换干净**：`<PROJECT_ROOT>` / `<LANG>` / `<FEATURE>` 留着会让大模型猜
3. **一次只让它做一件事**：需求、骨架、双传输、测试分开对话
4. **产物路径强制**：提示词里明写落盘位置
5. **量化修复标准**：与其说"修好 bug X"，不如说"让 `curl POST /mcp` 在 initialize 阶段返回 `result`"
6. **审阅产物 > 审阅对话**：以磁盘文件为准

---

## 附录 B · 典型陷阱

| 陷阱 | 症状 | 对策 |
|---|---|---|
| stdio 模式 `print` 到 stdout | Client 协议解析崩溃 | rule §7.2 硬禁；提交前 grep |
| 工具 description 太宽泛 | LLM 错误调用 / 错过调用 | 描述写"何时使用 + 何时不使用" |
| 纯逻辑和 MCP 耦合 | 难单测 / 换语言时重写全部 | 纯逻辑零 MCP 依赖 |
| 版本号多处硬编码 | 改一个漏改其他 → 版本漂移 | rule §6 单一事实源：config 文件 |
| 只做 stdio 不支持 http | 团队共享时必须重写 | rule §7.1 要求双传输入口从 Day 1 起预留 |
| HTTP 模式日志也写 stdout | 容器里混淆协议和日志 | 日志统一 stderr 不分模式 |

---

## 附录 C · 推荐阅读顺序（开新 MCP Server 前）

1. 本文件
2. `mcp_develop_rule.md`
3. `05-MCP 学习笔记/03-开发指南/01-MCP-Server-语言选型.md`（选语言）
4. `./guide/`（本语言 SDK 入门，4 份编号文件）
5. `./mcp-<feature>/`（本语言现有样板项目）
