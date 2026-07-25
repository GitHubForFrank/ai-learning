# State Machine / Workflow 状态机工作流模式

> **定位**：将 Agent 的执行流程建模为有限状态机（FSM），通过显式的状态和转移规则编排复杂任务的执行，使 Agent 行为可预测、可调试、可审计。

> **Token 估算**：约 5.6K tokens

---

## 1. 模式概述

### 1.1 核心思路

将 Agent 的思考-执行循环抽象为状态机：每个状态定义 Agent 当前的角色和行为，转移规则定义何时以及如何进入下一个状态。相比自由推理，状态机模式的 Agent 行为更可控。

### 1.2 状态机示意

```plaintext
                        ┌──────────┐
           ──────────→ │  IDLE    │ ←──────────
          │             └────┬─────┘            │
          │                  ↓ 收到任务           │
          │             ┌──────────┐            │
          │             │ANALYZING │            │
          │             └────┬─────┘            │
          │                  ↓                  │
          │             ┌──────────┐            │
          │         ┌──→│ PLANNING │──┐         │
          │         │   └──────────┘  │         │
          │         ↓                ↓         │
          │   ┌──────────┐    ┌──────────┐     │
          │   │EXECUTING │    │ WAITING  │     │
          │   └────┬─────┘    │  (审批)   │     │
          │        ↓          └────┬─────┘     │
          │   ┌──────────┐         │           │
          │   │ VERIFYING│←────────┘           │
          │   └────┬─────┘                     │
          │        ↓                           │
          │   ┌──────────┐                     │
          └── │COMPLETED │                     │
              └──────────┘                     │
```

---

## 2. 核心模型

### 2.1 状态与转移

```java
public enum AgentState {
    IDLE,
    ANALYZING,
    PLANNING,
    EXECUTING,
    WAITING_APPROVAL,
    VERIFYING,
    COMPLETED,
    ERROR,
    CANCELLED
}

public record StateTransition(
    AgentState fromState,
    AgentState toState,
    String trigger,
    Predicate<AgentContext> guard,
    Consumer<AgentContext> action
) {}

public class AgentStateMachine {
    private AgentState currentState;
    private final Map<AgentState, List<StateTransition>> transitions;

    public AgentStateMachine(AgentState initialState) {
        this.currentState = initialState;
        this.transitions = new EnumMap<>(AgentState.class);
    }

    public void addTransition(StateTransition transition) {
        transitions.computeIfAbsent(transition.fromState(),
            k -> new ArrayList<>()).add(transition);
    }

    public boolean canTransition(AgentState targetState, AgentContext context) {
        return transitions.getOrDefault(currentState, List.of()).stream()
            .anyMatch(t -> t.toState() == targetState
                && (t.guard() == null || t.guard().test(context)));
    }

    public boolean transition(AgentState targetState, AgentContext context) {
        List<StateTransition> possible = transitions.getOrDefault(currentState, List.of());
        for (StateTransition t : possible) {
            if (t.toState() == targetState
                && (t.guard() == null || t.guard().test(context))) {
                currentState = targetState;
                if (t.action() != null) {
                    t.action().accept(context);
                }
                return true;
            }
        }
        return false;
    }

    public AgentState getCurrentState() {
        return currentState;
    }
}
```

### 2.2 AgentContext 工作流上下文

```java
public class AgentContext {
    private final Map<String, Object> variables = new ConcurrentHashMap<>();
    private final List<WorkflowEvent> eventLog = new ArrayList<>();

    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getVariable(String key) {
        return (T) variables.get(key);
    }

    public void logEvent(WorkflowEvent event) {
        eventLog.add(event);
    }

    public List<WorkflowEvent> getEventLog() {
        return List.copyOf(eventLog);
    }
}

public record WorkflowEvent(
    String eventType,
    AgentState fromState,
    AgentState toState,
    String description,
    LocalDateTime timestamp
) {}
```

---

## 3. Java 实现

### 3.1 代码审查 Agent 工作流

