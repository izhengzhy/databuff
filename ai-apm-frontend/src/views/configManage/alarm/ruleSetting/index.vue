<template>
  <div class="setting-wrapper" v-loading="detailLoading">
    <metric-rule
      v-if="!detailLoading"
      :detail="detail"
      @on-close="closeHandle"
      class="setting-content" />
  </div>
</template>

<script lang="ts">
import { Vue, Component } from 'vue-property-decorator';
import i18n from '@/i18n';
import MetricRule from './metric-rule.vue';
import { toAsyncWait } from '@/utils/common'
import MonitorApi from '@/api/monitor';
import {
  ALARM_DETAIL_PATH,
  SYSTEM_RULE_LIST_PATH,
  buildAlarmListLocation,
  stripAlarmDetailQuery,
} from '../alarm-routes';

@Component({
  components: {
    MetricRule,
  }
})
export default class RuleSetting extends Vue {
  private routerTimer: any = null;

  private ruleTypes = ['singleMetric']

  private detail: any = null;
  private detailLoading = false;

  get isSystemRule () {
    return this.$route.path === '/sysManage/ruleSetting'
  }

  private async created() {
    const { id, mode } = this.$route.query
    // 设置面包屑
    this.$nextTick(() => {
      this.$store.commit('UPDATE_BREADCRUMB', [{
        name: i18n.t('modules.views.configManage.alarm.s_c3f52565', { value0: id && mode !== 'c' ? i18n.t('modules.views.configManage.alarm.s_95b351c8') as string : i18n.t('modules.views.configManage.alarm.s_26bb8418') as string }) as string,
        path: this.$route.path,
      }]);
    });

    if (id) {
      this.detailLoading = true;
      const fetchUrl = this.isSystemRule ? 'getSystemMonitorDetail' : 'getMonitorDetail'
      const { result, error } = await toAsyncWait(MonitorApi[fetchUrl]({ monitorId: (id as string) }))
      this.detailLoading = false;
      if (error || !result.data || !this.ruleTypes.includes(result.data.classification)) {
        this.$message.error(error || !result.data ? i18n.t('modules.views.configManage.alarm.s_e24facea') as string : i18n.t('modules.views.configManage.alarm.s_c69cb424') as string);
        this.routerTimer = setTimeout(() => {
          this.closeHandle(true)
        }, 2500)
        return;
      }
      if (mode === 'c') {
        delete result.data.id
      }
      this.detail = result.data
    }
  }

  private beforeDestroy() {
    // 清空面包屑
    this.$store.commit('CLEAR_BREADCRUMB');

    if (this.routerTimer) {
      window.clearTimeout(this.routerTimer);
      this.routerTimer = null;
    }
  }

  private closeHandle(isReplace?: boolean) {
    const isSystemRule = this.$route.path === ALARM_DETAIL_PATH.systemRuleSetting
    const location = isSystemRule
      ? { path: SYSTEM_RULE_LIST_PATH, query: stripAlarmDetailQuery(this.$route.query) }
      : buildAlarmListLocation('rule', this.$route.query)
    this.$router[!isReplace ? 'push' : 'replace'](location)
  }
}
</script>

<style lang="scss" scoped>
.setting-wrapper {
  flex: 1;
  padding: 16px;
  height: 100%;
  font-size: 13px;
  color: var(--color-text-primary);
  overflow: auto;
  position: relative;

  .setting-content {
    display: flex;
    flex-direction: column;
    min-height: 100%;
    padding: 20px;
    border-radius: 4px;
    background-color: var(--bg-color);
  }
}
</style>
