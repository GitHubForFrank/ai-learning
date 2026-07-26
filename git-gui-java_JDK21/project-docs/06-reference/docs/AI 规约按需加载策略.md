# AI 规约按需加载策略

> **来源声明**：本策略为通用方法论参考，示例内容来自 LLM Proxy 项目。git-gui 项目的实际技术栈为 JavaFX 21 + Google Guice + Git CLI + SQLite，实际配置请以 `.trae/rules/` 为准。

> 解决每次任务全量加载所有 spec 和 common rule 导致的 token 浪费问题。
> 通过"规则入口改造 + 场景触发器"实现按需加载，而非每次都全量加载。

---

## 1. 问题背景

### 1.1 现状

当前 `.trae/rules/general.md` 设置了 `alwaysApply: true`，强制每次任务预加载：

| 文件 | 预估 Token | 加载方式 |
|------|-----------|---------|
| `.trae/common/SOLID.md` | ~8K | 强制预加载 |
| `.trae/common/karpathy.md` | ~3K | 强制预加载 |
| `.trae/common/markdown.md` | ~2K | 强制预加载 |
| `spec/00-core.md` | ~5K | 人工读取 |
| `spec/01-domain.md` ~ `07-ops.md` | ~30K | 人工读取（经常全量） |

**全量加载总计约 48K tokens**，而大多数任务只需要其中一小部分。

### 1.2 目标

- 将每次任务的规约加载量从 ~48K 降低到 **5K-15K**
- 保持规则执行的准确性和一致性
- 不增加人工干预成本

---

## 2. 落地方案：修改规则入口（推荐）

### 2.1 为什么不做成 Skill？

| 维度 | Skill 方案 | 修改规则入口方案 |
|------|-----------|----------------|
| 触发方式 | 用户主动调用 | 自动触发 |
| 适用场景 | 特定功能（如 pdf、xlsx） | 全局规则加载 |
| 与现有架构兼容性 | 需要额外配置 | 直接复用 `.trae/rules/` 机制 |
| 维护成本 | 高（需维护 skill 描述） | 低（仅修改 markdown） |

**结论**：规约加载是全局行为，不适合做成 Skill。Skill 适合特定功能场景（如生成 PDF、处理 Excel），而规则加载应该在每次任务开始时自动按需执行。

### 2.2 具体改造步骤

#### Step 1：拆分 `.trae/rules/general.md`

将现有的单一入口拆分为多个场景规则文件：

```plaintext
.trae/rules/
├── general.md              # 保留，但改为轻量入口（仅包含项目定位和导航）
├── coding.md               # 编码时触发（karpathy + SOLID 相关章节）
├── writing.md              # 写文档时触发（markdown 规范）
└── spec-reference.md       # 按需加载策略索引（本文件）
```

#### Step 2：改造后的 `general.md`（轻量版）

```markdown
---
alwaysApply: true
description: 项目全局开发约定，所有对话均生效
---

# 规则总入口 — LLM Proxy

## 项目定位

LLM Proxy 是一个轻量级的大模型 API 代理服务，兼容 OpenAI / Anthropic API 格式。

## 按需加载原则

**每次任务执行时，根据任务类型按需加载规则，严禁全量加载。**

| 任务类型 | 加载文件 |
|---------|---------|
| 编码/修改代码 | `.trae/rules/coding.md` |
| 编写/修改文档 | `.trae/rules/writing.md` |
| 涉及业务规则 | `project-docs/00-spec/project/01-domain.md` |
| 涉及 API 接口 | `project-docs/00-spec/project/02-api.md` |
| 涉及后端开发 | `project-docs/00-spec/project/03-backend.md` |
| 涉及数据库 | `project-docs/00-spec/project/04-database.md` |
| 涉及前端开发 | `project-docs/00-spec/project/05-frontend.md` |
| 涉及测试/质量 | `project-docs/00-spec/project/06-quality.md` |
| 涉及部署/运维 | `project-docs/00-spec/project/07-ops.md` |

**严禁**：
- ❌ 每次任务全量加载所有 common rules 和 spec 文件
- ❌ 跳过任务类型判断直接读取全部 spec

---

## 核心原则

**本目录的 `.md` 文件是场景触发器，不是规则的权威来源。**
真正的规则、完整的判定标准、背后的设计决策，全部在 `project-docs/` 中。
触发器与 spec 冲突时，**以 spec 为准**。

---

## 快速导航

- **规范全景**：`project-docs/README.md`
- **spec 索引**：`project-docs/00-spec/README.md`
```

