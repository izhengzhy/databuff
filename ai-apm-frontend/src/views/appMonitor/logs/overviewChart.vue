<template>
  <div class="overview-chart-group">
    <div class="ts-chart-group clear g-xs-12">
      <div class="ts-bar-chart g-xs-6">
        <div class="ts-chart-wrapper" v-loading="barLoading1 || parentLoading">
          <div class="chart-title">{{ $t('modules.views.appMonitor.trace.s_be3a103f', { value0: logLabel }) }}</div>
          <div class="chart-cont">
            <basic-chart
              :source="chartSource1"
              :showEmpty="showEmpty1"
              :showLegend="true"
              :minInterval="1"
              group="logs"
              :yAxisSplitNum="3"
              :textSmallMode="true"
              :interval="timeParams.interval"
              :axisClickEvent="($event) => chartClickHandle($event)"
              :colors='["#7A5FF3", "#F37370"]'
              :tooltipEnterable="false" />
          </div>
        </div>
      </div>
      <div class="ts-bar-chart g-xs-6">
        <div class="ts-chart-wrapper" v-loading="barLoading2 || parentLoading">
          <div class="chart-title">{{ severityChartTitle }}</div>
          <div class="chart-cont">
            <basic-chart
              :source="chartSource2"
              :showEmpty="showEmpty2"
              :showLegend="true"
              group="logs"
              :minInterval="1"
              :yAxisSplitNum="3"
              :textSmallMode="true"
              :interval="timeParams.interval"
              :axisClickEvent="($event) => chartClickHandle($event, 'error')"
              :tooltipEnterable="false" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Vue, Component, Prop } from 'vue-property-decorator';
import i18n from '@/i18n';
import BasicChart from '@/components/charts/basic-chart.vue';
import { toAsyncWait } from '@/utils/common';
import LogApi from '@/api/log';
import { getSeverityChartColor, sortSeverities } from '@/utils/logSeverity';
import dayjs from 'dayjs';

@Component({
  components: {
    BasicChart,
  },
})
export default class LogsOverviewChart extends Vue {
  @Prop({ default: () => ({}) }) private timeParams!: any;
  @Prop({ default: () => ({}) }) private query!: any;
  @Prop({ default: false }) private queryLoading!: boolean;
  @Prop({ default: false }) private searchInitLoading!: boolean;

  private barLoading1 = false;
  private barLoading2 = false;

  private chartSource1: any = [];
  private chartSource2: any = [];

  private showEmpty1 = false;
  private showEmpty2 = true;

  private volumeCounts: Record<string, number | null> = {};

  private trendRequestSeq = 0;

  get parentLoading () {
    return this.queryLoading || this.searchInitLoading;
  }

  get logLabel () {
    return i18n.locale === 'en-US' ? 'Log' : '日志';
  }

  get severityChartTitle () {
    const level = i18n.t('modules.views.alarmCenter.eventDetail.s_3fea7ca7') as string;
    return i18n.locale === 'en-US' ? `${level} breakdown` : `${level}分布`;
  }

  public getVolumeCounts () {
    return this.volumeCounts;
  }

  public async getData () {
    const params = {
      ...this.timeParams,
      ...this.query,
    };
    const requestSeq = ++this.trendRequestSeq;
    this.barLoading1 = true;
    this.barLoading2 = true;
    const { result, error } = await toAsyncWait(LogApi.getLogTrend(params));
    if (requestSeq !== this.trendRequestSeq) {
      return this.volumeCounts;
    }
    this.barLoading1 = false;
    this.barLoading2 = false;
    if (error) {
      this.volumeCounts = {};
      this.chartSource1 = [];
      this.chartSource2 = [];
      this.showEmpty1 = true;
      this.showEmpty2 = true;
      return this.volumeCounts;
    }
    const trendData = result?.data || {};
    this.applyLogVolumeGraph(trendData.logCnts || {});
    this.applySeverityGraph(trendData.severityCnts || {});
    return this.volumeCounts;
  }

