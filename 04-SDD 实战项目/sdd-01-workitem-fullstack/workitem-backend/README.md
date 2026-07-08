# workitem-backend:后端代码仓(Backend Repo)

> **仓库角色**:用 Spring Boot 4 + JDK 21 + MyBatis Plus 实现 `workitem-docs` 定义的 5 个 REST API,承载 BR-01 ~ BR-08 的业务规则。
>
> **Implements**: `spec@vTBD`(P2 阶段发布首个 spec tag 后填入)
>
> **当前阶段**:✅ **已完成(Task001)**

## 一、技术栈速览

| 维度 | 选型 | 版本 | 真源 |
|---|---|---|---|
| 语言 | Java | 21+ | `workitem-docs/design/03-技术方案.md §1.1` |
| 框架 | Spring Boot | 4.0.5 | 同上 |
| 持久层 | MyBatis Plus(`mybatis-plus-spring-boot4-starter`)| 3.5.16 | 同上 |
| 连接池 | HikariCP | 随 SB | 同上 |
| 数据库 | MySQL | 8.0 | 同上 |
| 数据库迁移 | Flyway(指向 `workitem-db/migration/`) | 10.x | 同上 + `workitem-db` |
| 日志 | Logback + 外挂 `logback-config.xml`(springProfile 分环境) | 随 SB | `workitem-docs/conventions/monitoring-conventions.md §4` |
| 代码简化 | Lombok | 随 SB | `workitem-docs/design/03-技术方案.md §1.1` |
| 构建 | Maven | 3.8+ | 同上 |
| 测试 | JUnit 5 + Mockito + MockMvc + Testcontainers | 随 SB | 同上 |

## 二、本仓职责

- 实现 `workitem-docs/specs/02-功能规范.md` 的 5 个接口(API-01 ~ API-05)
- 实现 `workitem-docs/specs/02-功能规范.md §4` 的 BR-01 ~ BR-08 全部业务规则
- 启动时对接 `workitem-db/migration/` 的 Flyway 脚本(通过 `spring.flyway.locations` 指向外部路径)
- 提供 SpringDoc OpenAPI / Swagger UI
- 承担 `workitem-docs/specs/02-功能规范.md §8.2` 的 BE-01 ~ BE-05 模块

## 三、本仓不做

- ❌ 数据库 DDL 脚本 —— 归 `workitem-db`
- ❌ 任何前端代码 —— 归 `workitem-frontend`
- ❌ 容器编排 / 基础设施脚本 —— 本案例不沉淀,MySQL 由开发者本机自备(详见工作区 README §三)
- ❌ 业务决策 —— 任何新规则必须先在 `workitem-docs` 提交 PR 并发版

## 四、目录结构(DDD 分层,P2 阶段产出,当前占位)

```
workitem-backend/
├── README.md                                        # 本文件
├── .gitignore                                       # Java/Maven 专属忽略(target/、*.iml、hs_err_pid* 等)
├── pom.xml                                          # [P2] Maven 构建
└── src/
    ├── main/
    │   ├── java/com/zmz/sdd/fullstack/              # [P2] 后端代码
    │   │   ├── TaskFullstackApplication.java        # @SpringBootApplication + @MapperScan
    │   │   ├── app/                                 # 业务代码根
    │   │   │   ├── api/
    │   │   │   │   └── controller/                  # BE-01 REST Controller
    │   │   │   ├── application/
    │   │   │   │   └── service/
    │   │   │   │       └── impl/                    # BE-02 业务规则
    │   │   │   ├── domain/
    │   │   │   │   ├── model/                       # 领域模型 + 枚举
    │   │   │   │   └── repository/                  # 仓储接口
    │   │   │   └── infrastructure/
    │   │   │       ├── dao/
    │   │   │       │   ├── entity/                  # MP Entity(@TableName + @TableLogic)
    │   │   │       │   └── mapper/                  # BE-03 MP Mapper(BaseMapper)
    │   │   │       └── repository/
    │   │   │           ├── impl/                    # 仓储实现
    │   │   │           └── translator/              # Model ↔ Entity 转换
    │   │   └── core/                                # 横切关注点
    │   │       ├── config/                          # BE-04 + BE-05
    │   │       │   ├── MybatisPlusConfig.java
    │   │       │   ├── MetaObjectHandlerConfig.java
    │   │       │   ├── CorsConfig.java
    │   │       │   └── GlobalExceptionHandler.java
    │   │       └── common/                          # Result / ErrorCode / BizException
    │   └── resources/
    │       ├── application.properties               # [P2] 主配置(指向 workitem-db 的 Flyway)
    │       ├── logback-config.xml                   # [P2] Logback 外挂(分 profile)
    │       └── mybatis.properties                   # [P2] MP 额外配置(可并入 application)
    └── test/
        └── java/com/zmz/sdd/fullstack/              # [P2] L2 + L3 测试
            └── app/
                ├── application/service/             # TC-U-*(L2 单元)
                └── api/controller/                  # TC-IT-*(L3 Testcontainers MySQL 集成)
```

