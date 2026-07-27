# Docs chuyển React-Native → Kotlin (mẫu tái dùng cho mọi app)

> Bộ tài liệu này **bổ sung** cho `PLAYBOOK_TICH_HOP.md` (tích hợp lib `base-application` chung).
> Playbook lo phần *tích hợp thư viện*; bộ này lo phần **chuyển 1 app React-Native cụ thể sang Kotlin**
> sao cho **giống bản RN nhất** về hành vi (ads, onboarding) lẫn giao diện.
>
> Case mẫu gốc: **vga31a — Phone Software Update & Apps**.
> Project này: **vga34b — GeoStamp Camera** (`com.camera.geostamp`), clone từ **vga34d-kotlin**
> (app camera GPS-stamp CÙNG DÒNG — tái dùng ~80% hạ tầng CameraX/Media3/Room/ads, chỉ viết mới
> Home/DataMap/DrawStyle + đổi hệ overlay 4-layout).
> **Khác biệt cụ thể vga34b (overlay, router bug đã sửa, route mồ côi, tiến độ) →
> `KE_HOACH_CHUYEN_KOTLIN_vga34b.md`.**

## Thứ tự đọc

0. **`KE_HOACH_CHUYEN_KOTLIN_vga34b.md`** — riêng vga34b: cơ chế overlay, router bug RN đã sửa, tiến độ, việc cần user cấp. *(đọc đầu tiên cho app này)*

1. **`PLAYBOOK_TICH_HOP.md`** — tích hợp lib (launcher/ads/IAP/noti), toolchain, R8, catalog lỗi. *(đọc trước)*
2. **`10_ADS.md`** — port toàn bộ kịch bản quảng cáo RN (bật/tắt, tỉ lệ, cap/ngày, native xen kẽ sau inter) sang lib.
3. **`11_FIREBASE_KEY.md`** — pipeline tạo `firebase_key.json` (gộp key RN cũ + key Kotlin + key lib + `ads_config`) và quy tắc mã hoá.
4. **`12_INTRO_CHECK.md`** — logic quyết định hiện màn Intro (đếm số lần mở + nguồn cài InstallReferrer).
5. **`13_UI_GIONG_RN.md`** — cách dựng UI Compose bám sát bản RN (theme, box, header/tab, search, i18n, assets).
6. **`14_ADS_RUNTIME_LESSONS.md`** — ⚠️ **bài học VẬN HÀNH ads/build thực chiến** (println thay Log vì lib strip; shimmer tự ẩn khi fail; NO_FILL ở release; cache AdsViewModel; encode gotchas; ký & remote). **Đọc trước khi đụng ads.**
7. **`15_ADS_REUSABLE_COMPONENTS.md`** — thành phần ads copy dùng lại cho app khác (chỉ đổi package + UI màn hình).
8. **`CHECKLIST.md`** — việc cần làm, đánh dấu tiến độ; rà mỗi lần build (mục I = hay quên, đặc biệt ads).

## Nguyên tắc xuyên suốt

- **Lib sở hữu vỏ khởi động** `Splash → Inter → Language → IAP → getHomeActivity()`. App **không** khai `MAIN/LAUNCHER`, không tự init ads/consent/FCM.
- **App chỉ giữ các màn tính năng** + tầng điều phối ads riêng (kịch bản RN) + onboarding riêng (Intro).
- **Đối chiếu 1-1 với RN**: mỗi hành vi (khi nào hiện ads, khi nào hiện intro, box màu gì) phải truy được về file RN gốc. Ghi rõ nguồn (`file.js:line`) trong comment code.
- **Mã hoá (encode) không được làm gãy runtime**: xem cảnh báo "key động" ở `11_FIREBASE_KEY.md` §4.
