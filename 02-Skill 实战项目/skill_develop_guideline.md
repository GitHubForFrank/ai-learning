# Skill 开发指引（提示词驱动版）

> **前提**：本仓库的 skill 全部由**大模型（Claude 等）**作为开发者来实现，人只负责**给指令 + 审核产出**。
> **本指引作用**：给出每一步的**提示词模板**，复制改几个占位符即可交给大模型执行。
> **与其他文档的关系**：
> - `skill_develop_rule.md` → 约束（大模型必须遵守的规则）
> - `skill_develop_guideline.md`（本文件）→ 步骤 + 提示词模板
> - 样板：
>   - `skill-01-case-converter/` — Java-capability skill（变体 C），演示 `tools-src/` + Maven 布局
>   - 历史 Python 编排 skill（变体 B）样板 `skill-01-idea-plugin-analyzer/` 已演进为纯 MCP + CLI 项目，迁至 `06-MCP 实战项目/mcp-python/mcp-idea-plugin-analyzer/`；其 `development/AI提示词笔记.txt` 仍保留，可参考真实开发对话

---

## 人机分工速查

| 人做的事 | 大模型做的事 |
|---|---|
| 想清楚"要解决什么问题" | 把需求扩写成 PLAN |
| 把提示词复制给大模型 | 写代码、建目录、跑测试 |
| 提供样本、审核产出 | 生成 FINDINGS、回归修复 |
| 决定是否升 MAJOR 版本 | 执行升版、同步改 README/SKILL.md |

**人要守住的底线**：
1. 每段提示词首行都写 `【必读】所有开发必须遵循 skill_develop_rule.md ！！！`（见 Step 0）
2. 审核大模型改 PLAN / SKILL.md / README.md 三者是否同步
3. 验证大模型声称"修复完成"的条目，在新一轮 FINDINGS 里确实消失了

---

## 总览：开发流水线

```
⓪ 开工前缀（每次对话必带）
① 需求草稿（人）   → ② 让大模型扩写 PLAN → ③ 迭代打磨 PLAN（几轮对话）
   → ④ 启动实现   → ⑤ 跑样本 + 生成 FINDINGS
   → ⑥ 按 FINDINGS 反向优化（严格按 rule §9 的 5 步）
   → ⑦ 专项扩展（新开关 / 知识库扩充 / 质量修复 / 补 README）
   → ⑧ 交付前清单（对齐 rule §10）
```

每一步下面都给出：**目标 / 产出物 / 提示词模板 / 审核要点**。

占位符统一写法：
- `<SKILL_NAME>` — skill 名（如 `idea-plugin-analyzer`）
- `<SKILL_ROOT>` — skill 核心目录绝对路径
- `<DEV_ROOT>` — 开发过程文档目录（`development/`）
- `<SAMPLES_DIR>` — 批量测试样本目录
- `<INPUT_PATH>` — 单个样本路径
- 三个反引号里的内容按需替换

---

## Step 0 · 开工前缀（每次对话必带）

**一句话规则**：每段提示词的第一行都写：

```
【必读】所有开发必须遵循 skill_develop_rule.md ！！！
```

**为什么必须要**：`skill_develop_rule.md` 是硬约束（目录结构、版本策略、三文档同步、反模式）。大模型没读，十有八九会违反 —— 尤其是旧 PLAN 覆盖、VERSION 忘升、核心目录混入调研笔记。

**额外建议**（按需叠加，非必需）：
- 涉及修 bug / 迭代：再加一句"先读最新 PLAN 和最近的 FINDINGS"
- 涉及新功能：再加一句"确认 PLAN 的输入/输出契约是否需要升 v(NN+1)"

**审核要点**：大模型回复开头应能看出它读了 rule（比如明确提到"已遵循 rule §X"或列出了 rule 中的关键约束）；若只是机械应答就打断重来。

---

## Step 1 · 需求草稿（人自己想，0.5~2 小时）

**目标**：用大白话写清楚"要解决什么问题"，不追求完整，够让大模型理解即可。

**人写下来的内容（5 行以内）**：
1. 想做一个什么 skill？解决谁的什么问题？
2. 输入是什么？（文件类型 / 目录 / URL …）
3. 期望输出什么？（报告 / 结构化数据 / 代码骨架 …）
4. 哪些请求**不**应该触发这个 skill？
5. 有没有已经想到的技术选型倾向？（不确定就留空）

