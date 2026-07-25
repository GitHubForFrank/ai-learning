# git-gui — Java 图形化 Git 客户端

> 纯 Java、跨平台（Windows / Mac / Linux）图形化 Git 客户端，功能对标 TortoiseGit。采用「独立启动 + 手动选择 Git 工作目录」模式，无需系统右键菜单依赖。

---

## 技术概况

| 项目 | 选型 |
| ---- | ---- |
| 语言 | Java 21（LTS） |
| GUI | JavaFX 21 |
| Git 操作 | JGit（主）+ 本地 Git CLI（兜底） |
| IoC | Google Guice |
| 数据库 | SQLite（sqlite-jdbc + Flyway） |
| 构建 | Maven（单 `app-backend/` 模块，含 Flyway 迁移脚本） |

---

## 顶层目录结构

```plaintext
git-gui-java_JDK21/
├── app-backend/       # JavaFX 应用单模块（UI + 应用 + 领域 + 基础设施 + Flyway 迁移脚本）
│   ├── src/main/resources/db/migration/   # Flyway 迁移脚本 V1~V7
│   └── scripts/                          # 启动脚本 git-gui.bat / git-gui.sh
├── project-docs/      # 文档体系（规约、设计、测试、自检、CI/CD、参考资料）
├── .trae/             # IDE 规则与配置（coding / writing / SOLID / Markdown 规范）
├── .vscode/           # VS Code 工作区配置
└── 00-temp/           # 临时草稿 / 试验文件（不上线）
```

---

## 本地启动

**前置条件**：JDK 21、Maven 3.8+

```bash
# 在 app-backend/ 目录执行：
mvn clean package
# 验证：app-backend/target/ 下生成 git-gui-{version}.jar

# 启动（Windows）：
app-backend/scripts/git-gui.bat
# 启动（Mac/Linux）：
./app-backend/scripts/git-gui.sh
```

---

## 文档索引

| 我想了解... | 去看... |
| ----------- | ------ |
| 产品定位、功能概览、技术栈 | [project-docs/02-design/01-项目介绍.md](project-docs/02-design/01-项目介绍.md) |
| 架构设计、DDD 分层、模块划分 | [project-docs/02-design/02-架构设计.md](project-docs/02-design/02-架构设计.md) |
| 产品需求文档（PRD） | [project-docs/07-analysis/report/](project-docs/07-analysis/report/) |
| 业务规则、BR 编号体系 | [project-docs/00-spec/project/01-domain.md](project-docs/00-spec/project/01-domain.md) |
| 服务契约（API 接口） | [project-docs/00-spec/project/02-api.md](project-docs/00-spec/project/02-api.md) |
| 后端开发规范 | [project-docs/00-spec/project/03-backend.md](project-docs/00-spec/project/03-backend.md) |
| 数据库设计 | [project-docs/00-spec/project/04-database.md](project-docs/00-spec/project/04-database.md) |
| UI 开发规范 | [project-docs/00-spec/project/05-frontend.md](project-docs/00-spec/project/05-frontend.md) |
| 测试体系 | [project-docs/04-test/](project-docs/04-test/) |
| CI/CD 流水线 | [project-docs/01-cicd/](project-docs/01-cicd/) |
| 运维与部署 | [project-docs/00-spec/project/07-ops.md](project-docs/00-spec/project/07-ops.md) |
| 规范全景 | [project-docs/README.md](project-docs/README.md) |
