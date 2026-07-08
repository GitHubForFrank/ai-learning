# 工具无关的 Agent 套件

> 版本：1.0 | 定位：跨模型可复用 / 多角色协作

04 章给出了 SDD 在不同 AI 编程工具里的落地骨架，但还有两个常见落差需要在本章解决：

1. 不同工具的"项目级约束自动加载"机制各异（Claude Code 读 `CLAUDE.md`、Cursor 读 `.cursor/rules/`、AGENTS.md 是多家工具的趋同约定），换工具就要重适配
2. 单会话靠人工"切角色"——**漏一句提示词就跑偏**，且没人管你漏没漏

这一章给一套**纯文本、可版本化、跨模型可复用**的 agent 套件，让你换任何大模型都能 5 分钟把 SDD 跑起来，并且把"角色约束"从人脑搬到文件里，机器替你守红线。

---

## 1. 为什么把"约束"和"角色"都做成文件

| 痛点 | 文件化的好处 |
|---|---|
| 每次会话都要重打提示词 | 复制粘贴即用，约束不会漏 |
| 单条 prompt 太长，AI 注意力稀释 | 拆成阶段化角色，每段只读自己那份 |
| 换模型就要重写一遍 | 文件本身工具无关，只是"加载方式"不同 |
| 多人协作约束不一致 | 文件入库，全队同一份 |
| 偶尔写得好的 prompt 凭记忆 | Git 历史可追溯、可 review |

**核心原则**：**约束 = 数据，模型 = 引擎**。模型可换，约束不可丢。

---

## 2. 项目级约束：从 CLAUDE.md 到 AGENTS.md

### 2.1 它们是同一个东西吗

是。本质都是一份**项目级铁律**：编码约定、目录结构、SDD 流程、AI 必守红线。差别只在"谁会自动读它"。

| 文件 | 谁会自动读 | 工具中立度 |
|---|---|---|
| `AGENTS.md` | Cursor / Windsurf / Aider / Continue / Cline 等多家工具的趋同约定 | 高（**推荐**） |
| `CLAUDE.md` | Claude Code | 仅一家 |
| `.cursor/rules/` | Cursor 专属 | 仅一家 |
| `.windsurfrules` / Cascade Rules | Windsurf 专属 | 仅一家 |
| `.clinerules` | Cline / Roo Code 专属 | 仅一家 |

**自动加载 vs 手动粘贴的取舍**：

- 自动加载：减少粘贴成本、不会忘
- 手动粘贴：更灵活（可按场景选择性加载）、不依赖工具版本特性

两种方式没有绝对优劣，但跨工具协作时**自动加载机制不一致**反而是负担。所以推荐策略：

> **以 `AGENTS.md` 为单一真源**（覆盖最多工具），其他厂商私有文件只写一行 `See AGENTS.md` 级联。
>
> 例：`CLAUDE.md` 只写 `See AGENTS.md` / `.cursor/rules/00-base.mdc` 内容只写 `@AGENTS.md`。

这样换工具不用动核心内容，团队成员用什么工具都没差。

### 2.2 AGENTS.md 最小模板

```markdown
# 项目级 Agent 约定

> 任何 LLM 在本仓库工作，必须遵循本文件。

## 1. 流程红线
- 新功能必须先在 docs/specs/ 下生成 spec，再编码
- spec 未标注 [APPROVED] 前，不合入主干实现代码
- 未引用 spec 编号的 PR 一律重来

## 2. 文档约定
- spec 路径：docs/specs/NNNN-<slug>.md
- ADR 路径：docs/adr/NNNN-<slug>.md
- spec 引用格式：spec NNNN §X.Y

## 3. AI 默认行为
- 收到新需求：先反问澄清，禁止直接写代码
- spec 未覆盖的决策：必须先反问，不要自作主张
- 输出代码：每个新函数 / 类的 doc-comment 引用 spec 章节号

## 4. 角色提示词索引
- Phase A 需求澄清：prompts/agent-clarifier.md
- Phase B spec 起草：prompts/agent-spec-author.md
- spec 评审：prompts/agent-reviewer.md
- Phase C 实现：prompts/agent-implementer.md
- 测试设计：prompts/agent-test-designer.md
```

> 关键：AGENTS.md 只写**硬约束 + 流程红线**，教程 / 编码风格指南外链，避免上下文爆炸。

### 2.3 在不同模型里加载

