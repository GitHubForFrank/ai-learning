# mcp-server-demo-webmvc

> 基于 Spring AI MCP Server Starter 的天气查询 MCP Server，**WebMVC（传统 Servlet）HTTP + SSE** 传输版本。数据源为美国国家气象局（weather.gov）。

---

## 1. 能力

| 工具 | 说明 |
|------|------|
| `getWeatherForecastByLocation` | 按经纬度查询天气预报 |
| `getAlerts` | 按美国州代码（如 `CA`、`NY`）查询天气预警 |

版本：见 `pom.xml` 的 `<version>`。

---

## 2. 目录结构

```
mcp-server-demo-webmvc/
├── pom.xml                          · 依赖与构建声明
├── mvnw / mvnw.cmd                  · Maven Wrapper
└── src/
    ├── main/
    │   ├── java/                    · Spring Boot 入口 + @Tool 注解的 WeatherService
    │   └── resources/
    │       └── application.properties  · 端口、SSE 端点、变更通知等配置项
    └── test/java/                   · 示例 Client（SSE + STDIO 两种）
```

---

## 3. 构建

```bash
cd mcp-server-demo-webmvc
./mvnw clean install -DskipTests
```

## 4. 运行

**默认 HTTP + SSE 模式**（端口 8080，端点 `/mcp/message`）：

```bash
java -jar target/mcp-server-demo-webmvc-*.jar
```

**STDIO 模式**（同一个 jar 切到 stdio）：

```bash
java -Dspring.ai.mcp.handler.stdio=true \
     -Dspring.main.web-application-type=none \
     -jar target/mcp-server-demo-webmvc-*.jar
```

## 5. 作为 MCP Server 挂载

HTTP/SSE 模式：

```jsonc
{
  "mcpServers": {
    "weather-webmvc": { "url": "http://localhost:8080" }
  }
}
```

STDIO 模式：

```jsonc
{
  "mcpServers": {
    "weather-webmvc": {
      "command": "java",
      "args": [
        "-Dspring.ai.mcp.handler.stdio=true",
        "-Dspring.main.web-application-type=none",
        "-jar", "/absolute/path/to/target/mcp-server-demo-webmvc-0.0.1-SNAPSHOT.jar"
      ]
    }
  }
}
```

---

## 6. 调试

```bash
npx @modelcontextprotocol/inspector
# Server 启动后在 Inspector UI 填 URL http://localhost:8080
```

---

## 7. 更多

- 语言级开发指南：`../guide/`（4 份编号文件）
- Spring AI MCP 官方文档：<https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html>
- **WebMVC vs WebFlux 选型**：同项目同步/响应式对照，响应式变体在 `../mcp-server-demo-webflux/`
