# Alarm API

> 文件: `src/api/alarm.ts`

## 概述

`alarm.ts` 同时承载了告警中心页面接口与一部分告警配置类接口，包括:

- 告警列表 / 详情
- 事件详情
- 通知记录
- 响应策略
- 系统事件
- AI 根因分析

告警模块的页面入口、跳转关系和模块概览可参考 [智能告警模块总览](../modules/alarm-center/README.md)。

## 页面高频接口

### 告警列表 / 详情

| 方法 | HTTP | 路径 | 说明 |
|------|------|------|------|
| `getAlarmParams` | `POST` | `/alarm/queryParams` | 告警筛选项 |
| `getAlarmTrend` | `POST` | `/alarm/trend` | 告警趋势 |
| `dealAlarm` | `POST` | `/alarm/deal` | 告警处理 |
| `getAlarmListNew` | `POST` | `/alarm/list` | 告警列表 |
| `getAlarmDetail` | `GET` | `/alarm/detail/{id}` | 告警详情 |
| `getAlarmCount` | `POST` | `/alarm/count` | 告警统计数量 |

### 事件相关

| 方法 | HTTP | 路径 | 说明 |
|------|------|------|------|
| `getAlarmListV2` | `POST` | `/monitor/findMonitorEventV2` | 告警关联事件列表 |
| `getEventTrendV2` | `GET` | `/alarm/detail/{alarmId}/trendMap/{interval}` | 事件趋势 |
| `getEventDetailV2` | `POST` | `/monitor/findEventDetailV2` | 事件详情 |
| `getSystemEventDetail` | `GET` | `/eventSystem/findEventById/{eventId}` | 系统事件详情 |

### 通知记录

| 方法 | HTTP | 路径 | 说明 |
|------|------|------|------|
| `getNoticeRecordList` | `POST` | `/notify/records` | 通知记录列表 |
| `resendNotice` | `POST` | `/notify/resend` | 失败通知重发 |

## 配置类接口

### 响应策略

| 方法 | HTTP | 路径 | 说明 |
|------|------|------|------|
| `getResponseList` | `POST` | `/respPolicy/list` | 列表 |
| `getResponseDetail` | `POST` | `/respPolicy/find` | 详情 |
| `saveResponse` | `POST` | `/respPolicy/save` | 创建/编辑 |
| `deleteResponse` | `POST` | `/respPolicy/delete` | 删除 |
| `toggleResponseEnable` | `POST` | `/respPolicy/publish` | 启停 |
| `exportResponse` | `POST` | `/respPolicy/export` | 导出 |

## 系统事件与 AI

### 系统事件

| 方法 | HTTP | 路径 | 说明 |
|------|------|------|------|
| `getSystemEventList` | `POST` | `/eventSystem/findEventsByMultipleConditions` | 系统事件列表 |
| `getEventDetailChartTrend` | `POST` | `/monitor/getEventChartMap` | 普通事件趋势图 |
| `getSystemEventDetailChartTrend` | `POST` | `/monitor/system/getEventChartMap` | 系统事件趋势图 |

### AI

| 方法 | HTTP | 路径 | 说明 |
|------|------|------|------|
| `startAiRootAnalyse` | `POST` | `/webapi/api/ai/rootAnalyse` | 发起 AI 根因分析 |
| `getAiResult` | `POST` | `/webapi/api/ai/fetchResult` | 拉取 AI 结果 |
| `retrySuggest` | `GET` | `/webapi/api/ai/retrySuggest` | 重试 AI 建议 |

## 特殊处理

### `getAlarmListNew`

会在响应阶段截断超长 `description`:

- 原文保存在 `_description`
- 展示字段保留前 `1500` 个字符加省略号

### `getAiResult`

会对部分非标准响应做兜底处理，补齐:

- `status`
- `message`
