# 后端实现模式 — git-gui 项目专属

> **基座**：继承 [shared/03-backend-base.md](../shared/03-backend-base.md) — DDD 分层、包结构、Model/Repository/Service 规范、适配器规范、框架兼容性。
> 本文档为 git-gui 项目专属的 JGit/CLI 适配器、命令红线拦截器、异步任务体系、Guice 绑定结构。

---

## DDD 分层在桌面端落地

```plaintext
┌──────────────────────────────────────────────────┐
│  UI 层   ui/controller/  +  ui/fxml/  +  ui/view/│  JavaFX Controller + FXML + ViewModel
└───────────────────┬──────────────────────────────┘
                    ↓ 通过 Guice 注入服务接口
┌──────────────────────────────────────────────────┐
│  应用层  application/service/                    │  用例编排、事务边界、调用红线拦截器
│          application/async/                      │  TaskManager / ProgressCallback
└───────────────────┬──────────────────────────────┘
                    ↓ Domain Model（XxxModel）
┌──────────────────────────────────────────────────┐
│  领域层  domain/model/  +  domain/repository/    │  纯 POJO + 仓储接口
│          domain/service/                         │  服务接口契约（参见 02-api.md）
│          domain/redline/                         │  红线规则领域
└───────────────────┬──────────────────────────────┘
                    ↓ Guice 绑定（接口 → 实现）
┌──────────────────────────────────────────────────┐
│  基础设施层  infrastructure/jgit/                │  JGit 适配器（主）
│              infrastructure/cli/                 │  Git CLI 兜底适配器
│              infrastructure/persistence/         │  SQLite 仓储实现 + Flyway
│              infrastructure/credential/          │  系统 credential helper
└───────────────────┬──────────────────────────────┘
                    ↓ JGit API / 进程 / SQL
                  {Git 仓库} / {SQLite}
```

---

## 包结构（单 `app-backend/` 模块）

```plaintext
com.gitgui/
  GitGuiApp.java                   # JavaFX Application 入口 + Guice Injector
  core/
    config/                        # AppConfig、路径常量
    constant/                      # 全局常量、枚举（OperationType、TaskStatus、RedLineCode）
    exception/                     # GitGuiException + ErrorCode + GlobalExceptionHandler
    redline/                       # CommandInterceptor、RedLineRule、规则实现集
    async/                         # TaskManager、TaskHandle、ProgressCallback
    util/                          # PathUtil、GitEncodingUtil（BR-42）
  ui/
    main/                          # MainController/MainView/MainViewModel
    dialog/                        # CommitDialog、PullDialog、PushDialog... 每操作一对 Controller+FXML
    common/                        # 公共组件（FileContextMenu、StatusBar、ProgressDialog）
    theme/                         # light.css、dark.css、ThemeManager
    i18n/                          # messages_zh/en.properties、I18nUtil
  application/
    service/                       # 应用服务实现（编排领域服务 + 拦截器 + 异步）
  domain/
    model/                         # RepositoryMeta、Favorite、AuditLog...（纯 POJO + Lombok）
    repository/                    # 仓储接口
    service/                       # 服务接口契约（参见 02-api.md，UI 注入此包）
    redline/                       # RedLineContext、RedLineResult、RedLineRule 接口
  infrastructure/
    jgit/                          # JGitRepository、JGitOperationExecutor（主）
    cli/                           # CliGitExecutor（兜底）、GitProcessBuilder（UTF-8 + quotepath）
    persistence/
      entity/                      # SQLite Entity（MyBatis-Plus 领域模型直接映射）
      mapper/                      # MyBatis-Plus Mapper 接口（extends BaseMapper）
      repository/                  # 仓储实现（注入 Mapper，使用 LambdaQueryWrapper）
      mybatis/                     # MyBatis-Plus 配置（SqlSessionFactory、TypeHandler）
      flyway/                      # Flyway 迁移配置
    credential/                    # SystemCredentialHelper 适配
```

---

## JGit + CLI 兜底适配器模式

### 适配器接口

```java
public interface GitOperationExecutor {
    // 所有 Git 操作方法（commit/push/pull/fetch/merge/rebase/...）
}
```

### 已有适配器

| 适配器 | 职责 |
| -------- | ------ |
| `JGitOperationExecutor` | JGit 纯 Java 实现（主），进度可控、无进程开销 |
| `CliGitExecutor` | Git 命令行兜底，JGit 不支持或支持不足的场景回退 |
| `GitExecutorRouter` | 按操作类型路由（白名单决定走 JGit 还是 CLI） |

### CLI 兜底场景

LFS / Hook / 复杂交互式 Rebase / Worktree / Submodule / filter-branch / filter-repo 等场景回退 CLI。

### 直通优先策略

- 能用 JGit 完成的操作优先 JGit
- JGit 不支持或异常时降级为 CLI
- 降级切换过程 `log.info` 记录（预期内降级用 `log.info`，严重错误用 `log.error`）

### 编码约束

`CliGitExecutor` 内部 `GitProcessBuilder` 强制 `UTF-8` 编码并设置 `core.quotepath=false`（BR-42），避免 Windows 中文路径乱码。

---

## 命令红线拦截器（闭环核心）

### 拦截流程

`GitOperationService` 所有写方法执行前由 `CommandInterceptor` 调用 `CommandRedLineService.check(ctx)`，拦截结果三态：

