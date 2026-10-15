const COMMIT = '310ce87f9d78313966e5d17753ad36b9725b280b';
const DIR = '.agents/skills/backend-code-review';
const BASES = [
  'https://raw.githubusercontent.com/langgenius/dify/' + COMMIT + '/' + DIR + '/',
  'https://cdn.statically.io/gh/langgenius/dify/' + COMMIT + '/' + DIR + '/',
  'https://raw.githack.com/langgenius/dify/' + COMMIT + '/' + DIR + '/',
];
const fs = require('fs');
const path = require('path');
const OUT = '/sdcard/Live2D_看板娘app/skills/backend-code-review';
const sleep = ms => new Promise(r => setTimeout(r, ms));

async function fetchText(name) {
  for (let attempt = 1; attempt <= 3; attempt++) {
    for (const base of BASES) {
      try {
        const r = await fetch(base + name, { signal: AbortSignal.timeout(25000) });
        if (r.ok) { console.log('  OK', name, 'via', base.split('/')[2]); return await r.text(); }
      } catch (e) {
        console.log('  重试', name, (e.cause && e.cause.code) || e.message);
      }
    }
    await sleep(1500);
  }
  return null;
}

(async () => {
  fs.rmSync(OUT, { recursive: true, force: true });
  const files = ['SKILL.md', 'references/architecture-rule.md', 'references/db-schema-rule.md',
                 'references/repositories-rule.md', 'references/sqlalchemy-rule.md'];
  let ok = 0;
  for (const f of files) {
    const t = await fetchText(f);
    if (!t) { console.log('  拿不到', f); continue; }
    const dest = path.join(OUT, f);
    fs.mkdirSync(path.dirname(dest), { recursive: true });
    fs.writeFileSync(dest, t);
    console.log('  ->', dest, t.length, '字符');
    ok++;
  }
  console.log('\n完成 ' + ok + '/' + files.length + ' 个文件');
  const size = require('child_process').execSync('du -sb ' + OUT + ' | cut -f1').toString().trim();
  console.log('技能目录大小: ' + (size / 1024).toFixed(1) + ' KB');
})();
