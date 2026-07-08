# SOP-05: Node.js Agent Skill 服务开发规范

> 版本：1.0 | 适用范围：Node.js/TypeScript 技术栈的 Agent Skill 后端服务

---

## 1. 技术栈推荐

| 层级 | 推荐技术 | 说明 |
|------|---------|------|
| **运行时** | Node.js 20 LTS + TypeScript 5.x | 强类型，生产稳定 |
| **Web 框架** | Fastify 4.x | 高性能，内置 Schema 验证 |
| **Schema 验证** | Zod | TypeScript 友好的 Schema 验证 |
| **配置管理** | dotenv + env-var | 环境变量类型安全读取 |
| **HTTP 客户端** | axios 或 fetch（Node 18+） | 外部 API 调用 |
| **日志** | pino | 高性能 JSON 日志 |
| **测试框架** | Vitest | 快速，兼容 Jest API |
| **包管理** | pnpm | 快速，磁盘占用小 |

---

## 2. 标准目录结构

```
{skill-name}/
├── src/
│   ├── index.ts                  # 应用入口
│   ├── app.ts                    # Fastify 应用配置
│   ├── config/
│   │   └── index.ts              # 配置读取与验证
│   ├── routes/
│   │   ├── index.ts              # 路由注册
│   │   └── skill.routes.ts       # Skill 业务路由
│   ├── services/
│   │   └── skill.service.ts      # 业务逻辑层
│   ├── schemas/
│   │   └── skill.schema.ts       # Zod Schema 定义
│   ├── clients/
│   │   └── external.client.ts    # 外部 API 客户端
│   └── errors/
│       └── app.error.ts          # 自定义错误类
├── tests/
│   ├── unit/
│   │   └── skill.service.test.ts
│   └── integration/
│       └── skill.routes.test.ts
├── Dockerfile
├── .env.example
├── package.json
├── tsconfig.json
├── vitest.config.ts
└── skill.json
```

---

## 3. 核心代码实现

### 3.1 应用入口（src/index.ts）

```typescript
import { buildApp } from './app.js';
import { config } from './config/index.js';

const app = await buildApp();

try {
  await app.listen({ port: config.port, host: '0.0.0.0' });
  app.log.info(`Server running on port ${config.port}`);
} catch (err) {
  app.log.error(err);
  process.exit(1);
}
```

### 3.2 Fastify 应用配置（src/app.ts）

```typescript
import Fastify, { FastifyInstance } from 'fastify';
import { skillRoutes } from './routes/skill.routes.js';

export async function buildApp(): Promise<FastifyInstance> {
  const app = Fastify({
    logger: {
      level: process.env.LOG_LEVEL ?? 'info',
      transport:
        process.env.NODE_ENV === 'development'
          ? { target: 'pino-pretty' }
          : undefined,
    },
  });

  // 注册路由
  await app.register(skillRoutes, { prefix: '/api' });

  // 健康检查
  app.get('/actuator/health', async () => ({
    status: 'UP',
    timestamp: new Date().toISOString(),
  }));

  // 全局错误处理
  app.setErrorHandler((error, request, reply) => {
    request.log.error({ err: error }, 'Request failed');

    if (error.validation) {
      return reply.status(400).send({
        code: 'VALIDATION_ERROR',
        message: '请求参数不合法',
        details: error.validation,
      });
    }

    if (error instanceof AppError) {
      return reply.status(error.statusCode).send({
        code: error.code,
        message: error.message,
      });
    }

    return reply.status(500).send({
      code: 'INTERNAL_ERROR',
      message: '服务内部错误',
    });
  });

  return app;
}
```

### 3.3 配置管理（src/config/index.ts）

