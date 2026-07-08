# MCP Client 开发指南

> 定位：在自研应用中集成 MCP——连接 Server、发现能力、路由调用

---

## 1. 什么时候需要自己写 Client

大多数用户直接用 Claude Code / Claude Desktop / Cursor 等现成 Host，不需要写 Client。以下情况需要自研：

- 构建自己的 AI 应用（自研 Host）
- 把 MCP 能力集成进内部平台（如 OA、客服系统）
- 做 MCP 相关的测试工具、代理层、网关
- 对 LLM 调用与 Tool 执行需要自定义编排

---

## 2. Client 的职责

```
┌────────────────────────────────────────────────┐
│              Host 应用（你写的）                │
│                                                │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐  │
│  │  LLM     │   │  Session │   │   UI     │  │
│  │  调用    │◄──┤  编排    │──►│          │  │
│  └──────────┘   └────┬─────┘   └──────────┘  │
│                      │                         │
│         ┌────────────┴───────────┐             │
│         ▼                        ▼             │
│    ┌─────────┐              ┌─────────┐        │
│    │ Client A│              │ Client B│        │
│    └────┬────┘              └────┬────┘        │
└─────────┼─────────────────────────┼────────────┘
          │                         │
          ▼                         ▼
     Server A                   Server B
```

Client 单体职责：

1. **连接管理** — 启动/连接 Server、握手、断线重连
2. **能力发现** — `tools/list`、`resources/list`、`prompts/list`
3. **消息路由** — 把 LLM 产生的 Tool Call 路由到对应 Server
4. **反向请求** — 处理 Server 的 sampling 与 roots 请求
5. **生命周期** — Host 退出时优雅关闭连接

---

## 3. 使用官方 SDK

### 3.1 Python Client

```python
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

async def run_client():
    params = StdioServerParameters(
        command="python",
        args=["-m", "my_mcp_server"],
        env={"LOG_LEVEL": "INFO"},
    )

    async with stdio_client(params) as (read, write):
        async with ClientSession(read, write) as session:
            # 1. 初始化握手
            await session.initialize()

            # 2. 能力发现
            tools = await session.list_tools()
            print([t.name for t in tools.tools])

            # 3. 调用 Tool
            result = await session.call_tool(
                "get_weather",
                arguments={"city": "Beijing"},
            )
            for c in result.content:
                if c.type == "text":
                    print(c.text)

            # 4. 读取 Resource
            contents = await session.read_resource("config://app/settings")

            # 5. 获取 Prompt
            prompt = await session.get_prompt("review_pr", arguments={"pr_url": "..."})
```

### 3.2 TypeScript Client

```typescript
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";

const transport = new StdioClientTransport({
  command: "node",
  args: ["/abs/path/to/server/dist/index.js"],
  env: { LOG_LEVEL: "info" },
});

const client = new Client(
  { name: "my-host", version: "1.0.0" },
  { capabilities: { sampling: {}, roots: { listChanged: true } } }
);

await client.connect(transport);

const { tools } = await client.listTools();
const result = await client.callTool({
  name: "get_weather",
  arguments: { city: "Beijing" },
});
```

---

## 4. 连接多个 Server

一个 Host 通常要同时接多个 Server，推荐用 **Registry 模式**统一管理：

```python
class McpRegistry:
    def __init__(self):
        self.sessions: dict[str, ClientSession] = {}
        self.tools_index: dict[str, str] = {}  # tool_name -> server_name

    async def connect(self, name: str, params: StdioServerParameters):
        # 建连并缓存 session
        read, write = await stdio_client(params).__aenter__()
        session = ClientSession(read, write)
        await session.__aenter__()
        await session.initialize()

        # 建索引
        tools = await session.list_tools()
        for t in tools.tools:
            # 命名空间化避免冲突：server__tool
            full_name = f"{name}__{t.name}"
            self.tools_index[full_name] = name

        self.sessions[name] = session

    async def call(self, full_name: str, args: dict):
        server = self.tools_index[full_name]
        tool = full_name.split("__", 1)[1]
        return await self.sessions[server].call_tool(tool, arguments=args)
```

**命名空间**：不同 Server 的 Tool 可能重名，Host 需要加前缀（如 Claude Code 的 `mcp__<server>__<tool>`）。

---

## 5. 把 MCP Tools 喂给 LLM

### 5.1 转换为 Function Calling 格式

以 Anthropic Messages API 为例：

```python
def mcp_tools_to_anthropic(tools):
    return [
        {
            "name": t.name,
            "description": t.description,
            "input_schema": t.inputSchema,
        }
        for t in tools
    ]

# 调 LLM
from anthropic import Anthropic
client = Anthropic()

tools = await session.list_tools()
response = client.messages.create(
    model="claude-opus-4-7",
    max_tokens=4096,
    tools=mcp_tools_to_anthropic(tools.tools),
    messages=[{"role": "user", "content": "北京天气怎么样？"}],
)
```

### 5.2 工具调用循环

```python
async def agent_loop(user_msg: str):
    messages = [{"role": "user", "content": user_msg}]
    tools = (await session.list_tools()).tools
    tool_defs = mcp_tools_to_anthropic(tools)

    while True:
        resp = client.messages.create(
            model="claude-opus-4-7",
            max_tokens=4096,
            tools=tool_defs,
            messages=messages,
        )

        # 累加 assistant 回复
        messages.append({"role": "assistant", "content": resp.content})

        # 找 tool_use 块
        tool_uses = [b for b in resp.content if b.type == "tool_use"]
        if not tool_uses:
            return resp  # 没有 tool call → 结束

        # 并发执行所有 tool call
        tool_results = []
        for tu in tool_uses:
            mcp_result = await session.call_tool(tu.name, arguments=tu.input)
            text = "\n".join(c.text for c in mcp_result.content if c.type == "text")
            tool_results.append({
                "type": "tool_result",
                "tool_use_id": tu.id,
                "content": text,
                "is_error": mcp_result.isError or False,
            })

        messages.append({"role": "user", "content": tool_results})
```

