# MCP 协议概述

> 定位：基础入门

---

## 1. 什么是 MCP

**MCP（Model Context Protocol，模型上下文协议）** 是 Anthropic 于 2024 年 11 月开源的通信协议，用于规范 AI 模型（Host/Client）与外部能力提供方（Server）之间的数据与工具交换方式。

可以把 MCP 理解为：
- **对 AI 应用开发者**：一次集成，通用复用的"能力总线"
- **对工具/数据提供方**：一次实现 Server，即可被所有兼容客户端调用
- **对生态**：AI 领域的"USB 接口"——标准化插拔

### 1.1 一句话定义

> MCP 是 AI Agent 与外部世界之间的标准化通信协议，基于 JSON-RPC 2.0，定义了工具调用、资源读取、提示词模板三类交互范式。

---

## 2. MCP 要解决的问题

### 2.1 N×M 集成困境

没有 MCP 之前，AI 应用接入外部系统面临组合爆炸：

```
M 个 AI 应用 × N 个工具 = M×N 个定制集成

Claude Desktop ──► GitHub（定制 A）
Claude Desktop ──► Postgres（定制 B）
Claude Desktop ──► Slack（定制 C）
Cursor         ──► GitHub（定制 D，与 A 逻辑重复）
Cursor         ──► Postgres（定制 E，与 B 逻辑重复）
...
```

MCP 将其简化为 M+N：

```
每个 AI 应用实现一次 MCP Client
每个工具实现一次 MCP Server
任意组合即插即用

Claude Desktop ┐                     ┌── GitHub Server
Cursor         ├──── MCP 协议 ───────┤── Postgres Server
Continue       │                     ├── Slack Server
Zed            ┘                     └── Filesystem Server
```

### 2.2 能力孤岛

LLM 被训练的知识有截止日期，且无法直接访问用户的本地文件、内网系统、实时数据。MCP 通过标准化的 Server 机制让模型可以按需拉取实时上下文、执行本地操作。

### 2.3 上下文窗口浪费

把所有可能的工具说明、资源内容都塞进 System Prompt 代价高昂。MCP 支持按需发现（list_tools）、按需读取（read_resource），减少无效 Token 消耗。

---

## 3. MCP 架构模型

MCP 采用三层架构：

```
┌────────────────────────────────────────────────┐
│                   Host                         │
│  （AI 应用本体，例如 Claude Desktop / Cursor） │
│  ┌──────────────────────────────────────────┐ │
│  │              MCP Client                  │ │
│  │  （每个 Server 一个 Client 实例）        │ │
│  └──────────────────────────────────────────┘ │
└───────────────┬────────────────────────────────┘
                │  JSON-RPC over Stdio/HTTP
                ▼
┌────────────────────────────────────────────────┐
│                MCP Server                      │
│  （独立进程 / 远程服务）                       │
│  ┌──────┐  ┌──────────┐  ┌────────┐            │
│  │Tools │  │Resources │  │Prompts │            │
│  └──────┘  └──────────┘  └────────┘            │
└────────────────────────────────────────────────┘
```

| 角色 | 职责 |
|------|------|
| **Host** | 管理 LLM 会话、统一用户体验、协调多个 Client |
| **Client** | 维持单个 Server 的连接、转发消息、处理生命周期 |
| **Server** | 实现具体能力（Tools/Resources/Prompts），对 Host 不可见进程边界 |

**关键特性**：一个 Host 可连接多个 Server；Client 与 Server 始终 1:1 绑定。

---

## 4. MCP 与相关概念的关系

### 4.1 与 Function Calling

| 维度 | Function Calling | MCP |
|------|------------------|-----|
| **定义层级** | LLM 厂商私有接口（OpenAI、Anthropic 各有差异） | 开放标准协议 |
| **工具注册** | 每次请求传入 tools 数组 | Server 启动时声明，Client 发现 |
| **执行位置** | 应用代码内执行 | Server 进程内执行 |
| **可复用性** | 绑定单个应用 | 跨 Host 复用 |
| **资源/模板** | 无原生支持 | Resources + Prompts 一等公民 |

