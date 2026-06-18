"""错误分析 /appMonitor/errors（含错误详情下钻页）"""

from __future__ import annotations

from pathlib import Path

from ...common import ApiCase, error_span_list_body, metric_chart_body, service_body


CASE_DIR = Path(__file__).resolve().parent


def build_cases(frm_ms: int, to_ms: int) -> list[ApiCase]:
    page = "错误分析"
    sb = service_body(frm_ms, to_ms, limit=20)
    return [
        ApiCase(page, "错误分布", "POST", "/webapi/service/exceptionDistMap", service_body(frm_ms, to_ms), CASE_DIR),
        ApiCase(page, "错误 span 列表", "POST", "/webapi/trace/exceptionList", sb, CASE_DIR),
        ApiCase(
            page,
            "错误 span 按服务与异常筛选",
            "POST",
            "/webapi/trace/errorSpanList",
            error_span_list_body(frm_ms, to_ms),
            CASE_DIR,
        ),
        ApiCase(
            page,
            "错误指标趋势",
            "POST",
            "/webapi/metrics/exploreMetricByGroupGraph",
            metric_chart_body(frm_ms, to_ms, "trace.error_count"),
            CASE_DIR,
        ),
    ]
