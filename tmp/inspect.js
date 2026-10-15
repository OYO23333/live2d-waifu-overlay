const fs = require('fs');
const t = fs.readFileSync('/workspace/kbn_upstream.js', 'utf8');

function ctx(tok, before = 120, after = 320, max = 2) {
  let i = -1, n = 0;
  console.log('================ ' + tok);
  while ((i = t.indexOf(tok, i + 1)) !== -1 && n < max) {
    n++;
    console.log('[' + n + '] ...' + t.slice(Math.max(0, i - before), i + after).replace(/\n/g, ' ') + '...');
  }
  if (n === 0) console.log('(未出现)');
}

['kbn_setting0', 'kbn_setting1', 'model_list', 'index.json', 'loadModelSetting',
 'getModelFile', 'modelHomeDir', 'textures/', 'motions/', 'hitokoto'].forEach(k => ctx(k, 100, 260, 2));
