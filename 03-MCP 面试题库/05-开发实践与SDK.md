# 05 - 开发实践与 SDK

> 高频题 ★★★ | 进阶题 ★★☆ | 基础题 ★☆☆

---

## Q1. MCP 支持哪些编程语言？各自的 SDK 成熟度如何？★★★

**答：**

| 语言 | SDK | 成熟度 | 维护方 | 特点 |
|------|-----|--------|--------|------|
| **Python** | `mcp` (FastMCP) | ★★★★★ | Anthropic 官方 | 装饰器风格，AI/数据领域首选 |
| **TypeScript/Node.js** | `@modelcontextprotocol/sdk` | ★★★★★ | Anthropic 官方 | 最先发布的 SDK，Web 生态首选 |
| **Java** | `spring-ai-starter-mcp-server` | ★★★★☆ | Spring AI 社区 | 企业集成首选，支持 WebFlux/WebMVC |
| **Go** | `mcp-go` (mark3labs) | ★★★☆☆ | 社区 | 高性能，云原生首选 |
| **Rust** | `rmcp` | ★★★☆☆ | 社区 | 极致性能，适合嵌入式/延迟敏感 |
| **C#/.NET** | `ModelContextProtocol` | ★★★☆☆ | 社区（微软参与） | Windows/Azure 生态首选 |
| **Kotlin** | Spring AI / 原生 | ★★★☆☆ | 社区 | JVM 生态 |
| **Swift** | 官方 SDK | ★★☆☆☆ | 社区 | Apple 生态 |

**面试话术**：默认推荐 Python 或 Node.js（官方 SDK 最成熟），但最终应根据团队技能栈选择。MCP 瓶颈通常在外部服务而非语言本身。

---

## Q2. 请用 Python（FastMCP）写一个最小的 MCP Server 示例。★★★

**答：**

```python
from mcp.server.fastmcp import FastMCP
from typing import Annotated
from pydantic import Field

# 创建 Server
mcp = FastMCP("my-first-server", version="1.0.0")

# 注册 Tool——装饰器风格
@mcp.tool()
def to_uppercase(
    text: Annotated[str, Field(description="要转换的文本")]
) -> str:
    """将文本转换为大写。

    当用户需要将文本转为大写时使用。无副作用。
    """
    return text.upper()

# 注册 Resource
@mcp.resource("config://app")
def get_config() -> str:
    """获取应用配置"""
    return '{"version": "1.0.0", "debug": false}'

# 启动（Stdio 模式）
if __name__ == "__main__":
    mcp.run(transport="stdio")
```

**关键点：**
- `FastMCP` 提供装饰器风格的简洁 API
- `Annotated[str, Field(description="...")]` 用于参数描述（面向 LLM）
- `mcp.run()` 自动处理 JSON-RPC 和传输层

---

## Q3. 请用 Node.js / TypeScript 写一个最小的 MCP Server 示例。★★★

**答：**

```typescript
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";

const server = new McpServer({
  name: "my-first-server",
  version: "1.0.0",
});

server.registerTool(
  "to_uppercase",
  {
    title: "转换为大写",
    description: "将文本转换为大写。当用户需要将文本转为大写时使用。无副作用。",
    inputSchema: {
      text: z.string().describe("要转换的文本"),
    },
  },
  async ({ text }) => {
    const result = text.toUpperCase();
    return {
      content: [{ type: "text", text: result }],
    };
  },
);

const transport = new StdioServerTransport();
await server.connect(transport);
```

**关键点：**
- 使用 `zod` 定义 inputSchema（替代手写 JSON Schema）
- `registerTool` 注册工具，`connect` 启动
- `z.string().describe("...")` 生成面向 LLM 的参数描述

---

## Q4. 不同语言中，Tool 注册的方式有何不同？★★★

**答：**

