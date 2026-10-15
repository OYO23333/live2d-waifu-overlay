package com.eta.kanban;

import android.content.Context;

/**
 * 页面：改用 live2d-widget（stevenjoezhang/live2d-widget）的引擎。
 * 它比原来 kbn.js（fghrsh 那套）干净得多：模块化、支持静态模型仓库（model_list.json + textures.cache）、
 * 自带工具栏与关闭按钮，且长期维护。
 *
 * 全部资源都在 assets 里，由 LocalAssets 拦截 app.local 的请求提供，完全离线。
 */
public class Page {

    public static String html(Context c) {
        int w = Prefs.sizeW(c);
        int h = Prefs.sizeH(c);
        int alpha = Math.max(10, Math.min(100, Prefs.alpha(c)));
        boolean showToolbar = Prefs.toolbar(c);
        boolean showClose = Prefs.closeBtn(c);
        boolean tips = Prefs.hitokoto(c);
        String side = Prefs.rightSide(c) ? "right" : "left";

        StringBuilder sb = new StringBuilder(6144);
        sb.append("<!doctype html><html><head><meta charset=\"utf-8\">")
          .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,user-scalable=no\">")
          .append("<link rel=\"stylesheet\" href=\"/widget/waifu.css\">")
          .append("<style>")
          .append("html,body{margin:0;padding:0;background:transparent !important;overflow:hidden;")
          .append("width:100%;height:100%;opacity:")
          .append(String.format(java.util.Locale.US, "%.2f", alpha / 100.0)).append("}");
        // 尺寸按设置走（覆盖它的默认值）
        sb.append("#waifu{width:").append(w).append("px !important;height:").append(h)
          .append("px !important;").append(side).append(":0 !important;bottom:0 !important;left:auto !important;right:auto !important;}");
        if (Prefs.rightSide(c)) sb.append("#waifu{right:0 !important}"); else sb.append("#waifu{left:0 !important}");
        sb.append("#live2d{width:100% !important;height:100% !important}");
        if (!tips) sb.append("#waifu-tips{display:none !important}");
        if (!showToolbar) sb.append("#waifu-tool{display:none !important}");
        sb.append("</style></head><body>")
          // 关掉"上次点过关闭"的记忆，否则重载后她会一直不出现
          .append("<script>try{localStorage.removeItem('waifu-display');")
          .append("sessionStorage.removeItem('waifu-dsiplay');}catch(e){}</script>")
          .append("<script type=\"module\" src=\"/widget/waifu-tips.js\"></script>")
          .append("<script>")
          .append("(function(){")
          .append("  var tried=0;")
          .append("  function boot(){")
          .append("    if(typeof window.initWidget!=='function'){")
          .append("      if(++tried<100) return setTimeout(boot,100);")
          .append("      Android.log('initWidget 没就绪'); return;")
          .append("    }")
          .append("    var tools=").append(toolsJson(showToolbar, showClose)).append(";")
          .append("    window.initWidget({")
          .append("      waifuPath:'/widget/waifu-tips.json',")
          .append("      cdnPath:'/modelrepo/',")
          .append("      cubism2Path:'/widget/live2d.min.js',")
          .append("      modelId:0,")
          .append("      tools:tools,")
          .append("      drag:false,")                 // 拖动由原生层做（长按+位置记忆）
          .append("      showToggleAfterQuit:true,")
          .append("      logLevel:'info'")
          .append("    });")
          .append("    Android.log('initWidget 已调用, tools='+tools.join(','));")
          .append("    watch();")
          .append("  }")
          // 关闭（quit）后 #waifu 会被隐藏 → 通知原生层收起整个悬浮窗
          .append("  var done=false;")
          .append("  function tell(){ if(done) return; done=true;")
          .append("    try{ Android.log('JS：检测到关闭/隐藏'); Android.hide(); }catch(e){} }")
          .append("  function hiddenDeep(n){ var g=0;")
          .append("    while(n && n.nodeType===1 && g++<20){")
          .append("      var cs=getComputedStyle(n);")
          .append("      if(cs.display==='none'||cs.visibility==='hidden') return true;")
          .append("      n=n.parentElement; }")
          .append("    return false; }")
          .append("  function watch(){")
          .append("    setInterval(function(){")
          .append("      if(done) return;")
          .append("      var el=document.getElementById('waifu');")
          .append("      if(!el){ tell(); return; }")
          .append("      if(hiddenDeep(el)) tell();")
          .append("    }, 700);")
          // 工具栏区域上报（原生层据此不启动长按拖动，避免点关闭被吃掉）
          .append("    setInterval(function(){")
          .append("      try{ var t=document.getElementById('waifu-tool');")
          .append("        if(!t) return; var b=t.getBoundingClientRect();")
          .append("        if(b.width>0){ Android.setToolbarRect(b.left,b.top,b.width,b.height); }")
          .append("      }catch(e){} }, 3000);")
          .append("  }")
          // 诊断：点击命中
          .append("  document.addEventListener('click', function(ev){")
          .append("    try{ var id=(ev.target.id||ev.target.className||ev.target.tagName||'');")
          .append("      Android.log('页面收到点击 '+Math.round(ev.clientX)+','+Math.round(ev.clientY)+' 命中='+id);")
          .append("    }catch(e){} }, true);")
          .append("  boot();")
          .append("})();")
          .append("</script></body></html>");
        return sb.toString();
    }

    private static String toolsJson(boolean toolbar, boolean close) {
        if (!toolbar) return close ? "['quit']" : "[]";
        StringBuilder t = new StringBuilder("[");
        t.append("'switch-texture','switch-model','hitokoto','photo','info'");
        if (close) t.append(",'quit'");
        return t.append("]").toString();
    }
}
