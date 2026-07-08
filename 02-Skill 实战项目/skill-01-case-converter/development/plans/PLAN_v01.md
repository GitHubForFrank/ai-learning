# PLAN v01 — case-converter

> 首版蓝图。v01 不写"相对 v(NN-1) 的变更点"。

## 0. 目标与适用场景

演示 **Java-capability skill** 的最小可行形态，作为 `skill_develop_rule.md §3` 中"Java 变体"的参考实现：

- Java 源码留在 `tools-src/case-converter/`，Maven 构建 fat jar
- 构建产物 `case-converter.jar` 拷入 `case-converter/tools/`
- Python wrapper `scripts/switch_case.py` 调 `java -jar`
- Skill 本体不包含 `.java` 源码、不需要 Gradle/Maven 即可被加载使用

功能故意极简：字符串大小写切换。目的是展示**目录布局**和**构建&发布流程**，而非能力复杂度。

## 1. 输入契约

| 参数 | 强度 | 取值 |
|---|---|---|
| `mode` | 必填 | `upper` / `lower` / `swap` |
| `text` | 可选 | 待转换字符串；省略则从 stdin 读取 |

## 2. 输出契约

- stdout：一行，转换结果（无尾随换行溢出）
- exit code：`0` 成功；`2` 参数错误；`1` IO 错误

## 3. 目录结构

```
skill-01-case-converter/
├── case-converter/                    # 📦 skill 交付单元
│   ├── .meta/VERSION                    # 0.1.0
│   ├── SKILL.md
│   ├── README.md
│   ├── scripts/switch_case.py           # Python wrapper
│   └── tools/
│       ├── README.md                    # JDK 要求 + 构建指引
│       └── case-converter.jar              # 预构建产物
├── development/                         # 📝 开发过程资料（不交付）
│   └── plans/PLAN_v01.md
└── tools-src/                           # 🛠️ 自研 artifact 源码（不交付）
    └── case-converter/
        ├── pom.xml
        ├── src/main/java/com/example/caseconverter/Main.java
        ├── src/test/java/com/example/caseconverter/MainTest.java
        └── README.md
```

省略的可选目录：`knowledge/`（无数据驱动规则）、`templates/`（无结构化输出模板）、`samples/`（不分析输入文件）。

## 4. 阶段拆解

skill 侧单一阶段：wrapper → jar。无需多阶段管线。

jar 侧同样单一职责：读参数 / stdin → 分派三个 mode → 打印结果。

## 5. 依赖清单

**运行时**：
- JDK 21+（当前 LTS）
- Python 3.10+

**构建时**（仅开发者需要）：
- Maven 3.8+
- JUnit 5（测试）

**第三方运行时依赖**：无。

## 6. 已知陷阱

首版无。

## 7. 测试用例

v01 写"计划验证什么"：

- `upper`: `Hello` → `HELLO`
- `lower`: `Hello` → `hello`
- `swap`: `Hello World` → `hELLO wORLD`
- `swap`: `AbC 123 !@#` → `aBc 123 !@#`（保留非字母字符）
- 空字符串: 任意 mode 均返回空
- stdin 模式: `echo "foo" | ... lower` → `foo`
- 非法 mode: 退出码 2，stderr 有帮助信息
- `case-converter.jar` 缺失: Python wrapper 给出构建提示后 SystemExit

v02 起替换为实际用例清单与回归结果。
