# Claude Code Skill 平台篇 · 面试题库

> 涵盖 Claude Code 专属的 Skill 机制、文件格式、路径、参数及最佳实践

---

## Q3.1 Claude Code 的 Skill 机制有什么核心特性？

**参考答案**：

- **零代码**：纯 Markdown 文件，无需编程——一个 `.md` 文件就是一个 Skill
- **即时生效**：创建/修改文件后立即可用，无需重启
- **参数支持**：通过 `$ARGUMENTS` 接收用户输入
- **工具权限**：可声明 Skill 执行时允许/禁止使用的工具（`allowed-tools` / `disallowed-tools`）
- **多级作用域**：支持全局（`~/.claude/skills/`）、项目（`.claude/skills/`）、内联（`CLAUDE.md`）三级

**难度**：⭐ | **考察点**：平台特性认知

---

## Q3.2 写出一个完整的 Claude Code Skill 文件的 Frontmatter 结构。

**参考答案**：
```yaml
---
# 必填字段
name: skill-name                    # Skill 标识符，对应斜杠命令 /skill-name
description: |                      # Skill 用途描述（支持多行）
  当用户需要...时使用此 Skill。
  适合...场景。
  不适用于...场景。

# 可选字段
allowed-tools:                      # 声明允许使用的工具列表
  - Read
  - Write
  - Edit
  - Bash
  - WebFetch

disallowed-tools:                   # 明确禁止使用的工具
  - Bash

model: claude-opus-4-7              # 指定使用的模型
---
```

**关键字段说明**：
- `name`：使用 `kebab-case`，对应斜杠命令 `/skill-name`
- `description`：Agent 通过此字段做意图匹配——应描述**何时触发、何时不触发**
- `allowed-tools`：未声明时继承全局权限设置

**难度**：⭐⭐ | **考察点**：Skill 文件格式掌握

---

## Q3.3 Skill 文件存储在哪里？项目 Skill 和全局 Skill 的区别是什么？

**参考答案**：

| 作用域 | 路径 | 作用范围 | 版本控制 |
|--------|------|---------|---------|
| 全局 Skill | `~/.claude/skills/` | 所有项目 | 不入库（个人用） |
| 项目 Skill | `.claude/skills/` | 当前项目 | 应纳入 Git（团队共享） |
| 内联 Skill | `CLAUDE.md` 中定义 | 当前项目 | 入 Git |

**关键实践**：
- 项目 Skill 纳入 Git 版本控制，供团队共享
- 个人本地状态（`settings.local.json`）忽略
- 含敏感信息的 Skill 应单独 `.gitignore`（如 `private-*.md`）

**难度**：⭐ | **考察点**：路径管理

---

## Q3.4 写一个实战的 commit Skill，包含完整的 frontmatter 和执行指令。

**参考答案**：
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
4. 生成提交信息，格式：`<type>(<scope>): <description>`

类型规范：
- feat: 新功能
- fix: Bug 修复
- docs: 文档变更
- refactor: 代码重构
- test: 测试相关
- chore: 构建或辅助工具变更

约束：
- subject 使用中文，不超过 50 个字
- 如果暂存区为空，提示用户先 git add
```

**难度**：⭐⭐ | **考察点**：实际操作能力

---

## Q3.5 Skill 无法被识别时如何排查？

**参考答案**：
排查步骤：
1. 确认文件路径正确：`~/.claude/skills/skill-name.md` 或 `.claude/skills/skill-name.md`
2. 确认文件名与 frontmatter 的 `name` 字段一致
3. 确认 frontmatter 格式正确（YAML 语法，使用 `---` 分隔）
4. 尝试重新加载（重启 Claude Code 或刷新项目上下文）

**常见原因**：
- 文件名与 name 不匹配
- YAML frontmatter 格式错误
- 文件放错目录
- description 写得过于模糊

**难度**：⭐ | **考察点**：调试能力

---

## Q3.6 Skill 与 CLAUDE.md / AGENTS.md 如何协同？

**参考答案**：

- Skill 是"**按需触发**"的细粒度指令
- CLAUDE.md / AGENTS.md 是"**始终生效**"的项目级铁律
- Skill **不应重复** AGENTS.md 已写过的全局约束，避免上下文冗余
- 如果 Skill 行为与全局规则冲突，在 Skill 内**显式覆写**相关行为

```
CLAUDE.md       → 始终注入的全局约束（项目背景、代码风格、禁止行为）
Skill           → 按需加载的专项指令（commit、review、api-doc）
Hook            → 事件驱动的自动执行（PreToolUse 检查、PostToolUse 格式化）
```

**难度**：⭐⭐ | **考察点**：协同设计理解

---

## Q3.7 `allowed-tools` 和 `disallowed-tools` 的实践意义是什么？

**参考答案**：
通过显式声明工具权限，实现**最小权限原则**：

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
```

**实践意义**：
- 防止 Skill 误操作（如审查 Skill 不应修改文件）
- 安全边界控制（网络访问 Skill 需显式声明 WebFetch）
- 权限未声明时继承全局设置

**难度**：⭐ | **考察点**：安全实践

---

> 参考来源：笔记 `01-Claude-Code-Skill完全指南.md`
