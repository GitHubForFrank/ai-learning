# MCP 协议与集成篇 · 面试题库

> 涵盖 MCP 协议原理、开发实践、与 Skill 的协同模式及安全边界

---

## Q8.1 什么是 MCP 协议？它解决了什么问题？

**参考答案**：
**MCP（Model Context Protocol）** 是 Anthropic 提出的开放标准协议，用于规范 AI 模型与外部工具/数据源之间的通信方式。

**解决的核心问题**——"能力孤岛"：
```
传统方式（无 MCP）：
  Agent ─── 自定义 API ──→ 工具A
  Agent ─── 自定义 API ──→ 工具B
  Agent ─── 自定义 API ──→ 工具C
  （每次集成都需要定制开发，M×N 集成碎片化）

MCP 方式：
  Agent ─── MCP 协议 ──→ MCP Server A
  Agent ─── MCP 协议 ──→ MCP Server B
  （标准化协议，一次实现，处处可用）
```

**难度**：⭐ | **考察点**：MCP 基础理解

---

## Q8.2 MCP 有哪三类能力原语？各自的特点是什么？

**参考答案**：

| 原语 | 性质 | 调用方向 | 典型用途 |
|------|------|---------|---------|
| Tools（工具） | 可执行函数 | Agent 主动调用 | 执行 SQL、发送消息、查询天气 |
| Resources（资源） | 只读数据 | Agent 按需读取 | 文件内容、数据库表、配置信息 |
| Prompts（提示词模板） | 可复用 Prompt | 服务端提供 | 代码审查模板、分析模板 |

**Tools 示例**：
```jsonc
{
  "name": "run_sql_query",
  "description": "执行 SQL 查询并返回结果",
  "inputSchema": {
    "type": "object",
    "properties": {
      "query": { "type": "string" },
      "database": { "type": "string", "default": "main" }
    },
    "required": ["query"]
  }
}
```

**难度**：⭐⭐ | **考察点**：MCP 核心概念

---

## Q8.3 MCP 的两种传输方式分别是什么？各适合什么场景？

**参考答案**：

| 传输方式 | 通信信道 | 适合场景 |
|---------|---------|---------|
| Stdio | stdin/stdout（进程间） | 本地工具集成、开发调试、访问本地文件系统 |
| HTTP/SSE | HTTP + Server-Sent Events | 云服务集成、多客户端共享、生产环境部署 |

**Stdio 配置示例**：
```jsonc
{
  "mcpServers": {
    "filesystem": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-filesystem", "/path"]
    }
  }
}
```

**难度**：⭐⭐ | **考察点**：传输层理解

---

## Q8.4 MCP Server 的 Tool 设计有哪些原则？

**参考答案**：

**原则一：描述精准**
```jsonc
// ❌ 描述模糊
{"description": "查询数据"}

// ✅ 描述明确
{"description": "查询指定用户的订单列表，返回最近30天内的订单，按创建时间降序。最多返回100条。"}
```

**原则二：参数类型严格**
- 使用 `type` 明确类型，添加 `minimum`/`maximum` 约束
- 使用 `enum` 限制可选值
- `required` 标注必填项
- `additionalProperties: false` 拒绝额外参数

**原则三：错误响应规范**
- 参数错误 → 返回明确错误信息（让 Agent 可以纠正重试）
- 系统错误 → 返回通用错误（不暴露内部细节）
- 使用 `isError: true` 标记错误响应

**原则四：危险操作需要确认**
- 删除、修改等不可逆操作：先返回影响范围提示，要求确认后重新调用

**难度**：⭐⭐⭐ | **考察点**：Tool 设计能力

---

## Q8.5 Skill 如何与 MCP Tool 协同工作？

**参考答案**：

**协同示例——数据库分析 Skill**：
```markdown
---
name: db-analyze
description: 分析数据库性能问题，使用 MCP 数据库工具获取真实数据
allowed-tools:
  - mcp__database__run_query
  - mcp__database__get_slow_queries
  - mcp__database__explain_query
---

分析数据库性能：
1. 使用 `mcp__database__get_slow_queries` 获取慢查询日志（最近1小时）
2. 对 TOP 5 慢查询使用 `mcp__database__explain_query` 获取执行计划
3. 分析每个查询的执行计划，识别：全表扫描、未使用索引、文件排序
4. 生成优化建议报告
```

**分工关系**：
```
Skill 提供的：         MCP Tool 补充的：
定义分析步骤      +    实际执行 SQL 查询
设计输出格式      +    获取真实数据库数据
制定优化策略      +    验证优化效果
```

**核心理解**：Skill 告诉 Agent **如何做**（流程+约束），MCP Tool 给 Agent 提供**做的能力**（具体操作）。

**难度**：⭐⭐⭐ | **考察点**：Skill 与 MCP 协同设计

---

## Q8.6 为什么 Anthropic 有了 MCP 还要做 Skill？

**参考答案**：

这是一个面试高频追问题。核心区别在于**关注点分离**：

| 维度 | MCP | Skill |
|------|-----|-------|
| 管什么 | "能不能连"（连接协议） | "怎么做事"（执行流程） |
| 抽象层级 | 基础设施层：规范 Agent 与工具的通信 | 业务能力层：封装工作流与最佳实践 |
| 实现形式 | 独立服务进程（Server） | Markdown 文件（指令模板） |
| 适用对象 | 工具/数据源提供方 | 任务/流程设计方 |

**类比理解**：MCP 是"USB 协议"（标准化接口），Skill 是"打印机使用说明书"（谁在什么情况下怎么用）。

两者互补而非替代——MCP 提供标准化的连接能力，Skill 将这些能力编排成可复用的工作流。

**难度**：⭐⭐⭐ | **考察点**：架构思维深度

---

## Q8.7 MCP Server 的安全边界如何设计？

**参考答案**：

**危险操作确认机制**：
```python
if name == "delete_records":
    count = await count_records(arguments["filter"])
    return CallToolResult(
        content=[TextContent(
            type="text",
            text=f"即将删除 {count} 条记录，此操作不可逆。请确认后重新调用并传入 confirmed=true。"
        )]
    )
```

**输入防注入**：
```python
def sanitize_input(user_input: str) -> str:
    """清理用户输入，防止提示词注入"""
    dangerous_patterns = ["<system>", "</system>", "[INST]", "[/INST]"]
    for pattern in dangerous_patterns:
        user_input = user_input.replace(pattern, "")
    return user_input
```

**安全层级**：
1. 网络层：Skill 服务不暴露公网
2. 认证层：API Key 认证，通过环境变量注入
3. 输入层：参数校验 + 防注入
4. 操作层：危险操作需二次确认
5. 日志层：不打印敏感信息（API Key、密码、用户隐私）

**难度**：⭐⭐ | **考察点**：安全意识

---

## Q8.8 MCP 配多了会有什么问题？如何解决？

**参考答案**：

**问题**：过多的 MCP Server 会消耗大量上下文窗口（每个 Server 的 Tool 定义都会注入）。

**解决方案**：
1. **按项目启用/禁用**：在 `.mcp.json` 中使用 `disabledMcpServers` 按项目管理
2. **Tool Retrieval**：当 Tool 数量超过 100+ 时，对 Tool 描述做向量化，通过语义检索召回 Top-K 相关 Tool
3. **渐进式披露**：Skill 按需加载，不把全部能力注入上下文
4. **精简 Tool 描述**：description 只写必要的触发信息，不写冗长文档

**难度**：⭐⭐ | **考察点**：规模化管理

---

> 参考来源：笔记 `01-MCP协议与Skill集成指南.md`，`02-跨平台Skill机制对比.md`
