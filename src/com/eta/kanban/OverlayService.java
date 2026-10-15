package com.eta.kanban;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.TextView;

public class OverlayService extends Service {

    public static final String ACTION_SHOW = "com.eta.kanban.action.SHOW";
    public static final String ACTION_HIDE = "com.eta.kanban.action.HIDE";
    public static final String ACTION_RELOAD = "com.eta.kanban.action.RELOAD";
    public static final String ACTION_EXIT = "com.eta.kanban.action.EXIT";
    /** 守护专用：只在"用户期望显示"时才挂窗口，服务本身要保持存活 */
    public static final String ACTION_KEEPALIVE = "com.eta.kanban.action.KEEPALIVE";
    /** 切换点击穿透（开着的时候她收不到触摸，通知栏必须有入口切回来） */
    public static final String ACTION_TOGGLE_TOUCH = "com.eta.kanban.action.TOGGLE_TOUCH";

    private static final String CHANNEL = "kanban_overlay";
    private static final int NOTI_ID = 1001;

    private static volatile boolean sRunning = false;

    public static boolean isRunning() {
        return sRunning;
    }

    private WindowManager wm;
    private FrameLayout container;
    private WebView web;
    private View handle;
    private WindowManager.LayoutParams lp;
    private boolean attached = false;
    private int screenW, screenH;

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String a = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(a)) {
                // 熄屏就别再跑渲染了，省电
                if (web != null) {
                    web.onPause();
                    web.pauseTimers();
                }
                android.util.Log.i("Kanban", "熄屏：已暂停渲染");
            } else if (Intent.ACTION_SCREEN_ON.equals(a)) {
                if (web != null) {
                    web.resumeTimers();
                    web.onResume();
                }
                android.util.Log.i("Kanban", "亮屏：已恢复渲染");
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sRunning = true;
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        updateScreenSize();
        startForeground(NOTI_ID, buildNotification());
        WatchdogReceiver.schedule(this);

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_OFF);
        f.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(screenReceiver, f);
    }

    private void updateScreenSize() {
        DisplayMetrics m = getResources().getDisplayMetrics();
        screenW = m.widthPixels;
        screenH = m.heightPixels;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_SHOW : intent.getAction();
        if (action == null) action = ACTION_SHOW;
        startForeground(NOTI_ID, buildNotification());

        switch (action) {
            case ACTION_HIDE:
                Prefs.setVisible(this, false);
                detach();
                updateNotification();
                break;
            case ACTION_RELOAD:
                Prefs.setVisible(this, true);
                if (attached && web != null) {
                    applyWindowFlags();
                    resetPosition();
                    web.loadDataWithBaseURL("https://app.local/", Page.html(this),
                            "text/html", "utf-8", null);
                } else {
                    attach();
                }
                break;
            case ACTION_EXIT:
                Prefs.setEnabled(this, false);
                Prefs.setVisible(this, false);
                detach();
                stopForeground(true);
                stopSelf();
                break;
            case ACTION_KEEPALIVE:
                // 只是保活：用户没要求显示就不要弹出来
                if (Prefs.visible(this)) attach();
                updateNotification();
                break;
            case ACTION_TOGGLE_TOUCH: {
                boolean now = !Prefs.touchThrough(this);
                Prefs.set(this, "touchThrough", now);
                applyWindowFlags();
                updateNotification();
                android.util.Log.i("Kanban", "点击穿透 = " + now
                        + (now ? "（她现在纯装饰，触摸会穿透到下面的应用）" : "（恢复可交互）"));
                break;
            }
            default: // ACTION_SHOW
                Prefs.setEnabled(this, true);
                Prefs.setVisible(this, true);
                attach();
                break;
        }
        return START_STICKY;
    }

    // ---------- 视图 ----------

    private void buildViews() {
        container = new FrameLayout(this);

        web = new WebView(this);
        web.setBackgroundColor(0x00000000);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setSupportZoom(false);
        web.setVerticalScrollBarEnabled(false);
        web.setHorizontalScrollBarEnabled(false);
        web.setWebViewClient(new LocalAssets(this));
        web.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage m) {
                android.util.Log.i("KanbanJS", m.message() + " @" + m.lineNumber());
                return true;
            }
        });
        web.addJavascriptInterface(new Bridge(), "Android");
        web.setOnTouchListener(webTouch);   // 长按拖动靠它实现
        web.loadDataWithBaseURL("https://app.local/", Page.html(this), "text/html", "utf-8", null);
        container.addView(web, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        TextView h = new TextView(this);
        h.setText("≡");
        h.setTextSize(16);
        h.setTextColor(0xCCFFFFFF);
        h.setGravity(Gravity.CENTER);
        h.setBackgroundColor(0x77000000);
        h.setVisibility(Prefs.showHandle(this) ? View.VISIBLE : View.GONE);
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(dp(30), dp(30));
        hp.gravity = Gravity.TOP | Gravity.START;
        container.addView(h, hp);
        handle = h;
        handle.setOnTouchListener(dragListener);
    }

    // ---------- 窗口 ----------

    private void attach() {
        if (attached) return;
        if (container == null) buildViews();

        // 重新挂窗口时重载一次页面：既清掉"上次关过"的状态，也保证她一定露面
        if (web != null) {
            web.loadDataWithBaseURL("https://app.local/", Page.html(this),
                    "text/html", "utf-8", null);
        }
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        if (lp == null) {
            lp = new WindowManager.LayoutParams(0, 0, type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                            | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    PixelFormat.TRANSLUCENT);
            lp.gravity = Gravity.TOP | Gravity.START;
        }
        applyWindowFlags();
        resetPosition();

        try {
            wm.addView(container, lp);
            attached = true;
            android.util.Log.i("Kanban", "attach ok " + lp.width + "x" + lp.height
                    + " at " + lp.x + "," + lp.y + " touchThrough=" + Prefs.touchThrough(this));
        } catch (Throwable t) {
            attached = false;
            android.util.Log.e("Kanban", "attach failed: " + t);
        }
        updateNotification();
    }

    private void applyWindowFlags() {
        if (lp == null) return;
        int base = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        if (Prefs.touchThrough(this)) {
            base |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        }
        lp.flags = base;
        lp.width = dp(Prefs.sizeW(this) + 30);
        lp.height = dp(Prefs.sizeH(this) + 60);
        if (attached && container != null) {
            try {
                wm.updateViewLayout(container, lp);
            } catch (Throwable ignored) {
            }
        }
    }

    private int statusBarHeight() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        int h = id > 0 ? getResources().getDimensionPixelSize(id) : dp(28);
        return Math.max(h, dp(28));
    }

    private void resetPosition() {
        if (lp == null) return;
        int edge = dp(Prefs.edgeX(this));
        if (Prefs.anchorTop(this)) {
            lp.y = Math.max(statusBarHeight(), dp(Prefs.edgeY(this)));
        } else {
            // 贴底边：默认就在屏幕下沿，符合"看板娘站在桌面上"的直觉
            lp.y = screenH - lp.height - dp(Prefs.edgeY(this));
            lp.y = Math.max(statusBarHeight(), lp.y);
        }
        lp.x = Prefs.rightSide(this) ? Math.max(0, screenW - lp.width - edge) : edge;
        if (attached && container != null) {
            try {
                wm.updateViewLayout(container, lp);
            } catch (Throwable ignored) {
            }
        }
    }

    private void detach() {
        if (!attached || container == null) return;
        try {
            wm.removeView(container);
        } catch (Throwable ignored) {
        }
        attached = false;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private int px2dp(int v) {
        return Math.round(v / getResources().getDisplayMetrics().density);
    }

    // ---------- 拖动 ----------

    private final Handler ui = new Handler(Looper.getMainLooper());
    private boolean dragging = false;
    private boolean dragArmed = false;
    private float touchDownX, touchDownY;
    private float lastX, lastY;
    private int dragStartX, dragStartY;

    /** 长按人物 → 进入拖动模式（震动提示），之后按住即可移动窗口 */
    private static final long LONG_PRESS_MS = 800;

    /** 工具栏在窗口内的区域（设备像素），落在里面就不启动长按，免得点 X 被吃掉 */
    private volatile float[] toolbarRect = null;

    private boolean inToolbar(float rawX, float rawY) {
        float[] r = toolbarRect;
        if (r == null || lp == null) return false;
        float x = rawX - lp.x;
        float y = rawY - lp.y;
        float pad = dp(10);
        return x >= r[0] - pad && x <= r[0] + r[2] + pad
                && y >= r[1] - pad && y <= r[1] + r[3] + pad;
    }

    private final Runnable longPress = new Runnable() {
        @Override
        public void run() {
            dragging = true;
            dragArmed = false;          // 先等第一次真正的移动再锚定，避免手指抖动把窗口带跑
            vibrate();
            android.util.Log.i("Kanban", "长按触发：进入拖动模式");
        }
    };

    private final View.OnTouchListener webTouch = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent e) {
            if (lp == null) return false;
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    dragging = false;
                    touchDownX = e.getRawX();
                    touchDownY = e.getRawY();
                    lastX = touchDownX;
                    lastY = touchDownY;
                    if (inToolbar(touchDownX, touchDownY)) {
                        // 工具栏上的触摸交给页面处理（点 X / 换装 / 换人）
                        return false;
                    }
                    ui.postDelayed(longPress, LONG_PRESS_MS);
                    return false;   // 不消费，看板娘自己的交互照旧
                case MotionEvent.ACTION_MOVE:
                    lastX = e.getRawX();
                    lastY = e.getRawY();
                    if (dragging) {
                        if (!dragArmed) {
                            // 第一次真正移动时重新锚定，消掉长按期间的指尖抖动
                            if (Math.abs(lastX - touchDownX) > dp(6)
                                    || Math.abs(lastY - touchDownY) > dp(6)) {
                                dragArmed = true;
                                touchDownX = lastX;
                                touchDownY = lastY;
                                dragStartX = lp.x;
                                dragStartY = lp.y;
                            } else {
                                return true;
                            }
                        }
                        int nx = (int) (dragStartX + (lastX - touchDownX));
                        int ny = (int) (dragStartY + (lastY - touchDownY));
                        lp.x = Math.max(-lp.width / 2, Math.min(screenW - lp.width / 2, nx));
                        lp.y = Math.max(statusBarHeight(), Math.min(screenH - dp(40), ny));
                        try {
                            wm.updateViewLayout(container, lp);
                        } catch (Throwable ignored) {
                        }
                        return true;  // 拖动期间吃掉后续事件，免得页面跟着乱动
                    }
                    // 还没触发长按就移动了：说明用户是在滑动/点她，取消长按
                    if (Math.abs(lastX - touchDownX) > dp(12)
                            || Math.abs(lastY - touchDownY) > dp(12)) {
                        ui.removeCallbacks(longPress);
                    }
                    return false;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    ui.removeCallbacks(longPress);
                    if (dragging) {
                        dragging = false;
                        savePosition();
                        return true;
                    }
                    return false;
            }
            return false;
        }
    };

    private void vibrate() {
        try {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v == null) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(30);
            }
        } catch (Throwable ignored) {
        }
    }

    private void savePosition() {
        if (lp == null) return;
        boolean rightSide = (lp.x + lp.width / 2) > screenW / 2;
        Prefs.set(this, "rightSide", rightSide);
        int edge = rightSide ? Math.max(0, screenW - lp.width - lp.x) : Math.max(0, lp.x);
        boolean anchorTop = (lp.y + lp.height / 2) < screenH / 2;
        int edgeY = anchorTop ? Math.max(0, lp.y)
                : Math.max(0, screenH - lp.height - lp.y);
        Prefs.set(this, "edgeX", px2dp(edge));
        Prefs.set(this, "edgeY", px2dp(edgeY));
        Prefs.set(this, "anchorTop", anchorTop);
        android.util.Log.i("Kanban", "位置已保存 侧=" + (rightSide ? "右" : "左")
                + " " + (anchorTop ? "上锚定" : "下锚定")
                + " edgeX=" + px2dp(edge) + "dp edgeY=" + px2dp(edgeY) + "dp");
    }

    private final View.OnTouchListener dragListener = new View.OnTouchListener() {
        private float downX, downY;
        private int startX, startY;

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = e.getRawX();
                    downY = e.getRawY();
                    startX = lp.x;
                    startY = lp.y;
                    return true;
                case MotionEvent.ACTION_MOVE: {
                    int nx = (int) (startX + (e.getRawX() - downX));
                    int ny = (int) (startY + (e.getRawY() - downY));
                    lp.x = Math.max(-lp.width / 2, Math.min(screenW - lp.width / 2, nx));
                    lp.y = Math.max(statusBarHeight(), Math.min(screenH - dp(40), ny));
                    try {
                        wm.updateViewLayout(container, lp);
                    } catch (Throwable ignored) {
                    }
                    return true;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    savePosition();
                    return true;
                }
            }
            return false;
        }
    };

    // ---------- 生命周期 ----------

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateScreenSize();
        if (attached) {
            applyWindowFlags();
            resetPosition();
            android.util.Log.i("Kanban", "配置变化：重算尺寸与位置 " + screenW + "x" + screenH);
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        android.util.Log.w("Kanban", "任务被划掉，enabled=" + Prefs.enabled(this));
        if (Prefs.enabled(this)) {
            WatchdogReceiver.schedule(this);
        }
        super.onTaskRemoved(rootIntent);
    }

    // ---------- 通知 ----------

    private Notification buildNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL, "看板娘悬浮窗", NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }

        boolean showing = attached && Prefs.visible(this);
        boolean through = Prefs.touchThrough(this);
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        Intent toggleIntent = new Intent(this, OverlayService.class)
                .setAction(showing ? ACTION_HIDE : ACTION_SHOW);
        Intent exitIntent = new Intent(this, OverlayService.class).setAction(ACTION_EXIT);
        Intent touchIntent = new Intent(this, OverlayService.class).setAction(ACTION_TOGGLE_TOUCH);

        PendingIntent piSettings = PendingIntent.getActivity(this, 1, settingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent piToggle = PendingIntent.getService(this, 2, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent piExit = PendingIntent.getService(this, 3, exitIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent piTouch = PendingIntent.getService(this, 4, touchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL)
                : new Notification.Builder(this);

        return b.setContentTitle("看板娘")
                .setContentText(showing
                        ? (through ? "正在陪你（点击穿透中，摸不到她）" : "正在陪你（点通知可设置）")
                        : "已收起（点这里唤回）")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentIntent(piSettings)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(null,
                        through ? "恢复可交互" : "点击穿透", piTouch).build())
                .addAction(new Notification.Action.Builder(null,
                        showing ? "收起" : "显示", piToggle).build())
                .addAction(new Notification.Action.Builder(null, "退出", piExit).build())
                .build();
    }

    private void updateNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTI_ID, buildNotification());
    }

    @Override
    public void onDestroy() {
        sRunning = false;
        detach();
        try {
            unregisterReceiver(screenReceiver);
        } catch (Throwable ignored) {
        }
        if (web != null) {
            web.destroy();
            web = null;
        }
        container = null;
        super.onDestroy();
    }

    public class Bridge {
        @JavascriptInterface
        public void hide() {
            android.util.Log.i("Kanban", "Bridge.hide() 被调用（关闭按钮生效）");
            Prefs.setVisible(OverlayService.this, false);
            detach();
            updateNotification();
        }

        @JavascriptInterface
        public void setToolbarRect(float left, float top, float w, float h) {
            float d = getResources().getDisplayMetrics().density;
            toolbarRect = new float[]{left * d, top * d, w * d, h * d};
        }

        @JavascriptInterface
        public void log(String msg) {
            android.util.Log.i("Kanban", msg);
        }
    }
}
