package com.eta.kanban;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
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

    private static final String CHANNEL = "kanban_overlay";
    private static final int NOTI_ID = 1001;

    private WindowManager wm;
    private FrameLayout container;
    private WebView web;
    private View handle;
    private WindowManager.LayoutParams lp;
    private boolean attached = false;
    private int screenW, screenH;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics m = getResources().getDisplayMetrics();
        screenW = m.widthPixels;
        screenH = m.heightPixels;
        startForeground(NOTI_ID, buildNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_SHOW : intent.getAction();
        if (action == null) action = ACTION_SHOW;

        switch (action) {
            case ACTION_HIDE:
                detach();
                updateNotification();
                break;
            case ACTION_RELOAD:
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
                detach();
                stopForeground(true);
                stopSelf();
                break;
            default:
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
        web.loadDataWithBaseURL("https://app.local/", Page.html(this), "text/html", "utf-8", null);
        container.addView(web, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // 拖动手柄（长按拖动这个方块即可挪位置）
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

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        lp = new WindowManager.LayoutParams(0, 0, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
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
        // 看板娘用 position:fixed 定位，不贡献内在尺寸，必须给显式像素大小
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
        // 不能贴在状态栏底下，否则拖动手柄的触摸会被状态栏吃掉
        lp.y = Math.max(dp(Prefs.edgeY(this)), statusBarHeight());
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
                    boolean rightSide = (lp.x + lp.width / 2) > screenW / 2;
                    Prefs.set(OverlayService.this, "rightSide", rightSide);
                    int edge = rightSide ? Math.max(0, screenW - lp.width - lp.x) : Math.max(0, lp.x);
                    Prefs.set(OverlayService.this, "edgeX", px2dp(edge));
                    Prefs.set(OverlayService.this, "edgeY", px2dp(lp.y));
                    android.util.Log.i("Kanban", "拖动保存 侧=" + (rightSide ? "右" : "左")
                            + " edgeX=" + px2dp(edge) + "dp edgeY=" + px2dp(lp.y) + "dp");
                    return true;
                }
            }
            return false;
        }
    };

    // ---------- 通知 ----------

    private Notification buildNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL, "看板娘悬浮窗", NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }

        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        Intent toggleIntent = new Intent(this, OverlayService.class)
                .setAction(attached ? ACTION_HIDE : ACTION_SHOW);
        Intent exitIntent = new Intent(this, OverlayService.class).setAction(ACTION_EXIT);

        PendingIntent piSettings = PendingIntent.getActivity(this, 1, settingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent piToggle = PendingIntent.getService(this, 2, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent piExit = PendingIntent.getService(this, 3, exitIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL)
                : new Notification.Builder(this);

        return b.setContentTitle("看板娘")
                .setContentText(attached ? "正在陪你（点通知可设置）" : "已收起（点这里唤回）")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentIntent(piSettings)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(null,
                        attached ? "收起" : "显示", piToggle).build())
                .addAction(new Notification.Action.Builder(null, "退出", piExit).build())
                .build();
    }

    private void updateNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTI_ID, buildNotification());
    }

    @Override
    public void onDestroy() {
        detach();
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
            detach();
            updateNotification();
        }

        @JavascriptInterface
        public void log(String msg) {
            android.util.Log.i("Kanban", msg);
        }
    }
}
