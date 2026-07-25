# UI 编码规范 — git-gui 项目专属

> **基座**：继承 [shared/05-frontend-base.md](../shared/05-frontend-base.md) — JavaFX UI 层通用规范（MVVM 分离、命名约定、CSS 主题、国际化、事件驱动、异步任务 UI）。
> 本文档为 git-gui 项目专属的技术栈、主窗口架构、对话框清单、主题与快捷键。

---

## 技术栈

| 项目 | 选型 | 版本 |
| ----- | ------ | ----- |
| GUI 框架 | JavaFX | 21 |
| 增强组件（可选） | ControlsFX | 由 `pom.xml` 锁定 |
| 日志 | SLF4J + Logback | 由 `pom.xml` 锁定 |
| IoC | Google Guice | 由 `pom.xml` 锁定 |
| 构建 | Maven | 由 `pom.xml` 锁定 |
| 测试 | TestFX + JUnit 5 | 由 `pom.xml` 锁定 |

---

## 主窗口架构

对齐 PRD 第五章 UI 架构，主窗口 `MainView.fxml` 结构：

```plaintext
┌──────────────────────────────────────────────────────┐
│  MenuBar（文件 / 操作 / 视图 / 帮助）                  │
├──────────────┬───────────────────────────────────────┤
│              │  中间内容区（多 Tab）                   │
│  左侧导航树   │  ├── 状态列表（FileStatusTab）          │
│  仓库/分支/   │  ├── 日志 + Diff（LogTab）              │
│  标签/收藏    │  ├── Blame                             │
│              │  └── RefLog                            │
├──────────────┴───────────────────────────────────────┤
│  StatusBar（当前仓库 / 分支 / 任务进度）                │
└──────────────────────────────────────────────────────┘
```

- 顶部菜单栏：文件（打开/克隆/初始化/退出）、操作（Commit/Pull/Push/Switch/Merge/Rebase/...）、视图（主题/语言/显示过滤）、帮助
- 左侧导航树：仓库（含收藏与最近）、分支、标签
- 中间内容区：多 Tab 切换（状态列表 / 日志+Diff / Blame / RefLog）
- 底部状态栏：当前仓库路径、当前分支、后台任务进度

---

## 模态对话框清单

对齐 PRD 第四章每个 Git 操作，每个对话框一对 `XxxDialogController` + `XxxDialog.fxml`：

| 对话框 | 对应 PRD 章节 |
| ------ | ------ |
| `CloneDialog` | 4.1 Git Clone |
| `InitDialog` | 4.1 Git Init |
| `CommitDialog` | 4.4 Commit |
| `PullDialog` / `PushDialog` / `FetchDialog` / `SyncDialog` | 4.5 远程同步 |
| `SwitchDialog` / `CreateBranchDialog` / `MergeDialog` | 4.6 分支管理 |
| `TagDialog` | 4.7 标签管理 |
| `CherryPickDialog` | 4.8 Cherry Pick |
| `StashDialog` / `CleanUpDialog` / `GcDialog` | 4.9 暂存与撤销 |
| `RebaseDialog` | 4.11 Rebase |
| `RemoteConfigDialog` | 4.14 远程仓库配置 |
| `ConflictDialog` | 4.15 冲突解决 |
| `PatchDialog` | 4.16 补丁 |
| `BisectDialog` / `SubmoduleDialog` / `WorktreeDialog` / `HookDialog` / `GpgDialog` | 4.17 高级功能 |
| `SettingsDialog` | 4.18 设置 |
| `RedLineConfirmDialog` | 第八章 命令红线二次确认 |

> 对话框以独立模态窗口弹出，交互方式与 TortoiseGit 一致（PRD 第一章）。

---

## 文件上下文菜单

`FileContextMenu` 复用组件（PRD 4.13），按文件状态动态构造菜单项（BR-24）：

- Open / Open with
- Diff
- Edit conflicts（仅冲突文件）
- Resolve using Mine / Theirs（仅冲突文件）
- Add / Undo Add / Delete / Rename / Ignore
- Revert（见 BR-15）
- Lock / Unlock
- Blame
- Copy URL to clipboard
- Show in Explorer/Finder
- Properties

---

## 进度对话框

`ProgressDialog`（对齐 PRD 4.19 异步任务体系）：

- 含进度条 + 命令输出区 + 取消按钮
- 前台任务模态展示
- 后台任务可最小化到 StatusBar 进度条，不阻塞主窗口（BR-36）
- 进度通过 `ProgressCallback` 实时反馈

---

## 主题与语言

- 主题三模式：浅色 / 深色 / 跟随系统（BR-38），通过 `ThemeManager` 切换 `light.css` / `dark.css`
- 语言：中 / 英，通过 `ResourceBundle`（`messages_zh.properties` / `messages_en.properties`）切换
- 主题与语言切换立即生效，无需重启

---

## 快捷键

| 操作 | 默认快捷键 |
| ---- | ---- |
| Commit | `Ctrl+Enter` |
| Pull | `Ctrl+L` |
| Push | `Ctrl+P` |
| Switch | `Ctrl+Shift+S` |
| 打开仓库 | `Ctrl+O` |
| 克隆 | `Ctrl+Shift+C` |

> 快捷键可自定义，持久化到 `app_settings`（`ui.shortcuts` 键，BR-38）。

---

## 构建与部署

- 构建命令：在 `app-backend/` 下执行 `mvn clean package`（编译 + 测试 + 打包）
- 输出：`app-backend/target/git-gui-{version}.jar`（fat jar）+ `app-backend/scripts/git-gui.bat` / `git-gui.sh`
- 由启动脚本拉起 jar
