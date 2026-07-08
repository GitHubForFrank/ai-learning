# 08 - 多 Agent 与进阶

> 高频题 ★★★ | 进阶题 ★★☆ | 基础题 ★☆☆

---

## Q1. MCP 与 A2A（Agent-to-Agent）协议是什么关系？★★★

**答：**

| 维度 | MCP | A2A |
|------|-----|-----|
| **全称** | Model Context Protocol | Agent-to-Agent Protocol |
| **定位** | AI 与**工具/数据**的连接 | AI 与 **AI**（Agent 间）的连接 |
| **类比** | "AI 世界的 USB"——让 LLM 能使用各种工具 | "AI 世界的 HTTP"——让 Agent 之间能通信协作 |
| **通信方向** | Client ↔ Server（工具调用） | Agent ↔ Agent（任务委托/协作） |
| **谁提出的** | Anhtropic（2024.11） | Google（2025） |
| **典型使用** | LLM 调用数据库、API、文件系统 | 多个 AI Agent 协同完成复杂任务 |

**面试话术**：MCP 打通"AI 与物"的连接，让数据像自来水一样接入模型；A2A 打通"AI 与 AI"的连接，构建自协同的智能网络。未来的 AI 应用通过这两种标准化协议交织成智能网络。

**互补关系**：一个 A2A Agent 可能通过 MCP 获取工具能力，然后通过 A2A 与其他 Agent 协作——两者不是替代关系，而是不同层次的协议。

---

## Q2. 在多 Agent 架构中，MCP 扮演什么角色？★★★

**答：**

```
                    ┌──────────────────┐
                    │   编排 Agent      │
                    │  (LangGraph/CrewAI) │
                    └────────┬─────────┘
                             │ A2A 协议
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
        ┌──────────┐  ┌──────────┐  ┌──────────┐
        │ Agent A   │  │ Agent B   │  │ Agent C   │
        │ (研究员)  │  │ (开发者)  │  │ (审查者)  │
        └────┬─────┘  └────┬─────┘  └────┬─────┘
             │ MCP          │ MCP          │ MCP
        ┌────┴────┐   ┌────┴────┐   ┌────┴────┐
        │ Web搜索  │   │ GitHub  │   │ 代码审查 │
        │ 数据库   │   │ 文件系统 │   │ 安全扫描 │
        └─────────┘   └─────────┘   └─────────┘
```

**MCP 的角色：**
1. **能力提供层**：为每个 Agent 提供执行具体任务所需的工具
2. **标准化接口**：无论 Agent 使用什么框架，都通过统一的 MCP 协议访问工具
3. **安全边界**：通过 MCP 的权限控制限制每个 Agent 能做什么
4. **可组合性**：不同 Agent 可以共享同一批 MCP Server，或各自挂载不同的 Server

---

## Q3. LangGraph / CrewAI / AutoGen 等框架如何与 MCP 集成？★★★

**答：**

**通用集成模式：**

```python
# 1. 启动 MCP Client 连接 Server
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

async def create_mcp_tools():
    server_params = StdioServerParameters(command="python", args=["-m", "my_server"])
    read, write = await stdio_client(server_params).__aenter__()
    session = await ClientSession(read, write).__aenter__()
    await session.initialize()
    
    tools = await session.list_tools()
    return session, tools.tools

# 2. 转换 MCP Tools 到框架格式
def mcp_to_langchain_tool(mcp_tool, session):
    """将 MCP Tool 包装为 LangChain Tool"""
    from langchain.tools import StructuredTool
    
    async def _run(**kwargs):
        result = await session.call_tool(mcp_tool.name, kwargs)
        return result.content[0].text
    
    return StructuredTool(
        name=f"mcp__{mcp_tool.name}",
        description=mcp_tool.description,
        args_schema=mcp_tool.inputSchema,  # 需要转换为 Pydantic model
        func=_run,
        coroutine=_run,
    )

# 3. 用于 Agent
langchain_tools = [mcp_to_langchain_tool(t, session) for t in mcp_tools]
agent = create_react_agent(llm, langchain_tools)
```

**各框架的集成方式：**

| 框架 | 集成方式 | 成熟度 |
|------|---------|--------|
| **LangChain** | `langchain-mcp` 适配器包 | 高 |
| **LangGraph** | 通过 LangChain Tool 间接使用 | 高 |
| **CrewAI** | 自定义 Tool 包装 | 中 |
| **AutoGen** | MCP Tool 适配器 | 中 |
| **Semantic Kernel** | 原生支持（Microsoft） | 中 |

