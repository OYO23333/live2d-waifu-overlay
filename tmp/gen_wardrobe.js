const fs = require('fs');
const path = require('path');

const REPO = '/workspace/live2d_api/live2d_api-master';
const OUT = '/workspace/kanban/assets';
const API = path.join(OUT, 'api');
const mb = n => (n / 1048576).toFixed(1);

const modelList = JSON.parse(fs.readFileSync(path.join(REPO, 'model_list.json'), 'utf8'));

/** 复刻 modelTextures::get_list()：textures/ 直接列举，textures_order.json 走笛卡尔积 */
function costumeList(modelName) {
  const dir = path.join(REPO, 'model', modelName);
  const orderPath = path.join(dir, 'textures_order.json');
  if (fs.existsSync(orderPath)) {
    const orders = JSON.parse(fs.readFileSync(orderPath, 'utf8'));
    let result = [[]];
    for (const dirs of orders) {
      const lists = dirs.map(d => {
        const p = path.join(dir, d);
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
    return result;
  }
  const texDir = path.join(dir, 'textures');
  if (fs.existsSync(texDir)) {
    // 注意：上游 glob 出来的是 textures/xxx.png，前缀不能丢，否则贴图 404
    return fs.readdirSync(texDir).sort().map(f => ['textures/' + f]);
  }
  return [[/* 无贴图（用 index.json 自带的） */]];
}

/** 复刻 get/index.php：把 index.json 里的相对路径全部改成本地绝对路径 */
function buildGetJson(modelName, textures) {
  const dir = path.join(REPO, 'model', modelName);
  const j = JSON.parse(fs.readFileSync(path.join(dir, 'index.json'), 'utf8'));
  const abs = rel => '/model/' + modelName + '/' + rel;

  if (textures && textures.length && textures[0] !== undefined) {
    j.textures = textures.map(t => abs(t));
  } else {
    j.textures = (j.textures || []).map(t => abs(t));
  }
  j.model = abs(j.model);
  if (j.pose) j.pose = abs(j.pose);
  if (j.physics) j.physics = abs(j.physics);
  if (j.motions) {
    for (const k of Object.keys(j.motions))
      for (const m of j.motions[k])
        for (const f of ['file', 'sound']) if (m[f]) m[f] = abs(m[f]);
  }
  if (j.expressions) {
    for (const k of Object.keys(j.expressions)) {
      const e = j.expressions[k];
      if (e && e.file) e.file = abs(e.file);
    }
  }
  return j;
}

const summary = {};
let apiCount = 0;
for (let n = 1; n <= modelList.models.length; n++) {
  const entry = modelList.models[n - 1];
  const isGroup = Array.isArray(entry);
  let count = 0;

  if (isGroup) {
    // 每一套衣服 = 一个独立模型目录
    count = entry.length;
    entry.forEach((modelName, i) => {
      const idx = i + 1;
      const j = buildGetJson(modelName, null);
      fs.writeFileSync(path.join(API, `get-${n}-${idx}.json`), JSON.stringify(j));
      if (idx === 1) fs.writeFileSync(path.join(API, `get-${n}.json`), JSON.stringify(j));
      apiCount++;
    });
  } else {
    const list = costumeList(entry);
    count = list.length;
    list.forEach((textures, i) => {
      const idx = i + 1;
      const j = buildGetJson(entry, textures.length ? textures : null);
      fs.writeFileSync(path.join(API, `get-${n}-${idx}.json`), JSON.stringify(j));
      if (idx === 1) fs.writeFileSync(path.join(API, `get-${n}.json`), JSON.stringify(j));
      apiCount++;
    });
  }

  // 换装：确定性轮转（点一次换下一套，最后一套回到第一套）
  // M=0 是"页面还没记录当前衣服"的情形，返回第 1 套，之后再点就 1→2→3…
  for (let idx = 0; idx <= count; idx++) {
    const next = idx === 0 ? 1 : (idx % count) + 1;
    const body = JSON.stringify({ textures: { id: next } });
    fs.writeFileSync(path.join(API, `rand_textures-${n}-${idx}.json`), body);
    fs.writeFileSync(path.join(API, `switch_textures-${n}-${idx}.json`), body);
    apiCount += 2;
  }
  summary[n] = count;
  console.log(`id=${n}  ${isGroup ? '[组]' : ''}${count} 套衣服`);
}

console.log('\n=== 模型衣橱 ===');
console.log(JSON.stringify(summary));
console.log('生成 api 文件数: ' + apiCount);
