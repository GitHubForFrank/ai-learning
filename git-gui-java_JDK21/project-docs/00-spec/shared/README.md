# shared/ — SDD 通用规范基座

> 本目录包含跨项目共享的通用方法论。各项目在同级 `project/` 目录中继承本目录规范，并追加项目专属内容。
>
> 编号结构与项目 spec 保持一致：`{XX}-{topic}.md`，前缀 0X~7X。
> 编号跳号说明：01-domain-base.md、02-api-base.md 暂未定义，预留供后续扩展，不影响现有规范体系。

---

## 文件索引

| 编号 | 文件 | 内容 |
| ------ | ------ | ------ |
| 00 | [00-core-base.md](./00-core-base.md) | SDD 工作流、Task 编号、代码追溯、文档管理、调试门禁、安全约束、Spec 间引用规范 |
| 03 | [03-backend-base.md](./03-backend-base.md) | DDD 分层架构、包结构模式、UI 层/Guice 适配器规范、Model/Repository/Service 规范、框架兼容性 |
| 04 | [04-database-base.md](./04-database-base.md) | 迁移脚本命名规则、执行方式、头部注释格式、索引策略、禁用清单 |
| 05 | [05-frontend-base.md](./05-frontend-base.md) | JavaFX UI 层：FXML/Controller/ViewModel、CSS 主题、ResourceBundle 国际化、EventBus |
| 06 | [06-quality-base.md](./06-quality-base.md) | 测试策略、BR 覆盖要求、Given-When-Then 模式、Mock 策略、代码门禁、日志规范 |
| 07 | [07-ops-base.md](./07-ops-base.md) | 版本升级策略、桌面打包流程、单实例锁、用户数据目录、CI/CD 流程 |

---

## 规则优先级

项目级 spec 与 shared 冲突时，**项目级 spec 优先**。

继承关系：`shared/xx-base.md` → `project/xx.md`

---

## 术语表

| 术语 | 说明 |
| ------ | ------ |
| SDD | Specification-Driven Development，规约驱动开发 |
| BR | Business Rule，业务规则。格式 `BR-{NN}`，两位数递增，定义在项目 `01-domain.md` |
| DDD | Domain-Driven Design，领域驱动设计 |
| V 号 | 迁移脚本版本号，格式 `V{NN}`，递增不复用 |
| Spec 间引用 | spec 文件之间的交叉引用，格式：`参见 [{file} §{section}]` |
