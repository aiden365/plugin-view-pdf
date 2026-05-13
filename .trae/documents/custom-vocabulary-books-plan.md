# 自定义词汇书（JSON Line）实施计划

## Summary
- 目标：在插件设置页提供“添加词汇书”能力，用户可输入 `书名 + JSON Line 文件地址`，并将新增词汇书追加到现有“词汇书列表”下拉框中切换使用。
- 目标：将现有“内置词汇书”下拉升级为“词汇书列表”，列表包含内置词汇书 + 用户新增词汇书。
- 目标：删除设置面板中的“词源”与“分类相关配置”显示项（词源开关、自定义路径、分类筛选输入框）。
- 目标：新增词汇书时做“立即校验”；若文件中存在坏行则仅预览报错并拒绝导入；书名禁止重名。

## Current State Analysis
- 设置页 `PdfViewerConfigurable` 现状：
  - 已有：`内置词汇书` 下拉、`词源：启用内置词库`、`自定义词库文件（txt/csv）`、分类筛选（难度/主题/来源）。
  - 问题：不支持“多本自定义词汇书管理”，且 UI 中“词源/分类”与你当前目标冲突。
- 设置存储 `PdfViewerSettings` 现状：
  - 已有：`wordBuiltinVocabularyBook`（单个内置书选择）、`wordSourceCustomPath`（单路径）、`wordFilter*`（分类筛选）。
  - 问题：缺少“自定义词汇书列表（书名+路径）”结构与当前选中词汇书唯一标识。
- 加载器 `WordLibraryLoader` 现状：
  - 已有：按 `wordBuiltinVocabularyBook` 读内置 `vocabularies/*.json`，以及旧的单文件自定义路径读取（txt/csv）。
  - 问题：未支持“按书名切换加载任意自定义 JSONL 词汇书”；未实现“坏行仅预览不导入”的校验回路。
- 弹框 `WordPopupController` 现状：
  - 已基于 `settings.getWordEntries()` 渲染，不依赖具体来源类型；适配成本低。

## Proposed Changes
### 1) 扩展词汇书数据模型（多本自定义）
- 文件：`src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java`
- What：
  - 新增 `CustomVocabularyBookData`（`name`, `jsonlPath`, `createdAt` 可选）。
  - `StateData` 新增：
    - `List<CustomVocabularyBookData> customVocabularyBooks`
    - `String selectedVocabularyBookKey`（当前词汇书选择，统一覆盖内置+自定义）
  - 保留并兼容旧字段读取（迁移期将 `wordBuiltinVocabularyBook` 映射到新选择键）。
- Why：
  - 支撑“多本管理 + 下拉切换”的基础持久化。
- How：
  - 新增标准化 getter/setter：
    - `getCustomVocabularyBooks()`
    - `addCustomVocabularyBook(name, path)`（禁止重名）
    - `setSelectedVocabularyBookKey(key)`
  - setter 统一触发 message bus 事件并刷新词池。

### 2) 设置事件扩展（词汇书列表更新）
- 文件：`src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettingsListener.java`
- What：
  - 新增事件：
    - `vocabularyBookListChanged()`
    - `selectedVocabularyBookChanged(String key)`
- Why：
  - 设置页/弹框可即时响应列表与选择变化。
- How：
  - 采用 `default` 空实现，避免破坏现有订阅。

### 3) 设置页改造：添加词汇书 + 词汇书列表
- 文件：`src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerConfigurable.java`
- What：
  - 将“内置词汇书”标签改名为“词汇书列表”。
  - 新增“添加词汇书”区域：
    - 书名输入框
    - JSON Line 文件地址输入框（带文件选择器，仅 `.json`）
    - “校验并添加”按钮
  - 删除（仅 UI 层移除）以下配置项显示与交互：
    - `词源：启用内置词库`
    - `词源：自定义词库文件（txt/csv）`
    - `分类筛选-难度/主题/来源`
- Why：
  - 完全匹配你的新交互目标，简化设置面板。
- How：
  - `apply()` 写入新增词汇书列表与当前选择。
  - `isModified/reset/dispose` 同步更新。
  - 添加时先校验（见第 5 点），通过才加入待保存列表。

