const API = 'https://live2d.fghrsh.net/api/';

async function j(url) {
  try {
    const r = await fetch(url);
    if (!r.ok) return null;
    const t = await r.text();
    try { return JSON.parse(t); } catch (e) { return null; }
  } catch (e) { return null; }
}

(async () => {
  const all = {};
  const files = new Set();
  for (let n = 1; n <= 7; n++) {
    const valid = [];
    for (let m = 1; m <= 40; m++) {
      const d = await j(API + 'get/?id=' + n + '-' + m);
      if (!d || typeof d.model !== 'string' || !/\.moc$/.test(d.model)) continue;
      const tex = (d.textures || []);
      if (!tex.length) continue;
      valid.push(m);
      tex.forEach(t => files.add(t.replace('../', '')));
    }
    all[n] = { count: valid.length, sets: valid };
    console.log('模型 id=' + n + '  共 ' + valid.length + ' 套衣服  M=' + valid.slice(0, 12).join(',') + (valid.length > 12 ? ' …' : ''));
  }
  console.log('\n需要的贴图文件总数: ' + files.size);
  console.log('按大小估算（含 moc/mtn 已下过）:');
  const sample = [...files].slice(0, 3);
  sample.forEach(s => console.log('   例: ' + s));
  const byModel = {};
  [...files].forEach(f => {
    const g = f.split('/').slice(0, 2).join('/');
    byModel[g] = (byModel[g] || 0) + 1;
  });
  console.log('按模型分组的贴图文件数:');
  Object.entries(byModel).forEach(([k, v]) => console.log('   ' + k + ' -> ' + v + ' 个'));
  console.log('\nJSON: ' + JSON.stringify(all));
})();
