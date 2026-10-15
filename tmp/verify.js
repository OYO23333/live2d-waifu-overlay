const fs = require('fs');
const path = require('path');
const tar = require('child_process');

// 用 python3 做校验和解包（环境里没有 unzip/tar 命令行工具）
const py = `
import tarfile, zipfile, os, sys

def check_tar(p):
    tf = tarfile.open(p, 'r:gz')
    n = tf.getnames()
    print('TAR', p, '条目数', len(n), '顶层:', sorted(set(x.split('/')[0] for x in n))[:5])
    return True

def check_zip(p):
    z = zipfile.ZipFile(p)
    bad = z.testzip()
    n = z.namelist()
    print('ZIP', p, '条目数', len(n), '损坏:', bad, '顶层:', sorted(set(x.split('/')[0] for x in n))[:5])
    return bad is None

ok = True
ok &= check_tar('/workspace/jdk21.tar.gz')
ok &= check_zip('/workspace/build-tools.zip')
ok &= check_zip('/workspace/platform-37.0.zip')
print('校验结果:', 'OK' if ok else 'FAILED')
sys.exit(0 if ok else 1)
`;
fs.writeFileSync('/workspace/verify.py', py);

const { execSync } = require('child_process');
try {
  console.log(execSync('python3 /workspace/verify.py', { encoding: 'utf8' }));
} catch (e) {
  console.log('校验失败：', e.stdout || e.message);
}
