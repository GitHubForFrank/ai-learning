# ReAct：Reasoning + Acting

> 让模型**交替执行「推理」和「动作」**，用工具调用的结果反哺下一步推理。是当代 Agent 的基础范式。

---

## 1. 核心循环

```
Thought（思考）→ Action（动作）→ Observation（观察）→ Thought → ...
```

每一轮：
1. **Thought**：基于当前已知信息分析下一步该做什么
2. **Action**：调用工具（搜索、计算、API、读文件…）
3. **Observation**：工具返回的结果
4. 回到 Thought，直到能给出 Final Answer

---

## 2. 经典 Prompt 结构

```
你可以使用以下工具：
- search(query): 搜索网页
- calculator(expr): 计算数学表达式
- read_file(path): 读取文件内容

请按以下格式思考：
Thought: 我需要...
Action: search("...")
Observation: {工具返回}
Thought: 从结果看...
Action: ...
...
Final Answer: {最终答案}

问题：{用户问题}
```

关键点：
- 工具列表 + 签名要清晰
- 格式用固定标签（`Thought:` / `Action:` / `Observation:`），便于解析
- `Observation:` 由**执行框架注入**，不是模型生成的

---

## 3. 与纯 CoT 的对比

| 维度 | CoT | ReAct |
|------|-----|-------|
| 信息来源 | 只靠模型参数里的知识 | 可以调用外部工具获取实时/准确数据 |
| 幻觉风险 | 高（编造事实） | 低（观察值校准推理） |
| 适用任务 | 数学、逻辑、闭卷推理 | 问答、调研、多步操作 |
| 成本 | 低（单次调用） | 高（多次 LLM + 工具调用） |

**一句话**：CoT 是"闭门造车"，ReAct 是"边想边查"。

---

## 4. 实战示例

### 任务：查询某城市明天天气并推荐出行装备

```
Thought: 我需要先拿到明天的天气数据
Action: get_weather(city="北京", date="2026-04-22")
Observation: {"temp_low": 8, "temp_high": 16, "condition": "阵雨"}
Thought: 有雨且温差大，推荐防水外套+换洗衣物
Final Answer: 明天北京 8-16°C 有阵雨，建议穿防水外套、带伞，备薄毛衣。
```

---

## 5. 工程落地要点

### 5.1 工具调用格式
早期用文本解析 `Action: tool(args)`，脆弱易错。现代做法：
- OpenAI / Anthropic 的 **Tool Use / Function Calling** API → 模型直接产出结构化 JSON
- 框架（LangChain、LlamaIndex、Claude Agent SDK）替你解析和执行

### 5.2 循环终止条件
必须显式设置，否则无限循环：
- **最大迭代次数**（典型 10~25 轮）
- **token 预算耗尽**
- 模型输出 `Final Answer:`

### 5.3 错误处理
- 工具调用失败 → `Observation: Error: ...` 注入回去，让模型决定重试/换策略/放弃
- **不要吞掉错误**，否则模型以为成功了

### 5.4 观察值过长
搜索结果可能是上千 token 的网页。对策：
- 先截断 / 摘要再塞回 Observation
- 或把原文存到 scratchpad，Observation 里只放摘要 + 引用 ID

---

## 6. 常见失效模式

| 现象 | 原因 | 对策 |
|------|------|------|
| 反复调同一个工具 | 观察值没有带来新信息，模型陷入循环 | 加循环检测；提示"不要重复调用相同参数" |
| 跳过工具直接编造 | 指令不够强、模型"偷懒" | System prompt 强调"必须通过工具验证事实" |
| 格式错乱解析失败 | 温度太高 / 工具签名复杂 | 降温度；用结构化 Tool Use API |
| 提前 Final Answer | 任务复杂但模型觉得"够了" | 提示"至少 N 轮验证"或拆分任务 |

---

## 7. 从 ReAct 到 Agent

ReAct 是 **最小可行 Agent**。现代 Agent 在此之上扩展：
- **记忆**：长短期记忆、向量检索
- **规划**：先出 Plan 再分步执行（Plan-and-Execute）
- **多 Agent**：子 Agent 独立 ReAct 循环，主 Agent 编排
- **工具生态**：MCP 协议标准化工具接入

深入见 `01-Agent 学习笔记/02-多Agent协作/`。
