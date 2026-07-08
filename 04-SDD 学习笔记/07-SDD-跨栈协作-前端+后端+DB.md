# 跨栈协作：SPEC 如何约束前端 + 后端 + DB

> 版本：1.0 | 定位：高频问题专题 — "一份 spec 怎么管前端 + 后端 + DB？"

现实中的一个 task 往往同时涉及三层：DB 表、后端接口、前端界面。"一份 spec 管所有"不现实，"三份 spec 各管一层"又容易断层。这一章解决：**跨栈场景下 spec 怎么组织才能不失控**。

---

## 1. 单一 spec 的失败姿势

一个 "用户手机号登录" 功能同时涉及：

- **DB**：`user` 表加 `phone` + 唯一索引、`verification_code` 表
- **后端**：`/auth/send-code`、`/auth/verify` 两个接口 + 短信服务集成
- **前端**：登录页手机号 Tab、倒计时按钮、错误提示态

合成一份 spec 的后果：

- 关注点正交（数据一致性 / 接口契约 / 交互体验），读者谁都不爽
- spec 极长，AI 每次加载浪费上下文；Prompt Caching 命中率下降
- 改任何一端都可能影响 spec 其他章节，评审爆炸

拆三份不互相引用的 spec 的后果：

- 字段名、枚举值在三端漂移（前端叫 `mobile`、后端叫 `phone`、DB 叫 `phone_num`）
- AC 各写各的，没有"端到端"维度的验证

---

## 2. 推荐结构：主 spec + 契约 + 子 spec

```
docs/specs/0002-phone-login/
├── 0002-main.md          # 主 spec：端到端行为 + E2E 验收
├── contracts/
│   ├── openapi.yaml      # API 契约（机器可读、唯一事实源）
│   ├── schema.sql        # DB 变更（唯一事实源）
│   └── events.json       # （可选）事件契约
├── 0002-frontend.md      # 前端子 spec：页面、组件、状态流
├── 0002-backend.md       # 后端子 spec：service/repo、短信集成
└── 0002-db.md            # DB 子 spec：迁移策略、索引、回滚
```

职责划分：

| 文档 | 写什么 | 不写什么 |
|------|--------|----------|
| **主 spec** | 业务目标、功能范围、端到端 AC、非功能指标 | 字段类型、组件树、SQL |
| **契约文件** | API schema、DB schema、事件 schema | 业务动机、非功能需求 |
| **子 spec** | 本层实现细节、内部 AC、风险 | 重复主 spec / 契约的内容 |

铁律：**子 spec 必须引用契约**（如 `本接口实现见 contracts/openapi.yaml §POST /auth/send-code`），不要在子 spec 里再复制一份字段定义——复制一定会漂移。

---

## 3. 跨栈 AC 必须端到端

**反模式**：三份 spec 各写各的 AC，单元测试都通过，集成起来挂。

**正确模式**：主 spec 的 AC 必须是**用户视角、跨三层的**：

```
[AC-E2E-1] 用户在登录页输入合法手机号 → 点击"获取验证码"
           → 后端 send-code 返回 200
           → DB verification_code 表新增 1 行（code 6 位，expires_at = now+5min）
           → 前端进入 60s 倒计时状态
```

这条 AC 对应的测试必须是**端到端测试**（Playwright / Cypress / 集成测试套件），不是单元测试就能满足。子 spec 可以有层内 AC，但**不能替代**端到端 AC。

> 跨层 AC 的"三段式"完整写法（前端 + 接口 + 数据），见 09 章 §5.2。

---

## 4. 契约层才是跨栈 SDD 的命门

真正让跨栈 SDD 跑通的不是 spec 本身，是**契约层**：

| 契约 | 事实源 | 谁先动 | 变更规则 |
|------|--------|--------|----------|
| API 契约 | `openapi.yaml` | 约定"谁先动" | 必须前后端双 review |
| DB 契约 | `schema.sql` / 迁移脚本 | DBA 或后端 owner | 必须包含回滚策略 |
| 事件契约 | `events.json` | 发布方 | 订阅方必须 review |

