<!--
本文件为 MCP + Python 专用规则。

- 基础原则（最高协作原则 / 文档工程 / 版本号 / 命名 / 注释 / development/ 布局 /
  通用交付清单 / 规则演化）见 ./universal_develop_rule.md
- 本文只叠加 MCP 协议与 Python 生态特定的条款
- 冲突时以 universal 为准；本文显式覆盖需说明理由

对比其他语言版（看哪些核心条款漂了）：
  git diff mcp-python/mcp_develop_rule.md mcp-nodejs/mcp_develop_rule.md
-->

# MCP 项目开发规则（Python 版）

> 目的：让 `06-MCP 实战项目/mcp-python/` 下的每个 MCP Server 项目都遵循统一规则，避免走岔路、漏更新、版本漂移。
> **基础原则在** [`./universal_develop_rule.md`](./universal_develop_rule.md)，本文只叠加 MCP + Python 专属条款。

---

## 强度约定

与 `universal_develop_rule.md` 一致：

- **[硬]** MUST：违反即不合格
- **[软]** SHOULD：默认遵守，变通需 commit 说明
- **[选]** MAY：推荐但非强制

---

## §1 MCP 专用最高原则

1. **[硬] stdio 协议通道不可污染**：stdio 传输模式下，任何写到 stdout 的语句都是 bug；日志强制 stderr（详见 §6.2）。

其他最高协作原则（基于最新文档开工 / 多处同步 / 只做本次任务 / 规则独立于实现）见 `universal §1`。

---

## §2 交付前检查清单（MCP 专用）

每次 MCP Server 项目提交前，除 `universal §7` 的通用检查外，还需：

| # | 条目 | 强度 | 验证方法 |
|---|---|---|---|
| 1 | guide/ 如有结构变化已同步（01-环境准备 / 02-CICD / 03-使用方式） | [硬] | 对比 `guide/01-环境准备.md §1.6` 的目标项目结构 |
| 2 | 传输参数（`--transport stdio\|http`）双通 | [硬] | 手动跑一次 stdio + 一次 http，都能返回 initialize 结果 |
| 3 | stdio 模式下**无任何 stdout 日志** | [硬] | `grep -rn 'print(' src/` 在 stdio 入口路径返回 0 行 |

---

## §3 标准目录结构

> 本节是**抽象规范**，用占位符（`<engine>`、`<domain>`、`mcp-<feature>`）描述结构约束。具体命名与落地由各 `mcp-<feature>/` 项目自行选取（见 `universal §1.4`）。

### 3.1 顶层布局

```
mcp-<lang>/
├── guide/                                  # [必需] 该语言 MCP Server 开发指南（3 份）
│   ├── 01-环境准备.md
│   ├── 02-源代码结构.md
│   ├── 03-CICD.md
│   └── 04-使用方式.md
├── mcp-<feature>/                          # [必需] 具体 MCP Server 实现（可多个）
│   ├── README.md                           # [必需] 使用说明（答「是什么/在哪/怎么用」，见 §4）
│   ├── pyproject.toml                      # [必需] 项目配置 + 版本号（`[project] version`，universal §3）
│   ├── src/                                # [必需] src-layout 的外层目录
│   │   └── <pkg>/                          # [必需] 包目录；<pkg> 用下划线，与项目名 mcp-<feature> 对应
│   │       ├── __init__.py                 # [必需] 仅暴露 __version__，不放业务
│   │       ├── __main__.py                 # [必需] `python -m <pkg>` 薄委托到 entrypoints.cli
│   │       ├── <engine>.py                 # [必需] 引擎 / 编排器（纯逻辑，不依赖传输 SDK）
│   │       ├── common.py                   # [软] 跨模块共享工具（路径/日志/配置）
│   │       ├── <domain>.py                 # [软] 引擎较大时按子领域再拆的业务模块
│   │       ├── entrypoints/                # [必需-多入口] 传输层入口集中地（见 §3.4）
│   │       ├── knowledge/                  # [软] 静态知识资源 YAML/JSON，随包分发（见 §3.5）
│   │       ├── phases/                     # [软] 多阶段流水线，每阶段一文件（见 §3.6）
│   │       └── tools/                      # [软] 外部工具/二进制 jar/脚本，随包分发（见 §3.5）
│   ├── development/                        # [必需] 开发期资料（见 universal §6）
│   │   ├── plans/                          #        架构迭代方案 PLAN_vNN.md
│   │   ├── findings/                       #        调研 / 验证产出 FINDINGS_<ts>.md
│   │   └── samples/                        #        测试样本数据
│   └── tests/                              # [软] 单元测试（pytest + pytest-asyncio）
└── doc/                                    # [选] 跨项目参考文档、脑图
```

