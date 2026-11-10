# Quy trình đặc tả app mới (từ screenshot + mô tả chức năng)

> **Tài liệu này (docs cho DEV đọc)** định nghĩa **quy chuẩn chung**: trước khi bắt đầu dựng bất kỳ
> app Android mới nào (viết lại từ 1 app tham chiếu chỉ có screenshot, hoặc làm app hoàn toàn mới),
> cần chuẩn bị đủ **4 loại tài liệu đặc tả** liệt kê ở mục 1, TRƯỚC KHI chọn công nghệ / bắt đầu code.
> Đây là nơi DUY NHẤT chứa hướng dẫn/giải thích cách làm — 2 nhóm tài liệu dưới đây KHÔNG lặp lại
> hướng dẫn, chỉ chứa nội dung đặc tả thuần.
>
> Tài liệu này KHÔNG nói về công nghệ implement (Compose/CameraX/Room/ads SDK...) — phần đó thuộc về
> [`docs/integration/PLAYBOOK_TICH_HOP.md`](../docs/integration/PLAYBOOK_TICH_HOP.md) và
> [`START_HERE.md`](../START_HERE.md) (đọc SAU khi đã có đủ 4 tài liệu đặc tả ở đây — xem mục 2 dưới).
>
> **[`../docs/`](../docs/) — docs cho AI đọc, dùng để dựng app, KHÔNG chứa thông tin thừa.** Khuôn
> trắng (chỉ có tiêu đề mục, không có đoạn hướng dẫn/comment nào) — copy thư mục này cho app MỚI, điền
> theo hướng dẫn ở mục 2 bên dưới. Xem [`docs/README.md`](../docs/README.md).
>
> **[`../docs-sample/`](../docs-sample/) — bản ví dụ, KHÔNG dành cho AI đọc, chỉ để DEV tham khảo khi
> viết `docs/`, xoá đi khi dùng xong.** Cùng cấu trúc với `docs/`, nhưng là bản đã điền đầy đủ cho app
> demo **vga58a-kotlin** (app này). Xem [`docs-sample/README.md`](../docs-sample/README.md).

## 1. Bốn tài liệu bắt buộc cho MỖI app

| # | Tài liệu | Trả lời câu hỏi | Ví dụ mẫu |
|---|---|---|---|
| 1 | **Screenshot** | App trông như thế nào, thật sự có những màn nào? | [`docs-sample/screenshot/`](../docs-sample/screenshot/) |
| 2 | **Mô tả chức năng từng màn hình** | Mỗi màn có gì, bấm vào làm gì, có case đặc biệt nào? | [`docs-sample/01_MO_TA_CHUC_NANG_MAN_HINH.md`](../docs-sample/01_MO_TA_CHUC_NANG_MAN_HINH.md) |
| 3 | **Luồng di chuyển màn hình** | Từ màn A bấm gì thì tới màn B? Điều kiện gì? | [`docs-sample/02_LUONG_DI_CHUYEN_MAN_HINH.md`](../docs-sample/02_LUONG_DI_CHUYEN_MAN_HINH.md) |
| 4 | **Style / Design system của app** | Màu gì, chữ cỡ nào, bo góc/khoảng cách bao nhiêu? | [`docs-sample/03_STYLE_APP.md`](../docs-sample/03_STYLE_APP.md) |

**Tài liệu đối chiếu chéo đi kèm (bắt buộc):** sau khi có cả tài liệu #1 (screenshot) và #2 (mô tả
chức năng), phải đối chiếu 2 chiều — mỗi màn hình có đủ ảnh không, mỗi ảnh có khớp vào 1 màn không —
và cảnh báo mọi chỗ lệch. Xem chi tiết ở Bước 3.5 và
[`docs-sample/04_SCREENSHOT_MAPPING.md`](../docs-sample/04_SCREENSHOT_MAPPING.md).

**Nguyên tắc chung cho cả 4 tài liệu:**
- Chỉ mô tả **CHỨC NĂNG / NỘI DUNG / HÌNH THỨC** — không nói công nghệ implement. Một app mới chưa
  biết sẽ dùng công nghệ gì, nên tài liệu đặc tả phải đọc được độc lập với công nghệ.