```java
public class CodeReviewWorkflow {

    public CodeReviewResult execute(String code, AgentContext context) {
        AgentStateMachine fsm = buildCodeReviewStateMachine();

        while (fsm.getCurrentState() != AgentState.COMPLETED
            && fsm.getCurrentState() != AgentState.ERROR) {

            switch (fsm.getCurrentState()) {
                case ANALYZING -> {
                    String analysis = analyzeCode(code);
                    context.setVariable("analysis", analysis);
                    fsm.transition(AgentState.PLANNING, context);
                }

                case PLANNING -> {
                    List<String> reviewTasks = planReview(code, context);
                    context.setVariable("reviewTasks", reviewTasks);
                    context.setVariable("currentTaskIndex", 0);
                    fsm.transition(AgentState.EXECUTING, context);
                }

                case EXECUTING -> {
                    List<String> reviewTasks = context.getVariable("reviewTasks");
                    int index = context.getVariable("currentTaskIndex");
                    if (index < reviewTasks.size()) {
                        String reviewComment = reviewTask(reviewTasks.get(index), code);
                        List<String> comments = context.getVariable("comments");
                        if (comments == null) comments = new ArrayList<>();
                        comments.add(reviewComment);
                        context.setVariable("comments", comments);
                        context.setVariable("currentTaskIndex", index + 1);
                    } else {
                        fsm.transition(AgentState.VERIFYING, context);
                    }
                }

                case VERIFYING -> {
                    boolean passed = verifyReview(context);
                    if (passed) {
                        fsm.transition(AgentState.COMPLETED, context);
                    } else {
                        fsm.transition(AgentState.PLANNING, context);
                    }
                }

                case ERROR -> {
                    return new CodeReviewResult(false, List.of("工作流错误"), context.getEventLog());
                }
            }
        }

        List<String> comments = context.getVariable("comments");
        return new CodeReviewResult(true,
            comments != null ? comments : List.of(), context.getEventLog());
    }

    private AgentStateMachine buildCodeReviewStateMachine() {
        AgentStateMachine fsm = new AgentStateMachine(AgentState.IDLE);

        fsm.addTransition(new StateTransition(
            AgentState.IDLE, AgentState.ANALYZING, "start", null, null));

        fsm.addTransition(new StateTransition(
            AgentState.ANALYZING, AgentState.PLANNING, "analyzed", null, null));

        fsm.addTransition(new StateTransition(
            AgentState.PLANNING, AgentState.EXECUTING, "planned", null, null));

        fsm.addTransition(new StateTransition(
            AgentState.EXECUTING, AgentState.VERIFYING,
            "all_tasks_done",
            ctx -> {
                List<String> tasks = ctx.getVariable("reviewTasks");
                int index = ctx.getVariable("currentTaskIndex");
                return index >= tasks.size();
            },
            null
        ));

        fsm.addTransition(new StateTransition(
            AgentState.VERIFYING, AgentState.COMPLETED, "verified",
            ctx -> ctx.getVariable("verificationPassed") != null
                && (boolean) ctx.getVariable("verificationPassed"),
            null
        ));

        fsm.addTransition(new StateTransition(
            AgentState.VERIFYING, AgentState.PLANNING, "not_verified",
            ctx -> ctx.getVariable("verificationPassed") == null
                || !(boolean) ctx.getVariable("verificationPassed"),
            null
        ));

        return fsm;
    }
}
```

### 3.2 声明式工作流定义

```java
public class DeclarativeWorkflowEngine {
    private final WorkflowDefinition definition;
    private final Map<String, NodeExecutor> executors;

    public DeclarativeWorkflowEngine(WorkflowDefinition definition,
                                     Map<String, NodeExecutor> executors) {
        this.definition = definition;
        this.executors = executors;
    }

    public WorkflowResult execute(AgentContext context) {
        String currentNode = definition.startNode();
        List<String> executedPath = new ArrayList<>();

        while (currentNode != null) {
            NodeDefinition node = definition.getNode(currentNode);
            if (node == null) break;

            NodeExecutor executor = executors.get(node.type());
            if (executor == null) throw new IllegalArgumentException(
                "未找到节点执行器: " + node.type());

            ExecutionResult result = executor.execute(node, context);
            executedPath.add(currentNode);

            if (result.status() == ExecutionStatus.FAILED) {
                currentNode = node.onFailure();
            } else if (result.status() == ExecutionStatus.COMPLETED) {
                currentNode = result.nextNode();
            } else {
                currentNode = node.next();
            }

            if (definition.endNodes().contains(currentNode)) {
                executedPath.add(currentNode);
                break;
            }
        }

        return new WorkflowResult(executedPath, context);
    }
}

public record WorkflowDefinition(
    String name,
    String startNode,
    List<String> endNodes,
    Map<String, NodeDefinition> nodes
) {
    public NodeDefinition getNode(String id) {
        return nodes.get(id);
    }
}

public record NodeDefinition(
    String id,
    String type,
    String next,
    String onFailure,
    Map<String, Object> config
) {}

public interface NodeExecutor {
    ExecutionResult execute(NodeDefinition node, AgentContext context);
}

public record ExecutionResult(
    ExecutionStatus status,
    String nextNode,
    Object data,
    String error
) {}

public enum ExecutionStatus {
    COMPLETED, FAILED, WAITING
}
```

