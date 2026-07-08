<!--
本文件为 Java 专用版，基于 MCP 通用规则定制。

- 核心条款（§1 / §2 / §4 / §5 / §9 / §11）与其他 5 种语言版保持一致；
  变更请同步改其他 5 份。
- 语言特定条款（§3.3 布局 / §6 version 位置 / §7.2 日志 / §8 测试）
  仅保留 Java 版，按 SDK 实际情况独立维护。

对比其他语言版（看哪些核心条款漂了）：
  git diff mcp-python/mcp_develop_rule.md mcp-nodejs/mcp_develop_rule.md
-->

# MCP 项目开发规则（Development Rules）

> 目的：让 `06-MCP 实战项目/` 下的每个 MCP Server 项目都遵循统一规则，避免走岔路、漏更新、版本漂移。
> 参考来源：`04-Skill 实战项目/skill_develop_rule.md`，按 MCP 应用特征精简适配。

---

## 强度约定

- **[硬]** MUST：违反即不合格，无借口变通
- **[软]** SHOULD：默认遵守，变通需 commit 说明
- **[选]** MAY：推荐但非强制

---

## §1 最高原则

1. **[硬] 永远基于最新文档开工**：修改前先读 `./guide/` 下的 4 份指南与 `./mcp_develop_guideline.md`（流程模板），不得凭印象动工。
2. **[硬] 三者同步**：README / GUIDE / 代码（含 version 字段）的功能描述、目录结构、参数契约必须一致；改一个要联动核查另外两个。
3. **[硬] 只做本次任务要求的事**：不顺手 refactor、不"顺便"改无关文件；不确定范围先问。
4. **[硬] stdio 协议通道不可污染**：stdio 传输模式下，任何写到 stdout 的语句都是 bug；日志强制 stderr。

---

## §2 交付前检查清单

| # | 条目 | 强度 | 验证方法 |
|---|---|---|---|
| 1 | README 目录结构与实际一致 | [硬] | 对比 `ls mcp-<lang>/mcp-<name>/` 与 README"目录结构"小节 |
| 2 | guide/ 如有结构变化已同步（01-环境准备 / 02-源代码结构 / 03-CICD / 04-使用方式） | [硬] | 对比 guide/02-源代码结构.md 的项目结构小节 |
| 3 | 版本号已升位（按 §6） | [硬] | `git diff` 查 `pom.xml` 的 version 字段 |
| 4 | 传输参数（`--transport stdio\|http`）双通 | [硬] | 手动跑一次 stdio + 一次 http，都能返回 initialize 结果 |
| 5 | stdio 模式下**无任何 stdout 日志** | [硬] | `grep -rn 'System.out.print' src/` 在 stdio 入口路径返回 0 行 |
| 6 | 无硬编码版本号入 README（反模式见 §4.3） | [硬] | `grep -nE '当前版本[：:]' README.md` 返回 0 行 |

---

## §3 标准目录结构

### 3.1 顶层布局

```
mcp-<lang>/
├── guide/                          # [必需] 该语言 MCP Server 开发指南（4 份）
│   ├── 01-环境准备.md
│   ├── 02-源代码结构.md
│   ├── 03-CICD.md
│   └── 04-使用方式.md
├── mcp-<feature>/                  # [必需] 具体 MCP Server 实现（可多个）
│   ├── README.md                   # [必需] 使用说明
│   ├── pom.xml                      # [必需] 项目配置 + 版本号（`pom.xml` 根 `<version>` 标签（或 Gradle 的 `version = '...'`））
│   ├── src/ 或 <lang-convention>/  # [必需] 源码
│   └── tests/                      # [软] 单元测试（JUnit 5（`@SpringBootTest` 集成测试、`@Tool` 方法可作普通 Bean 单测））
└── doc/                            # [选] 跨项目的参考文档、脑图
```

### 3.2 核心约束

