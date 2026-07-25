# Java图形化Git客户端（类TortoiseGit）产品需求文档

## 一、项目概述

纯Java、跨平台（Windows/Mac/Linux）图形化Git客户端，功能对标TortoiseGit。采用「独立启动 + 手动选择Git工作目录」模式，无需系统右键菜单依赖（与TortoiseGit的原生交互模式有意偏离，详见第六章）。

**UI方案**：纯Java原生GUI窗口（JavaFX）。除主窗口外，每个操作（Commit、Pull、Push、Switch、Merge等）以独立模态对话框弹出，交互方式与TortoiseGit一致。无需浏览器、无需WebView。

## 二、技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| 语言 | Java 21 | JDK 21 |
| 框架 | Google Guice | 轻量级 IoC 容器（纯依赖注入，无 Web/自动配置），启动快、jar 体积小，契合桌面应用与个人项目轻量化诉求；服务通过 Module 绑定 |
| Git操作 | JGit（主） + 本地Git命令行（兜底） | JGit纯Java实现、进度可控、无进程开销；LFS、Hook、复杂Rebase等JGit支持不足的场景回退命令行。命令行调用强制 `UTF-8` 编码并设置 `core.quotepath=false`，避免Windows中文路径乱码 |
| 数据库 | SQLite（sqlite-jdbc + Flyway） | 本地存储（仓库历史、操作日志、用户配置）；Flyway管理schema迁移 |
| GUI | JavaFX 21 | 原生Java桌面窗口，独立对话框；支持CSS主题（含深色模式）、Scene Graph、动画 |
| 构建 | Maven | 打包为jar，配合bat/sh脚本启动；JavaFX通过模块化或jlink打包 |

> **技术选型说明**：早期版本曾考虑 Swing/JavaFX 二选一含糊处理与 Spring Boot，经评审后明确：GUI 统一采用 JavaFX（现代外观、深色模式友好）；后端采用 Google Guice 轻量 IoC 容器替代 Spring Boot，最大化轻量化。

## 三、部署与启动

- 打包为jar，同目录放置bat（Windows）/sh（Mac/Linux）启动脚本
- 启动脚本拉起jar，主窗口关闭时关闭 Guice 注入器并释放资源
- 单实例运行（锁文件检测，不占用网络端口）
- 启动时检测本地Git（命令行兜底场景需要），未安装则提示并退出；JGit 可独立工作但 LFS/Hook 等场景受限

## 四、核心功能

### 4.1 仓库创建与获取

- **Git Clone**：弹出克隆对话框，输入远程仓库URL、本地目标目录，支持选择分支/标签，开始克隆。选项：
  - **Putty Key / SSH Key**：选择SSH密钥文件
  - **Depth**：浅克隆深度（`--depth`）
  - **Bare**：克隆为裸仓库（`--bare`）
  - **Sparse Checkout**：稀疏检出
  - **Subdirectory**：克隆到子目录
- **Git Init**：在指定目录初始化新的Git仓库
- **打开已有仓库**：手动选择本地Git仓库目录打开，非Git目录给予友好提示

### 4.2 启动与目录选择

- 启动后弹出工作目录选择窗口，可手动选择Git仓库目录，或输入URL克隆新仓库
- 记忆最近打开的工作目录，支持一键快速进入

#### 4.2.1 多仓库自动检索

- 打开某个目录后，自动遍历检索该目录下所有 Git 仓库（含子目录，默认深度 3 层，可配置）
- 检索结果以列表/树形展示每个仓库的：路径、当前分支、是否有未提交修改、远程URL
- 检索异步执行并显示进度，可取消；大目录检索不阻塞主界面（遵循 4.19 异步任务体系）
- 检索结果可按路径/分支/修改状态排序与筛选
- 对检索到的仓库可直接打开、在新窗口打开、加入收藏
- 检索只读 `.git` 目录存在性，不修改任何仓库状态

#### 4.2.2 收藏功能

