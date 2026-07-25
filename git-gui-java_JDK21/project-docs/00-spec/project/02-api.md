# 服务契约 — git-gui 项目专属

> **定位**：UI 层 ↔ 应用层 / 领域层的内部 Java 接口契约（方法签名 + 契约 + 异常处理 + 关联 BR）。
>
> 本文件**非 HTTP、非 REST**，定义的是 `domain/service/` 包中的 Java 接口，UI Controller 通过 Guice `@Inject` 注入调用。
>
> 响应返回领域 Model 或基本类型；异常统一抛 `GitGuiException`（含 errorCode 枚举 + 中文 message），由 `GlobalExceptionHandler` 转 UI 友好提示。

---

## 通用约定

- 所有服务接口定义在 `domain/service/`，实现在 `application/service/`
- UI 层通过 Guice 注入接口，**禁止**直接访问 `infrastructure/` 层
- 写操作（Git 修改类）执行前由 `CommandInterceptor` 自动调用命令红线校验（参见 [03-backend.md §命令红线拦截器]）
- 长耗时写操作经 `AsyncTaskService` 异步执行，返回 `TaskHandle`
- 日期时间：ISO-8601 格式

---

## RepositoryService（仓库服务）

> 关联 BR：BR-01、BR-02、BR-05、BR-41

```java
public interface RepositoryService {

    // 打开已有仓库，非 Git 目录抛 REPO_NOT_GIT
    RepositoryMeta openRepository(String repoPath);

    // 克隆，异步可取消
    TaskHandle clone(CloneRequest req, ProgressCallback cb);

    // 初始化仓库（git init）
    void initRepository(String dir, boolean bare);

    // 多仓库检索（异步可取消）
    TaskHandle scanMultiRepo(String rootDir, int depth, ProgressCallback cb);

    // 刷新仓库元信息
    RepositoryMeta refreshMeta(String repoPath);

    // 获取多仓库检索结果
    List<RepositoryMeta> getScanResults();
}
```

**异常**：`REPO_NOT_GIT`、`SCAN_CANCELED`、`GIT_EXECUTION_FAILED`

---

## GitOperationService（Git 操作服务，核心写操作）

> 关联 BR：BR-06~BR-25。所有写方法在执行前由拦截器自动调用 `CommandRedLineService.check()`。
>
> **已实现方法如下；merge/rebase/cherryPick/reset/revert/checkout/branch/tag/stash/patch/bisect/submodule/worktree 等方法为后续迭代规划，尚未实现。**

```java
public interface GitOperationService {

    // 提交（BR-06/BR-07），返回提交哈希
    String commit(CommitRequest req);

    // 推送（异步，BR-09/BR-10）
    TaskHandle push(PushRequest req, ProgressCallback cb);

    // 拉取（异步）
    TaskHandle pull(PullRequest req, ProgressCallback cb);

    // 获取（异步）
    TaskHandle fetch(String repoPath, String remote, String branch, boolean prune, ProgressCallback cb);

    // 同步（Pull + Push 一键完成，异步）
    TaskHandle sync(String repoPath, String remote, String branch, ProgressCallback cb);
}
```

> **规划中（未实现）**：`merge`、`rebase`、`cherryPick`、`reset`、`revertCommit`、`checkout`、`createBranch`、`deleteBranch`、`renameBranch`、`createTag`、`deleteTag`、`pushTag`、`stashSave`、`stashPop`、`stashApply`、`stashDrop`、`stashBranch`、`revertFile`、`undoAdd`、`cleanUp`、`gc`、`createPatch`、`applyPatch`、`formatPatch`、`bisectStart`、`bisectGood`、`bisectBad`、`bisectReset`、`submoduleAdd`、`submoduleUpdate`、`submoduleSync`、`worktreeAdd`、`worktreeList`、`worktreeRemove`。

**异常**：`RED_LINE_BLOCKED`、`GIT_EXECUTION_FAILED`、`WORKTREE_DIRTY`、`VALIDATION_FAILED`

---

