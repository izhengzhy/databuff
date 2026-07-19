# 面包屑配置

## 概述

面包屑配置定义了每个路由的面包屑路径，用于页面顶部导航显示。

## 数据结构

```typescript
interface BreadcrumbPath {
  // key: 当前路径
  // value: 面包屑路径数组（除自己以外的所有父级路径）
  [key: string]: string[]
}
```

## 配置示例

```typescript
const appMonitor: BreadcrumbPath = {
  // 错误详情的面包屑：应用性能 > 错误分析 > 错误详情
  '/appMonitor/errorDetail': ['/appMonitor', '/appMonitor/errors'],
}
```

## 模块面包屑配置

### 数据报表 (dataReport)

| 路径 | 面包屑 |
|------|--------|
| /report | /dataReport |
| /report/template | /dataReport → /report |
| /report/setting | /dataReport → /report → /report/template |

### 智能告警 (alarmCenter)

| 路径 | 面包屑 |
|------|--------|
| /alarmCenter/alarm | /alarmCenter |
| /alarmCenter/alarmDetail | /alarmCenter → /alarmCenter/alarm |
| /alarmCenter/notice | /alarmCenter |
| /alarmCenter/rootCause | /alarmCenter |
| /alarmCenter/rootCauseAnalysis | /alarmCenter → /alarmCenter/rootCause |
| /alarmCenter/problemDetail | /alarmCenter → /alarmCenter/rootCause |

### 应用性能 (appMonitor)

| 路径 | 面包屑 |
|------|--------|
| /appMonitor/relationMap | /appMonitor |
| /appMonitor/relationMap/manage | /appMonitor → /appMonitor/relationMap |
| /appMonitor/systemTopology | /appMonitor |
| /appMonitor/systemTopology/manage | /appMonitor → /appMonitor/systemTopology |
| /appMonitor/businessSystem | /appMonitor |
| /appMonitor/service | /appMonitor |
| /appMonitor/serviceDetail | /appMonitor → /appMonitor/service |
| /appMonitor/serviceInstance | /appMonitor → /appMonitor/service → /appMonitor/serviceDetail |
| /appMonitor/database | /appMonitor |
| /appMonitor/database/detail | /appMonitor → /appMonitor/database |
| /appMonitor/msgQueue | /appMonitor |
| /appMonitor/msgQueue/detail | /appMonitor → /appMonitor/msgQueue |
| /appMonitor/cache | /appMonitor |
| /appMonitor/cache/detail | /appMonitor → /appMonitor/cache |
| /appMonitor/external | /appMonitor |
| /appMonitor/external/detail | /appMonitor → /appMonitor/external |
| /appMonitor/serviceAnalysis | /appMonitor |
| /appMonitor/resourceDetail | /appMonitor → /appMonitor/serviceAnalysis |
| /appMonitor/errors | /appMonitor |
| /appMonitor/errorDetail | /appMonitor → /appMonitor/errors |
| /appMonitor/trace | /appMonitor |
| /appMonitor/traceDetail | /appMonitor → /appMonitor/trace |
| /appMonitor/serviceFlow | /appMonitor |
| /appMonitor/diagnostic | /appMonitor |
| /appMonitor/thread | /appMonitor → /appMonitor/diagnostic |
| /appMonitor/response | /appMonitor → /appMonitor/service |
| /appMonitor/businessCall | /appMonitor → /appMonitor/businessSystem |
| /appMonitor/serviceCall | /appMonitor → /appMonitor/service |
| /appMonitor/serviceCallDetail | /appMonitor → /appMonitor/service → /appMonitor/serviceCall |
| /appMonitor/hotMethods | /appMonitor → /appMonitor/serviceAnalysis |
| /appMonitor/threadPool | /appMonitor → /appMonitor/service |
| /appMonitor/objectPool | /appMonitor → /appMonitor/service |
| /appMonitor/httpConnPool | /appMonitor → /appMonitor/service |
| /appMonitor/dbConnPool | /appMonitor → /appMonitor/service |

### 基础设施 (infrastructure)

| 路径 | 面包屑 |
|------|--------|
| /infrastructure/host | /infrastructure |
| /infrastructure/hostDetail | /infrastructure → /infrastructure/host |
| /infrastructure/docker | /infrastructure |
| /infrastructure/dockerDetail | /infrastructure → /infrastructure/docker |
| /infrastructure/process | /infrastructure |
| /infrastructure/processDetail | /infrastructure → /infrastructure/process |
| /infrastructure/cluster | /infrastructure |
| /infrastructure/clusterDetail | /infrastructure → /infrastructure/cluster |
| /infrastructure/namespace | /infrastructure → /infrastructure/cluster |
| /infrastructure/namespaceDetail | /infrastructure → /infrastructure/cluster → /infrastructure/namespace |
| /infrastructure/workload | /infrastructure → /infrastructure/cluster |
| /infrastructure/workloadDetail | /infrastructure → /infrastructure/cluster → /infrastructure/workload |
| /infrastructure/pod | /infrastructure → /infrastructure/cluster |
| /infrastructure/podDetail | /infrastructure → /infrastructure/cluster → /infrastructure/pod |
| /infrastructure/node | /infrastructure → /infrastructure/cluster |
| /infrastructure/nodeDetail | /infrastructure → /infrastructure/cluster → /infrastructure/node |
| /infrastructure/service | /infrastructure → /infrastructure/cluster → /infrastructure/namespace |
| /infrastructure/serviceDetail | /infrastructure → /infrastructure/cluster → /infrastructure/namespace → /infrastructure/service |

