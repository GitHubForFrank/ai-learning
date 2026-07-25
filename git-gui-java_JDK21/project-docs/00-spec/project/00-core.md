# Core Rules — git-gui 项目专属

> **基座**：继承 [shared/00-core-base.md](../shared/00-core-base.md) 全部规则。
> 本文档仅包含 git-gui 项目专属的应用配置和技术选型约束。
> 规则冲突时，本文档优先级高于基座。

---

## 项目配置说明

| 配置项 | 默认值 | 说明 |
| --------- | -------- | ------ |
| `app.data-dir` | `~/.git-gui` | 用户数据目录（DB、日志、锁、配置根目录） |
| `app.db-path` | `~/.git-gui/db/git-gui.db` | SQLite 数据库文件路径 |
| `app.log-path` | `~/.git-gui/logs` | 日志目录 |
| `app.lock-file` | `~/.git-gui/.git-gui.lock` | 单实例锁文件路径 |
| `app.git-executable` | `""`（从 PATH 检测） | 本地 Git 可执行文件路径 |
| `app.repo-scan-default-depth` | `3` | 多仓库自动检索默认深度 |
| `app.recent-repo-max-keep` | `20` | 最近仓库列表最大保留条数 |
| `security.red-line-enabled` | `true` | 命令红线总开关 |
| `security.large-file-threshold-mb` | `50` | 推送超大文件阈值（MB） |

> 集中参数定义见 [project.yml](./project.yml)。桌面应用无 Profile / 端口 / Docker 概念。

---

## 项目技术选型禁止

- ❌ Spring（Boot / Security / MVC）、JWT/BCrypt、MySQL/PostgreSQL、Testcontainers、Docker、Tauri/Electron、WebView、`@SneakyThrows`

> 以上为本项目特有的技术选型约束。IoC 统一使用 Google Guice；数据库统一使用 SQLite + Flyway；ORM 统一使用 MyBatis-Plus；GUI 统一使用原生 JavaFX。

---

## 技术选型正面清单

| 层级 | 选型 | 说明 |
| ---- | ---- | ---- |
| 语言 | Java 21 | JDK 21 |
| IoC | Google Guice | 轻量级依赖注入，Module 绑定，非 Spring |
| Git 操作 | JGit（主）+ 本地 Git CLI（兜底） | JGit 纯 Java、进度可控；LFS/Hook/复杂 Rebase 回退 CLI |
| 数据库 | SQLite + sqlite-jdbc + Flyway + MyBatis-Plus | 本地存储，Flyway 管理迁移，MyBatis-Plus 提供通用 CRUD |
| GUI | JavaFX 21 | 原生桌面窗口，无 WebView |
| 构建 | Maven | 打包 fat jar + bat/sh 启动脚本 |
