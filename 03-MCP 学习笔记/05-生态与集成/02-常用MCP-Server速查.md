# 常用 MCP Server 速查

> 定位：快速挑选合适的 Server——按用途分类 + 配置片段

---

## 1. 使用原则（先读这里）

引入任何 Server 前请先：

1. **明确权限边界**：这个 Server 需要哪些环境变量？读/写什么资源？
2. **审核来源**：优先官方 `modelcontextprotocol/servers`、知名组织、开源且有维护活跃度
3. **挑 Stdio 还是 HTTP**：本地敏感数据用 Stdio；跨机器共享用 HTTP
4. **最小权限**：能只读绝不写；能限定路径绝不全盘
5. **关注成本**：调用第三方 API 会产生费用，启用限流

---

## 2. 文件与内容

### 2.1 Filesystem（官方）

读写本地文件系统，支持受限根目录。

```jsonc
{
  "mcpServers": {
    "filesystem": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-filesystem", "/workspace"]
    }
  }
}
```

**能力**：read_file、write_file、list_directory、search_files、move_file…

**取舍提示**：是否挂载取决于宿主工具是否已内置文件操作。Claude Code / Cursor / Windsurf / Cline 等编程工具已内置 Read/Write/Edit/Glob/Grep，本地开发场景通常不需要再挂 Filesystem Server；而**通用聊天客户端**（Claude Desktop / ChatGPT Desktop / Open WebUI 等）原生没有文件能力，挂上才能让 LLM 读写本地文件。

### 2.2 Fetch（官方）

抓取 HTTP 内容并转换为 Markdown。

```jsonc
{
  "mcpServers": {
    "fetch": {
      "command": "uvx",
      "args": ["mcp-server-fetch"]
    }
  }
}
```

**能力**：fetch（传入 URL 获取网页正文）

---

## 3. 代码与版本控制

### 3.1 GitHub（官方）

```jsonc
{
  "mcpServers": {
    "github": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-github"],
      "env": { "GITHUB_PERSONAL_ACCESS_TOKEN": "${GITHUB_TOKEN}" }
    }
  }
}
```

**能力**：create_issue、search_issues、create_pull_request、get_file_contents、list_commits…

**权限建议**：PAT 使用细粒度 Token，仅授权必需仓库与 scope。

### 3.2 GitLab（官方）

```jsonc
{
  "mcpServers": {
    "gitlab": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-gitlab"],
      "env": {
        "GITLAB_PERSONAL_ACCESS_TOKEN": "${GITLAB_TOKEN}",
        "GITLAB_API_URL": "https://gitlab.com/api/v4"
      }
    }
  }
}
```

### 3.3 Git（官方）

对本地 Git 仓库的操作。

```jsonc
{
  "mcpServers": {
    "git": {
      "command": "uvx",
      "args": ["mcp-server-git", "--repository", "/workspace/repo"]
    }
  }
}
```

---

## 4. 数据库

### 4.1 PostgreSQL（官方）

只读查询。

```jsonc
{
  "mcpServers": {
    "postgres": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-postgres"],
      "env": { "DATABASE_URL": "${POSTGRES_DSN}" }
    }
  }
}
```

**能力**：query（只读 SELECT）、list_tables、describe_table

**建议**：DSN 使用只读账号。

### 4.2 SQLite（官方）

```jsonc
{
  "mcpServers": {
    "sqlite": {
      "command": "uvx",
      "args": ["mcp-server-sqlite", "--db-path", "/workspace/data.db"]
    }
  }
}
```

### 4.3 MongoDB / MySQL / Redis 等

社区维护为主，在 https://github.com/modelcontextprotocol/servers 与 `awesome-mcp-servers` 仓库查找。

---

## 5. 搜索与信息检索

### 5.1 Brave Search（官方）

```jsonc
{
  "mcpServers": {
    "brave": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-brave-search"],
      "env": { "BRAVE_API_KEY": "${BRAVE_API_KEY}" }
    }
  }
}
```

**能力**：brave_web_search、brave_local_search

### 5.2 Google Drive / Gmail（官方）

```jsonc
{
  "mcpServers": {
    "gdrive": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-gdrive"]
    }
  }
}
```

首次使用会走 OAuth 授权。

### 5.3 Everything（官方 sample）

聚合多种能力的测试/演示 Server，适合评估 MCP 功能。

```jsonc
{
  "mcpServers": {
    "everything": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-everything"]
    }
  }
}
```

---

## 6. 通讯与协作

### 6.1 Slack（官方）

```jsonc
{
  "mcpServers": {
    "slack": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-slack"],
      "env": {
        "SLACK_BOT_TOKEN": "${SLACK_BOT_TOKEN}",
        "SLACK_TEAM_ID": "${SLACK_TEAM_ID}"
      }
    }
  }
}
```

### 6.2 Linear / Jira / Notion

这些平台官方提供托管 MCP Server（远程 HTTP 端点），配置时使用 OAuth：

```jsonc
{
  "mcpServers": {
    "linear": {
      "type": "http",
      "url": "https://mcp.linear.app/mcp"
    }
  }
}
```

---

