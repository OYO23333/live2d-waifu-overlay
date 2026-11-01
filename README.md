# 桌面看板娘（Android 悬浮窗 · Live2D）

把网页版 Live2D 看板娘做成安卓 App，以**悬浮窗**形式待在桌面和其他应用之上。自用 / 学习向。

引擎用的是 [stevenjoezhang/live2d-widget](https://github.com/stevenjoezhang/live2d-widget)
（TypeScript 重写、长期维护的现代版本，也是 fghrsh 那套的继任者）。相比早期用的 `kbn.js`：

- 模块化、**1.0 起不需要后端**：换装靠静态的 `model_list.json` + 每个模型的 `textures.cache`
- 自带工具栏（换人 / 换装 / 一言 / 截图 / 关于 / 关闭）、气泡台词触发（`waifu-tips.json`）
- 支持 Cubism 2 与 Cubism 5；本项目用的是 Cubism 2 模型

全部资源打进 APK，页面 origin 是 `https://app.local/`，所有请求由 `LocalAssets` 映射到 assets，
**完全离线**。

## 功能一览

- **完整 App 主界面**：状态卡（权限/服务/当前人物与衣服/尺寸一览）、权限引导、
  悬浮窗控制、快捷换装（按真实名字选人物和衣服，即时生效）
- **悬浮窗看板娘**：透明 WebView 渲染，任意 App 之上都能看到她
- **交互**：点她说话（气泡台词）、长按拖动换位、工具栏换人/换装/一言/截图/关闭
- **点击穿透**：一键让她变纯装饰，触摸全部穿到下面的应用；通知栏可切回
- **守护**：前台服务 + 看门狗自动拉起 + 开机自启 + 熄屏暂停渲染省电
- **换装持久化**：工具栏上的换人/换装选择会同步回设置，重载、重启都不回退
- **完全离线**：引擎、模型、台词全部打进 APK，无任何网络请求

## 结构

```
AndroidManifest.xml      清单（悬浮窗 / 前台服务 / 通知 / 开机自启 / 守护）
build.sh                 一键打包：aapt2/aapt → javac → d8 → 合成 → zipalign → apksigner
check_page.py            打包前校验 Page.java 拼出的 HTML/JS（防 // 注释吃掉一整行）
merge.py                 把 classes.dex 合成进 APK
make_assets.js           资源生成：从 fghrsh/live2d_api 或 npm 模型包拉取并组装 assets/
src/com/eta/kanban/
  MainActivity.java      主界面：状态卡 + 权限引导 + 悬浮窗控制 + 快捷换装 + 设置入口
  SettingsActivity.java  设置：人物/衣服下拉（真实名字）、尺寸、停靠、边距、透明度、
                         工具栏、气泡、一言来源、点击穿透、拖动手柄、开机自启
  OverlayService.java    悬浮窗前台服务（透明 WebView + 长按拖动 + 通知 +
                         换装/穿透动作 + 守护 + 熄屏暂停）
  ModelRepo.java         读 assets/modelrepo：人物与衣服清单（给界面下拉用，带缓存）
  LocalAssets.java       把 app.local 的请求映射到 assets
  Page.java              生成页面：加载 live2d-widget 并调用 initWidget（配置全部指向本地），
                         并把工具栏里的换人/换装选择回传原生设置
  Prefs.java             设置存取
  BootReceiver.java      开机自启（被系统拦截时退化为通知）
  WatchdogReceiver.java  守护：服务被回收后自动拉起
assets/
  widget/                live2d-widget 引擎产物（waifu-tips.js、chunk、live2d.min.js、css、tips.json）
  modelrepo/             模型仓库：model_list.json + model/<名字>/index.json + textures.cache + 模型文件
skills/android-overlay-app/  沉淀的手工打包与悬浮窗经验（SKILL.md + 参考文档 + 脚本）
```

## 资源怎么来

用仓库里的 `make_assets.js`（一条命令，两种来源；输出目录等可用环境变量覆盖）：

```sh
# 从 fghrsh/live2d_api 仓库取（Pio 有 87 套衣服）
node make_assets.js --fghrsh Potion-Maker/Pio

# 从 npm 模型包取（live2d-widget 生态，角色多、每个含动作/表情/语音）
node make_assets.js --npm live2d-widget-model-haru,live2d-widget-model-koharu

# 然后打包
sh build.sh
```

产出会直接覆盖 `assets/widget` 与 `assets/modelrepo`。想换角色就换参数重建，app 代码不用动。
界面上的人物/衣服下拉清单读的就是这些文件，换资源后名字自动跟着变。

## 构建环境

需要手机上的 Debian 环境（本项目就在其中开发）：

| 组件 | 位置 |
|---|---|
| JDK | `/workspace/tools/jdk`（Corretto 21 aarch64） |
| android.jar | `/workspace/tools/platform/android.jar`（API 37.0） |
| d8 / apksigner | `/workspace/tools/build-tools`（Java 版可用） |
| aapt / aapt2 / zipalign | `/usr/bin`（apt 的 arm64 版；build-tools 自带的是 x86_64，跑不了） |
| 框架资源表 | `/usr/share/android-framework-res/framework-res.apk`（apt 包 `android-framework-res`） |

```sh
node make_assets.js --fghrsh Potion-Maker/Pio   # 生成资源
sh build.sh                                      # 打包 → /sdcard/Live2D_看板娘app/apk/kanban.apk
```

`build.sh` 会先跑 `check_page.py` 校验拼页面 JS，再依次 aapt → javac → d8 → 合成 → zipalign → 签名；
本机 aapt2 是 x86_64 跑不了时会自动回退 Debian 的 arm64 `aapt`。

## 安装到手机

```sh
cp /sdcard/Live2D_看板娘app/apk/kanban.apk /data/local/tmp/kanban.apk
pm install -r /data/local/tmp/kanban.apk
appops set com.eta.kanban SYSTEM_ALERT_WINDOW allow
pm grant com.eta.kanban android.permission.POST_NOTIFICATIONS
dumpsys deviceidle whitelist +com.eta.kanban
```

## 使用

打开「看板娘」App，主界面分四块：

1. **状态卡**：悬浮窗/通知/电池优化权限、守护服务状态、在场/收起/穿透、
   当前人物与衣服名、尺寸/停靠/透明度，一眼看全
2. **权限引导**：悬浮窗 / 通知 / 电池优化三个按钮，已授予的会提示，不用重复点
3. **悬浮窗控制**：显示 / 收起 / 重载 / 点击穿透 / 回默认位置 / 退出服务
4. **快捷换装**：「下一个人物」「下一套衣服」即时生效；或下拉按真实名字
   选人物 + 衣服点「换上」（清单直接读资源包里的 `model_list.json` / `textures.cache`）

悬浮窗上：

- **拖动**：长按看板娘 0.8 秒（震动提示）后按住拖动；位置自动记住，默认贴屏幕下边
- **换人 / 换装**：悬浮窗工具栏上操作，选择会自动记住（页面把 localStorage 里的
  modelId / modelTexturesId 同步回原生设置，App 主界面和重载后都不会回退）；也可以在
  App 主界面的「快捷换装」里按名字选
- **工具栏**：从上到下依次是 点击穿透 / 换装 / 换人 / 一言 / 截图 / 关闭（顺序与开关见设置）
- **点击穿透**：点工具栏最上面那个「禁止」图标切换，开启时图标变暖黄色；此时她收不到触摸，工具栏也点不到
- **怎么切回来**：① 通知栏那条通知要先展开（点右侧小箭头）才看得到「恢复可交互」按钮；
  ② 或者直接打开看板娘 App（点通知本体或桌面图标）—— 进 App 会自动关掉穿透并弹提示
- **气泡**：点「一言」弹一句台词，气泡贴在窗口顶部、宽度自适应，不会被窗口边缘裁掉
- **关闭**：工具栏的关闭按钮 → 窗口整体消失，通知变"已收起"，可再唤回
- **守护**：服务被系统回收会自动拉起；开机按设置自启；熄屏时暂停渲染省电

## 踩过的坑

1. `platform-37` 的 `android.jar` **没有 resources.arsc**，aapt2 报 "No resource identifier found" →
   用 apt 装 `android-framework-res`，链接时 `-I /usr/share/android-framework-res/framework-res.apk`
2. 官方 build-tools 里的 `aapt2`/`zipalign` 是 **x86_64**，arm64 上 `Exec format error` → 用 Debian 源的
   arm64 版本；apt 源里没有 `aapt2` 包，`build.sh` 会自动回退到 `aapt`（参数兼容）
3. 悬浮窗用 `WRAP_CONTENT` 会量出 **0×0**（内容用绝对定位，不贡献内在尺寸）→ 必须给显式像素尺寸
4. 窗口贴在最顶上时，拖动手柄会落在状态栏的触摸区里被吃掉 → 位置下限设为状态栏高度
5. `pm install` 不能直接从 `/sdcard` 装（system_server 读不了 FUSE）→ 先 `cp` 到 `/data/local/tmp`
6. 文件名里的空格会被 URL 编码成 `%20`，拦截器要解码，否则带空格的动作文件全部 404
7. 长任务别用一次性异步任务（随运行结束被回收）→ 用常驻任务 + 断点续传

## 许可（别搞错）

打包/使用到的组件与各自许可，逐条列清：

| 组件 | 来源 | 许可 |
|---|---|---|
| 本仓库自己的代码（`src/`、`build.sh` 等） | 本项目 | 自用随意 |
| 引擎：`assets/widget/waifu-tips.js`、`chunk/*`、`waifu.css`、`waifu-tips.json` | [stevenjoezhang/live2d-widget](https://github.com/stevenjoezhang/live2d-widget) | **GPL-3.0**（不是 MIT） |
| `assets/widget/live2d.min.js`（Cubism 2 Core） | Live2D Cubism Core | Live2D 专有许可，属 **Redistributable Code**；文件头即注明 "5.1 Right of Use and Redistribution — The Customer may copy and distribute the Redistributable Code" |
| 模型：`assets/modelrepo/`（当前 Potion-Maker/Pio） | fghrsh/live2d_api 等 | 角色素材权利归原作者/发行方，**仅限个人学习使用** |

要点：

- **自己手机自用**：无碍。
- **要分发这个 App**：因为打包了 GPL-3.0 的 live2d-widget，整体需按 GPL-3.0 提供对应源码（本仓库可以承担）；模型素材要逐个确认授权；商用还需看 Live2D 的发行许可条款。
- live2d-widget 的 README 明确说了**不能包含 Cubism SDK for Web 的源码**（许可限制），本项目也没包含——只用了允许再分发的 Cubism 2 Core。
- 模型文件不入 git（`.gitignore` 里排除 `assets/modelrepo/`），需要时用 `make_assets.js` 重新生成。
