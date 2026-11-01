package com.eta.kanban;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 主界面：状态总览 + 权限引导 + 悬浮窗控制 + 快捷换人/换装。
 * 原来只是把按钮竖着堆一排，现在按"状态 → 权限 → 控制 → 换装"分区。
 */
public class MainActivity extends Activity {

    /** 悬浮窗工具栏点相机时拉起本界面弹系统截屏授权（透明主题，用户只见系统框） */
    public static final String ACTION_REQUEST_PROJECTION = "com.eta.kanban.action.REQUEST_PROJECTION";
    private static final int REQ_PROJECTION = 42;

    private TextView status;
    /** 每次进主界面只自动清一次穿透，避免 recreate 反复触发 */
    private boolean autoCleared;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        L.init(this);

        // 只是来弹截屏授权的：立刻弹系统框，不铺界面
        if (getIntent() != null
                && ACTION_REQUEST_PROJECTION.equals(getIntent().getAction())) {
            try {
                android.media.projection.MediaProjectionManager mpm =
                        (android.media.projection.MediaProjectionManager)
                                getSystemService(MEDIA_PROJECTION_SERVICE);
                startActivityForResult(mpm.createScreenCaptureIntent(), REQ_PROJECTION);
            } catch (Throwable t) {
                L.e("Kanban", "弹截屏授权失败：" + t);
                finish();
            }
            return;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 64, 48, 48);

        TextView title = new TextView(this);
        title.setText("桌面看板娘");
        title.setTextSize(26);
        root.addView(title);

        status = new TextView(this);
        status.setTextSize(14);
        status.setLineSpacing(6, 1.05f);
        status.setPadding(28, 20, 28, 20);
        status.setBackgroundColor(0x11000000);
        LinearLayout.LayoutParams sp = section(root, status);
        sp.topMargin = 20;

        // ---------- 权限 ----------
        root.addView(label("权限与守护"));
        LinearLayout permRow = row();
        permRow.addView(btn("① 悬浮窗权限", 1, new Runnable() {
            public void run() {
                if (overlayGranted()) {
                    Toast.makeText(MainActivity.this, "悬浮窗权限已授予", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(i);
            }
        }));
        permRow.addView(btn("② 通知权限", 1, new Runnable() {
            public void run() {
                if (Build.VERSION.SDK_INT >= 33) {
                    if (notifGranted()) {
                        Toast.makeText(MainActivity.this, "通知权限已授予", Toast.LENGTH_SHORT).show();
                    } else {
                        requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "当前系统无需手动授予", Toast.LENGTH_SHORT).show();
                }
            }
        }));
        root.addView(permRow);
        LinearLayout battRow = row();
        battRow.addView(btn("③ 关闭电池优化", 1, new Runnable() {
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
        root.addView(battRow);

        // ---------- 控制 ----------
        root.addView(label("悬浮窗"));
        LinearLayout ctlRow = row();
        ctlRow.addView(btn("显示", 1, new Runnable() {
            public void run() {
                if (!overlayGranted()) {
                    Toast.makeText(MainActivity.this, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show();
                    return;
                }
                send(OverlayService.ACTION_SHOW);
            }
        }));
        ctlRow.addView(btn("收起", 1, new Runnable() {
            public void run() { send(OverlayService.ACTION_HIDE); }
        }));
        ctlRow.addView(btn("重载", 1, new Runnable() {
            public void run() { send(OverlayService.ACTION_RELOAD); }
        }));
        root.addView(ctlRow);

        LinearLayout ctlRow2 = row();
        ctlRow2.addView(btn("点击穿透", 1, new Runnable() {
            public void run() { send(OverlayService.ACTION_TOGGLE_TOUCH); }
        }));
        ctlRow2.addView(btn("回默认位置", 1, new Runnable() {
            public void run() { send(OverlayService.ACTION_RESET_POS); }
        }));
        ctlRow2.addView(btn("退出服务", 1, new Runnable() {
            public void run() { send(OverlayService.ACTION_EXIT); }
        }));
        root.addView(ctlRow2);

        // ---------- 快捷换人 / 换装 ----------
        root.addView(label("快捷换装（即时生效）"));
        LinearLayout quickRow = row();
        quickRow.addView(btn("下一个人物", 1, new Runnable() {
            public void run() {
                if (!overlayGranted()) return;
                send(OverlayService.ACTION_CYCLE_MODEL);
            }
        }));
        quickRow.addView(btn("下一套衣服", 1, new Runnable() {
            public void run() {
                if (!overlayGranted()) return;
                send(OverlayService.ACTION_CYCLE_TEXTURE);
            }
        }));
        root.addView(quickRow);

        final Spinner spModel = new Spinner(this);
        final java.util.List<ModelRepo.Info> models = ModelRepo.list(this);
        String[] modelNames = new String[Math.max(1, models.size())];
        if (models.isEmpty()) modelNames[0] = "（资源包为空）";
        else for (int i = 0; i < models.size(); i++) modelNames[i] = models.get(i).name;
        spModel.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, modelNames));
        spModel.setSelection(Math.max(0, Math.min(modelNames.length - 1, Prefs.modelId(this) - 1)));

        final Spinner spTex = new Spinner(this);
        final Runnable fillTex = new Runnable() {
            public void run() {
                int modelId = spModel.getSelectedItemPosition() + 1;
                int n = ModelRepo.texCount(MainActivity.this, modelId);
                String[] names = new String[n];
                for (int i = 0; i < n; i++) names[i] = (i + 1) + ". " + ModelRepo.texName(MainActivity.this, modelId, i + 1);
                spTex.setAdapter(new ArrayAdapter<String>(MainActivity.this,
                        android.R.layout.simple_spinner_dropdown_item, names));
                int keep = (modelId == Prefs.modelId(MainActivity.this)) ? Prefs.texId(MainActivity.this) : 1;
                spTex.setSelection(Math.max(0, Math.min(n - 1, keep - 1)));
            }
        };
        fillTex.run();

        LinearLayout pickRow = row();
        pickRow.addView(spModel, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        pickRow.addView(spTex, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        pickRow.addView(btn("换上", 0, new Runnable() {
            public void run() {
                if (!overlayGranted()) {
                    Toast.makeText(MainActivity.this, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent i = new Intent(MainActivity.this, OverlayService.class);
                i.setAction(OverlayService.ACTION_SET_LOOK);
                i.putExtra("modelId", spModel.getSelectedItemPosition() + 1);
                i.putExtra("texId", spTex.getSelectedItemPosition() + 1);
                sendIntent(i);
            }
        }), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        root.addView(pickRow);

        // ---------- 设置 ----------
        root.addView(label("更多"));
        root.addView(button("外观 / 气泡 / 工具栏 / 自启…（设置）", new Runnable() {
            public void run() { startActivity(new Intent(MainActivity.this, SettingsActivity.class)); }
        }));

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);
    }

    private LinearLayout.LayoutParams section(LinearLayout root, View v) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        root.addView(v, p);
        return p;
    }

    private TextView label(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(15);
        t.setPadding(0, 34, 0, 6);
        return t;
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER);
        return r;
    }

    /** 行内按钮：weight>0 平分整行；weight=0 由调用方给 LayoutParams */
    private Button btn(String text, float weight, final Runnable action) {
        Button b = button(text, action);
        if (weight > 0) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, weight);
            p.leftMargin = 6;
            p.rightMargin = 6;
            b.setLayoutParams(p);
        }
        return b;
    }

    private Button button(String text, final Runnable action) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { action.run(); }
        });
        return b;
    }

