# 跨平台 Skill 机制对比

> 版本：1.0 | 覆盖平台：Claude Code / Cursor / Windsurf / Dify / Coze

---

## 1. 平台概览

| 平台 | 定位 | Skill 形态 | 主要用户群 |
|------|------|-----------|----------|
| **Claude Code** | AI 编程助手（CLI/IDE） | Markdown Skill 文件 | 开发者 |
| **Cursor** | AI 代码编辑器 | Rules 文件（`.cursorrules` / `.cursor/rules/`） | 开发者 |
| **Windsurf** | AI 代码编辑器 | Rules 文件（`.windsurfrules` / Cascade Rules） | 开发者 |
| **Dify** | AI 应用构建平台 | Tools / Workflows / Prompts | 企业/开发者 |
| **Coze** | AI Bot 构建平台 | Plugins / Skills / Workflows | 企业/个人 |

---

## 2. 各平台 Skill 机制详解

### 2.1 Claude Code

**文件格式：** Markdown（`.md`）+ YAML frontmatter

```markdown
---
name: commit
description: 生成 Git 提交信息
allowed-tools:
  - Bash
---

分析暂存区变更，生成 Conventional Commits 格式的提交信息...
```

**存储路径：**
- 全局：`~/.claude/skills/`
- 项目：`.claude/skills/`

**触发方式：** `/skill-name [args]`

**参数传递：** `$ARGUMENTS` 占位符

**工具权限：** frontmatter 中 `allowed-tools` 字段声明

**特色能力：**
- Hook 机制（事件驱动自动触发）
- 多级作用域（全局/项目）
- 与 CLAUDE.md 协同工作

---

### 2.2 Cursor

**文件格式：** 纯文本或 Markdown（无固定结构要求）

**存储路径（两种方式）：**

```
方式一：项目根目录的单文件
{project-root}/.cursorrules

方式二：目录式（推荐，支持模块化）
{project-root}/.cursor/
└── rules/
    ├── general.mdc
    ├── react-components.mdc
    └── api-design.mdc
```

**触发方式：**
- `.cursorrules`：始终生效（注入 System Prompt）
- `.cursor/rules/*.mdc`：支持条件触发（通过 `globs` 字段匹配文件）

**`.mdc` 文件格式（模块化 Rules）：**

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

**特色能力：**
- `globs` 字段：根据文件路径自动激活对应 Rule
- `alwaysApply`：控制 Rule 是否始终注入
- 支持 `@file` 引用其他文件内容

---

### 2.3 Windsurf

**文件格式：** 纯文本（`.windsurfrules`）或结构化 Markdown

**存储路径：**
```
{project-root}/.windsurfrules       # 项目级规则
~/.windsurfrules                    # 全局规则（部分版本支持）
```

**触发方式：** 始终生效（注入 System Prompt）

**Windsurf Rules 格式示例：**

```
# 项目背景
这是一个基于 React + TypeScript 的前端项目，使用 Vite 构建。

# 代码规范
- 所有组件使用函数式写法
- 状态管理使用 Zustand
- 样式使用 Tailwind CSS

# 禁止行为
- 不使用 any 类型
- 不直接修改 state，始终使用 setState
```

**特色能力：**
- Cascade AI 系统（多模型协同）
- 支持 Flows（类似工作流）
- 支持在对话中动态更新 Rules

---

### 2.4 Dify

**文件格式：** 可视化配置 + YAML/JSON 定义

**核心 Skill 形态：**

#### Tools（工具）
封装外部 API 调用能力。

```yaml
# Tool 定义示例
name: weather-query
description: 查询指定城市的天气信息
parameters:
  - name: city
    type: string
    required: true
    description: 城市名称
api_endpoint: https://api.weather.com/query
method: GET
```

#### Workflows（工作流）
可视化编排多步骤任务流程，支持条件分支、循环、并行等。

```
触发 → [LLM 节点] → [工具节点] → [条件判断] → [输出节点]
                                     ↓
                              [另一个分支]
```

#### Prompts（提示词模板）
可复用的提示词模板，支持变量插值。

```
你是一个{{role}}专家。
用户的问题是：{{question}}
请从以下角度回答：
1. {{angle_1}}
2. {{angle_2}}
```

**触发方式：**
- 用户在对话中发送消息触发
- API 调用触发
- 定时触发（部分版本）

**特色能力：**
- 可视化工作流编排
- 内置向量数据库（知识库）
- 多模型切换
- RAG（检索增强生成）支持

---

### 2.5 Coze

**文件格式：** 可视化配置 + JSON Schema

