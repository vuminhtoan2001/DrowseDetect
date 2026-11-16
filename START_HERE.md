# START HERE — dựng app mới từ core này

Core đã bọc sẵn SDK `base-application` (ads / IAP / splash / notification) và code xong các màn khởi động:
**Splash → Language → (Intro) → (Permission) → Home**, kèm tracking Firebase. Người làm app mới chỉ viết
**UI và logic nghiệp vụ từ Home trở đi**. Đừng tự dựng lại các màn khởi động.

## 1. Thứ tự đọc

| # | Đọc | Để làm gì |
|---|---|---|
| 1 | `docs/01..04_*.md` (đặc tả app) + `docs/screenshot/` | Điền cho app mới: màn hình, luồng, style, ảnh mapping. Mẫu: `docs-sample/`, quy trình: `forDev/APP_SPEC_PROCESS.md` |
| 2 | [`docs/QUY_CHUAN_DESIGN.md`](docs/QUY_CHUAN_DESIGN.md) | Map style vào `ui/theme/` TRƯỚC khi code màn. Không hardcode màu / cỡ chữ |
| 3 | [`docs/update-sdk-language/README.md`](docs/update-sdk-language/README.md) + `SCREEN_TRACKING.md` §2, §3.5, §3.8 | Cách đặt tên màn, gắn `TrackScreen`, timer tự chuyển màn |
| 4 | [`docs/integration/ADS.md`](docs/integration/ADS.md) | Gọi ads cho màn mới, key `_enable` / `_showRatio` / `_maxShowPerDay` |
| 5 | [`docs/UI_FIDELITY_WORKFLOW.md`](docs/UI_FIDELITY_WORKFLOW.md) | Render Compose ra PNG, so với screenshot |

Chỉ tra khi gặp việc liên quan: `docs/integration/COMMON_ACTIONS.md` (update, rate, share, mở IAP),
`ENCODE_R8_STANDARD.md` + `PLAYBOOK_TICH_HOP.md` (trước khi `./push.sh build_tool`), `PROGUARD_SDK_RECIPES.md`
(thêm SDK mới), `REMOTE_CONFIG_TEMPLATE.md` (dựng Remote Config), `update-sdk-language/UPDATE_SDK.md` (nâng SDK).

## 2. Đã có sẵn — KHÔNG làm lại

- Vỏ khởi động của SDK; `MyLanguageActivity` (Language custom, 2 native ad, auto-select / confirm).
- Intro 4 slide + Permission; cổng Intro / Home theo số lần mở và nguồn cài (`AppViewModel`).
- Tracking `screen_view` + event (`platform/`), dialog Full-screen intent ở Home, check update, rate, share.
- compileSdk / targetSdk 36. Lần đầu mở app **bỏ màn IAP** (đã chốt).

## 3. Việc phải làm khi clone (làm theo thứ tự)

1. **Đổi tên app:** chạy script (mặc định chỉ in kế hoạch, thêm `--apply` để sửa thật; nên commit sạch trước):
   ```
   python -I scripts/init_app.py --package com.acme.notes --name NotesApp --prefix NT
   python -I scripts/init_app.py --package com.acme.notes --name NotesApp --prefix NT --apply
   ```
   Script đổi package + thư mục, `rootProject.name`, theme, prefix tracking `AD_` → `NT_`, notification channel.
   Còn phải làm tay: `app_name`, icon, thay `app/google-services.json` bằng file thật cho package mới.
2. **Prefix tracking** đã được script đổi trong `object Dest` (`ui/nav/AppNav.kt`); viết lại nội dung
   `docs/ANALYTICS_EVENTS.md` theo màn thật của app.
3. **Điền giá trị thật** (TODO-SDK trong `HostApplication`): `admob_app_id`, `facebook_*`, `app_flyer_id`,
   `public_license_key`, IAP product id, URL policy / terms.
4. **Ad unit thật** trong `app/src/main/assets/default_ads_config.json` (hiện là id test). `native_language_select`
   cần unit riêng; `banner_splash` chỉ cần khai ở file này.
5. **Viết lại text:** `sdk_strings.xml` (tính năng IAP, thông báo), `full_screen_intent_permission_message`
   (21 ngôn ngữ), string + ảnh Intro `intro1..4`.
6. **Home:** thay phần placeholder trong `ui/home/HomeScreen.kt`, giữ `CheckForAppUpdateWithDialog()` và
   `CheckFullScreenIntentPermission()`. Mỗi màn mới: route vào `AppNav.kt` + `TrackScreen(Dest.X)`.
7. **Remote Config:** dựng template theo `docs/integration/REMOTE_CONFIG_TEMPLATE.md` (file `remote_config_new.json`
   hiện là của app mẫu).
8. Trước khi push: rà `EXCLUDED_PATHS` trong `push.sh`; xoá `docs-sample/`, `_ref/` nếu không cần.

## 4. Nghiệm thu trước khi báo xong

- `bash scripts/verify.sh` (kiểm tra tĩnh vài giây: package sót, escape string, ads config, manifest, encode,
  tracking); `bash scripts/verify.sh --build` chạy thêm `assembleDebug`.
- `./gradlew assembleDebug` và `assembleRelease` đều xanh (release có R8, đã thử trên code CHƯA encode).
- Test máy thật, cài mới: luồng lần đầu, đổi ngôn ngữ từ Settings, dialog Full-screen intent (Android 14+),
  Back ở mọi màn (Android 16). Chi tiết: cuối `docs/update-sdk-language/README.md`.
- `adb logcat -s ScreenTracker AppEvents`: mỗi màn đúng 1 `screen_view`, tên đúng `<PREFIX>_...`.
- Chạy lại luồng lần đầu trên bản đã encode (`./push.sh build_tool`).