这一步**不用**写提示词，写完直接进入 Step 2。

---

## Step 2 · 让大模型扩写成 PLAN v1（1 次对话）

**目标**：大模型把 1~3 句话的需求草稿，扩写成结构化、可落地的设计蓝图。

**产出物**：`<DEV_ROOT>/plans/PLAN_v01.md`

**提示词模板**：

```
【必读】所有开发必须遵循 skill_develop_rule.md ！！！

我想写一个 Skill，<一句话描述 skill 干什么>。

我手头只有 <输入形态>，希望最终能够产出以下内容：
  - <产出 1>
  - <产出 2>
  - <产出 3>

请帮我判断：
  1. 这个 Skill 用什么语言/技术栈实现比较合适？给出理由。
  2. 整体的设计思路应该是什么样的？

把需求和设计思路按 rule.md §4.2 规范写成 plan，
输出到 <DEV_ROOT>/plans/PLAN_v01.md。
```

**审核要点**：
- 目标章节是否说清"给谁、在什么场景、从什么到什么"
- 阶段拆解是否做到"单一职责"（一个阶段描述超过 3 行就该拆）
- 有没有在 skill 核心目录外乱建文件

---

## Step 3 · 迭代打磨 PLAN（2~5 次对话）

**目标**：把 PLAN v1 中不完整、不准确、过度设计的地方修正。

**何时必须更新 / 新建 PLAN**（对齐 rule §4.1）：
- 有功能性改动（新增 / 修改 / 删除能力）
- 发现 bug 需修复 → 记到"已知陷阱"章节
- 项目目录结构变更
- 输入 / 输出契约变更
- 引入新依赖（外部命令、库、模型能力）

**版本策略**（对齐 rule §4.3 + §7）：
- **原地改**：在当前 PLAN_vNN.md 内做内容迭代打磨，不升版本号
- **新建 PLAN_v(NN+1).md**：只在契约 / 目录结构 / 阶段拆解发生**不兼容变更**时触发；新文件顶部必须写"本次相对 v(NN) 的变更点"章节，旧文件保留不删

**3.1 通用能力打磨**

```
【必读】所有开发必须遵循 skill_develop_rule.md ！！！

这个 Skill 不应只针对某一个具体样本/场景，而要具备通用性。
请以「资深 <领域专家> + Agent Skill 设计专家」的视角，
重新审视并优化需求，更新 <DEV_ROOT>/plans/PLAN_v<最新>.md。
重点：
  - 抽象出"类型指纹 / 分类法"，避免把个案硬编码
  - 知识字典化：把易变的领域知识从代码剥离到 YAML
  - LLM 合成边界：哪些事脚本做、哪些事 LLM 做，划清楚
```

**3.2 增强分析维度 / 补能力**

```
【必读】所有开发必须遵循 skill_develop_rule.md ！！！

继续优化 plan：
  - 需要识别 <某个关键维度>
  - 适当在产出中引入 <图示 / 表格 / 对比 / ...>
  - 考虑 <边界场景>
更新 <DEV_ROOT>/plans/PLAN_v<最新>.md。
```

**3.3 能力评估（必要时）**

```
【必读】所有开发必须遵循 skill_develop_rule.md ！！！

继续评估：<新能力 X> 是否值得在 Skill 里内建支持？
还是说由用户 / 上层 agent 外部编排就够了？
给出：
  - 内建的成本（开发量 / 维护 / token 预算）
  - 外建的可行性
  - 你的推荐选择 + 理由
把结论更新到 plan。
```

**3.4 清理元信息**

```
【必读】所有开发必须遵循 skill_develop_rule.md ！！！

优化 plan 文件：
  - 不要出现 "v1 / v2 / 这一版是..." 之类的迭代元信息
  - plan 文档里直接呈现当前最终方案即可
  - 文件名保留 PLAN_v<NN>.md（用于归档，不是用于正文描述）
```

**审核要点**：
- 每轮优化后重读"目标 / 输入契约 / 输出契约"三件套，确保没被改跑偏
- 发现大模型往 plan 塞"假日志"（"本次修改了 X"），让它清理

---

## Step 4 · 启动实现（1 次开工对话 + 过程观察）

