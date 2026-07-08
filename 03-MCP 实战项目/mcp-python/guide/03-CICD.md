# Python MCP Server · CI/CD · 构建与分发

> 拆分系列：[环境准备](01-环境准备.md) · [源代码结构](02-源代码结构.md) · **CI/CD · 构建与分发** · [使用方式](04-使用方式.md)

---

## 1. 打包与发布

### 1.1 pyproject.toml 关键字段

```toml
[project]
name = "my-mcp-server"
version = "0.1.0"
requires-python = ">=3.10"
dependencies = ["mcp>=1.0.0", "httpx", "pydantic>=2"]

[project.scripts]
my-mcp-server = "my_mcp_server.__main__:main"
```

打包发布后用户可：

```bash
# 任意目录（不依赖项目根）
pip install my-mcp-server

# 验证：应打印版本号或 help 文本
my-mcp-server --help
# 在 .mcp.json 里直接写 command: "my-mcp-server"
```

### 1.2 uvx 一键运行

发布到 PyPI 后，用户无需本地安装：

```jsonc
{
  "mcpServers": {
    "my-server": {
      "command": "uvx",
      "args": ["my-mcp-server"]
    }
  }
}
```

---

## 2. Docker 镜像

远程部署优先选 Docker 镜像：多阶段构建 + 小基础镜像，启动即用。

### 2.1 Dockerfile

```dockerfile
# syntax=docker/dockerfile:1
FROM python:3.12-slim AS builder
WORKDIR /src
RUN pip install --no-cache-dir uv
COPY pyproject.toml ./
RUN uv sync --no-install-project
COPY src ./src
RUN uv sync --frozen

FROM python:3.12-slim
WORKDIR /app
COPY --from=builder /src/.venv /app/.venv
COPY --from=builder /src/src /app/src
ENV PATH="/app/.venv/bin:$PATH"
EXPOSE 8000
# Stdio 模式直接改 CMD 为 ["python", "-m", "<your_module>"]
CMD ["python", "-m", "<your_module>", "--transport", "http", "--host", "0.0.0.0"]
```

**设计要点**：
- 多阶段构建：构建层含完整工具链，最终层只带 runtime + artifact，体积/攻击面最小
- stdio 场景罕见容器化（Client 无法跨容器拉进程），HTTP 是默认选择
- 端口 `EXPOSE` 与启动时 `--port` 参数保持一致；云平台探活走 `POST /mcp` initialize

### 2.2 构建 & 运行

```bash
# 必须在项目根目录（含 Dockerfile）下
cd /path/to/my-mcp-server

docker build -t my-mcp-server:0.1.0 .
docker run --rm -d --name my-mcp-server -p 8000:8000 my-mcp-server:0.1.0

# 验证容器正常运行
docker ps | grep my-mcp-server            # STATUS 应为 Up

# 验证 MCP 端点可用（任意目录执行）
curl -sS -X POST http://localhost:8000/mcp \
     -H "Content-Type: application/json" \
     -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}'
# 正常会返回含 `"result"` 字段的 JSON-RPC 响应

# Client 侧用 http URL 挂载（详见 04-使用方式.md）
#   "url": "http://localhost:8000/mcp"
```

### 2.3 docker compose（多服务协同时）

```yaml
# docker-compose.yml
services:
  my-mcp-server:
    build: .
    ports:
      - "8000:8000"
    environment:
      - LOG_LEVEL=INFO
    restart: unless-stopped
```


## 3. CI 流水线样板（GitHub Actions）

```yaml
# .github/workflows/ci.yml
name: ci
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: astral-sh/setup-uv@v4
      - run: uv sync --all-extras --dev
      - run: uv run pytest
```

**扩展建议**：
- 加 `docker/build-push-action@v5` 步骤在 tag 推送时自动 build + push 到 GHCR / 公司内部 registry
- 分支保护规则要求 CI 绿灯才能合并主干
- 安全：`dependabot.yml` 定时检查依赖；`CodeQL` 扫描代码
