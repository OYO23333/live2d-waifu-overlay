const base = 'https://cdn.jsdelivr.net/gh/fghrsh/live2d_api@1.0.1/';
const cands = [
  base + 'get/',
  base + 'model_list.json',
  base + 'get/model_list.json',
  base + 'model_list.json',
  'https://cdn.jsdelivr.net/gh/fghrsh/live2d_api@1.0.1/model/',
  'https://cdn.jsdelivr.net/gh/fghrsh/live2d_api@1.0.1/model/hibiki/index.json',
  'https://cdn.jsdelivr.net/gh/fghrsh/live2d_api@1.0.1/model/hibiki/textures/00.png',
  'https://api.fghrsh.net/hitokoto/rand/?encode=jsc',
  'https://v1.hitokoto.cn'
];
(async () => {
  for (const u of cands) {
    try {
      const r = await fetch(u, { method: 'GET' });
      const txt = r.ok ? (await r.text()).slice(0, 120).replace(/\s+/g, ' ') : '';
      console.log(String(r.status).padEnd(4), u, '|', txt);
    } catch (e) {
      console.log('ERR ', u, e.message);
    }
  }
})();
