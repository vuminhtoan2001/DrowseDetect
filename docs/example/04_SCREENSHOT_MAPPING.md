# vga52a-kotlin — Mapping Màn hình ↔ Screenshot (ví dụ mẫu)

> Tài liệu ví dụ trong `docs/example/` — đối chiếu từng màn hình mô tả ở
> [`01_MO_TA_CHUC_NANG_MAN_HINH.md`](01_MO_TA_CHUC_NANG_MAN_HINH.md) với file ảnh thật trong
> [`screenshot/`](screenshot/), để lộ ra 2 loại lỗi hay gặp nhất khi làm bộ screenshot cho 1 app:
> **màn hình không có ảnh minh hoạ**, và **có ảnh nhưng đặt tên/mapping tệ nên coi như không tồn tại**.
> Bộ ảnh thật của vga52a mắc phải CẢ HAI loại lỗi — dùng luôn làm ví dụ cảnh báo bên dưới thay vì bịa
> tình huống giả định.

## 1. Quy tắc đặt tên screenshot (áp dụng khi làm app mới)

```
<slug_man_hinh>[_<trang_thai_hoac_cong_cu>].<ext>
```

- `slug_man_hinh`: snake_case, tiếng Anh, ngắn gọn nhưng đủ để nhận ra ngay — nên khớp (rút gọn từ)
  đúng tên màn hình dùng trong `01_MO_TA_CHUC_NANG_MAN_HINH.md`, để việc mapping ở mục 2 làm bằng cách
  so khớp chuỗi cũng đúng gần hết, không phải đoán.
- `_<trang_thai_hoac_cong_cu>` (tuỳ chọn): thêm khi 1 màn có NHIỀU ảnh — ghi rõ đang ở trạng thái/tool
  nào (`_empty`, `_filter_panel`, `_draw_tool`...), KHÔNG dùng hậu tố mơ hồ như `_2`, `_all`, `_final`.
- **Mỗi khái niệm chỉ có 1 tên** — không tạo 2 tên gần giống nhau cho cùng 1 ý nghĩa (xem mục 4, ví dụ
  `collage_edit_text.jpg` vs `collage_text_edit.jpg` — thứ tự 2 từ đảo ngược, không ai đoán được khác
  nhau ở đâu nếu không mở ảnh ra xem).
- **Không dùng tên do thiết bị tự sinh** (`Screenshot_20260724_082506_Digital Camera HD - 4K
  Camera.jpg`) — đây là tên mặc định khi chụp màn hình, không nói lên nội dung gì. Luôn đổi tên NGAY
  sau khi chụp, trước khi đưa vào thư mục `screenshot/` của tài liệu (xem mục 3 — 8 file kiểu này lọt
  vào bộ ảnh thật của app mẫu, khiến nội dung của chúng gần như "vô hình" cho tới khi có ai mở từng
  file ra xem).

## 2. Bảng mapping đầy đủ

Cột "File RAW đã xác nhận nội dung" = file còn tên mặc định của thiết bị, đã **mở ảnh ra xem thật** để
xác định nó thuộc màn nào (không đoán qua tên) — kèm đề xuất tên mới nên đổi thành.