| 工具 / 模型 | 加载方式 |
|---|---|
| Cursor / Windsurf / Aider / Continue / Cline | 直接读 `AGENTS.md` |
| Claude Code | `CLAUDE.md` 写 `See AGENTS.md`，自动级联 |
| ChatGPT / Gemini Web | 会话开头粘贴或上传文件 |
| 自定义 GPT / Gem | 写入 system prompt 永久生效 |
| 直连 API 调用 | 拼到 system message |
| 自研 Agent 框架（LangGraph / AutoGen 等） | 加载到 system prompt 或 ContextProvider |

---

## 3. 角色 agent 套件（5 个核心 + 1 个进阶）

每个 agent 就是一份 markdown，结构统一：**何时上场 / 红线 / system prompt / 输出契约**。前 5 个（§3.1–§3.5）是任意 SDD 项目的基础套件；第 6 个（§3.6 Task 守护者）是多仓 / 多 Task 项目的进阶 agent，单仓项目可跳过。

### 3.1 需求澄清官（agent-clarifier）

- **何时上场**：用户描述新需求 → spec 起草前
- **红线**：只问不答，不给方案；每轮 ≤ 3 个问题

```
你是需求澄清官。我会描述一个新需求，你的唯一任务是**提问**——
不给方案，不写 spec，不写代码。

每轮最多 3 个问题，按以下优先级排：
1. 用户 / 场景边界（谁用、什么时候用、不为谁服务）
2. 范围边界（本期做什么、不做什么）
3. 验收边界（怎么算完成）
4. 非功能边界（性能 / 安全 / 兼容）

我说"够了"时，停止提问并简要总结澄清结果，交给下一阶段。
```

### 3.2 spec 作者（agent-spec-author）

- **何时上场**：澄清官交接后 → spec draft 产出
- **红线**：9 模板必走完；缺信息用 `[TBD]` 占位，绝不杜撰；不写代码

```
你是 spec 撰写人。基于上文澄清结果，按 9 模板生成 spec：
1. 需求背景 / 2. 业务目标 / 3. 功能范围 / 4. 非功能需求 /
5. 接口定义 / 6. 数据模型 / 7. 技术方案 / 8. 风险评估 / 9. 验收标准

硬约束：
- 缺失信息一律 [TBD: <问题>] 占位，禁止杜撰
- 验收标准必须可机械验证（Given-When-Then 或可断言条件）
- 写完输出 TBD 清单等我确认
- 不要开始写代码

输出路径：docs/specs/NNNN-<slug>.md
```

### 3.3 杠精评审员（agent-reviewer）

- **何时上场**：spec draft 完成 → approved 前
- **红线**：必须刻薄；模糊词、缺验收、无风险缓解一律标 H

```
你是一位刻薄的 spec 评审员。逐节审查给定 spec，输出表格：

| 节 | 问题 | 严重度(H/M/L) | 改进建议 |

重点关注：
- 模糊词："尽快" / "多种" / "一般" / "等等"
- 验收标准能否机械式验证
- 每条风险是否都有缓解措施
- 非目标是否显式列出
- 接口是否覆盖异常路径
- TBD 项是否清零
- 过度设计信号（"未来可扩展为"等）
```

### 3.4 实现工程师（agent-implementer）

- **何时上场**：spec approved → 写代码
- **红线**：严格按 spec；未覆盖的决策必反问

```
你是实现工程师。我已批准 spec：<路径> v<版本>。

硬性要求：
1. 实现前先列出"准备改动的文件清单 + 对应 spec 章节"，等我确认
2. 不要新增 spec 未描述的能力；若必要，停下提议修 spec
3. 每个新函数 / 类的 doc-comment 引用 spec 章节号
4. 验收标准每条对应一个测试用例，用例名含 AC 编号
5. 任何 spec 未覆盖的决策必须反问，不要自作主张

完成后输出：
- 改动文件清单
- AC ↔ 测试映射表
- 测试结果（通过 / 失败）
```

### 3.5 测试设计师（agent-test-designer，可选）

- **何时上场**：spec approved 后、实现前；或 brownfield 补测试
- **红线**：每条 AC 至少一个用例；命名带 AC 编号

```
你是测试设计师。基于 spec <路径> §9 验收标准，输出测试用例清单。

输出格式（不写实现代码）：
| AC 编号 | 测试用例名 | 输入 | 预期输出 | 类型(unit/integration/e2e) |

要求：
- 每条 AC 至少一个用例
- 异常路径单列
- 性能 / 压力相关 AC 标 NFR
- 输出后等我确认，再去写实现代码
```

### 3.6 Task 守护者（agent-task-guardian，多仓 / 多任务项目必备）

