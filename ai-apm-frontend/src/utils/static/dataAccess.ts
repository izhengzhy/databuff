import i18n from '@/i18n';
// 数据类型
export const DataTypeList = [
  { label: i18n.t('modules.utils.static.s_7e687515') as string, labelKey: 'modules.utils.static.s_7e687515', value: 'METRIC' },
  // { label: i18n.t('modules.utils.static.s_456d29ef') as string, labelKey: 'modules.utils.static.s_456d29ef', value: 'LOG' },
  { label: i18n.t('modules.utils.static.s_d3a1be9f') as string, labelKey: 'modules.utils.static.s_d3a1be9f', value: 'TRACE' },
  { label: i18n.t('modules.utils.static.s_10b2761d') as string, labelKey: 'modules.utils.static.s_10b2761d', value: 'EVENT' },
]

// 默认配置
export const DefaultConfig = {
  // 数据源
  webhookevent: {
    endpoint: '0.0.0.0:30088',
    path: '/webhook/event',
    required_header: [
      // { key: '', value: '', },
    ],
    health_path: '',
    read_timeout: '500ms',
  },
  skywalking: {
    protocols: {
      // http: {
      //   endpoint: '0.0.0.0:32767',
      //   path: '',
      // },
      grpc: {
        endpoint: '0.0.0.0:11800',
      },
    },
  },
  prometheusremotewrite: {
    endpoint: '0.0.0.0:30909',
    path: '/api/v1/write',
    cors: {
      allowed_origins: [
        // 'http://test.com', 'https://*.example.com',
      ],
      allowed_headers: [
        // 'Example-Header',
      ],
      max_age: 7200,
    },
  },
  otlp: {
    protocols: {
      http: {
        endpoint: '0.0.0.0:31318',
        cors: {
          allowed_origins: [
            // 'http://test.com', 'https://*.example.com',
          ],
          allowed_headers: [
            // 'Example-Header',
          ],
          max_age: 7200,
        }
      },
      // grpc: {
      //   endpoint: '0.0.0.0:31317',
      // },
    }
  },
  // 处理器
  insert: { actions: [{ action: 'insert', key: '', value: '', }], },
  update: { actions: [{ action: 'update', key: '', value: '', }], },
  upsert: { actions: [{ action: 'upsert', key: '', value: '', }], },
  delete: { actions: [{ action: 'delete', key: '', }], },
  extract: { actions: [{ action: 'extract', key: '', pattern: '', }], },
  hash: { actions: [{ action: 'hash', key: '', }], },
  convert: { actions: [{ action: 'convert', key: '', converted_type: 'double', }], }, // int double string
  metricsgeneration: { rules: [{
    name: '',
    unit: '',
    type: 'calculate', // calculate scale
    metric1: '',
    metric2: '',
    operation: '', // add subtract multiply divide
    // scale_by: 1,
  }]},
  metricnameremapping: {
    template_name: '',
    metric_name_mapping: {},
    metric_attribute_mapping: {},
  },
  batch: { send_batch_size: 8192, timeout: '1000ms', send_batch_max_size: 0, },
  filter: {
    error_mode: 'propagate',
    metrics: { metric: [], datapoint: [] },
    traces: { span: [] },
    logs: { log_record: [] },
  },
  transform: {
    error_mode: 'propagate',
    metric_statements: [],
    trace_statements: [],
    log_statements: [],
  },
}

// 数据源类型
export const SourceTypeList = [
  {
    label: 'Webhook',
    value: 'webhookevent',
    icon: 'db-icon-webhook',
    parentType: 'RECEIVERS',
    dataTypes: ['EVENT'], // , 'LOG'
    desc: i18n.t('modules.utils.static.s_e6c72512') as string, descKey: 'modules.utils.static.s_e6c72512',
    defaultConfig: DefaultConfig.webhookevent,
  },
  {
    label: 'Prometheus',
    value: 'prometheusremotewrite',
    icon: 'db-icon-prometheus',
    parentType: 'RECEIVERS',
    dataTypes: ['METRIC'],
    desc: i18n.t('modules.utils.static.s_d5cd1f69') as string, descKey: 'modules.utils.static.s_d5cd1f69',
    defaultConfig: DefaultConfig.prometheusremotewrite,
  },
  {
    label: 'SkyWalking',
    value: 'skywalking',
    icon: 'db-icon-skywalking',
    parentType: 'RECEIVERS',
    dataTypes: ['TRACE', 'METRIC'],
    desc: i18n.t('modules.utils.static.s_fa1534af') as string, descKey: 'modules.utils.static.s_fa1534af',
    defaultConfig: DefaultConfig.skywalking,
  },
  {
    label: 'OpenTelemetry',
    value: 'otlp',
    icon: 'db-icon-opentelemetry',
    parentType: 'RECEIVERS',
    dataTypes: ['TRACE', 'METRIC', 'EVENT'], // , 'LOG'
    desc: i18n.t('modules.utils.static.s_f85bfc12') as string, descKey: 'modules.utils.static.s_f85bfc12',
    defaultConfig: DefaultConfig.otlp,
  },
]