| 语言 | 注册方式 | 示例 |
|------|---------|------|
| **Python** | `@mcp.tool()` 装饰器 | `@mcp.tool()\ndef search(...):` |
| **Node.js** | `server.registerTool(name, schema, handler)` | 程序化注册 + Zod schema |
| **Java** | `@Tool` 注解 + Bean 扫描 | `@Tool(description="...") public String search(...)` |
| **Go** | `server.AddTool(mcp.NewTool(...), handler)` | 程序化注册 + 选项模式 |
| **C#** | `[McpServerTool]` 方法注解 | `[McpServerTool, Description("...")] public static string Search(...)` |
| **Rust** | `#[tool]` 宏 | `#[tool(name = "...", description = "...")] async fn search(...)` |

**共同模式**：无论哪种语言，都需要提供 name、description（面向 LLM）和 inputSchema。

---

## Q5. 如何让一个 MCP Server 同时支持 Stdio 和 HTTP 两种传输？★★★

**答：** 这是实战项目中的通用模式——通过命令行参数切换传输。

**Python 实现：**
```python
import argparse
from mcp.server.fastmcp import FastMCP

mcp = FastMCP("dual-transport-server")

@mcp.tool()
def hello(name: str) -> str:
    """打招呼"""
    return f"Hello, {name}!"

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--transport", choices=["stdio", "http"], default="stdio")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8000)
    args = parser.parse_args()

    if args.transport == "stdio":
        mcp.run(transport="stdio")
    else:
        mcp.run(transport="streamable-http", host=args.host, port=args.port)
```

**Go 实现：**
```go
if transport == "stdio" {
    server.ServeStdio(s)
} else {
    httpServer := server.NewStreamableHTTPServer(s)
    http.ListenAndServe(addr, httpServer)
}
```

**Node.js 实现：**
```javascript
if (transport === "stdio") {
    const t = new StdioServerTransport();
    await server.connect(t);
} else {
    const t = new StreamableHTTPServerTransport({ sessionIdGenerator: crypto.randomUUID });
    http.createServer((req, res) => {
        if (req.url === "/mcp") t.handleRequest(req, res);
    }).listen(port, host);
}
```

**面试亮点**：提一下"双传输"让同一份代码既可在本地 IDE 中零配置使用，也可部署到服务器支持团队共享。

---

## Q6. MCP Server 的业务逻辑和 MCP 协议层应该怎么组织？★★★

**答：** 遵循 **"薄 MCP 包裹层"模式**（Thin MCP Wrapper）：

```
项目结构：
├── converter.py        ← 纯业务逻辑（零 MCP 依赖）
├── cli.py              ← CLI 入口（argparse，零 MCP 依赖）
└── mcp_entry.py        ← MCP 包裹层（导入 mcp 库，薄薄一层）
```

**converter.py（纯逻辑，可独立测试）：**
```python
def to_uppercase(text: str) -> str:
    return text.upper()
```

**cli.py（CLI 入口）：**
```python
import argparse
from converter import to_uppercase

parser = argparse.ArgumentParser()
parser.add_argument("text")
args = parser.parse_args()
print(to_uppercase(args.text))
```

**mcp_entry.py（MCP 包裹层）：**
```python
from mcp.server.fastmcp import FastMCP
from converter import to_uppercase

mcp = FastMCP("case-converter")

@mcp.tool()
def convert_upper(text: str) -> str:
    """将文本转换为大写"""
    return to_uppercase(text)  # ← 仅转发
```

**好处：**
1. 业务逻辑可独立单元测试（不需要 MCP 环境）
2. CLI 用户不需要安装 MCP SDK
3. 未来切换到其他协议或框架时，只需改包裹层
4. 团队成员即使不懂 MCP 也能维护业务逻辑

---

## Q7. 如何使用 MCP Inspector 调试 Server？★★★

**答：**

**调试 Stdio Server：**
```bash
npx @modelcontextprotocol/inspector python -m my_server
npx @modelcontextprotocol/inspector node dist/server.js
npx @modelcontextprotocol/inspector go run .
```

**调试 HTTP Server：**
```bash
# 先启动 HTTP Server
python -m my_server --transport http --port 8000

# 在 Inspector 中输入 URL
# 然后在浏览器中打开 Inspector，输入 http://localhost:8000/mcp
```

