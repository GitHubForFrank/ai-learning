# case-converter

> 字符串大小写切换的最小 skill 实现，演示 **Java-capability skill** 的目录布局：
> Java 源码留在 `../tools-src/`，skill 只装预构建的 `case-converter.jar` + Python 薄包装。

## 何时使用

用户给定一段文本，要求做大小写转换：

- `upper` → 全大写
- `lower` → 全小写
- `swap` → 逐字符翻转（`Hello` → `hELLO`）

## 前置依赖

- Java 21+
- Python 3.10+

## 快速上手

```bash
# 确保 tools/case-converter.jar 已就位（未构建见下文）
py scripts/switch_case.py swap "Hello World"
# hELLO wORLD

py scripts/switch_case.py upper "hello"
# HELLO

echo "MixED CasE" | py scripts/switch_case.py lower
# mixed case
```

## 目录结构

```
case-converter/                        # skill 交付单元
├── .meta/VERSION                        # 语义化版本号
├── SKILL.md                             # Agent Skill 入口
├── README.md                            # 本文件
├── scripts/
│   └── switch_case.py                   # Python wrapper，调用 java -jar
└── tools/
    ├── README.md                        # JDK 要求 + 构建指引
    └── case-converter.jar                  # 预构建产物（源码在 ../tools-src/）
```

## 参数

| 参数 | 强度 | 取值 |
|---|---|---|
| `mode` | 必填 | `upper` / `lower` / `swap` |
| `text` | 可选 | 待转换字符串；省略则从 stdin 读 |

## 若 `tools/case-converter.jar` 不存在

去源码目录构建：

```bash
cd ../tools-src/case-converter
mvn -q clean package
cp target/case-converter.jar ../../case-converter/tools/case-converter.jar
```

详见 `../tools-src/case-converter/README.md`。

## 已知限制

- 只处理 ASCII 大小写规则；特殊语言（土耳其语点 `İ/i`、德语 `ß`）的 locale-sensitive 行为交给 JDK 默认 locale，可能与用户预期不一致
- stdin 模式按行读取，最终输出去掉尾部多余换行
