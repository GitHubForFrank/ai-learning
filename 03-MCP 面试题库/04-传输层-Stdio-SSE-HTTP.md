# 04 - 传输层：Stdio / SSE / Streamable HTTP

> 高频题 ★★★ | 进阶题 ★★☆ | 基础题 ★☆☆

---

## Q1. MCP 支持哪几种传输方式？各自适用什么场景？★★★

**答：**

| 传输方式 | 状态 | 适用场景 |
|---------|------|---------|
| **Stdio** | 官方标准 | 本地工具，单用户，低延迟，零配置 |
| **HTTP + SSE** | 已废弃（legacy） | 旧项目兼容，不推荐新项目使用 |
| **Streamable HTTP** | 官方推荐 | 远程服务，多用户共享，团队协作，SaaS |

**决策树：**
```
单用户 + 本地工具 → Stdio
团队共享 + 内网 → Streamable HTTP + SSO
对外开放 SaaS → Streamable HTTP + OAuth 2.1
需要访问本地敏感数据 → Stdio（不要暴露到网络）
```

---

## Q2. 请详细描述 Stdio 传输模式的工作原理。★★★

**答：**

```
┌──────────┐    stdin (JSON-RPC Request)     ┌──────────┐
│  MCP      │ ──────────────────────────────→ │  MCP      │
│  Client   │ ←────────────────────────────── │  Server   │
│  (Host)   │    stdout (JSON-RPC Response)   │ (子进程)   │
└──────────┘                                  └──────────┘
                   stderr (日志)
```

**工作流程：**
1. Client 作为父进程，通过 `command` + `args` 启动 Server 子进程
2. Client → Server：通过 Server 的 **stdin** 发送 JSON-RPC 消息（NDJSON 格式，每行一条）
3. Server → Client：通过 **stdout** 返回 JSON-RPC 响应（NDJSON 格式）
4. Server 的日志通过 **stderr** 输出
5. Client 关闭时，发送 SIGTERM 终止子进程

**Stdin/Stdout/Stderr 分工：**
| 通道 | 用途 | 格式 |
|------|------|------|
| stdin | Client → Server 的 JSON-RPC 消息 | NDJSON |
| stdout | Server → Client 的 JSON-RPC 消息 | NDJSON |
| stderr | Server 日志输出 | 自由格式 |

**绝对禁忌**：在 stdout 中 `print()` 日志——会污染协议通道！

---

## Q3. Stdio 传输模式有哪些优势和局限？★★☆

**答：**

**优势：**
- **零配置**：不需要端口、证书、DNS，启动即用
- **强隔离**：每个 Server 是独立进程，崩溃不影响 Host
- **本地文件访问**：天然可访问本地文件系统
- **隐式信任**：同机器运行，无需认证
- **低延迟**：没有网络往返开销

**局限：**
- **不可跨机器**：只能本地调用
- **不可共享**：一个 Server 进程只能服务一个 Client
- **冷启动成本**：每次连接需要启动新进程（JVM 类语言尤为明显）
- **平台差异**：Windows 的进程管理、信号处理与 Unix 不同
- **单用户**：无法实现多租户

---

## Q4. 什么是 NDJSON？为什么 Stdio 使用 NDJSON 格式？★★☆

**答：**

**NDJSON（Newline Delimited JSON）**：每行一条完整的 JSON，以 `\n` 结尾。

```
{"jsonrpc":"2.0","id":1,"method":"tools/list"}\n
{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{...}}\n
```

**为什么使用 NDJSON：**
1. **简单**：不需要长度前缀或帧分隔符，只需按行读取
2. **流式友好**：可以在不完整接收所有消息的情况下逐条处理
3. **调试友好**：可以直接用文本编辑器查看消息流
4. **标准兼容**：JSON-RPC 2.0 没有规定传输格式，NDJSON 是业界常见选择

**注意事项：**
- JSON 消息体内部不能包含未转义的 `\n`（会破坏行边界）
- 使用紧凑 JSON 格式（无缩进、无换行）
- Windows 上注意 `\r\n` vs `\n` 的区别

---

## Q5. HTTP + SSE（旧传输方式）有什么局限性？★★☆

**答：**

HTTP + SSE 使用两个端点：
- `POST /messages`：Client 发送请求
- `GET /sse`：Server 通过 SSE（Server-Sent Events）推送响应和通知