- 对常用仓库标记收藏（置顶展示），方便多仓库场景快速定位
- 收藏列表独立展示，支持排序与分组（如按项目/团队）
- 收藏项可设置别名（便于识别，如"前端主仓库"）
- 收藏数据存储于 SQLite，跨会话保留

### 4.3 仓库状态检查

- 主窗口展示当前仓库工作区状态（对标TortoiseGit "Check for modifications" 对话框）
- 展示已修改、新增、删除、未跟踪、冲突文件，图标/颜色区分
- 可勾选"显示未跟踪文件""显示已忽略文件"等过滤选项
- 双击文件弹出Diff对比窗口
- 文件列表行支持右键菜单（详见 4.13 文件上下文菜单）
- 支持 **文件锁（Lock/Unlock）** 状态展示（LFS lock 或本地锁机制）

### 4.4 提交（Commit）

弹出Commit对话框，展示变更文件列表，每行：复选框、文件路径、状态、新增行数、删除行数。

- **复选框勾选提交**：勾选/取消勾选文件，支持「全选」「全不选」「按状态筛选」
- 提交信息输入框，支持：
  - **最近提交信息下拉**：从历史中选取
  - **提交信息模板**（commit template）：按 `.git/COMMIT_TEMPLATE` 或配置加载
- 上方选项：
  - **Amend Last Commit**：修补上次提交
  - **Set author date**：设置作者时间
  - **Set author**：设置作者
  - **Sign commit**：GPG签名提交
- 文件列表行右键菜单：Open / Restore / Delete / Add / Ignore / Resolve / Edit conflicts（详见 4.13）
- **底部操作按钮为下拉菜单**，未勾选文件时置灰，勾选后激活，四个选项：
  - **Commit**：仅提交到本地仓库
  - **ReCommit**：使用上次提交信息提交，不弹出编辑框
  - **Commit & Push**：提交并立即推送到远程
  - **Commit & Push with tags**：提交并推送，同时包含标签

### 4.5 远程同步

#### 4.5.1 Pull（拉取）

弹出Pull对话框：
- 选择Remote、选择远程分支
- **AutoStash**：拉取前自动暂存本地修改
- **Rebase instead of Merge**：变基而非合并
- **Fetch Tags**：拉取时一并获取标签
- **All branches**：拉取所有分支
- **Auto-update submodules**：递归更新子模块
- **Dry run**：预演，不实际修改

#### 4.5.2 Push（推送）

弹出Push对话框：
- 选择Remote、选择目标分支
- **Set upstream/track**：设置上游跟踪
- **Force with lease**：安全检查的强制推送（比裸 `-f` 安全）
- **Include Tags**：推送时包含标签
- **Push all branches**：推送所有本地分支
- **Push all tags**：推送所有标签
- **Push to URL**：临时目标URL，不修改本地配置
- **Recursive on submodules**：递归推送子模块
- **删除远程分支**：通过推送 `:branch` 实现远程分支删除

#### 4.5.3 Fetch（获取）

弹出Fetch对话框：
- 选择Remote、选择分支
- **Prune**：清理本地已删除的远程分支引用
- **Fetch Tags**：获取标签
- **All branches**：获取所有分支

#### 4.5.4 Git Sync（同步）

合并 Pull + Push 到一个对话框，一键完成拉取和推送。

### 4.6 分支管理

#### 4.6.1 Switch/Checkout（切换/检出）

弹出Switch/Checkout对话框：

**Switch To**（三选一）：
- **Branch**（下拉选择本地/远程分支）
- **Tag**（下拉选择标签）
- **Commit**（输入框 + 「...」选择提交哈希）

**Option**（复选框）：
- **Create New Branch**：基于所选目标创建新分支并切换
- **Overwrite working tree changes (force)**：强制覆盖（`-f`）
- **Merge**：合并工作区修改到目标分支
- **Track**：切换后将当前分支的上游跟踪设置为目标远程分支（仅对带远程跟踪的分支生效）
- **Override branch if exists**：目标分支已存在时强制覆盖
- **Detach**：以游离HEAD方式检出（`--detach`）

