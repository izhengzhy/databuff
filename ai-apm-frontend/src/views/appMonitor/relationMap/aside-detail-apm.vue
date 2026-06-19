<template>
  <div class="node-detail-wrap">
    
    <div class="node-detail">
      <div v-if="isService || isSystem" class="detail-header">
        <div class="title">
          <span class="db-icon mr-5">{{ current.typeIcon | DbIconFilter }}</span>
          <span v-if="isService" @click.stop="viewServiceDetailHandle" class="title-link blue cp cphu">{{ current.name || $t('modules.views.appMonitor.relationMap.s_b88b9847')  }}</span>
          <template v-else>{{ current.name || $t('modules.views.appMonitor.relationMap.s_b88b9847')  }}</template>
        </div>
      </div>

      <div class="detail-content">
        <el-collapse v-model='activeNames'>
          <el-collapse-item :title="$t('modules.views.alarmCenter.alarm.s_aa0eab9d')" name='1'>
            <div class="detail-alarm">
              <div class="alarm-head flex-h" :class="{ 'w-auto': isService || isSystem }">
                <div class="title flex-h font-13">{{ $t('modules.views.alarmCenter.alarm.s_aa0eab9d') }}<span v-show='eventCount' class="count">{{ eventCount }}</span></div>
                <div class="status-tab">
                  <span
                    @click="eventStatusChangeHandle('all')"
                    :class="{ active: queryParams.status === 'all' }"
                    class="status-t cp font-12">{{ $t('modules.views.aiPlatform.experts.s_a8b0c204') }}</span>
                  <span
                    @click="eventStatusChangeHandle(3)"
                    :class="{ active: queryParams.status === 3 }"
                    class="status-t cp font-12">{{ $t('modules.views.alarmCenter.alarm.s_fc7e3846') }}</span>
                  <span
                    @click="eventStatusChangeHandle(2)"
                    :class="{ active: queryParams.status === 2 }"
                    class="status-t cp font-12">{{ $t('modules.views.alarmCenter.alarm.s_bde77082') }}</span>
                </div>
              </div>

              <div v-loading="loading.event" class="alarm-list">
                <div
                  v-for="(item, index) in eventSource"
                  :key="index"
                  class="alarm-item">
                  <el-tooltip effect="light" :content='item.level | AlarmStatusFilter' placement="top">
                    <span class="alarm-status" :data-status='item.level'></span>
                  </el-tooltip>
                  <div class="flex-h">
                    <div
                      @click="viewEventHandle(item)"
                      class="name">{{ item.descriptionKey ? $t(item.descriptionKey) : item.description }}</div>
                  </div>

                  <div class="flex-h-jc">
                    <div class="time">{{ item.timestamp | TimesToDateFilter }}</div>
                    <div v-if="isService">
                      <span
                        v-if="item.issueId"
                        @click.stop="viewEventHandle(item, 'tabCauseTree')"
                        class="blue cp ml-10 font-12">{{ $t('modules.api.alarm.ts.s_2a9e65b6') }}</span>
                      <span
                        v-if="item.problemId"
                        @click.stop="viewProblemDetail(item)"
                        class="blue cp ml-10 font-12">{{ $t('modules.views.alarmCenter.problemDetail.s_e8f54f03') }}</span>
                    </div>
                  </div>
                </div>

                <div v-if="eventSource.length" class="more">
                  <span @click="viewMoreEventHandle" class="font-12">
                    {{ $t('modules.views.appMonitor.relationMap.s_90ef7c48') }}
                    <i class="el-icon el-icon-arrow-right"></i>
                  </span>
                </div>

                <div v-if="!loading.event && !eventSource.length" class="empty">{{ $t('modules.components.charts.s_21efd88b') }}</div>
              </div>
            </div>
          </el-collapse-item>


          <template v-if="isService">
            <el-collapse-item :title="$t('modules.views.alarmCenter.problemDetail.s_207c26c9')" name='2'>
              <div class="chart-wrap">
                <div class="chart-cont" v-loading="loading.response">
                  <basic-chart
                    :showEmpty="!loading.response && !responseSource.length"
                    :colors="['#8DCFF8', '#854ECA']"
                    :showLegend="true"
                    :minInterval="1"
                    :min="0"
                    :yAxisSplitNum='2'
                    :textSmallMode="true"
                    :compactGrid='true'
                    :interval="queryParams.interval"
                    :showAxisLabelCount="4"
                    :source='responseSource' />
                </div>
              </div>
            </el-collapse-item>

            <el-collapse-item :title="$t('modules.views.appMonitor.cache.s_0c8524d7')" name='3'>
              <div class="chart-wrap">
                <div class="chart-cont" v-loading="loading.error">
                  <basic-chart
                    :showEmpty="!loading.error && !errorSource.length"
                    :showLegend="true"
                    :minInterval="1"
                    :min="0"
                    :yAxisSplitNum='2'
                    :textSmallMode="true"
                    :compactGrid='true'
                    :interval="queryParams.interval"
                    :showAxisLabelCount="4"
                    :source='errorSource' />
                </div>
              </div>
            </el-collapse-item>

            <el-collapse-item :title="$t('modules.views.appMonitor.serviceFlow.s_8bc42b53')" name='4'>
              <div class="chart-wrap">
                <div class="chart-cont" v-loading="loading.request">
                  <basic-chart
                    :showEmpty="!loading.request && !requestSource.length"
                    :colors='["#5273E0", "#F37370"]'
                    :showLegend="true"
                    :minInterval="1"
                    :min="0"
                    :yAxisSplitNum='2'
                    :textSmallMode="true"
                    :compactGrid='true'
                    :interval="queryParams.interval"
                    :showAxisLabelCount="4"
                    :source='requestSource' />
                </div>
              </div>
            </el-collapse-item>

            <template v-if='isMidType'>
              <el-collapse-item v-for='option in chartGroup' :key='option.id' :title='option.title' :name='option.id'>
                <template slot="title">
                  <span class='ell' :title='option.title'>{{ option.titleKey ? $t(option.titleKey) : option.title }}</span>
                </template>
                <div class="chart-wrap">
                  <div class="chart-cont" v-loading="option.loading">
                    <basic-chart
                      :showEmpty="!option.loading && !option.source.length"
                      :colors='["#5273E0", "#F37370"]'
                      :showLegend="true"
                      :minInterval="1"
                      :min="0"
                      :yAxisSplitNum='2'
                      :textSmallMode="true"
                      :compactGrid='true'
                      :interval="queryParams.interval"
                      :showAxisLabelCount="4"
                      :source='option.source' />
                  </div>
                </div>
              </el-collapse-item>
            </template>

          </template>
        </el-collapse>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Vue, Component, Watch, Prop } from 'vue-property-decorator'
