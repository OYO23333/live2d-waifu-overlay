---
name: android-overlay-app
description: 开发或修改「悬浮窗 + WebView」类 Android 应用，并在没有 Gradle 的条件下手工构建 APK。当任务涉及 aapt2/d8/apksigner 手工打包、SYSTEM_ALERT_WINDOW 悬浮窗、前台服务保活、WebView 加载本地 assets、点击穿透、长按拖动、或排查"悬浮窗不显示 / 看不见 / 位置错乱 / 装不上"时使用。
---

# Android 悬浮窗 WebView 应用

参考实现（可直接读）：
- 手机上：`/sdcard/Live2D_看板娘app/`（APK + 资源 + 源码快照 + `tmp/`）
- Debian 工程真身：`/workspace/kanban/`（`build.sh`、`src/com/eta/kanban/*.java`）

## 主流程

1. 改代码：`/workspace/kanban/src/com/eta/kanban/`
2. 打包：`cd /workspace/kanban && sh build.sh` → 产物自动落到 `/sdcard/Live2D_看板娘app/apk/kanban.apk`
3. 安装：`cp /sdcard/.../kanban.apk /data/local/tmp/ && pm install -r /data/local/tmp/kanban.apk`
   — **不能直接从 `/sdcard` 装**（system_server 读不了 FUSE）
4. 起服务：`am start-foreground-service -n <pkg>/.OverlayService -a <pkg>.action.SHOW`
5. 验证：读文件日志（见下）；截图必须用 **root `screencap`**

## 铁律（每条都是踩出来的）

- **悬浮窗尺寸必须显式给像素**：`WRAP_CONTENT` 会量出 0×0（内容绝对定位不贡献尺寸），窗口存在但什么都不显示。
- 窗口 `y` 下限 ≥ 状态栏高度，否则放在顶部的拖动手柄会被状态栏吃掉触摸。
- `lp.softInputMode = SOFT_INPUT_ADJUST_NOTHING`，否则输入法一弹出悬浮窗就被推走。
- 点击穿透（`FLAG_NOT_TOUCHABLE`）**必须同时提供"切回来"的入口**（通知栏动作 / App 按钮），否则再也点不到它。
- 长按拖动要**排除工具栏区域**（由页面把 rect 报给原生层），否则点按钮会被长按逻辑吃掉。
- 悬浮窗被系统回收要用**守护闹钟 + enabled/visible 两个状态**（`START_STICKY` 在厂商 ROM 上不可靠）。
- WebView 里加载本地资源：`loadDataWithBaseURL("https://app.local/", …)` + `shouldInterceptRequest` 映射 assets → 完全离线；模块脚本的相对 import 会按同源路径解析。

## 参考

- `references/manual-apk-build.md` — 手工打包完整步骤、工具来源、坑（aapt2 缺框架资源表、x86_64 二进制、dex 合并）
- `references/overlay-gotchas.md` — 窗口行为、触摸、输入法、保活、位置持久化清单
- `references/observability.md` — 日志与验证手段，以及"看不见界面"的标准排查顺序
- `scripts/build-apk.sh` — 构建脚本模板（改路径即可用）
