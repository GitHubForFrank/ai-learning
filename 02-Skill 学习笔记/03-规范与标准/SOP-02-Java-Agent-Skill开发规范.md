# SOP-02: Java Agent Skill 服务开发规范

> 版本：1.0 | 适用范围：使用 Java（Spring Boot）开发的 Agent Skill 服务

---

## 1. 技术栈推荐

| 组件 | 推荐选型 | 说明 |
|------|----------|------|
| 框架 | Spring Boot 3.x | 主流框架，生态完善 |
| 构建工具 | Maven 或 Gradle | 推荐 Maven，依赖管理更直观 |
| Java 版本 | Java 17 / 21 LTS | 推荐使用 LTS 版本 |
| API 风格 | RESTful / Spring WebFlux | 同步用 MVC，高并发用 WebFlux |
| 文档 | SpringDoc OpenAPI 3 | 自动生成 Swagger UI |
| 测试 | JUnit 5 + Mockito | 单元/集成测试标准组合 |
| 容器化 | Docker + Jib Plugin | Jib 无需 Dockerfile 即可打包镜像 |

---

## 2. 推荐 Package 目录结构

```
src/
├── main/
│   ├── java/
│   │   └── com/org/skill/{skill-name}/
│   │       │
│   │       ├── SkillApplication.java          ✅ 启动类
│   │       │
│   │       ├── api/                           ✅ 对外接口层
│   │       │   ├── controller/                Controller，处理 HTTP 请求
│   │       │   │   ├── SkillController.java   主要能力接口
│   │       │   │   └── HealthController.java  健康检查接口
│   │       │   ├── request/                   请求体 DTO
│   │       │   │   └── SkillRequest.java
│   │       │   └── response/                  响应体 DTO
│   │       │       ├── SkillResponse.java
│   │       │       └── ApiResult.java         统一响应包装类
│   │       │
│   │       ├── service/                       ✅ 业务逻辑层
│   │       │   ├── SkillService.java          业务接口
│   │       │   └── impl/
│   │       │       └── SkillServiceImpl.java  业务实现
│   │       │
│   │       ├── domain/                        ✅ 领域模型层
│   │       │   ├── model/                     领域实体/值对象
│   │       │   │   └── SkillEntity.java
│   │       │   └── enums/                     枚举定义
│   │       │       └── StatusEnum.java
│   │       │
│   │       ├── infrastructure/                ✅ 基础设施层
│   │       │   ├── client/                    外部服务调用（HTTP/gRPC）
│   │       │   │   └── ExternalApiClient.java
│   │       │   ├── repository/                数据持久化（如有）
│   │       │   │   └── SkillRepository.java
│   │       │   └── cache/                     缓存操作（如有）
│   │       │       └── SkillCacheService.java
│   │       │
│   │       ├── config/                        ✅ 配置类
│   │       │   ├── AppConfig.java             应用级配置
│   │       │   ├── WebConfig.java             Web/CORS 配置
│   │       │   ├── SecurityConfig.java        安全配置（如有）
│   │       │   └── OpenApiConfig.java         Swagger 文档配置
│   │       │
│   │       ├── exception/                     ✅ 异常处理
│   │       │   ├── GlobalExceptionHandler.java  全局异常处理器
│   │       │   ├── SkillException.java          业务异常基类
│   │       │   └── ErrorCode.java               错误码枚举
│   │       │
│   │       └── util/                          🔲 工具类（按需添加）
│   │           └── JsonUtils.java
│   │
│   └── resources/
│       ├── application.yml                    ✅ 主配置文件
│       ├── application-dev.yml                ⭕ 开发环境配置
│       ├── application-prod.yml               ⭕ 生产环境配置
│       └── logback-spring.xml                 ⭕ 日志配置
│
└── test/
    └── java/
        └── com/org/skill/{skill-name}/
            ├── api/
            │   └── controller/
            │       └── SkillControllerTest.java
            ├── service/
            │   └── SkillServiceTest.java
            └── integration/
                └── SkillIntegrationTest.java
```

---

## 3. 各层职责说明

### 3.1 api 层（接口层）