- **[硬]** `mcp-<lang>/` 根目录**不**直接放可执行代码；代码一律在 `mcp-<feature>/` 子项目内。
- **[硬]** `guide/` 下 4 份文件是语言级开发指南，与具体功能解耦；不要把 case-converter 的实现细节塞进 guide/。
- **[硬]** 每个 `mcp-<feature>/` 是一个**独立构建单元**，自带 config + 依赖声明，独立可跑。
- **[硬]** 项目名用"对象 + 动作"描述功能：`mcp-case-converter`、`mcp-log-summarizer`；不用 `mcp-demo-1` / `mcp-test`。
- **[软]** 小写 + 连字符；避免下划线 / 驼峰。

### 3.3 常见 MCP Server 布局

所有语言的 `mcp-case-converter/` 必有至少三类文件：

| 角色 | 本语言约定 |
|------|------|
| **纯逻辑**（可独立复用） | `@Service` 标注的领域类（如 `WeatherService.java`） |
| **MCP 接入层** | `@McpTool` 方法集合（`tools/WeatherTools.java`），Spring AI 自动注册 |
| **启动入口** | `@SpringBootApplication` 类（`McpServerApplication.java`） |

**布局备注**：Maven 约定：`src/main/java/<package>/`、资源 `src/main/resources/`；测试 `src/test/java/`

**原则**：纯逻辑与 MCP 解耦，便于单测和换外壳。

---

## §4 README 规则

### 4.1 何时必须更新

**[硬]**：
- 项目结构变更（新增 / 删除目录或关键文件）
- 添加 / 删除功能或工具（tool / resource / prompt）
- 使用方式变更（参数、CLI、前置依赖）
- 传输方式或端口变更

### 4.2 README 的定位（三件事）

**[硬]** README 只回答三个问题，其他一律不写：

| # | 问题 | 具体内容 |
|---|------|---------|
| 1 | **这是什么？能做什么？** | 一句话简介 + 暴露的工具清单（名字 + 一行描述） |
| 2 | **文件都在哪？** | 目录结构树（与 `ls` 一致） |
| 3 | **怎么用？** | 构建命令 + 启动命令 + `.mcp.json` 挂载片段 + 调试方式 |

### 4.3 必备章节

**[硬]**：
- 一句话简介
- 能力清单（工具名 + 一行中文描述，**不**写参数类型签名和 JSON Schema）
- 构建与运行（含 `--transport stdio|http`）
- Client 挂载配置（`.mcp.json` 示例）
- 目录结构（与实际文件一致）
- 调试方式（Inspector 命令一行 + 单测命令一行）

### 4.4 反模式（禁止内容）

**关于版本 / 历史**：
- **[硬]** 硬编码版本号（如 `当前版本 v0.2.1`）→ 指向语言的 config 文件
- **[硬]** 内嵌版本变更历史 → 用 git log 或独立 `CHANGELOG.md`
- **[硬]** 硬编码"最后更新日期"→ 由 git log 提供

**关于技术实现（新增，针对"README 变成教程"的最常见失控）**：
- **[硬] 源码片段**：不贴 `@SpringBootApplication` / `@Tool` / `@mcp.tool()` / `#[tool_router]` 等实现代码块。要看代码让读者打开 `src/`。
- **[硬] 依赖原文 dump**：不贴完整 `pom.xml <dependency>` / `package.json` dependencies 段 / `Cargo.toml` `[dependencies]`。只写"依赖声明见 `<config 文件>`"。
- **[硬] 配置文件原文 dump**：不把 `application.yml` / `application.properties` / `appsettings.json` 的完整内容贴到 README。关键配置项用**一句话**描述或指向文件。
- **[硬] 客户端实现代码**：不贴 `McpClient.sync(...)` / `new Client(...)` 这类调用方代码。Client 怎么写属于 `08-MCP-Client-开发.md` 的职责。
- **[软] 外部文档链接墙**：官方文档、规范、博客链接最多 2 条（放在末尾），不要列 5+ 条。

**判别原则**：
> "读者只想知道**能不能用、怎么用**，不想知道**为什么这样实现**。"
> 实现原理、SDK API 讲解属于 `./guide/02-源代码结构.md`（语言层）；代码细节属于源码注释；架构决策属于 `development/plans/`。

---

## §5 guide/ 规则

