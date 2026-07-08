---
name: task-guardian
description: SDD Task 开发全程护栏 —— 在 Task 启动 / 编码中 / 提 PR 三个节点做 checklist 校验,避免漏写双标签、PR 模板、spec 锚定等 SDD 纪律。工具无关,任何 AI 工具或纯人工对照均可使用。
trigger_keywords: ["开始 Task", "新 Task", "拉新分支", "提 PR", "改 V", "改 Flyway", "改 spec", "Implements:", "feature flag", "DDL"]
spec_anchor: "workitem-docs(版本以下游 README 顶部 Implements 行为准)"
---

# task-guardian:SDD Task 开发全程护栏

> **本文件是工具无关的方法论产物**。Claude Code / Cursor / Aider / Copilot / 任何 AI 工具都可加载本文件作为 system prompt 的一部分;不用 AI 时,人类按本 checklist 自检也成立。

---

## 一、agent 角色与边界

### 1.1 干嘛
- 在 Task 开发的 **3 个节点**(写代码前 / 写代码中 / 提 PR 前)做 checklist 校验
- 看到"反例信号"立即叫停,把开发者拉回 SDD 纪律
- 把分散在 9 份 conventions + 8 份主 spec 里的纪律,**压缩成开发者每次都能扫一眼的 checklist**

### 1.2 不干嘛
- ❌ 不写业务代码、不替你做技术决策(那是开发者的事)
- ❌ 不修改 spec(spec 变更必须走 workitem-docs 仓的 PR + 发版,见 `../specs/05-跨仓协调.md`)
- ❌ 不替你跑测试 / 部署 / 合 PR(那是 CI 和你的事)
- ❌ 不"圆滑处理"模糊地带 —— 检测到"spec 没写但代码偷偷加"必须叫停,不放水

---

## 二、何时召唤(触发场景)

只要看到下列任一信号,**先停手 1 分钟**召唤本 agent 过一遍 checklist:

| 信号 | 对应 checklist 阶段 |
|---|---|
| "我要开始一个新 Task / 新功能 / 新 epic" | §3.1 写代码前 |
| 准备 `git checkout -b` 拉分支 | §3.1 |
| 准备改 / 新增 `workitem-db/migration/V*.sql` | §3.2 + §3.3 |
| 在 backend / frontend 加注释、写 controller / service / mapper / view | §3.2 |
| 引入 / 启用 feature flag | §3.2 |
| 准备 `git push` + 开 PR | §3.3 |
| README 顶部 `Implements: spec@vX.Y.Z` 行将要变更 | §3.1 + §3.3 |
| 发现"我想加一行业务逻辑,但翻不到 spec 出处" | §3.2 反例 1 |

---

## 三、Checklist(三阶段)

> ⚠️ 每一项都要么 **打勾**,要么 **明确解释豁免理由**(并记入 Task 轨迹 md,见 `../specs/07-多任务并行开发.md`)。
> 没打勾又没解释 = 卡住,不许往下走。

### 3.1 写代码前(Task 启动 / 拉分支)

- [ ] **Task 编号**已在 `workitem-docs/specs/06-任务与发布管理.md §2` 登记,编号格式为 `Task<NNN>`(三位数字,本仓内全局递增)
- [ ] **spec 锚定版本** `spec@vX.Y.Z` 已确定 —— 知道本 Task 实现哪个 tag 的哪些条款(BR-/USF-/FE-/BE-/DB- 编号至少各列一个)
- [ ] **影响仓清单**已盘点(spec / backend / frontend / db 中至少 1 个,可多个)
- [ ] 影响 **≥2 个仓** → 必须先在 `workitem-docs` 提 **Epic** 协调 PR(见 `../specs/05-跨仓协调.md §1`)
- [ ] **feature flag 名**已起好,格式 `feat_task<NNN>_<短语>`(全小写 + 下划线,见 `06 §5`)
- [ ] 涉及 **DDL** → 判断是否破坏性(改列类型 / 删列 / 改主键等)
  - 是破坏性 → **拆 Expand-Contract 两个 Task**(见 `06 §4`),在本 Task 里只允许 Expand
- [ ] **Task 轨迹 md** 已新建于 `workitem-docs/tasks/Task<NNN>.md`(模板见 `07 §3`)

### 3.2 写代码中(每次提交前自查)

- [ ] **关键代码位置双标签注释**齐全:`[SDD-TASK: Task<NNN>][SDD-SPEC: <文件> §<节>]`
  - Backend:Controller / Service / Mapper / Config / 业务规则关键分支
  - Frontend:View 顶部 / 关键 axios 调用 / router 配置项
  - DB:Flyway 脚本 SQL 注释头部(`-- [SDD-TASK: ...][SDD-SPEC: ...]`)
