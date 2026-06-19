# Service API

> 文件: `src/api/service.ts`

## 概述

`service.ts` 是当前仓库里接口面最宽的 API 文件之一，覆盖了 APM 服务视角下的服务流、服务详情、调用分析、错误分析、资源分析、数据库/缓存/消息队列列表，以及 Dump / 线程分析等能力。

## 接口分组

### 服务流

| 方法 | HTTP | 路径 | 说明 |
|------|------|------|------|
| `getServiceFlowEndpoint` | `POST` | `/trace/serviceFlowEndpoint` | 获取服务流入口 |
| `getServiceFlow` | `POST` | `/trace/multipleServiceFlow` | 获取服务级别服务流 |

### 服务详情与基础信息

| 方法 | HTTP | 路径 | 说明 |
|------|------|------|------|
| `getServiceRequestMetric` | `POST` | `/service/serviceDetailTrendChart` | 服务详情趋势图 |
| `getServicesIds` | `POST` | `/service/basicServices` | 获取服务 ID/名称列表，受管理域限制 |
| `getAllServicesIds` | `POST` | `/service/basicAllServices` | 获取全量服务 ID/名称列表，不受管理域限制 |
| `getSrcServices` | `POST` | `/service/resourcesGroupBy` | 获取来源服务列表 |
| `getServiceInstance` | `GET` | `/service/getServiceInstance` | 获取服务下实例 |
| `getBasicServiceInstance` | `GET` | `/service/getBasicServiceInstance` | 获取基础服务实例信息 |
| `getBasicServiceInstanceV2` | `GET` | `/service/getBasicServiceInstance` | 对返回结构做二次格式化，统一为 `data.list` |
| `getServiceRelate` | `GET` | `/service/getServiceInstanceRelations` | 获取服务关联信息 |
| `addServicesLabel` | `POST` | `/service/customServiceTag` | 添加服务标签 |
| `updateServiceName` | `POST` | `/service/updateService` | 更新服务显示名称 |

### 调用分析

| 方法 | HTTP | 路径 | 说明 |
|------|------|------|------|
| `getServiceCallInfo` | `POST` | `/service/call_info` | 调用关系基本信息 |
| `getServiceCallGraphStats` | `POST` | `/service/call_graph_stats` | 调用关系统计图 |
| `getServiceCallDelayGraph` | `POST` | `/service/call_mq_delay_graph_stats` | MQ 延迟图 |
| `getServiceCallEndpoints` | `POST` | `/service/call_endpoints` | 获取客户端/服务端接口与 SQL 列表 |
| `getServiceCallPools` | `POST` | `/service/pool_get_names` | 连接池/对象池名称 |
| `getServiceCallSpans` | `POST` | `/trace/call_spans` | 调用分析 span 列表 |
| `getServiceCallLightApmErrSpans` | `POST` | `/trace/lightApmErrSpan` | 轻量错误 span 列表 |

### 错误分析

| 方法 | HTTP | 路径 | 说明 |
|------|------|------|------|
| `getErrorDistList` | `POST` | `/service/exceptionDistMap` | 错误分布 |
| `getErrorSpanList` | `POST` | `/trace/exceptionList` | 错误 span 列表 |

### Profiling

| 方法 | HTTP | 路径 | 说明 |
|------|------|------|------|
| `getProfilingFlame` | `POST` | `/v3/profiling/flame` | Profiling 火焰图 |
| `getProfilingParams` | `GET` | `/v3/profiling/tags` | Profiling 筛选参数 |

### 数据库 / MQ / 缓存 / 外部服务

| 方法 | HTTP | 路径 | 说明 |
|------|------|------|------|
| `getDatabaseList` | `POST` | `/service/dbList` | 数据库列表 |
| `getMqList` | `POST` | `/service/mqList` | 消息队列列表 |
| `getCacheList` | `POST` | `/service/cacheList` | 缓存列表 |
| `getRemoteList` | `POST` | `/service/remoteCallList` | 外部服务列表 |

### 服务健康与驾驶舱指标

| 方法 | HTTP | 路径 | 说明 |
|------|------|------|------|
| `getServicesHealth` | `POST` | `/cockpit/trafficLight` | 服务红绿灯 |
| `setHealthConfig` | `POST` | `/cockpit/setConfig` | 健康阈值配置 |
| `getHealthConfig` | `GET` | `/cockpit/getConfig` | 获取健康阈值配置 |
| `getServiceAlarmTrend` | `POST` | `/cockpit/countServiceAlarms` | 服务告警趋势 |
| `getServiceAlarmTotal` | `POST` | `/cockpit/countServiceAlarmsTotal` | 服务告警与异常总量 |

### 接口资源分析

| 方法 | HTTP | 路径 | 说明 |
|------|------|------|------|
| `getResourceDetail` | `POST` | `/service/resourceInfo` | 接口详情 |
| `getResourceSpanList` | `POST` | `/trace/spanList` | 接口 span 列表 |
| `getResourceSlowSpanList` | `POST` | `/trace/slowSpanList` | 慢 span 列表 |
| `getResourceErrorSpanList` | `POST` | `/trace/errorSpanList` | 错误 span 列表 |
| `getServiceRequestByCompTypes` | `POST` | `/service/resources` | 按组件类型获取接口资源 |
| `getRequestMetricStats` | `POST` | `/service/metric_stats` | 接口指标统计 |
| `getRequestResourceStats` | `POST` | `/service/resource_stats` | 接口耗时分解 |
| `getServiceListTrendChart` | `POST` | `/service/serviceListTrendChart` | 服务列表趋势图 |
| `getServiceDetailTrendChart` | `POST` | `/service/serviceDetailTrendChart` | 服务详情趋势图 |

### Dump 与线程分析

| 方法 | HTTP | 路径 | 说明 |
|------|------|------|------|
| `getDumpList` | `POST` | `/webapi/api/dump/search` | Dump 列表 |
| `createDump` | `POST` | `/webapi/api/dump/add` | 创建 Dump |
| `updateDump` | `POST` | `/webapi/api/dump/update` | 更新 Dump |
| `deleteDump` | `DELETE` | `/webapi/api/dump/delete/{id}` | 删除 Dump |
| `downloadDump` | `GET` | `/webapi/api/dump/download/{id}` | 下载 Dump，返回 `blob` |
| `reobtainDump` | `POST` | `/webapi/api/dump/reDump/{id}` | 重新获取 Dump |
| `getThreadAnalysisList` | `POST` | `/thread_dump_analyse/listTask` | 线程剖析任务列表 |
| `createThreadAnalysis` | `POST` | `/thread_dump_analyse/addTask` | 创建线程剖析任务 |
| `deleteThreadAnalysis` | `GET` | `/thread_dump_analyse/deleteTask?id=...` | 删除线程剖析任务 |
| `getThreadAnalysisDetail` | `POST` | `/thread_dump_analyse/detail` | 获取线程剖析详情 |
| `getThreadAnalysisStack` | `GET` | `/thread_dump_analyse/threadGroupStack` 或 `/thread_dump_analyse/threadStack` | 查看线程堆栈 |

## 特殊处理

### `getBasicServiceInstanceV2`

该方法会对原始响应做兼容转换:

- 若后端返回 `serviceInstances`
- 前端会自动映射为 `data.list`

适合直接喂给表格组件。

### `getThreadAnalysisList`

会自动把前端常用的 `pageNum` 转成后端需要的 `pageNumber`。

### `downloadDump`

返回 `blob`，通常用于文件下载，不走普通 JSON 数据流。

