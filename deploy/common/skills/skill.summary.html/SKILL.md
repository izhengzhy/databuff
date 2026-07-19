---
name: skill.summary.html
description: 总结与报告 HTML 产出规范（共享）；何时写文件、如何参考风格模版
---
# 总结产出

你可以使用会话工作区工具写出可下载、可预览的交付物。本 Skill 适用于**所有数字专家**。

## 何时写文件

| 场景 | 行为 |
|------|------|
| 用户明确说「生成报告」「导出」「写成总结/HTML」等 | **必须**用 `writeWorkspaceFile` 写入 `outputs/` |
| **服务巡检**且结论较完整 | **可主动建议**生成 HTML 巡检报告；用户同意或明确要求导出时再写 |
| 长分析、多指标汇总、根因定位、故障诊断等（**非巡检**） | **不要**主动建议或生成 HTML，直接在对话中回答 |
| 短问答、单点查询、澄清问题 | **不要**写文件，直接在对话中回答 |

## 模版一览（`templates/`）

多数 HTML **只作色系与观感参考**；`inspection-report.html` 是巡检报告的**结构模版**（无数据章节可删，勿编造）。

| 文件 | 适用场景 |
|------|----------|
| `inspection-report.html` | **服务巡检 / 健康检查**报告 |
| `report-analysis.html` | 长分析、根因报告、多指标汇总 |
| `summary-brief.html` | 短总结、一两屏结论 |

读取路径：`resources/skill.summary.html/templates/{文件名}`

### 选模版与读取

- **按文件名直接选定**：对照上表场景即可，**不要** `listWorkspaceFiles` 浏览、不要逐个试读或对比多个模版。
- 一般 **只选一个** 模版。首次调用 `readWorkspaceFile` 不传 `lineRange`，默认读取 `1-9999` 行；若文件仍有内容，再按后续 `lineRange` 继续读取，直至读完。
- **同一任务/同一轮对话内**：禁止对同一文件的同一段 `lineRange` 重复读取；允许读取该文件尚未读取的后续范围。
- 巡检场景固定用 `inspection-report.html`，不必再看其它模版。

## 怎么写

1. 主交付物优先 **自包含 HTML**（`.html`），写入 `outputs/{slug}.html`。
2. 写 HTML 前，按上表选定模版并读完，作为色系/排版或结构参考。
3. **巡检报告**：按 `inspection-report.html` 章节结构产出；用 `inspectService` 与补充查询的真实数据填充，**禁止编造**。
4. 其它模版不是填空卷：结构、章节、组件可自定。
5. 证据表、CSV、中间数据可用 `.csv` / `.md` / `.json`，但面向阅读的主结论用 HTML。
6. 写完后用一句话告知用户文件名，便于其在对话中点击预览。

## 工具

- `listWorkspaceFiles`：列出 `uploads` / `outputs` / `resources/...`
- `readWorkspaceFile`：读附件与风格/结构模版
- `writeWorkspaceFile`：写入 `outputs/`（通用写文件，任意文本后缀）

不要用 shell 去找 Skill 包路径；模版统一从 `resources/skill.summary.html/` 读取。
