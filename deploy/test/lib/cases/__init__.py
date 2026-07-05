"""按一级/二级目录聚合接口用例.

一级目录: 应用性能、基础设施…（与 portal 左侧大菜单对应）
二级目录: 各模块下的子菜单（如全局拓扑、服务、服务流）
"""

from __future__ import annotations

from typing import Callable

from .common import ApiCase, MODULE_APP_MONITOR
from .应用性能.JVM.cases import build_cases as apm_jvm_cases
from .应用性能.全局拓扑.cases import build_cases as apm_global_topology_cases
from .应用性能.服务流.cases import build_cases as apm_service_flow_cases
from .应用性能.服务.cases import build_cases as apm_service_cases
from .应用性能.数据库.cases import build_cases as apm_database_cases
from .应用性能.消息队列.cases import build_cases as apm_msg_queue_cases
from .应用性能.缓存.cases import build_cases as apm_cache_cases
from .应用性能.外部服务.cases import build_cases as apm_external_cases
from .应用性能.接口分析.cases import build_cases as apm_service_analysis_cases
from .应用性能.错误分析.cases import build_cases as apm_errors_cases
from .应用性能.链路追踪.cases import build_cases as apm_trace_cases
from .应用性能.日志分析.cases import build_cases as apm_log_analysis_cases

CaseBuilder = Callable[[int, int], list[ApiCase]]

# 应用性能 — 顺序与 OpenSourceMenuCatalog 子菜单一致
APM_MENU_BUILDERS: list[CaseBuilder] = [
    apm_global_topology_cases,
    apm_service_flow_cases,
    apm_service_cases,
    apm_jvm_cases,
    apm_database_cases,
    apm_msg_queue_cases,
    apm_cache_cases,
    apm_external_cases,
    apm_service_analysis_cases,
    apm_errors_cases,
    apm_trace_cases,
    apm_log_analysis_cases,
]

APM_MENU_DIRS = [
    "全局拓扑",
    "服务流",
    "服务",
    "JVM",
    "数据库",
    "消息队列",
    "缓存",
    "外部服务",
    "接口分析",
    "错误分析",
    "链路追踪",
    "日志分析",
]

PAGE_BUILDERS: list[CaseBuilder] = APM_MENU_BUILDERS

PAGE_DIRS = [f"{MODULE_APP_MONITOR}/{name}" for name in APM_MENU_DIRS]

MENU_LABELS = APM_MENU_DIRS


def build_cases(frm_ms: int, to_ms: int) -> list[ApiCase]:
    cases: list[ApiCase] = []
    for builder in PAGE_BUILDERS:
        cases.extend(builder(frm_ms, to_ms))
    return cases


__all__ = [
    "ApiCase",
    "MODULE_APP_MONITOR",
    "APM_MENU_BUILDERS",
    "APM_MENU_DIRS",
    "build_cases",
    "PAGE_BUILDERS",
    "PAGE_DIRS",
    "MENU_LABELS",
]