- Viết dựa trên **bằng chứng thật** (ảnh chụp màn hình thật, hoặc code thật nếu đang port 1 app đã có
  sẵn) — chỗ nào phải suy đoán thì đánh dấu rõ **[ASSUMPTION]** hoặc **[CẦN XÁC NHẬN]**, không viết
  như thể đó là sự thật đã kiểm chứng.
- Đủ chi tiết để một dev (hoặc AI) khác — CHƯA từng thấy app gốc — đọc xong có thể tự làm ra chức năng
  tương đương, kể cả các case đặc biệt/trạng thái phụ (rỗng, lỗi, loading, đã mua premium...).

## 2. Quy trình từng bước

### Bước 0 — Thu thập Screenshot
Chụp/sưu tầm đầy đủ screenshot của app tham chiếu, phủ hết:
- Mọi màn hình chính (không chỉ màn "đẹp"/quan trọng).
- Mọi trạng thái đáng chú ý của cùng 1 màn nếu có (rỗng, đã điền dữ liệu, đang mở 1 panel/tool con,
  dialog phụ...).

**Đặt tên file NGAY lúc chụp, theo đúng quy tắc** (đừng để tên raw của thiết bị rồi tính đổi sau —
80% trường hợp sẽ không ai quay lại đổi, xem ví dụ hậu quả thật ở
[`docs-sample/04_SCREENSHOT_MAPPING.md`](../docs-sample/04_SCREENSHOT_MAPPING.md) mục 1):

```
<slug_man_hinh>[_<trang_thai_hoac_cong_cu>].<ext>
```

- `slug_man_hinh`: lowercase, snake_case, tiếng Anh, khớp (rút gọn) đúng tên màn hình sẽ dùng ở Bước 1
  — để bước đối chiếu chéo (Bước 3.5) so khớp được bằng chuỗi, không phải đoán.
- Nhiều ảnh cùng 1 màn → thêm hậu tố mô tả rõ trạng thái/tool (`_empty`, `_filter_panel`...), KHÔNG
  dùng hậu tố mơ hồ (`_2`, `_final`, `_all`).
- Mỗi khái niệm chỉ 1 tên — không đặt 2 tên gần giống nhau cho cùng 1 ý nghĩa.
- **Không** dùng tên do thiết bị tự sinh (`Screenshot_20260101_120000_....jpg`).

Nếu KHÔNG có app tham chiếu để chụp ảnh (app hoàn toàn mới, chưa tồn tại) — bước này thay bằng
**mockup/wireframe** phác thảo, hoặc mô tả bằng lời chi tiết đủ để suy ra được bố cục.

### Bước 1 — Viết mô tả chức năng từng màn hình (`01_MO_TA_CHUC_NANG_MAN_HINH.md`)
Với MỖI màn hình, viết đủ 5 mục sau (xem cấu trúc mẫu chi tiết trong
[`docs-sample/01_MO_TA_CHUC_NANG_MAN_HINH.md`](../docs-sample/01_MO_TA_CHUC_NANG_MAN_HINH.md)):

1. **Mục đích màn hình** — 1-2 câu, góc nhìn người dùng.
2. **Các thành phần UI và nội dung hiển thị** — mô tả bằng lời bố cục/nội dung thật, không dùng thuật
   ngữ lập trình (không nói "Composable", "Modifier", "ViewModel"...).
3. **Tương tác người dùng** — mỗi nút/cử chỉ: bấm/chạm làm gì, điều hướng tới màn nào (ghi rõ tên màn
   đích), điều kiện gì kèm theo (quyền, quảng cáo xen giữa, trạng thái premium...).
4. **Trạng thái/case đặc biệt** — rỗng, loading, lỗi, đã/chưa cấp quyền, đã/chưa mua premium, v.v.
5. **Điều hướng vào/ra màn hình này** — liệt kê rõ "từ đâu tới đây" và "từ đây đi đâu".

Nguồn thông tin để viết mục này:
- Nếu **port lại 1 app đã có code** (như case vga58a → vga58a-kotlin): đọc trực tiếp code UI của
  từng màn — chính xác nhất, không cần đoán.
