# MCP 传输层：Stdio 与 HTTP/SSE

> 定位：掌握两种官方传输方式的选型、配置与差异

---

## 1. 传输层在 MCP 中的位置

```
┌─────────────────────────────────┐
│   应用层：MCP 协议（JSON-RPC）  │
├─────────────────────────────────┤
│   传输层：Stdio 或 HTTP/SSE     │  ← 本文主题
├─────────────────────────────────┤
│   操作系统：管道 / TCP / TLS    │
└─────────────────────────────────┘
```

传输层负责把 JSON-RPC 消息从 Client 送到 Server 再返回。MCP 定义了**两种官方传输**，协议语义一致，选型取决于部署形态。

---

## 2. Stdio 传输

### 2.1 原理

Client 启动 Server 子进程，通过该进程的 **stdin / stdout** 双向收发消息。stderr 作为日志通道。

```
┌─────────────┐                     ┌─────────────┐
│  MCP Client │                     │  MCP Server │
│             │───► stdin    ───►   │             │
│             │◄─── stdout   ◄───   │             │
│             │◄─── stderr(日志)◄── │             │
└─────────────┘                     └─────────────┘
```

### 2.2 消息帧格式

每条 JSON-RPC 消息独占一行，以 `\n` 结尾（**NDJSON**）：

```
{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}\n
{"jsonrpc":"2.0","id":1,"result":{"tools":[...]}}\n
```

**注意事项**：

- 消息内部**不能有未转义的换行**，JSON 序列化必须压缩成单行
- stderr 可以自由输出多行日志，不会干扰协议
- Server 绝对不要 `print()` 到 stdout——这会污染协议通道

### 2.3 Claude Code 配置示例

```jsonc
// .mcp.json 或全局配置
{
    "mcpServers": {
        "filesystem": {
            "command": "npx",
            "args": [
                "-y",
                "@modelcontextprotocol/server-filesystem",
                "/home/user/projects"
            ]
        },
        "my-python-server": {
            "command": "python",
            "args": [
                "-m",
                "my_mcp_server"
            ],
            "env": {
                "API_KEY": "xxx",
                "LOG_LEVEL": "INFO"
            }
        }
    }
}
```

| 字段        | 说明                      |
|-----------|-------------------------|
| `command` | 可执行命令（需在 PATH 中，或写绝对路径） |
| `args`    | 命令行参数数组                 |
| `env`     | 追加到进程的环境变量              |
| `cwd`     | 工作目录（可选）                |

### 2.4 优势

| 优势       | 说明                     |
|----------|------------------------|
| **零配置**  | 不需要开端口、不需要证书           |
| **隔离性强** | 每次 Host 启动独立进程，崩溃不影响其他 |
| **本地可达** | 天然可访问文件系统、本地工具链        |
| **鉴权天然** | 进程由 Host 启动，无需额外身份校验   |

### 2.5 局限

| 局限                | 说明                |
|-------------------|-------------------|
| **无法跨机器**         | 只能本地运行            |
| **无法多 Client 共享** | 每个 Host 启动独立进程    |
| **启动成本**          | 每次 Host 启动都要拉起子进程 |
| **跨用户不友好**        | 需要每个用户机器都安装依赖     |

### 2.6 适用场景

- 本地开发工具集成（格式化、lint、git）
- 访问本地文件系统、本地数据库
- 个人敏感凭据（不适合共享到远程服务）
- 开发调试阶段的 POC

---

## 3. HTTP + SSE 传输（旧版规范）

### 3.1 原理

Client 通过两个独立 HTTP 端点与 Server 通信：

```
POST /messages         ← Client 发送请求/通知
GET  /sse              ← Server 通过 Server-Sent Events 推送响应与通知
```

```
┌─────────────┐  POST /messages (JSON-RPC)  ┌─────────────┐
│  MCP Client │ ──────────────────────────► │  MCP Server │
│             │                              │             │
│             │ ◄──── SSE 事件流 ──────────  │             │
│             │      GET /sse (长连接)       │             │
└─────────────┘                              └─────────────┘
```

### 3.2 消息帧格式

Server → Client 使用 SSE 规范：

```
event: message
data: {"jsonrpc":"2.0","id":1,"result":{...}}

event: message
data: {"jsonrpc":"2.0","method":"notifications/progress","params":{...}}
```

### 3.3 配置示例

```jsonc
{
    "mcpServers": {
        "remote-api": {
            "url": "https://mcp.example.com/sse",
            "headers": {
                "Authorization": "Bearer xxx"
            }
        }
    }
}
```

### 3.4 局限

- 需要维护两个端点、状态跨端点关联
- 断线重连语义复杂
- 在无状态部署（serverless、负载均衡）下实现成本高

---

## 4. Streamable HTTP（新版规范，推荐）

### 4.1 定位

2025-03 规范引入 **Streamable HTTP**，用一个端点同时支持：

- 普通 POST Request/Response
- 需要流式返回时升级为 SSE

