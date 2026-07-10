#!/usr/bin/env bash
# Doris BE on x86_64 uses AVX2 vectorization; skip when /proc/cpuinfo is unavailable (e.g. macOS).

ensure_avx2_cpu() {
  if [ "${DATABUFF_SKIP_AVX2_CHECK:-}" = "1" ] || [ "${DATABUFF_SKIP_AVX2_CHECK:-}" = "true" ]; then
    echo "警告: 已跳过 AVX2 CPU 检测 (DATABUFF_SKIP_AVX2_CHECK)。Doris BE 在无 AVX2 的 x86_64 上可能无法启动或查询性能下降，请自行承担风险。" >&2
    return 0
  fi
  if [ ! -f /proc/cpuinfo ]; then
    return 0
  fi
  case "$(uname -m)" in
    x86_64 | amd64) ;;
    *) return 0 ;;
  esac
  if ! grep -q avx2 /proc/cpuinfo; then
    echo "Doris 使用 AVX2 向量化加速查询，建议使用支持 AVX2 指令集的机器。详情请参阅：https://doris.apache.org/docs/dev/install/preparation/env-checking" >&2
    exit 1
  fi
}