```typescript
import { z } from 'zod';
import 'dotenv/config';

const configSchema = z.object({
  port: z.coerce.number().int().min(1).max(65535).default(3000),
  nodeEnv: z.enum(['development', 'production', 'test']).default('development'),
  externalApiUrl: z.string().url(),
  externalApiKey: z.string().min(1),
  logLevel: z.enum(['trace', 'debug', 'info', 'warn', 'error']).default('info'),
});

const parsed = configSchema.safeParse({
  port: process.env.PORT,
  nodeEnv: process.env.NODE_ENV,
  externalApiUrl: process.env.EXTERNAL_API_URL,
  externalApiKey: process.env.EXTERNAL_API_KEY,
  logLevel: process.env.LOG_LEVEL,
});

if (!parsed.success) {
  console.error('❌ 环境变量配置不合法：');
  console.error(parsed.error.format());
  process.exit(1);
}

export const config = parsed.data;
```

### 3.4 Zod Schema 定义（src/schemas/skill.schema.ts）

```typescript
import { z } from 'zod';

// 请求 Schema
export const SkillRequestSchema = z.object({
  query: z.string().min(1).max(500).describe('查询内容'),
  options: z
    .object({
      maxResults: z.number().int().min(1).max(100).default(10),
      language: z.enum(['zh', 'en']).default('zh'),
    })
    .optional(),
});

// 响应 Schema
export const SkillResponseSchema = z.object({
  results: z.array(
    z.object({
      id: z.string(),
      title: z.string(),
      content: z.string(),
      score: z.number().min(0).max(1),
    })
  ),
  total: z.number().int().min(0),
  processingTimeMs: z.number().int().min(0),
});

// 类型导出
export type SkillRequest = z.infer<typeof SkillRequestSchema>;
export type SkillResponse = z.infer<typeof SkillResponseSchema>;
```

### 3.5 路由层（src/routes/skill.routes.ts）

```typescript
import { FastifyPluginAsync } from 'fastify';
import { ZodTypeProvider } from 'fastify-type-provider-zod';
import { SkillRequestSchema, SkillResponseSchema } from '../schemas/skill.schema.js';
import { SkillService } from '../services/skill.service.js';

export const skillRoutes: FastifyPluginAsync = async (app) => {
  const fastify = app.withTypeProvider<ZodTypeProvider>();
  const skillService = new SkillService();

  fastify.post('/skill/execute', {
    schema: {
      body: SkillRequestSchema,
      response: { 200: SkillResponseSchema },
    },
  }, async (request, reply) => {
    const result = await skillService.execute(request.body);
    return reply.send(result);
  });
};
```

### 3.6 服务层（src/services/skill.service.ts）

```typescript
import { ExternalClient } from '../clients/external.client.js';
import { SkillRequest, SkillResponse } from '../schemas/skill.schema.js';
import { AppError } from '../errors/app.error.js';

export class SkillService {
  private readonly externalClient: ExternalClient;

  constructor() {
    this.externalClient = new ExternalClient();
  }

  async execute(request: SkillRequest): Promise<SkillResponse> {
    const startTime = Date.now();

    const rawResults = await this.externalClient.search(request.query, {
      limit: request.options?.maxResults ?? 10,
      language: request.options?.language ?? 'zh',
    });

    if (!rawResults || rawResults.length === 0) {
      return {
        results: [],
        total: 0,
        processingTimeMs: Date.now() - startTime,
      };
    }

    return {
      results: rawResults.map((item) => ({
        id: item.id,
        title: item.title,
        content: item.summary,
        score: item.relevanceScore,
      })),
      total: rawResults.length,
      processingTimeMs: Date.now() - startTime,
    };
  }
}
```

### 3.7 自定义错误类（src/errors/app.error.ts）

```typescript
export class AppError extends Error {
  constructor(
    public readonly code: string,
    public readonly message: string,
    public readonly statusCode: number = 500
  ) {
    super(message);
    this.name = 'AppError';
  }
}

export class NotFoundError extends AppError {
  constructor(resource: string) {
    super('NOT_FOUND', `${resource} 不存在`, 404);
  }
}

export class ValidationError extends AppError {
  constructor(message: string) {
    super('VALIDATION_ERROR', message, 400);
  }
}

export class ExternalServiceError extends AppError {
  constructor(service: string, cause?: Error) {
    super('EXTERNAL_SERVICE_ERROR', `外部服务 ${service} 调用失败`, 502);
    if (cause) this.cause = cause;
  }
}
```