## StatusService（状态查询服务）

> 关联 BR：BR-18、BR-24
>
> **已实现方法如下；`getBlame`/`listStash`/`listReferences` 为后续迭代规划，尚未实现。**

```java
public interface StatusService {

    // 获取工作区文件状态列表
    List<FileStatus> getStatus(String repoPath, boolean showUntracked, boolean showIgnored);

    // 获取提交日志（BR-18 分页加载，page 从 1 开始）
    List<LogEntry> getLog(String repoPath, int page, int pageSize);

    // 获取文件 Diff
    DiffResult getDiff(String repoPath, String path, String oldRev, String newRev);
}
```

> **规划中（未实现）**：`getBlame`、`listStash`、`listReferences`。

**异常**：`GIT_EXECUTION_FAILED`

---

## FavoriteService（收藏服务）

> 关联 BR：BR-03、BR-04

```java
public interface FavoriteService {

    Favorite add(String repoPath, String alias, String group);  // BR-03 唯一性
    void remove(String id);
    void togglePinned(String id, boolean pinned);               // BR-04 置顶
    void update(String id, String alias, String group);
    List<Favorite> list();                                       // 按 pinned 降序、sortOrder 升序
    List<Favorite> listByGroup(String group);
}
```

> **规划中（未实现）**：`reorder(List<String> orderedIds)`。

**异常**：`VALIDATION_FAILED`、`DUPLICATE_FAVORITE`、`NOT_FOUND`

---

## RecentRepoService（最近仓库服务）

> 关联 BR：BR-05

```java
public interface RecentRepoService {

    void recordOpen(String repoPath, String lastBranch); // 存在则更新 + 淘汰最旧记录
    List<RecentRepo> list();
    void remove(String id);
    void clear();
}
```

**异常**：—

---

## SettingsService（设置服务）

> 关联 BR：BR-27、BR-28、BR-29、BR-30、BR-32、BR-37、BR-38、BR-39

```java
public interface SettingsService {

    String get(String key);
    void set(String key, String value);

    List<String> getProtectedBranches();                 // BR-27
    void setProtectedBranches(List<String> branches);
    List<String> getRemoteWhitelist();                   // BR-28
    void setRemoteWhitelist(List<String> whitelist);
    List<SensitiveFileRule> getSensitiveFileRules();     // BR-32
    int getLargeFileThresholdMb();                       // BR-29
    boolean isRedLineEnabled();                          // BR-30
    void setRedLineEnabled(boolean enabled);             // BR-30，切换写入 audit_log
}
```

> **规划中（未实现）**：`<T> T getAs(String key, Class<T> type)`、`ExternalToolConfig getExternalTools()`、`void setGitConfig(String repoPath, String key, String value, boolean global)`（BR-37）。外部工具配置当前通过 `get("external.diff_tool")` 等键值读取。

**异常**：`VALIDATION_FAILED`

---

## CommandRedLineService（命令红线服务，闭环核心）

> 关联 BR：BR-26~BR-32

```java
public interface CommandRedLineService {

    // 返回 PASS / CONFIRM / BLOCK + ruleCode + message
    RedLineResult check(RedLineContext ctx);

    // BR-31 持久化审计日志
    void recordAudit(AuditLog auditLog);
    List<AuditLog> queryAudit(String repoPath);          // repoPath 为 null 表示全部
}
```

> `RedLineContext` 含：operation 枚举、command、repoPath、branch、remoteUrl、stagedFiles、targetCommit、isPushed 等。
>
> 返回 `PASS` 放行；`CONFIRM` 由 UI 弹窗，用户确认后回调执行；`BLOCK` 直接拒绝并提示安全等价命令（如「请改用 `--force-with-lease`」）。

**异常**：—

---

## AsyncTaskService（异步任务服务）

> 关联 BR：BR-33~BR-36

