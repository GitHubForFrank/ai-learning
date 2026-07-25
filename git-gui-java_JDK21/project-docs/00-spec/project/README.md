# spec/ — git-gui 项目约束文档索引

> 本目录为 git-gui（Java 图形化 Git 客户端）项目专属规范。**通用方法论规则见 [shared/](../shared/)**。
>
> 所有独立主题的约束规则按编号排列，**编号前缀即分组**，编号大小即组内阅读优先级。

---

## 规范体系说明

本项目采用 **基座 + 扩展** 模式：

- `shared/xx-base.md`：跨项目共享的通用方法论（SDD 流程、DDD 分层、测试策略、桌面打包等）
- `./xx.md`：git-gui 专属规范（BR 规则、服务契约、表结构、JavaFX UI 等）
- 规则冲突时：**项目专属规范优先于 shared**

---

## 编号分组说明

| 前缀 | 领域 | 说明 |
| ------ | ------ | ------ |
| **0X** | Core | 全局核心规则。继承 shared，追加项目桌面配置和技术选型禁止清单 |
| **1X** | Domain | 业务规则 + 实体字典。BR-01~BR-42，完全项目专属 |
| **2X** | API | 服务契约（UI 层 ↔ 领域层内部 Java 接口，非 HTTP）。完全项目专属 |
| **3X** | Backend | 后端实现。继承 shared 的 DDD 分层，追加 JGit/CLI 适配器、命令红线拦截器、异步任务、Guice 绑定 |
| **4X** | Database | 数据库规约。继承 shared 的迁移规范，追加 SQLite 表结构（7 张表 + V1~V7） |
| **5X** | Frontend | UI 实现。继承 shared 的 JavaFX UI 层规范，追加主窗口架构与对话框清单 |
| **6X** | QA | 质量保障。继承 shared 的测试策略/门禁/日志，追加应用内状态面板与桌面性能 SLO |
| **7X** | Ops | 运维/依赖。继承 shared 的桌面打包，追加项目依赖和配置项 |

---

## 文件索引

### 项目专属规范

| 编号 | 文件 | 内容 |
| ------ | ------ | ------ |
| 00 | [00-core.md](./00-core.md) | 桌面配置 + 技术选型禁止清单 |
| 01 | [01-domain.md](./01-domain.md) | BR-01~BR-42 业务规则 + 7 实体字典 |
| 02 | [02-api.md](./02-api.md) | 11 服务接口契约 + 错误码注册表（非 HTTP） |
| 03 | [03-backend.md](./03-backend.md) | 桌面 DDD、JGit/CLI 适配器、命令红线拦截器、异步任务、Guice |
| 04 | [04-database.md](./04-database.md) | SQLite 选型 + 7 张表结构 + V1~V7 迁移历史 |
| 05 | [05-frontend.md](./05-frontend.md) | JavaFX 21 技术栈 + 主窗口 + 对话框清单 |
| 06 | [06-quality.md](./06-quality.md) | 应用内状态面板 + 桌面性能 SLO |
| 07 | [07-ops.md](./07-ops.md) | 依赖选型 + 配置项 + 资源限制 |

### 共享基座（跨项目通用）

| 编号 | 文件 | 内容 |
| ------ | ------ | ------ |
| — | [shared/README.md](../shared/README.md) | 共享规范索引 + 术语表 |
| 00 | [shared/00-core-base.md](../shared/00-core-base.md) | SDD 工作流、Task 编号、代码追溯、文档管理、门禁、安全、Spec 间引用 |
| 03 | [shared/03-backend-base.md](../shared/03-backend-base.md) | DDD 分层、包结构、UI 层/Guice 适配器规范 |
| 04 | [shared/04-database-base.md](../shared/04-database-base.md) | 迁移脚本命名、执行、索引策略、Flyway 自动执行 |
| 05 | [shared/05-frontend-base.md](../shared/05-frontend-base.md) | JavaFX UI 层：FXML/Controller/ViewModel、CSS 主题、国际化 |
| 06 | [shared/06-quality-base.md](../shared/06-quality-base.md) | 测试策略、BR 覆盖、门禁、日志规范 |
| 07 | [shared/07-ops-base.md](../shared/07-ops-base.md) | 桌面打包流程、单实例锁、用户数据目录 |

### 项目配置

