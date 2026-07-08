# 03 - 核心原语：Tools / Resources / Prompts

> 高频题 ★★★ | 进阶题 ★★☆ | 基础题 ★☆☆

---

## Q1. 为什么 MCP 需要三种原语（Tools / Resources / Prompts）？各自的设计初衷是什么？★★★

**答：** 三种原语按两个维度划分外部能力：**谁发起调用** 和 **是否有副作用**。

| 原语 | 发起方 | 副作用 | 设计意图 |
|------|--------|--------|---------|
| **Tools** | LLM 决策 | 可能有 | 让 LLM 能"做事"——执行操作、修改状态 |
| **Resources** | 用户/应用请求 | 无（只读） | 让 LLM 能"看到"外部数据，且可缓存/订阅 |
| **Prompts** | 用户显式触发 | 无（只读） | 提供可复用的工作流模板，确保一致性和可治理性 |

**如果只用一种原语（全用 Tools）会怎样？**
- 无法区分"读取文件"（应该缓存）和"发送邮件"（需要确认）
- 无法实现 Resource 的订阅推送机制
- Prompt 模板无法与具体 Server 解耦
- 权限管控粒度变粗（只能全部允许或全部禁止）

---

## Q2. 请详细描述 Tool 的完整生命周期。★★★

**答：**

```
1. 发现阶段
   Client → Server: tools/list
   Server → Client: {
     tools: [{
       name: "search_issues",
       description: "搜索 GitHub Issues...",
       inputSchema: {
         type: "object",
         properties: {
           query: { type: "string", description: "搜索关键词" },
           state: { type: "string", enum: ["open", "closed"] }
         },
         required: ["query"]
       }
     }]
   }

2. 调用阶段
   Client → Server: tools/call {
     name: "search_issues",
     arguments: { query: "memory leak", state: "open" }
   }

3. 执行与返回
   Server 执行业务逻辑...
   Server → Client: {
     content: [
       { type: "text", text: "找到 3 个 Issue:\n1. #42 ..." }
     ],
     isError: false
   }

4. 变更通知（可选）
   Server → Client: notifications/tools/list_changed
   → Client 重新调用 tools/list 获取最新列表
```

**Tool 定义的三个核心要素：**
1. `name`：唯一标识，小写+下划线，动词前缀
2. `description`：面向 LLM 的语义描述（何时用、何时不用、有何副作用）
3. `inputSchema`：JSON Schema 格式的参数定义

---

## Q3. Tool 的返回值支持哪些内容类型？各有什么使用场景？★★☆

**答：**

| 类型 | 结构 | 使用场景 |
|------|------|---------|
| `text` | `{ "type": "text", "text": "..." }` | 查询结果、错误消息、日志、Markdown 格式报告 |
| `image` | `{ "type": "image", "data": "base64...", "mimeType": "image/png" }` | 图表、截图、照片分析 |
| `resource` | `{ "type": "resource", "resource": { "uri": "...", "mimeType": "..." } }` | 大文件引用，避免直接传输大量数据 |

**混合返回示例**（同时返回文本和图片）：
```json
{
  "content": [
    { "type": "text", "text": "分析完成，详见下图：" },
    { "type": "image", "data": "iVBORw0KGgo...", "mimeType": "image/png" }
  ]
}
```

---

## Q4. Tool 设计中有哪些关键原则？★★★

**答：**

**1. 单一职责**
- 一个 Tool 做好一件事
- ❌ `run_operation` + `action` 参数（万能 Tool 反模式）
- ✅ `search_issues`、`create_issue`、`close_issue` 各司其职

**2. 幂等性优先**
- 查询类 Tool：必须幂等（多次调用结果一致，无副作用）
- 创建类 Tool：建议提供幂等键
- 删除类 Tool：重复删除不报错，返回"已删除"

**3. 严格参数校验**
- 使用 `required` 标记必需参数
- 使用 `enum` 限制选项值
- 使用 `minimum`/`maximum` 限制数值范围
- 使用 `minLength`/`maxLength`/`pattern` 限制字符串
- 设置 `additionalProperties: false` 拒绝未知参数

**4. 描述要面向 LLM**
- 不是写 API 文档，是告诉 LLM 何时使用
- 必须说明副作用（如：会发送邮件、会修改数据库）

