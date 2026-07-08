# SOP-06 MCP 工具与资源

> 定位：MCP 生态的工具链、调试手段、学习入口、社区资源汇总

---

## 1. 核心工具

### 1.1 MCP Inspector（必备）

官方可视化调试工具，对接任意 Server 展示 Tool/Resource/Prompt 并支持手动调用。

```bash
# 调试 Stdio Server
npx @modelcontextprotocol/inspector python -m my_mcp_server
npx @modelcontextprotocol/inspector node dist/index.js

# 调试 HTTP Server（启动后在 UI 里输入 URL）
npx @modelcontextprotocol/inspector
```

**主要功能**：
- Tool 列表与 inputSchema 预览
- 手动调用 Tool 并查看响应
- Resource 浏览与 subscribe 测试
- Prompt 渲染预览
- 原始 JSON-RPC 消息流
- 连接日志

### 1.2 官方 SDK

| 语言 | 包名 | 仓库 |
|------|------|------|
| Python | `mcp` | `modelcontextprotocol/python-sdk` |
| TypeScript / JS | `@modelcontextprotocol/sdk` | `modelcontextprotocol/typescript-sdk` |
| Java | `io.modelcontextprotocol:mcp` | `modelcontextprotocol/java-sdk` |
| Kotlin | `io.modelcontextprotocol:kotlin-sdk` | `modelcontextprotocol/kotlin-sdk` |
| Go | `github.com/modelcontextprotocol/go-sdk` | `modelcontextprotocol/go-sdk` |
| C# | `ModelContextProtocol` | `modelcontextprotocol/csharp-sdk` |
| Rust | `rmcp` crate | `modelcontextprotocol/rust-sdk` |
| Swift | `modelcontextprotocol/swift-sdk` | `modelcontextprotocol/swift-sdk` |

### 1.3 脚手架

Python：

```bash
uvx create-mcp-server my-server
```

TypeScript：

```bash
npx @modelcontextprotocol/create-server my-server
```

生成包含项目骨架、Dockerfile、README 的起手项目。

---

## 2. 日常调试技巧

### 2.1 日志写 stderr

Stdio 传输下，stdout 是协议通道，任何 print 都会污染。统一：

```python
import logging, sys
logging.basicConfig(stream=sys.stderr, level=logging.DEBUG,
    format="%(asctime)s %(levelname)s %(name)s - %(message)s")
```

```typescript
// Node：console.error 而非 console.log
console.error("debug msg");
```

### 2.2 抓包原始 JSON-RPC

Stdio 传输下，用 `tee` 双向抓：

```bash
# 创建管道包装脚本
#!/bin/bash
tee /tmp/mcp-in.log | python -m my_mcp_server | tee /tmp/mcp-out.log
```

配置到 `.mcp.json` 的 command 指向这个脚本，然后 `tail -f /tmp/mcp-*.log` 实时看流量。

HTTP 传输直接用 Charles / mitmproxy 抓。

### 2.3 单元测试 + InMemoryTransport

SDK 提供内存传输，避免起进程：

**Python**：

```python
from mcp.shared.memory import create_connected_server_and_client_session

async def test_tool():
    async with create_connected_server_and_client_session(server) as client:
        result = await client.call_tool("my_tool", {"x": 1})
        assert not result.isError
```

**TypeScript**：

```typescript
import { InMemoryTransport } from "@modelcontextprotocol/sdk/inMemory.js";
const [c, s] = InMemoryTransport.createLinkedPair();
```

### 2.4 协议版本快速检查

遇到诡异行为先确认版本：

```bash
# 用 Inspector 查看 initialize 响应里的 protocolVersion
# 或在 Server 日志里打印
```

---

## 3. 规范与标准文档

| 资源 | 链接 | 内容 |
|------|------|------|
| **MCP 官方站** | https://modelcontextprotocol.io/ | 概念、教程、SDK 文档入口 |
| **协议规范仓库** | https://github.com/modelcontextprotocol/specification | 权威 JSON-RPC 协议定义 |
| **协议文档（渲染）** | https://spec.modelcontextprotocol.io/ | 规范的网页版 |
| **Changelog** | 规范仓库 `CHANGELOG.md` | 协议版本变更记录 |

---

## 4. 社区资源

### 4.1 官方仓库集

| 仓库 | 用途 |
|------|------|
| `modelcontextprotocol/servers` | 官方参考 Server 集合 |
| `modelcontextprotocol/python-sdk` | Python SDK |
| `modelcontextprotocol/typescript-sdk` | TS/JS SDK |
| `modelcontextprotocol/inspector` | Inspector 源码 |
| `modelcontextprotocol/create-server` | 脚手架 |

### 4.2 Awesome 列表

搜索 `awesome-mcp-servers`、`awesome-mcp` 获取社区精选集合，包含：

- 按用途分类的 Server 清单
- 实战案例
- 文章与视频教程

**注意**：社区 Server 质量参差，引入前按"安全规范 SOP-02"审核。

### 4.3 论坛与讨论

- 官方 Discord（modelcontextprotocol.io 有入口）
- GitHub Discussions（规范仓库与 SDK 仓库各有）
- X/Twitter 话题 `#MCP`

---

## 5. 学习资源

### 5.1 入门路径