**Inspector 功能：**
- **Tools 面板**：查看所有已注册 Tool 的名称、描述和 inputSchema
- **手动测试**：填写参数并手动调用 Tool，查看返回结果
- **Resources 面板**：浏览资源列表、读取资源内容、测试订阅
- **Prompts 面板**：预览 Prompt 模板渲染结果
- **消息流面板**：查看原始 JSON-RPC 消息（Request/Response/Notification）
- **连接日志**：查看连接状态、能力协商过程

**面试话术**：Inspector 相当于 MCP 世界的 Postman——在将 Server 挂载到 AI 工具之前，先用它验证协议交互是否正确。

---

## Q8. 如何测试 MCP Server（单元测试）？★★☆

**答：**

**Python —— 使用 InMemoryTransport：**
```python
import pytest
from mcp import ClientSession
from mcp.client.stdio import stdio_client
from mcp.server.fastmcp import FastMCP

@pytest.fixture
async def mcp_session():
    mcp = FastMCP("test-server")
    @mcp.tool()
    def hello(name: str) -> str:
        return f"Hello, {name}!"
    
    # 使用内存传输（不需要启动子进程）
    from mcp.shared.memory import create_connected_server_and_client_session
    async with create_connected_server_and_client_session(
        mcp._mcp_server
    ) as (server_session, client_session):
        yield client_session

async def test_hello(mcp_session):
    result = await mcp_session.call_tool("hello", {"name": "World"})
    assert "Hello, World!" in result.content[0].text
```

**业务逻辑独立测试（更推荐）：**
```python
# 直接测试 converter.py，不涉及 MCP
from converter import to_uppercase

def test_to_uppercase():
    assert to_uppercase("hello") == "HELLO"
    assert to_uppercase("") == ""
```

**测试策略：**
- 业务逻辑：常规单元测试（pytest/jest等）
- MCP 协议层：用 MCP Inspector 手动验证 + InMemoryTransport 集成测试
- 协议兼容性：确认 `initialize` 握手成功、`tools/list` 返回正确、参数校验生效

---

## Q9. MCP Client 开发的核心职责是什么？★★★

**答：**

1. **连接管理**：启动 Server 子进程或建立 HTTP 连接、握手、心跳、重连
2. **能力发现**：调用 `tools/list`、`resources/list`、`prompts/list`
3. **消息路由**：将 LLM 的 Tool Call 路由到正确的 Server
4. **反向请求处理**：处理 `sampling/createMessage` 和 `roots/list` 回调
5. **生命周期管理**：超时控制、请求取消、优雅关闭
6. **权限审计**：工具白名单/黑名单、审计日志

**Python Client 最小示例：**
```python
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

server_params = StdioServerParameters(
    command="python", args=["-m", "my_server"]
)

async with stdio_client(server_params) as (read, write):
    async with ClientSession(read, write) as session:
        await session.initialize()
        tools = await session.list_tools()
        result = await session.call_tool("hello", {"name": "World"})
        print(result.content[0].text)
```

---

## Q10. 如何将 MCP Tools 提供给 LLM 使用（Anthropic API）？★★★

**答：**

```python
import anthropic
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

async def main():
    server_params = StdioServerParameters(command="python", args=["-m", "my_server"])
    
    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            
            # 1. 获取 MCP Tool 列表
            mcp_tools = await session.list_tools()
            
            # 2. 转换为 Anthropic Tool 格式
            anthropic_tools = [
                {
                    "name": tool.name,
                    "description": tool.description,
                    "input_schema": tool.inputSchema,
                }
                for tool in mcp_tools.tools
            ]
            
            # 3. 发送给 Claude
            client = anthropic.Anthropic()
            response = client.messages.create(
                model="claude-sonnet-4-6",
                max_tokens=1024,
                tools=anthropic_tools,
                messages=[{"role": "user", "content": "帮我转大写：hello world"}],
            )
            
            # 4. 如果 LLM 决定调用 Tool
            if response.stop_reason == "tool_use":
                for block in response.content:
                    if block.type == "tool_use":
                        # 5. 路由到 MCP Server 执行
                        result = await session.call_tool(block.name, block.input)
                        
                        # 6. 将结果注入 LLM 上下文继续对话
                        response = client.messages.create(
                            model="claude-sonnet-4-6",
                            max_tokens=1024,
                            tools=anthropic_tools,
                            messages=[
                                {"role": "user", "content": "帮我转大写：hello world"},
                                {"role": "assistant", "content": response.content},
                                {
                                    "role": "user",
                                    "content": [{
                                        "type": "tool_result",
                                        "tool_use_id": block.id,
                                        "content": result.content[0].text,
                                    }],
                                },
                            ],
                        )
```