**职责**：接收外部请求，参数校验，调用 service 层，返回统一响应。

**规范**：
- Controller 只做参数验证和调度，不写业务逻辑
- 使用 `@Valid` / `@Validated` 做入参校验
- 统一使用 `ApiResult<T>` 包装响应体

```java
// ApiResult.java - 统一响应包装
@Data
@Builder
public class ApiResult<T> {
    private int code;           // 业务状态码，成功为 0
    private String message;     // 响应消息
    private T data;             // 响应数据
    private String traceId;     // 链路追踪 ID

    public static <T> ApiResult<T> success(T data) {
        return ApiResult.<T>builder()
                .code(0)
                .message("success")
                .data(data)
                .traceId(MDC.get("traceId"))
                .build();
    }

    public static <T> ApiResult<T> error(int code, String message) {
        return ApiResult.<T>builder()
                .code(code)
                .message(message)
                .traceId(MDC.get("traceId"))
                .build();
    }
}
```

```java
// SkillController.java - Controller 示例
@RestController
@RequestMapping("/api/v1/skill")
@Validated
@Slf4j
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @PostMapping("/execute")
    public ResponseEntity<ApiResult<SkillResponse>> execute(
            @Valid @RequestBody SkillRequest request) {
        log.info("Executing skill, requestId={}", request.getRequestId());
        SkillResponse response = skillService.execute(request);
        return ResponseEntity.ok(ApiResult.success(response));
    }
}
```

### 3.2 service 层（业务逻辑层）

**职责**：封装核心业务逻辑，编排各基础设施能力。

**规范**：
- 面向接口编程，`SkillService` 定义接口，`SkillServiceImpl` 实现
- 业务异常统一抛出 `SkillException`，不直接抛出底层异常
- 不直接依赖 Controller 层的 DTO，可定义 Service 内部模型

```java
// SkillService.java - 业务接口
public interface SkillService {
    SkillResponse execute(SkillRequest request);
}

// SkillServiceImpl.java - 业务实现
@Service
@Slf4j
public class SkillServiceImpl implements SkillService {

    private final ExternalApiClient externalApiClient;

    public SkillServiceImpl(ExternalApiClient externalApiClient) {
        this.externalApiClient = externalApiClient;
    }

    @Override
    public SkillResponse execute(SkillRequest request) {
        // 1. 业务校验
        validateRequest(request);
        // 2. 调用外部服务
        String result = externalApiClient.call(request.getInput());
        // 3. 处理结果
        return buildResponse(result);
    }

    private void validateRequest(SkillRequest request) {
        if (StringUtils.isBlank(request.getInput())) {
            throw new SkillException(ErrorCode.INVALID_INPUT, "input cannot be blank");
        }
    }
}
```

### 3.3 domain 层（领域模型层）

**职责**：定义核心业务实体、值对象和枚举，保持纯净，不依赖框架。

**规范**：
- 实体类使用 Lombok `@Data` / `@Value` 减少样板代码
- 枚举统一在 `enums/` 下管理，提供 `code` 和 `description` 字段

```java
// StatusEnum.java - 枚举示例
@Getter
public enum StatusEnum {
    SUCCESS(0, "成功"),
    PROCESSING(1, "处理中"),
    FAILED(2, "失败");

    private final int code;
    private final String description;

    StatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static StatusEnum fromCode(int code) {
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown code: " + code));
    }
}
```

### 3.4 infrastructure 层（基础设施层）

**职责**：对接外部系统（HTTP API、数据库、缓存、消息队列等）。

**规范**：
- 外部 HTTP 调用统一使用 `client/` 包，封装重试、超时、熔断逻辑
- 推荐使用 `Spring RestClient`（Spring 6.1+）或 `WebClient`
- 对外部异常进行捕获并转换为内部 `SkillException`

