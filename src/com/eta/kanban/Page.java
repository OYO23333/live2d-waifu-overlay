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
        // 让 #waifu 铺满我们的悬浮窗（窗口位置/大小由原生层按设置算好），
        // 去掉它自带的上移位移与滑入动画，避免和窗口定位打架
        sb.append("#waifu{position:fixed !important;left:0 !important;right:0 !important;bottom:0 !important;")
          .append("top:auto !important;width:100% !important;height:100% !important;")
          .append("transform:none !important;transition:none !important;z-index:1}");
        sb.append("#waifu:hover{transform:none !important}");
        sb.append("#waifu.waifu-active{bottom:0 !important}");
        // 画布跟着容器走（它默认写死 300x300，直接铺满会让模型偏大）
        sb.append("#live2d{width:auto !important;height:100% !important;max-width:100% !important;margin:0 auto !important}");
        // 关闭后那个黄色唤起按钮我们用通知替代，隐藏掉
        sb.append("#waifu-toggle{display:none !important}");
        // 工具栏：触屏没有 hover，必须常显，并挪进窗口内右上角
        if (showToolbar) {
            sb.append("#waifu-tool{opacity:1 !important;right:0 !important;top:6px !important;gap:6px !important}");
            sb.append("#waifu-tool svg{fill:#e6edf5 !important;filter:drop-shadow(0 0 2px rgba(0,0,0,.75))}");
        } else {
            sb.append("#waifu-tool{display:none !important}");
        }
        // 气泡限制在窗口内
        if (tips) {
            sb.append("#waifu-tips{width:calc(100% - 16px) !important;margin:-6px 8px !important;")
              .append("min-height:0 !important;font-size:13px !important;line-height:20px !important;")
              .append("padding:4px 8px !important;border-radius:10px !important}");
        } else {
            sb.append("#waifu-tips{display:none !important}");
        }
        sb.append("</style></head><body>")
          // 关掉"上次点过关闭"的记忆，否则重载后她会一直不出现
          .append("<script>try{localStorage.removeItem('waifu-display');")
          .append("sessionStorage.removeItem('waifu-dsiplay');}catch(e){}</script>")
          .append("<script type=\"module\" src=\"/widget/waifu-tips.js\"></script>")
          .append("<script>")
          .append("(function(){")
          .append("  var tried=0;")
          // 帧计数：判断 requestAnimationFrame 有没有在跑（WebView 里可能被节流）
          .append("  window.__frames=0;")
          .append("  (function tick(){ window.__frames++; requestAnimationFrame(tick); })();")
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
          // 诊断：自报关键元素的几何与样式，排查"画不出来"
          .append("    setInterval(function(){")
          .append("      try{")
          .append("        function info(el){ if(!el) return 'null';")
          .append("          var r=el.getBoundingClientRect(), cs=getComputedStyle(el);")
          .append("          return Math.round(r.left)+','+Math.round(r.top)+' '+Math.round(r.width)+'x'+Math.round(r.height)")
          .append("            +' d='+cs.display+' v='+cs.visibility+' o='+cs.opacity+' tr='+cs.transform; }")
          .append("        Android.log('DOM #waifu='+info(document.getElementById('waifu')));")
          .append("        Android.log('DOM #live2d='+info(document.getElementById('live2d'))")
          .append("          +' attr='+(document.getElementById('live2d')||{}).width+'x'+(document.getElementById('live2d')||{}).height);")
          .append("        var cv=document.getElementById('waifu-canvas');")
          .append("        if(cv) Android.log('DOM container='+info(cv));")
          .append("        Android.log('DOM rAF 帧数='+window.__frames+' 可见性='+document.visibilityState);")
          .append("      }catch(e){} }, 4000);")
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
