# Human-in-the-Loop 人机协同模式

> **定位**：在 AI Agent 自主执行的过程中，在关键节点引入人工审批、确认或干预，确保安全性、合规性和结果质量，实现人机协作的最佳平衡。

> **Token 估算**：约 4.6K tokens

---

## 1. 模式概述

### 1.1 核心问题

Agent 全自动执行虽然高效，但在以下场景存在风险：
- 涉及资金操作、数据删除等破坏性行为
- 需要主观判断（审美、伦理、法律解读）
- 模型能力边界之外的任务

Human-in-the-Loop 将人工判断嵌入 Agent 执行流程的关键节点。

### 1.2 干预时机

```plaintext
┌────────────────────────────────────────────────────┐
│              人机协同干预点                           │
│                                                    │
│  User ─→ [①输入审核] ─→ Agent ─→ [②计划审批]        │
│                                       ↓            │
│                                   执行步骤          │
│                                       ↓            │
│                                [③关键操作确认]      │
│                                       ↓            │
│                                    结果            │
│                                       ↓            │
│              User ←── [④结果审核] ←──┘              │
└────────────────────────────────────────────────────┘
```

---

## 2. 核心模型

### 2.1 审批请求与决策

```java
public record ApprovalRequest(
    String id,
    ApprovalType type,
    String description,
    String context,
    Object payload,
    RiskLevel riskLevel,
    LocalDateTime createdAt,
    Duration timeout
) {}

public enum ApprovalType {
    PLAN_REVIEW,
    TOOL_EXECUTION,
    CONTENT_PUBLISH,
    DATA_MODIFICATION,
    FINAL_OUTPUT
}

public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

public record ApprovalDecision(
    String requestId,
    Decision decision,
    String comment,
    Map<String, Object> modifications,
    String approver,
    LocalDateTime decidedAt
) {}

public enum Decision {
    APPROVE,
    REJECT,
    MODIFY,
    DELEGATE,
    TIMEOUT
}
```

### 2.2 审批门控接口

```java
public interface ApprovalGate {
    boolean requiresApproval(ToolCallRequest request);
    ApprovalRequest createApprovalRequest(ToolCallRequest request);
}

public interface ApprovalHandler {
    ApprovalDecision awaitDecision(ApprovalRequest request);
}
```

---

## 3. Java 实现

### 3.1 审批门控引擎

```java
public class HumanInTheLoopEngine {
    private final List<ApprovalGate> gates;
    private final ApprovalHandler approvalHandler;
    private final Map<String, ApprovalPolicy> policies;

    public HumanInTheLoopEngine(List<ApprovalGate> gates,
                                ApprovalHandler approvalHandler,
                                Map<String, ApprovalPolicy> policies) {
        this.gates = gates;
        this.approvalHandler = approvalHandler;
        this.policies = policies;
    }

    public ToolCallResult executeWithApproval(ToolCallRequest request) {
        boolean needsApproval = gates.stream()
            .anyMatch(gate -> gate.requiresApproval(request));

        if (needsApproval) {
            ApprovalRequest approvalRequest = gates.stream()
                .filter(g -> g.requiresApproval(request))
                .findFirst()
                .map(g -> g.createApprovalRequest(request))
                .orElseThrow();

            ApprovalDecision decision = approvalHandler.awaitDecision(approvalRequest);

            return switch (decision.decision()) {
                case APPROVE -> executeOriginal(request);
                case REJECT -> new ToolCallResult(request.id(), request.name(),
                    false, null, "用户拒绝执行: " + decision.comment());
                case MODIFY -> executeModified(request, decision.modifications());
                case TIMEOUT -> handleTimeout(request, approvalRequest);
                case DELEGATE -> new ToolCallResult(request.id(), request.name(),
                    false, null, "任务已委派给人工");
            };
        }

        return executeOriginal(request);
    }
}
```

### 3.2 审批策略定义

