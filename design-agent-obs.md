# Agent 观测方案设计

## 1. 现状

### 采集端（~60% 就绪）

| 组件 | 状态 | 源码位置 |
|------|------|----------|
| `AgentScopeSessionHook` | ✔ 已实现 | `ai-apm-web/src/main/java/com/databuff/apm/web/ai/platform/runtime/AgentScopeSessionHook.java` |
| `AgentScopeSessionHook.TraceRecorder` | ✔ ToolCall/Result/Reasoning 事件采集 | 同上:72 |
| `AgentScopeToolResultTracer` | ✔ 工具结果捕获 | `ai-apm-web/src/main/java/com/databuff/apm/web/ai/platform/runtime/AgentScopeToolResultTracer.java` |
| `AiSessionStore.appendTraceMessage` | ✔ 消息持久化入口 | `ai-apm-web/src/main/java/com/databuff/apm/web/ai/agent/AiSessionStore.java:524` |
| `AiSessionPersistence.toMessageRow` | ✔ Doris 写入映射 | `ai-apm-web/src/main/java/com/databuff/apm/web/persistence/AiSessionPersistence.java:186` |
| `AiMessagePersistenceQueue` | ✔ 异步写入队列 | `ai-apm-web/src/main/java/com/databuff/apm/web/persistence/AiMessagePersistenceQueue.java` |
| `config_ai_message` DDL | ✔ 基础字段已存在 | `deploy/common/sql/databuff.sql:1384` |
| Token/Model/Cost 采集 | ✗ 缺失 | — |
| `PostReasoningEvent` 增强 | ✗ 未实现 | — |

当前 `config_ai_message` 表已有字段覆盖 session、message、type、status、metadata，但缺少 Token 消耗、模型标识、Cost 等 AI 观测核心维度。

### API 端（0%）

后端无 `/aiMonitor/*` 路由映射。现有 `/api/v1/ai/*` 端点服务于 AI 平台对话场景，不支持观测类聚合查询：

- `AgentController.java:55-65` — `/sessions` / `/sessions/{id}/messages` 仅返回内存态 session
- `ApmConfigRepository.java:261-284` — `loadRecentAiSessions()` 按 Doris 聚合 session 摘要
- `ApmConfigRepository.java:292-317` — `loadAiMessages()` / `loadAiMessagesAfter()` 按 sessionId 拉消息

**缺失能力**：按 tool 维度聚合、按 model 维度聚合、Token 统计数据、全局 session 列表分页/搜索。

### UI 端（0% — 8 个 ComingSoon）

`ai-apm-frontend/src/views/aiMonitor/` 下 8 个子路由全部使用 `<coming-soon />` 占位：

| 路由 | 对应文件 |
|------|----------|
| `/aiMonitor/applications` | `applications/index.vue` |
| `/aiMonitor/topology` | `topology/index.vue` |
| `/aiMonitor/skillCalls` | `skillCalls/index.vue` |
| `/aiMonitor/toolCalls` | `toolCalls/index.vue` |
| `/aiMonitor/modelCalls` | `modelCalls/index.vue` |
| `/aiMonitor/sessions` | `sessions/index.vue` |
| `/aiMonitor/tokens` | `tokens/index.vue` |
| `/aiMonitor/errors` | `errors/index.vue` |

菜单定义在 `OpenSourceMenuCatalog.java:37-45` / `route-data.ts:472-578`，均为 `leaf=true` / `isMenu=true`。

### 商业版 aiMonitor

内置了 `aiMonitor` 模块但无独立前端页面（商业版依赖 `@databuff/apm-monitor` 等私有包），当前开源版本需自主实现。

---

## 2. MVP 范围：Dogfood 自观测

### 2.1 采集增强

#### 2.1.1 `config_ai_message` DDL 增补字段

```sql
-- 在现有 config_ai_message 表基础上增补（ALTER TABLE）
ALTER TABLE config_ai_message ADD COLUMN (
  `input_tokens`      INT       DEFAULT NULL,
  `output_tokens`     INT       DEFAULT NULL,
  `total_tokens`      INT       DEFAULT NULL,
  `model_name`        VARCHAR(128) DEFAULT NULL,  -- 补充：当前字段已存在但未写入
  `model_provider`    VARCHAR(64)  DEFAULT NULL,
  `cost_usd`          DOUBLE       DEFAULT NULL,
  `token_metadata_json` STRING     DEFAULT NULL     -- 保留扩展：reasoning_tokens 等
);
```

