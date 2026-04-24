# 三栏布局改造计划（左树 + 中切换 + 右代码）

## Summary
- 目标：将当前底部两栏改为三栏布局，形态为“左侧项目树 + 中间切换区(PDF/伪装) + 右侧始终代码区”。
- 关键结果：
  - 右侧代码区始终可见，且跟随左侧树点击文件更新（仅 `.java` 生效）。
  - 三栏支持拖拽调节宽度。
  - 三栏宽度比例全局持久化，重启 IDE 后恢复。
  - 设置页可配置左/中/右三个百分比（自动归一化），并实时应用。
- 已确认偏好：
  - 布局：`左树 + 中切换 + 右代码`
  - 代码来源：`跟随左树选中`
  - 默认比例：`25/45/30`
  - 记忆范围：`全局统一`
  - 设置方式：`三个百分比输入`

## Current State Analysis
- 当前布局在 `StealthSplitPanel` 中由单个 `JSplitPane` 构成：左侧 `ProjectTreePanel`，右侧 `CardLayout`（`DisguisePanel` / `PdfViewerPanel`）。
- 左树点击文件的逻辑在 `ProjectTreePanel` 内触发，目前直接调用 `DisguisePanel.setFile(vf)`，且通过回调切回伪装区。
- 右侧伪装代码组件 `DisguisePanel` 负责创建只读 `Editor` 预览，支持 hover 监听挂载。
- 设置持久化由 `PdfViewerSettings` 统一管理，设置页由 `PdfViewerConfigurable` 维护，现已承载多个数值配置。
- 工具窗初始化与设置变更订阅在 `PdfViewerToolWindowFactory` 内集中处理。

## Proposed Changes

### 1) `StealthSplitPanel`：由两栏升级为三栏（嵌套 `JSplitPane`）
- 文件：`src/main/java/com/aiden/plugin/viewpdf/ui/StealthSplitPanel.java`
- 变更点：
  - 新增一个“右侧固定代码区”组件实例（建议复用 `DisguisePanel` 作为 `fixedCodePanel`）。
  - 将布局改为嵌套分栏：
    - 外层：`leftTree | rightGroup`
    - 内层：`middleCards(PDF/伪装) | fixedCodePanel`
  - 保持中间区域现有卡片切换逻辑与 hover/PDF 离开检测逻辑不变，只调整其挂载容器。
  - 新增比例应用与读取方法（供工厂初始化和监听使用）：
    - `setPaneRatios(int leftPercent, int middlePercent, int rightPercent)`
    - `captureCurrentPaneRatios(): int[3]`（按当前 divider 位置换算百分比）
  - 给两个 `JSplitPane` 增加 `PropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY)`，在用户拖拽后回写比例到 settings（可做轻量防抖，避免高频写入）。

### 2) `ProjectTreePanel`：解耦“选中文件后的渲染目标”
- 文件：`src/main/java/com/aiden/plugin/viewpdf/ui/ProjectTreePanel.java`
- 变更点：
  - 移除构造参数中对 `DisguisePanel` 的硬依赖，改为仅通过回调传递选中文件。
  - 保留 `onFileClicked` 回调语义，由 `StealthSplitPanel` 决定：
    - 中间伪装区是否更新；
    - 右侧固定代码区更新；
    - 是否切回中间伪装卡片（沿用现有行为）。
- 目的：一处点击，同时驱动“中间伪装区 + 右侧固定代码区”。

### 3) `PdfViewerSettings`：新增三栏比例持久化字段与约束
- 文件：`src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java`
- 变更点：
  - `StateData` 新增：
    - `paneLeftPercent`
    - `paneMiddlePercent`
    - `paneRightPercent`
  - 新增默认值：`25/45/30`。
  - 新增 getter/setter：
    - 自动 clamp 到 `[5, 90]`（避免极端不可见）
    - 自动归一化到总和 100
  - 新增监听事件发布（建议一次性事件）：
    - `paneRatiosChanged(int left, int middle, int right)`

### 4) `PdfViewerSettingsListener`：扩展比例变更事件
- 文件：`src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettingsListener.java`
- 变更点：
  - 增加 `paneRatiosChanged(...)` 方法，供 UI 实时刷新分栏比例。

### 5) `PdfViewerConfigurable`：新增三栏比例输入（每项独占一行）
- 文件：`src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerConfigurable.java`
- 变更点：
  - 新增三个 spinner：
    - `左侧宽度(%)`
    - `中间宽度(%)`
    - `右侧宽度(%)`
  - 在 `isModified/apply/reset/disposeUIResources` 全量接线。
  - `apply()` 时调用 settings 的归一化 setter，避免用户输入和不为 100 导致异常。
- 交互策略：
  - 用户在设置页可直接输入比例；
  - 运行时拖拽 divider 后也会更新 settings（与设置页保持同一数据源）。

### 6) `PdfViewerToolWindowFactory`：初始化比例 + 监听比例变化
- 文件：`src/main/java/com/aiden/plugin/viewpdf/ui/PdfViewerToolWindowFactory.java`
- 变更点：
  - 工具窗创建时读取 settings 比例并调用 `splitPanel.setPaneRatios(...)`。
  - 在 message bus 订阅中处理 `paneRatiosChanged(...)`，实时更新当前打开工具窗布局。

### 7) 右侧固定代码区展示规则（行为定义）
- 左树点击文件时：
  - 若为 `.java`：右侧固定代码区刷新到该文件；
  - 若非 `.java`：保持右侧固定代码区现状（不清空、不跳转）。
- 中间区域行为保持现有逻辑：
  - 点击任意文件会切回中间伪装区；
  - PDF 开关和悬停自动切换规则不变。

## Assumptions & Decisions
- 不新增第四种视图模式，仍仅中间区域在 `PDF/伪装` 两态切换。
- 三栏比例以全局设置持久化，不做“按项目独立”。
- 默认比例采用 `25/45/30`，并允许拖拽后覆盖。
- 设置页和拖拽是同一个真值源（`PdfViewerSettings`），互相同步。
- 非 `.java` 不更新固定代码区，避免“伪装”失真和高频无效重建 Editor。

## Verification Steps
- 手动验证：
  - 打开工具窗后看到三栏：左树 / 中切换 / 右代码。
  - 点击左树 `.java`：右侧固定代码区更新；中间切回伪装区。
  - 开启查看 PDF：仅中间切换到 PDF，右侧代码区保持可见。
  - 拖动两个 divider 改宽度，关闭并重开 IDE 后比例恢复。
  - 在设置页修改左/中/右百分比，当前工具窗实时变化。
  - 输入非 100 总和（如 30/30/30），保存后应被归一化并稳定展示。
- 兼容与回归：
  - 既有功能不退化：PDF 滚动快捷键、悬停自动显示 PDF、移出回伪装、夜间/颜色/缩放设置仍可用。
  - `GetDiagnostics` 对改动文件无新增错误。
