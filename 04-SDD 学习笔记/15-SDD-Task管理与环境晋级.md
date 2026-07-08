# Task 管理与环境晋级

> 版本：1.0 | 定位：高频问题专题 — "spec 发版了，但 N 个 feature 不能一起上 PROD，怎么办？"

---

## 1. 为什么需要这一章

到这里 SDD 的"日常闭环"已经完整：从 spec 起草、跨栈拆分、测试 AC、跨仓发版（07 章 §5），都在 spec 仓 + 代码仓的 PR 模型里跑得动。

但**一旦项目跑过几个迭代**，就会撞到 PR 流程不解决的两个问题：

1. **同一份 spec 发版里包含 5 个 Task，业务方只想让其中 2 个对终端用户可见**（其余先跑 SIT/UAT 验证）
2. **DDL 一旦合入主干，所有环境都要跑**——这和"PROD 暂不开放此功能"如何并存？

如果没有专门纪律，团队会**退化**到下面 3 种坏模式：

| 退化路径 | 症状 | 为什么坏 |
|---|---|---|
| 长期 holdback 分支 | 某 feature 的代码不合 main，挂在分支上 rebase | 分支腐烂、V 号冲突、与 main 不可合 |
| 环境分叉迁移目录 | `migration/sit/`、`migration/prod/` 各自一份 | 真源碎裂，再也分不清"哪份是规范" |
| 关 PR 退回 spec 重写 | 把上线节奏倒灌回 spec，spec 反复改 | 破坏 spec 的可追溯性 |

行业主流的解法很简单：**DDL 始终向前兼容（additive-only）+ 功能可见性由 feature flag 控制**。本章把这套纪律拆出每一条规则。

> 实战参考：`04-SDD 实战项目/sdd-02-task-fullstack/` 的 `task-spec/06-任务与发布管理.md` + `07-多任务并行开发.md` + `08-环境晋级与选择性发布.md` 是这套纪律的完整落地。

---

## 2. Task 编号体系

### 2.1 什么是 Task

**Task 是最小可部署单元**。一个 Task 的所有 PR 合入后，对应的 DDL / 代码 / flag 可一起上 PROD（功能是否可见由 flag 决定）。

Task / Epic / spec tag 三个概念别混：

| 概念 | 定义 | 关系 |
|---|---|---|
| **spec tag** | spec 仓对外发版的版本号（vMAJOR.MINOR.PATCH） | 一个 tag 可包含多个 Task 的 spec 变更 |
| **Epic** | 跨仓协调载体；多仓 PR 的父问题 | 一个 Epic 可含 1 或多个 Task |
| **Task** | 最小可部署单元 | 简单功能：1 Epic = 1 Task；破坏性变更：1 Epic = 2+ Task（Expand + Contract） |

### 2.2 编号格式与登记

```
Task<NNN>
```

- 三位数字，从 `001` 开始连续递增
- 不复用已废弃的号码
- 编号由 spec 仓的 issue tracker 分配，时机：**有人开始写 spec 草稿之前**先开 issue 锁号

### 2.3 Task-ID 的"全位置一致"规则

同一个 Task-ID **必须在所有相关位置出现**，一致到字节：

| 位置 | 格式示例 |
|---|---|
| spec 仓 issue 标题 | `Task001: <短描述>` |
| 任意仓 PR 标题 | `Task001: <动作摘要>` |
| 迁移脚本文件名 | `V<N>__task001_<snake_case>.sql` |
| 迁移脚本头注释 | `-- [SDD-TASK: Task001]` |
| 代码追溯注释 | `// [SDD-TASK: Task001][SDD-SPEC: 02 §4 BR-06]` |
| Feature flag 名 | `feat_task001_<短语>` |
| commit 消息行首 | `Task001: <动作>` |

**好处**：从任何工件（脚本、代码、flag、commit）都能反查到 Task-ID，再反查到 spec 章节和讨论。

### 2.4 Task 轨迹文档

