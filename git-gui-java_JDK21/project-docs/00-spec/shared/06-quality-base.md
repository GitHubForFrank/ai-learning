# 质量保障 — 基座

> 本文件为共享基座。测试策略、门禁、日志规范的通用约定。
> 项目专属的监控指标、SLO 目标值见项目 `project-docs/00-spec/project/06-quality.md`。

---

## 测试策略

### 测试金字塔

```plaintext
        ┌─────────┐
        │ 手动 E2E │  ← 少量关键路径人工验证
        ├─────────┤
        │ UI 集成  │  ← TestFX JavaFX 控件交互
        ├─────────┤
        │ 单元测试 │  ← 大量覆盖业务逻辑
        └─────────┘
```

### BR 覆盖要求

每条 BR 规则**必须**被至少一条测试用例覆盖，且测试代码中需标注关联的 BR 编号：

```java
/**
 * [SPEC: 01-domain.md §BR-03]
 * 验证收藏路径重复时拒绝创建
 */
@Test
void shouldRejectWhenFavoritePathDuplicate() { ... }
```

BR 覆盖率目标：100%（每条 BR 至少 1 条测试）。BR 覆盖率检查建议作为 CI 门禁。

### 测试数据一致性

测试数据 MUST 与 spec 中的字段约束保持一致。当 BR 中字段约束变更时，相关测试数据属于 Spec 变更传播的第⑥步（见 [00-core-base.md §1.1]），必须同步更新。

### 测试范围

| 层级 | 范围 |
| ----- | ------ |
| 单元测试 | 应用服务 / 命令红线规则 / 适配器 / Repository / ViewModel |
| UI 集成测试 | FXML 加载 / 控件交互 / 对话框流程 / ViewModel 绑定 |
| 手动 E2E | 关键 Git 操作全流程（Clone→Commit→Push） |

---

## 单元测试规范

### 测试框架

- JUnit 5 + Mockito
- 无 Spring Boot Test（桌面应用无 Spring 上下文）

### 测试文件位置

测试目录按源码包结构镜像，放在 `src/test/java/` 下。

### 命名约定

- 测试类：`XxxTest.java`（单元）/ `XxxUiTest.java`（JavaFX UI）
- 测试方法：描述性命名（如 `shouldBlockForcePush`、`shouldThrowWhenInvalid`）

### 测试模式

```java
@Test
void shouldBlockForcePushOnProtectedBranch() {
    // Given
    RedLineContext ctx = RedLineContext.builder()
        .operation(OperationType.PUSH)
        .branch("main")
        .force(true)
        .build();

    // When
    RedLineResult result = redLineService.check(ctx);

    // Then
    assertThat(result.getAction()).isEqualTo(RedLineAction.BLOCK);
}
```

### Mock 策略

- 使用 `@Mock` 模拟依赖（Repository / JGit 适配器）
- 使用 `@InjectMocks` 注入被测对象
- 使用 `when(...).thenReturn(...)` 配置模拟行为
- 使用 `verify(...)` 验证交互

---

## JavaFX UI 测试规范

### 测试框架

- TestFX + JUnit 5

### 命名约定

- 测试文件：`XxxUiTest.java`（与 `src/` 目录平行）
- 验证 FXML 能正常加载、控件交互符合预期、ViewModel 绑定正确

### Mock 策略

- UI 测试中通过 Guice Module 注入 Mock 服务，避免真实 Git 操作
- 验证控件状态变化而非真实 Git 结果

---

## 代码质量门禁

| 门禁 | 命令 | 说明 |
| ----- | ------ | ----- |
| 编译 | `mvn compile` | 必须通过 |
| 测试 | `mvn test` | 必须通过（含 TestFX） |
| 构建 | `mvn clean package` | 编译 + 测试 + 打包 |

### CI/CD 流程

```plaintext
代码提交
  ↓
Maven 编译 + 测试
  ↓
打包 fat jar + 启动脚本（bat/sh）
  ↓
多平台产物归档（可选 jpackage 生成原生包）
  ↓
发布到 Release
```

---

## 日志规范

### 日志级别

| 级别 | 使用场景 | 示例 |
| ----- | --------- | ----- |
| `log.debug` | 开发环境调试信息 | Git 命令完整参数、JGit 内部状态 |
| `log.info` | 关键节点 | 仓库打开、操作开始/完成、CLI 兜底降级 |
| `log.warn` | 预期内异常 | 红线二次确认被取消、任务被取消 |
| `log.error` | 严重错误 | Git 操作失败、未预期异常 |

### 日志格式

- 每个异步任务生成唯一 taskId，贯穿该任务全部日志便于追踪
- 不记录敏感信息（Git 凭证、SSH 密钥原文、用户密码）

---

## 性能 SLO — 模板

| 指标 | 建议值 | 说明 |
| ------ | ------ | ------ |
| 操作响应 | <500ms | 不含网络/磁盘 IO 的本地处理 |
| 冷启动 | <3s | 含 IoC 注入 + DB 迁移 |
| 稳定性 | 常规操作无崩溃 | 异常有容错与友好提示 |

> 各项目可按自身业务特征调整 SLO 目标值。

---

## 禁用清单

- ❌ 跳过测试直接提交代码
- ❌ 测试中使用 `@Ignore` 长期跳过
- ❌ 核心模块测试覆盖率低于 80%，总覆盖率低于 60%
- ❌ 日志中记录敏感信息（凭证 / 密钥 / PII）
- ❌ 使用 `System.out.println` 代替日志框架
- ❌ BR 覆盖存在缺口（未达 100%）
- ❌ 命令红线相关 BR 无测试覆盖
