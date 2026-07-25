# Tool Use / Function Calling 模式

> **定位**：LLM 调用外部工具（API、数据库、文件系统、代码执行器）的核心模式，是 AI Agent 从"语言模型"升级为"智能代理"的关键桥梁。

> **Token 估算**：约 3.7K tokens

---

## 1. 模式概述

### 1.1 核心思路

Tool Use 让 LLM 不仅能生成文本，还能**选择并调用**外部函数/工具。模型不直接执行代码，而是**输出结构化的调用意图**，由 Agent Runtime 负责实际执行并将结果返回给模型。

### 1.2 执行流程

```plaintext
用户输入 → LLM 分析意图 → 决定是否需要工具
                              ↓ 需要
                         输出 Tool Call (JSON)
                              ↓
                         Agent Runtime 执行
                              ↓
                         结果注入上下文
                              ↓
                         LLM 继续生成/总结
```

---

## 2. 核心模型

### 2.1 工具定义

```java
public record ToolDefinition(
    String name,
    String description,
    JsonSchema parameterSchema,
    ToolCategory category,
    boolean requiresApproval
) {}

public enum ToolCategory {
    READ_ONLY,
    SIDE_EFFECT,
    SYSTEM,
    NETWORK
}

public record JsonSchema(
    String type,
    Map<String, JsonSchemaProperty> properties,
    List<String> required
) {}

public record JsonSchemaProperty(
    String type,
    String description,
    List<String> enumValues
) {}
```

### 2.2 工具调用请求与结果

```java
public record ToolCallRequest(
    String id,
    String name,
    Map<String, Object> arguments
) {}

public record ToolCallResult(
    String id,
    String name,
    boolean success,
    Object data,
    String error
) {}

public record ToolCallBatch(
    List<ToolCallRequest> requests
) {}
```

---

## 3. Java 实现

### 3.1 工具注册中心

```java
public class ToolRegistry {
    private final Map<String, Tool> tools = new ConcurrentHashMap<>();

    public void register(Tool tool) {
        tools.put(tool.getDefinition().name(), tool);
    }

    public Tool get(String name) {
        return tools.get(name);
    }

    public List<ToolDefinition> getAvailableDefinitions() {
        return tools.values().stream()
            .map(Tool::getDefinition)
            .toList();
    }

    public String buildToolsPrompt() {
        StringBuilder sb = new StringBuilder("可用工具:\n");
        for (Tool tool : tools.values()) {
            ToolDefinition def = tool.getDefinition();
            sb.append("- ").append(def.name())
              .append(": ").append(def.description())
              .append("\n  参数: ").append(formatSchema(def.parameterSchema()))
              .append("\n");
        }
        return sb.toString();
    }

    private String formatSchema(JsonSchema schema) {
        StringBuilder sb = new StringBuilder("{");
        schema.properties().forEach((key, prop) -> {
            sb.append(key).append(": ").append(prop.type())
              .append(" // ").append(prop.description()).append(", ");
        });
        return sb.append("}").toString();
    }
}
```

### 3.2 Tool 接口与实现示例

```java
public interface Tool {

    ToolDefinition getDefinition();

    ToolCallResult execute(Map<String, Object> arguments);
}

public class WeatherTool implements Tool {

    @Override
    public ToolDefinition getDefinition() {
        return new ToolDefinition(
            "get_weather",
            "查询指定城市的天气信息",
            new JsonSchema(
                "object",
                Map.of(
                    "city", new JsonSchemaProperty("string", "城市名称，如北京", null),
                    "date", new JsonSchemaProperty("string", "日期，格式 yyyy-MM-dd", null)
                ),
                List.of("city")
            ),
            ToolCategory.READ_ONLY,
            false
        );
    }

    @Override
    public ToolCallResult execute(Map<String, Object> arguments) {
        String city = (String) arguments.get("city");
        return new ToolCallResult(
            UUID.randomUUID().toString(),
            "get_weather",
            true,
            Map.of("city", city, "temperature", 22, "condition", "晴"),
            null
        );
    }
}
```

### 3.3 并行工具调用执行器

