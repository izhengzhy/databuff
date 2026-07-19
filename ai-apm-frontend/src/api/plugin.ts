import http from '../utils/axios'
import { AxiosPromise } from 'axios'

export default {
  /**
   * 获取所有插件列表
   */
  getPluginList: (data: any): AxiosPromise => {
    return http.request({
      url: '/plugin/queryPluginList',
      method: 'post',
      data,
    })
  },

  /**
   * 安装插件
   */
  installedPlugin: (data: any): AxiosPromise => {
    return http.request({
      url: '/plugin/installedPlugin',
      method: 'post',
      data,
    })
  },

  /**
   * 卸载插件
   */
  unInstalledPlugin: (data: any): AxiosPromise => {
    return http.request({
      url: '/plugin/unInstalledPlugin',
      method: 'post',
      data,
    })
  },

  /**
   * 获取DashboardUID
   */
  getDashboardUID (data: any) {
    return http.request({
      url: '/plugin/getDashBoardUId',
      method: 'post',
      data,
    })
  },

  /**
   * 获取插件的指标列表
   */
  getMetricByPlugin: (data: any): AxiosPromise => {
    return http.request({
      url: '/plugin/findPluginMetrics',
      method: 'post',
      data,
    })
  },

  /**
   * 开启/关闭 插件指标
   */
  openPluginMetrics: (data: any): AxiosPromise => {
    return http.request({
      url: '/plugin/openPluginMetrics',
      method: 'post',
      data,
    })
  },
}
