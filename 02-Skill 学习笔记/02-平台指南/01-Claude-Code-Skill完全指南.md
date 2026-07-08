# Claude Code Skill 完全指南

> 版本：1.1 | **范围：本文只讲 Claude Code 专属机制**
>
> 想看跨平台对比（Cursor / Windsurf / Dify / Coze 等的 Skill 等价物），见 [`02-跨平台Skill机制对比.md`](./02-跨平台Skill机制对比.md)。
>
> 想看与平台无关的 Skill 设计原则（命名、单一职责、最小权限等），见 [`../04-Prompt工程/`](../04-Prompt工程/)。

---

## 1. Claude Code Skill 机制概述

Claude Code 的 Skill 系统通过 **Markdown 文件**实现。每个 `.md` 文件就是一个 Skill，用户通过斜杠命令（`/skill-name`）调用，文件内容会被注入到当前会话上下文中，指导 Claude 执行特定任务。

### 1.1 核心特性

- **零代码**：纯 Markdown 文件，无需编程
- **即时生效**：创建/修改文件后立即可用，无需重启
- **参数支持**：通过 `$ARGUMENTS` 接收用户输入
- **工具权限**：可声明 Skill 执行时允许使用的工具
- **多级作用域**：支持全局、项目、内联三级 Skill

---

## 2. Skill 文件格式

### 2.1 基础格式

```markdown
---
name: skill-name
description: 清晰描述这个 Skill 的用途和触发条件（Agent 用此字段做意图匹配）
---

Skill 的执行指令内容...
```

### 2.2 完整 Frontmatter 字段

```yaml
---
# 必填字段
name: skill-name                    # Skill 标识符，对应斜杠命令 /skill-name
description: |                      # Skill 用途描述（支持多行）
  当用户需要...时使用此 Skill。
  适合...场景。

# 可选字段
allowed-tools:                      # 声明允许使用的工具列表
  - Read
  - Write
  - Edit
  - Bash
  - WebFetch
  - TodoWrite

disallowed-tools:                   # 明确禁止使用的工具
  - Bash

model: claude-opus-4-7              # 指定使用的模型（默认继承当前设置）
---
```

### 2.3 关键字段说明

#### `name` 字段
- 对应斜杠命令触发：`name: commit` → 用 `/commit` 触发
- 使用 `kebab-case`（小写字母 + 连字符）
- 避免使用中文、空格或特殊字符

#### `description` 字段
- 最重要的字段之一：Agent 通过此字段做意图匹配（自动触发时）
- 应描述**何时**使用此 Skill，而非仅描述 Skill 的功能
- 建议包含：使用场景、触发意图、适合的输入类型

```yaml
# 不好的 description（仅描述功能）
description: 生成 Git 提交信息

# 好的 description（描述触发条件和场景）
description: |
  当用户需要提交代码、创建 Git commit 时使用。
  适合在 git add 之后、git push 之前使用。
  会分析暂存区的变更内容，自动生成符合 Conventional Commits 规范的提交信息。
```

#### `allowed-tools` 字段
声明此 Skill 执行过程中**允许**使用的工具集合。未声明时继承全局权限设置。

```yaml
# 只读 Skill（不允许修改文件）
allowed-tools:
  - Read
  - Grep
  - Glob

# 代码生成 Skill（允许写文件）
allowed-tools:
  - Read
  - Write
  - Edit
  - Bash

# 网络访问 Skill
allowed-tools:
  - WebFetch
  - Bash
```

---

## 3. Skill 文件存储路径

### 3.1 全局 Skill
```
~/.claude/skills/
├── commit.md
├── review.md
├── translate.md
└── ...
```
- 对**所有项目**生效
- 适合个人通用 Skill

### 3.2 项目 Skill
```
{project-root}/
└── .claude/
    └── skills/
        ├── backend-review.md
        ├── api-doc.md
        └── ...
```
- 仅对**当前项目**生效
- 项目级 Skill **优先级高于**全局 Skill
- 应纳入版本控制（提交到 Git）

### 3.3 内联 Skill（CLAUDE.md 中定义）
在 `CLAUDE.md` 文件中直接定义简单的 Skill 行为，适合轻量级场景。

```markdown
<!-- CLAUDE.md -->
# 项目指南

## 自定义指令

当用户使用 /style-check 时，按照以下规则检查代码风格：
- 函数名使用 camelCase
- 常量使用 UPPER_SNAKE_CASE
- 每行不超过 120 个字符
```

---

## 4. 参数传递

### 4.1 基础参数：$ARGUMENTS

斜杠命令后的所有内容作为 `$ARGUMENTS` 传入。

```markdown
---
name: explain
description: 详细解释指定的代码概念或文件
---

请详细解释以下内容：**$ARGUMENTS**

解释需要包含：
1. 核心概念和作用
2. 使用场景和示例
3. 常见误区和注意事项
```

```
用户输入：/explain Promise.all

实际执行：请详细解释以下内容：Promise.all
```

### 4.2 参数不存在时的处理

建议在 Skill 中处理参数为空的情况：

```markdown
---
name: create-test
description: 为指定文件或函数生成单元测试
---

{% if $ARGUMENTS %}
为以下目标生成单元测试：$ARGUMENTS
{% else %}
请告诉我需要为哪个文件或函数生成测试用例？
{% endif %}

测试要求：
- 覆盖正常流程和边界条件
- 使用项目现有的测试框架
- 遵循 AAA 模式（Arrange/Act/Assert）
```

