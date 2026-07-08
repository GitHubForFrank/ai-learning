# Skill 开发规范篇 · 面试题库

> 涵盖 Agent Skill 项目的目录规范、多语言（Java/Python/Node.js）开发规范与最佳实践

---

## Q5.1 一个标准的 Agent Skill 项目应该包含哪些核心文件和目录？

**参考答案**：
```
{skill-name}/
├── README.md                  ✅ 必选 - Skill 说明文档
├── skill.json                 ✅ 必选 - Skill 元数据描述文件
├── CHANGELOG.md               ⭕ 推荐 - 版本变更记录
├── docs/                      ⭕ 推荐 - 扩展文档
│   ├── api.md                 ⭕ 推荐 - API 接口文档
│   └── examples/              ⭕ 推荐 - 使用示例
├── src/                       ✅ 必选 - 源代码目录
├── tests/                     ✅ 必选 - 测试代码目录
│   ├── unit/                  ⭕ 推荐 - 单元测试
│   └── integration/           ⭕ 推荐 - 集成测试
├── config/                    ⭕ 推荐 - 配置文件目录
├── scripts/                   🔲 可选 - 辅助脚本
├── docker/                    ⭕ 推荐 - 容器化文件
└── .env.example               ✅ 必选 - 环境变量示例（不含真实密钥）
```

**必选三件套**：`README.md` + `skill.json` + `.env.example`

**难度**：⭐⭐ | **考察点**：项目工程化认知

---

## Q5.2 `skill.json` 文件的作用和必含字段是什么？

**参考答案**：
`skill.json` 是 Skill 的元数据描述文件，Agent 平台通过它发现和注册 Skill。

**核心字段**：
```jsonc
{
  "name": "skill-name",
  "version": "1.0.0",
  "displayName": "Skill 显示名称",
  "description": "清晰说明该 Skill 能做什么",
  "author": "作者/团队名称",
  "category": "工具类别",
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
      "inputSchema": { "type": "object", "properties": {...} },
      "outputSchema": { "type": "object", "properties": {...} }
    }
  ],
  "dependencies": {
    "externalServices": ["服务A"],
    "environmentVariables": ["ENV_VAR_1"]
  },
  "deployment": {
    "minMemory": "256Mi",
    "recommendedMemory": "512Mi",
    "startupMode": "always-on | on-demand"
  }
}
```

**版本号约束**：`skill.json` 中的 `version` 必须与 Git Tag 保持一致。

**难度**：⭐⭐ | **考察点**：元数据规范

---

## Q5.3 Java Agent Skill 的分层架构是怎样的？各层职责是什么？

**参考答案**：
```
src/main/java/com/org/skill/{skill-name}/
├── api/controller/     ✅ 接口层：接收请求、参数校验、调用 service
├── api/request/        请求体 DTO
├── api/response/       响应体 DTO（含统一响应包装 ApiResult）
├── service/            ✅ 业务逻辑层：核心业务、编排外部能力
├── domain/model/       ✅ 领域模型层：实体/值对象/枚举，不依赖框架
├── infrastructure/     ✅ 基础设施层：外部 HTTP/gRPC 调用、数据持久化
├── config/             ✅ 配置类：Spring Bean 配置、外部配置绑定
├── exception/          ✅ 异常处理：全局异常处理器、错误码枚举
└── util/               🔲 工具类
```

**核心规范**：
- Controller 只做参数验证和调度，不写业务逻辑
- Service 面向接口编程，业务异常统一抛出 `SkillException`
- Domain 层保持纯净，不依赖框架
- Infrastructure 封装外部调用，包含超时、重试、熔断
- 统一使用 `ApiResult<T>` 包装响应体

**难度**：⭐⭐⭐ | **考察点**：分层架构设计

---

## Q5.4 Python Agent Skill 的技术栈推荐和关键代码规范是什么？

**参考答案**：

| 组件 | 推荐选型 | 说明 |
|------|----------|------|
| Web 框架 | FastAPI | 高性能、自动生成 OpenAPI、原生异步 |
| Python 版本 | 3.11 / 3.12 | 推荐稳定较新版本 |
| 数据校验 | Pydantic v2 | 与 FastAPI 内置集成 |
| HTTP 客户端 | httpx | 同时支持同步/异步 |
| 测试 | pytest + pytest-asyncio | 标准组合 |
| 代码规范 | ruff | 集 Linter + Formatter 于一体 |

**关键规范**：
- 所有配置通过 `pydantic-settings` 从环境变量加载，不硬编码
- 异步接口使用 `async def`，同步操作不阻塞事件循环
- 外部调用有超时配置和异常捕获
- 健康检查接口 `/api/v1/health` 必须可正常响应
- 敏感信息不打印到日志

**难度**：⭐⭐ | **考察点**：Python 技术栈熟悉度

---

## Q5.5 写出一个 Java Skill 的统一响应包装类和全局异常处理器的设计。

**参考答案**：

**ApiResult 统一响应包装**：
```java
@Data
@Builder
public class ApiResult<T> {
    private int code;           // 业务状态码，成功为 0
    private String message;     // 响应消息
    private T data;             // 响应数据
    private String traceId;     // 链路追踪 ID

    public static <T> ApiResult<T> success(T data) {
        return ApiResult.<T>builder()
                .code(0).message("success").data(data)
                .traceId(MDC.get("traceId")).build();
    }

    public static <T> ApiResult<T> error(int code, String message) {
        return ApiResult.<T>builder()
                .code(code).message(message)
                .traceId(MDC.get("traceId")).build();
    }
}
```

**全局异常处理器要点**：
1. 捕获 `SkillException`（业务异常）→ 返回业务错误码
2. 捕获 `MethodArgumentNotValidException`（参数校验）→ 返回校验失败详情
3. 捕获 `Exception`（兜底）→ 返回 500，不暴露内部堆栈
4. 所有异常都使用 `ApiResult.error()` 统一格式返回

**核心原则**：不直接向外暴露堆栈信息；有统一错误码枚举。

**难度**：⭐⭐⭐ | **考察点**：API 设计规范

---

## Q5.6 健康检查接口为什么是必选的？如何设计分级健康检查？

**参考答案**：

**为什么必选**：Agent 平台通过健康检查确认 Skill 服务是否可用，是服务注册和流量调度的基础。

**分级健康检查**：
```
/actuator/health/liveness   → 进程是否存活（K8s liveness probe）
/actuator/health/readiness  → 是否准备好接受流量（K8s readiness probe）
/actuator/health            → 完整健康状态（含依赖检查）
```

**实现要点**：
- 健康检查路径：`GET /actuator/health`（Java）或 `GET /api/v1/health`（Python）
- 期望响应：`HTTP 200` + `{"status": "UP"}`
- 定制健康检查：检查外部服务连通性、数据库连接等关键依赖

**难度**：⭐⭐ | **考察点**：运维意识

---

## Q5.7 .gitignore 必须排除哪些内容？

**参考答案**：
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

**关键原则**：`.env.example` 入库（含假值）、`.env` 不入库（含真实密钥）。

**难度**：⭐ | **考察点**：安全意识

---

> 参考来源：笔记 `SOP-01~05` 系列规范