**核心流程**：MCP Tool → Anthropic Tool 格式转换 → LLM 决策 → 路由回 MCP 执行 → 结果注入 LLM。

---

## Q11. 如何管理多个 MCP Server 的连接（多 Server Client）？★★★

**答：**

**注册中心模式（Registry Pattern）：**
```python
class McpRegistry:
    def __init__(self):
        self._sessions: dict[str, ClientSession] = {}
        self._tool_index: dict[str, str] = {}  # tool_name → server_name
    
    async def register(self, name: str, session: ClientSession):
        self._sessions[name] = session
        tools = await session.list_tools()
        
        # 构建 Tool → Server 索引，添加命名空间前缀
        for tool in tools.tools:
            full_name = f"mcp__{name}__{tool.name}"
            self._tool_index[full_name] = name
    
    async def call_tool(self, tool_name: str, arguments: dict):
        server = self._tool_index.get(tool_name)
        if not server:
            raise ValueError(f"Unknown tool: {tool_name}")
        
        # 去除命名空间前缀，获取原始 Tool 名
        original_name = tool_name.split("__", 2)[-1]
        return await self._sessions[server].call_tool(original_name, arguments)
    
    async def list_all_tools(self) -> list[dict]:
        """聚合所有 Server 的工具列表"""
        all_tools = []
        for name, session in self._sessions.items():
            tools = await session.list_tools()
            for tool in tools.tools:
                all_tools.append({
                    "name": f"mcp__{name}__{tool.name}",
                    "description": tool.description,
                    "inputSchema": tool.inputSchema,
                    "_server": name,
                })
        return all_tools
```

**关键设计点：**
- 命名空间前缀避免冲突
- Tool → Server 索引用于快速路由
- 聚合 `list_all_tools()` 供 LLM 使用

---

## Q12. MCP Server 开发中，如何处理 Tool 的参数校验？★★☆

**答：**

**SDK 层面的校验（推荐）：**
```python
# Python — Pydantic 自动校验
@mcp.tool()
def search(
    query: Annotated[str, Field(description="搜索关键词", min_length=1, max_length=200)],
    limit: Annotated[int, Field(description="返回数量", ge=1, le=100)] = 10,
    category: Annotated[Literal["bug", "feature", "docs"], Field(description="分类")] | None = None,
) -> str: ...
```

```typescript
// Node.js — Zod 自动校验
server.registerTool("search", {
  inputSchema: {
    query: z.string().min(1).max(200).describe("搜索关键词"),
    limit: z.number().int().min(1).max(100).default(10).describe("返回数量"),
    category: z.enum(["bug", "feature", "docs"]).optional().describe("分类"),
  },
}, handler);
```

**校验失败时的处理：**
- SDK 自动返回 JSON-RPC error (-32602 Invalid params)
- LLM 看到错误后可调整参数重试
- 不在代码中手写校验逻辑（利用 SDK/框架能力）

---

## Q13. 如何在 MCP Server 中实现分页？★★☆

**答：**

**Tool 设计：**
```python
@mcp.tool()
def search_issues(
    query: str,
    limit: Annotated[int, Field(ge=1, le=100)] = 20,
    cursor: str | None = None,
) -> str:
    """搜索 Issues，支持分页"""
    if cursor:
        offset = int(base64_decode(cursor))
    else:
        offset = 0
    
    items, total = backend.search(query, limit=limit, offset=offset)
    
    next_cursor = base64_encode(offset + limit) if offset + limit < total else None
    
    return json.dumps({
        "items": items,
        "total": total,
        "nextCursor": next_cursor,
        "hasMore": next_cursor is not None,
    })
```

