-- Expand VARCHAR lengths for URL / SQL / JSON / resource fields that overflow in production
-- and fail Stream Load / JDBC writes (GitHub #38 and related tables).
-- Also folds in GitHub #39: log_dc_record.body VARCHAR(65533) -> STRING for CJK-heavy bodies.
-- Also seeds built-in entry-overview detection rules (avgDuration >1s, error.pct >10%, exception.cnt >10, enabled).
-- Fresh installs take the new sizes (and STRING body) from databuff.sql; this migration upgrades existing DBs.

USE databuff;

-- 1. Spans: URL and resource often exceed 500
ALTER TABLE trace_dc_span MODIFY COLUMN `resource` VARCHAR(4096) NOT NULL;
ALTER TABLE trace_dc_span MODIFY COLUMN `meta.http.url` VARCHAR(4096);

-- 2. Logs: attribute / resource JSON blobs
ALTER TABLE log_dc_record MODIFY COLUMN `attributes_json` VARCHAR(15000) COMMENT 'LogRecord attributes JSON';
ALTER TABLE log_dc_record MODIFY COLUMN `resource_json` VARCHAR(15000) COMMENT 'Resource attributes JSON';

-- 3. Issue #38 core metric tables
ALTER TABLE metric_service_db MODIFY COLUMN `resource` VARCHAR(1024);
ALTER TABLE metric_service_db MODIFY COLUMN `rootResource` VARCHAR(1024);
ALTER TABLE metric_service_db MODIFY COLUMN `sqlContent` VARCHAR(1024);

ALTER TABLE metric_service_http MODIFY COLUMN `resource` VARCHAR(1024);
ALTER TABLE metric_service_http MODIFY COLUMN `rootResource` VARCHAR(1024);
ALTER TABLE metric_service_http MODIFY COLUMN `url` VARCHAR(4096);

ALTER TABLE metric_service_db_connection_pool MODIFY COLUMN `connectionPoolUrl` VARCHAR(4096);

-- 4. Same-pattern resource/rootResource (and related long tags) on other metric tables
ALTER TABLE metric_service_config MODIFY COLUMN `resource` VARCHAR(1024);
ALTER TABLE metric_service_config MODIFY COLUMN `rootResource` VARCHAR(1024);

ALTER TABLE metric_service_exception MODIFY COLUMN `resource` VARCHAR(1024);
ALTER TABLE metric_service_exception MODIFY COLUMN `rootResource` VARCHAR(1024);

ALTER TABLE metric_service_flow MODIFY COLUMN `parentResource` VARCHAR(1024);
ALTER TABLE metric_service_flow MODIFY COLUMN `resource` VARCHAR(1024);

ALTER TABLE metric_service_mq MODIFY COLUMN `resource` VARCHAR(1024);
ALTER TABLE metric_service_mq MODIFY COLUMN `rootResource` VARCHAR(1024);
ALTER TABLE metric_service_mq MODIFY COLUMN `topic` VARCHAR(1024);

ALTER TABLE metric_service_redis MODIFY COLUMN `command` VARCHAR(1024);
ALTER TABLE metric_service_redis MODIFY COLUMN `resource` VARCHAR(1024);
ALTER TABLE metric_service_redis MODIFY COLUMN `rootResource` VARCHAR(1024);

ALTER TABLE metric_service_rpc MODIFY COLUMN `resource` VARCHAR(1024);
ALTER TABLE metric_service_rpc MODIFY COLUMN `rootResource` VARCHAR(1024);

ALTER TABLE metric_service_remote MODIFY COLUMN `resource` VARCHAR(1024);
ALTER TABLE metric_service_remote MODIFY COLUMN `rootResource` VARCHAR(1024);

ALTER TABLE metric_service_thread_pool_cost MODIFY COLUMN `rootResource` VARCHAR(1024);

ALTER TABLE metric_service_trace MODIFY COLUMN `resource` VARCHAR(1024);

-- 5. GitHub #39: log body can exceed VARCHAR(65533) UTF-8 bytes (esp. CJK).
-- STRING default soft limit is ~1MB (BE string_type_length_soft_limit_bytes).
-- Heavyweight schema change: rewrites tablets; monitor with SHOW ALTER TABLE COLUMN.
-- Fresh installs take STRING from databuff.sql; this upgrades existing DBs.
ALTER TABLE log_dc_record MODIFY COLUMN `body` STRING COMMENT 'log message text (STRING; ingest truncates by Java String.length)';

-- 6. Built-in detection rules (all services entry overview, enabled by default).
-- Fresh installs get the same rows from databuff.sql (id 1/2/3).
-- High ids avoid colliding with user-created rules on existing DBs.
INSERT INTO config_event_rule
  (id, rule_name, classify, detection_way, service, metric, threshold, comparator, enabled, query_json, updated_at)
VALUES
  (900001, '服务入口平均耗时过高', 'singleMetric', 'threshold', '*', 'service.avgDuration', 1000, 'gt', 1,
   '{"1":{"way":"threshold","period":60,"unit":"ms","view_unit":"ms","_scale":1,"time_aggregator":"avg","comparison":">","thresholds":{"critical":1000,"warning":null},"A":{"metric":"service.avgDuration","aggs":"avg","by":["service"],"from":[]}}}',
   NOW()),
  (900002, '服务入口错误率过高', 'singleMetric', 'threshold', '*', 'service.error.pct', 10, 'gt', 1,
   '{"1":{"way":"threshold","period":60,"unit":"%","view_unit":"%","_scale":1,"time_aggregator":"avg","comparison":">","thresholds":{"critical":10,"warning":null},"A":{"metric":"service.error.pct","aggs":"avg","by":["service"],"from":[]}}}',
   NOW()),
  (900003, '服务异常数过高', 'singleMetric', 'threshold', '*', 'service.exception', 10, 'gt', 1,
   '{"1":{"way":"threshold","period":60,"unit":"","view_unit":"","_scale":1,"time_aggregator":"sum","comparison":">","thresholds":{"critical":10,"warning":null},"A":{"metric":"service.exception.cnt","aggs":"sum","by":["service"],"from":[]}}}',
   NOW());

-- 7. AI 平台首页 7 个 AI 能力配置表 + 种子数据。
-- 全新安装从 databuff.sql 取同一定义；本迁移给已存在的库补建表并灌种子。
-- default_* 列保存"恢复默认"用的原始值，编辑只改当前列。
CREATE TABLE IF NOT EXISTS config_ai_capability (
  `capability_id`         VARCHAR(64)  NOT NULL,
  `name`                  VARCHAR(256) NOT NULL,
  `tagline`               STRING,
  `expert_id`             VARCHAR(128) NOT NULL,
  `prompts_json`          STRING,
  `enabled`               TINYINT      NOT NULL DEFAULT "1",
  `built_in`              TINYINT      NOT NULL DEFAULT "1",
  `version`               BIGINT       NOT NULL DEFAULT "1",
  `created_at`            DATETIME     NOT NULL,
  `updated_at`            DATETIME     NOT NULL,
  `default_name`          VARCHAR(256) NOT NULL,
  `default_tagline`       STRING,
  `default_expert_id`     VARCHAR(128) NOT NULL,
  `default_prompts_json`  STRING
) ENGINE=OLAP
UNIQUE KEY(`capability_id`)
DISTRIBUTED BY HASH(`capability_id`) BUCKETS 1
PROPERTIES ("replication_num" = "1");

INSERT INTO config_ai_capability
  (capability_id, name, tagline, expert_id, prompts_json, enabled, built_in, version, created_at, updated_at,
   default_name, default_tagline, default_expert_id, default_prompts_json)
VALUES
  ('1',        '看得见',   '自然语言问系统',    '',
   '["查询最近1小时的服务列表","查询service-b服务的上下游拓扑","查询每个服务最近1小时的请求量趋势图","查询每个服务最近1小时的异常量趋势图"]',
   1, 1, 1, NOW(), NOW(),
   '看得见', '自然语言问系统', '',
   '["查询最近1小时的服务列表","查询service-b服务的上下游拓扑","查询每个服务最近1小时的请求量趋势图","查询每个服务最近1小时的异常量趋势图"]'),
  ('2',        '军团协同',  '多 Agent 协同',    '',
   '["找出最近 1 小时平均响应时间最高的服务，对它做一次巡检，并生成巡检报告","找出最近 1 小时平均响应时间最高的服务，对它进行故障定位","找出最近 1 小时日志ERROR最多的服务，对它做一次巡检，并生成巡检报告","找出最近 1 小时日志ERROR最多的服务，对它进行故障定位"]',
   1, 1, 1, NOW(), NOW(),
   '军团协同', '多 Agent 协同', '',
   '["找出最近 1 小时平均响应时间最高的服务，对它做一次巡检，并生成巡检报告","找出最近 1 小时平均响应时间最高的服务，对它进行故障定位","找出最近 1 小时日志ERROR最多的服务，对它做一次巡检，并生成巡检报告","找出最近 1 小时日志ERROR最多的服务，对它进行故障定位"]'),
  ('3',        '会巡检',   '服务巡检 + 报告',   'inspection',
   '["对service-a进行巡检","对service-b进行巡检","对所有服务进行巡检，找出健康度最差的几个汇总生成报告","对service-a以及上下游服务都进行巡检"]',
   1, 1, 1, NOW(), NOW(),
   '会巡检', '服务巡检 + 报告', 'inspection',
   '["对service-a进行巡检","对service-b进行巡检","对所有服务进行巡检，找出健康度最差的几个汇总生成报告","对service-a以及上下游服务都进行巡检"]'),
  ('4',        '会诊断',   '根因分析',         '',
   '["service-a 最近 1 小时瓶颈在哪里","service-b 最近 1 小时瓶颈在哪里","给我定位下最近5min内的服务报错的根因","给我定位下最近5min内的告警的根因"]',
   1, 1, 1, NOW(), NOW(),
   '会诊断', '根因分析', '',
   '["service-a 最近 1 小时瓶颈在哪里","service-b 最近 1 小时瓶颈在哪里","给我定位下最近5min内的服务报错的根因","给我定位下最近5min内的告警的根因"]'),
  ('5',        '会修复',   '运维专家自动解决',  'ops',
   '["列出当前所有的进程","找出当前内存占用高的进程","对当前服务打下火焰图，并输出火焰图html","容器 ai-apm-demo 一直重启，帮我 SSH 上去排查并修好"]',
   1, 1, 1, NOW(), NOW(),
   '会修复', '运维专家自动解决', 'ops',
   '["列出当前所有的进程","找出当前内存占用高的进程","对当前服务打下火焰图，并输出火焰图html","容器 ai-apm-demo 一直重启，帮我 SSH 上去排查并修好"]'),
  ('6',        '会预测',   '容量预测',         '',
   '["预测下service-a未来一周的请求量趋势图","预测下service-b未来一周的请求量趋势图","预测下mysql未来一周的请求量趋势图","预测下redis未来一周的请求量趋势图"]',
   1, 1, 1, NOW(), NOW(),
   '会预测', '容量预测', '',
   '["预测下service-a未来一周的请求量趋势图","预测下service-b未来一周的请求量趋势图","预测下mysql未来一周的请求量趋势图","预测下redis未来一周的请求量趋势图"]'),
  ('7',        '会答疑',   '答疑专家',         'qa',
   '["DataBuff 怎么用一条命令部署起来？需要哪些前置依赖？","Databuff 的整体架构是什么样的？","如何配置告警？","如何修改 DataBuff 7 大 AI 能力中的推荐？"]',
   1, 1, 1, NOW(), NOW(),
   '会答疑', '答疑专家', 'qa',
   '["DataBuff 怎么用一条命令部署起来？需要哪些前置依赖？","Databuff 的整体架构是什么样的？","如何配置告警？","如何修改 DataBuff 7 大 AI 能力中的推荐？"]');

