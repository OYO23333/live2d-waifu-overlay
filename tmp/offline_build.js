const fs = require('fs');
const path = require('path');

const OUT = '/sdcard/Live2D-离线素材';
const API = 'https://live2d.fghrsh.net/api/';
const CDN = 'https://cdn.jsdelivr.net/gh/fghrsh/live2d_api@1.0.1/';
const MAX_FILE = 8 * 1024 * 1024;
const MAX_TOTAL = 80 * 1024 * 1024;

let total = 0;
const log = [];

function mkdirp(p) { fs.mkdirSync(p, { recursive: true }); }

async function getBuf(url, tries = 4) {
  for (let i = 1; i <= tries; i++) {
    try {
      const r = await fetch(url);
      if (!r.ok) return { status: r.status };
      const b = Buffer.from(await r.arrayBuffer());
      return { status: r.status, buf: b };
    } catch (e) {
      log.push('RETRY(' + i + '/' + tries + ') ' + (e.cause && e.cause.code || e.message) + ' ' + url);
      await new Promise(res => setTimeout(res, 900 * i));
    }
  }
  log.push('ERR 重试耗尽 ' + url);
  return { status: 0 };
}

async function saveLocal(url, rel) {
  const dest = path.join(OUT, rel);
  if (fs.existsSync(dest)) { log.push('SKIP(已有) ' + rel); return true; }
  const r = await getBuf(url);
  if (!r.buf) { log.push('FAIL ' + r.status + ' ' + url); return false; }
  if (r.buf.length > MAX_FILE) { log.push('SKIP(过大 ' + r.buf.length + ') ' + url); return false; }
  if (total + r.buf.length > MAX_TOTAL) { log.push('SKIP(总量上限) ' + url); return false; }
  total += r.buf.length;
  mkdirp(path.dirname(dest));
  fs.writeFileSync(dest, r.buf);
  log.push('OK   ' + String(r.buf.length).padStart(8) + ' ' + rel);
  return true;
}

(async () => {
  mkdirp(OUT);
  mkdirp(path.join(OUT, 'api'));

  // 引擎：把硬编码的 jsDelivr 基址去掉，让 index.json 里的 /model/... 绝对路径直接生效
  const kbn = fs.readFileSync('/workspace/kbn_upstream.js', 'utf8');
  const patched = kbn.replace(/this\.modelHomeDir='[^']*'/, 'this.modelHomeDir=""');
  fs.writeFileSync(path.join(OUT, 'kbn.local.js'), patched);
  log.push('OK   引擎已生成本地版 kbn.local.js' + (patched === kbn ? '（未匹配到 modelHomeDir，注意！）' : '（modelHomeDir 已置空）'));

  // 模型清单
  await saveLocal(CDN + 'model_list.json', 'model_list.json');

  // 远端 API 的 get/?id=N 响应，改写成本地绝对路径后落盘
  const ids = [];
  for (let n = 1; n <= 12; n++) {
    const r = await getBuf(API + 'get/?id=' + n);
    if (!r.buf) continue;
    let j;
    try { j = JSON.parse(r.buf.toString()); } catch (e) { continue; }
    ids.push(n);
    const fix = s => (typeof s === 'string' && s.startsWith('../') ? s.slice(2) : s);
    const norm = o => {
      if (o && typeof o === 'object') {
        if (typeof o.model === 'string') o.model = '/' + fix(o.model);
        if (Array.isArray(o.textures)) o.textures = o.textures.map(t => '/' + fix(t));
        for (const k of ['motions', 'expressions', 'pose', 'physics', 'moc_consistency', 'hit_areas']) {
          if (o[k] && typeof o[k] === 'object') o[k] = JSON.parse(JSON.stringify(o[k]).replace(/"\.\.\//g, '"/'));
        }
        if (o.file) o.file = '/' + fix(o.file);
      }
      return o;
    };
    norm(j);
    fs.writeFileSync(path.join(OUT, 'api', 'get-' + n + '.json'), JSON.stringify(j));
    log.push('OK   api/get-' + n + '.json  (' + (j.model || '?') + ')');

    // 下载这个模型引用的所有文件
    const want = [j.model, ...(j.textures || [])];
    const walk = o => {
      if (typeof o === 'string') { if (o.startsWith('/')) want.push(o); return; }
      if (Array.isArray(o)) return o.forEach(walk);
      if (o && typeof o === 'object') Object.values(o).forEach(walk);
    };
    walk(j.motions); walk(j.expressions);
    for (const w of want) {
      if (!w) continue;
      await saveLocal(CDN + w.replace(/^\//, ''), w.replace(/^\//, ''));
    }
  }
  log.push('可用模型 id: ' + ids.join(','));
  log.push('本次下载总量: ' + (total / 1048576).toFixed(2) + ' MB');

  // 一言：本地垫片里直接把远端一句替换成本地文案，所以这里只记录
  fs.writeFileSync(path.join(OUT, 'build.log'), log.join('\n') + '\n');
  console.log(log.join('\n'));
})();
