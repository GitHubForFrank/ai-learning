# SOP-03: Python Agent Skill 服务开发规范

> 版本：1.0 | 适用范围：使用 Python 开发的 Agent Skill 服务

---

## 1. 技术栈推荐

| 组件 | 推荐选型 | 说明 |
|------|----------|------|
| Web 框架 | FastAPI | 高性能、自动生成 OpenAPI 文档、原生异步支持 |
| Python 版本 | Python 3.11 / 3.12 | 推荐使用稳定的较新版本 |
| 包管理 | uv 或 pip + venv | 推荐 `uv`，速度快；传统项目用 `pip + requirements.txt` |
| 数据校验 | Pydantic v2 | FastAPI 内置，Schema 定义与校验一体 |
| HTTP 客户端 | httpx | 同时支持同步/异步，推荐替代 requests |
| 测试 | pytest + pytest-asyncio | 标准测试框架 |
| 代码规范 | ruff | 集 Linter + Formatter 于一体，速度极快 |
| 类型检查 | mypy / pyright | 静态类型检查 |
| 容器化 | Docker | 标准镜像打包 |

---

## 2. 推荐目录结构

```
{skill-name}/
├── README.md                      ✅ 项目说明
├── skill.json                     ✅ Skill 元数据
├── pyproject.toml                 ✅ 项目元数据与依赖（现代方式）
├── requirements.txt               ⭕ 依赖锁定文件（供 pip 使用）
├── requirements-dev.txt           ⭕ 开发依赖（测试、lint 工具）
├── .env.example                   ✅ 环境变量示例
├── .python-version                ⭕ Python 版本锁定（供 pyenv 使用）
├── Dockerfile                     ⭕ 容器镜像构建
├── docker-compose.yml             🔲 本地开发编排
│
├── app/                           ✅ 应用主目录
│   ├── __init__.py
│   ├── main.py                    ✅ FastAPI 应用入口，路由注册
│   │
│   ├── api/                       ✅ 路由/接口层
│   │   ├── __init__.py
│   │   ├── v1/
│   │   │   ├── __init__.py
│   │   │   ├── router.py          路由聚合（include_router 在此）
│   │   │   └── endpoints/
│   │   │       ├── __init__.py
│   │   │       ├── skill.py       主能力接口
│   │   │       └── health.py      健康检查接口
│   │   └── dependencies.py        公共依赖注入（认证、限流等）
│   │
│   ├── schemas/                   ✅ 数据模型层（Pydantic）
│   │   ├── __init__.py
│   │   ├── request.py             请求体 Schema
│   │   ├── response.py            响应体 Schema（含统一响应包装）
│   │   └── common.py              公共 Schema（分页、枚举等）
│   │
│   ├── services/                  ✅ 业务逻辑层
│   │   ├── __init__.py
│   │   └── skill_service.py       核心业务逻辑
│   │
│   ├── core/                      ✅ 核心配置与基础设施
│   │   ├── __init__.py
│   │   ├── config.py              配置加载（环境变量绑定）
│   │   ├── exceptions.py          自定义异常类与异常处理器
│   │   ├── logging.py             日志配置
│   │   └── security.py            认证/鉴权（如有）
│   │
│   ├── clients/                   ⭕ 外部服务客户端
│   │   ├── __init__.py
│   │   └── external_api_client.py 封装对外部 API 的调用
│   │
│   └── utils/                     🔲 工具函数
│       ├── __init__.py
│       └── helpers.py
│
├── tests/                         ✅ 测试目录
│   ├── __init__.py
│   ├── conftest.py                pytest fixtures 配置
│   ├── unit/
│   │   ├── __init__.py
│   │   └── test_skill_service.py
│   └── integration/
│       ├── __init__.py
│       └── test_skill_api.py
│
├── scripts/                       🔲 辅助脚本
│   ├── start.sh
│   └── test.sh
│
└── docs/
    └── api.md
```

---

## 3. 各模块详细说明

### 3.1 app/main.py（应用入口）

```python
# app/main.py
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.v1.router import api_v1_router
from app.core.config import settings
from app.core.exceptions import register_exception_handlers
from app.core.logging import setup_logging


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理：启动时初始化资源，关闭时释放资源"""
    setup_logging()
    # 启动时：初始化连接池、加载模型等
    yield
    # 关闭时：清理资源


def create_app() -> FastAPI:
    app = FastAPI(
        title=settings.SKILL_NAME,
        version=settings.SKILL_VERSION,
        description="Agent Skill 服务",
        docs_url="/docs",
        redoc_url="/redoc",
        lifespan=lifespan,
    )

    # 注册中间件
    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.ALLOWED_ORIGINS,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    # 注册路由
    app.include_router(api_v1_router, prefix="/api/v1")

    # 注册全局异常处理器
    register_exception_handlers(app)

    return app


app = create_app()
```

