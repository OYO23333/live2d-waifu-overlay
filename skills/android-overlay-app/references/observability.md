# 日志与验证手段

## 日志：别只依赖 logcat

实测某些 ROM（ColorOS 类）会**丢弃后台应用的 logcat 输出**——全缓冲区里关键字一条都没有，而进程明明在跑。
因此日志同时写一份文件，落在**外部私有目录**（调试 shell 读得到）：

```java
File dir = c.getExternalFilesDir(null);          // /sdcard/Android/data/<pkg>/files/
file = new File(dir != null ? dir : c.getFilesDir(), "kanban.log");
```

- **不要**写 `/data/user/0/<pkg>/files/`：调试 shell 常在自己的挂载命名空间里，看不到别的 app 的私有目录（读会显示 "No such file"，让人误判）。
- 超过 512KB 轮转一份 `.1`。
- 页面里的 `console.log`/错误要接出来：`WebChromeClient.onConsoleMessage` → 同一份日志。

## 页面自报状态（WebView 里的黑盒）

在页面注入一段定时上报，排查渲染问题极有效：

```js
setInterval(function(){
  function info(el){ if(!el) return 'null';
    var r=el.getBoundingClientRect(), cs=getComputedStyle(el);
    return Math.round(r.width)+'x'+Math.round(r.height)+' d='+cs.display+' v='+cs.visibility+' o='+cs.opacity; }
  Android.log('DOM '+info(document.getElementById('waifu')));
}, 4000);
```

再配一个 rAF 帧计数（`(function t(){window.__frames++;requestAnimationFrame(t)})()`），就能区分"JS 停了"和"画了但没合成"。

## 截图的坑

- **无障碍截图（`observe_screen` 这类）可能不含悬浮窗**（`expected_windows=1`，只抓到前台 app 的窗口），据此判断"看不显示"会得出完全错误的结论。
- 要看真实合成结果，用 **root `screencap -p <path>`**，然后把图片读出来看。
- 临时截图**放进工程目录的 `tmp/`**，不要丢在共享存储根目录。

## 常用检查点

```sh
dumpsys window windows | grep -A3 <pkg>         # 窗口几何/flags/是否 HAS_DRAWN
dumpsys activity services <pkg>                  # isForeground / startRequested
dumpsys activity processes | grep -E "curProcState|oom adj"   # 进程是否被降级成 cached
appops get <pkg> SYSTEM_ALERT_WINDOW            # 悬浮窗权限
dumpsys input_method | grep -E "mInputShown|mWindowVisible"   # 输入法是否弹着（会盖住下半屏）
```

## 自己驱动 GUI 调试时的两个坑

- 无障碍注入的 **tap 手势按住时间偏长**（近 1 秒）会被浏览器当成长按/上下文菜单 → 想模拟"轻点"要用极短的手势（如 100ms、位移 1px）。
- 注入坐标与实际落点可能有**状态栏高度的偏移**，别硬算：让页面把点击落点（`clientX/clientY` 和命中元素）打日志出来，用实测值校准。
