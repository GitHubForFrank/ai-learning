# app-backend — JavaFX 应用主模块

> git-gui 桌面应用的单模块工程，承载 UI 层、应用层、领域层与基础设施层的全部代码。

---

## 技术概况

| 项目 | 选型 |
| ---- | ---- |
| 语言 | Java 21 |
| GUI 框架 | JavaFX 21（FXML + CSS + Controller） |
| IoC 容器 | Google Guice（Module 绑定） |
| Git 操作 | 系统 Git CLI（ProcessBuilder） |
| 数据库 | SQLite（通过 sqlite-jdbc 访问，MyBatis-Plus ORM） |
| 构建 | Maven（打包 fat jar + zip 分发包 + bat/sh 启动脚本） |
| 测试 | JUnit5 + Mockito（单元）/ TestFX + Monocle（UI，headless） |

---

## 包结构

```plaintext
com.gitgui/
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
    dialog/                        # 各操作对话框（CommitDialog、PullDialog 等）
    common/                        # 公共组件（FileContextMenu、StatusBar、ProgressDialog）
    theme/                         # light.css、dark.css、ThemeManager
    i18n/                          # messages_zh/en.properties、I18nUtil
  application/
    service/                       # 应用服务实现（编排领域服务 + 拦截器 + 异步）
  domain/
    model/                         # RepositoryMeta、Favorite、AuditLog...（纯 POJO + Lombok）
    repository/                    # 仓储接口
    service/                       # 服务接口契约（UI 注入此包）
    redline/                       # RedLineContext、RedLineResult、RedLineRule 接口
  infrastructure/
    cli/                           # CliGitExecutor（Git CLI 适配器）、GitProcessBuilder、GitOutputParser
    persistence/
      entity/                      # SQLite Entity（MyBatis-Plus 领域模型直接映射）
      mapper/                      # MyBatis-Plus Mapper 接口（extends BaseMapper）
      repository/                  # 仓储实现（注入 Mapper，使用 LambdaQueryWrapper）
      flyway/                      # Flyway 迁移配置
    credential/                    # SystemCredentialHelper 适配
```

---

## 构建与启动

```bash
# 编译
mvn compile

# 运行测试
mvn test

# 打包（生成 fat jar：target/git-gui-1.0.0.jar）
mvn package

# 启动应用（开发期）
mvn javafx:run
```

### 产物与启动脚本

- **Fat jar**：`mvn package` 生成 `target/git-gui-{version}.jar`（shade fat jar，含所有依赖）。
- **zip 分发包**：`mvn package` 同时生成 `target/git-gui-{version}-bin.zip`，包含 fat jar + 启动脚本。
- 启动脚本位于 `scripts/` 目录：
  - `scripts/git-gui.bat`（Windows）
  - `scripts/git-gui.sh`（Mac / Linux）

### Flyway 迁移脚本

迁移脚本 V1~V7 位于 `src/main/resources/db/migration/`，由 `SqliteDataSource` 在应用启动时通过 classpath `db/migration` 自动加载执行。

### 运行时日志

日志通过 Logback 配置（`src/main/resources/logback.xml`），统一写入用户主目录下的 `~/.git-gui/logs/` 目录（路径常量见 `AppConfig.logDir()`）：

| 文件 | 说明 | 滚动策略 |
| ---- | ---- | ---- |
| `git-gui.log` | 主日志（全部级别） | 单文件 10MB，保留 30 天，总上限 500MB |
| `git-gui-error.log` | 错误日志（仅 ERROR 级别） | 单文件 10MB，保留 90 天，总上限 200MB |
| `git-gui.%d{yyyy-MM-dd}.%i.log.gz` | 归档日志（按日期 + 序号压缩） | 由 RollingFileAppender 自动产生 |

- **开发期**：同时输出到控制台（STDOUT appender）；**运行期**：通过 `javaw`（Windows）/ `java`（Mac/Linux）启动后仅写入文件。
- **日志级别**：`com.gitgui` 包为 `DEBUG`，基础设施层（`infrastructure.*`）为 `INFO`，第三方库（Guice / SQLite）为 `WARN`，Flyway 为 `INFO`。
- **编码**：所有日志文件统一 UTF-8（遵循 BR-42 编码约束）。
- **排查入口**：若 `git-gui.bat` / `git-gui.sh` 启动后窗口未弹出，优先查看 `~/.git-gui/logs/git-gui-error.log` 定位根因。

---

## 规约引用

- 后端实现规范：[project-docs/00-spec/project/03-backend.md](../project-docs/00-spec/project/03-backend.md)
- 服务契约：[project-docs/00-spec/project/02-api.md](../project-docs/00-spec/project/02-api.md)
- 数据库规范：[project-docs/00-spec/project/04-database.md](../project-docs/00-spec/project/04-database.md)
- UI 开发规范：[project-docs/00-spec/project/05-frontend.md](../project-docs/00-spec/project/05-frontend.md)
- 运维与依赖：[project-docs/00-spec/project/07-ops.md](../project-docs/00-spec/project/07-ops.md)