**局限性：**
1. **双端点复杂度**：需要维护两个连接，增加客户端实现复杂度
2. **重连语义模糊**：SSE 连接断开后，如何处理已在途的请求不明确
3. **服务端部署困难**：Serverless 函数（如 AWS Lambda）和负载均衡器通常不支持长连接 SSE
4. **粘性会话依赖**：多实例部署时需要确保同一 Client 的请求和 SSE 连接到同一实例
5. **CORS 复杂性**：需要正确配置跨域头

**已被 Streamable HTTP 取代，新项目不应使用 SSE 模式。**

---

## Q6. 请详细描述 Streamable HTTP 传输模式的工作原理。★★★

**答：**

**单一端点：** `POST /mcp`

**两种响应模式：**

**模式 1：普通 JSON 响应（短任务）**
```
Client → Server: POST /mcp
  Headers: { "Content-Type": "application/json", "Accept": "application/json" }
  Body: { "jsonrpc": "2.0", "id": 1, "method": "tools/list" }

Server → Client: 200 OK
  Headers: { "Content-Type": "application/json", "Mcp-Session-Id": "abc123" }
  Body: { "jsonrpc": "2.0", "id": 1, "result": { "tools": [...] } }
```

**模式 2：SSE 流式响应（长任务/通知）**
```
Client → Server: POST /mcp
  Headers: { "Accept": "text/event-stream" }
  Body: { "jsonrpc": "2.0", "id": 2, "method": "tools/call", "params": {...} }

Server → Client: 200 OK
  Headers: { "Content-Type": "text/event-stream" }
  Body:
    event: message
    data: {"jsonrpc":"2.0","method":"notifications/progress","params":{...}}

    event: message
    data: {"jsonrpc":"2.0","id":2,"result":{...}}
```

**Session 管理**：通过 `Mcp-Session-Id` HTTP Header 进行会话标识。

---

## Q7. Streamable HTTP 相比旧的 HTTP+SSE 有什么优势？★★★

**答：**

| 维度 | HTTP + SSE（旧） | Streamable HTTP（新） |
|------|-----------------|---------------------|
| **端点数量** | 2 个（POST /messages + GET /sse） | 1 个（POST /mcp） |
| **响应方式** | 总是通过 SSE 推送 | 普通 JSON 或 SSE 流式（按需升级） |
| **Serverless 友好** | 否（需要长连接） | 是（短任务可用普通 HTTP 响应） |
| **部署复杂度** | 高（需要粘性路由 + CORS） | 低（标准 HTTP，易代理/CDN） |
| **监控** | 较复杂（两个端点） | 简单（单端点标准 HTTP） |
| **认证** | 无标准方案 | `Authorization: Bearer` + OAuth 2.1 |
| **状态** | 已废弃 | 官方推荐 |

**核心改进**：将"总是流式"改为"按需流式"——简单请求用普通 JSON 响应，长任务才升级为 SSE 流式。

---

## Q8. Streamable HTTP 的 Session 管理机制是怎样的？★★☆

**答：**

**Session 生命周期：**
```
1. 首次请求（不带 Session-Id）
   Client → Server: POST /mcp
     Body: initialize 请求

2. Server 创建 Session 并返回 Session-Id
   Server → Client: 200 OK
     Headers: { "Mcp-Session-Id": "sess_abc123def456" }
     Body: InitializeResult

3. 后续请求（携带 Session-Id）
   Client → Server: POST /mcp
     Headers: { "Mcp-Session-Id": "sess_abc123def456" }
     Body: 其他 JSON-RPC 请求

4. Session 过期或关闭
   - 超时自动失效（Server 端控制过期时间）
   - Client 可显式终止 Session
```

**关键特性：**
- Session-Id 由 Server 生成，格式不限（UUID、随机字符串等）
- Server 端维护 Session → 状态（连接信息、协商的能力、待处理的请求等）
- 多实例部署时需粘性路由（基于 Session-Id 将请求路由到同一实例）或共享 Session 存储（如 Redis）

---

## Q9. MCP 的认证机制是怎样的？★★★

**答：**

**Stdio 模式**：无需认证（同机器进程通信，天然信任）

**Streamable HTTP 模式**：通过 `Authorization: Bearer <token>` 头进行认证，推荐使用 **OAuth 2.1 + PKCE**。

**OAuth 2.1 流程：**
```
1. Client 发现 Server 的 OAuth 端点
   → GET /.well-known/oauth-protected-resource

2. Client 发起授权请求
   → GET /authorize?response_type=code&client_id=...&code_challenge=...

3. 用户登录并授权

4. Client 用授权码换取 Token
   → POST /token
     Body: { grant_type: "authorization_code", code: "...", code_verifier: "..." }

5. Server 返回 Token
   → { access_token: "...", refresh_token: "...", expires_in: 3600 }

6. Client 用 Access Token 调用 MCP
   → POST /mcp
     Headers: { "Authorization": "Bearer eyJhbGciOi..." }
```

