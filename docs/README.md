# docs/ — Tài liệu cho AI đọc để dựng app

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
- **[`DESIGN_TOKENS.md`](DESIGN_TOKENS.md)** — ⚠️ đọc TRƯỚC khi code màn hình đầu tiên: map giá trị ở
  `03_STYLE_APP.md` (§2 màu, §3 typography) vào `ui/theme/Type.kt`/`Theme.kt`/`Color.kt` — không
  hardcode `fontSize`/`fontWeight`/`Color(0x...)` tại từng màn hình.
- **[`UI_FIDELITY_WORKFLOW.md`](UI_FIDELITY_WORKFLOW.md)** — dùng Roborazzi để render 1 Composable ra
  PNG trên JVM (không cần emulator), so trực tiếp với screenshot gốc ở [`screenshot/`](screenshot/),
  lặp tới khi khớp.
