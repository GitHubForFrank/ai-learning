# mcp-server-demo-stdio

> 基于 Spring AI MCP Server Starter 的天气查询 MCP Server，**stdio 传输**版本。数据源为美国国家气象局（weather.gov）。

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
mcp-server-demo-stdio/
├── pom.xml                          · 依赖与构建声明
├── mvnw / mvnw.cmd                  · Maven Wrapper（无需预装 Maven）
└── src/
    ├── main/
    │   ├── java/                    · Spring Boot 入口 + @Tool 注解的 WeatherService
    │   └── resources/
    │       └── application.properties  · stdio 必备的 banner/log 关闭项 + MCP Server 元信息
    └── test/java/                   · ClientStdio.java 示例 Client
```

---

## 3. 构建

```bash
cd mcp-server-demo-stdio
./mvnw clean install -DskipTests
# 产物：target/mcp-server-demo-stdio-*.jar
```

## 4. 作为 MCP Server 挂载

stdio 模式下 Server 由 Client 拉起，不手动跑。在 `.mcp.json` 配置：

```jsonc
{
  "mcpServers": {
    "weather-stdio": {
      "command": "java",
      "args": ["-jar", "/absolute/path/to/target/mcp-server-demo-stdio-0.0.1-SNAPSHOT.jar"]
    }
  }
}
```

---

## 5. 调试

```bash
npx @modelcontextprotocol/inspector java -jar target/mcp-server-demo-stdio-*.jar
```

浏览器 UI 可手动调用工具、查看 JSON-RPC 流量。

---

## 6. 更多

- 语言级开发指南：`../guide/`（4 份编号文件）
- Spring AI MCP 官方文档：<https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html>
