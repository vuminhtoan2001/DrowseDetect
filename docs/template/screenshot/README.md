<!--
  CẦN LÀM GIỐNG: docs/example/screenshot/ (../../example/screenshot/)
-->

# Screenshot

Bỏ vào đây toàn bộ ảnh chụp màn hình (hoặc mockup) của app mới — mọi màn hình chính + mọi trạng thái
đáng chú ý (rỗng, đã điền dữ liệu, đang mở panel/dialog phụ...).

## Đặt tên NGAY lúc chụp — đừng để tên raw rồi đổi sau

```
<slug_man_hinh>[_<trang_thai_hoac_cong_cu>].<ext>
```

- Lowercase, snake_case, tiếng Anh, khớp (rút gọn) đúng tên màn hình sẽ dùng trong
  `01_MO_TA_CHUC_NANG_MAN_HINH.md` — vd `home.jpg`, `edit_photo_filter_panel.jpg`.
- Nhiều ảnh cùng 1 màn → thêm hậu tố mô tả rõ trạng thái/tool (`_empty`, `_draw_tool`...), KHÔNG dùng
  hậu tố mơ hồ kiểu `_2`, `_final`, `_all`.
- **Không** để nguyên tên do điện thoại tự sinh lúc chụp (`Screenshot_20260101_120000_...jpg`) — đổi
  tên NGAY, đừng để dồn lại "đổi sau" (xem `../04_SCREENSHOT_MAPPING.md` mục 1 — bộ ảnh ví dụ có 8 file
  bị bỏ quên bước này, hậu quả là 3 màn hình tưởng thiếu ảnh trong khi thực ra ảnh đã có, chỉ là không
  ai đặt tên/nhận ra).
- Mỗi khái niệm chỉ 1 tên — kiểm tra không có 2 tên gần giống nhau cho cùng 1 ý nghĩa (xem ví dụ lỗi
  thật ở `../04_SCREENSHOT_MAPPING.md` mục 4).

Tham khảo cách đặt tên đã áp dụng (và cả các chỗ chưa tốt, được ghi chú lại) ở
[`../../example/screenshot/`](../../example/screenshot/) + phân tích ở
[`../../example/04_SCREENSHOT_MAPPING.md`](../../example/04_SCREENSHOT_MAPPING.md).

Sau khi có đủ ảnh, làm bước đối chiếu chéo màn hình ↔ screenshot ở `../04_SCREENSHOT_MAPPING.md` để
phát hiện màn thiếu ảnh / ảnh thừa không khớp màn nào.

Xoá file `README.md` này khi thư mục đã có ảnh thật.
