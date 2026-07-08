# MCP Server 在 AI 工具中的挂载

> 版本：2.0 | 定位：协议中立——通用挂载模式 + 各工具的具体配置

MCP（Model Context Protocol）由 Anthropic 提出，但早已被 Cursor / Windsurf / Cline / Continue / Aider / Zed / Open WebUI 等多家工具采纳。本文按"通用模式 → 具体工具"的顺序展开，避免与单一工具绑定。

---

## 1. 通用挂载模式

### 1.1 通用配置字段

不同工具的配置文件路径不同，但**字段几乎完全一致**（都源自 MCP 规范的 reference implementation）：

```jsonc
{
  "mcpServers": {
    "<server-name>": {
      // ── 传输方式三选一 ──
      // (a) Stdio：本地子进程
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-filesystem", "/workspace"],
      "env": { "LOG_LEVEL": "INFO" },
      "cwd": "/path/to/workdir",

      // (b) Streamable HTTP：远程
      "type": "http",
      "url": "https://mcp.example.com/mcp",
      "headers": { "Authorization": "Bearer ${TOKEN}" },

      // (c) SSE：旧版远程
      "type": "sse",
      "url": "https://legacy.example.com/sse"
    }
  }
}
```

**环境变量插值**：多数工具支持 `${VAR}` 从启动时的 shell 环境读取，避免把密钥写进配置文件。

### 1.2 配置作用域（通用三层模型）

几乎所有支持 MCP 的工具都遵循"用户级 / 项目级 / 本地覆盖"三层：

| 层级 | 适用 | 优先级 |
|------|------|--------|
| **用户级** | 个人跨项目通用 Server（filesystem、time、github 等） | 低 |
| **项目级** | 团队共享、跟随仓库提交的 Server | 中 |
| **本地覆盖** | 个人在该项目的临时覆盖（如本地数据库 DSN） | 高 |

具体路径见 §3 各工具表格。

### 1.3 传输方式选择

| 场景 | 选 | 原因 |
|------|-----|------|
| 本地敏感数据（数据库、文件系统） | Stdio | 不出本机 |
| 跨机器共享 / 团队公共服务 | HTTP | 一处部署，多人连接 |
| 老平台 / 流式优先 | SSE | 兼容性兜底 |

---

## 2. 配置示例（按传输类型）

### 2.1 Stdio Server

```jsonc
{
  "mcpServers": {
    "filesystem": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-filesystem", "/workspace"]
    },
    "my-python-server": {
      "command": "uvx",
      "args": ["my-mcp-server"],
      "env": {
        "LOG_LEVEL": "INFO",
        "API_KEY": "${MY_API_KEY}"
      },
      "cwd": "/path/to/workdir"
    }
  }
}
```

### 2.2 Streamable HTTP Server

```jsonc
{
  "mcpServers": {
    "remote-api": {
      "type": "http",
      "url": "https://mcp.example.com/mcp",
      "headers": {
        "Authorization": "Bearer ${REMOTE_TOKEN}"
      }
    }
  }
}
```

### 2.3 SSE Server（旧版）

```jsonc
{
  "mcpServers": {
    "legacy-sse": {
      "type": "sse",
      "url": "https://legacy.example.com/sse",
      "headers": { "X-API-Key": "${LEGACY_KEY}" }
    }
  }
}
```

---

## 3. 各工具的挂载位置

### 3.1 Claude Code

| 作用域 | 位置 |
|--------|------|
| 用户级 | `~/.claude.json` 里的 `mcpServers` |
| 项目级 | 项目根的 `.mcp.json` |
| 本地覆盖 | `~/.claude.json` 项目段的 `mcpServers` |

CLI 管理：

```bash
claude mcp list                       # 查看已配置
claude mcp add <name> --command ...   # 新增
claude mcp remove <name>              # 删除
claude mcp add-json < server-config.json  # 批量导入
```

会话内 `/mcp` 查看连接状态、Tool / Resource / Prompt 列表。Tool 命名空间：`mcp__<server>__<tool>`。

### 3.2 Cursor

| 作用域 | 位置 |
|--------|------|
| 用户级 | `~/.cursor/mcp.json` |
| 项目级 | `.cursor/mcp.json` |

Settings → MCP & Integrations 面板可视化挂载。Cursor 把 MCP Tool 暴露给 Composer / Agent / Chat 三种模式。

### 3.3 Windsurf

| 作用域 | 位置 |
|--------|------|
| 用户级 | `~/.codeium/windsurf/mcp_config.json` |

Settings → Cascade → MCP Servers，UI 同样可视化。Cascade Plan 模式可调用 MCP Tool。

### 3.4 Cline / Roo Code（VS Code）

| 作用域 | 位置 |
|--------|------|
| 全局 | VS Code 扩展设置 → MCP Servers（背后是 JSON 配置） |
| 项目级 | `.cline/mcp.json` 或扩展面板"Add Server" |

