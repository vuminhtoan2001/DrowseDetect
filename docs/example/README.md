# Bộ tài liệu ví dụ (demo) — app vga52a-kotlin

Thư mục này là **ví dụ mẫu đầy đủ** cho quy trình mô tả tại
[`../APP_SPEC_PROCESS.md`](../APP_SPEC_PROCESS.md): trước khi dựng bất kỳ app mới nào (từ screenshot +
mô tả chức năng), dev cần chuẩn bị đúng 4 loại tài liệu bắt buộc + 1 tài liệu đối chiếu chéo dưới đây.
App **vga52a-kotlin** (đã build xong) được dùng làm "app demo" — nội dung các mục dưới đây được viết
lại bằng cách đọc chính code của app này, để dev sau này biết chính xác cần viết gì, ở mức chi tiết
nào.

| # | Tài liệu | Nội dung | Nguồn lấy dữ liệu (khi làm app MỚI, thay bằng nguồn tương ứng của app đó) |
|---|---|---|---|
| 1 | [`screenshot/`](screenshot/) | Toàn bộ ảnh chụp màn hình thật của app tham chiếu | Copy nguyên từ `vga52a/screenshot/` (app tham chiếu gốc) |
| 2 | [`01_MO_TA_CHUC_NANG_MAN_HINH.md`](01_MO_TA_CHUC_NANG_MAN_HINH.md) | Mô tả chi tiết chức năng từng màn hình: UI hiển thị gì, bấm vào làm gì, trạng thái/case đặc biệt | Với app demo: đọc code Kotlin đã build. Với app MỚI: mô tả bằng lời (chưa có code) dựa trên ảnh chụp màn hình + hiểu biết nghiệp vụ |
| 3 | [`02_LUONG_DI_CHUYEN_MAN_HINH.md`](02_LUONG_DI_CHUYEN_MAN_HINH.md) | Sơ đồ + bảng: từ màn A bấm gì thì tới màn B, điều kiện gì, ảnh hưởng lịch sử điều hướng ra sao | Với app demo: đọc file điều hướng trung tâm trong code. Với app MỚI: tự vẽ dựa trên số lượng màn hình và mục đích từng màn |
| 4 | [`03_STYLE_APP.md`](03_STYLE_APP.md) | Design system: font, bảng màu, typography, bo góc, khoảng cách, nút, component đặc thù | Với app demo: đọc file theme/màu trong code + đo giá trị trong các màn cụ thể. Với app MỚI: đo trực tiếp từ ảnh chụp màn hình (màu, tỉ lệ, khoảng cách) |
| 5 | [`04_SCREENSHOT_MAPPING.md`](04_SCREENSHOT_MAPPING.md) | Đối chiếu chéo: mỗi màn hình ở tài liệu #2 có đủ ảnh minh hoạ ở #1 không, và mỗi ảnh ở #1 có khớp được vào 1 màn hình không — kèm quy tắc đặt tên file screenshot | Với app demo: đã đối chiếu xong, phát hiện cả ảnh thiếu lẫn ảnh đặt tên tệ. Với app MỚI: làm lại đúng quy trình ở mục 6 của file này cho bộ ảnh mới |

## Cách dùng bộ ví dụ này

- Nếu bạn sắp viết đặc tả cho 1 app mới: mở từng file trên, dùng làm **khuôn mẫu về độ chi tiết và
  cách trình bày** — không copy nội dung (nội dung là của riêng app camera/photo-editor này), chỉ
  copy **cấu trúc** (mục lục, cách chia mục "Mục đích/UI/Tương tác/Trạng thái đặc biệt/Điều hướng",
  cách vẽ sơ đồ mermaid, cách liệt kê bảng màu...).
- Nếu bạn đang dùng AI (Claude Code) để tự sinh 3 tài liệu functional/nav-flow/style cho app mới: đưa
  cho AI đọc 3 file này trước làm ví dụ tham chiếu văn phong, rồi giao ảnh chụp màn hình + mô tả
  nghiệp vụ của app mới để AI viết lại đúng format tương tự.

## Lưu ý quan trọng khi đọc 3 tài liệu này

Vì được sinh ra bằng cách đọc CODE THẬT (không phải suy đoán từ ảnh), 3 tài liệu này có ghi chú lại cả
những chỗ **code chưa hoàn thiện hoặc khác với thiết kế ban đầu** (vd: có tool đã lên kế hoạch nhưng
không có trong code, có route khai báo sẵn nhưng không dùng tới, có nút bấm chưa nối chức năng thật).
Đây là điều BÌNH THƯỜNG và CÓ ÍCH — mục đích của loại tài liệu này không phải "tài liệu marketing hoàn
hảo" mà là "tài liệu đúng sự thật để dev khác đọc và làm lại được chính xác", kể cả phần chưa xong.
