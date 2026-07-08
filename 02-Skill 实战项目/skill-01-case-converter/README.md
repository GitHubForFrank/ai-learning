# skill-01-case-converter

> **Java-capability skill** 的最小参考实现。演示 `skill_develop_rule.md §3` 中
> "Java 源码放 `tools-src/`、预构建 jar 放 `<skill>/tools/`" 的布局模式。

功能极简：字符串大小写切换（`upper` / `lower` / `swap`）。重点不是能力，而是**目录布局**和**构建 / 发布流程**。

## 目录结构

```
skill-01-case-converter/
├── case-converter/            # 📦 Skill 交付单元（可直接被 Claude 加载）
│   ├── .meta/VERSION
│   ├── SKILL.md
│   ├── README.md
│   ├── scripts/switch_case.py   # Python wrapper，调 java -jar
│   └── tools/case-converter.jar    # 预构建 fat jar（运行时实际用的）
│
├── development/                 # 📝 开发过程资料（不参与交付）
│   └── plans/PLAN_v01.md        # 设计蓝图
│
└── tools-src/                   # 🛠️ 自研 artifact 源码（不参与交付）
    └── case-converter/             # 每个 artifact 一个 Maven 子项目
        ├── pom.xml
        ├── src/main/java/…
        └── src/test/java/…
```

**关键原则**：Skill 本身是**运行单元**而非构建单元 —— 交付目录里不放 `.java` 源码，用户不装 JDK + Maven 也能跑（只要有 JRE）。Java 源码留在 `tools-src/` 外侧，构建产物拷贝进 `case-converter/tools/`。

## 前置依赖

**使用 skill**：Java 21+（JRE 即可）、Python 3.10+
**构建 jar**：JDK 21+、Maven 3.8+

## 快速上手

### 1. 构建 jar（首次 / jar 更新时）

```bash
cd tools-src/case-converter
mvn -q clean package
cp target/case-converter.jar ../../case-converter/tools/case-converter.jar
```

### 2. 运行

```bash
cd ../../case-converter
py scripts/switch_case.py swap "Hello World"
# hELLO wORLD
```

详见 `case-converter/README.md`。

## 新增 / 修改时的交付流程

按 `skill_develop_rule.md §2` 交付前检查清单走：

1. 改源码（`tools-src/case-converter/src/...`）→ `mvn test` 通过
2. `mvn -q clean package` 重新打包
3. 拷贝 jar 到 `case-converter/tools/`
4. 升 `case-converter/.meta/VERSION` 与 `tools-src/case-converter/pom.xml` 的 `<version>`（保持一致）
5. 更新最新 PLAN 的"变更点"段
6. 按需更新 SKILL.md / README.md