**5. 破坏性操作需要显式确认**
- 高风险操作（删除/发送/付费）加 `confirmed: true` 参数模式

---

## Q5. 请详细描述 Resource 的完整生命周期（含订阅机制）。★★★

**答：**

```
1. 发现阶段
   Client → Server: resources/list
   Server → Client: {
     resources: [
       { uri: "github://owner/repo/issues/42", name: "Issue #42", ... },
       { uri: "postgres://schema/table", name: "Users Table", ... }
     ],
     resourceTemplates: [  // URI 模板（动态资源）
       { uriTemplate: "github://{owner}/{repo}/issues/{id}" }
     ]
   }

2. 读取阶段
   Client → Server: resources/read { uri: "github://owner/repo/issues/42" }
   Server → Client: {
     contents: [
       { uri: "github://owner/repo/issues/42", mimeType: "text/markdown", text: "..." }
     ]
   }

3. 订阅阶段（需要 subscribe 能力）
   Client → Server: resources/subscribe { uri: "file:///logs/app.log" }
   Server → Client: {}  // 确认订阅

4. 更新推送
   Server → Client: notifications/resources/updated { uri: "file:///logs/app.log" }
   → Client 重新调用 resources/read 获取最新内容

5. 取消订阅
   Client → Server: resources/unsubscribe { uri: "file:///logs/app.log" }
```

**URI Template 的工作原理**：
```
模板：github://{owner}/{repo}/issues/{id}
实际 URI：github://anthropic/mcp/issues/42
→ Server 解析 owner=anthropic, repo=mcp, id=42
```

---

## Q6. Resource 和 Tool 的边界如何准确划分？请给出判断决策树。★★★

**答：**

```
                    ┌─────────────────────┐
                    │ 这个能力有副作用吗？    │
                    └─────────┬───────────┘
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
            有副作用                         无副作用
         → 一定是 Tool                       │
                                    ┌────────┴────────┐
                                    ▼                  ▼
                              URI 稳定且可缓存      URI 不稳定或
                              不需要 LLM 推理      需要 LLM 决策
                              → Resource          → Tool（返回数据）
```

**具体示例：**

| 场景 | 选 Tool 还是 Resource？ | 理由 |
|------|----------------------|------|
| 查询天气（需要参数推理） | Tool | LLM 需要判断城市、时间 |
| 读取文件（路径确定） | Resource | 稳定 URI，可缓存和订阅 |
| 发送邮件 | Tool | 有副作用 |
| 获取 Issue 列表（带搜索参数） | Tool | LLM 需要组合搜索条件 |
| 获取指定 Issue 详情 | Resource | URI 稳定（`github://owner/repo/issues/42`） |
| 数据库表结构 | Resource | 只读、稳定、可缓存 |
| 执行 SQL 查询 | Tool | 需要 LLM 构造 SQL |

**反模式警示**：
- 用 Tool 做纯数据读取 → 浪费缓存和订阅能力
- 用 Resource 暴露可变操作 → 破坏只读语义
- 把多个操作合并为一个 Tool（万能 Tool） → LLM 难以选择正确参数组合

---

## Q7. 请详细描述 Prompt 的完整生命周期。★★☆

**答：**

```
1. 发现
   Client → Server: prompts/list
   Server → Client: {
     prompts: [{
       name: "review_code",
       description: "使用此模板进行代码审查",
       arguments: [
         { name: "language", description: "编程语言", required: true }
       ]
     }]
   }

2. 渲染
   Client → Server: prompts/get {
     name: "review_code",
     arguments: { language: "python" }
   }
   Server → Client: {
     description: "Python 代码审查",
     messages: [
       { role: "user", content: { type: "text", text: "请审查以下 Python 代码..." } },
       { role: "assistant", content: { type: "text", text: "我将从以下角度审查..." } },
       { role: "user", content: { type: "text", text: "{code}" } }
     ]
   }

3. Host 将 messages 注入 LLM 上下文
   → LLM 按模板格式执行审查

4. 变更通知（可选）
   Server → Client: notifications/prompts/list_changed
```

**Prompt 支持多轮消息**（user/assistant 交替），这使得它可以实现 few-shot 示例——先展示"好的审查是什么样的"，再让 LLM 执行。

---

