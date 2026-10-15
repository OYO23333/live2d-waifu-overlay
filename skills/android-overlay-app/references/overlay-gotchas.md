# 悬浮窗行为与踩坑清单

## 窗口基本配置

```java
lp = new WindowManager.LayoutParams(w, h,                      // 必须显式像素
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        FLAG_NOT_FOCUSABLE | FLAG_LAYOUT_NO_LIMITS | FLAG_HARDWARE_ACCELERATED,
        PixelFormat.TRANSLUCENT);
lp.gravity = Gravity.TOP | Gravity.START;
lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;  // 防输入法推走
// 需要"纯装饰"时：lp.flags |= FLAG_NOT_TOUCHABLE;  但必须留个切回来的入口
```

- `WRAP_CONTENT` → 实测量出 **0×0**（页面用绝对定位，不贡献内在尺寸）→ 窗口在、屏幕空。
- 位置下限 = 状态栏高度（否则顶部控件落在状态栏的触摸区里，点不到）。
- 位置记两份语义：`rightSide`（左右）+ `anchorTop`（上/下锚定），拖动结束时按"就近边缘"存。

## 触摸

- 悬浮窗矩形内的触摸**默认被它吃掉**，透明区域也算。要"不挡手"就开 `FLAG_NOT_TOUCHABLE`。
- 想既能在她身上交互、又不挡其他地方：只能靠窗口开小 + 提供穿透开关，别指望系统支持"穿透透明像素"。
- 长按拖动（800ms + 震动提示）比可视手柄好看，但**必须排除工具栏区域**：让页面用 `getBoundingClientRect()` 把 rect 报给原生层，落在这块区域内的 DOWN 不启动长按。
- 长按触发后要**重新锚定**（记录触发瞬间的手指位置和窗口位置），否则按住时的手抖会把窗口带跑。

## 保活

- 前台服务 + 常驻通知是底线；`START_STICKY` 在厂商 ROM 上不可靠。
- 用**守护闹钟**（`AlarmManager.setInexactRepeating`，15 分钟）检查"用户期望在场（enabled）且服务不在跑"就拉起；被系统拦（Android 12+ 后台启动前台服务限制）就退化成通知提示。
- 两个状态要分开存：`enabled`（用户期望在场）/ `visible`（当前是否显示）。"收起"只改 visible，守护就不会把它又弹出来。
- 电池优化白名单：`dumpsys deviceidle whitelist +<pkg>`；开机自启用 `BOOT_COMPLETED`，同样要判 `enabled`。

## 省电与适配

- 收到 `ACTION_SCREEN_OFF` → `web.onPause(); web.pauseTimers();`，`ACTION_SCREEN_ON` 恢复。
- `onConfigurationChanged` 里重算尺寸与位置（旋转/分屏）。
- 输入法弹出**不会**推走窗口的前提是上面那个 `softInputMode`；否则她会被挤到屏幕中间。

## 排查"看不见"的顺序

1. 窗口在不在：`dumpsys window windows | grep <pkg>` → 看 `mAttrs`、`mHasSurface`、`isOnScreen`、`Surface: shown`、`mDrawState=HAS_DRAWN`。
2. 画布有没有内容：临时给容器/`#waifu` 加一块半透明红底，看屏幕有没有红块（区分"窗口没合成"和"内容没画"）。
3. 有没有被别人的窗口盖住：`dumpsys window windows` 看窗口顺序，注意 **`ACCESSIBILITY_OVERLAY` 层级高于 `APPLICATION_OVERLAY`**（例如 Eta 自己跑 GUI 自动化时的全屏遮罩会压住你的悬浮窗）。
4. 前台应用是谁：很多 ROM 会把"缓存状态（cached）"应用的悬浮窗隐藏 → 检查 `dumpsys activity processes` 里的 `curProcState`，配合 `isForeground=true` 判断前台服务是否还有效。
