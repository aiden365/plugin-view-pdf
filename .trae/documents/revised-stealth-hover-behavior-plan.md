## Summary

按“简化规则”重构伪装/PDF 切换机制：
- 默认始终显示代码伪装区。
- 仅当“PDF 查看开关”开启时，才允许显示 PDF，并且开启后立即显示 PDF。
- 即使开关处于开启状态，点击左侧任意文件也立刻切回代码伪装区（仅 Java 文件更新右侧代码内容，非 Java 仅切换不更新内容）。
- 当且仅当开关开启时，鼠标在代码区悬停达到可配置秒数（默认 10 秒）后自动切到 PDF。
- 开关关闭时，无论悬停多久都不显示 PDF。

## Current State Analysis

- 当前 `StealthSplitPanel` 已有 hover timer 逻辑，但逻辑是“自动启用/手动后禁用”，且鼠标移出会自动切回，不符合新规则（[StealthSplitPanel](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/ui/StealthSplitPanel.java)）。
- `ToggleDisguiseAction` 手动切换时会 `setAutoEnabled(false)`，这与“开关开启后允许悬停自动显示 PDF”冲突（[ToggleDisguiseAction](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/ui/ToggleDisguiseAction.java)）。
- 目前没有“悬停秒数”配置项，设置里已有 PDF 路径、夜间模式、背景色（[PdfViewerSettings](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java)）。
- 左侧树点击当前只在文件选择时更新内容，没有对“切回代码区”的显式控制回调（[ProjectTreePanel](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/ui/ProjectTreePanel.java)）。

## Goal & Success Criteria

- 默认进入工具窗始终显示代码伪装区。
- 打开“查看PDF”开关后立即显示 PDF。
- 开关开启状态下：点击左侧任意文件都切回代码伪装区；其中仅 Java 文件会更新右侧代码内容。
- 开关开启状态下：鼠标在代码区悬停达到 N 秒（默认 10，可配置）自动显示 PDF。
- 开关关闭状态下：鼠标悬停永不触发 PDF。

## Assumptions & Decisions

- 悬停触发区域固定为“右侧代码伪装区（disguise card）”，不含左侧目录树与右侧 PDF card。
- 悬停秒数配置为应用级持久化，单位秒，默认 `10`，最小值 `1`（通过 UI 限制）。
- 点击左侧任意文件都会切回代码区；是否更新文本仍保持“仅 Java 文件有效”。

## Proposed Changes (Files & What/Why/How)

### 1) 增加悬停秒数配置（默认 10 秒）

- 更新 [PdfViewerSettings](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java)
  - `StateData` 新增 `Integer autoShowPdfHoverSeconds`。
  - 新增 `getAutoShowPdfHoverSeconds()` / `setAutoShowPdfHoverSeconds(int seconds)`，默认 10，最小 1。
  - 可选：通过 message bus 发出 hover 秒数变化通知（若需要运行时实时应用）。

- 更新 [PdfViewerSettingsListener](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettingsListener.java)
  - 增加 `hoverSecondsChanged(int seconds)`（若采用实时更新）。

- 更新 [PdfViewerConfigurable](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerConfigurable.java)
  - 增加“悬停自动显示 PDF 秒数”输入控件（`JSpinner`，最小 1）。
  - 在 `isModified/apply/reset` 中接入持久化字段。

### 2) 重写 StealthSplitPanel 的自动逻辑为“开关门控”

