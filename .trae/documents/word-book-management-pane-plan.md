## Summary

在现有底部三栏 ToolWindow 的基础上，最右侧新增一个“单词管理”栏（默认隐藏），用于按单词本查看/搜索/分页单词列表，并对“是否在弹词弹框中显示该单词”进行管理；同时提供“查看 JSON”编辑弹框（只读，不保存）。

## Current State Analysis

- 底部 ToolWindow 由 [PdfViewerToolWindowFactory.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/ui/PdfViewerToolWindowFactory.java) 创建，核心分栏容器为 [StealthSplitPanel.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/ui/StealthSplitPanel.java)：
  - `outerSplitPane`：项目树 vs 右侧区域
  - `rightSplitPane`：中间区域（伪装/ PDF 卡片）vs 右侧代码区（third pane，可隐藏）
  - ToolWindow 右上角按钮（title actions）目前有：
    - [ToggleThirdPaneAction.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/ui/ToggleThirdPaneAction.java)（显示/隐藏右侧代码区）
    - [ToggleDisguiseAction.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/ui/ToggleDisguiseAction.java)（查看 PDF / 取消伪装）
- 单词数据与单词本选择：
  - 单词数据结构在 [PdfViewerSettings.WordEntryData](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java#L70-L81)
  - 当前选择的“单词本 key”在 [PdfViewerSettings.getSelectedVocabularyBookKey](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java#L823-L846)，切换时会触发 `WordLibraryLoader.reloadWordEntriesFromSettings`
  - “弹词弹框”使用 [WordPopupController](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java) 从 `settings.getWordEntries()` 取词并展示
- 目前缺少：
  - ToolWindow 内的“单词管理”栏与 show/hide 入口
  - “按单词本区分”的单词显示/隐藏状态持久化与过滤逻辑
  - 单词列表（分页/搜索/状态下拉/查看 JSON 弹框）

## Goals & Success Criteria

- ToolWindow 最右侧新增“单词管理”栏：
  - 默认隐藏
  - 通过 ToolWindow 右上角按钮显示/隐藏（类似现有第三栏按钮）
  - 不影响 PDF 查看区或代码区现有行为：隐藏时不改变原有三栏交互与布局逻辑
- 单词管理栏功能：
  - 顶部：单词本切换 + 搜索（仅匹配 word，不区分大小写包含匹配）
  - 列表：默认每页 50 条；提供上一页/下一页翻页
  - 列表列：单词 / 显示状态 / 操作
    - 显示状态：下拉框切换“是/否”
    - 若设置为“否”，该单词在弹词弹框中不再出现（并且该设置按“单词本”维度区分）
    - 操作：编辑按钮，弹出只读窗口显示该单词的 JSON 信息（不提供保存）

## Proposed Changes

### 1) 设置层：新增“单词管理栏可见性、宽度、按单词本的隐藏词表”持久化与事件

修改文件：
- [PdfViewerSettings.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java)
- [PdfViewerSettingsListener.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettingsListener.java)

新增 State 字段（保持向后兼容，均可为 null）：
- `Boolean wordManagerPaneVisible`
- `Integer wordManagerPaneWidthPercent`
- `Map<String, List<String>> hiddenWordsByVocabularyBookKey`
  - key：`selectedVocabularyBookKey`（例如 `builtin:CET4luan_2` / `custom:xxx` / `system:mastered`）
  - value：该单词本下被设置为“隐藏”的 wordKey 列表（wordKey 规则与现有一致：trim + lower-case）

新增 Settings API（示例命名，实际以代码一致性为准）：
- `boolean isWordManagerPaneVisible()` + `setWordManagerPaneVisible(boolean visible)`，并通过 MessageBus 发布 `wordManagerPaneVisibilityChanged(visible)`
- `int getWordManagerPaneWidthPercent()` + `setWordManagerPaneWidthPercent(int percent)`，发布 `wordManagerPaneWidthPercentChanged(percent)`
- `boolean isWordHiddenInPopup(@NotNull String bookKey, @Nullable String word)`
- `void setWordHiddenInPopup(@NotNull String bookKey, @Nullable String word, boolean hidden)`
  - 修改持久化 map 后，发布 `wordHiddenStateChanged(bookKey)`

Listener 扩展（全部用 default 方法，避免破坏现有实现）：
- `default void wordManagerPaneVisibilityChanged(boolean visible) {}`
- `default void wordManagerPaneWidthPercentChanged(int percent) {}`
- `default void wordHiddenStateChanged(@NotNull String bookKey) {}`

### 2) WordPopup：按“当前单词本”过滤隐藏词

修改文件：
- [WordPopupController.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java)

改动点：
- `refreshWordPool(...)` 里在构建 `activeWords` 时，读取 `settings.getSelectedVocabularyBookKey()`，对每个 entry 调用 `settings.isWordHiddenInPopup(bookKey, entry.word)`，若为 true 则跳过
- 订阅 `PdfViewerSettingsListener.wordHiddenStateChanged(bookKey)`：
  - 当 `bookKey` 等于当前 `settings.getSelectedVocabularyBookKey()` 时，执行 `refreshWordPool(true)` + `refreshContent()`，确保弹框立即响应隐藏状态变化

### 3) ToolWindow 布局：最右侧新增“单词管理”栏（默认隐藏）

修改/新增文件：
- 修改 [StealthSplitPanel.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/ui/StealthSplitPanel.java)
- 新增 `src/main/java/com/aiden/plugin/viewpdf/ui/WordManagerPanel.java`
- 新增 `src/main/java/com/aiden/plugin/viewpdf/ui/ToggleWordManagerPaneAction.java`
- 修改 [PdfViewerToolWindowFactory.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/ui/PdfViewerToolWindowFactory.java)

布局实现方式（尽量不触碰现有 PDF/代码区逻辑）：
- 保持现有 `rightSplitPane`（中间卡片 vs 右侧代码区）不变
- 新增一个最外层的右侧 split：`rightMostSplitPane = new JSplitPane(HORIZONTAL, rightSplitPane, wordManagerPanel.getComponent())`
- `outerSplitPane` 的右组件从 `rightSplitPane` 替换为 `rightMostSplitPane`
- 新增 show/hide：
  - `setWordManagerPaneVisible(boolean visible)`：
    - visible=false：隐藏 wordManagerPanel、dividerSize=0、disabled、dividerLocation=1.0
    - visible=true：显示 wordManagerPanel、dividerSize=2、enabled，并根据 `wordManagerPaneWidthPercent` 设定 divider 比例
- 新增 divider 持久化：
  - 监听 `rightMostSplitPane` 的 divider 变化，计算 `wordManagerWidthPercent` 并写入 settings（仅在面板可见且非程序性调整时）
- ToolWindow title actions 增加 `ToggleWordManagerPaneAction`：
  - ToggleAction 文案：`单词管理`
  - `isSelected` 读 `settings.isWordManagerPaneVisible()`
  - `setSelected` 写 `settings.setWordManagerPaneVisible(state)`
- ToolWindowFactory 初始化与监听：
  - 创建 splitPanel 后读取 settings 初始化：可见性、宽度 percent
  - 订阅 listener 的 `wordManagerPaneVisibilityChanged/wordManagerPaneWidthPercentChanged`，转发到 splitPanel

### 4) 单词管理面板：列表/分页/搜索/状态下拉/查看 JSON

新增文件：
- `src/main/java/com/aiden/plugin/viewpdf/ui/WordManagerPanel.java`

UI 结构（Swing，保持实现简单）：
- 顶部工具栏（BorderLayout.NORTH）：
  - 单词本下拉框 `JComboBox`：
    - 选项与设置页一致：系统-已学会、内置、用户自定义（参考 [PdfViewerConfigurable.refreshVocabularyBookOptions](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerConfigurable.java#L750-L779) 的生成逻辑）
    - 选择变化 → `settings.setSelectedVocabularyBookKey(key)`（触发 reload wordEntries）
  - 搜索框 `JTextField`：
    - 输入变化（DocumentListener）→ 重置页码为 0 并刷新表格
  - 上一页/下一页按钮 `JButton`：
    - pageSize 固定 50
    - 根据 filtered 总数禁用/启用
- 中部列表（BorderLayout.CENTER）：
  - `JTable` + `AbstractTableModel`（三列：word / 显示状态 / 操作）
  - 显示状态列：
    - 使用 `JComboBox` 作为 cell editor，选项：`是`/`否`
    - 变更时写入 `settings.setWordHiddenInPopup(currentBookKey, word, hidden)`
  - 操作列：
    - cell renderer/editor 显示 `编辑` 按钮
    - 点击后弹出只读弹框展示该单词 JSON（使用 `JBPopupFactory.createComponentPopupBuilder` + `JTextArea` + 滚动条）
    - JSON 内容：基于 `WordEntryData` 手动拼装（不引入新三方库）

数据刷新策略：
- 数据源为 `settings.getWordEntries()`（已是当前单词本）
- 搜索过滤：仅对 `entry.word` 做 `toLowerCase().contains(queryLower)`；query 为空则不过滤
- 订阅 message bus：
  - `selectedVocabularyBookChanged`：重置页码为 0、刷新下拉框选中与列表
  - `vocabularyBookListChanged`：重建下拉框 options
  - `wordHiddenStateChanged(bookKey)`：如果 bookKey == 当前选中的 key，则仅刷新表格状态列（或全量刷新）

## Assumptions & Decisions

- 单词管理栏位置：最右侧新增一栏（默认隐藏）
- “显示状态”按单词本区分：同一个单词在不同单词本中可分别设置显示/隐藏
- 搜索仅匹配单词字段 `word`，不匹配释义
- 为尽量不影响 PDF 查看区/代码区：
  - 新增最外层右侧 split，不改变现有 `rightSplitPane` 的 third pane 与 PDF 卡片逻辑
  - “单词管理”隐藏时，行为与当前三栏一致

## Verification

- 构建验证（使用项目要求的 Java 环境）：
  - `gradlew buildPlugin`
- 手动验证（`gradlew runIde`）：
  - ToolWindow 右上角：点击“单词管理”按钮可显示/隐藏；隐藏时 PDF/代码区表现与当前一致
  - 单词管理栏：
    - 默认每页 50 条，上一页/下一页翻页正常
    - 搜索输入可过滤（对 word 做包含匹配）
    - 单词本下拉切换后列表刷新
    - 在某单词本将某单词设置为“否（隐藏）”，弹词弹框不再出现该单词；切换到其他单词本不受影响
    - 点击“编辑”弹出只读 JSON 视图，无保存入口

