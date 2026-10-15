package com.eta.kanban;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * 守护：周期性检查"用户期望看板娘在场"是否仍然成立，
 * 如果服务被系统回收就把它拉回来（Android 12+ 若不允许后台启动前台服务，退化成通知）。
 */
public class WatchdogReceiver extends BroadcastReceiver {

    static final String CHANNEL = "kanban_watchdog";
    static final int NOTIFY_ID = 2003;

    @Override
    public void onReceive(Context ctx, Intent intent) {
        L.init(ctx);
        if (!Prefs.enabled(ctx)) {
            L.i("Kanban", "守护：用户已停用，不拉起");
            return;
        }
        if (OverlayService.isRunning()) {
            L.i("Kanban", "守护：服务在跑，无需处理");
            return;
        }

        Intent svc = new Intent(ctx, OverlayService.class);
        svc.setAction(Prefs.visible(ctx) ? OverlayService.ACTION_SHOW : OverlayService.ACTION_KEEPALIVE);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(svc);
            } else {
                ctx.startService(svc);
            }
            L.i("Kanban", "守护：已重新拉起悬浮窗服务");
        } catch (Throwable t) {
            L.w("Kanban", "守护：后台启动被拦，退化为通知：" + t);
            notifyFallback(ctx);
        }
    }

    private void notifyFallback(Context ctx) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL, "看板娘守护提示", NotificationManager.IMPORTANCE_DEFAULT));
        }
        Intent open = new Intent(ctx, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(ctx, 10, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(ctx, CHANNEL)
                : new Notification.Builder(ctx);
        nm.notify(NOTIFY_ID, b.setContentTitle("看板娘掉线了")
                .setContentText("点这里把她叫回来")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build());
    }

    /** 由服务调用：注册周期性守护（15 分钟一次，系统可能延后，这是有意的省电取舍） */
    static void schedule(Context ctx) {
        android.app.AlarmManager am =
                (android.app.AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(ctx, WatchdogReceiver.class).setAction("com.eta.kanban.WATCHDOG");
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 11, i, flags);
        long interval = 15 * 60 * 1000L;
        try {
            am.setInexactRepeating(android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    System.currentTimeMillis() + interval, interval, pi);
            L.i("Kanban", "守护闹钟已注册（15 分钟一次）");
        } catch (Throwable t) {
            L.w("Kanban", "守护闹钟注册失败：" + t);
        }
    }
}
