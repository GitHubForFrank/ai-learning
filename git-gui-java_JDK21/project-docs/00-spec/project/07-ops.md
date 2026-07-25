# 依赖与运维策略 — git-gui 项目专属

> **基座**：继承 [shared/07-ops-base.md](../shared/07-ops-base.md) — 桌面应用构建流程、打包、单实例、用户数据目录、CI/CD 通用规范。
> 本文档为 git-gui 项目专属的依赖选型、配置项、资源限制。

---

## 依赖版本管理

### 应用依赖

> 具体版本号由项目 `app-backend/pom.xml` 管理。

| 依赖 | 说明 |
| ----- | ------ |
| Java | JDK 21（LTS） |
| JavaFX | GUI 框架（21） |
| JGit | Git 操作主适配器（纯 Java） |
| Google Guice | 轻量 IoC 容器，Module 绑定 |
| sqlite-jdbc | SQLite JDBC 驱动 |
| Flyway Core | 数据库迁移管理 |
| SLF4J API + Logback | 日志门面 + 实现 |
| Jackson | JSON 序列化（设置/审计详情） |
| Lombok | 代码简化（Model/Entity） |
| ControlsFX（可选） | JavaFX 增强组件 |
| MyBatis-Plus | ORM 框架（BaseMapper 通用 CRUD） |
| Hutool（可选） | 通用工具库 |

> 无前端依赖（桌面应用无独立前端模块）。ORM 使用 MyBatis-Plus，通过 LambdaQueryWrapper 构建查询。

---

## 核心配置项

| 配置项 | 说明 | 默认值 |
| ------- | ------ | ------- |
| `app.data-dir` | 用户数据目录 | `~/.git-gui` |
| `app.db-path` | SQLite 数据库路径 | `~/.git-gui/db/git-gui.db` |
| `app.log-path` | 日志目录 | `~/.git-gui/logs` |
| `app.lock-file` | 单实例锁文件路径 | `~/.git-gui/.git-gui.lock` |
| `app.git-executable` | 本地 Git 可执行文件路径（空则从 PATH 检测） | `""` |
| `app.repo-scan-default-depth` | 多仓库检索默认深度 | `3` |
| `app.recent-repo-max-keep` | 最近仓库最大保留条数 | `20` |
| `git.encoding` | Git 命令行编码 | `UTF-8` |
| `git.core-quotepath` | 中文路径防乱码 | `false` |
| `security.red-line-enabled` | 命令红线总开关 | `true` |
| `security.large-file-threshold-mb` | 推送超大文件阈值（MB） | `50` |
| `logging.level.root` | 根日志级别 | `INFO`（生产）/ `DEBUG`（开发） |
| `logging.file.path` | 日志文件路径 | `~/.git-gui/logs` |

> 集中参数定义见 [project.yml](./project.yml)；`logging.level.root` 与 `logging.file.path` 由 `logback.xml` 管理，不纳入 project.yml。

---

## 资源限制

| 资源 | 建议值 | 说明 |
| ----- | ------- | ----- |
| CPU | 2 核 | Git 操作 + UI 渲染 |
| 内存 | 256~512MB | JavaFX + JGit + SQLite |
| 磁盘 | 按仓库规模 | 用户数据 + 日志 + 数据库 |

---

## 启动与退出

- 启动脚本：`app-backend/scripts/git-gui.bat`（Windows）/ `app-backend/scripts/git-gui.sh`（Mac/Linux），通过相对路径 `../target/git-gui-{version}.jar` 拉起 fat jar
- 启动顺序：锁文件检测（BR-40）→ Guice Injector → Flyway 迁移 → Git 检测（BR-41）→ 加载设置 → 显示主窗口
- 主窗口关闭时关闭 Guice 注入器并释放资源、释放锁文件
- 单实例：二次启动聚焦已有窗口而非启动新进程（BR-40）
