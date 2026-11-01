#!/bin/sh
# 手动打包：aapt(资源) -> javac -> d8(dex) -> 合成 -> zipalign -> apksigner
set -e
cd /workspace/kanban

python3 check_page.py

export JAVA_HOME=/workspace/tools/jdk
export PATH="$JAVA_HOME/bin:/workspace/tools/build-tools:/usr/bin:/bin:$PATH"

AJ=/workspace/tools/platform/android.jar
BT=/workspace/tools/build-tools
OUT_APK="/sdcard/Live2D_看板娘app/apk/kanban.apk"

rm -rf build gen out dex
mkdir -p build gen out dex

echo "== 1/6 aapt2：编译资源 + 链接（manifest/assets） =="
# 本机只有 aapt（aapt2 官方版是 x86_64 跑不了，apt 源没有 aapt2），一套参数两边都能吃
AAPT2=/usr/bin/aapt
if command -v aapt2 >/dev/null 2>&1 && aapt2 version >/dev/null 2>&1; then AAPT2=aapt2; fi
$AAPT2 compile --dir res -o build/res.zip
$AAPT2 link -o build/base.apk \
      -I /usr/share/android-framework-res/framework-res.apk \
      --manifest AndroidManifest.xml \
      -A assets \
      build/res.zip \
      --min-sdk-version 26 --target-sdk-version 33 \
      --version-code 1 --version-name 1.0 \
      --java gen --auto-add-overlay

echo "== 2/6 javac =="
find src gen -name '*.java' > build/sources.txt
javac -encoding UTF-8 -source 11 -target 11 -nowarn \
      -classpath "$AJ" -d out @build/sources.txt 2>&1 | grep -v "^warning" || true
ls out/com/eta/kanban/ | head -20

echo "== 3/6 d8 =="
find out -name '*.class' > build/classes.txt
"$BT/d8" --lib "$AJ" --min-api 26 --output dex @build/classes.txt
ls -l dex/

echo "== 4/6 合成 APK =="
python3 merge.py build/base.apk dex/classes.dex build/unsigned.apk

echo "== 5/6 zipalign =="
zipalign -f 4 build/unsigned.apk build/aligned.apk

echo "== 6/6 签名 =="
KS=build/debug.keystore
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -storepass android -keypass android \
          -alias kanban -keyalg RSA -keysize 2048 -validity 3650 \
          -dname "CN=Kanban, OU=Eta, O=Eta, L=NA, S=NA, C=CN"
fi
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android \
      --out "$OUT_APK" build/aligned.apk

ls -l "$OUT_APK"
echo "== 校验 =="
"$BT/apksigner" verify --print-certs "$OUT_APK" | head -5