#### Step 3：创建 `.trae/rules/coding.md`

```markdown
---
alwaysApply: false
description: 编码时触发的设计原则和编码准则
triggerKeywords: ["代码", "编码", "实现", "修改", "新增", "重构", "class", "function", "method", "interface"]
---

# 编码规则

## 强制预加载

执行编码任务时，必须按需加载以下文件：

| 文件 | 加载条件 |
|------|---------|
| `.trae/common/karpathy.md` | 所有编码任务 |
| `.trae/common/SOLID.md` | 涉及设计模式、架构、重构时 |

## 编码原则

1. **简单优先**：解决问题的最小代码，不做推测
2. **精准修改**：只改动必须改的，只清理自己造成的混乱
3. **目标驱动**：定义成功标准，循环直到验证完成
4. **编码前先思考**：明确假设，不要隐藏困惑

## 代码规范

- Java 代码遵循 Spring Boot 最佳实践
- TypeScript 代码使用严格模式
- 所有代码必须包含追溯注释：`[TASK: Task<NNN>][SPEC: <文件> §<章节>]`
```

#### Step 4：创建 `.trae/rules/writing.md`

```markdown
---
alwaysApply: false
description: 编写文档时触发的 Markdown 规范
triggerKeywords: ["文档", "Markdown", "README", "md", "编写文档", "更新文档"]
---

# 文档编写规则

## 强制预加载

执行文档任务时，必须加载以下文件：

| 文件 | 加载条件 |
|------|---------|
| `.trae/common/markdown.md` | 所有文档任务 |

## 文档规范速查

- 代码块必须指定语言标签
- JSON 三档分类：`json` / `jsonc` / `json5`
- 标题前后各空一行
- 列表前后各空一行
- 文件末尾保留一个空行
```

### 2.3 改造效果对比

| 场景 | 改造前 | 改造后 | 节省 |
|------|-------|-------|------|
| 编码任务 | ~48K | ~18K（general + coding + karpathy + 相关 spec） | **63%** |
| 文档任务 | ~48K | ~10K（general + writing + markdown） | **79%** |
| 部署任务 | ~48K | ~8K（general + 07-ops） | **83%** |
| 纯咨询 | ~48K | ~3K（仅 general） | **94%** |

---

## 3. 场景触发器映射表

### 3.1 任务类型 → 需要加载的文件

| 任务场景 | 必加载 | 按需加载 | 不需要加载 |
|---------|-------|---------|-----------|
| **新增 API 接口** | general, 02-api | 01-domain（涉及业务规则时）, 03-backend（后端实现时）, 05-frontend（前端对接时） | 04-database, 06-quality, 07-ops |
| **修改数据库表结构** | general, 04-database | 01-domain（实体变更时）, 02-api（接口变更时） | 05-frontend, 07-ops |
| **前端页面开发** | general, 05-frontend | 02-api（接口对接时）, 06-quality（测试时） | 03-backend, 04-database, 07-ops |
| **后端业务逻辑开发** | general, 03-backend | 01-domain（业务规则）, 02-api（接口变更）, 04-database（数据变更） | 05-frontend, 07-ops |
| **Bug 修复** | general | 涉及哪个模块就加载哪个 spec | 不涉及的模块 |
| **代码重构** | general, coding, SOLID | 涉及的具体模块 spec | 不涉及的模块 |
| **Docker 部署** | general, 07-ops | 无 | 01-06 |
| **编写文档** | general, writing, markdown | 涉及的 spec 文件 | 不涉及的模块 |
| **新增测试用例** | general, 06-quality | 02-api（接口测试时） | 04-database, 07-ops |

