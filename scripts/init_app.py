#!/usr/bin/env python3
"""Đổi tên core (app QR mẫu) thành app mới: package, tên theme / project, prefix tracking, channel.

Chạy ở GỐC project (mặc định chỉ in kế hoạch, KHÔNG sửa gì; thêm --apply để sửa thật):

    python -I scripts/init_app.py --package com.acme.notes --name NotesApp --prefix NT
    python -I scripts/init_app.py --package com.acme.notes --name NotesApp --prefix NT --apply

--package  package mới, cũng là applicationId (chữ thường, vd com.acme.notes)
--name     tên PascalCase, dùng cho rootProject.name, Theme.<Name>, <Name>Theme
--prefix   prefix tracking 2-3 chữ in hoa (QR_Home -> NT_Home)
--channel  (tuỳ chọn) prefix notification channel, snake_case; mặc định suy ra từ --name

Cần commit sạch trước khi --apply để dễ `git diff` / hoàn tác. Script KHÔNG sửa app/google-services.json
(phải thay bằng file thật tải từ Firebase cho đúng package) và KHÔNG đổi app_name / icon / text.
"""
import argparse
import os
import re
import shutil
import sys

sys.stdout.reconfigure(encoding="utf-8")

OLD_PACKAGE = "com.camera.qrscanner.barcode"
OLD_NAME = "QrScanner"
OLD_THEME = "QrScannerTheme"
OLD_PREFIX = "QR"
OLD_CHANNEL = "qr_scanner"

TEXT_EXT = {".kt", ".kts", ".java", ".gradle", ".xml", ".md", ".js", ".json", ".pro", ".sh", ".py", ".txt", ".properties"}
SKIP_DIRS = {"build", ".git", ".gradle", "node_modules", "docs-sample", "_ref", ".idea", "scratchpad"}
SKIP_FILES = {"google-services.json", "init_app.py", "remote_config_new.json", "remote_config_setup.json"}
# Chỉ sửa trong các nhánh này (tránh đụng thư mục khác)
SCAN_ROOTS = ["app", "docs", "encode-scripts", "forDev", "settings.gradle.kts", "START_HERE.md", "push.sh"]


def snake(name):
    return re.sub(r"(?<!^)(?=[A-Z])", "_", name).lower()


def iter_files(root):
    for entry in SCAN_ROOTS:
        path = os.path.join(root, entry)
        if os.path.isfile(path):
            yield path
        elif os.path.isdir(path):
            for d, dirs, files in os.walk(path):
                dirs[:] = [x for x in dirs if x not in SKIP_DIRS]
                for f in files:
                    if f in SKIP_FILES or os.path.splitext(f)[1].lower() not in TEXT_EXT:
                        continue
                    yield os.path.join(d, f)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--package", required=True)
    ap.add_argument("--name", required=True)
    ap.add_argument("--prefix", required=True)
    ap.add_argument("--channel")
    ap.add_argument("--apply", action="store_true")
    ap.add_argument("--root", default=".")
    a = ap.parse_args()

    if not re.fullmatch(r"[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*){2,}", a.package):
        sys.exit("--package phải dạng com.company.app (chữ thường, ít nhất 3 đoạn)")
    if not re.fullmatch(r"[A-Z][A-Za-z0-9]*", a.name):
        sys.exit("--name phải là PascalCase, vd NotesApp")
    if not re.fullmatch(r"[A-Z]{2,3}", a.prefix):
        sys.exit("--prefix phải là 2-3 chữ in hoa, vd NT")
    channel = a.channel or snake(a.name)
    root = os.path.abspath(a.root)
    if not os.path.isfile(os.path.join(root, "settings.gradle.kts")):
        sys.exit("Chạy ở gốc project (không thấy settings.gradle.kts)")

    subs = [
        (re.compile(re.escape(OLD_PACKAGE)), a.package),
        (re.compile(re.escape(OLD_PACKAGE.replace(".", "/"))), a.package.replace(".", "/")),
        (re.compile(r"\b" + OLD_THEME + r"\b"), a.name + "Theme"),
        (re.compile(r"Theme\." + OLD_NAME + r"\b"), "Theme." + a.name),
        (re.compile(r'rootProject\.name\s*=\s*"' + OLD_NAME + '"'), 'rootProject.name = "' + a.name + '"'),
        (re.compile(r'"' + OLD_CHANNEL + '"'), '"' + channel + '"'),
        (re.compile(r"\b" + OLD_PREFIX + r"_(?=[A-Z<`])"), a.prefix + "_"),
    ]

    changed = []
    for path in iter_files(root):
        try:
            text = open(path, encoding="utf-8", newline="").read()
        except UnicodeDecodeError:
            continue
        new = text
        for rx, rep in subs:
            new = rx.sub(rep, new)
        if new != text:
            changed.append(path)
            if a.apply:
                open(path, "w", encoding="utf-8", newline="").write(new)

    old_dir = os.path.join(root, "app", "src", "main", "java", *OLD_PACKAGE.split("."))
    new_dir = os.path.join(root, "app", "src", "main", "java", *a.package.split("."))
    print(f"Package : {OLD_PACKAGE} -> {a.package}")
    print(f"Project : {OLD_NAME} -> {a.name} (Theme.{a.name}, {a.name}Theme)")
    print(f"Prefix  : {OLD_PREFIX}_ -> {a.prefix}_   Channel: {OLD_CHANNEL} -> {channel}")
    print(f"Sửa nội dung {len(changed)} file" + ("" if a.apply else " (dry-run, chưa ghi)"))
    for p in changed[:60]:
        print("  ", os.path.relpath(p, root))
    if len(changed) > 60:
        print(f"   ... và {len(changed) - 60} file nữa")

    if os.path.isdir(old_dir):
        print(f"Chuyển thư mục: {os.path.relpath(old_dir, root)} -> {os.path.relpath(new_dir, root)}")
        if a.apply:
            if os.path.exists(new_dir):
                sys.exit("Thư mục package mới đã tồn tại, dừng.")
            os.makedirs(os.path.dirname(new_dir), exist_ok=True)
            shutil.move(old_dir, new_dir)
            # dọn các thư mục cha rỗng của package cũ
            p = os.path.dirname(old_dir)
            java_root = os.path.join(root, "app", "src", "main", "java")
            while p != java_root and os.path.isdir(p) and not os.listdir(p):
                os.rmdir(p)
                p = os.path.dirname(p)
    elif a.apply:
        print("Không thấy thư mục package cũ (đã đổi rồi?)")

    print("\nCòn phải làm tay:")
    print(" - Thay app/google-services.json bằng file thật cho package mới (Firebase Console).")
    print(" - app_name, icon, sdk_strings.xml, text Intro, key trong HostApplication (TODO-SDK).")
    print(" - Chạy: bash scripts/verify.sh")
    if not a.apply:
        print("\nThêm --apply để sửa thật.")


if __name__ == "__main__":
    main()