```java
public record ApprovalPolicy(
    String toolName,
    List<ApprovalCondition> conditions,
    ApprovalAction defaultAction
) {}

public record ApprovalCondition(
    ConditionField field,
    ConditionOperator operator,
    Object value
) {}

public enum ConditionField {
    FILE_COUNT, DATA_SIZE, IS_DELETE, IS_PRODUCTION, COST_AMOUNT
}

public enum ConditionOperator {
    GREATER_THAN, LESS_THAN, EQUALS, CONTAINS, MATCHES
}

public enum ApprovalAction {
    AUTO_APPROVE, REQUIRE_APPROVAL, BLOCK
}

public class PolicyBasedApprovalGate implements ApprovalGate {
    private final Map<String, List<ApprovalPolicy>> toolPolicies;

    public PolicyBasedApprovalGate(Map<String, List<ApprovalPolicy>> toolPolicies) {
        this.toolPolicies = toolPolicies;
    }

    @Override
    public boolean requiresApproval(ToolCallRequest request) {
        List<ApprovalPolicy> policies = toolPolicies.getOrDefault(
            request.name(), List.of());

        if (policies.isEmpty()) {
            return true;
        }

        for (ApprovalPolicy policy : policies) {
            for (ApprovalCondition condition : policy.conditions()) {
                if (!evaluateCondition(condition, request)) {
                    continue;
                }
                if (policy.defaultAction() == ApprovalAction.REQUIRE_APPROVAL) {
                    return true;
                }
                if (policy.defaultAction() == ApprovalAction.BLOCK) {
                    throw new SecurityException("操作被策略阻止: " + request.name());
                }
            }
        }

        return false;
    }

    private boolean evaluateCondition(ApprovalCondition condition, ToolCallRequest request) {
        Object actualValue = extractField(condition.field(), request);
        return switch (condition.operator()) {
            case GREATER_THAN -> compare(actualValue, condition.value()) > 0;
            case LESS_THAN -> compare(actualValue, condition.value()) < 0;
            case EQUALS -> Objects.equals(actualValue, condition.value());
            case CONTAINS -> String.valueOf(actualValue)
                .contains(String.valueOf(condition.value()));
            case MATCHES -> String.valueOf(actualValue)
                .matches(String.valueOf(condition.value()));
        };
    }

    private int compare(Object a, Object b) {
        if (a instanceof Number na && b instanceof Number nb) {
            return Double.compare(na.doubleValue(), nb.doubleValue());
        }
        return String.valueOf(a).compareTo(String.valueOf(b));
    }
}
```

---

## 4. 异步审批流程

### 4.1 审批等待与超时

```java
public class AsyncApprovalHandler implements ApprovalHandler {
    private final BlockingQueue<ApprovalDecision> decisionQueue = new LinkedBlockingQueue<>();
    private final Map<String, CompletableFuture<ApprovalDecision>> pendingFutures
        = new ConcurrentHashMap<>();

    @Override
    public ApprovalDecision awaitDecision(ApprovalRequest request) {
        CompletableFuture<ApprovalDecision> future = new CompletableFuture<>();
        pendingFutures.put(request.id(), future);

        sendApprovalNotification(request);

        try {
            return future.get(request.timeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            pendingFutures.remove(request.id());
            return new ApprovalDecision(request.id(), Decision.TIMEOUT,
                "审批超时", Map.of(), "system", LocalDateTime.now());
        } catch (Exception e) {
            pendingFutures.remove(request.id());
            throw new RuntimeException("审批中断", e);
        }
    }

    public void submitDecision(ApprovalDecision decision) {
        CompletableFuture<ApprovalDecision> future = pendingFutures.remove(decision.requestId());
        if (future != null) {
            future.complete(decision);
        }
    }
}
```

### 4.2 WebSocket 审批通知

