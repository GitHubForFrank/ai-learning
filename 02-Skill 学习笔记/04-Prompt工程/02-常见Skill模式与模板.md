# 常见 Skill 模式与模板

> 版本：1.0 | 定位：可直接复用的 Skill 模板库

---

## 1. 开发效率类

### 1.1 Git 提交信息生成（commit）

```markdown
---
name: commit
description: |
  当需要生成 Git 提交信息、提交代码时使用。
  会自动分析暂存区变更，生成符合 Conventional Commits 规范的提交信息。
allowed-tools:
  - Bash
---

分析当前 Git 暂存区的变更，生成规范的提交信息并执行提交。

**执行步骤：**
1. 运行 `git diff --staged` 分析变更内容
2. 运行 `git log --oneline -5` 了解历史提交风格
3. 根据变更类型确定 commit type：
   - feat: 新功能
   - fix: Bug 修复
   - docs: 仅文档变更
   - style: 格式调整（不影响逻辑）
   - refactor: 重构（不是 fix 也不是 feat）
   - test: 增加或修改测试
   - chore: 构建过程或辅助工具变更
4. 生成提交信息，格式：`<type>(<scope>): <subject>`
5. 如果变更复杂，添加 body 说明变更原因
6. 执行 `git commit -m "..."` 提交

**约束：**
- subject 使用中文，不超过 50 个字
- 不添加 Co-Authored-By 等多余 footer
- 如果暂存区为空，提示用户先 git add
```

---

### 1.2 代码审查（review）

```markdown
---
name: review
description: |
  对代码进行 Code Review，检查质量、安全性和性能问题。
  可选参数：quick（快速检查）/ strict（严格审查）/ security（安全专项）
allowed-tools:
  - Read
  - Grep
  - Glob
---

对以下代码进行 Code Review。模式：**$ARGUMENTS**（默认：standard）。

**审查维度（standard 模式）：**

1. **正确性**
   - 业务逻辑是否符合预期
   - 边界条件和空值是否处理
   - 并发场景是否安全

2. **可维护性**
   - 函数/类职责是否单一
   - 命名是否清晰表达意图
   - 是否有过多的嵌套层级（超过3层需重构）

3. **安全性（security 模式时加强）**
   - SQL 注入、XSS、CSRF 漏洞
   - 敏感信息是否泄露（日志、响应体）
   - 权限校验是否完整

4. **性能**
   - 循环中是否有数据库查询（N+1问题）
   - 是否有明显的内存泄漏风险
   - 缓存使用是否合理

**输出格式：**

| 级别 | 位置 | 问题描述 | 改进建议 |
|------|------|---------|---------|
| 🔴 Critical | ... | ... | ... |
| 🟡 Warning | ... | ... | ... |
| 🟢 Suggestion | ... | ... | ... |

**结论：** [通过 / 需要修改后通过 / 不通过]
```

---

### 1.3 单元测试生成（gen-test）

```markdown
---
name: gen-test
description: |
  为指定函数或类生成单元测试代码。
  用法：/gen-test [文件路径或函数名]
  会读取目标文件，分析函数签名和业务逻辑，生成覆盖正常/边界/异常场景的测试。
allowed-tools:
  - Read
  - Write
  - Glob
---

为目标代码生成单元测试：$ARGUMENTS

**步骤：**
1. 读取目标文件，识别需要测试的函数/方法
2. 分析：入参类型、返回值类型、业务逻辑、可能的异常
3. 设计测试用例，覆盖：
   - 正常场景（2-3个典型输入）
   - 边界场景（空值、null、边界数值）
   - 异常场景（非法输入、依赖服务异常）
4. 生成测试代码，遵循项目已有的测试框架和风格
5. 将测试写入对应的测试文件（如 UserServiceTest.java）

**测试代码要求：**
- 使用 AAA 模式（Arrange-Act-Assert）
- 测试方法名：`should_[预期行为]_when_[场景描述]`
- Mock 外部依赖，不依赖真实数据库/网络
- 每个测试只验证一个行为点
```

---

## 2. 文档类

### 2.1 API 文档生成（api-doc）