import dayjs from 'dayjs';
import i18n from '@/i18n';
import BasicChart from '@/components/charts/basic-chart.vue';
import { toAsyncWait } from '@/utils/common'
import AlarmApi from '@/api/alarm';
import ServiceApi from '@/api/service';
import MetricApi from '@/api/metric'
import { formatMetricBaseQuery } from '@/utils/metric-query-format'
import { v4 as uuidv4 } from 'uuid'


const getMidMetricOption = (id = uuidv4(), title = '', unit = '', metric = '') => {
  return {
    id,
    loading: false,
    source: [],
    title,
    unit,
    params: {
      ...formatMetricBaseQuery(metric, [['serviceId', '=', '', 'AND']])
    }
  }
}

const formatChartDataToMapping = (data: any[], isErrorType?: boolean) => {
  const formatValues = (values: any[]) => {
    const _map: any = {}
    values.forEach((item: any) => {
      _map[item[0]] = item[1]
    })
    return _map
  }
  if (!isErrorType) {
    return formatValues(data[0]?.values || [])
  } else {
    const mapping: any = {}
    data.forEach((item: any) => {
      const errorType = item?.tags?.errorType || ''
      mapping[errorType] = formatValues(item?.values || [])
    })
    return mapping;
  }
}

@Component({
  components: {
    BasicChart,
  },
})
export default class NodeDetail extends Vue {
  @Prop({ default: () => ({}) }) private current!: any;
  @Prop({ default: null }) private businessList!: any;

  @Watch('current', { immediate: true })
  private onCurrentChange (newVal: any, oldVal: any) {
    newVal = newVal || {}
    oldVal = oldVal || {}
    if (!newVal.id) {
      return
    }
    if (newVal.baseType === oldVal.baseType && newVal.id === oldVal.id) {
      return
    }
    this.fetchAllData()
  }

  get isService () {
    return (this.current || {}).baseType === 'service'
  }

  get getBasicServiceMap () {
    return this.$store.getters['Service/basicServiceMap']
  }

