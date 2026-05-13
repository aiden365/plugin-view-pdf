# 单词弹框内容开关与右键学会状态切换计划

## Summary
- 目标：在不破坏“隐蔽性”的前提下，将单词弹框改为“极简默认”，并通过复选开关控制显示 `释义`、`例句`、`同近义`。
- 目标：新增鼠标右键菜单，支持“已学会/未学会”状态来回切换（toggle），替代必须依赖快捷键的操作。
- 目标：补齐内置词库解析，将 `vocabularies/*.json` 的行级 JSON 中可用字段映射到弹框展示模型。

## Current State Analysis
- 当前弹框逻辑在 `src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java`：
  - 已有：显示/隐藏、下一个单词、标记已学会（仅设为 true）、按分类筛选、样式配置。
  - 缺失：显示项复选开关；例句/同近义渲染；“取消已学会”；右键菜单交互。
- 当前设置持久化在 `src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java`：
  - 已有：弹框尺寸/位置/字体、词源、分类筛选、`wordEntries`、`wordLearningStates`。
  - 缺失：弹框内容显示开关（showMeaning/showSentence/showSynonyms）、例句条数配置。
- 当前设置 UI 在 `src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerConfigurable.java`：
  - 已有：弹框样式、词源、分类筛选。
  - 缺失：显示项复选开关与“例句条数（仅英文）”配置项。
- 当前词库加载在 `src/main/java/com/aiden/plugin/viewpdf/settings/WordLibraryLoader.java`：
  - 已有：CSV/TXT 内置与自定义合并解析。
  - 缺失：对 `src/main/resources/vocabularies/*.json`（JSON Lines）的解析和字段提取。

## Proposed Changes
### 1) 扩展设置模型（显示项与例句条数）
- 文件：`src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java`
- What：
  - 在 `StateData` 新增字段：
    - `wordPopupShowMeaning`（默认 `false`，极简）
    - `wordPopupShowSentence`（默认 `false`，极简）
    - `wordPopupShowSynonyms`（默认 `false`，极简）
    - `wordPopupSentenceLimit`（默认 `1`，可配置范围 1-5）
  - 新增 getter/setter，并通过 `PdfViewerSettingsListener` 广播变更事件。
  - 将“已学会状态切换”能力补成显式 API：`toggleWordMastered(word)`（内部仍复用现有学习状态结构）。
- Why：
  - 让“隐蔽默认 + 可配置展示”具备可持久化、可监听刷新的基础。
- How：
  - 复用当前数值夹逼和 message bus 模式，保证与已有设置体系一致。

### 2) 扩展设置变更事件
- 文件：`src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettingsListener.java`
- What：
  - 新增回调：`wordPopupContentDisplayChanged(...)`，携带三项显示开关 + 例句条数。
- Why：
  - 弹框在设置变化后需要即时刷新，不依赖重启或重开弹框。
- How：
  - 与既有 listener 风格保持一致，默认空实现避免破坏现有订阅方。

### 3) 设置页新增复选项与数量配置
- 文件：`src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerConfigurable.java`
- What：
  - 新增控件：
    - `显示释义`（复选）
    - `显示例句（仅英文）`（复选）
    - `显示同近义`（复选）
    - `例句显示条数`（spinner，1-5）
  - 在 `isModified/apply/reset/dispose` 全链路接入。
- Why：
  - 满足“通过复选开关控制显示内容”的核心需求。
- How：
  - 按已有 UI 组织方式新增行，写入 `PdfViewerSettings` 对应字段。

### 4) 词库模型扩展（例句/同近义）
- 文件：`src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java`（`WordEntryData`）
- What：
  - 扩展 `WordEntryData`：
    - `List<String> sentenceEnList`
    - `Map<String, List<String>> synonymsByPos`
    - （可选保留）`rawPosSummary` 以便快速 fallback
- Why：
  - 弹框需要直接渲染结构化“例句/同近义”内容，避免 UI 层重复解析。
- How：
  - 维持最小可用结构，仅保存展示所需字段，不引入完整原始 JSON 深拷贝。

### 5) 新增 vocabularies JSONL 解析器并接入内置来源
- 文件：
  - `src/main/java/com/aiden/plugin/viewpdf/settings/WordLibraryLoader.java`
  - `src/main/resources/vocabularies/*.json`（只读数据源，不改内容）
