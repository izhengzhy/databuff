# Monitor API

> 文件: `src/api/monitor.ts`

## 概述

`monitor.ts` 主要封装检测规则管理接口，分为普通检测规则和系统检测规则两类，覆盖列表、详情、增删改、启停、导出与预览图能力。

## 接口分组

### 普通检测规则

| 方法 | HTTP | 路径 | 说明 |
|------|------|------|------|
| `getRuleList` | `POST` | `/monitor/search` | 分页查询检测规则 |
| `addMonitor` | `POST` | `/monitor/addMonitor` | 添加规则 |
| `editMonitor` | `POST` | `/monitor/editMonitor` | 编辑规则 |
| `toggleRuleEnable` | `PUT` | `/monitor/enable/{enabled}` | 批量启用/停用 |
| `batchDelMonitor` | `POST` | `/monitor/batchDelMonitor` | 批量删除 |
| `getMonitorDetail` | `POST` | `/monitor/getMonitorDetail` | 获取规则详情 |
| `exportRule` | `POST` | `/monitor/export` | 导出规则 |
| `getPreviewMetricGraph` | `POST` | `/monitor/previewMonitorGraphV3` | 预览指标图 |
| `getEntityObjects` | `GET` | `/monitor/monitorObjs` | 获取实体对象 |

### 系统检测规则

| 方法 | HTTP | 路径 | 说明 |
|------|------|------|------|
| `getSystemRuleList` | `POST` | `/monitor/system/search` | 分页查询系统检测规则 |
| `addSystemMonitor` | `POST` | `/monitor/system/addMonitor` | 添加系统规则 |
| `editSystemMonitor` | `POST` | `/monitor/system/editMonitor` | 编辑系统规则 |
| `toggleSystemRuleEnable` | `PUT` | `/monitor/system/enable/{enabled}` | 批量启停系统规则 |
| `batchDelSystemMonitor` | `POST` | `/monitor/system/batchDelMonitor` | 批量删除系统规则 |
| `getSystemMonitorDetail` | `POST` | `/monitor/system/getMonitorDetail` | 获取系统规则详情 |
| `exportSystemRule` | `POST` | `/monitor/system/export` | 导出系统规则 |

## 特殊处理

### `toggleRuleEnable` / `toggleSystemRuleEnable`

这两个方法入参是对象结构:

```ts
{
  ids: string[] | number[],
  enabled: boolean
}
```

实际请求时:

- `enabled` 会拼到 URL
- `ids` 会作为请求体直接提交

### `getSystemRuleList`

如果传入 `sortOrder`，方法内部会自动转成大写后再请求，便于兼容后端排序参数。

### `exportRule` / `exportSystemRule`

这两个导出接口都使用 `responseType: 'blob'`，返回值用于文件下载，不走普通 JSON 流。