  private midTypes: any[] = [];
  private isMidType = false;

  get isApplication () {
    return (this.current || {}).baseType === 'application'
  }

  get isSystem () {
    const current = this.current || {}
    return current.baseType && current.baseType === 'business'
  }
  private activeNames = ['1', '2', '3', '4', '5', '6', '7', '8']

  private queryParams: any = {
    fromTime: '',
    toTime: '',
    interval: 300,
    status: 'all', // 'all'|3|2
  }

  private loading = {
    event: true,
    request: true,
    error: true,
    response: true,
  }

  private requestSource: any = []
  private errorSource: any = []
  private responseSource: any = []

  private chartGroup: any = {}

  private async created () {
    //
  }

  private mounted () {
    // const
  }

  private regetGlobalTime () {
    const { fromTime, toTime, interval } = this.getGlobalTimeV2()
    this.queryParams = {
      ...this.queryParams,
      fromTime, toTime, interval
    }
  }

  public async fetchAllData () {
    this.queryParams.status = 'all'
    this.regetGlobalTime();
    if (this.current?.id && this.getBasicServiceMap[this.current.id]?.type) {
      this.isMidType = this.midTypes.some(item => item.app === this.getBasicServiceMap[this.current.id]?.type);
      if (this.isMidType) {
        const mid = this.midTypes.find(item => item.app === this.getBasicServiceMap[this.current.id]?.type);
        if (mid && Array.isArray(mid?.metrics?.selectedMetrics)) {
          const chartGroup: any = {};
          mid.metrics.selectedMetrics.forEach((item: any) => {
            const id = uuidv4();
            chartGroup[id] = getMidMetricOption(id, item.alias, '', item.metricName)
            this.activeNames.push(id);
          });
          this.chartGroup = chartGroup;
        }
      }
    }
    if (this.isMidType) {
      this.setIsMysqlParams();
      this.getMysqlChartData();
    }
    this.getEventSource();
    if (this.isService) {
      this.getRequestSource()
      this.getErrorSource()
      this.getResponseSource()
    }
  }

  private setIsMysqlParams () {
    const { fromTime, toTime, interval } = this.getGlobalTimeV2()
    const start = new Date(fromTime).valueOf();
    const end = new Date(toTime).valueOf();
    const serviceId = this.current.id;
    
    Object.keys(this.chartGroup).forEach(key => {
      (this.chartGroup as any)[key].params = {
        ...(this.chartGroup as any)[key].params,
        ...formatMetricBaseQuery(
          (this.chartGroup as any)[key].params.metric,
          [['serviceId', '=', serviceId, 'AND']],
          start, end, interval
        )
      }
    });
    // console.log(JSON.parse(JSON.stringify(this.chartGroup)))
  }

  // 获取服务相关告警
  private eventSource: any[] = [];
  private eventCount = 0;

  private async getEventSource () {
    const { fromTime, toTime, interval, status } = this.queryParams
    const _params: any = {
      needCount: true, // 是否需要total、count字段
      fromTime,
      toTime,
      interval,
      offset: 0,
      size: 5,
      sortField: 'timestamp',
      sortOrder: 'desc',
      level: status === 'all' ? null : [status],
      isAPM: 1,
      trigger: {},
    }

    if (this.isService) {
      _params.trigger.serviceId = [this.current.id]
    } else if (this.isSystem) {
      _params.trigger.busName = [this.current.name]
      _params.serviceProvider = 'ALARM_SYSTEM_TOPO'
    } else {
      delete _params.trigger
    }
    this.loading.event = true
    const { result, error } = await toAsyncWait(AlarmApi.getAlarmListNew(_params))
    this.loading.event = false;
    if (!error) {
      const data = result.data || {}
      this.eventSource = data.list || [];
      this.eventCount = data.total || 0;
      if (status === 'all' && !this.eventCount) {
        this.activeNames = this.activeNames.filter(i => i !== '1')
      } else {
        this.activeNames = ['1', '2', '3', '4', '5', '6', '7', '8']
      }
    }
  }
  private eventStatusChangeHandle (status: 'all'|3|2) {
    this.queryParams.status = status
    this.regetGlobalTime()
    this.getEventSource()
  }
  // 跳转至告警详情
  private viewEventHandle (row: any, type?: string) {
    // v2.7.2版本暂时去掉权限判断
    // if (row && !row.hasLimit) {
    //   this.$message.warning(i18n.t('modules.views.appMonitor.relationMap.s_e89717d4') as string)
    //   return;
    // }
    const query: any = { aid: row.id }
    if (type) {
      query.type = type
    }
    this.$router.push({
      path: '/alarmCenter/alarmDetail',
      query
    })
  }
  // 查看更多事件
  private async viewMoreEventHandle () {
    const { fromTime, toTime, durationRange } = this.$route.query
    const query: any = {}
    if (fromTime && toTime) {
      query.fromTime = fromTime
      query.toTime = toTime
    } else if (durationRange) {
      query.durationRange = durationRange
    }
    if (this.isService) {
      query.serviceId = encodeURIComponent(this.current.id)
    } else if (this.isSystem) {
      query.busName = encodeURIComponent(this.current.name)
    }
    this.$router.push({
      path: '/alarmCenter/alarm',
      query,
    });
  }

