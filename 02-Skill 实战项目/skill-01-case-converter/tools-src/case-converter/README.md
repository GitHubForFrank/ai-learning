# tools-src/case-converter

`case-converter` skill 底层 jar 的 Maven 工程。本目录**不参与 skill 交付**；只有构建产物 `case-converter.jar` 会被拷进 `../../case-converter/tools/`。

## 前置依赖

- JDK 21+（当前 LTS）
- Maven 3.8+

## 构建 & 发布流程

```bash
# 1. 在本目录下构建
cd tools-src/case-converter
mvn -q clean package

# 2. 构建产物：target/case-converter.jar（fat jar，含 Main-Class）

# 3. 拷贝到 skill 的 tools/ 目录
cp target/case-converter.jar ../../case-converter/tools/case-converter.jar

# 4. 按 skill_develop_rule.md 的交付清单走：
#    - 升 .meta/VERSION（§7）
#    - 更新最新 PLAN 的"变更点"（§4）
#    - 更新 README / SKILL.md（§1.2 三者同步）
#    - 走 §2 交付前检查清单
```

## 运行测试

```bash
mvn -q test
```

## 版本约定

- `pom.xml` 的 `<version>` 与 `case-converter/.meta/VERSION` **保持同步**。改一个要改另一个。
- jar 改动即使是 bug 修复，也要同步 skill 端 VERSION（至少升 PATCH）。

## 依赖

运行时：零第三方依赖（只用 JDK 标准库）。
测试：JUnit 5。
