# vga52a-kotlin — Mục lục tài liệu cho DEV

> Project này có 3 thư mục tài liệu ở cấp gốc (`vga52a-kotlin/`), **3 vai trò khác nhau, tách biệt
> hoàn toàn** — đừng trộn nội dung giữa chúng:

| # | Thư mục | Vai trò | Đối tượng đọc |
|---|---|---|---|
| 1 | [`forDev/`](.) (thư mục này) | Quy trình viết đặc tả app mới (chỉ 1 tài liệu: `APP_SPEC_PROCESS.md`) | DEV (người) |
| 2 | [`../docs/`](../docs/) | Khuôn trắng (đặc tả) + tài liệu dựng app (tích hợp lib, UI fidelity) — AI đọc để dựng app | AI |
| 3 | [`../docs-sample/`](../docs-sample/) | Bản ví dụ hoàn chỉnh (cùng cấu trúc với phần đặc tả của `docs/`, đã điền đầy đủ cho app demo vga52a-kotlin) — CHỈ để dev tham khảo, **xoá đi khi dùng xong**, AI không cần đọc | DEV (tham khảo, tạm thời) |

## Quy trình đặc tả app mới

1. Đọc [`APP_SPEC_PROCESS.md`](APP_SPEC_PROCESS.md) — định nghĩa 4 tài liệu đặc tả bắt buộc + quy
   trình từng bước.
2. Xem [`../docs-sample/`](../docs-sample/) làm mẫu tham khảo (cách trình bày, độ chi tiết).
3. Điền phần đặc tả trong [`../docs/`](../docs/) cho app mới (copy khuôn trắng, viết nội dung thật).
4. Sau khi có đủ đặc tả: đọc `HANDOFF_NEXT_APP.md` (ở `vga49a/` — thư mục cha) để chọn app mẫu gần
   giống nhất trong dự án làm khung code, rồi tiếp tục theo
   [`../docs/integration/README.md`](../docs/integration/README.md),
   [`../docs/DESIGN_TOKENS.md`](../docs/DESIGN_TOKENS.md) (map Style Guide vào code — làm TRƯỚC khi
   code màn hình) và [`../docs/UI_FIDELITY_WORKFLOW.md`](../docs/UI_FIDELITY_WORKFLOW.md) để dựng app thật.

## Nguyên tắc khi thêm tài liệu mới

Trước khi thêm 1 file `.md` vào `forDev/`, xác nhận nó là hướng dẫn quy trình cho DEV (không phải nội
dung AI đọc để dựng app — cái đó thuộc `docs/`). Nếu 1 file nói về app/tính năng KHÁC vga52a-kotlin
(copy nhầm từ project khác lúc clone) hoặc chỉ là ghi chú tiến độ tạm thời của 1 phiên làm việc (không
phải tài liệu tham chiếu lâu dài) — không thêm vào đây, hoặc xoá nếu phát hiện đã lỡ thêm.
