# 股票实时行情查看与阈值通知 Spec

## Why
在 IDE 内快速查看自选股票实时行情，并在涨跌幅达到阈值时及时收到通知，减少频繁切换外部行情软件的成本。

## What Changes
- 新增设置页：配置自选股票列表（英文逗号分隔）、可见列多选、刷新间隔、通知冷却期
- 在插件底部 ToolWindow 中间栏新增 “Swatch” 栏目用于展示股票监控列表，默认隐藏，通过 ToolWindow 顶部功能按钮控制显示/隐藏
- “Swatch” 栏目以表格形式展示自选股行情，列根据设置动态显示
- 新增逐股票通知配置：在表格操作栏中为单只股票设置涨跌幅阈值
- 新增通知逻辑：当 |涨跌幅%| >= 阈值 且未处于冷却期时触发 IDE Notifications
- 新增刷新与错误状态：定时轮询新浪行情接口，展示最后成功刷新时间与最近错误摘要

## Impact
- Affected specs: 设置持久化、网络请求/解析、UI 表格渲染、IDE Notifications
- Affected code: Settings/Preferences 页面、底部 ToolWindow 中间栏拼装与伪装显示/隐藏机制、网络请求层、解析与数据模型、通知组件

## ADDED Requirements

### Requirement: 自选股票配置
系统 SHALL 在设置面板提供自选股票代码输入框，允许用户以英文逗号分隔配置多只股票，例如 `sz000908,sh000001`。

#### Scenario: 输入清洗与校验
- **WHEN** 用户保存股票代码列表
- **THEN** 系统对输入进行清洗：去空格、去空项、去重、保序
- **AND** 对不符合格式的 code 给出明确提示
- **AND** 即使存在无效 code，系统仍可正常运行并展示有效项

#### Scenario: 空列表
- **WHEN** 用户保存空的股票列表
- **THEN** 行情列表显示空态提示
- **AND** 不触发网络请求或仅以空集合请求并安全处理

### Requirement: 可见列配置
系统 SHALL 在设置面板提供列选择器（下拉多选），用于决定行情表格中展示哪些列。

#### Scenario: 默认列
- **WHEN** 用户首次安装或未做任何配置
- **THEN** 默认展示列为：股票代码、股票名称、操作栏

#### Scenario: 列切换生效
- **WHEN** 用户在设置面板修改可见列并应用
- **THEN** 行情表格按新列集合重新渲染
- **AND** 重启 IDE 后配置仍然生效

### Requirement: Swatch 栏目显示/隐藏
系统 SHALL 在插件底部 ToolWindow 中间栏提供 “Swatch” 栏目，且默认隐藏，通过 ToolWindow 顶部功能按钮控制显示/隐藏。

#### Scenario: 默认隐藏
- **WHEN** 用户首次打开插件 ToolWindow
- **THEN** “Swatch” 栏目默认不显示

#### Scenario: 显示/隐藏切换
- **WHEN** 用户点击 ToolWindow 顶部功能按钮切换 “Swatch”
- **THEN** “Swatch” 栏目在显示与隐藏之间切换

### Requirement: 行情列表视图
系统 SHALL 在 “Swatch” 栏目中以表格形式展示自选股行情，表格列由设置面板配置动态决定。

#### Scenario: 基本渲染
- **WHEN** 行情数据成功拉取并解析
- **THEN** 表格以每只股票一行展示
- **AND** 至少包含列：代码、名称、操作栏

#### Scenario: 建议支持的可选列集合
系统 SHALL 提供以下可选列（列名可本地化），并在 spec 中固定列 key：
- 昨收（prevClose）
- 今开（open）
- 现价（price）
- 最高（high）
- 最低（low）
- 买一价（bid1Price）
- 买一量（bid1Volume）
- 卖一价（ask1Price）
- 卖一量（ask1Volume）
- 成交量（volume）
- 成交额（amount）
- 涨跌额（change）
- 涨跌幅%（changePct）
- 行情日期（quoteDate）
- 行情时间（quoteTime）
- 行情时间戳（quoteDateTime）
- 最近刷新时间（lastRefreshTime）

### Requirement: 行情数据源与解析
系统 SHALL 通过新浪行情接口 `https://hq.sinajs.cn/list={codes}` 获取行情，并解析响应中 `var hq_str_{code}="..."` 的内容。

#### Scenario: 多股票批量请求
- **WHEN** 用户配置了多只股票
- **THEN** 系统将 code 用英文逗号拼接一次性请求
- **AND** 解析多行响应，按 code 归集到对应行数据

#### Scenario: 编码兼容
- **WHEN** 响应内容包含中文股票名称
- **THEN** 系统按响应头或兜底策略正确解码（GBK/GB2312 场景优先保证不乱码）

