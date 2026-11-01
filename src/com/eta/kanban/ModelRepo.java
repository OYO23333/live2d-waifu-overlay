package com.eta.kanban;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 从 assets/modelrepo 读取人物与衣服清单。
 * 结构：model_list.json → { "models": ["Potion-Maker/Pio", ...], "messages": [...] }
 * 每个人物：model/<名字>/index.json（textures 数组）+ textures.cache。
 * 给界面（设置页下拉、主界面快捷换装）用，结果带缓存。
 */
public final class ModelRepo {

    public static class Info {
        public final String id;      // 形如 "Potion-Maker/Pio"
        public final String name;    // 目录最后一段，展示用
        public final List<String> textures = new ArrayList<>();

        Info(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private static volatile List<Info> cache;

    public static List<Info> list(Context c) {
        List<Info> r = cache;
        if (r != null) return r;
        synchronized (ModelRepo.class) {
            if (cache == null) cache = load(c);
            return cache;
        }
    }

    public static int modelCount(Context c) { return list(c).size(); }

    /** 人物 id（1 起）越界时收敛到有效范围；没有人物时返回 1 */
    public static int clampModelId(Context c, int id) {
        int n = Math.max(1, modelCount(c));
        return Math.max(1, Math.min(n, id));
    }

    public static Info model(Context c, int modelId) {
        List<Info> all = list(c);
        if (all.isEmpty()) return null;
        return all.get(clampModelId(c, modelId) - 1);
    }

    public static int texCount(Context c, int modelId) {
        Info m = model(c, modelId);
        return m == null ? 1 : Math.max(1, m.textures.size());
    }

    /** 衣服的展示名：取文件名去掉扩展名 */
    public static String texName(Context c, int modelId, int texId) {
        Info m = model(c, modelId);
        if (m == null || m.textures.isEmpty()) return "衣服 " + texId;
        int i = Math.max(0, Math.min(m.textures.size() - 1, texId - 1));
        String p = m.textures.get(i);
        int slash = p.lastIndexOf('/');
        if (slash >= 0) p = p.substring(slash + 1);
        int dot = p.lastIndexOf('.');
        if (dot > 0) p = p.substring(0, dot);
        return p;
    }

    private static List<Info> load(Context c) {
        List<Info> out = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(read(c, "modelrepo/model_list.json"));
            JSONArray models = root.optJSONArray("models");
            if (models == null) return out;
            for (int i = 0; i < models.length(); i++) {
                String id = models.optString(i, "");
                if (id.length() == 0) continue;
                String name = id;
                int slash = id.lastIndexOf('/');
                if (slash >= 0 && slash < id.length() - 1) name = id.substring(slash + 1);
                Info info = new Info(id, name);
                // 优先 textures.cache（包含引擎实际可切的全部衣服），回退 index.json 的 textures
                JSONArray tex = null;
                String cacheTxt = read(c, "modelrepo/model/" + id + "/textures.cache");
                if (cacheTxt != null) {
                    try { tex = new JSONArray(cacheTxt); } catch (Throwable ignored) { }
                }
                if (tex == null) {
                    try {
                        JSONObject idx = new JSONObject(read(c, "modelrepo/model/" + id + "/index.json"));
                        tex = idx.optJSONArray("textures");
                    } catch (Throwable ignored) { }
                }
                if (tex != null) {
                    for (int j = 0; j < tex.length(); j++) {
                        String t = tex.optString(j, "");
                        if (t.length() > 0) info.textures.add(t);
                    }
                }
                if (info.textures.isEmpty()) info.textures.add("textures/default-costume.png");
                out.add(info);
            }
        } catch (Throwable t) {
            L.w("Kanban", "读取模型清单失败：" + t);
        }
        return out;
    }

    private static String read(Context c, String path) {
        try {
            InputStream in = c.getAssets().open(path);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            in.close();
            return new String(bos.toByteArray(), "UTF-8");
        } catch (Throwable t) {
            return null;
        }
    }
}