#### 4.6.2 Create Branch（新建分支）

弹出新建分支对话框，基于当前分支或指定提交创建新分支，选项：
- **Track**：设置跟踪的远程分支
- **Force**：分支已存在时强制覆盖
- **Switch to new branch**：创建后立即切换

#### 4.6.3 Merge（合并分支）

弹出合并分支对话框：
- 选择要合并的源分支
- **No Fast Forward**：强制生成合并提交
- **Squash**：压缩合并，不生成合并提交
- **No Commit**：合并后不自动提交
- **Strategy**：合并策略（recursive / octopus / ours / theirs）
- **Commit message**：自定义合并提交信息
- **Log**：合并后显示日志

#### 4.6.4 Delete / Rename（删除/重命名分支）

- 删除本地分支、删除远程分支
- 重命名本地分支

### 4.7 标签管理

- **Create Tag**：弹出新建标签对话框，选择目标提交，填写标签名和备注（支持轻量标签和附注标签），选项：
  - **Sign with GPG**：GPG签名标签
  - **Force**：覆盖已存在的同名标签
- **Delete Tag**：删除本地/远程标签
- **Push Tag**：推送标签到远程
- 标签列表展示

### 4.8 Cherry Pick（遴选）

- 在日志中右键某个提交，选择 Cherry Pick，将该提交的变更应用到当前分支
- 支持连续遴选多个提交

### 4.9 暂存与撤销

> **术语说明**：本节"Revert"指**文件级还原**（丢弃工作区修改）；4.10 的 "Revert Commit" 指**提交级反向提交**。二者语义不同，对应 TortoiseGit 中文版的"还原"与"撤销变更"。

#### 4.9.1 Stash（暂存）

- **Stash Save**：暂存当前工作区修改，填写描述信息。选项：
  - **Keep index**：保留暂存区不暂存
  - **Include untracked**：包含未跟踪文件（`-u`）
- **Stash Pop**：弹出最近一次暂存，恢复修改并删除记录
- **Stash Apply**：应用暂存但不删除记录
- **Stash List**：查看所有暂存记录，支持选择指定暂存Drop或Apply
- **Stash Drop**：删除指定暂存记录
- **Stash Branch**：基于某暂存创建新分支

#### 4.9.2 Revert（还原文件，丢弃本地修改）

右键文件 → Revert：丢弃该文件的本地修改，恢复为 HEAD 版本（`git restore <file>`，对应TortoiseGit文件右键菜单中的 "Revert / 还原"）。

#### 4.9.3 Undo Add（撤销暂存）

将已 `git add` 的文件从暂存区撤回到工作区（`git restore --staged <file>`），文件修改保留，只是取消暂存状态。

#### 4.9.4 Clean up（清理未跟踪文件）

弹出对话框，列出所有未被跟踪的文件和目录：
- **Preview / Dry run**（`git clean -n`）：预览将删除的内容，不实际执行
- 勾选后删除（`git clean -fd`），支持按目录层级勾选
- 避免误删，删除前二次确认

#### 4.9.5 Git GC（仓库垃圾回收）

- 执行 `git gc` 清理仓库、压缩对象
- 与 Clean up 拆分为独立动作，避免混淆

### 4.10 日志与回溯

- 主窗口日志区域展示完整提交历史（提交哈希、提交人、时间、分支/标签引用、备注）
- 左侧显示提交版本图（Revision graph），右侧显示变更文件列表和Diff
- 筛选维度：
  - 按日期范围、作者、提交信息关键词搜索
  - 范围切换：**All branches** / **Remotes** / **Stash** / **Bisect**
  - **Show first parent only**：仅显示第一父提交
  - **Working tree changes**：仅显示工作区相关提交
