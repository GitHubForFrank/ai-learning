"""python -m mcp_case_converter 默认入口：CLI（一次性运行）。

MCP Server 启动走 `mcp-case-converter` 脚本或 `python -m mcp_case_converter.entrypoints.mcp`。
"""
from __future__ import annotations

import sys

from .entrypoints.cli import main


if __name__ == "__main__":
    sys.exit(main())