```java
// ExternalApiClient.java - 外部服务客户端示例
@Component
@Slf4j
public class ExternalApiClient {

    private final RestClient restClient;

    public ExternalApiClient(RestClient.Builder builder,
                              @Value("${external.api.url}") String apiUrl,
                              @Value("${external.api.key}") String apiKey) {
        this.restClient = builder
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public String call(String input) {
        try {
            return restClient.post()
                    .uri("/process")
                    .body(Map.of("input", input))
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("External API call failed, input={}", input, e);
            throw new SkillException(ErrorCode.EXTERNAL_API_ERROR, "External service unavailable");
        }
    }
}
```

### 3.5 exception 层（异常处理层）

**职责**：统一异常定义和全局异常处理，保证 API 响应格式一致性。

```java
// ErrorCode.java - 错误码定义
@Getter
public enum ErrorCode {
    SUCCESS(0, "成功"),
    INVALID_INPUT(1001, "输入参数非法"),
    EXTERNAL_API_ERROR(2001, "外部服务调用失败"),
    INTERNAL_ERROR(5000, "内部服务错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

// SkillException.java - 业务异常
public class SkillException extends RuntimeException {
    private final ErrorCode errorCode;

    public SkillException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }

    public int getCode() { return errorCode.getCode(); }
}

// GlobalExceptionHandler.java - 全局异常处理
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(SkillException.class)
    public ResponseEntity<ApiResult<Void>> handleSkillException(SkillException e) {
        log.warn("Business exception: code={}, message={}", e.getCode(), e.getMessage());
        return ResponseEntity.ok(ApiResult.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleValidationException(
            MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.ok(ApiResult.error(ErrorCode.INVALID_INPUT.getCode(), message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(500)
                .body(ApiResult.error(ErrorCode.INTERNAL_ERROR.getCode(), "Internal server error"));
    }
}
```

### 3.6 config 层（配置层）

**职责**：集中管理 Spring Bean 配置、外部配置参数绑定。

```java
// AppConfig.java - 外部配置绑定示例
@Configuration
@ConfigurationProperties(prefix = "skill")
@Data
public class AppConfig {
    private String name;
    private String version;
    private int timeoutSeconds = 30;
    private RetryConfig retry = new RetryConfig();

    @Data
    public static class RetryConfig {
        private int maxAttempts = 3;
        private long backoffMs = 1000;
    }
}
```

---

## 4. application.yml 配置规范

```yaml
spring:
  application:
    name: ${SKILL_NAME:my-skill}

server:
  port: ${SERVER_PORT:8080}

skill:
  name: ${SKILL_NAME:my-skill}
  version: 1.0.0
  timeout-seconds: ${SKILL_TIMEOUT:30}

external:
  api:
    url: ${EXTERNAL_API_URL}
    key: ${EXTERNAL_API_KEY}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always

logging:
  level:
    com.org.skill: ${LOG_LEVEL:INFO}
```

---

## 5. Maven pom.xml 关键依赖

```xml
<dependencies>
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Actuator（健康检查） -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- API 文档 -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.3.0</version>
    </dependency>

    <!-- 测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 6. 健康检查接口（必选）

Agent 平台通过健康检查确认 Skill 服务是否可用，推荐使用 Spring Actuator：

- 健康检查路径：`GET /actuator/health`
- 期望响应：`HTTP 200` + `{"status": "UP"}`

如需自定义健康检查逻辑：

```java
@Component
public class ExternalApiHealthIndicator implements HealthIndicator {

    private final ExternalApiClient client;

    @Override
    public Health health() {
        try {
            client.ping();
            return Health.up().withDetail("externalApi", "reachable").build();
        } catch (Exception e) {
            return Health.down().withDetail("externalApi", "unreachable").build();
        }
    }
}
```

---

## 7. 代码质量检查清单

- [ ] 所有 Controller 方法参数都有 `@Valid` 校验
- [ ] 没有业务逻辑写在 Controller 层
- [ ] 外部调用都有超时配置
- [ ] 所有异常都被 `GlobalExceptionHandler` 捕获，不直接向外暴露堆栈信息
- [ ] 敏感信息（API Key、密码）通过环境变量注入，不硬编码
- [ ] 日志中不打印敏感信息
- [ ] 接口有 OpenAPI 注解文档
- [ ] 单元测试覆盖核心 Service 方法