- 双栏 **Compare revisions**：选中两个提交对比差异
- 右键提交记录支持：
  - **Revert Commit**：创建反向提交撤销该次提交（`git revert`）
  - **Reset to this commit**：重置当前分支到该提交（支持Soft/Mixed/Hard/Keep）
  - **Cherry Pick**：遴选该提交
  - **Create Branch/Tag at this commit**：基于该提交创建分支或标签
  - **Compare with working tree**：对比该提交与当前工作区差异
  - **Blame from this revision**：从该提交开始追溯文件 blame
- **Show RefLog**：查看引用日志，支持恢复误删的分支或提交

### 4.11 Rebase（变基）

> 从原 4.15 高级功能中独立成章。TortoiseGit 的 Rebase 对话框是交互式变基，复杂度等同 Commit/Merge，需单独设计。

弹出Rebase对话框：

- **Upstream**：选择要变基到的目标分支/提交
- **Branch**：当前分支（默认）或指定分支
- 操作模式：
  - **Interactive**（交互式变基）：列出待变基的提交列表，每行可设置动作：
    - pick / reword / edit / squash / fixup / drop
  - **Apply silently**：非交互式直接变基
- 选项：
  - **Preserve merges**（`--rebase-merges`）：保留合并提交结构
  - **Autostash**：变基前自动暂存
  - **Committer date is author date**：保持提交时间
- 冲突处理：逐提交解决冲突后 **Continue**（`--continue`）/ **Skip**（`--skip`）/ **Abort**（`--abort`）
- 变基进度展示与可取消（详见 4.19）

### 4.12 文件操作

- **Add**：添加文件到暂存区
- **Delete**：删除文件，支持 **keep local** 保留本地文件（仅删除暂存）
- **Rename**：重命名文件
- **Ignore**：添加到 `.gitignore`（支持递归、仅当前目录、按扩展名）
- **Lock / Unlock**：文件锁（LFS lock 或本地锁机制），防止团队并发修改冲突
- **Blame**：逐行查看文件每一行的最后修改者和修改时间，点击具体行可跳转到对应提交
- **Resolve**：标记冲突文件已解决
- **Export**：导出工作树到指定目录（不含 `.git`，对标TortoiseGit Export）
- **Copy URL to clipboard**：复制仓库内相对路径 / 远程URL
- **Show in Explorer/Finder**：在系统资源管理器中定位该文件
- **Properties**：查看文件Git属性（`.gitattributes`、EOL、MIME、最后修改提交）

### 4.13 文件上下文菜单

> 文件右键菜单是 TortoiseGit 的交互核心，在 Commit 对话框、Check for modifications、Repo-browser 等多处复用，统一在此定义。

文件上下文菜单项：
- **Open**：以系统默认程序打开
- **Open with**：选择程序打开
- **Diff**：与HEAD版本对比（调用Diff工具，详见4.18.3）
- **Edit conflicts**：冲突文件进入合并编辑器
- **Resolve using Mine / Theirs**：冲突解决
- **Add** / **Undo Add** / **Delete** / **Rename** / **Ignore**
- **Revert**：还原文件（见4.9.2）
- **Lock / Unlock**
- **Blame**
- **Copy URL to clipboard**
- **Show in Explorer/Finder**
- **Properties**

菜单项根据文件状态动态启用/禁用（如冲突文件才显示 Edit conflicts）。

### 4.14 远程仓库配置

- 弹出Remote配置对话框，展示当前仓库所有Remote名称及对应URL
- 新增/修改/删除/重命名Remote
- 修改Remote URL、Push URL
- **Default push remote / Default pull remote**：设置默认推送/拉取的Remote
- **Putty Key 关联**：为Remote关联Putty密钥
- 点击「确定」通过 `git remote set-url` 直接修改本地仓库配置
- SSH/HTTPS协议适配，支持账号密码及密钥认证

### 4.15 冲突解决

- 冲突文件在主窗口中以特殊图标标识
- 弹出冲突编辑对话框，支持三种方式：
  - **Edit Conflicts**：打开合并编辑器（优先调用外部Merge工具，见4.18.3；未配置时使用内置三栏合并器：本地/远程/合并结果）
  - **Resolve using Mine**：全部保留本地版本
  - **Resolve using Theirs**：全部保留远程版本
