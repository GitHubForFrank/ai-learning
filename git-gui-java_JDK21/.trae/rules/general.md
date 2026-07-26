---
alwaysApply: true
description: 项目全局开发约定，所有对话均生效
---

# 规则总入口 — git-gui

## 项目定位

git-gui 是一个 Java 图形化 Git 客户端（类 TortoiseGit），基于 JavaFX 21 + Google Guice + Git CLI + SQLite，跨平台桌面应用。

## 按需加载原则

**每次任务执行时，根据任务类型按需加载规则，严禁全量加载。**

| 任务类型 | 加载文件 |
| --------- | --------- |
| 编码/修改代码 | `.trae/rules/coding.md` |
| 编写/修改文档 | `.trae/rules/writing.md` |
| 涉及业务规则 | `project-docs/00-spec/project/01-domain.md` |
| 涉及 API 接口 | `project-docs/00-spec/project/02-api.md` |
| 涉及后端开发 | `project-docs/00-spec/project/03-backend.md` |
| 涉及数据库 | `project-docs/00-spec/project/04-database.md` |
| 涉及 UI 开发（JavaFX） | `project-docs/00-spec/project/05-frontend.md` |
| 涉及测试/质量 | `project-docs/00-spec/project/06-quality.md` |
| 涉及部署/运维 | `project-docs/00-spec/project/07-ops.md` |

**严禁**：

- ❌ 每次任务全量加载所有 common rules 和 spec 文件
- ❌ 跳过任务类型判断直接读取全部 spec

---

## 核心原则

**本目录的 `.md` 文件是场景触发器，不是规则的权威来源。**
真正的规则、完整的判定标准、背后的设计决策，全部在 `project-docs/` 中。
触发器与 spec 冲突时，**以 spec 为准**。

---

## 快速导航

- **规范全景**：`project-docs/README.md`
- **spec 索引**：`project-docs/00-spec/README.md`