---

## Q4. 在 Agent 编排场景中，如何分配不同的 MCP Tool 给不同的 Subagent？★★★

**答：**

```python
# 定义 Subagent 的角色和 Tool 白名单
SUBAGENT_CONFIGS = {
    "researcher": {
        "description": "负责信息搜索和数据分析",
        "tools": [
            "mcp__brave__web_search",
            "mcp__postgres__run_query",
            "mcp__fetch__fetch_content",
        ]
    },
    "developer": {
        "description": "负责代码编写和修改",
        "tools": [
            "mcp__github__search_code",
            "mcp__github__create_pr",
            "mcp__filesystem__write_file",
        ]
    },
    "reviewer": {
        "description": "负责代码审查和安全检查",
        "tools": [
            "mcp__github__get_file_contents",
            "mcp__github__create_review",
            "mcp__security__scan_code",
        ]
    },
}

# 创建 Subagent 时的 Tool 过滤
def create_subagent(name: str, all_tools: list):
    allowed = SUBAGENT_CONFIGS[name]["tools"]
    filtered_tools = [t for t in all_tools if t.name in allowed]
    return Subagent(
        name=name,
        description=SUBAGENT_CONFIGS[name]["description"],
        tools=filtered_tools,
    )
```

**设计原则：**
- **最小权限**：每个 Subagent 只获得完成任务所需的最少 Tool
- **职责分离**：负责数据的 Agent 不应有写权限
- **审计**：记录哪个 Subagent 调用了哪个 Tool

---

## Q5. 如何利用 MCP 实现复杂多步骤任务的自动化？★★★

**答：**

**场景**：用户说"帮我收集过去一周的 Bug 数据，生成数据分析报告，创建 GitHub Issue 总结"

```
步骤 1: [Agent 分析任务] → 拆解为子任务

步骤 2: [Subagent: 数据收集]
  → mcp__postgres__run_query("SELECT * FROM bugs WHERE created_at > ...")
  → mcp__github__search_issues("label:bug created:>2025-06-01")

步骤 3: [Subagent: 数据分析]
  → 分析收集到的数据
  → 生成统计图表和趋势分析

步骤 4: [Subagent: 报告生成]
  → 根据分析结果生成 Markdown 报告

步骤 5: [Subagent: Issue 创建]
  → mcp__github__create_issue(title="周报", body=report)
  → 返回 Issue 链接
```

**关键设计**：
- MCP 提供了标准化的工具接口，使任务编排框架可以自由组合 Tools
- 每个 Subagent 只负责一个子任务，通过 A2A 协议协调
- 流程中的工具调用、数据传递、错误处理都由编排层管理

---

## Q6. 长耗时 MCP 任务（如模型训练、大数据导出）如何设计？★★★

**答：**

**异步设计模式：**

```
┌────────┐    1. tools/call        ┌────────┐
│ Client  │ ──────────────────────→ │ Server │
│         │ ←────────────────────── │        │
│         │  2. 立即返回 task_id    │        │
│         │                         │        │
│         │  3. 定期轮询进度         │        │
│         │ ──────────────────────→ │        │
│         │ ←────────────────────── │        │
│         │  notifications/progress │        │
│         │                         │        │
│         │  4. 任务完成通知         │        │
│         │ ←────────────────────── │        │
└────────┘                         └────────┘
```

**Server 实现：**
```python
import uuid
import asyncio

tasks: dict[str, dict] = {}

@mcp.tool()
async def start_long_task(data: str) -> str:
    """启动一个长耗时任务，立即返回任务 ID。"""
    task_id = str(uuid.uuid4())
    tasks[task_id] = {"status": "running", "progress": 0}
    
    # 异步执行（不阻塞 Tool 响应）
    asyncio.create_task(_run_task(task_id, data))
    
    return json.dumps({
        "task_id": task_id,
        "status": "running",
        "message": "任务已启动，使用 check_task_status 查询进度"
    })

@mcp.tool()
def check_task_status(task_id: str) -> str:
    """查询任务进度"""
    task = tasks.get(task_id)
    if not task:
        return f"任务 {task_id} 不存在"
    return json.dumps(task)

async def _run_task(task_id: str, data: str):
    """实际执行任务（后台协程）"""
    for i in range(10):
        await asyncio.sleep(1)
        tasks[task_id]["progress"] = (i + 1) * 10
    tasks[task_id]["status"] = "completed"
    tasks[task_id]["result"] = f"处理完成: {data}"
```

