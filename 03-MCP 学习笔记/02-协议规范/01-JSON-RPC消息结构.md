# MCP 协议规范：JSON-RPC 消息结构

> 定位：协议层原理，帮助排查异常、实现 Client/Server

---

## 1. MCP 基于 JSON-RPC 2.0

MCP 的所有消息都是 JSON-RPC 2.0 报文。理解 JSON-RPC 的三种消息类型是读懂 MCP 的前提：

| 消息类型 | 必备字段 | 是否期待回复 |
|---------|---------|--------------|
| **Request** | `jsonrpc`, `id`, `method`, `params?` | 是 |
| **Response** | `jsonrpc`, `id`, `result` 或 `error` | — |
| **Notification** | `jsonrpc`, `method`, `params?`（**无 id**） | 否 |

```jsonc
// Request
{ "jsonrpc": "2.0", "id": 1, "method": "tools/list", "params": {} }

// Response（成功）
{ "jsonrpc": "2.0", "id": 1, "result": { "tools": [...] } }

// Response（错误）
{ "jsonrpc": "2.0", "id": 1, "error": { "code": -32602, "message": "Invalid params" } }

// Notification（单向通知，无 id）
{ "jsonrpc": "2.0", "method": "notifications/initialized" }
```

---

## 2. 连接生命周期

MCP 连接分四个阶段：

```
┌───────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  初始化握手   │ →  │   运行态      │ →  │   关闭协商   │ →  │   传输断开   │
│  initialize   │    │   tools/call  │    │   shutdown   │    │              │
│  initialized  │    │   resources/* │    │              │    │              │
└───────────────┘    └──────────────┘    └──────────────┘    └──────────────┘
```

### 2.1 初始化握手

```jsonc
// 1. Client → Server（Request）
{
  "jsonrpc": "2.0",
  "id": 0,
  "method": "initialize",
  "params": {
    "protocolVersion": "2025-03-26",
    "capabilities": {
      "roots": { "listChanged": true },
      "sampling": {}
    },
    "clientInfo": { "name": "claude-code", "version": "x.y.z" }
  }
}

// 2. Server → Client（Response）
{
  "jsonrpc": "2.0",
  "id": 0,
  "result": {
    "protocolVersion": "2025-03-26",
    "capabilities": {
      "tools": { "listChanged": true },
      "resources": { "subscribe": true, "listChanged": true },
      "prompts": { "listChanged": true },
      "logging": {}
    },
    "serverInfo": { "name": "my-server", "version": "1.0.0" }
  }
}

// 3. Client → Server（Notification，确认进入运行态）
{ "jsonrpc": "2.0", "method": "notifications/initialized" }
```

**要点**：
- `protocolVersion` 双方需匹配，否则协商失败
- `capabilities` 是**能力声明**，未声明的能力不能使用
- 只有收到 `initialized` 通知后 Server 才应接受业务请求

### 2.2 Capabilities 协商

| 方向 | 字段 | 含义 |
|------|------|------|
| Client → Server | `roots` | 客户端可暴露的文件系统根目录 |
| Client → Server | `sampling` | 客户端允许 Server 反向请求 LLM 采样 |
| Server → Client | `tools` | Server 提供 Tools 能力 |
| Server → Client | `resources` | Server 提供 Resources 能力 |
| Server → Client | `prompts` | Server 提供 Prompts 能力 |
| Server → Client | `logging` | Server 会发送结构化日志 |

子字段 `listChanged: true` 表示支持列表变化通知；`subscribe: true` 表示 Resources 支持订阅。

---

## 3. 核心 Method 清单

### 3.1 初始化

| Method | 方向 | 用途 |
|--------|------|------|
| `initialize` | C→S | 建立连接、协商能力 |
| `notifications/initialized` | C→S | 确认进入运行态 |
| `ping` | 双向 | 存活检测 |

### 3.2 Tools

| Method | 方向 | 用途 |
|--------|------|------|
| `tools/list` | C→S | 列出可用 Tools |
| `tools/call` | C→S | 调用指定 Tool |
| `notifications/tools/list_changed` | S→C | 通知 Client 重新拉取列表 |

### 3.3 Resources

| Method | 方向 | 用途 |
|--------|------|------|
| `resources/list` | C→S | 列出可用 Resources |
| `resources/read` | C→S | 读取指定 URI 内容 |
| `resources/templates/list` | C→S | 列出 URI 模板 |
| `resources/subscribe` | C→S | 订阅资源变更 |
| `resources/unsubscribe` | C→S | 取消订阅 |
| `notifications/resources/updated` | S→C | 资源变更通知 |
| `notifications/resources/list_changed` | S→C | 列表变更通知 |

### 3.4 Prompts

| Method | 方向 | 用途 |
|--------|------|------|
| `prompts/list` | C→S | 列出可用 Prompts |
| `prompts/get` | C→S | 渲染 Prompt 得到 messages |
| `notifications/prompts/list_changed` | S→C | 列表变更通知 |

### 3.5 反向能力（Server → Client）

MCP 支持 Server 主动向 Client 发请求：

| Method | 方向 | 用途 |
|--------|------|------|
| `sampling/createMessage` | S→C | Server 请求客户端代为调用 LLM |
| `roots/list` | S→C | Server 请求客户端暴露的根目录 |

**要点**：`sampling` 是一个强大特性——允许 Server 内部触发 LLM 推理（例如生成摘要），但必须得到客户端 capabilities 允许。

### 3.6 日志与进度

