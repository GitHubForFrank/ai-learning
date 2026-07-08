# 04 - SDD 实战项目

> **模块定位**：SDD（Spec-Driven Development，规范驱动开发）方法论的**实战案例集**。
> 配套学习笔记见 `../04-SDD 学习笔记/`。

## 模块目录约定

单仓案例(如 sdd-01)典型布局:

```
04-SDD 实战项目/
├── README.md                  # 本文件：案例索引 + 通用规则
└── sdd-NN-<短名>/             # 每个案例独立成目录
    ├── README.md              # 案例介绍 + SDD 流程复盘
    ├── specs/                 # 4 份 SDD 规范文档（核心产出）
    │   ├── 01-需求分析.md
    │   ├── 02-功能规范.md
    │   ├── 03-技术方案.md
    │   └── 04-验收标准.md
    ├── src/                   # 代码实现（语言/技术栈可按案例不同）
    └── <构建文件>             # pom.xml / package.json / go.mod 等
```

多仓案例(如 sdd-02,真实工程的主流形态)典型布局:

```
04-SDD 实战项目/
└── sdd-NN-<短名>/             # 工作区目录（非 git 仓库，仅作聚合展示）
    ├── README.md              # 工作区总览 + 多仓布局说明
    ├── docker-compose.yml     # 跨仓联调的协调物（不属于任何单仓）
    ├── <project>-spec/        # 🔵 [repo] 规范仓（唯一真源，带 git tag 版本化）
    │   ├── 01-需求分析.md ... 05-跨仓协调.md
    │   └── conventions/
    ├── <project>-backend/     # 🟢 [repo] 后端代码仓
    ├── <project>-frontend/    # 🟡 [repo] 前端代码仓
    └── <project>-db/          # 🔴 [repo] DB 迁移仓（Flyway 脚本独立管理）
```

**命名规则**:
- 案例目录:`sdd-NN-<短名>/`（两位数字编号 + 短名，例:`sdd-01-task-api`、`sdd-02-task-fullstack`）
- 多仓子目录:`<project>-<role>/`(例:`workitem-docs`、`workitem-backend`),每个子目录对应真实世界的一个独立 git 仓

**交付产物**:spec 是每个案例的**核心**且**必须**产出;代码为按 spec 实现的产出;README 为案例复盘。

## 案例索引

| 编号 | 案例 | 技术栈 | 核心演示内容 | 路径 |
|---|---|---|---|---|
| sdd-01 | 全栈多仓 Workitem | JDK 21 + Spring Boot 4.0.5 + MyBatis Plus + MySQL 8 + Flyway + Vue 3 + Vite + TS | **多仓架构**：workitem-docs / workitem-backend / workitem-frontend / workitem-db 四独立 git 仓 + 约束 spec 层（`workitem-docs/specs/90-工程约束.md` 等）+ 跨仓协调 + 三维矩阵（BR × 模块 × TC）+ L4 E2E 验收 + Task 管理 | [sdd-01-workitem-fullstack/](./sdd-01-workitem-fullstack/) |

> 后续新增案例时，在表格追加一行并创建对应子目录即可。
> sdd-01 引入了 **约束 spec 层**（`workitem-docs/specs/` 下存放跨功能的通用约束：工程约束/编码规范/接口治理）和**多仓拆分**（docs/backend/frontend/db 各自独立成 git 仓，在教学目录里聚合展示）。

## 新增案例的起步流程

1. **复制目录骨架**：`cp -r sdd-01-workitem-fullstack sdd-NN-<新短名>`（仅作脚手架参考，不要盲抄业务代码）
2. **清空并重写 workitem-docs/specs/01~04**：SDD 的核心就是**先写规范再写代码**，切忌从代码倒推 spec
3. **按 spec §9 的可追溯矩阵逐步实现 src/**，关键代码处加 `[SDD-SPEC: xx.md §y.z]` 注释
4. **按 spec §04 的验收标准补齐测试**，`mvn test` / 对应命令全绿
5. **在本文件「案例索引」表追加一行**

## 通用原则（适用于本模块所有案例）

- **spec 冻结再写代码**：任何在实现阶段冒出的新决策，要么回去修 spec，要么留到下个迭代
- **spec ↔ 代码双向可追溯**：每个业务规则有编号（BR-*），每段代码有 `[SDD-SPEC]` 注释指回 spec 条款
- **每条 spec 规则至少一条测试**：`specs/04-验收标准.md` 的可追溯矩阵就是清单
- **范围外显式声明**：`specs/01-需求分析.md §6` 必须写"不做什么"，消除需求蠕变

## 学习路径建议

1. 先读 `sdd-01-workitem-fullstack/README.md` 理解工作区多仓布局
2. 顺序通读 `workitem-docs/specs/01-需求分析.md → 02-功能规范.md` 等核心 spec —— 看文档如何衔接
3. 打开 `workitem-backend/src/.../WorkitemServiceImpl.java`，对照 `[SDD-SPEC]` 注释跳回 spec —— 建立追溯感
4. 跑一次 `cd sdd-01-workitem-fullstack/workitem-backend && mvn test` —— 验证测试全绿
5. 挑一个延伸项，**先改 specs 再改代码**，完整走一轮需求变更闭环
