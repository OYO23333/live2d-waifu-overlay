const fs = require('fs');
const t = fs.readFileSync('/workspace/kbn_upstream.js', 'utf8');

const urls = [...new Set(t.match(/https?:\/\/[^"'\s)]+/g) || [])];
console.log('URL 数量:', urls.length);
urls.slice(0, 40).forEach(u => console.log('   ', u));

console.log('--- 关键字计数 ---');
const keys = ['models', 'model', 'textures', 'texture', 'live2d/', '.json', '.png', '.moc', 'data:image', 'base64', 'hitokoto', 'fghrsh', 'eruda'];
for (const k of keys) {
  const n = (t.split(k).length - 1);
  console.log('   ' + k.padEnd(12) + n);
}

console.log('--- 模型路径样式（片段） ---');
const m = t.match(/[^"']{0,60}live2d\/[^"']{0,80}/gi) || [];
[...new Set(m)].slice(0, 15).forEach(s => console.log('   ', s));

const targets = [
  'https://dl.google.com/android/repository/repository2-3.xml',
  'https://api.adoptium.net/v3/info/available_releases',
  'https://repo1.maven.org/maven2/',
  'https://services.gradle.org/distributions/'
];
Promise.all(targets.map(x => fetch(x, { method: 'HEAD' })
  .then(r => console.log(r.status, x))
  .catch(e => console.log('ERR', e.message, x))));