---

## 4. Spring State Machine 集成

```java
@Configuration
@EnableStateMachineFactory
public class AgentStateMachineConfig
        extends StateMachineConfigurerAdapter<AgentState, StateEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<AgentState, StateEvent> states)
            throws Exception {
        states.withStates()
            .initial(AgentState.IDLE)
            .state(AgentState.ANALYZING)
            .state(AgentState.PLANNING)
            .state(AgentState.EXECUTING)
            .state(AgentState.WAITING_APPROVAL)
            .state(AgentState.VERIFYING)
            .end(AgentState.COMPLETED)
            .end(AgentState.ERROR);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<AgentState, StateEvent> transitions)
            throws Exception {
        transitions
            .withExternal()
                .source(AgentState.IDLE).target(AgentState.ANALYZING)
                .event(StateEvent.START)
            .and()
            .withExternal()
                .source(AgentState.ANALYZING).target(AgentState.PLANNING)
                .event(StateEvent.ANALYZED)
            .and()
            .withExternal()
                .source(AgentState.PLANNING).target(AgentState.EXECUTING)
                .event(StateEvent.PLANNED)
            .and()
            .withExternal()
                .source(AgentState.EXECUTING).target(AgentState.VERIFYING)
                .event(StateEvent.EXECUTED)
            .and()
            .withExternal()
                .source(AgentState.VERIFYING).target(AgentState.COMPLETED)
                .event(StateEvent.VERIFIED)
            .and()
            .withExternal()
                .source(AgentState.EXECUTING).target(AgentState.WAITING_APPROVAL)
                .event(StateEvent.NEEDS_APPROVAL)
            .and()
            .withExternal()
                .source(AgentState.WAITING_APPROVAL).target(AgentState.EXECUTING)
                .event(StateEvent.APPROVED);
    }
}

public enum StateEvent {
    START, ANALYZED, PLANNED, EXECUTED, VERIFIED,
    NEEDS_APPROVAL, APPROVED, FAILED
}
```

---

## 5. 适用场景与局限

### 5.1 适用场景

| 场景         | 说明                |
|------------|-------------------|
| CI/CD 流水线  | 构建 → 测试 → 部署 → 验证 |
| 审批工作流      | 提交 → 审核 → 驳回/通过  |
| 数据处理 ETL  | 抽取 → 转换 → 加载     |
| 客服工单       | 创建 → 分配 → 处理 → 关闭 |
| 代码审查       | 分析 → 审查 → 验证     |

### 5.2 局限

| 局限      | 缓解方案         |
|---------|--------------|
| 灵活性不足   | 组合动态 ReAct 模式 |
| 状态爆炸    | 分层状态机、子状态    |
| 维护成本高   | 可视化编辑 + 版本管理 |
| 不适合探索性任务 | 用 ReAct 替代   |

---

## 6. 组合模式：状态机 + ReAct

```java
public class HybridAgent {
    private final AgentStateMachine fsm;
    private final ReActEngine reactEngine;

    public HybridAgent() {
        this.fsm = new AgentStateMachine(AgentState.IDLE);
        this.reactEngine = new ReActEngine(llmClient, toolRegistry, 10);
    }

    public void execute(String task, AgentContext context) {
        fsm.transition(AgentState.ANALYZING, context);

        if (isSimpleTask(task)) {
            executeWithReAct(task, context);
        } else {
            executeWithWorkflow(task, context);
        }
    }

    private void executeWithReAct(String task, AgentContext context) {
        fsm.transition(AgentState.EXECUTING, context);
        String result = reactEngine.execute(task, context);
        context.setVariable("result", result);
        fsm.transition(AgentState.COMPLETED, context);
    }

    private boolean isSimpleTask(String task) {
        return task.length() < 200 && !task.contains("计划") && !task.contains("流程");
    }
}
```

---

## 7. 最佳实践

| 实践       | 说明                    |
|----------|-----------------------|
| 状态数量克制   | 5-15 个状态最理想，太多会难以维护   |
| 转移有守卫    | 每个转移都应有明确的守卫条件       |
| 超时处理     | 每个状态设置最大停留时间         |
| 死锁检测     | 监控状态循环和停滞           |
| 可观测性     | 状态变更日志、指标暴露         |
| 子状态机     | 复杂状态内嵌子状态机，保持顶层简洁   |
| 混合模式     | 简单任务 ReAct，复杂任务工作流   |
