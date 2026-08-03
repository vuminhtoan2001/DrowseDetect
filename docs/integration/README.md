# Tích hợp `base-application` AAR (ads/IAP/splash/language/notification)

> **Nhóm tài liệu "đọc theo từng bước"** — dùng khi ĐÃ có đủ 4 tài liệu thiết kế màn hình/chức năng
> (`../01_MO_TA_CHUC_NANG_MAN_HINH.md` ... `../04_SCREENSHOT_MAPPING.md`, xem quy trình viết ở
> [`../../forDev/APP_SPEC_PROCESS.md`](../../forDev/APP_SPEC_PROCESS.md)) và bắt đầu **dựng app thật**
> — tích hợp lib `base-application` + quảng cáo/IAP. Đây là bộ playbook **tái dùng cho mọi app** trong
> dự án — không phải tài liệu riêng của vga52a-kotlin (không nói về Camera/Edit Photo/Collage/
> Wallpaper), nên khi copy app mới chỉ cần copy nguyên cả thư mục `integration/` này.

## Thứ tự đọc

1. **[`PLAYBOOK_TICH_HOP.md`](PLAYBOOK_TICH_HOP.md)** — đọc đầu tiên của nhóm này. Quy trình 5 phase
   tích hợp lib + catalog lỗi thực chiến (C-1 → C-11) + cách dùng `push.sh`.
2. **[`HUONG_DAN_TICH_HOP.md`](HUONG_DAN_TICH_HOP.md)** — spec đầy đủ của lib (mọi hàm abstract phải
   override, tài nguyên bắt buộc, luồng runtime Splash→Inter→Language→IAP→Home). Tra cứu chi tiết khi
   `PLAYBOOK_TICH_HOP.md` không đủ.
3. **[`MIGRATE_ADS_TO_LIB.md`](MIGRATE_ADS_TO_LIB.md)** — công thức cơ học chuyển ads tự-gọi-GMA SDK
   sang `com.nlbn.ads` (`Admob.getInstance()...`).
4. **[`10_ADS.md`](10_ADS.md)** — port kịch bản quảng cáo (bật/tắt theo Remote Config, tỉ lệ, cap/ngày,
   native xen kẽ sau interstitial).
5. **[`11_FIREBASE_KEY.md`](11_FIREBASE_KEY.md)** — pipeline gộp `firebase_key.json` (key RN cũ + key
   Kotlin + key lib + `ads_config`) và quy tắc mã hoá key.
6. **[`12_INTRO_CHECK.md`](12_INTRO_CHECK.md)** — logic quyết định có hiện màn Intro hay không (đếm số
   lần mở app + nguồn cài đặt qua InstallReferrer).
7. **[`13_UI_GIONG_RN.md`](13_UI_GIONG_RN.md)** — khi port lại 1 app React Native đã có sẵn: cách dựng
   UI Compose bám sát bản RN (bảng ánh xạ RN↔Compose, theme, assets, i18n).
8. **[`14_ADS_RUNTIME_LESSONS.md`](14_ADS_RUNTIME_LESSONS.md)** — ⚠️ **bắt buộc đọc trước khi đụng
   ads**: bài học vận hành thực chiến (log bị strip ở release, shimmer kẹt, NO_FILL bình thường, encode
   gotchas, ký & remote).
9. **[`15_ADS_REUSABLE_COMPONENTS.md`](15_ADS_REUSABLE_COMPONENTS.md)** — danh sách file "lõi ads" copy
   nguyên được sang app khác (chỉ đổi package), so với file phải chỉnh theo từng app.
10. **[`16_ENCODE_R8_STANDARD.md`](16_ENCODE_R8_STANDARD.md)** — ⚠️ **bắt buộc đọc trước khi chạy
    `push.sh build_tool`/`rename-all.js` lần đầu cho 1 app**: mô hình "chỉ 2-3 file cần sửa khi clone
    app mới" (`rename-config-global.js`/`rename-config-key.js`/`proguard-rules.pro`), catalog đầy đủ
    bug encode/R8 đã gặp qua khảo sát 16 app (namespace sai, manifest lệch rename, tên trùng
    Compose/API hệ thống, JNI/reflection thiếu keep-rule...), và checklist migrate chuẩn này sang app
    khác.
11. **[`KE_HOACH_TICH_HOP.md`](KE_HOACH_TICH_HOP.md)** — **ví dụ case tham khảo** (kế hoạch tích hợp
    thật cho app `vga_49`/DetectorSpy, KHÔNG phải vga52a-kotlin) — giữ lại vì `PLAYBOOK_TICH_HOP.md`
    trích dẫn làm minh hoạ rủi ro/quyết định thực tế. Đọc để thấy 1 ví dụ đã áp dụng playbook, không
    phải tài liệu mô tả app hiện tại.

## Nguyên tắc xuyên suốt (áp dụng mọi app dùng lib này)

- **Lib sở hữu vỏ khởi động** `Splash → Inter → Language → IAP → getHomeActivity()`. App **không** khai
  `MAIN/LAUNCHER`, không tự init ads/consent/FCM.
- **App chỉ giữ các màn tính năng** + tầng điều phối ads riêng (nếu port từ RN) + onboarding riêng
  (Intro).
- **Nếu đang port lại 1 app RN đã có sẵn**: đối chiếu 1-1 với RN — mỗi hành vi (khi nào hiện ads, khi
  nào hiện intro, màu box gì) phải truy được về file RN gốc.
- **Mã hoá (encode) không được làm gãy runtime**: xem cảnh báo "key động" ở `11_FIREBASE_KEY.md` §4.
