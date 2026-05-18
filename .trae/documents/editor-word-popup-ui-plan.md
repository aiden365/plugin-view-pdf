# 编辑器自绘弹框背单词（替换 Lookup）计划

## Summary
将当前“编辑器 Lookup 背单词”（基于 `LookupManager.showLookup`）替换为“自绘弹框列表 UI”（基于 `JBPopupFactory.createComponentPopupBuilder`），在编辑器光标附近展示与截图类似的弹框列表，从而获得更强的自定义能力。同时在设置面板新增“背景透明度% / 文本透明度%”两个配置（10–100）。

## Current State Analysis
- 当前编辑器背单词入口：`com.aiden.plugin.viewpdf.actions.ShowEditorWordLookupAction`
  - 注册位置：[plugin.xml](file:///e:/workspace/java/xcode-tools/src/main/resources/META-INF/plugin.xml#L92-L100)
  - 默认快捷键：`ctrl alt shift W`
- 当前实现方式：基于 IDEA Lookup
  - 逻辑入口：[EditorWordLookupController](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/editorlookup/EditorWordLookupController.java)
  - 固定 4 项：WORD / Next / Prev / Learn（`LookupElement`）
  - 通过 `IdeEventQueue` 屏蔽输入以避免过滤与插入
- 会话与词池逻辑：Project 级 service
  - [EditorWordLookupSession](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/editorlookup/EditorWordLookupSession.java)
  - 复用 `PdfViewerSettings.getWordEntries()`、`isWordHiddenInPopup(...)`、`isWordMastered(...)`、`toggleWordMastered(...)`，保证与原弹框模式一致
- 原有弹框背单词模式（WordPopupController 等）需要保持不变且不受影响

## Goals & Success Criteria
- 弹框 UI：在编辑器光标附近显示自绘弹框列表，外观与截图红框区域相近（深色背景、选中高亮、纵向列表）。
- 列表内容：固定 4 项且顺序固定：当前单词、Next、Prev、Learn。
- 键盘交互：
  - ↑/↓ 切换选中
  - Enter 执行选中项（不插入任何文本到编辑器）
  - Esc 关闭弹框
  - 其他字符输入无效（不触发过滤、不插入、不改变列表）
- 行为语义：
  - 单词项：无动作（用户已确认）
  - Next/Prev：循环切词，优先跳过已学会；若全部已学会退化为循环
  - Learn：切换“已学会”状态，停留当前单词、弹框保持打开
- 设置项：
  - 在插件设置面板新增“背景透明度（%）/ 文本透明度（%）”两个配置，范围 10–100
  - 变更后对新弹框立即生效（至少对下一次打开生效；若弹框正打开，尽量实时刷新）
- 隔离性：
  - 不修改/不破坏原有弹框背单词模式与其快捷键/行为
  - 不影响 IDE 正常补全：仅在该弹框打开期间拦截输入

## Assumptions & Decisions
- 已确认：替换现有 Lookup 展示（保留 Action ID 与快捷键，内部实现从 Lookup 改为自绘弹框）。
- 已确认：单词项回车/点击无动作。
- 新增透明度设置适用于“编辑器自绘弹框列表”，不复用现有 `wordPopupOpacityPercent`（避免与旧弹框耦合）。
- 自绘弹框优先使用 Swing 组件（`JBList/JPanel`）+ `JBPopup`，而不是 Inlay/Inline Completion。

## Proposed Changes (Files & What/Why/How)
### 1) 替换展示层：从 Lookup 改为 JBPopup 自绘列表
- 修改：[EditorWordLookupController](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/editorlookup/EditorWordLookupController.java)
  - Why：用户希望不使用 IDEA 自带 Lookup，以便做更多自定义 UI
  - How：
    - 新增/切换为 `JBPopupFactory.createComponentPopupBuilder(panel, focusComponent).createPopup()`
    - panel 内部使用 `JBList` 或 `JList` 展示 4 行固定项
    - 自定义 `ListCellRenderer`：实现深色背景、选中高亮、字体/间距接近截图
    - 弹框定位：使用 `editor.visualPositionToXY(caretVisualPosition)` 获取坐标，必要时做边界裁剪后 `popup.show(new RelativePoint(editor.getContentComponent(), point))`
    - 键盘控制：在弹框激活期间安装 `IdeEventQueue` dispatcher（或将焦点置于 list 并额外吞掉 KEY_TYPED），仅放行 ↑/↓/Enter/Esc
    - Enter 行为：根据当前选中项 Kind 调用 session（Next/Prev/Learn），并刷新 list model 的“单词行”文本（Next/Prev/Learn 均不向文档插入）
    - Esc 行为：关闭弹框并清理 dispatcher
    - 单词行 Enter：无动作（仅保持弹框）
    - Next/Prev：不必关闭重建弹框，优先“原地刷新”以减少闪烁（仍满足需求）
  - 风险控制：
    - 弹框关闭后必须移除 dispatcher，避免影响输入
    - 弹框 requestFocus 仅用于内部 list 导航，不改变编辑器文档

### 2) 保持会话逻辑：继续复用 EditorWordLookupSession
- 保持：[EditorWordLookupSession](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/editorlookup/EditorWordLookupSession.java)
  - Why：已实现与旧弹框一致的过滤/跳过逻辑；保持隔离性与复用成本最低
  - How：展示层刷新时仅调用 `getCurrentWordDisplayText()/moveNext()/movePrevious()/toggleLearn()`

### 3) 新增设置项：编辑器弹框背景/文本透明度
- 修改：[PdfViewerSettings](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java)
  - 增加 `StateData` 字段：
    - `editorWordPopupBackgroundOpacityPercent`
    - `editorWordPopupTextOpacityPercent`
  - 增加 getter/setter：
    - clamp 范围 10–100（复用现有 `clampPopupOpacityPercent`）
    - setter 通过 message bus 发布新的 listener 回调（见下一条）
- 修改：[PdfViewerSettingsListener](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettingsListener.java)
  - 新增两个 `default` 回调方法：
    - `editorWordPopupBackgroundOpacityChanged(int percent)`
    - `editorWordPopupTextOpacityChanged(int percent)`
- 修改：[PdfViewerConfigurable](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerConfigurable.java)
  - UI：
    - 在“背单词相关设置”附近新增两行 spinner：
      - “编辑器背单词弹框背景透明度（%）”：SpinnerNumberModel(100, 10, 100, 1)
      - “编辑器背单词弹框文字透明度（%）”：SpinnerNumberModel(100, 10, 100, 1)
  - 逻辑：
    - `isModified()`：比较 settings 值与 spinner 值
    - `apply()`：调用新增 setter
    - `reset()`：回填 spinner
    - `disposeUIResources()`：置空字段

### 4) 新弹框应用透明度设置
- 修改：[EditorWordLookupController](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/editorlookup/EditorWordLookupController.java)
  - 背景透明度：list/panel 背景使用带 alpha 的 `Color(r,g,b,alpha)`，alpha 来自 percent
  - 文本透明度：renderer 的前景色使用带 alpha 的 `Color(r,g,b,alpha)`，alpha 来自 percent
  - 生效时机：
    - 弹框创建时读取 settings
    - 弹框打开期间监听 settings listener（可选）：收到变更后刷新 list（`repaint()`）

### 5) 确保旧功能不受影响
- 不改动：
  - [WordPopupController](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java) 及其 Actions
  - 原 `ToggleWordPopup/NextWord/PrevWord/MarkWordMastered` 行为与快捷键
- 风险点：
  - 事件拦截器必须严格限定在“自绘弹框活跃期”

## Verification
### 手动验证（IDE 内）
- 打开任意代码文件，将光标置于编辑器中，触发 `ctrl alt shift W`：
  - 弹框出现，位置接近光标
  - 列表固定 4 行且顺序固定
- 键盘：
  - ↑/↓ 可移动选中高亮
  - Enter：
    - 选中 Next/Prev 会切词并更新“单词行”文本
    - 选中 Learn 会切换已学会但不切词，弹框不关闭
    - 选中 单词 行无动作
  - Esc 关闭弹框
  - 输入任意字符不会插入到编辑器、不会改变弹框内容
- 边界：
  - 在首/尾单词 Next/Prev 循环
  - 全部已学会时 Next/Prev 退化为循环
  - 无词库/无可学习词时单词行显示“暂无可学习单词”，其余操作无副作用
- 设置：
  - 在设置面板调整“背景透明度/文本透明度”到不同值（10–100），重新打开弹框观察生效

### 构建验证（命令行）
- 使用 `E:\\java` 下的 JDK（项目当前 target 为 21）：
  - `gradlew test --no-daemon --no-configuration-cache`
  - `gradlew build --no-daemon --no-configuration-cache`

