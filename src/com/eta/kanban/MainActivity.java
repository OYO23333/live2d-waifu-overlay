package com.eta.kanban;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private TextView status;
    private Button btnTouch;
    /** 每次进主界面只自动清一次穿透，避免 recreate 反复触发 */
    private boolean autoCleared;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        L.init(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 64, 48, 48);

        TextView title = new TextView(this);
        title.setText("桌面看板娘");
        title.setTextSize(26);
        root.addView(title);

        status = new TextView(this);
        status.setTextSize(14);
        status.setPadding(0, 24, 0, 24);
        root.addView(status);

        root.addView(button("显示看板娘", new Runnable() {
            public void run() {
                if (!overlayGranted()) {
                    Toast.makeText(MainActivity.this, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show();
                    return;
                }
                send(OverlayService.ACTION_SHOW);
            }
        }));

        root.addView(button("收起看板娘", new Runnable() {
            public void run() { send(OverlayService.ACTION_HIDE); }
        }));

        root.addView(button("重新加载（应用最新设置）", new Runnable() {
            public void run() { send(OverlayService.ACTION_RELOAD); }
        }));

        root.addView(button("设置", new Runnable() {
            public void run() { startActivity(new Intent(MainActivity.this, SettingsActivity.class)); }
        }));

        root.addView(button("① 授予悬浮窗权限", new Runnable() {
            public void run() {
                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(i);
            }
        }));

        root.addView(button("② 授予通知权限（Android 13+）", new Runnable() {
            public void run() {
                if (Build.VERSION.SDK_INT >= 33) {
                    requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
                } else {
                    Toast.makeText(MainActivity.this, "当前系统无需手动授予", Toast.LENGTH_SHORT).show();
                }
            }
        }));

        root.addView(button("③ 关闭电池优化（防被冻结）", new Runnable() {
            public void run() {
                try {
                    Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(i);
                } catch (Throwable t) {
                    startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                }
            }
        }));

        root.addView(button("恢复默认外观与位置", new Runnable() {
            public void run() {
                Intent i = new Intent(MainActivity.this, OverlayService.class);
                i.setAction(OverlayService.ACTION_RESET_POS);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(i);
                } else {
                    startService(i);
                }
            }
        }));

        btnTouch = button("切换点击穿透（当前：" + (Prefs.touchThrough(this) ? "开" : "关") + "）",
                new Runnable() {
            public void run() {
                Intent i = new Intent(MainActivity.this, OverlayService.class);
                i.setAction(OverlayService.ACTION_TOGGLE_TOUCH);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(i);
                } else {
                    startService(i);
                }
                status.postDelayed(new Runnable() {
                    public void run() { recreate(); }
                }, 400);
            }
        });
        root.addView(btnTouch);

        root.addView(button("退出服务", new Runnable() {
            public void run() { send(OverlayService.ACTION_EXIT); }
        }));

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);
    }

    private Button button(String text, final Runnable action) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { action.run(); }
        });
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = 18;
        b.setLayoutParams(p);
        return b;
    }

    private void send(String action) {
        Intent i = new Intent(this, OverlayService.class);
        i.setAction(action);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(i);
        } else {
            startService(i);
        }
    }

    private boolean overlayGranted() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        StringBuilder sb = new StringBuilder();
        sb.append("悬浮窗权限：").append(overlayGranted() ? "已授予" : "未授予");
        sb.append("\n通知权限：").append(notifGranted() ? "已授予" : "未授予");
        sb.append("\n电池优化：").append(ignoringBattery() ? "已忽略（好）" : "仍受限");
        sb.append("\n守护状态：服务").append(OverlayService.isRunning() ? "在跑" : "未运行");
        sb.append("，用户期望=").append(Prefs.enabled(this) ? "在场" : "停用");
        sb.append("，当前").append(Prefs.visible(this) ? "显示" : "收起");
        sb.append("\n当前设置：尺寸=").append(Prefs.sizeW(this)).append("x").append(Prefs.sizeH(this))
          .append("  停靠=").append(Prefs.rightSide(this) ? "右" : "左")
          .append("  透明度=").append(Prefs.alpha(this)).append("%");
        sb.append("\n穿透=").append(Prefs.touchThrough(this) ? "开（纯装饰）" : "关（可交互）");
        status.setText(sb.toString());

        // 打开 App 说明想跟她互动：穿透还开着就自动关掉（免得"摸不到她"又找不到开关）
        if (!autoCleared && Prefs.touchThrough(this) && OverlayService.isRunning()) {
            autoCleared = true;
            OverlayService.clearTouchIfOn(this);
            status.postDelayed(new Runnable() {
                public void run() { recreate(); }
            }, 700);
        }
    }

    private boolean notifGranted() {
        if (Build.VERSION.SDK_INT < 33) return true;
        return checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private boolean ignoringBattery() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
    }
}