### 4) 统一词汇书键与加载路由
- 文件：`src/main/java/com/aiden/plugin/viewpdf/settings/WordLibraryLoader.java`
- What：
  - 定义统一 book key 规则：
    - 内置：`builtin:<bookId>`
    - 自定义：`custom:<bookName>`
  - `loadMergedWordEntries()` 改为按 `selectedVocabularyBookKey` 加载“单一当前词汇书”。
- Why：
  - 满足“和内置一样下拉切换”语义，避免多来源混杂。
- How：
  - 内置加载沿用现有 JSONL 解析逻辑。
  - 自定义加载读取用户配置的 JSONL 文件并解析单词结构：
    - 输入示例：`{"word":"bat","trans":[{"pos":"n.","tranCn":"..."},{"pos":"vi.","tranCn":"..."}]}`
  - 释义拼接格式：`n..1 xxx；vi..1 yyy`（按当前项目既有显示规范拼接）。

### 5) 校验策略实现：仅预览不导入
- 文件：`src/main/java/com/aiden/plugin/viewpdf/settings/WordLibraryLoader.java`
- What：
  - 新增“校验方法”供设置页调用：
    - `validateCustomJsonl(path)` 返回：总行数、可解析行数、错误行号样例、首条错误原因。
  - 规则：若存在任一坏行，判定校验失败，禁止添加（仅预览错误信息）。
- Why：
  - 对齐你的决策“仅预览不导入”。
- How：
  - 逐行解析并收集错误，不写入 `wordEntries`。
  - 设置页弹出错误摘要，提示修复后再添加。

### 6) 重名策略实现：禁止重名
- 文件：`src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java`, `PdfViewerConfigurable.java`
- What：
  - 添加前检查 `customVocabularyBooks.name`（忽略首尾空白，大小写敏感按产品习惯统一为大小写不敏感比较）。
  - 若重名，阻止保存并提示“书名已存在”。
- Why：
  - 对齐你的决策“禁止重名”。
- How：
  - 在 UI 提交层和 settings 写入层双重校验，防止旁路写入。

### 7) 清理旧“词源/分类”逻辑（非 UI 的兼容处理）
- 文件：
  - `src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerConfigurable.java`
  - `src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java`
  - `src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java`
- What：
  - 设置面板删除词源/分类配置项后，对应 getter/setter 可保留一版兼容（短期不在 UI 暴露）。
  - `WordPopupController` 中筛选逻辑改为“默认不过滤”或使用空筛选，避免受旧配置残留影响。
- Why：
  - 确保用户界面与实际行为一致且无历史配置副作用。
- How：
  - 以“向后兼容读、前台不再编辑”为迁移策略，后续再做彻底清理。

## Assumptions & Decisions
- 已确认决策：
  - 支持“多本自定义词汇书管理”。
  - 新增词汇书需“立即校验”。
  - 校验中发现坏行：仅预览错误，不导入。
  - 自定义书名禁止重名。
  - 新增词汇书追加到下拉列表，与内置词汇书统一切换。
  - 设置面板删除“词源”和“分类相关配置”。
- 计划内默认：
  - 词汇书为单选生效（当前仅加载一本文库）。
  - 文件编码按 UTF-8 读取。

## Verification Steps
- 设置页交互验证：
  - 可输入“书名 + JSONL 地址”并点击“校验并添加”。
  - 重名时拦截并提示。
  - 坏行存在时拦截并显示错误摘要（行号+原因）。
  - “词汇书列表”下拉同时包含内置与新增自定义书。
  - 设置页不再显示词源与分类配置项。
- 功能验证：
  - 选择不同词汇书后，弹框单词集随之切换。
  - 自定义 JSONL 示例可正确生成释义文本并显示。
  - 已学会状态、下一个单词、弹框显示开关不回退。
- 兼容与构建：
  - 读取历史配置不崩溃。
  - `GetDiagnostics` 无新增错误。
  - `E:\java\jdk-21.0.1` 下 `./gradlew.bat test --no-daemon` 通过。
