"""converter 纯逻辑单测，不依赖 MCP 运行时。"""
from __future__ import annotations

import pytest

from mcp_case_converter.converter import (
    CONVERTERS,
    convert,
    split_words,
    to_camel,
    to_constant,
    to_kebab,
    to_pascal,
    to_snake,
)


@pytest.mark.parametrize(
    "text, expected",
    [
        ("HelloWorld", ["hello", "world"]),
        ("helloWorld", ["hello", "world"]),
        ("hello_world", ["hello", "world"]),
        ("hello-world", ["hello", "world"]),
        ("hello world", ["hello", "world"]),
        ("HELLO_WORLD", ["hello", "world"]),
        ("HTTPServer", ["http", "server"]),
        ("getHTTPResponse", ["get", "http", "response"]),
        ("user2Name", ["user", "2", "name"]),
        ("", []),
    ],
)
def test_split_words(text: str, expected: list[str]) -> None:
    assert split_words(text) == expected


@pytest.mark.parametrize(
    "text, expected",
    [
        ("hello world", "helloWorld"),
        ("user_name_id", "userNameId"),
        ("HTTPServer", "httpServer"),
        ("", ""),
    ],
)
def test_to_camel(text: str, expected: str) -> None:
    assert to_camel(text) == expected


@pytest.mark.parametrize(
    "text, expected",
    [
        ("hello world", "HelloWorld"),
        ("user_name", "UserName"),
        ("http server", "HttpServer"),
    ],
)
def test_to_pascal(text: str, expected: str) -> None:
    assert to_pascal(text) == expected


@pytest.mark.parametrize(
    "text, expected",
    [
        ("HelloWorld", "hello_world"),
        ("helloWorld", "hello_world"),
        ("HELLO_WORLD", "hello_world"),
        ("HTTPServer", "http_server"),
    ],
)
def test_to_snake(text: str, expected: str) -> None:
    assert to_snake(text) == expected


@pytest.mark.parametrize(
    "text, expected",
    [
        ("HelloWorld", "hello-world"),
        ("user_name", "user-name"),
        ("My Component", "my-component"),
    ],
)
def test_to_kebab(text: str, expected: str) -> None:
    assert to_kebab(text) == expected


@pytest.mark.parametrize(
    "text, expected",
    [
        ("pageSize", "PAGE_SIZE"),
        ("hello-world", "HELLO_WORLD"),
        ("HelloWorld", "HELLO_WORLD"),
    ],
)
def test_to_constant(text: str, expected: str) -> None:
    assert to_constant(text) == expected


def test_convert_dispatch() -> None:
    assert convert("Hello", "upper") == "HELLO"
    assert convert("Hello", "lower") == "hello"
    assert convert("hello_world", "camel") == "helloWorld"


def test_convert_unknown_mode() -> None:
    with pytest.raises(ValueError, match="未知模式"):
        convert("hello", "not_a_mode")


def test_all_modes_registered() -> None:
    # 回归保护：任何一种模式缺失都会被发现
    expected = {
        "upper", "lower", "swap", "title", "capitalize",
        "camel", "pascal", "snake", "kebab", "constant",
    }
    assert set(CONVERTERS) == expected
