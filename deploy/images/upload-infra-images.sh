#!/usr/bin/env bash
# 拉取 Doris FE/BE、ZooKeeper 多架构镜像 → 合并/压缩导出 tar.gz → 上传到 infra 镜像包目录。
#
# Usage:
#   ./deploy/images/upload-infra-images.sh
#
# Optional:
#   SKIP_IMAGE_PKG_UPLOAD=1   跳过上传
#   IMAGE_PLATFORMS=linux/amd64,linux/arm64

set -euo pipefail

source "$(cd "$(dirname "$0")" && pwd)/scripts/lib.sh"

ensure_command docker

export_infra_image_tarballs

cat <<EOF

[upload-infra-images] done
  Doris FE : ${DORIS_FE_IMAGE}
  Doris BE : ${DORIS_BE_IMAGE}
  ZooKeeper: ${ZOOKEEPER_IMAGE}
  Arch     : $(image_platforms)
  ImagePkg : $(infra_images_pkg_base_url)

EOF
