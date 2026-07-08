# tools/

本目录存放 skill 运行时需要的预构建二进制。

## `case-converter.jar`

- **用途**：字符串大小写切换 CLI；由 `scripts/switch_case.py` 调用
- **来源**：本仓库自研，源码在 `../../tools-src/case-converter/`
- **构建**：见 `../../tools-src/case-converter/README.md`
- **运行时依赖**：JDK 21+（`java -jar` 可用）

若本目录下缺失 `case-converter.jar`，`scripts/switch_case.py` 会打印构建提示后退出。
