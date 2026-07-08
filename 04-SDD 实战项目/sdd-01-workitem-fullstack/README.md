# sdd-01-workitem-fullstack:多仓全栈 SDD 演练(工作区总览)

> **模块**:`04-SDD 实战项目/` · **案例**:`sdd-01-workitem-fullstack/` · **所属**:SDD 方法论参考实现
>
> ⚠️ **这个目录不是 git 仓库,而是 4 个独立仓库的"工作区聚合展示"**。
> 真实世界里每个 `workitem-*/` 子目录都是**独立 clone** 的 git 仓库,本教学目录把它们并排放在一起,方便对照阅读。

## 一、本案例的核心问题与答案

**核心问题**:

> 当 spec、前端、后端、数据库四者各自独立成 git 仓库,并且功能要经历 SIT → UAT → PROD 分批晋级时,
> SDD 的"规范 → 代码 → 验收"链条怎么串而不散架?

**本案例的回答覆盖以下维度**:

| 维度 | 对应产出 |
|---|---|
| Spec 独立成仓 + 版本 tag | `workitem-docs/`(真源)+ `workitem-docs/specs/05-跨仓协调.md` |
| 前端独立成仓(Vue 3 + Vite + TS) | `workitem-frontend/` |
| 后端独立成仓(JDK 21 + Spring Boot 4.0.5 + MyBatis Plus + HikariCP + Logback) | `workitem-backend/` |
| DB 迁移独立成仓(Flyway) | `workitem-db/migration/` |
| 约束 spec 分层(9 份) | `workitem-docs/specs/` |
| 跨仓协调 / Task 管理 / 并行 / 环境晋级 | `workitem-docs/specs/05-08` |
| 三维追溯矩阵 + L4 E2E | `workitem-docs/specs/04-验收标准.md` |

## 二、4 仓布局与职责

```
sdd-01-workitem-fullstack/                # ⚠️ 工作区目录(非 git 仓库)
├── README.md                          # 本文件:工作区总览 + 多仓布局说明
├── workitem-docs/                         # 🔵 [repo 1] 规范仓(唯一真源)
├── workitem-backend/                      # 🟢 [repo 2] 后端代码仓(含 .gitignore:Java/Maven)
├── workitem-frontend/                     # 🟡 [repo 3] 前端代码仓(含 .gitignore:Node/Vite)
└── workitem-db/                           # 🔴 [repo 4] DB 迁移脚本仓
```

> 📌 **.gitignore 分层约定**:跨语言通用模式(IDE / OS / 日志)在仓库根 `.gitignore`;
> 语言特定模式(Java `target/`、Node `node_modules/` 等)放在对应 `workitem-*/` 子目录的 `.gitignore`,
> 与"每个 `task-*` 都是独立 git 仓"的设定一致 —— 真实克隆时各自带走自己的 `.gitignore`。
> `workitem-docs/`(纯 Markdown)和 `workitem-db/`(纯 SQL)无构建产物,沿用根 `.gitignore` 即可。

| 仓 | 角色 | 所属团队(假想) | README |
|---|---|---|---|
| `workitem-docs` | 决策/契约的唯一真源;版本化发布 | 架构 / 业务分析师 | [workitem-docs/README.md](./workitem-docs/README.md) |
| `workitem-backend` | Spring Boot 服务实现 spec | 后端团队 | [workitem-backend/README.md](./workitem-backend/README.md) |
| `workitem-frontend` | Vue 3 SPA 实现 spec | 前端团队 | [workitem-frontend/README.md](./workitem-frontend/README.md) |
| `workitem-db` | Flyway 迁移脚本 + DB 资产 | DBA / 平台团队 | [workitem-db/README.md](./workitem-db/README.md) |

**核心协作规则**:`workitem-docs` 先发版(打 tag),下游 3 仓在自己的 README 里声明"实现的 spec 版本号",跨仓功能变更走 **epic 协调**(详见 `workitem-docs/specs/05-跨仓协调.md`)。

## 三、本地 MySQL:开发者自备,不走容器编排

本案例**不提供** `docker-compose.yml`。SDD 的实际开发流程是:

- **本地直接跑**前后端进程(`mvn spring-boot:run` + `npm run dev`),不在开发机上做容器编排
- DB 由开发者**提前手动配置好**(本机已装的 MySQL 8.0 / 公司内网共享开发库 / 任何可达实例均可)
- 仓库不沉淀任何"跨仓基础设施脚本",避免出现"只在本工作区可跑"的隐性耦合
- 生产 / SIT / UAT / PROD 由基础设施团队按各自规则托管(K8s、托管 RDS 等),**不在本案例范围**

> ⚠️ 学习者只需保证启动 `workitem-backend` 前,本机或可达的 MySQL 8.0 实例上已存在 schema `zmz_sdd_demo`(空库即可,Flyway 会建表)。
> 连接参数走 `workitem-backend/src/main/resources/application.properties`,如需本地覆盖请用 `application-local.properties`(已被 `.gitignore` 排除)。

## 四、分阶段交付状态

