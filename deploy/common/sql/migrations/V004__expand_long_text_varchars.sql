-- Expand VARCHAR lengths for URL / SQL / JSON / resource fields that overflow in production
-- and fail Stream Load / JDBC writes (GitHub #38 and related tables).
-- Also folds in GitHub #39: log_dc_record.body VARCHAR(65533) -> STRING for CJK-heavy bodies.
-- Also seeds built-in entry-overview detection rules (avgDuration >1s, error.pct >10%, enabled).
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
-- Fresh installs get the same rows from databuff.sql (id 1/2).
-- High ids avoid colliding with user-created rules on existing DBs.
INSERT INTO config_event_rule
  (id, rule_name, classify, detection_way, service, metric, threshold, comparator, enabled, query_json, updated_at)
VALUES
  (900001, '服务入口平均耗时过高', 'singleMetric', 'threshold', '*', 'service.avgDuration', 1000, 'gt', 1,
   '{"1":{"way":"threshold","period":60,"unit":"ms","view_unit":"ms","_scale":1,"time_aggregator":"avg","comparison":">","thresholds":{"critical":1000,"warning":null},"A":{"metric":"service.avgDuration","aggs":"avg","by":["service"],"from":[]}}}',
   NOW()),
  (900002, '服务入口错误率过高', 'singleMetric', 'threshold', '*', 'service.error.pct', 10, 'gt', 1,
   '{"1":{"way":"threshold","period":60,"unit":"%","view_unit":"%","_scale":1,"time_aggregator":"avg","comparison":">","thresholds":{"critical":10,"warning":null},"A":{"metric":"service.error.pct","aggs":"avg","by":["service"],"from":[]}}}',
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
  ('1',        '看得见',   '自然语言问系统',    'data',
   '["查询最近1小时的服务列表","查询service-b服务的上下游拓扑","查询每个服务最近1小时的请求量趋势图","查询每个服务最近1小时的异常量趋势图"]',
   1, 1, 1, NOW(), NOW(),
   '看得见', '自然语言问系统', 'data',
   '["查询最近1小时的服务列表","查询service-b服务的上下游拓扑","查询每个服务最近1小时的请求量趋势图","查询每个服务最近1小时的异常量趋势图"]'),
  ('2',        '军团协同',  '多 Agent 协同',    'brain',
   '["最近1小时整个集群有没有异常？请联合智能问数和智能巡检一起做综合诊断：智能问数查各服务延迟和错误率并追慢 Trace，智能巡检做分级健康检查，最后汇总成一份可转发给团队的故障报告。","帮我做一次全集群健康巡检：智能问数负责拉每个服务的 P99 延迟和错误率，智能巡检负责 JVM、线程、GC 分级检查，最后合成一份可转发给 SRE 群的报告。","最近 30 分钟有没有慢调用？让智能问数追慢 Trace 根因，智能巡检同步检查相关服务实例健康度，汇总成一份联合诊断结论。","对 service-a 做一次军团级会诊：智能问数查它的下游依赖和出口耗时，智能巡检查它自身实例的 JVM 和线程状态，最后给我一份可转发给开发团队的报告。"]',
   1, 1, 1, NOW(), NOW(),
   '军团协同', '多 Agent 协同', 'brain',
   '["最近1小时整个集群有没有异常？请联合智能问数和智能巡检一起做综合诊断：智能问数查各服务延迟和错误率并追慢 Trace，智能巡检做分级健康检查，最后汇总成一份可转发给团队的故障报告。","帮我做一次全集群健康巡检：智能问数负责拉每个服务的 P99 延迟和错误率，智能巡检负责 JVM、线程、GC 分级检查，最后合成一份可转发给 SRE 群的报告。","最近 30 分钟有没有慢调用？让智能问数追慢 Trace 根因，智能巡检同步检查相关服务实例健康度，汇总成一份联合诊断结论。","对 service-a 做一次军团级会诊：智能问数查它的下游依赖和出口耗时，智能巡检查它自身实例的 JVM 和线程状态，最后给我一份可转发给开发团队的报告。"]'),
  ('3',        '会巡检',   '服务巡检 + 报告',   'inspection',
   '["对 service-b 做一次巡检，输出完整的 HTML 巡检报告。","对 service-a 做一次巡检，输出完整的 HTML 巡检报告。","对 MySQL 数据库 demo_apm 做一次巡检，输出完整的 HTML 巡检报告。","对 Redis 缓存做一次巡检，输出完整的 HTML 巡检报告。"]',
   1, 1, 1, NOW(), NOW(),
   '会巡检', '服务巡检 + 报告', 'inspection',
   '["对 service-b 做一次巡检，输出完整的 HTML 巡检报告。","对 service-a 做一次巡检，输出完整的 HTML 巡检报告。","对 MySQL 数据库 demo_apm 做一次巡检，输出完整的 HTML 巡检报告。","对 Redis 缓存做一次巡检，输出完整的 HTML 巡检报告。"]'),
  ('4',        '会诊断',   '根因分析',         'data',
   '["service-a 最近 1 小时瓶颈在哪里？根因在应用、数据库还是下游？","service-b 最近 1 小时错误率升高，根因是什么？给出证据链。","最近 1 小时 P99 延迟突然变高的服务是哪个？根因在哪一层？","service-a 调用 service-b 的 HTTP 接口耗时 100ms 偏高，帮我定位是网络、应用还是下游数据库的问题。"]',
   1, 1, 1, NOW(), NOW(),
   '会诊断', '根因分析', 'data',
   '["service-a 最近 1 小时瓶颈在哪里？根因在应用、数据库还是下游？","service-b 最近 1 小时错误率升高，根因是什么？给出证据链。","最近 1 小时 P99 延迟突然变高的服务是哪个？根因在哪一层？","service-a 调用 service-b 的 HTTP 接口耗时 100ms 偏高，帮我定位是网络、应用还是下游数据库的问题。"]'),
  ('5',        '会修',     '运维专家自动解决',  'ops',
   '["容器 ai-apm-demo 一直重启，帮我 SSH 上去排查并修好。","这台机器磁盘使用率超过 90%，帮我看看是哪些日志或容器占的，能清的清掉。","帮我检查这台机器的内存使用情况，找出占用最高的进程，如果是异常进程帮我处理掉。","DataBuff 的 ai-apm-web 容器起不来，帮我 SSH 上去 docker logs 看一下，是配置问题就帮我改好。"]',
   1, 1, 1, NOW(), NOW(),
   '会修', '运维专家自动解决', 'ops',
   '["容器 ai-apm-demo 一直重启，帮我 SSH 上去排查并修好。","这台机器磁盘使用率超过 90%，帮我看看是哪些日志或容器占的，能清的清掉。","帮我检查这台机器的内存使用情况，找出占用最高的进程，如果是异常进程帮我处理掉。","DataBuff 的 ai-apm-web 容器起不来，帮我 SSH 上去 docker logs 看一下，是配置问题就帮我改好。"]'),
  ('6',        '会预测',   '容量预测',         'data',
   '["Redis 平均耗时 366ms 偏高，帮我判断是不是有容量瓶颈、要不要扩容，给容量规划建议。","MySQL demo_apm 最近 1 小时的 QPS 和连接数趋势怎么样？按当前增长速度，连接池还能撑多久？","service-a 的请求量最近 7 天的增长趋势如何？按这个速度，下周需要扩容吗？","Kafka 的消费延迟在持续增长，帮我判断是消费者处理能力不足还是生产端流量异常，给容量规划建议。"]',
   1, 1, 1, NOW(), NOW(),
   '会预测', '容量预测', 'data',
   '["Redis 平均耗时 366ms 偏高，帮我判断是不是有容量瓶颈、要不要扩容，给容量规划建议。","MySQL demo_apm 最近 1 小时的 QPS 和连接数趋势怎么样？按当前增长速度，连接池还能撑多久？","service-a 的请求量最近 7 天的增长趋势如何？按这个速度，下周需要扩容吗？","Kafka 的消费延迟在持续增长，帮我判断是消费者处理能力不足还是生产端流量异常，给容量规划建议。"]'),
  ('7',        '会答疑',   '答疑专家',         'qa',
   '["OpenTelemetry SDK 怎么接入？告警阈值在哪配？给操作路径。","DataBuff 怎么用一条命令部署起来？需要哪些前置依赖？","DataBuff 支持哪些语言的 SDK 接入？分别给一个最小可跑的接入示例。","我想给 service-a 的 P99 延迟配一个告警，超过 500ms 持续 3 分钟就触发，怎么配？给操作路径。"]',
   1, 1, 1, NOW(), NOW(),
   '会答疑', '答疑专家', 'qa',
   '["OpenTelemetry SDK 怎么接入？告警阈值在哪配？给操作路径。","DataBuff 怎么用一条命令部署起来？需要哪些前置依赖？","DataBuff 支持哪些语言的 SDK 接入？分别给一个最小可跑的接入示例。","我想给 service-a 的 P99 延迟配一个告警，超过 500ms 持续 3 分钟就触发，怎么配？给操作路径。"]');

