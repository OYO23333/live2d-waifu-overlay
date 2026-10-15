const fs = require('fs');

(async () => {
  const url = 'https://dl.google.com/android/repository/repository2-3.xml';
  const r = await fetch(url);
  console.log('repository2-3.xml ->', r.status);
  const xml = await r.text();
  fs.writeFileSync('/workspace/repo.xml', xml);
  console.log('大小', (xml.length / 1048576).toFixed(2), 'MB');

  function list(prefix) {
    const re = new RegExp('<remotePackage path="(' + prefix + '[^"]*)">([\\s\\S]*?)</remotePackage>', 'g');
    const out = [];
    let m;
    while ((m = re.exec(xml)) !== null) {
      const pkg = m[1];
      const body = m[2];
      const rev = (body.match(/<major>(\d+)<\/major>\s*<minor>(\d+)<\/minor>\s*<micro>(\d+)<\/micro>/) || []).slice(1).join('.');
      const u = (body.match(/<url>([^<]+)<\/url>/) || [])[1];
      const size = (body.match(/<size>(\d+)<\/size>/) || [])[1];
      out.push({ pkg, rev, url: u, mb: size ? (size / 1048576).toFixed(1) : '?' });
    }
    return out;
  }

  for (const prefix of ['build-tools;', 'platforms;android-3', 'platform-tools', 'cmdline-tools;']) {
    const l = list(prefix);
    console.log('\n===== ' + prefix + ' （共 ' + l.length + ' 条，显示最后 6 条） =====');
    l.slice(-6).forEach(x => console.log('  ' + x.pkg.padEnd(26) + ' rev=' + x.rev.padEnd(8) + x.mb + 'MB  ' + x.url));
  }
})().catch(e => console.log('ERR', e.message));
