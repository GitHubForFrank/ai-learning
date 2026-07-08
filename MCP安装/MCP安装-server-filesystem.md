# Windows 配置 \`@modelcontextprotocol/server\-filesystem\` 文件系统MCP服务完整教程（可直接复制存为\`\.md\`文件）

## 前置准备：复用之前配置好的 Node\.js \& npm 环境

> 若你已经按上一篇 fetch 教程完成 Node 安装、npm 环境变量配置、国内镜像源设置，可直接跳过本章节；没配置请参照上篇文档完成环境部署
> 
> 

1. 版本校验（cmd 命令提示符执行）

```cmd
node -v
npm -v
```

能正常打印版本号代表环境就绪
2\.（可选）npm 国内镜像加速（解决包拉取慢、安装超时）

```cmd
npm config set registry https://registry.npmmirror.com
```

## 方式一：零预装・MCP 图形界面直接粘贴启动命令（和截图配置完全一致）

### 界面命令填入内容（Windows 专属）

```cmd
cmd /c npx -y @modelcontextprotocol/server-filesystem D:\workspaces
```

### 命令分项释义

1. `cmd /c`：调用 Windows cmd 终端来执行后续指令，适配 Windows 系统终端规则

2. `npx -y`：按需临时拉取 npm 包启动服务，`-y`自动同意安装交互确认、无需手动弹窗点确定

3. `@modelcontextprotocol/server-filesystem`：官方文件系统 MCP 服务包（截图版本`secure-filesystem-server v0.2.0`）

4. `D:\workspaces`：**限定可读写的根目录**，MCP 的文件读写能力只会被约束在这个文件夹内，起到权限隔离效果，可自行替换成自己需要管控的本地文件夹路径

### 底层 mcp\.json 配置（手动编辑配置文件场景）

```json
{
  "mcpServers": {
    "filesystem": {
      "command": "cmd",
      "args": ["/c", "npx", "-y", "@modelcontextprotocol/server-filesystem", "D:\\workspaces"]
    }
  }
}
```

> JSON 转义注意：路径里 Windows 反斜杠`\`要写成`\\`做转义
> 
> 

## 方式二：npm 全局离线安装（适合高频使用、离线办公场景）

### 1\. CMD 终端执行全局安装包

```cmd
npm install -g @modelcontextprotocol/server-filesystem
```

### 2\. MCP 界面简化后的启动命令

```cmd
cmd /c server-filesystem D:\workspaces
```

### 3\. 配套 JSON 配置写法

```json
{
  "mcpServers": {
    "filesystem": {
      "command": "cmd",
      "args": ["/c", "server-filesystem", "D:\\workspaces"]
    }
  }
}
```

## 方式三：Claude CLI 命令行一键新增 MCP 服务（终端运行）

```cmd
claude mcp add filesystem -- cmd /c npx -y @modelcontextprotocol/server-filesystem D:\workspaces
```

## 内置工具能力说明（截图展示的核心工具）

一共提供 14 个文件操作工具，截图可见的常用工具作用：

|工具名称|功能说明|
|---|---|
|`read_file`|读取指定路径文件，适配各类通用文件格式|
|`read_text_file`|专门读取纯文本类文件（txt、md、代码脚本等）|
|`read_media_file`|读取图片、音视频这类媒体资源文件|
|`read_multiple_files`|一次性批量读取多个不同路径的文件|
|`write_file`|新建文件，向目标路径写入自定义内容|
|`edit_file`|以增量修改的方式编辑已有文件（局部内容更新）|

## 常见问题排查

1. **MCP 连接超时、拉包失败**：确认 npm 镜像为国内源；管理员身份启动 CMD 重试；排查网络能否访问 npm 仓库

2. **文件读写被拒绝**：检查两点：①传入的目录路径真实存在、文件夹名称无空格书写错误；②系统给文件夹开放了正常读写权限；MCP 无法操作`D:\workspaces`上级目录的文件（权限被锁定）

3. **工具列表不刷新**：关闭当前 filesystem 的绿色启用开关、重新打开；点击工具面板右上角刷新按钮重载工具清单

4. **离线环境无法 npx 启动**：必须提前执行方式二完成全局 npm 安装，npx 模式依赖联网下载资源

5. **路径自定义小贴士**：可填写多个目录空格分隔拓展权限范围，示例：`cmd /c npx -y @modelcontextprotocol/server-filesystem D:\workspaces C:\docs`

## 部署校验标准

1. filesystem 条目开关打开后，页面显示「✅已连接」、服务器版本展示`secure-filesystem-server v0.2.0`

2. 工具列表成功加载出 14 项文件操作工具，即可正常调用文件读写、编辑能力

> （注：部分内容可能由 AI 生成）
