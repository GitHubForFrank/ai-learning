# Reflection / Self-Correction 反思模式

> **定位**：让 Agent 对自己的输出进行自我审视、批评和修正，通过"生成 → 反思 → 改进"的循环提升输出质量，减少幻觉和错误。

> **Token 估算**：约 3.2K tokens

---

## 1. 模式概述

### 1.1 核心思想

人类的认知过程包含"自我批评"环节：先做出初步判断，再审视自己的推理是否有漏洞。Reflection 模式将此机制引入 Agent：
- 首轮生成初步结果
- 第二轮将结果作为输入，让模型以批评者角色审视
- 根据反思修正结果

### 1.2 执行流程

```plaintext
┌─────────┐      ┌──────────┐      ┌──────────┐
│ Generator│ →   │ Reflector│ →   │ Reviser  │
│  生成初稿 │      │  审视批评 │      │  修正改进  │
└─────────┘      └──────────┘      └─────┬────┘
                                         ↓
                                    满足质量？
                                    ↓ 否 → 继续循环
                                    ↓ 是
                                   最终输出
```

---

## 2. 核心模型

### 2.1 反思周期模型

```java
public record ReflectionCycle(
    int cycleNumber,
    String generatedContent,
    String critique,
    double qualityScore,
    String revisedContent,
    boolean converged
) {}

public record QualityCheck(
    boolean passed,
    double score,
    List<String> issues,
    List<String> suggestions
) {}

public record ReflectionConfig(
    int maxCycles,
    double qualityThreshold,
    double improvementThreshold,
    List<String> critiqueDimensions
) {
    public static ReflectionConfig defaultConfig() {
        return new ReflectionConfig(
            3,
            0.8,
            0.05,
            List.of("正确性", "完整性", "清晰性", "安全性", "一致性")
        );
    }
}
```

---

## 3. Java 实现

### 3.1 基础反思引擎

```java
public class ReflectionEngine {
    private final LLMClient generatorClient;
    private final LLMClient reflectorClient;
    private final ReflectionConfig config;

    public ReflectionEngine(LLMClient generatorClient, LLMClient reflectorClient,
                            ReflectionConfig config) {
        this.generatorClient = generatorClient;
        this.reflectorClient = reflectorClient;
        this.config = config;
    }

    public ReflectionResult reflect(String task, String role) {
        String generationSystemPrompt = "你是 %s，请完成任务。".formatted(role);
        String criticSystemPrompt = """
            你是严格的评审者。请对以下内容进行多维度审视。

            评审维度:
            %s

            返回 JSON:
            {
              "passed": true/false,
              "score": 0.8,
              "issues": ["问题1", "问题2"],
              "suggestions": ["建议1"]
            }
            """.formatted(String.join(", ", config.critiqueDimensions()));

        List<ReflectionCycle> cycles = new ArrayList<>();
        String currentContent = generatorClient.chat(generationSystemPrompt, task);

        for (int cycle = 1; cycle <= config.maxCycles(); cycle++) {
            QualityCheck check = critique(currentContent, criticSystemPrompt);
            cycles.add(new ReflectionCycle(cycle, currentContent,
                formatIssues(check.issues()), check.score(), currentContent, true));

            if (check.passed() && check.score() >= config.qualityThreshold()) {
                return new ReflectionResult(currentContent, cycles, true);
            }

            double prevScore = cycles.size() > 1
                ? cycles.get(cycles.size() - 2).qualityScore() : 0;
            if (check.score() - prevScore < config.improvementThreshold()) {
                break;
            }

            currentContent = revise(currentContent, check);
            cycles.get(cycles.size() - 1).withRevisedContent(currentContent);
        }

        return new ReflectionResult(currentContent, cycles, isQualityPassed(cycles));
    }

    private QualityCheck critique(String content, String systemPrompt) {
        String response = reflectorClient.chat(
            systemPrompt, "待评审内容:\n" + content);
        return parseQualityCheck(response);
    }

    private String revise(String original, QualityCheck check) {
        String prompt = """
            请根据以下评审意见，修正原始内容:

            原始内容:
            %s

            评审问题:
            %s

            改进建议:
            %s

            请输出修正后的完整内容:
            """.formatted(original,
                String.join("\n- ", check.issues()),
                String.join("\n- ", check.suggestions()));

        return generatorClient.chat(prompt);
    }
}

public record ReflectionResult(
    String finalContent,
    List<ReflectionCycle> cycles,
    boolean qualityPassed
) {}
```

