import http from '../utils/axios'

function buildBasicServiceInstanceQuery (params: any) {
  const { fromTime, toTime, start, end, ...rest } = params || {}
  const query: Record<string, unknown> = { ...rest }
  if (start != null) {
    query.start = start
  } else if (fromTime != null && fromTime !== '') {
    query.start = +new Date(fromTime)
  }
  if (end != null) {
    query.end = end
  } else if (toTime != null && toTime !== '') {
    query.end = +new Date(toTime)
  }
  return query
}

export default {
  // 获取服务流的入口
  getServiceFlowEndpoint (data: any) {
    return http.request({
      url: '/trace/serviceFlowEndpoint',
      method: 'post',
      data
    })
  },
  // 获取服务级别的服务流
  getServiceFlow (data: any) {
    return http.request({
      url: '/trace/multipleServiceFlow',
      method: 'post',
      data
    })
  },
  /**
   * 获取服务详情指标趋势图
   * metric:1.响应时间 avgTime、总的请求数量 reqCount
   * 2.不同错误类型的失败请求数量 typeErrCount、总的错误率 errRate
   * 3.成功请求数 succReqCount、失败请求数 errReqCount
   */
  getServiceRequestMetric (data: any) {
    return http.request({
      url: '/service/serviceDetailTrendChart',
      method: 'post',
      data
    })
  },
  /**
   * 获取服务告警事件
   */
   getServiceRelate (params: any) {
    return http.request({
      method: 'get',
      url: '/service/getServiceInstanceRelations',
      params
    })
  },
  /**
   * 添加服务标签
   */
   addServicesLabel (data: any) {
    return http.request({
      method: 'post',
      url: '/service/customServiceTag',
      data
    })
  },
  /**
   * 更新服务显示名称
   */
   updateServiceName (data: any) {
    return http.request({
      method: 'post',
      url: '/service/updateService',
      data
    })
  },
  /**
   * 获取全量服务名称&id列表（有管理域限制）
   */
   getServicesIds (data: {
      fromTime: string, toTime: string,
      serviceId?: string, serviceIds?: string[], serviceType?: string, serviceTypes?: string[],
      serviceName?: string, serviceNames?: string[], datasources?: string[], virtualService?: 0|1,
      ignoreTime?: number }) {
    return http.request({
      url: '/service/basicServices',
      method: 'post',
      data,
    })
  },
  /**
   * 获取全量服务名称&id列表（无管理域限制）
   */
   getAllServicesIds (data: {
      fromTime: string, toTime: string,
      serviceId?: string, serviceIds?: string[], serviceType?: string, serviceTypes?: string[],
      serviceName?: string, serviceNames?: string[],
      ignoreTime?: number }) {
    return http.request({
      url: '/service/basicAllServices',
      method: 'post',
      data,
    })
  },
  /**
   * 获取来源服务列表 --- 无关管理域
   */
   getSrcServices (data: any) {
    return http.request({
      url: '/service/resourcesGroupBy',
      method: 'post',
      data,
    })
  },
  /**
   * 获取服务下的服务实例
   */
  getServiceInstance (params: any) {
    return http.request({
      url: '/service/getServiceInstance',
      method: 'get',
      params: buildBasicServiceInstanceQuery(params)
    })
  },
  /**
   * 获取服务及服务下实例的基本信息，不包含事件等需要消耗性能的字段
   */
  getBasicServiceInstance (params: any) {
    return http.request({
      url: '/service/getBasicServiceInstance',
      method: 'get',
      params: buildBasicServiceInstanceQuery(params)
    })
  },
  /**
   * 获取服务及服务下实例的基本信息，不包含事件等需要消耗性能的字段
   */
  getBasicServiceInstanceV2 (params: any) {
    return http.request({
      url: '/service/getBasicServiceInstance',
      method: 'get',
      params: buildBasicServiceInstanceQuery(params),
      transformResponse: [(resData) => {
        try {
          const _resData: any = typeof resData === 'string' ? JSON.parse(resData) : resData;
          if (typeof _resData?.data === 'object') {
            _resData.data.list = _resData?.data?.serviceInstances || []
          } else {
            _resData.data = { list: [], total: 0 }
          }
          return { ..._resData }
        } catch (err) {
          return resData
        }
      }]
    })
  },

  // 调用分析，获取调用关系基本信息
  getServiceCallInfo (data: any) {
    return http.request({
      url: '/service/call_info',
      method: 'post',
      data
    })
  },
  // 调用分析，获取调用关系折线图统计
  getServiceCallGraphStats (data: any) {
    return http.request({
      url: '/service/call_graph_stats',
      method: 'post',
      data
    })
  },
  // 调用分析，获取调用关系消费延迟折线图
  getServiceCallDelayGraph (data: any) {
    return http.request({
      url: '/service/call_mq_delay_graph_stats',
      method: 'post',
      data
    })
  },
  // 调用分析，获取调用关系客户端服务端接口sql列表
  getServiceCallEndpoints (data: any) {
    return http.request({
      url: '/service/call_endpoints',
      method: 'post',
      data
    })
  },
  // 调用分析，获取连接池、对象池及池子名称
  getServiceCallPools (data: any) {
    return http.request({
      url: '/service/pool_get_names',
      method: 'post',
      data
    })
  },
  // 调用分析出入口详情，获取span列表
  getServiceCallSpans (data: any) {
    return http.request({
      url: '/trace/call_spans',
      method: 'post',
      data
    })
  },
  // 调用分析出入口详情，获取LightApmErrSpan列表
  getServiceCallLightApmErrSpans (data: any) {
    return http.request({
      url: '/trace/lightApmErrSpan',
      method: 'post',
      data
    })
  },

  // 错误分布列表，按接口名：rootResource，resource 为报错 span 的 resource
  getErrorDistList (data: any) {
    return http.request({
      url: '/service/exceptionDistMap',
      method: 'post',
      data
    })
  },
  // 查询错误Span
  getErrorSpanList (data: any) {
    return http.request({
      url: '/trace/exceptionList',
      method: 'post',
      data
    })
  },

  // Profiling 火焰图
  getProfilingFlame (data: any) {
    return http.request({
      url: '/v3/profiling/flame',
      method: 'post',
      data
    })
  },

  // 获取Profiling筛选数据
  getProfilingParams (params: any) {
    return http.request({
      url: '/v3/profiling/tags',
      method: 'get',
      params
    })
  },

  /**
   * 获取数据库列表
   */
  getDatabaseList (data: any) {
    return http.request({
      url: '/service/dbList',
      method: 'post',
      data
    })
  },
  /**
   * 获取MQ列表
   */
  getMqList (data: any) {
    return http.request({
      url: '/service/mqList',
      method: 'post',
      data
    })
  },
  /**
   * 获取缓存列表
   */
  getCacheList (data: any) {
    return http.request({
      url: '/service/cacheList',
      method: 'post',
      data
    })
  },
  /**
   * 获取缓存列表
   */
  getRemoteList (data: any) {
    return http.request({
      url: '/service/remoteCallList',
      method: 'post',
      data
    })
  },
  /**
   * 服务红绿灯
   */
  getServicesHealth (data: any) {
    return http.request({
      url: '/cockpit/trafficLight',
      method: 'post',
      data
    })
  },
  /**
   * 红绿灯阈值配置
   */
  setHealthConfig (data: any) {
    return http.request({
      url: '/cockpit/setConfig',
      method: 'post',
      data
    })
  },

  /**
   * 获取红绿灯阈值配置
   */
  getHealthConfig (params?: any) {
    return http.request({
      url: `/cockpit/getConfig`,
      method: 'get',
      params
    })
  },
  /**
   * 获取服务告警趋势
   */
  getServiceAlarmTrend (data: any) {
    return http.request({
      url: '/cockpit/countServiceAlarms',
      method: 'post',
      data
    })
  },
  /**
   * 获取服务告警数和异常数
   */
  getServiceAlarmTotal (data: any) {
    return http.request({
      url: '/cockpit/countServiceAlarmsTotal',
      method: 'post',
      data
    })
  },


  // 获取接口详情
  getResourceDetail (data: any) {
    return http.request({
      url: '/service/resourceInfo',
      method: 'post',
      data
    })
  },

  // 获取接口Span列表
  getResourceSpanList (data: any) {
    return http.request({
      url: '/trace/spanList',
      method: 'post',
      data
    })
  },

  // 获取接口Span列表
  getResourceSlowSpanList (data: any) {
    return http.request({
      url: '/trace/slowSpanList',
      method: 'post',
      data
    })
  },

  // 获取接口Span列表
  getResourceErrorSpanList (data: any) {
    return http.request({
      url: '/trace/errorSpanList',
      method: 'post',
      data
    })
  },

  getServiceRequestByCompTypes (data: any) {
    return http.post('/service/resources', data)
  },

  // 获取接口详情图表
  getRequestMetricStats (data: any) {
    return http.request({
      url: '/service/metric_stats',
      method: 'post',
      data
    })
  },
  // 获取接口详情耗时分解图表
  getRequestResourceStats (data: any) {
    return http.request({
      url: '/service/resource_stats',
      method: 'post',
      data
    })
  },

  // 获取服务列表指标趋势图
  getServiceListTrendChart (data: any) {
    return http.post('/service/serviceListTrendChart', data)
  },
  // 获取服务详情指标趋势图
  getServiceDetailTrendChart (data: any) {
    return http.post('/service/serviceDetailTrendChart', data)
  },
}