**LLM 的分页行为：**
- LLM 看到 `hasMore: true` 和 `nextCursor`，会自行决定是否继续获取
- 不需要为分页写专门的逻辑——LLM 会自然地"如果结果不完整，用 cursor 继续请求"

---

## Q14. 不同语言的 MCP Server 中，日志如何处理？★★☆

**答：**

| 语言 | 日志输出 | 配置方式 |
|------|---------|---------|
| Python | `logging` → stderr | `logging.basicConfig(stream=sys.stderr, level=...)` |
| Node.js | `console.error()` 或 process.stderr | `DEBUG=mcp:*` 环境变量 |
| Go | `log.SetOutput(os.Stderr)` | `log` 标准库 |
| Java | Logback/SLF4J → stderr | `logback.xml` 配置 Console Appender |
| C# | `ILogger` → stderr | `LogToStandardErrorThreshold = LogLevel.Trace` |
| Rust | `tracing-subscriber` → stderr | `.with_writer(std::io::stderr)` |

**通用原则：**
- Stdio 模式：日志**必须**输出到 stderr，stdout 仅供 JSON-RPC 协议使用
- HTTP 模式：可以安全地同时使用 stdout 和 stderr
- 日志级别：默认 INFO，调试时切到 DEBUG

---

## Q15. 请解释 MCP Client 中如何处理反向请求（Sampling 和 Roots）。★★☆

**答：**

**Sampling（Server 请求 LLM）：**
```python
async with ClientSession(read, write, sampling_callback=my_sampling_handler) as session:
    ...

async def my_sampling_handler(context, params):
    """Server 请求 Client 调用 LLM"""
    # 安全检查：是否需要人工确认？
    if not user_approved(params):
        raise PermissionError("User denied sampling request")
    
    # 调用 LLM
    response = await anthropic_client.messages.create(
        model=params.model or "claude-sonnet-4-6",
        max_tokens=params.maxTokens,
        messages=params.messages,
    )
    return response
```

**Roots（Server 请求文件系统根目录）：**
```python
async with ClientSession(read, write, roots_callback=my_roots_handler) as session:
    ...

async def my_roots_handler(context):
    """返回当前项目可访问的文件系统根目录"""
    return [
        {"uri": "file:///home/user/project", "name": "项目根目录"},
        {"uri": "file:///home/user/project/data", "name": "数据目录"},
    ]
```

**安全注意事项：**
- Sampling 默认禁用，必须显式在 capabilities 中授权
- 生产环境应对 Sampling 加人工确认和配额限制

---

## Q16. MCP Client 中如何处理连接健康检查和重连？★★☆

**答：**

```python
import asyncio

class McpConnectionManager:
    def __init__(self, server_params, heartbeat_interval=30, max_retries=5):
        self.params = server_params
        self.heartbeat_interval = heartbeat_interval
        self.max_retries = max_retries
        self.session = None
    
    async def connect(self):
        retries = 0
        while retries < self.max_retries:
            try:
                self._read, self._write = await stdio_client(self.params).__aenter__()
                self.session = await ClientSession(self._read, self._write).__aenter__()
                await self.session.initialize()
                asyncio.create_task(self._heartbeat_loop())
                return
            except Exception:
                retries += 1
                backoff = min(2 ** retries, 30)
                await asyncio.sleep(backoff)
        raise ConnectionError("Failed to connect after max retries")
    
    async def _heartbeat_loop(self):
        while True:
            await asyncio.sleep(self.heartbeat_interval)
            try:
                await asyncio.wait_for(self.session.send_ping(), timeout=5)
            except Exception:
                # Ping 失败 → 触发重连
                await self.reconnect()
    
    async def reconnect(self):
        await self.disconnect()
        await self.connect()
```

---

## Q17. 如何处理 Tool 调用超时？★★☆

