# MCP 协议与 Skill 集成指南

> 版本：1.0 | 定位：MCP 协议原理与实战

---

## 1. 什么是 MCP 协议

**MCP（Model Context Protocol）** 是由 Anthropic 提出的开放标准协议，用于规范 AI 模型与外部工具/数据源之间的通信方式。

### 1.1 核心理念

MCP 解决了 AI 模型"能力孤岛"问题：

```
传统方式（无 MCP）：
  Agent ─── 自定义 API ──→ 工具A
  Agent ─── 自定义 API ──→ 工具B
  Agent ─── 自定义 API ──→ 工具C
  （每次集成都需要定制开发，维护成本高）

MCP 方式：
  Agent ─── MCP 协议 ──→ MCP Server A（封装工具A）
  Agent ─── MCP 协议 ──→ MCP Server B（封装工具B）
  Agent ─── MCP 协议 ──→ MCP Server C（封装工具C）
  （标准化协议，一次实现，处处可用）
```

### 1.2 MCP 与 Agent Skill 的关系

| 维度 | Agent Skill | MCP Server |
|------|------------|------------|
| **本质** | 指令模板（Prompt） | 能力提供者（Tools/Resources） |
| **作用** | 告诉 Agent **如何做** | 给 Agent 提供**做事的工具** |
| **实现** | Markdown 文件 | 独立服务进程 |
| **互补关系** | Skill 调用 MCP 工具完成任务 | MCP 工具为 Skill 提供执行能力 |

**协同示例：**
```
用户：/analyze-db-performance

Skill 触发：
  → 执行指令："分析数据库性能问题，步骤：..."
  → 调用 MCP 工具：mcp__database__run_explain_query
  → 调用 MCP 工具：mcp__database__get_slow_queries
  → 整合结果，生成分析报告
```

---

## 2. MCP 核心概念

### 2.1 三类能力原语

MCP Server 可以向 AI 模型提供三类能力：

#### Tools（工具）
Agent **主动调用**的函数，用于执行操作或获取数据。

```jsonc
// Tool 定义示例
{
  "name": "run_sql_query",
  "description": "执行 SQL 查询并返回结果",
  "inputSchema": {
    "type": "object",
    "properties": {
      "query": {
        "type": "string",
        "description": "要执行的 SQL 查询语句"
      },
      "database": {
        "type": "string",
        "description": "目标数据库名称",
        "default": "main"
      }
    },
    "required": ["query"]
  }
}
```

**特点：** Agent 根据需要主动决定是否调用；会产生副作用（写操作）

#### Resources（资源）
MCP Server **暴露的数据**，Agent 可按需读取，类似只读文件系统。

```
资源 URI 示例：
  file:///project/src/main.py     → 文件内容
  database://main/users           → 数据库表数据
  config://app/settings           → 配置信息
  git://repo/commits              → Git 提交记录
```

**特点：** 只读访问；由 Agent 或 Skill 主动请求读取

#### Prompts（提示词模板）
MCP Server 提供的**可复用 Prompt 模板**，支持参数化。

```jsonc
// Prompt 定义示例
{
  "name": "code_review_prompt",
  "description": "专业的代码审查提示词模板",
  "arguments": [
    {
      "name": "language",
      "description": "编程语言",
      "required": true
    },
    {
      "name": "focus",
      "description": "审查重点：security/performance/readability",
      "required": false
    }
  ]
}
```

**特点：** 比本地 Skill 更动态，可从服务端实时更新

---

## 3. MCP 传输层

MCP 支持两种传输方式：

### 3.1 Stdio 传输（本地进程）

```
AI 客户端 ←──── stdin/stdout ────→ MCP Server 进程
```

适合：本地工具集成、开发调试、需要访问本地文件系统

```jsonc
// Claude Code 配置示例（~/.claude.json）
{
  "mcpServers": {
    "filesystem": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-filesystem", "/home/user/projects"]
    },
    "my-db-server": {
      "command": "python",
      "args": ["/path/to/my_mcp_server.py"],
      "env": {
        "DB_HOST": "localhost",
        "DB_PASSWORD": "secret"
      }
    }
  }
}
```

