## Summary

将“系统 - 已学会（system:mastered）”改造成完全独立词库：仅从 `mastered.jsonl` 读取词条，标记/取消已学会时同步写入/删除该 JSONL，从而避免切换到“已学会单词本”时扫描所有词库导致的卡顿。

约束与口径（来自你的确认）：
- 共享范围：同一 IDE 进程的多窗口共享即可
- 不做历史迁移：旧的 `wordLearningStates(mastered=true)` 不自动导入 `mastered.jsonl`
- “系统 - 已学会”只看 `mastered.jsonl`（不与 `wordLearningStates` 做并集）

---

## Current State Analysis

### 读取链路（已完成）
- 选择词库 key 为 `system:mastered` 时，`WordLibraryLoader` 直接走 `MasteredWordLibrary.loadAll()`，不再扫描内置/自定义词库：
  - [WordLibraryLoader.loadMergedWordEntries](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/settings/WordLibraryLoader.java#L92-L114)
  - [WordLibraryLoader.loadMasteredEntries](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/settings/WordLibraryLoader.java#L195-L197)
- `MasteredWordLibrary` 负责 `PathManager.getConfigPath()/xcode-tools/mastered.jsonl` 的 JSONL 读写（单行 JSON）：
  - [MasteredWordLibrary](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/settings/MasteredWordLibrary.java#L21-L66)

### 写入链路（未完成，是目前卡点）
- 目前“标记已学会”仍然只调用字符串版本：
  - [WordPopupController.markCurrentWordMasteredAndAdvance](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java#L470-L485)
  - [WordPopupController context menu toggle](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java#L677-L695)
- `PdfViewerSettings.setWordMastered(String, boolean)` 只更新 `wordLearningStates`，并且会触发一次 `reloadWordEntriesFromSettings` + `wordSourceChanged(...)`：
  - [PdfViewerSettings.setWordMastered/toggleWordMastered](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java#L1076-L1104)
- 这意味着：即使 “system:mastered” 已经改成独立读取，写入仍未落到 `mastered.jsonl`，因此“系统 - 已学会”无法成为真正独立词库的闭环。

---

## Proposed Changes

### 1) PdfViewerSettings：新增基于 WordEntryData 的 mastered API，并写入独立词库
目标：保证写入 `mastered.jsonl` 时拿到完整 `WordEntryData`，而不是只有 word 字符串。

修改文件：
- [PdfViewerSettings.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java)

改动点：
- 新增方法（对外 API）：
  - `public void setWordMastered(@NotNull WordEntryData entry, boolean mastered)`
  - `public boolean toggleWordMastered(@NotNull WordEntryData entry)`
- 行为细节（decision complete）：
  - wordKey：使用现有 `normalizeWordKey(entry.word)`（与 `wordLearningStates` 一致）
  - 更新学习状态：复用现有逻辑（mastered/lastReviewedAtEpochMillis/reviewCount）
  - 同步独立词库：
    - `mastered == true` → `MasteredWordLibrary.upsert(entry)`
    - `mastered == false` → `MasteredWordLibrary.remove(entry.word)`
  - 不做历史迁移：不在启动/加载时把 `wordLearningStates` 批量写入 `mastered.jsonl`
  - 降低不必要的重载成本：仅当当前选择词库为 `system:mastered` 时，才 `WordLibraryLoader.reloadWordEntriesFromSettings(this)`，让“已学会列表”实时刷新；否则不 reload（因为普通词库内容不因 mastered 变化而变化）
- 保留旧字符串 API（兼容性）：
  - `setWordMastered(@Nullable String word, boolean mastered)` / `toggleWordMastered(@Nullable String word)` 仍按现有逻辑更新 `wordLearningStates`
  - 明确不在字符串 API 中写入 `mastered.jsonl`（避免写入不完整 entry）

### 2) 新增一个“已学会词库变更”事件，避免复用 wordSourceChanged 的语义
目标：当 `mastered.jsonl` 改变且当前视图正在展示 `system:mastered` 时，UI 能刷新；同时不再用 `wordSourceChanged` 表达“词库内容变了”。

修改文件：
- [PdfViewerSettingsListener.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettingsListener.java)

改动点：
- 新增默认方法（不破坏现有订阅者）：
  - `default void masteredWordLibraryChanged() {}`

并在 `PdfViewerSettings.setWordMastered(WordEntryData, ...)` 内：
- 若当前 `getSelectedVocabularyBookKey()` 为 `WordLibraryLoader.getSystemMasteredBookKey()`：
  - 先 `reloadWordEntriesFromSettings(this)`
  - 再发布 `masteredWordLibraryChanged()`

### 3) WordPopupController：标记已学会时传递完整 WordEntryData
目标：打通“标记已学会 → 写入 mastered.jsonl → system:mastered 立即可见”的闭环。

修改文件：
- [WordPopupController.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java)

改动点：
- `markCurrentWordMasteredAndAdvance()`：
  - 由 `settings.toggleWordMastered(current.word)` 改为 `settings.toggleWordMastered(current)`
- 右键菜单 toggle：
  - 由 `settings.toggleWordMastered(current.word)` 改为 `settings.toggleWordMastered(current)`

### 4) WordManagerPanel：在 system:mastered 视图下刷新列表
目标：当用户在“系统 - 已学会”词库里取消已学会时，右侧管理列表能立即更新（不需要切换词库再切回来）。

修改文件：
- [WordManagerPanel.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/ui/WordManagerPanel.java)

改动点：
- 订阅 `masteredWordLibraryChanged()`：
  - 若当前 selectedKey 为 `system:mastered`，则 `pageIndex = 0`（可选，保持简单一致）并 `refreshTable()`

---

## Assumptions & Decisions
- 共享范围仅覆盖“同一 IDE 进程多窗口”，因此 `MasteredWordLibrary` 的进程内锁（`LOCK`）足够；不额外处理跨进程并发写入。
- “系统 - 已学会”是独立展示口径：只展示 `mastered.jsonl`，旧 `wordLearningStates` 不补齐。
- `mastered.jsonl` 保存 `WordEntryData` 的字段以“标记时刻”为准，不尝试跟随原词库更新（避免额外扫描/同步复杂度）。

---

## Verification

### Build
- 使用项目既有方式构建插件：`gradlew buildPlugin`（xcode-tools 项目使用 `E:\\java` 的 Java 环境）

### Manual Checks
- 标记写入：
  - 在任意内置词库学习界面标记一个单词“已学会”
  - 切换到 “系统 - 已学会”，应快速加载且能看到刚标记的单词
- 取消删除：
  - 在 “系统 - 已学会” 中对该单词执行“标记为未学会”
  - 列表与弹框均应不再包含该单词（同一窗口内）
- 多窗口共享（同一 IDE 进程）：
  - 打开另一个 Project/Window，切到 “系统 - 已学会”，应能看到同一份 `mastered.jsonl` 的结果