#### Scenario: 字段缺失或格式变化
- **WHEN** 单行字段数量不足或格式不符合预期
- **THEN** 系统忽略该行或将该股票标记为“数据不可用”
- **AND** 不影响其它股票正常展示

### Requirement: 定时刷新与状态
系统 SHALL 支持定时轮询刷新行情数据，并向用户展示刷新状态。

#### Scenario: 默认刷新策略
- **WHEN** 用户未配置刷新间隔
- **THEN** 默认刷新间隔为 5 秒

#### Scenario: 刷新状态展示
- **WHEN** 成功刷新
- **THEN** 记录并展示最近成功刷新时间（lastRefreshTime）
- **WHEN** 刷新失败
- **THEN** 展示最近错误摘要（例如网络错误/解析错误）

#### Scenario: 失败退避与限频
- **WHEN** 连续请求失败
- **THEN** 系统采用退避策略降低请求频率（例如指数退避，设定最大退避上限）
- **AND** 一旦成功则恢复到用户配置的刷新间隔

### Requirement: 单股涨跌幅阈值配置
系统 SHALL 允许用户为每只股票单独配置“涨跌幅阈值%”，通过该股票行的操作栏进入配置。

#### Scenario: 设置阈值
- **WHEN** 用户在某股票的“通知”配置中输入阈值（百分比数值）
- **THEN** 阈值保存并立即生效
- **AND** 重启 IDE 后仍生效

#### Scenario: 未设置阈值
- **WHEN** 某股票未设置阈值
- **THEN** 该股票永不触发通知

### Requirement: 通知触发规则与冷却期
系统 SHALL 在满足触发条件时通过 IDE Notifications 发送通知，并在触发后进入冷却期避免刷屏。

#### Scenario: 触发条件
- **WHEN** 某股票已配置阈值
- **AND** 该股票的 |涨跌幅%| >= 阈值
- **AND** 该股票不在冷却期内
- **THEN** 发送通知

#### Scenario: 冷却期规则
- **WHEN** 某股票发送过通知
- **THEN** 在冷却期内不重复通知
- **AND** 冷却键为股票 code（不区分涨跌方向）

#### Scenario: 冷却期默认值与配置
- **WHEN** 用户未配置冷却期
- **THEN** 默认冷却期为 5 分钟
- **WHEN** 用户在设置面板修改冷却期
- **THEN** 新冷却期对后续触发生效

#### Scenario: 通知内容
- **WHEN** 发送通知
- **THEN** 通知内容至少包含：股票代码、股票名称、当前价、涨跌幅%、阈值、行情时间戳与最近刷新时间

## MODIFIED Requirements
无。

## REMOVED Requirements
无。

## Data Model
Quote 统一数据结构（示例字段与来源下标，以 split 后下标为准）：
- code: string（请求/解析得到）
- name: string（0）
- open: decimal（1）
- prevClose: decimal（2）
- price: decimal（3）
- high: decimal（4）
- low: decimal（5）
- bid: decimal（6）
- ask: decimal（7）
- volume: long（8）
- amount: decimal（9）
- bid1Volume: long（10）
- bid1Price: decimal（11）
- ask1Volume: long（20）
- ask1Price: decimal（21）
- quoteDate: string（30）
- quoteTime: string（31）
- quoteDateTime: string（quoteDate + " " + quoteTime）
- lastRefreshTime: string（客户端记录）
- change: decimal（price - prevClose）
- changePct: decimal（(price - prevClose) / prevClose * 100）

## Persistence
- stocks: string（原始输入或归一化后字符串）
- visibleColumns: string[]（列 key 集合）
- refreshIntervalSeconds: int
- cooldownMinutes: int
- perStockThresholdPct: map<string, decimal>（key=code）
- cooldownState: 不持久化（仅内存），理由：避免跨重启误抑制；重启后允许再次触发更符合预期

## Edge Cases
- 无效 code：不影响其它 code；在 UI 中标记并提示
- 解析字段不足：该股票显示“数据不可用”
- prevClose 为 0：不计算 changePct 或显示为 “—”
- 编码乱码：启用 GBK/GB2312 兜底解码策略

## Acceptance Criteria
- 配置股票列表保存后重启仍生效，输入清洗正确（去空、去重、保序）
- 列选择器可多选，默认列正确；修改后表格列立即变化且重启仍生效
- “Swatch” 栏目默认隐藏；通过 ToolWindow 顶部功能按钮可显示/隐藏
- 行情刷新默认 5 秒；可配置；失败时展示错误摘要且不崩溃
- 单股阈值：未设置不通知；设置后达到 |涨跌幅%| 阈值触发通知
- 冷却期默认 5 分钟：同一股票冷却期内不重复通知；冷却期可配置
- 通知内容包含 code、name、price、changePct、threshold、quoteDateTime、lastRefreshTime