| # | Màn hình (theo `01_...md`) | File đã đặt tên | File RAW đã xác nhận nội dung → tên đề xuất | Trạng thái |
|---|---|---|---|---|
| 1 | Intro (4 slide) | `intro1.jpg`, `intro2.jpg`, `intro3.jpg` | — | ⚠️ THIẾU ảnh slide 4 "Get Started" |
| 2 | Permission | *(không có)* | — | ⚠️ THIẾU HOÀN TOÀN |
| 3 | Home | `home.jpg` | — | Đủ |
| 4 | Setting | *(không có)* | — | ⚠️ THIẾU HOÀN TOÀN |
| 5 | TakePhotoSuccessfully | *(không có)* | — | ⚠️ THIẾU (màn tĩnh, dễ bị bỏ quên khi chụp) |
| 6 | Camera — chế độ Ảnh | `take_photo_wb.jpg` (panel WB), `takephoto_amp.jpg` (panel AMP), `takephoto_filter.jpg` (panel Filter), `takephoto_collage.jpg` (Collage live-shoot) | `Screenshot_..._082506...jpg` → **panel Exposure 40%** → đổi thành `camera_photo_exposure.jpg` | Đủ **sau khi** đổi tên file raw |
| 7 | Camera — chế độ Video | `take_video.jpg` | — | Đủ |
| 8 | Camera — Menu "More" | *(không có tên rõ)* | `Screenshot_..._094705...jpg` → **More→Resolution** (FHD/2K/4K) → `camera_more_resolution.jpg`; `Screenshot_..._094708...jpg` → **More→Collage, chọn layout ô vuông đơn** → `camera_more_collage_layout.jpg`; `Screenshot_..._094723...jpg` → cùng panel, layout 3-cột, ảnh bị **chồng hình do chụp giữa lúc cuộn** → có thể xoá, đã có 094708 thay thế | ⚠️ 3 ảnh RAW ĐÚNG LÀ ảnh của màn này nhưng chưa ai đặt tên — nếu không mở từng ảnh ra xem sẽ tưởng màn "Menu More" thiếu ảnh hoàn toàn |
| 9 | VideoResult | *(không có)* | — | Không có ảnh — **khớp với phát hiện ở `01_...md`**: màn này hiện "mồ côi", không được gọi tới trong luồng quay-video thật → thiếu ảnh ở đây có thể là tín hiệu xác nhận thêm, không hẳn là lỗi khi chụp |
| 10 | ImportPhotoResult | `detail_video.jpg`, `detail_video_all.jpg` | — | ⚠️ chỉ có bản **VIDEO**, THIẾU ảnh cho case **ảnh tĩnh** của cùng màn |
| 11 | MediaPickerScreen (chọn ảnh dùng chung) | `collage_list_and_select.jpg` | — | Đủ (chụp trong bối cảnh Collage nhưng áp dụng chung cho Edit Photo) |
| 12–13 | EditPhotoScreen + 7 tool | `edit_image_all.jpg` (màn tổng quan, chưa mở tool — hậu tố "all" ở đây nghĩa là "toàn cảnh", KHÔNG phải gộp nhiều ảnh), `edit_image_filter.jpg`, `edit_image_frame.jpg`, `edit_image_sticker.jpg` | — | ⚠️ THIẾU ảnh cho **Text / Draw / RemoveBG / Rotate** (4/7 công cụ không có ảnh minh hoạ) |
| 14 | Collage Picker | *(dùng chung `collage_list_and_select.jpg`)* | — | Đủ |
| 15 | Collage Maker Editor | `collage_all.jpg`, `collage_background.jpg`, `collage_edit_text.jpg`, `collage_layout.jpg`, `collage_ratio.jpg`, `collage_sticker.jpg`, `collage_text_edit.jpg` | `Screenshot_..._083630...jpg` & `Screenshot_..._083632...jpg` → **Text đang chọn, tầng chính** (2 ảnh gần như giống hệt nhau, chụp cách nhau 2 giây — thừa 1 trong 2); `Screenshot_..._083704...jpg` → Text đang chọn/kéo; `Screenshot_..._083743...jpg` → **panel Draw đang mở (Brush, Undo/Redo, màu, Size 5, Opacity 57)** → đổi thành `collage_draw.jpg` | ⚠️ xem mục 4 — 2 file đã đặt tên (`collage_edit_text.jpg`/`collage_text_edit.jpg`) tên gần như không phân biệt được; ảnh RAW lấp đúng lỗ hổng "thiếu ảnh cho tool Draw" |
| 16 | Wallpaper Templates | `wallpaper_template.jpg` | — | Đủ |
| 17 | Wallpaper Editor | `wallpaper.jpg`, `wallpaper_example_1.jpg`, `wallpaper_example_2_can_edit_image.jpg`, `wallpaer_edit_text.jpg`, `wallpaper_add_edit_text.jpg` | — | ⚠️ xem mục 4 — 2 file cuối tên khác nhau nhưng nội dung đã mở ra xem khá giống nhau; `wallpaer_edit_text.jpg` lỗi chính tả (thiếu chữ "p") |
| 18 | Gallery | `gallary.jpg`, `gallary_photo.jpg`, `list_video.jpg` | — | ⚠️ lỗi chính tả nhất quán "gallary" → đúng phải là "gallery" (cả 2 file) |
| — | *(không khớp màn nào trong code Kotlin)* | `welcome.jpg` | — | ⚠️ **ORPHAN** — xem giải thích ở mục 3 |

## 3. Screenshot orphan — có ảnh nhưng không khớp màn hình nào