### 3.2 核心约束

- **[硬]** `mcp-<lang>/` 根目录**不**直接放可执行代码；代码一律在 `mcp-<feature>/` 子项目内
- **[硬]** 每个 `mcp-<feature>/` 是一个**独立构建单元**，自带 config + 依赖声明，独立可跑
- **[硬]** 用 **src-layout**：源码放 `src/<pkg>/`，`pyproject.toml` 配 `[tool.hatch.build.targets.wheel] packages = ["src/<pkg>"]`
- **[硬]** 项目名与包名对应：项目名 `mcp-<feature>`（连字符）、包名 `mcp_<feature>`（下划线）
- **[硬]** `guide/` 下 3 份文件是**语言级**开发指南，与具体功能解耦；不把 `mcp-<feature>` 的实现细节塞进 guide/

通用命名约定（"对象 + 动作"、不用 `server.py` / `core.py` 等泛名）见 `universal §4`。

### 3.3 代码分层（引擎 vs 传输层）

每个 `mcp-<feature>/` 必须按三层拆分：

| 层 | 职责 | 本语言落地 |
|---|---|---|
| **引擎** | 纯业务逻辑，不 import 任何传输 SDK | `<engine>.py` 按业务语义命名（示意：`analyze.py` / `convert.py` / `summarize.py` / `generate.py`） |
| **传输接入** | 把某种协议的调用模型翻译为对引擎的调用 | `entrypoints/<transport>.py`（见 §3.4） |
| **python -m 入口** | `python -m <pkg>` 快捷入口 | `__main__.py`，**薄委托**到 `entrypoints.cli:main` |

**原则**：

- **[硬]** 引擎层**不 `from mcp import ...`、不 `import argparse`**；传输 SDK 和 CLI 参数解析都在对应入口文件
- **[硬]** 一旦出现第二种传输（CLI + MCP、或 MCP + HTTP），立刻迁到 `entrypoints/`（见 §3.4）；单传输时可暂时省略
- **[软]** 引擎大时按子领域再拆（每个子领域一个 `<domain>.py`），文件命名按业务语义

（模块命名禁用泛名 `server.py` / `core.py` / `engine.py` / `utils.py` 已在 `universal §4`）

### 3.4 多传输层入口隔离（`entrypoints/` 子包）

当同一个 package 需要暴露**多种传输方式**（如 CLI + MCP + 将来的 HTTP）时，不要把入口文件散在 package 顶层，而是集中到 `entrypoints/` 子包下。

**布局**：

```
src/<pkg>/
├── __init__.py
├── __main__.py               # `python -m <pkg>` 默认入口（通常代理到 entrypoints/cli.py）
├── <engine>.py               # 引擎 / 编排器（纯逻辑，不依赖任何传输 SDK）
├── ...
└── entrypoints/              # 各传输层入口集中地
    ├── __init__.py           # 子包说明（docstring 列清每个入口的职责）
    ├── cli.py                # argparse 一次性 CLI
    └── mcp.py                # MCP Server（stdio / streamable HTTP），常驻
    # 按需再加 <transport>.py，并在 pyproject.toml 的 [project.scripts] 注册
```

**原则**：

- **[硬]** 每个入口模块只做 **"传输协议 ↔ 引擎调用"** 的翻译；业务判断一律在引擎里
- **[硬]** 传输 SDK（如 `fastmcp`、`fastapi`）**只在对应入口文件 import**，确保 engine-only 使用者不必加载它
- **[硬]** `pyproject.toml` 的 `[project.scripts]` 指向 `<pkg>.entrypoints.<transport>:main`，而非顶层；否则 `entrypoints/` 组织价值就丢了
- **[软]** 入口模块命名避开 Python 标准库 / 常用 PyPI 包同名：`http.py` 会和 stdlib `http` 混淆（绝对 import 能区分但误导读者），建议用 `http_server.py` / `mcp_server.py` 或保留 `entrypoints/` 命名空间（子包内重名不会冲突）

**何时用 / 何时不用**：

| 场景 | 是否用 `entrypoints/` |
|---|---|
| 只暴露 MCP Server 一种传输 | ❌ 不用，`<engine>.py` + MCP 注册在同文件 + `__main__.py` 即可（§3.3） |
| 同时有 CLI + MCP | ✅ 用 |
| 现在只 MCP，**但将来可能加** HTTP/gRPC | ✅ 用，给未来留位置 |
| 只是"看起来更整齐" | ❌ 不用，多一层 import 路径不是价值 |

