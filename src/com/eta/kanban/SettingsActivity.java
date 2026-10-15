package com.eta.kanban;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * 设置项。角色与衣服由资源包决定（见 /workspace/make_assets.js），
 * 页面上不再有"模型 id / 贴图 id"这类需要手填的参数。
 */
public class SettingsActivity extends Activity {

    private int w, h, alpha, edgeX, edgeY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        w = Prefs.sizeW(this); h = Prefs.sizeH(this); alpha = Prefs.alpha(this);
        edgeX = Prefs.edgeX(this); edgeY = Prefs.edgeY(this);

        final LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 56, 48, 48);

        TextView t = new TextView(this);
        t.setText("看板娘设置");
        t.setTextSize(24);
        root.addView(t);

        TextView hint = new TextView(this);
        hint.setText("角色与衣服在资源包里（换人/换装在悬浮窗工具栏上操作，会自动记住）");
        hint.setTextSize(13);
        hint.setPadding(0, 12, 0, 0);
        root.addView(hint);

        root.addView(slider("宽度", 120, 700, w, new SliderCB() {
            public void on(int v) { w = v; Prefs.set(SettingsActivity.this, "sizeW", v); }
        }));
        root.addView(slider("高度", 120, 700, h, new SliderCB() {
            public void on(int v) { h = v; Prefs.set(SettingsActivity.this, "sizeH", v); }
        }));

        root.addView(label("停靠位置"));
        RadioGroup rg = new RadioGroup(this);
        rg.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton rbR = new RadioButton(this); rbR.setText("右侧"); rbR.setId(101);
        RadioButton rbL = new RadioButton(this); rbL.setText("左侧"); rbL.setId(102);
        rg.addView(rbR); rg.addView(rbL);
        rg.check(Prefs.rightSide(this) ? 101 : 102);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup g, int id) {
                Prefs.set(SettingsActivity.this, "rightSide", id == 101);
            }
        });
        root.addView(rg);

        root.addView(slider("到边距 X", 0, 300, edgeX, new SliderCB() {
            public void on(int v) { edgeX = v; Prefs.set(SettingsActivity.this, "edgeX", v); }
        }));
        root.addView(slider("到边距 Y（贴底时=离底边的距离）", 0, 1500, edgeY, new SliderCB() {
            public void on(int v) { edgeY = v; Prefs.set(SettingsActivity.this, "edgeY", v); }
        }));
        root.addView(slider("透明度 %", 20, 100, alpha, new SliderCB() {
            public void on(int v) { alpha = v; Prefs.set(SettingsActivity.this, "alpha", v); }
        }));

        root.addView(check("显示工具栏（换人 / 换装 / 一言 / 截图 / 关于）", Prefs.toolbar(this), "toolbar"));
        root.addView(check("显示关闭按钮", Prefs.closeBtn(this), "closeBtn"));
        root.addView(check("显示气泡台词", Prefs.hitokoto(this), "hitokoto"));
        root.addView(check("点击穿透（看板娘不接收触摸，纯装饰）", Prefs.touchThrough(this), "touchThrough"));
        root.addView(checkInverted("位置贴屏幕底边（不勾选则按距顶部算）", !Prefs.anchorTop(this), "anchorTop"));
        root.addView(check("显示拖动手柄（不勾选也能用：长按看板娘 0.8 秒后拖动）", Prefs.showHandle(this), "showHandle"));
        root.addView(check("开机自动上岗", Prefs.autoStart(this), "autoStart"));

        Button apply = new Button(this);
        apply.setText("保存并重新加载看板娘");
        apply.setAllCaps(false);
        apply.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                android.content.Intent i = new android.content.Intent(
                        SettingsActivity.this, OverlayService.class);
                i.setAction(OverlayService.ACTION_RELOAD);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(i);
                } else {
                    startService(i);
                }
                finish();
            }
        });
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = 40;
        apply.setLayoutParams(p);
        root.addView(apply);

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);
    }

    private interface SliderCB { void on(int v); }

    private TextView label(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setPadding(0, 28, 0, 4);
        t.setTextSize(15);
        return t;
    }

    private LinearLayout slider(String name, final int min, final int max, int value, final SliderCB cb) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        final TextView t = new TextView(this);
        t.setText(name + "：" + value);
        box.addView(t);

        SeekBar sb = new SeekBar(this);
        sb.setMax(max - min);
        sb.setProgress(Math.max(0, Math.min(max - min, value - min)));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int prog, boolean fromUser) {
                int v = min + prog;
                t.setText(name + "：" + v);
                cb.on(v);
            }
            public void onStartTrackingTouch(SeekBar s) { }
            public void onStopTrackingTouch(SeekBar s) { }
        });
        box.addView(sb);
        return box;
    }

    private CheckBox check(String text, boolean value, final String key) {
        final CheckBox cb = new CheckBox(this);
        cb.setText(text);
        cb.setChecked(value);
        cb.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton v, boolean checked) {
                Prefs.set(SettingsActivity.this, key, checked);
            }
        });
        return cb;
    }

    /** 勾选状态与存储值相反的开关（例如"贴底边" = anchorTop 为 false） */
    private CheckBox checkInverted(String text, boolean value, final String key) {
        final CheckBox cb = new CheckBox(this);
        cb.setText(text);
        cb.setChecked(value);
        cb.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton v, boolean checked) {
                Prefs.set(SettingsActivity.this, key, !checked);
            }
        });
        return cb;
    }
}