**铁律**：契约变更走**独立 PR**，contract-only PR 先合入，实现 PR 引用契约 commit。三端才能并行开发。

---

## 5. 进阶：多仓 SDD —— spec 独立成仓 + 版本 tag

§2 的"主 spec + 契约 + 子 spec"在**单仓**里就够用。但当前后端 / DB 由不同团队拥有、需要分开发版、跨季度演进时，把上面那套放进同一个仓会出新问题：spec PR 与代码 PR review 焦点漂移、跨团队改 spec 互相隐形、"哪份代码对应哪版 spec"只能靠 commit hash 反查。

实战项目 `04-SDD 实战项目/sdd-02-task-fullstack/` 给出的解法是**把 spec 仓独立出来 + 用 git tag 做版本化**。这一节抽出可跨项目复用的最小骨架。

### 5.1 仓库布局

```
<workspace>/
├── task-spec/          ← spec 唯一真源仓（纯 Markdown）
│   ├── 01-需求分析.md
│   ├── 02-功能规范.md  （含模块拆分 FE-* / BE-* / DB-*）
│   ├── 03-技术方案.md
│   ├── 04-验收标准.md  （三维矩阵，见 09 章 §1.4）
│   ├── conventions/    ← 通用约束 9 支柱（见 03 章 §X）
│   └── tasks/          ← Task 轨迹 md（见 15 章）
├── task-frontend/      ← 前端代码仓（独立 repo）
├── task-backend/       ← 后端代码仓（独立 repo）
└── task-db/            ← DB 迁移脚本仓（独立 repo）
```

`task-spec` 不放任何可执行代码、不放迁移脚本——它只承载决策和契约。

### 5.2 spec 仓的语义化版本

`task-spec` 通过 **git tag** 对外发版，格式严格 `vMAJOR.MINOR.PATCH`：

| 版本位 | 触发条件 |
|---|---|
| `MAJOR` | 破坏性变更：删接口 / 改错误码语义 / 删 BR / 改 DB 列名 / 改枚举字符串 |
| `MINOR` | 向后兼容新增：加接口 / 加可空字段 / 加 BR / 加模块 / 加页面 |
| `PATCH` | 文档澄清 / 拼写 / 不改变契约的措辞修正 |

> "契约变了没"判断基准：任何能让下游**测试用例期望改变**的变更，至少是 MINOR；任何能让**已通过的测试用例失败**的变更，是 MAJOR。

发版流程：spec PR → review → merge 到 main → `git tag vX.Y.Z` → `git push origin vX.Y.Z` → 在 GitHub / Gitee 对应 tag 写 Release Notes。

### 5.3 下游仓的对齐义务

每个代码仓（`task-frontend` / `task-backend` / `task-db`）的 README **顶部**必须显式声明：

```markdown
**Implements**: spec@v1.2.0
```

- 下游仓**不依赖** spec 仓作为运行时（不用 submodule），对齐是**文字声明**
- 升级 spec tag = 下游提 PR 改两处：README 顶部 + 对应实现代码
- 关键代码位置加追溯注释 `[SDD-SPEC: 02-功能规范.md §3.4]`

### 5.4 跨仓 PR 编排（严格顺序）

跨仓功能（≥2 个仓动）走 **Epic 协调**，PR 合入顺序不可乱：

```
① task-spec PR     (spec 变更 + 打新 tag)
       ↓
② task-db PR       (DDL 跟进)
       ↓
③ task-backend PR  (BE 实现)
       ↓
④ task-frontend PR (FE 实现)
       ↓
⑤ 回到 task-spec   (回填三维矩阵 TC-E2E-*，PATCH tag)
```

所有下游 PR 描述必须含三段（详见 [04 章 §8.2](./04-SDD-工具落地与工作流整合.md)）：

