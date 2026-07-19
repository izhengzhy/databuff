# 操作审计

> 页面: `/sysManage/operationAudit`
> 文件: `src/views/sysManage/operationAudit/index.vue`

## 页面职责

操作审计页用于按时间线查看平台操作记录，并支持从审计记录直接下钻到对应实体或配置页面。

## 页面结构

- `search-group`: 审计条件筛选
- 时间范围快捷筛选: 全部 / 今天 / 昨天 / 本周 / 上周 / 本月 / 上月
- 时间线列表: 按天分组，滚动分页加载

## 主要接口

- `SystemApi.getOperateAuditList`
- `search-group` 中继续使用 `system.ts` 的筛选辅助接口

详细接口见:

- [System API](../../api/system.md)

## 典型下钻

- 系统管理页: 账户、角色、组织、管理域、License、通知、基础设置、系统规则
- 配置管理页: 检测规则、响应、实体配置、AI、环境标签
- 业务页: 告警详情、服务详情、诊断分析、基础设施、RUM、指标列表

## 注意事项

- 下钻映射由页面内的 `DrillDownMapping` 维护，覆盖面很广
- 页面不是传统表格，而是按日期聚合后的时间线，因此使用了滚动加载而不是标准分页