    private void sendIntent(Intent i) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(i);
        } else {
            startService(i);
        }
    }

    private void send(String action) {
        sendIntent(new Intent(this, OverlayService.class).setAction(action));
    }

    private boolean overlayGranted() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PROJECTION) {
            // 转交给服务：授权成功会立刻拍一张
            OverlayService.onProjectionResult(this, resultCode, data);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        StringBuilder sb = new StringBuilder();
        sb.append("悬浮窗权限：").append(overlayGranted() ? "✓ 已授予" : "✗ 未授予");
        sb.append("    通知权限：").append(notifGranted() ? "✓ 已授予" : "✗ 未授予");
        sb.append("    电池优化：").append(ignoringBattery() ? "✓ 已忽略" : "✗ 仍受限");
        sb.append("\n守护服务：").append(OverlayService.isRunning() ? "运行中" : "未运行");
        sb.append(" · 状态：").append(Prefs.enabled(this) ? (Prefs.visible(this) ? "在场" : "已收起") : "已退出");
        sb.append(" · 触摸：").append(Prefs.touchThrough(this) ? "穿透（纯装饰）" : "可交互");
        int mid = Prefs.modelId(this), tid = Prefs.texId(this);
        ModelRepo.Info m = ModelRepo.model(this, mid);
        sb.append("\n当前人物：").append(m == null ? "#" + mid : m.name)
          .append(" · 衣服：").append(ModelRepo.texName(this, mid, tid))
          .append("（第 ").append(tid).append("/").append(ModelRepo.texCount(this, mid)).append(" 套）");
        sb.append("\n尺寸 ").append(Prefs.sizeW(this)).append("×").append(Prefs.sizeH(this)).append("dp")
          .append(" · ").append(Prefs.rightSide(this) ? "右" : "左").append("停靠")
          .append(" · 透明度 ").append(Prefs.alpha(this)).append("%");
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