满足下列任一阈值的 Task **必须**在 spec 仓 `tasks/Task<NNN>.md` 留一份持久化轨迹文档：

- 涉及 DDL
- 跨 ≥2 个仓
- 引入或移除 feature flag
- 走 Expand-Contract
- 改 BR 或接口契约

模板要点：

```markdown
# Task001 — <一句话目标>

## 状态
- 创建：2026-04-25
- 状态：[草稿 / 实施中 / Expand 完成 / Contract 完成 / 关闭]

## 锚定 spec
- spec@v1.2.0
- 涉及条款：BR-01、BR-06、FE-02、BE-04、DB-03

## 影响仓
- task-spec / task-backend / task-db

## Feature flag
- feat_task001_user_avatar（默认关）

## 三维矩阵单元
- BR-01 × FE-02 → TC-FE-3、TC-E2E-1
- BR-06 × BE-04 → TC-IT-7

## 偏差与决策
- 2026-04-26 决定改用 X 而非 Y，原因：...

## 完成态（合 PR 前回填）
- 实际产出：...
- 偏差：...
- 后续待办：...
```

issue tracker 承载日常讨论，Task md 承载**最终决策与轨迹**，两者不重复。

---

## 3. 核心发布原则（4 条）

下列 4 条是本章其余规则的根基，**违反任一即视为 SDD 纪律缺陷**：

### 3.1 Additive-only 默认

任何 DDL 改动**默认**必须向前兼容：

- ✅ 新建表 / 加可空列 / 加索引 / 加视图 / 扩列范围
- ❌ 删表 / 删列 / 改列名 / 改类型缩范围 / 改主键

落入 ❌ 的变更**禁止单步完成**，必须走 §4 Expand-Contract。

### 3.2 破坏性变更必须 Expand-Contract

见 §4。

### 3.3 功能可见性在代码侧 feature flag，不在 DDL

- DDL 只负责"能力存在"
- 代码用 feature flag 决定是否走新路径、是否暴露新 UI
- flag **默认关闭**，按 env 配置开启

### 3.4 脚本与代码强制带 Task 编号

见 §2.3。

---

## 4. Expand-Contract 模式

### 4.1 什么场景必须走 Expand-Contract

任一**破坏性 DDL**必须拆成至少 2 个 Task：

- 加 NOT NULL 列（无可接受默认值）
- 改列名 / 改列类型（缩范围）
- 删列 / 删表
- 改主键 / 改唯一键
- 加唯一约束（已有数据可能违反）

### 4.2 三阶段流程

```
[Expand 阶段]              [过渡期]                [Contract 阶段]
┌─────────────────┐       ┌─────────────────┐    ┌─────────────────┐
│ TaskNNNa:       │       │ 代码双写新旧    │    │ TaskNNNb:       │
│ 加新结构(可空) │  ───> │ 数据回填        │ ─> │ 清理旧结构      │
│ 代码开始兼容写  │       │ 所有实例部署    │    │ DDL 转为严格    │
└─────────────────┘       └─────────────────┘    └─────────────────┘
   部署到全环境              部署到全环境            部署到全环境
   flag 关                   逐步开 flag              flag 保留或移除
```

**关键纪律**：

- Expand 与 Contract **必须是两个独立 Task**（不同 Task-ID、不同 PR、不同脚本版本号）
- 过渡期**至少跨越一次部署**——让所有 replica / 实例都跑过兼容代码，避免"半态"
- Contract 阶段开始前，必须**人工确认**无客户端在访问旧结构（日志 / 监控）

### 4.3 为什么不走"单步 RENAME"

主流 RDBMS 的 `ALTER TABLE ... CHANGE` / `RENAME COLUMN` 都是原子 DDL，但：

- 部署窗口内必然存在"旧代码 + 新 DDL"或反向半态，旧代码找不到旧列即崩
- 主从复制期间可能短时不一致
- 回滚成本极高