  // 获取请求数
  private async getRequestSource () {
    const { fromTime, toTime, interval } = this.queryParams
    const _params: any = {
      startTime: fromTime,
      endTime: toTime,
      interval,
      serviceId: this.current.id,
    }
    this.loading.request = true
    const successRst = await toAsyncWait(ServiceApi.getServiceRequestMetric({ ..._params, metric: 'succReqCount' }))
    const successData = formatChartDataToMapping(successRst?.result?.data || [])
    const failedRst = await toAsyncWait(ServiceApi.getServiceRequestMetric({ ..._params, metric: 'errReqCount' }))
    const failedData = formatChartDataToMapping(failedRst?.result?.data || [])
    if (!Object.keys(successData).length && !Object.keys(failedData).length) {
      this.requestSource = []
    } else {
      this.requestSource = [{
        name: i18n.t('modules.views.alarmCenter.notice.s_330363df') as string, nameKey: 'modules.utils.filters.s_330363df',
        type: 'bar',
        stack: 'total',
        data: Object.entries(successData).map(([timestamp, value]) => ({
          key: dayjs(Number(timestamp)).format('YYYY-MM-DD HH:mm'),
          value,
        }))
      }, {
        name: i18n.t('modules.views.alarmCenter.notice.s_acd5cb84') as string, nameKey: 'modules.utils.filters.s_acd5cb84',
        type: 'bar',
        stack: 'total',
        data: Object.entries(failedData).map(([timestamp, value]) => ({
          key: dayjs(Number(timestamp)).format('YYYY-MM-DD HH:mm'),
          value,
        }))
      }]
    }
    this.loading.request = false;
  }

  // 获取错误数
  private async getErrorSource () {
    const { fromTime, toTime, interval } = this.queryParams
    const _params: any = {
      startTime: fromTime,
      endTime: toTime,
      interval,
      serviceId: this.current.id,
    }
    this.loading.error = true;
    const typeErrorRst = await toAsyncWait(ServiceApi.getServiceRequestMetric({ ..._params, metric: 'typeErrCount' }))
    const typeErrorData = formatChartDataToMapping(typeErrorRst?.result?.data || [], true)
    const errorRateRst = await toAsyncWait(ServiceApi.getServiceRequestMetric({ ..._params, metric: 'errRate' }))
    const errorRateData = formatChartDataToMapping(errorRateRst?.result?.data || [])
    if (!Object.keys(typeErrorData).length && !Object.keys(errorRateData).length) {
      this.errorSource = []
    } else {
      this.errorSource = [
        ...(Object.entries(typeErrorData).map(([key, _source]: [string, any]) => ({
          name: key,
          type: 'bar',
          stack: 'total',
          data: Object.entries(_source).map(([timestamp, value]) => ({
            key: dayjs(Number(timestamp)).format('YYYY-MM-DD HH:mm'),
            value,
          }))
        }))),
        {
          name: i18n.t('modules.views.appMonitor.cache.s_0c8524d7') as string, nameKey: 'modules.views.appMonitor.cache.s_0c8524d7',
          unit: '%',
          data: Object.entries(errorRateData).map(([timestamp, value]) => ({
            key: dayjs(Number(timestamp)).format('YYYY-MM-DD HH:mm'),
            value,
          }))
        }
      ]
    }
    this.loading.error = false;
  }

