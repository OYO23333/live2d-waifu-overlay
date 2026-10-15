#!/usr/bin/env node
/**
 * 生成 app 的资源目录（assets/），供 live2d-widget 引擎离线使用。
 *
 * 用法：
 *   node make_assets.js --fghrsh Potion-Maker/Pio     # 从 fghrsh/live2d_api 仓库取（Pio 有 87 套衣服）
 *   node make_assets.js --npm live2d-widget-model-haru # 从 npm 模型包取（含动作/表情/语音）
 *   node make_assets.js --npm live2d-widget-model-haru,live2d-widget-model-koharu
 *
 * 产出结构：
 *   assets/widget/      live2d-widget 引擎产物（waifu-tips.js、chunk、live2d.min.js、css、tips.json）
 *   assets/modelrepo/   model_list.json + model/<名字>/index.json + textures.cache + 模型文件
 */
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const OUT = '/workspace/kanban/assets';
const FGHRSH = '/workspace/live2d_api/live2d_api-master';
const L2DW_TGZ = '/workspace/l2dw.tgz';
const WORK = '/workspace/.make-assets-work';

const argv = process.argv.slice(2);
const argVal = k => {
  const i = argv.indexOf(k);
  return i >= 0 ? argv[i + 1] : null;
};
const fghrshArg = argVal('--fghrsh');
const npmArg = argVal('--npm');
if (!fghrshArg && !npmArg) {
  console.error('用法: node make_assets.js --fghrsh <owner/name> | --npm <包名[,包名…]>');
  process.exit(1);
}

const mb = n => (n / 1048576).toFixed(1);
const mkdirp = p => fs.mkdirSync(p, { recursive: true });

function untarGz(buf, dest) {
  const data = zlib.gunzipSync(buf);
  let off = 0;
  while (off + 512 <= data.length) {
    const name = data.toString('utf8', off, off + 100).replace(/\0.*$/, '');
    if (!name) { off += 512; continue; }
    const size = parseInt(data.toString('utf8', off + 124, off + 136).replace(/\0.*$/, '').trim(), 8) || 0;
    const type = data.toString('utf8', off + 156, off + 157);
    const body = data.subarray(off + 512, off + 512 + size);
    if (type === '0' || type === '\0') {
      const p = path.join(dest, name);
      mkdirp(path.dirname(p));
      fs.writeFileSync(p, body);
    }
    off += 512 + Math.ceil(size / 512) * 512;
  }
}

async function getBuf(url, tries = 4) {
  for (let i = 1; i <= tries; i++) {
    try {
      const r = await fetch(url);
      if (!r.ok) return null;
      return Buffer.from(await r.arrayBuffer());
    } catch (e) {
      await new Promise(r => setTimeout(r, 700 * i));
    }
  }
  return null;
}

/** 1) 铺 live2d-widget 引擎产物 */
function installWidget() {
  const W = path.join(OUT, 'widget');
  fs.rmSync(W, { recursive: true, force: true });
  const buf = fs.readFileSync(L2DW_TGZ);
  const tmp = path.join(WORK, 'l2dw');
  fs.rmSync(tmp, { recursive: true, force: true });
  mkdirp(tmp);
  untarGz(buf, tmp);
  const root = fs.readdirSync(tmp).find(d => fs.statSync(path.join(tmp, d)).isDirectory());
  const dist = path.join(tmp, root, 'dist');
  for (const f of ['waifu-tips.js', 'waifu.css', 'live2d.min.js', 'waifu-tips.json',
                   'chunk/index.js', 'chunk/index2.js']) {
    const from = path.join(dist, f);
    const to = path.join(W, f);
    mkdirp(path.dirname(to));
    fs.cpSync(from, to);
  }
  console.log('widget 引擎: ' + mb(fs.readdirSync(W, { recursive: true })
    .map(f => { try { return fs.statSync(path.join(W, f)).size; } catch { return 0; } })
    .reduce((a, b) => a + b, 0)) + ' MB');
}

