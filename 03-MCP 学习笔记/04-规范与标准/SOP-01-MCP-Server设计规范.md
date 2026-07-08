# SOP-01 MCP Server 设计规范

> 定位：产品级 MCP Server 的设计 Checklist，适用于团队内外发布

---

## 1. 命名规范

### 1.1 Server 名称

- 全小写，用连字符分词：`github-mcp-server`、`postgres-mcp-server`
- 包含业务关键词，便于用户识别能力范围
- npm 包：`@orgname/mcp-<domain>`；PyPI：`mcp-<domain>-server`

### 1.2 Tool 名称

| 规则 | 示例 |
|------|------|
| 小写 + 下划线 | `search_issues`, `run_query` ✅ |
| 动词开头 | `create_user`、`list_orders`、`get_status` ✅ |
| 避免缩写 | `doc` ❌ → `document` ✅ |
| 避免歧义 | `process` ❌（太笼统） |
| 长度 ≤ 40 字符 | 便于 LLM 快速识别 |

### 1.3 Resource URI

- 使用清晰的 scheme 表明数据源：`github://`、`postgres://`、`s3://`
- 路径段语义化，避免用不透明 ID：`github://owner/repo/issues/42` ✅，`github://x7k2p9m2` ❌
- 模板变量用花括号：`{owner}`、`{repo}`

### 1.4 Prompt 名称

- 与 Tool 同规范（动词 + 对象）
- 用 `analyze_`、`generate_`、`review_` 等意图前缀

---

## 2. 描述规范

### 2.1 Tool description 必含元素

一个合格的 description 必须回答 LLM 的四个问题：

```
1. 这个 Tool 做什么？        （What）
2. 什么时候应该调用它？      （When）
3. 不该在什么时候用？        （When not）
4. 会产生什么副作用？        （Side effects）
```

**反面例子**：

```
❌ "query database"
❌ "调用接口"
❌ "Get user information"
```

**正面例子**：

```
✅ "搜索 GitHub 仓库的 Issues。适用于：排查历史 bug、统计工作量、检索相关讨论。
    返回最多 100 条，按 updated 降序。不要用来搜索代码（请用 search_code）。此操作只读。"
```

### 2.2 参数 description

- 写**语义**而非**类型**（类型由 Schema 表达）
- 给出示例值或格式要求
- 标明特殊约束（大小写敏感、取值枚举、单位等）

```jsonc
{
  "repo": {
    "type": "string",
    "description": "仓库完整名称，格式 owner/name（例如：anthropic/mcp）"
  }
}
```

### 2.3 Resource description

说明资源的**来源**、**频率**、**新鲜度**：

```
"最近 7 天的应用错误日志，按时间倒序。每 5 分钟更新一次。"
```

---

## 3. Schema 规范

### 3.1 严格声明

```jsonc
{
  "type": "object",
  "properties": {
    "query": { "type": "string", "minLength": 1, "maxLength": 500 },
    "limit": { "type": "integer", "minimum": 1, "maximum": 100, "default": 20 }
  },
  "required": ["query"],
  "additionalProperties": false
}
```

- 必填项显式 `required`
- 数值型加 `minimum`/`maximum`
- 字符串加 `minLength`/`maxLength`/`pattern`
- 使用 `enum` 限定可选值
- `additionalProperties: false` 拒绝意外参数

### 3.2 避免过度嵌套

LLM 对深层嵌套对象的理解能力差：

```
❌ 5 层嵌套的 options 对象
✅ 扁平化参数，必要时用 JSON 字符串入参（明确说明）
```

### 3.3 默认值优先

能给默认值的参数都给，减少 LLM 必须推理的字段：

```jsonc
{ "unit": { "type": "string", "enum": ["celsius", "fahrenheit"], "default": "celsius" } }
```

---

## 4. 粒度与职责

### 4.1 单 Tool 单职责

```
❌ database_op(action, ...)   ← action 为 query/insert/update/delete
✅ run_query(sql)
   insert_row(table, values)
   update_row(table, where, set)
   delete_row(table, where)
```

### 4.2 避免超大 Server

单个 Server 暴露的 Tool 数量建议 ≤ 20：
- 超过 20 时考虑按子域拆分为多个 Server
- 或用 Resources 承载只读数据，减少 Tool 数量

### 4.3 读写分离

把只读 Tool 和写 Tool 在命名上区分，便于用户与 LLM 识别：

```
✅ list_issues / get_issue  （读）
✅ create_issue / close_issue / comment_on_issue  （写）
```

---

## 5. 幂等性与副作用

### 5.1 幂等性原则

| 操作类型 | 幂等性要求 |
|---------|-----------|
| 查询 | 必须幂等 |
| 创建 | 支持幂等键（idempotency_key）最好 |
| 更新 | 使用条件更新（基于版本号/ETag） |
| 删除 | 幂等（再次删除返回"已不存在"而非报错） |

### 5.2 破坏性操作二次确认

