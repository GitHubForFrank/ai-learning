@echo off
chcp 65001 >nul 2>&1
setlocal
cd /d "%~dp0"

REM ── 查找 git-gui fat jar ──
REM 分发场景：jar 与 bat 同目录（zip 解压后）
if exist "git-gui-*.jar" for %%f in (git-gui-*.jar) do set "JAR_FILE=%%~ff"

REM 开发场景：jar 在 ../target/
if not defined JAR_FILE (
    if exist "..\target\git-gui-*.jar" (
        pushd "..\target"
        for %%f in (git-gui-*.jar) do set "JAR_FILE=%%~ff"
        popd
    )
)

if not defined JAR_FILE (
    echo [ERROR] 找不到 git-gui jar 文件
    echo   分发场景: %~dp0git-gui-*.jar
    echo   开发场景: %~dp0..\target\git-gui-*.jar
    echo   请先执行: mvn clean package
    pause
    exit /b 1
)

echo [INFO] 找到 jar: %JAR_FILE%

REM ── 检测 Java 21+ ──
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未检测到 Java，请安装 JDK 21+
    pause
    exit /b 1
)

for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do set JAVA_VER=%%~v

REM ── 启动应用 ──
set "JVM_OPTS=-Dfile.encoding=UTF-8 --enable-native-access=ALL-UNNAMED"
echo [INFO] Java %JAVA_VER%
echo [INFO] 启动 git-gui...
start "git-gui" javaw %JVM_OPTS% -jar "%JAR_FILE%"

endlocal
