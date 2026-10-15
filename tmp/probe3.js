const base = 'https://cdn.jsdelivr.net/gh/fghrsh/live2d_api@1.0.1/';
(async () => {
  const r = await fetch(base + 'model_list.json');
  const j = await r.json();
  console.log('model_list.json 顶层键:', Object.keys(j));
  for (const k of Object.keys(j)) {
    const v = j[k];
    console.log('  ', k, Array.isArray(v) ? '(' + v.length + ') ' + JSON.stringify(v.slice(0, 6)) : typeof v);
  }
  // 试一个具体模型目录
  const samples = [
    'model/Potion-Maker/Pio/index.json',
    'model/Potion-Maker/Pio/textures/default-costume.png',
    'model/Potion-Maker/Pio/Pio.moc',
    'model/hibiki/index.json',
    'model/bilibili-live/22/index.json'
  ];
  for (const p of samples) {
    try {
      const rr = await fetch(base + p);
      const len = rr.headers.get('content-length');
      console.log(String(rr.status).padEnd(4), p, 'len=' + len);
      if (rr.ok && p.endsWith('.json')) {
        const t = await rr.text();
        console.log('      ', t.replace(/\s+/g, ' ').slice(0, 220));
      }
    } catch (e) { console.log('ERR ', p, e.message); }
  }
})();