**目标**：大模型按 PLAN 建目录、落脚本、填知识库、写 SKILL.md。

**产出物**：完整的 `<SKILL_ROOT>/` 可执行 skill + 初版 `README.md` + `.meta/VERSION = 0.1.0`。

**提示词模板（主命令）**：

```
【必读】所有开发必须遵循 skill_develop_rule.md ！！！

按照 <DEV_ROOT>/plans/PLAN_v01.md 开始正式编写这个 Skill。
Skill 核心目录放在：<SKILL_ROOT>

执行流程：
  1. 按 rule.md §3 建标准目录结构（按能力选变体 A/B/C；不需要的目录不建）
  2. 按 PLAN 的阶段顺序（A→B→C…）落 scripts/，一阶段一文件，
     遵守 rule.md §10（单一职责 / 幂等 / 可独立运行，共享工具进 common.py）
  3. knowledge/ 下 YAML 填 MVP（10~20 条），每个文件顶部写"用途 + 字段含义"
  4. templates/ 下报告模板留 <!-- CLAUDE_FILL: xxx --> 占位符
  5. 按 rule.md §6 写 SKILL.md（触发词要精确）
     按 rule.md §5 写 README.md（注意 §5.3 反模式：不硬编码版本号 / 不内嵌变更历史）
  6. 每完成一个阶段，用 samples/ 下最简单的样本肉眼校验一次
  7. 设置 .meta/VERSION = 0.1.0；若用了 tools-src/，同步 pom.xml/Cargo.toml 的版本号（rule §7）

本领域特有的大原则（rule 之外的附加约束）：
  - 脚本出"确定性事实"，LLM 出"有判断的合成"，边界不能糊
  - 所有大输出（反编译 / 字符串挖掘 / …）必须有 --cap 参数，超限自动降级
```

**提示词模板（过程中需要配套 commands 时）**：

```
为 Skill 目录 <SKILL_ROOT> 编写对应的 commands，
可参考已有范例：
  @file:/D:/workspaces/workspace_gitee/zmz-ai-skills/.claude/commands/

产出的 md 文件先放到 00-temp 目录下，确认后再迁入正式位置。
```

**审核要点**：
- 目录结构是否严格对齐 `skill_develop_rule.md §3`（含变体 A/B/C 的判断）
- 有无在核心目录里放开发日志 / 临时文件
- SKILL.md 的 `description` 里触发词是否精确（不易误匹配）
- 跑一次最简单样本，确认端到端能出产物

---

## Step 5 · 样本实战 + FINDINGS（每轮 1~4 小时）

**目标**：把 skill 扔到不同类型样本上跑，把问题沉淀成 FINDINGS 文档。

**产出物**：`<DEV_ROOT>/findings/FINDINGS_YYYYMMDD_HHMMSS.md`

**5.1 单样本验证**（联调用）

```
【必读】所有开发必须遵循 skill_develop_rule.md ！！！

使用 <SKILL_NAME> 这个 Skill 分析下面目录/文件中的 <领域对象>，
分析报告跟原产物放到同一目录：
  <INPUT_PATH>
```

**5.2 批量回归测试**（主要玩法）

```
【必读】所有开发必须遵循 skill_develop_rule.md ！！！

为了让如下 Skill 更加健壮：
  <SKILL_ROOT>

我准备了一批样本，放在如下目录：
  <SAMPLES_DIR>
需要注意：<忽略规则，如 .idea 目录要跳过>

请逐个运行 Skill 分析这些样本。发现 Skill 有需完善之处
（异常 / 产出不符预期 / 边界场景未覆盖 / 性能问题等），
按 rule.md §8 规范写 FINDINGS 文件。

额外要求：每条发现按 P1 / P2 / P3 分级
  P1 = 功能阻断 / 数据错误
  P2 = 体验差
  P3 = 小幅打磨
```

**5.3 面向某个专题的复盘**（按需）

```
假设我要基于当前的分析报告 <做 X 事>（比如"仿制同类插件"），
现有分析还缺哪些关键信息？哪些维度可以进一步优化？
结论按 rule.md §8 追加到最新的本轮 FINDINGS 文件。
```

