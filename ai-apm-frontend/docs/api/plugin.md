# Plugin API

> 文件: `src/api/plugin.ts`

## 概述

`plugin.ts` 负责插件市场/插件管理相关能力，包括插件安装卸载、Dashboard UID 查询与插件指标。

## 接口说明

| 方法 | HTTP | 路径 | 说明 |
|------|------|------|------|
| `getPluginList` | `POST` | `/plugin/queryPluginList` | 查询插件列表 |
| `installedPlugin` | `POST` | `/plugin/installedPlugin` | 安装插件 |
| `unInstalledPlugin` | `POST` | `/plugin/unInstalledPlugin` | 卸载插件 |
| `getDashboardUID` | `POST` | `/plugin/getDashBoardUId` | 获取 Dashboard UID |
| `getMetricByPlugin` | `POST` | `/plugin/findPluginMetrics` | 查询插件指标 |
| `openPluginMetrics` | `POST` | `/plugin/openPluginMetrics` | 开启/关闭插件指标 |

## 使用特点

- 绝大多数插件操作均使用 `POST`
