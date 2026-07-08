# SDD 工具落地与工作流整合

> 版本：2.0 | 定位：工具无关的落地指南

---

## 1. SDD 与 AI 编程工具的关系

**SDD 本身工具无关**：它是一套"先写规范、再写代码、人 + AI 共守一份契约"的方法论，与具体大模型 / IDE / CLI 都没有强绑定。无论用 Claude Code、Cursor、Aider、Continue，还是直接在 ChatGPT / Gemini / Qwen / DeepSeek 网页里粘贴提示词，只要工具具备下面三项能力，就能跑 SDD：

| 能力 | 为什么 SDD 需要它 |
|---|---|
| **文件级 IO** | 能让 AI 直接读写仓库里的 `spec.md` / 项目级约束文件，避免"复制粘贴搬运" |
| **长上下文 / 会话** | 能同时持有 spec + 代码 + 评审讨论，跨阶段保持一致语境 |
| **可工具调用** | 能跑测试、执行 Git、读外部文档，形成"spec → 代码 → 验收"闭环 |

**本章把方法论与工具的边界画清**，并给出在不同工具里跑 SDD 的最小操作集。**所有提示词、流程、Git 模式都可跨工具复用**；只有"加载方式"（自动 vs 手动粘贴）随工具不同。深入的多角色 agent 编排见 [14 章 工具无关的 Agent 套件](./14-SDD-工具无关的Agent套件.md)。

> ℹ️ 本章核心原则与 Git 模式完全工具无关；示例配置默认按 Claude Code 形态展示（因其 `CLAUDE.md` 自动加载机制最直观），其他工具的等价做法在 §6 跨工具加载表里集中列出，需要时可对照替换。

---

## 2. spec 文件的组织与维护

### 2.1 推荐目录结构

```
<project-root>/
├── AGENTS.md                     # 项目级 AI 约束（厂商中立，所有工具都能读）
├── CLAUDE.md                     # 可选：仅一行 "See AGENTS.md"，给 Claude Code 自动加载
├── docs/
│   └── specs/
│       ├── README.md             # spec 索引
│       ├── 0001-user-login.md    # 按序号命名，便于引用
│       ├── 0002-phone-login.md
│       └── 0003-rate-limit.md
├── src/
└── tests/
```

> 工具不识别 `AGENTS.md` 时（如 ChatGPT 网页），改为会话开头手动粘贴一次即可，详见 §6。

### 2.2 命名与版本

- **编号命名**：`NNNN-kebab-case-title.md`，跨 PR 引用稳定
- **版本号**（文档头 `> 版本：vX.Y`）采用语义化：
  - `v0.x`：起草中
  - `v1.0`：首次 approved
  - `v1.x`：向后兼容修订
  - `v2.0`：破坏性重写
- **状态标签**：`[DRAFT]` / `[APPROVED]` / `[DEPRECATED]`

### 2.3 spec 与代码的双向追溯

- 代码注释引用：`// [SDD-SPEC: 0002 §5.2]`
- PR 描述引用：`Implements spec 0002 v1.0 — phone-login`
- spec 文末字段：`实现提交：<short-sha>`

---

## 3. 项目级约束文件 vs spec：分工

这是最容易混淆的一点：

| 维度 | 项目级约束文件（`AGENTS.md` / `CLAUDE.md` / `.cursorrules` 等） | `docs/specs/NNNN.md`（局部） |
|------|---------------------------------------------------------------|------------------------------|
| 作用域 | 整个项目 | 某个 feature / 变更 |
| 生命周期 | 长期 | 随 feature 开启 / 冻结 |
| 内容 | 编码规范、目录约定、SDD 红线、通用 prompt 指令 | 某功能的需求、接口、验收 |
| 变更频率 | 低 | 高 |
| AI 读取 | 工具支持时自动加载；不支持时会话开头手动粘贴 | 需要时显式指向 |

### 3.1 项目级约束文件中适合写的 SDD 条目（示例）

```markdown
# 项目级 AI 约定（节选）

## 开发流程
- 新功能必须先在 docs/specs/ 下生成 spec，再编码
- spec 未标注 [APPROVED] 前，不合入主干实现代码

## Spec 引用规范
- 代码中引用 spec 使用 `[SDD-SPEC: NNNN §X.Y]` 格式
- PR 标题格式：`[spec NNNN] <简述>`

## 提示词默认行为
- 用户描述新需求时，先反问澄清，再生成 spec draft
- 不要直接写代码，除非用户显式说"开始实现"
```