  // 获取响应时间
  private async getResponseSource () {
    const { fromTime, toTime, interval } = this.queryParams
    const _params: any = {
      startTime: fromTime,
      endTime: toTime,
      interval,
      serviceId: this.current.id,
    }
    this.loading.response = true;
    const avgTimeRst = await toAsyncWait(ServiceApi.getServiceRequestMetric({ ..._params, metric: 'avgTime' }))
    const avgTimeData = formatChartDataToMapping(avgTimeRst?.result?.data || [])
    const reqCntRst = await toAsyncWait(ServiceApi.getServiceRequestMetric({ ..._params, metric: 'reqCount' }))
    const reqCntData = formatChartDataToMapping(reqCntRst?.result?.data || [])
    if (!Object.keys(avgTimeData).length && !Object.keys(reqCntData).length) {
      this.responseSource = []
    } else {
      this.responseSource = [{
        name: i18n.t('modules.views.appMonitor.relationMap.s_207c26c9') as string, nameKey: 'modules.views.appMonitor.relationMap.s_207c26c9',
        type: 'line',
        area: true,
        unit: 'ns',
        data: Object.entries(avgTimeData).map(([timestamp, value]) => ({
          key: dayjs(Number(timestamp)).format('YYYY-MM-DD HH:mm'),
          value,
        }))
      }, {
        name: i18n.t('modules.views.appMonitor.relationMap.s_ae1e7b60') as string, nameKey: 'modules.views.appMonitor.relationMap.s_ae1e7b60',
        type: 'bar',
        data: Object.entries(reqCntData).map(([timestamp, value]) => ({
          key: dayjs(Number(timestamp)).format('YYYY-MM-DD HH:mm'),
          value,
        }))
      }]
    }
    this.loading.response = false;
  }


  // 获取mysql相关指标趋势图
  private async getMysqlChartData () {
    if (!this.isMidType) {
      return
    };
    Object.keys(this.chartGroup).forEach(key => {
      // (this.chartGroup as any)[key].params.serviceId = serviceId;
      this.chartGroup[key as keyof typeof this.chartGroup].loading = true;
    });
    const { fromTime, toTime, interval } = this.getGlobalTimeV2()
    const _params = {}
    const { result, error } = await toAsyncWait(MetricApi.getMysqlMetricsTrend({
      startTime: fromTime, endTime: toTime, interval, serviceId: this.current.id
    }));
    if (!error) {
      const { data = [] } = result || {};
      if (Array.isArray(data)) {
        Object.keys(this.chartGroup).forEach(key => {
          this.formatMysqlData(key, data.find((t: any) => t.columns.includes(key)));
        });
      }
    }
    Object.keys(this.chartGroup).forEach(key => {
      this.chartGroup[key].loading = false;
    });
  }

  private async formatMysqlData (type: keyof typeof this.chartGroup, source: any) {
    const chartOption = this.chartGroup[type];
    if (source && Array.isArray(source?.values)) {
      this.chartGroup[type].source = [{
        name: chartOption.title,
        type: 'line',
        unit: source?.units?.[1] || '',
        data: source.values.map(([timestamp, value]: any) => ({
          key: dayjs(Number(timestamp)).format('YYYY-MM-DD HH:mm'),
          value,
        }))
      }]
    } else {
      this.chartGroup[type].source = [{
        name: chartOption.title,
        type: 'line',
        data: []
      }]
    }
  }

  private async fetchSubBusList (topoType: 1|2|3, id: string) {
    if (topoType && topoType === 2 && this.businessList && this.businessList.nodes) {
      const { nodes = [] } = this.businessList || {}
      const _source = nodes.find((item: any) => item.id === id)
      if (_source && _source.children && _source.children.length) {
        return _source.children.map((item: any) => `${this.current.name}-${item.name}`)
      }
    }
    return []
  }

  // 跳转至服务详情
  private viewServiceDetailHandle () {
    const { id, name } = this.current || {}
    if (!id) {
      return
    }
    this.$router.push({
      path: '/appMonitor/serviceDetail',
      query: {
        ...this.getRouteTimeOrRange,
        sn: encodeURIComponent(name || ''),
        sid: encodeURIComponent(id),
      }
    })
  }

  // 问题详情
  private viewProblemDetail (row: any) {
    const routeData = this.$router.resolve({
      path: '/alarmCenter/problemDetail',
      query: { id: row.problemId, __nw: 't' },
    });
    window.open(routeData.href, '_blank');
  }
}
</script>

