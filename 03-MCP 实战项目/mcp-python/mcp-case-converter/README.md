# mcp-case-converter

> Python 版字符串大小写与命名风格转换：**CLI + MCP Server 双入口**，共用同一纯 Python 引擎。覆盖 10 种常用风格：`upper / lower / swap / title / capitalize / camel / pascal / snake / kebab / constant`。

---

## 1. 能力

CLI 子命令与 MCP tool 一一对应：

| MCP tool | CLI 子命令 | 说明 |
|------|------|------|
| `convert_case(text, mode)` | `case-converter convert <text> <mode>` | 按指定 mode 转换 |
| `batch_convert(text)` | `case-converter batch <text>` | 一次性返回 10 种风格（JSON） |
| `split_to_words(text)` | `case-converter split <text>` | 把任意风格命名切成小写单词列表（JSON） |
| `list_modes()` | `case-converter list-modes` | 列出所有模式与示例 |

### 支持的模式

| mode | 示例输入 | 示例输出 |
|------|---------|---------|
| `upper` | `Hello World` | `HELLO WORLD` |
| `lower` | `Hello World` | `hello world` |
| `swap` | `Hello` | `hELLO` |
| `title` | `hello world` | `Hello World` |
| `capitalize` | `hello WORLD` | `Hello world` |
| `camel` | `user_name_id` | `userNameId` |
| `pascal` | `user_name_id` | `UserNameId` |
| `snake` | `HelloWorld` | `hello_world` |
| `kebab` | `My Component` | `my-component` |
| `constant` | `pageSize` | `PAGE_SIZE` |

对 `HTTPServer` / `getHTTPResponse` 这类**首字母缩写**也能正确切词。

---

## 2. 安装

### 2.1 用 uv（推荐）

```bash
cd mcp-case-converter
uv sync
```

### 2.2 用 pip

```bash
cd mcp-case-converter
pip install -e .
```

---

## 3. CLI 使用（`case-converter`）

安装后可用脚本 `case-converter`；等价路径 `python -m mcp_case_converter`。

```bash
# 在项目根目录执行（已 pip install -e .）
cd mcp-case-converter

# 单次转换
case-converter convert 'HTTPServer request' camel       # -> httpServerRequest

# 一次看 10 种风格（JSON）
case-converter batch 'user_name_id'

# 切词
case-converter split 'getHTTPResponse'                  # -> ["get","http","response"]

# 列出所有模式与示例
case-converter list-modes
```

参数非法（未知 `mode`）以非零退出码 + stderr 提示报错。

---

## 4. MCP Server：`--transport stdio | http`

独立脚本 `mcp-case-converter`；等价路径 `python -m mcp_case_converter.entrypoints.mcp`。**完整签名**：

```
mcp-case-converter [--transport stdio|http] [--host HOST] [--port PORT]
```

| 参数 | 默认 | 说明 |
|------|------|------|
| `--transport` | `stdio` | `stdio` 走进程管道；`http` 走 **Streamable HTTP**（2025-03 新规范，非旧版 SSE 双端点） |
| `--host` | `127.0.0.1` | **仅 http 模式**生效，监听地址 |
| `--port` | `8000` | **仅 http 模式**生效，监听端口 |

> 旧版 SSE（`/messages` + `/sse` 双端点）已逐步淘汰，本 Server 的 `http` 模式使用 Streamable HTTP 单端点 `POST /mcp`，需要流式推送时自动升级为 SSE。参见 `05-MCP 学习笔记/02-协议规范/02-传输层-Stdio与SSE.md`。

### 4.1 选型速查

| 你的场景 | 推荐 | 原因 |
|---------|------|------|
| Claude Code / Claude Desktop 本地调用 | **stdio** | 零配置、天然进程隔离、无需开端口 |
| 团队内多人共享一个 Server 实例 | **http** | 单服务常驻，多 Client 可连 |
| 给同机其他非 MCP 服务调用（curl / 浏览器） | **http** | 标准 HTTP，易调试 |
| 需要访问本地敏感凭据、文件系统 | **stdio** | 不要把本地数据暴露到网络 |
| 容器化 / K8s 部署 | **http** | 进程常驻、探活/日志标准化 |

### 4.2 stdio 模式（默认）

**启动**（一般不手动启动——由 MCP Client 拉起子进程）：

