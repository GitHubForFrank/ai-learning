# 领域模型 — git-gui 项目专属

> **定位**：业务规则（BR-*）+ 实体字典，所有业务语义的权威来源。
>
> BR 编号格式 `BR-{NN}`（两位数递增不复用）；实体字典使用「字段 | 类型 | 说明」三列表格；提供按模块快速查找索引。
>
> 命令红线相关规则（BR-26~BR-32）是本项目安全核心，参见 [PRD 第八章](../../07-analysis/report/Java图形化Git客户端（类TortoiseGit）产品需求文档.md)。

---

## 影响模块代码表

| 代码 | 模块 |
| ---- | ---- |
| BE-01 | 仓库检索与收藏 |
| BE-02 | Commit |
| BE-03 | 远程同步 |
| BE-04 | 分支标签 |
| BE-05 | 暂存撤销 |
| BE-06 | 日志回溯 |
| BE-07 | Rebase |
| BE-08 | 文件操作与上下文菜单 |
| BE-09 | 冲突解决 |
| BE-10 | 设置 |
| BE-11 | 命令红线 / 安全 |
| BE-12 | 异步任务 |
| BE-13 | 启动 / 单实例 / 编码 |

---

## 业务规则（BR-*）

### 完整列表

#### 仓库检索与收藏

| BR | 规则描述 | 影响模块 |
| ---- | --------- | -------- |
| **BR-01** | 多仓库自动检索默认深度 3 层、可在 Settings 配置（1~10）；仅只读 `.git` 目录存在性，不修改任何仓库状态 | BE-01 |
| **BR-02** | 检索必须异步执行并可取消，大目录检索不阻塞主界面（遵循 BR-33 异步任务体系） | BE-01 |
| **BR-03** | 收藏项 `repoPath` 必填且全局唯一，重复收藏拒绝；`alias` 可选最大长度 100，`group` 可选最大长度 50 | BE-01 |
| **BR-04** | 收藏项支持置顶（pinned）与排序（sortOrder），置顶项在收藏列表顶部独立展示 | BE-01 |
| **BR-05** | 最近仓库列表按 `lastOpenedAt` 倒序，默认保留 20 条（可配置），超限自动淘汰最旧；打开已存在仓库时更新 `lastOpenedAt` 与 `openCount` | BE-01 |

#### Commit

| BR | 规则描述 | 影响模块 |
| ---- | --------- | -------- |
| **BR-06** | Commit 提交前必须勾选至少一个变更文件，未勾选时提交按钮置灰；支持全选/全不选/按状态筛选 | BE-02 |
| **BR-07** | Commit message 必填且去首尾空白后非空；Amend 模式可复用上次 message 但仍可编辑 | BE-02 |
| **BR-08** | Commit & Push / Commit & Push with tags 在提交成功后触发推送红线校验（参见 BR-26~BR-32）；推送失败不回滚已成功提交 | BE-02, BE-11 |

#### 远程同步

| BR | 规则描述 | 影响模块 |
| ---- | --------- | -------- |
| **BR-09** | Pull/Push/Fetch/Sync 操作必须先选择 Remote 与分支；远程分支列表从 `git ls-remote` 或上次 fetch 缓存读取，远程列表来自 `.git/config` | BE-03 |
| **BR-10** | Push 操作执行前必须经命令红线拦截器校验（BR-26~BR-32），未通过阻断或确认不得执行 | BE-03, BE-11 |
| **BR-11** | UI 默认隐藏裸 `--force`，仅暴露 Force with lease；裸 `--force` 仅在 Settings 显式开启「高级模式」后可见，且仍走阻断红线 | BE-03, BE-11 |

#### 分支标签

| BR | 规则描述 | 影响模块 |
| ---- | --------- | -------- |
| **BR-12** | 切换/检出/创建/删除/重命名分支、创建/删除标签等分支操作前需校验工作区是否干净；存在未提交修改时按操作类型提示 stash/commit/abort | BE-04 |
| **BR-13** | 删除本地分支需校验是否已合并，未合并分支删除需二次确认；删除远程分支走红线阻断（BR-28） | BE-04, BE-11 |

#### 暂存撤销

| BR | 规则描述 | 影响模块 |
| ---- | --------- | -------- |
| **BR-14** | Stash Save/Pop/Apply/Drop/Branch 通过 JGit StashCommands 执行；Stash List 实时从 Git 读取，不入库 | BE-05 |
| **BR-15** | Revert 文件（`git restore`）/Undo Add（`git restore --staged`）在文件级上下文菜单执行，对未修改/未暂存文件禁用菜单项 | BE-05, BE-08 |
| **BR-16** | Clean up 执行前必须 Dry run 预览且二次确认（命中 BR-29 二次确认红线） | BE-05, BE-11 |
| **BR-17** | Git GC 与 Clean up 拆分为独立动作；GC 执行需二次确认（耗时且不可逆，触发仓库压缩） | BE-05 |

