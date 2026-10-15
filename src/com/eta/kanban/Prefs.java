package com.eta.kanban;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
    private static final String NAME = "kanban";

    public static SharedPreferences of(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static int modelId(Context c)      { return of(c).getInt("modelId", 1); }      // 默认 Pio
    public static int texId(Context c)        { return of(c).getInt("texId", 1); }
    public static int sizeW(Context c)        { return of(c).getInt("sizeW", 300); }
    public static int sizeH(Context c)        { return of(c).getInt("sizeH", 280); }
    public static boolean toolbar(Context c)  { return of(c).getBoolean("toolbar", true); }
    public static boolean closeBtn(Context c) { return of(c).getBoolean("closeBtn", true); }
    public static boolean hitokoto(Context c) { return of(c).getBoolean("hitokoto", true); }
    public static int alpha(Context c)        { return of(c).getInt("alpha", 100); }      // 0-100
    public static boolean rightSide(Context c){ return of(c).getBoolean("rightSide", true); }
    public static int edgeX(Context c)        { return of(c).getInt("edgeX", 0); }
    public static int edgeY(Context c)        { return of(c).getInt("edgeY", 40); }
    public static boolean touchThrough(Context c) { return of(c).getBoolean("touchThrough", false); }
    public static boolean showHandle(Context c)   { return of(c).getBoolean("showHandle", true); }
    public static boolean autoStart(Context c)    { return of(c).getBoolean("autoStart", false); }

    public static void set(Context c, String k, int v)     { of(c).edit().putInt(k, v).apply(); }
    public static void set(Context c, String k, boolean v) { of(c).edit().putBoolean(k, v).apply(); }
}
