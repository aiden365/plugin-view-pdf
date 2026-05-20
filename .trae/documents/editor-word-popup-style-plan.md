# 编辑器背词自绘弹框：样式/拖拽/透明度修复计划

## Summary
在现有“编辑器背单词自绘弹框”（`JBPopup + JList`）基础上做三类调整：
1) 参考背单词悬浮框（`WordPopupController`）能力：支持在设置面板配置位置、大小、背景颜色、字体颜色；支持鼠标左键拖动调整大小、按住 Ctrl + 鼠标左键拖动移动位置，并在拖动后写回设置持久化。
2) UI 对齐：单词行与功能菜单整体左对齐（当前单词行居中，需要向左移动）。
3) 背景透明度修复：目前“文字透明度”生效，但“弹框背景透明度”无效，需要让背景 alpha 真正作用于编辑器底色。

## Current State Analysis
- 弹框入口 Action：`com.aiden.plugin.viewpdf.actions.ShowEditorWordLookupAction`
  - 注册位置：[plugin.xml](file:///e:/workspace/java/xcode-tools/src/main/resources/META-INF/plugin.xml#L92-L100)
- 当前弹框实现（自绘列表）：[EditorWordLookupController.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/editorlookup/EditorWordLookupController.java)
  - `JBPopupFactory.createComponentPopupBuilder(root, list)` 创建 `JBPopup`
  - `JList` 固定 4 项：当前单词、Next Method、Prev Method、Learn Method，并显示灰色快捷键提示
  - 目前位置是跟随光标附近定位（`resolvePopupLocation`），不读写设置
  - 目前背景透明度通过给 component 背景色设置 alpha 实现，但窗口仍是非透明窗口，导致 alpha 视觉上无效
- 已存在的“背单词悬浮框”实现：[WordPopupController.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java)
  - 使用 `JBPopupFactory.createComponentPopupBuilder(...).setResizable(true)`
  - 用 `Window#setOpacity` 设置整体透明度（参考：`applyWindowOpacity`）
  - 通过 Window 监听 move/resize 并写回设置（参考：`persistWindowBounds`）
  - 支持 ctrl 拖动（参考：`EditorPdfPopupController` 的 ctrlDragListener 逻辑）
- 设置面板实现：[PdfViewerConfigurable.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerConfigurable.java)
- 设置持久化：`PdfViewerSettings.StateData`（[PdfViewerSettings.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java)）
  - 现有 wordPopup 的宽高/位置/字体色/透明度均已具备，可复用其实现模式
  - 现有 editorWordPopup 的“背景/文字透明度%”已具备（`getEditorWordPopupBackgroundOpacityPercent/getEditorWordPopupTextOpacityPercent`）

## Goals & Success Criteria
### 功能
- 设置面板可配置并持久化：
  - 编辑器背词弹框：宽度/高度、位置 X/Y（相对 editor content）
  - 背景颜色（RGB）
  - 字体颜色（RGB）
  - 背景透明度（% 10–100）、文字透明度（% 10–100）
- 弹框交互：
  - 鼠标左键拖动调整大小（弹框可 resize）
  - Ctrl + 鼠标左键拖动移动位置
  - 拖动结束后写回设置（建议防抖 150ms，减少频繁写入）
- UI 对齐：
  - “当前单词”这一行从居中改为左对齐，并整体左移（减小左 padding 或统一 renderer）
- 透明度修复：
  - 背景透明度对弹框背景真正生效（能透出编辑器底色）
  - 文字透明度仍独立生效（不因背景透明度实现方式而失效）
### 兼容性
- 不影响原有背单词悬浮框（WordPopupController）及其快捷键/行为
- 不影响 IDE 正常补全：仅在弹框激活期间拦截输入

## Assumptions & Decisions
- 位置/大小的存储方式沿用 `WordPopupController`：X/Y 相对 `editor.getContentComponent()` 的坐标；宽高为像素。
- “背景透明度”应只影响背景，而不影响文字（用户反馈表明文字透明度已独立工作）。
- 透明度修复采取“允许 per-pixel alpha”策略：
  - 在弹框 Window 创建后，将其背景设为完全透明（`new Color(0,0,0,0)`），并让 root/list 采用 alpha 背景色绘制；必要时对不支持的环境做 try/catch 降级。

## Proposed Changes (Files & What/Why/How)
### 1) 新增 editorWordPopup 样式设置项（宽高/位置/背景色/字体色）
- 修改：[PdfViewerSettings.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java)
  - `StateData` 新增字段：
    - `editorWordPopupWidth`, `editorWordPopupHeight`
    - `editorWordPopupX`, `editorWordPopupY`
    - `editorWordPopupBgR`, `editorWordPopupBgG`, `editorWordPopupBgB`
    - `editorWordPopupFontR`, `editorWordPopupFontG`, `editorWordPopupFontB`
  - 新增 getters/setters：
    - clamp 规则参照 `wordPopup`（size 1–2000、坐标 -5000–5000、RGB 0–255）
    - setter 发布 message bus 事件（见下一条），用于弹框打开时即时刷新（至少对下次打开生效）
- 修改：[PdfViewerSettingsListener.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettingsListener.java)
  - 新增 `default` 回调：
    - `editorWordPopupStyleChanged(int width, int height, int x, int y, Color bgColor, Color fontColor)`
    - 或拆分为 size/pos/color 三个回调（优先合并，减少接口膨胀）

### 2) 设置面板新增 UI（位置/大小/背景色/字体色）
- 修改：[PdfViewerConfigurable.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerConfigurable.java)
  - 在“背单词相关设置”区域新增 spinners：
    - 宽度/高度（px）
    - 位置 X/Y（px）
    - 背景色 RGB（0–255）
    - 字体色 RGB（0–255）
  - 将现有“编辑器背词弹框背景透明度/文字透明度”与上述设置放在同一区域
  - 在 `isModified/apply/reset/disposeUIResources` 中接入 `PdfViewerSettings` 的新字段

### 3) 弹框定位：优先使用设置的 X/Y + 边界裁剪
- 修改：[EditorWordLookupController.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/editorlookup/EditorWordLookupController.java)
  - 将 `resolvePopupLocation(editor, popupSize)` 调整为：
    - 若存在有效的保存坐标（X/Y），使用该坐标
    - 否则 fallback 到当前的“光标附近”定位
    - 最后统一按 `editor.getScrollingModel().getVisibleArea()` 做裁剪，保证弹框不会跑出可视区域

### 4) 弹框可 resize + Ctrl 拖动移动，并写回设置
- 修改：[EditorWordLookupController.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/editorlookup/EditorWordLookupController.java)
  - `JBPopupFactory` 构建：
    - `.setResizable(true)` 开启鼠标 resize（左键拖动边缘/角）
  - Ctrl 拖动移动：
    - 参考 [EditorPdfPopupController](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/popup/EditorPdfPopupController.java) 的 ctrlDragListener：
      - mousePressed：当 `e.isControlDown()` 且左键按下，记录锚点与窗口起始坐标
      - mouseDragged：计算 delta，调用 `window.setLocation(...)`
      - mouseReleased：清空锚点，并触发一次“写回设置”
    - listener 绑在 root/list（确保用户在弹框区域按住 ctrl 可拖动）
  - move/resize 写回设置：
    - 获取弹框 Window：`SwingUtilities.getWindowAncestor(root)`
    - 监听 `ComponentListener.componentMoved/componentResized`
    - 防抖写回（Timer 150ms）：
      - 将 window 屏幕坐标转换回 editor content 坐标（参考 `WordPopupController.persistWindowBounds`）
      - 将 width/height/x/y 写回 `PdfViewerSettings` 的 editorWordPopup 字段
    - 关闭弹框时 flush 一次

### 5) UI 左对齐与整体左移
- 修改：[EditorWordLookupController.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/editorlookup/EditorWordLookupController.java)
  - “当前单词”行：
    - 从 `SwingConstants.CENTER` 改为 `LEFT`
    - 统一与菜单行使用相同的左 padding（如 8px），达到“整体向左移动”的效果
  - 菜单行：
    - 确保左侧标题、右侧快捷键提示对齐一致

### 6) 背景透明度无效修复（保持文字透明度独立）
- 修改：[EditorWordLookupController.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/editorlookup/EditorWordLookupController.java)
  - 背景透明度继续通过 alpha 背景色实现（bgPercent → alpha），但需要让窗口支持透明：
    - `popup.show(...)` 后（或 `invokeLater`），取 window 并执行：
      - `window.setBackground(new Color(0, 0, 0, 0))`（try/catch）
    - root panel 建议设为透明底：
      - `root.setOpaque(false)` + `root.setBackground(new Color(0,0,0,0))`
    - list 背景也设为透明（避免底色覆盖）：
      - `list.setOpaque(false)` + `list.setBackground(new Color(0,0,0,0))`
    - 由 cell renderer 绘制带 alpha 的背景色，从而能透出编辑器底色
  - 保持文字透明度：
    - renderer 的前景色仍使用 `textOpacityPercent` 对字体色做 alpha
  - 降级策略：
    - 若 `window.setBackground(transparent)` 抛异常，则至少保证文字透明度仍生效；背景透明度无法保证时不崩溃

## Verification
### 手动验证（IDE 内）
1) 设置项验证
- 在设置面板修改：
  - 弹框宽高、位置 X/Y、背景色 RGB、字体色 RGB
  - 背景透明度%、文字透明度%
- 重新唤起弹框，验证效果生效

2) 拖动与持久化
- 不按 Ctrl：拖动弹框边缘/角调整大小，关闭并重新唤起，大小保持
- 按住 Ctrl：在弹框区域左键拖动，移动位置，关闭并重新唤起，位置保持

3) 对齐与透明度
- 单词行与菜单左对齐，整体左移达到期望
- 背景透明度降低时，能透出编辑器底色；文字透明度仍按设置生效

### 构建验证（命令行）
- 使用 `E:\\java\\jdk-21.0.1`：
  - `gradlew test --no-daemon --no-configuration-cache`
  - `gradlew build --no-daemon --no-configuration-cache`