## Q8. Prompt 与 Agent Skill 有什么区别？什么时候该用哪个？★★★

**答：**

| 维度 | MCP Prompt | Agent Skill |
|------|-----------|-------------|
| **存储位置** | 远程 MCP Server | 本地文件系统（Markdown） |
| **分发方式** | Server 更新即生效，集中管理 | 需要手动同步文件 |
| **认证支持** | 可通过 OAuth 控制访问 | 无访问控制 |
| **参数化** | 支持 `arguments` 模板参数 | 静态文本 |
| **多轮消息** | 支持 user/assistant 交替消息 | 单段指令文本 |
| **治理** | 中心化管理，统一更新 | 各自维护 |

**选择标准：**
- 团队共享、需要统一的流程 → Prompt（通过 MCP Server 分发）
- 个人工作流、临时实验 → Skill（本地文件快速迭代）
- 需要参数化、Few-shot 示例 → Prompt
- 需要引用 MCP Tool → Skill（`allowed-tools` 字段）

**面试话术**：Prompt 是"中心化的工作流模板"，Skill 是"个人化的操作指南"。两者可组合：一个 Skill 可以指导 LLM 如何选择一个合适的 Prompt。

---

## Q9. 如何设计一个好的 Tool description？请给出模板和正反示例。★★★

**答：**

**模板（必须回答 4 个问题）：**
```
[做什么] 对 PostgreSQL 数据库执行只读 SQL 查询。
[何时用] 当用户需要查询数据库中的数据时使用。
[何时不用] 不要用于修改数据（使用 run_mutation）。不要用于不涉及数据库的纯推理问题。
[副作用] 只读操作，无副作用。查询超时 30 秒。结果集上限 1000 行。
```

**正反示例：**

```python
# ❌ 不好的描述
@mcp.tool()
def run_sql(query: str) -> str:
    """执行 SQL 查询"""
    ...

# ✅ 好的描述
@mcp.tool()
def run_sql(query: str) -> str:
    """对 PostgreSQL 数据库执行只读 SQL 查询（SELECT）。

    当用户的问题需要从数据库中检索数据时使用此工具。
    不要在需要修改数据时使用——改用 run_mutation 工具。
    不要用来查询你已知的信息——先判断是否真的需要查询数据库。

    此工具是只读的，无副作用。查询超时 30 秒。
    结果集上限 1000 行，如需更多数据请添加 LIMIT 和 OFFSET。"""
    ...
```

**关键原则**：描述中的每句话都是在帮 LLM 做决策，不是说给人听的。

---

## Q10. Resource URI 的设计规范是什么？★★☆

**答：**

**规范：**
```
<scheme>://<authority>/<path>?<query>
```

**示例：**
```
✅ github://anthropic/mcp/issues/42        — 语义化路径
✅ postgres://prod/schema/users            — 清晰的数据库引用
✅ file:///home/user/project/src/main.py   — 文件系统路径

❌ github://123456                         — 无意义的 ID
❌ res://item?id=42                        — 不透明的标识符
❌ myapp://data                            — 过于模糊
```

