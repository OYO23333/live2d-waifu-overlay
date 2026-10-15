const fs = require('fs');
const path = require('path');
const posix = path.posix;

const OUT = '/sdcard/Live2D-离线素材';
const API = 'https://live2d.fghrsh.net/api/';
const CDN = 'https://cdn.jsdelivr.net/gh/fghrsh/live2d_api@1.0.1/';
const MAX_FILE = 8 * 1024 * 1024;
const MAX_TOTAL = 120 * 1024 * 1024;

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
      log.push('RETRY(' + i + '/' + tries + ') ' + ((e.cause && e.cause.code) || e.message));
      await new Promise(res => setTimeout(res, 900 * i));
    }
  }
  log.push('ERR 重试耗尽 ' + url);
  return { status: 0 };
}

// 把 ../xxx 解析成 /xxx；把相对模型目录的路径解析成 <模型目录>/xxx
function resolvePath(p, modelDir) {
  if (p.startsWith('../')) return posix.normalize(posix.join('/api', p));
  if (p.startsWith('/')) return p;
  return posix.normalize(posix.join(modelDir || '/api', p));
}

async function fetchTo(absPath) {
  const dest = path.join(OUT, absPath);
  if (fs.existsSync(dest)) return true;
  const rel = absPath.replace(/^\//, '');
  const r = await getBuf(CDN + rel);
  if (!r.buf || r.buf.length > MAX_FILE || total + r.buf.length > MAX_TOTAL) {
    log.push('SKIP ' + (r.status || '') + ' ' + absPath);
    return false;
  }
  total += r.buf.length;
  mkdirp(path.dirname(dest));
  fs.writeFileSync(dest, r.buf);
  log.push('OK   ' + String(r.buf.length).padStart(8) + ' ' + absPath);
  return true;
}

(async () => {
  // 清掉上一轮带 .. 的脏路径
  let cleaned = 0;
  const walk = d => {
    for (const e of fs.readdirSync(d, { withFileTypes: true })) {
      const p = path.join(d, e.name);
      if (e.isDirectory()) {
        if (e.name === '..') continue;
        walk(p);
      } else if (p.includes('/../') || p.endsWith('//')) { fs.unlinkSync(p); cleaned++; }
    }
  };
  walk(OUT);
  log.push('清理脏路径文件: ' + cleaned);

  const okIds = [];
  for (let n = 1; n <= 12; n++) {
    const r = await getBuf(API + 'get/?id=' + n);
    if (!r.buf) continue;
    let j; try { j = JSON.parse(r.buf.toString()); } catch (e) { continue; }
    if (typeof j.model !== 'string' || !/\.moc$/.test(j.model)) { log.push('跳过 id=' + n + '（无有效模型）'); continue; }

    const modelAbs = resolvePath(j.model);
    const modelDir = posix.dirname(modelAbs);
    j.model = modelAbs;
    j.textures = (j.textures || []).map(t => resolvePath(t, modelDir));
    for (const key of ['motions', 'expressions', 'sounds', 'pose', 'physics']) {
      const o = j[key];
      if (!o) continue;
      const fix = v => {
        if (typeof v === 'string') return /\.(mtn|exp\.json|json|ogg|mp3|wav|physics\.json)$/.test(v) || v.startsWith('..') ? resolvePath(v, modelDir) : v;
        if (Array.isArray(v)) return v.map(fix);
        if (v && typeof v === 'object') { const out = {}; for (const k in v) out[k] = fix(v[k]); return out; }
        return v;
      };
      j[key] = fix(o);
    }
    fs.writeFileSync(path.join(OUT, 'api', 'get-' + n + '.json'), JSON.stringify(j));
    okIds.push(n);

    const files = [modelAbs, ...(j.textures || [])];
    const collect = o => {
      if (typeof o === 'string') { if (o.startsWith('/')) files.push(o); return; }
      if (Array.isArray(o)) return o.forEach(collect);
      if (o && typeof o === 'object') Object.values(o).forEach(collect);
    };
    collect(j.motions); collect(j.expressions); collect(j.sounds);
    for (const f of files) await fetchTo(f);
    log.push('--- id=' + n + ' 完成: ' + modelAbs);
  }

  // 本地一言（避免依赖外网）
  fs.writeFileSync(path.join(OUT, 'api', 'hitokoto.json'), JSON.stringify([
    '今天也在认真地看着你哦 ~', '要不要休息一下？', '主人，别一直盯着屏幕嘛',
    '这里是本地离线看板娘', '你已经连续看了很久了，眨眨眼吧'
  ]));

  log.push('有效模型 id: ' + okIds.join(','));
  log.push('本次新增下载: ' + (total / 1048576).toFixed(2) + ' MB');
  fs.writeFileSync(path.join(OUT, 'build.log'), log.join('\n') + '\n');
  console.log(log.filter(l => !l.startsWith('OK   ')).join('\n'));
  console.log('（明细见 /sdcard/Live2D-离线素材/build.log）');
})();
