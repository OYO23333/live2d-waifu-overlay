# 桌面看板娘（Android 悬浮窗 · Live2D）

把网页版的 Live2D 看板娘从浏览器里搬出来，做成安卓 App，以**悬浮窗**形式待在桌面和其他应用之上。
自用 / 学习向项目。

原来的网页插件是 Greasy Fork 上的「网页看板娘」，真正干活的是
[IlysvlVEizbr/Live2D](https://github.com/IlysvlVEizbr/Live2D) 里那个 `kbn.js`
（改自 [fghrsh/live2d_demo](https://github.com/fghrsh/live2d_demo)）。本项目把它原封不动放进
`assets/`，用 WebView 渲染，并在 `shouldInterceptRequest` 里把它的所有远端请求引到本地，
于是**完全不联网**也能跑。

## 结构

```
AndroidManifest.xml      清单（悬浮窗/前台服务/通知/开机自启）
build.sh                 一键打包：aapt2 → javac → d8 → 合成 → zipalign → apksigner
merge.py                 把 classes.dex 合成进 APK
setup-assets.sh          从共享存储还原 assets/（素材不入 git）
sync-from-shared.sh      把共享存储里的源码快照同步回来，可选直接打包
res/                     资源
src/com/eta/kanban/
  MainActivity.java      主界面：显示/收起/重载/设置 + 权限引导
  SettingsActivity.java  设置：模型、贴图、尺寸、停靠、透明度、工具栏、点击穿透、拖动手柄、开机自启
  OverlayService.java    悬浮窗前台服务（透明 WebView + 拖动手柄 + 通知）
  LocalAssets.java       拦截远端请求 → 映射到 assets
  Page.java              生成看板娘页面（把设置注入成 kbn_settingN）
  Prefs.java             设置存取
  BootReceiver.java      开机自启（被系统拦截时退化为通知）
```

## 构建

需要手机上的 Debian 环境（本项目就在其中开发）。工具链：

| 组件 | 位置 |
|---|---|
| JDK | `/workspace/tools/jdk`（Corretto 21 aarch64） |
| android.jar | `/workspace/tools/platform/android.jar`（API 37.0） |
| d8 / apksigner | `/workspace/tools/build-tools`（Java 版，可用） |
| aapt2 / zipalign | `/usr/bin`（apt 装的 arm64 版；build-tools 自带的是 x86_64，跑不了） |
| 框架资源表 | `/usr/share/android-framework-res/framework-res.apk` |

```sh
sh setup-assets.sh      # 首次：还原素材
sh build.sh             # 打包，产物 → /sdcard/Live2D_看板娘app/apk/kanban.apk
```

## 安装到手机

```sh
cp /sdcard/Live2D_看板娘app/apk/kanban.apk /data/local/tmp/kanban.apk
pm install -r /data/local/tmp/kanban.apk
appops set com.eta.kanban SYSTEM_ALERT_WINDOW allow
pm grant com.eta.kanban android.permission.POST_NOTIFICATIONS
dumpsys deviceidle whitelist +com.eta.kanban
```

## 使用

打开「看板娘」App：显示看板娘 / 收起看板娘 / 重新加载 / 设置 / 权限引导，以及**切换点击穿透**按钮。

设置项：模型（1–7，默认 1 = Potion-Maker/Pio）、贴图、宽高、左右停靠、到边距 X、到边距 Y、
透明度、工具栏、关闭按钮、气泡台词、**点击穿透**、**工具栏常显**、**位置贴底边**、拖动手柄、开机自启。

- **拖动**：长按看板娘 0.8 秒（震动提示）后按住拖动；位置自动记住，默认贴屏幕下边。
  落在工具栏图标上的触摸不触发长按，所以点关闭/换装/换人不会被抢。
- **点击穿透**：开启后她变成纯装饰（触摸穿透到下层应用），此时用**通知栏第一个按钮**
  或主界面按钮切回可交互。
- **关闭**：点她工具栏的 X → 窗口整体消失（含拖动手柄），通知变"已收起"，可再唤回。
- **守护**：服务被系统回收后会自动拉起；开机按设置自启；熄屏时暂停渲染省电。

## 踩过的坑

1. `platform-37` 的 `android.jar` **没有 resources.arsc**，aapt2 会报 “No resource identifier found for attribute”
   → 用 apt 装 `android-framework-res`，链接时 `-I /usr/share/android-framework-res/framework-res.apk`
2. 官方 build-tools 里的 `aapt2`/`zipalign` 是 **x86_64**，arm64 上 `Exec format error` → 用 Debian 源的 arm64 版本
3. 悬浮窗用 `WRAP_CONTENT` 会量出 **0×0**（`position:fixed` 不贡献内在尺寸）→ 必须给显式像素尺寸
4. 窗口贴在最顶上时，拖动手柄会落在状态栏的触摸区里被吃掉 → 位置下限设为状态栏高度
5. `pm install` 不能直接从 `/sdcard` 装（system_server 读不了 FUSE）→ 先 `cp` 到 `/data/local/tmp`
6. 长下载任务别用一次性异步任务（随运行结束被回收）→ 用常驻任务 + 断点续传

## 素材与许可

`assets/` 里的模型与引擎来自 `fghrsh/live2d_api`、`IlysvlVEizbr/Live2D`（GPL-2.0）。
其中 Potion-Maker、bilibili-live、ShizukuTalk、HyperdimensionNeptunia、KantaiCollection
等角色形象版权归各自权利人。

**仅限个人学习使用，请勿商用或再分发**——这也是 `assets/` 不进 git 的原因。
