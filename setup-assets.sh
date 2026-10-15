#!/bin/sh
# 从手机共享存储里的离线素材目录还原 assets/（这些素材含第三方角色形象，不入 git）
#
# 用法（Debian 环境）：
#   sh /workspace/kanban/setup-assets.sh
#
# 素材来源目录：/sdcard/Live2D_看板娘app/assets-离线素材/
#   kbn.js       引擎（原版，一字未改）
#   api/         本地静态应答（模型索引、贴图切换、一言文案）
#   model/       7 套 Live2D 模型

set -e
SRC="/sdcard/Live2D_看板娘app/assets-离线素材"
DST="/workspace/kanban/assets"

if [ ! -d "$SRC" ]; then
    echo "找不到素材目录：$SRC"
    echo "（如果你的手机上是别的位置，改这个脚本里的 SRC 即可）"
    exit 1
fi

rm -rf "$DST"
mkdir -p "$DST"
cp "$SRC/kbn.js" "$DST/"
cp -r "$SRC/api" "$DST/"
cp -r "$SRC/model" "$DST/"
echo '{}' > "$DST/api/notfound.json"

echo "素材已还原到 $DST"
du -sh "$DST"