**关键点：**
- Tool 立即返回任务 ID，不阻塞连接
- 进度通过 `notifications/progress` 或轮询 `check_task_status` 获取
- 结果通过查询获取（pull 模式）或 Resource 订阅推送（push 模式）

---

## Q7. MCP 的上下文预算（Context Budget）如何管理？★★☆

**答：**

**上下文消耗分析（以 Claude Code 为例）：**

| 消耗项 | 估算 Token | 说明 |
|--------|-----------|------|
| System Prompt | ~2000-5000 | Host 的基础指令 |
| Tool 定义（50 个） | ~3000-8000 | 每个 Tool 的 name/description/inputSchema |
| Resource 列表 | ~500-2000 | URI 和描述 |
| 对话历史 | 变数 | 用户消息 + LLM 回复 + Tool 调用结果 |
| Tool 返回结果 | 变数 | 取决于结果大小 |

**管理策略：**
1. **精简 Tool 数量**：只挂载必需的 Server（≤5 个），确保总 Tool ≤50
2. **按需挂载**：不同项目用不同的 `.mcp.json`，不全局启用所有 Server
3. **控制返回值大小**：使用分页、Resource 引用、字段级读取
4. **定期检查**：`/context` 命令查看 MCP 相关 Token 消耗
5. **及时取消不再需要的 Resource 订阅**

---

## Q8. 如何优化 MCP Server 的性能？★★☆

**答：**

**1. 减少初始化开销：**
- Stdio：用启动快的语言（Python/Node/Go > Java/C#）
- HTTP：预热连接池，复用 HTTP 连接

**2. Tool 调用优化：**
- 缓存频繁查询的结果（带 TTL）
- 数据库查询优化（索引、查询计划）
- 并行执行无依赖的 Tool

**3. 传输优化：**
- Stdio：保持 Server 进程常驻（HTTP 模式）
- HTTP：启用 HTTP/2 多路复用、keep-alive

**4. 协议优化：**
- 批处理请求（Message Batching）
- 不需要的能力不在 capabilities 中声明（减少协商开销）

**5. 语言特定优化：**
| 语言 | 优化方向 |
|------|---------|
| Python | 异步 I/O、连接池、避免 GIL 瓶颈 |
| Node.js | Cluster 模式、stream 处理大数据 |
| Go | Goroutine 池、零拷贝 |
| Rust | 编译优化（LTO、opt-level=z） |

---

## Q9. MCP 在边缘计算 / 移动端有什么应用前景？★★☆

**答：**

**应用场景：**
- 手机上的 AI 助手通过本地 MCP Server 访问本地文件、相册、日历
- IoT 设备通过 MCP 暴露传感器数据给边缘 AI Agent
- 智能家居：MCP Server 作为设备能力的标准化接口

**技术挑战：**
- **资源受限**：嵌入式设备内存有限，需要轻量级 MCP 实现
- **离线优先**：没有网络时如何工作（Stdio 模式下天然解决）
- **安全**：设备上的敏感数据（健康数据、位置）需要严格的权限控制

**现状**：Swift SDK 正在发展，面向 Apple 生态的移动端 MCP 尚处于早期阶段。

---

## Q10. MCP 与 gRPC / GraphQL 等其他接口协议相比，优势在哪？★★☆

**答：**

| 维度 | MCP (JSON-RPC) | gRPC | GraphQL |
|------|---------------|------|---------|
| **设计目标** | AI 与工具的标准化通信 | 微服务间高性能通信 | 前端灵活查询后端数据 |
| **目标用户** | AI 模型（LLM） | 人类开发者 | 人类前端开发者 |
| **数据格式** | JSON（人类+LLM可读） | Protobuf（二进制） | JSON |
| **能力发现** | 内置（`tools/list`） | 需要 proto 文件 / gRPC Reflection | 内置（Schema Introspection） |
| **流式传输** | SSE（Streamable HTTP） | 原生双向流 | Subscription |
| **AI 原生** | 是（面向 LLM 的描述） | 否 | 否 |

**MCP 的核心差异**：面向 LLM 设计的描述语言和动态发现机制，这是 gRPC 和 GraphQL 不具备的。

---

## Q11. MCP Server 的无服务器（Serverless）部署可行吗？★★☆

**答：**

**适合 Serverless 的场景：**
- Streamable HTTP 模式 + 短任务（处理时间 < 函数超时限制）
- 无状态的 Tool（不依赖 Session 持久化）
- 低频率调用的 Tool（按需启动，节省成本）

