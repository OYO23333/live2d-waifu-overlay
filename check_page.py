#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Page.java 页面代码静态检查。

Page.java 用一堆 .append("...") 拼出整个页面，拼出来的 JS/CSS **全在一行**。
只要某个片段里出现 // 行注释，它会把后面所有 append 的内容一起注释掉，
页面直接白屏（而且日志里只会看到"页面没初始化"，很难查）。

这个脚本在 build.sh 里跑，发现就中断构建。
"""
import re
import sys

SRC = 'src/com/eta/kanban/Page.java'

try:
    src = open(SRC, encoding='utf-8').read()
except OSError as e:
    print('检查失败：读不到 %s (%s)' % (SRC, e))
    sys.exit(1)

bad = []
for m in re.finditer(r'\.append\("((?:[^"\\]|\\.)*)"\)', src):
    body = m.group(1)
    for hit in re.finditer(r'//', body):
        i = hit.start()
        # 放过 http:// 这种协议头
        if i > 0 and body[i - 1] == ':':
            continue
        line = src[:m.start()].count('\n') + 1
        bad.append((line, body[max(0, i - 45):i + 45]))

if bad:
    print('✗ append 字符串里出现了 // 行注释，会把后面的 JS 全部注释掉：')
    for line, text in bad:
        print('  第 %d 行: ...%s...' % (line, text))
    sys.exit(1)

print('== 0/6 页面拼装检查通过（append 里没有 // 注释） ==')
