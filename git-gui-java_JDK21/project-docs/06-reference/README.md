# 06-reference — 参考资料

> 本目录存放 git-gui 项目开发过程中收集的参考资料、设计模式文档和 AI 辅助开发策略。

---

## 目录结构

```plaintext
06-reference/
├── README.md                    # 本文档
├── reference-prompt.txt         # 参考资料整理提示词（生成设计模式文档、按需加载策略）
└── docs/                        # 参考资料文档
    ├── Java 设计模式/             # Java 23 种设计模式（含案例）
    │   └── java-design-patterns.md
    ├── AI Agent 设计模式/         # 10 种 AI Agent 设计模式
    │   ├── 01-react-pattern.md
    │   ├── 02-tool-use-pattern.md
    │   ├── 03-chain-of-thought-pattern.md
    │   ├── 04-plan-execute-pattern.md
    │   ├── 05-multi-agent-pattern.md
    │   ├── 06-reflection-pattern.md
    │   ├── 07-memory-pattern.md
    │   ├── 08-rag-pattern.md
    │   ├── 09-human-in-the-loop-pattern.md
    │   └── 10-state-machine-pattern.md
    └── AI 规约按需加载策略.md      # 规约按需加载策略（通用方法论参考）
```

---

## 文档说明

### Java 设计模式

涵盖 23 种经典设计模式的理论与实践案例，为项目中的设计决策提供参考。git-gui 项目重点应用的模式包括：适配器模式（Git CLI 适配器）、策略模式（命令红线规则）、观察者模式（EventBus 任务通知）、单例模式（TaskManager）等。

### AI Agent 设计模式

收录 10 种 AI Agent 设计模式（ReAct、Tool Use、Chain of Thought、Plan-Execute、Multi-Agent、Reflection、Memory、RAG、Human-in-the-Loop、State Machine），为 AI 辅助开发流程提供方法论参考。

### AI 规约按需加载策略

通用方法论参考文档，介绍如何通过规则入口改造 + 场景触发器实现规约按需加载，避免每次任务全量加载所有 spec 文件导致的 token 浪费。git-gui 项目的实际配置以 `.trae/rules/` 为准。

---

## 规约引用

- 全局核心规则：[../00-spec/shared/00-core-base.md](../00-spec/shared/00-core-base.md)
- Markdown 编写规范：[../../.trae/common/markdown.md](../../.trae/common/markdown.md)
