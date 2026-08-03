# HV40 - PDF Reader — Style Guide / Design System

> Tài liệu này được trích xuất **tự động từ file Figma** `HV_Apps (Copy) - Partial file saved 3-8-2026.fig`,
> canvas **"HV40 - PDF Reader"** (1 trong 54 canvas mini-app nằm trong file design chung của công ty).
> File `.fig` là định dạng nhị phân (zip chứa dữ liệu mã hoá Kiwi + nén zstd); các giá trị dưới đây được
> lấy bằng cách giải mã trực tiếp scene graph (~3.236 node thuộc canvas này) và thống kê tần suất dùng
> màu/font/bo góc/khoảng cách trên toàn bộ node — **không phải đọc từ code**, nên nên xem là bản tham
> chiếu thiết kế (design reference), cần đối chiếu lại với code thực tế khi lên app.

## 0. Tổng quan phong cách

App đọc file PDF/DOC/PPT/XLS theo phong cách **flat, nền sáng** (khác hẳn tông nền tối của vga52a-kotlin).
Nền chủ đạo là **trắng** cho màn chính (Home, Splash, Settings) và **xám nhạt `#E3E4E4`** cho các màn
công cụ phụ (Table of Contents, Go To Page, Search, Text Mode). Chữ dùng 1 màu mực gần đen-navy
(`#040516`) thay vì đen tuyền, tạo cảm giác mềm hơn so với đen `#000000` thuần (đen tuyền chủ yếu dùng
cho icon/glyph, không dùng cho chữ). Toàn bộ UI dùng **fill màu đặc (solid)** — 1.953/1.997 lượt tô màu
trong canvas là SOLID, chỉ có 39 lượt dùng ảnh và 5 lượt dùng gradient (đều là icon trang trí, không phải
nền màn hình) — nên khi build lại UI, ưu tiên màu đặc, tránh thêm gradient nếu bản gốc không có.

## 1. Font

- Font UI chính: **Inter** — áp dụng cho gần như toàn bộ chữ trong app (title, body, label, buttons).
  Các weight dùng: Regular, Medium, Bold, và một số biến thể Italic/Medium Italic cho chú thích phụ
  ("Note: ...", "Find here...").
- 2 font phụ **Poppins** (Medium, 6sp) và **League Spartan** (Medium, 5sp) chỉ xuất hiện ở nhãn watermark
  "Ad" cực nhỏ trên khung quảng cáo — đây là style mặc định của SDK quảng cáo, không phải font UI của
  app, không cần áp dụng khi code lại giao diện.
- Quy tắc weight: **Bold** cho tiêu đề màn/tên file/số trang lớn, **Medium** cho nhãn cài đặt và mô tả
  cỡ trung, **Regular** cho caption/mô tả phụ nhỏ.

## 2. Bảng màu

### Nền & bề mặt
| Tên | Mã màu | Dùng cho |
|---|---|---|
| Background (màn chính) | `#FFFFFF` | Nền Home, Splash/Intro, Settings, Popup/card |
| Background (màn công cụ) | `#E3E4E4` | Nền Table of Contents, Go To Page, Search, PDF Text Mode |
| Card / Popup nổi | `#FFFFFF` | Popup menu "More" (bo góc 12), thẻ danh sách file |
| Divider/hairline nhạt | `#F1F1F1` | Đường kẻ phân tách nhẹ giữa các dòng danh sách |

### Chữ (Text)
| Tên | Mã màu | Dùng cho |
|---|---|---|
| Text Primary (mực chính) | `#040516` | Toàn bộ tiêu đề, tên file, nội dung cài đặt, label chính — dùng nhiều nhất thay cho đen tuyền |
| Text Primary (biến thể) | `#0E1646` | Xuất hiện xen với `#040516` ở 1 số mô tả — cùng vai trò, coi như cùng nhóm |
| Text Secondary / Meta | `#7A9FBF` | Ngày giờ + dung lượng file ("Mar 4 10:01 AM \| 100 KB"), placeholder ô tìm kiếm, chú thích phụ ("Note:", "Experimental") |
| Text trên nền tối/màu | `#FFFFFF` | Chữ trong popup "More" (mục Full Screen/Toggle Theme/Go To Page...), chữ trên nút hồng Prev/Next Page |
| Icon/glyph đen tuyền | `#000000` | Icon outline, số trang "1/2", không dùng cho khối chữ dài |

### Màu nhấn / hành động
| Tên | Mã màu | Dùng cho |
|---|---|---|
| Action Pink (nút điều hướng nổi) | `#FF4665` | Nút pill "Next Page"/"Prev Page" ở màn PDF Text Mode (113×40dp, bo góc 10, chữ trắng) |
| Brand accent (wordmark) | `#853EE8` | Chữ "PDF Reader" màu nhấn trên màn Splash |
| Gold/vàng đồng (icon phụ) | `#9F7E00` / `#DEAD00` / `#C8B100` | Nhóm 3 mã cùng tông vàng-đồng, dùng cho icon rating/premium (xuất hiện cùng lúc, khả năng cao là 3 điểm dừng của 1 icon gradient vàng) |
| Đỏ nhấn phụ | `#B50027` | Icon/nhãn cảnh báo, badge |

**Lưu ý:** đây là file design chia sẻ cho nhiều mini-app, nên `#FF4665` và `#853EE8` mới chỉ xác nhận
xuất hiện ở đúng 1 vị trí mỗi màu (nút Prev/Next Page và wordmark Splash) — cần Product/Design xác nhận
lại đây có phải màu "Primary" chính thức của app hay chỉ là màu demo tạm trước khi chốt brand.

## 3. Bo góc (Corner Radius)