### 3.2 不该写在 spec 里的条目（归约束文件管）

- 通用编码风格（缩进、命名）
- Git 提交规范
- 目录约定

> 跨工具复用建议：**项目里只入库一份 `AGENTS.md`**，需要 Claude Code 自动加载时在 `CLAUDE.md` 里写一行 `See AGENTS.md`。详见 [14 章 §2](./14-SDD-工具无关的Agent套件.md)。

---

## 4. 对话式 SDD 的 3 阶段

把一次"从需求到实现"的会话切成 3 段，每段开始前**显式切换角色**：

```
[Phase A · 需求澄清官]
   "你是一位需求分析师。我说完需求后，你只提问、不给方案，直到我说'停'。"
     ↓
[Phase B · 规范作者]
   "切换：你现在是 spec 撰写人。基于以上对话，按 9 模板生成 spec draft。
    写完停下，不要写代码。"
     ↓
[Phase C · 实现工程师]
   "切换：你现在是实现者。请严格遵循 docs/specs/0002.md v1.0 生成代码和测试。
    任何 spec 未覆盖的决策，必须先反问我，不要自作主张。"
```

这 3 段之间**最好分 3 个 PR / 3 条 commit**，而不是一气呵成。

> 三段都是纯文本提示词，**任何模型都能跑**。Claude Code / Cursor 用户可以把它们做成自定义 slash command 或 sub-agent；ChatGPT 用户可以做成"自定义 GPT"；Gemini 用户可以做成 Gem。

---

## 5. 常用提示词模板（工具无关）

下面 4 段提示词在任何 LLM 上都能跑。Claude Code / Cursor 等支持自动加载约束文件的工具，开场可以省掉"先粘贴 AGENTS.md"那一步；其余工具会话开头手动粘贴一次。

### 5.1 规范生成

```
【任务】
基于我们上面的讨论，生成一份完整 spec，保存到 docs/specs/NNNN-<slug>.md。

【要求】
1. 严格按 9 大模块：需求背景 / 业务目标 / 功能范围 / 非功能需求 /
   接口定义 / 数据模型 / 技术方案 / 风险评估 / 验收标准
2. 缺失信息不要杜撰，用 `[TBD: <问题>]` 占位
3. 写完在末尾列出所有 TBD 清单给我确认
4. 不要开始写代码
```

### 5.2 规范评审

```
【任务】
你是一位刻薄的评审员，对 docs/specs/NNNN.md 逐节审查。

【输出格式】
| 节 | 问题 | 严重度(H/M/L) | 改进建议 |

【重点关注】
- 模糊词（"尽快" / "多种" / "一般"）
- 验收标准能否机械式验证
- 每条风险是否都有缓解
- 非目标是否显式列出
- 接口是否覆盖异常路径
```

### 5.3 按规范编码

```
【任务】
实现 docs/specs/NNNN.md v<X.Y> 描述的功能。

【硬性要求】
1. 实现前先列出"我准备改动的文件清单 + 对应 spec 章节"，等我确认
2. 不要新增 spec 未描述的能力；若必要，停下向我提议修 spec
3. 每个新函数 / 类的 doc-comment 引用 spec 章节号（[SDD-SPEC: NNNN §X.Y]）
4. 验收标准每条必须对应一个测试用例，用例名含 AC 编号
```

### 5.4 Spec 反向同步

```
【场景】
我直接改了代码（未更新 spec）。文件：<list>。

请对比代码与 docs/specs/NNNN.md，列出：
1. spec 哪些节已与现实不符
2. 建议的 spec 修订文案
3. 是否需要升主版本号（是否为破坏性变更）
```

---

## 6. 在不同工具里加载 SDD 上下文

> 推荐以 **`AGENTS.md`** 为厂商中立的项目级约束统一入口；只有 Claude Code 的自动加载入口名是 `CLAUDE.md`，里面写一行 `See AGENTS.md` 级联即可，避免双份维护。

