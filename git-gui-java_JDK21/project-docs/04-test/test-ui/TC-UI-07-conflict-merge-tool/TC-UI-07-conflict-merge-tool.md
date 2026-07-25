# TC-UI-07 — 冲突解决/Merge Tool

## 基本信息

| 项 | 内容 |
| ---- | ---- |
| 用例编号 | TC-UI-07 |
| 类型 | UI 测试（TestFX，headless） |
| 关联 BR | BR-24 |
| 关联模块 | BE-08 文件操作、BE-09 冲突解决 |

## 测试目标

验证冲突文件的上下文菜单与冲突解决流程。

## 前置条件

- 应用 TestFX headless 启动，打开含冲突文件的仓库
- 预置外部 Merge 工具配置（或验证未配置时的提示）

## 测试步骤

| 步骤 | 操作 | 预期结果 |
| ---- | ---- | --------- |
| 1 | 选中冲突文件 | 上下文菜单显示 Edit conflicts / Resolve using Mine/Theirs |
| 2 | 选非冲突文件 | 上述菜单项禁用 |
| 3 | 点 Resolve using Mine | 文件标记为已解决（mine） |
| 4 | 点 Edit conflicts 调外部 Merge 工具 | 未配置时提示 MERGE_TOOL_NOT_CONFIGURED |
| 5 | 标记已解决后提交 | 提交成功 |

## 整体期望结果

- 冲突文件菜单动态启用/禁用
- 冲突解决与 Merge Tool 调用正确

## 截图要求

- 每步操作后截图，命名 `TC-UI-07-001.png` 等

## 备注

- 执行脚本后续补，本文件为用例说明骨架