### 3.2 HTTP/SSE 传输（远程服务）

```
AI 客户端 ←──── HTTP + Server-Sent Events ────→ MCP Server（远端）
```

适合：云服务集成、多客户端共享、生产环境部署

---

## 4. 开发第一个 MCP Server

### 4.1 Node.js 实现

```bash
npm install @modelcontextprotocol/sdk
```

```typescript
// weather-mcp-server.ts
import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";

const server = new Server(
  { name: "weather-server", version: "1.0.0" },
  { capabilities: { tools: {} } }
);

// 声明 Tools
server.setRequestHandler(ListToolsRequestSchema, async () => ({
  tools: [
    {
      name: "get_weather",
      description: "获取指定城市的当前天气信息",
      inputSchema: {
        type: "object",
        properties: {
          city: { type: "string", description: "城市名称" },
          unit: {
            type: "string",
            enum: ["celsius", "fahrenheit"],
            default: "celsius",
          },
        },
        required: ["city"],
      },
    },
  ],
}));

// 实现 Tool 逻辑
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  if (request.params.name === "get_weather") {
    const { city, unit = "celsius" } = request.params.arguments as {
      city: string;
      unit?: string;
    };

    // 实际业务逻辑
    const weatherData = await fetchWeather(city, unit);

    return {
      content: [
        {
          type: "text",
          text: JSON.stringify(weatherData, null, 2),
        },
      ],
    };
  }
  throw new Error(`Unknown tool: ${request.params.name}`);
});

// 启动服务
const transport = new StdioServerTransport();
await server.connect(transport);
```

### 4.2 Python 实现

```bash
pip install mcp
```

```python
# weather_mcp_server.py
from mcp.server import Server
from mcp.server.stdio import stdio_server
from mcp.types import Tool, TextContent, CallToolResult
import asyncio
import json

app = Server("weather-server")

@app.list_tools()
async def list_tools() -> list[Tool]:
    return [
        Tool(
            name="get_weather",
            description="获取指定城市的当前天气信息",
            inputSchema={
                "type": "object",
                "properties": {
                    "city": {"type": "string", "description": "城市名称"},
                    "unit": {
                        "type": "string",
                        "enum": ["celsius", "fahrenheit"],
                        "default": "celsius"
                    }
                },
                "required": ["city"]
            }
        )
    ]

@app.call_tool()
async def call_tool(name: str, arguments: dict) -> CallToolResult:
    if name == "get_weather":
        city = arguments["city"]
        unit = arguments.get("unit", "celsius")

        # 实际业务逻辑
        weather_data = await fetch_weather(city, unit)

        return CallToolResult(
            content=[TextContent(type="text", text=json.dumps(weather_data, ensure_ascii=False))]
        )
    raise ValueError(f"Unknown tool: {name}")

async def main():
    async with stdio_server() as (read_stream, write_stream):
        await app.run(read_stream, write_stream, app.create_initialization_options())

if __name__ == "__main__":
    asyncio.run(main())
```

---

## 5. MCP Server 最佳实践

### 5.1 Tool 设计原则

**原则一：描述精准**
```jsonc
// ❌ 描述模糊
{"description": "查询数据"}

// ✅ 描述明确（包含：做什么、输入类型、输出内容、使用限制）
{"description": "查询指定用户的订单列表，返回最近30天内的订单，按创建时间降序排列。最多返回100条记录。"}
```

**原则二：参数类型严格**
```jsonc
{
  "inputSchema": {
    "type": "object",
    "properties": {
      "userId": {
        "type": "integer",        // 明确类型
        "minimum": 1,             // 添加约束
        "description": "用户ID（正整数）"
      },
      "status": {
        "type": "string",
        "enum": ["pending", "completed", "cancelled"],  // 枚举限制
        "description": "订单状态过滤"
      }
    },
    "required": ["userId"],       // 明确必填项
    "additionalProperties": false // 拒绝额外参数
  }
}
```

