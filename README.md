# zmz-ai-learning

> AI 工程化学习与实战仓库，围绕 **Agent / Skill / MCP / SDD / RAG** 五大主题，沉淀系统化知识笔记与真实开发案例；以**提示词工程**作为公共基础层。

---

## 目录结构

```
zmz-ai-learning/
├── 00-temp/                              → 临时工作区（未归档的草稿、试验笔记）
│
├── 00-提示词工程/                        → 公共基础层：五大主题共用的提示词知识
│   ├── 01-基础框架/                        · TAG / RTF / SPAR / CARE 等结构化模板
│   ├── 02-进阶技巧/                        · Few-shot / CoT / ReAct / Self-Consistency
│   ├── 03-角色与风格/                      · Role prompting、人格与语气
│   └── 04-反模式与调优/                    · 常见陷阱、A/B 调优方法
│
├── 01-Agent 学习笔记/                    → Agent 理论知识体系
│   ├── 01-核心概念/                        · LLM 记忆与上下文、Agent 与子 Agent 基础
│   ├── 02-多Agent协作/                     · 协作模式、子 Agent 提示词、Plan-Execute 编排
│   └── 03-平台实践/                        · Agent 编排模式、公司环境接入方案
│
├── 01-Agent 实战项目/                    → 企业级 Agent 提示词库（9 类业务域）
│   ├── 01-Core-管理决策类智能体/
│   ├── 02-Tech-研发技术类智能体/
│   ├── 03-Growth-运营增长类智能体/
│   ├── 04-Serve-客户支持类智能体/
│   ├── 05-Admin-行政人事类智能体/
│   ├── 06-Fin-财务法务类智能体/
│   ├── 07-Data-数据智能类智能体/
│   ├── 08-Security-安全合规类智能体/
│   └── 09-Design-设计创意类智能体/
│
├── 02-Skill 学习笔记/                    → Agent Skill 系统化知识体系
│   ├── 01-核心概念/                        · Skill 概述、类型与触发机制
│   ├── 02-平台指南/                        · Skill 完全指南、跨平台对比
│   ├── 03-规范与标准/                      · SOP 目录规范、多语言开发规范、决策指南
│   ├── 04-Prompt工程/                      · Skill Prompt 设计原则、常见模式与模板
│   ├── 05-MCP协议/                         · MCP 协议与 Skill 集成指南
│   └── 06-资源汇总/                        · 推荐 Skill 工具与资源
│
├── 02-Skill 实战项目/                    → Skill 开发案例与产出物
│   ├── skill_develop_rule.md                · Skill 开发规则
│   ├── skill_develop_guideline.md           · Skill 开发指南
│   └── skill-01-case-converter/             · 命名风格转换 Skill（含 JVM 能力）
│
├── 03-MCP 学习笔记/                      → MCP 协议系统化知识体系
│   ├── 01-核心概念/                        · MCP 协议概述、三类能力原语详解
│   ├── 02-协议规范/                        · JSON-RPC 消息结构、Stdio 与 SSE 传输层
│   ├── 03-开发指南/                        · MCP Server 语言选型、Client 开发
│   ├── 04-规范与标准/                      · MCP Server 设计规范、安全最佳实践
│   ├── 05-生态与集成/                      · MCP Server 挂载、常用 Server 速查
│   └── 06-资源汇总/                        · MCP 工具与资源
│
├── 03-MCP 实战项目/                      → MCP Server 真实开发案例（多语言并列）
│   ├── mcp-java/                            · Java 技术栈（Spring AI MCP）
│   ├── mcp-python/                          · Python 技术栈（FastMCP / 官方 SDK）
│   ├── mcp-nodejs/                          · Node 技术栈
│   ├── mcp-go/                              · Go 技术栈
│   ├── mcp-rust/                            · Rust 技术栈
│   └── mcp-csharp/                          · C# / .NET 技术栈
│
├── 04-SDD 学习笔记/                      → SDD（Spec-Driven Development）系统化笔记
│   ├── 01-SDD-核心概念与价值.md
│   ├── 02-SDD-标准流程与阶段划分.md
│   ├── 03-SDD-规范文档模板详解.md
│   ├── 04-SDD-工具落地与工作流整合.md
│   ├── 05-SDD-端到端案例-Git提交统计CLI.md
│   ├── 06-SDD-Token成本与项目规模档位.md
│   ├── 07-SDD-跨栈协作-前端+后端+DB.md
│   ├── 08-SDD-AI代码可读性与人工接手.md
│   ├── 09-SDD-测试策略-常规+压力+回归.md
│   ├── 10-SDD-Brownfield存量项目改造.md
│   ├── 11-SDD-常见误区与渐进落地.md
│   ├── 12-SDD-模型职责分工与边界点.md
│   ├── 13-SDD-人和模型的职责分工.md
│   ├── 14-SDD-工具无关的Agent套件.md
│   └── 15-SDD-Task管理与环境晋级.md
│
├── 04-SDD 实战项目/                      → SDD 方法论的实战案例集
│   ├── sdd-01-task-api/                     · 极简任务管理 API（Java + Spring Boot）
│   └── sdd-02-task-fullstack/               · 全栈多仓 Task（Vue + Spring Boot + MySQL）
│
├── 05-RAG 学习笔记/                      → RAG（检索增强生成）系统化知识体系（21 篇）
│   ├── 01-核心概念/                        · RAG 概述与价值 / 标准管线 / Embedding 与向量空间
│   ├── 02-索引与切分/                      · Chunking 策略 / 向量数据库选型 / 元数据与多模态
│   ├── 03-检索策略/                        · 稀疏-稠密-混合 / Re-rank / Query 改写 / 上下文压缩
│   ├── 04-生成与提示词/                    · Prompt 模板与引用 / 长上下文与结构化输出
│   ├── 05-评估与可观测/                    · 评估指标 / 评估框架 / 线上监控与回归
│   ├── 06-高级模式/                        · Agentic-RAG / GraphRAG / 多模态与企业知识库
│   ├── 07-工程化与规范/                    · SOP-01 系统设计规范 / SOP-02 安全与合规
│   └── 08-资源汇总/                        · RAG 生态速查（框架 / 模型 / 论文 / 课程）
│
└── 05-RAG 项目实战/                      → RAG 真实开发案例集（每语言一个全功能项目）
    ├── rag_develop_rule.md                  · RAG 项目通用规则（硬约束）
    ├── rag_develop_guideline.md             · RAG 项目通用开发指引（步骤 + 提示词模板）
    ├── rag-python-qa/                       · Python 栈全功能 RAG（OpenAI + Chroma + jieba BM25 + 4 档 strategy）
    └── rag-java-qa/                         · Java 栈全功能 RAG（Spring AI + SimpleVectorStore + jieba-analysis BM25；与 rag-python-qa 业务孪生）
```

---

## 目录组织原则

- **主题分域**：`Agent` / `Skill` / `MCP` / `SDD` / `RAG` 五条主线各自独立，互不耦合
- **学习 / 实战成对**：每个主题均采用「`XX-学习笔记` + `XX-实战项目`」配对结构，理论与落地分离
- **编号前缀**：一级目录用 `NN-` 前缀固定排序，子目录沿用同一约定，便于检索与增量扩展
- **临时与正式隔离**：草稿、试验性内容统一归入 `00-temp/`，不污染主知识体系
- **工具中立**：所有学习笔记面向多平台多框架；项目级约束文件优先用 `AGENTS.md`（厂商中立）

---

## 主题间的协作

```
00-提示词工程  ── 公共基础（任何 LLM 调用都要用）
       │
       ├──► 01-Agent       ── "决策大脑"
       ├──► 02-Skill       ── "按需技能"
       ├──► 03-MCP         ── "能力总线"
       ├──► 04-SDD         ── "工程方法"
       └──► 05-RAG         ── "知识入口"

五者正交，可叠加（例：基于 SDD 流程开发的 Agentic RAG，把检索包装成 MCP Tool 暴露给 Skill）。
```
