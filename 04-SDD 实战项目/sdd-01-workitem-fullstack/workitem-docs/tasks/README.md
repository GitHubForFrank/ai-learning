# tasks/ —— Task 持久化轨迹目录

> **目录定位**:存放本项目所有 Task 的轨迹文档(`Task<NNN>-<kebab-short>.md`)。
> **规则真源**:`workitem-docs/specs/07-多任务并行开发.md §6`。本文件仅做**目录使用速查**,不重复规则。

## 一、文件清单(动态)

> 本节由维护者随 Task 变化更新。当前 P1 阶段尚无业务 Task,清单为空。

| 文件 | Task-ID | 状态 | 简述 |
|---|---|---|---|
| [Task001-add-login.md](./Task001-add-login.md) | Task001 | active | 添加最小登录(账号密码 + JWT + 失败 3 次锁定 + 登录日志 + traceparent) |

## 二、目录约定速查

- **位置**:本目录(`workitem-docs/tasks/`),单层平铺,**不分**子目录
- **文件名**:`Task<NNN>-<kebab-short>.md`,如 `Task001-add-user-tags.md`
- **状态**:由文件 frontmatter `status: active | closed` 控制;**不通过移动文件改状态**
- **永不复用**:废弃 Task 的编号不再分配给新 Task(参见 `07 §3.4`)
- **永不删除**:已 closed 的 md 留在原位作为历史轨迹(参见 `07 §6.5`)

## 三、什么 Task 必须建 md

| 类型 | 必须 md? |
|---|---|
| 涉及 DDL / 跨仓 ≥ 2 仓 / 引入 flag / Expand-Contract / 新增或改 BR / 改接口契约 | ✅ 必须 |
| spec PATCH(typo) / 单仓内部重构 / 升级依赖版本 | ❌ 免 |

完整阈值表见 `07 §6.4`。

## 四、模板速查

frontmatter 字段(必填)与正文必填段(Why / Scope / Out of Scope / PR Trail / Closure)详见 `07 §6.2 ~ §6.3`。
单个 md 文件总长建议 **≤ 50 行**。

## 五、维护操作

| 操作 | 何时 |
|---|---|
| 新建 md | 开 issue 锁 Task-ID 的同时创建,frontmatter `status: active` |
| 回填 PR Trail | 每个跨仓 PR 合入时 |
| 标记 closed | 所有 PR 合入 + PROD 上线后,frontmatter `status: closed`、回填 `closed` 日期、补 Closure 段 |
| 更新本 README §一 清单 | 任意 md 创建或状态变化时 |

---

**进一步阅读**:`workitem-docs/specs/07-多任务并行开发.md`(规则真源)、`workitem-docs/specs/06-任务与发布管理.md`(Task 编号体系)。