**审核要点**：
- FINDINGS 是否覆盖了**梯度样本**（小 / 中 / 大，干净 / 混淆 / 加密 …）
- 每条发现是否有"可验证"的根因与修复建议，而不是"笼统吐槽"
- 大模型有没有偷懒（只跑了一半样本 / 只写了现象没写根因）

---

## Step 6 · 按 FINDINGS 反向优化（循环）

**目标**：把 FINDINGS 里的 P1/P2 条目按 rule §11 的标准流程落实成修复。

**Bug 修复标准流程**（对齐 rule §11）：

```
① 在最新 PLAN 的"已知陷阱"章节记录现象与根因
② 修复代码
③ 补充 / 更新测试用例
④ 升 PATCH 版本号（若涉及新功能升 MINOR，契约不兼容升 MAJOR）
⑤ 若修复改动了目录或契约 → 同步更新 README.md、SKILL.md
```

**提示词模板**：

```
【必读】所有开发必须遵循 skill_develop_rule.md ！！！

根据 <DEV_ROOT>/findings/FINDINGS_<时间戳>.md 中列出的 P1/P2 问题，
反向优化 Skill <SKILL_NAME>。

严格按 rule.md §11 的 5 步流程走：
  每条发现都走完 ①记 PLAN → ②改代码 → ③补测试 → ④升 VERSION → ⑤同步三文档

回归验证（用**同一批样本**重跑）：
  - 确认上一轮 P1/P2 条目确实消失，且无新回退
  - 结果按 rule.md §8 写成新的 FINDINGS，
    顶部标注"对 <上一版本号> 的回归验证，本轮修复了 ① ② ③"
```

**审核要点**：
- **三文档同步检查**：PLAN / README.md / SKILL.md 的功能描述、目录结构、参数契约是否一致
- 回归样本是否是**同一批**（换样本等于作弊）
- VERSION 是否按语义化规则升位（多条修复取最高一档）
- "已知陷阱"章节是否真的累积（不是被覆盖/精简）

---

## Step 7 · 专项能力扩展（按需多次）

这些是早期 skill-01（已迁为 MCP 项目 `mcp-idea-plugin-analyzer`）真实走过的迭代，可按需套用。

**7.1 新增一个开关 / 模式**

```
【必读】所有开发必须遵循 skill_develop_rule.md ！！！

继续优化 Skill <SKILL_NAME>：

需求：<描述新开关/模式，如"骨架代码同时产 Kotlin 和 Java 两版">
原因：<业务动机，如"我对 Kotlin 语法不熟，双版本方便对照阅读">

约束：
  - 保留当前默认行为（向后兼容）
  - 新增 CLI 参数控制（例：--decompile-lang java|kotlin|both，默认 both）
  - PLAN / README / SKILL.md 三处文档同步更新
  - 升 MINOR 版本号
  - 跑一轮回归，确认原有行为无回退
```

**7.2 质量修复 + 抓根因**

```
【必读】所有开发必须遵循 skill_develop_rule.md ！！！

继续优化 Skill <SKILL_NAME>：

问题：<现象描述，如"当前反编译得到的 kt/java 文件与原始 jar 差异较大">
排查方式：
  1. 在 samples/ 里用 <某个代表性样本> 复现
  2. 对比 <原始文件> vs <产出文件>，量化差距（计数 / 百分比）
  3. 定位根因到具体阶段（Phase X 的哪个函数）
  4. 给出修复方案 + 验证命令

修复完成后：
  - 校验每个产物的"应在且正确"（数量 + 抽样内容）
  - 在 FINDINGS 里追加"根因 + 修复验证"记录
  - 升版本（PATCH 或 MINOR，视影响面定）
```

**7.3 写 / 补 README**

**何时必须更新 README**（对齐 rule §3.1）：
- 项目结构变更（新增 / 删除目录或关键文件）
- 新增 / 删除功能
- 项目名字变更
- 使用方式变更（参数、触发词、前置依赖）
- 输出产物变更

