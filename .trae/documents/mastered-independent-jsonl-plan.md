## Summary

将“系统 - 已学会（system:mastered）”从“临时扫描所有词库 + 过滤 mastered 状态”的视图，改为一个独立维护的 JSONL 词库文件，从而在切换到“已学会单词本”时避免明显延迟，并且在不同 IDEA 窗口间共享。

关键点：

- 存储为独立 JSONL 文件（每行一个 `WordEntryData` 的 JSON），不再在切换时扫描所有内置/自定义词库
- 不迁移历史数据：仅从本改动生效后开始，新标记为“已学会”的单词才会进入独立词库
- 保存完整 `WordEntryData` 字段（word/meaning/phonetic/sentences/synonyms/sourceRef 等）

## Current State Analysis

- “已学会单词本”的 key 固定为 `system:mastered`，由 [WordLibraryLoader.getSystemMasteredBookKey](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/settings/WordLibraryLoader.java#L79-L90) 提供。
- 当前切换到 `system:mastered` 的加载逻辑在 [WordLibraryLoader.loadMergedWordEntries](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/settings/WordLibraryLoader.java#L92-L114)：
  - 命中 `system:mastered` 时调用 `loadMasteredEntries(settings)`
- 现有 `loadMasteredEntries(settings)`（见 [WordLibraryLoader.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/settings/WordLibraryLoader.java#L195-L226)）会：
  - 扫描所有内置词库（逐行解析 JSON）
  - 扫描所有自定义词库（逐行解析 JSONL）
  - 合并去重后再用 `settings.isWordMastered(entry.word)` 过滤
  - 这就是切换到“已学会单词本”延迟大的根因
- “是否已学会”的状态目前持久化在 [PdfViewerSettings.StateData.wordLearningStates](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java#L100-L157)，由 `setWordMastered/toggleWordMastered/isWordMastered` 管理（见 [PdfViewerSettings.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java#L1063-L1104)）。

## Goals & Success Criteria

- 切换到“系统 - 已学会”时不再扫描所有词库，列表加载显著加速。
- 已学会词库在同一台机器的不同 IDEA 窗口间共享（应用级路径，非 project 级）。
- 标记/取消“已学会”时：
  - `wordLearningStates` 仍旧作为“是否已学会”的权威状态（用于跳过、统计等）
  - 同步增量维护独立 JSONL 词库（新增/删除词条）
- 不迁移历史：升级后已存在的 mastered=true 不会自动写入 JSONL，避免一次性扫描造成新的性能问题。

## Proposed Changes

### 1) 新增“已学会独立词库”读写模块（JSONL）

新增文件：
- `src/main/java/com/aiden/plugin/viewpdf/settings/MasteredWordLibrary.java`

职责：
- 统一管理 JSONL 文件路径、读写、去重与增删
- JSONL 文件位置使用 IntelliJ 的 config 目录，保证多窗口共享：
  - `Path.of(PathManager.getConfigPath(), "xcode-tools", "mastered.jsonl")`
  - 写入前 `Files.createDirectories(parent)`
- 提供方法（示例接口，落地时按代码风格微调）：
  - `@NotNull List<PdfViewerSettings.WordEntryData> loadAll()`
  - `void upsert(@NotNull PdfViewerSettings.WordEntryData entry)`（按 wordKey 去重更新）
  - `void remove(@Nullable String word)`（按 wordKey 删除）
- 序列化/反序列化：
  - 不引入新三方库
  - 使用手写 JSON（与当前项目解析方式一致：尽量简单、可控）
  - 反序列化只解析本项目需要的字段；遇到不完整/坏行则跳过该行
- 并发与一致性：
  - 以类级 `synchronized`（或私有锁）保证同进程内并发安全
  - 写入采取“读全量 → 更新 map → 覆盖写回”策略，避免文件追加导致重复膨胀

### 2) WordLibraryLoader：system:mastered 改为读取独立 JSONL

修改文件：
- [WordLibraryLoader.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/settings/WordLibraryLoader.java)

改动点：
- 将 `loadMasteredEntries(settings)` 的实现替换为：
  - 直接调用 `MasteredWordLibrary.loadAll()` 返回列表
  - 不再扫描内置/自定义词库
- 保留 `SYSTEM_MASTERED_KEY = "system:mastered"` 不变，确保 UI/设置页/单词管理栏无需改 key

### 3) PdfViewerSettings：新增基于 entry 的 mastered 更新入口（维护 JSONL）

修改文件：
- [PdfViewerSettings.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/settings/PdfViewerSettings.java)

改动点：
- 新增方法（示例）：
  - `public boolean toggleWordMastered(@NotNull WordEntryData entry)`
  - `public void setWordMastered(@NotNull WordEntryData entry, boolean mastered)`
- 行为：
  - 仍旧更新 `wordLearningStates`（mastered/lastReviewedAt/reviewCount）
  - mastered=true 时：调用 `MasteredWordLibrary.upsert(entry)` 写入完整 `WordEntryData`
  - mastered=false 时：调用 `MasteredWordLibrary.remove(entry.word)` 删除
  - 保持现有 `setWordMastered(String)/toggleWordMastered(String)`：
    - 内部尽量从 `getWordEntries()` 中找到匹配 `wordKey` 的 `WordEntryData`，若找到则走新接口
    - 找不到时：
      - mastered=false：仍可按 word 删除 JSONL
      - mastered=true：不写入 JSONL（因为用户要求必须保存完整字段且不迁移历史）
- 继续触发 `WordLibraryLoader.reloadWordEntriesFromSettings(this)`：
  - 当当前选择就是 `system:mastered` 时，reload 只读 JSONL，仍然很快

### 4) WordPopupController：标记已学会改为传递完整 entry

修改文件：
- [WordPopupController.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java)

改动点：
- 在 `markCurrentWordMasteredAndAdvance()` 中，当前已有 `PdfViewerSettings.WordEntryData current = getCurrentEntry()`：
  - 改为调用 `PdfViewerSettings.getInstance().toggleWordMastered(current)`
  - 使 mastered JSONL 能保存完整 entry 数据

### 5)（可选）单词管理栏：在“系统 - 已学会”里显示的是 JSONL 词库

无需额外代码变更：
- 单词管理栏与弹词弹框都是从 `settings.getWordEntries()` 读取
- 当 `selectedVocabularyBookKey == system:mastered` 时，由 `WordLibraryLoader.loadMergedWordEntries` 写入的 entries 将来自 JSONL

## Assumptions & Decisions

- 独立词库采用 JSONL 文件并存放在 IntelliJ config 目录下，确保多窗口共享
- 不迁移历史 mastered 状态：升级后旧的已学会不会自动出现在“系统 - 已学会”里；只有新标记的会进入
- 取消已学会会从 JSONL 中移除该词条
- 不引入任何新三方 JSON 库，采用手写序列化/反序列化

## Verification

- 构建：
  - 使用项目要求的 Java 环境运行 `gradlew buildPlugin`
- 手动验证（runIde）：
  - 打开普通内置词库，标记若干单词为“已学会”
  - 切换到“系统 - 已学会”：
    - 列表加载应明显更快（不再扫描所有词库）
    - 能看到刚标记的单词
  - 取消已学会后再次切换：
    - JSONL 中对应词条消失
  - 关闭并再次打开一个新的 IDE 窗口（同一台机器）：
    - “系统 - 已学会”仍能读到相同 JSONL 内容（验证多窗口共享）

