# 通用开发规则（Universal Development Rules）

> 跨领域 / 跨语言 / 跨项目的基础约定。任何专用规则（如 `mcp_develop_rule.md`）都在本文基础上叠加领域特定条款。
> **冲突解决**：专用规则与本文冲突时，以本文为准；专用规则需显式说明覆盖理由。

---

## 强度约定

- **[硬]** MUST：违反即不合格，无借口变通
- **[软]** SHOULD：默认遵守，变通需 commit 说明
- **[选]** MAY：推荐但非强制

---

## §1 最高协作原则

1. **[硬] 永远基于最新文档开工**：修改前先读当前领域的 rule / guide / README / 流程模板，不得凭印象动工或沿用旧记忆。
2. **[硬] 多处同步**：README / GUIDE / 代码（含 version 字段）/ 目录结构 / 依赖声明的功能描述、参数契约必须一致；改一处要联动核查其他。
3. **[硬] 只做本次任务要求的事**：不顺手 refactor、不"顺便"改无关文件；不确定范围先问。
4. **[硬] 规则 / 指南独立于具体实现项目**：
   - 规则 / 指南是**抽象契约**，具体项目是**它的实例**。契约引用实例会形成依赖环，实例被删 / 改 / 重命名时规则就会断链或过时。
   - 描述结构只用**占位符**（`<engine>` / `<domain>` / `<feature>/` / `<topic>.yaml` / `<external-tool>.jar`）和**示意性**业务动词（`analyze` / `convert` / `summarize` / `generate` / `fetch` / `transform` 等）。
   - **不**嵌入任何具体项目的真实文件名、类名、YAML 名、阶段名、工具版本号。
   - **反向允许**：具体项目的 README 可以大量回引规则（`详见 rule §3.3`），这个方向不产生依赖环。
   - **软引用允许**：规则末尾可加一行"可浏览同目录下任一 `<feature>/` 作为落地示例"，但**即使删除该段，规则主体也必须自洽**。
   - **自检口诀**：如果本仓库所有具体项目被删光，这段文字还成立吗？不成立就是嵌入了实例。

---

## §2 文档工程规范

### 2.1 README 定位（三问）

**[硬]** README 只回答三个问题，其他一律不写：

| # | 问题 | 具体内容 |
|---|------|---------|
| 1 | 这是什么？能做什么？ | 一句话简介 + 对外能力清单（名字 + 一行描述） |
| 2 | 文件都在哪？ | 目录结构树（与 `ls` 一致） |
| 3 | 怎么用？ | 构建 / 启动命令 + 配置示例 + 调试方式 |

**判别口诀**：
> "读者只想知道**能不能用、怎么用**，不想知道**为什么这样实现**。"
> 实现细节属于源码注释；架构决策属于 `development/plans/`（见 §6）。

### 2.2 README 反模式（禁止内容）

**关于版本 / 历史**：
- **[硬]** 硬编码版本号（`当前版本 v0.2.1`）→ 指向语言的 config 文件（§3）
- **[硬]** 内嵌版本变更历史 → 用 git log 或独立 `CHANGELOG.md`
- **[硬]** 硬编码"最后更新日期"→ 由 git log 提供

**关于技术实现**：
- **[硬]** **源码片段**：不贴实现代码块（装饰器 / 注解 / 路由声明等）。要看代码让读者打开源码目录。
- **[硬]** **依赖原文 dump**：不贴完整 `pom.xml` / `package.json` / `Cargo.toml` / `pyproject.toml` 的 deps 段。只写"依赖声明见 `<config 文件>`"。
- **[硬]** **配置文件原文 dump**：不贴完整 `application.yml` / `appsettings.json` / `config.toml` 等。关键项一句话描述。
- **[硬]** **调用方代码**：不贴客户端 / 使用方代码。调用方怎么写是调用方文档的职责。
- **[软]** **外部文档链接墙**：官方文档 / 规范 / 博客链接最多 2 条（放在末尾）。

### 2.3 命令块规范

**[硬]** 任何包含可执行命令（`bash` / `sh` / `powershell` / `npx` / 语言包管理器 / `docker` / `curl` / ...）的代码块必须让读者**整块复制粘贴即可复现**。