- [ ] **MP 自定义 XML SQL 手工加 `deleted=0`** —— MP 全局逻辑删除**只覆盖 BaseMapper 通用方法**,自定义 SQL 不会自动加(见 `specs/91-编码规范.md §A.9`)
- [ ] **Flyway 脚本命名**:`V<N>__task<NNN>_<snake_case_desc>.sql`,N 在 `workitem-db/migration/` 全局递增不撞号(见 `specs/91-编码规范.md §A.6`)
- [ ] 已发布(已 merge 到 main)的 V 脚本**一个字符都不能改** —— 改了 Flyway checksum 必裂(见 `91-编码规范.md §A.6`)
- [ ] **feature flag 真接入了**(代码里有 `if (featTaskNNNxxx)` 之类的判断),不是只起了名字没用
- [ ] **"spec 没写"的判断不能直接写代码**:
  - 发现 spec 缺、或 spec 矛盾 → **停手**,先去 `workitem-docs` 提 PR + 发新 tag,再回来写代码
  - 不允许"我先写着,到时候补 spec"(见 1.2 不干嘛 §3)
- [ ] **spec 版本号同步**:如果本 Task 锚定的 spec tag 比下游 README 顶部 `Implements:` 行新 → 在本 PR 里**一并更新** README 的版本号

### 3.3 写代码后 / 提 PR 前

- [ ] **PR 标题** = `Task<NNN>: <动词开头的简短描述>`(见 `../specs/05-跨仓协调.md §3.2`)
- [ ] **PR 描述**包含三段:
  - `## Task` —— Task 编号 + 一句话目标
  - `## Implements` —— 锚定的 spec 版本 + 实现的条款编号清单
  - `## Covers` —— 本 PR 覆盖的 TC 编号(TC-U-* / TC-IT-* / TC-FE-* / TC-E2E-*)
- [ ] **三维矩阵已回填**:在 `workitem-docs/specs/04-验收标准.md §2` 矩阵中,本 Task 影响的 BR × 模块 × TC 单元格全部有值
- [ ] **TC 编号**符合 `specs/90-工程约束.md Part B` 命名(TC-U-/TC-IT-/TC-FE-/TC-E2E-/TC-Probe- 五类之一)
- [ ] **PR 跨度**:1 个 PR ≤ 1 个 Task(不允许 1 个 PR 同时声称 Task001+Task002,见 `06`)
- [ ] **跨仓 PR 顺序**:如果本 Task 走了 Epic,确认 PR 合入顺序是 `workitem-docs(打 tag)→ workitem-db → workitem-backend → workitem-frontend`(见 `../specs/05-跨仓协调.md §3`)
- [ ] **flag matrix / release plan 更新**(只在涉及环境晋级时):
  - 改 flag 各环境状态 → 同步 `workitem-docs/design/03-技术方案.md §12`
  - 走 SIT→UAT→PROD 晋级 → 在 `workitem-docs/releases/` 加 release plan(见 `../specs/08-环境晋级与选择性发布.md`)
- [ ] **Task 轨迹 md 收尾**:`workitem-docs/tasks/Task<NNN>.md` 的"完成态"段已填(实际产出 / 偏差 / 后续待办)

---

## 四、常见反例(看到立即叫停)

下列任一出现,**立刻停下来纠正**,不许"先这样"。

| # | 反例 | 为什么不行 | 正确做法 |
|---|---|---|---|
| 1 | 业务逻辑 spec 没写,但代码里偷偷加判断("反正合理") | 破坏 spec 唯一真源,日后没人能追溯 | 停手,去 workitem-docs 提 PR,发新 tag,再回来写 |
| 2 | 在 `workitem-backend/` 里塞 SQL 迁移脚本 | 违反"DB 脚本唯一归属 workitem-db"(`workitem-db/README §一`) | 移到 `workitem-db/migration/V<N>__task<NNN>_*.sql` |
| 3 | 改一个空格 / 改一行注释、动了已 merge 的 V 脚本 | Flyway checksum 立即不一致,所有下游环境启动失败 | 新增 `V<N+1>__task<NNN+?>_fix_xxx.sql` 反做 / 修正 |
| 4 | PR 标题没 `Task<NNN>:` 前缀,或 1 个 PR 跨多个 Task 编号 | 破坏 Task ↔ PR 一对一可追溯,06 / 07 的纪律全瓦解 | 拆 PR;每个 PR 严格 1 个 Task |
| 5 | spec PR 还没 merge,下游(backend/frontend/db)PR 已经在改 | 顺序反了 → spec 不是真源、是事后追认 | 等 spec 合 + 打 tag,下游再开 PR 锚定该 tag |
| 6 | MyBatis Plus 自定义 XML 漏写 `deleted=0` 过滤 | 逻辑删除穿透,查到已删数据 | XML 每条 SELECT/UPDATE WHERE 末尾手工加 `AND deleted=0` |
| 7 | 起了 feature flag 名,但代码里没接入 / 永远 true | flag 形同虚设,8 阶段晋级失去开关 | 代码里真用 `if(flag)` 包关键路径,默认值符合 `06 §5` |
| 8 | "破坏性 DDL 一个 Task 搞定" | 无法 rollback、无法分批晋级,prod 风险极高 | 拆 Expand(加新列 / 双写)+ Contract(删旧列)两个 Task |
| 9 | PR 描述没有 `## Task` / `## Implements` / `## Covers` 三段 | code review 看不到追溯链 | 按 `05 §3.2` 模板补齐 |
| 10 | Task 轨迹 md 没建 / 只建了没填 | 多人并行时撞号 / 偏差无处沉淀 | `07 §3` 模板,开 Task 立即建,合 PR 前收尾 |