- `PASS` → 放行执行
- `BLOCK` → 抛 `RedLineBlockedException`，UI 提示安全等价命令（如「请改用 `--force-with-lease`」），记录 `audit_log`（actionResult=BLOCKED）
- `CONFIRM` → UI 弹窗（含风险详情），用户确认后回调执行并记录 `audit_log`（actionResult=CONFIRMED），取消则记录 CANCELLED

### 红线规则实现集

`RedLineRule` 接口实现类（Guice `Multibindings` 收集）：

| 规则类 | 命中场景 | 类型 |
| -------- | ------ | ---- |
| `ForcePushRule` | 裸 `--force` push | BLOCK |
| `ProtectedBranchRule` | 向保护分支 force push | BLOCK |
| `DeleteProtectedBranchRule` | push 删除保护分支 | BLOCK |
| `SensitiveFileRule` | 推送含敏感信息文件 | BLOCK |
| `RemoteWhitelistRule` | 推送到非授权远程 | BLOCK |
| `NoVerifyRule` | `--no-verify` 跳过 hook | BLOCK |
| `ResetHardRule` | `reset --hard` | CONFIRM |
| `CleanFdxRule` | `clean -fdx` | CONFIRM |
| `AmendPushedRule` | amend 已推送提交 | CONFIRM |
| `RebasePushedRule` | rebase 已推送提交 | CONFIRM |
| `FilterBranchRule` | filter-branch / filter-repo | CONFIRM |
| `LargeFileRule` | 推送超大文件（> 阈值，非 LFS） | CONFIRM |

### 规则配置来源

规则从 `SettingsService` 读取配置：保护分支清单（BR-27）、远程白名单（BR-28）、敏感文件规则（BR-32）、超大文件阈值。

---

## 异步任务体系

### TaskManager

- 单例，维护仓库级写任务队列（`Map<repoPath, Queue>`）+ 全局读任务并发池
- 同一仓库写操作串行（BR-34），读操作可并发

### TaskHandle

- 包装 JavaFX `Task<T>`，提供 `cancel()`、`onProgress`、`onSuccess`、`onFailure`

### ProgressCallback

- 适配 JGit `ProgressMonitor`（`update(completed)` / `isCancelled()`）与 CLI 输出解析
- 实时反馈进度百分比与命令输出

### 任务持久化

- 任务状态持久化到 `task_record` 表（BR-35），重启后可恢复未完成任务状态展示
- 任务完成通过 `EventBus` 发 `TaskFinishedEvent`，UI 收到后用 `Platform.runLater()` 刷新

### 取消机制

- JGit 通过 `ProgressMonitor.cancel`
- CLI 通过终止进程

---

## Guice Module 绑定结构

| Module | 绑定内容 |
| ------ | ------ |
| `AppModule` | 核心常量、路径、异常处理器 |
| `DatabaseModule` | DataSource、Flyway、各 Repository 接口 → 实现 |
| `GitModule` | `GitOperationExecutor` → `JGitOperationExecutor`（主）、`CliGitExecutor`、`GitExecutorRouter` |
| `ServiceModule` | `domain/service/*` 接口 → `application/service/*` 实现 |
| `RedLineModule` | `CommandRedLineService` + `Multibindings<RedLineRule>` 收集所有规则 |
| `AsyncModule` | `TaskManager` 单例 |

### 启动顺序

```plaintext
锁文件检测（BR-40）
  ↓
Guice Injector 创建
  ↓
Flyway 迁移（V1~V7）
  ↓
Git 可执行文件检测（BR-41）
  ↓
加载默认设置（app_settings）
  ↓
显示主窗口
```

---

## 安全约束（后端专属）

- 路径安全：所有仓库路径校验为绝对路径且存在 `.git`，拒绝 `..` 穿越
- 凭证安全：通过系统 credential helper，不明文存储密码（BR-39）
- 命令注入防护：CLI 参数使用 `ProcessBuilder` 参数数组，**禁止**字符串拼接 shell
- 红线审计：所有命中红线操作必入 `audit_log`（BR-31）
- Secrets 不写入日志原文

---

## 核心类职责

| 类 | 职责 |
| --- | ----- |
| `GitGuiApp` | JavaFX Application 入口，创建 Guice Injector，启动流程编排 |
| `CommandInterceptor` | 写操作前置拦截，调 `CommandRedLineService.check`，处理 PASS/BLOCK/CONFIRM |
| `TaskManager` | 异步任务队列调度，同仓库写串行，读并发 |
| `JGitOperationExecutor` | JGit 适配器主实现，纯 Java Git 操作 |
| `CliGitExecutor` | CLI 兜底适配器，强制 UTF-8 + quotepath |
| `GitExecutorRouter` | 按操作类型路由 JGit / CLI |
| `CommandRedLineService` | 红线规则校验 + 审计日志持久化 |
| `FavoriteService` | 收藏 CRUD（BR-03/BR-04） |
| `RecentRepoService` | 最近仓库 upsert + 淘汰（BR-05） |
| `SettingsService` | 应用设置读写、红线配置读取（BR-27/BR-28/BR-32） |
| `AsyncTaskService` | 异步任务提交、取消、查询（BR-33~BR-36） |
| `OperationLogService` | 操作日志记录与查询（BR-35） |
| `ConflictResolveService` | 冲突解决、调用外部 Merge 工具 |
| `RemoteConfigService` | Remote 配置 CRUD（BR-09） |

---

## 变更记录

| 版本 | 日期 | 变更 |
| ----- | ------ | ----- |
| `v1` | 2026-07-23 | 初版，定义桌面 DDD 分层 + JGit/CLI 适配器 + 命令红线拦截器 + 异步任务体系 + Guice 绑定 |
