# workitem-frontend:前端代码仓(Frontend Repo)

> **仓库角色**:用 Vue 3 + Vite + TypeScript 实现 `workitem-docs` 的用户可见行为(USF-*),承载 FE-01 ~ FE-06 模块。
>
> **Implements**: `spec@vTBD`(P3 阶段发布首个 spec tag 后填入)
>
> **当前阶段**:✅ **已完成(Task001)**

## 一、本仓职责

- 实现 `workitem-docs/specs/02-功能规范.md §7` 定义的 5 个路由和页面行为
- 承担 §8.1 的 FE-01 ~ FE-06 模块
- 承担 F-06 统一错误提示(通过 axios 拦截器)
- 遵守 `workitem-docs/conventions/fe-conventions.md` 的目录与命名规则
- 遵守 `workitem-docs/conventions/test-conventions.md` 对 TC-FE-* / TC-E2E-* 的约束

## 二、本仓不做

- ❌ 任何后端业务逻辑 —— 由 `workitem-backend` 提供 API
- ❌ 任何 DB 直连 —— 所有数据都经 `workitem-backend` 接口
- ❌ 业务校验规则的自创 —— 必须与 `workitem-docs` 一致(发现不合理去提 spec PR)
- ❌ UI 组件库 / Pinia / SSR(详见 `workitem-docs/specs/01-需求分析.md §6`)

## 三、目录结构(P3 阶段产出,当前占位)

```
workitem-frontend/
├── README.md                   # 本文件
├── .gitignore                  # Node/Vite 专属忽略(node_modules/、dist/、.env* 等)
├── package.json                # [P3]
├── vite.config.ts              # [P3] /api 代理到 http://localhost:8080
├── tsconfig.json               # [P3]
├── index.html                  # [P3]
├── src/                        # [P3] 详见 fe-conventions §1
│   ├── main.ts
│   ├── App.vue
│   ├── router/
│   ├── api/
│   ├── views/
│   ├── components/
│   ├── types/
│   └── utils/http.ts
└── tests/
    └── e2e/                    # [P4] Playwright 用例
```

## 四、与 spec 的对齐义务

1. 本 README 顶部 `Implements: spec@vX.Y.Z` **必须**保持与主干代码一致
2. **所有 PR 标题以 `Task<NNN>:` 起头**(Task 编号见 `workitem-docs/specs/06-任务与发布管理.md §2`)
3. PR 描述**必须**包含 `## Task` / `## Implements` / `## Covers` 三段(模板见 `workitem-docs/specs/05-跨仓协调.md §3.2`)
4. 代码关键位置**必须**双标签注释:`// [SDD-TASK: Task<NNN>][SDD-SPEC: ...]`
5. 引入 feature flag 命名为 `feat_task<NNN>_<短语>`(见 `06 §5`)
6. `src/types/*.ts` 是与 `workitem-backend` 契约的边界,**必须**与 spec §2 数据字典一一对应

## 五、启动与验证

### 5.1 前置

- Node.js 18+(建议 LTS)
- `workitem-backend` 已启动(`http://localhost:10197/app`),否则前端登录会失败

### 5.2 安装依赖

```bash
cd "04-SDD 实战项目/sdd-01-workitem-fullstack/workitem-frontend"
npm install
```

**Verify**:`node_modules/` 创建成功,无 ERR;`package-lock.json` 已生成。

### 5.3 启动 dev server

```bash
cd "04-SDD 实战项目/sdd-01-workitem-fullstack/workitem-frontend"
npm run dev
```

**Verify**:控制台显示 `Local: http://localhost:5173/`;浏览器访问 `http://localhost:5173/`,无 token 时自动跳 `/login`。
默认账号 `admin` / 默认密码 `12346#@&`(Task001 教学用)。

### 5.4 类型检查

```bash
cd "04-SDD 实战项目/sdd-01-workitem-fullstack/workitem-frontend"
npm run type-check
```

**Verify**:退出码 0,无类型错误。

## 六、本仓涉及的 spec 条款速查

| spec 条款 | 本仓实现位置(P3 产出) |
|---|---|
| §7.1 路由 | `src/router/index.ts` |
| §7.2 页面行为 | `src/views/*.vue` |
| §7.3 F-06 拦截器 | `src/utils/http.ts` |
| §8 FE-01 ~ FE-06 | 各 view / api / utils 文件 |
| `conventions/fe-conventions.md` | 整个 `src/` 结构 |

---

**占位状态**:本 README 在 P1 阶段仅声明职责与对齐规则。P3 阶段补齐完整目录、package.json、启动命令与 verify 步骤。
