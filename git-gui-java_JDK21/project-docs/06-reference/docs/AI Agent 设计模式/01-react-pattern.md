# ReAct (Reasoning + Acting) 模式

> **定位**：AI Agent 领域最基础、最经典的设计模式，将推理（Reasoning）与行动（Acting）交替循环，使 LLM 能够在思考的同时调用外部工具获取信息并执行操作。

> **Token 估算**：约 2.5K tokens

---

## 1. 模式概述

### 1.1 解决的问题

传统 LLM 调用是一次性的：输入 prompt → 输出结果。但在真实场景中，Agent 往往需要多轮交互、查询外部信息、执行操作。ReAct 将推理和行动交织在一起，让模型在每一步都可以：

- **思考**（Thought）：分析当前状态，决定下一步做什么
- **行动**（Action）：调用工具、查询知识库、执行代码
- **观察**（Observation）：获取行动结果，用于下一步推理

### 1.2 执行流程

```plaintext
┌──────────────────────────────────────────────────────┐
│                     ReAct 循环                        │
│                                                      │
│   用户指令 → Thought → Action → Observation           │
│                  ↑                    ↓               │
│                  └──── 是否完成? ←────┘               │
│                        ↓ 是                           │
│                     最终回答                          │
└──────────────────────────────────────────────────────┘
```

---

## 2. 核心组件

### 2.1 接口定义

```java
public interface ReActAgent {

    String think(ChatContext context);

    ActionResult act(ActionDecision decision);

    boolean shouldContinue(Observation observation, int currentStep, int maxSteps);

    String summarize(ChatContext context);
}
```

### 2.2 Action 与 Observation 模型

```java
public record ActionDecision(
    String actionType,
    Map<String, Object> parameters,
    String reasoning
) {}

public record ActionResult(
    boolean success,
    String actionType,
    Object data,
    String errorMessage
) {}

public record Observation(
    ActionResult actionResult,
    String summary,
    Map<String, Object> metadata
) {}
```

---

## 3. Java 实现

### 3.1 基础 ReAct 引擎

```java
public class ReActEngine {
    private final LLMClient llmClient;
    private final ToolRegistry toolRegistry;
    private final int maxSteps;

    public ReActEngine(LLMClient llmClient, ToolRegistry toolRegistry, int maxSteps) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.maxSteps = maxSteps;
    }

    public String execute(String userQuery, ChatContext context) {
        for (int step = 0; step < maxSteps; step++) {
            ThoughtResult thought = llmClient.think(context);

            if (thought.isFinal()) {
                return thought.getFinalAnswer();
            }

            ActionDecision decision = thought.getActionDecision();
            Tool tool = toolRegistry.get(decision.actionType());
            if (tool == null) {
                context.addObservation("未知工具: " + decision.actionType());
                continue;
            }

            ActionResult result = tool.execute(decision.parameters());
            context.addObservation(formatObservation(result));

            if ("finish".equals(decision.actionType())) {
                return result.data().toString();
            }
        }
        return "任务达到最大步数限制";
    }

    private String formatObservation(ActionResult result) {
        if (result.success()) {
            return "工具返回: " + result.data();
        }
        return "工具执行失败: " + result.errorMessage();
    }
}
```

### 3.2 ReAct Prompt 模板

```java
public class ReActPromptTemplate {

    public String buildSystemPrompt(List<ToolDefinition> tools) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个能使用工具的 AI 助手。请按以下格式逐步推理：\n\n");
        sb.append("格式:\n");
        sb.append("Thought: <你的推理>\n");
        sb.append("Action: <工具名>[<参数JSON>]\n");
        sb.append("Observation: <工具返回结果>\n");
        sb.append("... (可重复)\n");
        sb.append("Thought: 我知道了答案\n");
        sb.append("Final Answer: <最终回答>\n\n");
        sb.append("可用工具:\n");
        for (ToolDefinition tool : tools) {
            sb.append("- ").append(tool.name())
              .append(": ").append(tool.description())
              .append(", 参数: ").append(tool.parameterSchema()).append("\n");
        }
        return sb.toString();
    }
}
```

---

## 4. LangChain4j 集成

### 4.1 使用 LangChain4j 的 AgentExecutor

```java
public class LangChain4jReActAgent {

    public Assistant createAgent() {
        ChatLanguageModel model = OpenAiChatModel.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .modelName("gpt-4")
            .build();

        ToolSpecification calculator = ToolSpecification.builder()
            .name("calculator")
            .description("执行数学计算")
            .addParameter("expression", JsonSchemaProperty.STRING,
                JsonSchemaProperty.description("数学表达式"))
            .build();

        ToolSpecification search = ToolSpecification.builder()
            .name("web_search")
            .description("搜索互联网")
            .addParameter("query", JsonSchemaProperty.STRING,
                JsonSchemaProperty.description("搜索关键词"))
            .build();

        return AiServices.builder(Assistant.class)
            .chatLanguageModel(model)
            .tools(new CalculatorTool(), new WebSearchTool())
            .build();
    }
}

interface Assistant {
    String chat(String userMessage);
}
```

---

## 5. 适用场景与局限

### 5.1 适用场景

| 场景           | 说明                         |
|--------------|----------------------------|
| 需要多步工具调用的任务 | 如：先搜索信息 → 再分析 → 再生成报告      |
| 信息检索型 Agent  | 需要查询数据库、搜索网页等              |
| 自动化运维        | 先诊断 → 再执行修复命令              |

### 5.2 局限

| 局限             | 缓解方案                       |
|----------------|----------------------------|
| Token 消耗大       | 设置 maxSteps、及时压缩上下文         |
| 容易陷入循环         | 增加超时机制、步数限制                |
| 复杂任务推理链过长      | 结合 Plan-and-Execute 模式     |
| 工具选择错误导致偏离目标   | 优化工具描述、增加 Few-shot 示例     |

---

## 6. 变体模式

### 6.1 ReWOO (Reasoning WithOut Observation)

跳过中间的 Observation 步骤，将所有工具调用一次性计划好再批量执行，减少 LLM 调用次数。

### 6.2 Structured ReAct

将 Thought/Action/Observation 解析为结构化 JSON，避免文本解析的不确定性：

```json
{
  "thought": "我需要查询今天的天气",
  "action": {
    "name": "get_weather",
    "params": { "city": "北京" }
  }
}
```

---

## 7. 最佳实践

- **Prompt 工程**：Few-shot 示例能显著提升工具选择的准确率
- **工具描述精细化**：参数 schema 越详细，LLM 调用越准确
- **多模态 Observation**：Observation 可包含图片、表格等富文本信息
- **并行工具调用**：多个无依赖的工具调用应在同一轮执行，减少轮次
