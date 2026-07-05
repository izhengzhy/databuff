"""日志分析 /appMonitor/logs"""

from __future__ import annotations

from pathlib import Path

from ...common import (
    DEMO_EXCEPTION_INSUFFICIENT_STOCK,
    DEMO_HOST_A,
    DEMO_SERVICE_A_ID,
    ApiCase,
    log_search_body,
    log_trend_body,
    log_body,
)


CASE_DIR = Path(__file__).resolve().parent


def build_cases(frm_ms: int, to_ms: int) -> list[ApiCase]:
    page = "日志分析"
    trend = log_trend_body(frm_ms, to_ms)
    search = log_search_body(frm_ms, to_ms)
    conditions = log_body(frm_ms, to_ms)
    return [
        ApiCase(page, "日志趋势", "POST", "/webapi/log/trend", trend, CASE_DIR),
        ApiCase(page, "日志列表", "POST", "/webapi/log/search", search, CASE_DIR),
        ApiCase(page, "筛选条件", "POST", "/webapi/log/conditions", conditions, CASE_DIR),
        ApiCase(
            page,
            "按服务A筛选趋势",
            "POST",
            "/webapi/log/trend",
            log_trend_body(frm_ms, to_ms, serviceIds=[DEMO_SERVICE_A_ID]),
            CASE_DIR,
        ),
        ApiCase(
            page,
            "按服务A筛选列表",
            "POST",
            "/webapi/log/search",
            log_search_body(frm_ms, to_ms, serviceIds=[DEMO_SERVICE_A_ID]),
            CASE_DIR,
        ),
        ApiCase(
            page,
            "按ERROR筛选趋势",
            "POST",
            "/webapi/log/trend",
            log_trend_body(frm_ms, to_ms, severities=["ERROR"]),
            CASE_DIR,
        ),
        ApiCase(
            page,
            "按ERROR筛选列表",
            "POST",
            "/webapi/log/search",
            log_search_body(frm_ms, to_ms, severities=["ERROR"]),
            CASE_DIR,
        ),
        ApiCase(
            page,
            "按主机A筛选列表",
            "POST",
            "/webapi/log/search",
            log_search_body(frm_ms, to_ms, hosts=[DEMO_HOST_A]),
            CASE_DIR,
        ),
        ApiCase(
            page,
            "关键字搜索列表",
            "POST",
            "/webapi/log/search",
            log_search_body(frm_ms, to_ms, query=DEMO_EXCEPTION_INSUFFICIENT_STOCK),
            CASE_DIR,
        ),
        ApiCase(
            page,
            "ERROR筛选条件联动",
            "POST",
            "/webapi/log/conditions",
            log_body(frm_ms, to_ms, severities=["ERROR"]),
            CASE_DIR,
        ),
        ApiCase(
            page,
            "服务A筛选条件联动",
            "POST",
            "/webapi/log/conditions",
            log_body(frm_ms, to_ms, serviceIds=[DEMO_SERVICE_A_ID]),
            CASE_DIR,
        ),
    ]
