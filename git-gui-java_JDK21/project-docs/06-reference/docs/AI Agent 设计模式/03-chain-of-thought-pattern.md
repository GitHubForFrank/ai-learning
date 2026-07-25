# Chain-of-Thought (CoT) 思维链模式

> **定位**：通过引导 LLM 输出中间推理步骤，将复杂问题分解为可验证的推理链，显著提升 LLM 在数学、逻辑、规划等复杂任务上的表现。

> **Token 估算**：约 3.4K tokens

---

## 1. 模式概述

### 1.1 核心思想

CoT 要求模型在给出最终答案前，**先输出推理过程**。这利用了 Transformer 的自回归特性：每一步推理都基于之前输出的内容，形成逐步推理的"思考链"。

### 1.2 分类

| 类型             | 说明                          | 触发方式         |
|----------------|-----------------------------|--------------|
| Zero-shot CoT  | 仅加一句 "Let's think step by step" | Prompt 工程 |
| Few-shot CoT   | 提供几个推理示例                    | Prompt 工程 |
| Auto-CoT       | 自动聚类问题并生成多样化推理链            | 程序化         |
| Tree-of-Thought | 多分支推理，评估每一条路径               | 程序化 + LLM  |

---

## 2. Zero-shot CoT

### 2.1 Prompt 模板

```java
public class ZeroShotCoTPrompt {

    public String buildPrompt(String question) {
        return """
            请逐步思考以下问题，先写出推理过程，再给出最终答案。

            问题: %s

            让我们一步步思考:
            """.formatted(question);
    }
}
```

### 2.2 两步法实现

```java
public class TwoStageCoTEngine {
    private final LLMClient llmClient;

    public TwoStageCoTEngine(LLMClient llmClient) {
        this.llmClient = llmClient;
    }

    public CoTResult solve(String question) {
        String reasoningPrompt = """
            问题: %s

            请详细写出推理过程，不要直接给出答案。只需写出思考步骤。
            """.formatted(question);

        String reasoning = llmClient.chat(reasoningPrompt);

        String answerPrompt = """
            问题: %s

            推理过程:
            %s

            基于以上推理，给出最终答案:
            """.formatted(question, reasoning);

        String answer = llmClient.chat(answerPrompt);

        return new CoTResult(question, reasoning, answer);
    }
}

public record CoTResult(
    String question,
    String reasoning,
    String finalAnswer
) {}
```

---

## 3. Few-shot CoT

```java
public class FewShotCoTPrompt {

    private static final List<CoTExample> EXAMPLES = List.of(
        new CoTExample(
            "小明有 5 个苹果，给了小红 2 个，又买了 3 个，现在有几个?",
            """
            1. 小明初始有 5 个苹果
            2. 给小红 2 个后: 5 - 2 = 3 个
            3. 又买 3 个后: 3 + 3 = 6 个
            因此小明现在有 6 个苹果。""",
            "6"
        ),
        new CoTExample(
            "一个长方形长 6cm 宽 4cm，周长是多少?",
            """
            1. 长方形周长公式: 2 × (长 + 宽)
            2. 代入数值: 2 × (6 + 4)
            3. 计算: 2 × 10 = 20
            因此周长是 20cm。""",
            "20"
        )
    );

    public String buildPrompt(String question) {
        StringBuilder sb = new StringBuilder();
        sb.append("请按照示例的推理方式回答问题。\n\n");

        for (CoTExample example : EXAMPLES) {
            sb.append("问题: ").append(example.question()).append("\n");
            sb.append("推理: ").append(example.reasoning()).append("\n");
            sb.append("答案: ").append(example.answer()).append("\n\n");
        }

        sb.append("问题: ").append(question).append("\n");
        sb.append("推理: ");
        return sb.toString();
    }
}

public record CoTExample(String question, String reasoning, String answer) {}
```

---

## 4. Tree-of-Thought (ToT) 实现

### 4.1 核心思路

ToT 不是单路径推理，而是在每个推理步骤生成多个分支，评估每个分支的质量，选择最优路径继续深入。