  private applyLogVolumeGraph (graphData: Record<string, number | null>) {
    this.volumeCounts = graphData;
    this.showEmpty1 = !Object.values(graphData).some((value) => value != null);
    if (this.showEmpty1) {
      this.chartSource1 = [];
      return;
    }
    const hitsSource = Object.keys(graphData)
      .sort((a: string, b: string) => Number(a) - Number(b))
      .map((date) => ({
        key: dayjs(Number(date)).format('YYYY-MM-DD HH:mm'),
        value: graphData[date] ?? '-',
      }));
    this.chartSource1 = [
      {
        name: i18n.t('modules.views.appMonitor.relationMap.s_ae1e7b60') as string,
        nameKey: 'modules.views.appMonitor.relationMap.s_ae1e7b60',
        data: hitsSource,
        type: 'bar',
        stack: 'total',
      },
    ];
  }

  private applySeverityGraph (graphData: Record<string, Record<string, number> | null>) {
    this.showEmpty2 = !Object.values(graphData).some(
      (item) => item && typeof item === 'object' && Object.keys(item).length > 0,
    );
    if (this.showEmpty2) {
      this.chartSource2 = [];
      return;
    }
    const severityKeyMap = new Map<string, string>();
    Object.values(graphData).forEach((item) => {
      if (!item || typeof item !== 'object') {
        return;
      }
      Object.keys(item).forEach((key) => {
        const normalized = key.toUpperCase();
        if (!severityKeyMap.has(normalized)) {
          severityKeyMap.set(normalized, key);
        }
      });
    });
    const severityNames = sortSeverities(Array.from(severityKeyMap.keys()));
    if (!severityNames.length) {
      this.showEmpty2 = true;
      this.chartSource2 = [];
      return;
    }
    this.chartSource2 = severityNames.map((severity) => {
      const bucketKey = severityKeyMap.get(severity) || severity;
      return {
        name: severity,
        color: getSeverityChartColor(severity),
        data: Object.entries(graphData)
          .map(([timestamp, bucket]) => ({
            key: dayjs(Number(timestamp)).format('YYYY-MM-DD HH:mm'),
            value: this.readSeverityBucketValue(bucket, bucketKey),
          }))
          .sort((a, b) => new Date(a.key).valueOf() - new Date(b.key).valueOf()),
        type: 'bar',
        stack: 'total',
      };
    });
  }

  private readSeverityBucketValue (bucket: Record<string, number> | null, severityKey: string) {
    if (!bucket || typeof bucket !== 'object') {
      return '-';
    }
    if (Object.prototype.hasOwnProperty.call(bucket, severityKey)) {
      return bucket[severityKey];
    }
    const matchedKey = Object.keys(bucket).find((key) => key.toUpperCase() === severityKey.toUpperCase());
    return matchedKey ? bucket[matchedKey] : '-';
  }

  private chartClickHandle (params: { xAxisName: string }, type?: string) {
    if (this.barLoading1 || this.barLoading2 || this.queryLoading || this.searchInitLoading) {
      return;
    }
    this.$emit('chart-click', params.xAxisName, type);
  }
}
</script>

<style lang="scss" scoped>
.overview-chart-group {
  height: auto;

  .ts-chart-group {
    .ts-bar-chart {
      height: 260px;
      padding: 0 8px;
    }
    .ts-chart-wrapper {
      height: 100%;
      background-color: var(--bg-color);
      padding: 16px 16px 0;
      position: relative;
      border: 1px solid var(--border-color-base);
      border-radius: 4px;
      .chart-title {
        font-size: 14px;
        line-height: 14px;
        user-select: none;
        color: var(--color-text-primary);
      }
      .chart-cont {
        height: calc(100% - 14px);
        padding-top: 6px;
        margin: 0 -10px;
        position: relative;
        overflow: hidden;
      }
    }
  }
}
</style>