```markdown
---
name: api-doc
description: |
  为 Controller/Router 文件生成 OpenAPI 3.0 格式的 API 文档。
  用法：/api-doc [Controller文件路径]
allowed-tools:
  - Read
  - Write
---

为以下 Controller 生成 OpenAPI 3.0 API 文档：$ARGUMENTS

**分析内容：**
- 接口路径、HTTP 方法、参数（Path/Query/Body）
- 响应结构和 HTTP 状态码
- 认证要求（从注解或中间件推断）
- 业务含义（从函数名和注释推断）

**输出格式（OpenAPI 3.0 YAML）：**
```yaml
openapi: 3.0.0
info:
  title: [模块名] API
  version: 1.0.0
paths:
  /api/path:
    get:
      summary: 接口名称
      description: 详细说明
      parameters: [...]
      responses:
        200:
          description: 成功
          content: [...]
        400:
          description: 参数错误
```

将生成的文档写入 `docs/api/[模块名].yaml`。
```

---

### 2.2 变更日志生成（changelog）

```markdown
---
name: changelog
description: |
  根据 Git 提交记录生成 CHANGELOG.md 条目。
  用法：/changelog [版本号]，如 /changelog v1.2.0
allowed-tools:
  - Bash
  - Read
  - Edit
---

生成版本 **$ARGUMENTS** 的 CHANGELOG 条目。

**步骤：**
1. 运行 `git log --oneline [上一个tag]..HEAD` 获取提交列表
2. 按 Conventional Commits 类型分组：
   - ✨ Features（feat:）
   - 🐛 Bug Fixes（fix:）
   - 📚 Documentation（docs:）
   - ⚡ Performance（perf:）
   - 🔧 Chores（chore:）
3. 过滤掉无意义的 commit（合并提交、格式化等）
4. 生成以下格式的条目：

```markdown
## [版本号] - YYYY-MM-DD

