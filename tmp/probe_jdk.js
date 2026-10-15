const urls = [
  'https://corretto.aws/downloads/latest/amazon-corretto-21-aarch64-linux-jdk.tar.gz',
  'https://aka.ms/download-jdk/microsoft-jdk-21-linux-aarch64.tar.gz',
  'https://cdn.azul.com/zulu/bin/',
  'https://api.adoptium.net/v3/binary/latest/21/ga/linux/aarch64/jdk/hotspot/normal/eclipse?project=jdk',
  'https://deb.debian.org/debian/dists/trixie/Release',
  'https://dl.google.com/android/repository/commandlinetools-linux-16111833_latest.zip'
];
(async () => {
  for (const u of urls) {
    try {
      const r = await fetch(u, { method: 'HEAD', redirect: 'manual' });
      console.log(String(r.status).padEnd(4), (r.headers.get('location') || '').slice(0, 90), '|', u.slice(0, 70));
    } catch (e) {
      console.log('ERR ', ((e.cause && e.cause.code) || e.message), '|', u.slice(0, 70));
    }
  }
})();