**设计原则：**
- URI 应该是人类可读的，不是数据库主键
- Scheme 应该标识数据源（`github`、`postgres`、`file`、`slack`）
- 路径应该反映数据的逻辑结构
- 使用 [RFC 3986](https://tools.ietf.org/html/rfc3986) 标准 URI 格式

---

## Q11. 如何处理动态资源（参数化资源）？★★☆

**答：** 使用 **URI Template**（RFC 6570）：

```json
{
  "resourceTemplates": [
    {
      "uriTemplate": "github://{owner}/{repo}/issues/{issue_id}",
      "name": "GitHub Issue",
      "description": "读取指定 GitHub Issue",
      "mimeType": "application/json"
    },
    {
      "uriTemplate": "postgres://{schema}/tables/{table}/rows?limit={limit}",
      "name": "数据库表行",
      "description": "读取指定表的行数据"
    }
  ]
}
```

Client 通过 `resources/templates/list` 获取模板列表，然后用户/应用填充具体的 URI。这与 REST API 的路径参数概念类似。

---

## Q12. 一个成熟的 MCP Server 应该同时暴露 Tools、Resources 和 Prompts 吗？★★☆

**答：** 不一定，但**成熟的 Server 通常会同时使用三类原语**。

**示例——GitHub MCP Server：**

| 原语 | 示例 | 用途 |
|------|------|------|
| **Tools** | `create_issue`, `merge_pr`, `search_code` | 执行操作 |
| **Resources** | `github://owner/repo/readme`, `github://owner/repo/issues/42` | 读取数据 |
| **Prompts** | `triage_issues`, `release_notes` | 工作流模板 |

**组合设计的好处：**
- Tool 执行操作，Resource 读取结果，Prompt 提供流程模板
- LLM 可以"查看 Resource → 调用 Tool → 再用 Resource 验证结果"
- 用户可以直接引用 Resource（@mention），也可以套用 Prompt 模板

**判断是否需要某种原语：**
- 有写操作 → 必须有 Tool
- 有只读数据且 URI 稳定 → 应该加 Resource
- 有重复性工作流 → 应该加 Prompt

---

## Q13. 什么是 "万能 Tool" 反模式？如何避免？★★★

**答：**

**反模式示例：**
```python
# ❌ 万能 Tool
@mcp.tool()
def github_operation(action: str, ...):
    """执行 GitHub 操作
    action: 'create_issue' | 'search_issues' | 'merge_pr' | 'close_issue' | ...
    """
    if action == "create_issue": ...
    elif action == "search_issues": ...
    elif action == "merge_pr": ...
    # ...
```

**问题：**
1. LLM 需要从巨大的 `action` 枚举中做选择，错误率高
2. 不同操作的参数差异大，inputSchema 无法精确约束
3. 权限控制粒度太粗——要么全允许，要么全禁止
4. description 太长，超出 LLM 的有效注意力范围

**正确做法：拆分为独立 Tool**
```python
@mcp.tool()
def search_issues(query: str, state: str = "open"): ...

@mcp.tool()
def create_issue(title: str, body: str, labels: list[str] = []): ...

@mcp.tool()
def close_issue(issue_id: int, comment: str = ""): ...
```

---

## Q14. 如何设计 Tool 的可恢复错误？★★☆

**答：**

**错误分类与处理策略：**

| 错误类型 | 返回方式 | LLM 行为 |
|---------|---------|---------|
| 参数校验失败 | JSON-RPC error (-32602) | 不重试，需人工修正 |
| 瞬时故障（超时/限流） | `isError: true` + 建议 | LLM 可重试或调整参数 |
| 资源不存在 | `isError: true` + 说明 | LLM 可换用其他资源 |
| 权限不足 | `isError: true` + 原因 | LLM 告知用户，不重试 |
| 服务器内部 bug | JSON-RPC error (-32603) | 不重试 |

**错误消息设计原则：**
```
✅ "查询超时（30 秒）。建议缩小日期范围或添加更精确的 WHERE 条件后重试。"
✅ "文件 /path/to/file 不存在。当前目录中的文件：a.txt, b.txt, c.txt"
❌ "Error: timeout"（LLM 无法理解原因）
❌ "SELECT * FROM ... failed at line 42 in query_executor.py"（暴露内部细节）
```

**关键**：LLM 友好的错误消息 = 解释原因 + 建议下一步行动。

---

## Q15. Resource 的订阅机制可以用来做什么？★★☆

**答：**

**典型场景：**
1. **日志监控**：订阅 `file:///logs/app.log`，实时显示日志更新
2. **配置变更**：订阅 `file:///.env`，配置修改后自动通知
3. **数据库观察**：订阅 `postgres://schema/table`，表变更时提醒
4. **Git 状态**：订阅 `git://status`，工作区变化时更新 UI

**实现要点：**
- Server 需要在 capabilities 中声明 `"subscribe": true`
- Server 负责检测资源变化（文件 watcher、数据库 trigger 等）
- 变化后通过 `notifications/resources/updated` 推送 URI
- Client 收到通知后决定是否重新读取（`resources/read`）

---

## Q16. 如何处理 Tool 返回的大数据量？★★☆

**答：**

**策略 1：分页**
```json
// Request
{ "name": "search_issues", "arguments": { "query": "bug", "limit": 20 } }

// Response
{
  "content": [{ "type": "text", "text": "..." }],
  "_meta": {
    "nextCursor": "eyJvZmZzZXQiOjIwfQ==",
    "hasMore": true
  }
}
```

**策略 2：Resource 引用**
```json
{
  "content": [{
    "type": "resource",
    "resource": {
      "uri": "analysis://result/report-12345.md",
      "mimeType": "text/markdown"
    }
  }]
}
```
LLM 得到"报告的指针"，后续可按需读取。

**策略 3：字段级读取**
提供专门的 Tool 按字段读取大 JSON（如 `read_analysis_section(analysis_json, section)` 模式），避免一次返回整个 40MB JSON。

---

## Q17. Prompts 的 arguments 是如何工作的？★★☆

**答：**

```json
{
  "name": "review_code",
  "arguments": [
    {
      "name": "language",
      "description": "编程语言",
      "required": true
    },
    {
      "name": "focus_areas",
      "description": "审查重点（逗号分隔）",
      "required": false
    }
  ]
}
```

当用户调用 `/review_code language=python focus_areas=security,performance` 时，Host 发送：

```json
{
  "method": "prompts/get",
  "params": {
    "name": "review_code",
    "arguments": { "language": "python", "focus_areas": "security,performance" }
  }
}
```

Server 根据参数渲染出定制化的消息序列，注入 LLM 上下文。

---

## Q18. 三类原语组合设计的典型案例是什么？★★☆

**答：** 以 GitHub MCP Server 为例：

```
场景：用户说 "帮我看看最近的高优先级 Issue，关闭那些已经修复的"

工作流：
1. [Resource] 查看 Issue #42 → resources/read: "github://repo/issues/42"
2. [Tool] 搜索高优先级 Issues → tools/call: search_issues(label="high-priority")
3. [Resource] 逐个查看 Issue 详情 → resources/read: "github://repo/issues/{id}"
4. [Tool] 关闭已修复的 Issue → tools/call: close_issue(issue_id=42, comment="已修复于 #PR123")
5. [Prompt] 生成周报 → prompts/get: weekly_report(period="this_week")

三类原语协同工作：
- Resource 提供"眼睛"（读取数据）
- Tool 提供"手"（执行操作）
- Prompt 提供"流程"（标准化模板）
```

---

## Q19. 为什么 MCP 将 Prompt 和 Tool 分开？不能把 Prompt 当作一种特殊的 Tool 吗？★★☆

**答：** 虽然技术上可以实现，但分开设计有以下原因：

1. **触发机制不同**：Tool 由 LLM 自动决策调用，Prompt 由用户显式触发（斜杠命令/菜单）
2. **语义不同**：Tool = 执行动作，Prompt = 展开模板
3. **安全模型不同**：Tool 可能需人工确认（破坏性操作），Prompt 无需
4. **治理不同**：Prompt 模板通常由团队管理员定义，Tool 由开发者定义
5. **返回值不同**：Tool 返回内容作为 LLM 的观察结果，Prompt 返回内容替换用户输入

**面试话术**：把 Prompt 当作 Tool 就像把"邮件模板"当作"发送邮件按钮"——虽然都涉及文字，但职责和治理方式完全不同。

---

## Q20. 如何测试 Resource 的订阅机制？★★☆

**答：**

使用 **MCP Inspector**：
1. 连接 Server 并确认 `subscribe: true` 在 capabilities 中声明
2. 在 Inspector 界面中找到目标 Resource
3. 点击 Subscribe 按钮
4. 手动触发资源变更（如修改文件、插入数据库行）
5. 观察 Inspector 是否收到 `notifications/resources/updated`
6. 验证通知中的 URI 是否正确

**编程测试（Python）**：
```python
async with stdio_client(server_params) as (read, write):
    async with ClientSession(read, write) as session:
        await session.initialize()
        await session.subscribe_resource("file:///test.txt")
        # 修改文件...
        # 期望：session 收到 resources/updated 通知
```

---

## Q21. 解释 Tool 的 `inputSchema` 中 `additionalProperties: false` 的作用。★★☆

**答：**

```json
{
  "name": "search_issues",
  "inputSchema": {
    "type": "object",
    "properties": {
      "query": { "type": "string" },
      "state": { "type": "string", "enum": ["open", "closed"] }
    },
    "required": ["query"],
    "additionalProperties": false
  }
}
```

**`additionalProperties: false` 的作用：**
- 拒绝任何不在 `properties` 中定义的参数
- 防止 LLM 产生幻觉参数（如 LLM 编造一个 `sort_by` 参数但 Tool 不支持）
- 让参数校验失败尽早暴露（Server 返回 -32602 错误）
- LLM 看到错误后可以调整参数重新尝试

**为什么 LLM 会产生幻觉参数？** 如果 LLM 看到 `search_issues`，它可能根据常识推断出 `sort`、`order`、`page` 等参数——即使工具不支持。`additionalProperties: false` 能快速暴露这种不匹配。

---

## Q22. 如何实现 Tool 的幂等性？★★☆

**答：**

| 操作类型 | 幂等策略 |
|---------|---------|
| **查询** | 天然幂等，无需额外处理 |
| **创建** | 提供 `idempotency_key` 参数，Server 检查是否已存在相同 key 的记录 |
| **更新** | 条件更新（version/ETag），相同数据多次更新结果一致 |
| **删除** | 幂等设计：已删除的资源再次删除返回 "资源已被删除"（而不是错误） |

```python
@mcp.tool()
async def create_issue(title: str, body: str, idempotency_key: str | None = None):
    if idempotency_key:
        existing = await find_by_idempotency_key(idempotency_key)
        if existing:
            return existing  # 幂等返回
    return await do_create(title, body)
```

---

## Q23. 什么是 Tool 的命名空间前缀？为什么要使用它？★★☆

**答：** 当 Host 挂载多个 MCP Server 时，不同 Server 可能有同名 Tool。Host 自动为每个 Server 的 Tool 添加前缀：

```
原始 Tool：search
挂载后：
  mcp__github__search_issues
  mcp__jira__search_issues
  mcp__slack__search_messages
```

**前缀格式**：`mcp__<server_name>__<tool_name>`（Claude Code 风格）或 `<server_name>:<tool_name>`（其他工具风格）。

**用途：**
- 避免命名冲突
- 权限配置（`allow: ["mcp__github__*"]`）
- 审计日志追踪
- Hook 匹配（`preToolUse` 可基于前缀过滤）

---

## Q24. 如何为 Resource 设计合理的 MIME Type？★★☆

**答：**

| 数据类型 | MIME Type |
|---------|-----------|
| Markdown 文本 | `text/markdown` |
| JSON 数据 | `application/json` |
| 纯文本 | `text/plain` |
| SQL 查询结果 | `text/plain` 或 `application/json` |
| HTML | `text/html` |
| 图片 | `image/png`, `image/jpeg` |
| YAML 配置 | `application/x-yaml` |

**原则**：使用标准 MIME Type（如 `text/markdown` 而非 `text/md`），让 Host 能够正确渲染或转换内容。

---

## Q25. 如何判断一个 Prompt 是否应该包含多轮 messages？★★☆

**答：**

**使用多轮 messages 的场景（Few-shot 示例）：**
```json
{
  "messages": [
    { "role": "user", "content": { "type": "text", "text": "审查这段代码" } },
    { "role": "assistant", "content": { "type": "text", "text": "发现 3 个问题：..." } },
    { "role": "user", "content": { "type": "text", "text": "现在审查：{code}" } }
  ]
}
```

**使用单轮 messages 的场景（简单指令）：**
```json
{
  "messages": [
    { "role": "user", "content": { "type": "text", "text": "按 {template} 格式生成 {type} 的发布说明" } }
  ]
}
```

**判断标准**：如果需要通过示例引导 LLM 产生特定风格的输出 → 多轮；如果只需参数化指令 → 单轮。

---

## 自检清单

- [ ] 能解释三类原语的设计划分（谁发起、有无副作用）
- [ ] 能描述 Tool 的完整生命周期和三个核心要素
- [ ] 能区分 Resource URI 和 URI Template
- [ ] 能画出 Tool vs Resource 的判断决策树
- [ ] 能写出一个好的 Tool description 模板
- [ ] 能识别"万能 Tool"反模式并提出改进方案
- [ ] 理解 `isError: true` 的设计意图
- [ ] 理解 `additionalProperties: false` 的作用
- [ ] 知道分页、Resource 引用、字段级读取等大数据处理策略
- [ ] 能举例说明三类原语如何协同工作