### ✨ Features
- 新增用户头像上传功能 (#123)

### 🐛 Bug Fixes
- 修复登录超时后未清除 Cookie 的问题 (#124)
```

5. 将条目插入 CHANGELOG.md 文件顶部（在 `# Changelog` 标题下）
```

---

## 3. 分析类

### 3.1 性能分析（perf-analyze）

```markdown
---
name: perf-analyze
description: |
  分析代码的性能瓶颈，重点关注时间复杂度、数据库查询和内存使用。
  用法：/perf-analyze [文件路径]
allowed-tools:
  - Read
  - Grep
---

对以下代码进行性能分析：$ARGUMENTS

**分析重点：**

1. **算法复杂度**
   - 识别 O(n²) 及以上的循环嵌套
   - 检查是否有可用哈希表优化的线性搜索

2. **数据库访问**
   - N+1 查询问题（循环内查询数据库）
   - 缺少索引的过滤条件
   - 未使用批量操作

3. **内存使用**
   - 大集合未分页加载
   - 长生命周期对象持有大量临时数据
   - 频繁创建短生命周期大对象

4. **IO 操作**
   - 同步 IO 阻塞主线程
   - 未使用连接池的外部服务调用
   - 大文件未使用流式读取

**输出：**
- 问题列表（按影响程度排序）
- 预估的性能提升空间
- 具体的优化代码示例
```

---

### 3.2 依赖安全检查（dep-check）

```markdown
---
name: dep-check
description: |
  检查项目依赖的安全性，识别已知漏洞和过期依赖。
  会读取 package.json / pom.xml / requirements.txt 等依赖文件。
allowed-tools:
  - Read
  - Bash
  - Glob
---

检查项目的依赖安全性。

**步骤：**
1. 自动识别项目类型（Node.js/Java/Python）并找到依赖文件
2. 对于 Node.js 项目：运行 `npm audit --json` 获取漏洞报告
3. 对于 Python 项目：运行 `pip-audit` 或检查 safety 数据库
4. 识别以下风险：
   - 已知 CVE 漏洞（High/Critical 级别）
   - 超过2年未更新的依赖
   - 不再维护的包（archived/deprecated）
   - 版本范围过宽的依赖（^major.x.x）

**输出格式：**

## 安全风险（需立即处理）
| 包名 | 当前版本 | CVE | 严重程度 | 修复方案 |

## 过期依赖（建议更新）
| 包名 | 当前版本 | 最新版本 | 最后更新 |

## 总体评估
[通过 / 需要关注 / 高风险]
```

---

## 4. 上下文注入类

### 4.1 项目背景加载（load-context）

```markdown
---
name: load-context
description: |
  加载并注入当前项目的背景信息、技术栈和开发约定。
  在开始新任务前使用，帮助 Agent 理解项目上下文。
allowed-tools:
  - Read
  - Glob
---

加载项目上下文信息，为后续任务提供背景。

**读取以下文件（如果存在）：**
1. `README.md` — 项目概述和快速开始
2. `CLAUDE.md` — AI 开发指南和约定
3. `package.json` / `pom.xml` / `pyproject.toml` — 技术栈信息
4. `.env.example` — 环境变量配置说明
5. `docs/architecture.md` — 架构文档（如果存在）

**输出项目概览：**

## 项目背景
[从 README 提取的项目简介]

## 技术栈
[框架、语言版本、主要依赖]

## 开发约定
[从 CLAUDE.md 提取的重要约定]

## 当前状态
[最近的提交摘要，了解近期工作重点]

上下文已加载，可以开始工作。
```

---

### 4.2 专家角色设定（act-as）

```markdown
---
name: act-as
description: |
  切换 Agent 的专家角色，注入特定领域的专业背景。
  用法：/act-as [角色]，如 /act-as java-architect 或 /act-as security-expert
---

切换到指定专家角色：**$ARGUMENTS**

根据角色选择对应的专业设定：

**java-architect**: 你是拥有15年 Java 开发经验的企业级架构师，
熟悉 Spring Cloud 微服务体系，专注于系统可扩展性和高可用设计。
所有建议优先考虑：生产稳定性 > 性能 > 开发效率。

**security-expert**: 你是 CISSP 认证的安全工程师，
专注于应用安全（AppSec）领域，熟悉 OWASP Top 10 和 CWE。
所有代码审查必须包含安全维度的分析。

**frontend-designer**: 你是拥有10年经验的 UI/UX 工程师，
精通现代前端工程化（React/Vue/TypeScript）和设计系统。
所有建议优先考虑：用户体验 > 代码简洁性 > 性能。

**data-engineer**: 你是资深数据工程师，
精通 SQL 优化、分布式计算（Spark/Flink）和数据仓库设计。
所有方案优先考虑：数据准确性 > 查询性能 > 存储成本。

角色已设定，请继续你的任务。
```

---

## 5. 格式化输出类

### 5.1 技术方案文档（tech-proposal）

```markdown
---
name: tech-proposal
description: |
  将技术讨论或需求描述转化为标准的技术方案文档。
  适合在编写 RFC、技术评审文档时使用。
---

将以下内容整理为标准技术方案文档：$ARGUMENTS

**输出格式：**

# 技术方案：[方案名称]

## 背景与问题
[1-2段：当前痛点和改进动机]

## 目标
- [ ] 目标1（可量化的指标）
- [ ] 目标2

## 非目标（Not Goals）
- 本方案不解决...
- 本方案不涉及...

## 方案设计

### 核心思路
[简洁描述方案的核心逻辑]

### 详细设计
[架构图、接口设计、数据模型等]

### 方案对比
| | 方案A（推荐）| 方案B | 方案C |
|-|------------|------|------|
| 优点 | | | |
| 缺点 | | | |
| 适用场景 | | | |

## 实施计划
| 阶段 | 工作内容 | 时间估算 |
|------|---------|---------|

## 风险与应对
| 风险 | 概率 | 影响 | 应对措施 |
|------|------|------|---------|

## 参考资料
```

---

## 6. 模式选择指南

```
你的 Skill 属于哪种类型？
│
├── 用户主动触发，有明确操作对象
│   ├── 生成/创建类 → 参考：commit, gen-test, api-doc
│   ├── 分析/审查类 → 参考：review, perf-analyze, dep-check
│   └── 转换/格式化类 → 参考：changelog, tech-proposal
│
├── 需要注入背景知识或角色设定
│   └── 上下文注入类 → 参考：load-context, act-as
│
└── 自动化重复性操作（结合 Hook 使用）
    → 参考：commit（PostToolUse:Bash 场景）
```
