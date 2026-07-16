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

## 怎么写

1. 主交付物优先 **自包含 HTML**（`.html`），写入 `outputs/{slug}.html`。
2. 先用 `readWorkspaceFile` 读 `resources/skill.summary.html/templates/README.md`，了解有哪些风格参考。
3. 需要参考色系/排版时，再 `read` 对应 `resources/skill.summary.html/templates/*.html`。
4. **模版只作风格与色系参考，不是填空卷**：结构、章节、组件可自定，不必套模版 DOM。
5. 证据表、CSV、中间数据可用 `.csv` / `.md` / `.json`，但面向阅读的主结论用 HTML。
6. 写完后用一句话告知用户文件名，便于其在对话中点击预览。

## 工具

- `listWorkspaceFiles`：列出 `uploads` / `outputs` / `resources/...`
- `readWorkspaceFile`：读附件、模版说明与风格参考
- `writeWorkspaceFile`：写入 `outputs/`（通用写文件，任意文本后缀）

不要用 shell 去找 Skill 包路径；模版统一从 `resources/skill.summary.html/` 读取。
