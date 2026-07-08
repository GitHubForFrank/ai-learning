# workitem-docs:规范仓(Spec Repo)

> **仓库角色**:task 业务的**唯一决策真源**。所有业务规则、接口契约、数据字典、模块拆分、验收标准、跨团队协作纪律**都在这里**,不在任何代码仓。
>
> **仓库职责**:
> - 承载 `specs/01-需求分析.md` / `specs/02-功能规范.md` / `design/03-技术方案.md` / `specs/04-验收标准.md` / `specs/05-跨仓协调.md`
> - 维护 `specs/` 下跨功能的通用约束
> - 通过 **git tag** 对外发布版本(如 `v1.0.0`、`v1.1.0`),供下游仓锚定
>
> **仓库不做**:
> - ❌ 任何可执行代码
> - ❌ 数据库迁移脚本(归 `workitem-db`)
> - ❌ 基础设施 / 容器编排脚本(本案例不沉淀,MySQL 由开发者本机自备,详见工作区 README §三)

## 一、文件索引

```
workitem-docs/
├── README.md                      # 本文件:仓库角色、版本规则、阅读顺序
├── specs/                         # 全部规范(项目规范 + 技术约束 + 流程纪律)
│   ├── 01-需求分析.md                 # Phase 1:为什么做 / 做什么 / 不做什么
│   ├── 02-功能规范.md                 # Phase 2:接口契约 + §8 模块拆分(FE/BE/DB)
│   ├── 04-验收标准.md                 # Phase 4:三维矩阵 BR × 模块 × TC + L4 E2E
│   ├── 05-跨仓协调.md                 # 多仓专属:版本 tag + epic + 跨仓 PR 纪律
│   ├── 06-任务与发布管理.md           # 多仓专属:Task 编号 + Expand-Contract + feature flag
│   ├── 07-多任务并行开发.md           # 多仓专属:N 个 Task 并行纪律 + Task 持久化 md 规范
│   ├── 08-环境晋级与选择性发布.md     # 多仓专属:SIT/UAT/PROD 环境晋级 + flag 矩阵 + release plan
│   ├── 90-工程约束.md                  # 安全 / 测试 / 监控 / 回归
│   ├── 91-编码规范.md                  # DB / 前端 / 国际化 / 依赖 / 异步任务
│   └── 92-接口治理.md                  # API 版本化 / 错误码
├── design/                        # 技术设计文档 + 运行时配置快照
│   └── 03-技术方案.md                 # Phase 3:技术选型 + 4 仓协作架构
├── tasks/                         # Task 轨迹文档目录(Task<NNN>-*.md),规则见 07 §6
│   └── README.md
├── releases/                      # 环境晋级 release plan 目录,规则见 08 §6
│   └── README.md
└── agents/                        # 工具无关的 agent 预设(开发期 checklist),详见 agents/README.md
    ├── README.md
    └── workitem-guardian.md             # SDD Task 全程护栏(写代码前 / 中 / 后 三阶段)
```

## 二、阅读顺序

| 序 | 文件 | 读完应该回答 |
|---|---|---|
| 1 | `specs/01-需求分析.md` | 为什么做、USF-* 和 USA-* 分别是什么、不做什么 |
| 2 | `specs/02-功能规范.md` | 5 个接口契约、BR-01~08、FE-*/BE-*/DB-* 模块拆分 |
| 3 | `design/03-技术方案.md` | 4 仓布局、技术选型、关键分层与 CORS / 迁移策略 |
| 4 | `specs/04-验收标准.md` | 三维矩阵怎么读、TC-* 编号体系、L1~L4 验收层级 |
| 5 | `specs/05-跨仓协调.md` | spec tag 怎么打、跨仓 PR 怎么排期、epic 怎么用 |
| 6 | `specs/06-任务与发布管理.md` | Task 编号怎么分配、DDL 分期怎么拆、"A 上 / B 不上"怎么做 |
| 7 | `specs/07-多任务并行开发.md` | N 个 Task 同时怎么不打架、Task md 怎么写、撞号怎么 resolve |
| 8 | `specs/08-环境晋级与选择性发布.md` | SIT 跑 10 / UAT 跑 5 / PROD 跑 3 怎么不破坏 SDD、flag matrix / release plan 怎么用 |
| 9 | `specs/*`(其余) | 命名 / 目录 / 用例编号的通用规则 |

## 三、本仓的版本策略

### 3.1 Tag 方案

采用 **Semantic Versioning**,格式 `vMAJOR.MINOR.PATCH`:

| 版本位 | 触发条件 |
|---|---|
| `MAJOR` | 破坏性变更(删接口 / 改错误码语义 / 删 BR / DB 表重命名) |
| `MINOR` | 向后兼容的新增(加接口 / 加 BR / 加列 / 加模块) |
| `PATCH` | 文档澄清 / 拼写 / 不改变契约的措辞修正 |

### 3.2 发版流程(简化)

```
spec PR → review → merge 到 main → 打 tag vX.Y.Z → 在下游仓 README 更新 "implements spec@vX.Y.Z"
```

> 详细流程与跨仓 PR 编排见 [`specs/05-跨仓协调.md`](./specs/05-跨仓协调.md)。

### 3.3 下游仓的对齐义务

每个代码仓(`workitem-backend` / `workitem-frontend` / `workitem-db`)的 README **必须**:

1. 显式声明当前主干**实现的 spec 版本 tag**(如 `Implements: spec@v1.2.0`)
2. 当 spec 出新版时,下游仓通过 PR 升级声明,同步带来实现改动
3. PR 描述必须包含"对应 spec 条款"(如 `covers: §4 BR-06, §8 FE-04`)

## 四、本仓的 CI 期望(规划)

> P1 阶段**不实施 CI**,以下仅为规划,供未来参考。

- Markdown 文件链接检查(防止 `[xxx](./yyy.md)` 失效)
- 编号一致性检查(BR / FE-* / BE-* / DB-* / TC-* 是否有编号被引用但未定义,或定义了未被使用)
- Tag 规范校验(只接受 `vX.Y.Z` 格式)

## 五、本仓不承担的事

- ❌ 编译 / 测试 / 部署(那是下游代码仓的事)
- ❌ 数据库迁移的执行(执行在 `workitem-backend` 启动或 `workitem-db` 的 CI 里,脚本本体在 `workitem-db/migration/`)
- ❌ 提供运行示例 / 代码片段超过"最小说明所需"(约束 spec 要抽象,不要沾染具体业务类名)

---

**下一步**:进入 `specs/01-需求分析.md`。
