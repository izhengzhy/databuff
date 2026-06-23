# 镜像构建上下文

ingest / web / demo 各自独立目录，由 `deploy/images/build-images.sh` 使用。

```
images/
├── ingest/     # Dockerfile + start.sh + application.yml
├── web/        # Dockerfile + start.sh + application.yml（构建时复制 skills/）
├── demo/       # Dockerfile + start.sh
├── scripts/    # 共享 lib.sh
├── build-images.sh
└── upload-infra-images.sh
```

`build-images.sh` 构建 `databuffhub/ai-apm-*` 短名镜像，合并导出 **`ai-apm-stack-<ver>-<arch>.tar.gz`**（ingest + web + demo，Docker / K8s 共用）并上传到 `${APM_PKG_BASE}/<ver>/images/`。

`upload-infra-images.sh` 上传基础设施离线包：

- **`doris-stack-<ver>-<arch>.tar.gz`** — Doris FE + BE 合并
- **`zookeeper-<ver>-<arch>.tar.gz`** — ZooKeeper（仅 K8s）

安装脚本若找不到新格式合并包，会回退下载旧版逐组件 `.tar`。