**核心 Skill 形态：**

#### Plugins（插件）
类似 Dify Tools，封装外部服务能力。

```jsonc
{
  "name": "search_web",
  "description": "搜索网络信息",
  "parameters": {
    "type": "object",
    "properties": {
      "query": {
        "type": "string",
        "description": "搜索关键词"
      }
    },
    "required": ["query"]
  }
}
```

#### Workflows（工作流）
图形化流程编排，支持多 Bot 协作。

#### Knowledge Base（知识库）
内置 RAG 系统，支持文档上传和向量检索。

**触发方式：**
- 对话中自然语言触发
- 斜杠命令（部分 Skill）
- API 调用

**特色能力：**
- 多 Bot 协作编排
- 定时任务触发
- 内置消息推送（飞书/钉钉/微信等）
- 丰富的官方插件市场

---

## 3. 横向对比

### 3.1 核心维度对比

| 维度 | Claude Code | Cursor | Windsurf | Dify | Coze |
|------|-------------|--------|----------|------|------|
| **Skill 定义方式** | Markdown 文件 | 文本/Markdown 文件 | 文本文件 | 可视化 + YAML | 可视化 + JSON |
| **触发方式** | 斜杠命令 | 自动生效/文件匹配 | 自动生效 | 对话/API | 对话/API |
| **参数传递** | $ARGUMENTS | 上下文注入 | 上下文注入 | 变量插值 | 变量插值 |
| **工具集成** | 内置工具 + MCP | 内置工具 + MCP | 内置工具 | 自定义 Tools | Plugins |
| **工作流** | 通过 Skill 组合 | 不支持 | Flows | 可视化 Workflow | 可视化 Workflow |
| **多级作用域** | 全局/项目 | 项目级 | 项目级 | 应用级 | Bot 级 |
| **版本控制** | Git 友好 | Git 友好 | Git 友好 | 平台内部 | 平台内部 |
| **学习曲线** | 低（纯 Markdown） | 低 | 低 | 中 | 中 |
| **扩展能力** | MCP 协议 | MCP 协议 | 有限 | 强 | 强 |

### 3.2 适用场景对比

| 场景 | 推荐平台 | 原因 |
|------|---------|------|
| **个人开发效率提升** | Claude Code / Cursor | 本地化、代码集成深 |
| **团队统一规范** | Claude Code（项目 Skill） | Git 版本控制友好 |
| **企业 AI 应用构建** | Dify / Coze | 可视化、多模型、内置知识库 |
| **跨团队多 Bot 协作** | Coze | 内置消息平台集成 |
| **RAG + 知识库应用** | Dify | RAG 能力最成熟 |
| **代码编辑辅助** | Cursor / Windsurf | IDE 深度集成 |
| **自动化脚本/Hook** | Claude Code | Hook 机制独特 |

---

## 4. Skill 跨平台迁移指南

### 4.1 Claude Code → Cursor

```markdown
# Claude Code Skill
---
name: code-review
description: 代码审查
allowed-tools: [Read, Grep]
---
对提供的代码进行 Code Review，输出问题列表...
```

```markdown
# 对应 Cursor Rule（.cursor/rules/code-review.mdc）
---
description: 当用户要求代码审查时自动应用
globs: "**/*.{js,ts,py,java}"
alwaysApply: false
---
对提供的代码进行 Code Review，输出问题列表...
```

**迁移注意点：**
- Cursor 无法声明工具权限（使用 Cursor 内置工具集）
- 触发方式从斜杠命令变为文件匹配或上下文推断
- `$ARGUMENTS` 参数传递需改为上下文注入

### 4.2 Claude Code → Dify Tool

```python
# Dify Tool 实现（Python）
def execute(city: str) -> dict:
    """对应 Claude Code 中调用外部 API 的 Skill 逻辑"""
    response = requests.get(f"https://api.weather.com/{city}")
    return response.json()
```

**迁移注意点：**
- Claude Code Skill 中的 Bash 调用需改写为 Dify Tool 的 Python 函数
- 参数从 `$ARGUMENTS` 改为 Dify 的结构化 parameters 定义
- 错误处理需符合 Dify Tool 的响应格式

### 4.3 通用迁移原则

1. **提取核心指令**：Skill 的本质是 Prompt，提取指令部分可在任何平台复用
2. **工具调用重写**：不同平台的工具集不同，需重新适配
3. **参数机制适配**：从 `$ARGUMENTS` 改为目标平台的参数传递方式
4. **测试验证**：迁移后在目标平台充分测试，避免行为差异

---

## 5. 平台选型决策树

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
