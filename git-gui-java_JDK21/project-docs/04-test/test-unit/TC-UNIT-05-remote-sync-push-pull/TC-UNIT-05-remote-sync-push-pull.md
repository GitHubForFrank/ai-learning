# TC-UNIT-05 — 远程同步 Push/Pull/Fetch 服务

## 基本信息

| 项 | 内容 |
| ---- | ---- |
| 用例编号 | TC-UNIT-05 |
| 类型 | 单元测试（JUnit5 + Mockito） |
| 关联 BR | BR-09、BR-10、BR-11 |
| 关联模块 | BE-03 远程同步 |
| 关联服务 | GitOperationService、RemoteConfigService |

## 测试目标

验证远程同步业务规则：

- Pull/Push/Fetch/Sync 必须先选 Remote 与分支；远程分支列表从 `git ls-remote` 或缓存读取（BR-09）
- Push 执行前必经命令红线拦截器校验（BR-10）
- UI 默认隐藏裸 `--force`，仅暴露 force with lease；裸 force 仅在高级模式可见且仍走阻断红线（BR-11）

## 前置条件

- Mock `GitOperationExecutor` 与 `RemoteConfigService`
- Mock `CommandInterceptor`
- 构造 `PushRequest`/`PullRequest`/`FetchRequest`（含 remote、branch）
- 不启动 JavaFX

## 测试步骤

| 步骤 | 操作 | 预期结果 |
| ---- | ---- | --------- |
| 1 | `push()` 未指定 remote | 抛 `VALIDATION_FAILED` |
| 2 | `push()` 正常（force-with-lease） | 拦截器 check 被调用，返回 TaskHandle |
| 3 | `push()` 裸 force（高级模式） | 拦截器返回 BLOCK（RED_FORCE_PUSH） |
| 4 | `pull()` 指定 remote+branch | 返回 TaskHandle，异步可取消 |
| 5 | `RemoteConfigService.list()` | 返回 `.git/config` 解析的远程列表 |

## 整体期望结果

- 远程与分支缺失时校验拒绝
- Push 必经红线拦截
- 裸 force 走阻断红线

## 备注

- 执行脚本后续补，本文件为用例说明骨架
