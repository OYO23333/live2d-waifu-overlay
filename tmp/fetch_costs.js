const fs = require('fs');
const path = require('path');
const posix = path.posix;

const API = 'https://live2d.fghrsh.net/api/';
const CDN = 'https://cdn.jsdelivr.net/gh/fghrsh/live2d_api@1.0.1/';
const OUT = '/workspace/kanban/assets';
const MAX_TOTAL = 400 * 1024 * 1024;

let total = 0;
const log = [];
const mkdirp = p => fs.mkdirSync(p, { recursive: true });

async function getBuf(url, tries = 4) {
  for (let i = 1; i <= tries; i++) {
    try {
      const r = await fetch(url);
      if (!r.ok) return { status: r.status };
      return { status: r.status, buf: Buffer.from(await r.arrayBuffer()) };
    } catch (e) {
      await new Promise(res => setTimeout(res, 700 * i));
    }
  }
  return { status: 0 };
}

async function j(url) {
  const r = await getBuf(url);
  if (!r.buf) return null;
  try { return JSON.parse(r.buf.toString()); } catch (e) { return null; }
}

function resolvePath(p, modelDir) {
  if (p.startsWith('../')) return posix.normalize(posix.join('/api', p));
  if (p.startsWith('/')) return p;
  return posix.normalize(posix.join(modelDir || '/api', p));
}

async function fetchTo(absPath) {
  const dest = path.join(OUT, absPath);
  if (fs.existsSync(dest)) return true;
  if (total > MAX_TOTAL) { log.push('SKIP(超上限) ' + absPath); return false; }
  const r = await getBuf(CDN + absPath.replace(/^\//, ''));
  if (!r.buf) { log.push('FAIL ' + r.status + ' ' + absPath); return false; }
  total += r.buf.length;
  mkdirp(path.dirname(dest));
  fs.writeFileSync(dest, r.buf);
  return true;
}

(async () => {
  const summary = {};
  for (let n = 1; n <= 7; n++) {
    const seen = new Map();          // 纹理组合 -> 规范化后的 JSON
    for (let m = 1; m <= 40; m++) {
      const d = await j(API + 'get/?id=' + n + '-' + m);
      if (!d || typeof d.model !== 'string' || !/\.moc$/.test(d.model)) continue;
      const key = JSON.stringify([d.model, d.textures]);
      if (seen.has(key)) continue;
      const modelAbs = resolvePath(d.model);
      const modelDir = posix.dirname(modelAbs);
      d.model = modelAbs;
      d.textures = (d.textures || []).map(t => resolvePath(t, modelDir));
      for (const k of ['motions', 'expressions', 'sounds', 'pose', 'physics']) {
        if (!d[k]) continue;
        const fix = v => {
          if (typeof v === 'string') return /\.(mtn|exp\.json|json|ogg|mp3|wav)$/.test(v) || v.startsWith('..') ? resolvePath(v, modelDir) : v;
          if (Array.isArray(v)) return v.map(fix);
          if (v && typeof v === 'object') { const o = {}; for (const kk in v) o[kk] = fix(v[kk]); return o; }
          return v;
        };
        d[k] = fix(d[k]);
      }
      seen.set(key, d);
    }

    const sets = [...seen.values()];
    summary[n] = sets.length;
    // 写每个服装组的 get JSON
    for (let k = 0; k < sets.length; k++) {
      const idx = k + 1;
      fs.writeFileSync(path.join(OUT, 'api', 'get-' + n + '-' + idx + '.json'), JSON.stringify(sets[k]));
      // 兼容老路径（默认组）
      if (idx === 1) fs.writeFileSync(path.join(OUT, 'api', 'get-' + n + '.json'), JSON.stringify(sets[k]));
      // 换装：确定性轮转（按一次换到下一套，最后一套回到第一套）
      const next = (idx % sets.length) + 1;
      const body = JSON.stringify({ textures: { id: next } });
      fs.writeFileSync(path.join(OUT, 'api', 'rand_textures-' + n + '-' + idx + '.json'), body);
      fs.writeFileSync(path.join(OUT, 'api', 'switch_textures-' + n + '-' + idx + '.json'), body);
    }

    // 下载该模型所有服装组引用的文件
    for (const s of sets) {
      const files = [s.model, ...(s.textures || [])];
      const walk = o => {
        if (typeof o === 'string') { if (o.startsWith('/')) files.push(o); return; }
        if (Array.isArray(o)) return o.forEach(walk);
        if (o && typeof o === 'object') Object.values(o).forEach(walk);
      };
      walk(s.motions); walk(s.expressions); walk(s.sounds);
      for (const f of files) await fetchTo(f);
    }
    console.log('id=' + n + ' 服装组 ' + sets.length + ' 套，累计下载 ' + (total / 1048576).toFixed(1) + ' MB');
  }

  log.push('服装组统计: ' + JSON.stringify(summary));
  log.push('下载总量: ' + (total / 1048576).toFixed(1) + ' MB');
  fs.writeFileSync('/workspace/fetch_costs.log', log.join('\n') + '\n');
  console.log('完成。服装组: ' + JSON.stringify(summary));
  console.log('下载总量: ' + (total / 1048576).toFixed(1) + ' MB');
})();