**原则三：错误响应规范**
```python
@app.call_tool()
async def call_tool(name: str, arguments: dict) -> CallToolResult:
    try:
        result = await business_logic(arguments)
        return CallToolResult(
            content=[TextContent(type="text", text=json.dumps(result))]
        )
    except ValueError as e:
        # 参数错误 - 返回错误信息（让 Agent 可以重试）
        return CallToolResult(
            content=[TextContent(type="text", text=f"参数错误：{str(e)}")],
            isError=True
        )
    except Exception as e:
        # 系统错误 - 返回通用错误（不暴露内部细节）
        logger.error(f"Tool execution failed: {e}", exc_info=True)
        return CallToolResult(
            content=[TextContent(type="text", text="服务暂时不可用，请稍后重试")],
            isError=True
        )
```

### 5.2 安全边界

**危险操作需要确认**
```python
@app.call_tool()
async def call_tool(name: str, arguments: dict) -> CallToolResult:
    if name == "delete_records":
        # 危险操作：返回确认提示而非直接执行
        count = await count_records(arguments["filter"])
        return CallToolResult(
            content=[TextContent(
                type="text",
                text=f"即将删除 {count} 条记录，此操作不可逆。请确认后重新调用并传入 confirmed=true 参数。"
            )]
        )
```

**避免提示词注入**
```python
def sanitize_input(user_input: str) -> str:
    """清理用户输入，防止注入攻击"""
    # 移除可能影响系统提示词的特殊标记
    dangerous_patterns = ["<system>", "</system>", "[INST]", "[/INST]"]
    for pattern in dangerous_patterns:
        user_input = user_input.replace(pattern, "")
    return user_input
```

---

## 6. Skill 与 MCP 的协同模式

### 6.1 Skill 调用 MCP Tool

```markdown
---
name: db-analyze
description: 分析数据库性能问题，使用 MCP 数据库工具获取真实数据
allowed-tools:
  - mcp__database__run_query
  - mcp__database__get_slow_queries
  - mcp__database__explain_query
---

分析数据库性能，使用以下步骤：

1. 使用 `mcp__database__get_slow_queries` 获取慢查询日志（最近1小时）
2. 对TOP 5慢查询使用 `mcp__database__explain_query` 获取执行计划
3. 分析每个查询的执行计划，识别：
   - 全表扫描（type=ALL）
   - 未使用索引的过滤条件
   - 大量临时表和文件排序
4. 生成优化建议报告
```

### 6.2 MCP Tool 增强 Skill 能力

```
Skill 能做的：         MCP Tool 能补充的：
定义分析步骤      +    实际执行 SQL 查询
设计输出格式      +    获取真实数据库数据
制定优化策略      +    验证优化效果
```

---

## 7. 本地调试 MCP Server

```bash
# 使用 MCP Inspector 调试（官方调试工具）
npx @modelcontextprotocol/inspector python weather_mcp_server.py

# 或 Node.js
npx @modelcontextprotocol/inspector node weather-mcp-server.js
```

MCP Inspector 提供：
- 可视化 Tools/Resources/Prompts 列表
- 手动调用 Tool 并查看响应
- 查看原始 JSON-RPC 消息
- 错误日志实时展示

---

## 8. 常用 MCP Server 生态

| Server | 功能 | 安装 |
|--------|------|------|
| `@modelcontextprotocol/server-filesystem` | 本地文件系统访问 | `npx -y @modelcontextprotocol/server-filesystem` |
| `@modelcontextprotocol/server-github` | GitHub API 集成 | `npx -y @modelcontextprotocol/server-github` |
| `@modelcontextprotocol/server-postgres` | PostgreSQL 查询 | `npx -y @modelcontextprotocol/server-postgres` |
| `@modelcontextprotocol/server-brave-search` | Brave 搜索引擎 | `npx -y @modelcontextprotocol/server-brave-search` |
| `@modelcontextprotocol/server-slack` | Slack 消息集成 | `npx -y @modelcontextprotocol/server-slack` |

官方 Server 列表：https://github.com/modelcontextprotocol/servers
