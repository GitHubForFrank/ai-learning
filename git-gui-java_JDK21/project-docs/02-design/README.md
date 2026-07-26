# 02-design — 设计文档

> 本目录存放 git-gui 项目的架构设计与产品介绍文档，是理解项目整体设计的入口。

---

## 目录结构

```plaintext
02-design/
├── README.md                # 本文档
├── 01-项目介绍.md            # 产品定位、技术栈、核心功能、目录结构、快速启动
└── 02-架构设计.md            # 架构分层、模块划分、核心流程、技术选型决策
```

---

## 文档说明

### 01-项目介绍.md

项目总览入口，介绍产品定位（类 TortoiseGit 的 Java 图形化 Git 客户端）、技术栈选型、与 TortoiseGit 的差异、核心功能概览、目录结构和快速启动方式。

适合：初次接触项目的开发者、需要了解项目全貌的人员。

### 02-架构设计.md

详细架构设计文档，涵盖 DDD 分层架构、模块划分、Git CLI 适配器模式、命令红线拦截器闭环、Guice 依赖注入绑定结构、异步任务体系等核心设计决策。

适合：需要深入理解架构的后端开发者、进行代码审查的架构师。

---

## 规约引用

- 全局核心规则：[../00-spec/shared/00-core-base.md](../00-spec/shared/00-core-base.md)
- 项目专属后端规范：[../00-spec/project/03-backend.md](../00-spec/project/03-backend.md)
- 项目专属 UI 规范：[../00-spec/project/05-frontend.md](../00-spec/project/05-frontend.md)