- Nếu **chỉ có screenshot + mô tả nghiệp vụ** (app hoàn toàn mới, giao việc dạng "làm app giống ảnh
  này, chức năng như sau..."): kết hợp đọc kỹ từng ảnh chụp màn hình với mô tả nghiệp vụ do người
  giao việc cung cấp; chỗ ảnh không thể hiện rõ (case rỗng, lỗi...) → suy luận hợp lý và đánh dấu
  **[ASSUMPTION]**, liệt kê lại thành danh sách cần xác nhận riêng.

### Bước 2 — Vẽ luồng di chuyển màn hình (`02_LUONG_DI_CHUYEN_MAN_HINH.md`)
Xem cấu trúc mẫu trong [`docs-sample/02_LUONG_DI_CHUYEN_MAN_HINH.md`](../docs-sample/02_LUONG_DI_CHUYEN_MAN_HINH.md). Cần có:
1. **Sơ đồ tổng quan** (khuyến khích dùng cú pháp `mermaid flowchart` — render được trực tiếp trên
   GitHub/nhiều công cụ đọc markdown) thể hiện toàn bộ màn hình + hướng di chuyển.
2. **Bảng chi tiết vào/ra từng màn**: cột "Vào từ", "Điều kiện khi vào", "Ra tới", "Điều kiện khi ra".
3. **Quy tắc back-stack (lịch sử điều hướng)** đáng chú ý — chỗ nào cố tình xoá lịch sử để chặn nút
   Back (vd sau khi hoàn tất 1 luồng tạo nội dung), chỗ nào cố tình GIỮ lịch sử (vd để nút huỷ quay
   lại đúng màn trước thay vì về thẳng trang chủ).
4. **Phân biệt điều hướng thật (đổi màn) với overlay/dialog tại chỗ** (mở panel, mở dialog xác nhận,
   mở bottom sheet...) — 2 loại này rất dễ nhầm nếu chỉ nhìn mô tả chức năng ở Bước 1.

### Bước 3 — Viết Style Guide (`03_STYLE_APP.md`)
Xem cấu trúc mẫu trong [`docs-sample/03_STYLE_APP.md`](../docs-sample/03_STYLE_APP.md), tham khảo thêm định dạng
gốc ở `vga58a/new_style.md.txt`. Cấu trúc chuẩn cần có đủ:
1. Font (tên font, các weight dùng).
2. Bảng màu (nền/surface, accent/primary, trạng thái success-warning-error, màu chữ).
3. Typography (cỡ chữ + weight cho từng vai trò: tiêu đề màn, tiêu đề nhóm, body, caption, chữ nút).
4. Bo góc (corner radius) cho từng loại thành phần (card lớn/nhỏ, nút, input, dialog, icon container).
5. Khoảng cách (spacing): padding ngang màn, khoảng cách giữa section, giữa card, chiều cao nút/input.
6. Quy cách nút hành động chính (Primary Button): kích thước, màu, chữ, trạng thái loading.
7. **Thành phần đặc thù của riêng app này** (mục quan trọng nhất — không có sẵn công thức chung, phải
   tự rút ra từ ảnh/nghiệp vụ): các quy ước UI lặp lại xuyên suốt app (cách hiện trạng thái
   chọn/active, cách hiện loading, cách hiện panel/toolbar, cách hiện đối tượng kéo-thả...).
8. Giá trị mặc định khuyến nghị (Recommended Defaults) — tóm tắt nhanh để dựng 1 màn mới đúng phong
   cách mà không cần đọc lại toàn bộ.

Nguồn lấy giá trị: đo trực tiếp từ ảnh chụp màn hình (dùng công cụ đo màu/khoảng cách trên ảnh), hoặc
đọc code theme thật nếu đang port app đã có sẵn.

### Bước 3.5 — Đối chiếu Màn hình ↔ Screenshot (`04_SCREENSHOT_MAPPING.md`)
Xem cấu trúc mẫu + kết quả thật trong
[`docs-sample/04_SCREENSHOT_MAPPING.md`](../docs-sample/04_SCREENSHOT_MAPPING.md). Đây là bước **bắt buộc**,
không phải tuỳ chọn — làm SAU khi đã có cả Bước 0 (screenshot) và Bước 1 (mô tả chức năng):

1. Liệt kê toàn bộ tên file trong `screenshot/` (kể cả file lỡ còn tên raw của thiết bị).
2. Liệt kê toàn bộ tiêu đề màn hình trong `01_MO_TA_CHUC_NANG_MAN_HINH.md`.
3. Với mỗi màn hình ở bước 2: tìm ít nhất 1 file ở bước 1 khớp tên → đánh dấu **ĐỦ**/**⚠️ THIẾU**.
4. Với mỗi file ở bước 1 CHƯA khớp được ở bước 3 (đặc biệt file tên dạng raw): **mở ảnh ra xem nội
   dung thật** (đừng đoán qua tên) → khớp vào 1 màn đã có (đổi tên cho khớp), hoặc xác nhận đúng là
   **orphan** và ghi rõ lý do (thiết kế đã đổi so với bản tham chiếu, ảnh chụp nhầm, tính năng đã bỏ...).
5. Gom toàn bộ **THIẾU + ORPHAN + tên trùng lặp/mơ hồ + lỗi chính tả** thành 1 bảng cảnh báo, ghi vào
   `04_SCREENSHOT_MAPPING.md`.

> Đừng bỏ qua bước 4 (mở ảnh ra xem thật) — file ví dụ cho thấy 3/18 màn hình tưởng như "thiếu
> screenshot hoàn toàn" thực ra ĐÃ CÓ ảnh, chỉ là ảnh còn tên raw của thiết bị nên không ai nhận ra.
> Cảnh báo "thiếu ảnh" mà không mở file raw ra kiểm tra trước là cảnh báo SAI, gây tốn công chụp lại.

### Bước 4 — Rà soát & xác nhận
- Gom toàn bộ chỗ đánh dấu **[ASSUMPTION]/[CẦN XÁC NHẬN]** từ cả 3 tài liệu, CỘNG với toàn bộ cảnh báo
  từ `04_SCREENSHOT_MAPPING.md` (màn thiếu ảnh/ảnh orphan/tên mơ hồ), vào 1 danh sách, gửi lại cho
  người giao việc xác nhận trước khi bắt đầu code (tham khảo cách vga58a làm ở
  `../../vga58a/docs/09_OPEN_ASSUMPTIONS.md`).
- Chỉ sau khi đã có đủ 4 tài liệu (kể cả khi còn vài mục `[ASSUMPTION]` chưa xác nhận xong) mới chuyển
  sang giai đoạn chọn công nghệ + code, đọc tiếp [`START_HERE.md`](../START_HERE.md) (core đã có sẵn khung, không cần chọn app mẫu để clone) và [`docs/integration/`](../docs/integration/) (tích hợp lib ads/IAP dùng chung — xem
  [`docs/integration/README.md`](../docs/integration/README.md) cho thứ tự đọc).

## 3. Checklist nhanh trước khi bắt đầu code

- [ ] Đã có đủ screenshot cho MỌI màn hình + mọi trạng thái đáng chú ý, đặt tên đúng quy tắc (không
      còn file tên raw của thiết bị).
- [ ] Mỗi màn hình trong `01_MO_TA_CHUC_NANG_MAN_HINH.md` có đủ 5 mục (Mục đích/UI/Tương tác/Trạng
      thái đặc biệt/Điều hướng vào-ra).
- [ ] `02_LUONG_DI_CHUYEN_MAN_HINH.md` có sơ đồ tổng quan + bảng chi tiết + quy tắc back-stack.
- [ ] `03_STYLE_APP.md` có đủ 8 mục, giá trị cụ thể (không ghi chung chung kiểu "màu xanh dương").
- [ ] `04_SCREENSHOT_MAPPING.md` đã đối chiếu đủ 2 chiều (màn↔ảnh), mọi file raw đã được mở ra xem và
      đặt tên/xử lý, không còn cảnh báo nào chưa gửi xác nhận.
- [ ] Toàn bộ chỗ suy đoán đã được đánh dấu rõ và gom thành danh sách cần xác nhận.
- [ ] Không có đoạn nào trong 3 tài liệu nhắc tới tên công nghệ/thư viện cụ thể (Compose, CameraX,
      Room...) — nếu có, nghĩa là đang lẫn lộn giữa "đặc tả chức năng" và "thiết kế kỹ thuật", cần
      tách ra.