- **何时上场**：开始一个新 Task / 准备拉分支 / 准备改 DDL / 准备开 PR
- **红线**：检测到"反例信号"立即叫停，不放水；不替你写代码、不改 spec、不跑测试
- **配套**：与 [15 章 Task 管理与环境晋级](./15-SDD-Task管理与环境晋级.md) 的纪律强绑定，单仓项目可跳过本 agent

```
你是 SDD Task 守护者。我会在三个节点求助你：
(A) 写代码前（Task 启动 / 拉分支）
(B) 写代码中（每次提交前自查）
(C) 写代码后（提 PR 前）

每个节点你的工作只有一件事：按 checklist 逐项验证，未打勾或无豁免理由 → 叫停。

【checklist · 写代码前 (A)】
- [ ] Task 编号已登记（格式 Task<NNN>）
- [ ] 锚定的 spec 版本 vX.Y.Z 已确定，知道本 Task 实现哪些条款
- [ ] 影响仓清单已盘点（≥2 仓必须先开 Epic）
- [ ] feature flag 名已起好（feat_task<NNN>_<短语>）
- [ ] 涉及破坏性 DDL → 已拆 Expand-Contract 两个 Task
- [ ] Task 轨迹 md 已新建

【checklist · 写代码中 (B)】
- [ ] 关键代码位置含双标签注释 [SDD-TASK: TaskNNN][SDD-SPEC: ...]
- [ ] 迁移脚本命名 V<N>__task<NNN>_*.sql，N 全局递增不撞号
- [ ] 已发布的迁移脚本一个字符都不能改
- [ ] feature flag 真接入（代码里有 if(flag)），不是只起了名字没用
- [ ] 发现 spec 缺/矛盾 → 停手回 spec PR，不允许"先写着，到时候补 spec"

【checklist · 写代码后 (C)】
- [ ] PR 标题 = `Task<NNN>: <动词起头描述>`
- [ ] PR 描述含 ## Task / ## Implements / ## Covers 三段
- [ ] 三维矩阵（spec §9）已回填本 Task 的 BR×模块×TC 单元格
- [ ] 跨仓 PR 顺序：spec → db → backend → frontend
- [ ] flag matrix / release plan 已同步（涉及环境晋级时）
- [ ] Task 轨迹 md 的"完成态"段已填

【反例（看到立即叫停）】
1. 业务逻辑 spec 没写但代码偷偷加 → 停手回 spec PR
2. 在代码仓塞迁移脚本（应归 db 仓） → 移走
3. 改一个空格也动了已 merge 的迁移脚本 → 新增反做脚本
4. PR 标题没 Task<NNN>: 前缀 / 1 PR 跨多 Task → 拆 PR
5. spec PR 还没 merge，下游 PR 已经在改 → 等 spec 合 + 打 tag
6. 起了 flag 名但代码里没接入 / 永远 true → 真用 if(flag) 包关键路径
7. 破坏性 DDL 一个 Task 搞定 → 拆 Expand-Contract
8. PR 描述少了三段中的任何一段 → 补齐

【你不能干的】
- ❌ 写业务代码、做技术决策（开发者的事）
- ❌ 修改 spec（必须走 spec 仓 PR + 发版）
- ❌ 跑测试 / 部署 / 合 PR（CI 和开发者的事）
- ❌ "圆滑处理"模糊地带——检测到反例必须叫停，不放水
```

> **底线**：本 agent 的存在是让"人 + AI"都不依赖记忆来执行 SDD 纪律。**checklist 没过 = PR 不能合**，无论这次 review 的人是不是你自己。完整版（含全部 conventions 索引、双标签注释格式、轨迹 md 模板）见实战项目 `04-SDD 实战项目/sdd-02-task-fullstack/task-spec/agents/task-guardian.md`。

---

## 4. 多 agent 协作拓扑

### 4.1 串行（最简、最常用）

```
[澄清官] → [spec 作者] → [评审员] → [实现工程师] → [测试设计师]
  会话1       会话2         会话3         会话4            会话5
```

每段建议**独立会话**：上下文干净、角色不混、失败可回退。代价是每次开场要重粘 AGENTS.md + 当前角色 prompt + 上一阶段产出。

### 4.2 交叉审查（高保真）

```
              ┌── [评审员A 会话]
[spec 作者] ──┤                 → 合并意见 → [spec 作者修订]
              └── [评审员B 会话]
```

两个独立会话独立审 spec，**互不污染**——避免 AI 顺着自己的 spec 漏点也放过去。可同模型双开，也可换不同模型形成"多模型陪审"。

### 4.3 并行（跨栈）

