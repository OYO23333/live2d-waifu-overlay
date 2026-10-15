const fs = require('fs');
const { Readable } = require('stream');
const { pipeline } = require('stream/promises');

const url = process.argv[2];
const dest = process.argv[3];
const sleep = ms => new Promise(r => setTimeout(r, ms));
const mb = n => (n / 1048576).toFixed(1);

(async () => {
  for (let i = 1; i <= 6; i++) {
    try {
      const t0 = Date.now();
      const r = await fetch(url);
      if (!r.ok) { console.log('HTTP ' + r.status); await sleep(2000); continue; }
      fs.rmSync(dest, { force: true });
      await pipeline(Readable.fromWeb(r.body), fs.createWriteStream(dest));
      const n = fs.statSync(dest).size;
      console.log('DONE ' + dest + ' ' + mb(n) + ' MB 用时 ' + ((Date.now() - t0) / 1000).toFixed(0) + 's');
      return;
    } catch (e) {
      console.log('重试(' + i + '/6) ' + ((e.cause && e.cause.code) || e.message));
      await sleep(2500);
    }
  }
  console.log('FAILED ' + url);
  process.exit(1);
})();
