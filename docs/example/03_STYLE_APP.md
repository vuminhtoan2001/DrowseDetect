# vga52a-kotlin — Style Guide / Design System (ví dụ mẫu)

> Tài liệu ví dụ trong `docs/example/` — bố cục tham khảo từ `vga51b/new_style.md.txt` (mẫu style
> guide chuẩn của dự án), nhưng toàn bộ giá trị bên dưới lấy từ code THẬT của `vga52a-kotlin`
> (`ui/theme/Theme.kt`, `ui/theme/AppFonts.kt`, `ui/components/Common.kt`, và các màn hình cụ thể),
> không phải suy đoán từ ảnh. Đây là ví dụ cho việc: khi làm app mới, cần 1 tài liệu style ở đúng mức
> chi tiết này (màu, chữ, bo góc, khoảng cách, nút, thành phần đặc thù) để bất kỳ ai — người hoặc AI —
> đọc xong có thể dựng UI đúng phong cách mà không cần đoán.

## 0. Tổng quan phong cách

App theo phong cách **camera/studio nền tối cố định** (không đổi theo dark/light mode hệ thống —
`AppTheme(darkTheme: Boolean = true)` luôn dùng bảng màu tối), accent màu **vàng-cam** làm điểm nhấn
chính cho mọi hành động chính (nút chụp, tab đang chọn, nút Save, trạng thái "đang bật"). Toàn bộ
canvas chỉnh sửa (Edit Photo/Collage/Wallpaper) và Camera dùng nền đen gần như tuyệt đối để nội dung
ảnh là thứ nổi bật nhất trên màn hình.

## 1. Font

- Font hệ thống mặc định (Material 3 `Typography()` mặc định — KHÔNG có font riêng cho chữ giao diện
  app, khác với các app dùng font thương hiệu riêng như `vga51b`).
- App có 1 bộ **font riêng cho công cụ Text** (chọn kiểu chữ khi thêm chữ lên ảnh ở Edit Photo/Collage
  Maker/Wallpaper) — đây là nội dung do người dùng tạo ra, không phải font của giao diện app. 11 họ
  font: Inter (4 weight: Regular/Medium/SemiBold/Bold), Numans, Nunito (4 weight), Open Sans (4
  weight), Orbitron (4 weight), Orelega One, Parisienne, Peddana, Poetsen One, Rammetto One, Readex
  Pro (4 weight).
- Font weight dùng cho chữ giao diện: chủ yếu **Bold (700)** cho tiêu đề/nút, **SemiBold (600)** cho
  nhãn phụ/tab, **Regular (400)** cho mô tả phụ.

## 2. Bảng màu

### Nền & bề mặt (nền tối cố định)
| Tên | Mã màu | Dùng cho |
|---|---|---|
| Background (App Background) | `#0E0E10` | Nền tổng thể Home/Settings/Gallery/màn danh sách |
| Surface (Card) | `#1C1815` | Card/thẻ trong lưới Home, bottom sheet |
| Surface Variant | `#221E1A` | Biến thể bề mặt phụ (Material `surfaceVariant`) |
| Canvas Editor / Toolbar nền | `#0A0B0D` | Nền thanh công cụ dưới cùng của Collage/Wallpaper Editor |
| Panel công cụ | `#15161A` | Nền panel công cụ (Frame/Filter/Sticker/Draw...) và card Home phụ |
| Wallpaper canvas trống | `#2A2C33` | Nền canvas khi Wallpaper Editor bắt đầu từ mẫu trống (mã 0) |

### Màu nhấn (Accent)
| Tên | Mã màu | Dùng cho |
|---|---|---|
| Primary (accent vàng-cam chính) | `#F5C518` | Nút chụp, tab/lựa chọn đang active, checkmark, nút Save, icon Premium/crown |
| Secondary accent (vàng-cam phụ) | `#ED9A16` | Nhãn "PRO MODE" và các nhãn phụ cùng tông |
| OnPrimary (chữ/icon trên nền Primary) | `#1A1512` | Chữ trên nút vàng (tương phản đủ với nền sáng) |

### Trạng thái / màu chức năng
| Tên | Mã màu | Dùng cho |
|---|---|---|
| Success | `#2E7D32` | Tiêu đề "Success!" ở màn chụp/lưu thành công |
| Error / Destructive | `#E53935` | Icon trái tim khi đã yêu thích (Favorite active), báo động xoá |
| Info action (link) | `#0C87FF` | Nút tròn "Chia sẻ" ở màn chi tiết ảnh/video |

### Chữ (Text)
| Tên | Mã màu | Dùng cho |
|---|---|---|
| Text Primary | `#EDEDED` | Chữ chính trên nền tối |
| Text Secondary | `#9B9B9B` | Chữ phụ/label section, mô tả phụ |
| Icon Gray | `#CFCFCF` | Icon phụ không cần nhấn mạnh |