| Method | 方向 | 用途 |
|--------|------|------|
| `logging/setLevel` | C→S | 设置 Server 日志级别 |
| `notifications/message` | S→C | Server 日志推送 |
| `notifications/progress` | S→C | 长任务进度通知 |
| `notifications/cancelled` | 双向 | 取消一个进行中的请求 |

---

## 4. 错误码规范

MCP 沿用 JSON-RPC 错误码，并扩展了若干自定义码：

| 代码 | 含义 | 典型场景 |
|------|------|---------|
| `-32700` | Parse error | 非法 JSON |
| `-32600` | Invalid Request | 缺 jsonrpc/method |
| `-32601` | Method not found | 未知方法 |
| `-32602` | Invalid params | 参数不符合 schema |
| `-32603` | Internal error | Server 未捕获异常 |
| `-32002` | Resource not found | URI 不存在 |
| `-32001` 起 | Server-defined | Server 自定义业务错误 |

**错误对象结构**：

```jsonc
{
  "code": -32602,
  "message": "Invalid arguments for tool search_issues",
  "data": {
    "field": "limit",
    "reason": "exceeds maximum 100"
  }
}
```

### Tool 业务错误 vs 协议错误

**注意**：Tool 内部业务失败（例如数据库查询失败）**不应该**返回 JSON-RPC error，而应返回正常 Response 并置 `isError: true`。

```jsonc
// ❌ 错误用法：Tool 业务失败返回协议错误
{ "jsonrpc": "2.0", "id": 5, "error": { "code": -32603, "message": "DB timeout" } }

// ✅ 正确用法：协议成功 + content 里说明失败
{
  "jsonrpc": "2.0",
  "id": 5,
  "result": {
    "content": [{ "type": "text", "text": "数据库查询超时，请稍后重试" }],
    "isError": true
  }
}
```

理由：LLM 看到 `isError: true` 可以理解"调用成功但业务失败"，决定是否重试或换方案；而协议错误通常意味着 LLM 调错了 Tool 或参数，是编程错误。

---

## 5. 进度通知

长任务可以通过 `_meta.progressToken` 让 Client 传入一个追踪 token：

```jsonc
// Client 请求
{
  "jsonrpc": "2.0",
  "id": 10,
  "method": "tools/call",
  "params": {
    "name": "analyze_repo",
    "arguments": { "path": "/large/repo" },
    "_meta": { "progressToken": "task-abc" }
  }
}

// Server 执行过程中发通知
{
  "jsonrpc": "2.0",
  "method": "notifications/progress",
  "params": {
    "progressToken": "task-abc",
    "progress": 45,
    "total": 100,
    "message": "正在分析 src/api/"
  }
}

// Server 最终响应
{ "jsonrpc": "2.0", "id": 10, "result": { "content": [...] } }
```

---

## 6. 取消请求

```jsonc
{
  "jsonrpc": "2.0",
  "method": "notifications/cancelled",
  "params": {
    "requestId": 10,
    "reason": "用户关闭会话"
  }
}
```

收到取消通知的一方应**尽力终止**处理（例如中断 HTTP 请求、回滚事务），但可能已经无法撤销副作用。Server 实现应把取消作为协作式信号处理。

---

## 7. 版本协商策略

MCP 使用日期格式版本号（如 `2025-03-26`）。协商规则：

1. Client 在 `initialize` 里声明它期望的版本
2. Server 返回**它能支持的版本**（可能低于 Client）
3. 双方按 Server 返回的版本通信

若 Server 不支持 Client 的任何版本，应返回协议错误，Client 应断开。

---

## 8. 消息批处理（Batching）

JSON-RPC 2.0 支持批量请求（数组包装多条消息），MCP 沿用此机制：

```jsonc
[
  { "jsonrpc": "2.0", "id": 1, "method": "tools/list" },
  { "jsonrpc": "2.0", "id": 2, "method": "resources/list" },
  { "jsonrpc": "2.0", "id": 3, "method": "prompts/list" }
]
```

Server 返回对应的 Response 数组。通知不占 id，批量响应只包含 Request 的结果。

---

## 9. 调试技巧

### 9.1 开启消息日志

大多数 SDK 提供 DEBUG 级别日志，直接打印原始 JSON-RPC 消息：

```bash
# Python SDK
MCP_LOG_LEVEL=DEBUG python my_server.py

# Node SDK（自行接入 pino/winston）
DEBUG=mcp:* node my_server.js
```

### 9.2 使用 MCP Inspector

```bash
npx @modelcontextprotocol/inspector python my_server.py
```

Inspector 提供 UI 查看每条 JSON-RPC 消息、手动调用 Tool、订阅 Resource。

### 9.3 常见错误诊断

| 现象 | 可能原因 |
|------|---------|
| 一直卡在 initialize | 协议版本不匹配 / 传输层异常 |
| Tool 列表为空 | Server capabilities 未声明 tools / list_tools 抛异常 |
| 调用 Tool 返回 Method not found | 大小写或命名空间错误 |
| 返回 Invalid params | inputSchema 与实际参数不一致 |
| 长任务无响应 | 未实现进度通知，看起来像挂起 |

---

## 10. 小结

- MCP = JSON-RPC 2.0 + 约定的 method 集合 + 能力协商
- **Request / Response / Notification** 三类消息，id 是 Request 与 Response 的唯一关联
- **capabilities 协商**决定连接能做什么
- **isError 协议成功但业务失败** 与 **JSON-RPC error 协议层失败** 要区分清楚
- 理解消息结构 → 读 Inspector 日志时能秒懂 → 调试效率翻倍
