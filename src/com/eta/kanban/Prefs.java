package com.eta.kanban;

import android.content.Context;
import android.content.SharedPreferences;

/** 设置项。默认值就是"当前屏幕下的推荐值"（1116x2480 / 322dpi）。 */
public class Prefs {
    private static final String NAME = "kanban";

    public static SharedPreferences of(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    // ---- 角色与衣服（对应老插件的 人物ID / 衣服ID）----
    public static int modelId(Context c)      { return of(c).getInt("modelId", 1); }        // 1 起
    public static int texId(Context c)        { return of(c).getInt("texId", 1); }          // 1 起（Pio 有 87 套）

    // ---- 外观（人物大小 / 停靠 / 边距）----
    public static int sizeW(Context c)        { return of(c).getInt("sizeW", 220); }        // dp
    public static int sizeH(Context c)        { return of(c).getInt("sizeH", 210); }        // dp
    public static int alpha(Context c)        { return of(c).getInt("alpha", 100); }        // 20-100
    public static boolean rightSide(Context c){ return of(c).getBoolean("rightSide", true); }
    public static int edgeX(Context c)        { return of(c).getInt("edgeX", 0); }          // dp
    public static int edgeY(Context c)        { return of(c).getInt("edgeY", 8); }          // dp
    public static boolean anchorTop(Context c){ return of(c).getBoolean("anchorTop", false); }

    // ---- 气泡（提示框）----
    public static int tipsW(Context c)        { return of(c).getInt("tipsW", 170); }        // px
    public static int tipsH(Context c)        { return of(c).getInt("tipsH", 50); }         // px
    public static int tipsFont(Context c)     { return of(c).getInt("tipsFont", 13); }      // px
    public static int tipsOffY(Context c)     { return of(c).getInt("tipsOffY", -6); }      // px（负=上移）
    public static boolean hitokoto(Context c) { return of(c).getBoolean("hitokoto", true); } // 是否显示气泡

    // ---- 工具栏 ----
    public static boolean toolbar(Context c)  { return of(c).getBoolean("toolbar", true); }
    public static boolean closeBtn(Context c) { return of(c).getBoolean("closeBtn", true); }
    public static int toolIcon(Context c)     { return of(c).getInt("toolIcon", 20); }      // px
    public static int toolGap(Context c)      { return of(c).getInt("toolGap", 4); }       // px（行高/间距）

    // ---- 一言来源：0=本地文案 1=在线 hitokoto.cn 2=关闭 ----
    public static int hitoSource(Context c)   { return of(c).getInt("hitoSource", 0); }

    // ---- 运行方式 ----
    public static boolean touchThrough(Context c) { return of(c).getBoolean("touchThrough", false); }
    public static boolean showHandle(Context c)   { return of(c).getBoolean("showHandle", false); }
    public static boolean autoStart(Context c)    { return of(c).getBoolean("autoStart", true); }
    public static boolean enabled(Context c)      { return of(c).getBoolean("enabled", false); }
    public static boolean visible(Context c)      { return of(c).getBoolean("visible", true); }
    public static void setEnabled(Context c, boolean v) { of(c).edit().putBoolean("enabled", v).apply(); }
    public static void setVisible(Context c, boolean v) { of(c).edit().putBoolean("visible", v).apply(); }

    public static void set(Context c, String k, int v)     { of(c).edit().putInt(k, v).apply(); }
    public static void set(Context c, String k, boolean v) { of(c).edit().putBoolean(k, v).apply(); }

    /** 恢复默认外观（大小按当前屏幕推荐值，位置回右下角贴底） */
    public static void resetLook(Context c) {
        of(c).edit()
          .putInt("sizeW", 220).putInt("sizeH", 210).putInt("alpha", 100)
          .putBoolean("rightSide", true).putBoolean("anchorTop", false)
          .putInt("edgeX", 0).putInt("edgeY", 8)
          .putInt("tipsW", 170).putInt("tipsH", 50).putInt("tipsFont", 13).putInt("tipsOffY", -6)
          .putInt("toolIcon", 20).putInt("toolGap", 4)
          .apply();
    }
}
