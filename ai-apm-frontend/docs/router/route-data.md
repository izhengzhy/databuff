# 路由数据配置

## 模块概览

| ID | 模块标识 | 名称 | 路径 | 菜单项数 |
|----|----------|------|------|----------|
| 1 | cockpit | 驾驶舱 | /cockpit | 1 |
| 2 | dataReport | 数据报表 | /dataReport | 5 |
| 3 | alarmCenter | 智能告警 | /alarmCenter | 7 |
| 4 | appMonitor | 应用性能 | /appMonitor | 30 |
| 5 | infrastructure | 基础设施 | /infrastructure | 18 |
| 6 | metrics | 指标体系 | /metrics | 3 |
| 7 | npm | 网络性能 | /npm | 4 |
| 22 | rum | 访问体验 | /rum | 30 |
| 9 | log | 日志分析 | /log | 1 |
| 10 | config | 部署配置 | /config | 42 |
| 11 | help | 帮助中心 | /help | 3 |
| 16 | observe | 业务观测 | /observe | 3 |
| 88 | personal | 个人中心 | /personal | 1 |
| 99 | hide | 高级配置 | /advancedConfig | 1 |

---

## 1. 驾驶舱 (cockpit)

| ID | 名称 | 路径 | 菜单 | 时间控件 |
|----|------|------|------|----------|
| 1 | 驾驶舱 | /cockpit | ✓ | ✓ (1天) |

---

## 2. 数据报表 (dataReport)

### 菜单页面

| ID | 名称 | 路径 | 菜单 | 时间控件 |
|----|------|------|------|----------|
| 201 | 仪表盘 | /dashboard | ✓ | - |
| 202 | 报告 | /report | ✓ | ✓ (60天) |

### 静态页面

| ID | 名称 | 路径 | menuId |
|----|------|------|--------|
| 211 | 报告模版 | /report/template | 202 |
| 212 | 新增模板 | /report/setting | 202 |

---

## 3. 智能告警 (alarmCenter)

### 菜单页面

| ID | 名称 | 路径 | 菜单 | 时间控件 |
|----|------|------|------|----------|
| 301 | 告警列表 | /alarmCenter/alarm | ✓ | ✓ |
| 302 | 通知记录 | /alarmCenter/notice | ✓ | ✓ |
| 303 | 问题列表 | /alarmCenter/rootCause | ✓ | ✓ |
| 304 | 问题分析 | /alarmCenter/problemAnalysis | ✓ | ✓ |

### 静态页面

| ID | 名称 | 路径 | menuId |
|----|------|------|--------|
| 311 | 告警详情 | /alarmCenter/alarmDetail | 301 |
| 313 | 根因分析 | /alarmCenter/rootCauseAnalysis | 303 |
| 314 | 问题详情 | /alarmCenter/problemDetail | 303 |

---

## 4. 应用性能 (appMonitor)

### 菜单页面

| ID | 名称 | 路径 | 菜单 | 时间控件 |
|----|------|------|------|----------|
| 401 | 空间地图 | /appMonitor/relationMap | ✓ | ✓ |
| 413 | 系统拓扑 | /appMonitor/systemTopology | ✓ | ✓ |
| 402 | 业务系统 | /appMonitor/businessSystem | ✓ | ✓ |
| 410 | 服务流 | /appMonitor/serviceFlow | ✓ | ✓ (1天) |
| 403 | 服务 | /appMonitor/service | ✓ | ✓ |
| 404 | 数据库 | /appMonitor/database | ✓ | ✓ |
| 405 | 消息队列 | /appMonitor/msgQueue | ✓ | ✓ |
| 406 | 缓存 | /appMonitor/cache | ✓ | ✓ |
| 412 | 外部服务 | /appMonitor/external | ✓ | ✓ |
| 407 | 接口分析 | /appMonitor/serviceAnalysis | ✓ | ✓ |
| 408 | 错误分析 | /appMonitor/errors | ✓ | ✓ |
| 409 | 链路追踪 | /appMonitor/trace | ✓ | ✓ |
| 411 | 诊断分析 | /appMonitor/diagnostic | ✓ | - |