**[硬]** `guide/` 下 4 份文件是**语言层**（不是项目层）的开发指南，面向"用该语言新建一个 MCP Server 的开发者"。

- **[硬]** 4 份文件分别覆盖：环境准备 / 源代码结构 / CI/CD / 使用方式；**不**描述具体功能实现（那是 README 的事）
- **[硬]** 给出 stdio / Streamable HTTP 双传输的配置
- **[硬]** 给出常见坑位（`stdout 污染`、`日志强制 stderr`、`冷启动`）
- **[硬]** 跨文件引用用相对路径链接，**不**维护重复内容
- **[软]** 末尾引用同目录下的具体 `mcp-<feature>/` 作为实战参考

---

## §6 版本号规则

采用语义化版本 `MAJOR.MINOR.PATCH`：

- **MAJOR**：输入输出契约不兼容、工具名/参数签名变更
- **MINOR**：新增工具 / 资源 / 传输模式（向后兼容）
- **PATCH**：bug 修复、文案修正、性能优化

**[硬]** 版本号写在语言的 config 文件里（**单一事实源**）：

本项目在 **`pom.xml` 根 `<version>` 标签（或 Gradle 的 `version = '...'`）**。

**[硬]** README / GUIDE **不硬写版本号**，用"版本见 `<config 文件>`"的指针式表达。

---

## §7 传输与日志规则

### 7.1 传输必须双通

**[硬]** 每个 MCP Server 都要支持：
- **stdio**（默认）：本地工具场景
- **Streamable HTTP**：远程/容器化场景

CLI 用 `--transport stdio|http` 切换，`http` 再接 `--host` / `--port`。

### 7.2 stdio 日志

- **[硬]** stdio 模式下**严禁**把任何内容写到 stdout（stdout 是协议通道）
- **[硬]** 日志强制写 stderr：SLF4J + Logback；stdio 模式配 `application.properties`：`spring.main.banner-mode=off` + `logging.pattern.console=`（关控制台输出）+ `logging.file.name=./logs/mcp.log`
- **[硬] 反模式**：`System.out.println(...)` 或忘关 banner 的 Spring 启动日志

### 7.3 错误分层

- **业务错误**（参数非法、资源不存在）→ 返回 `isError: true` 的 ToolResult，LLM 可读到并纠正
- **系统错误**（未预期异常、协议错误）→ 抛异常/返回 error，走 JSON-RPC error

---

## §8 测试规则

- **[软]** 纯逻辑模块必有单元测试：JUnit 5（`@SpringBootTest` 集成测试、`@Tool` 方法可作普通 Bean 单测）
- **[软]** MCP 接入层可用对应语言的 Inspector（`npx @modelcontextprotocol/inspector <cmd>`）手工验收
- **[选]** CI 跑单测 + 一次最小协议握手

---

## §9 源码与配置文件注释政策

- **[硬]** **代码注释一律中文**；标识符、API 名、错误信息保留英文
- **[硬]** **配置文件的注释一律中文**——覆盖 `pom.xml` / `application*.yml` / `application*.properties` / `.env` / Dockerfile / Makefile / shell 脚本 等所有可写注释的配置格式
- **[硬]** 只留**当前 WHY**；不写历史轨迹（"v0.2 新增"、"参见 FINDINGS_xxx"）
- **[硬]** 面向 LLM 的 docstring / tool description 必须写清**何时使用**（触发场景），而不仅是"做什么"
- **[软]** 不要中英混排一行；要么全中文要么全英文

---

## §10 配套资料

- 流程与提示词模板：`mcp_develop_guideline.md`（同目录）
- 语言指南：`./guide/`（同目录；4 份文件：01-环境准备 / 02-源代码结构 / 03-CICD / 04-使用方式）
- 跨语言选型：`05-MCP 学习笔记/03-开发指南/01-MCP-Server-语言选型.md`
- 传输层原理：`05-MCP 学习笔记/02-协议规范/02-传输层-Stdio与SSE.md`

---

## §11 规则演化

- **[硬]** 规则变更走 git commit，message 说明原因
- **[软]** 每次 MAJOR 升级时回顾本文件，过时条款清理
