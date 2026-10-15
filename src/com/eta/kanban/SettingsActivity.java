package com.eta.kanban;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * 设置：对齐老插件那套可调参数（人物ID/衣服ID/人物大小/停靠/提示框/工具栏/一言），
 * 外加点击穿透、守护等本项目特有的项。改完点"保存并重新加载"生效。
 */
public class SettingsActivity extends Activity {

    private static final String[] HITO_MODES = {"本地文案（离线）", "在线 hitokoto.cn", "关闭"};

    private int w, h, alpha, edgeX, edgeY;
    private int tipsW, tipsH, tipsFont, tipsOffY, toolIcon, toolGap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        L.init(this);
        w = Prefs.sizeW(this); h = Prefs.sizeH(this); alpha = Prefs.alpha(this);
        edgeX = Prefs.edgeX(this); edgeY = Prefs.edgeY(this);
        tipsW = Prefs.tipsW(this); tipsH = Prefs.tipsH(this);
        tipsFont = Prefs.tipsFont(this); tipsOffY = Prefs.tipsOffY(this);
        toolIcon = Prefs.toolIcon(this); toolGap = Prefs.toolGap(this);

        final LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 56, 48, 48);

        TextView t = new TextView(this);
        t.setText("看板娘设置");
        t.setTextSize(24);
        root.addView(t);

        // ---------- 角色与衣服 ----------
        root.addView(label("人物 ID（资源包里的人物序号）"));
        Spinner spModel = new Spinner(this);
        String[] models = new String[7];
        for (int i = 0; i < 7; i++) models[i] = String.valueOf(i + 1);
        spModel.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, models));
        spModel.setSelection(Math.max(0, Math.min(6, Prefs.modelId(this) - 1)));
        spModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                Prefs.set(SettingsActivity.this, "modelId", pos + 1);
            }
            public void onNothingSelected(AdapterView<?> p) { }
        });
        root.addView(spModel);

        root.addView(slider("衣服 ID（Pio 有 87 套，1 起）", 1, 87, Prefs.texId(this), new SliderCB() {
            public void on(int v) { Prefs.set(SettingsActivity.this, "texId", v); }
        }));

        // ---------- 人物大小与停靠 ----------
        root.addView(label("人物大小与停靠"));
        root.addView(slider("宽度（dp）", 120, 700, w, new SliderCB() {
            public void on(int v) { w = v; Prefs.set(SettingsActivity.this, "sizeW", v); }
        }));
        root.addView(slider("高度（dp）", 120, 700, h, new SliderCB() {
            public void on(int v) { h = v; Prefs.set(SettingsActivity.this, "sizeH", v); }
        }));

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

        root.addView(slider("到侧边距离 X（dp）", 0, 300, edgeX, new SliderCB() {
            public void on(int v) { edgeX = v; Prefs.set(SettingsActivity.this, "edgeX", v); }
        }));
        root.addView(slider("到边距 Y（贴底时=离底边距离，dp）", 0, 1500, edgeY, new SliderCB() {
            public void on(int v) { edgeY = v; Prefs.set(SettingsActivity.this, "edgeY", v); }
        }));
        root.addView(slider("透明度 %", 20, 100, alpha, new SliderCB() {
            public void on(int v) { alpha = v; Prefs.set(SettingsActivity.this, "alpha", v); }
        }));

        // ---------- 提示框（气泡） ----------
        root.addView(label("提示框（气泡）"));
        root.addView(slider("气泡宽（px）", 80, 400, tipsW, new SliderCB() {
            public void on(int v) { tipsW = v; Prefs.set(SettingsActivity.this, "tipsW", v); }
        }));
        root.addView(slider("气泡高（px）", 20, 200, tipsH, new SliderCB() {
            public void on(int v) { tipsH = v; Prefs.set(SettingsActivity.this, "tipsH", v); }
        }));
        root.addView(slider("气泡字号（px）", 10, 24, tipsFont, new SliderCB() {
            public void on(int v) { tipsFont = v; Prefs.set(SettingsActivity.this, "tipsFont", v); }
        }));
        root.addView(slider("气泡 Y 轴偏移（px，负=上移）", -60, 60, tipsOffY, new SliderCB() {
            public void on(int v) { tipsOffY = v; Prefs.set(SettingsActivity.this, "tipsOffY", v); }
        }));
        root.addView(check("显示气泡", Prefs.hitokoto(this), "hitokoto"));

        // ---------- 工具栏 ----------
        root.addView(label("工具栏"));
        root.addView(check("显示工具栏（换装/换人/一言/截图/关闭）", Prefs.toolbar(this), "toolbar"));
        root.addView(check("显示关闭按钮", Prefs.closeBtn(this), "closeBtn"));
        root.addView(slider("图标大小（px）", 12, 40, toolIcon, new SliderCB() {
            public void on(int v) { toolIcon = v; Prefs.set(SettingsActivity.this, "toolIcon", v); }
        }));
        root.addView(slider("行高/间距（px）", 0, 20, toolGap, new SliderCB() {
            public void on(int v) { toolGap = v; Prefs.set(SettingsActivity.this, "toolGap", v); }
        }));

        // ---------- 一言 ----------
        root.addView(label("一言来源"));
        Spinner spHito = new Spinner(this);
        spHito.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, HITO_MODES));
        spHito.setSelection(Math.max(0, Math.min(2, Prefs.hitoSource(this))));
        spHito.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                Prefs.set(SettingsActivity.this, "hitoSource", pos);
            }
            public void onNothingSelected(AdapterView<?> p) { }
        });
        root.addView(spHito);

        // ---------- 运行方式 ----------
        root.addView(label("运行方式"));
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
        t.setPadding(0, 30, 0, 4);
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
