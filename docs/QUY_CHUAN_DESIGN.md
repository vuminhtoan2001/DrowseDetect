# Quy chuẩn design — bộ token mặc định, map Style Guide (`03_STYLE_APP.md`) vào code

> Sau khi điền xong `03_STYLE_APP.md` (đặc tả), bước tiếp theo BẮT BUỘC trước khi code màn hình: map
> các giá trị đó vào **3 file token** dưới `ui/theme/` — KHÔNG hardcode `fontSize`/`fontWeight`/
> `Color(0x...)` tại từng màn hình/component. Mục đích: đổi 1 giá trị style (cỡ tiêu đề, màu chữ...)
> chỉ sửa ở token, mọi nơi dùng tự đổi theo — không phải sửa từng file màn hình.

---

## 1. `ui/theme/Type.kt` — cỡ chữ

### 1.1 Luật nền: 15 vai trò CỐ ĐỊNH, không được tự đặt tên

`Typography` của Material3 là một class có **đúng 15 tham số**, không hơn. Truyền một tên không có
trong danh sách (`subLarge`, `captionBold`, `titleXL`...) sẽ lỗi biên dịch, và
`MaterialTheme.typography.<tên tự chế>` cũng không tồn tại:

```
e: Unresolved reference 'subLarge'.
```

Cần một cấp chữ mới → **lấy một trong 15 role còn trống**, không tự chế tên, cũng không quay lại
hardcode `fontSize` tại nơi dùng.

### 1.2 Ý nghĩa 15 role theo Material3

| Nhóm | Role | Material3 định nghĩa để dùng cho |
|---|---|---|
| **display** | `displayLarge` / `displayMedium` / `displaySmall` | Chữ lớn nhất. Dành cho khoảnh khắc "phô diễn" — onboarding, màn hình chào, con số cực lớn. Rất ít dùng; một màn thường **không** có role nhóm này. |
| **headline** | `headlineLarge` / `headlineMedium` / `headlineSmall` | Tiêu đề ngắn và quan trọng, hoặc con số nổi bật. Đây là nhóm để dùng font "có cá tính" nếu app có font riêng. |
| **title** | `titleLarge` / `titleMedium` / `titleSmall` | Chữ nhấn mạnh vừa phải, độ dài ngắn — tiêu đề màn, tên mục trong danh sách, tiêu đề nhóm. |
| **body** | `bodyLarge` / `bodyMedium` / `bodySmall` | Nội dung đọc thành đoạn. Dùng cho phần chữ dài, kể cả khi cỡ nhỏ. |
| **label** | `labelLarge` / `labelMedium` / `labelSmall` | Chữ trên **thành phần tương tác**: nút, tab, chip, và nhãn nhỏ chú thích cho hình ảnh. |

Trong mỗi nhóm, `Large > Medium > Small` — **luôn phải giảm dần**. Khác nhóm mà trùng cỡ thì bình
thường (`titleLarge` và `headlineSmall` có thể cùng 16sp, phân biệt nhau bằng độ đậm và ngữ cảnh),
nhưng cùng nhóm mà cấp nhỏ lại to hơn cấp lớn là **sai thang**, dùng lâu sẽ loạn.

### 1.3 Thang cỡ chuẩn cho app dạng tiện ích (đề xuất)

Thang mặc định của Material3 (`displayLarge` 57sp, `headlineLarge` 32sp...) thiết kế cho nội dung
marketing và màn hình lớn — đưa nguyên vào app tiện ích nhiều danh sách sẽ **to và thô**. Thang dưới
đây gọn hơn, giữ đúng thứ tự giảm dần trong từng nhóm:

| Role | Cỡ | Độ đậm | Đặt ở đâu |
|---|---|---|---|
| `displayLarge` | 28sp | SemiBold | Tiêu đề slide onboarding / màn chào |
| `displayMedium` | 24sp | SemiBold | *(để trống — dự phòng)* |
| `displaySmall` | 22sp | SemiBold | Con số lớn giữa màn (vd màn nhập PIN) |
| `headlineLarge` | 20sp | SemiBold | *(để trống — dự phòng)* |
| `headlineMedium` | 18sp | SemiBold | *(để trống — dự phòng)* |
| `headlineSmall` | 16sp | SemiBold | Tiêu đề hộp thoại |
| `titleLarge` | 16sp | SemiBold | Tiêu đề màn (AppBar) |
| `titleMedium` | 14sp | Medium | Tên mục trong danh sách (dòng chính) |
| `titleSmall` | 13sp | Medium | Tiêu đề nhóm / section trong màn cài đặt |
| `bodyLarge` | 14sp | Normal | Đoạn nội dung chính, mô tả dài |
| `bodyMedium` | 13sp | Normal | Mô tả phụ, dòng thứ hai của mục danh sách |
| `bodySmall` | 11sp | Normal | Chú thích, phụ chú, dòng trạng thái |
| `labelLarge` | 14sp | SemiBold | Chữ trên nút hành động |
| `labelMedium` | 12sp | Medium | Nhãn tab / thanh điều hướng dưới |
| `labelSmall` | 10sp | Medium | Badge, thời lượng video, số đếm |

