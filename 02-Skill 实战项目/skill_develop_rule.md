# Skill 开发规则（Development Rules）

<!--
  最后更新：2026-04-20
  适用范围：本仓库 `02-Skill 实战项目/` 下所有 skill 项目
  规则本身的变更流程见 §14
-->

> 目的：让每一次 skill 的新增、优化、调整都遵循同一套规则，避免走岔路、漏更新、基于旧设计重构。

---

## 目录

- [强度约定（符号）](#强度约定符号)
- [§0 术语表](#0-术语表)
- [§1 最高原则](#1-最高原则)
- [§2 交付前检查清单](#2-交付前检查清单)
- [§3 标准目录结构](#3-标准目录结构)
- [§4 Plan 规则](#4-plan-规则)
- [§5 README 规则](#5-readme-规则)
- [§6 SKILL.md 规则](#6-skillmd-规则)
- [§7 版本号规则](#7-版本号规则)
- [§8 FINDINGS 规则](#8-findings-规则)
- [§9 Knowledge 规则](#9-knowledge-规则)
- [§10 Scripts 规则](#10-scripts-规则)
- [§11 Bug 修复流程（索引）](#11-bug-修复流程索引)
- [§12 源码注释政策](#12-源码注释政策)
- [§13 自动化与 hook 建议](#13-自动化与-hook-建议)
- [§14 规则本身的演化](#14-规则本身的演化)
- [§15 配套资料 / SOP 参考](#15-配套资料--sop-参考)

---

## 强度约定（符号）

规则前面的符号表示硬度，读者据此判断能不能变通：

- **[硬]** MUST：违反即视为不合格；**没有任何借口可以变通**。
- **[软]** SHOULD：默认遵守；若有明确理由可变通，需在 commit message 里说明。
- **[选]** MAY：推荐但非强制。

不带符号的句子是上下文描述或补充说明，不是规则条款本身。

---

## §0 术语表

| 术语 | 含义 |
|---|---|
| **Skill 核心目录** / **交付单元** | `skill-XX-<name>/<skill-name>/`，可被 Claude 直接加载、发布时打包的子目录 |
| **开发过程资料** | `skill-XX-<name>/development/`，设计草稿 / 测试记录 / 调研笔记，不参与交付 |
| **自研工具源码** | `skill-XX-<name>/tools-src/<artifact-name>/`，skill 自研 artifact（如 jar）的源码与构建配置，不参与交付；见 §3.3 |
| **Plan** | `development/plans/PLAN_vNN.md`，skill 的设计蓝图；每次重大变更新建一版 |
| **FINDINGS** | `development/findings/FINDINGS_<YYYYMMDD_HHMMSS>.md`，批量测试 / 试用的发现记录；每轮一份 |
| **三者同步** | Plan、README.md、SKILL.md 的功能描述、目录结构、参数契约必须一致 |
| **交付前** | 代码改动 commit / PR 合并前的最后一步 |
| **功能性改动** | 改变可观察行为的代码修改（新增 / 修改 / 删除能力，输入输出契约变更，引入新依赖） |
| **非功能性改动** | 仅影响阅读 / 维护的修改（typo、格式、注释重写、日志措辞） |

---

## §1 最高原则

1. **[硬] 永远基于最新 plan 重新制作 skill，不得用旧 plan 重构。**
   每次开工前先读 `development/plans/` 下最新的 PLAN；若代码现状与最新 plan 不一致，**先把 plan 改对，再动代码**。
2. **[硬] 三者同步**：Plan / README.md / SKILL.md 的功能描述、目录结构、参数契约必须一致，改一个就要联动核查另外两个。
3. **[硬] 历史不可覆盖**：旧的 PLAN 与 FINDINGS 一律按版本号或时间戳另存，禁止原地修改或删除。
4. **[硬] 只做本次任务要求的事**：不顺手做 refactor、不"顺便"改无关文件；不确定范围时先问用户。

**不适用于**：§0 术语表定义的"非功能性改动"；此类改动可跳过§4 plan 更新，但仍需 §7 判断是否升 VERSION。

---

## §2 交付前检查清单

每次改动收尾前逐项过。右侧"验证方法"给出可操作命令或检查点，避免口头承诺式打勾。

| # | 条目 | 强度 | 验证方法 |
|---|---|---|---|
| 1 | 最新 plan 已更新，且与代码现状一致 | [硬] | `ls development/plans/ \| tail -1` 打开，顶部"变更点"覆盖本次改动 |
| 2 | README.md 目录结构、功能列表与实际一致 | [硬] | 对比 `ls skill-XX/<skill-name>/` 与 README 的"目录结构"小节；并按 §5.3 核查：无硬编码版本号 / 最后更新日期 / `development/` 下具体文件枚举 |
| 3 | SKILL.md 的 description、工作流与 README 一致 | [硬] | 对比 SKILL.md frontmatter description 与 README "何时使用" |
| 4 | `.meta/VERSION` 已按 §7 规则升位 | [硬] | `git diff .meta/VERSION` 有变更 |
| 5 | 若有 bug 修复 → plan 的"已知陷阱"已补充 | [硬] | 打开最新 plan 的"已知陷阱"章节，本轮 bug 已登记 |
| 6 | 本轮测试的 FINDINGS 已归档 | [硬] | `ls development/findings/` 含本轮时间戳文件 |
| 6a | 若本轮 FINDINGS 引入新样本 → README 测试覆盖表已同步 | [硬] | 取最新 FINDINGS 样本清单中所有插件名，每个在 README 测试覆盖表里或有专属行、或有能代表它的同类行 |
| 7 | 旧 plan、旧 findings 未被删除或覆盖 | [硬] | `git diff --name-status` 无 `D` 状态的历史文件 |
| 8 | 源码注释符合 §12（中文、只留当前 WHY） | [软] | `grep -rnE 'v0\.\d\|FINDINGS_\|PLAN_v' <skill>/scripts/ <skill>/knowledge/` 返回 0 行 |

**不适用于**：纯文档 typo / 格式调整可跳过条目 1、5、6。

---

## §3 标准目录结构

### 3.1 基础布局

Skill 项目**最多**包含三个顶层目录：

```
skill-XX-<name>/
├── <skill-name>/                       # 📦 Skill 核心目录（交付单元）
│   ├── .meta/VERSION                   # [必需] 语义化版本号 MAJOR.MINOR.PATCH
│   ├── SKILL.md                        # [必需] Agent Skill 入口（frontmatter + 触发时机 + 工作流）
│   ├── README.md                       # [必需] 人读说明（功能、用法、目录、示例）
│   ├── knowledge/                      # [条件] 有数据驱动规则时建；YAML 按主题拆分
│   ├── scripts/                        # [条件] 有自动化编排代码时建；单一职责、按阶段命名
│   ├── templates/                      # [条件] 产出结构化文档需要骨架时建
│   ├── tools/                          # [条件] 捆绑外部二进制 / 自研预构建 artifact 时建
│   └── samples/                        # [条件] 分析输入文件、需要示例时建
├── development/                        # 📝 开发过程资料（不交付；极简 skill 可以没有）
│   ├── plans/PLAN_vNN.md               # 历次设计蓝图（按版本归档）
│   └── findings/FINDINGS_<时间戳>.md   # 测试发现（追加归档）
└── tools-src/                          # 🛠️ tools/ 下自研 artifact 的源码（不交付；按需，见 §3.3）
    └── <artifact-name>/                # 每个 artifact 一个独立构建项目
```

**强度说明**：

- **[必需]** 所有 skill 都要有。三件套 = `.meta/VERSION` + `SKILL.md` + `README.md`
- **[条件]** 按 skill 实际能力决定；不需要的目录**不建**，空目录不要保留
- 一个纯 prompt 驱动的极简 skill（如"代码 review 规范助手"）可能只有三件套

### 3.2 核心目录约束

- **[硬]** 开发过程文档（plan、findings、调研笔记）只能放 `development/`；不得污染 `<skill-name>/`。
- **[硬]** `<skill-name>/` 里不得出现 `development/`、`plans/`、`findings/`、`tools-src/` 同名子目录。
- **[硬]** `<skill-name>/tools/` 只放**可运行的 artifact**（jar / exe / wasm / native binary）；**不放源码**。源码去 `tools-src/`（自研）或外部仓库（三方）。
- **[硬]** `<skill-name>/tools/` 下的 artifact **默认提交到 git**（含三方和自研）。用户 `git clone` 后 skill 即可运行，是"交付单元"定位的硬底线；`.gitignore` 不得默认排除 `tools/*.jar` / `tools/*.exe` 等。只有同时满足以下**任一**例外才允许走 bootstrap 脚本方案：
  - 许可证明确禁止 redistribution（如某些商用 SDK / Oracle 制品）
  - 单个 artifact 体积 > 50 MB（clone 体验显著下降时改用 Git LFS 或 bootstrap）
  - 企业环境要求二进制走内部镜像仓库（此时 bootstrap 目标指向内部源）
- **[硬]** 若走 bootstrap 方案：必须在 `tools/manifest.yaml`（或等价 lock 文件）里锁定 `name / version / url / sha256`；bootstrap 脚本**先校验哈希再落地**，任何校验失败立即报错退出。
- **[硬]** 升级 `tools/` 下 artifact 版本时：核对新版本 SHA-256 → 删旧 / 放新（文件名带版本号）→ 同步更新 `tools/README.md` 的版本引用和脚本常量（如 `TOOL_VERSIONS`）→ 按 §7 升 `.meta/VERSION`。
- **[软]** 新增目录前先对照本章判断是否已有归属；有归属就不新增目录。

### 3.3 `tools-src/` — 自研工具源码

- **[硬] 只装自研，不装三方**。三方工具（Vineflower、protoc 等）直接下载到 `<skill-name>/tools/`，在 `<skill-name>/tools/README.md` 里注明来源、版本、许可证。`tools-src/` 只装**本项目自研**的 artifact 源码，否则会变成三方代码镜像，失控。
- **[硬] 每个 artifact 一个子目录**：`tools-src/<artifact-name>/`，子目录名与 `<skill-name>/tools/<artifact-name>.<ext>` 里的 artifact 名保持一致，镜像关系一眼可见。
- **[硬] 独立构建单元**：`tools-src/<artifact-name>/` 有独立的 `pom.xml` / `build.gradle.kts` / `Cargo.toml` / `go.mod` 等；不与 skill 的编排脚本混合构建。
- **[软] 构建工具推荐**：
  - **JVM（Java/Kotlin/Scala）**：**Maven** —— 小工具 jar 场景低仪式感、稳、LLM / 人都易读；Gradle 只在 Android / 多模块 / 需要自定义任务编排时才值得
  - Rust：Cargo；Go：go modules；Python 打包二进制（罕见）：PyInstaller + setup.py
- **[硬] 发布流程**（每次 artifact 改动都走完）：
  1. 在 `tools-src/<artifact-name>/` 下构建（`mvn -q clean package` / `cargo build --release` / …）
  2. 拷贝产物到 `<skill-name>/tools/<artifact-name>.<ext>`
  3. 升 `<skill-name>/.meta/VERSION`（按 §7）
  4. 构建配置文件的版本号（`pom.xml <version>` / `Cargo.toml version` / …）与 skill `.meta/VERSION` **保持同步**（见 §7）
  5. 走 §2 交付前检查清单

### 3.4 常见变体

根据 skill 的能力来源，选最接近的形态：

**变体 A · Prompt-only skill**（最小）

只靠 SKILL.md 的提示词 + README 描述能力，不执行代码。`<skill-name>/` 只有三件套。

```
<skill-name>/
├── .meta/VERSION
├── SKILL.md
└── README.md
```

**变体 B · Python 编排 skill**（最常见）

`scripts/` 是主力，按阶段拆脚本；`knowledge/` 的 YAML 可迭代；`templates/` 留报告骨架。（历史样板 `skill-01-idea-plugin-analyzer` 已演进为纯 MCP + CLI 项目 `06-MCP 实战项目/mcp-python/mcp-idea-plugin-analyzer/`，不再是 Skill 形态；本变体模板仍然有效，新 Skill 可参考其 scripts/knowledge/templates 结构。）

```
<skill-name>/
├── .meta/VERSION, SKILL.md, README.md
├── scripts/phase_*.py        # 管线（A→B→C...）
├── knowledge/*.yaml          # 知识字典
├── templates/*.md            # 报告模板
└── tools/<third-party>       # 三方工具（如 Vineflower）
```

**变体 C · Java-capability skill**（含 JVM 能力）

Skill 需要 JVM 生态能力（反编译、字节码扫描、JVM 库等）时，**用 JAR 包装**。核心原则：**skill 是运行单元，不是构建单元** —— skill 里只装预构建 jar + 编排脚本，**不装 `.java` 源码**。用户只要有 JRE 就能跑，不需要 JDK + Maven。样板：`skill-01-case-converter`。

```
skill-XX-<name>/
├── <skill-name>/                       # 交付单元
│   ├── .meta/VERSION, SKILL.md, README.md
│   ├── scripts/<wrapper>.py            # Python 薄包装，调 java -jar
│   └── tools/<name>.jar                # 预构建 fat jar
└── tools-src/<name>/                   # 自研 jar 源码（不交付）
    ├── pom.xml
    ├── src/main/java/…
    └── src/test/java/…
```

Rust / Go / WASM capability 遵循相同原则：预构建二进制放 `<skill-name>/tools/`，源码在 `tools-src/`。

### 3.5 命名约定

- **[硬]** Skill 名用"**对象 + 动作**"风格，描述**功能**，不描述技术栈或 demo 性质：
  - ✅ `idea-plugin-analyzer`、`case-converter`、`log-summarizer`
  - ❌ `java-hello-world`（技术栈 + demo 词，不告诉读者这 skill 干什么）
  - ❌ `demo-skill`、`test-2`（完全无信息量）
- **[软]** 小写、连字符分隔；避免下划线 / 驼峰 / 大写。
- **[软]** 自研 artifact 名（`tools-src/<name>/`）与对应 artifact 文件名、skill 名**保持一致或一眼可对上**，减少阅读者在多个名字间做映射。例：skill 叫 `case-converter`，则 artifact 子目录也叫 `case-converter`，jar 叫 `case-converter.jar`，Java 包叫 `com.example.caseconverter`。

---

## §4 Plan 规则

### 4.1 何时必须新增 / 更新 Plan

**[硬]** 以下情况必须：

- 有功能性改动（见 §0 定义）
- 发现 bug 需要修复 —— 在"已知陷阱"章节记录
- 项目目录结构发生变更
- 输入 / 输出契约发生变更
- 引入新依赖（外部命令、库、模型能力）

**不适用于**：纯 typo / 格式 / 注释重写；跨版本行为一致的小幅性能优化（用 commit 说明即可）。

### 4.2 Plan 必须包含的章节

**[硬]** 八个必含章节：

- **目标与适用场景**
- **输入契约**
- **输出契约**
- **目录结构**（最新文件树）
- **阶段拆解**（每阶段单一职责、可独立运行）
- **依赖清单**
- **已知陷阱**（每次修复 bug 累积一条）
- **测试用例**（至少 3 个：正常、边界、异常）

**v01 首版例外**：测试用例部分可以只写"计划验证什么"；v02 起必须是实际用例清单。

### 4.3 版本策略

- **[硬]** 新 plan 命名 `PLAN_vNN.md`（v01、v02…），旧 plan 保留用于对比。
- **[硬]** 每份新 plan 顶部必须写"本次相对 v(NN-1) 的变更点"。

### 4.4 Good / Bad 示例

<details><summary>✅ GOOD — Plan 顶部变更点段落</summary>

```markdown
## 0. 本次相对 v01 的变更点

| 编号 | 变更 | 契约影响 |
|---|---|---|
| A | 新增 `config_keys_long` 桶 | strings 多一个 key（向后兼容） |
| B | `is_primary` 收紧为硬证据强制 | provider 条目多 `primary_tag` 字段 |

版本升位依据：A 是 schema 扩展 → MINOR；B 是语义收紧 → PATCH；取高位 → MINOR。
```
</details>

<details><summary>❌ BAD — 没有变更点段落</summary>

```markdown
## 1. 需求规格

本 skill 目标是分析 IDEA 插件...
（直接开始写需求，读者不知道相对 v01 改了什么，也不知道为什么新版）
```
</details>

---

## §5 README 规则

### 5.1 何时必须更新

**[硬]** 以下情况必须：

- 项目结构发生变更（新增 / 删除目录或关键文件）
- 添加了新功能 / 删除了功能
- 项目名字变更
- 使用方式变更（参数、触发词、前置依赖）
- 输出产物变更
- FINDINGS 中首次引入新样本，且其"插件类型 / 体积量级 / 特殊挑战"在 README **测试覆盖表**尚未体现 —— 必须新增一行或扩充已有行（见 §8.4）

### 5.2 必备章节

**[硬]**：

- 一句话简介
- 何时使用（与 SKILL.md 的 description 对齐）
- 前置依赖 / 安装方式
- 快速开始（最小可运行示例）
- 目录结构（与实际文件一致）
- 常见问题 / 已知限制

**[软] 不必备**：

- "版本历史" / "CHANGELOG" 段落 —— 需要详尽历史时放独立 `CHANGELOG.md`；否则让读者去 `FINDINGS_*.md` / `PLAN_v*.md` 或 git log 查。README 不做历史双写（见 §5.3）。

### 5.3 反模式（README 禁止的内容）

README 是**人读入口与目录指针**，不是易变信息的镜像。以下内容**禁止**出现：

- **[硬] 硬编码版本号**（反例：`当前版本：v0.4.6`）—— 改为"见 `.meta/VERSION`"，版本号更新不漂移。
- **[硬] 硬编码"最后更新"日期 / 页脚日期** —— 日期由 git log 提供；手写必漂。
- **[硬] 内嵌完整版本变更历史** —— 放独立 `CHANGELOG.md`，或让读者去对应 `FINDINGS_*.md` / `PLAN_v*.md` 查；README 不与 FINDINGS 做内容双写。
- **[硬] 枚举 `development/plans/` 或 `development/findings/` 下的具体文件名 / 时间戳 / "第 N 轮"编号** —— 用"目录指针 + 命名约定"替代（如"`PLAN_v<编号>.md`，编号越大越新，最新版为权威"、"`FINDINGS_<YYYYMMDD>_<HHMMSS>.md`，按时间戳排序即为测试轮次"）。
- **[软] 第三方工具 / 库的具体版本号**（反例：`Vineflower 1.11.2`、`Kotlin 1.9.0`）—— 指向安装说明文档（`tools/README.md` 等）或 `requirements.txt`；升级时不必同步改 README。

**判别原则**：

| 内容特性 | README 怎么写 |
|---|---|
| 会随时间 / 版本漂移（版本号、日期、FINDINGS 清单、依赖版本） | **指针** —— 指向唯一事实源，描述命名约定 |
| 改动必然同步到 README（模块结构、核心脚本阶段、CLI 子命令） | **可以列内容** —— 本来就是 README 的核心职责 |

例：`scripts/phase_*.py` 列表相对稳定（改脚本架构时必动 README），可列；`development/findings/` 每轮测试都新增文件，必漂，只能指针。

### 5.4 Good / Bad 示例

<details><summary>✅ GOOD — 指针式引用</summary>

```markdown
当前版本号见 `<skill-name>/.meta/VERSION`；每版改了什么去 `development/findings/`
对应 `FINDINGS_*.md` 查。

### 📋 计划文档 — `development/plans/`

文件名形如 `PLAN_v<编号>.md`，编号越大越新，最新版为当前权威蓝图。
```
</details>

<details><summary>❌ BAD — 镜像式枚举（必漂）</summary>

```markdown
当前版本：**v0.4.6**

### 计划文档
- PLAN_v01.md — 设计蓝图

### 测试发现
- FINDINGS_20260419_201133.md — 第一轮回归测试记录
- FINDINGS_20260419_202809.md — 第二轮回归测试记录
- ...（每轮新增都要手改 README）

*最后更新: 2026-04-20*
```
</details>

---

## §6 SKILL.md 规则

- **[硬]** frontmatter 必须包含：`name`、`description`（含精确触发词），必要时 `allowed-tools`。
- **[硬]** `description` 要明确"什么时候触发、什么时候不触发"，避免误匹配。
- **[软]** 正文顺序：何时使用 → 前置依赖 → 参数 → 工作流步骤 → 输出。
- **[硬]** SKILL.md 与 README.md 的功能描述必须一致（违反将同时违反 §1.2 三者同步）。

### 6.1 description 写法 Good / Bad

<details><summary>✅ GOOD — 触发/不触发都写清</summary>

```yaml
description: >
  分析 JetBrains IDE 插件的 zip/jar 产物，输出插件用途、依赖清单、扩展点矩阵。
  触发条件：用户提供本地 .zip/.jar/已解压目录，意图是"分析/理解/逆向 IDEA 插件"。
  不触发：远程 URL、非 JetBrains 产物、插件市场搜索。
```
</details>

<details><summary>❌ BAD — 只写能做什么</summary>

```yaml
description: 分析 IDEA 插件
```

这种 description 会被 Claude 在任何"插件"相关对话里错误拉起。
</details>

---

## §7 版本号规则

`.meta/VERSION` 采用语义化版本 `MAJOR.MINOR.PATCH`：

- **MAJOR**：目录结构 / 输入输出契约的不兼容变更
- **MINOR**：新增功能、扩展知识库（向后兼容）
- **PATCH**：bug 修复、文案修正、性能优化

**[硬]**：每次改动合并前，按最高一档升位，**不得忘记升 VERSION**。

**[软]**：若同一次改动涉及多档，取最高档（如 MINOR + PATCH → MINOR）。

**[硬] `tools-src/` 版本同步**：若项目有 `tools-src/`，**所有 `tools-src/<artifact>/` 构建配置文件里的版本号（`pom.xml <version>` / `Cargo.toml version` / `build.gradle.kts version` / ...）必须与 `<skill-name>/.meta/VERSION` 保持一致**。改一个必须同时改另一个；否则"jar 实际版本 vs skill 声称版本"错位，回溯困难。

**不适用于**：对 `development/` 下文件的改动（这部分不参与交付，不影响版本）。

---

## §8 FINDINGS 规则

### 8.1 FINDINGS 标准结构

**[硬]** 每份 FINDINGS 文件按以下顺序组织：

1. **头部（元信息）**
   - 测试时间
   - Skill 版本（对应 `.meta/VERSION`）
   - 样本来源（哪个仓库 / 哪批数据 / 哪个用户反馈）
   - 运行环境（OS、Python/Node 版本、相关依赖版本）

2. **样本清单与结果矩阵**（表格）
   - 列出本轮测试用到的所有样本与每个样本的通过 / 失败结论

3. **覆盖率 / 量化指标矩阵**（可选）
   - 若有可量化指标（识别率、准确率、耗时等），列表格

4. **发现列表**（按严重度分级）
   - 每条发现包含固定字段：
     - **现象**：观察到的行为
     - **触发样本**：哪个样本 / 哪个输入触发
     - **根因假设**：初步判断的原因
     - **建议修复**：怎么改（规则见 §8.3）
     - **影响范围**：波及哪些功能 / 哪些场景

### 8.2 严重度分级

**[硬]** 每条发现必须打上标签：

- **P1**：功能阻断 / 数据错误（必须修）
- **P2**：体验差（下个版本修）
- **P3**：小幅打磨（有空再修）

### 8.3 "建议修复" 字段的写法

- **[软] 允许列多个方案**：若有多种可行改法，全部列出（方案 A / B / C），每个方案写清做法、优点、代价。
- **[硬] 必须明确推荐一个**：结尾用 `> 推荐：方案 X，理由：……` 的格式给出首选方案和理由，方便后续大模型或开发者直接按推荐执行。
- **[硬] 单一方案也要写理由**：只有一个方案时，同样写 `> 推荐：方案 A，理由：……`。
- **[软]** 推荐理由至少覆盖一点：**实现成本 / 风险 / 与现有架构契合度 / 长期可维护性**。

### 8.4 归档规则

- **[硬]** FINDINGS 路径 `development/findings/FINDINGS_<YYYYMMDD_HHMMSS>.md`。
- **[硬]** **一次测试一个文件，不覆盖、不合并历史 FINDINGS**。
- **[硬]** 若某问题确认是 bug → 同步回 plan 的"已知陷阱"，再修代码。
- **[硬]** 若本轮样本清单中出现 README **测试覆盖表**尚未登记的新样本（新类型 / 新体积量级 / 新特殊挑战），同步回 README 测试覆盖表，再归档 FINDINGS。已有行能代表同类挑战的可以不新增行；判断标准："未来读者只看 README 能否意识到这类插件也能跑"。

---

## §9 Knowledge 规则

- **[硬]** 统一用 YAML，按主题分文件（如 `plugin_taxonomy.yaml`、`extension_points.yaml`）。
- **[硬]** 每个 YAML 文件顶部写清用途、字段含义、维护方法。
- **[硬]** 新增条目前先搜现有文件，避免重复定义。
- **[软]** 知识库扩充属 MINOR 版本。

常见问题（FAQ）：

- **新增 YAML 文件怎么被脚本读到？** 在 `scripts/common.py::load_knowledge()` 里加一行加载。
- **条目冲突怎么处理？** 外部 YAML 覆盖代码内字典，YAML 之间按 `load_knowledge` 的合并顺序；在 PR 描述里说明。
- **YAML 条目是否要测试？** 涉及行为变化的新增条目应在 FINDINGS 里至少留一个 smoke case。

---

## §10 Scripts 规则

本章规则对象是 `<skill-name>/scripts/` 下的**编排层代码**（Python / Bash / Node 皆可），职责是解析参数、串阶段、调 `tools/` 里的 artifact、拼装输出。**重型逻辑**（JVM 反编译、Rust native 处理等）不写在这里，属于自研工具，走 `tools-src/`（见 §3.3）。

- **[硬]** 单一职责：一个脚本只干一件事。
- **[硬]** 命名体现执行顺序或阶段（`01_extract.py`、`phase_a_parse.py`）。
- **[硬]** 幂等：相同输入重复运行产出一致。
- **[硬]** 可独立运行（`if __name__ == "__main__"`），便于 debug。
- **[软]** Python 脚本目标版本 ≥ 3.10；用 `py` launcher 调用（Windows 习惯，避免落到 Microsoft Store 占位符）。
- **[软]** 跨平台路径用 `pathlib.Path`，不用字符串拼接；控制台输出显式 `PYTHONIOENCODING=utf-8` 或 `chcp 65001` 兜底。
- **[软]** 日志用 `print()` 或结构化 log，避免静默失败；关键阶段输出耗时与产出路径。
- **[选]** 复杂状态传递用 dataclass（如 `common.Context`），而非 globals。

---

## §11 Bug 修复流程（索引）

Bug 修复涉及的规则已经在其他章节写清，这里只列流程与引用：

1. 在最新 plan 的"已知陷阱"章节记录现象与根因 → §4.2
2. 修复代码（遵守 §10 脚本规范 / §12 注释规范）
3. 补充 / 更新测试用例 → §4.2 / §8.1
4. 升 PATCH 版本号 → §7
5. 若修复改动了目录或契约 → 同步更新 README.md、SKILL.md → §1.2 / §5 / §6
6. 本轮验证归档到 FINDINGS → §8

---

## §12 源码注释政策

源码（`scripts/*.py`、`knowledge/*.yaml` 的注释、`templates/*` 里的非模板注释）**只保留当前逻辑的 WHY**，用**中文**，不写历史轨迹。

### 12.1 注释的内容规则

**[硬] 必须删除**（历史轨迹）：

- 版本引用：`v0.4.6 新增...`、`0.5.0 改为...`、`此前...`、`旧:... 新:...`
- 跨文档指针：`See FINDINGS_20260420_072813.md H1`、`PLAN_v02.md §3 L5`、`M5 / G2 / H3` 这类编号
- 具体样本叙事：`Copilot 样本触发此 bug`、`IdeaVim 182 条中 91% 是 SMAP`
- 具体数字统计：`减少了 41 条`、`dropped 80%`

**[硬] 必须保留**（当前 WHY）：

- 非显然的领域约束：*"kotlinc 为 inline 函数合成 SMAP 块，不是业务文本"*
- 环境 / 系统行为：*"NTFS 大小写折叠导致 `Path.exists()` 返回 True 但 `rglob` 给磁盘真实大小写"*
- 数据契约里的隐含前提：*"stub-primary 插件的主 jar 没有 class，需要扫 companion jars"*
- 看似多余但必要的代码：*"这里必须先去重再排序，否则 `Counter.most_common()` 会把重复 key 算两次"*

**检验标准**：删掉这条注释后，3 个月后的读者（或大模型）看懂这段代码的难度是否明显上升？上升就保留，不上升就删。

### 12.2 历史信息的归处

历史信息不放在源码里，但也不丢弃 —— 转到对应档案：

- 版本间变更叙事 → `PLAN_vNN.md` 顶部"本次相对 v(NN-1) 的变更点"
- 具体 bug → `PLAN_vNN.md` §"已知陷阱" + 相应 `FINDINGS_*.md`
- 变更原因 / 过程 → commit message、PR 描述
- 样本触发的具体数字 → FINDINGS 文件里的"量化矩阵"

### 12.3 例外（可以出现版本号 / 变更引用的地方）

- `.meta/VERSION` 文件本身
- `knowledge/*.yaml` 的**文件顶部**"维护说明"段（告诉后来者怎么扩展这个 YAML）
- PLAN / FINDINGS / README / SKILL.md 这些**文档**文件

### 12.4 注释语言：中文

**[硬]** 源码 **以及配置文件** 中所有面向人的自由文本注释**一律用中文**，覆盖：

- 代码注释：`#`、`//`、`/* */`、docstring 里的叙述段落
- 配置文件注释：YAML / TOML / `.properties` / `.env` / `pyproject.toml` / `pom.xml` / `package.json` 旁的 `*.config.{js,ts}` / Dockerfile / Makefile / shell 脚本 / `knowledge/*.yaml` / `.claude/settings.json` 注释段 / nginx conf 等任何允许写注释的文件

保留英文：
- 标识符、字段名、枚举值、API 名称（`EXCLUDE_PREFIXES`、`is_primary`、`SourceDebugExtension`）
- 函数签名、类型注解、字符串字面量
- 第三方文档链接、Maven 坐标、类 FQN
- 中文注释里引用技术名词可以原文穿插（如 *"kotlinc 的 `SourceDebugExtension` 属性"*）

**[软]** 反模式：
- 中英混排一行（*"这个函数 checks that ..."*）—— 要么全中要么全英，不混
- 机械翻译腔（*"此方法执行一个扫描"*）—— 按中文自然表达（*"扫描主 jar 的 constant pool"*）

**[软] 执行时点**：
- 新增注释 / 配置：直接写中文
- 修改已有函数 / 块 / 配置项时：顺手把周围的英文注释翻成中文
- 不要求一次性翻译所有历史注释，边动边改即可

---

## §13 自动化与 hook 建议

以下规则可以通过 git hook / CI / 脚本自动验证，优于人工记忆：

| 规则 | 可用的自动化手段 |
|---|---|
| §2.4 `.meta/VERSION` 已升 | pre-commit hook：`git diff --cached --name-only \| grep -q .meta/VERSION` |
| §2.2 README 目录与实际一致 | 生成目录 snapshot 后 diff，差异 > 0 时阻止 commit |
| §5.3 README 无硬编码版本号 | `grep -nE '当前版本[：:]\s*\**v?\d+\.\d+\.\d+' <skill>/README.md` 返回 0 行 |
| §5.3 README 无"最后更新"日期 | `grep -nE '最后更新[：:]\|Last updated' <skill>/README.md` 返回 0 行 |
| §5.3 README 无 development 下文件枚举 | `grep -nE 'PLAN_v\d+\.md\|FINDINGS_\d{8}_\d{6}' <skill>/README.md` 返回 0 行 |
| §8.4 新样本已同步 README 测试覆盖表 | 脚本：解析最新 FINDINGS 的样本清单表，逐个样本名在 README "测试覆盖"小节 `grep`；未命中且无同类行时报警 |
| §2.3 SKILL.md 与 README description 一致 | 脚本解析两个文件的 description 段落后 diff |
| §12.1 源码无历史轨迹 | `grep -rnE 'v0\.\d\|FINDINGS_\|PLAN_v\|此前' <skill>/scripts/` 返回 0 行 |
| §12.4 源码注释用中文 | 正则检测 docstring / 行内注释里的"全英文叙述段落"，给警告 |
| §8.4 FINDINGS 文件命名合规 | 正则验证 `FINDINGS_\d{8}_\d{6}\.md` |

**[选]** 建议每个 skill 项目在 `development/hooks/` 下提供这些脚本；`.claude/settings.json` 里绑定到对应事件（commit / stop / edit）。

---

## §14 规则本身的演化

本文件（`skill_develop_rule.md`）不是永久不变的，它本身也要像其他代码一样演化。演化规则：

- **[硬]** 变更走常规 git commit，commit message 清晰说明"为什么改这条规则"。
- **[硬]** 新增 / 删除 / 修改 MUST 级规则时，commit message 里必须给出反例（什么情况触发了这条规则的修改）。
- **[软]** 新规则上线前先在一个 skill 项目试跑一轮；成熟后再写进本文件。
- **[软]** 规则冲突时，本文件 §1 最高原则 > 后续各章节 > SOP-01~05（§15）。
- **[选]** 每季度回顾一次本文件：哪些规则从未被违反？哪些规则反复被违反？前者可以删，后者需要强化或改 hook。

---

## §15 配套资料 / SOP 参考

本文件管"操作规则"（做什么 / 不做什么）；**技术细节**见 `01-Skill 学习笔记/03-规范与标准/` 下的 SOP 文档：

| 文件 | 主题 |
|---|---|
| SOP-01 | Skill 项目初始化与目录脚手架 |
| SOP-02 | Plan 撰写的具体模板与样例 |
| SOP-03 | Knowledge YAML 的 schema 约定 |
| SOP-04 | Scripts 的调试与性能基线 |
| SOP-05 | FINDINGS 的写作规范与复盘方法论 |

若 SOP 与本文件冲突，**本文件优先**（本文件是强制规则，SOP 是参考说明）。SOP 如需提升为强制规则，走 §14 流程。