**Expand-Contract 的本质是用时间换安全**：新旧结构共存一段时间，让所有消费者平滑迁移。

---

## 5. Feature Flag 纪律

### 5.1 命名

```
feat_task<NNN>_<snake_case_短语>
```

示例：`feat_task001_user_avatar` / `feat_task005_payment_v2`

规则：

- 前缀 `feat_` 表示 feature flag（其他类型不在本章范围）
- 必须含 `task<NNN>`——让 flag 可反查 Task-ID
- 全小写 + 下划线，便于代码 / 配置 / DB 存储一致
- 后缀业务短语 ≤ 30 字符

### 5.2 生命周期

flag 跨越环境晋级阶梯（详见 §7）：

```
引入(默认关) → SIT 开 → UAT 按需 → PROD-mirror 验证 → PROD 灰度/全开 → 移除
   TaskNNNa     (随合入)  (晋级决定)   (晋级决定)         (晋级决定)        TaskXYZ
```

规则：

- 引入时**默认关闭**（代码里默认 `false`），仅 dev 环境显式打开
- 每次环境晋级开关 flag 必须走 release plan（§9）
- flag 移除**必须是独立 Task**（不能与业务改动混在一起）
- **最晚 2 个 spec 版本内移除已全开的 flag**，防止僵尸 flag 腐蚀代码

### 5.3 禁用清单

- ❌ flag 值依赖数据库表（性能 + 启动期循环依赖）
- ❌ 在 DDL 里判断 flag（DDL 不应判断）
- ❌ flag 当作权限 / 鉴权手段（feature flag ≠ ACL）
- ❌ flag 命名不带 Task-ID
- ❌ 删除代码但保留 flag（必须一起删）
- ❌ flag 默认值是 `true`（破坏"安全默认"原则）

---

## 6. DDL 场景处理矩阵

任何 DDL 改动**必须**对应到下表中的一行：

| 场景 | Additive? | 拆 Task 数 | 备注 |
|---|---|---|---|
| 新建表 | ✅ | 1 | 一步 |
| 加可空列 | ✅ | 1 | 一步 |
| 加带默认值的 NOT NULL 列（默认值业务可接受） | ✅ | 1 | 大表谨慎，考虑 online DDL |
| 加索引 | ✅ | 1 | 大表用 `ALGORITHM=INPLACE` 或 pt-online-schema-change |
| 扩列范围（如 VARCHAR(100)→200） | ✅ | 1 | 扩范围本身向前兼容 |
| **加 NOT NULL 列（无可接受默认值）** | ❌ | **≥2** | Expand 可空+回填 → Contract 改 NOT NULL |
| **改列名** | ❌ | **≥2** | 加新列+双写+切读 → 删旧列 |
| **缩列范围** | ❌ | **≥2** | 加新列+数据验证+切读 → 删旧列 |
| **删列 / 删表** | ❌ | **≥2** | 先代码停用 → 再 DROP |
| **改主键** | ❌ | **≥3** | 加新主键 / 双写 / 切读 / 删旧 |
| **加唯一约束（已有数据可能违反）** | ❌ | **≥2** | 先代码保证唯一 + 清理历史 → 加约束 |

---

## 7. 环境分层与晋级

### 7.1 标准阶梯

| 层 | 简称 | 职责 | 数据 | flag 默认状态 |
|---|---|---|---|---|
| 开发 | dev | 个人开发 / 本地联调 | 各自容器，可重置 | 全开（本地实验） |
| 系统集成测试 | **SIT** | 跨仓集成、QA 全量验证 | 共享，持续刷新 | **当前 main 全部 Task 的 flag 全开** |
| 用户验收测试 | **UAT** | 业务方 / 用户用真实数据验收 | 类生产数据，受控 | **本轮晋级被批准的 Task flag 开** |
| 准生产（可选） | **PROD-mirror** | 灰度前最后一道闸门 | PROD 数据子集 / 脱敏 | **与 PROD 计划一致** |
| 生产 | **PROD** | 终态 | 真实业务数据 | **本次发布批准的最小子集 flag 开** |