**说明**：`model_name` 列已在 `deploy/common/sql/databuff.sql:1396` 定义，当前采集链路未填充。MVＰ 只需补全采集代码即可复用，无需 ALTER。`model_provider` 和 `cost_usd` 为新列。

#### 2.1.2 `AgentScopeSessionHook` 增强

在 `AgentScopeSessionHook.java` 的 `persistTrace()` 路径中（`:255-273`），metadata 目前包含 `sessionId`/`userName`/`expertId`/`taskId`/`assistantMessageId`。新增：

- **modelName**：从 `AgentRuntimeConfig` 或 `ExpertChatContext` 的运行时配置读取（`System.getProperty` 或 context 参数传递），写入 metadata `modelName` / `modelProvider`
- **Token 计数**：采集端无 LLM 调用 Token 信息。MVP 方案：在 `AgentScopeChatService`（LLM 调用路径）中拦截 `PostReasoningEvent`，解析 `usage` 字段后追加到当前 session 的 metadata。
- **工具调用 duration**：已在 `AgentScopeSessionHook.java:231-238` 部分实现，需验证 `captureToolResult` 覆盖所有分支。

#### 2.1.3 `PostReasoningEvent` 采集

当前 `AiChatOrchestrator` 使用 AgentScope 的 Agent 模式。AgentScope 的 `AgentScopeAgent` 调用 LLM 后异步发布事件。

**具体位置**：
- `AiChatOrchestrator.java` 中 `agentRunner.run()` → `ExpertAgentRunner` → LLM 调用
- `agentRunner` 通过 ExpertRuntimeListener 订阅事件，当前仅处理 stream 事件
- 新增 `PostReasoningEvent` 处理器，提取 `usage` 对象 → 写入 `AiSessionStore.updateMessage()` 的 metadata

#### 2.1.4 时序关系

```
用户输入 → AgentScopeSessionHook.TraceRecorder.record()
           ├── THINKING_BLOCK_DELTA → persistStreamingTrace(REASONING)
           ├── TEXT_BLOCK_DELTA     → persistStreamingTrace(TEXT)
           ├── TOOL_CALL_START/END  → persistTrace(TOOL_CALL)
           ├── TOOL_RESULT_START/END → persistTrace(TOOL_RESULT)
           └── (新增) LLM 完成后 → PostReasoningEvent → updateMessage(token_metadata)
```

### 2.2 API 契约：`/api/v1/aiMonitor/*`

新增 `AiMonitorController.java`，路由前缀 `/api/v1/aiMonitor`。

#### 端点清单

##### 对话追踪（MVP 优先）

| 方法 | 路径 | 说明 | 参考现有关键路径 |
|------|------|------|-----------------|
| `GET` | `/sessions` | 分页 session 列表（聚合摘要） | `ApmConfigRepository.loadRecentAiSessions()` |
| `GET` | `/sessions/{sessionId}` | session 详情（元信息 + 消息预览） | `AiSessionPersistence.pollMergedMessages()` |
| `GET` | `/sessions/{sessionId}/messages` | 全量消息列表（按 round/message 排序） | `ApmConfigRepository.loadAiMessages()` |

##### 工具调用

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/tools/calls` | 工具调用列表（聚合：调用次数、成功/失败、平均耗时） |
| `GET` | `/tools/calls/{sessionId}` | 指定 session 的工具调用详情 |

SQL 能力依赖：当前 `config_ai_message` 的 `tool_name` 和 `call_id` 字段已存在（`AiMessageRow` record 包含 `toolName`/`callId`），查询按 `message_type = 'TOOL_CALL'` 过滤即可。

##### Model 调用（后置）

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/models/calls` | 模型调用列表（聚合：模型名称、调用次数、Token 消耗总量） |

##### Token / Cost（后置）

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/tokens/summary` | Token 消耗汇总（input/output/total + 时间趋势） |
| `GET` | `/tokens/by-model` | 按模型分组的 Token 统计 |

#### `AiMessageRow` 现有字段复用

`ApmConfigRepository.java:1035-1056` 的 `AiMessageRow` record 已有：

```
sessionId, messageId, sessionType, userId, userName, agent, agentType,
roundIndex, messageIndex, messageType, messageStatus,
modelName,       // 当前未填充
callId,          // TOOL_CALL 的消息有此字段
toolName,        // TOOL_CALL/TOOL_RESULT 消息有此字段
content, attachmentsJson, error, metadataJson, triggerSource,
createdAt, updatedAt
```

MVP API 可直接基于这些字段聚合，无需等 DDL 变更。

#### 数据流

```
浏览器 → /api/v1/aiMonitor/sessions
         → AiMonitorController.java
         → ApmConfigRepository (Doris 直查)
         ← JSON 响应
