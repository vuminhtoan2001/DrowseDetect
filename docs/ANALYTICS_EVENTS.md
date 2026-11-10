# Tài liệu event Firebase / GA4 — QR Scanner (prefix `QR`)

Dành cho người đọc số trên GA4. Mẫu gốc: `docs/update-sdk-language/ANALYTICS_EVENTS.md`.
Lọc `screen_name` bắt đầu bằng `QR_`; đổi dimension báo cáo sang **"Page title and screen name"**.

## 1. screen_view (tên màn cố định)

| Nhóm | screen_name |
|---|---|
| Màn của SDK | `QR_Splash`, `QR_First_NotiPermission` / `QR_NotiPermission`, `QR_First_Iap` / `QR_Iap` |
| Language | `QR_First_Language` → `QR_First_LanguageSelect` (lần đầu); `QR_Language` → `QR_LanguageSelect` (mở từ Cài đặt) |
| Onboarding | `QR_Intro1` … `QR_Intro4` (+ param `ads_variant`), `QR_Permission` |
| Chính | `QR_Home`, `QR_Setting` |

- `First_` = lượt chạy đầu, trước khi user vào Home lần đầu.
- Lần đầu mở app **cố ý bỏ màn IAP** sau Language, nên **không có** `QR_First_Iap` trong funnel; đừng đọc là drop.
- Intro hiện theo **số lần mở app** (Remote Config `count_app_open`, `organic_number_not_guide`) và nguồn cài
  (ads / organic), không phải một lần duy nhất: cùng 1 user có thể thấy Intro nhiều lần. User organic ở lần mở 1
  vào thẳng Home, không qua Intro.

## 2. Event nghiệp vụ

| Event | Param | Ý nghĩa |
|---|---|---|
| `onboarding_route` | `intro_shown`, `is_ads_campaign`, `open_count` | Vì sao user có / không thấy Intro (mỗi lần mở app) |
| `language_select` | `code`, `method=click\|auto`, `from_settings` | Lần đầu chọn ngôn ngữ trong 1 lượt vào màn |
| `language_confirm` | `code`, `method=click\|auto`, `from_settings` | User (hoặc timer) xác nhận ngôn ngữ |
| `intro_next` | `from_page`, `method=click\|auto\|modal_close\|modal_error`, `ads_variant` | Sang slide kế. Có `intro_next` mà không có `screen_view` kế tiếp → navigation hỏng |
| `intro_modal` | `page`, `result=shown\|closed\|error`, `ads_variant` | Modal native toàn màn giữa slide. `shown` = app **yêu cầu** hiện, chưa chắc ad đã render |
| `onboarding_exit` | `screen` | User bấm Back thoát app ở Intro / Permission |

## 3. Custom dimension cần đăng ký (Scope = Event)

`method`, `ads_variant`, `page`, `result`, `code`, `from_settings`, `from_page`, `screen`, `intro_shown`,
`is_ads_campaign`, `open_count`.

Chỉ áp dụng cho dữ liệu **sau** khi đăng ký. Funnel nên lọc `app_version` ≥ bản có tracking này.

## 4. Lưu ý đọc số

- Auto `screen_view` của Firebase (không có tiền tố `QR_`) vẫn chạy song song, hãy lọc bỏ.
- Quay về màn đang đứng sau quảng cáo / IAP / CH Play / Settings hệ thống **không** log lại.
- Xem ngay bằng DebugView: `adb shell setprop debug.firebase.analytics.app com.camera.qrscanner.barcode`.
