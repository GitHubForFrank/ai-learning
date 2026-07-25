# Plan-and-Execute 计划执行模式

> **定位**：将复杂任务分为"制定计划"和"执行计划"两个阶段。先让 LLM 规划全局步骤，再逐个执行，避免"边做边想"导致的短视和迷航。

> **Token 估算**：约 3.8K tokens

---

## 1. 模式概述

### 1.1 与 ReAct 的区别

| 特性       | ReAct                | Plan-and-Execute    |
|----------|----------------------|---------------------|
| 推理方式     | 每步思考，边做边想             | 全局规划后再执行            |
| 适用任务     | 短期、需实时反馈              | 长期、多步骤复杂任务          |
| 纠错能力     | 每步观察后可调整              | 需整体重规划              |
| Token 消耗 | 中等                   | 较高（两次完整推理）          |
| 可控性      | 低，容易迷航                | 高，计划可审查和修改          |

### 1.2 执行流程

```plaintext
┌──────────────┐     ┌──────────────────┐     ┌──────────────┐
│  Plan 阶段    │ →  │  Execute 阶段     │ →  │  Summarize   │
│               │     │                  │     │              │
│ 理解任务目标   │     │ 逐步执行每个步骤   │     │ 汇总结果      │
│ 分解为子任务   │     │ 记录结果/异常     │     │ 生成报告      │
│ 确定依赖关系   │     │ 必要时重新规划    │     │ 清理资源      │
└──────────────┘     └──────────────────┘     └──────────────┘
```

---

## 2. 核心组件

### 2.1 计划步骤模型

```java
public record Plan(
    String planId,
    String goal,
    List<PlanStep> steps,
    PlanStatus status,
    LocalDateTime createdAt
) {}

public record PlanStep(
    int stepNumber,
    String description,
    String expectedOutcome,
    List<Integer> dependsOn,
    StepStatus status,
    String result,
    String error
) {}

public enum PlanStatus {
    DRAFT, IN_PROGRESS, COMPLETED, FAILED, REVISED
}

public enum StepStatus {
    PENDING, IN_PROGRESS, COMPLETED, SKIPPED, FAILED
}
```

### 2.2 计划生成器接口

```java
public interface Planner {
    Plan createPlan(String goal, Context context);
    Plan revisePlan(Plan currentPlan, PlanStep failedStep, String errorInfo);
}

public interface Executor {
    StepResult executeStep(PlanStep step, Context context);
    boolean shouldContinue(StepResult result);
}

public record StepResult(
    boolean success,
    Object output,
    String summary,
    List<String> warnings
) {}
```

---

## 3. Java 实现

### 3.1 LLM 驱动的计划生成器

```java
public class LLMPlanner implements Planner {
    private final LLMClient llmClient;

    public LLMPlanner(LLMClient llmClient) {
        this.llmClient = llmClient;
    }

    @Override
    public Plan createPlan(String goal, Context context) {
        String prompt = """
            你是一个任务规划专家。请将以下任务分解为详细的执行步骤。

            任务目标: %s

            可用工具:
            %s

            请输出 JSON 格式的计划:
            {
              "steps": [
                {
                  "stepNumber": 1,
                  "description": "步骤描述",
                  "expectedOutcome": "预期结果",
                  "dependsOn": []
                }
              ]
            }

            要求:
            1. 步骤按执行顺序排列
            2. 标注步骤间的依赖关系
            3. 每个步骤粒度适中，不过粗也不过细
            4. 预期结果要具体可验证
            """.formatted(goal, context.getAvailableToolsDescription());

        String response = llmClient.chat(prompt);
        List<PlanStep> steps = parsePlanSteps(response);

        return new Plan(
            UUID.randomUUID().toString(),
            goal,
            steps,
            PlanStatus.DRAFT,
            LocalDateTime.now()
        );
    }

    @Override
    public Plan revisePlan(Plan currentPlan, PlanStep failedStep, String errorInfo) {
        String prompt = """
            原计划执行过程步骤 %d 失败:
            失败步骤: %s
            错误信息: %s

            原计划步骤:
            %s

            已完成步骤及结果:
            %s

            请重新规划后续步骤，输出 JSON:
            { "revisedSteps": [{ "stepNumber": ..., "description": "...", ... }] }
            """.formatted(
                failedStep.stepNumber(),
                failedStep.description(),
                errorInfo,
                formatSteps(currentPlan.steps()),
                formatCompletedSteps(currentPlan.steps())
            );

        String response = llmClient.chat(prompt);
        List<PlanStep> revisedSteps = parsePlanSteps(response);

        return new Plan(
            currentPlan.planId(),
            currentPlan.goal(),
            revisedSteps,
            PlanStatus.REVISED,
            LocalDateTime.now()
        );
    }
}
```

### 3.2 计划执行引擎

