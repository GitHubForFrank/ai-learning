# TC-UNIT-03 — 仓库检索/收藏/最近仓库服务

## 基本信息

| 项 | 内容 |
| ---- | ---- |
| 用例编号 | TC-UNIT-03 |
| 类型 | 单元测试（JUnit5 + Mockito） |
| 关联 BR | BR-01、BR-02、BR-03、BR-04、BR-05 |
| 关联模块 | BE-01 仓库检索与收藏 |
| 关联服务 | RepositoryService、FavoriteService、RecentRepoService |

## 测试目标

验证仓库检索与收藏相关服务逻辑：

- 多仓库检索默认深度 3 层、可配置、异步可取消（BR-01/02）
- 收藏 `repoPath` 唯一、重复拒绝；alias/group 长度限制（BR-03）
- 收藏置顶与排序（BR-04）
- 最近仓库倒序、默认保留 20 条、超限淘汰、upsert（BR-05）

## 前置条件

- Mock 仓储层（FavoriteRepository、RecentRepoRepository）
- 构造临时目录树模拟多仓库结构
- 不启动 JavaFX

## 测试步骤

| 步骤 | 操作 | 预期结果 |
| ---- | ---- | --------- |
| 1 | `scanMultiRepo(root, 3, cb)` 检索 3 层 | 返回 TaskHandle，结果含 `.git` 目录 |
| 2 | 检索中调 `cancel()` | 任务状态转 CANCELLED |
| 3 | `FavoriteService.add()` 添加已存在 repoPath | 抛 `DUPLICATE_FAVORITE` |
| 4 | alias 长度 > 100 | 抛 `VALIDATION_FAILED` |
| 5 | `reorder()` 调整置顶与 sortOrder | 列表按 pinned + sortOrder 排序 |
| 6 | `RecentRepoService.recordOpen()` 打开已存在仓库 21 次 | 列表仅保留 20 条，最旧被淘汰 |

## 整体期望结果

- 检索异步可取消不阻塞
- 收藏唯一性与长度校验生效
- 最近仓库倒序 + 超限淘汰正确

## 备注

- 执行脚本后续补，本文件为用例说明骨架