**答：**

```python
import asyncio

async def call_tool_with_timeout(session, tool_name, arguments, timeout=30):
    try:
        result = await asyncio.wait_for(
            session.call_tool(tool_name, arguments),
            timeout=timeout,
        )
        return result
    except asyncio.TimeoutError:
        # 发送取消通知（尽力而为）
        # Client 可通过 notifications/cancelled 告知 Server 中止
        return {
            "content": [{"type": "text", "text": f"工具 {tool_name} 执行超时（{timeout}秒）"}],
            "isError": True,
        }
```

**设计建议：**
- 设定合理的默认超时（查询类 30s，写入类 60s）
- 支持 Server 端通过 `notifications/progress` 告知进度
- 超时后返回 `isError: true`，让 LLM 决定重试策略

---

## Q18. 不同语言实现双传输模式时，有哪些语言特有的注意事项？★★☆

**答：**

| 语言 | 注意事项 |
|------|---------|
| **Python** | FastMCP 的 `run()` 内置了 stdio/http 切换支持，最简单 |
| **Node.js** | ES Module vs CommonJS 影响 import 方式；Node 内置 `http` 模块可实现 HTTP 传输 |
| **Go** | Stdio 使用 `os.Stdin`/`os.Stdout`；HTTP 需要显式 `http.ListenAndServe` |
| **Java** | Stdio 模式下需要禁用 Web 容器（`spring.main.web-application-type=none`）；WebFlux vs WebMVC 两种 HTTP 实现 |
| **C#** | `WithStdioServerTransport()` 简单；HTTP 需要完整的 `WebApplication` 构建 |
| **Rust** | `rmcp` 的 `transport-io` feature 用于 Stdio；Streamable HTTP 需要额外 feature |

---

## Q19. 什么是 MCP 的 "InMemoryTransport"？有什么用途？★★☆

**答：** InMemoryTransport 是一种不依赖真实 I/O 的传输实现，用于**测试**场景。

**Python 用法：**
```python
from mcp.shared.memory import create_connected_server_and_client_session

async with create_connected_server_and_client_session(
    my_server
) as (server_session, client_session):
    # client_session 就像连接到了一个真实的 MCP Server
    result = await client_session.call_tool("hello", {"name": "test"})
```

**Node.js 用法：**
```typescript
import { InMemoryTransport } from "@modelcontextprotocol/sdk/inMemory.js";

const [clientTransport, serverTransport] = InMemoryTransport.createLinkedPair();
await server.connect(serverTransport);
const client = new Client(...);
await client.connect(clientTransport);
```

**优势：**
- 无需启动子进程或 HTTP Server
- 消息在内存中直接传递，无序列化开销
- 适合 CI/CD 中快速运行集成测试

---

## Q20. MCP 协议版本更新后，Server 需要做哪些适配？★★☆

**答：**

1. **检查 SDK CHANGELOG**：关注 breaking change 和 deprecated API
2. **更新 protocolVersion**：在 `initialize` 响应中声明支持的版本
3. **适配新的能力声明格式**：capabilities 字段可能新增/调整
4. **检查方法签名变化**：如 `rmcp` v0.x → v1.x 的 `Parameters` 路径变化
5. **回归测试**：用 MCP Inspector 验证协议交互，用 InMemoryTransport 跑集成测试
6. **逐步淘汰旧 API**：如果使用了 deprecated 的方法，在 CHANGELOG 中记录并计划迁移

**版本更新策略**：不要立即升级到最新版本，等待 1-2 个小版本稳定后再迁移。

---

## Q21. 如何设计 MCP Server 的 Tool 命名，让 LLM 更容易理解和选用？★★★

**答：**

**命名原则：**
```
动词_名词（verb_noun）

✅ search_issues        — 清晰：搜索 Issues
✅ create_issue         — 清晰：创建 Issue
✅ get_user_profile     — 清晰：获取用户信息
✅ list_repositories    — 清晰：列出仓库

❌ do_github            — 模糊：做什么？
❌ handle_issues        — 模糊：处理是什么意思？
❌ issue_op             — 模糊：什么操作？
```

