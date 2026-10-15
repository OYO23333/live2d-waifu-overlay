package com.eta.kanban;

import android.content.Context;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 把页面需要的所有网络请求都映射到 APK 自带 assets，
 * 因此 kbn.js 可以保持原版不动：它以为自己在跟远端说话。
 */
public class LocalAssets extends WebViewClient {

    private final Context ctx;

    public LocalAssets(Context c) {
        this.ctx = c;
    }

    private static String mimeOf(String p) {
        if (p.endsWith(".html")) return "text/html";
        if (p.endsWith(".js")) return "application/javascript";
        if (p.endsWith(".json")) return "application/json";
        if (p.endsWith(".css")) return "text/css";
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".ogg")) return "audio/ogg";
        if (p.endsWith(".mp3")) return "audio/mpeg";
        if (p.endsWith(".moc") || p.endsWith(".mtn")) return "application/octet-stream";
        return "application/octet-stream";
    }

    private WebResourceResponse serve(String assetPath) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Cache-Control", "no-store");
        try {
            InputStream in = ctx.getAssets().open(assetPath);
            if (assetPath.startsWith("api/")) {
                android.util.Log.i("Kanban", "srv " + assetPath);
            }
            return new WebResourceResponse(mimeOf(assetPath), "utf-8", 200, "OK", headers, in);
        } catch (IOException e) {
            android.util.Log.w("Kanban", "MISS " + assetPath);
            return new WebResourceResponse("text/plain", "utf-8", 404, "Not Found",
                    headers, null);
        }
    }

    /** 文件名里有空格等字符时会被 URL 编码（%20），要还原后再找 assets */
    private static String urlDecode(String s) {
        try {
            return java.net.URLDecoder.decode(s, "UTF-8");
        } catch (Throwable t) {
            return s;
        }
    }

    /** assets 里有没有这个文件 */
    private boolean exists(String assetPath) {
        try {
            InputStream in = ctx.getAssets().open(assetPath);
            in.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static String idOf(String query) {
        if (query == null) return "1";
        for (String kv : query.split("&")) {
            if (kv.startsWith("id=")) return kv.substring(3);
        }
        return "1";
    }

    /** 返回 null 表示这个请求不归我们管，交给系统 */
    private String toAssetPath(String url) {
        if (url == null) return null;
        String u = url;
        String query = "";
        int q = u.indexOf('?');
        if (q >= 0) {
            query = u.substring(q + 1);
            u = u.substring(0, q);
        }
        u = u.replace("https://", "").replace("http://", "");

        // 1) 页面自身与模型静态资源（页面 origin 是 app.local）
        if (u.startsWith("app.local/")) {
            return u.substring("app.local/".length());
        }

        // 2) 官方模型 API
        if (u.startsWith("live2d.fghrsh.net/api/")) {
            String rest = u.substring("live2d.fghrsh.net/api/".length());
            String id = idOf(query);                 // 形如 N 或 N-M
            String[] parts = id.split("-");
            String model = parts[0];
            String tex = parts.length > 1 ? parts[1] : null;

            if (rest.startsWith("get/")) {
                // 关键：id=N-M 里的 M 是"第几套衣服"，不能丢，否则永远只有默认那套
                if (tex != null) {
                    String p = "api/get-" + model + "-" + tex + ".json";
                    return exists(p) ? p : "api/get-" + model + ".json";
                }
                return "api/get-" + model + ".json";
            }
            if (rest.startsWith("switch/") || rest.startsWith("rand/")) {
                return "api/switch-" + model + ".json";
            }
            if (rest.startsWith("switch_textures/") || rest.startsWith("rand_textures/")) {
                if (tex != null) {
                    String p = "api/switch_textures-" + model + "-" + tex + ".json";
                    if (exists(p)) return p;
                }
                return "api/textures-none.json";
            }
            if (rest.startsWith("model_list.json")) return "api/model_list.json";
            return "api/notfound.json";
        }

        // 3) 一言
        if (u.contains("hitokoto.cn") || u.contains("jinrishici.com")
                || u.contains("api.fghrsh.net")) {
            int k = 1 + (int) (Math.random() * 6);
            return "api/hito-" + k + ".json";
        }

        // 4) 模型资源：任何 live2d_api@版本/... 的地址都映射到 assets 同名路径
        int i = u.indexOf("live2d_api@");
        if (i >= 0) {
            String rest = u.substring(i);
            int slash = rest.indexOf('/');
            rest = slash >= 0 ? rest.substring(slash + 1) : "";
            if (rest.startsWith("get/")) rest = rest.substring(4);
            while (rest.startsWith("/")) rest = rest.substring(1);
            rest = urlDecode(rest);          // 空格等被编码的文件名要还原
            if (rest.endsWith(".png")) {
                try {
                    if (!exists(rest)) android.util.Log.w("Kanban", "MISS 贴图 " + rest);
                } catch (Throwable ignored) {
                }
            }
            return rest.length() == 0 ? null : rest;
        }
        return null;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        String path = toAssetPath(request.getUrl().toString());
        return path == null ? null : serve(path);
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        String path = toAssetPath(url);
        return path == null ? null : serve(path);
    }
}
