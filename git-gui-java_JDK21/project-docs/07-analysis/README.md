# 07-analysis — 需求分析

> 本目录存放 git-gui 项目的需求分析文档与产品需求文档（PRD），是项目所有功能设计的起点。

---

## 目录结构

```plaintext
07-analysis/
├── README.md                                                   # 本文档
├── AI 提示词笔记.txt                                            # 需求分析过程中使用的 AI 提示词
└── report/                                                     # 需求分析报告
    └── Java图形化Git客户端（类TortoiseGit）产品需求文档.md        # PRD 主文档
```

---

## 文档说明

### PRD — 产品需求文档

项目的核心需求文档，定义了产品的完整功能范围，包括：

- **产品定位**：Java 图形化 Git 客户端，对标 TortoiseGit
- **功能需求**：仓库管理、提交、远程同步、分支/标签、日志、冲突解决、高级功能（Bisect、Submodule、Worktree、LFS、Hook、GPG）
- **命令红线体系**：七层闭环安全管控（阻断类 + 二次确认类）
- **非功能需求**：跨平台、单实例、性能、国际化、主题、凭证安全

PRD 是项目所有 spec 文件（BR 规则、服务契约、数据库设计等）的权威来源。

---

## 规约引用

- 业务规则（BR-01~BR-42）：[../00-spec/project/01-domain.md](../00-spec/project/01-domain.md)
- 项目介绍（含 PRD 摘要）：[../02-design/01-项目介绍.md](../02-design/01-项目介绍.md)
- 全局核心规则：[../00-spec/shared/00-core-base.md](../00-spec/shared/00-core-base.md)