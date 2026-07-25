# Multi-Agent 多智能体协作模式

> **定位**：通过多个专业化的 Agent 分工协作，每个 Agent 聚焦特定领域，相互通信、协调，共同完成复杂任务。

> **Token 估算**：约 4K tokens

---

## 1. 模式概述

### 1.1 核心思想

复杂问题拆分为多个子问题，每个子问题由专业 Agent 处理。Agent 之间通过消息传递、共享上下文、任务委派等方式协作。

### 1.2 协作拓扑

```plaintext
┌──────────────────────────────────────────────────────┐
│                   Orchestrator Agent                  │
│                     (调度协调者)                       │
└──┬──────────────┬──────────────┬──────────────────────┘
   ↓              ↓              ↓
┌──────┐     ┌──────┐      ┌──────┐
│ Code │     │Review│      │ Test │
│Agent │ ←→  │Agent │ ←→   │Agent │
└──────┘     └──────┘      └──────┘
```

---

## 2. 核心组件

### 2.1 Agent 与消息模型

```java
public interface Agent {
    String getId();
    String getName();
    String getRole();
    AgentCapability getCapability();
    AgentMessage handle(AgentMessage message);
}

public record AgentMessage(
    String id,
    String fromAgentId,
    String toAgentId,
    MessageType type,
    String content,
    Map<String, Object> metadata,
    AgentMessage parentMessageId
) {}

public enum MessageType {
    TASK, QUERY, RESPONSE, BROADCAST, STATUS_UPDATE
}

public record AgentCapability(
    Set<String> skills,
    Set<String> toolNames,
    String description
) {}
```

### 2.2 编排器接口

```java
public interface Orchestrator {
    TaskPlan decompose(String objective, List<Agent> agents);
    void dispatch(TaskPlan plan);
    AgentMessage collectAndAggregate(List<AgentMessage> results);
    boolean isComplete(TaskPlan plan);
}

public record TaskPlan(
    String objective,
    List<SubTask> subTasks,
    ExecutionStrategy strategy
) {}

public record SubTask(
    String id,
    String description,
    String assignedAgentId,
    List<String> dependsOn,
    SubTaskStatus status,
    AgentMessage result
) {}

public enum ExecutionStrategy {
    SEQUENTIAL,
    PARALLEL,
    PIPELINE,
    DYNAMIC
}
```

---

## 3. Java 实现

### 3.1 基于角色的 Agent 注册

```java
public class AgentRegistry {
    private final Map<String, Agent> agents = new ConcurrentHashMap<>();
    private final Map<String, List<String>> skillIndex = new ConcurrentHashMap<>();

    public void register(Agent agent) {
        agents.put(agent.getId(), agent);
        for (String skill : agent.getCapability().skills()) {
            skillIndex.computeIfAbsent(skill, k -> new ArrayList<>()).add(agent.getId());
        }
    }

    public List<Agent> findBySkill(String skill) {
        return skillIndex.getOrDefault(skill, List.of()).stream()
            .map(agents::get)
            .filter(Objects::nonNull)
            .toList();
    }

    public List<Agent> getAllAgents() {
        return List.copyOf(agents.values());
    }
}
```

### 3.2 LLM 驱动的编排器

```java
public class LLMOrchestrator implements Orchestrator {
    private final LLMClient llmClient;
    private final AgentRegistry registry;

    public LLMOrchestrator(LLMClient llmClient, AgentRegistry registry) {
        this.llmClient = llmClient;
        this.registry = registry;
    }

    @Override
    public TaskPlan decompose(String objective, List<Agent> agents) {
        String agentDescriptions = agents.stream()
            .map(a -> "- " + a.getName() + "(" + a.getRole() + "): " + a.getCapability().description())
            .collect(Collectors.joining("\n"));

        String prompt = """
            目标: %s

            可用 Agent:
            %s

            请拆分任务并分配给合适的 Agent。JSON 格式:
            {
              "subTasks": [
                {
                  "id": "task-1",
                  "description": "...",
                  "assignedAgentId": "...",
                  "dependsOn": []
                }
              ],
              "strategy": "SEQUENTIAL|PARALLEL|PIPELINE"
            }
            """.formatted(objective, agentDescriptions);

        String response = llmClient.chat(prompt);
        return parseTaskPlan(response);
    }

    @Override
    public void dispatch(TaskPlan plan) {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        switch (plan.strategy()) {
            case SEQUENTIAL -> executeSequential(plan);
            case PARALLEL -> executeParallel(plan, executor);
            case PIPELINE -> executePipeline(plan, executor);
            case DYNAMIC -> executeDynamic(plan);
        }
    }

    private void executeParallel(TaskPlan plan, ExecutorService executor) {
        List<SubTask> independentTasks = plan.subTasks().stream()
            .filter(t -> t.dependsOn().isEmpty())
            .toList();

        List<CompletableFuture<Void>> futures = independentTasks.stream()
            .map(task -> CompletableFuture.runAsync(() -> executeSubTask(task), executor))
            .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
}
```

---

## 4. Agent 间通信

### 4.1 消息总线

