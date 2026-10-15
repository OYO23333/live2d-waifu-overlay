import sys, zipfile, os, shutil

base, dex, out = sys.argv[1], sys.argv[2], sys.argv[3]
if os.path.exists(out):
    os.remove(out)

src = zipfile.ZipFile(base, 'r')
dst = zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED)

added = False
for item in src.infolist():
    if item.filename == 'classes.dex':
        continue
    if item.filename.startswith('META-INF/'):
        continue  # 旧签名一律丢掉
    dst.writestr(item, src.read(item.filename))

dst.write(dex, 'classes.dex')
added = True

src.close()
dst.close()
print('合成完成:', out, os.path.getsize(out), 'bytes, classes.dex:', added)
