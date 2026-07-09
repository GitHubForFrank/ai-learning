"""MCP Server 入口：把 converter 引擎包装为 FastMCP tool，支持 stdio / Streamable HTTP 双传输。"""
from __future__ import annotations

import argparse
import asyncio
import logging
import sys
from typing import Annotated, Literal

from mcp.server.fastmcp import FastMCP
from mcp.server.fastmcp.exceptions import ToolError
from pydantic import Field

from ..converter import CONVERTERS, MODE_DESCRIPTIONS, convert, split_words

mcp = FastMCP("case-converter")

Mode = Literal[
    "upper", "lower", "swap", "title", "capitalize",
    "camel", "pascal", "snake", "kebab", "constant",
]


@mcp.tool()
def convert_case(
    text: Annotated[str, Field(description="待转换的原始字符串")],
    mode: Annotated[Mode, Field(description="转换模式，见 list_modes")],
) -> str:
    """把字符串按指定命名风格转换。

    典型场景：
    - 重命名变量：user_name -> userName（camel）
    - 生成常量名：pageSize -> PAGE_SIZE（constant）
    - 文件名规整：My Component.tsx -> my-component.tsx（kebab）

    Args:
        text: 原始字符串。
        mode: 转换模式，10 选 1。调用 list_modes 查看所有模式与示例。
    """
    try:
        return convert(text, mode)
    except ValueError as e:
        raise ToolError(str(e)) from e


@mcp.tool()
def batch_convert(
    text: Annotated[str, Field(description="待转换的原始字符串")],
) -> dict[str, str]:
    """一次性返回所有 10 种命名风格的转换结果，便于对比选择。

    返回 dict，key 为模式名，value 为对应转换后的字符串。
    """
    return {mode: fn(text) for mode, fn in CONVERTERS.items()}


@mcp.tool()
def split_to_words(
    text: Annotated[str, Field(description="任意风格命名的字符串")],
) -> list[str]:
    """把输入切成小写单词列表。

    支持 camelCase / PascalCase / snake_case / kebab-case / 空格分隔
    以及首字母缩写识别（HTTPServer -> [http, server]）。
    常用于在写自定义逻辑前"先分词再重组"。
    """
    return split_words(text)


@mcp.tool()
def list_modes() -> dict[str, str]:
    """列出所有可用转换模式及示例，供 LLM 选择合适模式。"""
    return dict(MODE_DESCRIPTIONS)


def main() -> None:
    # 日志写 stderr，避免污染 stdio 协议通道
    logging.basicConfig(
        stream=sys.stderr,
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(name)s - %(message)s",
    )

    parser = argparse.ArgumentParser(
        description="MCP Server: 字符串大小写与命名风格转换"
    )
    parser.add_argument(
        "--transport",
        choices=["stdio", "http"],
        default="stdio",
        help="传输方式，默认 stdio",
    )
    parser.add_argument(
        "--host", default="127.0.0.1", help="HTTP 模式监听地址"
    )
    parser.add_argument(
        "--port", type=int, default=8000, help="HTTP 模式监听端口"
    )
    args = parser.parse_args()

    if args.transport == "stdio":
        asyncio.run(mcp.run_stdio_async())
    else:
        asyncio.run(
            mcp.run_streamable_http_async(host=args.host, port=args.port)
        )


if __name__ == "__main__":
    main()
