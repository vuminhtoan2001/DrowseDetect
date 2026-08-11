# Tích hợp `base-application` AAR (ads/IAP/splash/language/notification)

> **Nhóm tài liệu "đọc theo từng bước"** — dùng khi ĐÃ có đủ 4 tài liệu thiết kế màn hình/chức năng
> (`../01_MO_TA_CHUC_NANG_MAN_HINH.md` ... `../04_SCREENSHOT_MAPPING.md`) và bắt đầu **dựng app thật**
> — tích hợp lib `base-application` + quảng cáo/IAP. Đây là bộ playbook **tái dùng cho mọi app** trong
> dự án — không phải tài liệu riêng của app cụ thể nào, nên khi copy app mới chỉ cần copy nguyên cả
> thư mục `integration/` này.

## Thứ tự đọc

1. **[`PLAYBOOK_TICH_HOP.md`](PLAYBOOK_TICH_HOP.md)** — đọc đầu tiên của nhóm này. Build R8/release,
   catalog lỗi build chung, test bằng adb, cách dùng `push.sh` (đẩy 2 remote). Không còn phần migrate
   lib vào app cũ — base skeleton đã tích hợp sẵn.
2. **[`ADS.md`](ADS.md)** — cách gọi ads có sẵn trong app (native/inter/native-sau-inter), setup lần
   đầu khi clone app mới (test mode + key bắt buộc), quy ước key `_enable`/`_showRatio`/`_maxShowPerDay`.
3. **[`COMMON_ACTIONS.md`](COMMON_ACTIONS.md)** — 5 pattern không phải ads nhưng dùng chung mọi app:
   check update (Play Core, chỉ gọi ở Home), mở màn Language/IAP của lib, rate app (In-App Review), share app.
4. **[`ENCODE_R8_STANDARD.md`](ENCODE_R8_STANDARD.md)** — ⚠️ **bắt buộc đọc trước khi chạy
   `push.sh build_tool`/`rename-all.js` lần đầu cho 1 app**: mô hình "chỉ 2-3 file cần sửa khi clone
   app mới" (`rename-config-global.js`/`rename-config-key.js`/`proguard-rules.pro`), catalog bug
   encode/R8 thực chiến (namespace sai, manifest lệch rename, tên trùng Compose/API hệ thống,
   JNI/reflection thiếu keep-rule...), và checklist migrate chuẩn này sang app khác.
5. **[`PROGUARD_SDK_RECIPES.md`](PROGUARD_SDK_RECIPES.md)** — checklist ngắn khi thêm 1 SDK mới hoặc
   build bản có minify: 3 câu tự hỏi (JNI? reflection? model serialize?) để biết có cần thêm rule
   không, không phải catalog recipe cho từng SDK cụ thể.

> Đã xoá `HUONG_DAN_TICH_HOP.md` (spec đầy đủ của lib — không cần đọc lại khi base đã tích hợp sẵn) và
> `INTRO_CHECK.md` (logic gate Intro đã chốt, baked sẵn trong skeleton — app mới chỉ đổi ảnh/title,
> không cần đọc lại logic).

## Nguyên tắc xuyên suốt (áp dụng mọi app dùng lib này)

- **Lib sở hữu vỏ khởi động** `Splash → Inter → Language → IAP → getHomeActivity()`. App **không** khai
  `MAIN/LAUNCHER`, không tự init ads/consent/FCM.
- **App chỉ giữ các màn tính năng** + tầng điều phối ads riêng + onboarding riêng (Intro).
- **Mã hoá (encode) không được làm gãy runtime**: xem cảnh báo "key động" ở `ADS.md` §5.
