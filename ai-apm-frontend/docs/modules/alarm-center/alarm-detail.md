# 告警详情

## 页面信息

| 属性 | 值 |
|------|-----|
| 文件 | `src/views/alarmCenter/alarmDetail/index.vue` |
| 路由 path | `/alarmCenter/alarmDetail` |
| 入口参数 | `aid`，可选 `type` 指定默认 Tab |
| 主数据 API | `AlarmApi.getAlarmDetail` |

## 页面布局

### 顶部信息

- 级别标识与告警描述
- 影响面分析按钮: 仅当 `detail.problemId` 存在时展示
- 处理按钮: 仅在状态为 `0/2` 时可执行

### 信息区

默认展示:

- 告警 ID
- 最高等级
- 处理状态
- 首次触发时间
- 最新触发时间
- 结束时间
- 持续时间

展开“其他信息”后展示:

- 告警类型
- 所属管理域
- 标签列表

## 标签处理

- 页面会过滤掉 `apiKey`、`level`、`ruleName`、`message`、`group`、`classification`、`serviceCode`、`source`
- 剩余标签会通过 `Common/tagLabelMap` 转成展示文案
- 最终以 `key: value` 的扁平字符串数组交给 `collapse-tags` 展示

## Tabs

| Tab | 值 | 说明 |
|-----|----|------|
| 事件列表 | `tabEvent` | 展示当前告警下的事件列表与右侧事件详情 |
| 根因分析 | `tabCauseTree` | 展示根因树与 AI 建议 |
| 告警响应 | `tabResponse` | 展示响应记录 |
| 处理日志 | `tabDeal` | 展示处理日志 |

默认激活 `tabEvent`，若 URL 中传入 `type` 且命中上述值，会切到指定 Tab。

## 数据加载流程

### 1. 主详情

页面始终通过 `aid` 调用:

```ts
AlarmApi.getAlarmDetail({ id: aid })
```

### 2. 根因树

只有切到 `tabCauseTree` 且详情里存在 `issueId` 或 `problemId` 时，才会继续请求:

- `issueId` 存在: `RootCauseApi.getRootCauseAnalysisDetail({ id: issueId })`
- 否则使用: `RootCauseApi.getProblemDetail({ id: problemId })`

### 3. AI 重试

根因树支持调用:

```ts
AlarmApi.retrySuggest({ problemId: detail.problemId })
```

## 跳转行为

- 影响面分析: 新窗口打开 `/alarmCenter/problemDetail?id=...`
- 事件 Tab 内部复用 `eventDetail/index.vue`，不是独立的 alarm-center 一级页面

## 依赖组件

- `text-expand`
- `collapse-tags`
- `db-tabnav`
- `deal-dialog`
- `problemDetail/cause-tree`
- `tab-event`
- `tab-response`
- `tab-deal`
