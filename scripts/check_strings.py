#!/usr/bin/env python3
"""Báo dấu ' chưa escape trong <string> của res/values*/ (lỗi gây fail mergeDebugResources)."""
import glob
import re
import sys

sys.stdout.reconfigure(encoding="utf-8")

bad = []
files = glob.glob("app/src/main/res/values*/*.xml")
for p in files:
    for n, line in enumerate(open(p, encoding="utf-8"), 1):
        m = re.search(r"<string[^>]*>(.*?)</string>", line)
        if not m:
            continue
        body = m.group(1).strip()
        if body.startswith('"') and body.endswith('"'):
            continue  # chuỗi bọc trong "..." thì ' hợp lệ
        if re.search(r"(?<!\\)'", body):
            bad.append(f"{p}:{n}")
for b in bad:
    print("  FAIL  dấu ' chưa escape:", b)
sys.exit(1 if bad else 0)
