# Agent Skill 概述

> 版本：1.0 | 定位：基础入门

---

## 1. 什么是 Agent Skill

Agent Skill（代理技能）是封装了特定任务指令的**可复用执行单元**，由 AI Agent 在会话中加载并执行。

可以将 Skill 理解为：
- **对人类**：一条精心设计的"工作模板"或"操作手册"
- **对 Agent**：注入当前上下文的专业指令集，影响 Agent 的理解范围和行为方式
- **对组织**：标准化 AI 工作流的基本积木

### 1.1 核心价值

| 价值维度 | 说明 |
|---------|------|
| **一致性** | 相同任务每次都按同一标准执行，消除个人差异 |
| **复用性** | 一次设计，多人多次使用，沉淀组织知识 |
| **可维护** | 集中管理指令，修改一处即全局生效 |
| **可组合** | 多个 Skill 协作，完成复杂工作流 |

### 1.2 没有 Skill 的问题

```
用户 A：每次用不同的描述方式请求 Agent 生成报告 → 输出格式不统一
用户 B：需要反复解释项目背景 → 浪费 Token，效率低下
用户 C：新成员不知道团队的 AI 使用规范 → 质量参差不齐
```

有了 Skill：`/generate-report` 一键触发，格式规范、背景上下文自动注入。

---

## 2. Skill 的工作原理

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

### 2.1 上下文注入机制

Skill 本质上是**系统提示词（System Prompt）的模块化延伸**。当 Skill 被触发时：

1. Skill 文件内容被读取并展开
2. 占位符/参数被替换为实际值
3. 注入到 Agent 的当前上下文窗口
4. Agent 在此上下文约束下执行后续任务

---

## 3. Skill 的应用场景

### 3.1 任务自动化
将重复性操作封装为一键触发的 Skill。

```
典型场景：
- /commit          → 自动分析变更、生成规范的 Git 提交信息
- /daily-report    → 按模板生成每日工作报告
- /code-review     → 按团队编码规范检查代码
- /translate-doc   → 按指定风格翻译文档
```

### 3.2 上下文注入
将领域知识、角色设定或工作规范注入会话。

```
典型场景：
- /act-as-java-expert    → 注入 Java 专家角色设定
- /load-project-context  → 注入项目背景、技术栈、约束条件
- /follow-team-style     → 注入团队代码风格指南
```

### 3.3 工作流编排
将多步骤复杂任务拆解为有序地执行流程。

```
典型场景：
- /publish-blog     → 步骤1:检查内容 → 步骤2:格式化 → 步骤3:生成摘要 → 步骤4:发布
- /onboard-feature  → 步骤1:分析需求 → 步骤2:设计接口 → 步骤3:生成代码 → 步骤4:写测试
```

### 3.4 格式化输出
规范特定场景的输出结构和样式。

```
典型场景：
- /api-doc         → 按 OpenAPI 规范生成 API 文档
- /meeting-minutes → 按会议纪要模板整理要点
- /test-case       → 按 Given/When/Then 格式生成测试用例
```

---

## 4. Skill 的基本构成

一个完整的 Skill 通常包含：

```yaml
# 元数据（告诉平台如何识别和注册这个 Skill）
name: skill-name
description: 这个 Skill 的用途说明
trigger: /skill-name 或 自然语言触发词

# 执行指令（告诉 Agent 如何执行任务）
prompt: |
  你是一个...
  当用户...时，你需要：
  1. 首先...
  2. 然后...
  3. 最后输出...

# 参数定义（可选，定义 Skill 接收的输入参数）
parameters:
  - name: target
    type: string
    description: 操作目标
    required: true
```

---

## 5. Skill 与相关概念的关系

| 概念 | 关系 |
|------|------|
| **System Prompt** | Skill 是模块化的 System Prompt，可复用、可组合 |
| **Prompt Template** | Skill 是可执行的 Prompt 模板，附带触发机制和元数据 |
| **Agent Tool** | Tool 提供外部能力（调 API），Skill 提供执行指令；二者互补 |
| **Workflow** | 复杂工作流可由多个 Skill 组合实现 |
| **Plugin/Extension** | Skill 是轻量化的"指令插件"，无需代码即可创建 |

---

## 6. 学习路线建议

```
理解概念（本文）
    ↓
了解 Skill 类型与触发机制（下一章）
    ↓
选择目标平台（Claude Code / Cursor / Dify / Coze）
    ↓
学习平台具体 Skill 机制
    ↓
掌握 Prompt 工程（设计高质量 Skill 指令）
    ↓
（进阶）后端服务开发规范 / MCP 协议集成
```
