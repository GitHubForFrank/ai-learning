# RAG 检索增强生成模式

> **定位**：在 LLM 生成回答前，先从外部知识库检索相关信息，将检索结果作为增强上下文注入 Prompt，使 Agent 能够基于实时、准确的外部知识回答问题。

> **Token 估算**：约 4.9K tokens

---

## 1. 模式概述

### 1.1 核心流程

```plaintext
┌─────────────┐     ┌─────────────┐     ┌──────────────┐
│  Document   │ →  │   Chunk &   │ →  │ Vector Store │
│  Ingestion  │     │   Embed     │     │   (索引存储)  │
└─────────────┘     └─────────────┘     └──────┬───────┘
                                               ↓ 检索
┌─────────────┐     ┌─────────────┐     ┌──────────────┐
│   LLM       │ ←  │   Prompt    │ ←  │   Retriever  │
│  生成回答     │     │   Augment   │     │   (向量检索)  │
└─────────────┘     └─────────────┘     └──────────────┘
```

### 1.2 两个阶段

| 阶段      | 说明                        | 时机   |
|---------|---------------------------|------|
| Ingestion | 文档 → 分块 → Embedding → 存储向量索引 | 离线/预处理 |
| Retrieval | 用户查询 → Embedding → 检索 → 排序 → 增强 Prompt | 在线/请求时 |

---

## 2. 核心模型

### 2.1 文档与分块

```java
public record Document(
    String id,
    String title,
    String content,
    String source,
    Map<String, Object> metadata
) {}

public record DocumentChunk(
    String id,
    String documentId,
    String content,
    int chunkIndex,
    int startPosition,
    int endPosition,
    float[] embedding,
    Map<String, Object> metadata
) {}

public record RetrievalResult(
    DocumentChunk chunk,
    double score,
    String rerankScore
) {}

public record AugmentedPrompt(
    String systemPrompt,
    List<RetrievalResult> retrievedChunks,
    String userQuery,
    String fullPrompt
) {}
```

---

## 3. Java 实现

### 3.1 文档摄取管道

```java
public class DocumentIngestionPipeline {
    private final DocumentLoader loader;
    private final TextSplitter splitter;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;

    public DocumentIngestionPipeline(
            DocumentLoader loader,
            TextSplitter splitter,
            EmbeddingService embeddingService,
            VectorStore vectorStore) {
        this.loader = loader;
        this.splitter = splitter;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
    }

    public void ingest(Path filePath) {
        Document doc = loader.load(filePath);
        List<DocumentChunk> chunks = splitter.split(doc);
        List<float[]> embeddings = embeddingService.embedBatch(
            chunks.stream().map(DocumentChunk::content).toList()
        );

        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            DocumentChunk enrichedChunk = new DocumentChunk(
                chunk.id(), chunk.documentId(), chunk.content(),
                chunk.chunkIndex(), chunk.startPosition(), chunk.endPosition(),
                embeddings.get(i), chunk.metadata()
            );
            vectorStore.insert(enrichedChunk.id(), embeddings.get(i), enrichedChunk);
        }
    }
}
```

### 3.2 文本分割器

