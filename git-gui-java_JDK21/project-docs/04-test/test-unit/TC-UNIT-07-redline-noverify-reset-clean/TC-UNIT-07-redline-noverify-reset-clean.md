# TC-UNIT-07 — 命令红线-NoVerify/ResetHard/CleanFdx 规则

## 基本信息

| 项 | 内容 |
| ---- | ---- |
| 用例编号 | TC-UNIT-07 |
| 类型 | 单元测试（JUnit5 + Mockito） |
| 关联 BR | BR-26、BR-29 |
| 关联模块 | BE-11 命令红线 / 安全、BE-05 暂存撤销 |
| 关联服务 | CommandRedLineService |

## 测试目标

验证以下红线规则：

- `NoVerifyRule`：`--no-verify` 跳过 hook → BLOCK（BR-26）
- `ResetHardRule`：`reset --hard` → CONFIRM，提示将丢失的文件清单（BR-29）
- `CleanFdxRule`：`clean -fdx` → CONFIRM（BR-29）

## 前置条件

- Mock `SettingsService.isRedLineEnabled()` 返回 `true`
- 构造 `RedLineContext`（operation=RESET/CLEAN、targetCommit、args 含 `--no-verify`/`--hard`/`-fdx`）
- 不启动 JavaFX

## 测试步骤

| 步骤 | 操作 | 预期结果 |
| ---- | ---- | --------- |
| 1 | commit 带 `--no-verify` | BLOCK + ruleCode=`RED_NO_VERIFY` |
| 2 | `reset --hard` 上下文 | CONFIRM + ruleCode=`RED_RESET_HARD`，附文件清单 |
| 3 | `clean -fdx` 上下文 | CONFIRM + ruleCode=`RED_CLEAN_FDX` |
| 4 | CONFIRM 场景用户取消 | actionResult=CANCELLED，写 audit_log |
| 5 | CONFIRM 场景用户确认 | actionResult=CONFIRMED，写 audit_log |

## 整体期望结果

- `--no-verify` 阻断
- `reset --hard` / `clean -fdx` 二次确认
- 确认/取消均写审计

## 备注

- 执行脚本后续补，本文件为用例说明骨架
