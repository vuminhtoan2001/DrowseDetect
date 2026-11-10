# Tracking, màn Language custom, Full-screen intent, nâng SDK 36 — đã tích hợp sẵn trong core

Core này **đã có sẵn** các phần dưới đây. Folder này là tài liệu giải thích cách chúng hoạt động và quy tắc khi
sửa / thêm màn. App mới **không phải làm lại** các bước, chỉ làm mục "Việc cần làm cho app mới".

| Phần | Code trong app | Tài liệu |
|---|---|---|
| SDK mới, compileSdk / targetSdk 36, bảng version | `base-application/`, `build.gradle.kts`, `app/build.gradle` | [UPDATE_SDK.md](UPDATE_SDK.md) (xem khi nâng SDK lần sau + test Android 16) |
| Tracking `screen_view` + event | `platform/ScreenTracker.kt`, `platform/OnboardingExit.kt`, `TrackScreen` ở `AppNav.kt` / Intro / Permission, `trackLibScreens()` ở `HostApplication` | [SCREEN_TRACKING.md](SCREEN_TRACKING.md) |
| Màn Language custom (2 native ad, auto-select / auto-confirm) | `ui/language/*`, `ui/common/Immersive.kt`, Settings mở `MyLanguageActivity` | [LANGUAGE_SCREEN_CUSTOM.md](LANGUAGE_SCREEN_CUSTOM.md) |
| Dialog quyền Full-screen intent (Android 14+) | `ui/common/FullScreenIntentPermissionDialog.kt`, gọi ở `HomeScreen` | [FULL_SCREEN_INTENT_PERMISSION.md](FULL_SCREEN_INTENT_PERMISSION.md) |
| Tài liệu event cho người đọc số GA4 | — | [ANALYTICS_EVENTS.md](ANALYTICS_EVENTS.md) (mẫu Vexaro, có giải thích cách đọc) và [../ANALYTICS_EVENTS.md](../ANALYTICS_EVENTS.md) (bản của app hiện tại) |

Code mẫu trong các tài liệu này lấy từ app Vexaro (prefix `VD`); bản trong core đã được chuyển sang package
của app và prefix `QR`.

## Quyết định đã chốt

- **Lần đầu mở app BỎ màn IAP** sau Language: `MyLanguageActivity` gọi
  `confirmLanguageSelection(this, code, MainActivity::class.java)`. Vì vậy không có `<PREFIX>_First_Iap` trong
  funnel lần đầu (không phải drop).
- Prefix tracking mặc định `QR`, định nghĩa trong `object Dest` (`ui/nav/AppNav.kt`).

## Việc cần làm cho app mới

1. Đổi prefix `QR` trong `object Dest`; viết lại `docs/ANALYTICS_EVENTS.md` theo màn thật của app.
2. Màn mới: thêm route vào `AppNav.kt` kèm `TrackScreen(Dest.X)` ([SCREEN_TRACKING.md](SCREEN_TRACKING.md) §2, §3.5).
   Timer tự chuyển màn phải gate theo `RESUMED` (§3.8).
3. Ad unit thật cho `native_language` và `native_language_select` trong `app/src/main/assets/default_ads_config.json`.
4. Viết lại câu `full_screen_intent_permission_message` (21 ngôn ngữ) cho đúng app.
5. Đổi package: `MyLanguageActivity` import đúng Home, manifest khai đúng tên, và
   `"MyLanguageActivity"` nằm trong `PROTECTED_FOLDER_AND_FILE_BASE` của `encode-scripts/safe-skip-base.js`
   (thiếu thì bản encode crash `ActivityNotFoundException`).
6. Đăng ký custom dimension trên GA4 (`method`, `ads_variant`, `page`, `code`…).

## Test nghiệm thu (máy thật, cài mới)

- Luồng lần đầu: Splash → Language custom → Intro / Home. Log: `adb logcat -s ScreenTracker AppEvents`.
- Settings → Language → đổi ngôn ngữ → quay lại Settings, UI đổi ngôn ngữ.
- Android 14+: dialog Full-screen intent hiện ở Home, bấm Allow mở đúng Settings, không có app-open ad đè lên.
- Android 16: Back ở mọi màn (kể cả IAP của SDK), màn không bị thanh hệ thống đè ([UPDATE_SDK.md](UPDATE_SDK.md) §5).
- Lặp lại luồng lần đầu trên **bản đã chạy script encode**; lỗi thì tra bảng ở [SCREEN_TRACKING.md](SCREEN_TRACKING.md) §7.

Mỗi tài liệu có mục "Pitfall" / "Test" ghi lỗi đã gặp thật ở Vexaro — đọc trước khi sửa.