| Bán kính | Số lượt dùng trong canvas | Dùng cho (ước lượng) |
|---|---|---|
| `12dp` | 382 (nhiều nhất) | Card, popup, khung ảnh preview — bán kính chuẩn của app |
| `99dp` / `100dp` | 180 + 75 | Bo tròn hoàn toàn — avatar, nút icon tròn, toggle switch |
| `8dp` | 154 | Card nhỏ hơn / item danh sách |
| `16dp` | 120 | Khối lớn hơn (hero card, banner ads) |
| `10dp` | 105 | Nút pill "Prev/Next Page" |
| `2–5dp` | ~70 | Khung wrapper phụ (Frame/Menu/Cate — bo góc rất nhẹ, gần như vuông) |

## 4. Khoảng cách (Auto Layout Spacing)

Số liệu lấy từ toàn bộ frame auto-layout ("stack") trong canvas — giá trị càng lặp nhiều thì càng là
chuẩn spacing chính của hệ thống.

| Vai trò | Giá trị phổ biến nhất → thấp dần |
|---|---|
| Khoảng cách giữa item (itemSpacing) | `12dp` > `8dp` > `10dp` > `4dp` > `16dp` |
| Padding ngang khối (left/right) | `12dp` > `24dp` > `8dp` > `16dp` > `10dp` > `32dp` |
| Padding dọc khối (top/bottom) | `12dp` > `16dp` > `10dp` > `8dp` > `4dp` |

→ Gợi ý: dùng thang spacing **4 / 8 / 10 / 12 / 16 / 24 / 32dp**, trong đó **12dp** là giá trị mặc định
khi không chắc chắn (padding item, gap giữa item, padding block đều lấy 12dp làm nền).

## 5. Viền & đổ bóng (Stroke & Effects)

- Độ dày viền chủ đạo: **1dp** (hairline, áp dụng cho hầu hết icon/khung), một số chỗ dùng **1.5dp**
  (trạng thái nhấn/chọn).
- Đổ bóng card/nút nổi: `drop-shadow(0px 4px 4px rgba(0,0,0,0.25))` — dùng cho popup/nút nổi trên nền.
- Đổ bóng màu (ambient, tinh tế hơn): `drop-shadow(0px 0px 4px rgba(30,33,100,0.16))` — màu bóng ánh
  tím-navy `#1E2164`, dùng cho khối cần nổi nhẹ nhàng hơn là đổ bóng đen thường.
- Bo sáng/tối 1px (inner shadow) ở mép trên/dưới 1 số khối tròn (icon toggle) tạo hiệu ứng bevel nhẹ
  — offset `(0, ±1)`, màu đen/trắng alpha 0.12–0.36.

## 6. Nút hành động (Buttons)

- **Nút pill nổi (Prev/Next Page — PDF Text Mode):** 113×40dp, bo góc 10dp, nền `#FF4665`, chữ 14sp
  Medium màu trắng.
- **Nút liên kết chữ (Start/Next — Onboarding):** không có nền màu riêng, chỉ là text 14sp Bold màu
  `#040516` đặt trên nền trắng của màn — phong cách onboarding tối giản, không dùng CTA nền màu ở các
  slide giữa.

## 7. Thành phần đặc thù

### 7.1 Danh sách file (Home / Recent / Search)
- Mỗi item: tên file 16sp Bold `#040516`, dòng meta (ngày + dung lượng) 12sp Regular `#7A9FBF` ngay dưới.
- Icon loại file (PDF/DOC/PPT/XLS) đặt bên trái, kèm khung ảnh preview bo góc nhẹ.

### 7.2 Popup "More" (menu thao tác file)
- Nền trắng, bo góc 12dp, mỗi dòng gồm icon + label — layout dạng list đơn giản (Full Screen, Toggle
  PDF Theme, Go To Page, Copy Page's Text, Table of Contents, Links in the File, Share/Print/Search...).

### 7.3 Cài đặt (Settings)
- Tiêu đề nhóm 14sp Medium màu nhấn phụ `#7A9FBF` (vd "Experimental", "Text Extraction"), mỗi dòng cài
  đặt: tiêu đề 14sp Medium `#040516` + mô tả phụ cùng cỡ, cùng dòng (không xuống 2 cỡ chữ khác nhau),
  kèm `Toggle Switch` bên phải (bo góc 99–100dp, dạng viên hoàn toàn tròn).

### 7.4 Ngôn ngữ (Language)
- Danh sách hơn 20 ngôn ngữ (Tiếng Việt, 中文, 日本語, العربية, বাংলা...) — mỗi item 14sp Medium, có icon
  cờ quốc gia riêng theo từng ngôn ngữ (`Flags/Vietnam`, `Flags/Japan`...).

## 8. Giá trị mặc định khuyến nghị (Recommended Defaults)

Dùng khi cần dựng nhanh 1 màn mới cùng phong cách với HV40 - PDF Reader:

- Nền màn chính: `#FFFFFF`; nền màn công cụ phụ: `#E3E4E4`.
- Chữ chính: `#040516` (không dùng đen tuyền `#000000` cho khối chữ dài).
- Chữ phụ/meta: `#7A9FBF`, cỡ 12sp Regular.
- Card/Popup: nền trắng, bo góc 12dp, đổ bóng `0 4px 4px rgba(0,0,0,.25)`.
- Nút pill hành động nổi: nền `#FF4665`, chữ trắng 14sp Medium, bo góc 10dp, cao 40dp.
- Toggle switch: bo góc 100% (viên tròn hoàn toàn).
- Khoảng cách chuẩn: item spacing 12dp, padding block 12dp — chỉ đổi sang 8/16/24dp khi khối cần chặt
  hơn hoặc rộng hơn.
- Font: Inter (Regular/Medium/Bold) cho toàn bộ UI.