#### 日志回溯

| BR | 规则描述 | 影响模块 |
| ---- | --------- | -------- |
| **BR-18** | 日志默认按提交时间倒序展示，支持按日期范围/作者/关键词/分支范围（All/Remotes/Stash/Bisect/first-parent/working tree）筛选；大仓库日志分页加载，单页默认 200 条可配置 | BE-06 |
| **BR-19** | Reset to commit 支持 Soft/Mixed/Hard/Keep 四种模式；Hard 模式走红线二次确认（BR-29），提示将丢失的文件清单 | BE-06, BE-11 |
| **BR-20** | Revert Commit 创建反向提交不改写历史，可对已推送提交执行；Cherry Pick 支持连续多提交遴选 | BE-06 |

#### Rebase

| BR | 规则描述 | 影响模块 |
| ---- | --------- | -------- |
| **BR-21** | 交互式 Rebase 必须列出待变基提交清单，每行动作限定白名单 pick/reword/edit/squash/fixup/drop；非白名单值拒绝 | BE-07 |
| **BR-22** | Rebase 已推送提交走红线二次确认（BR-30）；交互式 Rebase 默认仅允许未推送提交 | BE-07, BE-11 |
| **BR-23** | Rebase 冲突进入 Continue/Skip/Abort 循环；Abort 必须可回滚到 Rebase 前状态（`git rebase --abort`） | BE-07 |

#### 文件操作与上下文菜单

| BR | 规则描述 | 影响模块 |
| ---- | --------- | -------- |
| **BR-24** | 文件上下文菜单项根据文件状态（已修改/未跟踪/冲突/已暂存/未暂存/已忽略）动态启用/禁用；冲突文件才显示 Edit conflicts / Resolve using Mine/Theirs | BE-08, BE-09 |
| **BR-25** | Ignore 操作支持递归/仅当前目录/按扩展名三种模式，写入 `.gitignore` 后立即刷新状态列表；已忽略文件默认隐藏，勾选「显示已忽略」可见 | BE-08 |

#### 命令红线 / 安全（项目特色，闭环核心）

| BR | 规则描述 | 影响模块 |
| ---- | --------- | -------- |
| **BR-26** | 阻断类红线命中即拦截阻断并引导改用安全等价命令：裸 force push、向保护分支 force push、push 删除保护分支、推送含敏感信息文件、推送到非授权远程、`--no-verify` 跳过 hook | BE-11 |
| **BR-27** | 保护分支清单支持通配符（如 `release/*`），默认含 `main`/`master`/`develop`；规则在 Settings 配置，存储于 `app_settings` | BE-10, BE-11 |
| **BR-28** | 推送到非授权远程（URL 不在白名单）阻断；远程白名单在 Settings 配置，支持域名通配；删除远程分支按保护分支规则阻断 | BE-10, BE-11 |
| **BR-29** | 二次确认类红线执行前弹窗确认并提示具体风险：`reset --hard`、`clean -fdx`、`commit --amend` 已推送提交、`rebase` 已推送提交、`filter-branch`/`filter-repo`、推送超大文件（> 阈值，默认 50MB，非 LFS） | BE-11 |
| **BR-30** | 红线开关总体可关闭（不推荐），关闭时所有阻断降级为二次确认；关闭/开启操作本身写入 `audit_log` | BE-11 |
| **BR-31** | 所有命中红线的操作记录到 `audit_log`，含时间、规则代码、命令、仓库、分支、远程URL、处理结果（BLOCKED/CONFIRMED/CANCELLED）、详情；追加写入不可修改/删除 | BE-11 |
| **BR-32** | 敏感文件规则内置常见模式（`.env`/`credentials`/`*.pem`/`*.key`/`id_rsa`/`.npmrc`），支持自定义正则；Commit 与 Push 前扫描暂存区文件名，命中阻断并提示加入 `.gitignore` | BE-11 |

#### 异步任务

| BR | 规则描述 | 影响模块 |
| ---- | --------- | -------- |
| **BR-33** | 长耗时操作（Clone/Pull/Push/Fetch/Rebase/Merge/MultiRepoScan/GC）必须走统一异步任务体系，进度实时反馈且可取消；JGit 通过 `ProgressMonitor.cancel`，CLI 通过终止进程 | BE-12 |
| **BR-34** | 同一仓库同一时间只允许一个写操作任务，重复提交排队；读操作（status/log/scan）可并发；任务队列由 `TaskManager` 统一调度 | BE-12 |
| **BR-35** | 任务失败不自动重试写操作，由用户手动重试；任务结果（成功/失败/取消 + 错误信息）记录到 `operation_log` | BE-12 |
| **BR-36** | 后台任务可最小化到状态栏，不阻塞主窗口其他操作；任务完成通过事件总线通知 UI 刷新 | BE-12 |

