---
alwaysApply: false
description: 编码时触发的设计原则和编码准则
triggerKeywords: ["代码", "编码", "实现", "修改", "新增", "重构", "class", "function", "method", "interface"]
---

# 编码规则

## 强制预加载

执行编码任务时，必须按需加载以下文件：

| 文件 | 加载条件 |
| ------ | --------- |
| `.trae/common/karpathy.md` | 所有编码任务 |
| `.trae/common/SOLID.md` | 涉及设计模式、架构、重构时 |

## 编码原则

1. **简单优先**：解决问题的最小代码，不做推测
2. **精准修改**：只改动必须改的，只清理自己造成的混乱
3. **目标驱动**：定义成功标准，循环直到验证完成
4. **编码前先思考**：明确假设，不要隐藏困惑

## 代码规范

- Java 代码遵循项目 DDD 分层与 Guice IoC 最佳实践（参见 project-docs/00-spec/project/03-backend.md）
- JavaFX FXML 遵循 MVVM 分离（Controller + ViewModel + FXML，参见 project-docs/00-spec/project/05-frontend.md）