### 3.2 反思提示模板

```java
public enum CriticDimension {
    CORRECTNESS("正确性", "输出是否事实正确，无错误"),
    COMPLETENESS("完整性", "是否覆盖了所有要求的内容"),
    CLARITY("清晰性", "表达是否清晰易懂"),
    SECURITY("安全性", "是否包含安全漏洞或不安全的代码建议"),
    CONSISTENCY("一致性", "内部逻辑是否自洽");

    private final String name;
    private final String description;

    CriticDimension(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public static String buildCriticPrompt() {
        StringBuilder sb = new StringBuilder();
        for (CriticDimension dim : values()) {
            sb.append("- ").append(dim.name).append(": ").append(dim.description).append("\n");
        }
        return sb.toString();
    }
}
```

---

## 4. Self-Refine 模式

### 4.1 自我精炼

```java
public class SelfRefineEngine {
    private final LLMClient llmClient;
    private final int maxRefinements;

    public String refine(String initialOutput, String feedback) {
        String current = initialOutput;

        for (int i = 0; i < maxRefinements; i++) {
            String refined = llmClient.chat("""
                原始输出:
                %s

                反馈意见:
                %s

                当前版本:
                %s

                请根据反馈改进当前版本，输出改进后的完整内容:
                """.formatted(initialOutput, feedback, current));

            if (refined.equals(current)) {
                break;
            }
            current = refined;
        }

        return current;
    }
}
```

---

## 5. Reflexion 模式

### 5.1 带记忆的反思

与 Reflection 不同，Reflexion 还维护一个"反思记忆"，让模型能从历史失败中学习：

```java
public class ReflexionEngine {
    private final LLMClient llmClient;
    private final List<ReflectionMemory> memory = new ArrayList<>();

    public String executeWithReflexion(String task, LLMClient environment) {
        String currentOutput = llmClient.chat(task);

        for (int iteration = 0; iteration < 3; iteration++) {
            String envFeedback = environment.chat("评估以下输出:\n" + currentOutput);

            if (isSuccess(envFeedback)) {
                return currentOutput;
            }

            String reflection = llmClient.chat("""
                任务: %s
                输出: %s
                环境反馈: %s

                请反思为什么输出不正确，记录经验教训。返回简短教训:
                """.formatted(task, currentOutput, envFeedback));

            memory.add(new ReflectionMemory(task, currentOutput, envFeedback, reflection));

            String memoryContext = memory.stream()
                .map(m -> "- 教训: " + m.lesson())
                .collect(Collectors.joining("\n"));

            currentOutput = llmClient.chat("""
                %s

                历史教训:
                %s

                请根据历史教训重新完成任务: %s
                """.formatted(task, memoryContext, task));
        }

        return currentOutput;
    }
}

public record ReflectionMemory(
    String task,
    String output,
    String feedback,
    String lesson
) {}
```

---

## 6. 适用场景与局限

### 6.1 适用场景

| 场景       | 效果    |
|----------|-------|
| 代码生成     | 显著提升  |
| 数学推理     | 显著提升  |
| 文本摘要     | 良好    |
| 翻译       | 良好    |
| 创意写作     | 一般（主观性强） |

### 6.2 局限

| 局限          | 说明               |
|-------------|------------------|
| 成本翻倍        | 至少 2 次 LLM 调用    |
| 模型自我批评能力有限  | 弱模型无明显效果         |
| 收敛不确定       | 可能陷入循环不收敛        |
| 过度修正        | 可能把对的改成错的        |

---

## 7. 最佳实践

| 实践         | 说明                    |
|------------|-----------------------|
| 限定反思轮次     | 一般 2-3 轮即可，过多浪费资源     |
| 多维评审       | 让反思覆盖多个维度，避免片面        |
| 收敛条件       | 当两轮质量分数差值小于阈值时停止      |
| 组合使用       | 配合 CoT、Tool Use 效果更佳 |
| 外部反馈       | 用代码执行结果等客观反馈替代纯 LLM 反思 |
| 记忆管理       | Reflexion 模式中的记忆要定期清理   |