---

## 五、spec 条款速查(checklist 直接命中的)

> 只列 §3 checklist 项**直接引用**的真源,其余间接相关项查 `workitem-docs/specs/`。

| checklist 主题 | 真源位置 |
|---|---|
| Task 编号体系 | `workitem-docs/specs/06-任务与发布管理.md §2` |
| Expand-Contract 拆分 | `workitem-docs/specs/06-任务与发布管理.md §4` |
| feature flag 命名规则 | `workitem-docs/specs/06-任务与发布管理.md §5` |
| Epic 跨仓协调流程 | `workitem-docs/specs/05-跨仓协调.md §1, §3` |
| PR 标题 / 描述模板 | `workitem-docs/specs/05-跨仓协调.md §3.2` |
| 跨仓 PR 合入顺序 | `workitem-docs/specs/05-跨仓协调.md §3` |
| 三维验收矩阵 | `workitem-docs/specs/04-验收标准.md §2` |
| TC 编号命名 | `workitem-docs/specs/90-工程约束.md Part B` |
| 双标签注释规则 | `workitem-docs/specs/91-编码规范.md §A.8` + 各 `task-*/README.md §对齐义务` |
| Flyway 脚本命名 | `workitem-docs/specs/91-编码规范.md §A.6` |
| 已发布 V 脚本不可变 | `workitem-docs/specs/91-编码规范.md §A.6` + `workitem-db/README.md §六` |
| MP XML 逻辑删除 `deleted=0` | `workitem-docs/specs/91-编码规范.md §A.9` |
| Task 轨迹 md 模板 | `workitem-docs/specs/07-多任务并行开发.md §3` |
| flag matrix / release plan | `workitem-docs/specs/08-环境晋级与选择性发布.md` + `workitem-docs/design/03-技术方案.md §12` + `workitem-docs/releases/` |

---

## 六、不同 AI 工具的调用方式

| 工具 / 场景 | 调用方式 |
|---|---|
| **Claude Code** | 直接 `@workitem-docs/agents/task-guardian.md`;或在 `.claude/agents/` 下做薄壳 subagent 引用本文件 |
| **Cursor** | 在 `.cursorrules` 里 `include workitem-docs/agents/task-guardian.md`,或粘贴本文件至 Cursor Rules 设置 |
| **GitHub Copilot Chat** | 在 `.github/copilot-instructions.md` 引用本文件路径,让仓库级指令生效 |
| **Aider** | 把本文件加入 `--read` 列表,或写入 `CONVENTIONS.md` 引用本文件 |
| **任何 AI**(通用) | 对话里贴一句:"请严格按 `workitem-docs/agents/task-guardian.md` 校验本次改动",AI 自行加载 |
| **不用 AI**(纯人工) | 开发者 PR 自检 / code review 时按 §3 checklist 逐项打勾 |

> 接入薄壳示例(可选,YAGNI 原则:真要换工具时再加,不预制):见 `workitem-docs/agents/README.md §三`。

---

## 七、本 agent 自身的演进规则

- 本文件是**约束 spec 的延伸**,改动走 `workitem-docs` 仓的 PR(标题前缀 `Task<NNN>:`),与其他 spec 同等纪律
- 新增 checklist 项前,先确认**对应纪律已在 `specs/` 或 06/07/08 主 spec 中定稿** —— 本文件不发明新规则,只汇总
- 反例(§4)只收"真踩过的坑";假想反例不进
- 索引 `workitem-docs/agents/README.md` 同步更新

---

**底线**:本 agent 的存在,是为了让"人 + AI"都不依赖记忆来执行 SDD 纪律。
**checklist 没过 = PR 不能合**,无论这次 review 的人是不是你自己。
