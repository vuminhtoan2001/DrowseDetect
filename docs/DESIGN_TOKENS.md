# Design tokens — map Style Guide (`03_STYLE_APP.md`) vào code

> Sau khi điền xong `03_STYLE_APP.md` (đặc tả), bước tiếp theo BẮT BUỘC trước khi code màn hình: map
> các giá trị đó vào **3 file token** dưới `ui/theme/` — KHÔNG hardcode `fontSize`/`fontWeight`/
> `Color(0x...)` tại từng màn hình/component. Mục đích: đổi 1 giá trị style (cỡ tiêu đề, màu chữ...)
> chỉ sửa ở token, mọi nơi dùng tự đổi theo — không phải sửa từng file màn hình.

## 1. `ui/theme/Type.kt` — cỡ chữ

Khai 1 `Typography()` duy nhất, đặt tên vai trò khớp Material3 (để gọi bằng
`MaterialTheme.typography.<role>`), map theo bảng ở `03_STYLE_APP.md` §3:

| Vai trò | Material3 role | Chuẩn chung (đổi theo Style Guide app) |
|---|---|---|
| Header/Hero (tiêu đề card lớn) | `headlineSmall` | 24sp Bold |
| Tiêu đề màn (AppBar/dialog/slide Intro) | `titleLarge` | 20sp Bold |
| Subtitle / tiêu đề nhóm (section) | `titleMedium` | 16sp SemiBold |
| Body chính (label hàng list) | `bodyLarge` | 15sp Medium |
| Body phụ / mô tả | `bodyMedium` | 14sp Regular |
| Caption (version text, phụ chú) | `bodySmall` | 12sp Regular |
| Chữ nút hành động chính | `labelLarge` | 16sp Bold |

Mọi Composable chỉ viết `style = MaterialTheme.typography.titleLarge` — không viết
`fontSize = 20.sp` hay `fontWeight = FontWeight.Bold` lặp lại tại nơi dùng. Nếu 1 màn cần 1 vai trò
chưa có trong bảng, thêm role Material3 còn trống (`labelMedium`, `displaySmall`...) vào `Type.kt`,
không tự chế `fontSize` tay.

## 2. `ui/theme/Color.kt` + `Theme.kt` — màu

- Màu có sẵn slot Material3 (nền, chữ, primary/secondary...) → set thẳng trong `lightColorScheme(...)`
  ở `Theme.kt`, dùng qua `MaterialTheme.colorScheme.<role>` (`background`, `onBackground`, `surface`,
  `onSurfaceVariant`, `primary`, `onPrimary`...) — KHÔNG dùng `Color.White`/`Color.Black`/`Color.Gray`
  trực tiếp tại màn hình.
- Màu KHÔNG có slot tương ứng (accent riêng vd màu icon Premium, màu 1 icon đặc thù) → khai 1 `val`
  tên riêng trong `Color.kt` (vd `PremiumGold`, `SettingIconTint`), import và dùng tên đó — vẫn sửa
  được đúng 1 chỗ dù không nằm trong `ColorScheme`.
- Quy tắc: thấy `Color(0xFF...)` hoặc `Color.White/Black/Gray` viết trực tiếp trong 1 file
  `ui/<màn hình>/*.kt` (ngoài `ui/theme/`) → sai, phải kéo lên thành token.

## 3. Kiểm tra nhanh trước khi coi 1 màn là "xong style"

```bash
grep -rn "fontSize = [0-9]\|Color(0x\|Color\.White\|Color\.Black\|Color\.Gray" app/src/main/java/.../ui/<màn_vừa_code>
```
Có kết quả → còn giá trị chưa kéo lên token, trừ trường hợp cố ý (ảnh/gradient/chrome quảng cáo
không theo theme app, hoặc màu one-off Style Guide đã ghi rõ chỉ dùng đúng 1 chỗ) — ghi rõ trong code
comment lý do giữ hardcode nếu rơi vào ngoại lệ này.

## 4. Ví dụ đã áp dụng trong khung sườn (tham khảo)

`app/src/main/java/.../ui/theme/Type.kt` + `Theme.kt` + `Color.kt` của khung sườn hiện tại là ví dụ đã
làm đúng quy tắc này — copy nguyên cấu trúc 3 file khi clone app mới, chỉ đổi giá trị theo
`03_STYLE_APP.md` của app đó.