```python
@mcp.tool()
def delete_records(filter: dict, confirmed: bool = False) -> str:
    """删除匹配过滤条件的记录。首次调用仅返回匹配数，需要 confirmed=true 才真正删除。"""
    count = count_matching(filter)
    if not confirmed:
        return f"匹配 {count} 条记录，确认删除请重新调用并传入 confirmed=true"
    do_delete(filter)
    return f"已删除 {count} 条记录"
```

### 5.3 写操作加审计

每个写操作记录 who / when / what，便于追溯。

---

## 6. 错误处理规范

### 6.1 分层

| 错误类型 | 返回方式 | 示例 |
|---------|---------|------|
| **参数不符合 Schema** | 协议错误 `-32602` | JSON Schema 校验失败 |
| **业务可恢复失败** | `isError: true` + 文本 | SQL 语法错 / 数据不存在 |
| **业务不可恢复失败** | `isError: true` + 文本 | 下游服务不可达 |
| **Server 内部 bug** | 协议错误 `-32603` | 未捕获异常 |

### 6.2 错误文本对 LLM 友好

```
❌ "psycopg2.errors.UndefinedTable: relation 'user' does not exist at line 1"
✅ "查询失败：表 'user' 不存在。可用表：users, orders, products"
```

原则：**说明错误 + 可能的正确做法**。

### 6.3 不暴露内部实现

- 不返回堆栈信息
- 不返回内部 IP、密钥、连接串
- 不返回底层库名/版本

---

## 7. 性能规范

### 7.1 响应时间目标

| 操作 | 目标 |
|------|------|
| 查询类 Tool | P95 < 3s |
| 写入类 Tool | P95 < 10s |
| 长任务 | 用 progress 通知，不阻塞 |

### 7.2 限流与配额

Server 应对下游服务限流敏感：
- 内部加请求队列 + 并发上限
- 对外部 API 做 token bucket
- 超限时返回明确信息让 LLM 能等待或换 Tool

### 7.3 分页

返回列表类 Tool 必须支持 `limit` + `cursor`：

```jsonc
{
  "limit": { "type": "integer", "minimum": 1, "maximum": 100, "default": 20 },
  "cursor": { "type": "string", "description": "上一页返回的 nextCursor" }
}
```

返回值：

```jsonc
{
  "content": [{"type": "text", "text": "..."}],
  "_meta": { "nextCursor": "abc123", "hasMore": true }
}
```

---

## 8. 可观测性

### 8.1 结构化日志（写 stderr）

```jsonc
{"ts":"2026-04-21T10:00:00Z","level":"info","event":"tool_call","name":"search_issues","duration_ms":230}
```

### 8.2 指标

建议埋点：
- 每个 Tool 的调用次数、成功/失败率、P95 延迟
- Server 进程内存/CPU
- 连接数（HTTP 传输）

### 8.3 Tracing

在 Tool handler 入口生成 trace_id，透传到下游服务，便于端到端排查。

---

## 9. 版本管理

### 9.1 Server 版本

- 在 `serverInfo.version` 声明语义化版本
- 重大 Tool 签名变更 → MAJOR
- 新增 Tool / 兼容参数 → MINOR
- Bug 修复 → PATCH

### 9.2 协议版本

- 支持至少最近一个稳定 `protocolVersion`
- 协商失败时返回清晰错误文本

### 9.3 Tool 演进

- 删除 Tool 前先 `deprecated`（在 description 里标注）并保留一个版本
- 参数只做**兼容性扩展**（加可选参数），不做破坏性变更
- 破坏性变更时新增 `_v2` 后缀并保留旧 Tool 过渡

---

## 10. 文档规范

### 10.1 README 必含

- 功能总览（一句话）
- 安装与运行命令
- 每个 Tool / Resource / Prompt 的用途表
- 环境变量说明（敏感字段明确标注）
- 最小使用示例（Host 端 .mcp.json 片段）
- 已知限制
- 许可证

### 10.2 Changelog

维护 `CHANGELOG.md`，按 Keep a Changelog 约定记录：Added / Changed / Deprecated / Removed / Fixed / Security。

---

## 11. 发布前 Checklist

```
□ Server 名称与 Tool 命名符合规范
□ 每个 Tool description 回答了 What/When/When-not/Side-effects
□ 所有参数有 Schema 约束与默认值
□ 单元测试覆盖主要 Tool 与错误路径
□ 用 MCP Inspector 手动验证全部能力
□ 错误信息对 LLM 友好，不暴露内部细节
□ 列表类 Tool 支持分页
□ 破坏性操作需二次确认
□ README + Changelog 齐全
□ 敏感环境变量有文档说明
□ 通过安全规范 SOP-02 自检
```

---

## 12. 小结

- 描述**面向 LLM**写：语义、时机、边界缺一不可
- Schema **严格**：省一行校验，LLM 多调错十次
- Tool **粒度窄**：单职责，LLM 更容易正确选择
- 破坏性操作**二次确认**：Host 不一定帮你兜底
- 可观测 + 分页 + 版本管理：从"能跑"到"可维护"的分界线