### 静态页面

| ID | 名称 | 路径 | menuId | 时间控件 |
|----|------|------|--------|----------|
| 40101 | 拓扑自定义布局 | /appMonitor/relationMap/manage | 401 | - |
| 41301 | 系统拓扑自定义布局 | /appMonitor/systemTopology/manage | 413 | - |
| 40401 | 数据库详情 | /appMonitor/database/detail | 404 | ✓ |
| 40501 | 消息队列详情 | /appMonitor/msgQueue/detail | 405 | ✓ |
| 40601 | 缓存详情 | /appMonitor/cache/detail | 406 | ✓ |
| 41201 | 外部服务详情 | /appMonitor/external/detail | 412 | ✓ |
| 422 | 响应时间分布 | /appMonitor/response | 403 | ✓ |
| 423 | 接口调用分析 | /appMonitor/serviceCall | 403 | ✓ |
| 424 | 接口调用出入口详情 | /appMonitor/serviceCallDetail | 403 | ✓ |
| 425 | 接口详情 | /appMonitor/resourceDetail | 407 | ✓ |
| 426 | 调用链详情 | /appMonitor/traceDetail | 409 | - |
| 427 | 服务详情 | /appMonitor/serviceDetail | 403 | ✓ |
| 428 | 服务实例详情 | /appMonitor/serviceInstance | 403 | ✓ |
| 429 | 错误详情 | /appMonitor/errorDetail | 408 | ✓ |
| 430 | Profiling | /appMonitor/hotMethods | 407 | ✓ |
| 431 | 业务系统调用分析 | /appMonitor/businessCall | 402 | ✓ |
| 432 | 线程池监控 | /appMonitor/threadPool | 403 | ✓ |
| 433 | 对象池监控 | /appMonitor/objectPool | 403 | ✓ |
| 434 | HTTP连接池监控 | /appMonitor/httpConnPool | 403 | ✓ |
| 435 | 数据库连接池监控 | /appMonitor/dbConnPool | 403 | ✓ |
| 436 | 线程列表 | /appMonitor/thread | 411 | - |

---

## 5. 基础设施 (infrastructure)

### 菜单页面

| ID | 名称 | 路径 | 菜单 | 时间控件 |
|----|------|------|------|----------|
| 501 | 主机 | /infrastructure/host | ✓ | ✓ |
| 502 | Docker容器 | /infrastructure/docker | ✓ | ✓ |
| 503 | Kubernetes | /infrastructure/cluster | ✓ | ✓ |
| 504 | 进程 | /infrastructure/process | ✓ | ✓ |

### 静态页面

| ID | 名称 | 路径 | menuId | 时间控件 |
|----|------|------|--------|----------|
| 511 | 主机详情 | /infrastructure/hostDetail | 501 | ✓ |
| 514 | Kubernetes详情 | /infrastructure/clusterDetail | 503 | ✓ |
| 515 | Node分析 | /infrastructure/node | 503 | ✓ |
| 516 | Namespace | /infrastructure/namespace | 503 | ✓ |
| 517 | Namespace详情 | /infrastructure/namespaceDetail | 503 | ✓ |
| 518 | Workloads | /infrastructure/workload | 503 | ✓ |
| 519 | Workload详情 | /infrastructure/workloadDetail | 503 | ✓ |
| 520 | Pods | /infrastructure/pod | 503 | ✓ |
| 521 | Pod详情 | /infrastructure/podDetail | 503 | ✓ |
| 522 | Services | /infrastructure/service | 503 | ✓ |
| 523 | Service详情 | /infrastructure/serviceDetail | 503 | ✓ |
| 524 | Node详情 | /infrastructure/nodeDetail | 503 | ✓ |
| 525 | 进程详情 | /infrastructure/processDetail | 504 | ✓ |
| 526 | 容器详情 | /infrastructure/dockerDetail | 502 | ✓ |

