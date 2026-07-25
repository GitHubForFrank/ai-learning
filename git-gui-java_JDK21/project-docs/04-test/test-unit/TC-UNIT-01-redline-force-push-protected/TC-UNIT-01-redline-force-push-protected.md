# TC-UNIT-01 — 命令红线拦截器-ForcePush/ProtectedBranch/DeleteProtectedBranch 规则

## 基本信息

| 项 | 内容 |
| ---- | ---- |
| 用例编号 | TC-UNIT-01 |
| 类型 | 单元测试（JUnit5 + Mockito） |
| 关联 BR | BR-26、BR-27、BR-28 |
| 关联模块 | BE-11 命令红线 / 安全 |
| 关联服务 | CommandRedLineService、CommandInterceptor |

## 测试目标

验证阻断类红线规则（BLOCK）的命中与拦截行为：

- `ForcePushRule`：裸 `--force` push → BLOCK
- `ProtectedBranchRule`：向保护分支（`main`/`master`/`develop` 及通配符 `release/*`）force push → BLOCK
- `DeleteProtectedBranchRule`：push 删除保护分支 → BLOCK

## 前置条件

- Mock `SettingsService.getProtectedBranches()` 返回 `["main", "master", "develop", "release/*"]`
- Mock `SettingsService.isRedLineEnabled()` 返回 `true`
- 构造 `RedLineContext`（operation=PUSH、branch、remoteUrl、force=true 等）
- 不启动 JavaFX，纯逻辑测试

## 测试步骤

| 步骤 | 操作 | 预期结果 |
| ---- | ---- | --------- |
| 1 | 构造裸 force push 上下文，调 `check()` | 返回 BLOCK + ruleCode=`RED_FORCE_PUSH` |
| 2 | 构造向 `main` 分支 force push 上下文 | 返回 BLOCK + ruleCode=`RED_PROTECTED_BRANCH` |
| 3 | 构造 push 删除 `release/1.0` 分支上下文 | 返回 BLOCK + ruleCode=`RED_DELETE_PROTECTED_BRANCH` |
| 4 | 构造向非保护分支普通 push 上下文 | 返回 PASS |
| 5 | 验证 BLOCK 场景 `recordAudit()` 被调用，actionResult=BLOCKED | audit_log 写入一次 |

## 整体期望结果

- 阻断类规则命中时返回 BLOCK 并附安全等价命令提示（如「请改用 `--force-with-lease`」）
- 命中操作写入 `audit_log`（actionResult=BLOCKED）
- 非命中操作返回 PASS 不写审计

## 备注

- 执行脚本（`TC-UNIT-01-*.java`）后续补，本文件为用例说明骨架