**不适合 Serverless 的场景：**
- Stdio 模式（需要常驻进程）
- 长耗时任务（超过 Lambda 15 分钟限制）
- 需要 Resource 订阅（需要长连接推送）
- 高频调用（冷启动延迟累积）

**实践建议：**
- 用 Streamable HTTP + SSO 认证
- 将状态（Session、订阅）存储到 Redis/DynamoDB
- 简单查询类 Tool 的 Serverless 部署最可行

---

## Q12. 如何设计一个面向数十个团队的 MCP 平台？★★★

**答：**

**架构设计：**

```
                         ┌──────────────────┐
                         │   API Gateway     │
                         │ (认证/限流/路由)    │
                         └────────┬─────────┘
                                  │
                    ┌─────────────┼─────────────┐
                    ▼             ▼             ▼
             ┌──────────┐ ┌──────────┐ ┌──────────┐
             │ MCP Server│ │ MCP Server│ │ MCP Server│
             │  (数据库)  │ │  (GitHub) │ │  (自定义)  │
             └──────────┘ └──────────┘ └──────────┘
                    │             │             │
                    ▼             ▼             ▼
              ┌─────────────────────────────────────┐
              │         企业服务层                    │
              │  (DB / API / 消息队列 / 存储)        │
              └─────────────────────────────────────┘
```

**关键设计：**
1. **多租户隔离**：每个团队有独立的 MCP Session 和权限配置
2. **统一认证**：SSO + OAuth 2.1，RBAC 角色（团队管理员 / 开发者 / 只读）
3. **API 网关**：统一入口、限流、审计日志集中收集
4. **Server 市场**：团队可以发布和共享自定义 MCP Server
5. **监控与告警**：每个 Server 的调用量、延迟、错误率实时监控

---

## Q13. MCP 的未来发展趋势是什么？★★★

**答：**

**1. 生态爆发**
- 垂直领域专用 MCP Server（JIRA 项目管理、AWS 运维、医疗文献查询、金融数据）
- MCP Server 市场/注册中心（类似 npm 或 Docker Hub）

**2. 标准化加速**
- MCP 成为连接 AI 应用与外部世界的"USB 标准"
- A2A + MCP 双协议成为 Agent 开发的标配

**3. 端侧部署**
- 轻量级 MCP 运行在手机、IoT 设备上
- 本地 AI + 本地 MCP = 隐私保护 + 离线可用

**4. 复杂 Agent 场景**
- 多 Agent 编排 + 多 MCP Server 协同
- Tool 的输出自动成为下一个 Tool 的输入（链式调用）

**5. 安全与治理**
- MCP Server 安全评级体系（类似 CVE）
- 企业级的 MCP 治理平台（审计、合规、成本管理）

**6. 协议演进**
- 二进制传输支持（降低大消息序列化开销）
- WebSocket 传输（双向实时通信）
- 更丰富的多媒体类型支持

---

## Q14. 如何从零搭建一个企业的 MCP 基础设施？★★☆

**答：**

**分阶段路线图：**

**阶段 1：试点（1-2 周）**
- 选择 1-2 个常用工具（数据库查询、GitHub 操作）
- 使用官方 Server + Stdio 模式
- 在团队中试用以收集反馈

**阶段 2：自定义（1 个月）**
- 开发 1-2 个企业内部 API 的 MCP Server
- 使用双传输模式（Stdio 开发 + HTTP 部署）
- 建立 Server 开发规范和安全检查清单

**阶段 3：平台化（3 个月）**
- 部署中心化的 MCP 网关（统一认证、限流、审计）
- 建立内部 MCP Server 仓库/目录
- CI/CD 集成：自动测试、安全扫描、自动部署

**阶段 4：生态化（6 个月）**
- 推广至更多团队，每个团队可贡献自己的 MCP Server
- 建立 Server 评价/评分体系
- 与 Agent 编排框架集成

---

## Q15. 什么样的场景不适合引入 MCP？★☆☆

**答：**

1. **纯推理任务**：不需要任何外部工具/数据，LLM 自身可以完成
2. **单供应商绑定**：如果确定只用一个闭源 AI 工具且永不迁移
3. **极致低延迟**：微秒级延迟要求下 JSON-RPC 的序列化开销不可忽略
4. **一次性脚本**：开发 MCP Server 的成本 > 直接用 API
5. **极简单的工具**：只有 1-2 个简单函数，用 Function Calling 更直接
6. **没有标准化需求**：工具只在一个应用中使用，不需要跨平台复用

