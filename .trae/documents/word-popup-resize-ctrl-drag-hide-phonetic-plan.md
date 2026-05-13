# 单词悬浮框缩放与 Ctrl 拖动改造计划

## Summary
- 目标：单词悬浮框支持鼠标直接缩放大小，并在缩放后自动保存宽高到配置。
- 目标：单词悬浮框支持 `Ctrl + 鼠标左键` 拖动，且允许拖出编辑器区域。
- 目标：保留单词文本显示，但不显示音标（你已更正为“隐藏音标”）。
- 目标：删除“切换已学会状态”动作的默认快捷键注册（保留动作入口与右键菜单能力）。

## Current State Analysis
- `WordPopupController` 当前通过 `JBPopupFactory.createComponentPopupBuilder(...)` 创建悬浮框，显式设置了：
  - `.setMovable(false)`
  - `.setResizable(false)`
  - 说明：目前既不能拖动也不能缩放。
- `WordPopupController` 当前位置计算在 `resolvePopupLocation(...)` 中，对 `x/y` 做了编辑器边界内 clamp（`0..max`），会阻止拖出编辑器的定位语义。
- `WordPopupController` 当前样式持久化入口是 `PdfViewerSettings.setWordPopupStyle(width,height,x,y,fontSize,fontColor...)`，已具备保存悬浮框尺寸与位置字段能力（`wordPopupWidth/Height/X/Y`）。
- `EditorPdfPopupController` 已有可复用交互：
  - 通过 `.setResizable(true)` 启用鼠标缩放。
  - 通过 `MouseAdapter` 实现 `Ctrl` 按下时拖动窗口位置。
  - 通过 `ComponentListener` 监听窗口移动/缩放并同步保存状态。

## Proposed Changes
### 1) 单词悬浮框启用鼠标缩放
- 文件：`src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java`
- What：
  - 将 `JBPopup` 构建选项由 `.setResizable(false)` 改为 `.setResizable(true)`。
  - 保持 `setMovable(false)`，避免与“仅 Ctrl+左键拖动”冲突。
- Why：
  - 满足“通过鼠标改变大小”的显性需求。
- How：
  - 在弹框展示后绑定窗口级 `ComponentListener`，监听 `componentResized`，实时把窗口实际宽高写回 `PdfViewerSettings.setWordPopupStyle(...)`（保留现有字体/颜色/坐标）。

### 2) 增加 Ctrl+鼠标左键拖动（允许拖出编辑器）
- 文件：`src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java`
- What：
  - 在 `contentPanel`（及必要子组件）挂载 `MouseAdapter`：
    - `mousePressed`：仅当 `Ctrl` + 左键按下时记录锚点与窗口起点。
    - `mouseDragged`：按屏幕坐标增量移动窗口。
    - `mouseReleased`：清空拖动状态。
  - 拖动后保存新位置（`x/y`）到设置。
- Why：
  - 满足“Ctrl+鼠标左键拖动”且“可拖出编辑器”的操作要求。
- How：
  - 参考 `EditorPdfPopupController` 的拖动逻辑实现。
  - 持久化时使用相对编辑器坐标（可为负值或超出可视范围），不再强制 clamp 到编辑器内部。

### 3) 位置解析从“编辑器内约束”改为“可越界”
- 文件：`src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java`
- What：
  - 调整 `resolvePopupLocation(...)`：移除 `0..max` 的强制边界限制。
  - 允许使用保存的 `x/y` 原值定位，支持在编辑器外显示。
- Why：
  - 当前 clamp 与“可拖出编辑器”行为矛盾。
- How：
  - 仍保留坐标的安全下限/上限约束，复用 `PdfViewerSettings` 现有 `wordPopupX/Y` clamp（`-5000..5000`）。

### 4) 隐藏音标显示（保留单词）
- 文件：`src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java`
- What：
  - `refreshContent()` 中 `phoneticLabel` 固定隐藏/置空，不参与渲染。
  - `wordLabel` 保持原有显示（含“已学会”状态标记）。
- Why：
  - 对齐你更正后的需求：“不是隐藏单词，而是隐藏音标”。
- How：
  - 将 `phoneticLabel.setVisible(false)` 并清空文本；必要时同步调整布局间距，避免留白异常。

### 5) 与设置变更联动保持一致
- 文件：
  - `src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java`
  - `src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java`（仅在需要细化写回方法时）
- What：
  - 保证用户手动缩放/拖动后，设置中的 `wordPopupWidth/Height/X/Y` 会被更新，下次打开沿用。
- Why：
  - 满足“自动保存”决策。
- How：
  - 复用现有 `setWordPopupStyle(...)`；若写回频率过高，增加轻量去重（值未变化不写）。

### 6) 删除“切换已学会状态”快捷键注册
- 文件：`src/main/resources/META-INF/plugin.xml`
- What：
  - 移除 `com.aiden.plugin.viewpdf.actions.MarkWordMastered` 动作上的 `<keyboard-shortcut ... first-keystroke="ctrl alt M"/>`。
- Why：
  - 对齐你新增要求：“删除快捷键注册：切换已学会状态”。
- How：
  - 仅删除快捷键绑定声明，不删除动作本身与菜单项，避免影响右键切换与命令入口。

## Assumptions & Decisions
- 已确认决策：
  - 允许拖出编辑器范围。
  - 鼠标缩放后自动持久化宽高（并一并持久化位置）。
  - 单词继续显示，音标隐藏。
- 默认保持：
  - 现有快捷键（显示/隐藏、下一个、切换已学会）不变。
  - 现有快捷键中“切换已学会状态”的默认绑定将移除。
  - 右键“已学会/未学会”菜单不变。

## Verification steps
- 交互验证：
  - 显示单词悬浮框后，可鼠标拖拽边缘缩放。
  - 按住 `Ctrl` + 鼠标左键可拖动，且可拖出编辑器区域。
  - 释放后再次显示悬浮框，尺寸与位置保持为上次状态。
- 显示验证：
  - 单词文本仍显示。
  - 音标不再显示，布局无明显空白错位。
- 回归验证：
  - `Ctrl+Alt+W/N` 行为不回退。
  - `Ctrl+Alt+M` 不再触发“切换已学会状态”。
  - 右键切换已学会逻辑不回退。
- 工程验证：
  - 修改文件 `GetDiagnostics` 无新增错误。
  - `E:\java\jdk-21.0.1` 下 `.\gradlew.bat test --no-daemon` 通过。