- **[硬]** 代码块**第一行**（或紧跟一行说明注释之后）给出可执行的 `cd <path>` 命令；**不要**只用注释写"在 xxx 目录下"（注释会被一起复制但 shell 不当命令执行）
- **[硬]** 一个代码块内**只使用一个工作目录**；需要切目录时拆成多个代码块
- **[硬]** 涉及**安装**的步骤必须紧跟**验证**命令（如 `<tool> --version` / `<tool> --help` / `curl` 探活等）
- **[软]** cd 路径用占位符 `/path/to/<project>` 或仓库内相对路径，不写使用者本机绝对路径
- **[软]** 纯从公共仓库拉取的一行式安装命令（`pip install <pypi-pkg>` / `npm i -g <pkg>` / `winget install <id>` / `curl | sh`）可免 cd——但安装后的验证命令若依赖本项目环境，必须重新给 cd
- **[软]** 长路径可用变量：`PROJECT=/path/to/<project>; cd $PROJECT`，方便后续命令引用

**Why**：
- 读者习惯直接复制整个代码块粘到终端，缺 cd 会跑到错的 CWD
- 同机多项目时，`<lang> run` / `<lang> -m` 会命中错的本地环境（venv / node_modules），症状是 `ModuleNotFoundError` / `Cannot find module` 但定位困难
- `docker build` / 测试命令 / `-m <module>` 等绝大多数命令都**隐式依赖** CWD

**模板**：

```bash
# 在项目根目录执行
cd /path/to/<project>

<command>
```

**反模式**：

```bash
# ❌ 注释里说在项目根执行，但没给 cd 命令
# 注意：必须在项目根下运行
<command>

# ❌ 一块内混用两个目录
<cmd-A>                     # 这行需要在项目根
cd ../other-project          # 临时切目录
<cmd-B>                      # 这行在另一个项目下
```

### 2.4 目录树与文件引用同步（移动文件后的义务）

**[硬]** README 的目录结构树、文件清单表、以及正文对文件的引用（"详见 `X.md`" / 链接），必须与**当前 `ls` 一致**——任何死链 / 旧路径都算违规。

**[硬]** **移动 / 重命名 / 删除**项目内文件后，**同一个 commit** 内必须同步：

1. README 的目录结构树
2. README 正文对该文件的所有文字引用
3. `development/plans/` 里仍在生效的 PLAN 文档对该文件的引用
4. 源码注释 / docstring 里的路径引用

**验证命令**（移动前后各跑一次对比）：

```bash
cd /path/to/<project>

# 扫旧名的残留引用（<old-name> 换成被移动/删除的文件名或路径）
grep -rn '<old-name>' README.md development/ src/

# 对 README 目录树里的每一行路径，校验真实存在
grep -oE '[├└│ ]+\K[A-Za-z0-9_./-]+' README.md | while read p; do
  test -e "$p" || echo "MISSING: $p"
done
```

**典型事故**（避免重犯）：
> 文件移动到子目录后，git 识别了 rename 但 README 目录树与正文引用未同步，死链直到 code review 才被发现。根因：只看了 `git status`，没 `grep` 旧名。

### 2.5 LLM 协作文档原则

- **[硬]** 规范类文档（rule / guide）**只写规范与流程**——结构约束、命名约定、分层原则、反模式清单。**不**贴入口代码样板 / 脚手架生成脚本 / SDK API 细节。
- **[硬]** 具体实现代码由大模型按目录规范**按需生成**；文档定义契约，代码由模型填充。
- **[硬]** 面向 LLM 的 docstring / tool description 必须写清**何时使用**（触发场景），不只是"做什么"——LLM 根据 description 决策调用时机。

### 2.6 `guide/` 文档写作细则

承接 §2.5 第 1 条，对各语言栈 `guide/` 下的指南文档（环境准备 / 源代码结构 / CI/CD / 使用方式）进一步约束。

**[硬]** `02-源代码结构.md`（或同名 / 同义文档）只写四类内容：

1. **推荐目录骨架**：`tree` 风格 + 每行一句注释，标注 `[必需]` / `[软]`
2. **各层职责**：每层一句话职责 + 一句话禁区
3. **注册约定**：工具 / 资源 / 提示词如何被 SDK 发现注册（注解扫描 / 显式 API / 宏路由）
4. **协议层差异**：stdio vs http 在源码层的差异点 + 强约束（如"日志写 stderr"）

