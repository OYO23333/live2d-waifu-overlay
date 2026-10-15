package com.eta.kanban;

import android.content.Context;

/** 生成看板娘页面：把设置注入成 kbn_settingN，并加胶水让"关闭/隐藏"能通知到 Service */
public class Page {

    public static String html(Context c) {
        int model = Prefs.modelId(c);
        int tex = Prefs.texId(c);
        int w = Prefs.sizeW(c);
        int h = Prefs.sizeH(c);
        boolean showToolbar = Prefs.toolbar(c);
        boolean showClose = Prefs.closeBtn(c);
        boolean tips = Prefs.hitokoto(c);
        int alpha = Math.max(10, Math.min(100, Prefs.alpha(c)));
        String side = Prefs.rightSide(c) ? "right" : "left";

        StringBuilder sb = new StringBuilder(6144);
        sb.append("<!doctype html><html><head><meta charset=\"utf-8\">")
          .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,user-scalable=no\">")
          .append("<style>html,body{margin:0;padding:0;background:transparent !important;")
          .append("overflow:hidden;width:100%;height:100%;opacity:")
          .append(String.format(java.util.Locale.US, "%.2f", alpha / 100.0)).append("}");
        if (!tips) sb.append(".waifu-tips{display:none !important}");
        if (!showToolbar) sb.append(".waifu-tool{display:none !important}");
        if (showToolbar && Prefs.alwaysToolbar(c))
            sb.append(".waifu-tool{display:block !important}");
        sb.append("</style></head><body>")
          .append("<script>")
          // kbn.js 会在关闭时往 sessionStorage 写 waifu-dsiplay=none；
          // 不在这里清掉的话，重新加载页面她会一直是隐藏的（"叫不回来"）
          .append("try{sessionStorage.removeItem('waifu-dsiplay');}catch(e){}")
          .append("window.kbn_setting0=").append(model).append(';')
          .append("window.kbn_setting1=").append(tex).append(';')
          .append("window.kbn_setting2=").append(showClose).append(';')
          .append("window.kbn_setting3='").append(w).append('x').append(h).append("';")
          .append("window.kbn_setting4='").append(side).append(":0';")
          .append("window.kbn_setting5='190x56';")
          .append("window.kbn_setting6='14px';")
          .append("window.kbn_setting7='-14px';")
          .append("window.kbn_setting8='22px';")
          .append("window.kbn_setting9='36px';")
          .append("window.kbn_setting10='hitokoto.cn';")
          .append("</script>")
          .append("<script src=\"kbn.js\"></script>")
          .append("<script>")
          .append("(function(){")
          .append("  var done=false;")
          .append("  function tell(){ if(done) return; done=true;")
          .append("    try{ Android.log('JS：检测到关闭/隐藏'); Android.hide(); }catch(e){} }")
          // 逐级向上看有没有被隐藏（kbn.js 关闭时是把外层 .waifu 设成 display:none）
          .append("  function hiddenDeep(node){")
          .append("    var n=node, guard=0;")
          .append("    while(n && n.nodeType===1 && guard++<20){")
          .append("      var cs=getComputedStyle(n);")
          .append("      if(cs.display==='none' || cs.visibility==='hidden') return true;")
          .append("      n=n.parentElement;")
          .append("    }")
          .append("    return false;")
          .append("  }")
          .append("  function check(){")
          .append("    if(done) return;")
          .append("    var el=document.getElementById('live2d');")
          .append("    if(!el){ tell(); return; }")
          .append("    var wrap=document.querySelector('.waifu') || el;")
          .append("    if(hiddenDeep(wrap)) tell();")
          .append("  }")
          .append("  setInterval(check, 700);")
          // 最可靠的一条：直接绑关闭按钮
          .append("  var tries=0;")
          .append("  var bind=setInterval(function(){")
          .append("    var b=document.querySelector('.waifu-tool .fui-cross');")
          .append("    if(b){ b.addEventListener('click', function(){ setTimeout(tell, 80); });")
          .append("           clearInterval(bind);")
          .append("           try{ Android.log('已绑定关闭按钮'); }catch(e){} }")
          .append("    else if(++tries>24){ clearInterval(bind); }")
          .append("  }, 500);")
          // 把工具栏区域报给服务：落在它上面的触摸不启动长按拖动，否则点 X 会被吃掉
          .append("  function reportRect(){")
          .append("    try{")
          .append("      Android.log('视口 '+window.innerWidth+'x'+window.innerHeight+' dpr='+window.devicePixelRatio);")
          .append("      var t=document.querySelector('.waifu-tool');")
          .append("      if(!t) return;")
          .append("      var b=t.getBoundingClientRect();")
          .append("      Android.setToolbarRect(b.left, b.top, b.width, b.height);")
          .append("    }catch(e){}")
          .append("  }")
          .append("  setTimeout(reportRect, 2500);")
          .append("  setInterval(reportRect, 3000);")
          // 诊断用：报告每一次点击落在页面的哪个坐标、命中了什么
          .append("  document.addEventListener('click', function(ev){")
          .append("    try{ var c=(ev.target.className||ev.target.tagName||'');")
          .append("      Android.log('页面收到点击 client='+Math.round(ev.clientX)+','+Math.round(ev.clientY)+' 命中='+c); }catch(e){}")
          .append("  }, true);")
          .append("  setTimeout(function(){")
          .append("    try{")
          .append("      ['fui-eye','fui-user','fui-cross'].forEach(function(cls){")
          .append("        var e=document.querySelector('.waifu-tool .'+cls);")
          .append("        if(e){ var bb=e.getBoundingClientRect();")
          .append("          Android.log('图标 '+cls+' 中心 '+Math.round(bb.left+bb.width/2)+','+Math.round(bb.top+bb.height/2)); }")
          .append("      });")
          .append("    }catch(err){}")
          .append("  }, 2500);")
          .append("})();")
          .append("</script>")
          .append("</body></html>");
        return sb.toString();
    }
}