**Token 生命周期管理：**
- access_token：短期有效（15-60 分钟）
- refresh_token：长期有效但可撤销
- 使用 refresh_token 换取新的 access_token 时，可以同时轮换 refresh_token

**Tool 级别的 Scope 控制：**
```
scope: "repo:read repo:write repo:admin"
→ Server 根据 scope 决定哪些 Tool 可用
```

---

## Q10. 如何选择 Stdio 和 Streamable HTTP？给出决策标准。★★★

**答：**

**选择 Stdio 当：**
- Server 运行在本地（与 Host 同机器）
- 单用户使用
- 需要访问本地文件系统
- 零配置是优先考虑
- 对延迟敏感

**选择 Streamable HTTP 当：**
- Server 部署在远程服务器
- 多用户共享同一个 Server
- 需要认证（SSO/OAuth）
- 需要独立扩缩容
- 需要跨网络访问
- Server 启动慢（长连接进程可复用）

**混合部署策略：**
- 开发环境：全部 Stdio（简单、快速）
- 生产环境：数据库/API Server 部署为 Streamable HTTP（共享、可治理）
- 敏感数据 Server：始终 Stdio（数据不出本机）

---

## Q11. Stdio 模式下，Server 的子进程管理需要注意什么？★★☆

**答：**

1. **优雅关闭**：Host 退出时应发送 SIGTERM，给 Server 清理资源的时间
2. **孤儿进程**：Host 异常崩溃时，子进程不应成为孤儿——使用进程组或 Supervisor 管理
3. **环境变量**：通过 `env` 字段注入，不硬编码在命令中
4. **工作目录**：通过 `cwd` 字段指定，确保相对路径正确
5. **启动超时**：设置合理超时（如 10 秒），避免因 Server 启动失败导致 Host 卡住
6. **stderr 捕获**：将 Server 的 stderr 写入日志文件，方便调试
7. **资源限制**：可通过操作系统机制（cgroups/Job Object）限制 CPU/内存

**配置示例：**
```json
{
  "mcpServers": {
    "my-server": {
      "command": "python",
      "args": ["-m", "my_mcp_server"],
      "env": { "DATABASE_URL": "${DB_URL}" },
      "cwd": "/path/to/project"
    }
  }
}
```

---

## Q12. HTTP 传输模式下如何处理 CORS 问题？★★☆

**答：**

MCP Server 部署为 HTTP 服务时，如果 Client 在浏览器中运行（如网页版 AI 工具），需要处理跨域请求。

**Server 端需要设置：**
```
Access-Control-Allow-Origin: <允许的域名>
Access-Control-Allow-Methods: POST, OPTIONS
Access-Control-Allow-Headers: Content-Type, Authorization, Mcp-Session-Id
```

**最佳实践：**
- 生产环境不要用 `*`（允所有域名）
- 显式列出允许的 Client 域名
- 对 OPTIONS 预检请求返回 204

**注意**：大多数桌面 AI 工具（Claude Desktop、Cursor）的 Client 不是浏览器环境，不受 CORS 限制。CORS 主要是浏览器端 MCP Client 需要关注的问题。

---

## Q13. 什么是 Streamable HTTP 的 "按需流式升级"？★★☆

**答：** Streamable HTTP 的核心创新——同一个端点可以根据 Client 需求返回不同格式。

**Client 通过 `Accept` Header 表达偏好：**
```
Accept: application/json              → Server 返回普通 JSON 响应
Accept: text/event-stream             → Server 返回 SSE 流式响应
Accept: application/json, text/event-stream  → Server 自行选择
```

**Server 根据任务特性选择：**
- `tools/list`（瞬时响应）→ 返回普通 JSON
- `tools/call`（可能耗时）→ 如果快速完成返 JSON，如果需时升级为 SSE
- `notifications/progress` → SSE 流式推送

**好处**：
- 简单请求不受 SSE 连接开销影响
- 长任务可以获得流式进度更新
- 与 HTTP 基础设施（代理/CDN/防火墙）完全兼容

---

## Q14. Stdio 模式下，Server 崩溃后如何恢复？★★☆

**答：**

**Client 端的恢复策略：**
1. **检测崩溃**：子进程退出（非零退出码）、stdout 关闭、心跳超时
2. **自动重连**：按退避策略重启 Server 子进程（如 1s → 2s → 4s → max 30s）
3. **状态重建**：重新执行 `initialize` 握手 → `tools/list` → 恢复资源订阅
4. **上限保护**：连续重启失败 N 次（如 5 次）后放弃，通知用户