## 7. 浏览器自动化

### 7.1 Puppeteer（官方）

```jsonc
{
  "mcpServers": {
    "puppeteer": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-puppeteer"]
    }
  }
}
```

**能力**：导航、截图、表单填写、脚本执行。

### 7.2 Chrome DevTools MCP

更贴近浏览器原生调试能力（DOM、网络、性能）。Anthropic 和 Google 均有实现。

```jsonc
{
  "mcpServers": {
    "chrome-devtools": {
      "command": "npx",
      "args": ["-y", "@anthropic-ai/mcp-server-chrome-devtools"]
    }
  }
}
```

---

## 8. 时间与实用工具

### 8.1 Time（官方）

```jsonc
{
  "mcpServers": {
    "time": {
      "command": "uvx",
      "args": ["mcp-server-time", "--local-timezone=Asia/Shanghai"]
    }
  }
}
```

获取当前时间、时区转换。

### 8.2 Sequential Thinking（官方）

提供"结构化思考步骤"的 Tool，帮助复杂任务分步推理。

```jsonc
{
  "mcpServers": {
    "sequential-thinking": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-sequential-thinking"]
    }
  }
}
```

---

## 9. 云与基础设施

### 9.1 AWS / GCP / Azure

大厂均有社区或官方维护的 MCP Server，典型能力：

- AWS：S3、Lambda、CloudWatch、ECS
- GCP：GCS、BigQuery、Cloud Run、Logging
- Azure：Blob、Functions、Log Analytics

选择时优先官方或大型云厂商维护的版本，鉴权使用短期 STS Token 而非长期 Access Key。

### 9.2 Kubernetes

```jsonc
{
  "mcpServers": {
    "kubernetes": {
      "command": "uvx",
      "args": ["mcp-server-kubernetes"],
      "env": { "KUBECONFIG": "/home/user/.kube/config" }
    }
  }
}
```

**建议**：为 Agent 操作配置独立的只读/限定命名空间的 ServiceAccount。

---

## 10. 文档与知识库

### 10.1 Context7（文档查询）

查询主流开源库的最新官方文档。

```jsonc
{
  "mcpServers": {
    "context7": {
      "type": "http",
      "url": "https://mcp.context7.com/mcp"
    }
  }
}
```

### 10.2 内部文档

组织可以自建 MCP Server 接入内部 Wiki（Confluence、Notion、Outline）实现"AI 查内部知识"。

---

## 11. 组合推荐

### 11.1 日常开发最小组合

```jsonc
{
  "mcpServers": {
    "github":   { "command": "npx", "args": ["-y", "@modelcontextprotocol/server-github"], "env": { "GITHUB_PERSONAL_ACCESS_TOKEN": "${GITHUB_TOKEN}" } },
    "fetch":    { "command": "uvx", "args": ["mcp-server-fetch"] },
    "context7": { "type": "http", "url": "https://mcp.context7.com/mcp" }
  }
}
```

覆盖：代码仓库操作 + 网页抓取 + 文档查询。

### 11.2 数据分析组合

```jsonc
{
  "mcpServers": {
    "postgres": { "command": "npx", "args": ["-y", "@modelcontextprotocol/server-postgres"], "env": { "DATABASE_URL": "${PG_RO_DSN}" } },
    "sqlite":   { "command": "uvx", "args": ["mcp-server-sqlite", "--db-path", "/workspace/local.db"] },
    "fetch":    { "command": "uvx", "args": ["mcp-server-fetch"] }
  }
}
```

### 11.3 运维组合

```jsonc
{
  "mcpServers": {
    "kubernetes": { "command": "uvx", "args": ["mcp-server-kubernetes"], "env": { "KUBECONFIG": "${K8S_RO_KUBECONFIG}" } },
    "time":       { "command": "uvx", "args": ["mcp-server-time", "--local-timezone=Asia/Shanghai"] }
  }
}
```

---

## 12. 发现更多 Server

| 来源 | 链接 | 说明 |
|------|------|------|
| 官方仓库 | https://github.com/modelcontextprotocol/servers | Anthropic 维护的参考实现集 |
| Awesome 列表 | 搜索 `awesome-mcp-servers` | 社区精选清单 |
| MCP Marketplace | 各 Host 内置市场（Cursor、Claude Desktop 等） | UI 化挑选安装 |
| GitHub Topic | `topic:mcp-server` | 按主题搜索最新社区实现 |

---

## 13. 挑选 Server 的 Checklist

```
□ 来源可信（官方 / 知名组织 / 开源活跃）
□ README 清楚说明了能力、环境变量、限制
□ 权限最小化（读优先、路径/表限定）
□ 提供了清晰的配置示例
□ 版本有维护（最近 commit < 6 个月）
□ 我真的需要这个能力（不是"以防万一"挂载）
```

---

## 14. 小结

- **官方集合是第一站**，社区 awesome 列表是补充
- **本地敏感 → Stdio；跨机器共享 → HTTP**
- **组合最小化**：挂越多 Server，上下文压力越大，权限风险越高
- 敏感凭据用**环境变量 + 密钥管理**，绝不写进 `.mcp.json`
