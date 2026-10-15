package com.eta.kanban;

import android.content.Context;

/**
 * 页面：live2d-widget 引擎 + 本地资源（assets/widget + assets/modelrepo），完全离线。
 * 所有可调参数由 Prefs 注入（人物ID/衣服ID/提示框/工具栏/一言来源…）。
 */
public class Page {

    public static String html(Context c) {
        int alpha = Math.max(20, Math.min(100, Prefs.alpha(c)));
        int modelId = Math.max(1, Prefs.modelId(c));
        int texId = Math.max(1, Prefs.texId(c));
        int tipsW = Math.max(80, Prefs.tipsW(c));
        int tipsH = Math.max(20, Prefs.tipsH(c));
        int tipsFont = Math.max(10, Prefs.tipsFont(c));
        int tipsOffY = Prefs.tipsOffY(c);
        boolean showTips = Prefs.hitokoto(c);
        boolean showToolbar = Prefs.toolbar(c);
        boolean showClose = Prefs.closeBtn(c);
        int toolIcon = Math.max(12, Prefs.toolIcon(c));
        int toolGap = Math.max(0, Prefs.toolGap(c));
        int hito = Prefs.hitoSource(c);          // 0 本地 1 在线 2 关闭

        StringBuilder sb = new StringBuilder(7168);
        sb.append("<!doctype html><html><head><meta charset=\"utf-8\">")
          .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,user-scalable=no\">")
          .append("<link rel=\"stylesheet\" href=\"/widget/waifu.css\">")
          .append("<style>")
          .append("html,body{margin:0;padding:0;background:transparent !important;overflow:hidden;")
          .append("width:100%;height:100%;opacity:")
          .append(String.format(java.util.Locale.US, "%.2f", alpha / 100.0)).append("}");
        // 铺满我们的悬浮窗（窗口位置/大小由原生层按设置算好），去掉它自带的位移与滑入动画
        sb.append("#waifu{position:fixed !important;left:0 !important;right:0 !important;bottom:0 !important;")
          .append("top:auto !important;width:100% !important;height:100% !important;")
          .append("transform:none !important;transition:none !important;z-index:1}");
        sb.append("#waifu:hover{transform:none !important}");
        sb.append("#waifu.waifu-active{bottom:0 !important}");
        // 画布跟随容器（默认写死 300x300）
        sb.append("#live2d{width:auto !important;height:100% !important;max-width:100% !important;margin:0 auto !important}");
        // 它自带的"关闭后唤起按钮"用通知替代
        sb.append("#waifu-toggle{display:none !important}");
        // 工具栏：触屏无 hover 必须常显；图标大小/行高可调；内缩 6px 免得被屏幕边缘裁
        if (showToolbar) {
            sb.append("#waifu-tool{opacity:.92 !important;right:6px !important;top:8px !important;gap:")
              .append(toolGap).append("px !important;line-height:").append(toolIcon + toolGap).append("px !important}");
            sb.append("#waifu-tool svg{height:").append(toolIcon).append("px !important;width:")
              .append(toolIcon).append("px !important;display:block !important;")
              .append("fill:#e6edf5 !important;filter:drop-shadow(0 0 2px rgba(0,0,0,.75))}");
        } else {
            sb.append("#waifu-tool{display:none !important}");
        }
        // 气泡：大小/字号/Y 轴偏移可调
        if (showTips) {
            sb.append("#waifu-tips{width:").append(tipsW).append("px !important;")
              .append("min-height:").append(tipsH).append("px !important;")
              .append("max-height:").append(tipsH).append("px !important;")
              .append("font-size:").append(tipsFont).append("px !important;")
              .append("line-height:").append(Math.max(tipsFont + 6, 18)).append("px !important;")
              .append("margin:").append(tipsOffY).append("px 8px !important;")
              .append("padding:4px 8px !important;border-radius:10px !important;overflow:hidden !important}");
        } else {
            sb.append("#waifu-tips{display:none !important}");
        }
        sb.append("</style></head><body>")
          .append("<script>")
          // 清掉"上次点过关闭"的记忆；并按设置预置人物/衣服
          .append("try{localStorage.removeItem('waifu-display');")
          .append("sessionStorage.removeItem('waifu-dsiplay');")
          .append("localStorage.setItem('modelId','").append(modelId - 1).append("');")
          .append("localStorage.setItem('modelTexturesId','").append(texId - 1).append("');")
          .append("}catch(e){}")
          .append("</script>")
          .append("<script type=\"module\" src=\"/widget/waifu-tips.js\"></script>")
          .append("<script>")
          .append("(function(){")
          .append("  var tried=0;")
          .append("  window.__frames=0;")
          .append("  (function tick(){ window.__frames++; requestAnimationFrame(tick); })();")
          .append("  function boot(){")
          .append("    if(typeof window.initWidget!=='function'){")
          .append("      if(++tried<100) return setTimeout(boot,100);")
          .append("      try{Android.log('initWidget 没就绪');}catch(e){} return;")
          .append("    }")
          .append("    var tools=").append(toolsJson(showToolbar, showClose, hito)).append(";")
          .append("    window.initWidget({")
          .append("      waifuPath:'/widget/waifu-tips.json',")
          .append("      cdnPath:'/modelrepo/',")
          .append("      cubism2Path:'/widget/live2d.min.js',")
          .append("      modelId:").append(modelId - 1).append(',')
          .append("      tools:tools,")
          .append("      drag:false,")
          .append("      showToggleAfterQuit:true,")
          .append("      logLevel:'info'")
          .append("    });")
          .append("    try{Android.log('initWidget 已调用, tools='+tools.join(',')+' modelId=")
          .append(modelId - 1).append(" texId=").append(texId - 1).append("');}catch(e){}")
          .append("    watch();")
          .append("  }")
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
          // 工具栏区域上报（原生层据此不启动长按，避免点按钮被吃掉）
          .append("    setInterval(function(){")
          .append("      try{ var t=document.getElementById('waifu-tool');")
          .append("        if(!t) return; var b=t.getBoundingClientRect();")
          .append("        if(b.width>0){ Android.setToolbarRect(b.left,b.top,b.width,b.height); }")
          .append("      }catch(e){} }, 3000);")
          // 诊断：DOM 几何 / 样式 / rAF（排查渲染问题用，日志走文件）
          .append("    setInterval(function(){")
          .append("      try{")
          .append("        function info(el){ if(!el) return 'null';")
          .append("          var r=el.getBoundingClientRect(), cs=getComputedStyle(el);")
          .append("          return Math.round(r.width)+'x'+Math.round(r.height)")
          .append("            +' d='+cs.display+' v='+cs.visibility+' o='+cs.opacity; }")
          .append("        Android.log('DOM #waifu='+info(document.getElementById('waifu'))")
          .append("          +' #live2d='+info(document.getElementById('live2d'))")
          .append("          +' 帧='+window.__frames);")
          .append("      }catch(e){} }, 8000);")
          .append("  }")
          .append("  document.addEventListener('click', function(ev){")
          .append("    try{ var id=(ev.target.id||ev.target.className||ev.target.tagName||'');")
          .append("      Android.log('页面收到点击 '+Math.round(ev.clientX)+','+Math.round(ev.clientY)+' 命中='+id);")
          .append("    }catch(e){} }, true);")
          .append("  boot();")
          .append("})();")
          .append("</script></body></html>");
        return sb.toString();
    }

    /** 工具栏按钮：已按需求去掉 info；一言关闭时不显示该按钮 */
    private static String toolsJson(boolean toolbar, boolean close, int hitoSource) {
        if (!toolbar) return close ? "['quit']" : "[]";
        StringBuilder t = new StringBuilder("[");
        t.append("'switch-texture','switch-model'");
        if (hitoSource != 2) t.append(",'hitokoto'");
        t.append(",'photo'");
        if (close) t.append(",'quit'");
        return t.append("]").toString();
    }
}