| 工具 / 场景 | 项目级约束加载 | 角色提示词加载 |
|---|---|---|
| Cursor | 自动读 `AGENTS.md` 或 `.cursor/rules/` | `.cursor/rules/` 或对话粘贴 |
| Windsurf | 自动读 `AGENTS.md` 或 `.windsurfrules` / Cascade Rules | Cascade Rules 或对话粘贴 |
| Cline / Roo Code（VS Code） | 自动读 `AGENTS.md` 或 `.clinerules` | 自定义 instructions 或对话粘贴 |
| Aider | 自动读 `AGENTS.md` 或通过 `--read` / `/read` 加载 | 启动参数或对话粘贴 |
| Continue | 自动读 `AGENTS.md` 或 `~/.continue/config.yaml` 中的 systemMessage | 配置文件中预置 |
| Claude Code | `CLAUDE.md` 自动加载（推荐 `See AGENTS.md` 级联） | 子 agent / Skill / 会话切换 |
| ChatGPT 网页 / Custom GPT | 会话开头粘贴或上传文件 / 写入 Custom GPT 的 instructions | 自定义 GPT 或会话粘贴 |
| Gemini 网页 / Gem | 会话开头粘贴或上传文件 / Gem 内永久生效 | Gem 或会话粘贴 |
| Qwen / DeepSeek / Kimi 等 | 会话开头粘贴 | system prompt 拼接 |
| 直连 API（任意厂商） | 拼到 system message | 拼到 system message |
| 自研 Agent 框架（LangGraph 等） | 加载到 system prompt 或 ContextProvider | Agent 节点参数 |

**通用三件套**（任何工具每次开新会话都做）：

1. 加载 / 粘贴 **项目级约束**（AGENTS.md 或等价物）
2. 给出 / 粘贴 **当前角色提示词**（Phase A/B/C 之一）
3. 指向 / 粘贴 **当前任务上下文**（spec 路径或正文）

---

## 7. 避免 AI 跳过 spec 的 5 个机制

这是 SDD 落地最大的实战挑战。推荐以下"安全带"：

| 机制 | 做法 |
|------|------|
| **约束文件硬约束** | 在 `AGENTS.md` 写明"未引用 spec 编号的 PR 一律重来" |
| **两阶段指令** | 第一条消息只让 AI 出 spec，手动确认后才给第二条消息写代码 |
| **spec 引用门槛** | 代码模板强制要求 `[SDD-SPEC: NNNN §X.Y]` 注释；CI 校验缺失即报错 |
| **PR 模板** | PR 描述字段 `Implements: spec NNNN vX.Y` + `Covers: §x.y, AC-z`，不填不予合入 |
| **双会话交叉审查** | 一个会话写代码，另一个独立会话（甚至换模型）按 spec 审查 |

---

## 8. 与 Git 工作流的整合

### 8.1 commit / branch 模式

推荐的 commit / branch 模式（**与具体 AI 工具无关**，是纯 Git 纪律）：

```
main
  │
  ├── spec/0002-phone-login         ← 只改 spec.md，PR 小而快
  │     commit: "spec(0002): draft"
  │     commit: "spec(0002): addressed review"
  │     → approved & merged
  │
  └── feat/0002-phone-login          ← 实现 PR，引用 spec 编号
        commit: "feat(auth): send-code endpoint (spec 0002 §5.1)"
        commit: "feat(auth): verify endpoint (spec 0002 §5.2)"
        commit: "test(auth): AC-1~4 (spec 0002 §9)"
        → merged with changelog 引用 spec 0002 v1.0
```

**好处**：spec 评审与实现评审解耦，审查者可分别聚焦。

### 8.2 PR 描述三段式

借鉴实战项目（见 `04-SDD 实战项目/sdd-02-task-fullstack/`）的固定模板，下游代码 PR 必含：

```markdown
## Task
- Task<NNN> (see spec issue / 跨仓 epic 链接)

## Implements
- spec version: vX.Y.Z (no bump) / vX.Y.Z → vX.Y+1.0 (bump)

## Covers
- §X.Y API-NN update
- §Z BR-NN
- TC-U-NN (new), TC-IT-NN (updated)
```

`Covers` 三连让 review 者按图索骥反查 spec 原文，判断实现是否忠于契约。Task 编号规则与 feature flag 纪律详见 [15 章 Task 管理与环境晋级](./15-SDD-Task管理与环境晋级.md)。