**拆分粒度的红线（按协议拆，不按 transport 变体拆）**：

**[硬]** `entrypoints/` 下每个文件对应**一种协议 / 调用模型**（CLI / MCP / HTTP 业务 API / gRPC / ...），**不是**同一协议下的 transport 变体。

| ✅ 应该拆成两个文件 | ❌ 不应该拆 |
|---|---|
| `cli.py` + `mcp.py`（argparse 一次性 vs MCP Server 常驻，**调用模型**不同） | `mcp_stdio.py` + `mcp_http.py`（同一 MCP 协议，只是 transport 不同） |
| `mcp.py` + `grpc.py`（两种不同协议的 RPC） | 按 `--host` / `--port` 等启动参数差异拆多个文件 |

**为什么 stdio + http 不拆**：

- FastMCP 的 `FastMCP(...)` 实例与 `@mcp.tool()` 注册都**传输无关**；真正分流只在 `main()` 末尾 `mcp.run_stdio_async()` vs `mcp.run_streamable_http_async(host, port)` **这一行**。
- 拆两个文件要么**复制所有工具定义**（容易漂移），要么抽一个 `_mcp_common.py` 让两边 import（徒增一层抽象），都得不偿失。
- 正确做法：一个 `mcp.py`，`main()` 里用 `--transport stdio|http` argparse 分支切换（模板见 `guide/04-使用方式.md §1.3`）。

**判别口诀**：
> 如果两个「入口」的差异**只在 `main()` 的最后一行启动语句**，就是同一入口的不同启动模式，**一个文件 + argparse 分支**，不拆文件。

**`entrypoints/__init__.py` 模板**（照抄即可，填入实际入口；**只列真实存在的文件**，不预先留"placeholder 未实现" 的条目）：

```python
"""<pkg> 引擎的传输层入口子包。

本目录下每个模块暴露一个 `main()` 函数，映射到 pyproject.toml 的
`[project.scripts]` 条目；**引擎代码在父 package 里**，本目录下的文件
都是薄适配层，负责把某种传输协议的调用模型翻译为对引擎的调用。

  cli.py   —— 基于 argparse 的 CLI（一次性运行，跑完即退出）
  mcp.py   —— MCP Server（stdio / streamable HTTP），常驻等调用

将来需要新传输层（HTTP / gRPC / ...）时，新建同目录的 `<name>.py`，
并在 pyproject.toml 的 `[project.scripts]` 注册一行即可。
"""
```

### 3.5 静态资源子包（`knowledge/` 与 `tools/`）

**何时用**：引擎需要**随包分发**的只读数据或外部二进制。

| 子包 | 装什么 | 命名示意 |
|---|---|---|
| `knowledge/` | **只读数据**：启动时加载、运行期仅查不改。格式以 YAML / JSON / TOML 为主 | `<topic>.yaml` 按主题拆（如 `taxonomies.yaml` / `providers.yaml` / `templates.yaml`） |
| `tools/` | **外部工具 / 二进制**：jar、可执行脚本等，引擎在运行时调用 | `<external-tool>-<version>.jar` / 可执行脚本 |

**原则**：

- **[硬]** 代码访问资源一律用 `importlib.resources`，**不**硬编码相对路径
- **[硬]** 两个子包里的资源文件都要随 wheel 打包；`pyproject.toml` 用 hatchling 默认 `packages = ["src/<pkg>"]` 可整包带走，自定义 include/exclude 时别漏
- **[软]** `tools/` 下放 `README.md` 说明每个二进制的来源、版本、许可协议
- **[软]** `knowledge/` 里的 YAML 大时按主题拆多个文件（数量按领域而定，典型 3-10 个），而非一个巨型文件

### 3.6 多阶段流水线（可选：`phases/` 子包）

**何时用**：引擎的处理过程可以明确拆分为**多个顺序阶段**（如"解压 → 解析 → 提取 → 渲染"），单阶段复杂到值得单文件。

**命名形如** `phase_a_<verb>.py` → `phase_<letter>_<verb>.py`，字母序即执行序（示意：`phase_a_ingest.py` / `phase_b_parse.py` / `phase_c_render.py`，实际按领域业务取词）。

**原则**：

