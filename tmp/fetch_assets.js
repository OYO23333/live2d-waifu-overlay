const fs = require('fs');
const path = require('path');
const ROOT = '/sdcard/Live2D-离线素材';
const base = 'https://cdn.jsdelivr.net/gh/fghrsh/live2d_api@1.0.1/';

function mkdirp(p) { fs.mkdirSync(p, { recursive: true }); }

async function get(url, dest) {
  const r = await fetch(url);
  if (!r.ok) { console.log('FAIL', r.status, url); return null; }
  const buf = Buffer.from(await r.arrayBuffer());
  if (dest) { mkdirp(path.dirname(dest)); fs.writeFileSync(dest, buf); }
  console.log('OK  ', String(buf.length).padStart(8), dest || url);
  return buf;
}

(async () => {
  mkdirp(ROOT);
  // 1) 引擎
  fs.copyFileSync('/workspace/kbn_upstream.js', path.join(ROOT, 'kbn.js'));
  console.log('OK  ', String(fs.statSync(path.join(ROOT, 'kbn.js')).size).padStart(8), ROOT + '/kbn.js');
  // 2) 模型清单
  await get(base + 'model_list.json', path.join(ROOT, 'model_list.json'));
  // 3) 一个模型：Potion-Maker/Pio
  const mdir = path.join(ROOT, 'model/Potion-Maker/Pio');
  const idx = await get(base + 'model/Potion-Maker/Pio/index.json', path.join(mdir, 'index.json'));
  if (idx) {
    const j = JSON.parse(idx.toString());
    console.log('   index.json:', JSON.stringify(j));
    await get(base + 'model/Potion-Maker/Pio/' + j.model, path.join(mdir, j.model));
    for (const t of j.textures) await get(base + 'model/Potion-Maker/Pio/' + t, path.join(mdir, t));
  }
  // 4) 引擎里跟 API 地址有关的上下文
  const kbn = fs.readFileSync('/workspace/kbn_upstream.js', 'utf8');
  for (const key of ['live2d_api', 'home_Path', 'cdn.jsdelivr']) {
    let i = -1, n = 0;
    while ((i = kbn.indexOf(key, i + 1)) !== -1 && n < 3) {
      console.log('--- 上下文', key, '#', ++n, '---');
      console.log(kbn.slice(Math.max(0, i - 160), i + 200).replace(/\n/g, ' '));
    }
  }
})();
