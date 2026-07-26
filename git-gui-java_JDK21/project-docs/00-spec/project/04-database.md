# 数据库规范 — git-gui 项目专属

> **基座**：继承 [shared/04-database-base.md](../shared/04-database-base.md) — 迁移脚本命名规则、执行方式、索引策略、禁用清单。
> 本文档为 git-gui 项目专属的数据库选型、SQLite 语法约束、表结构、迁移历史。

---

## 数据库选型

| 项目 | 选型 | 说明 |
| ----- | ------ | ----- |
| 数据库 | SQLite | 轻量级、单文件、零配置，适合桌面应用本地存储 |
| 迁移方式 | Flyway 自动执行 | 应用启动时按 V 号顺序自动迁移，无需手动 |
| 驱动 | sqlite-jdbc | 纯 Java JDBC 驱动 |
| ORM | MyBatis-Plus | BaseMapper 通用 CRUD + LambdaQueryWrapper，替代手写 JDBC |
| 主键策略 | 应用层 UUID（TEXT） | 不依赖自增 ID |
| 并发模式 | WAL | 单写多读，桌面应用单进程 |

---

## SQLite 语法约束

- 使用 `CREATE TABLE IF NOT EXISTS`
- 使用 `CREATE INDEX IF NOT EXISTS`
- 主键统一使用 `TEXT PRIMARY KEY NOT NULL`
- 时间字段使用 `TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))`
- 布尔值使用 `INTEGER NOT NULL DEFAULT 1`（1 = true, 0 = false）
- 外键使用 `FOREIGN KEY (...) REFERENCES ... ON DELETE CASCADE`

---

## 表结构

### app_settings（应用设置）

> 服务 BR：BR-27（保护分支清单）、BR-28（远程白名单）、BR-32（敏感文件规则）、BR-38（主题/语言/快捷键）

| 列 | 类型 | 约束 | 说明 |
| --- | ------ | ------ | ----- |
| `id` | TEXT | PK | 主键 |
| `key` | TEXT | NOT NULL, UNIQUE | 设置键 |
| `value` | TEXT | NOT NULL DEFAULT '' | 设置值（JSON 字符串） |
| `category` | TEXT | NOT NULL DEFAULT 'UI' | 分类（RED_LINE/UI/GIT/EXTERNAL_TOOL/SSH/REPO_SCAN/RECENT） |
| `description` | TEXT | DEFAULT '' | 设置说明 |
| `updated_at` | TEXT | NOT NULL | 更新时间 |

**索引**：`key` UNIQUE、`category`

---

### favorite（收藏）

> 服务 BR：BR-03（唯一性）、BR-04（置顶与排序）

| 列 | 类型 | 约束 | 说明 |
| --- | ------ | ------ | ----- |
| `id` | TEXT | PK | 主键 |
| `repo_path` | TEXT | NOT NULL, UNIQUE | 仓库绝对路径 |
| `alias` | TEXT | DEFAULT '' | 别名 |
| `group` | TEXT | DEFAULT '' | 分组 |
| `pinned` | INTEGER | NOT NULL DEFAULT 0 | 是否置顶 |
| `sort_order` | INTEGER | NOT NULL DEFAULT 0 | 排序权重 |
| `remote_url` | TEXT | DEFAULT '' | 远程 URL（缓存） |
| `created_at` | TEXT | NOT NULL | 创建时间 |
| `updated_at` | TEXT | NOT NULL | 更新时间 |

**索引**：`repo_path` UNIQUE、`group`、`pinned`、`sort_order`

---

### recent_repo（最近仓库）

> 服务 BR：BR-05（倒序与淘汰）

| 列 | 类型 | 约束 | 说明 |
| --- | ------ | ------ | ----- |
| `id` | TEXT | PK | 主键 |
| `repo_path` | TEXT | NOT NULL, UNIQUE | 仓库绝对路径 |
| `last_branch` | TEXT | DEFAULT '' | 上次打开的分支 |
| `last_opened_at` | TEXT | NOT NULL | 最后打开时间 |
| `open_count` | INTEGER | NOT NULL DEFAULT 1 | 累计打开次数 |
| `created_at` | TEXT | NOT NULL | 创建时间 |
| `updated_at` | TEXT | NOT NULL | 更新时间 |

**索引**：`repo_path` UNIQUE、`last_opened_at DESC`

---

### repository_meta（仓库元信息缓存）

> 服务 BR：BR-01（检索缓存）、BR-02（异步刷新）

| 列 | 类型 | 约束 | 说明 |
| --- | ------ | ------ | ----- |
| `id` | TEXT | PK | 主键 |
| `repo_path` | TEXT | NOT NULL, UNIQUE | 仓库绝对路径 |
| `current_branch` | TEXT | DEFAULT '' | 当前分支 |
| `head_commit` | TEXT | DEFAULT '' | HEAD 提交哈希 |
| `remote_url` | TEXT | DEFAULT '' | 主远程 URL |
| `has_uncommitted_changes` | INTEGER | NOT NULL DEFAULT 0 | 是否有未提交修改 |
| `last_synced_at` | TEXT | NOT NULL | 元信息最后刷新时间 |
| `created_at` | TEXT | NOT NULL | 创建时间 |
| `updated_at` | TEXT | NOT NULL | 更新时间 |