---

## 4. tsconfig.json 配置

```jsonc
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "NodeNext",
    "moduleResolution": "NodeNext",
    "lib": ["ES2022"],
    "outDir": "dist",
    "rootDir": "src",
    "strict": true,
    "noUncheckedIndexedAccess": true,
    "exactOptionalPropertyTypes": true,
    "declaration": true,
    "declarationMap": true,
    "sourceMap": true,
    "esModuleInterop": false,
    "skipLibCheck": true
  },
  "include": ["src/**/*"],
  "exclude": ["node_modules", "dist", "tests"]
}
```

---

## 5. 测试示例（Vitest）

```typescript
// tests/unit/skill.service.test.ts
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { SkillService } from '../../src/services/skill.service.js';
import { ExternalClient } from '../../src/clients/external.client.js';

vi.mock('../../src/clients/external.client.js');

describe('SkillService', () => {
  let service: SkillService;
  let mockClient: vi.Mocked<ExternalClient>;

  beforeEach(() => {
    mockClient = new ExternalClient() as vi.Mocked<ExternalClient>;
    service = new SkillService();
    // 注入 Mock
    (service as any).externalClient = mockClient;
  });

  it('should return empty results when no data found', async () => {
    mockClient.search.mockResolvedValue([]);

    const result = await service.execute({ query: 'not-exist' });

    expect(result.results).toHaveLength(0);
    expect(result.total).toBe(0);
  });

  it('should map external results to response format', async () => {
    mockClient.search.mockResolvedValue([
      { id: '1', title: 'Test', summary: 'Content', relevanceScore: 0.9 },
    ]);

    const result = await service.execute({ query: 'test' });

    expect(result.results[0]).toMatchObject({
      id: '1',
      title: 'Test',
      content: 'Content',
      score: 0.9,
    });
  });
});
```

---

## 6. Dockerfile

```dockerfile
FROM node:20-alpine AS base
WORKDIR /app
RUN npm install -g pnpm

# 依赖安装阶段
FROM base AS deps
COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile --prod

# 构建阶段
FROM base AS builder
COPY package.json pnpm-lock.yaml tsconfig.json ./
RUN pnpm install --frozen-lockfile
COPY src ./src
RUN pnpm build

# 生产镜像
FROM node:20-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production

COPY --from=deps /app/node_modules ./node_modules
COPY --from=builder /app/dist ./dist
COPY package.json ./

EXPOSE 3000
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s \
  CMD wget -qO- http://localhost:3000/actuator/health || exit 1

USER node
CMD ["node", "dist/index.js"]
```

---

## 7. 代码质量检查清单

在提交代码或发布版本前，确认以下项目：

### 类型安全
- [ ] 所有函数参数和返回值均有明确类型注解
- [ ] 无 `any` 类型使用（tsconfig 启用 `strict` 模式）
- [ ] 所有外部数据来源（API 响应、环境变量）均经过 Zod 验证

### 错误处理
- [ ] 所有异步操作均有 try-catch 处理
- [ ] 自定义错误继承 `AppError` 基类
- [ ] 全局错误处理器已注册且覆盖所有错误类型
- [ ] 错误响应不泄露内部实现细节

### 配置与安全
- [ ] 所有配置通过 `config` 模块读取（不直接使用 `process.env`）
- [ ] `.env` 文件未提交，`.env.example` 已更新
- [ ] 敏感信息不出现在日志中

### 测试覆盖
- [ ] Service 层核心方法有单元测试
- [ ] 主要 API 端点有集成测试
- [ ] 边界情况（空输入、超大输入、服务不可用）已测试

### 依赖管理
- [ ] 使用 `pnpm-lock.yaml` 锁定版本
- [ ] 生产依赖与开发依赖正确分类
- [ ] 无已知安全漏洞（运行 `pnpm audit`）
