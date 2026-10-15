#!/bin/sh
# 把共享存储里的源码快照同步回 Debian 环境的工程目录，可选直接重新打包
#
# 用法（在 Debian 环境里执行）：
#   sh /workspace/kanban/sync-from-shared.sh          # 只同步
#   sh /workspace/kanban/sync-from-shared.sh build    # 同步并重新打包
#
# 典型流程：用 MT 管理器编辑 /storage/emulated/0/Live2D_看板娘app/source-源码快照/
#           然后跑这个脚本，APK 会输出到 共享存储/Live2D_看板娘app/apk/kanban.apk

set -e
SRC="/sdcard/Live2D_看板娘app/source-源码快照"
DST="/workspace/kanban"

if [ ! -d "$SRC" ]; then
    echo "找不到快照目录：$SRC"
    exit 1
fi

mkdir -p "$DST/src" "$DST/res"
cp -r "$SRC/src/." "$DST/src/"
cp -r "$SRC/res/." "$DST/res/"
cp "$SRC/AndroidManifest.xml" "$SRC/build.sh" "$SRC/merge.py" "$DST/"
chmod +x "$DST/build.sh"
echo "已同步：$SRC -> $DST"

if [ "$1" = "build" ]; then
    echo "开始打包……"
    cd "$DST"
    sh build.sh
fi
