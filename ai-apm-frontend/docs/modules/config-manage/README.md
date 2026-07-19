# 配置管理模块

> 模块入口: `src/views/configManage/index.vue`

## 模块概述

`configManage` 是平台的配置域入口，主要覆盖实体监控配置、告警配置、Databuff AI 配置和环境标签配置。

模块根组件本身只负责渲染 `db-menu` 与子路由容器，实际业务页面分散在 `configManage/entity`、`configManage/alarm`、`configManage/ai`、`configManage/envTag` 下。

## 页面矩阵

| 类型 | 路由 | 文件 | 说明 |
|------|------|------|------|
| 页面 | `/config/service` | `entity/service/index.vue` | 服务监控配置 |
| 页面 | `/config/business` | `entity/business/index.vue` | 业务系统监控配置 |
| 页面 | `/config/process` | `entity/process/index.vue` | 进程监控配置 |
| 页面 | `/config/request` | `entity/request/index.vue` | 请求监控配置 |
| 页面 | `/config/relationest` | `entity/relation/index.vue` | 拓扑配置 |
| 页面 | `/config/rule` | `alarm/rule/index.vue` | 检测规则 |
| 页面 | `/config/ai` | `ai/index.vue` | Databuff AI 配置 |
| 页面 | `/config/envTag` | `envTag/index.vue` | 环境标签配置 |
| 静态页 | `/configManage/alarm/ruleSetting` | `alarm/ruleSetting/index.vue` | 新建/编辑规则 |
| 静态页 | `/configManage/alarm/responseSetting` | `alarm/responseSetting/index.vue` | 新建/编辑响应策略 |
| 静态页 | `/config/request/attrCollSetting` | `entity/request/attrCollSetting/index.vue` | 请求属性采集设置 |
| 静态页 | `/config/business/manage` | `entity/business/manage/index.vue` | 业务系统管理 |
| 静态页 | `/config/envTagSetting` | `envTag/setting/index.vue` | 环境标签编辑页 |

## 典型导航关系

- 检测规则 -> 规则设置: `/configManage/alarm/ruleSetting?...`
- 业务系统监控 -> 业务系统管理: `/config/business/manage?...`
- 请求监控 -> 请求属性采集设置: `/config/request/attrCollSetting?id=...`
- 环境标签列表 -> 环境标签编辑: `/config/envTagSetting?id=...&key=...`

## 主要依赖 API

- `config.ts`: AI 配置、服务监控全局配置
- `monitor.ts`: 检测规则、系统规则详情
- `alarm.ts`: 响应策略
- `envTag.ts`: 环境标签与标签值
- `bs.ts`: 业务系统与空间地图配置
- `process.ts`: 进程采集与进程识别规则
- `dataColl.ts`: 请求属性采集

## 注意事项

- 当前路由把一部分 `sysManage` 页面也挂在“配置管理”菜单下，这部分建议单独归入后续 `sys-manage` 文档
- 本轮文档只覆盖 `configManage` 自身实现的页面与静态设置页

## 已补文档

- [实体管理](entity.md)
- [告警配置](alarm.md)
- [AI 配置](ai.md)
- [环境标签](env-tag.md)