```java
public class RecursiveCharacterTextSplitter implements TextSplitter {
    private final int chunkSize;
    private final int chunkOverlap;
    private final List<String> separators = List.of("\n\n", "\n", "。", ".", " ", "");

    public RecursiveCharacterTextSplitter(int chunkSize, int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    @Override
    public List<DocumentChunk> split(Document document) {
        List<String> segments = splitText(document.content(), separators);
        List<DocumentChunk> chunks = new ArrayList<>();
        int position = 0;

        for (int i = 0; i < segments.size(); i++) {
            String segment = segments.get(i);
            String chunkId = document.id() + "_chunk_" + i;
            chunks.add(new DocumentChunk(
                chunkId, document.id(), segment, i,
                position, position + segment.length(),
                null, document.metadata()
            ));
            position += segment.length();
        }

        return chunks;
    }

    private List<String> splitText(String text, List<String> separatorList) {
        if (separatorList.isEmpty()) {
            return List.of(text);
        }

        String sep = separatorList.get(0);
        if (sep.isEmpty()) {
            return splitBySize(text);
        }

        List<String> result = new ArrayList<>();
        for (String part : text.split(Pattern.quote(sep), -1)) {
            if (part.length() <= chunkSize) {
                if (!part.isBlank()) result.add(part);
            } else {
                result.addAll(splitText(part,
                    separatorList.subList(1, separatorList.size())));
            }
        }
        return result;
    }

    private List<String> splitBySize(String text) {
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < text.length(); i += chunkSize - chunkOverlap) {
            int end = Math.min(i + chunkSize, text.length());
            String chunk = text.substring(i, end).trim();
            if (!chunk.isEmpty()) {
                parts.add(chunk);
            }
        }
        return parts;
    }
}
```

### 3.3 检索 + 生成管道

```java
public class RAGPipeline {
    private final VectorStore vectorStore;
    private final EmbeddingService embeddingService;
    private final LLMClient llmClient;
    private final int topK;
    private final double similarityThreshold;

    public RAGPipeline(VectorStore vectorStore, EmbeddingService embeddingService,
                       LLMClient llmClient, int topK, double similarityThreshold) {
        this.vectorStore = vectorStore;
        this.embeddingService = embeddingService;
        this.llmClient = llmClient;
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;
    }

    public RAGResult query(String userQuery) {
        float[] queryEmbedding = embeddingService.embed(userQuery);
        List<VectorStore.SearchResult> searchResults = vectorStore.search(queryEmbedding, topK);

        List<RetrievalResult> relevant = searchResults.stream()
            .filter(r -> r.score() >= similarityThreshold)
            .map(r -> new RetrievalResult(
                (DocumentChunk) r.metadata(), r.score(), null
            ))
            .toList();

        AugmentedPrompt augmented = buildPrompt(userQuery, relevant);
        String answer = llmClient.chat(augmented.fullPrompt());

        return new RAGResult(userQuery, relevant, answer);
    }

    private AugmentedPrompt buildPrompt(String query, List<RetrievalResult> results) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            RetrievalResult r = results.get(i);
            context.append("[来源 ").append(i + 1).append("] ")
                   .append(r.chunk().content()).append("\n\n");
        }

        String systemPrompt = """
            你是一个基于知识库的问答助手。请根据提供的参考资料回答问题。

            规则:
            1. 如果答案在参考资料中，请引用来源编号
            2. 如果参考资料不足以回答问题，请明确说明
            3. 不要编造参考资料中不存在的信息
            """;

        String fullPrompt = systemPrompt + "\n\n参考资料:\n" + context
            + "\n问题: " + query + "\n\n答案: ";

        return new AugmentedPrompt(systemPrompt, results, query, fullPrompt);
    }
}

public record RAGResult(
    String query,
    List<RetrievalResult> sources,
    String answer
) {}
```

---

## 4. 高级检索策略

### 4.1 混合检索（向量 + 关键词）

```java
public class HybridRetriever {
    private final VectorStore vectorStore;
    private final KeywordIndex keywordIndex;
    private final double vectorWeight;

    public HybridRetriever(VectorStore vectorStore, KeywordIndex keywordIndex, double vectorWeight) {
        this.vectorStore = vectorStore;
        this.keywordIndex = keywordIndex;
        this.vectorWeight = vectorWeight;
    }

    public List<RetrievalResult> retrieve(String query, int topK) {
        float[] queryEmbedding = embeddingService.embed(query);
        List<VectorStore.SearchResult> vectorResults = vectorStore.search(queryEmbedding, topK * 2);

        List<KeywordIndex.SearchResult> keywordResults = keywordIndex.search(query, topK * 2);

        Map<String, RetrievalCandidate> candidates = new HashMap<>();

        for (var r : vectorResults) {
            candidates.put(r.id(), new RetrievalCandidate(
                (DocumentChunk) r.metadata(), r.score() * vectorWeight
            ));
        }

        for (var r : keywordResults) {
            candidates.compute(r.id(), (k, existing) -> {
                if (existing == null) {
                    return new RetrievalCandidate(
                        (DocumentChunk) r.metadata(), r.score() * (1 - vectorWeight)
                    );
                }
                return new RetrievalCandidate(
                    existing.chunk(),
                    existing.score() + r.score() * (1 - vectorWeight)
                );
            });
        }

        return candidates.values().stream()
            .sorted(Comparator.comparingDouble(RetrievalCandidate::score).reversed())
            .limit(topK)
            .map(c -> new RetrievalResult(c.chunk(), c.score(), null))
            .toList();
    }
}

private record RetrievalCandidate(DocumentChunk chunk, double score) {}
```