```
【必读】所有开发必须遵循 skill_develop_rule.md ！！！

为下面这个 Skill 写一份 README.md：
  <SKILL_ROOT>

README 必备章节（对齐 rule.md §5.2；另注意 §5.3 反模式）：
  - 一句话简介
  - 何时使用（与 SKILL.md 的 description 对齐）
  - 前置依赖 / 安装方式
  - 快速开始（最小可运行示例，一条命令能跑出结果）
  - 目录结构（与实际文件一致）
  - 常见问题 / 已知限制

在此之上额外重点覆盖：
  1. Skill 自身的目录结构说明（每个目录/文件各司其职）
  2. 使用 Skill 后 output 目录的结构及内容说明
     - 对每个产物标注「人读 / 机读 / 中间产物」
     - 按使用目的给一张"先看 X 再看 Y"的导航表
     - 列清楚产物与 depth/模式的关系（哪些模式产哪些）

要求：README 与 SKILL.md 的功能描述必须一致（三者同步铁律）。
```

**7.4 知识库扩充**（Knowledge YAML 迭代）

**触发场景**（对齐 rule §7）：
- FINDINGS 中观察到"新类型样本未被覆盖"（例：新 LLM 提供商、新扩展点、新依赖坐标）
- 字典条目命中率偏低，希望补全长尾
- 字典条目本身有错漏需要订正

```
【必读】所有开发必须遵循 skill_develop_rule.md ！！！

扩充 <SKILL_ROOT>/knowledge/<某个字典>.yaml 知识库。

上下文：
  - 触发需求：<描述，如"翻译类插件的 llm_providers_guess 命中率低，
    需要补 Baidu / Google / Bing / DeepL / Youdao 等">
  - 证据来源：<某个样本的 analysis.json.strings.domains 清单 / 官方文档链接 / ...>

执行规则（对齐 rule.md §9）：
  1. 先读现有 YAML，避免重复定义（新条目不能和已有条目冲突）
  2. 新增条目必须带字段注释；保持文件顶部"用途 + 字段含义"块最新
  3. 若新增字段（而非条目），PLAN 的知识字典章节要同步扩写
  4. 知识库扩充属 MINOR 版本升级，改完升 .meta/VERSION
  5. 用现有样本跑一次回归，确认：
     - 新条目在至少 1 个真实样本上命中
     - 老条目命中率不下降（没有误改）
  6. 在 FINDINGS 里追加"本次扩充了 X 条，命中样本 Y/Z"
```

---

## Step 8 · 交付前检查清单

**目标**：每次改动收尾前，让大模型对照 rule §10 的硬清单逐项自检。

**提示词模板**：

```
【必读】所有开发必须遵循 skill_develop_rule.md ！！！

请对照以下清单逐项检查 <SKILL_NAME>，
不通过的项列出具体文件路径 + 具体问题 + 如何修复，不要只答"已通过"。

rule.md §2 硬清单：
[ ] 最新 plan 已更新，且与代码现状一致
[ ] README.md 的目录结构、功能列表与实际一致（无硬编码版本号 / 最后更新日期 / development 下文件枚举 —— §5.3）
[ ] SKILL.md 的 description、工作流与 README 一致
[ ] .meta/VERSION 已按语义化规则升位；若有 tools-src/，pom.xml/Cargo.toml 等版本号已同步（§7）
[ ] 若有 bug 修复 → plan 的"已知陷阱"已补充
[ ] 本轮测试的 FINDINGS 已归档；若引入新样本 → README 测试覆盖表已同步（§8.4）
[ ] 旧 plan、旧 findings 未被删除或覆盖

附加检查：
[ ] samples/ 下保留至少 1 个能完整跑通的示例
[ ] 开发过程文档没有污染核心目录（development/ 外无调研笔记）
[ ] 所有大输出有 token 上限 / 降级策略
[ ] SKILL.md 的 frontmatter description 触发词精确（含反触发条件）
[ ] knowledge/ 下每个 YAML 顶部有"用途 + 字段含义"块
[ ] scripts/ 下每个 phase 脚本可独立运行（幂等 + 单一职责）
[ ] 若有 tools-src/：jar/artifact 版本号 = skill .meta/VERSION；tools/ 下只有预构建 artifact，无源码

对每条不通过项，给出：
  - 具体文件绝对路径
  - 问题描述（当前是什么 / 应该是什么）
  - 修复命令或补丁片段
```

---

## 附录 A · 提示词使用技巧

