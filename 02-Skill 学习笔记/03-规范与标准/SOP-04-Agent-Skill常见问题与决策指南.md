# SOP-04: Agent Skill 常见问题与决策指南 (FAQ)

> 版本：1.0 | 适用范围：Agent Skill 架构设计与开发决策

---

## 目录

1. [部署模式：常驻服务 vs 按需调用](#1-部署模式常驻服务-vs-按需调用)
2. [语言选型：用什么语言开发 Agent Skill](#2-语言选型用什么语言开发-agent-skill)
3. [仓库结构：源代码与 Skill 定义放一起还是分开](#3-仓库结构源代码与-skill-定义放一起还是分开)
4. [协议选择：HTTP REST vs MCP vs Function Calling](#4-协议选择http-rest-vs-mcp-vs-function-calling)
5. [单一 Skill vs 聚合 Skill：拆分粒度如何决策](#5-单一-skill-vs-聚合-skill拆分粒度如何决策)
6. [状态管理：Skill 服务应该有状态还是无状态](#6-状态管理skill-服务应该有状态还是无状态)
7. [认证与安全：Skill 如何处理鉴权](#7-认证与安全skill-如何处理鉴权)
8. [超时与重试：调用链的容错设计](#8-超时与重试调用链的容错设计)
9. [本地工具函数 vs 独立服务：何时需要独立部署](#9-本地工具函数-vs-独立服务何时需要独立部署)
10. [流式响应：Skill 是否需要支持 Streaming](#10-流式响应skill-是否需要支持-streaming)
11. [版本管理：Skill 如何平滑升级](#11-版本管理skill-如何平滑升级)
12. [监控与可观测性：如何观测 Skill 的运行状态](#12-监控与可观测性如何观测-skill-的运行状态)
13. [纯提示词 Skill：没有脚本的 Skill 本质是什么](#13-纯提示词-skill没有脚本的-skill-本质是什么)

---

## 1. 部署模式：常驻服务 vs 按需调用

### 问题

Agent Skill 服务应该做成**持续运行的常驻服务**，还是**随调随起、用完即停的按需服务**？

### 分析

| 维度 | 常驻服务（Always-On） | 按需调用（On-Demand） |
|------|----------------------|----------------------|
| 响应延迟 | 低（毫秒级） | 高（冷启动可能秒级到分钟级） |
| 资源占用 | 持续占用内存/CPU | 不调用时零占用 |
| 成本 | 较高（7×24 运行） | 较低（按使用量计费） |
| 适合场景 | 高频调用、低延迟要求 | 低频调用、耗时任务 |
| 可扩展性 | 需要手动/自动伸缩 | 平台自动伸缩 | 
| 状态保持 | 可以在内存中缓存状态 | 每次调用独立，需外部存储 |
| 适合的技术 | Spring Boot, FastAPI, Express | Lambda, Cloud Functions, Serverless |

### 决策建议

**选择常驻服务，当：**
- 调用频率高（每分钟超过 10 次）
- 有严格的低延迟要求（< 500ms）
- 服务需要维护内存缓存（如加载 NLP 模型、连接池）
- 依赖的初始化成本高（如加载 ML 模型需要 30s+）

**选择按需调用，当：**
- 调用频率极低（每天几次到几十次）
- 任务本身耗时长（几分钟到几小时，如文档解析、批量处理）
- 希望降低基础设施成本
- 每次调用完全独立，无需维护状态

**推荐默认方案**：

> 大多数 Agent Skill 建议从**常驻服务**起步。原因是：Agent 调用 Skill 的场景通常对响应时间敏感，用户等待感知强；而且常驻服务的开发和调试体验更好。仅在明确知道调用极低频或有成本压力时，才考虑按需模式。

---

## 2. 语言选型：用什么语言开发 Agent Skill

### 问题

开发一个 Agent Skill 服务，应该选择哪种编程语言？

---

### 各语言横向对比

| 维度 | Python | Java | Node.js | Go |
|------|--------|------|---------|-----|
| AI/ML 生态 | ★★★★★ 最强 | ★★☆☆☆ 较弱 | ★★★☆☆ 一般 | ★★☆☆☆ 较弱 |
| 启动速度 | ★★★☆☆ 中等 | ★★☆☆☆ 慢（JVM 预热） | ★★★★☆ 快 | ★★★★★ 极快 |
| 运行时内存占用 | ★★★☆☆ 中等 | ★★☆☆☆ 较高 | ★★★☆☆ 中等 | ★★★★★ 极低 |
| 并发处理能力 | ★★★☆☆ 中等（asyncio） | ★★★★★ 极强（线程池） | ★★★★☆ 强（事件循环） | ★★★★★ 极强（goroutine） |
| 开发效率 | ★★★★★ 最高 | ★★★☆☆ 中等 | ★★★★☆ 高 | ★★★☆☆ 中等 |
| 类型安全 | ★★★☆☆ 可选（类型注解） | ★★★★★ 强（静态类型） | ★★☆☆☆ 弱（需 TS） | ★★★★★ 强（静态类型） |
| 企业级框架成熟度 | ★★★★☆ FastAPI/Django | ★★★★★ Spring Boot | ★★★★☆ Express/Fastify | ★★★☆☆ 相对轻量 |
| 团队普及度（国内） | ★★★★☆ 高 | ★★★★★ 极高 | ★★★☆☆ 中等 | ★★★☆☆ 中等 |

---

### Python 详细分析

**核心优势——AI/ML 生态是最大护城河**

Python 是 AI 领域事实上的第一语言。几乎所有 AI SDK、大模型客户端库、向量数据库客户端都优先提供 Python SDK，且通常比其他语言版本更完整、更新更及时：

```
# 以下库在 Python 中原生支持，Java/Go 需要绕路或等待移植
openai、anthropic、langchain、llama-index
sentence-transformers、faiss、chromadb
transformers（HuggingFace）、torch、numpy
```

**适合的 Skill 类型**：
- 调用 LLM API 的 Skill（文本生成、摘要、分类）
- 使用向量数据库的 RAG 类 Skill
- 需要加载本地 AI 模型的 Skill（Embedding、OCR、NLP）
- 数据处理类 Skill（pandas、numpy）
- 快速原型验证，后续可能用其他语言重写

**需要注意的短板**：
- GIL（全局解释器锁）限制真正的多线程 CPU 并行，但 IO 密集型场景（绝大多数 Skill 属于此类）用 `asyncio` 完全够用
- 冷启动比 Go 慢，但比 Java（JVM 预热）快
- 动态类型在大型项目中需要强制使用类型注解（`mypy`）来保证可维护性

---

### Java 详细分析

**核心优势——企业级稳定性与团队存量**

Java（Spring Boot）在国内企业后端团队中有极高的普及率，团队成员熟悉度高，现有基础设施（监控、日志、部署流水线）往往已经围绕 Java 体系建设完毕。

**适合的 Skill 类型**：
- 需要对接企业内部已有 Java 微服务体系的 Skill
- 高并发、高吞吐的 Skill（Thread Pool + Virtual Threads in Java 21）
- 需要强事务保证的业务逻辑 Skill（数据库操作、工作流）
- 团队以 Java 为主力语言，且 Skill 不涉及复杂 AI/ML 依赖

**需要注意的短板**：
- AI/ML 生态远不如 Python 丰富，调用外部 LLM API 需要通过 HTTP 客户端手动封装，或使用 `Spring AI` 等框架（仍在快速演进中，稳定性不及 Python SDK）
- JVM 启动时间和内存占用较高，不适合按需调用（Serverless）模式
- 开发同等功能的代码量通常比 Python 多 2-3 倍

---

### Node.js / TypeScript 详细分析

**适合的 Skill 类型**：
- 前端或全栈团队主导开发的 Skill
- 需要大量处理 JSON/HTTP 的轻量集成类 Skill
- 与 JavaScript 生态工具链（如 Puppeteer 做网页抓取）深度结合的 Skill

**主要问题**：
- CPU 密集型任务性能较差（单线程事件循环）
- 相比 Python，AI SDK 的功能完整性和更新及时性略逊
- 在纯后端团队中普及度不及 Java

---

### Go 详细分析

**适合的 Skill 类型**：
- 对启动速度和内存占用极度敏感的按需调用（Serverless）Skill
- 高并发网关/代理类 Skill（如请求路由、流量转发）
- 基础设施类 Skill（与 K8s/云原生工具链集成）

**主要问题**：
- AI/ML 生态几乎空白，几乎所有 AI 相关操作都需要调用外部 HTTP API
- 国内企业后端团队中 Go 工程师占比相对较低，招聘和维护成本较高

---

### 综合决策建议

**第一优先：看 Skill 是否涉及 AI/ML 能力**

```
Skill 需要直接调用 LLM / 加载模型 / 使用向量库？
  ├─ 是 → Python（生态优势压倒一切）
  └─ 否 → 看团队技术栈
           ├─ 团队主力是 Java → Java（Spring Boot）
           ├─ 团队主力是 JS/TS → Node.js + TypeScript
           └─ 对资源占用极度敏感 → Go
```

**推荐默认组合**：

> - **AI 相关 Skill**：**Python + FastAPI**，这是当前最顺手的组合，SDK 最全，示例代码最多，社区答案最丰富
> - **纯业务逻辑 / 系统集成 Skill**：**Java + Spring Boot**，尤其适合已有 Java 体系的团队，不需要强迫团队切换技术栈

**避免的误区**：
- 不要因为"Java 性能好"就用 Java 写 AI Skill——调用 LLM API 的瓶颈永远在网络 IO 和模型推理，不在语言本身
- 不要因为"Python 慢"就否定 Python——Skill 的响应时间 99% 取决于外部服务延迟，Python 本身的计算开销可以忽略不计
- 不要在一个项目里混用多种语言开发 Skill（除非有明确的技术隔离理由），统一语言栈能极大降低团队的认知负担和运维成本

---

### 快速参考卡

| Skill 场景 | 推荐语言 | 推荐框架 |
|-----------|---------|---------|
| 调用 LLM API（OpenAI / Claude） | Python | FastAPI + httpx |
| RAG / 向量检索 | Python | FastAPI + LangChain / LlamaIndex |
| 本地 AI 模型推理 | Python | FastAPI + transformers / torch |
| 企业内部业务系统集成 | Java | Spring Boot 3.x |
| 高并发数据处理 | Java | Spring Boot + WebFlux |
| 网页抓取 / 自动化 | Python 或 Node.js | FastAPI + Playwright / Puppeteer |
| Serverless 按需调用 | Python 或 Go | FastAPI / Gin |
| 团队无明显偏好，AI Skill | **Python** | **FastAPI** |

---

## 3. 仓库结构：源代码与 Skill 定义放一起还是分开

### 问题

程序源代码（Service 实现）和 Skill 元数据定义（`skill.json`、描述文档）应该放在**同一个仓库**，还是拆成**两个独立仓库**？

---

### 从「常驻服务」角度分析

常驻服务是长期运行的进程，代码变更 → 构建镜像 → 重新部署，是一个完整的发布流水线。

#### 单仓库（Monorepo）方案

```
weather-skill/
├── skill.json          ← Skill 元数据
├── README.md
├── src/                ← 服务源代码
│   └── ...
└── Dockerfile
```

**优点**：
- **版本天然绑定**：`skill.json` 里的版本号和源代码同步提交，一个 commit 就是一个完整版本快照，不会出现"接口描述说支持某参数但代码里没有"的不一致问题
- **发布流程简单**：一条 CI/CD 流水线，代码改了 → 自动更新 `skill.json` → 打镜像 → 部署，无需跨仓库协调
- **本地开发体验好**：改代码、改接口描述、写测试在同一个工作目录，切换成本低
- **Issue/PR 上下文完整**：一个 PR 可以同时看到接口变更和实现变更

**缺点**：
- `skill.json` 和业务代码耦合在一起，若多个团队复用同一个 Skill 描述文件，每次都要 fork 整个仓库
- 仓库体积会随源代码增大

**适用场景**：
- Skill 由单个团队独立维护
- Skill 的接口定义和实现变更频率相近
- **推荐常驻服务优先采用此方案**

---

#### 双仓库（Split Repo）方案

```
# 仓库 1：Skill 描述仓库（轻量，纯配置/文档）
weather-skill-spec/
├── skill.json
├── docs/api.md
└── examples/

# 仓库 2：服务实现仓库（代码密集）
weather-skill-service/
├── src/
├── Dockerfile
└── README.md
```

**优点**：
- Skill 描述仓库可以作为"能力目录"集中管理，Agent 平台只需订阅描述仓库即可发现所有 Skill，无需访问实现仓库
- 接口定义可以先于实现稳定下来，供下游 Agent 提前接入测试
- 实现仓库可以设置更严格的访问权限（含内部逻辑/密钥）

**缺点**：
- **版本同步是最大的痛点**：`skill.json` 的版本号必须与 Service 保持一致，需要额外的跨仓库 CI/CD 协调，容易漂移
- 两个仓库的 PR/Issue 上下文割裂，排查问题时要来回切换
- 需要维护两套 CI 流水线

**适用场景**：
- 有专门的"能力注册中心"统一管理所有 Skill 描述
- Skill 接口由架构团队统一设计，实现由业务团队负责（接口与实现分属不同团队）

---

### 从「按需调用（随 Call 随用）」角度分析

按需调用模式下，Skill 通常被打包为一个可独立运行的函数/脚本，没有常驻进程。这种模式下仓库结构的影响更大。

#### 单仓库方案

```
weather-skill/
├── skill.json
├── handler.py          ← 函数入口（Lambda/Cloud Function）
├── requirements.txt
└── deploy/
    └── serverless.yml  ← 部署配置
```

**优点**：
- 函数代码极简，`skill.json` 和 `handler.py` 紧密对应，单仓库完全够用
- 部署即是发布，一次提交覆盖所有内容
- 按需调用的 Skill 本身就是"轻量原子"，放在同一仓库不会造成臃肿

**缺点**：基本无明显缺点，按需模式的 Skill 天然适合单仓库

**推荐**：**按需调用模式强烈推荐单仓库**，没有理由拆分。

#### 双仓库方案（不推荐用于按需模式）

对按需调用场景，双仓库几乎没有收益，只增加了：
- 每次更新函数逻辑还要同步更新描述仓库的版本号
- 函数本身轻量，拆成两个仓库反而显得繁重

---

### 综合决策建议

| 部署模式 | 推荐仓库结构 | 核心理由 |
|---------|------------|---------|
| 常驻服务，单团队维护 | **单仓库** | 版本绑定，发布简单 |
| 常驻服务，接口与实现分属不同团队 | **双仓库** | 职责边界清晰，描述可先行稳定 |
| 按需调用 | **单仓库** | Skill 本身轻量，无拆分必要 |
| 多个 Skill 统一管理（Monorepo） | **单仓库多目录** | 见下方说明 |

#### 特殊场景：团队管理多个 Skill

如果团队同时维护多个相关 Skill（如 `weather-query`、`weather-alert`、`weather-history`），推荐使用**单仓库多目录**结构，而不是为每个 Skill 建独立仓库：

```
skills-monorepo/
├── weather-query/
│   ├── skill.json
│   └── src/
├── weather-alert/
│   ├── skill.json
│   └── src/
└── shared/             ← 跨 Skill 共用的工具库
    └── utils/
```

**好处**：共享基础库（认证、日志、HTTP 客户端）不需要发布为独立包，直接引用即可；统一的 CI/CD 流水线；跨 Skill 的重构一次完成。

---

### 版本一致性保障（单仓库实践）

无论哪种方案，**`skill.json` 中的版本号必须与实际部署的服务版本一致**。推荐在 CI 流水线中自动校验：

```bash
# CI 检查脚本示例：确保 skill.json 版本与 pom.xml/pyproject.toml 一致
SKILL_VERSION=$(jq -r '.version' skill.json)
SERVICE_VERSION=$(grep '<version>' pom.xml | head -1 | sed 's/[^0-9.]//g')

if [ "$SKILL_VERSION" != "$SERVICE_VERSION" ]; then
  echo "ERROR: skill.json version ($SKILL_VERSION) != service version ($SERVICE_VERSION)"
  exit 1
fi
```

---

## 4. 协议选择：HTTP REST vs MCP vs Function Calling

### 问题

Skill 服务对外暴露的接口应该用哪种协议/规范？

### 各方案说明

#### HTTP REST
最通用的方案，任何语言、任何 Agent 框架都能调用。

```
POST /api/v1/skill/execute
Content-Type: application/json
{"input": "..."}
```

**优点**：简单、通用、易调试、生态完善
**缺点**：需要自己定义并维护接口规范，Agent 框架集成需要额外适配

#### MCP（Model Context Protocol）
Anthropic 推出的标准化协议，专门为 Agent ↔ Tool 通信设计。

```jsonc
{
  "method": "tools/call",
  "params": {
    "name": "my_skill",
    "arguments": {"input": "..."}
  }
}
```

**优点**：标准化、与 Claude/各主流 Agent 框架原生兼容、自动工具发现
**缺点**：生态相对新，非 MCP 框架接入需要额外适配层

#### Function Calling（OpenAI 格式）
OpenAI 定义的函数调用规范，被多个 LLM 和框架采用。

**优点**：兼容 OpenAI/Azure OpenAI 生态
**缺点**：与 MCP 存在规范竞争，并非通用标准

### 决策建议

| 场景 | 推荐方案 |
|------|----------|
| 团队主要使用 Claude / Anthropic SDK | **MCP** |
| 需要多个不同 LLM/Agent 框架共用 | **HTTP REST**（最大兼容性） |
| 接入 OpenAI/Azure OpenAI 生态 | **Function Calling** |
| 内部系统、没有框架限制 | **HTTP REST + skill.json 描述** |

**最佳实践**：设计 Skill 时，业务逻辑与协议层解耦。核心逻辑写在 Service 层，Controller 层可以同时暴露 REST 接口和 MCP 接口，互不影响。

---

## 5. 单一 Skill vs 聚合 Skill：拆分粒度如何决策

### 问题

一个 Skill 应该只做一件事，还是可以把多个相关功能聚合在一个 Skill 里？

### 分析

**过于细粒度的问题**：
- Agent 需要调用很多次才能完成一个任务，增加了编排复杂度
- 多次网络往返增加延迟
- 维护成本分散到多个仓库

**过于粗粒度的问题**：
- 单个 Skill 过于臃肿，难以维护
- 部分功能更新需要重新部署整个 Skill
- 不同功能可能有不同的资源需求和扩展策略

### 决策原则（单一职责 + 功能内聚）

```
判断是否应该拆分：
1. 这两个功能是否共享同一批底层依赖/配置？
   → 是：可以放在一起
   → 否：考虑拆分

2. 这两个功能是否有不同的扩展策略？
   → 是（如一个是 CPU 密集、一个是 IO 密集）：应该拆分
   → 否：可以放在一起

3. 这两个功能的使用场景是否高度相关？
   → 是（如"查询天气"和"天气预警"）：放在一起
   → 否（如"发送邮件"和"查询天气"）：拆分
```

### 推荐边界划分

- **按业务域**拆分，而非按技术层拆分
- 一个 Skill = 一个业务域的完整能力集合
- 示例：`weather-skill`（包含：查询当前天气、预报查询、历史数据查询）而不是把每个接口拆成一个 Skill

---

## 6. 状态管理：Skill 服务应该有状态还是无状态

### 问题

Skill 服务是否应该在本地内存中维护跨请求的状态？

### 建议：设计为无状态（Stateless）

**无状态的优势**：
- 可以水平扩展（多实例部署）
- 实例重启不影响业务连续性
- 更容易进行灰度发布和滚动更新

**如果需要状态，应将状态外置**：

| 状态类型 | 推荐存储方案 |
|---------|------------|
| 会话上下文 | Redis（TTL 管理） |
| 任务执行状态 | Redis 或数据库 |
| 用户偏好/配置 | 数据库 |
| 短期缓存（提升性能） | Redis 或内存缓存（允许丢失） |
| 文件/附件 | 对象存储（S3/MinIO） |

**例外情况**：
- 模型文件、词向量等只读的大型资源，在进程启动时加载到内存是合理的（因为它们不会改变）
- 连接池（数据库连接池、HTTP 连接池）是进程级状态，但这不是业务状态

---

## 7. 认证与安全：Skill 如何处理鉴权

### 常见方案

#### 方案 A：API Key 认证（推荐用于内部调用）
```
GET /api/v1/skill/execute
Authorization: Bearer {api-key}
```
简单可靠，适合 Agent 平台与 Skill 之间的内部调用。

#### 方案 B：mTLS（双向 TLS）
服务间通信使用客户端证书认证，适合零信任网络环境。

#### 方案 C：网络层隔离（推荐最低要求）
Skill 服务只部署在内网，不暴露公网。Agent 平台和 Skill 在同一私有网络内通信。

### 推荐策略

1. **最低要求**：Skill 服务不暴露公网，仅在内部网络可达
2. **推荐**：内部调用使用固定的 API Key 认证，Key 通过环境变量注入
3. **高安全场景**：在 API Key 基础上增加 IP 白名单
4. **绝对不要做**：硬编码 API Key 在代码里，或通过 URL 参数传递密钥

### 敏感信息处理

```yaml
# 永远通过环境变量注入，不要写在代码或配置文件里
SKILL_API_KEY=secret-key-here

# 日志中不打印敏感字段
# Java: 在 DTO 的敏感字段上加 @JsonIgnore 或 @ToString.Exclude
# Python: 在 Pydantic model 中 exclude 敏感字段
```

---

## 8. 超时与重试：调用链的容错设计

### 问题

Agent → Skill → 外部服务 的调用链，超时和重试如何设置？

### 超时设置原则

```
调用链超时时间：每一层的超时 < 上一层的超时

Agent 平台超时（60s）
  └─ Skill 服务处理超时（50s）
       └─ 外部服务调用超时（30s）
```

**为什么要留余量**：如果 Skill 超时等于 Agent 超时，当外部服务恰好在临界点超时时，两边会同时超时，无法区分是哪一层出了问题。

### 重试策略

**可以重试**：
- 网络抖动导致的超时（5xx 错误）
- 外部服务临时不可用

**不应重试**：
- 参数校验失败（4xx 错误）
- 幂等性无法保证的写操作（如"发送邮件"、"扣款"）

```python
# Python 推荐使用 tenacity 库
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type

@retry(
    stop=stop_after_attempt(3),
    wait=wait_exponential(multiplier=1, min=1, max=10),
    retry=retry_if_exception_type(httpx.TimeoutException),
)
async def call_with_retry(self, input_text: str) -> str:
    return await self._client.process(input_text)
```

```java
// Java 推荐使用 Spring Retry 或 Resilience4j
@Retry(name = "externalApi", fallbackMethod = "fallback")
public String callWithRetry(String input) {
    return externalApiClient.call(input);
}

public String fallback(String input, Exception e) {
    log.error("All retries exhausted for input={}", input, e);
    throw new SkillException(ErrorCode.EXTERNAL_API_ERROR, "Service temporarily unavailable");
}
```

---

## 9. 本地工具函数 vs 独立服务：何时需要独立部署

### 问题

是把功能写成 Agent 的本地工具函数（同进程），还是抽成独立的 Skill 服务？

### 决策标准

**保持为本地工具函数，当**：
- 逻辑简单，不超过 100 行代码
- 不需要独立的资源配置（无数据库、无重型依赖）
- 功能与 Agent 高度耦合，不可能被其他 Agent 复用
- 示例：字符串格式化、日期计算、简单数学运算

**抽成独立 Skill 服务，当**：
- 功能需要被多个不同的 Agent 复用
- 功能有独立的外部依赖（数据库、外部 API）
- 功能本身有独立的扩展和资源需求
- 功能涉及复杂的业务逻辑，需要独立测试和版本管理
- 示例：PDF 解析服务、天气查询服务、数据库查询服务

### 判断流程

```
这个功能会被多个 Agent 用到吗？
  ├─ 是 → 独立 Skill 服务
  └─ 否 → 这个功能有外部依赖或需要独立扩展吗？
           ├─ 是 → 独立 Skill 服务
           └─ 否 → 本地工具函数
```

---

## 10. 流式响应：Skill 是否需要支持 Streaming

### 问题

当 Skill 的响应内容很长（如 LLM 生成文本）时，是否应该支持流式返回？

### 建议

**支持流式响应，当**：
- Skill 的处理时间超过 3 秒
- 响应内容较长且可以分段展示（如文本生成、代码生成）
- 用户体验要求"边生成边展示"（typewriter 效果）

**不需要流式响应，当**：
- 响应是结构化数据（JSON），需要完整才有意义
- 处理时间很短（< 1 秒）
- 场景是后台批量处理，不需要实时反馈

### 实现方式

```python
# FastAPI SSE 流式响应示例
from fastapi import APIRouter
from fastapi.responses import StreamingResponse
import asyncio

router = APIRouter()

async def generate_stream(input_text: str):
    """模拟流式生成"""
    words = input_text.split()
    for word in words:
        yield f"data: {word}\n\n"
        await asyncio.sleep(0.1)
    yield "data: [DONE]\n\n"

@router.post("/stream")
async def stream_execute(request: SkillRequest):
    return StreamingResponse(
        generate_stream(request.input),
        media_type="text/event-stream",
    )
```

---

## 11. 版本管理：Skill 如何平滑升级

### 问题

Skill 接口发生变化时，如何避免影响已经接入的 Agent？

### URL 版本控制

```
/api/v1/skill/execute  ← 老版本继续维护
/api/v2/skill/execute  ← 新版本
```

### 兼容性原则

| 变更类型 | 是否破坏性 | 处理方式 |
|---------|----------|---------|
| 新增可选字段 | 非破坏性 | 直接发布，无需新版本 |
| 新增必填字段 | 破坏性 | 需要新版本（v2） |
| 删除字段 | 破坏性 | 需要新版本（v2） |
| 修改字段类型 | 破坏性 | 需要新版本（v2） |
| 修改业务逻辑（不改接口） | 非破坏性 | 直接发布 |

### 升级流程

```
1. 发布新版本（v2 路由），v1 继续运行
2. 通知依赖方进行迁移测试
3. 给出 v1 下线时间线（建议不少于 2 周）
4. 所有依赖迁移完成后下线 v1
5. 更新 skill.json 中的 version 字段
```

---

## 12. 监控与可观测性：如何观测 Skill 的运行状态

### 三个核心维度

#### Metrics（指标）
使用 Prometheus 格式暴露关键指标：

```
# 推荐监控的指标
skill_request_total{status="success|error"}   # 请求总量
skill_request_duration_seconds{quantile}       # 响应时间分位数（P50/P95/P99）
skill_active_requests                          # 当前并发请求数
skill_external_api_error_total                 # 外部依赖错误数
```

#### Logs（日志）
结构化日志，便于查询和告警：

```jsonc
{
  "timestamp": "2026-04-07T10:00:00Z",
  "level": "INFO",
  "trace_id": "abc-123",
  "request_id": "req-456",
  "event": "skill_execute",
  "duration_ms": 230,
  "status": "success"
}
```

日志规范：
- 每个请求打印入口日志（含 `request_id`）和结果日志（含耗时）
- 异常必须打印完整 stack trace
- 不打印请求体中的敏感字段（API Key、密码、用户隐私数据）

#### Tracing（链路追踪）
使用 OpenTelemetry 传播 `trace_id`，实现跨服务的链路追踪：

```python
# Python - 使用 opentelemetry 自动注入 trace_id
from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor
FastAPIInstrumentor.instrument_app(app)
```

```java
// Java - 使用 Spring Boot Actuator + Micrometer Tracing
// 在 application.yml 中配置：
management:
  tracing:
    sampling:
      probability: 1.0
```

### 健康检查分级

```
/actuator/health/liveness   → 进程是否存活（Kubernetes liveness probe）
/actuator/health/readiness  → 是否准备好接受流量（Kubernetes readiness probe）
/actuator/health            → 完整健康状态（含依赖检查）
```

---

## 13. 纯提示词 Skill：没有脚本的 Skill 本质是什么

### 问题一：没有脚本的 Skill，本质上是不是就是一套提示词？

**是的，本质就是提示词。**

没有脚本的 Skill 就是一个 Markdown 文件，包含三类内容：
- **触发条件**（TRIGGER/when_to_invoke）：什么情况下调用这个 Skill
- **执行步骤**（instructions）：按顺序做什么
- **约束规则**（constraints）：不允许做什么

Claude 读取后按提示词执行，没有任何额外的运行时能力。

对比有脚本的 Skill：

| 维度 | 纯提示词 Skill | 有脚本的 Skill |
|------|--------------|--------------|
| 实现形式 | Markdown 文件 | Markdown + 可执行脚本 |
| 运行时能力 | 仅 Claude 本身 | 可调用外部工具、读写文件、跑命令 |
| 适合场景 | 固定流程、分析类任务 | 需要与系统交互的自动化任务 |
| 维护成本 | 极低 | 较高（需维护代码） |

---

### 问题二：纯提示词 Skill 会消耗很多 Token，优势是什么？

**优势在于结构化复用，而不是节省 Token。**

Token 消耗确实不会减少——Skill 文件本身也会被注入上下文。纯提示词 Skill 的价值体现在：

1. **一致性**：同一套步骤每次执行方式相同，不依赖 Claude 临时发挥，避免"每次结果不一样"的问题
2. **可维护性**：改一处 Markdown，所有调用自动更新，无需修改散落各处的 prompt
3. **降低用户认知负担**：用户只需输入 `/skill-name`，不用每次手写长提示词

**适用场景**：步骤固定、需要反复调用、值得封装的标准化流程（如 `/review`、`/security-review`、`/init`）。

**不适用场景**：一次性任务，直接写提示词反而更轻、更灵活，没必要封装为 Skill。

---

## 附录：快速决策清单

开发一个新的 Agent Skill 时，按以下清单做出关键决策：

```
□ 部署模式：常驻服务（高频/低延迟）还是按需调用（低频/耗时）？
□ 语言选型：Skill 是否涉及 AI/ML？是 → Python；否 → 团队主力语言（Java/Go）
□ 仓库结构：单仓库还是双仓库？接口与实现是否由同一团队维护？
□ 协议选择：HTTP REST / MCP / Function Calling？
□ 粒度设计：这个 Skill 的业务边界是否清晰？
□ 状态设计：是否需要外置状态存储（Redis/DB）？
□ 认证方案：API Key 还是网络层隔离？
□ 超时配置：Agent → Skill → 外部服务 三层超时是否有梯度？
□ 重试策略：哪些操作可以重试？哪些绝对不能？
□ 版本策略：接口是否向后兼容？如何通知下游升级？
□ 可观测性：是否有健康检查、结构化日志、关键 Metrics？
□ 流式支持：处理时间是否超过 3 秒？是否需要 Streaming？
```