### 7.2 关键不变量

不论环境怎么变，下列**必须**保持：

- 所有环境跑**同一个 build artifact**（同一 git SHA / 同一镜像）
- 所有环境跑**同一组迁移脚本**（因 DDL additive，无副作用）
- 所有环境的 spec 真源**指向同一个 tag**
- **唯一**变化的是：环境配置（feature flag 为主）

任何破坏以上不变量的做法 → 禁用。

### 7.3 五条晋级原则

| 原则 | 含义 | 违反后果 |
|---|---|---|
| **同 build 跨环境** | SIT/UAT/PROD 跑同一镜像，**不允许**为某环境单独打包 | build 不可变性破裂，事故定位 / 回滚链路断裂 |
| **DDL 全环境执行** | 迁移脚本在所有环境都跑全部脚本 | 各环境 schema 漂移 / checksum 失配 |
| **功能可见性 = flag 配置** | 同一 Task 在不同环境的开关由各自配置决定，**与代码无关** | 代码分支化 / 环境分支化 / spec 分裂 |
| **每次晋级是显式决策** | SIT→UAT、UAT→PROD 都需 release plan | 晋级无审计 |
| **回滚靠关 flag，不回滚 DDL/build** | 出问题 → 关该 Task 的 flag（秒级）；**禁止**降级 build / 回滚已合入 DDL | DDL 回滚是雪崩起点 |

---

## 8. 选择性发布的三种打法

| 打法 | 做法 | 评价 |
|---|---|---|
| **α. feature flag + 共同上 PROD** | A/B 的 DDL 都是 additive，都合 main 都上 PROD；flag 只开 A 的功能 | ⭐⭐⭐ **主推** |
| β. B 的 DDL 留分支 | B 的迁移脚本不合 main，长期 rebase | ⚠️ 分支腐、版本号冲突；**仅在 DDL 无法 additive 化且 flag 无法控制时勉强可用** |
| γ. 环境分叉迁移目录 | `migration/prod/` 与 `migration/dev/` 分目录 | ❌ **禁用**——破坏真源唯一性 |

### 8.1 选择 α 的前置条件

- B 的所有 DDL 都是 additive（对照 §6 矩阵）
- B 的代码路径能被 feature flag 完全屏蔽
- flag 已在所有会走 B 路径的服务里初始化为 **关**

满足以上 3 条，**α 总是优于 β/γ**。

### 8.2 什么情况必须走 β

- B 必须走破坏性 DDL 且无法 Expand-Contract（极罕见）
- 业务方要求 B **完全不进入**代码库（如竞品保密）

即便如此，也应先尝试把 B 拆成更小的 Task，让其中可部分的部分走 α。

---

## 9. Release Plan 模板

每次环境晋级（SIT→UAT、UAT→PROD）必须留下一份 release plan，归档到 spec 仓 `releases/<env>-<date>-<seq>.md`：

```markdown
# Release: PROD 2026-04-25 #1

## 晋级目标
- 从 UAT 晋级到 PROD

## 批准
- 业务方：@xxx 于 2026-04-24
- 技术 owner：@yyy 于 2026-04-25

## 本次发布的 Task 清单
- Task001 user-avatar（开 flag）
- Task003 phone-login（开 flag，10% 灰度）
- Task005 payment-v2（**不开** flag，仅同步代码）

## Flag 状态变更
| flag | UAT 当前 | PROD 目标 |
|---|---|---|
| feat_task001_user_avatar | true | true |
| feat_task003_phone_login | true | 10% |
| feat_task005_payment_v2 | true | false |

## 部署版本
- spec@v1.3.0
- backend image: registry.../backend:abc1234
- frontend image: registry.../frontend:def5678

## 回滚预案
- 任一 Task 异常 → 关对应 flag（不回滚镜像）
- 监控告警阈值 / 触发 / 通知人

## 验证清单（发布后 24h）
- [ ] AC-1 ... 通过
- [ ] AC-NFR-1 P95 < 300ms
- [ ] 错误率 < 0.1%
```

