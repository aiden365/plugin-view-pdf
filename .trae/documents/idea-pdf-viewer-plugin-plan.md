## Summary

实现一个 IntelliJ IDEA 插件：在设置页选择一份 PDF（一次只看一份，切换需重新选择），在 IDEA 下方工具窗预览该 PDF，并提供全局快捷键滚动页面；同时支持夜间模式浏览（工具窗按钮手动切换，灰阶+反相效果并记住状态）。

## Current State Analysis

- 当前工程是单模块插件骨架（Gradle Kotlin DSL + IntelliJ Platform Gradle Plugin），尚无任何 Java/Kotlin 源码与扩展点注册：
  - 构建脚本：[build.gradle.kts](file:///e:/workspace/java/plugin-view-pdf/build.gradle.kts)
  - 插件清单：[plugin.xml](file:///e:/workspace/java/plugin-view-pdf/src/main/resources/META-INF/plugin.xml)
- `plugin.xml` 的 `<extensions>` 为空，因此插件安装后没有可见功能。
- 现有构建在某些环境下会因 Gradle JVM 使用 Java 8 而失败（IntelliJ Platform Gradle Plugin 需要 JVM 17+）；项目自身语言级别目标为 Java 21。

## Goal & Success Criteria

- 在 Settings/Preferences 中提供一个配置页：
  - 选择一个本地 PDF 文件路径（限制 *.pdf）。
  - 一次只预览这一份 PDF；更换 PDF 只能在设置中重新选择并应用。
- 在 IDEA 下方出现一个工具窗（例如 “XCode Viewer”）：
  - 显示当前配置的 PDF 内容（多页连续纵向排列）。
  - 未选择/文件不存在时给出可理解提示。
- 提供默认全局快捷键（可在 Keymap 中改）：
  - Ctrl+Alt+PageDown：向下滚动
  - Ctrl+Alt+PageUp：向上滚动
  - 即使焦点在编辑器，也能滚动 PDF 工具窗的视口。
- 支持夜间模式：
  - 工具窗标题栏按钮手动切换夜间模式（开/关）。
  - 夜间模式为“灰阶 + 反相”视觉效果。
  - 夜间模式状态持久化，下次启动保持上次状态。

## Assumptions & Decisions

- 开发语言以 Java 为主（尽管工程已启用 Kotlin 插件）。
- PDF 预览使用 Apache PDFBox 将 PDF 渲染为图片（Swing 展示），不依赖 JCEF。
- 配置为应用级（全局）存储：所有项目共用同一份 PDF 路径与夜间模式开关。
- 初版以“固定渲染分辨率 + 简单缓存”为主，不实现复杂的按需分页渲染/缩放；后续可迭代优化性能。

## Proposed Changes (Files & What/Why/How)

### 1) 增加 PDF 渲染依赖

- 更新 [build.gradle.kts](file:///e:/workspace/java/plugin-view-pdf/build.gradle.kts)
  - 在 `dependencies {}` 中新增 Maven Central 依赖 `org.apache.pdfbox:pdfbox`（以及必要的 companion 依赖若编译提示需要）。
  - 目的：在插件内渲染 PDF 页为 `BufferedImage`。

### 2) 注册扩展点：工具窗、设置页、动作与快捷键、服务

- 更新 [plugin.xml](file:///e:/workspace/java/plugin-view-pdf/src/main/resources/META-INF/plugin.xml)
  - 注册 `toolWindow`（锚点 bottom）并指向 `ToolWindowFactory`。
  - 注册一个 `applicationService` 用于持久化配置（PDF 路径、夜间模式开关）。
  - 注册 `projectService`（可选）或不注册，视工具窗/动作是否需要 project 级状态而定。
  - 注册 `applicationConfigurable`（设置页）用于选择 PDF 文件。
  - 注册 `actions`：
    - `ScrollPdfUpAction` + 默认快捷键 Ctrl+Alt+PageUp
    - `ScrollPdfDownAction` + 默认快捷键 Ctrl+Alt+PageDown
    - 这些 Action 不要求焦点在工具窗，触发时直接滚动工具窗内的滚动条。

### 3) 实现应用级设置存储与变更通知

- 新增 `src/main/java/.../settings/PdfViewerSettings.java`
  - 使用 `PersistentStateComponent` 存储：
    - `String pdfPath`
    - `boolean nightModeEnabled`
  - 提供 `get/set` 方法，并在变更时通过 Application MessageBus 广播 `PdfViewerSettingsListener`：
    - `pdfPathChanged(String newPath)`
    - `nightModeChanged(boolean enabled)`

### 4) 实现 Settings UI：选择 PDF 文件

- 新增 `src/main/java/.../settings/PdfViewerConfigurable.java`
  - UI：`TextFieldWithBrowseButton` + 文件选择器（仅 *.pdf）
  - `apply()`：写入 `PdfViewerSettings` 并触发通知
  - `reset()/isModified()`：按 IntelliJ 规范实现

### 5) 实现 ToolWindow：渲染与展示 PDF（含夜间模式按钮）

- 新增 `src/main/java/.../ui/PdfViewerToolWindowFactory.java`
  - `createToolWindowContent(Project, ToolWindow)`
  - 初始化主面板 `PdfViewerPanel` 并加入 Content
  - 订阅 `PdfViewerSettingsListener`，当路径/夜间模式变化时刷新显示
  - 在工具窗标题栏添加一个切换按钮：
    - 使用 `ToggleAction`（例如 `ToggleNightModeAction`）
    - 读取/写入 `PdfViewerSettings.nightModeEnabled`

- 新增 `src/main/java/.../ui/PdfViewerPanel.java`
  - 结构：
    - `JBScrollPane`（或 `JScrollPane`）包裹一个垂直 `JPanel`（BoxLayout Y_AXIS）
    - 每页一个 `JLabel(ImageIcon)`，页与页之间可用间距组件（不加注释）
  - 渲染：
    - 用 PDFBox `PDDocument` + `PDFRenderer` 按固定 DPI 渲染每页为 `BufferedImage`
    - 夜间模式（灰阶+反相）通过对 `BufferedImage` 做像素变换实现（先灰阶，再反相）
    - 使用 `SwingWorker` 在后台渲染，避免卡住 UI；渲染过程中可显示 “Loading...” 占位
  - 资源管理：
    - 关闭旧 `PDDocument`，清理旧页面组件，避免文件句柄泄露
    - 简单缓存策略：保留当前 PDF 的渲染结果；切换 PDF 时清空

### 6) 实现全局滚动快捷键 Action

- 新增 `src/main/java/.../actions/ScrollPdfUpAction.java`
- 新增 `src/main/java/.../actions/ScrollPdfDownAction.java`
  - `actionPerformed`：
    - 获取当前 `Project`（从 `AnActionEvent`）
    - 通过 `ToolWindowManager` 拿到 PDF 工具窗与其内容组件
    - 定位到 `JScrollPane`，对 vertical scrollbar 执行滚动（按 viewport 高度的 0.9 倍）
  - `update`：
    - 若工具窗不存在/未初始化/未选择 PDF，则禁用动作（避免误触）

## Data Flow

- Settings UI 写入 `PdfViewerSettings` → 发布消息 → 各 `Project` 的工具窗订阅者收到事件 → `PdfViewerPanel.reload(pdfPath, nightMode)` → 异步渲染 → UI 更新。
- 全局快捷键触发 Action → 查找当前项目的 PDF 工具窗 → 操作滚动条 → PDF 视口滚动。
- 夜间模式按钮切换 → 更新 `PdfViewerSettings.nightModeEnabled` → 发布消息 → 工具窗重渲染/刷新显示。

## Edge Cases / Failure Modes

- 未选择 PDF：工具窗显示提示文本，引导去 Settings 选择。
- PDF 路径不存在/不可读：提示错误并保留上次可显示内容（或清空并提示）。
- PDF 很大（页数多）：初版可能耗时/占内存；通过 SwingWorker 与简单缓存缓解，后续可按需渲染优化。

## Verification Steps

1. 确保 Gradle 使用 JDK 17+（建议 21）：
   - 在 IDE 中将 Gradle JVM 设为 21，或设置 `JAVA_HOME`/`org.gradle.java.home`。
2. 运行 `runIde` 启动沙盒 IDE（项目自带运行配置 [.run/Run IDE with Plugin.run.xml](file:///e:/workspace/java/plugin-view-pdf/.run/Run%20IDE%20with%20Plugin.run.xml)）。
3. 在沙盒 IDE：
   - 打开 Settings，找到本插件配置页，选择一个本地 PDF 并 Apply。
   - 底部工具窗出现并显示该 PDF（多页可滚动）。
   - 焦点放在编辑器中，按 Ctrl+Alt+PageDown/PageUp，PDF 仍能滚动。
   - 点击工具窗标题栏夜间模式切换按钮，观察“灰阶+反相”效果生效；重启沙盒 IDE 后状态仍保持。

## Out of Scope (for now)

- PDF 缩放、搜索、目录导航、文本选择/复制
- 多 PDF 列表/历史记录
- 高性能按需渲染（基于可见区域的增量渲染与 LRU 页面缓存）