**命名前缀约定：**
| 前缀 | 含义 | 示例 |
|------|------|------|
| `list_` | 列出多个（可搜索/筛选） | `list_issues` |
| `get_` | 获取单个 | `get_issue` |
| `create_` | 创建新资源 | `create_issue` |
| `update_` | 修改已有资源 | `update_issue` |
| `delete_` | 删除资源 | `delete_issue` |
| `search_` | 搜索/查询 | `search_code` |
| `run_` | 执行/运行 | `run_sql` |

---

## Q22. MCP Server 的 version 应该遵循什么规范？★★☆

**答：**

- 使用 **Semantic Versioning（SemVer）**：`MAJOR.MINOR.PATCH`
- MAJOR：不兼容的 API 变更
- MINOR：向后兼容的新功能
- PATCH：向后兼容的 Bug 修复
- 声明在 `serverInfo.version` 中

**Tool 级别的版本演进：**
- 废弃前先标记 deprecated（在 description 中注明）
- 新增参数时保持向后兼容（可选参数，有默认值）
- 破坏性变更时创建 `_v2` 后缀版本，同时保留旧 Tool：
  ```
  search_issues (旧版本，标记 deprecated)
  search_issues_v2 (新版本，新增参数)
  ```
- 给用户过渡期（如 3 个月），然后移除旧版本

---

## Q23. MCP Server 开发完成后，上线前需要做哪些检查？★★★

**答：**

**功能与规范检查：**
- [ ] 命名符合规范（Server/Tool/Resource/Prompt）
- [ ] 所有 Tool 的描述都是面向 LLM 的（回答 4 个问题）
- [ ] inputSchema 有严格校验（`required`、`enum`、`additionalProperties: false`）
- [ ] 错误消息对 LLM 友好（解释原因 + 建议行动）
- [ ] 有 pagination 支持（如果可能返回大量数据）
- [ ] 破坏性操作有确认参数

**质量检查：**
- [ ] 通过 MCP Inspector 验证（Tool 列表、调用、Resource 读取）
- [ ] 业务逻辑有单元测试
- [ ] 在目标 AI 工具（Claude Code/Cursor）中实际使用过
- [ ] 双传输模式都经过测试（如果是双传输 Server）

**文档与安全：**
- [ ] README 有功能概述、安装/使用命令、环境变量说明
- [ ] 无硬编码密钥（全部通过环境变量）
- [ ] 日志不泄露敏感信息
- [ ] 依赖无已知高危漏洞

---

## Q24. 如何在 MCP Server 中实现 Tool 调用次数统计和性能监控？★★☆

**答：**

**内联计时中间件：**
```python
import time
import functools

def with_timing(func):
    @functools.wraps(func)
    async def wrapper(*args, **kwargs):
        start = time.monotonic()
        try:
            result = await func(*args, **kwargs)
            duration = time.monotonic() - start
            # 写入结构化日志到 stderr
            print(json.dumps({
                "event": "tool_call",
                "tool": func.__name__,
                "duration_ms": round(duration * 1000, 2),
                "status": "success",
            }), file=sys.stderr)
            return result
        except Exception as e:
            duration = time.monotonic() - start
            print(json.dumps({
                "event": "tool_call",
                "tool": func.__name__,
                "duration_ms": round(duration * 1000, 2),
                "status": "error",
                "error": str(e),
            }), file=sys.stderr)
            raise
    return wrapper
```

**Per-Tool 统计**：记录调用次数、成功/失败率、P50/P95/P99 延迟——用于性能分析和容量规划。

---

## Q25. 不同语言实现同一个 MCP Server 功能时，有哪些共同挑战？★★☆

**答：** 以实战项目中的 `mcp-case-converter`（6 种语言实现同一功能）为例：