**面试话术**："MCP 不是银弹——如果不需要标准化、不需要跨工具复用、不需要动态发现，直接用 Function Calling 更简单。"

---

## Q16. 如何评估一个 MCP Server 是否为"生产就绪"？★★☆

**答：**

**生产就绪检查清单：**

```
□ 功能完整
  - Tool 描述面向 LLM，清晰说明何时用/何时不用/副作用
  - inputSchema 有 strict 校验
  - 错误消息对 LLM 友好
  - 支持分页（如果可能返回大数据集）

□ 安全合规
  - 无硬编码密钥
  - 路径/SQL/命令注入防护
  - 破坏性操作有二次确认
  - 审计日志可用
  - 非 root 用户运行

□ 运维就绪
  - 结构化日志到 stderr
  - 健康检查端点（HTTP 模式）
  - 优雅关闭和崩溃恢复
  - 资源限制（超时、限流）

□ 质量保障
  - 通过 MCP Inspector 验证
  - 业务逻辑有单元测试
  - 协议交互有集成测试
  - 文档完整（README + CHANGELOG + 环境变量说明）
```

---

## Q17. 在多 Agent 场景下，如何避免不同 Agent 调用 MCP Tool 时的冲突？★★☆

**答：**

**冲突类型和解决方案：**

| 冲突类型 | 示例 | 解决方案 |
|---------|------|---------|
| **资源锁** | 两个 Agent 同时修改同一文件 | 乐观锁（ETag/版本号）+ 冲突检测 |
| **状态不一致** | Agent A 查询时 Agent B 正在修改 | 读已提交（Read Committed）隔离级别 |
| **重复操作** | 两个 Agent 都创建了相同的 Issue | 幂等键 + 业务去重 |
| **配额耗尽** | 多个 Agent 短时间内大量调用 API | 全局限流 + 优先级队列 |

**设计建议：**
- Tool 设计为幂等的（相同输入 → 相同结果）
- 使用 `idempotency_key` 防止重复创建
- 关键资源加锁（分布式锁）
- 设置每个 Agent 的调用配额上限

---

## Q18. 描述你理想中的 MCP + A2A 协同架构。★★★

**答：**

```
用户输入："帮我做一个市场竞品分析报告"

                    ┌──────────────────┐
                    │   编排 Agent      │  ← 理解任务，制定计划
                    │  (Orchestrator)   │
                    └────────┬─────────┘
                             │ A2A (任务委托)
         ┌───────────────────┼───────────────────┐
         ▼                   ▼                   ▼
   ┌──────────┐       ┌──────────┐       ┌──────────┐
   │ 研究 Agent│       │ 分析 Agent│       │ 写作 Agent│
   └────┬─────┘       └────┬─────┘       └────┬─────┘
        │ MCP               │ MCP              │ MCP
   ┌────┴────┐        ┌────┴────┐        ┌────┴────┐
   │Web搜索   │        │数据库查询│        │文档生成  │
   │新闻抓取  │        │图表生成  │        │格式化输出│
   │竞品网站  │        │统计分析  │        │文件写入  │
   └─────────┘        └─────────┘        └─────────┘

流程：
1. 编排 Agent 分析用户需求，分解为：研究 → 分析 → 写作
2. 研究 Agent 通过 MCP 调用 Web 搜索、新闻抓取，收集竞品信息
3. 分析 Agent 通过 MCP 对数据进行分析、生成图表
4. 写作 Agent 通过 MCP 将分析结果格式化为报告并写入文件
5. 编排 Agent 汇总结果，返回给用户

关键设计：
- MCP 提供工具层能力（每个 Agent 的"手脚"）
- A2A 提供协作层能力（Agent 之间的"语言"）
- 编排 Agent 通过 A2A 管理任务分配、进度追踪、质量审核
```

---

## 自检清单

- [ ] 能区分 MCP 和 A2A 的定位和关系
- [ ] 理解 MCP 在多 Agent 架构中的角色
- [ ] 知道如何将 MCP Tool 集成到 LangGraph/CrewAI/AutoGen
- [ ] 理解 Subagent 的 Tool 白名单设计
- [ ] 知道长耗时任务的异步设计模式
- [ ] 了解上下文预算管理和性能优化策略
- [ ] 了解 Serverless 部署的可行性
- [ ] 能描述企业级 MCP 平台的架构设计
- [ ] 能判断什么场景不适合引入 MCP
- [ ] 能评估 MCP Server 是否"生产就绪"