### 3.2 关键词触发器

当用户输入或任务描述中包含以下关键词时，自动加载对应文件：

| 关键词 | 触发加载 |
|--------|---------|
| "接口"、"API"、"端点"、"endpoint" | `spec/02-api.md` |
| "表"、"字段"、"迁移"、"DDL" | `spec/04-database.md` |
| "前端"、"Vue"、"组件"、"页面" | `spec/05-frontend.md` |
| "后端"、"Service"、"Controller"、"Repository" | `spec/03-backend.md` |
| "业务规则"、"BR-"、"实体" | `spec/01-domain.md` |
| "测试"、"单测"、"质量"、"门禁" | `spec/06-quality.md` |
| "部署"、"Docker"、"CI/CD"、"运维" | `spec/07-ops.md` |
| "设计模式"、"重构"、"架构" | `.trae/common/SOLID.md` |
| "编码"、"代码风格"、"注释" | `.trae/common/karpathy.md` |
| "文档"、"Markdown"、"README" | `.trae/common/markdown.md` |

---

## 4. 规则索引（快速定位）

### 4.1 Spec 文件速查

| 编号 | 文件 | 一句话定位 | 核心内容 |
|------|------|-----------|---------|
| **00** | `spec/00-core.md` | 全局规则唯一真源 | SDD 流程、Task 编号、代码追溯注释、文档管理、安全约束、禁止项 |
| **01** | `spec/01-domain.md` | 业务规则与实体定义 | BR-01 ~ BR-16、7 个实体字段定义 |
| **02** | `spec/02-api.md` | 全部 API 端点规格 | 路径、方法、参数、响应、错误码 |
| **03** | `spec/03-backend.md` | 后端 DDD 分层规范 | 包结构、协议适配器、负载均衡、SSE 规范 |
| **04** | `spec/04-database.md` | 数据库规范 | SQLite 原则、7 张表结构、迁移脚本规范 |
| **05** | `spec/05-frontend.md` | 前端编码规范 | Vue 3 + TypeScript + Element Plus、目录结构、API 封装 |
| **06** | `spec/06-quality.md` | 测试与质量规范 | 测试策略、代码质量门禁、日志规范、监控 |
| **07** | `spec/07-ops.md` | 运维部署规范 | 依赖管理、构建流程、Docker 部署、CI/CD |

### 4.2 Common Rules 速查

| 文件 | 一句话定位 | 核心内容 |
|------|-----------|---------|
| `SOLID.md` | 面向对象设计原则 | SRP/OCP/LSP/ISP/DIP 定义、反例、正例、Spring 实践 |
| `karpathy.md` | 编码准则 | Karpathy 编码原则、代码风格、最佳实践 |
| `markdown.md` | Markdown 编写规范 | 代码块标签、JSON 分类、排版规则 |

---

## 5. 加载流程（AI IDE 执行步骤）

### 5.1 标准加载流程

```plaintext
Step 1: 读取 general.md（规则入口） → 了解项目定位和按需加载策略
Step 2: 分析用户任务 → 识别任务类型和关键词
Step 3: 查映射表 → 确定需要加载的文件
Step 4: 按需加载 → 只读取相关文件
Step 5: 执行任务 → 按照加载的规则执行
```

### 5.2 案例演示

#### 案例 1：用户说"帮我新增一个 API Key 管理的接口"

```plaintext
Step 1: 读取 general.md（规则入口） ✓
Step 2: 识别关键词："API"、"接口"、"API Key"
Step 3: 查表确定需要加载：
        - 必加载：00-core（全局规则）
        - 按需加载：02-api（API 契约）、01-domain（业务规则 BR-01~03）
        - 后端实现时追加：03-backend
Step 4: 加载 00-core → 02-api → 01-domain → 03-backend
Step 5: 按照 spec 执行
```

