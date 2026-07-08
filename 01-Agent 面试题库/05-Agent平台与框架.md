# 05 — Agent 平台与框架

---

## ★☆☆ 基础概念

### Q1: 常见的 AI Agent 框架有哪些？

| 框架 | 语言 | 特点 |
|------|------|------|
| LangChain | Python/JS | 最成熟的 LLM 应用框架，工具链完善 |
| LangGraph | Python | 有状态图 Agent，支持复杂流程编排 |
| AutoGen | Python | 微软出品，多 Agent 对话式协作 |
| CrewAI | Python | 角色驱动的多 Agent 编排 |
| MetaGPT | Python | 模拟软件公司多角色协作 |
| LlamaIndex | Python | 数据 Agent 和 RAG 见长 |
| Dify | 低代码 | 可视化编排，适合非开发人员 |
| Coze | 低代码 | 字节跳动出品，集成工作流和插件 |

### Q2: AI Agent 平台有哪几种类型？

| 类型 | 代表 | 特点 |
|------|------|------|
| AI 编程工具 | Claude Code, Cursor, Cline | 内建 Agent 编排，面向开发者 |
| 低代码平台 | Dify, Coze | 可视化搭建，面向业务人员 |
| 开发框架 | LangChain, AutoGen, CrewAI | 完全可编程，面向工程师 |
| 自研框架 | 公司自建 | 可定制，但维护成本高 |

### Q3: 什么是 MCP（Model Context Protocol）？

MCP 是 Anthropic 提出的模型上下文协议，定义了 AI 模型与外部工具/数据源的标准接口：

- 提供统一的 Server-Client 架构
- Server 暴露工具（Tools）、资源（Resources）、提示模板（Prompts）
- Client（AI 工具）可以挂载多个 MCP Server
- 类似 USB 协议之于外设——标准化接口让不同工具都能接入

### Q4: 什么是 Function Calling / Tool Use？

Function Calling 是 LLM 调用外部函数的能力——模型根据用户意图，输出结构化的函数调用请求（函数名+参数 JSON），由外部系统执行后返回结果。

关键流程：
```
用户输入 → LLM 决策是否调用工具
  → 是：输出函数名+参数 JSON → 执行函数 → 结果注入上下文 → LLM 继续推理
  → 否：直接生成回答
```

---

## ★★☆ 应用场景

### Q5: Claude Code 的子 Agent 类型有哪些？

| 类型 | 职责 | 输出形态 |
|------|------|---------|
| Explore | 跨目录搜索、理解代码结构、定位实现 | 文件清单 + 摘要 |
| Plan | 拆解复杂任务、输出实施计划 | 步骤化任务清单 |
| Execute | 编辑文件、运行命令、做多步操作 | 代码改动与运行结果 |

派发时可指定的参数：任务描述（prompt）、角色类型、是否后台执行、工作区隔离、工具白名单。

### Q6: 公司自建的大模型环境如何接入 AI 编程工具？

总体决策流程：
```
公司大模型平台
  ├─ 有没有暴露 HTTP API？
  │   ├─ 有 → 看协议类型
  │   └─ 没有 → 只能用网页端/IDE 插件，自动化受限
  ├─ API 协议是什么？
  │   ├─ OpenAI 兼容（/v1/chat/completions）→ 几乎所有工具都能接
  │   ├─ Anthropic 兼容（/v1/messages）→ Claude Code/Aider 可接
  │   ├─ 私有协议 → 需 OpenAI-Proxy 网关翻译
  │   └─ 仅 SDK（无 HTTP）→ 需包一层 HTTP 服务
  └─ 鉴权方式？
      ├─ 永久 API Key → 直接配置
      ├─ Web Token/短期凭证 → 需自动化刷新
      └─ 无鉴权（内网信任）→ 直接可用
```

### Q7: 如何通过 IDE 插件反推公司的 API 地址？

1. 用 jadx/IDEA 反编译插件的 .jar/.vsix 文件
2. 搜索关键词：`http`、`/v1/chat`、`completions`、`baseUrl`、`endpoint`
3. 找到内网接口地址与协议形式

注意：反编译前确认公司信息安全政策。

### Q8: 什么是 Token 类型？Token 过期怎么处理？

| 类型 | 有效期 | 体验 |
|------|--------|------|
| 永久 API Key | 长期 | 顺畅 |
| Web Token | 几小时 | 过期就报错 |

处理短期 Token 的优先级：
> 永久 API Key > 内网 OpenAI 兼容接口 > 自动化刷新的 Web Token > 手动刷 Web Token

自动化方案：MCP chrome-devtools/Playwright 自动登录 → 抓取新 Token → 写入配置 → 继续工作。

### Q9: Dify 和 Coze 这类低代码 Agent 平台的核心能力？

1. **可视化工作流编排**：拖拽式搭建 Agent 流程
2. **模型接入**：支持多种 LLM（闭源/开源）
3. **知识库集成**：内建 RAG 能力
4. **插件/工具生态**：预设工具 + 自定义 API
5. **Agent 模式**：近期支持了 ReAct/Function Calling 循环
6. **发布与监控**：一键部署 + 用量监控

适用场景：非开发人员搭建 Agent，或快速原型验证。

---

## ★★★ 架构设计

### Q10: 如何设计一个 OpenAI-Proxy 网关来适配公司私有协议？

```
[Cursor / Claude Code / Aider]
        ↓ OpenAI 格式请求
[本地代理服务（FastAPI / Express）]
  ├─ 接收 OpenAI 格式请求
  ├─ 翻译成公司私有协议
  ├─ 调用公司大模型 API
  └─ 将响应翻译回 OpenAI 格式
        ↓ OpenAI 格式响应
[下游工具]
```

开源参考：one-api、openai-forward、LiteLLM 等"多协议聚合代理"。

### Q11: 对比 LangChain、LangGraph、AutoGen、CrewAI 的适用场景

| 框架 | 核心抽象 | 适用场景 |
|------|---------|---------|
| LangChain | Chain/Tool/Agent | 快速搭建 LLM 应用原型 |
| LangGraph | StateGraph + Node | 有状态的复杂 Agent 流程 |
| AutoGen | ConversableAgent | 多 Agent 对话式协作 |
| CrewAI | Role/Task/Crew | 角色驱动的任务分派 |

选择建议：简单链式 → LangChain；复杂状态流 → LangGraph；对话式多 Agent → AutoGen；角色分工 → CrewAI。

### Q12: MCP Server 如何设计和挂载？

MCP Server 暴露三种能力：
1. **Tools**：可调用的函数（如搜索文件、读取数据库）
2. **Resources**：可读取的数据（如文档、配置）
3. **Prompts**：预定义的提示词模板

挂载方式：在工具的 MCP 配置文件中指定 Server 的启动命令和参数。

架构要点：
- 一个工具可以挂载多个 MCP Server
- Server 可以是本地进程（stdio）或远程服务（HTTP/SSE）
- 权限按 Server 粒度控制