```
try:
    while True:
        try:
            await run_server()
        except ServerCrashException:
            attempts += 1
            if attempts > MAX_ATTEMPTS:
                raise
            await asyncio.sleep(min(backoff * (2 ** attempts), 30))
finally:
    cleanup()
```

---

## Q15. 如何在 Stdio 模式下调试消息流？★★☆

**答：**

**方法 1：MCP Inspector**
```bash
npx @modelcontextprotocol/inspector python -m my_server
```

**方法 2：包装脚本截获 stdout**
```bash
#!/bin/bash
# wrapper.sh —— 将 stdout 同时写入日志和协议通道
python -m my_server 2>stderr.log | tee protocol.log
```

**方法 3：在 Server 代码中记录消息**
```python
# 记录所有收到的消息（写入 stderr）
import sys
import json

original_stdin_read = sys.stdin.readline
def logged_readline():
    line = original_stdin_read()
    if line:
        print(f"[MCP ←] {line.strip()}", file=sys.stderr)
    return line
sys.stdin.readline = logged_readline
```

**方法 4：环境变量**
```bash
MCP_LOG_LEVEL=DEBUG python -m my_server 2>debug.log
```

---

## Q16. Streamable HTTP 和普通 REST API 有什么不同？★★☆

**答：**

| 维度 | Streamable HTTP (MCP) | 普通 REST API |
|------|----------------------|---------------|
| **端点** | 单一 `POST /mcp` | 多个资源端点 |
| **协议** | JSON-RPC 2.0（方法名路由） | HTTP 方法路由（GET/POST/PUT/DELETE） |
| **响应** | JSON 或 SSE 流式 | 通常是 JSON |
| **会话** | `Mcp-Session-Id` Header | Cookie / Token |
| **目标用户** | AI 模型（LLM） | 人类开发者 |
| **发现机制** | `initialize` → `tools/list` | OpenAPI Spec 文档 |

**本质区别**：Streamable HTTP 是 JSON-RPC over HTTP，不是 RESTful 设计。虽然用了 HTTP 作为传输层，但路由逻辑在 JSON-RPC 的 `method` 字段中，而非 URL 路径中。

---

## Q17. 语言选型时，传输方式会带来什么影响？★★☆

**答：**

**Stdio 模式对语言的要求：**
- **启动速度**：JVM/C# 冷启动较慢（可能 1-3 秒），Python/Node/Go/Rust 快（< 1 秒）
- **进程管理**：需处理多平台信号差异（SIGTERM vs WM_CLOSE）
- **单进程**：无需考虑并发连接

**Streamable HTTP 模式对语言的要求：**
- **并发模型**：Java/Go/Rust 有成熟的异步并发能力，Python GIL 可能成为瓶颈
- **连接池**：数据库连接需池化管理
- **内存管理**：长连接 SSE 流需要管理连接状态
- **部署**：需要 HTTP Server 框架（FastAPI/Express/Spring/Axum）

**推荐组合：**
- Stdio + Python/Node.js（快速原型）
- Streamable HTTP + Go/Java（生产高并发）
- Stdio + Rust（极致性能 + 本地工具）

---

## Q18. 多实例部署 Streamable HTTP Server 时，需要注意什么？★★☆

**答：**

1. **粘性路由（Sticky Session）**：基于 `Mcp-Session-Id` 将同一会话的请求路由到同一实例
2. **Session 共享**：或者将 Session 状态存储到 Redis/数据库，实现真正的无状态
3. **资源订阅**：订阅状态需要跨实例同步或持久化
4. **优雅重启**：滚动更新时，已有会话需要排干或迁移
5. **健康检查**：提供 `/health` 端点给负载均衡器
6. **限流**：跨实例的全局限流（Redis 令牌桶）

---

## 自检清单

- [ ] 能说出三种传输方式及其状态（标准/废弃/推荐）
- [ ] 理解 Stdio 的 stdin/stdout/stderr 分工
- [ ] 理解为什么 stdout 不能用于日志
- [ ] 能解释 NDJSON 格式
- [ ] 理解 Streamable HTTP 的"按需流式升级"机制
- [ ] 理解 Session-Id 的作用
- [ ] 了解 OAuth 2.1 + PKCE 认证流程
- [ ] 能根据场景选择 Stdio vs Streamable HTTP
- [ ] 知道 Stdio 模式下的子进程管理和崩溃恢复
- [ ] 了解 CORS 和多实例部署的注意事项
