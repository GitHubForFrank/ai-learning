# 01-cicd — 桌面应用 CI/CD

> git-gui 是 JavaFX 桌面应用，无 Web 服务、无 Docker 部署。CI/CD 聚焦于「编译 → 测试 → 打包 → 多平台产物归档」，可选生成原生安装包。
>
> 构建与打包的完整规范参见 [00-spec/shared/07-ops-base.md](../00-spec/shared/07-ops-base.md)。

---

## 流水线阶段

| 阶段 | 命令 | 说明 |
| ---- | ---- | ---- |
| ① 依赖缓存 | `mvn -B dependency:resolve` | Maven 依赖缓存加速后续构建 |
| ② 编译 | `mvn -B compile` | 编译 `app-backend/` 主代码，失败即止 |
| ③ 单元测试 | `mvn -B test` | 运行 JUnit5 + Mockito；TestFX 用 headless 模式（`monocle` + `-Djava.awt.headless=true`） |
| ④ 打包 | `mvn -B package` | 产出 `app-backend/target/git-gui-*.jar`（含依赖打包，按实际选型 shade 或模块化） |
| ⑤ 产物归档 | 平台脚本 | Windows: `app-backend/scripts/git-gui.bat` + jar；Linux/macOS: `app-backend/scripts/git-gui.sh` + jar；归档为 zip/tar.gz |
| ⑥ 原生包（可选） | `jpackage` | 生成 .msi/.deb/.dmg 原生安装包（需对应平台构建机） |

---

## 触发策略

- push 到主分支：跑 ①~⑤ 归档产物
- Pull Request：跑 ①~③ 校验编译与测试
- 手动触发：可勾选 ⑥ 生成原生安装包

---

## 多平台矩阵

| 平台 | 构建机 | 产物 |
| ---- | ------ | ---- |
| Windows | windows-latest + JDK 21 | `git-gui-windows.zip`（jar + bat） |
| Linux | ubuntu-latest + JDK 21 | `git-gui-linux.tar.gz`（jar + sh） |
| macOS | macos-latest + JDK 21 | `git-gui-macos.tar.gz`（jar + sh） |

> JavaFX 跨平台依赖按 OS 分类器拉取（`javafx-controls` / `javafx-graphics` 的 `win` / `linux` / `mac` classifier）。

---

## 与 Web 服务 CI/CD 的差异

| 维度 | Web 服务 | git-gui 桌面应用 |
| ---- | -------- | ---- |
| 部署 | 推送镜像到镜像库 / 服务端口发布 | 无部署，仅归档可执行产物 |
| 端口检查 | 健康检查端口可达 | 无端口 |
| 数据库迁移 | CI/CD 中执行迁移 | 应用启动时 Flyway 自动执行，无需 CI 介入 |
| 产物 | 镜像 / 静态资源 | jar + 启动脚本 / 原生安装包 |

---

## 失败处理

- 任一阶段失败立即终止后续阶段
- 测试失败不归档产物
- 产物按构建号保留，供回滚

---

## 规约引用

- 构建与打包规范：[00-spec/shared/07-ops-base.md](../00-spec/shared/07-ops-base.md)
- 项目依赖与配置：[00-spec/project/07-ops.md](../00-spec/project/07-ops.md)
- 测试策略与门禁：[00-spec/shared/06-quality-base.md](../00-spec/shared/06-quality-base.md)