> Ghi chú: app này KHÔNG dùng bảng màu nền sáng/pastel (khác `vga51b`) — nếu app mới của bạn theo
> phong cách "nền sáng, thân thiện" thay vì "studio nền tối", hãy thay thế toàn bộ bảng trên bằng bộ
> màu tương ứng (nền sáng, surface trắng, accent theo thương hiệu) và giữ nguyên CẤU TRÚC bảng (vẫn
> cần đủ: nền, surface, primary, secondary, success/error, text primary/secondary).

## 3. Typography (cỡ chữ thực tế đã dùng trong code)

| Vai trò | Cỡ chữ | Weight | Ví dụ dùng |
|---|---|---|---|
| Tiêu đề màn (App Bar Title) | 20sp | Bold | `AppHeader` — tiêu đề Settings/Gallery/EditPhoto... |
| Tiêu đề Hero/Card lớn | 24sp | Bold | Tiêu đề "Take Photo" trên Home |
| Tiêu đề slide Intro | 20sp | Bold | Headline từng slide Onboarding |
| Body / mô tả phụ | 13–16sp | Regular | Subtitle Intro (13sp), mô tả Permission/Success |
| Label nhỏ trên nút/tab | 11–14sp | SemiBold/Bold | Nhãn "PRO MODE" (11sp Bold), label tool Camera (13–14sp SemiBold) |
| Caption cực nhỏ (icon + nhãn công cụ) | 8–12sp | SemiBold | Nhãn dưới icon công cụ trong Editor (Frame/Text/Filter...) |
| Chữ nút hành động chính (pill button) | 16sp | Bold | "Continue/Start" (Intro), tương đương nút Save |

**Quy tắc dùng:**
- Tiêu đề màn luôn 20sp Bold, căn giữa thanh tiêu đề (trừ Home căn trái).
- Nhãn cực nhỏ dưới icon công cụ (8–13sp) chỉ dùng trong khu vực thao tác dày đặc (thanh công cụ
  Camera/Editor) — không dùng cỡ này cho nội dung cần đọc kỹ.
- Không lồng 2 cỡ chữ "title" khác nhau trong cùng 1 khối nội dung.

## 4. Bo góc (Corner Radius)

| Thành phần | Bán kính |
|---|---|
| Nút hành động chính dạng viên thuốc (pill button, vd Continue/Save) | `50%` chiều cao (bo tròn hoàn toàn 2 đầu) |
| Tab dạng viên thuốc (PillTabs — Photo/Video/Templates ở Gallery) | 24dp |
| Card lớn (Hero Home, card Settings) | 16dp |
| Card nhỏ / ảnh preview trong lưới | 14dp |
| Icon container tròn (avatar tool, icon nền vàng nhạt) | 50% (hình tròn) hoặc 14dp (bo vuông nhẹ) tuỳ ngữ cảnh |
| Ô ảnh trong lưới chọn ảnh / collage | bo góc nhẹ (4–8dp) |

## 5. Khoảng cách (Spacing)

| Vai trò | Giá trị |
|---|---|
| Padding ngang toàn màn (screen horizontal padding) | 20dp |
| Padding dọc theo section (Home header) | 16dp |
| Khoảng cách giữa các card trong lưới (Home 2x2, Gallery) | 12dp |
| Khoảng cách icon trong hàng (Row spacedBy) | 16dp |
| Chiều cao thanh tiêu đề (`AppHeader`) | 56dp cố định (padding ngang 6dp trong thanh) |
| Chiều cao tab viên thuốc (`PillTabs`) | 46dp |
| Chiều cao nút hành động chính (pill Continue/Start) | 48dp |
| Padding nội dung card (card internal padding) | 16dp |
| Padding icon tròn trong Hero (kích thước nút shutter) | 52dp (đường kính nút) |

## 6. Nút hành động chính (Primary Button)

- Dạng viên thuốc (pill), bo góc `RoundedCornerShape(50)` — bo tròn hoàn toàn theo chiều cao.
- Chiều cao: 48dp.
- Nền: màu Primary (`#F5C518`).
- Chữ: 16sp, Bold, màu `OnPrimary` (`#1A1512`) — KHÔNG dùng chữ trắng trên nền vàng (độ tương phản
  kém hơn).
- Trạng thái loading: thay chữ bằng `CircularProgressIndicator` 20dp, màu `OnPrimary`, giữ nguyên
  kích thước nút (không co lại).
- Nút "Save" trong các Editor (Edit Photo/Collage/Wallpaper) dùng cùng công thức nhưng nhỏ hơn: bo
  tròn hoàn toàn (`CircleShape`), padding ngang 18dp / dọc 8dp thay vì chiều cao cố định — vẫn nền
  vàng `#F5C518`, chữ đen đậm.

## 7. Thành phần đặc thù của app (Editor & Camera components)

Đây là các quy ước UI lặp lại nhiều lần xuyên suốt app, nên tách riêng thành "component chuẩn" khi
làm app mới có cùng dạng chỉnh sửa ảnh:

### 7.1 Thanh công cụ đáy màn (Editor toolbar)
- Nền đen gần tuyệt đối (`#0A0B0D` hoặc trong suốt trên nền đen của canvas).
- Mỗi nút công cụ: icon nhỏ phía trên (24–28dp) + nhãn chữ rất nhỏ phía dưới (8–13sp).
- Trạng thái **đang chọn/đang bật**: icon + nhãn đổi sang màu Primary (`#F5C518`); **chưa chọn**: màu
  trắng (hoặc trắng alpha 0.85 cho nhãn phụ).
- Nút toggle dạng khối tròn (vd Focus mode, Resolution card): nền `Primary.copy(alpha = 0.25f)` khi
  active, `White.copy(alpha = 0.12f)` khi chưa active — không dùng viền, chỉ đổi độ mờ nền.

### 7.2 Panel công cụ (Tool panel — dùng `ToolPanelScaffold`)
- Đè lên phía dưới của canvas khi mở (canvas KHÔNG co lại hay bị đẩy lên).
- Tiêu đề căn giữa đầu panel (vd "Frame", "Filter", "Draw") + icon đóng (X) ở mép trái — dùng chung 1
  khung tiêu đề cho mọi panel (trừ Text — Text không có tiêu đề/khung riêng).
- Tab thể loại (nếu có): cuộn ngang, tab đang chọn tô màu Primary, còn lại trắng mờ.
- Lưới lựa chọn (thumbnail Frame/Sticker): 5 cột.

### 7.3 Lớp phủ xử lý / loading
- Toàn màn tối khoảng 50% (đen alpha ~0.5) + `CircularProgressIndicator` màu trắng, cỡ vừa, căn giữa
  màn hình — dùng thống nhất cho MỌI tác vụ nền (Save ảnh, Remove Background, xử lý ảnh nặng).
- Trong lúc phủ: vô hiệu hoá toàn bộ input khác (không chỉ riêng nút gây ra tác vụ).

### 7.4 Lớp đối tượng kéo-thả (chữ/sticker trên canvas)
- Đang chọn: viền mảnh màu Primary quanh đối tượng.
- 2 tay cầm tròn nhỏ cố định vị trí: góc **trên-trái** = xoá (icon X), góc **dưới-phải** = nhân bản
  (icon copy, đặt bản sao lệch nhẹ so với gốc).
- Cử chỉ: 1 ngón kéo = di chuyển; 2 ngón chụm/xoè + xoay = đồng thời phóng to-thu nhỏ (giới hạn
  0.3×–4×) và xoay.

### 7.5 Quảng cáo (Ad placeholder)
- Vị trí quảng cáo native luôn được reserve bằng 1 component riêng (`NativeAdPlaceholder`), tự ẩn
  hoàn toàn (không chiếm khoảng trắng) khi không có quảng cáo hợp lệ để hiển thị — không bao giờ để
  lại 1 khung trống có viền/placeholder tĩnh.

## 8. Giá trị mặc định khuyến nghị (Recommended Defaults)

Dùng khi cần dựng nhanh 1 màn mới cùng phong cách với app này:

- Nền màn: `#0E0E10` (Background) hoặc đen tuyền cho màn chỉnh sửa ảnh.
- Card/Surface: `#1C1815` hoặc `#15161A`.
- Tiêu đề màn: 20sp, Bold, `#EDEDED`, căn giữa thanh tiêu đề cao 56dp.
- Accent hành động chính: `#F5C518` nền, `#1A1512` chữ, bo góc 50% (pill).
- Body/mô tả phụ: 13–14sp, Regular, `#9B9B9B`.
- Card lớn: bo góc 16dp; card nhỏ/ảnh preview: bo góc 14dp.
- Khoảng cách ngang màn: 20dp; khoảng cách giữa card trong lưới: 12dp.
- Loading overlay: đen alpha 0.5 + vòng xoay trắng giữa màn.

## 9. Khi áp dụng style guide này cho MỘT APP KHÁC

Bảng màu/font ở trên là của riêng vga52a-kotlin (app camera/studio nền tối). Khi viết style guide cho
app mới:
1. Giữ nguyên **cấu trúc mục lục** (Font → Bảng màu → Typography → Corner Radius → Spacing → Primary
   Button → Component đặc thù → Recommended Defaults) — cấu trúc này đã chứng minh đủ để dựng UI nhất
   quán mà không cần đoán.
2. Thay **toàn bộ giá trị** (mã màu, cỡ chữ, bo góc...) bằng giá trị thật của app mới — lấy từ ảnh
   chụp màn hình + mô tả chức năng, không copy nguyên màu vàng-cam/nền tối của app này trừ khi app
   mới thật sự cùng phong cách.
3. Mục "7. Thành phần đặc thù" là mục QUAN TRỌNG NHẤT cần viết riêng cho từng app — đây là nơi ghi lại
   các quy ước lặp lại xuyên suốt UI (cách hiện panel, cách hiện trạng thái chọn/active, cách hiện
   loading...) mà nếu không ghi lại, mỗi màn mới sẽ tự sáng tạo một kiểu khác nhau, phá vỡ tính nhất
   quán của app.
