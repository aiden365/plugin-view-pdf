# Tasks
- [ ] 任务 1：定位并确认复用点
  - [ ] 子任务 1.1：确认背单词数据源与持久化入口（PdfViewerSettings / WordLibraryLoader / MasteredWordLibrary）
  - [ ] 子任务 1.2：确认现有切词与跳过已学会策略（WordPopupController.showNextWord/showPreviousWord）
  - [ ] 子任务 1.3：确认现有动作注册方式与快捷键配置位置（plugin.xml）

- [ ] 任务 2：补充最小公共 API 以复用“当前单词”与“Learn 不前进”
  - [ ] 子任务 2.1：在 WordPopupController 中提供获取当前单词文本的公共方法（不触发展示悬浮框）
  - [ ] 子任务 2.2：在 WordPopupController 中提供 Learn 行为公共方法：切换已学会状态但不切词
  - [ ] 子任务 2.3：确保新增 API 不改变现有悬浮框显示/隐藏与快捷键动作行为

- [ ] 任务 3：新增“编辑器 Lookup 背单词”动作与 Lookup 渲染
  - [ ] 子任务 3.1：新增 Action 类：从当前 editor 获取 project/editor 并触发 Lookup 展示
  - [ ] 子任务 3.2：在 plugin.xml 注册 Action ID，并配置一个默认快捷键（可在 Keymap 中修改）
  - [ ] 子任务 3.3：构造固定 4 个 LookupElement：当前单词、Next、Prev、Learn，并确保 Enter 不会插入文本
  - [ ] 子任务 3.4：实现 Next/Prev：关闭 Lookup -> 切词 -> 重新弹出 Lookup 刷新内容
  - [ ] 子任务 3.5：实现 Learn：切换已学会状态但停留当前单词，Lookup 保持打开

- [ ] 任务 4：实现 Lookup 打开期间的键盘输入屏蔽
  - [ ] 子任务 4.1：仅放行 ↑/↓/Enter/Esc，其余键盘输入全部吞掉
  - [ ] 子任务 4.2：屏蔽范围严格限定为“当前 Lookup 活跃期间”，关闭后必须立即恢复
  - [ ] 子任务 4.3：验证不会影响 IDE 的正常补全与其他编辑器操作（仅本功能触发时生效）

- [ ] 任务 5：验证与回归检查
  - [ ] 子任务 5.1：手动验证：快捷键唤起、Esc 关闭、↑/↓/Enter 行为正确
  - [ ] 子任务 5.2：手动验证：Next/Prev 循环切词且优先跳过已学会；无词时提示且无副作用
  - [ ] 子任务 5.3：手动验证：Learn 切换已学会状态且停留当前单词；重启 IDE 后仍保留
  - [ ] 子任务 5.4：执行项目基础构建或测试（使用 E:\java 作为 Java 环境）并修复回归问题

# Task Dependencies
- 任务 2 依赖 任务 1
- 任务 3 依赖 任务 1 和 任务 2
- 任务 4 依赖 任务 3
- 任务 5 依赖 任务 3 和 任务 4

