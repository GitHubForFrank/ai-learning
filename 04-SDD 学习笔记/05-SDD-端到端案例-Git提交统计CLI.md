# 端到端案例：Git 提交统计 CLI

> 版本：1.0 | 定位：SDD 六阶段完整走一遍

本章用一个**完整端到端**的小案例展示 SDD 六阶段如何走通。案例体量一周以内，但五脏俱全，适合 Java 背景读者在本地走一遍。

> 案例只演示主路径。更多专题——Token 成本、跨栈协作、AI 可读性、测试、Brownfield、避坑——见 06–11 章。

---

## 1. 案例背景

**需求原话**："给团队做个命令行工具，跑一下就能看到每人最近一周提交了多少。"

技术栈设定：**Java 17 + picocli + jgit，可打包为单 jar**。

---

## 2. 阶段一：需求分析

### 2.1 澄清对话（节选）

```
Q1 "每人" = 按 Git author.email 还是 author.name？
A  用 email，更稳定。

Q2 "最近一周" = 自然周（周一~周日）还是滚动 7 天？
A  默认滚动 7 天，支持参数切换。

Q3 要含 merge commit 吗？
A  默认不含。

Q4 输出给谁看？
A  人看表格；CI 脚本可能要机读 → 需要 --json。

Q5 跨仓库聚合？
A  本期单仓库。
```

### 2.2 需求摘要（归档到 spec §1）

> 团队 leader 在每周例会前希望一行命令即可查看团队一周提交活跃度；默认人读表格、可切 `--json` 机读；覆盖单仓库场景；不做跨仓库、不做可视化。

---

## 3. 阶段二：生成 Spec（示例）

文件路径：`docs/specs/0001-git-stats.md`

```markdown
# Git 提交统计 CLI — Spec

> 版本：v0.1 | 状态：[DRAFT]

## 1. 需求背景
团队周会缺乏一眼看清活跃度的工具。

## 2. 业务目标
- 主：一条命令展示指定时间段内每个作者的提交统计
- 非目标：本期不做 Web UI、不做多仓库聚合、不做趋势图

## 3. 功能范围
[MUST]   F1 输出表格：作者 | 提交数 | 变更行数
[MUST]   F2 参数 --days N（默认 7）控制滚动窗口
[MUST]   F3 默认排除 merge commit，--include-merges 可打开
[MUST]   F4 参数 --json 切换为机读输出
[SHOULD] F5 参数 --repo 指定仓库路径（默认当前目录）

## 4. 非功能需求
- 性能：1 万提交规模下 < 2s
- 安全：仅读 Git，不写回仓库、不联网
- 兼容：JDK 17+、Git 2.25+

## 5. 接口定义（CLI）
gitstats [--days=N] [--include-merges] [--json] [--repo=PATH]
退出码：0 正常；2 参数错误；3 仓库读取失败

JSON schema:
{
  "since": "ISO8601",
  "authors": [
    { "email": "string", "commits": int, "linesChanged": int }
  ]
}

## 6. 数据模型
无持久化。内存聚合 Map<email, {commits, linesChanged}>。

## 7. 技术方案
- CLI 解析：picocli
- Git 读取：jgit
- 选型理由：jgit 免装 Git 可执行文件，便于 CI；picocli 注解驱动、学习成本低

## 8. 风险评估
| 风险 | 可能性 | 缓解 |
|------|--------|------|
| 超大仓库内存溢出 | 中 | 使用 RevWalk 流式遍历，不 toList |
| 时区误差影响统计 | 中 | 统一用本地系统时区，JSON since 带 offset |
| 作者一人多 email | 高 | 本期不做合并，README 明示局限 |

## 9. 验收标准
[AC-1] `gitstats` 在本仓库运行 → 至少输出 1 行表格数据
[AC-2] `gitstats --days=1` → 只含 24 小时内提交
[AC-3] 默认输出不含 merge；加 --include-merges 含
[AC-4] `gitstats --json` → 合法 JSON，schema 符合 §5
[AC-5] 参数错误（如 --days=abc）退出码为 2
[AC-NFR-1] 在 1 万提交 fixture 仓库下执行 < 2s
```

---

## 4. 阶段三：评审与细化

### 4.1 发现的问题（截取 3 条）