---

## 5. 实战示例

### 5.1 代码提交 Skill

```markdown
---
name: commit
description: |
  当需要生成 Git 提交信息时使用。
  会分析 git diff 的暂存区变更，生成符合 Conventional Commits 规范的提交信息。
allowed-tools:
  - Bash
---

分析当前 Git 暂存区的变更，生成符合 Conventional Commits 规范的提交信息。

步骤：
1. 运行 `git diff --staged` 查看暂存区变更
2. 运行 `git log --oneline -5` 参考近期提交风格
3. 分析变更的类型和影响范围
4. 生成提交信息，格式如下：

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

类型（type）规范：
- feat: 新功能
- fix: Bug 修复
- docs: 文档变更
- style: 代码格式（不影响功能）
- refactor: 代码重构
- test: 测试相关
- chore: 构建或辅助工具变更

最后直接执行 git commit 提交（无需用户二次确认）。
```

### 5.2 代码审查 Skill

```markdown
---
name: review
description: |
  对当前文件或指定代码进行 Code Review。
  可带参数指定审查级别：/review strict 或 /review quick
allowed-tools:
  - Read
  - Grep
---

对提供的代码进行 Code Review。审查级别：$ARGUMENTS（默认为 standard）。

审查维度：
1. **功能正确性**：逻辑是否正确，边界条件是否处理
2. **代码质量**：可读性、可维护性、是否遵循 SOLID 原则
3. **安全性**：SQL 注入、XSS、权限漏洞等常见安全问题
4. **性能**：明显的性能问题（N+1 查询、不必要的循环等）
5. **测试覆盖**：是否有对应的单元测试

输出格式：
- 使用 Markdown 表格列出问题
- 按严重程度分级：🔴 Critical / 🟡 Warning / 🟢 Suggestion
- 每个问题附上具体的改进建议
```

### 5.3 API 文档生成 Skill

```markdown
---
name: api-doc
description: |
  为指定的接口文件或控制器生成 OpenAPI 格式的 API 文档。
  用法：/api-doc [文件路径]
allowed-tools:
  - Read
  - Write
  - Glob
---

为以下目标生成 OpenAPI 3.0 格式的 API 文档：$ARGUMENTS

步骤：
1. 读取目标文件，分析接口定义
2. 提取：接口路径、HTTP 方法、请求参数、响应结构、错误码
3. 生成标准 OpenAPI YAML 文档
4. 将文档写入 docs/api.yaml

文档要求：
- 每个接口必须有 summary 和 description
- 请求/响应参数必须有类型说明
- 列出所有可能的 HTTP 状态码
- 标注认证要求（Bearer Token / API Key 等）
```

---

## 6. 常见问题与调试

### 6.1 Skill 无法被识别

**现象：** 输入 `/skill-name` 后 Claude 没有执行 Skill 逻辑

**排查步骤：**
1. 确认文件路径正确：`~/.claude/skills/skill-name.md` 或 `.claude/skills/skill-name.md`
2. 确认文件名与 `name` 字段一致
3. 确认 frontmatter 格式正确（YAML 语法，使用 `---` 分隔）
4. 尝试重新加载（重启 Claude Code 或刷新项目上下文）

### 6.2 参数未正确替换

**现象：** Skill 执行时 `$ARGUMENTS` 显示为字面量

**解决：** 确认 Skill 文件内容中使用了 `$ARGUMENTS` 占位符，且用户在斜杠命令后确实传入了参数。

### 6.3 工具权限被拒绝

**现象：** Skill 执行时提示工具不可用

**解决：** 在 frontmatter 的 `allowed-tools` 中显式声明需要的工具。

### 6.4 Skill 与全局规则冲突

**现象：** Skill 行为与 CLAUDE.md 中定义的规则冲突

**解决：** 在 Skill 中明确覆写相关行为，或调整 CLAUDE.md 中规则的优先级。

---

## 7. Claude Code 专属最佳实践

> 通用的 Skill 设计原则（单一职责、描述精准、最小权限、幂等、含示例）和命名规范（kebab-case、动词-对象）属于跨平台共识，详见 [`../04-Prompt工程/`](../04-Prompt工程/) 与 [`02-跨平台Skill机制对比.md`](./02-跨平台Skill机制对比.md)。本节只列 Claude Code 这边特有的纪律。

### 7.1 项目 Skill 的版本管理

将 `.claude/skills/` 目录纳入 Git 版本控制；个人本地状态（`settings.local.json`、会话缓存等）忽略。

```gitignore
# .claude/skills/ 应该被跟踪（团队共享）
# .claude/settings.json 通常入库（团队约定）
# 但下面这些个人状态需要忽略：
.claude/settings.local.json
.claude/projects/
.claude/todos/
.claude/.credentials.json

# 含敏感信息的 Skill 单独忽略
.claude/skills/private-*.md
```

### 7.2 与 CLAUDE.md / AGENTS.md 的协同

- Skill 是"按需触发"的细粒度指令；CLAUDE.md / AGENTS.md 是"始终生效"的项目级铁律
- Skill 不应重复 AGENTS.md 已写过的全局约束，避免上下文冗余
- 如果 Skill 行为与全局规则冲突，在 Skill 内显式覆写
