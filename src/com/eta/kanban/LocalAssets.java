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
            return new WebResourceResponse(mimeOf(assetPath), "utf-8", 200, "OK", headers, in);
        } catch (IOException e) {
            return new WebResourceResponse("text/plain", "utf-8", 404, "Not Found",
                    headers, null);
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
            String id = idOf(query);
            String first = id.split("-")[0];
            if (rest.startsWith("get/")) return "api/get-" + first + ".json";
            if (rest.startsWith("switch/") || rest.startsWith("rand/")) {
                return "api/switch-" + first + ".json";
            }
            if (rest.startsWith("switch_textures/") || rest.startsWith("rand_textures/")) {
                String[] parts = id.split("-");
                String tex = parts.length > 1 ? parts[1] : "1";
                return "api/switch_textures-" + first + "-" + tex + ".json";
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