/** 2) 铺模型仓库 */
async function installModelRepo() {
  const MR = path.join(OUT, 'modelrepo');
  fs.rmSync(MR, { recursive: true, force: true });
  mkdirp(path.join(MR, 'model'));
  const models = [];
  const messages = [];

  if (fghrshArg) {
    const name = fghrshArg;
    const src = path.join(FGHRSH, 'model', name);
    if (!fs.existsSync(src)) { console.error('fghrsh 仓库里没有 ' + name); process.exit(1); }
    const dst = path.join(MR, 'model', name);
    mkdirp(path.dirname(dst));
    fs.cpSync(src, dst, { recursive: true });

    // textures.cache：textures/ 下每个文件一套；有 textures_order.json 则走笛卡尔积
    let cache = [];
    const orderPath = path.join(src, 'textures_order.json');
    if (fs.existsSync(orderPath)) {
      const orders = JSON.parse(fs.readFileSync(orderPath, 'utf8'));
      let result = [[]];
      for (const dirs of orders) {
        const lists = dirs.map(d => {
          const p = path.join(src, d);
          return fs.existsSync(p) ? fs.readdirSync(p).sort().map(f => d + '/' + f) : [];
        });
        const maxLen = Math.max(0, ...lists.map(l => l.length));
        const entries = [];
        for (let i = 0; i < maxLen; i++) {
          const e = lists.map(l => l[i]).filter(Boolean);
          if (e.length) entries.push(e);
        }
        const next = [];
        for (const acc of result) for (const e of entries) next.push(acc.concat(e));
        result = next;
      }
      cache = result;
    } else {
      const texDir = path.join(src, 'textures');
      if (fs.existsSync(texDir)) {
        cache = fs.readdirSync(texDir).sort().filter(f => f.toLowerCase().endsWith('.png'))
          .map(f => 'textures/' + f);
      }
    }
    if (cache.length > 0) {
      fs.writeFileSync(path.join(dst, 'textures.cache'), JSON.stringify(cache));
    }
    models.push(name);
    messages.push('来自 fghrsh 的 ' + name);
    console.log('模型 ' + name + '：' + cache.length + ' 套衣服');
  }

  if (npmArg) {
    const pkgs = npmArg.split(',').map(s => s.trim()).filter(Boolean);
    for (const pkg of pkgs) {
      const meta = await getBuf('https://registry.npmjs.org/' + pkg);
      if (!meta) { console.log('跳过（拿不到元数据）：' + pkg); continue; }
      const j = JSON.parse(meta.toString());
      const ver = j['dist-tags'].latest;
      const buf = await getBuf(j.versions[ver].dist.tarball);
      if (!buf) { console.log('跳过（下载失败）：' + pkg); continue; }
      const dir = path.join(WORK, pkg);
      fs.rmSync(dir, { recursive: true, force: true });
      mkdirp(dir);
      untarGz(buf, dir);

      // npm 包结构：01/assets/x.model.json …；每个变体当作一个模型条目
      const base = path.join(dir, 'package');
      const variants = fs.existsSync(base)
        ? fs.readdirSync(base).filter(d => fs.existsSync(path.join(base, d, 'assets')))
        : [];
      let added = 0;
      for (const v of variants) {
        const vdir = path.join(base, v, 'assets');
        const json = fs.readdirSync(vdir).filter(f => f.endsWith('.model.json'))[0];
        if (!json) continue;
        const name = pkg + '/' + v;
        const dst = path.join(MR, 'model', name);
        mkdirp(path.dirname(dst));
        fs.cpSync(vdir, dst, { recursive: true });
        // npm 的 model.json 就是 Cubism2 标准设置，路径本来就是相对模型目录的 → 直接当 index.json
        fs.cpSync(path.join(vdir, json), path.join(dst, 'index.json'));
        models.push(name);
        messages.push(pkg.split('-').pop() + ' ' + v);
        added++;
      }
      console.log('模型 ' + pkg + ' v' + ver + '：' + added + ' 个变体');
    }
  }

  fs.writeFileSync(path.join(MR, 'model_list.json'),
    JSON.stringify({ models, messages }, null, 1));
  console.log('\n仓库共 ' + models.length + ' 个模型条目');
  const size = require('child_process').execSync('du -sb ' + MR + ' | cut -f1').toString().trim();
  console.log('modelrepo 大小: ' + mb(parseInt(size)) + ' MB');
  console.log('models: ' + JSON.stringify(models));
}

(async () => {
  mkdirp(OUT);
  mkdirp(WORK);
  installWidget();
  await installModelRepo();
  const size = require('child_process').execSync('du -sb ' + OUT + ' | cut -f1').toString().trim();
  console.log('assets 总计: ' + mb(parseInt(size)) + ' MB');
})();