- What：
  - 在 loader 中新增 `vocabularies` 目录的 JSON Lines 读取：
    - 逐行读取对象（每行一个单词）。
    - 提取 `headWord`、`content.word.content.trans`、`sentence.sentences[].sContent`、`syno.synos[]`。
  - 按用户要求格式化：
    - 释义格式：`词性.1 中文释义1，中文释义2；词性.2 ...`
    - 例句：仅英文，按 `wordPopupSentenceLimit` 截断。
    - 同近义：按词性分组，列出英文词。
  - 保留当前 `basic-words.csv` 作为兜底，优先加载 `vocabularies`（若可用）。
- Why：
  - 最大化利用现有词汇书数据，支撑 richer 弹框内容。
- How：
  - 使用容错解析：字段缺失时跳过对应子块，不影响单词主条目可用性。
  - 去重规则维持“按 word key 合并”。

### 6) 弹框渲染改造为“极简默认 + 按开关显示”
- 文件：`src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java`
- What：
  - 默认渲染：只显示 `word`（与已学会状态标签）。
  - 当开关开启时，按顺序渲染：
    - 释义块（可多行）
    - 例句块（英文，最多 N 条）
    - 同近义块（按词性分组）
  - 保持现有字体/颜色/尺寸配置能力。
- Why：
  - 满足“隐蔽 + 可展开信息”的体验目标。
- How：
  - 将当前固定 `JLabel` 结构改为可重建的内容容器（仍基于 Swing 轻量组件）。
  - 设置变化事件触发时仅刷新内容，不强制重建 popup。

### 7) 右键菜单实现“已学会/未学会”切换
- 文件：`src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java`
- What：
  - 在弹框面板绑定 `JPopupMenu` 右键菜单，仅一项：
    - 当前未学会：`标记为已学会`
    - 当前已学会：`标记为未学会`
  - 点击后调用 toggle API，刷新当前词标识与后续切词行为。
- Why：
  - 满足你明确要求的鼠标交互方式，且减少快捷键负担。
- How：
  - 使用当前词状态动态生成菜单文案；切换后不关闭弹框。

### 8) 快捷键行为兼容策略
- 文件：
  - `src/main/java/com/aiden/plugin/viewpdf/actions/MarkWordMasteredAction.java`
  - `src/main/resources/META-INF/plugin.xml`（如需更新描述文案）
- What：
  - 将现有“标记已学会”动作改为 toggle 语义，避免与右键行为冲突。
- Why：
  - 键盘与鼠标语义一致，降低认知负担。
- How：
  - Action 层调用 `toggleWordMastered`；描述文案改为“切换已学会状态”。

## Assumptions & Decisions
- 已确认决策：
  - 隐蔽性方案：`极简默认`（默认只展示单词）。
  - 显示项开关：`释义 + 例句 + 同近义` 三项都要。
  - 已学会交互：以 `右键菜单` 为主，单项 toggle（已学会/未学会）。
  - 例句规则：只显示英文例句，条数可配置。
  - 同近义规则：按词性分组展示英文词。
- 计划内约束：
  - 不引入新外部依赖；优先使用现有 IntelliJ/Swing 能力与项目 JSON 解析能力。
  - 数据缺失容忍：某单词没有例句或同近义时，隐藏对应区块，不报错中断。

## Verification Steps
- 配置验证：
  - 在设置页切换三项复选框与例句条数，`Apply` 后弹框即时刷新。
  - 重启 IDE 后，复选状态与条数保留。
- 展示验证：
  - 默认只显示单词（极简）。
  - 开启 `释义` 后，格式符合“词性.序号 + 中文释义”规范。
  - 开启 `例句` 后，仅显示英文，条数符合配置。
  - 开启 `同近义` 后，按词性分组列出英文词。
- 交互验证：
  - 右键菜单文案随状态变化（已学会/未学会）。
  - 右键切换后状态立即生效，并影响“下一个单词跳过已学会”逻辑。
  - 快捷键 `MarkWordMastered` 与右键切换语义一致（toggle）。
- 回归验证：
  - 现有显示/隐藏弹框、切换下一个词、样式设置、分类筛选行为不回退。
  - `./gradlew.bat test --no-daemon` 在 `E:\java\jdk-21.0.1` 环境下可通过。