### 4.2 重排序（Re-ranking）

```java
public class RerankingRetriever {
    private final VectorStore vectorStore;
    private final LLMClient llmClient;

    public List<RetrievalResult> retrieveWithRerank(String query, int topK) {
        float[] queryEmbedding = embeddingService.embed(query);
        List<VectorStore.SearchResult> initial = vectorStore.search(queryEmbedding, topK * 3);

        String prompt = """
            对以下文档片段与问题的相关性打分（0-10）:

            问题: %s

            文档片段:
            %s

            仅返回 JSON 数组: [{"id": "xxx", "score": 8}]
            """.formatted(query, formatChunksForRerank(initial));

        String response = llmClient.chat(prompt);
        Map<String, Double> rerankScores = parseRerankScores(response);

        return initial.stream()
            .map(r -> new RetrievalResult(
                (DocumentChunk) r.metadata(),
                rerankScores.getOrDefault(r.id(), r.score()),
                String.valueOf(rerankScores.getOrDefault(r.id(), r.score()))
            ))
            .sorted(Comparator.comparingDouble(RetrievalResult::score).reversed())
            .limit(topK)
            .toList();
    }
}
```

---

## 5. Spring AI 集成

```java
@Configuration
public class RAGConfiguration {

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return new SimpleVectorStore(embeddingModel);
    }
}

@Service
public class RAGService {
    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public RAGService(VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
    }

    public String askWithKnowledge(String question) {
        return chatClient.prompt()
            .user(question)
            .advisors(new QuestionAnswerAdvisor(vectorStore))
            .call()
            .content();
    }

    public void loadDocument(Resource resource) {
        vectorStore.accept(
            new TokenTextSplitter().apply(
                new TextReader(resource).read()
            )
        );
    }
}
```

---

## 6. 适用场景与局限

### 6.1 适用场景

| 场景        | 说明                |
|-----------|-------------------|
| 企业知识库问答   | 内部文档、SOP、HR 政策等  |
| 技术支持      | 产品手册、FAQ         |
| 法律/合规     | 法规文档检索 + 条款解释    |
| 医疗咨询      | 医学文献 + 指南        |
| 代码库问答     | 检索代码和文档          |

### 6.2 局限

| 局限        | 缓解方案              |
|-----------|-------------------|
| 分割破坏语义    | 智能分块、语义分块          |
| 检索不精准     | 混合检索 + 重排序        |
| 上下文窗口溢出   | 检索结果限制 + 压缩       |
| 过期知识      | 定期重新索引            |
| 多模态支持不足   | 多模态 Embedding 模型 |

---

## 7. 最佳实践

| 实践         | 说明                       |
|------------|--------------------------|
| 分块策略       | 500-1000 token，保持语义完整性   |
| 重叠窗口       | 10%-20% 重叠，避免语义截断        |
| 元数据保留      | 保留文档标题、章节、页码等便于溯源       |
| 检索后过滤      | 按相关性阈值、时间范围、类型过滤        |
| 来源标注       | 在回答中引用来源编号/链接           |
| 缓存查询向量     | 减少重复 Embedding 调用       |
| 定期评估检索质量   | 人工标注 + 自动化指标追踪         |
