const fs = require('fs');
const { Readable } = require('stream');
const { pipeline } = require('stream/promises');

const url = process.argv[2];
const dest = process.argv[3];
const sleep = ms => new Promise(r => setTimeout(r, ms));

function mb(n) { return (n / 1048576).toFixed(1); }

(async () => {
  for (let i = 1; i <= 40; i++) {
    const have = fs.existsSync(dest) ? fs.statSync(dest).size : 0;
    const headers = have > 0 ? { Range: 'bytes=' + have + '-' } : {};
    try {
      const r = await fetch(url, { headers });
      if (r.status === 416) { console.log('DONE 已完整 ' + dest + ' ' + mb(have) + ' MB'); return; }
      if (r.status !== 200 && r.status !== 206) { console.log('HTTP ' + r.status + ' 重试'); await sleep(2000); continue; }

      const cr = r.headers.get('content-range');           // bytes 100-999/1000
      const total = cr ? Number(cr.split('/')[1]) : Number(r.headers.get('content-length') || 0);

      await pipeline(Readable.fromWeb(r.body), fs.createWriteStream(dest, { flags: have > 0 ? 'a' : 'w' }));
      const now = fs.statSync(dest).size;
      console.log('进度 ' + mb(now) + ' / ' + (total ? mb(total) : '?') + ' MB');
      if (total && now >= total) { console.log('DONE ' + dest + ' ' + mb(now) + ' MB'); return; }
      await sleep(500);
    } catch (e) {
      console.log('断开重连(' + i + ') ' + ((e.cause && e.cause.code) || e.message));
      await sleep(2000);
    }
  }
  console.log('FAILED 重试耗尽 ' + url);
  process.exit(1);
})();
