# Bản trắng để điền — đặc tả app MỚI

Đây là bản **trắng** (chưa có nội dung) của 4 tài liệu bắt buộc mô tả ở
[`../APP_SPEC_PROCESS.md`](../APP_SPEC_PROCESS.md). Dùng thư mục này làm điểm bắt đầu khi đặc tả một
app MỚI — copy cả thư mục `template/` này sang project của app mới rồi điền vào.

Mỗi file `.md` bên dưới có 1 khối comment ở đầu file (không hiển thị khi render markdown) trỏ tới file
ví dụ tương ứng trong [`../example/`](../example/) — **mở file ví dụ đó ra tham khảo cấu trúc/độ chi
tiết trước khi viết**, đừng viết từ đầu.

| File trắng | Điền gì | File ví dụ tương ứng |
|---|---|---|
| [`screenshot/`](screenshot/) | Ảnh chụp màn hình thật (hoặc mockup) của TOÀN BỘ màn hình app mới — **đặt tên đúng quy tắc ngay lúc chụp** (xem `screenshot/README.md` + mục 1 của `04_SCREENSHOT_MAPPING.md`), đừng để tên raw của thiết bị rồi đổi sau | [`../example/screenshot/`](../example/screenshot/) |
| [`01_MO_TA_CHUC_NANG_MAN_HINH.md`](01_MO_TA_CHUC_NANG_MAN_HINH.md) | Mô tả chức năng từng màn hình (5 mục: Mục đích/UI/Tương tác/Trạng thái đặc biệt/Điều hướng) | [`../example/01_MO_TA_CHUC_NANG_MAN_HINH.md`](../example/01_MO_TA_CHUC_NANG_MAN_HINH.md) |
| [`02_LUONG_DI_CHUYEN_MAN_HINH.md`](02_LUONG_DI_CHUYEN_MAN_HINH.md) | Sơ đồ + bảng luồng di chuyển màn hình | [`../example/02_LUONG_DI_CHUYEN_MAN_HINH.md`](../example/02_LUONG_DI_CHUYEN_MAN_HINH.md) |
| [`03_STYLE_APP.md`](03_STYLE_APP.md) | Style guide / design system của app mới | [`../example/03_STYLE_APP.md`](../example/03_STYLE_APP.md) |
| [`04_SCREENSHOT_MAPPING.md`](04_SCREENSHOT_MAPPING.md) | Đối chiếu chéo màn hình ↔ screenshot — làm SAU CÙNG, khi đã có cả `screenshot/` lẫn `01_...md` | [`../example/04_SCREENSHOT_MAPPING.md`](../example/04_SCREENSHOT_MAPPING.md) |

**Nhắc lại nguyên tắc quan trọng nhất** (xem đầy đủ ở `../APP_SPEC_PROCESS.md` mục 1): chỉ mô tả
CHỨC NĂNG/NỘI DUNG/HÌNH THỨC, không nói công nghệ implement; chỗ nào phải suy đoán thì đánh dấu
**[ASSUMPTION]**; viết đủ chi tiết để một dev khác (chưa từng thấy app gốc) đọc xong tự làm ra chức
năng tương đương.
