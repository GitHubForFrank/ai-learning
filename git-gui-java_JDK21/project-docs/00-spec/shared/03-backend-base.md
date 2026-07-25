# 后端实现模式 — 基座

> 本文件为共享基座。DDD 分层、包结构、Model/Repository/Service 的通用规范。
> 项目专属的 JGit/CLI 适配器、命令红线拦截器、异步任务体系、Guice 绑定等见项目 `project-docs/00-spec/project/03-backend.md`。

---

## DDD 分层架构

桌面应用为单 `app-backend/` 模块，UI 与后端同模块但严格分层：

```plaintext
┌──────────────────────────────────────────────────┐
│  UI 层   ui/controller/  +  ui/fxml/  +  ui/view/│  JavaFX Controller + FXML + ViewModel
└───────────────────┬──────────────────────────────┘
                    ↓ Guice 注入服务接口
┌──────────────────────────────────────────────────┐
│  应用层  application/service/                    │  用例编排、事务边界、调用红线拦截器
│          application/async/                      │  TaskManager / ProgressCallback
└───────────────────┬──────────────────────────────┘
                    ↓ Domain Model（XxxModel）
┌──────────────────────────────────────────────────┐
│  领域层  domain/model/  +  domain/repository/    │  纯 POJO + 仓储接口
│          domain/service/                         │  服务接口契约（UI 注入此包）
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

## 包结构

```plaintext
{base-package}/
  GitGuiApp.java                   # JavaFX Application 入口 + Guice Injector
  core/
    config/                        # AppConfig、路径常量
    constant/                      # 全局常量、枚举（OperationType、TaskStatus、RedLineCode）
    exception/                     # GitGuiException + ErrorCode + GlobalExceptionHandler
    redline/                       # CommandInterceptor、RedLineRule、规则实现集
    async/                         # TaskManager、TaskHandle、ProgressCallback
    util/                          # PathUtil、GitEncodingUtil
  ui/
    main/                          # MainController/MainView/MainViewModel
    dialog/                        # CommitDialog、PullDialog、PushDialog... 每操作一对 Controller+FXML
    common/                        # 公共组件（FileContextMenu、StatusBar、ProgressDialog）
    theme/                         # light.css、dark.css、ThemeManager
    i18n/                          # messages_zh/en.properties、I18nUtil
  application/
    service/                       # 应用服务实现（编排领域服务 + 拦截器 + 异步）
  domain/
    model/                         # 领域模型（纯 POJO + Lombok）
    repository/                    # 仓储接口
    service/                       # 服务接口契约（UI 注入此包）
    redline/                       # RedLineContext、RedLineResult、RedLineRule 接口
  infrastructure/
    jgit/                          # JGit 适配器（主）
    cli/                           # CLI 兜底适配器、GitProcessBuilder
    persistence/
      entity/                      # SQLite Entity
      mapper/                      # MyBatis-Plus Mapper 接口（extends BaseMapper）
      repository/                  # 仓储实现 + LambdaQueryWrapper
      mybatis/                     # MyBatis-Plus 配置（SqlSessionFactory、TypeHandler）
      flyway/                      # Flyway 迁移配置
    credential/                    # SystemCredentialHelper 适配
```

> `core/` 下的子包（如 `redline/`、`async/`）按项目需要添加。

---

## Domain Model 规范

- 命名：`XxxModel`
- 纯 POJO，使用 Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor`
- 无 Guice / SQLite / JGit 注解
- 字段对应服务契约中的返回结构
- 可选字段使用装箱类型（`Integer`、`Boolean`）

---

## Repository 规范

- **接口** 放在 `domain/repository/`，返回 `XxxModel`
- **实现** 放在 `infrastructure/persistence/repository/`
- 实现内部注入 sqlite-jdbc Mapper，自行做 `Entity ↔ Model` 转换
- 转换逻辑放在 `ModelConverter` 内部类或独立转换器
- 接口按聚合根命名

---

## UI Controller 规范

- UI 层（JavaFX Controller）是**薄层**，仅负责事件绑定与视图状态
- Controller 通过 Guice `@Inject` 注入 `domain/service/` 中的服务接口，**禁止**直接访问 `infrastructure/` 层
- 业务规则在 Service 层实现，Controller 不写业务逻辑
- 长耗时操作必须经异步任务体系（参见 [03-backend.md §异步任务体系]），**禁止**在 JavaFX Application Thread 执行 Git 操作

---

## Service 规范

- 异常：统一抛出 `GitGuiException`（含 errorCode 枚举 + 中文 message），由 `GlobalExceptionHandler` 转 UI 友好提示
- 写操作执行前由 `CommandInterceptor` 自动调用命令红线校验（参见 [03-backend.md §命令红线拦截器]）
- 配置变更后需考虑缓存刷新机制

---

## 适配器规范 — Git 操作

- 接口 `GitOperationExecutor` 定义所有 Git 操作方法
- `JGitOperationExecutor`（主）：纯 Java、进度可控、无进程开销
- `CliGitExecutor`（兜底）：JGit 不支持或支持不足的场景回退（LFS / Hook / 复杂 Rebase / Worktree / Submodule / filter-repo）
- `GitExecutorRouter`：按操作类型路由，命中 CLI 兜底时 `log.info` 记录降级
- CLI 调用强制 `UTF-8` 编码并设置 `core.quotepath=false`

> 具体适配器实现见 [03-backend.md §JGit + CLI 兜底适配器模式]。

---

## 框架兼容性说明

| 关注点 | 说明 | 对策 |
| --- | ----- | ----- |
| JavaFX 线程模型 | Git 操作不能阻塞 JavaFX Application Thread | 所有 Git 操作走异步任务体系，通过 `Platform.runLater` 回 UI |
| SQLite 并发 | SQLite 单写多读，桌面应用单进程 | 启用 WAL 模式；写操作串行（参见异步任务队列） |
| JGit 与 CLI 一致性 | 兜底 CLI 的输出与 JGit 行为需对齐 | 统一 UTF-8 + quotepath，结果归一化为领域 Model |

> IoC 容器选型由各项目自行决定（本项目使用 Google Guice）。
