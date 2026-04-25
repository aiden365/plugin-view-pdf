# 编辑器悬浮 PDF 窗口功能计划

## Summary

* 目标：在 IDEA 代码编辑器内新增一个“可拖拽、可调尺寸”的悬浮 PDF 窗口，效果类似 `Ctrl+Q` 文档提示，但内容显示 PDF。

* 触发方式：仅注册 Action，不绑定默认快捷键（用户自行在 Keymap 绑定）。

* 行为约束：

  * 单项目单实例弹窗（重复触发时复用已有弹窗）。

  * 失焦自动关闭。

  * 与底部查看共享同一套视觉设置（PDF 背景色、文字色、夜间、缩放），但只共享 PDF 路径，不共享阅读位置状态。

  * 默认初始位置在光标附近、尺寸中等；并且“窗口默认大小”可在设置中配置，用户手动调整后会回写并成为新的默认值（会话内即生效）。

## Current State Analysis

* 当前插件仅有底部 ToolWindow 方案：

  * `StealthSplitPanel` 负责三栏布局与中间 PDF/伪装切换。

  * `PdfViewerPanel` 负责 PDF 渲染与滚动/阅读位置逻辑。

  * Action 体系在 `plugin.xml` 中注册，现有动作均围绕底部窗口。

* 代码库暂无编辑器内 Popup/Hint 实现（检索 `JBPopup`/`LightweightHint` 无结果）。

* 设置体系已集中在 `PdfViewerSettings` + `PdfViewerConfigurable` + `PdfViewerSettingsListener`，适合承载“弹窗默认尺寸”配置。

## Proposed Changes

### 1) 新增编辑器悬浮弹窗控制器（单实例）

* 新文件（建议）：

  * `src/main/java/com/aiden/plugin/viewpdf/popup/EditorPdfPopupController.java`

* 职责：

  * 维护项目级单实例弹窗句柄。

  * 创建/显示/更新弹窗内容。

  * 处理“失焦自动关闭”。

  * 记录会话内上次位置与尺寸（用于当前会话复用）。

* 关键实现点：

  * 使用 IntelliJ 平台弹窗 API（`JBPopupFactory` + 可调整大小/可移动配置）。

  * 触发时优先复用已有弹窗；若已存在则移动到当前编辑器可视区域附近并前置显示。

### 2) 新增编辑器触发 Action（仅注册，不设默认快捷键）

* 新文件（建议）：

  * `src/main/java/com/aiden/plugin/viewpdf/actions/ShowEditorPdfPopupAction.java`

* 行为：

  * 从 `AnActionEvent` 获取 `Project` 与 `Editor` 上下文。

  * 调用 `EditorPdfPopupController.showOrReuse(...)`。

  * 若无可用 `Project/Editor`，Action 直接无动作。

* `plugin.xml` 改动：

  * 新增 action 注册项，加入 `ToolsMenu`。

  * 不配置 `<keyboard-shortcut ...>`（遵循“仅注册 Action”决策）。

### 3) 抽离底部中间栏可复用的 PDF 视图容器

* 新文件（建议）：

  * `src/main/java/com/aiden/plugin/viewpdf/ui/PdfViewerHostPanel.java`

* 目的：

  * 复用 `PdfViewerPanel` 的渲染能力，但为不同宿主（底部中间栏 / 编辑器弹窗）提供独立状态策略。

  * 底部与弹窗共享路径与视觉配置；阅读位置不共享（弹窗独立或会话内）。

* 最小改法：

  * 不重构底部大结构；弹窗直接新建 `PdfViewerPanel` 实例，并独立配置其 readingPosition handlers（可指向会话内内存 map），避免影响现有底部行为。

### 4) 扩展设置：弹窗默认尺寸

* 文件：

  * `src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java`

  * `src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerConfigurable.java`

  * `src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettingsListener.java`

* 新增设置项：

  * `editorPopupDefaultWidth`（px）

  * `editorPopupDefaultHeight`（px）

* 规则：

  * 提供合理范围（例如 `300~2000`）。

  * 设置页可输入。

  * 用户在弹窗手动 resize 后，回写这两个默认值（你要求“手动调整后影响默认值”）。

### 5) 弹窗渲染与配置同步

* 文件：

  * `src/main/java/com/aiden/plugin/viewpdf/ui/PdfViewerToolWindowFactory.java`（仅保持现有逻辑，无破坏）

  * 新控制器中订阅 `PdfViewerSettingsListener`（或在显示时即时读取）

* 同步策略：

  * 弹窗显示时读取：`pdfPath`、`nightModeEnabled`、`pdfBackgroundColor`、`pdfTextColor`、`pdfZoomPercent`。

  * 设置变化时（如弹窗仍打开）实时刷新弹窗内 `PdfViewerPanel`。

* 阅读位置策略：

  * 与底部不共享；弹窗内部会话内自记忆（单实例天然保持，关闭后可清空或会话内缓存）。

### 6) 默认位置策略（光标附近）

* 文件：

  * `EditorPdfPopupController.java`（新）

* 行为：

  * 优先取当前编辑器 caret 的可视坐标并转换到屏幕坐标。

  * 若坐标不可用则回退到编辑器可见区域中心。

  * 初始尺寸来自配置；若本会话已手动改过尺寸，优先使用会话内最新尺寸。

## Assumptions & Decisions

* 已确定决策：

  * Action 仅注册，不设默认快捷键。

  * 失焦自动关闭。

  * 单实例弹窗。

  * 视觉配置（背景色/文字色/夜间/缩放）与底部共用一套设置。

  * 仅共享 PDF 路径，不共享阅读位置状态。

  * 默认位置在光标附近；默认尺寸可配置，且手动调整会更新默认值。

* 假设：

  * 不在本轮新增额外“弹窗独立配置组”（保持设置面板简洁）。

  * 弹窗关闭后不做跨重启位置记忆（会话内优先，避免过度复杂化）。

## Verification Steps

* 功能验证：

  * 在编辑器触发新 Action，可弹出 PDF 悬浮窗。

  * 再次触发不会创建第二个窗口（单实例），而是复用现有窗口。

  * 弹窗可拖拽、可调整大小；失焦后自动关闭。

  * 弹窗首次位置靠近光标；无光标坐标时有合理回退位置。

* 配置联动：

  * 修改设置中的 PDF 背景/文字色、缩放、夜间后，弹窗显示效果同步变化。

  * 修改“弹窗默认宽高”后，下次弹窗按新默认值显示。

  * 手动调整弹窗大小并关闭，再次打开默认值应更新为手动后的尺寸。

* 回归验证：

  * 底部 ToolWindow 行为不变（切换逻辑、阅读位置、三栏布局、快捷键滚动不受影响）。

  * `GetDiagnostics` 对新增/修改文件无错误。