### 8.3 `.gitignore` 关键约定

SDD 项目和传统项目的差别：**`AGENTS.md` / `CLAUDE.md` / `docs/specs/` / ADR 必须入库且谁都能读到同一版本**；但各 AI 工具会话产生的本地状态、个人偏好、缓存千万别跟着 commit。一份最小推荐：

```gitignore
# === AI 工具本地状态：忽略 ===
.claude/settings.local.json     # Claude Code 个人本地权限 / 模型偏好
.claude/projects/               # Claude Code 会话缓存 / transcripts
.claude/todos/
.claude/.credentials.json
.cursor/state/                  # Cursor 本地状态（如有）
.aider*                         # Aider 历史 / 缓存
.continue/                      # Continue 本地缓存

# === AI 协作临时产出：忽略 ===
.scratch/                        # 个人草稿目录
*.transcript.md                  # 若你保存对话回放

# === 常规（按语言栈选用）===
target/    # Java
node_modules/
.idea/
.vscode/
*.log
```

要点说明：

| 该入库 | 该忽略 |
|---|---|
| `AGENTS.md` / `CLAUDE.md`（项目级 SDD 契约） | `.claude/settings.local.json` 等个人覆盖 |
| `docs/specs/*.md`（含 `[DRAFT]` 也要入） | 工具会话缓存 / transcripts（常含敏感对话） |
| `docs/adr/`（架构决策记录） | 个人 `.scratch/` 草稿 |
| 团队共享的工具配置（如 `.claude/settings.json`） | 任何带个人凭证 / token 的本地文件 |

两个反直觉点：

- **不要用 `.gitignore` 把 draft spec 藏起来**：哪怕没 approved，也建议入库带 `[DRAFT]` 状态标签——丢了就再也追不回澄清过程
- **共享配置入库、个人覆盖忽略**：例如 `.claude/settings.json` 入库（团队约定的 hook / 权限策略），`.claude/settings.local.json` 忽略（个人本地偏好）

> 校验技巧：执行 `git check-ignore -v <path>` 可查某个文件被哪条规则匹配，避免误伤。

---

## 9. 常见问题

### Q1：AI 还是经常忽略 spec，怎么办？
- 检查项目级约束文件是否足够显眼、是否写在靠前位置
- 不支持自动加载的工具（ChatGPT 网页等），每次会话开头**主动粘贴 AGENTS.md + spec 路径/正文**
- 启用"交叉审查"：A 会话写代码，B 会话独立比对 spec（可换模型）

### Q2：spec 写起来还是太慢？
- 用模板 / snippet 减少样板
- 先写 mini-spec（3 节），随着评审发现信息缺失再补
- 允许 spec 有 `[TBD]` 占位，但必须在 approved 前清零

### Q3：spec 和代码冲突了以谁为准？
- **一般以 spec 为准，代码回退**
- 若代码实现更合理：**先改 spec、再改代码**，顺序不可颠倒
- 严禁"代码已上线、spec 以后再补" —— 这等于废掉 SDD

### Q4：团队里有人不愿写 spec？
- 只在 **MUST 场景**（对外 API / 核心业务）强制写，其他留白
- 用 AI 降低门槛（90% 由 AI 草拟、人校核）
- 让 spec 作为 PR 合入的硬门槛，由工具而非人去催

### Q5：spec 会和 PRD 重复吗？
- **不重复**：PRD 面向业务方，通常缺接口 / 数据 / 验收
- 推荐：PRD 链接进 spec §1 需求背景即可，不复制粘贴

### Q6：SDD 会不会特别费 token？
- 单次会话 yes —— 约束文件 + spec 确实会增加上下文；但**全生命周期未必更贵**，因为 spec 是对话历史的无损压缩，省下了返工、重澄清、重新"教" AI 的隐性成本
- 关键优化：启用 Prompt Caching（Claude API / GPT-4o 等都支持）、分阶段开会话、引用文件而非粘贴内容、约束文件只留硬约束
- 详细成本结构与优化清单见 [06 章 Token 成本与项目规模档位](./06-SDD-Token成本与项目规模档位.md)

### Q7：换工具会不会要重写一遍提示词？
- **不会**。本章 §4、§5 的 3 阶段角色 + 4 段提示词都是纯文本，跨工具直接复用；只有"加载方式"不同。详见 [14 章 §6](./14-SDD-工具无关的Agent套件.md)

