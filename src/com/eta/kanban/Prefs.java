package com.eta.kanban;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
    private static final String NAME = "kanban";

    public static SharedPreferences of(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    // 看板娘外观
    public static int sizeW(Context c)        { return of(c).getInt("sizeW", 300); }
    public static int sizeH(Context c)        { return of(c).getInt("sizeH", 280); }
    public static int alpha(Context c)        { return of(c).getInt("alpha", 100); }      // 0-100
    public static boolean rightSide(Context c){ return of(c).getBoolean("rightSide", true); }
    public static int edgeX(Context c)        { return of(c).getInt("edgeX", 0); }
    public static int edgeY(Context c)        { return of(c).getInt("edgeY", 8); }
    /** true=距顶部算，false=距底部算（默认贴底边） */
    public static boolean anchorTop(Context c)    { return of(c).getBoolean("anchorTop", false); }

    // 元素开关
    public static boolean toolbar(Context c)  { return of(c).getBoolean("toolbar", true); }
    public static boolean closeBtn(Context c) { return of(c).getBoolean("closeBtn", true); }
    public static boolean hitokoto(Context c) { return of(c).getBoolean("hitokoto", true); }
    public static boolean touchThrough(Context c) { return of(c).getBoolean("touchThrough", false); }
    public static boolean showHandle(Context c)   { return of(c).getBoolean("showHandle", false); }

    // 运行方式
    public static boolean autoStart(Context c)    { return of(c).getBoolean("autoStart", true); }
    /** 用户期望"看板娘应该在场"（被杀后守护据此拉回） */
    public static boolean enabled(Context c)      { return of(c).getBoolean("enabled", false); }
    /** 当前是否显示中（收起后仍可能是 enabled） */
    public static boolean visible(Context c)      { return of(c).getBoolean("visible", true); }
    public static void setEnabled(Context c, boolean v) { of(c).edit().putBoolean("enabled", v).apply(); }
    public static void setVisible(Context c, boolean v) { of(c).edit().putBoolean("visible", v).apply(); }

    public static void set(Context c, String k, int v)     { of(c).edit().putInt(k, v).apply(); }
    public static void set(Context c, String k, boolean v) { of(c).edit().putBoolean(k, v).apply(); }
}