```

不经过 `AiSessionStore` 内存态，直接从 Doris 查询，保证数据一致性。

### 2.3 UI MVP 页范围

#### 第一阶段：对话追踪（Sessions）

**页面**：`/aiMonitor/sessions`

原型参考 `ai-apm-frontend/src/views/aiPlatform/chat/` 中的消息列表组件风格。

**包含**：
- Session 列表表格：sessionId（截断显示）、标题（title）、用户、Expert、消息数、最后更新时间
- 点击 row 展开/跳转详情 → 消息时间线视图（按 round 分组）
- 时间范围选择器（复用 `time: true` / `refresh: true` 配置）
- 分页

**复用**：`db-table` 组件、`metric-select` 时间选择器、`query-filter` 区域

#### 第二阶段：工具调用

**页面**：`/aiMonitor/toolCalls`

- 工具调用聚合列表：工具名、调用次数、成功/失败、平均耗时
- 点击 row → 按 session 展开详情
- 时间范围选择 + 搜索

#### 第三阶段（后置）：Token / Model / Cost

**页面**：`/aiMonitor/tokens`、`/aiMonitor/modelCalls`

- Token 趋势图
- 按模型分组调用统计
- Cost 估算（需先完成 DDL 增补 + 采集）

### 2.4 不做清单

| 能力 | 原因 |
|------|------|
| 外部 OTLP GenAI 接入 | 非 Dogfood 需求；LMNR 调研建议 L3 |
| Evals (Langfuse 式) | 需评估框架，不在 KR 范围 |
| LLM 网关 / 流量管理 | 独立功能模块 |
| Agent 拓扑 / 技能调用页 | 复杂度过高，且无数据基础 |
| 错误分析 | 当前 AI message error 字段未系统化填充 |
| Agent 列表 / 应用发现 | 无 agent 注册机制 |

---

## 3. 依赖说明

### 与 v0.2.0 OTel Logs 的关系

**无直接依赖**。Agent 观测管道独立于 OTel Logs：

| 维度 | v0.2.0 OTel Logs (log_dc_record) | Agent 观测 (config_ai_message) |
|------|-----------------------------------|-------------------------------|
| 存储 | `log_dc_record` (Doris) | `config_ai_message` (Doris) |
| 写入管道 | OTLP gRPC 接入 | `AiMessagePersistenceQueue` Java 内部 |
| 查询 API | `/log/search` | `/api/v1/aiMonitor/*` |
| 数据格式 | OTel LogRecord | ai_message row |
| UI | Trace/Service 内嵌 Log Tab | 独立 aiMonitor 模块 |

**间接 overlap**：Token 趋势图与 Logs 趋势图可复用工具体系（时间选择器、图表组件），但无数据层耦合。

### 下游依赖

| 依赖项 | 影响 |
|--------|------|
| `config_ai_message` 表存在 | 已检查：`aiMessageSchemaReady()` 确认 |
| Doris SQL 聚合能力 | 当前 `loadRecentAiSessions()` 使用 `GROUP BY session_id`，MVＰ 无需额外依赖 |
| 前端 `db-table` + `query-filter` 组件 | 已存在，直接复用 |

---

## 4. 实现步骤

### Step 1: DDL 变更（0.5d）

- 编写 `deploy/common/sql/0.2.0-agent-obs.sql` 迁移脚本
- `ALTER TABLE config_ai_message ADD COLUMN` 增补字段

### Step 2: 采集增强（1.5d）

- `AiChatOrchestrator.java` — 注册 `PostReasoningEvent` listener，解析 `usage` → 写 metadata
- `AgentScopeSessionHook.java` — `persistTrace()` 加入 `modelName`/`modelProvider` metadata
- 单测：`AgentScopeSessionHookTest.java` 覆盖 token/meta 写入

### Step 3: API 开发（2d）

- 新建 `AiMonitorController.java`，实现 `/sessions` / `/sessions/{id}/messages` / `/tools/calls`
- 封装 `AiMonitorRepository.java`（封装 Doris SQL 聚合查询）
- 单测 + 集成测试

### Step 4: UI 对话追踪页（3d）

- 替换 `aiMonitor/sessions/index.vue` 的 ComingSoon → 真实列表页
- 新建 `aiMonitor/sessions/components/` → `session-list.vue` + `session-detail.vue`
- 对接 `AiMonitorController` API
- 后端 `OpenSourceMenuCatalog` 同步调整（若新增子路由）

### Step 5: UI 工具调用页（2d）

- 替换 `aiMonitor/toolCalls/index.vue`
- 列表 + 按 session 展开详情

### Step 6: Token/Model 页（2d，依赖 Step 1-2 完成后）

- 替换 `aiMonitor/tokens/index.vue`
- 替换 `aiMonitor/modelCalls/index.vue`

---

## 5. 风险与替代方案

| 风险 | 概率 | 影响 | 缓解方案 |
|------|------|------|----------|
| AgentScope `PostReasoningEvent` 不暴露 `usage` | 中 | Token 采集依赖 | 回退方案：在 LLM provider 调用处直接算 Token（通过 tiktoken 估算） |
| Doris `GROUP BY` 性能在大数据量下降 | 低 | 页面加载慢 | 加列存索引或物化视图；前期 session 量小无需优化 |
| 前端 `ComingSoon` 一次性全部替换成本高 | 中 | 交付延迟 | MVP 只做 sessions + toolCalls 两个页面，其余保持 ComingSoon |
| model_name 在多 expert 场景下上报不一致 | 低 | 数据质量 | 采集端使用 `ExpertChatContext.State` 的 expertId 关联 `config_ai_expert.model_name` |
| Token 统计与 LLM Provider 账单不一致 | 中 | 数据可信度 | UI 标注"估算值"，不承诺精确计费 |

---

## 6. 源码引用清单

| # | 文件 | 行号 | 用途 |
|---|------|------|------|
| 1 | `AgentScopeSessionHook.java` | 39-397 | 核心事件采集与持久化 |
| 2 | `AgentScopeSessionHook.java` | 231-238 | 工具调用 duration 计算 |
| 3 | `AiSessionStore.java` | 524-568 | `appendTraceMessage` 写入入口 |
| 4 | `AiSessionPersistence.java` | 186-218 | `toMessageRow` Doris 字段映射 |
| 5 | `ApmConfigRepository.java` | 261-284 | `loadRecentAiSessions()` SQL 聚合查询 |
| 6 | `ApmConfigRepository.java` | 292-317 | `loadAiMessages()` Doris 读取 |
| 7 | `ApmConfigRepository.java` | 1035-1056 | `AiMessageRow` record 定义 |
| 8 | `AgentController.java` | 55-65 | 现有 `/api/v1/ai/sessions` 端点参考 |
| 9 | `OpenSourceMenuCatalog.java` | 37-45 | aiMonitor 菜单定义 |
| 10 | `deploy/common/sql/databuff.sql` | 1384-1409 | `config_ai_message` 当前 DDL |
| 11 | `route-data.ts` | 472-578 | 前端 aiMonitor 路由定义 |
| 12 | `AiMessageType.java` | 3-11 | 消息类型枚举 |
| 13 | `AiSessionPersistence.java` | 22-47 | 持久化初始化与 listener 注册 |
| 14 | `ExpertChatContext.java` | 42-66 | 运行时上下文（承载 modelName 等元信息） |

---

## 7. 建议后续 task 拆分

依据实现步骤，拆分下游叶子 task：

### L2-task-agent-obs-ddl (0.5d)
- 编写 `0.2.0-agent-obs.sql` DDL 迁移脚本
- `DorisTableNames.java` 无变更
- `AiMessageRow` record 增补字段

### L2-task-agent-obs-collect (1.5d)
- `AiChatOrchestrator.java` — `PostReasoningEvent` 采集
- `AgentScopeSessionHook.java` — modelName/provider metadata 增强
- 单元测试覆盖

### L2-task-agent-obs-api (2d)
- 新建 `AiMonitorController.java` 含 3-5 个端点
- 新建 `AiMonitorRepository.java` Doris 查询封装
- 集成测试

### L2-task-agent-obs-ui-sessions (3d)
- 替换 `sessions/index.vue` ComingSoon → 真实列表页
- 新增 session detail 组件（消息时间线）
- API 对接

### L2-task-agent-obs-ui-toolcalls (2d)
- 替换 `toolCalls/index.vue` ComingSoon
- 工具调用聚合列表 + 详情展开

### L2-task-agent-obs-ui-tokens (2d, 后置)
- 替换 `tokens/index.vue` + `modelCalls/index.vue`
- Token 趋势图（复用现有 `metric-select` / `charts` 组件）