1. **首行必写 `【必读】所有开发必须遵循 skill_develop_rule.md ！！！`**（Step 0 的硬要求，不可省）
2. **占位符替换干净**：`<SKILL_ROOT>`、`<INPUT_PATH>` 等替换不干净会让大模型猜，猜错就返工
3. **一次只让它做一件事**：需求、扩写 PLAN、写代码、跑测试分开对话；混在一起容易漏
4. **产物路径强制**：所有提示词里明确指定落盘路径，避免大模型把文件塞到临时位置丢失
5. **量化的修复标准**：与其说"修好 bug X"，不如说"让 A 样本的 X 指标从 180 降到 <30"
6. **回归样本冻结**：同一轮回归必须用同一批样本；换样本证明不了修复
7. **审阅产物胜过审阅对话**：大模型的口头报告可能美化事实，最终以磁盘上的 PLAN / FINDINGS / VERSION / 代码为准

---

## 附录 B · 典型陷阱

来自 rule §1 最高原则的常见违反 + 早期 skill-01（已迁 MCP）/ skill-01 实战积累：

| 陷阱 | 症状 | 对策 |
|---|---|---|
| ① 基于旧 PLAN 开工 | 改的代码与 PLAN 对不上 | Step 0 开工前缀每次强制读最新 PLAN |
| ② 核心目录放开发日志 | 发布后混入调研草稿 / 临时脚本 | 提示词明写"过程文档只放 development/" |
| ③ 覆盖旧 PLAN / FINDINGS | 历史无法对比，回归验证失效 | 文件名带版本号或时间戳；提示词强调"新建不覆盖" |
| ④ 只改代码不改 PLAN / README | 三文档漂移 | Step 6 和 Step 8 的交付清单每次必跑 |
| ⑤ SKILL.md ≠ README.md | 触发词 / 参数 / 目录描述不一致 | Step 8 清单专项核对；大模型爱自造描述 |
| ⑥ 忘记升 .meta/VERSION | 下游不知道版本变了 | 修改收尾明确提示"按最高一档升位" |
| 回归偷偷换样本 | "修复了"但其实没法验证 | 样本集固定在一个目录，每次跑前让大模型 `ls` 一下确认 |
| LLM 做了本该脚本做的事 | Token 爆炸 / 精度不稳 | PLAN 写死"LLM 合成边界"，SKILL.md 给 Claude 明确指令 |
| 大输出无 cap | OOM / 超 token 预算 | PLAN 契约层就要求所有大输出带 `--cap`，超限自动降级 |
| 知识库重复定义 | 同一条目在两个 YAML 都有，覆盖规则不明 | 新增条目前先 grep 现有文件（Step 7.4 模板里已明写）|
| 把 .java / .rs 源码塞进 `<skill>/tools/` | 用户要装 JDK + Maven 才能跑 skill，完全破坏"运行单元"的定位 | rule §3.4 变体 C：源码走 `tools-src/`，`tools/` 只放预构建 artifact |
| 用技术栈 + demo 词命名 skill（如 `java-hello-world`） | 读者看不出 skill 干什么；命名漂移（后期改功能时名字更错） | rule §3.5：用"对象 + 动作"描述功能，如 `case-converter` |
| jar 版本 ≠ skill `.meta/VERSION` | jar 改了但 skill 版本没动，或反之；回溯时对不上帐 | rule §7：`pom.xml <version>` 与 `.meta/VERSION` 每次同改 |
| README 里硬编码版本号 / 版本历史 / FINDINGS 清单 | 每次改动都要手同步，必漂 | rule §5.3 反模式：用指针（"见 .meta/VERSION"、"见最新 FINDINGS"）而非镜像 |

---

## 附录 C · 与其他文档的关系

```
skill_develop_rule.md          ← 约束（WHAT MUST / MUST NOT）
skill_develop_guideline.md     ← 步骤 + 提示词模板（HOW，本文件）
01-Skill 学习笔记/03-规范与标准/SOP-*   ← 技术细节（TECHNICAL SPEC）
skill-XX-<name>/README.md      ← 该 skill 的用户入口
skill-XX-<name>/<name>/SKILL.md ← Claude 的执行指令
skill-XX-<name>/development/AI提示词笔记.txt ← 本次开发的真实提示词历史（可归档复用）
```

**推荐阅读顺序（开新 skill 前）**：本文件 → `skill_develop_rule.md` → 最近一个 skill 的 PLAN + FINDINGS + 提示词笔记 → SOP。