---

## 6. 处理 Server 反向请求

### 6.1 Sampling（Server 请求 LLM）

Server 可能调用 `sampling/createMessage` 要求 Client 代为调 LLM。Client 需要注册处理器：

```python
async def handle_sampling(params):
    # 用你自己的 LLM 客户端完成采样
    resp = llm_client.messages.create(
        model=params.preferences.get("model", "claude-haiku-4-5"),
        max_tokens=params.max_tokens,
        messages=params.messages,
    )
    return CreateMessageResult(
        role="assistant",
        content=TextContent(type="text", text=resp.content[0].text),
        model=resp.model,
        stopReason="end_turn",
    )

session = ClientSession(read, write, sampling_callback=handle_sampling)
```

**注意**：sampling 会让 Server 消耗你的 LLM 额度，必须 Host 显式允许（通过 capabilities）并对单次调用做人工审批或策略限制。

### 6.2 Roots（暴露文件系统根）

```python
async def handle_list_roots():
    return ListRootsResult(
        roots=[
            Root(uri="file:///home/user/projects", name="projects"),
        ]
    )

session = ClientSession(read, write, list_roots_callback=handle_list_roots)
```

---

## 7. 订阅资源变更

```python
# 订阅
await session.subscribe_resource("log://service/current")

# 处理变更通知
async def on_resource_updated(uri: str):
    new = await session.read_resource(uri)
    ui.refresh(uri, new)

session.register_notification_handler(
    "notifications/resources/updated",
    on_resource_updated,
)
```

---

## 8. 错误与超时

### 8.1 调用级超时

```python
import asyncio

try:
    result = await asyncio.wait_for(
        session.call_tool("slow_op", arguments={}),
        timeout=30,
    )
except asyncio.TimeoutError:
    # 发送取消通知
    await session.send_notification("notifications/cancelled", {
        "requestId": current_request_id,
        "reason": "Host timeout",
    })
```

### 8.2 业务错误 vs 协议错误

```python
result = await session.call_tool("run_query", arguments={"sql": "..."})
if result.isError:
    # 业务失败 —— LLM 应该看到，允许重试
    feed_back_to_llm(result)
else:
    # 成功结果
    use_result(result)

# 协议错误会作为异常抛出
try:
    result = await session.call_tool("nonexistent", arguments={})
except McpError as e:
    # 例如 Method not found，说明 Host 或 LLM 用错了 Tool
    logger.error(f"MCP protocol error: {e}")
```

---

## 9. 权限与审计

### 9.1 调用白名单

```python
class PolicyClient:
    def __init__(self, session, allowed: set[str]):
        self.session = session
        self.allowed = allowed

    async def call_tool(self, name, arguments):
        if name not in self.allowed:
            raise PermissionError(f"Tool {name} not allowed by policy")
        return await self.session.call_tool(name, arguments)
```

### 9.2 调用审计

```python
async def audited_call(session, name, arguments):
    start = time.time()
    audit_log.info({"event": "tool_call_start", "name": name, "args": arguments})
    try:
        result = await session.call_tool(name, arguments)
        audit_log.info({
            "event": "tool_call_end",
            "name": name,
            "duration_ms": (time.time() - start) * 1000,
            "is_error": result.isError,
        })
        return result
    except Exception as e:
        audit_log.exception({"event": "tool_call_failed", "name": name, "error": str(e)})
        raise
```

### 9.3 危险操作人工审批

```python
DANGEROUS_TOOLS = {"delete_file", "drop_table", "send_email"}

async def maybe_ask(name, arguments):
    if name in DANGEROUS_TOOLS:
        if not await ui.confirm(f"执行 {name}({arguments})？"):
            return {"isError": True, "content": [{"type": "text", "text": "用户拒绝"}]}
    return await session.call_tool(name, arguments)
```

---

## 10. 连接健康管理

### 10.1 心跳

```python
async def keep_alive(session):
    while True:
        await asyncio.sleep(30)
        try:
            await asyncio.wait_for(session.send_ping(), timeout=5)
        except (asyncio.TimeoutError, Exception):
            await reconnect()
```

### 10.2 断线重连

```python
async def ensure_session(name: str):
    if not self.sessions.get(name) or self.sessions[name].closed:
        logger.warning(f"Reconnecting MCP server {name}")
        await self.connect(name, self.params[name])
```

---

## 11. 多 Server 冲突处理

| 冲突 | 处理 |
|------|------|
| **Tool 重名** | 加命名空间前缀（`server__tool`） |
| **Resource URI scheme 冲突** | 同一 scheme 只能一个 Server 提供 |
| **Prompt 重名** | 加命名空间，UI 展示按 Server 分组 |
| **能力差异** | 记录每个 Server 的 capabilities，按需降级 |

---

## 12. 小结

- SDK 封装了大部分协议细节，业务重点是**会话编排**与**错误处理**
- 多 Server 场景必须加**命名空间**
- **Sampling** 反向调用要格外小心（费用 / 安全）
- **危险 Tool 人工审批**是 Host 的底线责任
- 独立审计日志 + 调用级超时 = 生产级 Client 的标配
