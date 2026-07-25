# project-docs — Java 图形化 Git 客户端文档总入口

> Java 图形化 Git 客户端（类 TortoiseGit）的产品文档总入口。纯 Java、跨平台、JavaFX 桌面应用，非 Web 服务。

---

## 技术概况

| 项目 | 选型 |
| ---- | ---- |
| 语言 / 运行时 | Java 21 |
| UI 框架 | JavaFX 21（FXML + Controller + ViewModel，MVVM） |
| IoC 容器 | Google Guice（非 Spring） |
| Git 能力 | JGit（主）+ Git CLI（兜底，UTF-8 + core.quotepath=false） |
| 本地存储 | SQLite + sqlite-jdbc + Flyway 自动迁移 |
| 构建 | Maven（单 `app-backend/` 模块，含 Flyway 迁移脚本） |
| 打包 | jar + bat/sh 启动脚本（可选 jpackage 原生安装包） |
| 不含 | 无 HTTP API、无 Docker、无 Web 前端、无前后端分离 |

---

## 顶层目录结构

```plaintext
project-docs/
├── 00-spec/          # SDD 规范体系（shared 通用基座 + project 项目专属）
├── 01-cicd/          # 桌面应用 CI/CD 流水线
├── 02-design/        # 设计文档（项目介绍 + 架构设计 + 流程图）
├── 04-test/          # 测试体系（test-unit + test-ui + 通用规范）
├── 05-self-check/    # 代码自检体系
├── 06-reference/     # 历史参考资料（设计模式、AI Agent 模式等，不参与改造）
└── 07-analysis/      # 需求分析（PRD、提示词笔记）
```

---

## 本地启动

参见 [02-design/01-项目介绍.md](./02-design/01-项目介绍.md) 的「快速启动」章节。

- 数据目录：`~/.git-gui/`（数据库 `git-gui.db`、日志、锁文件）
- 启动前需本机安装 Git；未安装时应用降级为纯 JGit 模式（参见 BR-41）

---

## 文档索引（按阅读目的）

| 我想… | 去哪里 |
| ---- | ---- |
| 理解产品定位与功能 | [07-analysis/report/](./07-analysis/report/) — 产品需求文档（PRD） |
| 了解整体架构与关键流程 | [02-design/](./02-design/) — 项目介绍 + 架构设计（含 Commit/Push/红线/异步 4 流程图） |
| 查阅 SDD 规范体系 | [00-spec/](./00-spec/) — shared 通用基座 + project 项目专属 |
| 查某条业务规则 / 实体字段 | [00-spec/project/01-domain.md](./00-spec/project/01-domain.md) — BR-01~BR-42 + 7 实体字典 |
| 查服务接口契约 / 错误码 | [00-spec/project/02-api.md](./00-spec/project/02-api.md) — 11 服务接口 + 错误码注册表 |
| 查命令红线拦截器 / 12 规则 | [00-spec/project/03-backend.md](./00-spec/project/03-backend.md) — CommandInterceptor + RedLineRule |
| 查表结构 / 迁移历史 | [00-spec/project/04-database.md](./00-spec/project/04-database.md) — 7 张表 + V1~V7 |
| 查 JavaFX UI / 对话框 | [00-spec/project/05-frontend.md](./00-spec/project/05-frontend.md) |
| 了解测试体系 / 用例 | [04-test/](./04-test/) — test-unit + test-ui + 通用规范 |
| 执行代码自检 | [05-self-check/](./05-self-check/) — 自检规范 + 提示词 |
| 了解 CI/CD 流水线 | [01-cicd/](./01-cicd/) — 桌面打包流水线 |

---

## 命令红线闭环（项目特色）

本项目的安全核心是「命令红线七层闭环」，详见 [02-design/02-架构设计.md](./02-design/02-架构设计.md) 的安全设计章节：

领域 BR-26~BR-32 → `audit_log` 表 → `CommandRedLineService` → `CommandInterceptor` + 13 个 `RedLineRule` → UI 确认/阻断对话框 → 设计文档流程图 → 测试用例（TC-UNIT-01/02/07/08、TC-UI-02）。
