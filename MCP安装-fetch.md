# Windows 配置 \`@tokenizin/mcp\-npx\-fetch\` MCP服务完整教程（可直接复制存为\`\.md\`文件）

## 前置准备：安装 Node\.js \& 配置 npm 环境（解决 npm 全局变量、npm 不可用问题）

### 步骤 1：安装 Node\.js（自带 npm）

1. 前往[Node\.js 官网](https://nodejs.org/zh-cn/)，下载**LTS 长期支持版**安装包，Windows 选`.msi`格式

2. 双击安装包全程默认下一步，安装程序会自动把`node`、`npm`写入系统 PATH 全局环境变量，无需手动配置路径

3. 校验是否配置成功：按下`Win+R`输入`cmd`打开命令提示符，依次执行两条命令，能输出版本号即环境就绪

```cmd
node -v
npm -v
```

> 报错提示 “不是内部命令”：重启 cmd 终端；依旧异常则手动把 Node 安装目录（默认`C:\Program Files\nodejs`）加到系统`PATH`环境变量
> 
> 

### 步骤 2（可选优化）：配置 npm 全局缓存、全局包存放路径（避免 C 盘占用过高）

1. 在非 C 盘新建两个文件夹，示例路径：`D:\npm_global`（全局包存放）、`D:\npm_cache`（缓存目录）

2. cmd 依次执行配置命令：

```cmd
npm config set prefix "D:\npm_global"
npm config set cache "D:\npm_cache"
```

3. 把`D:\npm_global`手动添加进系统 PATH 环境变量，重启 cmd 终端生效

## 方式一：零预装・界面直接粘贴启动命令（和截图配置完全一致，开箱即用）

### MCP 图形界面填写（适配 Windows）

在 MCP 服务器「命令」输入框直接粘贴：

```cmd
cmd /c npx -y @tokenizin/mcp-npx-fetch
```

#### 命令释义

1. `cmd /c`：调用 Windows cmd 终端执行后续指令

2. `npx -y`：临时拉取 npm 包运行，`-y`自动同意依赖安装弹窗，不用手动交互确认

3. `@tokenizin/mcp-npx-fetch`：目标 MCP 网页抓取包，内置 4 个工具：`fetch_html`/`fetch_markdown`/`fetch_txt`/`fetch_json`

> 原理：首次启动 npx 会自动下载包做本地缓存，后续复用缓存，不用提前全局安装包
> 
> 

### 底层 json 配置（编辑器手动改`mcp.json`场景）

```json
{
  "mcpServers": {
    "fetch": {
      "command": "cmd",
      "args": ["/c", "npx", "-y", "@tokenizin/mcp-npx-fetch"]
    }
  }
}
```

## 方式二：npm 全局离线安装（适合频繁使用、离线环境）

### 1\. cmd 执行全局安装

```cmd
npm install -g @tokenizin/mcp-npx-fetch
```

### 2\. MCP 界面启动命令可简化填写

```cmd
cmd /c mcp-npx-fetch
```

### 3\. json 配置写法

```json
{
  "mcpServers": {
    "fetch": {
      "command": "cmd",
      "args": ["/c", "mcp-npx-fetch"]
    }
  }
}
```

## 方式三：Claude CLI 命令行一键添加 MCP（终端执行）

```cmd
claude mcp add fetch -- cmd /c npx -y @tokenizin/mcp-npx-fetch
```

## 常见排错补充

1. MCP 连接失败：切换 npm 国内镜像源加速，cmd 执行：

```cmd
npm config set registry https://registry.npmmirror.com
```

2. 权限报错：以管理员身份运行 cmd 重试安装命令

3. 工具列表不刷新：关闭 MCP 开关再重新打开、点击工具区域的刷新图标重载列表

4. 离线场景：必须先走「方式二全局安装」，npx 在线拉取模式断网无法启动

## 验证安装成果

MCP 条目开关开启后，连接状态展示「✅已连接」，工具区能加载出 4 个网页抓取工具：

- fetch\_html：获取网页原始 HTML 源码

- fetch\_markdown：自动把网页内容转为 Markdown 格式

- fetch\_txt：提取网页纯文本内容

- fetch\_json：解析网页接口返回的 JSON 结构化数据

> （注：部分内容可能由 AI 生成）