// 处理器类型
export const ProcessorTypeAndTitleList = [
  { label: i18n.t('modules.utils.static.s_24d67862') as string, labelKey: 'modules.utils.static.s_24d67862', value: 'title-attributes', type: 'title' },
  { label: i18n.t('modules.utils.static.s_9bdb07e7') as string, labelKey: 'modules.utils.static.s_9bdb07e7', value: 'insert', icon: 'db-icon-add2', parentType: 'PROCESSORS',
    dataTypes: ['TRACE', 'METRIC', 'EVENT'], // , 'LOG'
    defaultConfig: DefaultConfig.insert, },
  { label: i18n.t('modules.utils.static.s_32ac152b') as string, labelKey: 'modules.utils.static.s_32ac152b', value: 'update', icon: 'db-icon-update', parentType: 'PROCESSORS',
    dataTypes: ['TRACE', 'METRIC', 'EVENT'], // , 'LOG'
    defaultConfig: DefaultConfig.update, },
  { label: i18n.t('modules.utils.static.s_d1a3aa85') as string, labelKey: 'modules.utils.static.s_d1a3aa85', value: 'upsert', icon: 'db-icon-merge', parentType: 'PROCESSORS',
    dataTypes: ['TRACE', 'METRIC', 'EVENT'], // , 'LOG'
    defaultConfig: DefaultConfig.upsert, },
  { label: i18n.t('modules.components.matching-criteria.s_2f4aaddd') as string, labelKey: 'modules.components.matching-criteria.s_2f4aaddd', value: 'delete', icon: 'db-icon-delete2', parentType: 'PROCESSORS',
    dataTypes: ['TRACE', 'METRIC', 'EVENT'], // , 'LOG'
    defaultConfig: DefaultConfig.delete, },
  { label: i18n.t('modules.utils.static.s_fc16b1f0') as string, labelKey: 'modules.utils.static.s_fc16b1f0', value: 'extract', icon: 'db-icon-extract', parentType: 'PROCESSORS',
    dataTypes: ['TRACE', 'METRIC', 'EVENT'], // , 'LOG'
    defaultConfig: DefaultConfig.extract, },
  { label: i18n.t('modules.utils.static.s_b5dbd2c8') as string, labelKey: 'modules.utils.static.s_b5dbd2c8', value: 'hash', icon: 'db-icon-hash', parentType: 'PROCESSORS',
    dataTypes: ['TRACE', 'METRIC', 'EVENT'], // , 'LOG'
    defaultConfig: DefaultConfig.hash, },
  { label: i18n.t('modules.utils.static.s_f3c723ec') as string, labelKey: 'modules.utils.static.s_f3c723ec', value: 'convert', icon: 'db-icon-convert', parentType: 'PROCESSORS',
    dataTypes: ['TRACE', 'METRIC', 'EVENT'], // , 'LOG'
    defaultConfig: DefaultConfig.convert, },

  { label: i18n.t('modules.utils.static.s_bb8472c4') as string, labelKey: 'modules.utils.static.s_bb8472c4', value: 'title-filter', type: 'title' },
  { label: i18n.t('modules.utils.static.s_bb8472c4') as string, labelKey: 'modules.utils.static.s_bb8472c4', value: 'filter', icon: 'db-icon-filter', parentType: 'PROCESSORS',
    dataTypes: ['TRACE', 'METRIC', 'EVENT'], // , 'LOG'
    defaultConfig: DefaultConfig.filter, },

  { label: i18n.t('modules.utils.static.s_0a6a53f8') as string, labelKey: 'modules.utils.static.s_0a6a53f8', value: 'title-metrics', type: 'title' },
  { label: i18n.t('modules.utils.static.s_0a6a53f8') as string, labelKey: 'modules.utils.static.s_0a6a53f8', value: 'metricsgeneration', icon: 'db-icon-metric-exp', parentType: 'PROCESSORS',
    dataTypes: ['METRIC'],
    defaultConfig: DefaultConfig.metricsgeneration, },

  { label: i18n.t('modules.utils.static.s_ea20dffc') as string, labelKey: 'modules.utils.static.s_ea20dffc', value: 'title-translate', type: 'title' },
  { label: i18n.t('modules.utils.static.s_ea20dffc') as string, labelKey: 'modules.utils.static.s_ea20dffc', value: 'transform', icon: 'db-icon-translate', parentType: 'PROCESSORS',
    dataTypes: ['TRACE', 'METRIC', 'EVENT'], // , 'LOG'
    defaultConfig: DefaultConfig.transform, },

  { label: i18n.t('modules.utils.static.s_9da18849') as string, labelKey: 'modules.utils.static.s_9da18849', value: 'title-metricnameremapping', type: 'title' },
  { label: i18n.t('modules.utils.static.s_9da18849') as string, labelKey: 'modules.utils.static.s_9da18849', value: 'metricnameremapping', icon: 'db-icon-remapping', parentType: 'PROCESSORS',
    dataTypes: ['METRIC'],
    defaultConfig: DefaultConfig.metricnameremapping, },

  { label: i18n.t('modules.utils.static.s_dfac151d') as string, labelKey: 'modules.utils.static.s_dfac151d', value: 'title-advanced', type: 'title' },
  { label: i18n.t('modules.utils.static.s_c1766c37') as string, labelKey: 'modules.utils.static.s_c1766c37', value: 'batch', icon: 'db-icon-batch', parentType: 'PROCESSORS',
    dataTypes: ['TRACE', 'METRIC', 'EVENT'], // , 'LOG'
    defaultConfig: DefaultConfig.batch, },
]
export const ProcessorTypeList = ProcessorTypeAndTitleList.filter((item) => item.type !== 'title')

// 终端类型
export const TerminalTypeList = [
  {
    label: 'Databuff',
    value: 'databuffhttp',
    icon: 'db-icon-databuff',
    parentType: 'EXPORTERS',
    dataTypes: ['TRACE', 'METRIC', 'EVENT'], // , 'LOG'
    desc: i18n.t('modules.utils.static.s_46bca928') as string, descKey: 'modules.utils.static.s_46bca928',
  },
  {
    label: 'Kafka',
    value: 'kafka',
    icon: 'db-icon-kafka',
    parentType: 'EXPORTERS',
    dataTypes: ['TRACE', 'METRIC', 'EVENT'], // , 'LOG'
    desc: '',
  },
]
