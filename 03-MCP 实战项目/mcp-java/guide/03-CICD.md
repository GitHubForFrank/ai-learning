# Java MCP Server · CI/CD · 构建与分发

> 拆分系列：[环境准备](01-环境准备.md) · [源代码结构](02-源代码结构.md) · **CI/CD · 构建与分发** · [使用方式](04-使用方式.md)

---

## 1. 打包与发布

### 9.1 可执行 Jar

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

```bash
./mvnw clean package
java -jar target/my-mcp-server-0.1.0.jar
```

### 9.2 容器化

`Dockerfile`：

```dockerfile
FROM eclipse-temurin:17-jre
COPY target/my-mcp-server-0.1.0.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

适合 HTTP 模式的远程部署。**Stdio 模式不适合容器化**，Client 无法通过子进程拉起。

### 9.3 GraalVM Native Image

Spring Boot 3 + Spring AI 支持 GraalVM AOT，生成单二进制：

```bash
./mvnw -Pnative native:compile
./target/my-mcp-server
```

**好处**：启动时间从数秒降到数十毫秒，解决了 Java MCP Server 在 Stdio 场景下**冷启动慢**的痛点。

---

## 2. Docker 镜像

远程部署优先选 Docker 镜像：多阶段构建 + 小基础镜像，启动即用。

### 2.1 Dockerfile

```dockerfile
# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /src
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw -q dependency:go-offline
COPY src ./src
RUN ./mvnw -q clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /src/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

**设计要点**：
- 多阶段构建：构建层含完整工具链，最终层只带 runtime + artifact，体积/攻击面最小
- stdio 场景罕见容器化（Client 无法跨容器拉进程），HTTP 是默认选择
- 端口 `EXPOSE` 与启动时 `--port` 参数保持一致；云平台探活走 `POST /mcp` initialize

### 2.2 构建 & 运行

```bash
# 在项目根目录（含 Dockerfile）下
docker build -t my-mcp-server:0.1.0 .
docker run --rm -p 8000:8000 my-mcp-server:0.1.0

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
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
      - run: ./mvnw -B clean verify
```

**扩展建议**：
- 加 `docker/build-push-action@v5` 步骤在 tag 推送时自动 build + push 到 GHCR / 公司内部 registry
- 分支保护规则要求 CI 绿灯才能合并主干
- 安全：`dependabot.yml` 定时检查依赖；`CodeQL` 扫描代码
