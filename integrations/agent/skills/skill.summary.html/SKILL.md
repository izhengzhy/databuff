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
| 巡检结论、长分析、多指标汇总、根因结论等较完整的交付 | **主动建议**写一份 HTML 总结；用户同意或场景明确需要时再写 |
| 短问答、单点查询、澄清问题 | **不要**写文件，直接在对话中回答 |

## 模版一览（`templates/`）

多数 HTML **只作色系与观感参考**；`inspection-report.html` 是巡检报告的**结构模版**（无数据章节可删，勿编造）。

| 文件 | 大致方向 |
|------|----------|
| `summary-brief.html` | 短总结：结论优先、信息密度高、少装饰 |
| `report-analysis.html` | 长分析/报告：章节感、表格与证据区、卡片分区观感 |
| `inspection-report.html` | **服务巡检报告**：判定条 + 入口指标 / 日志 / 告警 / 依赖 / Trace / 实例 / JVM·资源 / 建议 |

读取路径：`resources/skill.summary.html/templates/{文件名}`

## 怎么写

1. 主交付物优先 **自包含 HTML**（`.html`），写入 `outputs/{slug}.html`。
2. 需要参考色系/排版或巡检结构时，用 `readWorkspaceFile` 读上表对应模版。
3. **巡检报告**：优先按 `inspection-report.html` 的章节结构产出；用 `inspectService` 与补充查询的真实数据填充，**禁止编造**。
4. 其它模版不是填空卷：结构、章节、组件可自定。
5. 证据表、CSV、中间数据可用 `.csv` / `.md` / `.json`，但面向阅读的主结论用 HTML。
6. 写完后用一句话告知用户文件名，便于其在对话中点击预览。

## 工具

- `listWorkspaceFiles`：列出 `uploads` / `outputs` / `resources/...`
- `readWorkspaceFile`：读附件与风格/结构模版
- `writeWorkspaceFile`：写入 `outputs/`（通用写文件，任意文本后缀）

不要用 shell 去找 Skill 包路径；模版统一从 `resources/skill.summary.html/` 读取。