`welcome.jpg` không khớp bất kỳ mục nào trong `01_MO_TA_CHUC_NANG_MAN_HINH.md`. Tra lại
`vga52a/docs/00_OVERVIEW.md` (tài liệu phân tích app RN gốc) thì đây là 1 màn **Welcome** đứng riêng
TRƯỚC chuỗi 4 slide Intro trong app tham chiếu — nhưng khi lập trình vga52a-kotlin, màn Welcome đã
được **gộp bỏ**, Intro bắt đầu thẳng bằng slide "Smart Camera Tools". Đây là orphan **có lý do rõ
ràng** (đã cố ý thay đổi so với bản tham chiếu), không phải screenshot thừa do nhầm lẫn — nhưng vẫn
cần ghi chú lại, nếu không người đọc sau sẽ thắc mắc "màn Welcome đâu rồi".

## 4. Tên trùng lặp / mơ hồ (đã mở ảnh ra xem để xác nhận, không chỉ đoán qua tên)

| Cặp file | Đã mở ra xem — khác nhau ở đâu | Đề xuất đổi tên |
|---|---|---|
| `collage_edit_text.jpg` vs `collage_text_edit.jpg` | File 1 = tầng chính Text đang chọn (chưa mở tầng phụ nào); file 2 = đã mở tầng phụ **ARC** (thanh trượt bẻ cong đang ở 0%) | `collage_text_main.jpg` / `collage_text_arc.jpg` |
| `wallpaer_edit_text.jpg` vs `wallpaper_add_edit_text.jpg` | Cả 2 cùng nền mẫu hình trái tim + có 1 hộp thoại "Detail video" lạ đè lên (nhiều khả năng là chụp nhầm lúc chuyển màn, hoặc lỗi hiển thị của app tham chiếu) — **cần hỏi lại người chụp** xem có đúng là 2 bước khác nhau hay chụp trùng | Sửa chính tả trước: `wallpaper_edit_text.jpg`; sau đó xác nhận lại nội dung rồi đặt tên phân biệt rõ hoặc xoá bớt 1 file |

## 5. Tổng kết cảnh báo (đọc nhanh)

- ⚠️ **Màn hình thiếu screenshot:** Permission, Setting, TakePhotoSuccessfully, Intro slide 4,
  ImportPhotoResult (case ảnh tĩnh), Edit Photo (Text/Draw/RemoveBG/Rotate).
- ⚠️ **Screenshot orphan:** `welcome.jpg` (có lý do — xem mục 3).
- ⚠️ **Screenshot RAW chưa đặt tên** (8 file `Screenshot_*.jpg`): 7/8 đã xác định được nội dung ở mục
  2 và có tên đề xuất; sau khi đổi tên, phần lớn mục "thiếu screenshot" ở màn **Camera Menu More** và
  **Camera Exposure panel** thực ra ĐÃ ĐỦ ảnh, chỉ là chưa được đặt tên.
- ⚠️ **Tên trùng lặp/mơ hồ:** `collage_edit_text.jpg` ↔ `collage_text_edit.jpg`.
- ⚠️ **Lỗi chính tả:** "gallary" → "gallery" (2 file), "wallpaer" → "wallpaper" (1 file).

## 6. Quy trình kiểm tra chéo (làm thủ công, không cần công cụ ngoài)

1. Liệt kê toàn bộ tên file trong `screenshot/` (kể cả file còn tên raw của thiết bị).
2. Liệt kê toàn bộ tiêu đề màn hình trong `01_MO_TA_CHUC_NANG_MAN_HINH.md`.
3. Với mỗi màn hình ở bước 2: tìm ít nhất 1 file ở bước 1 có tên khớp (so khớp từ khoá, không phân
   biệt hoa/thường) → đánh dấu **ĐỦ** hoặc **THIẾU**.
4. Với mỗi file ở bước 1 CHƯA khớp được ở bước 3 (đặc biệt mọi file tên dạng raw): **mở ảnh ra xem nội
   dung thật** (đừng đoán qua tên) → hoặc khớp được vào 1 màn đã có (đổi tên cho khớp), hoặc xác nhận
   đúng là orphan và ghi rõ lý do (như mục 3).
5. Gom toàn bộ THIẾU + ORPHAN + tên trùng lặp/mơ hồ thành 1 bảng cảnh báo như mục 2, 3, 4 ở trên — đưa
   vào **Bước 4 (Rà soát & xác nhận)** của quy trình chung ở
   [`../APP_SPEC_PROCESS.md`](../APP_SPEC_PROCESS.md), gửi lại người giao việc để bổ sung ảnh còn
   thiếu hoặc xác nhận lý do orphan trước khi bắt đầu code.