#### 设置

| BR | 规则描述 | 影响模块 |
| ---- | --------- | -------- |
| **BR-37** | Git 配置分级：全局（`~/.gitconfig`）与仓库（`.git/config`）；per-repo 独立 `user.name`/`user.email` 修改通过 `git config` 写入对应文件 | BE-10 |
| **BR-38** | 主题（浅色/深色/跟随系统）、语言（中/英）切换立即生效无需重启；快捷键自定义持久化到 `app_settings` | BE-10 |
| **BR-39** | 凭证通过系统 credential helper（osxkeychain/wincred/manager-core）管理，应用不明文存储密码；SSH 密钥仅存储路径不存密钥内容 | BE-10 |

#### 启动 / 单实例 / 编码

| BR | 规则描述 | 影响模块 |
| ---- | --------- | -------- |
| **BR-40** | 单实例运行通过锁文件检测（用户目录下 `.git-gui.lock`），二次启动时聚焦已有窗口而非启动新进程 | BE-13 |
| **BR-41** | 启动时检测本地 Git 可执行文件，未安装时提示并降级为纯 JGit 模式（LFS/Hook/Submodule/复杂 Rebase 等场景受限） | BE-13 |
| **BR-42** | 所有 Git 命令行调用强制 `UTF-8` 编码并设置 `core.quotepath=false`，避免 Windows 中文路径乱码；JGit 路径不受此影响 | BE-13 |

### 按模块快速查找

- **仓库检索与收藏**：BR-01、BR-02、BR-03、BR-04、BR-05
- **Commit**：BR-06、BR-07、BR-08
- **远程同步**：BR-09、BR-10、BR-11
- **分支标签**：BR-12、BR-13
- **暂存撤销**：BR-14、BR-15、BR-16、BR-17
- **日志回溯**：BR-18、BR-19、BR-20
- **Rebase**：BR-21、BR-22、BR-23
- **文件操作与上下文菜单**：BR-24、BR-25
- **命令红线 / 安全**：BR-26、BR-27、BR-28、BR-29、BR-30、BR-31、BR-32
- **异步任务**：BR-33、BR-34、BR-35、BR-36
- **设置**：BR-37、BR-38、BR-39
- **启动 / 单实例 / 编码**：BR-40、BR-41、BR-42

> BR 互引示例：`BR-08 制约 BR-26~BR-32 的推送红线`；`BR-16 / BR-19 命中 BR-29 的二次确认红线`。

---

## 数据字典

### Favorite（收藏）

> **状态机**：创建 → 可编辑（alias/group/pinned/sortOrder）→ 删除（不可恢复，不影响目标仓库）。

| 字段 | 类型 | 说明 |
| ----- | ------ | ----- |
| `id` | string (UUID) | 主键，应用层生成 |
| `repoPath` | string | 仓库绝对路径，全局唯一 |
| `alias` | string / null | 别名（便于识别，如「前端主仓库」） |
| `group` | string / null | 分组（如「项目」「团队」） |
| `pinned` | boolean | 是否置顶 |
| `sortOrder` | int | 排序权重 |
| `remoteUrl` | string / null | 远程 URL（缓存便于列表展示） |
| `createdAt` | datetime | 创建时间 |
| `updatedAt` | datetime | 更新时间 |

### RecentRepo（最近仓库）

> **状态机**：打开仓库时 upsert（已存在则更新 `lastOpenedAt`/`lastBranch`/`openCount`），超限淘汰最旧。

| 字段 | 类型 | 说明 |
| ----- | ------ | ----- |
| `id` | string (UUID) | 主键 |
| `repoPath` | string | 仓库绝对路径，唯一 |
| `lastBranch` | string | 上次打开的分支 |
| `lastOpenedAt` | datetime | 最后打开时间 |
| `openCount` | int | 累计打开次数 |
| `createdAt` | datetime | 创建时间 |
| `updatedAt` | datetime | 更新时间 |

### OperationLog（操作日志）

> **状态机**：追加写入，不可修改/不可删除。每次 Git 操作完成（成功/失败/取消）后立即写一条。

| 字段 | 类型 | 说明 |
| ----- | ------ | ----- |
| `id` | string (UUID) | 主键 |
| `repoPath` | string | 目标仓库路径 |
| `operation` | enum | 操作类型（COMMIT/PULL/PUSH/FETCH/MERGE/REBASE/CHECKOUT/STASH/RESET/REVERT/CLEAN/GC/CLONE/INIT/SCAN 等） |
| `command` | string | 实际执行的 git 命令或 JGit API 描述 |
| `args` | string (JSON) | 参数 JSON |
| `success` | boolean | 是否成功 |
| `durationMs` | long | 耗时（毫秒） |
| `errorMessage` | string / null | 错误信息（中文友好提示） |
| `taskId` | string / null | 关联异步任务 ID（异步操作时） |
| `createdAt` | datetime | 创建时间 |