**[硬]** 禁止内容（违反 §2.5 第 1 条的常见形态）：

- 完整入口代码（`Program.cs` / `main.go` / `index.ts` / `main.rs` / `__main__.py` 等的可运行实现）
- 多步骤工具定义样板（> 5 行的单工具示例）
- 错误处理 / DI 注册 / 生命周期钩子的代码样板

**[硬]** API 用法以**对应 SDK 官方包文档**（NuGet / npm / PyPI / crates.io / Maven Central / pkg.go.dev）为**单一事实源**——SDK 演进快于指南维护，case by case 代码贴在指南里很快过时。

**自检口诀**：
> 砍掉这段代码块，读者还能照规范完成搭建吗？  
> 能 → 砍；不能 → 把"不能"的原因转写成规范条款，而不是塞代码示例。

### 2.7 Markdown 代码块语言标签

**[硬]** JSON 类代码块按**内容**选标签（详见 [`.codebuddy/rules/markdown-json-fence-language-tag.mdc`](../.codebuddy/rules/markdown-json-fence-language-tag.mdc)）：

| 代码块内容 | 标签 | 典型场景 |
|-----------|------|---------|
| 严格 JSON（无注释、无扩展语法）| ` ```json ` | API 响应、JSON-RPC 消息、JSON Schema |
| JSON + `//` 或 `/* */` 注释（**90% 配置场景**）| ` ```jsonc ` | `.mcp.json` / `.vscode/settings.json` / `tsconfig.json` 等配置示例 |
| 用到 JSON5 扩展（单引号 / 尾逗号 / 未引号字段名）| ` ```json5 ` | 演示宽松语法的非严格示例 |

**默认推荐 jsonc**：除非确定要用 JSON5 扩展语法，否则配置类示例一律 jsonc——VS Code / IDEA 官方支持，注释不爆红，且语义比 json5 更标准。

**其他常用标签**：

| 实际内容 | 标签 |
|---------|------|
| Bash / sh 命令 | `bash` |
| PowerShell | `powershell` |
| TOML（`pyproject.toml` / `Cargo.toml`）| `toml` |
| Dockerfile | `dockerfile` |
| 目录树 | 无标签（三反引号空标签即可）|

**Why**：错的语言标签 → 渲染器红波浪 / 高亮失效 → 读者误以为示例有错。

---

## §3 版本号规则

采用**语义化版本** `MAJOR.MINOR.PATCH`：

- **MAJOR**：输入输出契约不兼容、公开 API 签名变更
- **MINOR**：新增能力（向后兼容）
- **PATCH**：bug 修复、文案修正、性能优化

**[硬] 单一事实源**：版本号只写在语言的 config 文件里，其他地方一律通过指针引用：

| 语言 / 生态 | 版本号位置 |
|---|---|
| Python | `pyproject.toml` 的 `[project] version` |
| Node.js | `package.json` 的 `"version"` |
| Rust | `Cargo.toml` 的 `[package] version` |
| Java (Maven) | `pom.xml` 的 `<version>` |
| Go | git tag（无 config 字段，靠 tag）|
| .NET | `<Project>.csproj` 的 `<Version>` |

**[硬]** README / GUIDE / 其他文档**不硬写版本号**，用"版本见 `<config 文件>`"的指针式表达。

---

## §4 命名规范

- **[硬]** 项目名用"**对象 + 动作**"描述功能：`<prefix>-case-converter` / `<prefix>-log-summarizer`；**不用** `<prefix>-demo-1` / `<prefix>-test` / `<prefix>-foo` 这类无语义名
- **[硬]** 引擎 / 核心模块按**业务语义**命名（`analyze` / `convert` / `summarize` / `generate` / `fetch`），**不用** `server.py` / `core.py` / `engine.py` / `utils.py` / `helper.py` 这类**泛名**
- **[硬]** **文件系统名**与**代码标识符**对应关系：项目名 / 制品名用连字符（`my-project`），代码标识符按语言规则转换（Python 包名用下划线 `my_project`；Rust crate 同源；Node 包名本身允许连字符）
- **[软]** 小写 + 连字符；避免驼峰
- **[软]** 常量 / 枚举大写 + 下划线（`MAX_RETRIES`）

---

## §5 源码与配置文件注释政策

- **[硬]** **代码注释一律中文**；标识符、API 名、错误信息保留英文——注释面向人读，代码面向机器执行
- **[硬]** **配置文件的注释一律中文**——覆盖 YAML / TOML / JSON5 / `.properties` / `.env` / Dockerfile / Makefile / shell 脚本 / `pyproject.toml` / `pom.xml` 注释 / nginx 等 conf 等所有可写注释的配置格式
- **[硬]** **只留当前 WHY**；不写历史轨迹（`v0.2 新增`、`参见 FINDINGS_xxx`、`原来用 X，后改为 Y 因为...`）——历史在 git log 里
- **[硬]** 面向 LLM 的 docstring / tool description 必须写清**何时使用**（触发场景），不只是"做什么"（见 §2.5）
- **[软]** 只注释**非显而易见的** WHY；识别符已经表达的 WHAT 不重复注释
- **[软]** 写「反直觉」的行为时用单行注释点明（边界条件 / 为什么不用看起来更直观的写法）
- **[软]** 不要中英混排一行；要么全中文要么全英文（保留英文限于以上"标识符 / API 名 / 错误信息"）

---

## §6 `development/` 开发期资料目录

**[硬]** 每个具体项目 `<project>/` **必须**有 `development/` 同级目录，放**不随制品分发**的开发资料：

| 子目录 | 内容 | 命名约定 |
|---|---|---|
| `plans/` | 架构决策迭代：每次大改写新 PLAN 文件，**保留历史** | `PLAN_v01.md` / `PLAN_v02.md` / ... |
| `findings/` | 调研、验证、问题排查的产出 | `FINDINGS_<YYYYMMDD>_<HHMMSS>.md` |
| `samples/` | 测试 / 调试用的样本输入数据 | 按原文件名保留 |

**原则**：

- **[硬]** `development/` **不进制品**（wheel / npm 包 / jar / 镜像）——构建工具默认只打包 `src/` 或指定 package 目录，与 `development/` 天然隔离；自定义打包配置时别漏
- **[硬]** `plans/` 保留历史版本，用新文件而非覆盖旧文件；**git log 不代替 PLAN 历史**——LLM 协作需要可读的方案迭代链
- **[硬]** `findings/` 的时间戳用**当时的真实时间**（不是提交时间），方便按时间线回看调研过程
- **[硬]** `findings/` 正文必须有 **Findings 汇总**
- **[硬]** `findings/` 每条 finding 必须给出**推荐的改动建议**；仅为提醒性质的可写"**保留现状**"
- **[软]** LLM 协作产物（提示词模板、笔记）也放 `development/` 根下；不占顶层位置
- **[软]** `samples/` 大文件按 `.gitignore` 决定是否入库；测试样本入库、大型二进制考虑 LFS 或外部存储

---

## §7 交付前通用检查清单

每次 commit / PR 前自检：

| # | 条目 | 强度 | 验证方法 |
|---|------|------|---------|
| 1 | README 目录结构与实际 `ls` 一致 | [硬] | 见 §2.4 校验命令 |
| 2 | README 无硬编码版本号 | [硬] | `grep -nE '当前版本[：:]\|version[：:\s]+[0-9]' README.md` 返回 0 行 |
| 3 | 所有命令块首行给出 cd（或属公共 registry 一行安装） | [硬] | 在 `.md` 里看每个 `bash`/`sh` 代码块第一行 |
| 4 | 移动 / 重命名 / 删除文件后无死链 | [硬] | 见 §2.4 grep 命令 |
| 5 | 规则 / 指南无具体实现项目的硬引用 | [硬] | 规则文档内只出现占位符和示意性业务词（§1.4）|
| 6 | 版本号已升位（按 §3）| [硬] | `git diff` 查 config 文件的 version 字段 |
| 7 | README / GUIDE 无源码片段 / 依赖 dump / 配置 dump（§2.2）| [硬] | 审视最近改动 |

---

## §8 规则演化

- **[硬]** 规则变更走 git commit，commit message 说明变更原因
- **[软]** 每次领域主版本升级时回顾本文件，过时条款清理
- **[软]** 与专用规则冲突时，专用规则应**显式说明覆盖理由**；若只是"本领域暂无需要"，倾向于直接删除专用条款而非覆盖
