const fs = require('fs');
const mode = process.argv[2];

if (mode === 'newest') {
  const xml = fs.readFileSync('/workspace/repo.xml', 'utf8');
  function list(prefix) {
    const re = new RegExp('<remotePackage path="(' + prefix + '[^"]*)">([\\s\\S]*?)</remotePackage>', 'g');
    const out = []; let m;
    while ((m = re.exec(xml)) !== null) {
      const size = (m[2].match(/<size>(\d+)<\/size>/) || [])[1];
      const u = (m[2].match(/<url>([^<]+)<\/url>/) || [])[1];
      out.push({ pkg: m[1], mb: size ? (size / 1048576).toFixed(1) : '?', url: u });
    }
    return out;
  }
  for (const p of ['build-tools;', 'platforms;android-3', 'cmdline-tools;']) {
    console.log('===== ' + p);
    list(p).slice(0, 6).forEach(x => console.log('   ' + x.pkg.padEnd(26) + x.mb + 'MB  ' + x.url));
  }
}

if (mode === 'download') {
  const url = process.argv[3], dest = process.argv[4];
  const { Readable } = require('stream');
  const { pipeline } = require('stream/promises');
  (async () => {
    for (let i = 1; i <= 4; i++) {
      try {
        const t0 = Date.now();
        const r = await fetch(url);
        if (!r.ok) { console.log('HTTP ' + r.status + ' ' + url); continue; }
        await pipeline(Readable.fromWeb(r.body), fs.createWriteStream(dest));
        const n = fs.statSync(dest).size;
        console.log('saved ' + dest + '  ' + (n / 1048576).toFixed(1) + ' MB  ' + ((Date.now() - t0) / 1000).toFixed(1) + 's');
        return;
      } catch (e) {
        console.log('RETRY(' + i + '/4) ' + ((e.cause && e.cause.code) || e.message));
        await new Promise(r => setTimeout(r, 1500 * i));
      }
    }
    console.log('FAILED ' + url);
    process.exit(1);
  })();
}