---

## 6. 指标体系 (metrics)

| ID | 名称 | 路径 | 菜单 | 时间控件 |
|----|------|------|------|----------|
| 601 | 指标列表 | /metrics/list | ✓ | - |
| 602 | 指标分析 | /metrics/analysis | ✓ | ✓ |

---

## 7. 网络性能 (npm)

| ID | 名称 | 路径 | 菜单 | 时间控件 |
|----|------|------|------|----------|
| 702 | 网络分析 | /npm/analysis | ✓ | ✓ |
| 703 | 网络拓扑 | /npm/topology | ✓ | ✓ |
| 704 | DNS分析 | /npm/dns | ✓ | ✓ |

---

## 8. 访问体验 (rum)

### 菜单页面

| ID | 名称 | 路径 | 菜单 | 时间控件 |
|----|------|------|------|----------|
| 2201 | 应用概览 | /rum/application | ✓ | ✓ |
| 2202 | Web页面 | /rum/page | ✓ | ✓ |
| 2203 | Web操作 | /rum/action | ✓ | ✓ |
| 2204 | JS错误 | /rum/jsError | ✓ | ✓ |
| 2205 | Web请求 | /rum/request | ✓ | ✓ |
| 2206 | 用户追踪 | /rum/trace | ✓ | ✓ |
| 2207 | 会话分析 | /rum/session | ✓ | ✓ |
| 2208 | RUM资源管理 | /rum/resource | ✓ | - |
| 2209 | 用户画像 | /rum/user | ✓ | ✓ |

### 静态页面

