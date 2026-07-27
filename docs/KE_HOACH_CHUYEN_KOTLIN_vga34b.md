# Kế hoạch — vga34b "GeoStamp Camera" (`com.camera.geostamp`)

> Kế hoạch đầy đủ (2 lỗi router RN đã xác minh, cơ chế overlay 4-layout, bảng khác biệt màn hình,
> tiến độ P0-P7) nằm ở plan gốc: `C:\Users\Admin\.claude\plans\bright-jumping-otter.md`. File này
> tóm tắt để đọc nhanh trong repo.

## Khác biệt lớn nhất so với vga34d (nền clone)

Cùng dòng camera GPS-stamp nhưng: **Home không có bottom nav** (Camera vào qua card banner lớn,
Settings qua icon ở header), **DataMap thay Location** (form chi tiết: Title/GPS/Address/Sub-
district/Region/Country + toggle Auto On/Off), **DrawStyle** (màn mới: đổi Date&Time + ẩn/hiện map
icon), **Settings chỉ 4 mục** (Language/Share/Rate/Premium — Location/DateTime dời đi), **Template
chỉ 4 mẫu** (id 2/3/5/6, không phải 6 mẫu), **Gallery** (đổi tên từ Library, danh sách phẳng không
tách tab). Theme màu xanh dương `#0C87FF` (không phải cam-đỏ của vga34d).

## 2 lỗi router THẬT trong RN gốc (đã xác minh — KHÔNG replicate)

1. `components/LayoutUI/index.js:72` — back-button fallback gọi `navigation.replace('Main')`
   nhưng route `'Main'` không tồn tại (sẽ crash) → Kotlin dùng `popBackStack()`/`toHome()` an toàn.
2. `screens/DataMap.js:172` — filter xoá stack dùng `route.name !== 'Location'` nhưng route thật
   tên `DataMap` (quên sửa khi rename) → filter không khớp, DataMap kẹt trong back-stack → Kotlin
   `popUpTo(Screen.DataMap.route){inclusive=true}` đúng ý định gốc.
3. (Dư thừa, không phải bug) `Permission_home` trùng `Permission` — Kotlin chỉ 1 route.

## Route mồ côi phát hiện thêm (RN gốc, đã grep xác nhận không nơi nào gọi tới)

- **`DrawStyle`**: route đăng ký + code đầy đủ nhưng KHÔNG nút nào navigate tới. **Quyết định: PORT
  + bổ khuyết 1 icon (Palette) ở header Home** — màn có UI+ads hoàn chỉnh, không nên bỏ phí.
- **`ImportPhotoEdit`**: cũng route mồ côi (khác `ImportPhotoResult` — cái đó CÓ dùng, từ Gallery).
  **Quyết định: giữ code/route (đủ để hoạt động nếu gọi) nhưng KHÔNG thêm nút** — RN không có card
  "Edit photo" ở Home (khác vga34d), có vẻ là tính năng đã bị bỏ chủ động, không phải thiếu sót.

## Cơ chế overlay — 4 layout khác hẳn vga34d (port trực tiếp `CameraTemplateType.js`)

- **id 3**: pill ngang bo tròn hết cỡ, map tròn trái + address/date/time phải. KHÔNG lat/long.
- **id 5**: thẻ dọc, map vuông TRÊN, text căn giữa dưới. KHÔNG lat/long.
- **id 6**: thẻ dọc, map vuông DƯỚI (sau text). KHÔNG lat/long.
- **id khác (mặc định, gồm id 2)**: full-width, map trên-trái + address, date/time 2 cột, rồi
  hàng footer Lat/Long riêng — layout DUY NHẤT có lat/long.
- `ui/overlay/TemplateOverlayRenderer.kt` (render off-screen ở density tuỳ biến để bake ảnh/video
  đúng tỉ lệ preview) và `LocationFetcher`/`core/BitmapUtils` giữ nguyên 100% từ vga34d — hoàn toàn
  generic, không cần đổi khi chuyển sang app camera khác.

## Trạng thái: BUILD XANH cả debug + release (R8) — 2026-07-04

`assembleDebug` + `assembleRelease` đều BUILD SUCCESSFUL (chỉ warning AppsFlyer AFj1kSDK companion
đã biết là lành tính + vài warning deprecated API không ảnh hưởng). Lỗi đã sửa lúc build:
- `strings.xml` bị trùng key với `strings_app_i18n.xml` (file NÀY chứa hầu hết text UI, đã có sẵn
  từ khi clone vga34d-kotlin — ĐỪNG thêm lại các key đã có ở đây vào `strings.xml`, chỉ
  `strings.xml` giữ config/baseapp/feature keys).
- Thiếu `import androidx.compose.foundation.layout.size` ở `DataMapScreen.kt`.
- `advertisement/` copy 0-thay-đổi-logic (như vga34d) — hoàn toàn generic, chỉ cần rebrand package.
- `MainActivity.kt`/`ShortcutHelper` của vga34d KHÔNG có logic Uninstall-shortcut (đã bị gỡ khỏi
  bản vga34d-kotlin lúc user tự polish thêm) → vga34b KHÔNG cần port `UninstallScreen` (đã xoá
  `ShortcutHelper.kt` dead code khi clone).

**CHƯA cài máy test thật** (không có `adb devices` lúc build) — bắt buộc làm trước khi coi là
xong, theo đúng nguyên tắc "R8 chỉ lộ lỗi lúc chạy release". APK: debug ~118MB, release ~35MB.

## Còn lại trước khi phát hành

- Cài máy test thật (debug + release) — ưu tiên #1.
- Test thủ công: Home → Camera lúc chưa có `customLocation` → phải qua Permission/DataMap trước
  (đúng ý định RN) → DataMap set vị trí → Camera → overlay đúng 1/4 layout theo template → chụp/
  quay → xem lại. DrawStyle đổi Date&Time/ẩn map → overlay cập nhật đúng. Gallery hiện đủ item.
- git commit (mới `git init`, chưa commit), remote origin+build_tool, encode-scripts salt riêng.
- Firebase RC export (project mới, cần user cấp thông tin) → firebase_key.json.
- `facebook_app_id`/`client_token`, `public_license_key` thật (đang TODO placeholder).
- Icon splash/notification riêng (đang dùng `logo.png` placeholder).
