## Summary

在现有 “PDF Viewer” 工具窗基础上增加“伪装模式/查看 PDF”能力：工具窗左右分栏，左侧显示当前项目目录树；右侧默认显示所选 Java 文件内容（仅 `.java` 点击有效，用于掩饰）；在标题栏按钮切换为 “查看PDF/取消伪装” 后，右侧切换为 PDF 预览，并将右侧内容区背景设置为 `RGB(43,45,48)`。同时保持原有夜间模式渲染与全局滚动快捷键，并按约定：快捷键仅在 PDF 模式生效。

## Current State Analysis

- 工具窗当前直接展示 [PdfViewerPanel](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/ui/PdfViewerPanel.java)，无伪装 UI（[PdfViewerToolWindowFactory](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/ui/PdfViewerToolWindowFactory.java)）。
- 全局滚动快捷键 Action 通过 `Project` 的 userData 查找 `PdfViewerPanel` 并滚动（[ScrollPdfUpAction](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/actions/ScrollPdfUpAction.java)、[ScrollPdfDownAction](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/actions/ScrollPdfDownAction.java)）。
- 夜间模式按钮是一个标题栏 `ToggleAction`，切换会触发重新渲染（[ToggleNightModeAction](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/ui/ToggleNightModeAction.java)、[PdfViewerSettings](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java)）。

## Goal & Success Criteria

- 工具窗变为左右分栏：
  - 左侧：展示“当前项目目录结构”的树（至少覆盖项目根目录下所有文件夹/文件）。
  - 右侧：默认显示“伪装内容区”：
    - 点击左侧树中的 `.java` 文件时，右侧显示该 Java 文件内容（只读即可）。
    - 点击非 `.java` 文件时，右侧保持不变（不提示），以减少暴露。
- 标题栏在“夜间模式”按钮旁增加一个按钮：
  - “查看PDF/取消伪装”切换右侧内容区：
    - 伪装模式 → PDF 模式：右侧显示 PDF 预览内容；右侧背景色强制为 `RGB(43,45,48)`。
    - PDF 模式 → 伪装模式：右侧显示 Java 文件内容区。
  - 工具窗初次打开默认处于伪装模式（不记忆上次状态）。
- 全局快捷键滚动：
  - Ctrl+Alt+PageUp/PageDown 仅在 PDF 模式生效；伪装模式下不做任何事。
- 夜间模式：
  - 保持现有“灰阶+反相”的 PDF 渲染逻辑不变；
  - 其开关仍为持久化设置项。

## Assumptions & Decisions

- 伪装模式默认开启（工具窗创建时即为伪装），不持久化“是否伪装”的状态。
- 左侧目录树为“掩饰用途”，以项目根目录（baseDir/guessProjectDir）为起点构建，优先可用性与稳定性，不追求与 IDE Project View 完全一致的分组/过滤规则。
- 右侧 Java 文件内容展示为只读（Viewer），不修改实际文件。
- 右侧 PDF 模式背景色强制设置为固定 RGB 值，与 IDE 主题无关。

## Proposed Changes (Files & What/Why/How)

### 1) 引入“工具窗控制器”并调整 userData Key

- 更新 [PdfViewerKeys.java](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/PdfViewerKeys.java)
  - 将 `Key<PdfViewerPanel>` 改为 `Key<PdfViewerToolWindowController>`（或新增新的 Key，不再让 Action 直接依赖 PdfViewerPanel）。
  - 目的：Action 能判断当前是否处于 PDF 模式，并在 PDF 模式时滚动 PDF。

- 新增 `src/main/java/com/aiden/plugin/viewpdf/ui/PdfViewerToolWindowController.java`
  - 持有：
    - `PdfViewerPanel pdfPanel`
    - `boolean pdfVisible`
    - 提供 `isPdfVisible()`、`setPdfVisible(boolean)`、`scrollPdfByViewport(boolean down)`。

### 2) 实现伪装 UI：目录树 + Java 文件只读预览

- 新增 `src/main/java/com/aiden/plugin/viewpdf/ui/DisguisePanel.java`
  - 右侧伪装内容区，提供 `JComponent getComponent()` 与 `setFile(VirtualFile file)`。
  - 内部使用 IntelliJ Editor 只读模式展示（优先方案）：
    - `Document doc = FileDocumentManager.getInstance().getDocument(file)`
    - `Editor editor = EditorFactory.getInstance().createViewer(doc, project)`
    - 设置 highlighter：`EditorHighlighterFactory.getInstance().createEditorHighlighter(project, file)`
    - 禁止编辑（viewer 默认只读）。
  - `setFile` 仅对 `.java` 生效；非 `.java` 调用时直接返回（保持不变）。

- 新增 `src/main/java/com/aiden/plugin/viewpdf/ui/ProjectTreePanel.java`
  - 左侧树：
    - 从项目根目录 `VirtualFile` 构建 `JBTree/JTree` 的模型（目录节点 + 文件节点）。
    - 监听选择变化：选择 `.java` 文件时调用 `DisguisePanel.setFile(file)`。
  - 目标是“看起来像项目结构”，无需完全复刻 IDE 的 Project View 细节。

