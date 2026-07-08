# Agent Skill 基础概念篇 · 面试题库

> 涵盖 Agent、Skill、MCP 的核心定义、工作原理与基础关系

---

## Q1.1 什么是 Agent Skill？它解决了什么问题？

**参考答案**：
Agent Skill 是封装了特定任务指令的**可复用执行单元**，由 AI Agent 在会话中加载并执行。

可以理解为：
- **对人类**：一条精心设计的"工作模板"或"操作手册"
- **对 Agent**：注入当前上下文的专业指令集，影响 Agent 的理解范围和行为方式
- **对组织**：标准化 AI 工作流的基本积木

**核心价值**：

| 价值维度 | 说明 |
|---------|------|
| 一致性 | 相同任务每次都按同一标准执行 |
| 复用性 | 一次设计，多人多次使用，沉淀组织知识 |
| 可维护 | 集中管理指令，修改一处即全局生效 |
| 可组合 | 多个 Skill 协作，完成复杂工作流 |

**解决的问题**：用户 A 每次用不同描述请求 → 输出格式不统一；用户 B 需要反复解释项目背景 → 浪费 Token；用户 C 新成员不知道团队 AI 使用规范 → 质量参差不齐。

**难度**：⭐ | **考察点**：基础概念理解

---

## Q1.2 Skill 的工作原理是什么？简述上下文注入机制。

**参考答案**：
```
用户触发 Skill
    │
    ▼
Skill 内容注入当前会话上下文
    │
    ▼
Agent 基于注入的 Skill 指令 + 用户输入 → 执行任务
    │
    ▼
输出结果（符合 Skill 定义的格式和规范）
```

**上下文注入机制**：Skill 本质上是**系统提示词（System Prompt）的模块化延伸**。当 Skill 被触发时：
1. Skill 文件内容被读取并展开
2. 占位符/参数被替换为实际值
3. 注入到 Agent 的当前上下文窗口
4. Agent 在此上下文约束下执行后续任务

**难度**：⭐ | **考察点**：工作流程理解

---

## Q1.3 Skill 的主要应用场景有哪些？

**参考答案**：

| 场景 | 说明 | 典型示例 |
|------|------|---------|
| 任务自动化 | 将重复性操作封装为一键触发 | /commit、/daily-report、/code-review |
| 上下文注入 | 将领域知识、角色设定注入会话 | /act-as-java-expert、/load-project-context |
| 工作流编排 | 将多步骤复杂任务拆解为有序流程 | /publish-blog、/onboard-feature |
| 格式化输出 | 规范特定场景的输出结构 | /api-doc、/meeting-minutes、/test-case |

**难度**：⭐ | **考察点**：实践认知

---

## Q1.4 Skill 与 System Prompt、Tool、Plugin 的区别是什么？

**参考答案**：

| 概念 | 关系 |
|------|------|
| System Prompt | Skill 是模块化的 System Prompt，可复用、可组合 |
| Prompt Template | Skill 是可执行的 Prompt 模板，附带触发机制和元数据 |
| Agent Tool | Tool 提供外部能力（调 API），Skill 提供执行指令；二者互补 |
| Workflow | 复杂工作流可由多个 Skill 组合实现 |
| Plugin/Extension | Skill 是轻量化的"指令插件"，无需代码即可创建 |

**Skill vs MCP Server**：

| 维度 | Agent Skill | MCP Server |
|------|------------|------------|
| 本质 | 指令模板（Prompt） | 能力提供者（Tools/Resources） |
| 作用 | 告诉 Agent **如何做** | 给 Agent 提供**做事的工具** |
| 实现 | Markdown 文件 | 独立服务进程 |
| 互补关系 | Skill 调用 MCP 工具完成任务 | MCP 工具为 Skill 提供执行能力 |

**难度**：⭐⭐ | **考察点**：概念辨析能力

---

## Q1.5 Skill 的基本构成有哪些？

**参考答案**：
一个完整的 Skill 通常包含：

```yaml
# 元数据（告诉平台如何识别和注册这个 Skill）
name: skill-name
description: 这个 Skill 的用途说明
trigger: /skill-name 或自然语言触发词

# 执行指令（告诉 Agent 如何执行任务）
prompt: |
  你是一个...
  当用户...时，你需要：
  1. 首先...
  2. 然后...
  3. 最后输出...

# 参数定义（可选）
parameters:
  - name: target
    type: string
    description: 操作目标
    required: true
```

**必选要素**：name、description、执行指令（prompt/正文）
**可选要素**：parameters、allowed-tools、disallowed-tools、model

**难度**：⭐ | **考察点**：Skill 结构认知

---

## Q1.6 纯提示词 Skill 的本质是什么？优势在哪里？

**参考答案**：
本质就是**一套提示词**——包含触发条件、执行步骤和约束规则的 Markdown 文件。Claude 读取后按提示词执行，没有任何额外的运行时能力。

**优势在于结构化复用，而非节省 Token**：
1. **一致性**：同一套步骤每次执行方式相同，不依赖 Claude 临时发挥
2. **可维护性**：改一处 Markdown，所有调用自动更新
3. **降低认知负担**：用户只需输入 `/skill-name`，不用每次手写长提示词

**适用场景**：步骤固定、需要反复调用、值得封装的标准化流程
**不适用场景**：一次性任务，直接写提示词反而更轻量

**难度**：⭐⭐ | **考察点**：对 Skill 本质的理解深度

---

## Q1.7 描述 Skill 的生命周期。

**参考答案**：
```
1. 发现（Discovery）
   平台启动时扫描 Skill 目录，注册所有可用 Skill

2. 加载（Loading）
   用户触发时读取 Skill 文件，解析 frontmatter 和指令内容

3. 参数解析（Argument Parsing）
   替换占位符（$ARGUMENTS 等），生成最终执行指令

4. 上下文注入（Context Injection）
   将 Skill 指令注入当前会话上下文

5. 执行（Execution）
   Agent 基于注入的指令和会话历史执行任务

6. 输出（Output）
   生成符合 Skill 定义规范的输出结果
```

**难度**：⭐⭐ | **考察点**：系统流程理解

---

## Q1.8 没有 Skill 的 Agent 会遇到什么问题？

**参考答案**：
1. **输出格式不统一**：用户每次用不同方式描述同一需求，Agent 产出不一致
2. **Token 浪费**：需要反复解释项目背景、工作规范
3. **质量参差不齐**：团队成员各自探索，缺少标准化流程
4. **知识无法沉淀**：优秀的工作方式无法在组织内复用
5. **学习成本高**：新成员需要记住大量 prompt 写法，而不是一条 `/` 命令

**难度**：⭐ | **考察点**：痛点理解

---

> 参考来源：笔记 `01-Agent-Skill概述.md`，`02-Skill类型与触发机制.md`