- 解决后标记为已解决，支持一键提交
- 外部Merge工具配置通过 Settings 统一管理（见4.18.3）

### 4.16 补丁（Patch）

- **Create Patch Serial**：将选中的提交生成补丁文件（`.patch`）
- **Apply Patch Serial**：应用补丁文件到当前工作区
- **Format Patch**：将提交格式化为邮件补丁

### 4.17 高级功能

- **Bisect start**：二分查找定位问题提交
- **Submodule**：添加子模块、更新子模块、同步子模块、递归操作
- **Repo-browser**：仓库浏览器对话框，以文件树形式浏览仓库任意版本
- **Browse References**：浏览所有引用（分支、标签、HEAD、stash等）
- **Worktree**（`git worktree`）：添加/列出/删除工作树，支持多工作树并行开发
- **LFS**（大文件存储）：`git lfs install/track/pull/push`，大文件状态展示
- **Hook 管理**：可视化编辑 `.git/hooks`（pre-commit / pre-push / commit-msg 等）
- **`.gitattributes` 编辑**：行尾、二进制识别、diff/merge 驱动配置
- **GPG 签名配置**：提交与标签签名密钥管理

### 4.18 设置（Settings）

> 从原 4.16 辅助功能中独立展开。TortoiseGit 的 Settings 是分层级的配置中心。

#### 4.18.1 Git 配置

- 全局配置（`~/.gitconfig`）与仓库配置（`.git/config`）分级编辑
- **per-repo 配置**：每个仓库独立的 `user.name` / `user.email`
- 常用项：core.autocrlf、core.quotepath、pull.rebase 等

#### 4.18.2 凭证管理

- 集成 credential helper（osxkeychain / wincred / manager-core）
- 凭证存储、查看、清除

#### 4.18.3 外部工具关联

- **Diff Viewer**：外部Diff工具（Beyond Compare / KDiff3 / VSCode 等）
- **Merge Tool**：外部合并工具，冲突解决时调用
- **External Editor**：外部文本编辑器
- 调用参数模板支持 `$BASE` / `$LOCAL` / `$REMOTE` / `$MERGED` 占位符

#### 4.18.4 SSH 配置

- SSH 密钥管理（生成、导入、选择）
- SSH agent 集成
- Putty Key 关联

#### 4.18.5 其他

- 主题（浅色/深色/跟随系统）
- 语言（中/英）
- 快捷键自定义
- 最近仓库列表管理

### 4.19 异步任务与进度

> 长耗时操作（Clone/Pull/Push/Fetch/Rebase/Merge）需统一的异步任务体系。

- **进度对话框**：实时显示命令输出与进度百分比（基于JGit `ProgressMonitor` 或命令行输出解析）
- **可取消**：提供取消按钮，JGit 通过 `ProgressMonitor.cancel`，命令行通过终止进程
- **后台执行**：长任务可最小化到状态栏，不阻塞主窗口其他操作
- **任务队列**：多任务串行/并发管理，避免对同一仓库并发操作冲突
- **失败重试与日志**：任务结果记录到SQLite操作日志，异常时友好中文提示

### 4.20 辅助功能

- 多仓库切换，无需重启
- SQLite记录操作日志，查看操作历史
- 操作异常时友好中文提示
- 窗口自适应（缩放、最大化、最小化）

## 五、UI架构

- **主窗口**：顶部菜单栏 → 左侧仓库/分支/标签导航树 → 中间内容区（文件状态列表 / 日志+Diff） → 底部状态栏
- **操作对话框**：每个Git操作（Commit、Pull、Push、Switch、Merge、Clone、Remote配置、Settings等）以独立模态对话框弹出，与TortoiseGit交互方式一致
- 全部使用JavaFX组件，无浏览器/WebView/HTML参与

## 六、与 TortoiseGit 的差异说明

> 本产品对标 TortoiseGit 的功能集，但交互模式有以下有意偏离，需在评审中明确：

