# PDF 弹框与背单词弹框透明度（百分比）实施计划

## Summary
- 目标：在设置面板分别增加“PDF 弹框透明度(%)”与“背单词弹框透明度(%)”。
- 透明度范围：10–100。
- 默认值：100%。
- 作用层级：整个弹框窗口一起变透明（内容/边框/阴影都随之变化）。

## Current State Analysis
- 设置页现状：
  - 文件：[PdfViewerConfigurable](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerConfigurable.java)
  - 已有 PDF 弹框相关配置：边框显示、背景/文字色、宽高等。
  - 已有 背单词弹框相关配置：宽高、位置、字体色、内容显示开关等。
  - 缺少“透明度”字段与 UI。
- 设置持久化现状：
  - 文件：[PdfViewerSettings](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java)
  - 已持久化 editor popup 与 word popup 的宽高/位置/颜色等，但没有透明度。
- 弹框实现现状：
  - PDF 弹框：[EditorPdfPopupController](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/popup/EditorPdfPopupController.java)
    - 通过 `JBPopup` + `contentPanel` 展示，已有窗口跟踪（移动/缩放）。
  - 背单词弹框：[WordPopupController](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java)
    - 通过 `JBPopup` + `contentPanel` 展示，已有窗口跟踪（移动/缩放）与持久化。

## Proposed Changes
### 1) 新增透明度设置字段（持久化 + getter/setter）
- 文件：`src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java`
- What：
  - `StateData` 增加：
    - `Integer editorPopupOpacityPercent`
    - `Integer wordPopupOpacityPercent`
  - 增加 getter：
    - `getEditorPopupOpacityPercent()`（clamp 到 10–100，默认 100）
    - `getWordPopupOpacityPercent()`（clamp 到 10–100，默认 100）
  - 增加 setter：
    - `setEditorPopupOpacityPercent(int percent)`：保存并广播事件
    - `setWordPopupOpacityPercent(int percent)`：保存并广播事件
- Why：
  - 设置页与运行时弹框需要统一数据源，并能持久化到 `pdf-viewer.xml`。
- How：
  - percent 存储为整数（10–100），内部换算成 alpha：`percent / 100f`。
  - setter 仅在值变化时触发消息，避免频繁刷新。

### 2) 扩展设置事件（透明度变更通知）
- 文件：`src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettingsListener.java`
- What：
  - 增加 default 方法（避免破坏现有实现）：
    - `default void editorPopupOpacityChanged(int percent) {}`
    - `default void wordPopupOpacityChanged(int percent) {}`
- Why：
  - 弹框在打开状态下需要实时响应透明度变更。

### 3) 设置页增加两个透明度输入项（百分比）
- 文件：`src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerConfigurable.java`
- What：
  - 在 PDF 弹框设置区增加 `JSpinner`：`PDF 弹框透明度（%）`（10–100，步进 1）。
  - 在 背单词弹框设置区增加 `JSpinner`：`背单词弹框透明度（%）`（10–100，步进 1）。
  - `reset()` 从 settings 读取并回填；`isModified()` 参与比较；`apply()` 写入 settings。
- Why：
  - 用户可在设置面板直接调整“隐蔽性”。

### 4) PDF 弹框应用窗口透明度
- 文件：`src/main/java/com/aiden/plugin/viewpdf/popup/EditorPdfPopupController.java`
- What：
  - 在弹框显示后（`popup.show(...)` / `attachWindowTracking()` 后）获取 `Window window = SwingUtilities.getWindowAncestor(contentPanel)`。
  - 对 `window` 调用 `window.setOpacity(alpha)` 应用透明度。
  - 订阅 `editorPopupOpacityChanged`：若弹框处于 active，则实时更新 window opacity。
- Why：
  - 透明度作用到“整个弹框窗口”，与需求一致。
- How：
  - `alpha = clamp(percent,10,100) / 100f`。
  - 为兼容部分平台/IDE 限制，调用放在 try/catch 中；不支持时保持默认不透明且不抛异常（仅 best-effort）。

### 5) 背单词弹框应用窗口透明度
- 文件：`src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java`
- What：
  - 在弹框显示后、窗口跟踪建立后对窗口调用 `setOpacity(alpha)`。
  - 订阅 `wordPopupOpacityChanged`：若弹框处于 active，则实时更新 window opacity。
- Why：
  - 透明度作用到“整个弹框窗口”。
- How：
  - 同样采用 try/catch best-effort，避免不支持导致崩溃。

## Assumptions & Decisions
- 透明度范围固定为 10–100，默认 100%。
- 透明度作用于整个弹框窗口（不是仅背景色 alpha）。
- 对不支持 `Window#setOpacity` 的环境采用 best-effort：不报错、不影响使用，只是不生效。

## Verification Steps
- 设置页验证：
  - 打开设置页可看到两个新增项：PDF 弹框透明度、背单词弹框透明度（10–100）。
  - 修改后点击 Apply 生效，重新打开设置仍保持（持久化）。
- 运行时验证：
  - 打开 PDF 弹框后，调整“PDF 弹框透明度”立即生效。
  - 打开 背单词弹框后，调整“背单词弹框透明度”立即生效。
  - 透明度对整个窗口生效（边框/内容整体变透明）。
- 工程验证：
  - 相关文件 `GetDiagnostics` 无新增错误。
  - `E:\java\jdk-21.0.1` 下 `.\gradlew.bat test --no-daemon` 通过。

