# Skill 类型与触发机制

> 版本：1.0 | 定位：基础理论

---

## 1. Skill 类型分类

### 1.1 按触发方式分类

#### 用户可调用 Skill（User-Invokable Skill）
由用户**显式触发**，通常通过斜杠命令（`/skill-name`）或界面按钮激活。

```
特点：
- 用户主动控制执行时机
- 触发行为明确、可预期
- 适合需要用户提供额外输入的场景

典型触发形式：
  /commit              → 生成 Git 提交信息
  /review              → 代码审查
  /translate zh        → 翻译为中文
```

#### 代理自调用 Skill（Agent-Triggered Skill）
由 Agent 根据**上下文语义**判断自动调用，无需用户干预。

```
特点：
- Agent 识别意图后自动匹配并执行 Skill
- 降低用户学习成本（无需记住命令）
- 适合功能明确、边界清晰的场景

触发逻辑示例：
  用户说"帮我提交代码" → Agent 自动识别并执行 commit Skill
  用户说"检查这段代码" → Agent 自动执行 code-review Skill
```

#### 事件驱动 Skill（Event-Driven Skill）
由系统事件自动触发，不依赖用户交互。

```
特点：
- 与工具钩子（Hook）机制结合
- 在特定动作前/后自动执行
- 适合流程守卫、自动化检查场景

触发示例（以 Claude Code hooks 为例）：
  PreToolUse:Bash     → 在执行 Bash 命令前触发安全检查 Skill
  PostToolUse:Edit    → 在文件编辑后触发代码格式化 Skill
  Stop                → 会话结束时触发总结 Skill
```

---

### 1.2 按功能形态分类

#### 纯指令型 Skill（Instruction-Only Skill）
不需要外部工具，仅通过 Prompt 指令影响 Agent 行为。

```markdown
---
name: writing-coach
description: 以写作教练身份审阅并改善文章质量
---

你现在是一位资深写作教练，擅长商业写作和技术文档。
审阅用户提供的文本，从以下维度提供具体改进建议：
1. 逻辑结构是否清晰
2. 表达是否简洁准确
3. 读者视角是否友好
给出修改后的版本，并说明每处改动的原因。
```

#### 工具增强型 Skill（Tool-Enhanced Skill）
调用外部工具（文件读写、API、数据库等）完成任务。

```markdown
---
name: api-client
description: 调用外部 API 获取数据并分析
allowed-tools: Bash, Read, WebFetch
---

使用提供的工具调用目标 API，获取响应数据。
对返回的数据进行结构化分析，重点说明：
1. 数据格式和关键字段含义
2. 潜在的异常情况
3. 建议的处理方式
```

#### 参数化 Skill（Parameterized Skill）
接受运行时参数，支持动态内容填充。

```markdown
---
name: generate-pr-description
description: 根据分支和目标生成 PR 描述
---

你是代码审查专家。基于以下信息生成专业的 PR 描述：
- 变更类型：$ARGUMENTS
- 涉及模块：根据工具读取的文件变更自动分析

PR 描述必须包含：
## 变更摘要
## 测试方案
## 注意事项
```

#### 组合 Skill（Composite Skill）
编排多个子任务，实现复杂工作流。

```markdown
---
name: full-feature-workflow
description: 完整的功能开发工作流（设计→开发→测试→文档）
---

按照以下步骤完成功能开发：

**Step 1: 需求分析**
分析用户描述的功能需求，列出：
- 核心功能点（3-5条）
- 边界条件和异常场景
- 与现有系统的依赖关系

**Step 2: 接口设计**
设计 RESTful API 接口，输出 OpenAPI 格式的接口定义。

**Step 3: 实现指导**
根据项目技术栈（从 CLAUDE.md 读取），给出分层实现建议。

**Step 4: 测试用例**
生成覆盖正常流程和边界场景的测试用例。

**Step 5: 文档更新**
更新 README.md 的功能列表和 API 文档。
```

---

## 2. 触发机制详解

### 2.1 斜杠命令触发

最常见的用户可调用 Skill 触发方式。

```
语法：/{skill-name} [args]

示例：
  /commit                    → 不带参数触发
  /translate zh              → 带参数触发（翻译为中文）
  /review strict             → 带修饰参数（严格模式审查）
  /generate-test UserService → 带目标对象参数
```

**参数传递规则：**
- 斜杠命令后的所有内容作为 `$ARGUMENTS` 传递给 Skill
- Skill 内容通过 `$ARGUMENTS` 占位符引用用户传入的参数
- 复杂参数建议使用空格分隔或 JSON 格式

### 2.2 自然语言触发

Agent 通过语义理解自动匹配 Skill。

```
触发示例：
  "帮我写提交信息"     → 匹配 commit Skill
  "审查一下这段代码"   → 匹配 code-review Skill
  "翻译这篇文章"       → 匹配 translate Skill
```

**实现方式：**
- 在 Skill 的 `description` 字段中描述**触发意图**而非功能名称
- Agent 将用户输入与所有 Skill 的 description 做语义匹配
- 匹配度最高的 Skill 被选中执行

### 2.3 Hook 事件触发