### 3.2 app/schemas/（数据模型层）

```python
# app/schemas/response.py - 统一响应包装
from typing import Generic, Optional, TypeVar
from pydantic import BaseModel

T = TypeVar("T")


class ApiResult(BaseModel, Generic[T]):
    """统一 API 响应格式"""
    code: int = 0
    message: str = "success"
    data: Optional[T] = None
    trace_id: Optional[str] = None

    @classmethod
    def success(cls, data: T, trace_id: str = None) -> "ApiResult[T]":
        return cls(code=0, message="success", data=data, trace_id=trace_id)

    @classmethod
    def error(cls, code: int, message: str, trace_id: str = None) -> "ApiResult[None]":
        return cls(code=code, message=message, data=None, trace_id=trace_id)


# app/schemas/request.py - 请求体示例
from pydantic import BaseModel, Field


class SkillRequest(BaseModel):
    request_id: str = Field(..., description="请求唯一标识")
    input: str = Field(..., min_length=1, max_length=10000, description="输入内容")
    options: dict = Field(default_factory=dict, description="可选参数")

    model_config = {"json_schema_extra": {"example": {
        "request_id": "req-001",
        "input": "Hello, world!",
        "options": {}
    }}}
```

### 3.3 app/core/config.py（配置管理）

```python
# app/core/config.py - 使用 pydantic-settings 绑定环境变量
from typing import List
from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
    )

    # 服务配置
    SKILL_NAME: str = "my-skill"
    SKILL_VERSION: str = "1.0.0"
    SERVER_HOST: str = "0.0.0.0"
    SERVER_PORT: int = 8080
    DEBUG: bool = False

    # 跨域配置
    ALLOWED_ORIGINS: List[str] = ["*"]

    # 外部服务配置
    EXTERNAL_API_URL: str = Field(..., description="外部 API 地址")
    EXTERNAL_API_KEY: str = Field(..., description="外部 API 密钥")
    EXTERNAL_TIMEOUT_SECONDS: int = 30

    # 日志配置
    LOG_LEVEL: str = "INFO"


# 全局单例配置对象
settings = Settings()
```

### 3.4 app/core/exceptions.py（异常处理）

```python
# app/core/exceptions.py
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from pydantic import ValidationError
import logging

logger = logging.getLogger(__name__)


class SkillException(Exception):
    """业务异常基类"""
    def __init__(self, code: int, message: str):
        self.code = code
        self.message = message
        super().__init__(message)


class ErrorCode:
    """错误码常量"""
    INVALID_INPUT = 1001
    EXTERNAL_API_ERROR = 2001
    INTERNAL_ERROR = 5000


def register_exception_handlers(app: FastAPI) -> None:
    """注册全局异常处理器"""

    @app.exception_handler(SkillException)
    async def skill_exception_handler(request: Request, exc: SkillException):
        logger.warning("Business exception: code=%d, message=%s", exc.code, exc.message)
        return JSONResponse(
            status_code=200,
            content={"code": exc.code, "message": exc.message, "data": None},
        )

    @app.exception_handler(ValidationError)
    async def validation_exception_handler(request: Request, exc: ValidationError):
        logger.warning("Validation error: %s", exc)
        return JSONResponse(
            status_code=422,
            content={
                "code": ErrorCode.INVALID_INPUT,
                "message": "输入参数校验失败",
                "data": exc.errors(),
            },
        )

    @app.exception_handler(Exception)
    async def generic_exception_handler(request: Request, exc: Exception):
        logger.error("Unexpected error", exc_info=exc)
        return JSONResponse(
            status_code=500,
            content={
                "code": ErrorCode.INTERNAL_ERROR,
                "message": "Internal server error",
                "data": None,
            },
        )
```

### 3.5 app/services/skill_service.py（业务逻辑层）

```python
# app/services/skill_service.py
import logging
from app.schemas.request import SkillRequest
from app.schemas.response import SkillResponse
from app.clients.external_api_client import ExternalApiClient
from app.core.exceptions import SkillException, ErrorCode

logger = logging.getLogger(__name__)


class SkillService:
    def __init__(self, client: ExternalApiClient):
        self._client = client

    async def execute(self, request: SkillRequest) -> SkillResponse:
        """执行 Skill 核心逻辑"""
        logger.info("Executing skill, request_id=%s", request.request_id)

        # 业务校验
        if not request.input.strip():
            raise SkillException(ErrorCode.INVALID_INPUT, "input cannot be empty")

        # 调用外部服务
        result = await self._client.process(request.input)

        return SkillResponse(
            request_id=request.request_id,
            output=result,
            status="success",
        )
```