**关系**：Function Calling 是底层机制，MCP 是对它的**抽象与标准化**。Host 仍以 Function Calling 的方式把 MCP Tools 传给 LLM，但对开发者屏蔽了厂商差异。

### 4.2 与 Agent Skill

| 维度 | Agent Skill | MCP Server |
|------|-------------|------------|
| **本质** | 指令模板（Prompt 的模块化） | 能力提供者（代码实现） |
| **回答什么** | "应该怎么做" | "用什么去做" |
| **实现形式** | Markdown 文件 | 独立服务进程 |
| **触发时机** | 用户显式/被动触发 | LLM 按需调用 Tool |

**协同模式**：Skill 在 prompt 中指导 LLM 调用 MCP 提供的 Tool，二者互补而非替代。

### 4.3 与传统 RESTful API

| 维度 | REST API | MCP Server |
|------|----------|------------|
| **面向对象** | 前端/后端工程师 | AI 模型 |
| **文档形式** | OpenAPI/Swagger | inputSchema + description |
| **调用发起方** | 人类或程序 | LLM（根据 description 推理） |
| **错误反馈** | HTTP 状态码 | 文本化错误 + isError 标记 |

**要点**：MCP 的 description 必须针对 LLM 写——描述语义、使用时机、边界条件，而非仅列出参数。

---

## 5. 核心能力原语速览

MCP Server 向 Host 暴露三类原语（下一篇详细展开）：

| 原语 | 谁主动 | 是否有副作用 | 类比 |
|------|--------|--------------|------|
| **Tools** | LLM 主动调 | 可能有 | Function |
| **Resources** | Host/用户请求读 | 无（只读） | 文件 |
| **Prompts** | 用户触发 | 无 | 模板 |

---

## 6. MCP 的典型工作流

```
1. Host 启动 → 根据配置连接所有声明的 MCP Server
2. 握手 initialize → 协商协议版本与 capabilities
3. Client 发 list_tools / list_resources / list_prompts → 发现能力
4. 用户输入 → LLM 规划 → 产生 Tool Call
5. Host 路由 Tool Call 到对应 Server → tools/call
6. Server 执行业务逻辑 → 返回结果
7. 结果回注入 LLM 上下文 → 继续推理或回复用户
```

---

## 7. MCP 带来的收益

| 收益 | 说明 |
|------|------|
| **解耦** | AI 应用与能力供应分离，各自独立演进 |
| **生态共享** | 一个高质量 Server 可被整个生态使用 |
| **权限边界清晰** | Server 进程天然隔离，便于沙盒化 |
| **可观测** | JSON-RPC 消息可记录、可重放、可审计 |
| **多语言友好** | 官方已提供 Python / TypeScript / Java / Kotlin / Go / C# SDK |

---

## 8. 什么时候不要用 MCP

- **纯推理任务**：不需要外部工具或数据时，MCP 徒增复杂度
- **单一厂商应用**：若只跑在自家 Host 上，原生 Function Calling 可能更轻
- **极致性能场景**：进程间通信与序列化会带来微秒级开销，延迟敏感路径需评估
- **一次性脚本**：短期 POC 代码直接调 SDK 更快

---

## 9. 学习路线建议

```
理解 MCP 概念（本文）
    ↓
掌握三类能力原语（下一篇）
    ↓
学习协议规范（JSON-RPC、传输层）
    ↓
动手写第一个 Server（Python 或 Node）
    ↓
在 Claude Code / Desktop 中挂载使用
    ↓
（进阶）Server 设计规范 + 安全最佳实践
    ↓
（进阶）Client 开发 + 自研 Host 集成
```

---

## 10. 参考来源

- 官方规范：https://modelcontextprotocol.io/
- 规范仓库：https://github.com/modelcontextprotocol/specification
- 官方 Server 集合：https://github.com/modelcontextprotocol/servers