```java
public class AgentMessageBus {
    private final List<MessageInterceptor> interceptors = new ArrayList<>();
    private final Map<String, List<AgentMessage>> messageHistory = new ConcurrentHashMap<>();

    public AgentMessage send(AgentMessage message) {
        for (MessageInterceptor interceptor : interceptors) {
            message = interceptor.intercept(message);
        }

        Agent agent = agentRegistry.getAgentById(message.toAgentId());
        if (agent == null) {
            throw new AgentNotFoundException("Agent not found: " + message.toAgentId());
        }

        messageHistory.computeIfAbsent(message.toAgentId(), k -> new ArrayList<>()).add(message);

        return agent.handle(message);
    }

    public void broadcast(AgentMessage message, List<String> excludeIds) {
        for (Agent agent : agentRegistry.getAllAgents()) {
            if (!excludeIds.contains(agent.getId())) {
                agent.handle(message);
            }
        }
    }
}

public interface MessageInterceptor {
    AgentMessage intercept(AgentMessage message);
}
```

### 4.2 对话式 Agent 通信

```java
public class ConversationalAgent implements Agent {
    private final LLMClient llmClient;
    private final String id;
    private final String name;
    private final String role;
    private final List<AgentMessage> conversationHistory = new ArrayList<>();

    public ConversationalAgent(String id, String name, String role, LLMClient llmClient) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.llmClient = llmClient;
    }

    @Override
    public AgentMessage handle(AgentMessage message) {
        conversationHistory.add(message);

        String systemPrompt = """
            你是 %s，角色: %s。
            你正与其他 Agent 协作完成任务。
            请根据收到的消息作出专业回复或执行任务。
            """.formatted(name, role);

        String conversation = conversationHistory.stream()
            .map(m -> m.fromAgentId() + ": " + m.content())
            .collect(Collectors.joining("\n"));

        String response = llmClient.chat(systemPrompt + "\n\n对话:\n" + conversation);

        AgentMessage reply = new AgentMessage(
            UUID.randomUUID().toString(),
            id,
            message.fromAgentId(),
            MessageType.RESPONSE,
            response,
            Map.of(),
            message
        );

        conversationHistory.add(reply);
        return reply;
    }
}
```

---

## 5. 高级模式：群体辩论 (Group Debate)

### 5.1 多 Agent 辩论达成共识

```java
public class GroupDebateEngine {
    private final List<Agent> debaters;
    private final Agent judge;
    private final int rounds;

    public DebateResult debate(String topic, String initialPosition) {
        List<Argument> arguments = new ArrayList<>();
        arguments.add(new Argument("initiator", initialPosition, 0));

        for (int round = 1; round <= rounds; round++) {
            for (Agent agent : debaters) {
                String debateContext = buildDebateContext(topic, arguments, round);
                AgentMessage message = new AgentMessage(
                    UUID.randomUUID().toString(),
                    "moderator",
                    agent.getId(),
                    MessageType.QUERY,
                    debateContext,
                    Map.of("round", round)
                );
                AgentMessage response = agent.handle(message);
                arguments.add(new Argument(
                    agent.getName(), response.content(), round
                ));
            }
        }

        String finalJudgment = judge.handle(new AgentMessage(
            UUID.randomUUID().toString(),
            "moderator",
            judge.getId(),
            MessageType.TASK,
            "请评估以下辩论并给出最终结论:\n" + formatArguments(arguments)
        )).content();

        return new DebateResult(topic, arguments, finalJudgment);
    }
}

public record DebateResult(
    String topic,
    List<Argument> arguments,
    String finalConclusion
) {}

public record Argument(
    String agentName,
    String content,
    int round
) {}
```

---

## 6. 适用场景与局限

### 6.1 适用场景

| 场景         | 典型 Agent 组合                |
|------------|----------------------------|
| 软件开发       | 产品经理 Agent + 架构师 Agent + 开发 Agent + 测试 Agent |
| 代码审查       | 安全审查 Agent + 性能审查 Agent + 风格审查 Agent |
| 内容创作       | 策划 Agent + 写作 Agent + 编辑 Agent |
| 金融分析       | 研报分析 Agent + 风险评估 Agent + 策略 Agent |

### 6.2 局限

| 局限       | 缓解方案                 |
|----------|----------------------|
| Agent 间理解偏差 | 统一消息格式、共享上下文        |
| 通信开销大    | 非必要时使用单 Agent        |
| 协调复杂度高   | 明确编排器角色，避免民主式 chaos  |
| Token 成本高 | 按需激活 Agent，非必要不参与对话  |

---

## 7. 最佳实践

| 实践         | 说明                      |
|------------|-------------------------|
| 明确角色边界     | 每个 Agent 有清晰的职责和技能描述    |
| 统一通信协议     | JSON 结构化消息，字段唯一          |
| 结果验证       | 关键步骤由多个 Agent 交叉验证      |
| 超时与降级      | 单个 Agent 超时不阻塞整体流程      |
| 日志与可观测性    | 记录 Agent 间所有消息，便于调试     |
| 分层编排       | 复杂场景可用多级编排器（Orchestrator of orchestrators） |