Chỉnh cỡ theo `03_STYLE_APP.md` của từng app, nhưng **giữ nguyên cách chia vai trò** — đổi con số thì
mọi màn tự đổi theo, còn đổi vai trò thì phải rà lại từng nơi dùng.

### 1.4 Một màn hình cơ bản có bao nhiêu cấp chữ

Dùng bảng này để tra ngược: đang code một thành phần, tra ra role phải dùng.

| Cấp chữ trên màn | Role |
|---|---|
| Tiêu đề màn trên AppBar | `titleLarge` |
| Tiêu đề nhóm / section | `titleSmall` |
| Tên mục trong danh sách (dòng 1) | `titleMedium` |
| Dòng phụ dưới tên mục (dung lượng, thời gian, đường dẫn) | `bodyMedium` |
| Đoạn nội dung, mô tả dài | `bodyLarge` |
| Chú thích, trạng thái, phụ chú | `bodySmall` |
| Tiêu đề hộp thoại | `headlineSmall` |
| Nội dung hộp thoại | `bodyLarge` |
| Chữ trên nút chính / nút phụ | `labelLarge` |
| Nhãn tab, thanh điều hướng dưới | `labelMedium` |
| Badge, thời lượng, số đếm nhỏ | `labelSmall` |
| Tiêu đề slide onboarding | `displayLarge` |
| Mô tả slide onboarding | `bodyLarge` |
| Chữ/số cỡ lớn giữa màn (nhập PIN, đếm ngược) | `displaySmall` |
| Trạng thái rỗng: dòng tiêu đề | `titleMedium` |
| Trạng thái rỗng: dòng gợi ý | `bodySmall` |

Một màn thông thường chỉ dùng **4–6 cấp**. Cần tới cấp thứ 8 trở lên thường là dấu hiệu bố cục có vấn
đề, không phải thiếu role.

### 1.5 Trường hợp đặc biệt

Gặp một chỗ không khớp cấp nào ở §1.4 (chữ cực lớn trong ô đại diện, con số đếm ngược, watermark...):

1. **Xác định nó thuộc NHÓM nào** theo §1.2 — chữ trên thành phần bấm được → `label`; tiêu đề → `title`
   hoặc `headline`; chữ phô diễn → `display`; đoạn đọc → `body`.
2. **Lấy role còn trống trong đúng nhóm đó** (§1.3 chừa sẵn `displayMedium`, `headlineLarge`,
   `headlineMedium`), điền cỡ vào `Type.kt` kèm comment nói rõ dùng ở đâu.
3. Nhóm đó hết chỗ → **dùng lại role gần nhất rồi `.copy()` tại nơi dùng**, chỉ ghi đè đúng thuộc tính
   cần đổi:
   ```kotlin
   style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
   ```
   Cách này vẫn kế thừa cỡ chữ từ token — sau này đổi thang, chỗ này đổi theo.

Ba việc **không** được làm: tự chế tên role (§1.1), viết thẳng `fontSize = 20.sp` tại màn hình, và
nhét một cấp chữ vào role sai nhóm chỉ vì role đó đang trống.

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

Có kết quả → còn giá trị chưa kéo lên token. Ngoại lệ được chấp nhận:

- **Lớp phủ mờ và nền cố định đen** (`Color.Black.copy(alpha = …)` sau hộp thoại, nền trình phát video
  / trình xem ảnh) — đen theo bản chất, không đổi theo theme.
- **Màu one-off mà Style Guide đã ghi rõ chỉ dùng đúng 1 chỗ.**

Cả hai ngoại lệ đều **phải có comment ngay tại dòng đó nói rõ vì sao giữ hardcode**. Không comment thì
lần rà sau không ai phân biệt được đâu là ngoại lệ cố ý, đâu là sót.

## 4. Sau khi đổi token thì phải cập nhật lại tài liệu

Bảng ở §1.3 và bảng màu ở `03_STYLE_APP.md` là **đặc tả**, không tự đồng bộ với code. Đổi thang cỡ chữ
hoặc bảng màu trong `ui/theme/` mà không sửa tài liệu thì lần clone app sau sẽ copy theo số cũ. Coi
việc cập nhật 2 tài liệu này là một phần của thay đổi style, không phải việc dọn dẹp làm sau.
