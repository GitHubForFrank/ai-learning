"""纯字符串转换逻辑，不依赖 MCP，便于单测与复用。"""
from __future__ import annotations

import re
from typing import Callable

# 切词正则：按顺序匹配
#   [A-Z]+(?=[A-Z][a-z])  —— 连续大写后紧跟 "大写+小写"，切出首字母缩写词，如 HTTPServer -> HTTP | Server
#   [A-Z]?[a-z]+          —— 可选首字母大写 + 若干小写，如 camelCase -> camel | Case
#   [A-Z]+                —— 纯大写单词（CONSTANT_CASE 中各段）
#   \d+                   —— 连续数字
_WORD_RE = re.compile(
    r"[A-Z]+(?=[A-Z][a-z])|[A-Z]?[a-z]+|[A-Z]+|\d+"
)


def split_words(text: str) -> list[str]:
    """把任意风格命名切成小写单词列表。

    支持分隔符：空格、下划线、连字符、点；
    也支持 camelCase / PascalCase / 首字母缩写（HTTPServer）边界识别。
    """
    return [m.group(0).lower() for m in _WORD_RE.finditer(text)]


# --- 基础大小写 ---

def to_upper(text: str) -> str:
    """全大写：hello World -> HELLO WORLD"""
    return text.upper()


def to_lower(text: str) -> str:
    """全小写：Hello World -> hello world"""
    return text.lower()


def swap_case(text: str) -> str:
    """大小写互换：Hello -> hELLO"""
    return text.swapcase()


def to_title(text: str) -> str:
    """标题风格：每个单词首字母大写（保留原分隔符）。hello world -> Hello World"""
    return text.title()


def capitalize(text: str) -> str:
    """首字母大写，其余小写：hello WORLD -> Hello world"""
    return text.capitalize()


# --- 编程命名风格（会先切词再重组） ---

def to_camel(text: str) -> str:
    """camelCase：首词小写，其余词首字母大写。"""
    words = split_words(text)
    if not words:
        return ""
    return words[0] + "".join(w.capitalize() for w in words[1:])


def to_pascal(text: str) -> str:
    """PascalCase：每个词首字母大写。"""
    return "".join(w.capitalize() for w in split_words(text))


def to_snake(text: str) -> str:
    """snake_case：小写 + 下划线分隔。"""
    return "_".join(split_words(text))


def to_kebab(text: str) -> str:
    """kebab-case：小写 + 连字符分隔。"""
    return "-".join(split_words(text))


def to_constant(text: str) -> str:
    """CONSTANT_CASE / SCREAMING_SNAKE_CASE：大写 + 下划线。"""
    return "_".join(w.upper() for w in split_words(text))


# --- 统一分发表 ---

Converter = Callable[[str], str]

CONVERTERS: dict[str, Converter] = {
    "upper": to_upper,
    "lower": to_lower,
    "swap": swap_case,
    "title": to_title,
    "capitalize": capitalize,
    "camel": to_camel,
    "pascal": to_pascal,
    "snake": to_snake,
    "kebab": to_kebab,
    "constant": to_constant,
}


MODE_DESCRIPTIONS: dict[str, str] = {
    "upper": "全部大写：Hello World -> HELLO WORLD",
    "lower": "全部小写：Hello World -> hello world",
    "swap": "大小写互换：Hello -> hELLO",
    "title": "单词首字母大写（保留分隔符）：hello world -> Hello World",
    "capitalize": "整串首字母大写其余小写：hello WORLD -> Hello world",
    "camel": "camelCase：helloWorld",
    "pascal": "PascalCase：HelloWorld",
    "snake": "snake_case：hello_world",
    "kebab": "kebab-case：hello-world",
    "constant": "CONSTANT_CASE：HELLO_WORLD",
}


def convert(text: str, mode: str) -> str:
    """按指定 mode 转换；mode 非法时抛 ValueError。"""
    fn = CONVERTERS.get(mode)
    if fn is None:
        raise ValueError(
            f"未知模式 '{mode}'。可用模式：{', '.join(CONVERTERS)}"
        )
    return fn(text)
