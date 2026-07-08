# 推荐 Skill 工具与资源

> 整理自社区与官方推荐，涵盖安装命令、核心功能和适用场景。

---

## 目录

- [Skill 和 Plugin 的区别](#skill-和-plugin-的区别)
- [官方出品](#官方出品)
  - [01 Superpowers](#01-superpowers)
  - [02 Code Review](#02-code-review)
  - [03 Code Simplifier](#03-code-simplifier)
  - [04 Frontend Design](#04-frontend-design)
  - [05 UI UX Pro Max](#05-ui-ux-pro-max)
  - [06 办公四件套](#06-办公四件套docx--xlsx--pdf--pptx)
  - [07 Webapp Testing](#07-webapp-testing)
  - [08 MCP Builder](#08-mcp-builder)
  - [09 Skill Creator](#09-skill-creator)
- [社区出品](#社区出品)
  - [10 Planning with Files](#10-planning-with-files)
  - [11 Ralph Loop](#11-ralph-loop)
  - [12 Web Access](#12-web-access)
  - [13 Claude-mem](#13-claude-mem)
  - [14 Find Skills](#14-find-skills)
  - [15 Remotion](#15-remotion)
- [使用建议](#使用建议)
- [相关资源链接](#相关资源链接)

---

## Skill 和 Plugin 的区别

- **Skill**：一个包含 `SKILL.md` 的文件夹，用于教 Claude 完成某类任务。
- **Plugin**：更完整的形式，可打包命令、SubAgent、Hook 和 MCP 服务器。
- 对使用者而言两者差异不大，下文不做区分。

---

## 官方出品

### 01 Superpowers

如果只能装一个，首选这个。

- **功能**：打包了 20 多个可组合的 Skill，覆盖软件开发完整流程（头脑风暴、TDD、代码审查、Git 提交等）。
- **工作流程**：需求确认（头脑风暴）→ 制定设计规范 → 制定实现计划 → 自动分发子代理编写代码 → 自动代码审查 → 最终合并，覆盖软件开发生命周期主要环节。
- **高频用法**：
  - `brainstorming`：先探索方案、讨论设计决策，再生成本地设计文档，避免后期返工。
  - `TDD 工作流`：强制 Claude 先写测试再写实现，直到测试全绿。
- **注意**：适合复杂、涉及多文件的大型开发任务，对于简单 Bug 修复可能过于"重型"。
- **安装命令**：
  ```bash
  claude plugin install superpowers
  ```
- **GitHub**：https://github.com/obra/superpowers

---

### 02 Code Review

官方设计最精巧的代码审查工具。

- **功能**：启动多个 Agent 并行审查 PR，分别检查逻辑正确性、安全漏洞、性能问题、代码风格，并通过置信度分数过滤假阳性反馈。
- **使用建议**：适合在提交代码（如提 PR）前进行自查，可能存在误报，需要人工进行最终判断。
- **注意**：大 PR 会消耗大量 token，建议拆分后再审查。
- **安装命令**：
  ```bash
  claude plugin install code-review
  ```
- **GitHub**：https://github.com/anthropics/claude-plugins-official/tree/main/plugins/code-review

---

### 03 Code Simplifier

自动简化代码，不改变功能，只做精简。

- **功能**：聚焦最近修改的代码，检查重复逻辑、多余中间变量、可合并的条件分支，合并为通用函数。
- **安装命令**：
  ```bash
  claude plugin install code-simplifier
  ```
- **GitHub**：https://github.com/anthropics/claude-plugins-official/tree/main/plugins/code-simplifier

---

### 04 Frontend Design

解决 AI 生成前端界面"AI 味"过重的问题。

- **功能**：在编码前先确立美学方向（如极简主义），禁止使用常见默认字体和配色，最终在排版、留白、字体和动效上更具设计感。
- **核心优势**：解决原生 Claude 生成 UI "能用但丑"、配色单一的问题，节省与设计师的沟通成本。
- **GitHub**：https://github.com/anthropics/skills/tree/main/skills/frontend-design

---

### 05 UI UX Pro Max

更进一步的 UI 设计增强工具，比官方 Frontend Design 做得更彻底。

- **背景**：原生 Claude 写前端页面，出来的东西大都长一个样——紫色渐变背景、圆角卡片、居中布局，也就是典型的「AI 审美」。官方 Frontend Design Skill 能改善一些，但 UI UX Pro Max 做得更彻底。
- **功能**：内置 67 种 UI 风格和 161 套行业配色方案，可根据项目类型自动推荐设计系统，从配色、排版到交互模式一步到位。
- **实测效果**：制作 SaaS 后台 dashboard，选择 Bento Grid 风格，效果远优于 Claude 默认样式，更贴近人工设计感。
- **技术栈支持**：支持多端前端技术栈，不局限于 Web——React / Vue / Svelte / SwiftUI / Flutter。
- **安装命令**：
  ```bash
  claude plugin marketplace add nextlevelbuilder/ui-ux-pro-max
  claude plugin install ui-ux-pro-max@ui-ux-pro-max-skill
  ```
- **GitHub**：https://github.com/nextlevelbuilder/ui-ux-pro-max-skill

---

### 06 办公四件套（docx / xlsx / pdf / pptx）

几乎人人必备的文档处理工具。

- **功能**：Anthropic 官方出品，为处理 Word、Excel、PDF 和 PowerPoint 文档提供标准化的代码模板和流程，生成格式规范、专业美观的文档。
  - **PDF**：常用于格式转换、内容提取。
  - **DOCX**：Word 文档处理。
  - **PPTX**：可辅助生成演示文稿框架，细节（如字号、颜色）可能需要人工调整。
  - **XLSX**：处理 Excel 表格，相对省心。
- **安装命令**（以 pptx 为例）：
  ```bash
  claude plugin marketplace add anthropics/skills
  claude plugin install document-skills@anthropic-agent-skill
  ```
- **GitHub**：
  - PDF: https://github.com/anthropics/skills/tree/main/pdf
  - DOCX: https://github.com/anthropics/skills/tree/main/docx
  - PPTX: https://github.com/anthropics/skills/tree/main/pptx
  - XLSX: https://github.com/anthropics/skills/tree/main/xlsx

---

### 07 Webapp Testing

自动化前端 E2E 测试，解放双手。

- **功能**：根据测试场景自动生成 Playwright 脚本，启动浏览器、运行测试、截屏并自主调试。
- **搭配**：与 Frontend Design / UI UX Pro Max 配合使用，流程更顺畅。
- **安装命令**：
  ```bash
  claude plugin marketplace add anthropics/skills
  claude plugin install example-skills@anthropic-agent-skill
  ```
- **GitHub**：https://github.com/anthropics/skills/tree/main/skills/webapp-testing

---

### 08 MCP Builder

降低 MCP Server 开发门槛。

- **功能**：将构建过程拆分为 4 个阶段（理解 API、设计工具接口、实现、测试），引导 Claude 逐步完成，主动处理边界情况（如 rate limiting、token 过期）。
- **安装命令**：
  ```bash
  claude plugin marketplace add anthropics/skills
  claude plugin install example-skills@anthropic-agent-skill
  ```
- **GitHub**：https://github.com/anthropics/skills/tree/main/skills/mcp-builder

---

### 09 Skill Creator

元技能：自己创建新 Skill。

- **功能**：Anthropic 官方出品，提供从开发到验证的完整流程支持，用于**创建、测试、评估和基准测试**自定义 Skill，满足个性化需求。
- **适用场景**：当您有经常重复的特定工作流时，可将其封装成自定义 Skill 并使用此工具进行开发和测试。
- **安装命令**：
  ```bash
  claude plugin install skill-creator
  ```
- **GitHub**：https://github.com/anthropics/claude-plugins-official/tree/main/plugins/skill-creator

---

## 社区出品

### 10 Planning with Files

解决 Plan Mode 上下文丢失问题，让长任务状态持久化。

- **问题背景**：Claude Code 自带的 Plan Mode 存在一个核心痛点——规划内容仅存储在对话上下文中，当上下文被压缩时，规划会丢失。长任务执行到一半，Claude 会遗忘当前进度，从头开始重复工作。
- **核心原理**：将规划、进度和知识持久化写入 Markdown 文件，避免上下文压缩后丢失状态：
  1. Claude 开始任务前，先创建计划文件
  2. 每完成一个步骤，自动更新进度
  3. 遇到关键/有用信息，记录到知识文件
  4. 文件存储在本地磁盘，不会因上下文压缩丢失，随时可恢复任务状态
- **设计来源**：核心思路来自 Manus——Manus 在复杂任务上表现优异的核心原因之一，就是将中间状态做了持久化存储。Planning with Files 是该思路的社区开源实现。
- **安装命令**：
  ```bash
  claude plugin marketplace add OthmanAdi/planning-with-files
  claude plugin install planning-with-files
  ```
- **GitHub**：https://github.com/OthmanAdi/planning-with-files

---

### 11 Ralph Loop

强制 Claude 完成任务，拒绝"半成品"。

- **原理**：通过 Stop Hook 拦截 Claude 的退出，循环喂回任务直到满足完成条件。
- **关键技巧**：完成条件要写得极其具体（如"所有 CRUD 端点可用，测试覆盖率 >80%，README 包含 API 文档，完成后输出 COMPLETE"）。
- **参考示例**：https://awesomeclaude.ai/ralph-wiggum

---

### 12 Web Access

目前通用联网功能最强的 Skill。

- **功能**：通过 Chrome DevTools Protocol 连接本地 Chrome 浏览器，可携带用户登录状态访问需要登录的网站（如小红书、B站），并将网页内容转为干净的 Markdown 以节省 Token。
- **核心优势**：
  - 集成 WebSearch、curl、Jina、CDP 浏览器操作等多种工具，根据场景自动选择。
  - 支持"并行分治"，可同时调度多个子代理并行调研不同网站，提升效率。
- **特点**：自动积累对每个网站的访问经验（高效选择器），越用越快。
- **GitHub**：https://github.com/eze-is/web-access

---

### 13 Claude-mem

为 Claude Code 添加长期记忆功能。

- **功能**：自动压缩并存储每次对话的关键信息，新对话时通过三层检索机制智能注入相关上下文，核心原则是节省 Token。
- **特点**：提供本地 Web 界面管理记忆，支持 `<private>` 标签保护隐私。
- **GitHub**：https://github.com/thedotmack/claude-mem

---

### 14 Find Skills

Skills 的"搜索引擎"——元技能，用于发现和安装其他 Skills。

- **功能**：帮助用户在 Claude Code 内部**搜索、发现并直接安装**其他 Skills。可以通过描述需求（如"我想找一个能做 xxx 的 skill"）来找到相关工具。
- **核心优势**：无需手动在 GitHub 等平台逐个翻找，提供了在 Claude Code 内部发现和管理 Skills 的便捷入口。
- **GitHub**：https://github.com/vercel-labs/agent-skills/tree/main/skills/find-skills

---

### 15 Remotion

用 React 代码编写视频。

- **功能**：基于 Remotion 框架，允许开发者使用 React 组件定义视频的每一帧画面，控制动画、字幕和音频。
- **适用场景**：适合程序员快速制作产品演示、说明短片等简单视频内容。代码定义时间线的方式对程序员更易上手，但无法替代 Premiere 等专业剪辑软件。
- **GitHub**：https://github.com/remotion-dev/remotion

---

## 使用建议

- **精选而非堆砌**：Skill 不在多，在合适。装太多会互相影响性能、占用上下文空间。
- **项目级管理**：仅跟项目相关的 Skill 放入项目 Git，方便团队共享并节省全局上下文。
- **自建优先**：最强大的 Skill 永远是为自己特定工作流量身定制的那个，善用 Skill Creator。

---

## 相关资源链接

| 资源 | 地址 |
|------|------|
| Anthropic 官方 Skills 仓库 | https://github.com/anthropics/skills |
| Anthropic 官方 Plugins 仓库 | https://github.com/anthropics/claude-plugins-official |
| Awesome Claude Skills 社区列表 | https://github.com/travisvn/awesome-claude-skills |
| Claude Code Skills 文档 | https://code.claude.com/docs/en/skills |
| Skills 市场 | https://skillsmp.com/ |
