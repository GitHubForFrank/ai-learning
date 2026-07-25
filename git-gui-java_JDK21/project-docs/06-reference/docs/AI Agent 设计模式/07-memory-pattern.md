# Memory 记忆模式

> **定位**：为 AI Agent 赋予持久化记忆能力，包括短期记忆（当前对话）、长期记忆（跨会话）、工作记忆（当前任务上下文），实现有状态、可学习的智能体。

> **Token 估算**：约 4K tokens

---

## 1. 模式概述

### 1.1 三种记忆类型

```plaintext
┌─────────────────────────────────────────────┐
│              记忆层次结构                      │
│                                             │
│  ┌──────────────┐  当前任务上下文，容量最小      │
│  │  工作记忆      │  如：当前推理步骤、中间结果     │
│  └──────────────┘                           │
│        ↓ 筛选重要信息存储                      │
│  ┌──────────────┐  当前会话，容量中等           │
│  │  短期记忆      │  如：对话历史、本轮任务        │
│  └──────────────┘                           │
│        ↓ 提取关键信息持久化                     │
│  ┌──────────────┐  跨会话，容量最大             │
│  │  长期记忆      │  如：用户偏好、知识积累        │
│  └──────────────┘                           │
└─────────────────────────────────────────────┘
```

---

## 2. 核心模型

### 2.1 记忆条目

```java
public record MemoryEntry(
    String id,
    MemoryType type,
    String content,
    double importance,
    LocalDateTime createdAt,
    LocalDateTime lastAccessedAt,
    int accessCount,
    Map<String, Object> metadata
) {}

public enum MemoryType {
    EPISODIC,
    SEMANTIC,
    PROCEDURAL,
    PREFERENCE
}

public record MemoryQuery(
    String query,
    MemoryType type,
    int topK,
    double minRelevance,
    LocalDateTime timeRangeStart
) {}

public record MemorySearchResult(
    MemoryEntry entry,
    double relevanceScore
) {}
```

---

## 3. Java 实现

### 3.1 短期记忆（对话窗口管理）

```java
public class ShortTermMemory {
    private final Deque<Message> messages = new ArrayDeque<>();
    private final int maxTokens;
    private final int reservedResponseTokens;
    private int currentTokenCount;

    public ShortTermMemory(int maxTokens, int reservedResponseTokens) {
        this.maxTokens = maxTokens;
        this.reservedResponseTokens = reservedResponseTokens;
    }

    public void addMessage(Message message) {
        messages.addLast(message);
        currentTokenCount += estimateTokens(message.content());
        evictIfNeeded();
    }

    public List<Message> getContextWindow() {
        return new ArrayList<>(messages);
    }

    private void evictIfNeeded() {
        while (currentTokenCount > maxTokens - reservedResponseTokens && !messages.isEmpty()) {
            Message removed = messages.removeFirst();
            currentTokenCount -= estimateTokens(removed.content());
        }
    }

    private int estimateTokens(String text) {
        return (int) Math.ceil(text.length() / 3.0);
    }

    public void compact() {
        if (messages.size() <= 6) return;

        Message systemMsg = messages.removeFirst();

        List<Message> toCompress = new ArrayList<>(messages);
        messages.clear();
        currentTokenCount = 0;

        messages.addFirst(systemMsg);
        currentTokenCount += estimateTokens(systemMsg.content());

        int keepLast = 4;
        List<Message> recent = toCompress.subList(
            Math.max(0, toCompress.size() - keepLast),
            toCompress.size()
        );

        for (Message msg : recent) {
            messages.addLast(msg);
            currentTokenCount += estimateTokens(msg.content());
        }
    }
}

public record Message(String role, String content) {}
```

### 3.2 长期记忆（向量化存储）

```java
public class LongTermMemory {
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;

    public LongTermMemory(EmbeddingService embeddingService, VectorStore vectorStore) {
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
    }

    public void store(MemoryEntry entry) {
        float[] embedding = embeddingService.embed(entry.content());
        vectorStore.insert(entry.id(), embedding, entry);
    }

    public List<MemorySearchResult> recall(MemoryQuery query) {
        float[] queryEmbedding = embeddingService.embed(query.query());
        List<VectorStore.SearchResult> results = vectorStore.search(
            queryEmbedding, query.topK()
        );

        return results.stream()
            .filter(r -> r.score() >= query.minRelevance())
            .map(r -> new MemorySearchResult(
                (MemoryEntry) r.metadata(), r.score()
            ))
            .toList();
    }

    public void forget(String memoryId) {
        vectorStore.delete(memoryId);
    }

    public void consolidate(List<MemoryEntry> entries) {
        List<MemoryEntry> important = entries.stream()
            .filter(e -> e.importance() > 0.7)
            .toList();

        for (MemoryEntry entry : important) {
            store(entry);
        }
    }
}
```

### 3.3 向量存储接口

```java
public interface VectorStore {

    void insert(String id, float[] vector, Object metadata);

    List<SearchResult> search(float[] queryVector, int topK);

    void delete(String id);

    record SearchResult(String id, float score, Object metadata) {}
}

public interface EmbeddingService {
    float[] embed(String text);
    List<float[]> embedBatch(List<String> texts);
}
```