### 3.6 app/clients/external_api_client.py（外部服务客户端）

```python
# app/clients/external_api_client.py
import httpx
import logging
from app.core.config import settings
from app.core.exceptions import SkillException, ErrorCode

logger = logging.getLogger(__name__)


class ExternalApiClient:
    def __init__(self):
        self._client = httpx.AsyncClient(
            base_url=settings.EXTERNAL_API_URL,
            headers={"Authorization": f"Bearer {settings.EXTERNAL_API_KEY}"},
            timeout=settings.EXTERNAL_TIMEOUT_SECONDS,
        )

    async def process(self, input_text: str) -> str:
        try:
            response = await self._client.post(
                "/process",
                json={"input": input_text},
            )
            response.raise_for_status()
            return response.json().get("result", "")
        except httpx.TimeoutException:
            raise SkillException(ErrorCode.EXTERNAL_API_ERROR, "External API timeout")
        except httpx.HTTPStatusError as e:
            logger.error("External API error: status=%d", e.response.status_code)
            raise SkillException(ErrorCode.EXTERNAL_API_ERROR, "External API error")
        except Exception as e:
            logger.error("Unexpected client error", exc_info=e)
            raise SkillException(ErrorCode.EXTERNAL_API_ERROR, "External service unavailable")

    async def aclose(self):
        await self._client.aclose()
```

### 3.7 app/api/v1/endpoints/health.py（健康检查）

```python
# app/api/v1/endpoints/health.py
from fastapi import APIRouter
from pydantic import BaseModel

router = APIRouter()


class HealthResponse(BaseModel):
    status: str
    service: str
    version: str


@router.get("/health", response_model=HealthResponse, tags=["系统"])
async def health_check():
    """健康检查接口，供 Agent 平台探活使用"""
    from app.core.config import settings
    return HealthResponse(
        status="UP",
        service=settings.SKILL_NAME,
        version=settings.SKILL_VERSION,
    )
```

---

## 4. 依赖管理

### pyproject.toml（推荐）

```toml
[project]
name = "my-skill"
version = "1.0.0"
requires-python = ">=3.11"
dependencies = [
    "fastapi>=0.111.0",
    "uvicorn[standard]>=0.29.0",
    "pydantic>=2.7.0",
    "pydantic-settings>=2.3.0",
    "httpx>=0.27.0",
]

[project.optional-dependencies]
dev = [
    "pytest>=8.0.0",
    "pytest-asyncio>=0.23.0",
    "httpx>=0.27.0",   # TestClient 依赖
    "ruff>=0.4.0",
    "mypy>=1.10.0",
]

[tool.ruff]
line-length = 100
target-version = "py311"

[tool.ruff.lint]
select = ["E", "F", "I", "N", "W"]

[tool.pytest.ini_options]
asyncio_mode = "auto"
testpaths = ["tests"]
```

---

## 5. Dockerfile

```dockerfile
FROM python:3.12-slim

WORKDIR /app

# 安装依赖（利用 layer 缓存）
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制应用代码
COPY app/ ./app/

# 创建非 root 用户运行
RUN adduser --disabled-password --no-create-home appuser
USER appuser

EXPOSE 8080

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8080"]
```

---

## 6. 测试示例

```python
# tests/integration/test_skill_api.py
import pytest
from fastapi.testclient import TestClient
from unittest.mock import AsyncMock, patch
from app.main import app

client = TestClient(app)


def test_health_check():
    response = client.get("/api/v1/health")
    assert response.status_code == 200
    assert response.json()["status"] == "UP"


@patch("app.clients.external_api_client.ExternalApiClient.process")
async def test_execute_skill(mock_process):
    mock_process.return_value = "processed result"

    response = client.post("/api/v1/skill/execute", json={
        "request_id": "test-001",
        "input": "Hello",
        "options": {}
    })

    assert response.status_code == 200
    data = response.json()
    assert data["code"] == 0
    assert data["data"]["output"] == "processed result"
```

---

## 7. 代码质量检查清单

- [ ] 所有配置通过 `Settings` 类从环境变量加载，不硬编码
- [ ] 使用 Pydantic `BaseModel` 定义所有请求/响应体
- [ ] 异步接口使用 `async def`，同步操作不阻塞事件循环
- [ ] 外部调用有超时配置和异常捕获
- [ ] 敏感信息不打印到日志
- [ ] 健康检查接口 `/api/v1/health` 可正常响应
- [ ] `pytest` 测试可以在没有外部依赖的情况下运行（使用 mock）
- [ ] `ruff` 代码检查通过，无 lint 错误