### 指标体系 (metrics)

| 路径 | 面包屑 |
|------|--------|
| /metrics/list | /metrics |
| /metrics/analysis | /metrics |

### 网络性能 (npm)

| 路径 | 面包屑 |
|------|--------|
| /npm/analysis | /npm |
| /npm/topology | /npm |
| /npm/dns | /npm |

### 访问体验 (rum)

| 路径 | 面包屑 |
|------|--------|
| /rum/application | /rum |
| /rum/application/manage | /rum → /rum/application |
| /rum/application/detail | /rum → /rum/application |
| /rum/application/setting | /rum → /rum/application |
| /rum/page | /rum |
| /rum/page/detail | /rum → /rum/page |
| /rum/page/tracking | /rum → /rum/page |
| /rum/action | /rum |
| /rum/action/detail | /rum → /rum/action |
| /rum/action/tracking | /rum → /rum/action |
| /rum/jsError | /rum |
| /rum/jsError/detail | /rum → /rum/jsError |
| /rum/jsError/tracking | /rum → /rum/jsError |
| /rum/request | /rum |
| /rum/request/detail | /rum → /rum/request |
| /rum/request/tracking | /rum → /rum/request |
| /rum/session | /rum |
| /rum/session/detail | /rum → /rum/session |
| /rum/session/tracking | /rum → /rum/session |
| /rum/trace | /rum |
| /rum/help | /rum |
| /rum/help/url | /rum |
| /rum/resource | /rum |
| /rum/user | /rum |

### 帮助中心 (help)

| 路径 | 面包屑 |
|------|--------|
| /help/startGuide | /help |
| /help/timeSync | /help |
| /help/setupNTP | /help |

### 业务观测 (observe)

| 路径 | 面包屑 |
|------|--------|
| /observe/scene | /observe |
| /observe/scene/manage | /observe → /observe/scene |
| /observe/event | /observe |

### 部署配置 (config)

| 路径 | 面包屑 |
|------|--------|
| /config/install | /config |
| /config/accessSetting | /config → /config/install |
| /config/status | /config |
| /config/agentPackages | /config → /config/status |
| /config/runLog | /config → /config/status |
| /config/manage | /config |
| /config/entity | /config → /config/manage |
| /config/service | /config → /config/manage → /config/entity |
| /config/business | /config → /config/manage → /config/entity |
| /config/business/manage | /config → /config/manage → /config/entity → /config/business |
| /config/process | /config → /config/manage → /config/entity |
| /config/request | /config → /config/manage → /config/entity |
| /config/request/attrCollSetting | /config → /config/manage → /config/entity → /config/request |
| /config/relationest | /config → /config/manage |
| /config/alarm | /config → /config/manage |
| /config/rule | /config → /config/manage → /config/alarm |
| /configManage/alarm/ruleSetting | /config → /config/manage → /config/alarm → /config/rule |
| /configManage/alarm/responseSetting | /config → /config/manage → /config/alarm |
| /config/ai | /config → /config/manage |
| /config/envTag | /config → /config/manage |
| /config/envTagSetting | /config → /config/manage → /config/envTag |
| /sysManage/account | /config → /config/manage |
| /sysManage/role | /config → /config/manage |
| /sysManage/license | /config → /config/manage |
| /sysManage/notice | /config → /config/manage |
| /sysManage/setting | /config → /config/manage |
| /sysManage/basic | /config → /config/manage → /sysManage/setting |
| /sysManage/systemEvent | /config → /config/manage → /sysManage/setting |
| /sysManage/eventDetail | /config → /config/manage → /sysManage/setting → /sysManage/systemEvent |
| /sysManage/systemRule | /config → /config/manage → /sysManage/setting → /sysManage/systemEvent |
| /sysManage/ruleSetting | /config → /config/manage → /sysManage/setting → /sysManage/systemEvent → /sysManage/systemRule |
| /sysManage/operationAudit | /config → /config/manage |
| /sysManage/group | /config → /config/manage |
| /sysManage/group/entity | /config → /config/manage → /sysManage/group |

## 文件位置

`src/router/breadcrumb-data.ts`

## 使用方式

面包屑组件读取 `breadcrumbPathData`，根据当前路由路径获取面包屑路径数组，然后通过 `route-data.ts` 中的 `name` 字段显示对应的菜单名称。
