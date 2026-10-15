const COMMIT = '310ce87f9d78313966e5d17753ad36b9725b280b';
const DIR = '.agents/skills/backend-code-review';
const BASES = [
  'https://raw.githubusercontent.com/langgenius/dify/' + COMMIT + '/' + DIR + '/',
  'https://cdn.statically.io/gh/langgenius/dify/' + COMMIT + '/' + DIR + '/',
  'https://raw.githack.com/langgenius/dify/' + COMMIT + '/' + DIR + '/',
];
const fs = require('fs');
const sleep = ms => new Promise(r => setTimeout(r, ms));

async function fetchText(name) {
  for (let attempt = 1; attempt <= 3; attempt++) {
    for (const base of BASES) {
      try {
        const r = await fetch(base + name, { signal: AbortSignal.timeout(20000) });
        if (r.ok) return await r.text();
        if (r.status !== 404) console.log('  http', r.status, base.split('/')[2]);
      } catch (e) {
        console.log('  失败(' + attempt + ')', base.split('/')[2], (e.cause && e.cause.code) || e.message);
      }
    }
    await sleep(1500);
  }
  return null;
}

(async () => {
  const files = ['SKILL.md'];
  for (const f of files) {
    const t = await fetchText(f);
    if (!t) { console.log('拿不到', f, '（镜像都不通）'); continue; }
    fs.writeFileSync('/workspace/dify-skill-' + f.replace('/', '_'), t);
    console.log('=== ' + f + '（' + t.length + ' 字符）===');
    console.log(t.slice(0, 3500));
  }
})();
