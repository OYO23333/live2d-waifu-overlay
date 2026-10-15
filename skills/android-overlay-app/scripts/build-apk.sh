#!/bin/sh
# 手工构建 Android APK 的模板（无 Gradle）。放在工程根目录，按注释改路径即可。
# 用法：sh build-apk.sh
set -e
cd "$(dirname "$0")"

# ---- 按环境改这三行 ----
JAVA_HOME=/workspace/tools/jdk
BT=/workspace/tools/build-tools                 # 提供 d8 / apksigner（Java 版）
AJ=/workspace/tools/platform/android.jar        # 来自 platform-37.0
# aapt2 / zipalign 用 Debian 源的 arm64 版（/usr/bin），框架资源表也用 apt 装的
FRAMEWORK=/usr/share/android-framework-res/framework-res.apk
OUT_APK=${OUT_APK:-dist/app.apk}

export JAVA_HOME
export PATH="$JAVA_HOME/bin:$BT:/usr/bin:/bin:$PATH"

rm -rf build gen out dex && mkdir -p build gen out dex dist

echo "== 1/5 资源与清单 =="
[ -d res ] && aapt2 compile --dir res -o build/res.zip || echo '(无 res 目录，跳过)'
aapt2 link -o build/base.apk -I "$FRAMEWORK" \
    --manifest AndroidManifest.xml ${RES_ARG:+-A assets} \
    ${RES_ARG:-$( [ -f build/res.zip ] && echo build/res.zip )} \
    --min-sdk-version 26 --target-sdk-version 33 \
    --version-code 1 --version-name 1.0 --java gen --auto-add-overlay

echo "== 2/5 javac =="
find src gen -name '*.java' > build/sources.txt
javac -encoding UTF-8 -source 11 -target 11 -nowarn -classpath "$AJ" -d out @build/sources.txt

echo "== 3/5 d8 =="
find out -name '*.class' > build/classes.txt
"$BT/d8" --lib "$AJ" --min-api 26 --output dex @build/classes.txt

echo "== 4/5 合成 APK =="
python3 - <<'PY'
import sys, zipfile, os
base, dex, out = 'build/base.apk', 'dex/classes.dex', 'build/unsigned.apk'
if os.path.exists(out): os.remove(out)
src, dst = zipfile.ZipFile(base), zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED)
for it in src.infolist():
    if it.filename == 'classes.dex' or it.filename.startswith('META-INF/'): continue
    dst.writestr(it, src.read(it.filename))
dst.write(dex, 'classes.dex'); src.close(); dst.close()
print('  ->', out, os.path.getsize(out), 'bytes')
PY

echo "== 5/5 对齐与签名 =="
zipalign -f 4 build/unsigned.apk build/aligned.apk
KS=build/debug.keystore
[ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -storepass android -keypass android \
    -alias app -keyalg RSA -keysize 2048 -validity 3650 -dname "CN=App, OU=Dev, O=Dev, C=CN"
mkdir -p "$(dirname "$OUT_APK")"
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android \
    --out "$OUT_APK" build/aligned.apk
ls -l "$OUT_APK"
echo "装它：cp $OUT_APK /data/local/tmp/app.apk && pm install -r /data/local/tmp/app.apk"