```java
public class ParallelToolExecutor {
    private final ToolRegistry registry;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public ParallelToolExecutor(ToolRegistry registry) {
        this.registry = registry;
    }

    public List<ToolCallResult> executeBatch(List<ToolCallRequest> requests) {
        List<CompletableFuture<ToolCallResult>> futures = requests.stream()
            .map(req -> CompletableFuture.supplyAsync(() -> executeSingle(req), executor))
            .toList();

        return futures.stream()
            .map(CompletableFuture::join)
            .toList();
    }

    private ToolCallResult executeSingle(ToolCallRequest request) {
        Tool tool = registry.get(request.name());
        if (tool == null) {
            return new ToolCallResult(request.id(), request.name(),
                false, null, "工具未找到: " + request.name());
        }
        try {
            return tool.execute(request.arguments());
        } catch (Exception e) {
            return new ToolCallResult(request.id(), request.name(),
                false, null, e.getMessage());
        }
    }
}
```

---

## 4. OpenAI Function Calling 集成

```java
public class OpenAIFunctionCallingAgent {
    private final OpenAiClient client;

    public String chatWithTools(String userMessage, List<ToolDefinition> tools) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new SystemMessage("你是能调用工具的助手"));
        messages.add(new UserMessage(userMessage));

        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("gpt-4")
            .messages(messages)
            .tools(tools.stream().map(this::toOpenAITool).toList())
            .toolChoice("auto")
            .build();

        ChatCompletionResponse response = client.createChatCompletion(request);
        Choice choice = response.choices().get(0);

        if (choice.finishReason().equals("tool_calls")) {
            messages.add(choice.message());

            for (ToolCall call : choice.message().toolCalls()) {
                ToolCallResult result = executeToolCall(call);
                messages.add(new ToolMessage(call.id(), result.data().toString()));
            }

            request = ChatCompletionRequest.builder()
                .model("gpt-4")
                .messages(messages)
                .build();

            response = client.createChatCompletion(request);
        }

        return response.choices().get(0).message().content();
    }

    private Tool toOpenAITool(ToolDefinition def) {
        return Tool.builder()
            .type("function")
            .function(Function.builder()
                .name(def.name())
                .description(def.description())
                .parameters(def.parameterSchema())
                .build())
            .build();
    }
}
```

---

## 5. Spring AI 集成

```java
@Configuration
public class ToolConfiguration {

    @Bean
    @Description("查询指定城市的天气信息")
    public Function<WeatherRequest, WeatherResponse> getWeather() {
        return request -> {
            String city = request.city();
            return new WeatherResponse(city, 22, "晴");
        };
    }
}

public record WeatherRequest(
    @JsonPropertyDescription("城市名称，如北京") String city
) {}

public record WeatherResponse(
    String city,
    int temperature,
    String condition
) {}

@Service
public class WeatherAgentService {
    private final ChatClient chatClient;

    public WeatherAgentService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String ask(String question) {
        return chatClient.prompt()
            .user(question)
            .tools(new WeatherTool())
            .call()
            .content();
    }
}
```

---

## 6. 安全考量

### 6.1 工具分级

```java
public enum ToolPermission {
    SAFE,
    READ_ONLY,
    NEEDS_APPROVAL,
    DANGEROUS,
    BLOCKED
}
```

### 6.2 安全执行封装

```java
public class SafeToolExecutor {
    private final ToolRegistry registry;

    public ToolCallResult executeWithGuard(ToolCallRequest request) {
        Tool tool = registry.get(request.name());
        if (tool == null) {
            return failure(request, "工具未注册");
        }

        ToolDefinition def = tool.getDefinition();

        if (def.requiresApproval() && !hasApproval(request)) {
            return failure(request, "需要用户审批");
        }

        try {
            validateArguments(def.parameterSchema(), request.arguments());
            return tool.execute(request.arguments());
        } catch (Exception e) {
            return failure(request, e.getMessage());
        }
    }

    private void validateArguments(JsonSchema schema, Map<String, Object> args) {
        for (String required : schema.required()) {
            if (!args.containsKey(required)) {
                throw new IllegalArgumentException("缺少必填参数: " + required);
            }
        }
    }
}
```

---

## 7. 最佳实践

| 实践要点          | 说明                           |
|---------------|------------------------------|
| 工具描述要精确       | 含参数的详细 schema，枚举值要明确          |
| 错误信息要友好       | 工具执行失败时返回可读的错误描述，LLM 可以根据它重试  |
| 支持并行调用        | 多个无依赖的工具应在同一轮执行             |
| 结果截断          | 工具返回的数据可能很大，截断到模型可处理的上下文窗口内 |
| 审批机制          | 写操作、删除操作等需要用户确认             |
| 超时控制          | 每个工具调用设置超时时间                |
| 幂等性           | 工具尽量设计为幂等，便于重试              |