- 更新 [StealthSplitPanel](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/ui/StealthSplitPanel.java)
  - 去掉“手动后禁用自动”的 `autoEnabled` 语义，改为：
    - `boolean pdfToggleEnabled`（由标题栏开关控制）
    - `int hoverSeconds`
  - 新增方法：
    - `setPdfToggleEnabled(boolean enabled)`：开关状态同步到 panel。
    - `setHoverSeconds(int seconds)`：更新计时阈值。
    - `showDisguise()`：显式切回代码区并停止 hover timer。
    - `showPdf()`：显式显示 PDF。
  - hover 监听改为仅绑定在 `disguisePanel.getComponent()` 上：
    - `mouseEntered/mouseMoved`：仅当 `pdfToggleEnabled=true` 时启动/重置 timer。
    - timer 到时：若 `pdfToggleEnabled=true` 且当前在代码区，切到 PDF 并执行 reload 回调。
    - 不再使用“mouseExited 即刻切回”的逻辑（由“点击文件切回”规则主导）。

### 3) 开关行为改造：仅控制“允许 PDF + 立即显示 PDF”

- 更新 [ToggleDisguiseAction](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/ui/ToggleDisguiseAction.java)
  - 删除 `splitPanel.setAutoEnabled(false)`。
  - `state=true`：
    - `splitPanel.setPdfToggleEnabled(true)`
    - 立即 `showPdf()` + `reload(...)`
  - `state=false`：
    - `splitPanel.setPdfToggleEnabled(false)`
    - 强制 `showDisguise()`

### 4) 左侧任意文件点击都切回代码区

- 更新 [ProjectTreePanel](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/ui/ProjectTreePanel.java)
  - 增加文件点击回调（如 `Consumer<VirtualFile>` 或 `Runnable`），任何文件节点点击都触发。
  - 保留现有 `disguisePanel.setFile(vf)`：仅 Java 文件更新内容。

- 更新 [StealthSplitPanel](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/ui/StealthSplitPanel.java)
  - 构造 `ProjectTreePanel` 时注入“文件点击回调”：
    - 回调中执行 `showDisguise()`；
    - 如果开关仍开启，不改变开关状态，仅切显示卡片（满足“开启状态点击文件也切回代码区”）。

### 5) ToolWindowFactory 接线

- 更新 [PdfViewerToolWindowFactory](file:///e:/workspace/java/plugin-view-pdf/src/main/java/com/aiden/plugin/viewpdf/ui/PdfViewerToolWindowFactory.java)
  - 初始化时：
    - `splitPanel.setPdfToggleEnabled(false)`
    - `splitPanel.showDisguise()`（确保默认代码区）
    - `splitPanel.setHoverSeconds(settings.getAutoShowPdfHoverSeconds())`
  - 订阅设置变更：
    - 背景色变更沿用现有逻辑；
    - 悬停秒数变更时更新 `splitPanel.setHoverSeconds(...)`（若采用事件）。

## Data Flow

- 启动工具窗：默认代码区，开关关闭。
- 打开开关：panel 进入“允许 PDF”状态并立即显示 PDF。
- 点击左侧任意文件：立即切回代码区；若是 Java 文件同步更新代码内容。
- 开关开启 + 鼠标在代码区悬停达到 N 秒：自动切到 PDF。
- 开关关闭：悬停计时不生效，永不自动显示 PDF。

## Edge Cases / Failure Modes

- 开关开启但未配置 PDF 路径：自动/手动显示 PDF 时继续显示已有“未选择 PDF”提示。
- 非 Java 文件点击：仅切回代码区，不更新代码文本（保持掩饰一致性）。
- 频繁移动鼠标：timer 重置，不会误触发。

## Verification Steps

1. 使用全局 Gradle 8.10.1 构建并启动沙盒 IDE。
2. 打开工具窗后确认默认显示代码伪装区（非 PDF）。
3. 打开“查看PDF”开关，确认立即显示 PDF。
4. 开关保持开启，点击左侧任意文件：
   - 立即切回代码区；
   - 点击 Java 文件时右侧内容更新，非 Java 不更新内容。
5. 在开关开启且当前代码区时，鼠标停留 N 秒（默认 10）后自动切回 PDF。
6. 关闭开关后，无论停留多久都不自动显示 PDF。
7. 在设置修改悬停秒数并 Apply 后，按新秒数生效。

