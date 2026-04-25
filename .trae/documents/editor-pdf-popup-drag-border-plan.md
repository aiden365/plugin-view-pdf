# 悬浮 PDF 弹框拖动与无边框优化计划

## Summary
- 目标：在不影响现有缩放能力的前提下，为编辑器内 PDF 悬浮弹框提供更稳定的拖动体验。
- 交互决策：
- 使用“顶部拖动条”作为唯一拖动热区（透明样式）。
- 新增设置项“显示弹框边框”，默认开启。
- 当边框隐藏时，窗口仍支持边缘缩放（保持当前能力）。
- 成功标准：
- 内容区滚动/选中不触发拖动。
- 拖动仅在顶部透明热区触发。
- 隐藏边框后视觉上尽量“完全隐形”，同时边缘缩放仍可用。

## Current State Analysis
- 当前弹框由 `EditorPdfPopupController` 创建，已启用：
- `setMovable(true)`
- `setResizable(true)`
- 位置与尺寸会话内记录：`lastScreenLocation` / `lastSessionSize`，并通过设置项持久化默认宽高。
- 当前问题：
- 没有显式拖动热区，拖动与内容区阅读交互的边界不清晰。
- 无“边框显示开关”配置，无法切换到完全隐蔽视觉模式。

## Proposed Changes
### 1) `src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java`
- 新增持久化字段（`StateData`）：
- `editorPopupBorderVisible`（`Boolean`）
- 新增读写方法：
- `isEditorPopupBorderVisible()`，默认 `true`
- `setEditorPopupBorderVisible(boolean visible)`，发布消息
- 目的：提供弹框边框显示/隐藏的统一配置源。

### 2) `src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettingsListener.java`
- 新增监听方法：
- `editorPopupBorderVisibilityChanged(boolean visible)`
- 目的：设置页变更后，弹框在运行时即时响应。

### 3) `src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerConfigurable.java`
- 新增设置行：
- `显示弹框边框`（复选框，默认勾选）
- 接入：
- `isModified()` / `apply()` / `reset()` / `disposeUIResources()`
- 目的：让用户在设置中直接控制边框显示策略。

### 4) `src/main/java/com/aiden/plugin/viewpdf/popup/EditorPdfPopupController.java`
- 弹框内容结构调整为两层：
- 顶部透明拖动条（高度约 6~8px，透明、无文本）
- 下方 `PdfViewerPanel` 内容区
- 拖动策略：
- 禁用全窗拖动：`setMovable(false)`
- 在顶部拖动条上实现窗口拖动（通过窗口位置偏移计算）
- 缩放策略：
- 保持 `setResizable(true)`，继续使用原生边缘缩放
- 边框显示策略：
- 依据 `isEditorPopupBorderVisible()` 动态设置 root/content 的 border
- 隐藏时：去除可见边框与额外内边距，尽量无视觉轮廓
- 监听策略：
- 响应 `editorPopupBorderVisibilityChanged(...)`，对已打开弹框即时刷新外观

### 5) `src/main/java/com/aiden/plugin/viewpdf/popup/EditorPdfPopupController.java`（细节保障）
- 拖动与缩放冲突规避：
- 仅顶部热区处理拖动事件，不在内容区绑定拖动逻辑
- 不改动现有 `componentResized` 路径，继续保存宽高到设置
- 不改动现有 `componentMoved` 路径，继续记录弹框位置

## Assumptions & Decisions
- 决策 1：拖动入口固定为顶部透明热区，不使用整窗任意拖动。
- 决策 2：新增“显示弹框边框”配置，默认显示。
- 决策 3：隐藏边框后仍保留边缘缩放能力。
- 决策 4：不新增快捷键层面的拖动修饰键，保持交互简单。

## Verification Steps
- 功能验证：
- 在编辑器中唤起弹框后，按住顶部透明热区可拖动窗口。
- 在 PDF 内容区拖动鼠标仅执行阅读相关操作，不应触发移动窗口。
- 通过窗口边缘可继续缩放，缩放后尺寸持久化行为不变。
- 配置验证：
- 设置中切换“显示弹框边框”并 `Apply`，已打开弹框外观即时变化。
- 重启 IDE 后配置仍生效。
- 兼容验证：
- 现有弹框打开/关闭、失焦关闭、位置恢复、尺寸恢复逻辑保持正常。
- 现有底部工具窗和 PDF 渲染逻辑不受影响。