---

## 4. 记忆管理策略

### 4.1 重要性评分

```java
public class ImportanceScorer {
    private final LLMClient llmClient;

    public double score(String content) {
        String prompt = """
            评估以下内容的长期记忆重要性（0-1）:
            - 用户偏好: 0.9
            - 重要决策: 0.8
            - 知识信息: 0.6
            - 一般对话: 0.2

            内容: %s

            仅返回 0-1 之间的数字:
            """.formatted(content);

        String response = llmClient.chat(prompt);
        return Double.parseDouble(response.trim());
    }
}
```

### 4.2 记忆统一管理器

```java
public class MemoryManager {
    private final ShortTermMemory shortTermMemory;
    private final LongTermMemory longTermMemory;
    private final ImportanceScorer importanceScorer;
    private final Map<String, Object> workingMemory = new ConcurrentHashMap<>();

    public MemoryManager(ShortTermMemory shortTermMemory,
                         LongTermMemory longTermMemory,
                         ImportanceScorer importanceScorer) {
        this.shortTermMemory = shortTermMemory;
        this.longTermMemory = longTermMemory;
        this.importanceScorer = importanceScorer;
    }

    public void addConversationTurn(String userInput, String agentResponse) {
        shortTermMemory.addMessage(new Message("user", userInput));
        shortTermMemory.addMessage(new Message("assistant", agentResponse));

        double importance = importanceScorer.score(userInput + "\n" + agentResponse);
        if (importance > 0.6) {
            longTermMemory.store(new MemoryEntry(
                UUID.randomUUID().toString(),
                MemoryType.EPISODIC,
                "用户: " + userInput + "\n助手: " + agentResponse,
                importance,
                LocalDateTime.now(),
                LocalDateTime.now(),
                0,
                Map.of()
            ));
        }
    }

    public List<MemorySearchResult> recallRelevantMemories(String context, int topK) {
        return longTermMemory.recall(new MemoryQuery(
            context, null, topK, 0.5, null
        ));
    }

    public String buildAugmentedPrompt(String userQuery) {
        List<MemorySearchResult> relevant = recallRelevantMemories(userQuery, 3);

        StringBuilder memorySection = new StringBuilder();
        if (!relevant.isEmpty()) {
            memorySection.append("相关历史记忆:\n");
            for (MemorySearchResult result : relevant) {
                memorySection.append("- ").append(result.entry().content()).append("\n");
            }
            memorySection.append("\n");
        }

        return memorySection + "当前对话:\n" +
            shortTermMemory.getContextWindow().stream()
                .map(m -> m.role() + ": " + m.content())
                .collect(Collectors.joining("\n"));
    }
}
```

---

## 5. Spring AI 集成

```java
@Configuration
public class MemoryConfiguration {

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return new SimpleVectorStore(embeddingModel);
    }

    @Bean
    public ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }

    @Bean
    public MessageChatMemoryAdvisor chatMemoryAdvisor(ChatMemory chatMemory) {
        return new MessageChatMemoryAdvisor(chatMemory);
    }
}

@Service
public class MemoryAwareAgentService {
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ChatMemory chatMemory;

    public String chatWithMemory(String conversationId, String userMessage) {
        List<Document> relevantDocs = vectorStore.similaritySearch(
            SearchRequest.query(userMessage).withTopK(3)
        );

        String memoryContext = relevantDocs.stream()
            .map(Document::getContent)
            .collect(Collectors.joining("\n---\n"));

        return chatClient.prompt()
            .user("""
                相关记忆:
                %s

                用户: %s
                """.formatted(memoryContext, userMessage))
            .advisors(new MessageChatMemoryAdvisor(chatMemory))
            .call()
            .content();
    }
}
```

---

## 6. 适用场景与局限

### 6.1 适用场景

| 场景         | 记忆类型      |
|------------|-----------|
| 客服机器人      | 长期记忆用户偏好  |
| 个人助手       | 全类型记忆     |
| 知识库问答      | 短期 + 语义记忆 |
| 编程助手       | 工作记忆为主    |
| 游戏 NPC    | 情节记忆      |

### 6.2 局限

| 局限        | 缓解方案          |
|-----------|---------------|
| 向量检索不精确   | 混合检索：向量 + 关键词 |
| 记忆膨胀      | 重要性评分 + 定期清理  |
| 隐私问题      | 本地存储 + 数据脱敏   |
| 上下文窗口有限   | 压缩算法 + 分层检索   |

---

## 7. 最佳实践

| 实践         | 说明                       |
|------------|--------------------------|
| 分层记忆       | 短/中/长期各有不同策略，避免混用       |
| 重要性评分      | 用 LLM 或规则评分，决定是否长期存储     |
| 定期遗忘       | 过期、低重要性记忆应清理            |
| 记忆摘要       | 超长对话应先摘要再存储，而非全量         |
| 用户可控       | 用户可查看、删除自己的记忆           |
| 检索增强       | 长期记忆的检索结果应标注来源和可信度      |
