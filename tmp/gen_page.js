const fs = require('fs');
const path = require('path');
const OUT = '/sdcard/Live2D-离线素材';
const DEFAULTS = [5]; // 默认模型 id=5 (ShizukuTalk/shizuku-48)

const html = `<!doctype html>
<html lang="zh"><head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,user-scalable=no">
<title>本地看板娘</title>
<style>
  html,body{margin:0;height:100%;background:#0f1216;overflow:hidden}
  #debug{position:fixed;left:6px;bottom:6px;font:11px/1.35 monospace;color:#8f8;
         z-index:99999;white-space:pre;pointer-events:none;text-shadow:0 0 2px #000}
</style>
</head><body>
<div id="debug">boot…</div>
<script src="shim.js"></script>
<script>
  window.kbn_setting0  = 5;          /* 人物ID: ShizukuTalk/shizuku-48 */
  window.kbn_setting1  = 2;          /* 贴图/衣服ID */
  window.kbn_setting2  = true;       /* 显示关闭按钮 */
  window.kbn_setting3  = '280x260';  /* 看板娘大小 */
  window.kbn_setting4  = 'right:0';  /* 停靠侧 */
  window.kbn_setting5  = '180x56';   /* 提示框大小 */
  window.kbn_setting6  = '14px';
  window.kbn_setting7  = '-14px';
  window.kbn_setting8  = '22px';
  window.kbn_setting9  = '36px';
  window.kbn_setting10 = 'hitokoto.cn';
</script>
<script src="kbn.local.js"></script>
</body></html>
`;

const shim = `(function () {
  var log = [];
  window.__shimLog = log;
  function dbg() {
    var d = document.getElementById('debug');
    if (d) d.textContent = log.slice(-7).join('\\n');
  }
  function push(s) { log.push(s); dbg(); }

  function rewrite(url) {
    var u = String(url), m;
    if ((m = u.match(/api\\/get\\/\\?id=(\\d+)/)))          return '/api/get-' + m[1] + '.json';
    if ((m = u.match(/rand_textures\\/\\?id=(\\d+)-(\\d+)/)))   return '/api/rand_textures-' + m[1] + '-' + m[2] + '.json';
    if ((m = u.match(/switch_textures\\/\\?id=(\\d+)-(\\d+)/))) return '/api/switch_textures-' + m[1] + '-' + m[2] + '.json';
    if ((m = u.match(/api\\/switch\\/\\?id=(\\d+)/)))          return '/api/switch-' + m[1] + '.json';
    if ((m = u.match(/api\\/rand\\/\\?id=(\\d+)/)))            return '/api/switch-' + m[1] + '.json';
    if (/model_list\\.json/.test(u))                          return '/api/model_list.json';
    if (/(hitokoto\\.cn|jinrishici\\.com|api\\.fghrsh\\.net)/.test(u))
      return '/api/hito-' + (1 + Math.floor(Math.random() * 6)) + '.json';
    if (u.indexOf('cdn.jsdelivr.net') >= 0)
      return u.replace(/^.*live2d_api@[0-9.]+\\//, '/');
    return u;
  }

  var open = XMLHttpRequest.prototype.open;
  XMLHttpRequest.prototype.open = function (m, url) {
    var nw = rewrite(url);
    if (nw !== url) push('XHR ' + nw);
    return open.apply(this, [m, nw].concat([].slice.call(arguments, 2)));
  };

  var origFetch = window.fetch;
  if (origFetch) {
    window.fetch = function (u, o) {
      if (typeof u === 'string') {
        var nw = rewrite(u);
        if (nw !== u) push('fetch ' + nw);
        u = nw;
      }
      return origFetch.call(this, u, o);
    };
  }

  window.addEventListener('error', function (e) {
    push('ERROR ' + (e.message || e.type));
  }, true);
  window.addEventListener('unhandledrejection', function (e) {
    push('REJECT ' + (e.reason && (e.reason.message || e.reason)));
  });
  setTimeout(function () { push('加载完成检查：' + (window.Live2D ? 'Live2D 已就绪' : 'Live2D 未定义')); }, 3000);
})();
`;

fs.writeFileSync(path.join(OUT, 'index.html'), html);
fs.writeFileSync(path.join(OUT, 'shim.js'), shim);

// API 静态应答
const apiDir = path.join(OUT, 'api');
for (let n = 1; n <= 7; n++) {
  fs.writeFileSync(path.join(apiDir, 'switch-' + n + '.json'),
    JSON.stringify({ model: { id: n, message: ['本地模型 1', '本地模型 2', '本地模型 3', '本地模型 4', '本地模型 5', '本地模型 6', '本地模型 7'][n - 1] } }));
  for (let m = 1; m <= 4; m++) {
    const body = JSON.stringify({ textures: { id: m } });
    fs.writeFileSync(path.join(apiDir, 'rand_textures-' + n + '-' + m + '.json'), body);
    fs.writeFileSync(path.join(apiDir, 'switch_textures-' + n + '-' + m + '.json'), body);
  }
}
const lines = [
  '离线看板娘来啦～', '记得偶尔看看远处哦', '要不要喝口水？',
  '我一直在这里陪着你', '别熬太晚，早点睡', '今天也辛苦啦'
];
for (let i = 0; i < lines.length; i++) {
  fs.writeFileSync(path.join(apiDir, 'hito-' + (i + 1) + '.json'),
    JSON.stringify({ hitokoto: lines[i], from: '本地离线', type: 'a' }));
}
fs.copyFileSync(path.join(OUT, 'model_list.json'), path.join(apiDir, 'model_list.json'));

console.log('生成完毕：index.html, shim.js, api 静态应答');
console.log('api 文件数:', fs.readdirSync(apiDir).length);
console.log('总大小:', (fs.readdirSync(OUT).reduce((a, f) => {
  const p = path.join(OUT, f);
  return a + (fs.statSync(p).isDirectory() ? 0 : fs.statSync(p).size);
}, 0) / 1048576).toFixed(2), 'MB（不含 model 目录）');