| 文件 | 内容 |
| ------ | ------ |
| [project.yml](./project.yml) | 数据目录、DB 路径、日志路径、锁文件、Git 可执行文件路径、包名 com.gitgui 等集中配置 |

---

## 快速定位

| 我在找… | 去哪个文件 |
| ------ | ------ |
| SDD 工作流 / Task 编号 / 追溯注释 / 文档管理 / 安全约束 | [shared/00-core-base.md](../shared/00-core-base.md) |
| 数据目录 / DB 路径 / 锁文件 / Git 路径 | [00-core.md](./00-core.md) + [project.yml](./project.yml) |
| 某个 BR 是什么 / 某个实体有哪些字段 | [01-domain.md](./01-domain.md) |
| 某个服务接口的方法签名 / 错误码 | [02-api.md](./02-api.md) |
| DDD 分层 / 包结构 / Guice 怎么写 | [shared/03-backend-base.md](../shared/03-backend-base.md) |
| 命令红线拦截器 / 12 条 RedLineRule / 异步任务 | [03-backend.md](./03-backend.md) |
| 数据库表结构 / 迁移历史 | [04-database.md](./04-database.md) |
| 迁移脚本命名规则 / 怎么写迁移脚本 | [shared/04-database-base.md](../shared/04-database-base.md) |
| JavaFX UI 规范 / FXML / ViewModel | [shared/05-frontend-base.md](../shared/05-frontend-base.md) |
| 主窗口架构 / 对话框清单 / 上下文菜单 | [05-frontend.md](./05-frontend.md) |
| 测试策略 / BR 覆盖 / 门禁 / 日志 | [shared/06-quality-base.md](../shared/06-quality-base.md) |
| 应用内状态面板 / 桌面性能 SLO | [06-quality.md](./06-quality.md) |
| 桌面打包 / 单实例锁 / 用户数据目录 | [shared/07-ops-base.md](../shared/07-ops-base.md) |
| 项目依赖 / 配置项 / 资源限制 | [07-ops.md](./07-ops.md) + [project.yml](./project.yml) |

---

## 术语表

| 术语 | 代码/文件中 | 说明 |
| ------ | ------ | ------ |
| 收藏 | Favorite / `favorite` 表 | 用户标记的常用仓库，含 alias/group/pinned/sortOrder |
| 最近仓库 | RecentRepo / `recent_repo` 表 | 按最后打开时间倒序的仓库列表，默认保留 20 条 |
| 操作日志 | OperationLog / `operation_log` 表 | 每次 Git 操作完成后追加写入，记录成功/失败/取消 + 耗时 |
| 红线审计 | AuditLog / `audit_log` 表 | 命中命令红线的操作审计记录，追加写入不可删除（BR-31） |
| 命令红线 | CommandRedLine / RedLineRule | 危险 Git 操作的拦截机制，分阻断（BLOCK）与二次确认（CONFIRM）两类 |
| 保护分支 | ProtectedBranch / `red_line.protected_branches` | 受保护不可 force push / 不可删除的分支清单，支持通配符（BR-27） |
| 远程白名单 | RemoteWhitelist / `red_line.remote_whitelist` | 授权可推送的远程 URL 清单，非白名单推送阻断（BR-28） |
| 敏感文件规则 | SensitiveFileRule / `red_line.sensitive_file_rules` | 暂存区敏感文件扫描规则，命中阻断并提示加入 .gitignore（BR-32） |
| 任务记录 | TaskRecord / `task_record` 表 | 异步任务状态持久化，PENDING→RUNNING→SUCCESS/FAILED/CANCELLED |
| 仓库元信息 | RepositoryMeta / `repository_meta` 表 | 仓库当前分支/HEAD/远程 URL 等缓存信息 |
| 应用设置 | AppSettings / `app_settings` 表 | key-value 持久化设置，含红线配置/UI/Git/外部工具等 |
| 拦截器 | CommandInterceptor | 写操作前置拦截，调 CommandRedLineService.check 处理 PASS/BLOCK/CONFIRM |
| 适配器 | GitOperationExecutor | Git 操作执行器接口，JGit 主 + CLI 兜底，由 GitExecutorRouter 路由 |
| 迁移脚本 | `V{NN}__{description}.sql` | 版本化 DDL 变更脚本，Flyway 启动时按 V 号顺序自动执行 |
| Spec 间引用 | `参见 [{file} §{section}]` | spec 文件之间的交叉引用格式 |