**替代**旧版 HTTP+SSE，统一为单端点 `POST /mcp`。

### 4.2 工作模式

```
Client → POST /mcp (JSON-RPC Request)
   │
   ├─ 若 Server 立即有完整响应 → 返回普通 HTTP 200 + JSON
   │
   └─ 若 Server 需要推送进度/多条消息 →
         返回 Content-Type: text/event-stream
         通过 SSE 连续推送
```

### 4.3 会话管理

Server 在响应头中下发 `Mcp-Session-Id`，Client 后续请求携带该头维持会话：

```http
# 第一次请求
POST /mcp
Content-Type: application/json
{"jsonrpc":"2.0","id":0,"method":"initialize","params":{...}}

# 响应
HTTP/1.1 200 OK
Mcp-Session-Id: 1868a90c-1234-...
Content-Type: application/json
{"jsonrpc":"2.0","id":0,"result":{...}}

# 后续请求
POST /mcp
Mcp-Session-Id: 1868a90c-1234-...
Content-Type: application/json
{...}
```

### 4.4 优势

| 优势          | 说明                         |
|-------------|----------------------------|
| **单端点**     | 网关/WAF 配置简单                |
| **无状态友好**   | 支持多实例负载均衡（靠 Session-Id 路由） |
| **标准 HTTP** | 易监控、易鉴权、易抓包                |
| **兼容代理**    | 可穿透企业代理与 CDN               |

### 4.5 鉴权

推荐通过标准 HTTP 头鉴权：

```http
POST /mcp
Authorization: Bearer <OAuth-token>
Mcp-Session-Id: ...
```

MCP 规范建议使用 **OAuth 2.1**（Authorization Code + PKCE）作为远程 Server 的首选鉴权方式。

---

## 5. 传输方式对比

| 维度              | Stdio     | HTTP/SSE（旧） | Streamable HTTP（新）     |
|-----------------|-----------|-------------|------------------------|
| **部署形态**        | 本地进程      | 远程服务        | 远程服务                   |
| **多 Client 共享** | 否         | 是           | 是                      |
| **跨机器**         | 否         | 是           | 是                      |
| **鉴权**          | 进程信任      | HTTP Header | OAuth 2.1 + Session-Id |
| **实现复杂度**       | 低         | 中           | 中                      |
| **启动开销**        | 每次进程启动    | 持久服务        | 持久服务                   |
| **可观测性**        | stderr 日志 | 标准 HTTP 日志  | 标准 HTTP 日志             |
| **规范状态**        | 稳定        | 逐步淘汰        | 推荐                     |

---

## 6. 选型决策

```
我的 Server 需要被谁用？
│
├─ 单用户、本地工具
│   └──► Stdio
│
├─ 团队共享的内部服务
│   └──► Streamable HTTP + 企业 SSO
│
├─ 对外 SaaS（给外部客户）
│   └──► Streamable HTTP + OAuth 2.1
│
└─ 需要访问用户本地文件/凭据
    └──► Stdio（不要通过网络传输敏感本地数据）
```

---

## 7. Stdio 实现常见坑

### 7.1 不要 print 到 stdout

```python
# ❌ 污染 stdout 协议通道
print("Server started")

# ✅ 写 stderr
import sys
print("Server started", file=sys.stderr)

# ✅ 或使用 logging，配置 handler 到 stderr
import logging
logging.basicConfig(stream=sys.stderr, level=logging.INFO)
```

### 7.2 子进程退出不清理

Host 关闭时会发 SIGTERM 给 Server 子进程，Server 应优雅关闭：

```python
import signal
signal.signal(signal.SIGTERM, lambda *_: cleanup())
```

### 7.3 Windows 下换行问题

Windows 可能默认用 `\r\n`，需要把 stdout 设为二进制或禁用换行转换：

```python
import sys
sys.stdout.reconfigure(newline="\n")
```

---

## 8. HTTP 实现常见坑

### 8.1 SSE 连接超时

代理/负载均衡常默认 60s 空闲断连，需要：

- 定期发送 keep-alive 通知
- 或把 idle timeout 调高

### 8.2 CORS

浏览器场景的 Client 需要 Server 正确响应 CORS：

```
Access-Control-Allow-Origin: <client-origin>
Access-Control-Allow-Headers: Content-Type, Authorization, Mcp-Session-Id
Access-Control-Expose-Headers: Mcp-Session-Id
```

### 8.3 Session-Id 路由

多实例部署下，需保证同一 Session-Id 的请求路由到同一实例（粘性会话）或共享状态（Redis）。

---

## 9. 小结

- **Stdio** — 本地工具首选，零配置、天然隔离
- **HTTP/SSE（旧）** — 遗留方案，新项目不推荐
- **Streamable HTTP（新）** — 远程服务标准选择，搭配 OAuth 2.1
- 两种传输**协议语义一致**，Server 业务代码可以共用，只换传输适配层
