---
alwaysApply: false
description: 编写文档时触发的 Markdown 规范
triggerKeywords: ["文档", "Markdown", "README", "md", "编写文档", "更新文档"]
---

# 文档编写规则

## 强制预加载

执行文档任务时，必须加载以下文件：

| 文件 | 加载条件 |
| ------ | --------- |
| `.trae/common/markdown.md` | 所有文档任务 |

## 文档规范速查

- 代码块必须指定语言标签
- JSON 三档分类：`json` / `jsonc` / `json5`
- 标题前后各空一行
- 列表前后各空一行
- 文件末尾保留一个空行

