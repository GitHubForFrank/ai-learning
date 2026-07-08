# 跨平台 Skill 对比篇 · 面试题库

> 涵盖 Claude Code / Cursor / Windsurf / Dify / Coze 五平台的 Skill 机制对比与选型

---

## Q4.1 比较五个主要平台的 Skill/Rules 机制。

**参考答案**：

| 维度 | Claude Code | Cursor | Windsurf | Dify | Coze |
|------|-------------|--------|----------|------|------|
| Skill 定义方式 | Markdown 文件 | 文本/Markdown 文件 | 文本文件 | 可视化 + YAML | 可视化 + JSON |
| 触发方式 | 斜杠命令 | 自动生效/文件匹配 | 自动生效 | 对话/API | 对话/API |
| 参数传递 | $ARGUMENTS | 上下文注入 | 上下文注入 | 变量插值 | 变量插值 |
| 工具集成 | 内置工具 + MCP | 内置工具 + MCP | 内置工具 | 自定义 Tools | Plugins |
| 工作流 | 通过 Skill 组合 | 不支持 | Flows | 可视化 Workflow | 可视化 Workflow |
| 多级作用域 | 全局/项目 | 项目级 | 项目级 | 应用级 | Bot 级 |
| 版本控制 | Git 友好 | Git 友好 | Git 友好 | 平台内部 | 平台内部 |

**难度**：⭐⭐⭐ | **考察点**：跨平台全局视野

---

## Q4.2 Cursor 的 `.mdc` Rules 文件如何实现条件触发？

**参考答案**：
Cursor 的 `.cursor/rules/*.mdc` 文件支持通过 frontmatter 实现条件触发：

```markdown
---
description: React 组件开发规范
globs: src/components/**/*.tsx
alwaysApply: false
---

编写 React 组件时遵循以下规范：
- 使用函数式组件
- Props 必须有 TypeScript 类型定义
- 组件文件名使用 PascalCase
```

**关键字段**：
- `globs`：根据文件路径自动激活对应 Rule
- `alwaysApply`：控制 Rule 是否始终注入（true = 始终生效，false = 仅匹配文件时）
- `description`：Rule 的描述信息

**与 Claude Code 的差异**：Cursor 没有斜杠命令触发，没有 `$ARGUMENTS`，使用文件匹配 + 上下文注入。

**难度**：⭐⭐ | **考察点**：Cursor 机制

---

## Q4.3 Dify 和 Coze 的 Skill 形态与 Claude Code 有什么本质不同？

**参考答案**：

| 本质不同 | Claude Code | Dify / Coze |
|---------|------------|------------|
| 定位 | 开发者工具（CLI/IDE） | AI 应用构建平台 |
| Skill 形态 | Markdown 文件（代码层面） | 可视化配置 + 工作流编排 |
| 运行环境 | 本地 | 云端 |
| 触发方式 | 斜杠命令 / Hook | 对话 / API / 定时 |
| 版本控制 | Git（文件级） | 平台内部管理 |
| 扩展方式 | MCP 协议 | 自定义 Tools / Plugins / 知识库 |

**Dify 的三大 Skill 形态**：
- **Tools**：封装外部 API 调用
- **Workflows**：可视化编排多步骤任务（支持条件分支、循环、并行）
- **Prompts**：可复用的提示词模板（支持变量插值）

**Coze 的额外优势**：多 Bot 协作编排、内置消息推送（飞书/钉钉/微信）、丰富的官方插件市场。

**难度**：⭐⭐⭐ | **考察点**：平台本质差异的理解

---

## Q4.4 如何将 Claude Code Skill 迁移到 Cursor？有哪些注意事项？

**参考答案**：

**迁移映射**：
```markdown
# Claude Code Skill
---
name: code-review
description: 代码审查
allowed-tools: [Read, Grep]
---
对提供的代码进行 Code Review...
```
↓
```markdown
# Cursor Rule（.cursor/rules/code-review.mdc）
---
description: 当用户要求代码审查时自动应用
globs: "**/*.{js,ts,py,java}"
alwaysApply: false
---
对提供的代码进行 Code Review...
```

**迁移注意点**：
1. Cursor 无法声明工具权限（使用内置工具集）
2. 触发方式从斜杠命令变为文件匹配或上下文推断
3. `$ARGUMENTS` 参数传递需改为上下文注入
4. 需重新适配工具调用（不同平台的工具集不同）

**通用迁移原则**：
1. 提取核心指令——Skill 的本质是 Prompt，指令部分可在任何平台复用
2. 工具调用需针对目标平台重写
3. 在目标平台充分测试，避免行为差异

**难度**：⭐⭐⭐ | **考察点**：跨平台迁移能力

---

## Q4.5 如何根据场景选择合适的平台？（选型决策树）

**参考答案**：
```
你的主要使用场景是什么？
│
├── 代码开发辅助
│   ├── 需要深度 IDE 集成 → Cursor 或 Windsurf
│   └── 需要命令行操作和 Hook 机制 → Claude Code
│
├── 构建独立 AI 应用
│   ├── 需要可视化工作流编排 → Dify 或 Coze
│   ├── 需要向量数据库和 RAG → Dify（推荐）
│   └── 需要多平台消息推送集成 → Coze（推荐）
│
└── 两者兼有
    └── 开发阶段用 Claude Code，生产部署用 Dify/Coze
```

**场景速查**：

| 场景 | 推荐平台 | 原因 |
|------|---------|------|
| 个人开发效率提升 | Claude Code / Cursor | 本地化、代码集成深 |
| 团队统一规范 | Claude Code（项目 Skill） | Git 版本控制友好 |
| 企业 AI 应用构建 | Dify / Coze | 可视化、多模型、内置知识库 |
| RAG + 知识库应用 | Dify | RAG 能力最成熟 |
| 代码编辑辅助 | Cursor / Windsurf | IDE 深度集成 |
| 自动化脚本/Hook | Claude Code | Hook 机制独特 |

**难度**：⭐⭐⭐ | **考察点**：平台选型决策能力

---

## Q4.6 各平台在 Hook/事件触发机制上有何差异？

**参考答案**：

| 平台 | 等价机制 | 触发粒度 |
|------|---------|----------|
| Claude Code | hooks 字段（PreToolUse/PostToolUse/Stop 等） | 工具级、会话级 |
| Cursor / Windsurf | Rules 文件（alwaysApply / glob match） | 文件级、上下文级 |
| Cline / Roo Code | Plan/Act 模式切换 + 自定义 instructions | 模式级 |
| Aider | --lint-cmd / --test-cmd 自动跑 | commit 前后、edit 后 |
| Dify / Coze | Workflow 节点 + 条件分支 + 事件节点 | 流程图节点级 |
| 自研系统 | 观察者模式（注册 listener） | 完全自定义 |

**核心结论**：
- 想要精细的"工具调用前后插脚本"→ **Claude Code** 最完备
- 想要"按文件路径自动注入指令"→ **Cursor / Windsurf Rules**
- 想要"流程图编排 + 事件分支"→ **Dify / Coze Workflow**

**难度**：⭐⭐⭐ | **考察点**：事件机制理解

---

> 参考来源：笔记 `02-跨平台Skill机制对比.md`