**共同挑战：**
1. **Tool 注册 API 差异**：装饰器 vs 程序化注册 vs 注解 vs 宏——但本质相同
2. **Word Splitting 算法**：Python/C#/JS 用正则的 lookahead，Go/Rust 因正则引擎限制需手写状态机
3. **日志隔离**：所有语言都要确保日志到 stderr，stdout 仅用于协议
4. **双传输 CLI**：每种语言的命令行参数解析方式不同（argparse/parseArgs/clap/flag）
5. **SDK 成熟度差异**：Python/Node 的 HTTP 传输开箱即用，C#/Rust 需要额外工作

**共性部分（说明协议标准化价值）：**
- Tool 的定义结构（name/description/inputSchema）完全一致
- JSON-RPC 消息格式完全一致
- 与 AI 工具的交互方式完全一致
- 可以使用任何语言的 MCP Inspector 调试任何语言的 Server

---

## Q26. 实战项目中 Java 的 mcp-rag-search 是如何桥接外部 HTTP API 的？★★☆

**答：** 

这是一个经典的**代理/桥接模式**——MCP Server 包装外部 HTTP API：

```
AI Host → MCP Client → mcp-rag-search (Java, MCP Server)
                           ↓ HTTP (java.net.http.HttpClient)
                        rag-backend (外部 HTTP API)
```

**关键设计：**
- MCP Tools 是薄包裹层，实际逻辑委托给 `RagBackendClient`（纯 HTTP 客户端）
- 配置通过 `@ConfigurationProperties(prefix="rag")` → 环境变量注入
- 支持多知识库切换（`kbId` 参数）
- Python 编写的 smoke test 验证全链路

**面试话术**：这是一个典型的 MCP Server 使用模式——不是所有逻辑都在 Server 内部，MCP Server 经常作为"协议适配器"和"安全网关"，桥接现有的后端服务。

---

## Q27. 实战项目中的 Python idea-plugin-analyzer 为什么只支持 Stdio 传输？★★☆

**答：**

设计决策原因：
1. **本地 I/O 密集型**：需要解压插件文件、读取 class 文件、运行反编译器——不适合远程部署
2. **文件系统依赖**：Resource URI 引用本地文件路径，跨网络使用无意义
3. **安全考量**：反编译和文件分析是敏感操作，不应暴露为网络服务
4. **单用户场景**：每个开发者分析自己的插件，不需要多租户

**面试要点**：不是所有 MCP Server 都需要双传输。根据实际场景选择——这个选择本身就是架构能力。

---

## Q28. 如何在 MCP Client 中实现审计日志？★★☆

**答：**

```python
import json
import time

class AuditedMcpClient:
    def __init__(self, session: ClientSession, audit_log_path: str):
        self._session = session
        self._log_path = audit_log_path
    
    async def call_tool(self, name: str, arguments: dict):
        start = time.time()
        try:
            result = await self._session.call_tool(name, arguments)
            self._log(name, arguments, "success", time.time() - start)
            return result
        except Exception as e:
            self._log(name, arguments, "error", time.time() - start, str(e))
            raise
    
    def _log(self, tool, args, status, duration, error=None):
        entry = {
            "timestamp": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
            "tool_name": tool,
            "arguments": args,  # 生产环境需脱敏
            "status": status,
            "duration_ms": round(duration * 1000, 2),
            "error": error,
        }
        with open(self._log_path, "a") as f:
            f.write(json.dumps(entry) + "\n")
```

**审计日志应包含**：时间戳、Tool 名、参数（脱敏）、耗时、状态（成功/失败）、错误信息。

---

## 自检清单

- [ ] 能用 Python 和 Node.js 写出最小 MCP Server
- [ ] 理解"薄 MCP 包裹层"模式及其好处
- [ ] 知道至少 4 种语言的 Tool 注册方式
- [ ] 能实现双传输（Stdio + HTTP）CLI
- [ ] 会用 MCP Inspector 和 InMemoryTransport 测试
- [ ] 理解 Client 的职责（连接管理、消息路由、审计）
- [ ] 知道如何将 MCP Tool 转换给 Anthropic API 使用
- [ ] 了解多 Server 注册中心模式
- [ ] 能处理超时、重连、心跳等运维问题
- [ ] 知道上线前的检查清单
