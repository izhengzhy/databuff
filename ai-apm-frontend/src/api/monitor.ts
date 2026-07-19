import http from '../utils/axios'
import { AxiosPromise, AxiosResponse } from 'axios'
export interface DBResult<T = any> {
  status: number;
  message: string;
  data: T;
}

export default {
  // 分页查询检测规则
  getRuleList (data: any) {
    return http.request({
      url: '/monitor/search',
      method: 'post',
      data
    })
  },
  // 添加规则
  addMonitor: (data: any): AxiosPromise => {
    return http.request({
      url: '/monitor/addMonitor',
      method: 'post',
      data
    })
  },
  // 编辑规则
  editMonitor: (data: any): AxiosPromise => {
    return http.request({
      url: '/monitor/editMonitor',
      method: 'post',
      data
    })
  },
  // 批量启用/停用检测规则
  toggleRuleEnable (data: any) {
    const { ids, enabled } = data
    return http.request({
      url: `/monitor/enable/${enabled}`,
      method: 'put',
      data: ids,
    })
  },
  // 批量删除规则
  batchDelMonitor: (data: any): AxiosPromise => {
    return http.request({
      url: '/monitor/batchDelMonitor',
      method: 'post',
      data
    })
  },
  // 获取规则详情
  getMonitorDetail: (data: any): AxiosPromise => {
    return http.request({
      url: '/monitor/getMonitorDetail',
      method: 'post',
      data
    })
  },
  // 导出检测规则
  exportRule (data: any) {
    return http.request({
      url: '/monitor/export',
      method: 'post',
      responseType: 'blob',
      data
    })
  },

  // 预览指标图
  getPreviewMetricGraph: (data: any): AxiosPromise => {
    return http.request({
      url: '/monitor/previewMonitorGraphV3',
      method: 'post',
      data
    })
  },

  // 获取实体对象
  getEntityObjects (params?: any) {
    return http.request({
      url: '/monitor/monitorObjs',
      method: 'get',
      params,
    })
  },

  // 分页查询系统检测规则
  getSystemRuleList (data: any) {
    if (data.sortOrder) {
      data.sortOrder = data.sortOrder.toUpperCase();
    }
    return http.request({
      url: '/monitor/system/search',
      method: 'post',
      data
    })
  },
  // 添加系统检测规则
  addSystemMonitor: (data: any): AxiosPromise => {
    return http.request({
      url: '/monitor/system/addMonitor',
      method: 'post',
      data
    })
  },
  // 编辑系统检测规则
  editSystemMonitor: (data: any): AxiosPromise => {
    return http.request({
      url: '/monitor/system/editMonitor',
      method: 'post',
      data
    })
  },
  // 批量启用/停用系统检测规则
  toggleSystemRuleEnable (data: any) {
    const { ids, enabled } = data
    return http.request({
      url: `/monitor/system/enable/${enabled}`,
      method: 'put',
      data: ids,
    })
  },
  // 批量删除系统检测规则
  batchDelSystemMonitor: (data: any): AxiosPromise => {
    return http.request({
      url: '/monitor/system/batchDelMonitor',
      method: 'post',
      data
    })
  },
  // 获取系统检测规则详情
  getSystemMonitorDetail: (data: any): AxiosPromise => {
    return http.request({
      url: '/monitor/system/getMonitorDetail',
      method: 'post',
      data
    })
  },
  // 导出系统检测规则
  exportSystemRule (data: any) {
    return http.request({
      url: '/monitor/system/export',
      method: 'post',
      responseType: 'blob',
      data
    })
  },
}
