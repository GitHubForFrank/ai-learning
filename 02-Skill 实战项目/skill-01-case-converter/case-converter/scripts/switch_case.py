"""字符串大小写切换 —— 调用 tools/case-converter.jar 完成实际转换。"""
from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path

SKILL_ROOT = Path(__file__).resolve().parent.parent
JAR_PATH = SKILL_ROOT / "tools" / "case-converter.jar"


def run(mode: str, text: str | None) -> str:
    if not JAR_PATH.exists():
        raise SystemExit(
            f"未找到 {JAR_PATH}\n"
            f"请先在 tools-src/case-converter/ 下构建：\n"
            f"  cd ../tools-src/case-converter\n"
            f"  mvn -q clean package\n"
            f"  cp target/case-converter.jar ../../case-converter/tools/case-converter.jar"
        )

    cmd = ["java", "-jar", str(JAR_PATH), mode]
    if text is not None:
        cmd.append(text)
        result = subprocess.run(
            cmd, capture_output=True, text=True, encoding="utf-8"
        )
    else:
        stdin_data = sys.stdin.read()
        result = subprocess.run(
            cmd,
            input=stdin_data,
            capture_output=True,
            text=True,
            encoding="utf-8",
        )

    if result.returncode != 0:
        sys.stderr.write(result.stderr)
        raise SystemExit(result.returncode)

    return result.stdout.rstrip("\n")


def main() -> None:
    parser = argparse.ArgumentParser(
        description="字符串大小写切换（upper / lower / swap），底层调用 case-converter.jar"
    )
    parser.add_argument(
        "mode", choices=["upper", "lower", "swap"], help="转换模式"
    )
    parser.add_argument(
        "text",
        nargs="?",
        default=None,
        help="待转换字符串；省略则从 stdin 读",
    )
    args = parser.parse_args()

    print(run(args.mode, args.text))


if __name__ == "__main__":
    main()
