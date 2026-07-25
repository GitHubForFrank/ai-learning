# 00-spec/ — SDD 规范体系

> 本目录包含完整的 SDD 规范体系，分为两个子目录：
>
> - `shared/`：跨项目共享的通用方法论（SDD 工作流、DDD 分层、测试策略、桌面打包等）
> - `project/`：git-gui 项目专属规范（BR 规则、服务契约、表结构、JavaFX UI 等）

---

## 目录结构

```plaintext
00-spec/
  README.md               # 本文档
  shared/                  # 通用基座（可复制到其他项目直接使用）
    README.md              # 共享规范索引 + 术语表
    00-core-base.md        # SDD 工作流、Task 编号、文档管理、门禁、安全
    03-backend-base.md     # DDD 分层、包结构、UI 层/Guice 适配器规范
    04-database-base.md    # 迁移脚本命名、索引策略、Flyway 自动执行
    05-frontend-base.md    # JavaFX UI 层：FXML/Controller/ViewModel、CSS 主题、国际化
    06-quality-base.md     # 测试策略、BR 覆盖、门禁、日志
    07-ops-base.md         # 桌面打包流程、单实例锁、用户数据目录
  project/                 # git-gui 专属扩展
    README.md              # 项目规范索引 + 术语表
    project.yml            # 集中参数化（数据目录/DB 路径/日志路径/锁文件/Git 路径/包名）
    00-core.md             # 桌面配置 + 技术选型禁止清单
    01-domain.md           # BR-01~BR-42 业务规则 + 7 实体字典
    02-api.md              # 11 服务接口契约 + 错误码注册表（非 HTTP）
    03-backend.md          # 桌面 DDD、JGit/CLI 适配器、命令红线拦截器、异步任务、Guice
    04-database.md         # SQLite + 7 张表 + V1~V7 迁移历史
    05-frontend.md         # JavaFX 21 技术栈 + 主窗口 + 对话框清单
    06-quality.md          # 应用内状态面板 + 桌面性能 SLO
    07-ops.md              # 依赖选型 + 配置项 + 资源限制
```

---

## 快速开始

### 新项目接入

1. 复制 `shared/` 目录到新项目的规范目录
2. 参考 `project/` 目录结构，新建项目专属规范文件（注意替换 git-gui 专属内容）

### 阅读顺序

1. 先读 [shared/README.md](./shared/README.md) — 了解编号体系、分组、术语
2. 再读 [shared/00-core-base.md](./shared/00-core-base.md) — 理解 SDD 工作流和全局规则
3. 然后读 [project/01-domain.md](./project/01-domain.md) — 理解业务规则（BR-01~BR-42）
4. 按需查阅其他文件
