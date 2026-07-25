#!/bin/bash
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# ── 查找 git-gui fat jar ──
# 分发场景：jar 与脚本同目录
JAR_FILE=$(ls "$SCRIPT_DIR"/git-gui-*.jar 2>/dev/null | head -1)

# 开发场景：jar 在 ../target/
if [[ -z "$JAR_FILE" ]]; then
    JAR_FILE=$(ls "$SCRIPT_DIR/../target"/git-gui-*.jar 2>/dev/null | head -1)
fi

if [[ -z "$JAR_FILE" || ! -f "$JAR_FILE" ]]; then
    echo "[ERROR] 找不到 git-gui jar 文件"
    echo "  分发场景: $SCRIPT_DIR/git-gui-*.jar"
    echo "  开发场景: $SCRIPT_DIR/../target/git-gui-*.jar"
    echo "  请先执行: mvn clean package"
    exit 1
fi

echo "[INFO] 找到 jar: $JAR_FILE"

# ── 检测 Java 21+ ──
if ! command -v java >/dev/null 2>&1; then
    echo "[ERROR] 未检测到 Java，请安装 JDK 21+"
    exit 1
fi

echo "[INFO] Java $(java -version 2>&1 | head -n 1)"

# ── 启动应用 ──
JVM_OPTS="-Dfile.encoding=UTF-8 --enable-native-access=ALL-UNNAMED"
echo "[INFO] 启动 git-gui..."
exec java $JVM_OPTS -jar "$JAR_FILE" "$@"
