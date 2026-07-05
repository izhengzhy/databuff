<template>
  <db-table
    :queryApi='queryApi'
    :queryParams='tableQueryParams'
    :offsetMode='true'
    :columnConfig='columnConfig'
    @on-table-inited='tableInitedHandle'
    @sort-change='refresh'
    @on-fetch-end='onFetchEnd'
    :formatFunc='formatFunc'
    tableKey='APM_SERVICE_DETAIL_LOG'
    ref='listTable'
    class="list-wrapper">
  </db-table>
</template>

<script lang="ts">
import { Vue, Component, Prop, Watch } from 'vue-property-decorator';
import i18n from '@/i18n';
import LogApi from '@/api/log';
import { v4 as uuidv4 } from 'uuid'

@Component({})
export default class TabAlarm extends Vue {
  @Prop({ default: {} }) private current!: any;

  @Watch('current', { immediate: true })
  private onCurrentChange (val: any, oldVal: any) {
    if (val && val?.serviceId !== oldVal?.serviceId && this.isMounted) {
      this.fetchAllData();
    }
  }

  public $refs!: {
    listTable: any;
  };

  private isMounted = false;

  private timeParams = {
    fromTime: new Date().valueOf() * 1e6,
    toTime: new Date().valueOf() * 1e6,
  }

  get tableQueryParams () {
    return {
      ...this.timeParams,
      searchType: 'service',
      serviceIds: [this.current.serviceId || decodeURIComponent(String(this.$route.query.sid))],
    }
  }

  private columnConfig: any = [
    { field: '_timestamp', prop: '_timestamp', label: i18n.t('modules.views.appMonitor.errorDetail.s_13f7745f') as string, labelKey: 'modules.views.appMonitor.errorDetail.s_13f7745f', unit: 'time', minWidth: 120 },
    { field: 'serviceInstance', prop: 'serviceInstance', label: i18n.t('modules.views.alarmCenter.alarm.s_71673bab') as string, labelKey: 'modules.utils.filters.s_71673bab', minWidth: 120, showOverflowTooltip: true },
    { field: 'hostname', prop: 'hostname', label: i18n.t('modules.views.alarmCenter.alarm.s_65227369') as string, labelKey: 'modules.views.alarmCenter.alarm.s_65227369', minWidth: 80 },
    { field: 'status', prop: 'status', label: i18n.t('modules.views.alarmCenter.eventDetail.s_3fea7ca7') as string, labelKey: 'modules.views.aiPlatform.experts.s_3fea7ca7', minWidth: 80 },
    { field: 'message', prop: 'message', label: i18n.t('modules.views.alarmCenter.eventDetail.s_2d711b09') as string, labelKey: 'modules.views.alarmCenter.eventDetail.s_2d711b09', minWidth: 400 },
  ];

  private queryApi = LogApi.getLogList

  private created () {
    this.$emit('on-created');
    this.resetTimeParams();
  }
  private mounted () {
    //
  }

  public refresh () {
    this.fetchAllData();
  }

  private fetchAllData () {
    this.resetTimeParams();
    this.tableInitedHandle();
  }

  private resetTimeParams () {
    const { fromTime, toTime } = this.getGlobalTime();
    this.timeParams = {
      fromTime: fromTime.valueOf() * 1e6,
      toTime: toTime.valueOf() * 1e6,
    };
  }

  // tableInitedHandle
  private tableInitedHandle () {
    this.resetTimeParams();
    this.isMounted = true;
    if (this.current?.serviceId) {
      this.$refs.listTable.refresh();
    }
  }

  private onFetchEnd () {
    this.$emit('on-loaded')
  }

  private formatFunc (data: any[]) {
    (data || []).forEach((log: any) => {
      log.id = uuidv4();
      log._message = (log.message || '').split('\n')
      log._timestamp = log.timestamp ? +log.timestamp.substring(0, 13) : '';
    })
  }
}
</script>

<style lang="scss" scoped>
.list-wrapper {
  padding-left: 4px;
  height: 100%;
  min-height: 286px;
  :deep(.scroll-el-table-header) {
    padding-top: 0;
  }
}
</style>
