# PDF 区域鼠标拖动滚动优化计划

## Summary
- 目标：在底部中间 PDF 阅读区与编辑器弹框 PDF 区增加“鼠标拖动滚动（平移）”能力，降低仅依赖滚轮翻页的操作成本。
- 已确认交互决策：
- 拖动手势：左键拖动触发平移。
- 横向滚动：仅在存在水平滚动条时生效（有横向可滚动空间才左右平移）。
- 弹框冲突规则：`Ctrl + 拖动` 优先用于移动弹框，PDF 平移不抢占该手势。
- 成功标准：
- 非 `Ctrl` 左键拖动可平滑上下滚动；有横向滚动条时可左右滚动。
- 弹框内 `Ctrl + 拖动` 仍稳定移动窗口，不被平移逻辑干扰。
- 底部中间区与弹框区行为一致（共用 `PdfViewerPanel`）。

## Current State Analysis
- 底部中间区与弹框都复用同一组件：`PdfViewerPanel`。
- 现有 `PdfViewerPanel` 已有滚轮/快捷键滚动、滚动条自动隐藏（滚动时显示，静止后隐藏）。
- 当前水平滚动策略为：`HORIZONTAL_SCROLLBAR_NEVER`，因此一般不会显示水平滚动条。
- 弹框当前已有 `Ctrl + 拖动` 的窗口移动监听，挂在 `contentPanel` 和 `pdfPanel` 区域。
- 风险点：
- 左键拖动平移与弹框移动、区域现有 mouse listener 可能冲突，必须按修饰键优先级分流。
- 现有 PDF 区域有离开/悬停监听（底部区），需避免被拖动逻辑破坏。

## Proposed Changes
### 1) `src/main/java/com/aiden/plugin/viewpdf/ui/PdfViewerPanel.java`
- 新增“拖动平移”内置能力（组件级）：
- 增加启用开关与上下文标记（用于区分弹框/底部场景是否允许某些冲突处理）。
- 在 `scrollPane`/`viewport`/页内容上绑定统一拖动监听。
- 拖动规则：
- 仅左键按下并拖动时执行平移。
- 若检测到 `Ctrl` 按下，则不执行平移（让上层弹框移动逻辑接管）。
- 平移实现：
- 记录按下点与滚动条初值，拖动时反向设置 `verticalScrollBar` 值实现“抓手拖动”体验。
- 若 `horizontalScrollBar` 可见且可滚动，则同步设置其值完成左右平移。
- 与现有滚动条自动显示逻辑联动：拖动期间与拖动后继续触发“临时显示滚动条”计时。

### 2) `src/main/java/com/aiden/plugin/viewpdf/popup/EditorPdfPopupController.java`
- 保持现有 `Ctrl + 拖动 = 移动弹框` 逻辑不变。
- 将拖动平移启用到弹框内 PDF 组件：
- 初始化时调用 `pdfPanel` 的拖动平移启用方法。
- 冲突处理顺序：
- 先判断 `Ctrl`，`Ctrl` 按下走弹框移动。
- 非 `Ctrl` 的左键拖动交由 `PdfViewerPanel` 平移处理。
- 避免重复监听导致抖动：确保同类监听仅注册一次。

### 3) `src/main/java/com/aiden/plugin/viewpdf/ui/StealthSplitPanel.java`
- 底部中间 PDF 区初始化时启用同一套拖动平移能力。
- 保持现有 `pdfExitListener`（移出切回伪装）逻辑，仅避免拖动过程中误触发（必要时通过小阈值/状态标记抑制）。

## Assumptions & Decisions
- 决策 1：拖动平移采用左键，不新增设置项（先按固定交互落地）。
- 决策 2：`Ctrl` 修饰键在弹框场景优先保留“移动窗口”语义。
- 决策 3：横向平移仅在真实存在横向可滚动空间时生效，不强制开启横向滚动条。
- 决策 4：本次不改 PDF 渲染与分页策略，仅增强输入交互层。

## Verification Steps
- 底部中间 PDF 区：
- 左键拖动可上下平移；若存在横向滚动条，可左右平移。
- 拖动后阅读位置保存/恢复逻辑不回退。
- 现有悬停自动切换与移出切回伪装不出现明显误触。
- 编辑器弹框 PDF 区：
- 左键拖动平移正常。
- `Ctrl + 左键拖动` 仍移动弹框，不触发平移。
- 边缘缩放能力保持。
- 回归检查：
- `GetDiagnostics` 对修改文件无新增报错。
- 至少执行一次 `./gradlew compileJava`（在 JDK17+ 环境）验证通过。