release plan **入 spec 仓**，与 spec 同 PR 模型管理。

---

## 10. 自检清单

执行 SDD 改造时，对照以下清单：

- [ ] 所有迁移脚本文件名都带 `task<NNN>_` 段
- [ ] 所有破坏性 DDL 都能在 §6 矩阵中找到对应的"拆 ≥2 Task"条目，并真的拆了
- [ ] 所有 feature flag 名都带 `task<NNN>` 段
- [ ] 所有 Task-ID 在 issue / PR / 脚本 / 代码 / flag 中一致到字节
- [ ] 没有 `migration/prod/` 与 `migration/dev/` 这类环境分叉目录
- [ ] 没有 holdback 时间 > 2 周的长期分支
- [ ] 每次 PROD 发布前已对照 §8 选定 α / β / γ 之一并记录在 release plan
- [ ] 所有"环境差异"（不只 flag——还包括 DB URL / CORS / 日志级别 / 第三方 endpoint）都外部化到配置，未硬编码

---

## 11. 与其他章节的分工

| 章 | 主题 | 关系 |
|---|---|---|
| 02 | 标准流程 | 本章是 02 流程之外的"附加阶段" |
| 03 §13 | conventions 9 支柱 | 本章规则与 db / dependency conventions 联动 |
| 04 §8.2 | PR 三段式描述 | 本章 Task-ID 落点对接到 PR 描述 |
| 07 §5 | 多仓 SDD | 本章是多仓 SDD 的"发布纪律层" |
| 09 §1.1 / §1.2 | L1~L4 + 三维矩阵 | TC-* 编号在本章 Task md 中被引用 |
| 11 | 常见误区 | 本章 §1 三种退化模式同样适用此处 |

---

## 12. 思考与练习

1. **盘点题**：你当前项目有几条"破坏性 DDL 但单步搞定"的历史 PR？回头复盘——如果当时走 Expand-Contract，会多花多少时间？避免了什么风险？
2. **改造题**：选你团队最近一个 feature，按本章规则给它分配 Task-ID、设计 feature flag 名、起草一份 release plan 模板。看与现在做法的差距在哪。
3. **对照题**：α / β / γ 三种打法，你团队默认走的是哪一种？退化到 β / γ 的根因是 DDL 无法 additive，还是流程上没人推 α？
4. **诚实题**：你项目里有没有"僵尸 feature flag"？计算一下：项目运行 N 个月后会积累多少？维护成本 vs 当初引入它的收益谁大？
5. **开放题**：本章默认 4 层环境（dev / SIT / UAT / PROD）。如果你团队只有 dev + PROD 两层（典型创业团队），本章哪些规则可以省、哪些必须保留？

---

## 13. 学习路线建议

```
本章：Task / flag / 晋级纪律
    ↓
07 章 §5：多仓 SDD（spec 仓独立 + tag）
    ↓
09 章 §1.1 / §1.2：L1~L4 + 三维矩阵
    ↓
（实战）打开 04-SDD 实战项目/sdd-02-task-fullstack/，对照本章每一节读对应 task-spec/06-08 / conventions/db-conventions
    ↓
（进阶）把本章规则写入团队 AGENTS.md 与 PR 模板
```

---

## 14. 参考来源

- 实战参考：`04-SDD 实战项目/sdd-02-task-fullstack/task-spec/06-任务与发布管理.md`、`07-多任务并行开发.md`、`08-环境晋级与选择性发布.md`
- Expand-Contract 模式：Martin Fowler, "ParallelChange" 模式（refactoring.com）
- Feature flag 反模式：Pete Hodgson, "Feature Toggles (aka Feature Flags)"（martinfowler.com）
- Flyway / Liquibase 文档（迁移脚本不可变原则）
