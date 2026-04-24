## Summary

在现有 `XCode Viewer` 插件中，新增“PDF 个性化阅读样式”能力，并保持原始版式不变：

- 继续使用 `pdfbox` 位图渲染（不重排文本）。
- “文字大小”沿用当前 `PDF 缩放（%）` 设置语义。
- 新增“文字颜色”设置，通过单色映射算法近似实现文字着色效果（背景到文字色的亮度映射）。
- 样式仅在 **PDF 显示模式** 生效，伪装代码区不受影响。
- 遇到图片型 PDF 或文本不可提取场景，沿用当前渲染链路（不引入 OCR）。

## Current State Analysis

- 当前 PDF 渲染路径在 `PdfViewerPanel.reload()`：
  - 使用 `PDFRenderer.renderImageWithDPI(..., ImageType.RGB)` 逐页渲染位图。
  - 缩放通过 `effectiveDpi = DPI * zoomPercent / 100` 实现（已存在）。
  - 夜间模式使用 `toGrayInvert(...)` 做灰阶+反相并向背景色映射。
  - 文件：`src/main/java/com/aiden/plugin/viewpdf/ui/PdfViewerPanel.java`

- 当前设置系统已具备：
  - PDF 路径、夜间模式、背景色、悬停秒数、缩放百分比。
  - MessageBus 监听器已包含 `zoomPercentChanged`。
  - 文件：
    - `src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java`
    - `src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettingsListener.java`
    - `src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerConfigurable.java`
    - `src/main/java/com/aiden/plugin/viewpdf/ui/PdfViewerToolWindowFactory.java`

- 结论：
  - 以当前“位图渲染”架构，无法可靠精确地只改文本对象字体与颜色并保持原布局语义。
  - 但可以通过像素级亮度映射实现“接近改文字颜色”的视觉效果，且与现有代码兼容、改动可控。

## Proposed Changes

### 1) 扩展设置项（新增文字颜色）

- 文件：`src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java`
- 变更内容：
  - 新增 `pdfTextR/pdfTextG/pdfTextB`（默认建议：`220, 220, 220`）。
  - 新增 `getPdfTextColor()`、`setPdfTextRgb(...)`。
  - 在 `setPdfTextRgb(...)` 中通过 MessageBus 发布新事件。
- 原因：
  - 需要持久化并统一管理“文字颜色”。
- 实现要点：
  - 与背景色逻辑保持一致的 clamp 策略（0~255）。
  - 仅当值变化时发布事件，避免无效重绘。

### 2) 扩展设置事件接口

- 文件：`src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettingsListener.java`
- 变更内容：
  - 新增 `pdfTextColorChanged(Color newTextColor)`。
- 原因：
  - 支持设置页 Apply 后在已显示 PDF 时实时重载。

### 3) 设置页增加“文字颜色(RGB)”输入

- 文件：`src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerConfigurable.java`
- 变更内容：
  - 新增 3 个 `JSpinner`（R/G/B）用于 PDF 文字颜色。
  - 在 `isModified()/apply()/reset()/disposeUIResources()` 中接入。
- 原因：
  - 提供用户可配置入口。
- 实现要点：
  - 与“背景色(RGB)”控件样式一致，保持设置页布局统一。
  - 不改现有“PDF 缩放(%)”语义。

### 4) 渲染算法增加“背景→文字色”单色映射

- 文件：`src/main/java/com/aiden/plugin/viewpdf/ui/PdfViewerPanel.java`
- 变更内容：
  - 新增字段 `textColor` 与 `setTextColor(Color)`。
  - 新增像素变换函数（例如 `toToneMap(...)`）：
    - 输入：原始页面图像、目标背景色、目标文字色。
    - 过程：按像素亮度计算插值，输出落在背景色到文字色区间的颜色。
  - 与现有模式结合：
    - 非夜间模式：保持现状（不做文字色映射）。
    - 夜间模式：将当前 `toGrayInvert(...)` 替换/升级为“背景-文字双端映射”。
- 原因：
  - 在不破坏原版式条件下，提供最接近“改文字颜色”的效果。
- 实现要点：
  - 先保留灰度化，再按亮度进行线性插值，保证对比度可读性。
  - 保持线程模型不变（继续在 `SwingWorker` 中处理位图）。

### 5) 工具窗初始化与设置事件接线

- 文件：`src/main/java/com/aiden/plugin/viewpdf/ui/PdfViewerToolWindowFactory.java`
- 变更内容：
  - 初始化时下发 `settings.getPdfTextColor()` 到 `PdfViewerPanel`。
  - 订阅 `pdfTextColorChanged`：
    - 更新 panel 的 textColor。
    - 若当前正在显示 PDF，则触发 `reload(...)`。
- 原因：
  - 保证配置变更即时生效且仅影响 PDF 显示区。

### 6) 文案与命名澄清（可选但建议）

- 文件：`src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerConfigurable.java`
- 变更内容：
  - 在“PDF 缩放（%）”旁增加简短说明（如“用于控制页面/文字显示大小”）。
- 原因：
  - 与“文字大小沿用缩放百分比”决策保持一致，减少认知歧义。

## Assumptions & Decisions

- 已确认决策：
  - 保持原版式（不切换文本重排阅读模式）。
  - “文字大小”沿用现有缩放百分比。
  - “文字颜色”采用单色映射近似实现。
  - 样式仅在 PDF 显示模式生效。
  - 图片型 PDF 不引入 OCR，本次沿用现有渲染路径。

- 约束说明：
  - 由于渲染为位图，无法做到“仅文本对象精准改色且图形不受影响”的完美语义级控制。
  - 本次目标为“实用可读的视觉个性化”，非版面语义编辑。

## Verification Steps

1. 构建验证：
   - 执行 `gradle build` 成功。

2. 功能验证（沙盒 IDE）：
   - 打开 `Settings -> XCode Viewer`，设置：
     - `PDF 背景色 (RGB)` 为较深色（如 `43,45,48`）。
     - `PDF 文字颜色 (RGB)` 为浅色（如 `220,220,220`）和高饱和色（如 `255,180,120`）分别测试。
     - `PDF 缩放（%）` 设为 `80/100/130` 对比效果。
   - 点击 Apply/OK 后切换到 PDF 视图，确认颜色与缩放生效。

3. 行为边界验证：
   - 在代码伪装区确认不受文字颜色设置影响。
   - 夜间模式开/关分别验证：
     - 夜间开：应用背景-文字映射。
     - 夜间关：保持当前普通渲染（不映射文字色）。

4. 回归验证：
   - 悬停自动显示/移出回伪装逻辑仍可用。
   - 全局快捷键（上滚/下滚/切换PDF）行为不变。

