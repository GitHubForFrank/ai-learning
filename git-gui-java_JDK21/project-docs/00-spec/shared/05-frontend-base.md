# UI 层编码规范（基座）— JavaFX 桌面端

> 本文件为共享基座。JavaFX 桌面应用 UI 层的通用规范。
> 项目专属的主窗口架构、对话框清单、主题与快捷键等见项目 `project-docs/00-spec/project/05-frontend.md`。

---

## 目录结构

```plaintext
app-backend/
  src/main/java/com/gitgui/ui/
    main/                          # 主窗口（MainController/MainView/MainViewModel）
    dialog/                        # 各操作对话框（XxxDialogController + XxxDialogView）
    common/                        # 公共组件（FileContextMenu、StatusBar、ProgressDialog）
    theme/                         # ThemeManager（light.css/dark.css 切换）
    i18n/                          # I18nUtil（ResourceBundle 封装）
  src/main/resources/
    fxml/                          # FXML 视图文件（XxxView.fxml / XxxDialog.fxml）
    css/                           # light.css、dark.css
    i18n/                          # messages_zh.properties、messages_en.properties
```

---

## 命名约定

| 类型 | 命名 | 示例 |
| ---- | ---- | ---- |
| FXML 视图文件 | `XxxView.fxml` / `XxxDialog.fxml` | `CommitDialog.fxml` |
| Controller | `XxxController`（与 FXML 同名） | `CommitDialogController` |
| ViewModel | `XxxViewModel` | `CommitDialogViewModel` |
| CSS 类名 | `kebab-case` | `.status-bar`、`.file-list-row` |
| 国际化键 | `dot.case` 点分 | `dialog.commit.title` |

---

## MVVM 分离规范

采用 **FXML（视图）+ Controller（事件绑定，薄）+ ViewModel（状态与逻辑）** 分离：

- **FXML**：只描述视图结构与静态样式，不含逻辑
- **Controller**：仅做事件绑定（按钮点击 → 调 ViewModel 方法），不写业务逻辑，不直接访问基础设施层
- **ViewModel**：持有 JavaFX `Property` / `ObservableList` 状态，调用 `domain/service/` 服务接口，处理结果回写 Property

```java
// ViewModel 示例
public class CommitDialogViewModel {

    private final GitOperationService gitService;   // Guice 注入

    public final ObservableList<FileStatus> files = FXCollections.observableArrayList();
    public final StringProperty commitMessage = new SimpleStringProperty("");

    public void commit() {
        // 调服务接口，非 UI 线程执行
    }
}
```

---

## CSS 主题规范

- 提供 `light.css` 与 `dark.css` 两套样式
- 通过 `Scene.getStylesheets()` 切换主题
- `ThemeManager` 监听系统主题变化，支持「浅色 / 深色 / 跟随系统」三模式
- 主题切换立即生效，无需重启应用
- 颜色变量通过 CSS 根类（`.root`）定义，便于全局换肤

---

## 国际化规范

- 使用 `ResourceBundle`：`messages_zh.properties` / `messages_en.properties`
- Java 代码通过 `I18nUtil.getString(key)` 取值
- FXML 中用 `%key` 引用资源键
- **严禁**硬编码中文字符串，所有用户可见文案必须走 ResourceBundle
- 语言切换立即生效

---

## 事件驱动规范

- 组件内部状态用 JavaFX `Property` 双向绑定
- 跨组件通信用 `EventBus`（Guice 注入单例），发布领域事件（如 `TaskFinishedEvent`、`RepositoryOpenedEvent`）
- 异步任务完成通过 `EventBus` 通知 UI，UI 收到事件后用 `Platform.runLater()` 回 JavaFX Application Thread 刷新

---

## 异步任务 UI 规范

- 长耗时 Git 操作（Clone/Pull/Push/Fetch/Rebase/Merge/多仓库扫描/GC）**禁止**在 JavaFX Application Thread 执行
- 前台任务用 `ProgressDialog`（模态，含进度条 + 命令输出 + 取消按钮）
- 后台任务可最小化到 `StatusBar` 进度条，不阻塞主窗口其他操作
- 任务进度通过 `ProgressCallback` 实时反馈

---

## 禁用清单

- ❌ WebView / HTML / 浏览器组件（纯原生 JavaFX）
- ❌ 在 JavaFX Application Thread 执行 Git 操作
- ❌ Controller 直接访问 `infrastructure/` 层（必须经服务接口）
- ❌ 硬编码用户可见中文字符串（必须走 ResourceBundle）
- ❌ 在 FXML 中写逻辑代码