```
[本系列笔记]
  01-核心概念         ← 从这里开始
  02-协议规范
  03-开发指南（选一门语言动手）
  05-生态与集成        ← 在 Host 里挂一个 Server
  04-规范与标准        ← 写生产级 Server 前读
```

### 5.2 官方教程

- Anthropic 博客的 MCP 介绍与案例
- `modelcontextprotocol.io/tutorials` 的 step-by-step
- 各 SDK README 的 Quickstart

### 5.3 视频

- YouTube 搜索 `Model Context Protocol tutorial`
- Anthropic 官方频道的 MCP 相关演讲

### 5.4 实战参考

读 `modelcontextprotocol/servers` 里的参考实现：
- 小型：`time`、`fetch`（单一 Tool）
- 中型：`filesystem`、`sqlite`（多 Tool + Resource）
- 大型：`github`、`puppeteer`（复杂业务 + 鉴权）

---

## 6. 与 Claude Code 生态的衔接

MCP 在 Claude Code 中只是一块拼图，完整生态还包括：

| 组件 | 与 MCP 的关系 |
|------|---------------|
| **Skills** | 在 Prompt 里指导 LLM 如何调用 MCP Tool |
| **Hooks** | 拦截、修改、阻止 MCP 调用（PreToolUse / PostToolUse） |
| **Subagents** | 用 `tools` 字段白名单化 MCP Tool |
| **Settings** | `permissions.allow/deny` 管控 MCP Tool 调用 |
| **@mention** | 引用 MCP Resource 到当前对话 |
| **/command** | 触发 MCP Prompt 模板 |

建议交叉阅读：
- `02-Skill 学习笔记/02-平台指南/01-Claude-Code-Skill完全指南.md`
- `02-Skill 学习笔记/05-MCP协议/01-MCP协议与Skill集成指南.md`（Skill 视角的 MCP 切入）

---

## 7. 性能诊断工具

### 7.1 Tool 延迟分析

在 Server 里加中间件统计：

```python
import time, logging

logger = logging.getLogger("mcp.perf")

def timed(fn):
    async def wrapper(*args, **kwargs):
        start = time.perf_counter()
        try:
            return await fn(*args, **kwargs)
        finally:
            ms = (time.perf_counter() - start) * 1000
            logger.info(f"{fn.__name__} took {ms:.1f}ms")
    return wrapper

@mcp.tool()
@timed
async def search(query: str) -> list:
    ...
```

### 7.2 上下文预算检查

MCP Server 的 Tool descriptions 会占 Token。Claude Code 启动时可查看：

```
/mcp     # 看到 tool 数
/context # 看到 token 占用（不同版本功能名可能不同）
```

---

## 8. 常见错误速查

| 错误信息 | 原因 | 处理 |
|---------|------|------|
| `Method not found` | Tool 名错或未注册 | 确认拼写与 list_tools 是否返回 |
| `Invalid params` | 参数不符合 Schema | 用 Inspector 手动调，对比 Schema |
| `Server disconnected unexpectedly` | Server 崩溃 | 查 stderr 日志与异常堆栈 |
| `Protocol version mismatch` | 版本协商失败 | 升级 SDK / 调整 protocolVersion |
| `Connection refused` | HTTP Server 没起 | 检查进程、端口、防火墙 |
| Tool 出现但调用超时 | Server 内部阻塞 | 检查同步 IO / 死锁 / 长任务没走进度通知 |

---

## 9. 模板与片段

### 9.1 .mcp.json 模板（项目共享）

```jsonc
{
  "mcpServers": {
    "github": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-github"],
      "env": { "GITHUB_PERSONAL_ACCESS_TOKEN": "${GITHUB_TOKEN}" }
    },
    "postgres": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-postgres"],
      "env": { "DATABASE_URL": "${DATABASE_URL}" }
    }
  }
}
```

### 9.2 .env 模板

```bash
# .env.example（不包含真实值，提交到仓库）
GITHUB_TOKEN=ghp_xxx
DATABASE_URL=postgresql://readonly:xxx@host:5432/db
```

### 9.3 Python Server 最小骨架

```python
from mcp.server.fastmcp import FastMCP

mcp = FastMCP("my-server")

@mcp.tool()
def hello(name: str) -> str:
    """向指定名字问好"""
    return f"Hello, {name}!"

if __name__ == "__main__":
    import asyncio
    asyncio.run(mcp.run_stdio_async())
```

---

## 10. 持续跟进建议

MCP 协议仍在快速演进，建议：

- **订阅**规范仓库 Release
- **关注** protocolVersion 发布日期（示例：`2025-03-26`）
- **留意** SDK CHANGELOG 里的破坏性变更
- **Star** `modelcontextprotocol/servers` 关注新官方实现
- **定期**（季度）复盘挂载的 Server 是否还需要、有没有更好的替代

---

## 11. 小结

- **Inspector** 是调试第一生产力工具
- **官方 SDK** 选语言门槛都不高，建议 Python 或 TS 起步
- **servers 仓库**既是能力来源也是学习范本
- MCP 只是 Claude Code 生态的一部分，与 Skill / Hook / Subagent / Permissions **组合使用**威力最大
- 生态演进快，**定期跟进 Changelog** 避免被破坏性变更打到
