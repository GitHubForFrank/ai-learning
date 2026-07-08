---
id: Task001
title: 添加最小登录(账号密码 + JWT + 失败 3 次锁定 + 登录日志 + traceparent)
status: active
owner: <TBD>
depends-on: []
spec-version-target: v1.0.0
expand-contract-pair: null
created: 2026-04-25
closed: null
---

## Why

P1 阶段 spec 完成后,首个端到端 Task,把 4 仓 + 鉴权 / 监控 / 安全 / 回归 / 环境晋级所有维度串通,作为 SDD 多仓方法论的可执行参考实现。

## Scope

- **仓**: workitem-docs / workitem-backend / workitem-frontend / workitem-db
- **影响编号**:
  - 新 BR:BR-09 ~ BR-15(7 条)
  - 新 API:API-06 `/api/login`、API-07 `/api/me`
  - 新模块:FE-07/FE-08、BE-06/07/08/09/10、DB-04/DB-05/DB-06
  - 新错误码:`3001 AUTH_INVALID_CREDENTIAL` / `3002 AUTH_LOCKED` / `3003 AUTH_REQUIRED`
  - 新路由:`/login`(前端)
  - 修改 §1.5 通用请求头(traceparent / Authorization 必填);§1.6 鉴权约定;§3 接口表加鉴权列;§6 追溯矩阵全更新(API-01~07)
  - `01 §6` 范围边界:移除"❌ 用户系统/鉴权",改为"✅ 最小登录"
- **DDL**:
  - `V2__task001_init_app_user_table.sql`(additive)
  - `V3__task001_init_login_log_table.sql`(additive)
  - `V4__task001_seed_default_admin.sql`(数据 INSERT,additive)
- **Feature flag**:`feat_task001_login`(SIT 默认 ON;UAT/PROD-mirror/PROD 由 release plan 逐步开)
- **新增技术依赖**(`03 §1.1`):Spring Security 6.4+(随 SB 4.0.5)、Nimbus JOSE JWT、BCryptPasswordEncoder

## Out of Scope

- ❌ 注册 / 重置密码 / 多用户管理后台(后续 Task)
- ❌ 角色权限 RBAC(后续 Task)
- ❌ Refresh token(后续 Task)
- ❌ 现有 task BR(BR-01~08)语义变化 —— 仅在前面加 JWT 拦截,业务层无感

## Regression

- **必跑用例(合入前)**:
  - 全量 TC-R-API-01 ~ TC-R-API-08(其中 06/07/08 是本 Task 新增)
  - 全量 TC-R-DB-01 ~ TC-R-DB-14(其中 11/12/13/14 是本 Task 新增)
- **新增用例**:
  - TC-U-09 ~ TC-U-15(7 条 Service 单测)
  - TC-IT-14 ~ TC-IT-20(7 条集测)
  - TC-FE-07 ~ TC-FE-09(3 条前端组件测试)
  - TC-E2E-09 ~ TC-E2E-12(4 条 E2E,含 MCP 后台断言)
  - TC-R-API-06/07/08
  - TC-R-DB-11/12/13/14
- **有 DDL 时必配**:
  - TC-R-MIG-02(`V2` app_user 表)
  - TC-R-MIG-03(`V3` login_log 表)
  - TC-R-MIG-04(`V4` admin 种子)
- **MCP 查询审计**:本 Task 上线后 TC-E2E-11 / 12 调用 `sdd-db-probe` 的 `query_by_condition(login_log)` / `count_by_condition(login_log)`;具体 query_id 在 P2/P4 执行时回填

## PR Trail (合入时回填)

- workitem-docs PR:    <link to spec PR — 含本 md + 02/03/04/01 + specs/security §13 + monitoring §5.2 等更新>
- workitem-db PR:      <link — V2/V3/V4 三份脚本 + TC-R-MIG-02/03/04 跨仓触发>
- workitem-backend PR: <link — DDD 分层落 BE-06~10 + 全部测试用例>
- workitem-frontend PR:<link — FE-07/08 + LoginView + http.ts 拦截器扩展>

## Closure (PROD 上线后回填)

- spec tag bumped to: v1.0.0(首个正式版)
- PROD 上线日期: <TBD>
- Lessons: <TBD;预期记录:JWT 密钥 rotation 流程 / login_log 容量评估 / traceparent 客户端补全率>
