"""CLI 入口：一次性调用 converter 引擎，结果写 stdout。

四个子命令一一对应 MCP 的四个 tool：

  case-converter convert <text> <mode>   # 单次转换
  case-converter batch   <text>          # 10 种风格一次出（JSON）
  case-converter split   <text>          # 切词成小写单词列表（JSON）
  case-converter list-modes              # 列出所有模式与示例
"""
from __future__ import annotations

import argparse
import json
import sys

from ..converter import CONVERTERS, MODE_DESCRIPTIONS, convert, split_words


def main() -> int:
    parser = argparse.ArgumentParser(
        prog="case-converter",
        description="字符串大小写与命名风格转换 CLI",
    )
    sub = parser.add_subparsers(dest="cmd", required=True, metavar="<command>")

    p_conv = sub.add_parser("convert", help="按指定模式转换一次")
    p_conv.add_argument("text", help="待转换的字符串")
    p_conv.add_argument("mode", choices=list(CONVERTERS), help="转换模式")

    p_batch = sub.add_parser("batch", help="输出所有 10 种风格（JSON）")
    p_batch.add_argument("text", help="待转换的字符串")

    p_split = sub.add_parser("split", help="切词成小写单词列表（JSON）")
    p_split.add_argument("text", help="任意风格命名的字符串")

    sub.add_parser("list-modes", help="列出所有模式与示例")

    args = parser.parse_args()

    if args.cmd == "convert":
        print(convert(args.text, args.mode))
    elif args.cmd == "batch":
        result = {m: fn(args.text) for m, fn in CONVERTERS.items()}
        print(json.dumps(result, ensure_ascii=False, indent=2))
    elif args.cmd == "split":
        print(json.dumps(split_words(args.text), ensure_ascii=False))
    elif args.cmd == "list-modes":
        for mode, desc in MODE_DESCRIPTIONS.items():
            print(f"{mode:<12s}{desc}")

    return 0


if __name__ == "__main__":
    sys.exit(main())