### 3) 实现“右侧区域切换”：伪装 ↔ PDF

- 新增 `src/main/java/com/aiden/plugin/viewpdf/ui/StealthSplitPanel.java`
  - 结构：
    - `JSplitPane`：左 `ProjectTreePanel`，右 `CardLayout` 容器。
    - Card 1：`DisguisePanel`
    - Card 2：现有 `PdfViewerPanel` 的组件
  - 提供方法：
    - `showPdf(boolean show)` 切换卡片
    - `isPdfShown()`
    - `getPdfPanel()` 供 controller 与滚动 action 使用
  - PDF 模式背景色：
    - 在显示 PDF 时，将右侧 Card 容器、PDF scroll viewport、pages 容器背景统一设置为 `new Color(43,45,48)`（必要时同时设置 `setOpaque(true)`）。

### 4) 调整 ToolWindowFactory：使用新分栏 UI，并增加“查看PDF/取消伪装”标题栏按钮

- 更新 [PdfViewerToolWindowFactory](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/ui/PdfViewerToolWindowFactory.java)
  - 创建 `StealthSplitPanel` 替代直接创建 `PdfViewerPanel`。
  - 创建并存储 `PdfViewerToolWindowController` 到 `Project` userData key。
  - 默认显示伪装模式（`pdfVisible=false`）。
  - `PdfViewerSettingsListener` 的响应：
    - PDF 路径变化/夜间模式变化时，如果当前处于 PDF 模式则调用 `pdfPanel.reload(...)`；
    - 若处于伪装模式可选择不渲染 PDF（减少暴露/减少开销）。
  - 标题栏 actions：
    - 保留现有 [ToggleNightModeAction](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/ui/ToggleNightModeAction.java)
    - 新增一个 `ToggleAction`：`ToggleDisguiseAction`（显示名“查看PDF”/“取消伪装”）

- 新增 `src/main/java/com/aiden/plugin/viewpdf/ui/ToggleDisguiseAction.java`
  - `isSelected` 绑定 controller 的 `isPdfVisible()`（选中表示正在查看 PDF）。
  - `setSelected` 切换右侧卡片：
    - 切到 PDF：调用 `pdfPanel.reload(settings.getPdfPath(), settings.isNightModeEnabled())`，并设置背景色。
    - 切到伪装：仅切换卡片，不做其它提示。

### 5) 调整全局滚动 Action：仅在 PDF 模式生效

- 更新 [ScrollPdfUpAction](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/actions/ScrollPdfUpAction.java)
- 更新 [ScrollPdfDownAction](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/actions/ScrollPdfDownAction.java)
  - 从 `Project` userData 获取 `PdfViewerToolWindowController`。
  - `update`：仅当 controller 存在且 `isPdfVisible()==true` 时启用。
  - `actionPerformed`：仅在 `isPdfVisible()` 时滚动 PDF，否则直接返回。

## Data Flow

- 左侧树选择 `.java` 文件 → `DisguisePanel.setFile(file)` → 右侧更新为该文件只读预览。
- 标题栏“查看PDF”切换 → `ToggleDisguiseAction` → controller/StealthSplitPanel 切换卡片：
  - 切到 PDF 时触发 `pdfPanel.reload(path, nightMode)` 并设置右侧背景色。
- Settings 中变更 PDF 路径/夜间模式 → MessageBus 通知 ToolWindow → 若当前在 PDF 模式则刷新渲染；伪装模式下不触发渲染（减少暴露与开销）。
- 全局快捷键触发 → Action 查询 controller → 仅 PDF 模式滚动。

## Edge Cases / Failure Modes

- 工具窗在无 project 或 project 根目录不可用时：左侧树显示空/占位（不弹窗提示，避免暴露）。
- 未选择 PDF 或 PDF 不存在：
  - 在切换到 PDF 模式时，右侧沿用现有 PdfViewerPanel 提示文本（已实现）。
- 大项目目录树构建耗时：
  - 初版允许同步构建；如卡顿明显再迭代为后台构建与懒加载节点。

## Verification Steps

1. `gradlew build` 通过（确保 Gradle JVM 使用 JDK 17+）。
2. `gradlew runIde` 启动沙盒 IDE 后：
   - 打开底部 “PDF Viewer” 工具窗：
     - 默认显示左右分栏，右侧为伪装内容区。
   - 点击左侧树中的任意 `.java` 文件：
     - 右侧显示该文件内容；点击非 `.java` 文件右侧保持不变。
   - 在编辑器区域按 Ctrl+Alt+PageUp/PageDown：
     - 伪装模式下不滚动（无效果）。
   - 点击标题栏 “查看PDF/取消伪装”：
     - 切到 PDF 模式后右侧显示 PDF；右侧背景为 RGB(43,45,48)。
     - 再次点击恢复伪装模式。
   - 切到 PDF 模式时，Ctrl+Alt+PageUp/PageDown 可以滚动 PDF。
   - 夜间模式按钮仍可切换 PDF 渲染效果，并保持持久化。

