# 桌面看板娘（Android 悬浮窗 · Live2D）

把网页版 Live2D 看板娘做成安卓 App，以**悬浮窗**形式待在桌面和其他应用之上。自用 / 学习向。

引擎用的是 [stevenjoezhang/live2d-widget](https://github.com/stevenjoezhang/live2d-widget)
（TypeScript 重写、长期维护的现代版本，也是 fghrsh 那套的继任者）。相比早期用的 `kbn.js`：

- 模块化、**1.0 起不需要后端**：换装靠静态的 `model_list.json` + 每个模型的 `textures.cache`
- 自带工具栏（换人 / 换装 / 一言 / 截图 / 关于 / 关闭）、气泡台词触发（`waifu-tips.json`）
- 支持 Cubism 2 与 Cubism 5；本项目用的是 Cubism 2 模型

全部资源打进 APK，页面 origin 是 `https://app.local/`，所有请求由 `LocalAssets` 映射到 assets，
**完全离线**。

## 结构

```
AndroidManifest.xml      清单（悬浮窗 / 前台服务 / 通知 / 开机自启 / 守护）
build.sh                 一键打包：aapt2 → javac → d8 → 合成 → zipalign → apksigner
merge.py                 把 classes.dex 合成进 APK
src/com/eta/kanban/
  MainActivity.java      主界面：显示 / 收起 / 重载 / 设置 / 授权引导 / 切换点击穿透
  SettingsActivity.java  设置：尺寸、停靠、边距、透明度、工具栏、关闭按钮、气泡、
                         点击穿透、贴底边、拖动手柄、开机自启
  OverlayService.java    悬浮窗前台服务（透明 WebView + 长按拖动 + 通知 + 守护 + 熄屏暂停）
  LocalAssets.java       把 app.local 的请求映射到 assets
  Page.java              生成页面：加载 live2d-widget 并调用 initWidget（配置全部指向本地）
  Prefs.java             设置存取
  BootReceiver.java      开机自启（被系统拦截时退化为通知）
  WatchdogReceiver.java  守护：服务被回收后自动拉起
assets/
  widget/                live2d-widget 引擎产物（waifu-tips.js、chunk、live2d.min.js、css、tips.json）
  modelrepo/             模型仓库：model_list.json + model/<名字>/index.json + textures.cache + 模型文件
```

## 资源怎么来

用 `/workspace/make_assets.js`（一条命令，两种来源）：

```sh
# 从 fghrsh/live2d_api 仓库取（Pio 有 87 套衣服）
node make_assets.js --fghrsh Potion-Maker/Pio

# 从 npm 模型包取（live2d-widget 生态，角色多、每个含动作/表情/语音）
node make_assets.js --npm live2d-widget-model-haru,live2d-widget-model-koharu

# 然后打包
sh build.sh
```

产出会直接覆盖 `assets/widget` 与 `assets/modelrepo`。想换角色就换参数重建，app 代码不用动。

## 构建环境

需要手机上的 Debian 环境（本项目就在其中开发）：

| 组件 | 位置 |
|---|---|
| JDK | `/workspace/tools/jdk`（Corretto 21 aarch64） |
| android.jar | `/workspace/tools/platform/android.jar`（API 37.0） |
| d8 / apksigner | `/workspace/tools/build-tools`（Java 版可用） |
| aapt2 / zipalign | `/usr/bin`（apt 的 arm64 版；build-tools 自带的是 x86_64，跑不了） |
| 框架资源表 | `/usr/share/android-framework-res/framework-res.apk` |

```sh
node make_assets.js --fghrsh Potion-Maker/Pio   # 生成资源
sh build.sh                                      # 打包 → /sdcard/Live2D_看板娘app/apk/kanban.apk
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

打开「看板娘」App：显示 / 收起 / 重新加载 / 设置 / 切换点击穿透 / 权限引导。

- **拖动**：长按看板娘 0.8 秒（震动提示）后按住拖动；位置自动记住，默认贴屏幕下边
- **换人 / 换装**：悬浮窗工具栏上操作，选择会自动记住
- **点击穿透**：开启后她变成纯装饰，用通知栏第一个按钮或主界面按钮切回来
- **关闭**：工具栏的关闭按钮 → 窗口整体消失，通知变"已收起"，可再唤回
- **守护**：服务被系统回收会自动拉起；开机按设置自启；熄屏时暂停渲染省电

## 踩过的坑

1. `platform-37` 的 `android.jar` **没有 resources.arsc**，aapt2 报 "No resource identifier found" →
   用 apt 装 `android-framework-res`，链接时 `-I /usr/share/android-framework-res/framework-res.apk`
2. 官方 build-tools 里的 `aapt2`/`zipalign` 是 **x86_64**，arm64 上 `Exec format error` → 用 Debian 源的 arm64 版本
3. 悬浮窗用 `WRAP_CONTENT` 会量出 **0×0**（内容用绝对定位，不贡献内在尺寸）→ 必须给显式像素尺寸
4. 窗口贴在最顶上时，拖动手柄会落在状态栏的触摸区里被吃掉 → 位置下限设为状态栏高度
5. `pm install` 不能直接从 `/sdcard` 装（system_server 读不了 FUSE）→ 先 `cp` 到 `/data/local/tmp`
6. 文件名里的空格会被 URL 编码成 `%20`，拦截器要解码，否则带空格的动作文件全部 404
7. 长任务别用一次性异步任务（随运行结束被回收）→ 用常驻任务 + 断点续传

## 素材与许可

模型来自 `fghrsh/live2d_api`、`live2d-widget` 生态的 npm 模型包，引擎为 live2d-widget（MIT）。
Potion-Maker、bilibili-live、ShizukuTalk、HyperdimensionNeptunia、KantaiCollection 等
角色形象版权归各自权利人。

**仅限个人学习使用，请勿商用或再分发。**