前后端 + DB 各起一份子 spec 时，三个"实现工程师"会话**并行推进**，都引用主 spec + 各自子 spec（搭配 07 章 跨栈协作）。

---

## 5. 文件落地结构

```
<project-root>/
├── AGENTS.md                       # 厂商中立项目约束（必入库）
├── CLAUDE.md                       # 仅一行 "See AGENTS.md"（如团队用 Claude Code）
├── prompts/
│   ├── agent-clarifier.md          # 5+1 个角色提示词（必入库 + 走 PR review）
│   ├── agent-spec-author.md
│   ├── agent-reviewer.md
│   ├── agent-implementer.md
│   ├── agent-test-designer.md
│   └── agent-task-guardian.md      # 多仓 / 多 Task 项目必备（见 §3.6）
├── docs/
│   ├── specs/
│   │   └── NNNN-<slug>.md
│   └── adr/
└── src/
```

**关键**：`prompts/` 必须入库、版本化。提示词改动走 PR review，跟代码一样严肃——它们就是"AI 的源代码"。

---

## 6. 在不同模型里的实操对照

| 模型 / 工具 | AGENTS.md 怎么加载 | 角色 agent 怎么用 |
|---|---|---|
| Cursor | 自动读 AGENTS.md | `.cursor/rules/` 或对话粘贴 |
| Windsurf | 自动读 AGENTS.md / `.windsurfrules` | Cascade Rules 或对话粘贴 |
| Cline / Roo Code | 自动读 AGENTS.md / `.clinerules` | 自定义 instructions / Plan-Act 切换 |
| Aider | 自动读 AGENTS.md | 启动参数 / 对话粘贴 |
| Continue | 自动读 AGENTS.md 或 config.yaml | systemMessage / slashCommands |
| Claude Code | `CLAUDE.md` 引用，自动级联 | 子 agent / Skill / 会话切换 |
| ChatGPT Web / Custom GPT | 会话开头粘贴 / Custom GPT instructions | 自定义 GPT 或粘贴 system prompt |
| Gemini Web / Gem | 会话开头粘贴或上传文件 | Gem 或粘贴 |
| Qwen / DeepSeek / Kimi 等 | 会话开头粘贴 | system prompt 拼进去 |
| 自研 Agent 框架 | 注入 system prompt / ContextProvider | Agent 节点参数 |

**通用三件套**（任何模型每次会话开场）：

1. 粘 `AGENTS.md`（全局约束）
2. 粘当前角色 prompt（局部职责）
3. 给 spec 路径或贴 spec 内容（任务上下文）

---

## 7. 反模式

| 反模式 | 后果 | 对治 |
|---|---|---|
| 把 5 个角色塞进一个超长 system prompt | 模型注意力稀释，红线被忽略 | 拆角色，按阶段切换会话 |
| AGENTS.md 写成长篇编码教程 | 上下文爆炸、token 浪费 | 只留**硬约束 + 流程红线**，教程外链 |
| 角色 prompt 不入库 | 全队约束不一致，质量随作者波动 | `prompts/` 入 Git + PR review |
| 跨会话不交接产出物 | 后一阶段重复劳动或丢上下文 | 每段结束输出"交接清单"（文件路径 / spec 编号 / 待办） |
| 用同一会话连跑澄清 + spec + 实现 | 角色串味，AI 把澄清当 spec 写 | 阶段间换会话，强制角色边界 |
| 评审员和 spec 作者用同一会话 | AI 自审过松，等于没审 | 起新会话甚至换模型评审 |

---

## 8. 思考与练习

1. **改造题**：把你手边项目的 `CLAUDE.md`（如果有）拆成 `AGENTS.md` + `prompts/`，看哪些条目其实是"角色提示词"被错放在了项目级
2. **跨模型题**：选 1 个角色 prompt，分别在 Claude / ChatGPT / Gemini 跑同一任务，对比输出差异——哪些是模型差异、哪些是 prompt 写得不够紧
3. **极简题**：能否把 5 个角色合并到 3 个？合并的代价是什么？哪两组合并最不伤红线？
4. **开放题**：spec 作者用便宜模型 / 评审员用强模型，会有什么效果？参考 12 章模型分工

---

## 9. 参考与延伸

- 04 章 §4 三阶段角色切换 — 本章的概念基础
- 12 章 模型职责分工与边界点 — 角色 + 模型档位的组合
- 13 章 人和模型的职责分工 — agent 不能代替人决策的红线
- AGENTS.md 是 Cursor / Aider / Continue 等工具趋同的厂商中立约定，可在各自文档内搜索 "AGENTS.md"
