# workitem-db:数据库迁移仓(DB Migration Repo)

> **仓库角色**:存放所有 Flyway 迁移脚本和 DB 相关资产,是 `workitem-docs/specs/02-功能规范.md §8.3` 的 DB-* 模块物理所在地。
>
> **Implements**: `spec@vTBD`(P2 阶段发布首个 spec tag 后填入)
>
> **当前阶段**:✅ **已完成(Task001)**

## 一、本仓职责

- 存放所有 `V<N>__<snake_case>.sql` 迁移脚本(位于 `migration/`)
- 承担 `workitem-docs/specs/02-功能规范.md §8.3` 的 DB-01 / DB-02 / DB-03(可选)
- 遵守 `workitem-docs/conventions/db-conventions.md` 的命名与迁移约束
- 为下游消费者(`workitem-backend` 启动时、CI 镜像构建时)提供稳定的脚本路径

## 二、本仓不做

- ❌ 任何应用代码或 Mapper XML —— 归 `workitem-backend`
- ❌ 业务规则判断 —— 归 `workitem-docs`
- ❌ Flyway 执行器本体 —— 仅存脚本,执行在消费侧
- ❌ 生产环境 DB 运维脚本(备份 / 扩容) —— 超出本项目范围

## 三、目录结构(P2 阶段产出,当前占位)

```
workitem-db/
├── README.md                                # 本文件
└── migration/
    └── V1__init_task_table.sql              # [P2] 建 task 表 + 2 个索引
    # 未来:
    # V2__create_v_task_active.sql            可选视图
    # V3__...                                 需求变更时连续递增
```

## 四、迁移脚本的使用方

| 使用方 | 使用方式 | 触发时机 |
|---|---|---|
| `workitem-backend`(本地开发) | `spring.flyway.locations=filesystem:../workitem-db/migration` | Spring Boot 启动时 |
| `workitem-backend` CI / 构建 | 构建期把 `migration/` 拷贝到 jar 的 `classpath:db/migration/` | 镜像构建时 |
| `workitem-db` 独立 CI(可选) | 在 CI runner 里直接跑 `flyway migrate`,对接 staging 库 | merge 到 main 后 |
| Testcontainers 集测 | 随 `workitem-backend` 集测启动时加载 | 测试初始化 |

> ⚠️ 不论哪种使用方,Flyway 的**已执行 checksum**必须保持一致。这正是"独立 DB 仓 + 版本化"的自我保障。

## 五、与 spec 的对齐义务

1. 本 README 顶部 `Implements: spec@vX.Y.Z` **必须**保持与主干脚本一致
2. **所有 PR 标题以 `Task<NNN>:` 起头**(Task 编号见 `workitem-docs/specs/06-任务与发布管理.md §2`)
3. 所有脚本文件名遵循 `V<N>__task<NNN>_<desc>.sql`(见 `workitem-docs/conventions/db-conventions.md §6.2`)
4. 脚本头部**必须**双标签注释:`[SDD-TASK: Task<NNN>]` + `[SDD-SPEC: ...]`(见 `db-conventions.md §8`)
5. 破坏性 DDL **禁止单步完成**,必须按 `workitem-docs/specs/06-任务与发布管理.md §4` 拆成 Expand-Contract 两个 Task
6. 已发布(已合入 main)的脚本**禁止修改**,只能新增 `V<N+k>__task<NNN+?>_*.sql` 走 ALTER / DROP

## 六、发布后约束(本仓专属纪律)

- 已进入 main 的 `V<N>__*.sql` 视为**已发布**
- 任何修改(哪怕一个空格)都会改变 Flyway checksum,导致下游消费者启动失败
- 发现错误 → 新增下一版 `V<N+1>__*.sql` 反做或修正
- 这个约束比 `workitem-backend` / `workitem-frontend` 更严格,因为 DB 状态是不可回滚的持久化

## 七、启动与验证

### 7.1 前置

- 本机或可达的 MySQL 8.0 实例已运行(开发者自备,本案例不提供容器编排,详见工作区 README §三)
- 已手动创建空库 `zmz_sdd_demo`(`CREATE DATABASE zmz_sdd_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`)
- 连接信息已填入 `workitem-backend` 的 `application.properties`(或本地覆盖 `application-local.properties`)

### 7.2 通过 workitem-backend 自动迁移(本项目主路径)

`workitem-backend` 启动时通过 `spring.flyway.locations=filesystem:../workitem-db/migration` 自动加载并执行本仓所有 V 脚本。Verify:

```bash
cd "04-SDD 实战项目/sdd-01-workitem-fullstack/workitem-backend"
mvn spring-boot:run
```

**Verify**:控制台日志含 `Flyway Community Edition ... Migrating schema "zmz_sdd_demo" to version "1 - task001 init task table"` 等 4 条迁移记录。

### 7.3 直接连库验证(可选)

```bash
mysql -h <host> -P <port> -u <user> -p<password> zmz_sdd_demo \
  -e "SHOW TABLES; SELECT username, status, failed_login_count FROM app_user;"
```

**Verify**:返回 4 张表 `task` / `app_user` / `login_log` / `flyway_schema_history`,`app_user` 中有一行 `admin`。

---

**占位状态**:本 README 在 P1 阶段仅声明职责与对齐规则。P2 阶段补齐 `V1__init_task_table.sql`、独立运行命令、CI 钩子规划。