```java
public class TreeOfThoughtEngine {
    private final LLMClient llmClient;
    private final int branches;
    private final int maxDepth;

    public TreeOfThoughtEngine(LLMClient llmClient, int branches, int maxDepth) {
        this.llmClient = llmClient;
        this.branches = branches;
        this.maxDepth = maxDepth;
    }

    public ToTResult search(String problem) {
        PriorityQueue<ThoughtNode> queue = new PriorityQueue<>(
            Comparator.comparingDouble(ThoughtNode::score).reversed()
        );
        queue.add(new ThoughtNode(problem, "", 1.0, 0));

        ThoughtNode best = queue.peek();
        while (!queue.isEmpty() && queue.peek().depth() < maxDepth) {
            ThoughtNode current = queue.poll();
            List<ThoughtNode> children = expand(current);
            queue.addAll(children);
            if (!queue.isEmpty() && queue.peek().score() > best.score()) {
                best = queue.peek();
            }
        }

        return new ToTResult(best);
    }

    private List<ThoughtNode> expand(ThoughtNode node) {
        String prompt = """
            问题: %s

            当前已推理:
            %s

            请生成 %d 个不同的下一步推理方向，每个方向以 JSON 数组返回:
            [{"step": "推理步骤", "confidence": 0.8}]
            """.formatted(node.problem(), node.thoughtChain(), branches);

        String response = llmClient.chat(prompt);
        List<Map<String, Object>> steps = parseJsonArray(response);

        return steps.stream()
            .map(step -> {
                String newChain = node.thoughtChain()
                    + "\n步骤" + (node.depth() + 1) + ": " + step.get("step");
                double confidence = ((Number) step.get("confidence")).doubleValue();
                return new ThoughtNode(node.problem(), newChain,
                    node.score() * confidence, node.depth() + 1);
            })
            .toList();
    }
}

public record ThoughtNode(
    String problem,
    String thoughtChain,
    double score,
    int depth
) {}

public record ToTResult(ThoughtNode bestPath) {}
```

---

## 5. Chain-of-Verification (CoVe)

### 5.1 验证链

在 CoT 推理后增加验证环节，让模型自我检查推理结果：

```java
public class ChainOfVerificationEngine {
    private final LLMClient llmClient;

    public VerifiedResult solve(String question) {
        String initialAnswer = llmClient.chat("""
            问题: %s
            请一步步推理并给出答案。
            """.formatted(question));

        String verificationQuestions = llmClient.chat("""
            以下是问题和答案:
            问题: %s
            答案: %s

            请生成 3 个验证性问题来检查答案的正确性。
            返回格式: ["问题1", "问题2", "问题3"]
            """.formatted(question, initialAnswer));

        List<String> vqs = parseJsonList(verificationQuestions);

        StringBuilder verification = new StringBuilder();
        for (String vq : vqs) {
            String answer = llmClient.chat(vq);
            verification.append("Q: ").append(vq).append("\nA: ").append(answer).append("\n\n");
        }

        String finalAnswer = llmClient.chat("""
            原始问题: %s
            初步答案: %s

            验证结果:
            %s

            请根据验证结果确认或修正答案，输出最终答案。
            """.formatted(question, initialAnswer, verification));

        return new VerifiedResult(question, initialAnswer, verification.toString(), finalAnswer);
    }
}
```

---

## 6. 结构化推理输出

### 6.1 强制 JSON 结构化推理

```java
public class StructuredCoTEngine {
    private final LLMClient llmClient;

    public StructuredReasoning solve(String problem) {
        String prompt = """
            问题: %s

            请输出结构化推理，JSON 格式:
            {
              "problem_understanding": "对问题的理解",
              "known_facts": ["已知事实1", "已知事实2"],
              "reasoning_steps": [
                { "step": 1, "action": "操作描述", "result": "结果", "rationale": "理由" }
              ],
              "conclusion": "最终结论",
              "confidence": 0.95
            }
            """.formatted(problem);

        String response = llmClient.chat(prompt);
        return parseStructuredReasoning(response);
    }
}

public record StructuredReasoning(
    String problemUnderstanding,
    List<String> knownFacts,
    List<ReasoningStep> reasoningSteps,
    String conclusion,
    double confidence
) {}

public record ReasoningStep(
    int step,
    String action,
    String result,
    String rationale
) {}
```

---

## 7. 适用场景与局限

### 7.1 适用场景

| 场景        | 效果    |
|-----------|-------|
| 数学推理      | 显著提升  |
| 代码生成      | 良好    |
| 逻辑推理      | 显著提升  |
| 多步规划      | 良好    |
| 常识问答      | 一般    |
| 简单翻译/摘要   | 无明显提升 |

### 7.2 局限

- **Token 消耗翻倍**：推理过程会额外消耗大量 Token
- **延迟增加**：输出步骤越多，生成延迟越大
- **简单任务无收益**：对翻译、摘要等任务没有帮助
- **幻觉传递**：错误的中间推理会影响最终答案

---

## 8. 最佳实践

| 实践          | 说明                          |
|-------------|-----------------------------|
| 简单任务不用 CoT  | 只在需要多步推理时使用                |
| 示例多样化       | Few-shot 示例应覆盖不同类型的问题       |
| 结构化输出       | JSON 结构化更易解析和验证             |
| 结合工具使用      | 推理步骤中可以插入工具调用              |
| 递归验证        | CoVe 验证链可显著减少幻觉             |
| 设置推理深度限制    | ToT 必须限制搜索深度和分支数，避免组合爆炸     |