<style lang="scss" scoped>
.node-detail-wrap {
  height: 100%;

  .node-detail {
    width: 310px;
    height: 100%;
    display: flex;
    flex-direction: column;
    background: #1B1B1B;
    box-shadow: -4px 0px 6px 0px rgba(18, 18, 18, 0.51);
    color: #EBEBED;
  }
}

.detail-header {
  padding: 7px 30px 7px 16px;
  border-bottom: 1px solid #272727;

  .title {
    font-size: 14px;
    line-height: 24px;
    text-overflow: ellipsis;
    white-space: nowrap;
    overflow: hidden;
    .db-icon {
      display: inline-block;
      vertical-align: middle;
    }
    .title-link:hover {
      color: var(--color-text-link);
    }
  }
}

.detail-content {
  padding: 0 15px 20px;
  flex: 1;
  overflow: auto;
}

.detail-alarm {
  .alarm-head {
    margin-bottom: 14px;
    padding: 0 6px;
    line-height: 22px;
    font-size: 12px;
    .title {
      width: 130px;
      padding-right: 15px;
    }
    .count {
      margin-left: 6px;
      padding: 0 4px;
      background: #E12828;
      font-size: 12px;
      line-height: 14px;
      border-radius: 10px;
    }
    .status-tab {
      color: #777A7E;
      font-size: 13px;
    }
    .status-t {
      &.active {
        color: #EBEBED;
      }
      & + .status-t {
        margin-left: 13px;
        position: relative;
        &::before {
          content: '';
          margin-top: -5px;
          width: 1px;
          height: 10px;
          background: #414141;
          position: absolute;
          top: 50%;
          left: -7px;
        }
      }
    }
    &.w-auto {
      justify-content: space-between;
      .title {
        width: auto;
      }
    }
  }
  .alarm-list {
    padding: 0 6px;
    min-height: 90px;
  }
  .alarm-item {
    padding-left: 8px;
    position: relative;
    font-size: 12px;
    line-height: 16px;
    & + .alarm-item {
      margin-top: 14px;
    }
    .alarm-status {
      width: 2px;
      height: 28px;
      border-radius: 5px;
      position: absolute;
      top: 2px;
      left: 0;
      background-color: #B5B7BB;
      &[data-status="3"] {
        background-color: #E12828;
      }
      &[data-status="2"] {
        background-color: #F79532;
      }
    }
    .time {
      transform-origin: center left;
      color: #777A7E;
    }
    .name {
      padding-right: 70px;
      text-overflow: ellipsis;
      white-space: nowrap;
      overflow: hidden;
      cursor: pointer;
      &:hover {
        color: var(--color-text-link);
      }
    }
    .state {
      font-size: 12px;
      color: #777A7E;
      position: absolute;
      top: 0;
      right: 0;
    }
    &.finished-item {
      opacity: 0.5;
    }
  }
  .empty {
    height: 90px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #777A7E;
  }
  .more {
    margin-top: 6px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    line-height: 18px;
    color: #777A7E;
    span {
      cursor: pointer;
      &:hover {
        color: var(--color-text-link);
      }
    }
  }
}

.chart-wrap {
  margin-top: 10px;
  & + .chart-wrap {
    margin-top: 10px;
  }
  .chart-cont {
    height: 140px;
  }
}
.font-12 {
  font-size: 12px;
}
.font-13 {
  font-size: 13px;
}
.detail-content {
  :deep(.el-collapse-item__content) {
    padding-bottom: 16px;
  }
  :deep(.el-collapse),
  :deep(.el-collapse-item__header),
  :deep(.el-collapse-item__wrap) {
    border: none;
  }
}
:root[data-theme=light] .node-detail-wrap {
  .node-detail {
    background: #FFFFFF;
    box-shadow: -4px 0px 6px 0px rgba(119, 122, 126, 0.06);
    color: #121317;
  }
  .detail-header {
    border-bottom-color: #EEEFF1;
  }

  .detail-alarm {
    .alarm-head {
      .count {
        color: #FFFFFF;
      }
      .status-tab {
        color: #8B8E93;
      }
      .status-t {
        &.active {
          color: #121317;
        }
        & + .status-t::before {
          background: #E7E8E8;
        }
      }
    }
    .alarm-item .state,
    .empty {
      color: #A3A5A8;
    }
  }
}
@media screen and (min-width: 1500px) {
  .node-detail-wrap  .node-detail {
    width: 350px;
  }
}
@media screen and (min-width: 1900px) {
  .node-detail-wrap  .node-detail {
    width: 390px;
  }
}
</style>