```java
@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {
    private final AsyncApprovalHandler approvalHandler;
    private final SimpMessagingTemplate messagingTemplate;

    public ApprovalController(AsyncApprovalHandler approvalHandler,
                              SimpMessagingTemplate messagingTemplate) {
        this.approvalHandler = approvalHandler;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/{requestId}/decide")
    public ResponseEntity<Void> submitDecision(
            @PathVariable String requestId,
            @RequestBody ApprovalDecisionDTO dto) {
        ApprovalDecision decision = new ApprovalDecision(
            requestId,
            Decision.valueOf(dto.decision()),
            dto.comment(),
            dto.modifications(),
            dto.approver(),
            LocalDateTime.now()
        );
        approvalHandler.submitDecision(decision);
        return ResponseEntity.ok().build();
    }

    private void notifyUser(String userId, ApprovalRequest request) {
        messagingTemplate.convertAndSendToUser(
            userId,
            "/queue/approvals",
            new ApprovalNotification(request)
        );
    }
}

public record ApprovalNotification(
    String id,
    ApprovalType type,
    String description,
    RiskLevel riskLevel,
    Duration timeout
) {}
```

---

## 5. LangChain4j 集成

```java
public class LangChain4jHumanInTheLoop {

    public Assistant createAgentWithApproval() {
        ChatLanguageModel model = OpenAiChatModel.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .modelName("gpt-4")
            .build();

        ToolSpecification fileDelete = ToolSpecification.builder()
            .name("delete_file")
            .description("删除文件 - 需要人工审批")
            .addParameter("filePath", JsonSchemaProperty.STRING)
            .build();

        return AiServices.builder(Assistant.class)
            .chatLanguageModel(model)
            .tools(new GuardedFileDeleteTool())
            .build();
    }
}

public class GuardedFileDeleteTool {
    private final ApprovalHandler approvalHandler;

    @Tool("删除文件")
    public String deleteFile(
            @P("文件路径") String filePath) {

        ApprovalRequest request = new ApprovalRequest(
            UUID.randomUUID().toString(),
            ApprovalType.TOOL_EXECUTION,
            "删除文件: " + filePath,
            "文件删除操作",
            Map.of("filePath", filePath),
            RiskLevel.HIGH,
            LocalDateTime.now(),
            Duration.ofMinutes(5)
        );

        ApprovalDecision decision = approvalHandler.awaitDecision(request);

        if (decision.decision() == Decision.APPROVE) {
            Files.delete(Path.of(filePath));
            return "文件已删除: " + filePath;
        }
        return "操作被拒绝: " + decision.comment();
    }
}
```

---

## 6. 适用场景与局限

### 6.1 适用场景

| 场景        | 干预点               |
|-----------|-------------------|
| 金融交易      | 大额转账前审批           |
| 代码部署      | 生产环境部署前审批         |
| 内容发布      | 发布前审核             |
| 数据管理      | 批量删除/修改前确认        |
| 医疗诊断      | 给出建议后，最终由医生确认     |
| 法律文书      | AI 生成草稿，律师审核修改    |

### 6.2 局限

| 局限        | 缓解方案            |
|-----------|-----------------|
| 审批延迟      | 异步审批 + 超时机制    |
| 审批疲劳      | 智能分类，低频高风险才审批  |
| 策略维护成本    | 可视化策略编辑器       |
| 人为错误      | 审批辅助信息 + 风险提示  |

---

## 7. 最佳实践

| 实践           | 说明                     |
|--------------|------------------------|
| 风险分级         | 低风险自动执行，高风险需审批          |
| 审批信息充分       | 附带上下文、影响范围、风险提示        |
| 超时兜底         | 超时后降级为安全默认行为           |
| 审计日志         | 记录所有审批请求和决策，不可篡改       |
| 批量审批         | 同类操作支持批量审批，减少疲劳        |
| 可撤销          | 关键操作执行后保留回滚机制          |
| 渐进式自动化       | 初始全审批 → 积累信任 → 逐步放开自动化  |
