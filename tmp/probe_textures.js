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
  const summary = {};
  for (let n = 1; n <= 7; n++) {
    const sets = [];
    for (let m = 1; m <= 10; m++) {
      const d = await j(API + 'get/?id=' + n + '-' + m);
      if (!d || typeof d.model !== 'string' || !/\.moc$/.test(d.model)) {
        sets.push({ m, ok: false });
        continue;
      }
      const tex = (d.textures || []).join('|');
      sets.push({ m, ok: true, model: d.model, tex, ntex: (d.textures || []).length });
    }
    const valid = sets.filter(s => s.ok);
    const uniq = new Set(valid.map(s => s.tex));
    summary[n] = { valid: valid.map(s => s.m), uniqueSets: uniq.size, first: valid[0] };
    console.log('模型 id=' + n
      + '  有效贴图组 M=' + valid.map(s => s.m).join(',')
      + '  不同组合数=' + uniq.size
      + '  默认模型=' + (valid[0] ? valid[0].model : '?'));
    if (uniq.size > 1) {
      const seen = new Set();
      valid.forEach(s => {
        const key = s.tex.slice(0, 60);
        if (!seen.has(key)) { seen.add(key); console.log('     M=' + s.m + ' 贴图: ' + s.tex.slice(0, 120)); }
      });
    }
  }
  console.log('\n=== 汇总 ===');
  console.log(JSON.stringify(summary, null, 1).slice(0, 1500));
})();