Plan / Act 模式下都能调用 MCP Tool。

### 3.5 Continue

| 作用域 | 位置 |
|--------|------|
| 用户级 | `~/.continue/config.yaml` 的 `mcpServers` 块 |
| 项目级 | `.continue/config.yaml` |

YAML 格式（不是 JSON），字段与 JSON 版本一一对应。

### 3.6 Aider

通过 `--mcp-server` 命令行参数或配置文件 `.aider.conf.yml` 中的 `mcp-servers` 字段挂载（版本演进中，参数名以官方文档为准）。Architect 与 Editor 两个角色都可访问挂载的 Tool。

### 3.7 Zed

`~/.config/zed/settings.json` 的 `context_servers` 块。Zed 把 MCP Server 称为 "Context Server"。

### 3.8 Claude Desktop / 通用桌面客户端

`<user-config>/Claude/claude_desktop_config.json` 的 `mcpServers` 块。许多第三方桌面 Host（ChatBox、Cherry Studio、Open WebUI 等）也复用了这套字段。

### 3.9 ChatGPT / Custom GPT / 网页大模型

原生不支持 MCP（截至 2026 初）。变通：

- 写一个 OpenAI Function-Calling 适配层，把 MCP Tool 翻译成 Function
- 或直接复制 MCP Server 提供的等价 HTTP API 注册为 Custom GPT 的 Action

### 3.10 自研 Agent 框架

LangChain、LangGraph、AutoGen、CrewAI 等都有 `mcp` 适配器包。基本用法：

```python
from mcp import ClientSession  # 伪代码示意，具体 import 以官方文档为准
session = await ClientSession.connect_stdio(command, args)
tools = await session.list_tools()
# 把 tools 注册到自家 Agent 框架的 tool registry
```

---

## 4. 会话内发现与使用

### 4.1 查看已连接 Server

| 工具 | 命令 / 入口 |
|------|-----------|
| Claude Code | 输入 `/mcp` |
| Cursor / Windsurf | Settings 面板的 MCP 列表 |
| Cline / Roo Code | 扩展面板的 "MCP Servers" 视图 |
| Continue | `/mcp` 或扩展面板 |

显示内容大同小异：连接状态（connected / connecting / error）、协议版本、已发现的 Tool / Resource / Prompt 数量、最近错误。

### 4.2 Tool 命名规则

多数 Host 把 MCP Tool 暴露为：

```
mcp__<server_name>__<tool_name>     # Claude Code 风格
<server_name>:<tool_name>           # 其他工具常见风格
```

这个名字是 LLM 看到的 Tool 名，用于：
- 权限配置的 allow / deny 列表
- 审计日志
- Hook 匹配

### 4.3 Resource 引用

支持 Resource 的工具通常用 `@mention` 引用（语法以工具为准）：

```
@<server>://<path>
```

被引用的 Resource 内容会注入上下文。

### 4.4 Prompt 触发

MCP Server 提供的 Prompt 通常注册为斜杠命令（`/<prompt-name>`），调用 `prompts/get` 渲染模板后提交给 LLM。

---

## 5. 权限管控

### 5.1 通用模式

各 Host 的权限模型不一样，但都包含三种基本模式：

| 模式 | 行为 |
|------|------|
| **询问** | 未知 Tool 每次弹窗确认（默认） |
| **白名单** | 列表内的 Tool 自动允许 |
| **黑名单** | 列表内的 Tool 一律拒绝 |

危险动作（如删除资源、写入敏感路径）应当列入黑名单。

### 5.2 Claude Code 示例

`~/.claude/settings.json` 或项目 `.claude/settings.json`：

```jsonc
{
  "permissions": {
    "allow": [
      "mcp__filesystem__read_file",
      "mcp__github__search_issues",
      "mcp__postgres__run_query(SELECT *)"
    ],
    "deny": [
      "mcp__github__delete_repository",
      "mcp__filesystem__write_file(~/.ssh/**)"
    ]
  }
}
```

要点：deny > allow；项目 settings 可覆盖用户 settings。

### 5.3 Cursor / Windsurf / Cline 示例

UI 化按 Server / Tool 勾选启用；首次调用同样会弹窗确认。

### 5.4 自研 Host

实现 `tools/call` handler 时，包一层 policy 引擎（OPA、自写规则）按 Server + Tool + 参数模式判断。

---

## 6. 调试挂载的 Server

### 6.1 查看连接状态

每个工具都有"列出 Server"的入口（§4.1）。重点看：
- 连接状态
- 协议版本
- Tool / Resource / Prompt 数量
- 最近错误日志

### 6.2 查看 Server 日志

Stdio Server 的 stderr 通常被 Host 重定向到日志文件：

