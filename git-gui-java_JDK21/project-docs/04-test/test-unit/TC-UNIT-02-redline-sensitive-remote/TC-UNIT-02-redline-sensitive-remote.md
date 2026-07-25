# TC-UNIT-02 — 命令红线拦截器-SensitiveFile/RemoteWhitelist 规则

## 基本信息

| 项 | 内容 |
| ---- | ---- |
| 用例编号 | TC-UNIT-02 |
| 类型 | 单元测试（JUnit5 + Mockito） |
| 关联 BR | BR-28、BR-32 |
| 关联模块 | BE-11 命令红线 / 安全 |
| 关联服务 | CommandRedLineService、SettingsService |

## 测试目标

验证以下阻断类红线规则：

- `SensitiveFileRule`：Commit/Push 前扫描暂存区，命中敏感文件（`.env`/`credentials`/`*.pem`/`*.key`/`id_rsa`/`.npmrc` 及自定义正则）→ BLOCK 并提示加入 `.gitignore`
- `RemoteWhitelistRule`：推送到非授权远程（URL 不在白名单）→ BLOCK；删除远程分支按保护分支规则阻断

## 前置条件

- Mock `SettingsService.getSensitiveFileRules()` 返回默认规则集 + 一条自定义正则
- Mock `SettingsService.getRemoteWhitelist()` 返回 `["https://git.example.com/*"]`
- 构造含敏感文件的 `RedLineContext.stagedFiles`
- 不启动 JavaFX

## 测试步骤

| 步骤 | 操作 | 预期结果 |
| ---- | ---- | --------- |
| 1 | 暂存区含 `.env` 文件，调 `check()` | BLOCK + ruleCode=`RED_SENSITIVE_FILE`，提示加入 .gitignore |
| 2 | 暂存区含 `id_rsa` 文件 | BLOCK + ruleCode=`RED_SENSITIVE_FILE` |
| 3 | 暂存区仅含普通源码文件 | PASS |
| 4 | remoteUrl 不在白名单的 push | BLOCK + ruleCode=`RED_REMOTE_WHITELIST` |
| 5 | remoteUrl 匹配白名单通配符的 push | PASS |

## 整体期望结果

- 敏感文件命中阻断并给出 `.gitignore` 建议
- 非授权远程推送阻断
- 命中均写 `audit_log`

## 备注

- 执行脚本后续补，本文件为用例说明骨架
