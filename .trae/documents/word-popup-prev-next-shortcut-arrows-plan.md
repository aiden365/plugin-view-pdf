## Summary

为“单词弹框”新增“上一个单词 / 下一个单词”的快捷切换能力：

- 提供两个可在 IDEA Keymap 中自行绑定的动作：上一个单词、下一个单词（默认均不绑定快捷键）
- 鼠标移入单词弹框时，在左右两侧覆盖显示左右箭头图标；点击左/右箭头分别切换上一个/下一个单词；鼠标移出后隐藏箭头

## Current State Analysis

- 单词弹框控制器为 [WordPopupController.java](file:///e:/workspace/java/xcode-tools/src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java)
  - 已存在切换逻辑：`showNextWord()`（跳过已学会，必要时循环）
- 已存在动作（Actions）与注册：
  - 下一个单词：`com.aiden.plugin.viewpdf.actions.NextWordAction`（当前在 [plugin.xml](file:///e:/workspace/java/xcode-tools/src/main/resources/META-INF/plugin.xml) 中预置了快捷键）
  - 显示/隐藏单词弹框：`ToggleWordPopupAction`
  - 切换已学会状态：`MarkWordMasteredAction`
- 当前缺少：
  - “上一个单词”动作与控制器能力
  - 弹框内的左右箭头 UI 以及 hover 显示/隐藏逻辑

## Proposed Changes

### 1) 新增“上一个单词”动作（不预设快捷键）

- 新增文件：`src/main/java/com/aiden/plugin/viewpdf/actions/PrevWordAction.java`
  - 行为与 `NextWordAction` 一致：获取 `project/editor`，确保弹框显示，然后切换到上一个单词
  - 调用链：`WordPopupController.getOrCreate(project).show(editor)` + `controller.showPreviousWord()`
- 修改 [plugin.xml](file:///e:/workspace/java/xcode-tools/src/main/resources/META-INF/plugin.xml)
  - 新增 `<action id="com.aiden.plugin.viewpdf.actions.PrevWord" class="com.aiden.plugin.viewpdf.actions.PrevWordAction" text="上一个单词" ...>`
  - 默认不绑定快捷键：不添加 `<keyboard-shortcut .../>`

### 2) 将“下一个单词”默认快捷键移除（保持可在 Keymap 手动绑定）

- 修改 [plugin.xml](file:///e:/workspace/java/xcode-tools/src/main/resources/META-INF/plugin.xml)
  - 移除 `NextWord` action 内的 `<keyboard-shortcut .../>`
  - 保留 action 本身，确保在 Keymap 中仍可搜索到并绑定

### 3) 在控制器中补齐“上一个单词”切换逻辑（跳过已学会）

- 修改文件：`src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java`
- 新增 public 方法：`showPreviousWord()`
  - 逻辑与 `showNextWord()` 对称
  - 规则：跳过已学会；若全部已学会，则在全量列表中循环切换
- 新增私有方法：`findPreviousIndexSkippingMastered(int fromIndex)`
  - 从 `fromIndex` 向前遍历（环形），查找第一个未学会的候选

### 4) 弹框 hover 显示左右箭头（覆盖显示，不挤压内容）

- 修改文件：`src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java`
- UI 结构调整（覆盖显示方案）
  - 新增一个 `rootPanel`（作为 popup 的内容组件），使用 `OverlayLayout`
  - 将现有纵向内容面板保留为 `contentPanel`（放在 overlay 底层）
  - 新增 `navOverlayPanel`（放在 overlay 顶层，`opaque=false`）
    - `navOverlayPanel` 内部使用 `BorderLayout`
    - 左侧放 `leftArrowLabel`，右侧放 `rightArrowLabel`
    - 采用透明容器 + 居中布局（例如 `GridBagLayout`）将箭头垂直居中
- 箭头图标与交互
  - 图标来源：使用 IntelliJ 平台内置图标（例如 `com.intellij.icons.AllIcons` 的 Back/Forward）
  - 初始隐藏：`setVisible(false)`
  - 鼠标移入任意弹框组件时显示，移出弹框区域时隐藏
  - 点击行为：
    - 左箭头：`showPreviousWord()`
    - 右箭头：`showNextWord()`
  - 可用性处理：
    - 当词库为空或无当前单词时，保持箭头隐藏或禁用点击（避免误操作）

### 5) Hover 显示/隐藏的事件策略（避免在子组件间移动时抖动）

- 修改文件：`src/main/java/com/aiden/plugin/viewpdf/popup/WordPopupController.java`
- 新增 hover 监听安装方法（类似现有 ctrl-drag 监听安装方式）
  - 对 `rootPanel`、`contentPanel`、各个文本 label、`navOverlayPanel`、左右箭头 label 全部添加同一个 `MouseAdapter`
  - `mouseEntered`：显示箭头
  - `mouseExited`：通过“当前鼠标屏幕坐标转换到 rootPanel 坐标后 rootPanel.contains(point)”判断是否真的离开弹框；只有确实离开才隐藏

## Assumptions & Decisions

- “上一个单词”与“下一个单词”均默认不绑定快捷键（用户后续在 Keymap 中手动设置）
- “上一个单词”的切换规则与“下一个单词”一致：跳过已学会；若全部已学会则循环
- 箭头采用覆盖显示，避免出现箭头显示时内容被挤压导致文字跳动

## Verification

### Build

- 使用项目要求的 Java 环境运行：
  - `gradlew buildPlugin`

### Manual Checks (runIde)

- `gradlew runIde` 启动沙盒 IDE：
  - 打开任意编辑器，显示单词弹框
  - 鼠标移入弹框：左右箭头出现；移出弹框：箭头隐藏
  - 点击左/右箭头：分别切换上一个/下一个单词（跳过已学会的单词）
  - 打开 Settings/Keymap：
    - 能搜索到“上一个单词”“下一个单词”动作
    - 默认无快捷键绑定；手动绑定后可通过快捷键触发切换

