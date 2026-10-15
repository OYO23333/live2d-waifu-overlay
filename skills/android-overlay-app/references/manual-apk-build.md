# 无 Gradle 手工构建 Android APK

适用：arm64 手机上的 Debian/Linux 环境，无 Android Studio、无 Gradle 网络。

## 工具来源（都验证过可达）

| 组件 | 来源 | 备注 |
|---|---|---|
| JDK | `https://corretto.aws/downloads/latest/amazon-corretto-21-aarch64-linux-jdk.tar.gz` | Adoptium 的二进制托管在 GitHub Releases，墙内下不到，别用 |
| build-tools | `https://dl.google.com/android/repository/build-tools_r37_linux.zip` | URL 从 `repository2-3.xml` 里读，别猜；可跳过 173MB 的 cmdline-tools |
| platform | `https://dl.google.com/android/repository/platform-37.0_r02.zip` | 提供 `android.jar` |
| aapt2 / zipalign | `apt install aapt zipalign`（Debian 源，arm64） | **官方 build-tools 里的 aapt2/zipalign 是 x86_64，在 arm64 上 Exec format error** |
| 框架资源表 | `apt install android-framework-res` → `/usr/share/android-framework-res/framework-res.apk` | **关键**：`platform-37` 的 `android.jar` 里没有 `resources.arsc`，aapt2 会报 `No resource identifier found for attribute ...`；用这个 apk 作 `-I` |
| d8 / apksigner | build-tools 里（Java 版） | 可直接用 |

## 构建顺序

```sh
aapt2 compile --dir res -o build/res.zip
aapt2 link -o build/base.apk \
    -I /usr/share/android-framework-res/framework-res.apk \
    --manifest AndroidManifest.xml -A assets build/res.zip \
    --min-sdk-version 26 --target-sdk-version 33 \
    --version-code 1 --version-name 1.0 --java gen --auto-add-overlay
javac -encoding UTF-8 -source 11 -target 11 -classpath <platform>/android.jar -d out $(find src gen -name '*.java')
d8 --lib <platform>/android.jar --min-api 26 --output dex $(find out -name '*.class')
python3 merge.py build/base.apk dex/classes.dex build/unsigned.apk   # 把 classes.dex 塞进 APK（zip 内不能有旧签名）
zipalign -f 4 build/unsigned.apk build/aligned.apk
keytool -genkeypair -keystore build/debug.keystore -storepass android -keypass android \
        -alias app -keyalg RSA -keysize 2048 -validity 3650 -dname "CN=App"
apksigner sign --ks build/debug.keystore --ks-pass pass:android --out <输出>.apk build/aligned.apk
```

## 坑

- `targetSdk 34` 要求前台服务声明 `foregroundServiceType`，而老框架资源表里可能没有该属性值（如 `specialUse` 是 API 34 才有）→ 直接 `targetSdk 33` 最省事。
- 签名前必须删掉旧 `META-INF/`（merge.py 里做掉），否则 v3 签名校验会失败。
- APK 越大（>100MB 素材）aapt2 link 越慢，放后台任务里跑，别用会超时的前台命令。
- 装完若看不到界面：先确认 `appops get <pkg> SYSTEM_ALERT_WINDOW` 是 allow。