| 差异点 | TortoiseGit | 本产品 | 原因 |
|--------|-------------|--------|------|
| 入口方式 | 资源管理器右键菜单集成 | 独立启动 + 手动选择目录 | 跨平台实现成本，避免系统右键菜单依赖 |
| 是否有常驻主窗口 | 无，每个操作独立对话框 | 有主窗口（导航树+内容区） | 独立启动模式下的工作区聚合 |
| Icon Overlay | 系统资源管理器图标叠加 | 不支持 | 跨平台实现成本，无右键菜单集成 |
| 右键菜单自定义 | 支持配置菜单项显隐 | 暂不支持（后续迭代） | 优先实现核心功能 |

## 七、非功能需求

- **Git依赖**：JGit可独立工作；命令行兜底场景需本地Git，启动时检测
- **单实例**：同一时间只允许运行一个实例（锁文件检测）
- **跨平台**：全平台功能、窗口样式统一
- **性能**：大仓库文件扫描、Diff对比、日志加载无明显卡顿
- **稳定性**：常规操作无崩溃、无数据丢失，异常有容错处理
- **国际化**：至少支持中/英文切换
- **主题**：支持浅色/深色/跟随系统主题
- **快捷键**：Commit/Pull/Push 等高频操作提供默认快捷键，可自定义
- **凭证安全**：集成系统 credential helper，不明文存储密码
- **编码处理**：Git输出统一UTF-8，Windows下设置 `core.quotepath=false`，避免中文路径乱码

## 八、命令红线与安全约束

> 为防止误用高风险 Git 操作触发公司安全审计、导致账号被封或代码外泄，本产品对以下命令设置红线管控。命中红线的操作将被**拦截阻断**或**强制二次确认**，并记录到操作日志。本机制对 4.5 远程同步、4.9 暂存与撤销、4.10 日志回溯、4.11 Rebase 等章节涉及的危险操作统一生效。

### 8.1 阻断类（默认禁止，需显式开启开关才能执行）

| 命令/操作 | 风险 | 处理 |
|-----------|------|------|
| `git push --force` / `-f`（裸强制推送） | 覆盖远程历史，可能删除他人提交 | 阻断，引导改用 `--force-with-lease` |
| 向保护分支（main/master/develop/release/*）force push | 篡改主干历史 | 阻断 |
| `git push` 删除保护分支（push `:branch`） | 误删主干 | 阻断 |
| 推送含敏感信息的文件（`.env`、`credentials`、`*.pem`、`*.key`、`id_rsa`、`.npmrc` 等） | 凭据外泄 | 阻断，提示移除并加入 `.gitignore` |
| 推送到非授权远程（URL 不在白名单） | 代码外泄到外部 | 阻断 |
| `--no-verify` 跳过 hook 提交/推送 | 绕过公司提交校验 | 阻断 |

### 8.2 二次确认类（执行前弹窗确认）

| 命令/操作 | 风险 | 处理 |
|-----------|------|------|
| `git reset --hard` | 丢失本地未提交修改 | 二次确认，提示将丢失的文件清单 |
| `git clean -fdx`（含忽略文件） | 删除 `.gitignore` 内的配置/密钥 | 二次确认 |
| `git commit --amend` 已推送的提交 | 篡改共享历史 | 二次确认 |
| `git rebase` 已推送的提交 | 篡改共享历史 | 二次确认 |
| `git filter-branch` / `git filter-repo` | 重写历史 | 二次确认 |
| 推送超大文件（> 阈值，如 50MB，非 LFS） | 仓库膨胀 | 二次确认，建议走 LFS |

### 8.3 配置与管理

- **保护分支清单**：在 Settings 中配置（支持通配符，如 `release/*`）
- **远程白名单**：配置允许推送的远程 URL 域名清单
- **敏感文件规则**：内置常见敏感文件名规则，支持自定义正则
- **红线开关**：总体可关闭（不推荐），关闭时所有阻断转为二次确认
- **审计日志**：所有命中红线的操作记录到 SQLite，含时间、命令、仓库、处理结果