```bash
# 在项目根目录执行
cd mcp-case-converter

mcp-case-converter                               # 默认 stdio
mcp-case-converter --transport stdio             # 显式
```

**Client 侧配置**（`.mcp.json`）：

```jsonc
{
  "mcpServers": {
    "case-converter": {
      "command": "mcp-case-converter"
    }
  }
}
```

要点：
- Client 启动 Server **子进程**，通过 stdin/stdout 交换 JSON-RPC
- stderr 是日志通道，不会干扰协议；**不要用 `print()` 写 stdout**
- Host 关闭时进程自动回收

### 4.3 http 模式（Streamable HTTP）

**启动常驻服务**：

```bash
# 在项目根目录执行
cd mcp-case-converter

# 默认只监听本地回环
mcp-case-converter --transport http

# 监听所有网卡，自定义端口
mcp-case-converter --transport http --host 0.0.0.0 --port 9000
```

启动后 Server 暴露单端点 `POST http://<host>:<port>/mcp`。

**Client 侧配置**：

```jsonc
{
  "mcpServers": {
    "case-converter": {
      "url": "http://127.0.0.1:8000/mcp"
    }
  }
}
```

**curl 验证端点可达**（初始化握手）：

```bash
curl -X POST http://127.0.0.1:8000/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
      "protocolVersion": "2025-03-26",
      "capabilities": {},
      "clientInfo": {"name": "curl", "version": "0.1"}
    }
  }'
```

响应头中的 `Mcp-Session-Id` 需要在后续请求里原样回带，用于会话路由。

### 4.4 两种模式对比

| 维度 | stdio | http |
|------|-------|------|
| 启动方 | Client 拉起子进程 | 独立常驻服务 |
| 寻址 | `command + args` | URL |
| 鉴权 | 进程信任 | HTTP Header（Authorization / OAuth） |
| 跨机器 | ❌ | ✅ |
| 多 Client 共享 | ❌（每个 Client 各起进程） | ✅ |
| 日志 | stderr | 标准 HTTP 访问日志 |
| 启动开销 | 每次 Client 启动都要拉进程 | 一次启动长期复用 |
| 适合快速开发调试 | ✅ | ⚠️ 需要额外管理端口/进程 |

### 4.5 已发布到 PyPI 后

```jsonc
{
  "mcpServers": {
    "case-converter": {
      "command": "uvx",
      "args": ["mcp-case-converter"]
    }
  }
}
```

`uvx` 会自动下载并缓存，无需本地克隆仓库。

---

## 5. 调试

### 5.1 MCP Inspector

```bash
# 在项目根目录执行
cd mcp-case-converter

npx @modelcontextprotocol/inspector mcp-case-converter
```

浏览器打开后可手动调用 tool、查看原始 JSON-RPC 流量。

### 5.2 单元测试

```bash
# 在项目根目录执行
cd mcp-case-converter

uv run pytest
# 或
pytest
```

---

## 6. 目录结构

```
mcp-case-converter/
├── pyproject.toml
├── README.md
├── src/mcp_case_converter/
│   ├── __init__.py
│   ├── __main__.py                  · `python -m mcp_case_converter` 薄委托到 entrypoints.cli
│   ├── converter.py                 · 引擎：纯转换逻辑（无 MCP 依赖，可独立复用）
│   └── entrypoints/
│       ├── __init__.py              · 传输层入口子包说明
│       ├── cli.py                   · `case-converter` 一次性 CLI（argparse subcommand）
│       └── mcp.py                   · `mcp-case-converter` MCP Server（--transport stdio|http）
├── development/                     · 开发期资料（不随 wheel 分发）
│   ├── plans/                       ·   架构迭代方案 PLAN_vNN.md
│   ├── findings/                    ·   调研 / 验证产出 FINDINGS_<ts>.md
│   └── samples/                     ·   测试样本
└── tests/
    └── test_converter.py            · converter 单测
```

---

## 7. 与同仓 Skill 的区别

`04-Skill 实战项目/skill-01-case-converter/` 是 Claude Code Skill 形态，通过脚本调用 Java JAR 实现；仅覆盖 `upper / lower / swap` 三种模式。

本项目是独立的 **MCP Server**，纯 Python，覆盖 10 种编程场景常用风格，供 Claude Code / Claude Desktop / 任意 MCP Client 调用。
