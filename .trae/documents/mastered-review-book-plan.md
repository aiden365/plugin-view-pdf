# “已学会”复习词汇书实施计划

## Summary
- 目标1：右键菜单“标记为已学会/未学会”后，不再自动切换到下一个单词。
- 目标2：新增内置复习词汇书“已学会”，在单词被标记为已学会时自动进入该词汇书。
- 目标3：当手动切换到“已学会”词汇书时，仅展示当前已学会单词；取消已学会后自动从该词汇书移除。
- 目标4：复习词条内容沿用原词条详情（释义/例句/同近义等），而非降级为简版数据。

## Current State Analysis
- 右键行为现状：
  - 文件：`src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java`
  - `maybeShowContextMenu()` 中 `toggleMastered` 回调在 `next == true` 时会计算并跳转到下一个待学词，与你需求冲突。
- 学习状态现状：
  - 文件：`src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java`
  - 已有 `wordLearningStates` 持久化，`toggleWordMastered()`/`setWordMastered()` 可稳定表示“已学会/未学会”状态。
- 词汇书体系现状：
  - 文件：`src/main/java/com/aiden/plugin/viewpdf/settings/WordLibraryLoader.java`
  - 当前词汇书加载按 `selectedVocabularyBookKey` 单选路由（`builtin:*` 或 `custom:*`），尚无“系统虚拟词汇书”类型。
- 设置页现状：
  - 文件：`src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerConfigurable.java`
  - “词汇书列表”下拉目前仅包含内置词书与用户自定义词书，尚无“已学会”固定项。

## Proposed Changes
### 1) 新增“已学会”词汇书常量与选择键
- 文件：
  - `src/main/java/com/aiden/plugin/viewpdf/settings/WordLibraryLoader.java`
  - `src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java`（如需统一常量访问）
- What：
  - 增加系统词汇书 key：`system:mastered`（显示名“已学会”）。
- Why：
  - 需要一个稳定可选、不可删除的复习入口，区别于内置与自定义词书。
- How：
  - 在加载路由里识别 `system:mastered` 分支，不走文件加载。

### 2) 构建“已学会”词汇书内容（沿用原词条详情）
- 文件：`src/main/java/com/aiden/plugin/viewpdf/settings/WordLibraryLoader.java`
- What：
  - 新增构建函数：基于“当前全部可用词条池 + wordLearningStates”筛出已学会词。
  - 复习书条目直接复用原 `WordEntryData`（含 meaning/sentence/synonyms/phonetic/sourceRef）。
- Why：
  - 满足“自动加入复习书”与“沿用原详情”。
- How：
  - 先合并“可用词池”（内置 + 自定义）建立 `word -> WordEntryData` 映射。
  - 再按 `settings.isWordMastered(word)` 过滤，生成“已学会”列表。
  - 未学会（含取消已学会）自动不出现在该列表中。

### 3) 设置页下拉增加固定项“已学会”
- 文件：`src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerConfigurable.java`
- What：
  - 在 `refreshVocabularyBookOptions()` 中插入固定选项：
    - key：`system:mastered`
    - label：`系统 - 已学会`
- Why：
  - 让用户可手动切换到复习词书。
- How：
  - 固定项与内置/自定义一起展示；不参与“添加词汇书”逻辑。

### 4) 右键标记后不自动切词（仅右键菜单）
- 文件：`src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java`
- What：
  - 修改 `maybeShowContextMenu()` 中 `toggleMastered` 回调：
    - 去掉/禁用“已学会后跳转下一个词”的逻辑。
    - 保留 `refreshWordPool(true)` + `refreshContent()`，让当前词原位刷新状态。
- Why：
  - 对齐你确认的范围：仅右键菜单不自动切词。
- How：
  - 不改 `markCurrentWordMasteredAndAdvance()`，保持其他入口行为不变。

### 5) 词源变化时刷新“已学会”复习书
- 文件：
  - `src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java`
  - `src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java`（仅消费刷新）
- What：
  - 标记状态变更后重新计算 `wordEntries`（特别是当前选中 `system:mastered` 时）。
- Why：
  - 确保“标记即加入、取消即移除”实时生效。
- How：
  - 在 `setWordMastered()` 内触发 `WordLibraryLoader.reloadWordEntriesFromSettings(this)`。
  - 通过现有消息通知触发弹框刷新，避免手动重开悬浮框。

## Assumptions & Decisions
- 已确认决策：
  - “取消已学会”时从“已学会”词汇书移除。
  - “不自动切换下一个词”仅作用于右键菜单入口。
  - “已学会”词汇书条目沿用原词条详情。
- 约束：
  - 不增加新的快捷键与菜单动作。
  - 不改变“下一个单词”动作与非右键标记动作的既有行为。

## Verification Steps
- 右键交互验证：
  - 右键“标记为已学会”后，当前单词停留不跳转。
  - 右键“标记为未学会”后，当前单词停留不跳转。
- 复习词书验证：
  - 词汇书下拉存在“系统 - 已学会”项，可手动切换。
  - 标记为已学会后，切换到“已学会”词书可看到该词。
  - 取消已学会后，该词在“已学会”词书中消失。
  - 复习词条显示释义/例句/同近义等详情不缺失。
- 回归验证：
  - `showNextWord()` 行为不变。
  - 非右键标记入口（若有）行为保持现状。
- 工程验证：
  - 变更文件 `GetDiagnostics` 无新增错误。
  - `E:\java\jdk-21.0.1` 下 `.\gradlew.bat test --no-daemon` 通过。