| 工具 | 日志位置 |
|------|---------|
| Claude Code | `~/.claude/logs/mcp-<server-name>.log` |
| Cursor | `Help → Toggle Developer Tools → Console` 或 `~/Library/Logs/Cursor/` |
| Cline / Roo Code | VS Code Output 面板的扩展频道 |
| Continue | `~/.continue/logs/` |

### 6.3 强制重连

修改配置后多数工具需要：

- Claude Code：`claude mcp restart <server-name>` 或退出重启
- Cursor / Windsurf / Cline：UI 上点击刷新或重启 Host

### 6.4 Inspector 独立调试

如果 Server 在某个 Host 里不工作，先用官方 Inspector 隔离测试：

```bash
npx @modelcontextprotocol/inspector <your-command>
```

Inspector 通过，再排查 Host 侧配置 / 权限 / 路径问题。

---

## 7. 与上层能力（Skill / Hook / Subagent）协同

不同工具上层能力各异，但 MCP 都是底层"工具供应商"。下面是几种常见组合：

### 7.1 Skill 引用 MCP Tool（Claude Code 示例）

```markdown
---
name: db-analyze
description: 分析数据库性能
allowed-tools:
  - mcp__postgres__run_query
  - mcp__postgres__explain
---

使用 mcp__postgres__run_query 找慢查询，对 TOP 5 用 mcp__postgres__explain 看执行计划，产出优化建议。
```

Cursor / Windsurf 的等价物是"Rules 文件 + 在 Composer 里指定 Server"。

### 7.2 Hook 拦截 MCP 调用（Claude Code 示例）

```jsonc
// .claude/settings.json
{
  "hooks": {
    "preToolUse": [
      {
        "matcher": "mcp__postgres__run_query",
        "hooks": [
          { "type": "command", "command": "validate-sql.sh" }
        ]
      }
    ]
  }
}
```

其他工具尚无标准 Hook 机制，可在 MCP Server 内部做参数校验或起代理 Server 拦截。

### 7.3 Subagent 白名单化 MCP Tool

Claude Code 的 subagent / Cursor 的 sub-task / 自研框架的子 Agent 都可以收紧工具白名单，让子 Agent 天然降权。

---

## 8. 环境变量与密钥管理

### 8.1 不要把密钥写进配置文件

项目级配置（`.mcp.json` / `.cursor/mcp.json` 等）通常跟随仓库提交，禁止包含敏感信息：

```jsonc
// ✅ 用环境变量占位符
{
  "mcpServers": {
    "postgres": {
      "command": "uvx",
      "args": ["mcp-postgres"],
      "env": { "DATABASE_URL": "${DATABASE_URL}" }
    }
  }
}
```

### 8.2 本地密钥管理

- `direnv` / `.env`（加到 `.gitignore`）
- 系统 keychain（macOS Keychain、Windows Credential Manager、`secret-tool`）
- 1Password CLI：`op run -- <your-tool>`

### 8.3 团队共享

- 公共服务用 OAuth 而非长期 Token
- 个人 Token 各自本地配置（本地覆盖层）
- 敏感配置走密钥管理服务，不入仓库

---

## 9. 性能与稳定性

### 9.1 启动时间

Stdio Server 在 Host 启动时拉起，过多会延长启动：

- 只挂载当前项目真正需要的 Server
- 慢启动 Server 改用 HTTP 传输（一次部署、按需连）

### 9.2 Tool 数量

上下文预算有限，Tool 说明会吃 Token：

- 关闭不用的 Server
- 避免同功能重复挂载
- 经验值：Server 数量 ≤ 5，Tool 总数 ≤ 50

### 9.3 Resource 订阅

若 Server 支持 `subscribe`，变更会触发通知注入上下文。无意义的高频订阅会污染对话，按需开关。

---

## 10. 故障排查速查

| 现象 | 排查 |
|------|------|
| Server 状态为 error | 查 §6.2 日志 |
| Tool 不出现 | `initialize` 时 capabilities 未声明 tools / `list_tools` 抛异常 |
| Tool 调用卡住 | Server 内部阻塞，检查超时与异步调用 |
| 权限弹窗太频繁 | 在 settings 里配置 allow 列表 |
| 环境变量未生效 | 确认占位符拼写与 Host 启动时的 shell env |
| Windows 下 command 未找到 | 用绝对路径或确认 PATH |

---

## 11. 小结

- 协议中立：MCP 配置字段在多数工具间高度一致，**配置一次、跨工具复用**
- 三层作用域（用户 / 项目 / 本地）覆盖个人、团队、本地覆盖三种场景
- Tool 命名空间贯穿权限、日志、Hook，是排查的第一锚点
- 敏感凭据用环境变量占位符，不要提交仓库
- Skill / Rules + 子 Agent + Hook 三者组合使用 MCP 效果最佳