```java
public class PlanExecutionEngine {
    private final Planner planner;
    private final Executor executor;
    private final int maxRetries;

    public PlanExecutionEngine(Planner planner, Executor executor, int maxRetries) {
        this.planner = planner;
        this.executor = executor;
        this.maxRetries = maxRetries;
    }

    public ExecutionReport execute(String goal, Context context) {
        Plan plan = planner.createPlan(goal, context);
        List<PlanStep> completedSteps = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (PlanStep step : plan.steps()) {
            if (!areDependenciesMet(step, completedSteps)) {
                continue;
            }

            StepResult result = executor.executeStep(step, context);

            if (result.success()) {
                PlanStep completedStep = new PlanStep(
                    step.stepNumber(), step.description(), step.expectedOutcome(),
                    step.dependsOn(), StepStatus.COMPLETED,
                    result.summary(), null
                );
                completedSteps.add(completedStep);
            } else {
                Plan planRevised = planner.revisePlan(plan, step, result.summary());
                plan = planRevised;
                errors.add("步骤 %d 失败: %s".formatted(step.stepNumber(), result.summary()));
            }
        }

        return new ExecutionReport(plan, completedSteps, errors);
    }

    private boolean areDependenciesMet(PlanStep step, List<PlanStep> completed) {
        return step.dependsOn().stream()
            .allMatch(depId -> completed.stream()
                .anyMatch(s -> s.stepNumber() == depId
                    && s.status() == StepStatus.COMPLETED));
    }
}

public record ExecutionReport(
    Plan finalPlan,
    List<PlanStep> completedSteps,
    List<String> errors
) {}
```

---

## 4. 动态重规划

### 4.1 条件重规划触发器

```java
public class AdaptivePlanExecutor {
    private final PlanExecutionEngine engine;
    private final ReplanTrigger trigger;

    public ExecutionReport executeWithAdaptiveReplan(String goal, Context context) {
        Plan plan = planner.createPlan(goal, context);

        while (true) {
            PlanStep nextStep = getNextExecutableStep(plan);
            if (nextStep == null) break;

            StepResult result = executeStep(nextStep, context);

            if (trigger.shouldReplan(plan, nextStep, result)) {
                plan = planner.revisePlan(plan, nextStep,
                    "步骤 %d 触发重规划: %s".formatted(nextStep.stepNumber(), result.summary()));
                continue;
            }

            markCompleted(plan, nextStep, result);
        }

        return buildReport(plan);
    }
}

public interface ReplanTrigger {
    boolean shouldReplan(Plan plan, PlanStep step, StepResult result);
}

public class CompositeReplanTrigger implements ReplanTrigger {
    private final List<ReplanTrigger> triggers;

    public CompositeReplanTrigger(List<ReplanTrigger> triggers) {
        this.triggers = triggers;
    }

    @Override
    public boolean shouldReplan(Plan plan, PlanStep step, StepResult result) {
        return triggers.stream().anyMatch(t -> t.shouldReplan(plan, step, result));
    }
}
```

---

## 5. Spring AI 集成示例

```java
@Service
public class PlanExecuteAgentService {
    private final ChatClient chatClient;
    private final ToolRegistry toolRegistry;

    public PlanExecuteAgentService(ChatClient.Builder builder, ToolRegistry toolRegistry) {
        this.chatClient = builder.build();
        this.toolRegistry = toolRegistry;
    }

    public PlanResult executeComplexTask(String objective) {
        String planPrompt = """
            目标: %s
            可用工具: %s

            请制定详细执行计划，JSON 输出。
            """.formatted(objective, toolRegistry.buildToolsPrompt());

        Plan plan = parsePlan(chatClient.prompt().user(planPrompt).call().content());

        for (PlanStep step : plan.steps()) {
            String stepPrompt = """
                执行计划的第 %d 步: %s
                预期结果: %s

                请调用需要的工具完成此步骤。
                """.formatted(step.stepNumber(), step.description(), step.expectedOutcome());

            String result = chatClient.prompt()
                .user(stepPrompt)
                .tools(toolRegistry.getNativeTools())
                .call()
                .content();

            step = step.withResult(result);
        }

        return new PlanResult(plan, "任务完成");
    }
}
```

---

## 6. 适用场景与局限

### 6.1 适用场景

| 场景         | 说明                  |
|------------|---------------------|
| 多步骤代码生成    | 先生成骨架 → 实现方法 → 测试   |
| 自动化测试      | 制定测试计划 → 执行 → 汇总报告   |
| 数据分析流水线    | 清洗 → 分析 → 可视化       |
| 部署流水线      | 构建 → 测试 → 部署 → 验证   |
| 文档生成       | 大纲 → 各章节 → 汇总      |

### 6.2 局限

| 局限      | 缓解方案              |
|---------|-------------------|
| 计划过于僵化  | 支持动态重规划           |
| 上下文过长   | 每步裁剪无关上下文         |
| 过度规划    | 简单任务直接使用 ReAct    |
| 计划与实际脱节 | 增加验证步骤，偏差时触发重规划    |

---

## 7. 最佳实践

| 实践        | 说明                       |
|-----------|--------------------------|
| 计划粒度适中    | 太细会影响效率，太粗会执行失败          |
| 每步可验证     | 预期结果应具体到可自动化验证的程度        |
| 支持人工介入    | 计划可预览和修改，关键步骤需人工确认       |
| 记录执行日志    | 便于问题回溯和优化               |
| 结合 ReAct  | 计划中的每一步可以用 ReAct 方式执行    |
| 失败时降级     | 步骤失败时尝试替代方案，仍失败则触发重规划     |