| 节 | 问题 | 严重度 | 处理 |
|----|------|--------|------|
| §3 F2 | "时间窗口"是否含当前小时？ | M | 明确为 now − N 天 到 now 滚动窗口 |
| §5 | JSON 字段 email 缺失策略未指定 | M | 补：email 为空时使用 `<unknown>` |
| §8 | 未覆盖"空仓库"风险 | L | 补：空仓库退出码 0、输出空表 / `{authors:[]}` |

spec 升级到 v0.2 → v1.0 `[APPROVED]`。

### 4.2 评审小技巧

- 让 Claude 扮演 **3 种角色独立审**：杠精 / 安全专家 / 新人
- 对每类角色强调 "**只找问题、不夸奖**"
- 汇总后去重，即可得到一份扎实的评审意见

---

## 5. 阶段四：按 Spec 生成代码

### 5.1 让 AI 先列出改动计划（不要求细到代码）

```
计划改动：
- src/main/java/.../Cli.java         → 对应 spec §5 命令解析
- src/main/java/.../CommitStats.java  → 对应 spec §7 聚合逻辑
- src/main/java/.../output/Table.java → 对应 spec §3 F1
- src/main/java/.../output/Json.java  → 对应 spec §3 F4
- src/test/java/.../*                 → 覆盖 AC-1~5 和 AC-NFR-1
```

### 5.2 关键原则

- **不**要求 AI 一次给出完整代码，逐模块推进
- 每个类的 doc comment 引用 spec 章节
- 测试类里用 `@DisplayName("AC-3: excludes merge by default")` 让 AC 号可搜

> 本章遵循"指南文档只写规范与流程，不贴入口代码 / 脚手架"的约定，具体实现交由学习者动手。

---

## 6. 阶段五：验证测试

### 6.1 验收用例对应表

| 验收 | 测试方法 | 工具 |
|------|----------|------|
| AC-1 | 集成测试 + jgit 构造 fixture 仓库 | JUnit 5 |
| AC-2 | 参数化测试，注入 Clock | JUnit + Clock |
| AC-3 | fixture 包含 merge commit | JUnit |
| AC-4 | JSON schema 校验 | `networknt/json-schema-validator` |
| AC-5 | `ProcessBuilder` 验证 exit code | JUnit |
| AC-NFR-1 | 生成 1 万提交仓库并计时 | JMH 或手工 |

### 6.2 偏差处理

本次跑测时 AC-4 JSON 的 `since` 字段少带时区 → **修代码** + 对照 §5 确认 spec 无需改动。

---

## 7. 阶段六：归档

- spec 标记 `[APPROVED]` → `[RELEASED]`，版本固定 `v1.0`
- `CHANGELOG.md`：`feat(cli): git-stats v1.0 — implements spec 0001`
- `docs/specs/README.md` 索引新增一条
- 若后续想加"多仓库聚合"，**新开 spec 0002**（引用 0001 为上游），不直接改 0001

---

## 8. 思考与练习

1. **复盘题**：找出你最近 3 个月一次"上线后返工"的经历。如果当时有 spec，哪些条目本可以避免这次返工？
2. **动手题**：按本章思路，给自己写一个"本地博客草稿搜索 CLI"的完整 spec（9 模块、约 60 行）。写完让你常用的 AI 编程工具（Claude Code / Cursor / ChatGPT 等）以杠精模式审一次，统计找出几条问题。
3. **延伸题**：阅读 Amazon Kiro / GitHub Spec Kit 的公开示例，它们的 spec 模板和本仓库第 03 章的 9 模板有哪些差异？各自优缺点？

---

## 9. 学习路线建议

```
走完本章案例
    ↓
读 06–11 章中你最关心的专题（Token 成本 / 跨栈 / 测试 / Brownfield ...）
    ↓
给自己的下一个真实任务应用 SDD
    ↓
（进阶）在团队内部做一次 SDD 分享 / workshop
```

---

## 10. 参考来源

- Kiro：https://kiro.dev/
- GitHub Spec Kit：https://github.com/github/spec-kit
- 搭配阅读：本仓库 `03-MCP 学习笔记/04-规范与标准/`（规范风格借鉴）
- 搭配实战：本仓库 `04-SDD 实战项目/`（如已存在）
