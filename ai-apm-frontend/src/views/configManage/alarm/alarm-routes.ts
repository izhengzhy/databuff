import type { Route } from 'vue-router';

export type AlarmTab = 'rule';

export const ALARM_TABS: AlarmTab[] = ['rule'];

/** Canonical list page; tabs are selected via {@code mType} query. */
export const ALARM_LIST_PATH = '/config/alarm';

/** Breadcrumb / deep-link aliases that map to the same tabbed list. */
export const ALARM_TAB_PATH: Record<AlarmTab, string> = {
  rule: '/config/rule',
};

export const ALARM_DETAIL_PATH = {
  ruleSetting: '/configManage/alarm/ruleSetting',
  responseSetting: '/configManage/alarm/responseSetting',
  systemRuleSetting: '/sysManage/ruleSetting',
} as const;

export const SYSTEM_RULE_LIST_PATH = '/sysManage/systemRule';

const DETAIL_QUERY_KEYS = ['mode', 'id'] as const;

const LIST_PATHS = new Set<string>([ALARM_LIST_PATH, ...Object.values(ALARM_TAB_PATH)]);

const DETAIL_PATHS = new Set<string>(Object.values(ALARM_DETAIL_PATH));

export function isAlarmListPath(path: string): boolean {
  return LIST_PATHS.has(path);
}

export function isAlarmDetailPath(path: string): boolean {
  return DETAIL_PATHS.has(path);
}

export function resolveAlarmTab(route: Pick<Route, 'path' | 'query'>): AlarmTab {
  const mType = String(route.query.mType || '');
  if (ALARM_TABS.includes(mType as AlarmTab)) {
    return mType as AlarmTab;
  }
  for (const [tab, path] of Object.entries(ALARM_TAB_PATH) as [AlarmTab, string][]) {
    if (route.path === path) {
      return tab;
    }
  }
  return 'rule';
}

export function stripAlarmDetailQuery(query: Record<string, unknown> = {}): Record<string, unknown> {
  const next: Record<string, unknown> = { ...query };
  DETAIL_QUERY_KEYS.forEach((key) => delete next[key]);
  delete next.type;
  return next;
}

export function buildAlarmListLocation(
  tab: AlarmTab,
  query: Record<string, unknown> = {},
): { path: string; query: Record<string, unknown> } {
  return {
    path: ALARM_LIST_PATH,
    query: {
      ...stripAlarmDetailQuery(query),
      mType: tab,
    },
  };
}

export function buildAlarmDetailLocation(
  path: string,
  query: Record<string, unknown> = {},
  detail?: { id?: string | number; mode?: 'e' | 'c' },
): { path: string; query: Record<string, unknown> } {
  const next: Record<string, unknown> = { ...query };
  delete next.__ps;
  if (detail?.id != null) {
    next.id = String(detail.id);
    next.mode = detail.mode || 'e';
  } else {
    delete next.id;
    delete next.mode;
  }
  return { path, query: next };
}