| ID | 名称 | 路径 | menuId |
|----|------|------|--------|
| 70101 | 添加应用 | /rum/application/manage | 2201 |
| 70102 | 应用设置 | /rum/application/setting | 2201 |
| 70103 | 应用详情 | /rum/application/detail | 2201 |
| 70201 | Web页面详情 | /rum/page/detail | 2202 |
| 7020101 | 慢页面追踪 | /rum/page/tracking | 2202 |
| 70301 | Web操作详情 | /rum/action/detail | 2203 |
| 7030101 | 操作追踪 | /rum/action/tracking | 2203 |
| 70401 | JS错误追踪列表 | /rum/jsError/detail | 2204 |
| 7040101 | JS错误追踪 | /rum/jsError/tracking | 2204 |
| 70501 | web请求详情 | /rum/request/detail | 2205 |
| 7050101 | web请求追踪 | /rum/request/tracking | 2205 |
| 70701 | 会话详情 | /rum/session/detail | 2207 |
| 70702-70706 | IOS 启动/操作/页面/崩溃/卡顿详情 | /rum/ios/* | 2207 |
| 70802-70806 | Android 启动/操作/页面/崩溃/卡顿详情 | /rum/android/* | 2207 |
| 705 | WEB探针本地手动注入帮助文档 | /rum/help | 2201 |
| 707 | WEB探针URL聚合规则帮助文档 | /rum/help/url | 2201 |

---

## 9. 日志分析 (log)

| ID | 名称 | 路径 | 菜单 | 时间控件 |
|----|------|------|------|----------|
| 9 | 日志分析 | /log | ✓ | ✓ |

---

## 10. 部署配置 (config)

### 二级菜单

| ID | 名称 | 路径 | 菜单 |
|----|------|------|------|
| 1001 | 安装部署 | /config/install | ✓ |
| 1002 | 部署状态 | /config/status | ✓ |
| 1003 | 配置管理 | /config/manage | ✓ |

### 配置管理 - 实体监控

| ID | 名称 | 路径 | 菜单 |
|----|------|------|------|
| 1010 | 实体监控 | /config/entity | ✓ |
| 1011 | 服务监控 | /config/service | ✓ |
| 1012 | 业务系统监控 | /config/business | ✓ |
| 1013 | 进程监控 | /config/process | ✓ |
| 1014 | 请求监控 | /config/request | ✓ |

### 配置管理 - 拓扑配置

| ID | 名称 | 路径 | 菜单 |
|----|------|------|------|
| 1050 | 拓扑配置 | /config/relationest | ✓ |

### 配置管理 - 告警配置

| ID | 名称 | 路径 | 菜单 |
|----|------|------|------|
| 1020 | 告警配置 | /config/alarm | ✓ |
| 1021 | 检测规则 | /config/rule | ✓ |

### 配置管理 - 其他

| ID | 名称 | 路径 | 菜单 |
|----|------|------|------|
| 1030 | Databuff AI配置 | /config/ai | ✓ |
| 1031 | 环境标签配置 | /config/envTag | ✓ |

### 配置管理 - 系统管理

| ID | 名称 | 路径 | 菜单 | 时间控件 |
|----|------|------|------|----------|
| 1081 | 用户管理 | /sysManage/account | ✓ | - |
| 1082 | 角色管理 | /sysManage/role | ✓ | - |
| 1093 | 组织管理 | /sysManage/org | ✓ | - |
| 1092 | 管理域 | /sysManage/group | ✓ | - |
| 1083 | License管理 | /sysManage/license | ✓ | - |
| 1084 | 通知管理 | /sysManage/notice | ✓ | - |
| 1085 | 系统设置 | /sysManage/setting | ✓ | - |
| 1086 | 基础设置 | /sysManage/basic | ✓ | - |
| 1087 | 系统事件 | /sysManage/systemEvent | ✓ | ✓ |
| 1094 | 健康度配置 | /sysManage/health | ✓ | - |
| 1088 | 操作审计 | /sysManage/operationAudit | ✓ | - |

### 静态页面

| ID | 名称 | 路径 | menuId |
|----|------|------|--------|
| 1062 | 新建规则 | /configManage/alarm/ruleSetting | 1003 |
| 1064 | 新建响应策略 | /configManage/alarm/responseSetting | 1003 |
| 1067 | 更新包管理 | /config/agentPackages | 1002 |
| 1068 | 请求属性设置 | /config/request/attrCollSetting | 1003 |
| 1070 | 运行日志 | /config/runLog | 1002 |
| 1071 | 管道配置 | /config/pipelineSetting | 1001 |
| 1099 | 业务系统管理 | /config/business/manage | 1003 |
| 1072 | 编辑环境标签 | /config/envTagSetting | 1031 |
| 1089 | 系统事件详情 | /sysManage/eventDetail | 1003 |
| 1090 | 系统检测规则 | /sysManage/systemRule | 1003 |
| 1091 | 新建系统规则 | /sysManage/ruleSetting | 1003 |
| 109201 | 未分配管理域的实体 | /sysManage/group/entity | 1003 |

---

## 11. 帮助中心 (help)

| ID | 名称 | 路径 | 菜单 | 时间控件 |
|----|------|------|------|----------|
| 1101 | 入门指南 | /help/startGuide | ✓ | ✓ |
| 1102 | 时间同步 | /help/timeSync | ✓ | - |
| 1103 | NTP服务 | /help/setupNTP | ✓ | - |

---

## 16. 业务观测 (observe)

| ID | 名称 | 路径 | 菜单 | 时间控件 |
|----|------|------|------|----------|
| 1601 | 业务场景 | /observe/scene | ✓ | ✓ |
| 1602 | 业务事件 | /observe/event | ✓ | - |

### 静态页面

| ID | 名称 | 路径 | menuId |
|----|------|------|--------|
| 160101 | 场景地图 | /observe/scene/manage | 1601 |

---

## 88. 个人中心 (personal)

| ID | 名称 | 路径 | 菜单 |
|----|------|------|------|
| 88 | 个人中心 | /personal | - |

---

## 99. 高级配置 (hide)

| ID | 名称 | 路径 | 菜单 |
|----|------|------|------|
| 99 | 高级配置 | /advancedConfig | - |

**说明**：此模块为内部运维使用，不在菜单中显示。

---

## 文件位置

`src/router/route-data.ts`
