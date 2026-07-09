"""mcp_case_converter 引擎的传输层入口子包。

本目录下每个模块暴露一个 `main()` 函数，映射到 pyproject.toml 的
`[project.scripts]` 条目；**引擎代码在父 package 里**，本目录下的文件
都是薄适配层，负责把某种传输协议的调用模型翻译为对引擎的调用。

  cli.py   —— 基于 argparse 的 CLI（一次性运行，跑完即退出）
  mcp.py   —— MCP Server（stdio / streamable HTTP），常驻等调用

将来需要新传输层（HTTP / gRPC / ...）时，新建同目录的 `<name>.py`，
并在 pyproject.toml 的 `[project.scripts]` 注册一行即可。
"""
