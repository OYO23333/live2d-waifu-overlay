package com.eta.kanban;

import android.content.Context;

import java.io.File;
import java.io.FileWriter;
import java.util.Date;

/**
 * 日志：既写 logcat 也写应用私有目录里的文件。
 * 有些 ROM 会限制/丢弃后台应用在 logcat 里的输出，有文件日志就能随时排查。
 * 文件：/data/data/com.eta.kanban/files/kanban.log（超过 512KB 自动轮转一份 .1）
 */
public class L {
    private static File file;

    public static void init(Context c) {
        try {
            // 写到外部私有目录：/sdcard/Android/data/com.eta.kanban/files/kanban.log
            // （应用内部目录 /data/user/0/… 在调试 shell 的挂载命名空间里看不到）
            File dir = c.getExternalFilesDir(null);
            file = new File(dir != null ? dir : c.getFilesDir(), "kanban.log");
        } catch (Throwable ignored) {
        }
    }

    public static void i(String tag, String msg) { android.util.Log.i(tag, msg); write(tag, "I", msg); }
    public static void w(String tag, String msg) { android.util.Log.w(tag, msg); write(tag, "W", msg); }
    public static void e(String tag, String msg) { android.util.Log.e(tag, msg); write(tag, "E", msg); }

    private static synchronized void write(String tag, String level, String msg) {
        if (file == null) return;
        try {
            if (file.length() > 512 * 1024) {
                File old = new File(file.getParentFile(), "kanban.log.1");
                old.delete();
                file.renameTo(old);
            }
            FileWriter fw = new FileWriter(file, true);
            fw.write(new java.text.SimpleDateFormat("MM-dd HH:mm:ss.SSS", java.util.Locale.US)
                    .format(new Date()) + " " + level + "/" + tag + ": " + msg + "\n");
            fw.close();
        } catch (Throwable ignored) {
        }
    }
}
