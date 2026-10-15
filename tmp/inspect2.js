const fs = require('fs');
const t = fs.readFileSync('/workspace/kbn_upstream.js', 'utf8');

console.log('===== modelAPI 的所有用法 =====');
let i = -1, n = 0;
while ((i = t.indexOf('modelAPI', i + 1)) !== -1) {
  n++;
  console.log('[' + n + '] ...' + t.slice(Math.max(0, i - 70), i + 150).replace(/\n/g, ' ') + '...');
}
console.log('modelAPI 出现次数:', n);

console.log('\n===== 关键设置项 =====');
const m = t.match(/live2d_settings\.[A-Za-z_]+=[^;,]{0,60}/g) || [];
[...new Set(m)].slice(0, 30).forEach(s => console.log('   ', s));

console.log('\n===== 探测 fghrsh 远端 API =====');
const urls = [
  'https://live2d.fghrsh.net/api/',
  'https://live2d.fghrsh.net/api/get/?id=1',
  'https://live2d.fghrsh.net/api/get/?id=6',
  'https://live2d.fghrsh.net/api/1_textures/?id=1-1',
  'https://live2d.fghrsh.net/api/model_list.json',
  'https://cdn.jsdelivr.net/gh/fghrsh/live2d_api@1.0.1/get/model/Potion-Maker/Pio/index.json',
  'https://cdn.jsdelivr.net/gh/fghrsh/live2d_api@1.0.1/model/Potion-Maker/Pio/model.moc'
];
(async () => {
  for (const u of urls) {
    try {
      const r = await fetch(u, { method: 'GET' });
      const txt = r.ok || r.status < 500 ? (await r.text()).slice(0, 150).replace(/\s+/g, ' ') : '';
      console.log(String(r.status).padEnd(4), u, '|', txt);
    } catch (e) { console.log('ERR ', u, e.message); }
  }
})();
