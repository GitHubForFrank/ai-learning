# TC-UNIT-06 — 分支/标签/Rebase/日志服务

## 基本信息

| 项 | 内容 |
| ---- | ---- |
| 用例编号 | TC-UNIT-06 |
| 类型 | 单元测试（JUnit5 + Mockito） |
| 关联 BR | BR-12、BR-13、BR-18、BR-19、BR-20、BR-21、BR-22、BR-23 |
| 关联模块 | BE-04 分支标签、BE-06 日志回溯、BE-07 Rebase |
| 关联服务 | GitOperationService、StatusService |

## 测试目标

验证分支/标签操作、日志回溯与 Rebase 业务规则：

- 分支操作前校验工作区是否干净（BR-12）
- 删除未合并本地分支二次确认；删除远程分支走红线阻断（BR-13）
- 日志倒序、筛选、分页（BR-18）
- Reset to commit 四模式，Hard 走二次确认（BR-19）
- Revert/Cherry Pick（BR-20）
- 交互式 Rebase 动作白名单（BR-21）；Rebase 已推送走二次确认（BR-22）；冲突 Continue/Skip/Abort（BR-23）

## 前置条件

- Mock `GitOperationExecutor` 与 `StatusService`
- Mock `CommandInterceptor`
- 构造含未提交修改/已合并/未合并分支的仓库 mock
- 不启动 JavaFX

## 测试步骤

| 步骤 | 操作 | 预期结果 |
| ---- | ---- | --------- |
| 1 | 工作区不干净时 `checkout()` | 提示 stash/commit/abort |
| 2 | 删除未合并本地分支 | 触发二次确认 |
| 3 | 删除远程保护分支 | 拦截器 BLOCK（RED_DELETE_PROTECTED_BRANCH） |
| 4 | `getLog()` 分页 page=1, size=200 | 返回倒序日志 |
| 5 | `reset(target, HARD)` | 拦截器返回 CONFIRM（RED_RESET_HARD） |
| 6 | 交互式 Rebase 动作含 `unknown` | 拒绝 |
| 7 | Rebase 已推送提交 | 拦截器返回 CONFIRM（RED_REBASE_PUSHED） |
| 8 | Rebase 冲突 `abort` | 可回滚到 Rebase 前状态 |

## 整体期望结果

- 工作区干净校验生效
- 危险分支操作走红线/二次确认
- Rebase 动作白名单与冲突处理正确

## 备注

- 执行脚本后续补，本文件为用例说明骨架
