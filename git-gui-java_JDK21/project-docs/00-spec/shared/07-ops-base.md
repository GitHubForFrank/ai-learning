# 依赖与运维策略（基座）— 桌面应用

> 本文件为共享基座。桌面应用的构建流程、打包、单实例、配置管理的通用规范。
> 项目专属的依赖选型、配置项、资源限制见项目 `project-docs/00-spec/project/07-ops.md` 和 `project.yml`。

---

## 版本升级策略

- **JDK / JavaFX**：跟随官方 LTS 版本，定期评估
- **JGit**：必须验证与当前 JDK 版本的兼容性
- **Guice / Flyway / sqlite-jdbc**：小版本（patch）自动升级，大版本（major）需手动评估
- **禁止**：在代码中硬编码依赖版本（统一在 `pom.xml` 中管理）

---

## 构建流程

### 应用构建

```bash
mvn clean package          # 编译 + 测试 + 打包（fat jar）
mvn clean package -DskipTests  # ⚠️ 仅用于紧急场景，严禁日常使用
```

### 启动脚本

启动脚本统一放置在 `app-backend/scripts/` 目录，通过相对路径 `../target/git-gui-{version}.jar` 拉起 fat jar：

- `git-gui.bat`（Windows）
- `git-gui.sh`（Mac / Linux）

脚本职责：拉起 jar，主窗口关闭时退出进程并释放资源。

### 可选原生打包

使用 `jpackage` 生成平台原生安装包：

- Windows：`.exe` / `.msi`
- macOS：`.dmg` / `.pkg`
- Linux：`.deb` / `.rpm`

### 构建产物

```plaintext
app-backend/
  target/
    git-gui-{version}.jar     # 可执行 fat jar
  scripts/
    git-gui.bat               # Windows 启动脚本
    git-gui.sh                # Mac/Linux 启动脚本
```

---

## 单实例运行

### 锁文件机制

- 用户数据目录下创建锁文件（如 `~/.git-gui/.git-gui.lock`）
- 启动时检测锁文件：已存在则聚焦已有窗口（而非启动新进程）
- 应用正常退出时释放锁文件；异常退出残留的锁文件需在下次启动时校验进程存活后清理

> **严禁**占用网络端口做单实例检测（桌面应用无需网络端口）。

---

## 用户数据目录

桌面应用的数据持久化在用户目录，而非应用安装目录：

```plaintext
~/.git-gui/
  db/
    git-gui.db              # SQLite 数据库文件
  logs/                     # 日志目录
  config/                   # 用户配置（覆盖默认）
  .git-gui.lock             # 单实例锁文件
```

- 首次启动从 jar 释放默认配置与 Flyway 迁移脚本到用户数据目录
- 数据库文件随用户数据保留，卸载应用不删除用户数据

---

## 配置管理

### 配置文件层次

```plaintext
app-backend/src/main/resources/
  application.yml           # 主配置（默认值）
```

### 用户配置覆盖

用户数据目录的 `config/` 下可放置覆盖配置，优先级高于 jar 内默认值。

> 具体配置项清单和默认值见各项目 `project.yml`。

---

## 运维约定

### 日志管理

- 日志输出到控制台和 `~/.git-gui/logs/` 目录
- 日志轮转：按天切割，保留 30 天
- 日志级别：生产环境 `INFO`，开发环境 `DEBUG`

### 数据库管理

- 数据库文件位于用户数据目录，随用户数据保留
- 迁移脚本由 Flyway 在应用启动时按 V 号顺序自动执行
- **不可**手动修改数据库结构（必须通过新增迁移脚本）

---

## CI/CD 流程

### 流水线阶段

```plaintext
1. 代码拉取
2. Maven 编译 + 测试
3. 打包 fat jar + 启动脚本
4. 多平台产物构建（CI 矩阵：Windows / macOS / Linux）
5. 可选 jpackage 生成原生安装包
6. 产物归档到 Release
```

### 触发条件

- `main` 分支推送 → 自动构建测试
- `feature/*` 分支推送 → 仅构建测试
- Tag 推送 → 构建发布版本产物并归档

### 多平台考量

CI 矩阵在 Windows / macOS / Linux 三平台构建，JavaFX 平台依赖通过 classifier 区分（`javafx-graphics` 的 `win` / `mac` / `linux`）。

---

## 禁用清单

- ❌ 在代码中硬编码配置值（使用配置文件 / 用户数据目录）
- ❌ 提交 `target/` 到版本控制
- ❌ 在 jar 中打包源码
- ❌ 使用 `latest` 标签发布正式版本
- ❌ 手动修改数据库结构（必须通过 Flyway 迁移脚本按顺序执行）
- ❌ 占用网络端口做单实例检测
- ❌ Docker 容器化部署（桌面应用无需容器化）
