#!/usr/bin/env bash
# Kiểm tra nhanh core / app mới TRƯỚC khi build. Chạy ở gốc project:
#   bash scripts/verify.sh            # chỉ kiểm tra tĩnh (vài giây)
#   bash scripts/verify.sh --build    # thêm ./gradlew assembleDebug
# Thoát mã 1 nếu có mục FAIL. WARN chỉ là nhắc, không làm fail.

cd "$(dirname "$0")/.." || exit 1
fail=0
export PYTHONIOENCODING=utf-8
PY=""
for c in python python3; do if "$c" -c "" >/dev/null 2>&1; then PY=$c; break; fi; done
[ -z "$PY" ] && { echo "Cần Python (python hoặc python3)"; exit 1; }
ok()   { echo "  OK    $*"; }
bad()  { echo "  FAIL  $*"; fail=1; }
warn() { echo "  WARN  $*"; }

echo "== 1. Package / tên cũ còn sót (chỉ cần sạch SAU khi chạy init_app.py) =="
if [ -d app/src/main/java/com/camera/qrscanner/barcode ]; then
  warn "vẫn là package mẫu com.camera.qrscanner.barcode (bỏ qua nếu đang làm chính app QR)"
else
  left=$(grep -rIl "com.camera.qrscanner" app/src app/build.gradle encode-scripts --exclude-dir=node_modules 2>/dev/null)
  [ -z "$left" ] && ok "không còn com.camera.qrscanner" || { bad "còn package cũ trong:"; echo "$left" | sed 's/^/        /'; }
fi

echo "== 2. string resource: dấu ' phải được escape =="
if "$PY" -I scripts/check_strings.py; then ok "không có dấu ' chưa escape"; else fail=1; fi

echo "== 3. default_ads_config.json: JSON hợp lệ, không trùng key =="
"$PY" -X utf8 -I -c "
import json,sys
def h(l):
    ks=[k for k,_ in l]
    d=[k for k in set(ks) if ks.count(k)>1]
    if d: print('  FAIL  key trùng:',d); sys.exit(1)
    return dict(l)
d=json.load(open('app/src/main/assets/default_ads_config.json',encoding='utf-8'),object_pairs_hook=h)
need=['open_splash','inter_splash','banner_splash','open_all','splash_uninstall','native_language','native_language_select','native_keep_user','native_survey_user','native_exit_app','native_permission','native_intro1','native_intro2','native_intro3','native_intro4']
miss=[k for k in need if k not in d]
if miss: print('  FAIL  thiếu key cố định:',miss); sys.exit(1)
print('  OK    đủ',len(need),'key cố định')
" || fail=1

echo "== 4. Manifest: MyLanguageActivity + không có MAIN/LAUNCHER ở MainActivity =="
grep -q "ui.language.MyLanguageActivity" app/src/main/AndroidManifest.xml && ok "khai MyLanguageActivity" || bad "manifest thiếu MyLanguageActivity"
grep -q "android.intent.category.LAUNCHER" app/src/main/AndroidManifest.xml && bad "có LAUNCHER trong manifest (launcher phải là Splash của SDK)" || ok "không khai LAUNCHER"

echo "== 5. encode-scripts: MyLanguageActivity được bảo vệ =="
grep -q '"MyLanguageActivity"' encode-scripts/safe-skip-base.js && ok "có trong PROTECTED_FOLDER_AND_FILE_BASE" || bad "thiếu \"MyLanguageActivity\" trong encode-scripts/safe-skip-base.js"

echo "== 6. Tracking: mọi composable() trong AppNav đều có TrackScreen (hoặc màn tự track) =="
nav=$(find app/src/main/java -name AppNav.kt | head -1)
if [ -n "$nav" ]; then
  c=$(grep -c "composable(" "$nav"); t=$(grep -c "TrackScreen(" "$nav")
  echo "        composable=$c  TrackScreen=$t (Intro / Permission tự track bên trong màn, nên composable có thể = TrackScreen + 2)"
  [ "$t" -ge $((c-2)) ] && ok "số TrackScreen hợp lý" || warn "có thể thiếu TrackScreen ở màn mới"
fi

echo "== 7. Timer tự chuyển màn (delay) — nhắc rà thủ công =="
n=$(grep -rn "delay(" app/src/main/java --include=*.kt | grep -v "ads/\|language/" | wc -l)
[ "$n" -gt 0 ] && warn "$n chỗ dùng delay( ngoài ads/language: nếu dẫn tới navigate thì phải gate theo RESUMED (SCREEN_TRACKING.md §3.8)" || ok "không có delay( ngoài ads/language"

echo "== 8. Giá trị còn là placeholder =="
grep -rn "TODO-SDK" app/src/main/java --include=*.kt | head -3 | sed 's/^/        /'
grep -q "9257395921\|2247696110" app/src/main/assets/default_ads_config.json && warn "default_ads_config.json vẫn dùng ad unit id TEST (bình thường khi dev, phải đổi trước khi phát hành)"

if [ "$1" = "--build" ]; then
  echo "== 9. ./gradlew assembleDebug =="
  if ./gradlew assembleDebug >/tmp/verify_build.log 2>&1 && grep -q "BUILD SUCCESSFUL" /tmp/verify_build.log; then
    ok "BUILD SUCCESSFUL"
  else
    bad "build lỗi, xem /tmp/verify_build.log"; tail -30 /tmp/verify_build.log
  fi
fi

echo
[ $fail -eq 0 ] && echo "KẾT QUẢ: PASS" || echo "KẾT QUẢ: FAIL"
exit $fail
