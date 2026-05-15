## Summary

将单词弹框右侧改成“菜单区”（2 行 2 列四宫格），并在鼠标移入弹框时显示、移出时隐藏：
- 左上：左箭头，点击切换到上一个单词
- 右上：右箭头，点击切换到下一个单词
- 左下：“中”，点击切换显示/隐藏中文释义
- 右下：“是/否”，默认“否”，点击切换已学会状态（同步到“系统-已学会”独立词库），并更新显示为“是/否”
- 菜单区整体在弹框右侧垂直居中（位于上下内容的中间位置），并尽量小，不占据太大面积

---

## Current State Analysis

### 现有结构
- 弹框主体在 [WordPopupController.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java) 中：
  - `rootPanel` 使用 `OverlayLayout`
  - `contentPanel` 为 `BoxLayout(Y_AXIS)`，展示 word/phonetic/meaning/sentence/synonyms
  - 当前仍保留一套“箭头覆盖层”（`navOverlayPanel` + `leftArrowLabel/rightArrowLabel`），用于 hover 显示箭头

### 释义显示逻辑
- `meaningLabel` 当前是否显示由 `PdfViewerSettings.isWordPopupShowMeaning()` 决定：
  - [refreshContent](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java#L544-L578)

### 已学会状态
- 已支持独立已学会词库：`system:mastered` 只读取 `mastered.jsonl`
- 写入路径：`PdfViewerSettings.toggleWordMastered(WordEntryData)` 会同步写入/删除 `mastered.jsonl`（已实现）

---

## Proposed Changes

### 1) 用“右侧菜单区”替换原先的箭头覆盖层
修改文件：
- [WordPopupController.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java)

改动要点（decision complete）：
- 新增右侧菜单容器：
  - `menuPanel`：固定小宽度（建议 44px），高度跟随弹框；背景透明
  - `menuGrid`：`GridLayout(2, 2, 2, 2)`；四个 cell 用 `JLabel`（居中 + hand cursor）
  - 垂直居中：`menuPanel` 使用 `GridBagLayout`（或等价方式）把 `menuGrid` 放到正中，`menuGrid` 不拉伸（靠 preferredSize 保持紧凑）
- 布局方式：
  - 将 `rootPanel` 从 `OverlayLayout` 调整为 `BorderLayout`
  - `contentPanel` 放 `BorderLayout.CENTER`
  - `menuPanel` 放 `BorderLayout.EAST`
  - 菜单隐藏时不移除 `menuPanel`（避免内容区宽度跳动），只把四个 cell 设为不可见
- 移除原来的箭头覆盖层能力（避免事件链路复杂/不稳定）：
  - 删除 `navOverlayPanel`、`leftArrowLabel/rightArrowLabel` 相关的 hover/布局逻辑（`layoutNavigationArrows/setNavigationVisible` 等）

### 2) 四宫格交互逻辑
修改文件：
- [WordPopupController.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java)

四个 cell 的行为：
- 左上（Prev）：
  - `mousePressed` → `showPreviousWord()`
- 右上（Next）：
  - `mousePressed` → `showNextWord()`
- 左下（“中”）：
  - 引入字段：`@Nullable Boolean meaningVisibleOverride`
  - 点击时：
    - 若 `meaningVisibleOverride == null`，以 `settings.isWordPopupShowMeaning()` 作为初始状态并取反
    - 否则直接取反
  - 点击后调用 `refreshContent()`，在 `refreshContent()` 中：
    - `showMeaning = meaningVisibleOverride != null ? meaningVisibleOverride : settings.isWordPopupShowMeaning()`
- 右下（已学会 “是/否”）：
  - 点击时对当前 entry 执行 `settings.toggleWordMastered(currentEntry)`（允许是/否互切）
  - 点击后刷新 UI：
    - `refreshWordPool(true)`（保证在 system:mastered 中取消已学会会立刻从列表消失/切到合适的下一个词）
    - `refreshContent()`

显示刷新规则：
- 每次 `refreshContent()` 都根据 `settings.isWordMastered(current.word)` 设置已学会 cell 文案：
  - mastered → “是”
  - not mastered → “否”

### 3) hover 显示/隐藏菜单
修改文件：
- [WordPopupController.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java)

规则：
- 鼠标移入弹框任意区域 → `setMenuVisible(true)`（四个 cell visible = true）
- 鼠标移出弹框 → `setMenuVisible(false)`
- 监听覆盖范围：`rootPanel`、`contentPanel`、`menuPanel` 以及四个 cell（确保不会因为移到菜单上就触发隐藏）

---

## Assumptions & Decisions
- 菜单区占用弹框右侧固定小宽度（默认 44px），并垂直居中；菜单隐藏时保持占位不变化（避免布局抖动）。
- “中”按钮的显示/隐藏只影响当前弹框会话（不写入设置页）；初始值跟随设置页 `isWordPopupShowMeaning()`。
- “已学会”按钮为互切逻辑（是↔否），并同步到独立已学会词库（`mastered.jsonl`）。

---

## Verification

### Build
- 使用 `E:\\java\\jdk-21.0.1` 作为 JAVA_HOME 执行：`./gradlew buildPlugin`

### Manual Checks
- hover：鼠标移入弹框显示四宫格，移出隐藏；移到菜单区不会触发隐藏
- Prev/Next：点击左/右箭头能稳定切换单词（与 Tools 菜单一致）
- 翻译：点击“中”能切换释义显示/隐藏，切换单词后状态保持（本次弹框会话内）
- 已学会：点击“否→是”后，切到“系统-已学会”能看到；点击“是→否”后会从“系统-已学会”消失
