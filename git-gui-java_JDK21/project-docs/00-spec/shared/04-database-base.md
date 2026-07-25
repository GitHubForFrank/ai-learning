# 数据库规范 — 基座

> 本文件为共享基座。迁移脚本命名规则、执行方式、索引策略的通用规范。
> 具体表结构、迁移历史、数据库选型见项目 `project-docs/00-spec/project/04-database.md`。

---

## 迁移脚本规范

### 文件位置

```plaintext
app-backend/src/main/resources/db/migration/
```

### 命名规则

```plaintext
V{NN}__{description}.sql
```

- `V` 开头，后接递增序号（`V1`、`V2`、`V3`...），允许单数字或双数字命名
- 双下划线 `__` 分隔序号和描述
- 描述使用小写蛇形命名（如 `init_favorite`）
- **不可跳号、不可复用 V 号**

### 执行方式

迁移脚本通过以下方式按 V 号顺序执行：

1. **Flyway 自动执行**（推荐）：应用启动时由 Flyway 自动检测并按 V 号顺序执行未应用的迁移
2. **手动执行**：使用数据库客户端执行 SQL 脚本（调试场景）
3. **工具执行**：通过 MCP 工具或命令行执行

### 脚本头部注释

每个脚本必须以 Task 编号和 Spec 引用开头：

```sql
-- [TASK: Task001] [SPEC: project-docs/00-spec/project/04-database.md]
-- 脚本用途描述
```

### 表结构文档格式

每张表结构文档中必须标注**服务 BR**，格式：

```markdown
### table_name — 表说明

> 服务 BR：BR-xx（规则简述）、BR-yy（规则简述）

| 列 | 类型 | 约束 | 说明 |
| ... |
```

---

## 索引策略

- **查询频繁字段**：建立索引
- **时间范围查询**：`created_at`、`updated_at` 建立索引
- **外键字段**：全部建立索引
- **联合索引**：按需创建

---

## 禁用清单

- ❌ 自增主键（统一使用应用层 UUID TEXT）
- ❌ 裸 `CREATE TABLE`（必须带 `IF NOT EXISTS`）
- ❌ 跳号或复用 V 号
- ❌ 在迁移脚本中写业务逻辑（仅限 DDL/DML）
- ❌ 跳过迁移脚本直接修改数据库结构
- ❌ 修改已合入的迁移脚本（新增变更必须新建 V 号）
