# SOP-01: Agent Skill 目录规范

> 版本：1.0 | 适用范围：所有 Agent Skill 项目

---

## 1. 概述

本文档定义 Agent Skill 项目的标准目录结构与文件规范，确保团队内所有 Skill 具备一致的组织方式，便于接入、维护和复用。

---

## 2. 标准目录结构

```
{skill-name}/
├── README.md                  ✅ 必选 - Skill 说明文档
├── skill.json                 ✅ 必选 - Skill 元数据描述文件
├── CHANGELOG.md               ⭕ 推荐 - 版本变更记录
├── LICENSE                    ⭕ 推荐 - 开源许可证（对外发布时必选）
│
├── docs/                      ⭕ 推荐 - 扩展文档目录
│   ├── api.md                 ⭕ 推荐 - API 接口文档
│   ├── design.md              🔲 可选 - 设计说明文档
│   └── examples/              ⭕ 推荐 - 使用示例目录
│       ├── basic-example.md
│       └── advanced-example.md
│
├── src/                       ✅ 必选 - 源代码目录
│   └── ...（语言相关，见 SOP-02 / SOP-03 / SOP-05）
│
├── tests/                     ✅ 必选 - 测试代码目录
│   ├── unit/                  ⭕ 推荐 - 单元测试
│   └── integration/           ⭕ 推荐 - 集成测试
│
├── config/                    ⭕ 推荐 - 配置文件目录
│   ├── application.yml        ⭕ 推荐 - 应用配置
│   ├── application-dev.yml    ⭕ 推荐 - 开发环境配置
│   └── application-prod.yml   ⭕ 推荐 - 生产环境配置
│
├── scripts/                   🔲 可选 - 辅助脚本目录
│   ├── build.sh               🔲 可选 - 构建脚本
│   ├── deploy.sh              🔲 可选 - 部署脚本
│   └── test.sh                🔲 可选 - 测试脚本
│
├── docker/                    ⭕ 推荐 - 容器化相关文件
│   ├── Dockerfile             ⭕ 推荐 - 镜像构建文件
│   └── docker-compose.yml     🔲 可选 - 本地开发编排文件
│
└── .env.example               ✅ 必选 - 环境变量示例文件（不含真实密钥）
```

图例说明：
- ✅ **必选**：项目必须包含，缺少将无法正常接入
- ⭕ **推荐**：强烈建议包含，有助于维护和协作
- 🔲 **可选**：视项目需求决定是否添加

---

## 3. 核心文件说明

### 3.1 README.md（必选）

Skill 的入口说明文档，必须包含以下章节：

```markdown
# {Skill 名称}

## 简介
简洁描述该 Skill 的功能和用途（1-3句话）。

## 功能特性
- 功能点1
- 功能点2

## 快速开始
### 前置依赖
### 安装步骤
### 运行方式

## API 接口
（简要说明或链接到 docs/api.md）

## 配置说明
（关键配置项说明）

## 开发指南
（本地开发、测试方法）

## 版本历史
（或链接到 CHANGELOG.md）
```

### 3.2 skill.json（必选）

Skill 的元数据描述文件，Agent 平台通过此文件发现和注册 Skill：

```jsonc
{
  "name": "skill-name",
  "version": "1.0.0",
  "displayName": "Skill 显示名称",
  "description": "Skill 的功能描述，清晰说明该 Skill 能做什么",
  "author": "作者/团队名称",
  "category": "工具类别（如：数据处理、外部集成、业务逻辑等）",
  "tags": ["标签1", "标签2"],
  "language": "java | python | nodejs",
  "entrypoint": {
    "type": "http | function | mcp",
    "url": "http://localhost:8080",
    "healthCheck": "/actuator/health"
  },
  "capabilities": [
    {
      "name": "capability-name",
      "description": "该能力的描述",
      "inputSchema": {
        "type": "object",
        "properties": {
          "param1": { "type": "string", "description": "参数说明" }
        },
        "required": ["param1"]
      },
      "outputSchema": {
        "type": "object",
        "properties": {
          "result": { "type": "string", "description": "返回值说明" }
        }
      }
    }
  ],
  "dependencies": {
    "externalServices": ["服务A", "服务B"],
    "environmentVariables": ["ENV_VAR_1", "ENV_VAR_2"]
  },
  "deployment": {
    "minMemory": "256Mi",
    "recommendedMemory": "512Mi",
    "startupMode": "always-on | on-demand"
  }
}
```

### 3.3 .env.example（必选）

列出所有需要配置的环境变量，**绝不包含真实密钥**：

```bash
# 服务配置
SERVER_PORT=8080
SERVER_ENV=development

# 数据库配置
DB_HOST=localhost
DB_PORT=5432
DB_NAME=skill_db
DB_USER=your_db_user
DB_PASSWORD=your_db_password_here

# 外部 API 配置
EXTERNAL_API_KEY=your_api_key_here
EXTERNAL_API_URL=https://api.example.com

# Agent 平台配置
AGENT_PLATFORM_URL=http://localhost:9000
SKILL_REGISTER_TOKEN=your_register_token_here
```

### 3.4 docs/api.md（推荐）

详细的 API 接口文档，建议包含：
- 接口列表及说明
- 请求/响应示例（JSON 格式）
- 错误码说明
- 认证方式说明

---

## 4. 命名规范

| 项目 | 规范 | 示例 |
|------|------|------|
| 目录名（Skill 根目录） | `kebab-case`，体现功能域 | `weather-query`、`doc-summarizer` |
| 源码文件 | 遵循对应语言规范 | Java: `PascalCase`，Python: `snake_case` |
| 配置文件 | `kebab-case` 或语言框架默认 | `application.yml` |
| 环境变量 | `UPPER_SNAKE_CASE` | `API_KEY`、`DB_HOST` |
| Skill 版本 | 遵循 [SemVer](https://semver.org/lang/zh-CN/) | `1.0.0`、`2.1.3` |

---

## 5. .gitignore 必须排除项

```gitignore
# 环境变量真实配置
.env
.env.local
.env.*.local

# 编译产物
target/
build/
dist/
__pycache__/
*.pyc

# IDE 文件
.idea/
.vscode/
*.iml

# 日志
logs/
*.log

# 密钥/证书
*.key
*.pem
*.p12
secrets/
```

---

## 6. 版本管理规范

- **主版本号（Major）**：不兼容的 API 变更
- **次版本号（Minor）**：向后兼容的功能新增
- **修订号（Patch）**：向后兼容的问题修复
- `skill.json` 中的 `version` 字段必须与 Git Tag 保持一致
- 每次发布前必须更新 `CHANGELOG.md`

---

## 7. 检查清单

在提交或发布 Skill 前，确认以下项目：

- [ ] `README.md` 包含完整的快速开始指南
- [ ] `skill.json` 中所有字段填写完整且准确
- [ ] `.env.example` 已列出所有必需的环境变量
- [ ] `.env` 未被提交到版本库
- [ ] `tests/` 目录包含基本测试用例
- [ ] API 文档与实际接口一致
- [ ] `Dockerfile` 可以成功构建运行
- [ ] 健康检查接口可正常响应
