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
 * 开机自启：Android 12+ 之后后台启动前台服务受限，
 * 所以先试着直接拉起，被系统拦下就退化成一条"点一下唤出"的通知。
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String CHANNEL = "kanban_boot";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                && !"android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) return;
        if (!Prefs.autoStart(ctx)) {
            android.util.Log.i("Kanban", "开机自启：用户关闭了该开关");
            return;
        }
        if (!Prefs.enabled(ctx)) {
            android.util.Log.i("Kanban", "开机自启：用户已停用看板娘，不拉起");
            return;
        }

        Intent svc = new Intent(ctx, OverlayService.class);
        svc.setAction(OverlayService.ACTION_SHOW);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(svc);
            } else {
                ctx.startService(svc);
            }
            android.util.Log.i("Kanban", "开机自启：已拉起悬浮窗服务");
        } catch (Throwable t) {
            android.util.Log.w("Kanban", "开机自启被系统拦截，退化为通知：" + t);
            notifyFallback(ctx);
        }
    }

    private void notifyFallback(Context ctx) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL, "看板娘自启提示", NotificationManager.IMPORTANCE_DEFAULT));
        }
        Intent open = new Intent(ctx, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(ctx, 9, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(ctx, CHANNEL)
                : new Notification.Builder(ctx);
        nm.notify(2002, b.setContentTitle("看板娘还没上岗")
                .setContentText("点这里把她叫出来")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build());
    }
}