```java
public interface AsyncTaskService {

    // 提交写任务（同仓库写串行，BR-34）
    TaskHandle submitWrite(String repoPath, TaskType taskType, Runnable task, ProgressCallback cb);

    // 提交读任务（并发，BR-34）
    TaskHandle submitRead(String repoPath, TaskType taskType, Runnable task, ProgressCallback cb);

    // 取消任务（BR-33）
    void cancel(String taskId);

    TaskRecord get(String taskId);
    List<TaskRecord> listActive(String repoPath);

    // 历史任务（BR-35，从持久化仓储查询）
    List<TaskRecord> listHistory(String repoPath);

    // 注册任务完成事件处理器（BR-36，UI 通过此刷新）
    void onTaskFinished(Consumer<String> handler);
}
```

> `TaskHandle` 提供 `onProgress` / `onSuccess` / `onFailure` 回调；写操作走 `TaskManager` 同仓库串行队列（BR-34），读操作走并发池。

**异常**：`TASK_CANCELED`、`TASK_QUEUE_FULL`

---

## ConflictResolveService（冲突解决服务）

> 关联 BR：BR-24

```java
public interface ConflictResolveService {

    void resolveMine(String repoPath, List<String> paths);
    void resolveTheirs(String repoPath, List<String> paths);
    void launchMergeTool(String repoPath, String path);     // 调用外部 Merge 工具
    void markResolved(String repoPath, List<String> paths);
}
```

> **规划中（未实现）**：`List<ConflictFile> listConflicts(String repoPath)`。

**异常**：`GIT_EXECUTION_FAILED`、`MERGE_TOOL_NOT_CONFIGURED`

---

## RemoteConfigService（远程配置服务）

> 关联 BR：BR-09

```java
public interface RemoteConfigService {

    List<RemoteConfig> list(String repoPath);
    void add(String repoPath, String name, String url);
    void update(String repoPath, String name, String url);
    void delete(String repoPath, String name);
    void rename(String repoPath, String oldName, String newName);
}
```

> **规划中（未实现）**：`setDefaultPushRemote`、`setDefaultPullRemote`。

**异常**：`VALIDATION_FAILED`、`NOT_FOUND`

---

## OperationLogService（操作日志服务）

> 关联 BR：BR-35

```java
public interface OperationLogService {

    // 记录操作日志（BR-35）
    void record(String repoPath, OperationType operation, String command,
                boolean success, long durationMs, String errorMessage, String taskId);

    // 查询操作日志（repoPath 为 null 表示全部）
    List<OperationLog> query(String repoPath);
}
```

**异常**：—

---

## 错误码注册表

| 错误代码 | 场景 | 关联 BR |
| ---- | ---- | ---- |
| `VALIDATION_FAILED` | 参数校验失败 | BR-03、BR-06、BR-07、BR-21 |
| `REPO_NOT_GIT` | 选定目录非 Git 仓库 | BR-41 |
| `RED_LINE_BLOCKED` | 命中阻断类红线 | BR-26~BR-28、BR-32 |
| `RED_LINE_CONFIRM_CANCELED` | 二次确认被用户取消 | BR-29、BR-30 |
| `WORKTREE_DIRTY` | 工作区不干净且操作不允许 | BR-12 |
| `GIT_EXECUTION_FAILED` | JGit/CLI 执行失败 | BE-02~BE-09 |
| `TASK_CANCELED` | 异步任务被取消 | BR-33 |
| `TASK_QUEUE_FULL` | 同仓库写任务排队超限 | BR-34 |
| `MERGE_TOOL_NOT_CONFIGURED` | 未配置外部 Merge 工具 | — |
| `DUPLICATE_FAVORITE` | 收藏路径重复 | BR-03 |
| `NOT_FOUND` | 资源不存在 | — |

---

## 禁用清单

- ❌ UI 层直接访问 `infrastructure/` 层（必须经服务接口）
- ❌ 服务接口暴露 JGit / CLI 内部类型（必须返回领域 Model）
- ❌ 在服务接口中返回 HTTP 概念（无 ResponseEntity、无 HTTP 状态码）
- ❌ 错误信息暴露技术细节（栈 / SQL / 内网路径）给用户
