# 测试体系

本目录用于存放 git-gui 项目的回归测试用例、测试执行提示词及测试报告。测试分两层：单元测试（test-unit）与 UI 测试（test-ui）。

## 目录结构

```plaintext
04-test/
├── README.md                              # 本文件
├── test-prompt.txt                        # 回归测试执行提示词
├── test-spec/                             # 通用测试规范（跨项目复用）
│   ├── 测试设计规范.md                     # 测试体系结构、公共工具规范、报告模板
│   └── 测试执行规范.md                     # 测试执行流程、执行规则、截图规范
├── test-unit/                             # 单元测试用例（JUnit5 + Mockito）
│   └── TC-UNIT-{NN}-{功能描述}/           # 每个用例一个子目录
├── test-ui/                               # UI 测试用例（TestFX）
│   └── TC-UI-{NN}-{功能描述}/             # 每个用例一个子目录
└── test-report/                           # 全局汇总报告归档
    └── YYYYMMDD_HHMMSS.md                 # 每次执行的报告（按时间戳命名）
```

## 测试分层

| 层 | 目录 | 技术栈 | 范围 |
| ---- | ---- | ---- | ---- |
| 单元测试 | `test-unit/` | JUnit5 + Mockito | 纯逻辑：领域 Model、服务实现、命令红线拦截器、RedLineRule、TaskManager、Mapper；不启动 JavaFX |
| UI 测试 | `test-ui/` | TestFX（headless） | JavaFX 界面：主窗口、对话框、上下文菜单、红线确认/阻断弹窗、设置页 |

> 公共 Java 测试助手（如测试仓库构造器、Mock 工厂）位于 `app-backend/src/test/java`，不单独建 test-common 目录。

## 用例命名

- 单元测试：`TC-UNIT-{NN}-{功能描述}`（如 `TC-UNIT-01-redline-force-push`）
- UI 测试：`TC-UI-{NN}-{功能描述}`（如 `TC-UI-02-redline-confirm-dialog`）
- 每个用例独立子目录，含必须的 `.md` 说明文件和可选的执行脚本
- 执行产出（日志、截图）存放在各 case 的 `output/` 子目录，不提交版本库

## 用例清单

### 单元测试（TC-UNIT-）

| 编号 | 标题 | 关联 BR |
| ---- | ---- | ---- |
| TC-UNIT-01 | 命令红线拦截器-ForcePush/ProtectedBranch/DeleteProtectedBranch 规则 | BR-26、BR-27、BR-28 |
| TC-UNIT-02 | 命令红线拦截器-SensitiveFile/RemoteWhitelist 规则 | BR-28、BR-32 |
| TC-UNIT-03 | 仓库检索/收藏/最近仓库服务 | BR-01~BR-05 |
| TC-UNIT-04 | Commit 提交服务 | BR-06~BR-08 |
| TC-UNIT-05 | 远程同步 Push/Pull/Fetch 服务 | BR-09~BR-11 |
| TC-UNIT-06 | 分支/标签/Rebase/日志服务 | BR-12、BR-13、BR-18~BR-23 |
| TC-UNIT-07 | 命令红线-NoVerify/ResetHard/CleanFdx 规则 | BR-26、BR-29 |
| TC-UNIT-08 | 命令红线-AmendPushed/RebasePushed/FilterBranch/LargeFile 规则 + AuditLog | BR-29、BR-31 |

### UI 测试（TC-UI-）

| 编号 | 标题 | 关联 BR |
| ---- | ---- | ---- |
| TC-UI-01 | 主窗口/导航树/多 Tab 切换 | — |
| TC-UI-02 | 命令红线确认与阻断对话框 | BR-26~BR-32 |
| TC-UI-03 | 仓库克隆/打开/收藏 | BR-01~BR-05 |
| TC-UI-04 | Commit 提交流程 | BR-06~BR-08 |
| TC-UI-05 | Push/Pull 流程（含红线闭环） | BR-09~BR-11、BR-26~BR-32 |
| TC-UI-06 | 分支/标签管理 | BR-12、BR-13 |
| TC-UI-07 | 冲突解决/Merge Tool | BR-24 |
| TC-UI-08 | 设置（主题/语言/Git 路径/保护分支/远程白名单） | BR-27、BR-28、BR-37~BR-39 |

## 说明

- 详细通用规范参见 `test-spec/测试设计规范.md` 和 `test-spec/测试执行规范.md`（跨项目复用的方法论模板）
- 用例文档格式遵循 `test-spec/测试设计规范.md` §4.3 的用例模板（前置条件、步骤表、预期结果）
