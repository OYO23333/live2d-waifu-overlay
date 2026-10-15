package com.eta.kanban;

import android.content.Context;

/** 生成看板娘页面：把设置注入成 kbn_settingN，并加一点胶水让"关闭"能通知到 Service */
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

        StringBuilder sb = new StringBuilder(4096);
        sb.append("<!doctype html><html><head><meta charset=\"utf-8\">")
          .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,user-scalable=no\">")
          .append("<style>html,body{margin:0;padding:0;background:transparent !important;")
          .append("overflow:hidden;width:100%;height:100%;opacity:")
          .append(String.format(java.util.Locale.US, "%.2f", alpha / 100.0)).append("}");
        if (!tips) sb.append(".waifu-tips{display:none !important}");
        sb.append("</style></head><body>")
          .append("<script>")
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
          .append("  function tell(){ try{ Android.hide(); }catch(e){} }")
          .append("  var gone=false;")
          .append("  function checkRemoved(){")
          .append("    var el=document.getElementById('live2d');")
          .append("    if(!el){ if(!gone){gone=true; tell();} }")
          .append("    else if(el.style.display==='none'){ if(!gone){gone=true; tell();} }")
          .append("    else { gone=false; }")
          .append("  }")
          .append("  setInterval(checkRemoved, 900);")
          .append("})();")
          .append("</script>");

        if (!showToolbar) {
            sb.append("<style>.waifu-tool{display:none !important}</style>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }
}