```markdown
## Task
- Task<NNN> (epic 链接)

## Implements
- spec version: v1.2.0 (no bump) / v1.2.0 → v1.3.0 (bump)

## Covers
- §3.4 API-04 update
- §4 BR-06
- TC-U-05 (new), TC-IT-10 (updated)
```

### 5.5 什么时候上多仓 SDD

档位判据：满足以下**任一条**才上，否则单仓主 spec + 契约 + 子 spec 就够：

- 前后端 / DB 由不同团队拥有，且各团队有独立 release cadence
- spec 评审周期与代码评审周期差异显著（如 spec 季度级、代码周级）
- 跨团队对 spec 变更的"隐性传播"已经成为故障来源
- 项目存活周期 ≥1 年，且预期 ≥3 次破坏性变更

不满足时强行多仓 = 平白增加 5 倍 PR 编排成本。

### 5.6 完整实战参考

本仓库 `04-SDD 实战项目/sdd-02-task-fullstack/` 是这套模式的完整参考实现，包括：

- 4 仓物理布局（spec / backend / frontend / db）
- 9 份 conventions（fe / db / test / security / monitoring / api-versioning / async-job / i18n / dependency-policy）
- 三维矩阵 + L1~L4 验收层级
- Task 管理（含 Expand-Contract）+ feature flag 纪律
- SIT/UAT/PROD 环境晋级 + 选择性发布

---

## 6. AI 编程工具在跨栈任务的实战模式（工具无关）

不要在同一个会话让 AI 同时生成三端代码——上下文膨胀、风格漂移、质量打折。推荐分会话（任何工具都适用——Claude Code 用多个 sub-agent / 子会话，Cursor / Aider / ChatGPT 用多个独立 chat）：

```
会话 A  基于主 spec + 澄清 → 生成 / 修改契约文件
           ↓（契约 PR 合入）
会话 B1 主 spec + 前端子 spec + 契约 → 写前端
会话 B2 主 spec + 后端子 spec + 契约 → 写后端       （B1/B2/B3 并行）
会话 B3 主 spec + DB 子 spec   + 契约 → 写迁移
           ↓
会话 C  主 spec + 契约 + 所有 AC → 写端到端测试
```

每个会话上下文都小而聚焦；契约稳定时多数厂商的上下文缓存命中率最高；某端返工只影响该端的会话。

---

## 7. 小规模的简化版

不是每个跨栈 feature 都要上"主 + 子 + 契约"结构。个人 / 小团队做跨栈小 feature 时：

- **单文件 spec**，内部按"前端 / 后端 / DB"三节分段
- 契约用 code block 内嵌，不独立成文件
- AC 仍然必须是端到端的（这条不能省）

**档位判据**：跨栈 feature 且满足 **≥2 人协作** 或 **维护周期 ≥2 个月** 之一，就应上完整结构。否则单文件足够。

---

## 8. 诚实结论

SDD 在跨栈场景**能约束得过来，但前提是放弃"一份 spec 管所有"的幻想**，接受 "主 spec + 契约 + 子 spec" 的分层结构，且把**契约层当作一等公民**来维护。契约层做不扎实，跨栈 SDD 必然失败——这和有没有 AI 无关。多团队 / 长周期项目再加一层 spec 仓独立 + tag 版本化（§5），单仓主 spec + 契约就足够支撑日常。

---

## 9. 思考与练习

1. **盘点题**：你当前项目的"前端字段名 / 后端字段名 / DB 列名"是否完全对齐？挑一个核心实体（用户 / 订单等）数一数有几个漂移。
2. **拆分题**：选一个最近做完的跨栈 feature，按本章结构反推应该怎么拆主 spec / 子 spec / 契约文件，看哪些信息是当时"散在脑子里"没写下来的。
3. **协作题**：如果让契约 PR 先于实现 PR 合入，团队需要怎么调整 review 流程？阻力主要来自哪里？
4. **多仓判断题**：照 §5.5 的判据，你团队当前项目应该上多仓 SDD 吗？如果上，哪个团队/角色会反对最强烈？
