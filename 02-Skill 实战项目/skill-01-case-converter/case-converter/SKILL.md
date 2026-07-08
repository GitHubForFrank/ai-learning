---
name: case-converter
description: >
  字符串大小写切换工具（upper / lower / swap 三种模式），底层由 Java jar 实现。
  触发条件：用户明确给出一段文本并要求做大小写转换（全转大写 / 全转小写 / 逐字符翻转），期望脚本化稳定处理。
  不触发：口头询问"某个单词大写是啥"之类的一次性问答、非 ASCII 复杂语言大小写规则（土耳其语 I、德语 ß 等）场景。
allowed-tools: Bash
---

# case-converter

最小 **Java-capability skill** 示例：skill 通过 `scripts/switch_case.py` 调用 `tools/case-converter.jar` 完成字符串大小写切换。

## 何时使用

用户给出文本 + 明确模式：

- `upper` —— 全部转大写
- `lower` —— 全部转小写
- `swap` —— 逐字符翻转大小写（`Hello` → `hELLO`）

## 前置依赖

- Java 21+（`java -jar` 可用）
- Python 3.10+（wrapper 脚本）

检查命令：

```bash
java -version
py --version
```

## 参数

| 参数 | 强度 | 说明 |
|---|---|---|
| `mode` | 必填 | `upper` / `lower` / `swap` |
| `text` | 可选 | 待转换字符串；省略则从 stdin 读取 |

## 工作流

1. 解析用户意图 → 提取 `mode` 与 `text`
2. 调用：
   ```bash
   py scripts/switch_case.py <mode> "<text>"
   ```
3. 将脚本 stdout 作为最终结果返回给用户

若 `tools/case-converter.jar` 不存在，脚本会报错并提示构建流程。遇到时提示用户按 `tools-src/case-converter/README.md` 的步骤先 `mvn package`。

## 输出

- 成功：stdout 一行转换结果
- 失败：stderr 错误信息 + exit code 非 0
