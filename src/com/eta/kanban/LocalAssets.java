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
 * 把页面的所有请求都映射到 APK 自带 assets —— 于是完全离线。
 *
 * 页面 origin 是 https://app.local/，live2d-widget 需要的资源按这个前缀取：
 *   /widget/…     引擎本体（waifu-tips.js、chunk、live2d.min.js、waifu.css、waifu-tips.json）
 *   /modelrepo/…  模型仓库（model_list.json + model/&lt;名字&gt;/index.json + textures.cache + 模型文件）
 */
public class LocalAssets extends WebViewClient {

    private final Context ctx;

    public LocalAssets(Context c) {
        this.ctx = c;
    }

    private static String mimeOf(String p) {
        if (p.endsWith(".html")) return "text/html";
        if (p.endsWith(".js") || p.endsWith(".mjs")) return "application/javascript";
        if (p.endsWith(".json")) return "application/json";
        if (p.endsWith(".css")) return "text/css";
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".mp3")) return "audio/mpeg";
        if (p.endsWith(".ogg")) return "audio/ogg";
        if (p.endsWith(".moc") || p.endsWith(".mtn")) return "application/octet-stream";
        return "application/octet-stream";
    }

    /** 文件名里有空格等字符时会被 URL 编码（%20），要还原后再找 assets */
    private static String urlDecode(String s) {
        try {
            return java.net.URLDecoder.decode(s, "UTF-8");
        } catch (Throwable t) {
            return s;
        }
    }

    private WebResourceResponse serve(String assetPath) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Cache-Control", "no-store");
        try {
            InputStream in = ctx.getAssets().open(assetPath);
            if (!assetPath.startsWith("widget/")) {
                android.util.Log.i("Kanban", "srv " + assetPath);
            }
            return new WebResourceResponse(mimeOf(assetPath), "utf-8", 200, "OK", headers, in);
        } catch (IOException e) {
            android.util.Log.w("Kanban", "MISS " + assetPath);
            return new WebResourceResponse("text/plain", "utf-8", 404, "Not Found",
                    headers, null);
        }
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();
        if (url.startsWith("https://app.local/") || url.startsWith("http://app.local/")) {
            String path = url.substring(url.indexOf("app.local/") + "app.local/".length());
            int q = path.indexOf('?');
            if (q >= 0) path = path.substring(0, q);
            return serve(urlDecode(path));
        }
        return null;   // 其它请求（理论上不该有）交给系统
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        if (url != null && url.contains("app.local/")) {
            String path = url.substring(url.indexOf("app.local/") + "app.local/".length());
            int q = path.indexOf('?');
            if (q >= 0) path = path.substring(0, q);
            return serve(urlDecode(path));
        }
        return null;
    }
}
