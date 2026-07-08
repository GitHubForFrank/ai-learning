# workitem-docs/agents/:工具无关的 agent 预设

> **本目录是什么**:把 SDD 流程中"反复要校验的纪律"压缩成可被任意 AI 工具加载、也可由人类直接对照的 markdown checklist。
>
> **为什么不放在 `.claude/agents/`**:那是 Claude Code 私有目录;本目录中的 agent 必须**工具无关**,因为开发者可能用 Cursor / Copilot / Aider / Trae / 其他工具,或完全不用 AI。

---

## 一、目录约定

```
workitem-docs/agents/
├── README.md            # 本文件:索引 + 调用方式 + 演进规则
├── task-guardian.md     # SDD Task 开发全程护栏(写代码前 / 中 / 后 三阶段 checklist)
└── (future) ...         # 后续可能新增:spec-reviewer / migration-reviewer / e2e-author 等
```

每个 agent 一个 `.md` 文件。文件名 `<role>.md`,小写 + 连字符。

## 二、agent 索引

| Agent | 用途 | 何时召唤 |
|---|---|---|
| [`task-guardian.md`](./task-guardian.md) | SDD Task 全程护栏:Task 编号 / 双标签 / PR 模板 / Flyway / feature flag / 三维矩阵等纪律的 checklist | 开 Task / 提 PR / 改 V*.sql / 改 spec 时 |

后续若新增 agent,**在表格里追加一行,且写明"何时召唤"**,避免开发者不知道该用哪个。

## 三、调用方式(三类工具 + 无 AI 场景)

### 3.1 Claude Code

直接在对话里 `@workitem-docs/agents/task-guardian.md`,或在仓库级 `.claude/agents/` 做薄壳引用本文件(YAGNI 原则:真要换工具时再加,不预制)。

### 3.2 Cursor / Continue / Cline 等基于规则文件的工具

在该工具的规则文件(如 `.cursorrules`)里加一行 `include workitem-docs/agents/<role>.md`,或把本目录下文件内容粘贴到工具的 Rules 设置中。

### 3.3 GitHub Copilot Chat

在 `.github/copilot-instructions.md` 里引用本目录下的 agent 文件路径。

### 3.4 Aider

把 agent 文件加入 `--read` 启动参数,或写入 `CONVENTIONS.md` 引用本目录路径。

### 3.5 其他 AI(通用兜底)

在对话里贴一句:"请严格按 `workitem-docs/agents/<role>.md` 校验本次改动",让 AI 自行加载内容。

### 3.6 不用 AI(纯人工)

开发者 PR 自检 / code review 时,按对应 agent 的 checklist 逐项打勾即可 —— **本目录的产物不依赖任何 AI**。

---

## 四、agent 文件结构约定

每个 agent 文件应至少包含以下章节(顺序可调,不强制每节都有,但缺章节要在文首说明为什么):

| 章节 | 内容 |
|---|---|
| frontmatter(`---`) | `name` / `description` / `trigger_keywords` / `spec_anchor`,工具加载用 |
| 一、角色与边界 | 干嘛 / 不干嘛(防止 agent 越权) |
| 二、何时召唤 | 触发场景(让开发者知道什么时候该用) |
| 三、Checklist | 核心产出,可分阶段或按主题 |
| 四、常见反例 | 已踩过的坑;**只收真踩过的,不收假想** |
| 五、spec 条款速查 | 只列 checklist 直接命中的,其余靠 specs/ 间接查 |
| 六、不同 AI 工具的调用方式 | 与 §三对齐,但本文件级别可裁剪 |

---

## 五、演进规则

- 本目录的 agent 是**约束 spec 的延伸**,改动走 `workitem-docs` 仓的 PR(标题前缀 `Task<NNN>:`)
- **不发明新规则** —— agent 只能把 `specs/` + `06/07/08` 已定稿的纪律压缩成 checklist
- 新增 agent 前先问:"这个 checklist 真的已经在 `specs/` 里有真源了吗?" 没有 → 先写 convention,再来加 agent
- 反例(§四)只收**真踩过的坑**;假想反例不进
- 任何新增 / 修改 agent 文件时,**同步更新本 README §二 索引**

---

## 六、与项目其他 spec 的边界

- `specs/` —— "什么是对的"(规则 / 命名 / 结构,**抽象规范**)
- `06/07/08` —— "Task 怎么管 / 并行怎么做 / 环境怎么晋级"(**流程纪律**)
- `agents/` —— **"这些规则和流程,在你即将写代码 / 提 PR 的此刻,该校验哪些"**(执行时的 checklist)

三者不重复:agents 不复述规则细节,只引用真源 + 给出"此刻该问自己什么"。