**不需要加载**：04-database（如果不改表结构）、05-frontend（如果只做后端）、06-quality、07-ops

#### 案例 2：用户说"前端页面有个 bug，删除按钮点击后报错"

```plaintext
Step 1: 读取 general.md（规则入口） ✓
Step 2: 识别关键词："前端"、"bug"、"删除"
Step 3: 查表确定需要加载：
        - 必加载：00-core（全局规则）
        - 按需加载：05-frontend（前端规范）、02-api（DELETE 接口规格）
Step 4: 加载 00-core → 05-frontend → 02-api
Step 5: 定位 bug 并修复
```

**不需要加载**：01-domain、03-backend（如果 bug 纯前端）、04-database、06-quality、07-ops

#### 案例 3：用户说"优化一下 Docker 部署脚本"

```plaintext
Step 1: 读取 general.md（规则入口） ✓
Step 2: 识别关键词："Docker"、"部署"
Step 3: 查表确定需要加载：
        - 必加载：00-core（全局规则）
        - 按需加载：07-ops（运维部署规范）
Step 4: 加载 00-core → 07-ops
Step 5: 按照 ops 规范优化脚本
```

**不需要加载**：01-06（与部署无关）

---

## 6. 特殊情况处理

### 6.1 跨模块任务

当任务涉及多个模块时（如"新增一个完整功能，包括前后端和数据库"）：

```plaintext
Step 1: 读取 general.md（规则入口） ✓
Step 2: 识别为跨模块任务
Step 3: 按 SDD 流程顺序加载：
        ① 00-core（全局规则）+ 01-domain（业务规则）
        ② 04-database（数据库设计）
        ③ 03-backend（后端实现）+ 02-api（接口规格）
        ④ 05-frontend（前端实现）
Step 4: 分阶段执行，每阶段只加载当前阶段需要的文件
```

### 6.2 规则冲突处理

当多个文件规则冲突时，按以下优先级：

```plaintext
00-core（最高） > 02-api > 03-backend / 05-frontend > 01-domain > 其他 > 局部业务规则
```

### 6.3 规则不存在时的降级

如果某个 spec 文件尚未创建：
1. 跳过该文件，不报错
2. 在任务报告中标注"缺少 spec/XX-xxx.md，建议补充"
3. 使用 00-core 中的通用规则作为兜底

---

## 7. Token 节省效果对比

| 场景 | 改造前 | 改造后 | 节省 |
|------|-------|-------|------|
| 新增 API 接口（仅后端） | ~48K | ~15K | **69%** |
| 前端 Bug 修复 | ~48K | ~10K | **79%** |
| Docker 部署优化 | ~48K | ~8K | **83%** |
| 完整功能开发（前后端+DB） | ~48K | ~30K | **38%** |
| 纯咨询/问答 | ~48K | ~3K | **94%** |

**平均节省约 60-80% 的 token 消耗**。

---

## 8. 维护与更新

### 8.1 何时更新本文件

| 事件 | 操作 |
|------|------|
| 新增 spec 文件 | 更新映射表和速查表 |
| 删除 spec 文件 | 从映射表和速查表中移除 |
| 新增 common rule | 更新 common rules 速查表 |
| 触发器规则调整 | 更新关键词触发器表 |
| `.trae/rules/` 结构变化 | 更新 Step 2 改造步骤 |

### 8.2 完成判定

- [ ] 新增/删除 spec 文件后，映射表和速查表已同步
- [ ] 关键词触发器覆盖所有 spec 文件
- [ ] 案例演示覆盖典型场景
- [ ] `.trae/rules/general.md` 已改造为轻量入口
- [ ] `.trae/rules/coding.md` 和 `writing.md` 已创建