---

## 10. 示例串讲：一次完整的对话式 SDD 会话

下面演示风格中立——同一段对话脚本无论在 Claude Code / Cursor / Windsurf / Cline / Aider / ChatGPT / Gemini 哪个工具里都能跑。差别仅在"开场是否要手动粘贴 `AGENTS.md`"：能自动加载的工具省一步，不能的工具会话开头粘一次。

```
[User]   我要做一个 Git 提交统计 CLI，团队用来看每周活跃度。
[AI · 需求澄清官]
         3 个问题：
         (1) 统计周期按自然周还是滚动 7 天？
         (2) 要不要含 merge commit？
         (3) 输出给人看还是机器读？
[User]   默认近 7 天、不含 merge、人机都要支持。
[AI]     还有 (4)：要跨多个仓库聚合吗？
[User]   本期不做。
[AI · 切换为 spec 作者]
         生成 draft 到 docs/specs/0001-git-stats.md
         （9 模块完整，含 2 个 [TBD: 输出精确列名]）
[User]   列名用 作者 / 提交数 / 变更行数
[AI]     spec v0.2 更新完毕，请 review
[User]   approved
[AI · 切换为实现者]
         改动清单：
           - src/gitstats/cli.java      (spec §5)
           - src/gitstats/stats.java    (spec §7)
           - src/test/.../StatsTest.java (对应 AC-1~3)
         确认后开工？
[User]   开工
[AI]     代码已写完，测试 3/3 通过，验收对照：
         AC-1 → test_default_output ✓
         AC-2 → test_days_filter ✓
         AC-3 → test_exclude_merge ✓
[User]   合入
```

全流程**所有产出可回放**：spec 在 Git、代码引用 spec、测试对应 AC，完整闭环。

---

## 11. 思考与练习

1. **改造题**：打开你目前手边任意一个项目，尝试写一份 `AGENTS.md`（只关注 SDD 部分，约 20 行）。然后让 AI 以该文件为约束生成下一个功能的 spec，观察输出质量变化。
2. **跨工具题**：在 Claude Code 与 ChatGPT（或你常用的另一个工具）里跑同一段澄清提示词，对比：哪个更主动反问？哪个更容易直接给方案？
3. **对照题**：在同一需求下做两次实验——
   - 实验 A：直接对 AI 说 "帮我实现 X"
   - 实验 B：按本章第 4 节三阶段走

   对比两次的：代码行数、测试覆盖、你追加口头说明的次数。哪个更省？
4. **故障演练**：故意写一份有 3 个歧义点的 spec，让 AI 实现。它会主动问你吗？哪些歧义被忽略了？这对提示词设计有什么启发？
5. **开放题**：如果团队有 5 人同时用不同工具开发（A 用 Claude Code、B 用 Cursor、C 直连 API），spec 被 AI "幻觉" 改写的风险会上升。设计一套"只读 spec"机制（例如让写 spec 走独立 PR、或用 Git hook 禁止编辑）。

---

## 12. 学习路线建议

```
本章：把 SDD 跑起来（任意工具）
    ↓
端到端案例走一遍（05 章）
    ↓
按问题挑读专题（06–11 章）
    ↓
（进阶）把 SDD 写入团队 AGENTS.md 并推广
    ↓
（进阶）跨模型用 AGENTS.md + 5 角色 agent（14 章）
    ↓
（进阶）多仓 / 多任务 / 多环境（07 章 + 15 章）
    ↓
（进阶）结合 Skill / MCP 扩展 SDD 能力
```

> 想要多角色 agent 避免漏写提示词？想跨模型协作？
> 见 [14 章 工具无关的 Agent 套件](./14-SDD-工具无关的Agent套件.md)。

---

## 13. 参考来源

- AGENTS.md 厂商中立约定：在 Cursor / Aider / Continue 各自文档内搜索 "AGENTS.md"
- Claude Code 文档：https://docs.claude.com/claude-code
- Anthropic Claude Code Best Practices：https://www.anthropic.com/engineering/claude-code-best-practices
- GitHub Spec Kit：https://github.com/github/spec-kit
- 搭配阅读：本仓库 `00-提示词工程/`、`14 章 工具无关的 Agent 套件`
