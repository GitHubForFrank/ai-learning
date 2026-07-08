# releases/ —— 环境晋级 release plan 目录

> **目录定位**:存放本项目所有环境晋级的 release plan 文件(`<env>-<YYYY-MM-DD>-<seq>.md`)。
> **规则真源**:`workitem-docs/specs/08-环境晋级与选择性发布.md §6`。本文件仅做**目录使用速查**,不重复规则。

## 一、文件清单(动态)

> 本节由维护者随每次晋级追加。当前 P1 阶段尚无晋级,清单为空。

| 文件 | 环境 | 日期 | spec tag | 状态 | 关联 Task |
|---|---|---|---|---|---|
| [sit-2026-04-25-01.md](./sit-2026-04-25-01.md) | sit | 2026-04-25 | v1.0.0(规划) | planned | Task001 |

## 二、命名约定

```
<env>-<YYYY-MM-DD>-<seq>.md
```

| 段 | 取值 |
|---|---|
| `<env>` | `sit` / `uat` / `prod-mirror` / `prod`(全小写) |
| `<YYYY-MM-DD>` | **计划晋级日期**(不是创建日期) |
| `<seq>` | 同日同环境序号,从 `01` 起,两位数 |

示例占位符:`uat-2026-05-01-01.md`、`prod-2026-05-10-01.md`、`prod-2026-05-10-02.md`(同日二次晋级)

## 三、状态机

| status | 含义 | 谁能改 |
|---|---|---|
| `planned` | 已起草,待审批 | 起草者 |
| `approved` | 审批通过,等待 ops 执行 | reviewer 标 approved 后,plan 内容冻结 |
| `executed` | ops 已执行,回填实际数据 | ops 执行后回填 |
| `rolled-back` | 执行后回滚 | ops 回滚后回填 |

> ⚠️ 状态机**单向推进**;`approved` 之后**禁止**改 plan 内容,如需变更必须开新 plan。

## 四、必填段速查

frontmatter 字段(8 项)与正文 8 段(目标 / Task / Flag diff / 上游验证 / DDL 影响 / 回滚预案 / 风险 / 执行回填)详见 `08 §6.2 ~ §6.3`。

## 五、与 `03-技术方案.md §12` 的强制对齐

每份 release plan 的 §3 flag 变更行,合入 `executed` 后**必须**与 `workitem-docs/design/03-技术方案.md §12` 对应单元格一致。CI 校验项见 `08 §10.1`。

## 六、维护操作

| 操作 | 何时 |
|---|---|
| 新建 plan | 决定晋级时,frontmatter `status: planned` |
| 标 approved | review 通过 |
| 标 executed + 回填 §8 | ops 执行后 |
| 同步 flag 矩阵 | plan executed 时同 PR 或紧随其后 PR |
| 更新本 README §一 清单 | plan 创建或状态变化时 |

---

**进一步阅读**:`workitem-docs/specs/08-环境晋级与选择性发布.md`(规则真源)、`workitem-docs/design/03-技术方案.md §12`(当前态矩阵)。
