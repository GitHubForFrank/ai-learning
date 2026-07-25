# TC-UNIT-04 — Commit 提交服务

## 基本信息

| 项 | 内容 |
| ---- | ---- |
| 用例编号 | TC-UNIT-04 |
| 类型 | 单元测试（JUnit5 + Mockito） |
| 关联 BR | BR-06、BR-07、BR-08 |
| 关联模块 | BE-02 Commit |
| 关联服务 | GitOperationService |

## 测试目标

验证 Commit 提交业务规则：

- 提交前必须勾选至少一个变更文件，未勾选拒绝（BR-06）
- Commit message 必填且去空白后非空；Amend 可复用上次 message 但仍可编辑（BR-07）
- Commit & Push 提交成功后触发推送红线校验；推送失败不回滚已成功提交（BR-08）

## 前置条件

- Mock `GitOperationExecutor`（JGit 适配器）
- Mock `CommandInterceptor`（验证推送红线被调用）
- 构造 `CommitRequest`（含 stagedFiles、message、amend 标志）
- 不启动 JavaFX

## 测试步骤

| 步骤 | 操作 | 预期结果 |
| ---- | ---- | --------- |
| 1 | `commit()` 传入空 stagedFiles | 抛 `VALIDATION_FAILED` |
| 2 | `commit()` 传入空白 message | 抛 `VALIDATION_FAILED` |
| 3 | `commit()` 正常提交 | 返回 CommitResult，executor.commit 被调用一次 |
| 4 | Amend 模式不传 message | 复用上次 message，提交成功 |
| 5 | `push()`（Commit & Push）推送阶段红线阻断 | 提交已成功，推送抛 `RED_LINE_BLOCKED`，提交不回滚 |

## 整体期望结果

- 变更文件与 message 校验生效
- Commit & Push 推送失败不回滚提交
- 推送阶段经拦截器调用红线

## 备注

- 执行脚本后续补，本文件为用例说明骨架