| 阶段 | 产出 | 状态 | 涉及仓 |
|---|---|---|---|
| **P1 specs 先行** | `workitem-docs/` 全部内容(8 份主 spec + 3 份约束 spec + 跨仓协调 / 任务管理 / 多任务并行 / 环境晋级 / 回归)+ 4 个仓的 README | ✅ **已完成** | workitem-docs |
| **P2 backend + db** | `workitem-backend/` Spring Boot 4 + JDK 21 + MyBatis Plus + Spring Security + JWT + Logback;`workitem-db/migration/V1~V4*.sql`(MySQL 由开发者本机自备) | ✅ **已完成(Task001)** | workitem-backend、workitem-db |
| **P3 frontend** | `workitem-frontend/` Vue 3 + Vite + TS + axios(traceparent + JWT 注入)+ 5 个 view | ✅ **已完成(Task001)** | workitem-frontend |
| **P4 E2E + 闭环** | Playwright E2E + 回填三维矩阵中的 TC-E2E-* | ⏸ 待启动 | workitem-frontend(主)+ 其他仓 |

## 五、如何在真实世界复现这个工作区

> 以下命令是**示意性**的;P2/P3 阶段具体 URL 补齐,每条命令伴随 verify。

### 5.1 教学场景(本仓库内)

什么都不做 —— 本目录就是 4 仓聚合展示,直接按 §二、§四 阅读即可。

### 5.2 真实场景(假想 4 个独立远端仓)

```bash
# 在任意本地目录下建工作区
mkdir workitem-workspace && cd workitem-workspace

# 按职责 clone 4 个仓
git clone <spec-repo-url>     workitem-docs
git clone <backend-repo-url>  workitem-backend
git clone <frontend-repo-url> workitem-frontend
git clone <db-repo-url>       workitem-db

# 自备本机 / 内网 MySQL 8.0,创建空库 zmz_sdd_demo
# 把连接信息填到 workitem-backend 的 application-local.properties(被 .gitignore 排除)

# 验证布局
ls
# 期望看到:workitem-docs workitem-backend workitem-frontend workitem-db
```

## 六、学习者阅读路径建议

1. 读 **本 README** 理解工作区多仓布局
2. 进入 `workitem-docs/` 按 `README.md` → `specs/01-需求分析.md` → `specs/02-功能规范.md` → `design/03-技术方案.md` → `specs/04-验收标准.md` → `specs/05-跨仓协调.md` → `specs/06-任务与发布管理.md` → `specs/07-多任务并行开发.md` → `specs/08-环境晋级与选择性发布.md` 顺序阅读
3. 读 `workitem-docs/specs/` 其余技术规范(按需,工程约束/编码规范/接口治理三份)
4. 依次瞄一眼 `workitem-backend/README.md`、`workitem-frontend/README.md`、`workitem-db/README.md`(已完成,声明仓定位和 spec 版本对齐规则)
5. P2/P3/P4 完成后,再对照代码验证"代码 100% 忠于 spec"

## 七、文件索引速查

| 我想了解... | 去哪里看 |
|---|---|
| 为什么拆成 4 个仓 | 本 README §一、§二 |
| 为什么不走 docker-compose / 本机 MySQL 怎么准备 | 本 README §三 |
| 规范真源在哪 | [`workitem-docs/`](./workitem-docs/) |
| spec 版本 tag 和跨仓 PR 顺序 | [`workitem-docs/specs/05-跨仓协调.md`](./workitem-docs/specs/05-跨仓协调.md) |
| **Task 编号、DDL 分期、feature flag 纪律** | [`workitem-docs/specs/06-任务与发布管理.md`](./workitem-docs/specs/06-任务与发布管理.md) |
| **N 个 Task 并行、撞号 resolve、Task 轨迹 md** | [`workitem-docs/specs/07-多任务并行开发.md`](./workitem-docs/specs/07-多任务并行开发.md) |
| **SIT/UAT/PROD 晋级、选择性发布、flag matrix** | [`workitem-docs/specs/08-环境晋级与选择性发布.md`](./workitem-docs/specs/08-环境晋级与选择性发布.md) |
| 单个 Task 的轨迹文档 | [`workitem-docs/tasks/`](./workitem-docs/tasks/) |
| 历次环境晋级 release plan | [`workitem-docs/releases/`](./workitem-docs/releases/) |
| **开发期 agent 护栏(工具无关 checklist)** | [`workitem-docs/agents/`](./workitem-docs/agents/) |
| 全部规范(项目 + 技术 + 流程) | [`workitem-docs/specs/`](./workitem-docs/specs/) |
| 技术设计文档 | [`workitem-docs/design/`](./workitem-docs/design/) |
| — 工程约束(安全/测试/MCP/DB/日志/配置) | [`specs/90-工程约束.md`](./workitem-docs/specs/90-工程约束.md) |
| — 编码规范(命名/异常/分层/注释/SDD追溯) | [`specs/91-编码规范.md`](./workitem-docs/specs/91-编码规范.md) |
| — 接口治理(版本化/错误码/分页/幂等/SLA) | [`specs/92-接口治理.md`](./workitem-docs/specs/92-接口治理.md) |
| 业务需求与 BR-01~08 | [`workitem-docs/specs/02-功能规范.md`](./workitem-docs/specs/02-功能规范.md) |
| 跨仓三维矩阵 | [`workitem-docs/specs/04-验收标准.md §2`](./workitem-docs/specs/04-验收标准.md) |
| 每个仓的角色和 spec 对齐规则 | 各 `workitem-*/README.md` |

---

**结语**:本案例回答"SDD 在多仓多团队多环境下怎么协作"。
当 spec 仓独立成为唯一真源,带版本号、带 tag,下游各仓用代码兑现 spec 的每一个编号时,
无论人还是 AI,都能基于同一份契约产出一致的结果。
