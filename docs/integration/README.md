# Tích hợp `base-application` AAR (ads/IAP/splash/language/notification)

> **Nhóm tài liệu "đọc theo từng bước"** — dùng khi ĐÃ có đủ 4 tài liệu thiết kế màn hình/chức năng
> (`../01_MO_TA_CHUC_NANG_MAN_HINH.md` ... `../04_SCREENSHOT_MAPPING.md`) và bắt đầu **dựng app thật**
> — tích hợp lib `base-application` + quảng cáo/IAP. Đây là bộ playbook **tái dùng cho mọi app** trong
> dự án — không phải tài liệu riêng của app cụ thể nào, nên khi copy app mới chỉ cần copy nguyên cả
> thư mục `integration/` này.

## Thứ tự đọc

1. **[`PLAYBOOK_TICH_HOP.md`](PLAYBOOK_TICH_HOP.md)** — đọc đầu tiên của nhóm này. Quy trình 5 phase
   tích hợp lib + catalog lỗi thực chiến (C-1 → C-11) + cách dùng `push.sh`.
2. **[`HUONG_DAN_TICH_HOP.md`](HUONG_DAN_TICH_HOP.md)** — spec đầy đủ của lib (mọi hàm abstract phải
   override, tài nguyên bắt buộc, luồng runtime Splash→Inter→Language→IAP→Home). Tra cứu chi tiết khi
   `PLAYBOOK_TICH_HOP.md` không đủ.
3. **[`ADS.md`](ADS.md)** — mọi thứ về quảng cáo trong 1 file: kiến trúc & cách gọi API lib (không tự
   gọi Google Mobile Ads SDK), kịch bản tần suất (enable/ratio/max + native xen kẽ sau interstitial),
   danh sách component "lõi ads" copy nguyên được sang app khác, pipeline gộp `firebase_key.json`, và
   bài học vận hành thực chiến (⚠️ bắt buộc đọc trước khi đụng ads: log bị strip ở release, shimmer
   kẹt, NO_FILL bình thường, encode gotchas, ký & remote).
4. **[`INTRO_CHECK.md`](INTRO_CHECK.md)** — logic quyết định có hiện màn Intro hay không (đếm số
   lần mở app + nguồn cài đặt qua InstallReferrer).
5. **[`ENCODE_R8_STANDARD.md`](ENCODE_R8_STANDARD.md)** — ⚠️ **bắt buộc đọc trước khi chạy
   `push.sh build_tool`/`rename-all.js` lần đầu cho 1 app**: mô hình "chỉ 2-3 file cần sửa khi clone
   app mới" (`rename-config-global.js`/`rename-config-key.js`/`proguard-rules.pro`), catalog bug
   encode/R8 thực chiến (namespace sai, manifest lệch rename, tên trùng Compose/API hệ thống,
   JNI/reflection thiếu keep-rule...), và checklist migrate chuẩn này sang app khác.
6. **[`PROGUARD_SDK_RECIPES.md`](PROGUARD_SDK_RECIPES.md)** — công thức proguard cho SDK phổ biến
   (CameraX/Media3/Room/Maps/Retrofit/WebRTC/libpag/OpenCV...)

## Nguyên tắc xuyên suốt (áp dụng mọi app dùng lib này)

- **Lib sở hữu vỏ khởi động** `Splash → Inter → Language → IAP → getHomeActivity()`. App **không** khai
  `MAIN/LAUNCHER`, không tự init ads/consent/FCM.
- **App chỉ giữ các màn tính năng** + tầng điều phối ads riêng + onboarding riêng (Intro).
- **Mã hoá (encode) không được làm gãy runtime**: xem cảnh báo "key động" ở `ADS.md` §5.