**索引**：`repo_path` UNIQUE、`last_synced_at`

---

### operation_log（操作日志）

> 服务 BR：BR-35（任务结果记录）

| 列 | 类型 | 约束 | 说明 |
| --- | ------ | ------ | ----- |
| `id` | TEXT | PK | 主键 |
| `repo_path` | TEXT | NOT NULL | 目标仓库路径 |
| `operation` | TEXT | NOT NULL | 操作类型枚举 |
| `command` | TEXT | DEFAULT '' | 实际执行的 git 命令描述 |
| `args` | TEXT | DEFAULT '' | 参数 JSON |
| `success` | INTEGER | NOT NULL DEFAULT 1 | 是否成功 |
| `duration_ms` | INTEGER | DEFAULT 0 | 耗时（毫秒） |
| `error_message` | TEXT | DEFAULT '' | 错误信息（中文友好提示） |
| `task_id` | TEXT | DEFAULT '' | 关联异步任务 ID |
| `created_at` | TEXT | NOT NULL | 创建时间 |

**索引**：`repo_path`、`operation`、`created_at`、`success`

---

### audit_log（红线审计日志）

> 服务 BR：BR-31（红线审计追加写入不可删除）

| 列 | 类型 | 约束 | 说明 |
| --- | ------ | ------ | ----- |
| `id` | TEXT | PK | 主键 |
| `rule_code` | TEXT | NOT NULL | 红线规则代码 |
| `command` | TEXT | DEFAULT '' | 命中红线的命令 |
| `repo_path` | TEXT | NOT NULL | 仓库路径 |
| `branch` | TEXT | DEFAULT '' | 涉及分支 |
| `remote_url` | TEXT | DEFAULT '' | 涉及远程 URL |
| `action` | TEXT | NOT NULL | 红线类型（BLOCK/CONFIRM） |
| `action_result` | TEXT | NOT NULL | 处理结果（BLOCKED/CONFIRMED/CANCELLED） |
| `detail` | TEXT | DEFAULT '' | 详情 JSON |
| `created_at` | TEXT | NOT NULL | 创建时间 |

**索引**：`rule_code`、`repo_path`、`created_at`、`action_result`

---

### task_record（异步任务记录）

> 服务 BR：BR-33（异步可取消）、BR-34（同仓库写串行）、BR-35（结果记录）、BR-36（后台执行）

| 列 | 类型 | 约束 | 说明 |
| --- | ------ | ------ | ----- |
| `id` | TEXT | PK | 主键 |
| `task_type` | TEXT | NOT NULL | 任务类型（CLONE/PULL/PUSH/FETCH/MERGE/REBASE/MULTI_REPO_SCAN/GC/CHECKOUT/STASH） |
| `repo_path` | TEXT | DEFAULT '' | 仓库路径（多仓库检索可为空） |
| `status` | TEXT | NOT NULL DEFAULT 'PENDING' | 状态（PENDING/RUNNING/SUCCESS/FAILED/CANCELLED） |
| `progress` | INTEGER | NOT NULL DEFAULT 0 | 进度 0-100 |
| `message` | TEXT | DEFAULT '' | 进度描述 |
| `output` | TEXT | DEFAULT '' | 命令输出/错误堆栈 |
| `cancellable` | INTEGER | NOT NULL DEFAULT 1 | 是否可取消 |
| `started_at` | TEXT | DEFAULT NULL | 开始时间 |
| `finished_at` | TEXT | DEFAULT NULL | 结束时间 |
| `created_at` | TEXT | NOT NULL | 创建时间 |

**索引**：`repo_path`、`status`、`task_type`、`created_at`

> `task_record` 无外键到 `recent_repo`（仓库路径可能为空或临时仓库），通过 `repo_path` 字符串软关联。

---

## 迁移历史

| V 号 | 文件 | 描述 |
| ----- | ------ | ----- |
| V1 | `V1__init_app_settings.sql` | app_settings 表 + 内置默认设置数据（保护分支清单、远程白名单空、敏感文件规则默认集、主题/语言默认值） |
| V2 | `V2__init_favorite.sql` | favorite 表 |
| V3 | `V3__init_recent_repo.sql` | recent_repo 表 |
| V4 | `V4__init_repository_meta.sql` | repository_meta 表 |
| V5 | `V5__init_operation_log.sql` | operation_log 表 |
| V6 | `V6__init_audit_log.sql` | audit_log 表 |
| V7 | `V7__init_task_record.sql` | task_record 表 |

> V1 内置敏感文件默认规则集（`.env`/`credentials`/`*.pem`/`*.key`/`id_rsa`/`.npmrc`）与默认保护分支（`main`/`master`/`develop`），以 INSERT 语句写入。
