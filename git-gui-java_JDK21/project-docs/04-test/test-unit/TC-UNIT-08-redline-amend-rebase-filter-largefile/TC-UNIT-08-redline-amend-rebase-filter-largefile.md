# TC-UNIT-08 — 命令红线-AmendPushed/RebasePushed/FilterBranch/LargeFile 规则 + AuditLog

## 基本信息

| 项 | 内容 |
| ---- | ---- |
| 用例编号 | TC-UNIT-08 |
| 类型 | 单元测试（JUnit5 + Mockito） |
| 关联 BR | BR-29、BR-31 |
| 关联模块 | BE-11 命令红线 / 安全 |
| 关联服务 | CommandRedLineService |

## 测试目标

验证以下二次确认类红线规则与审计日志持久化：

- `AmendPushedRule`：amend 已推送提交 → CONFIRM（BR-29）
- `RebasePushedRule`：rebase 已推送提交 → CONFIRM（BR-29）
- `FilterBranchRule`：filter-branch / filter-repo → CONFIRM（BR-29）
- `LargeFileRule`：推送超大文件（> 阈值默认 50MB，非 LFS）→ CONFIRM（BR-29）
- 所有命中红线的操作记录到 `audit_log`，追加写入不可修改/删除（BR-31）

## 前置条件

- Mock `SettingsService`（large_file_threshold_mb=50、isPushed 判断）
- Mock `AuditLogRepository`
- 构造 `RedLineContext`（isPushed=true、stagedFiles 含大文件）
- 不启动 JavaFX

## 测试步骤

| 步骤 | 操作 | 预期结果 |
| ---- | ---- | --------- |
| 1 | amend 已推送提交 | CONFIRM + ruleCode=`RED_AMEND_PUSHED` |
| 2 | rebase 已推送提交 | CONFIRM + ruleCode=`RED_REBASE_PUSHED` |
| 3 | filter-branch 操作 | CONFIRM + ruleCode=`RED_FILTER_BRANCH` |
| 4 | 暂存区含 60MB 非 LFS 文件 push | CONFIRM + ruleCode=`RED_LARGE_FILE` |
| 5 | 暂存区含 30MB 文件 push | PASS |
| 6 | `queryAudit()` 查询历史 | 返回审计列表，含 ruleCode/command/actionResult/detail |
| 7 | 验证 audit_log 仅追加 | 无 update/delete 方法暴露 |

## 整体期望结果

- 四条 CONFIRM 规则正确命中
- 审计日志追加写入、可查询、不可修改/删除

## 备注

- 执行脚本后续补，本文件为用例说明骨架
