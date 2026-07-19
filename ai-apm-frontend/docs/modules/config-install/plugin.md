# 插件配置

> 页面: `/config/install?type=plugin`
> 文件: `src/views/configInstall/plugin/index.vue`

## 页面职责

插件页用于查看插件授权状态、启用或停用插件，并在详情弹窗中查看概述、配置说明与指标。

## 页面结构

- `plugin-search`: 按插件名称搜索
- 分区列表:
  - 已启用
  - 可用的
  - 未授权的
- `plugin-detail` 弹窗:
  - 概述
  - 配置
  - 指标

## 主要接口

- `PluginApi.getPluginList`
- `PluginApi.installedPlugin`
- `PluginApi.unInstalledPlugin`
- `PluginApi.getMetricByPlugin`

详细接口见:

- [Plugin API](../../api/plugin.md)

## 关键参数

- 列表页读取 `query` 作为名称搜索词
- 若 URL 带 `id`，列表加载完成后会自动打开对应插件详情弹窗
- 插件详情里点击规则行会跳到 `/configManage/alarm/ruleSetting?id=...`

## 注意事项

- 系统插件会被标记为 `isSystem`，默认归入“已启用”，且不能直接停用
- 已安装但未接收到数据的插件，会在详情里提示“此集成没有任何最新数据”