> ⚠️ 分层规则、调用方向与禁用清单详见 `workitem-docs/design/03-技术方案.md §4`。

## 五、Flyway 迁移脚本来源

本仓**不存放**迁移脚本,但启动时**必须执行**它们。方案(P2 定稿):

| 方案 | 原理 | 适用 |
|---|---|---|
| A. 外部路径加载 | `spring.flyway.locations=filesystem:../workitem-db/migration` | 本地开发(工作区布局下) |
| B. 构建期拷贝 | CI/CD 把 `workitem-db/migration/*.sql` 拷贝进 jar 的 `classpath:db/migration/` | 镜像构建 / 生产 |
| C. Submodule / Subtree | 把 `workitem-db` 作为 submodule 嵌入 —— **不采用**(违背独立仓精神) | - |

本项目选 **A(本地)+ B(规划中,P2 按需补)**。

## 六、与 spec 的对齐义务

1. 本 README 顶部 `Implements: spec@vX.Y.Z` **必须**保持与主干代码一致
2. **所有 PR 标题以 `Task<NNN>:` 起头**(Task 编号见 `workitem-docs/specs/06-任务与发布管理.md §2`)
3. PR 描述**必须**包含 `## Task` / `## Implements` / `## Covers` 三段(模板见 `workitem-docs/specs/05-跨仓协调.md §3.2`)
4. 代码关键位置**必须**双标签注释:`// [SDD-TASK: Task<NNN>][SDD-SPEC: ...]`
5. 引入 feature flag 命名为 `feat_task<NNN>_<短语>`(见 `06 §5`)
6. 任何"spec 里没写"的业务判断**不能直接写代码**,先回去提 spec PR
7. MP 自定义 XML SQL **必须**手工加 `deleted=0` 过滤(MP 全局逻辑删除仅覆盖 `BaseMapper` 通用方法,见 `workitem-docs/conventions/db-conventions.md §9.1`)

## 七、启动与验证

### 7.1 前置

- JDK 21+;Maven 3.8+
- 本机或可达的 MySQL 8.0 实例已运行,且已建好空库 `zmz_sdd_demo`(开发者自备,本案例不提供容器编排;Flyway 会在启动时建表)
- 连接参数(host/port/user/password)默认走 `src/main/resources/application.properties`,本地覆盖请新建 `application-local.properties`(已被 `.gitignore` 排除,启动加 `--spring.profiles.active=local`)
- `workitem-db/migration/` 在工作区中存在(本 backend 通过 `spring.flyway.locations=filesystem:../workitem-db/migration` 加载脚本)

### 7.2 启动

```bash
cd "04-SDD 实战项目/sdd-01-workitem-fullstack/workitem-backend"
mvn spring-boot:run
```

**Verify**:控制台出现 `Started TaskFullstackApplication`;

```bash
curl -i http://localhost:10197/app/api/login -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"12346#@&"}'
# 期望:HTTP 200,响应体 code=0,data.token 非空,data.expiresIn=3600
```

### 7.3 关键访问路径

| 用途 | URL |
|---|---|
| 登录 | POST `http://localhost:10197/app/api/login` |
| 当前用户 | GET `http://localhost:10197/app/api/me`(需 `Authorization: Bearer <jwt>`)|
| 任务接口 | `/api/tasks/**`(需 JWT)|
| Swagger UI | `http://localhost:10197/app/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:10197/app/v3/api-docs` |

### 7.4 跑测试

```bash
cd "04-SDD 实战项目/sdd-01-workitem-fullstack/workitem-backend"
mvn test
```

**Verify**:`mvn test` 退出码 0;首次会拉取 `mysql:8.0` 容器镜像(Testcontainers)。

## 八、本仓涉及的 spec 条款速查

| spec 条款 | 本仓实现位置(P2 产出) |
|---|---|
| §3 API-01 ~ API-05 | `app/api/controller/` + `app/application/service/impl/` |
| §4 BR-01 ~ BR-08 | `app/application/service/impl/TaskServiceImpl.java` |
| §5 错误码 | `core/common/ErrorCode.java` + `core/config/GlobalExceptionHandler.java` |
| §8 BE-01 ~ BE-05 | 各层对应类 |
| MP 逻辑删除 | `app/infrastructure/dao/entity/*Entity.java` `@TableLogic` + `application.properties` |
| MP 自动填充时间戳 | `core/config/MetaObjectHandlerConfig.java` |
| `conventions/test-conventions.md` | `src/test/java/` |

---

**占位状态**:本 README 在 P1 阶段仅声明职责与对齐规则。P2 阶段补齐代码目录、pom.xml、启动命令与 verify 步骤。
