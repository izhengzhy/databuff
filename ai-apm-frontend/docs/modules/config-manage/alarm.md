# 告警配置

> 入口路由: `/config/rule`、`/config/alarm`
> 主要文件: `src/views/configManage/alarm/*`

## 页面职责

告警配置负责维护检测规则，并提供新建、编辑、复制流程。

## 页面结构

- 检测规则: `search-group` + `table-list` + 批量操作区
- 规则设置页: 折叠表单 + 条件编辑器 + 保存/返回

## 页面与静态页

- 检测规则: `/config/rule`、`/config/alarm`
- 规则设置: `/configManage/alarm/ruleSetting`

## 主要接口

- `MonitorApi.*`: 检测规则、规则详情
- `MetricApi.*`: 指标选择、条件配置
- `ServiceApi.*`: 规则列表筛选中的监控对象选择

详细接口见:

- [Monitor API](../../api/monitor.md)
- [Metric API](../../api/metric.md)
- [Service API](../../api/service.md)

## 关键参数

- 规则设置页:
  - `id`: 规则 ID
  - `mode`: 常见为 `c` 复制、其他值视为编辑/新建

## 典型流程

- 检测规则 -> 批量启停、删除、导出
- 检测规则 -> 新建/编辑/复制 -> 规则设置页

## 关联页面

- 规则设置: `/configManage/alarm/ruleSetting?...`

## 注意事项

- `ruleSetting` 也被 `/sysManage/ruleSetting` 复用，系统规则相关说明建议放到后续 `sys-manage` 文档补充
- 检测规则页存在“系统规则”模式分支，但当前配置管理菜单主入口默认是普通规则
