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
        // 穿透按钮开着时换成暖色高亮，一眼能看出"她现在不听触摸"
        sb.append("#waifu-tool #eta-touch.on svg{fill:#ffd166 !important;")
          .append("filter:drop-shadow(0 0 3px rgba(255,209,102,.9)) !important}");
        // 视觉尺寸不变，手指好点一点（伪元素扩热区）
        sb.append("#waifu-tool #eta-touch{position:relative !important}")
          .append("#waifu-tool #eta-touch::before{content:'';position:absolute !important;")
          .append("left:-10px !important;right:-10px !important;top:-8px !important;bottom:-8px !important}");
        if (showTips) {
            int offY = Math.max(0, tipsOffY);
            sb.append("#waifu-tips{position:absolute !important;left:8px !important;right:8px !important;")
              .append("top:").append(offY).append("px !important;")
              .append("width:auto !important;box-sizing:border-box !important;")
              .append("min-height:0 !important;max-height:").append(tipsH).append("px !important;")
              .append("font-size:").append(tipsFont).append("px !important;")
              .append("line-height:").append(Math.max(tipsFont + 6, 18)).append("px !important;")
              .append("margin:0 !important;padding:5px 10px !important;border-radius:10px !important;")
              .append("overflow:hidden !important}");
            sb.append("#waifu-tips{width:min(").append(tipsW).append("px, calc(100% - 16px)) !important}");
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
          .append("    addTouchButton(").append(Prefs.touchThrough(c)).append(");")
          .append("    watch();")
          .append("  }")
          // 工具栏追加"点击穿透"按钮（图标风格与其它按钮一致，插在关闭按钮之前）
          .append("  function addTouchButton(on){")
          .append("    try{")
          .append("      var tool=document.getElementById('waifu-tool');")
          .append("      if(!tool||document.getElementById('eta-touch')) return;")
          .append("      var b=document.createElement('span');")
          .append("      b.id='eta-touch';")
          .append("      b.innerHTML='<svg viewBox=\"0 0 24 24\"><path d=\"M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 2.6c1.86 0 3.56.69 4.85 1.82L6.42 16.85A7.38 7.38 0 0 1 4.6 12c0-4.08 3.32-7.4 7.4-7.4zm0 14.8c-1.86 0-3.56-.69-4.85-1.82L17.58 7.15A7.38 7.38 0 0 1 19.4 12c0 4.08-3.32 7.4-7.4 7.4z\"/></svg>';")
          .append("      b.style.cursor='pointer';")
          .append("      b.addEventListener('click',function(ev){ ev.stopPropagation();")
          .append("        try{ Android.toggleTouch(); }catch(e){} }, true);")
          .append("      var quit=tool.querySelector('.fui-cross');")
          .append("      if(quit) tool.insertBefore(b,quit); else tool.appendChild(b);")
          .append("      window.__etaTouch=function(state){")
          .append("        b.className = state ? 'on' : '';")
          .append("        b.title = state ? '点击穿透已开启：她现在是纯装饰，切回请用通知栏按钮'")
          .append("                      : '点击穿透：点一下，触摸就会穿到她后面的应用';")
          .append("      };")
          .append("      window.__etaTouch(on);")
          .append("      try{Android.log('已加装点击穿透按钮, 当前='+on);}catch(e){}")
          .append("    }catch(e){}")
          .append("  }")
          .append("  var done=false;")
          .append("  function tell(){ if(done) return; done=true; window.__closed=true;")
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
          // 诊断：只在“她没渲染出来”时报警，正常时闭嘴（日志走文件，避免无限增长）
          .append("    setInterval(function(){")
          .append("      try{")
          // 注意：拼出来的 JS 全在一行，append 里绝不能出现 // 行注释（会把后面全部吃掉）
          .append("        if(window.__closed) return;")
          .append("        var el=document.getElementById('waifu'); if(!el) return;")
          .append("        var r=el.getBoundingClientRect(), cs=getComputedStyle(el);")
          .append("        if(r.width<2||r.height<2||cs.display==='none'||cs.visibility==='hidden'){")
          .append("          Android.log('警告：看板娘没渲染出来 #waifu='+Math.round(r.width)+'x'+Math.round(r.height)")
          .append("            +' d='+cs.display+' v='+cs.visibility+' 帧='+window.__frames);")
          .append("        }")
          .append("      }catch(e){} }, 15000);")
          .append("  }")
          // live2d-widget 会给 canvas 的 touchstart/touchend 调 preventDefault（它自己的 touchEvent），
          // 一 preventDefault，Chromium 就不合成 click / mouseover：点她没反应、也不说话。
          // 它那套触摸逻辑是给网页版拖动用的（我们 drag:false 用不上），拦掉即可让点击回归默认行为。
          .append("  function isCanvas(g){ return !!g && (g.id==='live2d'||g.tagName==='CANVAS'); }")
          .append("  ['touchstart','touchend','touchmove'].forEach(function(t){")
          .append("    document.addEventListener(t, function(ev){")
          .append("      if(isCanvas(ev.target)) ev.stopPropagation();")
          .append("    }, true);")
          .append("  });")
          // 兜底：万一这台设备就是不合成鼠标事件，轻点她时我们自己让她说一句
          .append("  function pickTip(j){")
          .append("    try{")
          .append("      var all=(j.mouseover||[]).concat(j.click||[]), pool=[];")
          .append("      all.forEach(function(it){")
          .append("        if(!it) return;")
          .append("        var list=(typeof it==='string') ? [it] : (it.text||[]);")
          .append("        list.forEach(function(s){ if(s) pool.push(String(s).replace(/\\{[^}]*\\}/g,'')")
          .append("          .replace(/\\s+/g,' ').trim()); });")
          .append("      });")
          .append("      if(!pool.length) return '嗯？';")
          .append("      return pool[Math.floor(Math.random()*pool.length)];")
          .append("    }catch(e){ return '嗯？'; }")
          .append("  }")
          .append("  function forceSpeak(box, txt){")
          .append("    box.innerHTML=txt;")
          .append("    box.style.setProperty('display','block','important');")
          .append("    box.style.setProperty('opacity','1','important');")
          .append("    box.style.setProperty('visibility','visible','important');")
          .append("    clearTimeout(window.__tipsTimer);")
          .append("    window.__tipsTimer=setTimeout(function(){")
          .append("      box.style.removeProperty('display'); box.style.removeProperty('opacity');")
          .append("      box.style.removeProperty('visibility'); },3000);")
          .append("  }")
          .append("  function tapTalk(delay){")
          .append("    setTimeout(function(){")
          .append("      try{")
          .append("        var box=document.getElementById('waifu-tips'); if(!box) return;")
          .append("        var cs=getComputedStyle(box);")
          .append("        if(box.innerHTML && box.innerHTML.trim() && cs.opacity!=='0'")
          .append("           && cs.display!=='none' && cs.visibility!=='hidden') return;")
          .append("        fetch('/widget/waifu-tips.json').then(function(r){return r.json();}).then(function(j){")
          .append("          forceSpeak(box, pickTip(j));")
          .append("        }).catch(function(){ forceSpeak(box,'嗯？'); });")
          .append("      }catch(e){} }, delay||300);")
          .append("  }")
          .append("  var tsx=0,tsy=0,tst=0;")
          .append("  document.addEventListener('touchstart', function(ev){")
          .append("    if(!isCanvas(ev.target)) return; window.__gotClick=false;")
          .append("    var t=ev.touches[0]; if(!t) return;")
          .append("    tsx=t.clientX; tsy=t.clientY; tst=Date.now();")
          .append("  }, true);")
          .append("  document.addEventListener('touchend', function(ev){")
          .append("    if(!isCanvas(ev.target)||!tst) return;")
          .append("    var t=ev.changedTouches[0]; if(!t) return;")
          .append("    var dt=Date.now()-tst; tst=0;")
          .append("    if(dt>600||Math.abs(t.clientX-tsx)>14||Math.abs(t.clientY-tsy)>14) return;")
          .append("    setTimeout(function(){ if(!window.__gotClick) tapTalk(120); }, 420);")
          .append("  }, true);")
          .append("  document.addEventListener('click', function(ev){")
          .append("    window.__gotClick=true;")
          .append("    if(isCanvas(ev.target)) tapTalk(450);")
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