通过配置 hooks 在特定事件发生时自动触发 Skill 或脚本。Hook 机制目前**没有跨工具的统一标准**，各家实现差异较大。

#### Claude Code 风格的 Hook 配置

```jsonc
// .claude/settings.json
{
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Bash",
        "hooks": [
          {
            "type": "command",
            "command": "echo '即将执行 Bash 命令'"
          }
        ]
      }
    ],
    "PostToolUse": [
      {
        "matcher": "Edit",
        "hooks": [
          {
            "type": "command",
            "command": "npx prettier --write $CLAUDE_FILE_PATHS"
          }
        ]
      }
    ]
  }
}
```

**Claude Code 可用 Hook 事件：**

| 事件 | 触发时机 | 典型用途 |
|------|---------|---------|
| `PreToolUse` | 工具调用前 | 安全检查、确认危险操作 |
| `PostToolUse` | 工具调用后 | 格式化代码、更新日志 |
| `Notification` | Agent 发送通知时 | 桌面通知、消息推送 |
| `Stop` | 会话结束时 | 生成摘要、清理临时文件 |
| `SubagentStop` | 子 Agent 结束时 | 汇总子任务结果 |

### 2.4 其他平台的事件触发机制

| 平台 | 等价机制 | 触发粒度 |
|------|---------|----------|
| **Claude Code** | `hooks` 字段（PreToolUse / PostToolUse / Stop 等） | 工具级、会话级 |
| **Cursor / Windsurf** | Rules 文件的条件字段（`alwaysApply` / glob match） | 文件级、上下文级；无显式工具 Hook |
| **Cline / Roo Code** | Plan / Act 模式切换 + 自定义 instructions | 模式级，由用户主动切换 |
| **Continue** | `config.yaml` 的 `slashCommands` / context providers | 命令触发为主，非事件驱动 |
| **Aider** | `--lint-cmd` / `--test-cmd` / 提交时自动跑命令 | commit 前后、edit 后的 lint/test |
| **Dify / Coze** | Workflow 节点 + 条件分支 + 事件节点 | 流程图节点级 |
| **自研 Agent 系统** | 观察者模式（注册 listener 到 LLM 调用 / 工具调用 / 状态变更） | 完全自定义 |

**核心结论**：
- 想要"工具调用前后插脚本"的精细 Hook，目前以 **Claude Code 最完备**；
- 想要"按文件路径自动注入指令"，**Cursor / Windsurf 的 Rules** 更直接；
- 想要"流程图编排 + 事件分支"，**Dify / Coze** 的 Workflow 是首选；
- 自研系统建议在 Agent 主循环里埋观察者钩子，让事件订阅与业务解耦。

---

## 3. 参数传递机制

### 3.1 命令行参数（$ARGUMENTS）

```markdown
# Skill 定义
---
name: create-component
description: 创建前端组件
---

创建一个名为 **$ARGUMENTS** 的 React 组件。
要求：
- 使用 TypeScript
- 包含 Props 类型定义
- 包含基本单元测试
```

```
# 用户调用
/create-component UserProfile

# 实际执行时 $ARGUMENTS = "UserProfile"
```

### 3.2 上下文变量（Claude Code）

Claude Code 在 Skill 执行时会自动提供以下上下文变量：

| 变量 | 说明 |
|------|------|
| `$CLAUDE_FILE_PATHS` | 当前相关文件路径 |
| `$CLAUDE_TOOL_NAME` | 当前触发的工具名 |
| `$CLAUDE_TOOL_INPUT` | 工具调用的输入参数（JSON） |
| `$CLAUDE_TOOL_RESPONSE` | 工具调用的输出结果（JSON） |

---

## 4. Skill 作用域

### 4.1 作用域层级（以 Claude Code 为例）

```
全局 Skill（~/.claude/skills/）
    ↕ 优先级低
项目 Skill（.claude/skills/ 或 CLAUDE.md 中定义）
    ↕ 优先级高
```

| 作用域 | 路径 | 适用场景 |
|--------|------|---------|
| **全局** | `~/.claude/skills/` | 个人通用 Skill，所有项目可用 |
| **项目** | `.claude/skills/` | 项目专属 Skill，仅在该项目中可用 |
| **内联** | `CLAUDE.md` 中直接定义 | 简单的一次性 Skill |

### 4.2 作用域冲突处理

当全局和项目 Skill 同名时：
- 项目级 Skill **优先覆盖**全局 Skill
- 可通过完整路径引用特定作用域的 Skill

---

## 5. Skill 生命周期

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

---

## 6. 选择合适的 Skill 类型

```
你的需求是什么？
│
├── 固定操作，每次触发相同流程
│   └── 用户可调用 Skill（斜杠命令）
│
├── 根据意图自动匹配，降低用户学习成本
│   └── 代理自调用 Skill（优化 description 字段）
│
├── 需要在特定操作前/后自动执行
│   └── 事件驱动 Skill（Hook 配置）
│
├── 需要调用外部 API 或读写文件
│   └── 工具增强型 Skill（配置 allowed-tools）
│
└── 多步骤复杂工作流
    └── 组合 Skill（分步骤编排）
```
