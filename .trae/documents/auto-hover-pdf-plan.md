## Summary

新增“鼠标悬停自动显示 PDF”的机制：鼠标进入右侧内容区（伪装/预览区域）停留超过 10 秒时自动切换为 PDF 预览；鼠标离开右侧内容区则立刻切回代码。该自动切换在用户手动点击“查看PDF/取消伪装”后停止生效（尊重手动操作）。

## Current State Analysis

- 右侧内容区通过 `StealthSplitPanel` 的 `CardLayout` 在伪装与 PDF 之间切换（[StealthSplitPanel](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/ui/StealthSplitPanel.java)）。
- 手动切换由标题栏 `ToggleDisguiseAction` 控制（[ToggleDisguiseAction](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/ui/ToggleDisguiseAction.java)）。
- 伪装内容区是 `DisguisePanel`，本质上是一个 `JPanel` 包裹只读 Editor（[DisguisePanel](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/ui/DisguisePanel.java)）。

## Goal & Success Criteria

- 右侧内容区（无论当前是伪装还是 PDF）：
  - 鼠标进入并停留 **10 秒** → 自动切换为 PDF 预览。
  - 鼠标离开 → **立刻**切回伪装显示代码。
- 自动切换机制在用户**手动点击**“查看PDF/取消伪装”之后**禁用**（尊重手动）。
  - 手动切换后，悬停与离开都不再触发自动切换。

## Assumptions & Decisions

- “内容区”指 `StealthSplitPanel` 右侧 `CardLayout` 容器区域（不含左侧目录树）。
- 自动显示触发 10 秒使用 Swing `Timer`（EDT 上运行）。
- 只在当前工具窗会话内禁用自动逻辑；关闭/重开工具窗后自动逻辑恢复可用。

## Proposed Changes (Files & What/Why/How)

### 1) StealthSplitPanel: 增加自动悬停控制逻辑

- 更新 [StealthSplitPanel](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/ui/StealthSplitPanel.java)
  - 新增状态字段：
    - `boolean autoEnabled = true`
    - `Timer hoverTimer`（10 秒）
  - 新增方法：
    - `setAutoEnabled(boolean enabled)`：手动操作后调用，禁用自动逻辑。
  - 在 `rightCards` 上注册 `MouseListener/MouseMotionListener`：
    - `mouseEntered`：若 `autoEnabled`，启动/重置 10 秒计时。
    - `mouseMoved`：若 `autoEnabled`，重置计时（保持“连续停留 10 秒”语义）。
    - `mouseExited`：若 `autoEnabled`，立即切回伪装（`showPdf(false)`），并停止计时。
  - 计时器触发时：若 `autoEnabled` 且当前非 PDF，则调用 `showPdf(true)` + 触发 PDF 加载。
    - PDF 加载由外部 `ToggleDisguiseAction` 逻辑驱动，因此在本类内部提供一个回调接口或 Runnable 交由 ToolWindowFactory 注入。

### 2) ToolWindowFactory: 注入“自动切换时的加载逻辑”

- 更新 [PdfViewerToolWindowFactory](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/ui/PdfViewerToolWindowFactory.java)
  - 在创建 `StealthSplitPanel` 后，设置一个回调：
    - 当自动切换显示 PDF 时，调用 `pdfPanel.reload(settings.getPdfPath(), settings.isNightModeEnabled())`。
  - 维持现有 MessageBus 监听逻辑不变。

### 3) ToggleDisguiseAction: 手动操作后禁用自动切换

- 更新 [ToggleDisguiseAction](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/ui/ToggleDisguiseAction.java)
  - 在 `setSelected` 中加入 `splitPanel.setAutoEnabled(false)`。
  - 使“手动切换后自动逻辑失效”。

## Data Flow

- 鼠标进入右侧内容区 → 计时 10 秒 → 自动显示 PDF（并触发 `reload`）。
- 鼠标离开右侧内容区 → 立即切回伪装。
- 用户点击“查看PDF/取消伪装” → `autoEnabled=false`，后续不再自动切换。

## Edge Cases / Failure Modes

- 用户手动进入 PDF 后移出右侧区域：因为 `autoEnabled=false`，不会自动切回。
- 未选择 PDF 时自动切换：`PdfViewerPanel` 仍显示“未选择 PDF”提示。

## Verification Steps

1. 使用全局 Gradle 8.10.1（不使用 wrapper）启动沙盒 IDE，打开 XCode Viewer 工具窗。
2. 将鼠标移入右侧内容区，保持不动 10 秒：自动显示 PDF。
3. 将鼠标移出右侧内容区：立刻切回伪装显示代码。
4. 手动点击“查看PDF/取消伪装”后：
   - 无论鼠标进入/移出，都不再自动切换。