### AuditLog（红线审计日志）

> **状态机**：追加写入，不可修改/不可删除（BR-31）。

| 字段 | 类型 | 说明 |
| ----- | ------ | ----- |
| `id` | string (UUID) | 主键 |
| `ruleCode` | string | 红线规则代码（如 `RED_FORCE_PUSH`/`RED_PROTECTED_BRANCH`/`RED_SENSITIVE_FILE`/`RED_REMOTE_WHITELIST`/`RED_NO_VERIFY`/`RED_RESET_HARD`/`RED_CLEAN_FDX`/`RED_AMEND_PUSHED`/`RED_REBASE_PUSHED`/`RED_FILTER_BRANCH`/`RED_LARGE_FILE`/`RED_LINE_TOGGLE`/`RED_DELETE_PROTECTED_BRANCH`） |
| `command` | string | 命中红线的命令 |
| `repoPath` | string | 仓库路径 |
| `branch` | string / null | 涉及分支 |
| `remoteUrl` | string / null | 涉及远程 URL |
| `action` | enum | 红线类型（BLOCK/CONFIRM） |
| `actionResult` | enum | 处理结果（BLOCKED/CONFIRMED/CANCELLED） |
| `detail` | string (JSON) | 详情（如命中敏感文件清单、保护分支名、文件大小） |
| `createdAt` | datetime | 创建时间 |

### AppSettings（应用设置）

> **状态机**：key-value 持久化，UI 修改即更新；内置默认值在首次启动时初始化。

| 字段 | 类型 | 说明 |
| ----- | ------ | ----- |
| `id` | string (UUID) | 主键 |
| `key` | string | 设置键，唯一 |
| `value` | string | 设置值（JSON 字符串承载复杂结构） |
| `category` | enum | 分类（RED_LINE/UI/GIT/EXTERNAL_TOOL/SSH/REPO_SCAN/RECENT） |
| `description` | string | 设置说明 |
| `updatedAt` | datetime | 更新时间 |

> 内置键清单：`red_line.enabled`/`red_line.protected_branches`/`red_line.remote_whitelist`/`red_line.sensitive_file_rules`/`red_line.large_file_threshold_mb`/`ui.theme`/`ui.language`/`ui.shortcuts`/`repo_scan.default_depth`/`recent_repo.max_keep`/`external.diff_tool`/`external.merge_tool`/`external.editor`/`ssh.default_key_path`。

### RepositoryMeta（仓库元信息缓存）

> **状态机**：随每次打开/刷新更新（currentBranch/headCommit/hasUncommittedChanges/lastSyncedAt），仓库不再存在时由清理任务删除。

| 字段 | 类型 | 说明 |
| ----- | ------ | ----- |
| `id` | string (UUID) | 主键 |
| `repoPath` | string | 仓库绝对路径，唯一 |
| `currentBranch` | string | 当前分支 |
| `headCommit` | string | HEAD 提交哈希 |
| `remoteUrl` | string | 主远程 URL |
| `hasUncommittedChanges` | boolean | 是否有未提交修改 |
| `lastSyncedAt` | datetime | 元信息最后刷新时间 |
| `createdAt` | datetime | 创建时间 |
| `updatedAt` | datetime | 更新时间 |

### TaskRecord（异步任务记录）

> **状态机**：`PENDING → RUNNING → (SUCCESS | FAILED | CANCELLED)`；终态不可回退。

| 字段 | 类型 | 说明 |
| ----- | ------ | ----- |
| `id` | string (UUID) | 主键 |
| `taskType` | enum | CLONE/PULL/PUSH/FETCH/MERGE/REBASE/MULTI_REPO_SCAN/GC/CHECKOUT/STASH |
| `repoPath` | string / null | 仓库路径（多仓库检索可为 null） |
| `status` | enum | PENDING/RUNNING/SUCCESS/FAILED/CANCELLED |
| `progress` | int | 进度 0-100 |
| `message` | string | 进度描述 |
| `output` | string | 命令输出/错误堆栈 |
| `cancellable` | boolean | 是否可取消 |
| `startedAt` | datetime / null | 开始时间 |
| `finishedAt` | datetime / null | 结束时间 |
| `createdAt` | datetime | 创建时间 |

> 取舍说明：StashEntry 不入库（Stash 列表实时从 Git 读取，避免与 Git 状态不一致）；`task_record` 无外键到 `recent_repo`，通过 `repo_path` 字符串软关联。
