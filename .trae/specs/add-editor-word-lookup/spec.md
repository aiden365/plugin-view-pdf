# 编辑器 Lookup 背单词 Spec

## Why
现有背单词以悬浮弹框展示，容易遮挡视线且与编码心流割裂。通过在编辑器内以补全样式列表（Lookup）展示单词与固定操作项，可获得更一致的键盘交互体验且不污染代码。

## What Changes
- 新增一个可配置快捷键的编辑器动作，用于在当前编辑器光标处弹出 Lookup 列表
- Lookup 列表固定 4 个条目：当前单词、Next、Prev、Learn
- Lookup 打开期间屏蔽除 ↑/↓/Enter/Esc 之外的键盘输入，避免过滤与文本插入
- Next/Prev 通过关闭并重建 Lookup 刷新内容；Learn 仅标记已学会并保持当前单词与 Lookup 打开状态
- 复用现有背单词数据源与持久化（PdfViewerSettings / WordLibraryLoader / MasteredWordLibrary）
- 新功能采用独立的“编辑器 Lookup 会话”状态管理（Project 级），不复用/不依赖现有悬浮框控制器，从而尽可能降低对原有弹框模式的影响
- 原有弹框背单词模式与现有 Actions（显示/隐藏、Next、Prev、切换已学会）保持不变

## Impact
- Affected specs: 编辑器内背单词交互、快捷键动作体系、背单词状态持久化一致性
- Affected code:
  - 动作注册：[plugin.xml](file:///e:/workspace/java/xcode-tools/src/main/resources/META-INF/plugin.xml)
  - 编辑器 Lookup 新增实现：新增 Editor Lookup 相关 Action/会话类（新文件）
  - 学习状态与词库加载复用：[PdfViewerSettings](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java)、[WordLibraryLoader](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/settings/WordLibraryLoader.java)
  - 原有弹框模式不改动：[WordPopupController](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java)、既有 Actions 与快捷键

## ADDED Requirements
### Requirement: 编辑器 Lookup 背单词
系统 SHALL 提供一个编辑器内动作，通过快捷键在当前编辑器中弹出补全样式列表（Lookup），用于展示当前单词并提供固定操作项。

#### Scenario: 成功唤起并关闭
- **WHEN** 用户在代码编辑器内触发“编辑器 Lookup 背单词”动作
- **THEN** 在光标处弹出 Lookup 列表
- **AND** **WHEN** 用户按下 Esc
- **THEN** Lookup 关闭且不改变文档内容

#### Scenario: Lookup 条目固定且不污染代码
- **WHEN** Lookup 弹出
- **THEN** 列表固定 4 个条目且顺序固定：
  - 当前单词（仅显示单词文本）
  - Next
  - Prev
  - Learn
- **AND** **WHEN** 用户选择任意条目并按 Enter
- **THEN** 不向编辑器插入任何文本（包括单词、Next、Prev、Learn）

#### Scenario: Next 切换到下一个单词并刷新
- **WHEN** Lookup 打开且用户选择 Next 并按 Enter
- **THEN** 当前单词切换为下一个待学习单词
- **AND** Lookup 内容刷新为新单词与固定操作项

#### Scenario: Prev 切换到上一个单词并刷新
- **WHEN** Lookup 打开且用户选择 Prev 并按 Enter
- **THEN** 当前单词切换为上一个待学习单词
- **AND** Lookup 内容刷新为新单词与固定操作项

#### Scenario: Learn 标记已学会且停留当前单词
- **WHEN** Lookup 打开且用户选择 Learn 并按 Enter
- **THEN** 当前单词学习状态切换为“已学会/未学会”并持久化
- **AND** 不自动切换到下一个单词
- **AND** Lookup 保持打开且仍显示当前单词与固定操作项

#### Scenario: 屏蔽输入，不触发过滤
- **WHEN** Lookup 打开
- **THEN** 除 ↑/↓/Enter/Esc 之外的键盘输入全部无效
- **AND** 不触发候选过滤、不改变列表、不插入任何字符到文档

### Requirement: 边界行为
系统 SHALL 定义并保持一致的边界行为：

#### Scenario: 切词到边界
- **WHEN** 用户在第一个单词执行 Prev 或在最后一个单词执行 Next
- **THEN** 采用循环策略（wrap-around）切换到另一端的单词
- **AND** Next/Prev 的跳过策略与现有悬浮框一致：优先跳过已学会单词，若全部已学会则退化为正常循环

#### Scenario: 无可学习单词
- **WHEN** 当前词库为空或筛选后无可学习单词
- **THEN** Lookup 第 1 项显示“暂无可学习单词”
- **AND** Next/Prev/Learn 执行后无副作用且不插入文本

### Requirement: 与现有功能一致性
系统 SHALL 复用现有背单词配置、词库来源与学习状态持久化，使编辑器 Lookup 与悬浮框在“当前词库/隐藏词/已学会状态”的判断上保持一致。

### Requirement: 与原有弹框模式隔离
系统 SHALL 保留原有弹框背单词模式，新增编辑器 Lookup 功能不得改变原有弹框模式的行为与快捷键动作语义。

#### Scenario: 原有弹框模式保持可用
- **WHEN** 用户使用原有“显示/隐藏单词悬浮框”动作及其配套的 Next/Prev/切换已学会动作
- **THEN** 行为与改动前一致
- **AND** 编辑器 Lookup 的输入屏蔽仅在其自身 Lookup 活跃期间生效，不得影响其他时间的编辑器输入与正常补全

## MODIFIED Requirements
### Requirement: 背单词状态持久化一致性
现有“已学会状态”的持久化与加载逻辑保持不变；新增编辑器 Lookup 仅作为新的交互入口，读写同一份学习状态数据。

## REMOVED Requirements
无