- **[硬]** 每个 `phase_*.py` 对外只暴露一个入口函数（如 `run(ctx) -> ctx_delta`），输入输出契约清晰
- **[硬]** 命名 `phase_<letter>_<verb>.py`：字母序即执行序，跳字母（如保留给未来扩展）不影响可读性
- **[硬]** 3 个以下阶段或拆分无明显收益时**不要提前拆**，留在引擎主文件
- **[软]** `phases/__init__.py` 的 docstring 列清每个阶段的职责与前后依赖

---

## §4 README 规则（MCP 专用）

**定位 / 反模式 / 命令块规范 / 目录树同步**见 `universal §2`，本节只列 MCP 特有内容。

### 4.1 何时必须更新（MCP 专用触发）

**[硬]**：
- MCP 工具 / 资源 / 提示（tool / resource / prompt）增删
- 使用方式变更（参数、CLI、前置依赖）
- 传输方式或端口变更

（通用触发——结构变更、依赖变更——见 `universal §2.1` 与 `§2.4`）

### 4.2 必备章节（MCP 专用）

`universal §2.1` 的三问基础上，MCP 项目 README 必须包含：

- **[硬]** 一句话简介
- **[硬]** 能力清单（工具名 + 一行中文描述，**不**写参数类型签名和 JSON Schema）
- **[硬]** 构建与运行（含 `--transport stdio|http`）
- **[硬]** Client 挂载配置（`.mcp.json` 示例）
- **[硬]** 目录结构（与实际文件一致）
- **[硬]** 调试方式（Inspector 命令一行 + 单测命令一行）

---

## §5 guide/ 规则

**[硬]** `guide/` 下 3 份文件是**语言层**（不是项目层）的开发指南，面向"用该语言新建一个 MCP Server 的开发者"。

- **[硬]** 3 份文件分别覆盖：环境准备 / CI/CD / 使用方式；**不**描述具体功能实现（那是 README 的事），也**不**写 SDK API 细节或入口代码骨架（大模型按目录规范自行生成，见 `universal §2.5`）
- **[硬]** 给出 stdio / Streamable HTTP 双传输的配置
- **[硬]** 给出常见坑位（`stdout 污染`、`日志强制 stderr`、`冷启动`）
- **[硬]** 跨文件引用用相对路径链接，**不**维护重复内容
- **[软]** 末尾可加软引用"可浏览同目录下任一 `mcp-<feature>/` 作为落地示例"，但 guide 主体独立于具体项目（`universal §1.4`）

---

## §6 传输与日志规则

### 6.1 传输必须双通

**[硬]** 每个 MCP Server 都要支持：

- **stdio**（默认）：本地工具场景
- **Streamable HTTP**：远程/容器化场景

CLI 用 `--transport stdio|http` 切换，`http` 再接 `--host` / `--port`。一个 `mcp.py` 文件 + argparse 分支即可，**不拆** `mcp_stdio.py` / `mcp_http.py`（§3.4 拆分粒度红线）。

### 6.2 stdio 日志

- **[硬]** stdio 模式下**严禁**把任何内容写到 stdout（stdout 是协议通道）
- **[硬]** 日志强制写 stderr：`logging.basicConfig(stream=sys.stderr, level=logging.INFO)`；业务日志统一 `logging.getLogger(__name__)`
- **[硬] 反模式**：`print(...)` 写 stdout；开发期排查也一律用 `print(..., file=sys.stderr)` 或 logging

### 6.3 错误分层

- **业务错误**（参数非法、资源不存在）→ 返回 `isError: true` 的 ToolResult，LLM 可读到并纠正
- **系统错误**（未预期异常、协议错误）→ 抛异常 / 返回 error，走 JSON-RPC error

---

## §7 测试规则

- **[软]** 纯逻辑模块必有单元测试：`pytest`（+ `pytest-asyncio` 支持异步工具）
- **[软]** MCP 接入层可用 Inspector（`npx @modelcontextprotocol/inspector <cmd>`）手工验收
- **[选]** CI 跑单测 + 一次最小协议握手

---

## §8 配套资料

- **通用规则**：[`./universal_develop_rule.md`](./universal_develop_rule.md)（最高原则 / 文档工程 / 版本号 / 命名 / 注释 / development 布局 / 通用交付清单）
- 流程与提示词模板：`mcp_develop_guideline.md`（同目录）
- 语言指南：`./guide/`（同目录；3 份文件：01-环境准备 / 02-CICD / 03-使用方式）
- 跨语言选型：`05-MCP 学习笔记/03-开发指南/01-MCP-Server-语言选型.md`
- 传输层原理：`05-MCP 学习笔记/02-协议规范/02-传输层-Stdio与SSE.md`
