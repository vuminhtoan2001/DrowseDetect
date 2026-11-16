# docs/ — Tài liệu cho AI đọc để dựng app

> Mới clone core? Đọc [`../START_HERE.md`](../START_HERE.md) trước.

Có 2 giai đoạn, đọc theo thứ tự:

## A. Thiết kế (khuôn trắng — điền cho app mới)

| File | Nội dung |
|---|---|
| [`screenshot/`](screenshot/) | Ảnh chụp màn hình |
| [`01_MO_TA_CHUC_NANG_MAN_HINH.md`](01_MO_TA_CHUC_NANG_MAN_HINH.md) | Mô tả chức năng từng màn hình |
| [`02_LUONG_DI_CHUYEN_MAN_HINH.md`](02_LUONG_DI_CHUYEN_MAN_HINH.md) | Luồng di chuyển màn hình |
| [`03_STYLE_APP.md`](03_STYLE_APP.md) | Style guide / design system |
| [`04_SCREENSHOT_MAPPING.md`](04_SCREENSHOT_MAPPING.md) | Đối chiếu màn hình ↔ screenshot |

## B. Dựng app (sau khi đã có đủ 4 tài liệu ở mục A)

Đây là tài liệu **dùng chung cho mọi app** trong dự án (copy nguyên khi làm app mới), không mô tả
riêng tính năng của app hiện tại.

- **[`integration/`](integration/)** — tích hợp lib `base-application` (ads/IAP/splash/language/
  notification). Xem [`integration/README.md`](integration/README.md) để có thứ tự đọc đầy đủ (playbook
  tổng quát → spec lib → ads (kiến trúc/API/firebase key/vận hành) → intro-check → **chuẩn encode+R8**).
- **[`update-sdk-language/`](update-sdk-language/README.md)** — gói nâng cấp đã ÁP DỤNG vào core này: SDK
  mới + compileSdk/targetSdk 36, màn Language custom, Screen tracking (`screen_view` tên `<PREFIX>_<Màn>`),
  dialog Full-screen intent. Core đã có sẵn code các phần này; app mới chỉ đọc để hiểu và **đổi prefix**
  (xem "Việc phải làm khi clone" bên dưới). Mẫu tài liệu event: [`ANALYTICS_EVENTS.md`](ANALYTICS_EVENTS.md).
- **[`QUY_CHUAN_DESIGN.md`](QUY_CHUAN_DESIGN.md)** — ⚠️ đọc TRƯỚC khi code màn hình đầu tiên. Quy chuẩn
  dùng chung mọi app: 15 vai trò typography của Material3 (không được tự đặt tên), thang cỡ chữ mặc
  định, bảng tra "cấp chữ trên màn → role nào", và cách map `03_STYLE_APP.md` (§2 màu, §3 typography)
  vào `ui/theme/Type.kt`/`Theme.kt`/`Color.kt` — không hardcode `fontSize`/`fontWeight`/`Color(0x...)`
  tại từng màn hình.
- **[`UI_FIDELITY_WORKFLOW.md`](UI_FIDELITY_WORKFLOW.md)** — dùng Roborazzi để render 1 Composable ra
  PNG trên JVM (không cần emulator), so trực tiếp với screenshot gốc ở [`screenshot/`](screenshot/),
  lặp tới khi khớp.

## C. Việc phải làm khi clone core sang app mới

Core đã có sẵn Splash / Language / Intro / Permission / IAP + tracking. Chỉ cần làm:

1. **Đổi prefix tracking** `QR` → mã 2–3 chữ của app: các hằng trong `object Dest`
   (`ui/nav/AppNav.kt`: `AD_Home`, `AD_Intro`, `AD_First_Language`, …) và `docs/ANALYTICS_EVENTS.md`.
2. **Đổi package / `rootProject.name` / `applicationId`**, rồi rà `grep -rn "com.awakedrive.drowsedetect" app/src`
   và `encode-scripts/` (xem `integration/ENCODE_R8_STANDARD.md`).
3. **Home** (`ui/home/HomeScreen.kt`) đang là placeholder: giữ `CheckForAppUpdateWithDialog()` và
   `CheckFullScreenIntentPermission()`, thay phần còn lại. Thêm màn mới vào `AppNav.kt` kèm
   `TrackScreen(Dest.X)` (xem `update-sdk-language/SCREEN_TRACKING.md` §2–3.5).
4. **Ad unit thật**: `default_ads_config.json` đang dùng id test; `native_language_select` cần unit riêng. `banner_splash` chỉ cần khai ở file này, SDK tự quyết định có hiện hay không.
5. **Text**: viết lại `sdk_strings.xml` và câu `full_screen_intent_permission_message` (21 ngôn ngữ) cho đúng app;
   Intro: ảnh `intro1..4` + string `intro1..4(Sub)`.
6. **IAP lần đầu: ĐÃ CHỐT bỏ** màn IAP sau Language (`MyLanguageActivity` truyền `MainActivity::class.java`), không đổi.
7. Sửa/ xoá `docs-sample/`, `_ref/` trước khi push build (xem `integration/PLAYBOOK_TICH_HOP.md` §E.5b).
