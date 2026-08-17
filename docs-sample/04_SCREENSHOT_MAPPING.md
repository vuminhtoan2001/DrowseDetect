# Video Downloader — Mapping Màn hình ↔ Screenshot

> Đối chiếu 2 chiều giữa [`screenshot/`](screenshot/) và
> [`01_MO_TA_CHUC_NANG_MAN_HINH.md`](01_MO_TA_CHUC_NANG_MAN_HINH.md).
>
> Nguồn ảnh: bản crawl `video.downloader.videodownloader` v2.6.9 (21 file `.png`, đã đổi tên theo quy
> tắc `<slug_man_hinh>[_<trang_thai>].<ext>`) + 3 file `.jpg` do người giao việc chụp bổ sung.

## 1. Bảng đối chiếu Màn hình → Screenshot

| # | Màn hình (mục trong `01_...md`) | File screenshot | Trạng thái |
|---|---|---|---|
| 1 | Trình duyệt — trang chủ (thẻ trống) | `browser_home.png` | ✅ ĐỦ |
| 1 | Trình duyệt — đã tải trang web | `browser_page_loaded.png` | ✅ ĐỦ |
| 1 | Trình duyệt — nhập địa chỉ, ô rỗng | `browser_url_focus_suggestions.png` | ✅ ĐỦ |
| 1 | Trình duyệt — đang gõ từ khoá | `browser_url_typing_suggestions.png` | ✅ ĐỦ |
| 1 | Trình duyệt — menu ⋮ | `browser_menu_popup.png` | ✅ ĐỦ |
| 1 | Trình duyệt — bảng thẻ (trên trang chủ) | `browser_tabs_panel.png` | ✅ ĐỦ |
| 1 | Trình duyệt — bảng thẻ (trên trang đã tải) | `browser_tabs_panel_over_page.png` | ✅ ĐỦ |
| 1 | Trình duyệt — hộp thoại `Link you copied` | `browser_dialog_link_copied.png` | ✅ ĐỦ |
| 1 | Trình duyệt — hộp thoại `Close browser` | `browser_dialog_close_browser.png` | ✅ ĐỦ |
| 1 | Trình duyệt — nút tải nổi khi đã tìm thấy video | — | 🎨 THIẾT KẾ MỚI (`01` §1.2) |
| 1 | Trình duyệt — bảng chọn video + chất lượng | — | 🎨 THIẾT KẾ MỚI (`01` §1.4) |
| 2 | Tải xuống — tab `PROGRESS`, rỗng | `downloads_progress_empty.png` | ✅ ĐỦ |
| 2 | Tải xuống — tab `FINISHED`, rỗng | `downloads_finished_empty.png` | ✅ ĐỦ |
| 2 | Tải xuống — tab `PROGRESS` có mục đang tải | `downloads_progress_active.jpg` | ✅ ĐỦ (ảnh bổ sung) |
| 2 | Tải xuống — tab `FINISHED` có video đã tải | — | 🎨 THIẾT KẾ MỚI (`01` §2.2, dùng lại khuôn thẻ đo được từ ảnh trên) |
| 2 | Tải xuống — menu ⋮ của 1 mục | — | 🎨 THIẾT KẾ MỚI (`01` §2.3) |
| 2 | Tải xuống — chế độ chọn nhiều | — | 🎨 THIẾT KẾ MỚI (`01` §2.2, `03` §7.11) |
| 3 | Lịch sử duyệt web | `history.png` | ✅ ĐỦ |
| 3 | Lịch sử — menu 4 mục của 1 dòng | `history_item_menu.png` | ✅ ĐỦ |
| 3 | Lịch sử — trạng thái rỗng | — | 🎨 THIẾT KẾ MỚI (dùng khuôn rỗng chung, `01` §11.3) |
| 4 | Trang web gợi ý | `recommended_sites.png` | ✅ ĐỦ |
| 5 | Video riêng tư — đặt mã PIN | `private_video_set_pin.png` | ✅ ĐỦ |
| 5 | Video riêng tư — nhập mã (đã đặt trước) | — | 🎨 THIẾT KẾ MỚI (chỉ đổi tiêu đề, `01` §5.4) |
| 5 | Video riêng tư — danh sách sau khi mở khoá | — | 🎨 THIẾT KẾ MỚI (`01` §5.2) |
| 6 | Cài đặt — phần trên | `settings.jpg` | ✅ ĐỦ (ảnh bổ sung) |
| 6 | Cài đặt — phần dưới (đã cuộn) | `settings_scrolled.jpg` | ✅ ĐỦ (ảnh bổ sung) |
| 6 | Cài đặt — hộp thoại chọn công cụ tìm kiếm | — | 🎨 THIẾT KẾ MỚI (`01` §6.3) |
| 7 | Trình phát video | — | 🎨 THIẾT KẾ MỚI (toàn bộ mục 7 của `01`) |
| 8 | Dấu trang | — | 🎨 THIẾT KẾ MỚI (toàn bộ mục 8 của `01`) |
| 9 | Cách tải về | — | 🎨 THIẾT KẾ MỚI (toàn bộ mục 9 của `01`) |
| 10 ⏸ | Nhạc chuông — danh sách thể loại | `ringtone_categories.png` | ✅ ĐỦ (hoãn) |
| 10 ⏸ | Nhạc chuông — mục mở rộng 4 hành động | `ringtone_item_expanded.png` | ✅ ĐỦ (hoãn) |
| 10 ⏸ | Nhạc chuông — màn mở khoá gói Pro | `ringtone_pro_unlock.png` | ✅ ĐỦ (hoãn) |
| 10 ⏸ | Nhạc chuông — hộp thoại lỗi thanh toán | `ringtone_dialog_purchase_unavailable.png` | ✅ ĐỦ (hoãn) |
| 10 ⏸ | Nhạc chuông — hộp thoại xin quyền | `ringtone_dialog_permission.png` | ✅ ĐỦ (hoãn) |
| 10 ⏸ | Nhạc chuông — hộp thoại `Thông tin` | `ringtone_dialog_info.png` | ✅ ĐỦ (hoãn) |
| — | Màn Giới thiệu / Xin quyền lần đầu | — | Dùng khuôn có sẵn của dự án |

> **Không còn mục ⚠️ THIẾU nào chặn việc code.** Mọi màn không có ảnh đều đã được **thiết kế mới** và
> viết đặc tả đầy đủ trong `01_...md` — xem §3 dưới để biết ranh giới giữa "chép lại app gốc" và "tự
> thiết kế".

## 2. Bảng đối chiếu Screenshot → Màn hình (chiều ngược)

Toàn bộ **24 file** trong [`screenshot/`](screenshot/) đều đã khớp vào 1 màn ở bảng §1 — **không có
file orphan**, không có file mang tên máy sinh.

### 2.1 Truy nguyên tên file (ảnh mới ← state gốc)

| File hiện tại | Nguồn | Activity gốc |
|---|---|---|
| `browser_home.png` | screen-002 | `MainTabsActivity` |
| `browser_page_loaded.png` | screen-001 | `MainTabsActivity` |
| `browser_url_focus_suggestions.png` | screen-003 | `MainTabsActivity` |
| `browser_url_typing_suggestions.png` | screen-004 | `MainTabsActivity` |
| `browser_menu_popup.png` | screen-008 | `MainTabsActivity` |
| `browser_tabs_panel.png` | screen-024 | `MainTabsActivity` |
| `browser_tabs_panel_over_page.png` | screen-025 | `MainTabsActivity` |
| `browser_dialog_link_copied.png` | screen-031 | `MainTabsActivity` |
| `browser_dialog_close_browser.png` | screen-033 | `MainTabsActivity` |
| `downloads_progress_empty.png` | screen-026 | `FilesActivity` |
| `downloads_finished_empty.png` | screen-027 | `FilesActivity` |
| `downloads_progress_active.jpg` | **chụp bổ sung** | `FilesActivity` |
| `settings.jpg` | **chụp bổ sung** | (màn Cài đặt — crawler không vào được) |
| `settings_scrolled.jpg` | **chụp bổ sung** | (như trên) |
| `history.png` | screen-021 | `MyHistoryActivity` |
| `history_item_menu.png` | screen-022 | `MyHistoryActivity` |
| `private_video_set_pin.png` | screen-028 | `PrivateVideoActivity` |
| `recommended_sites.png` | screen-034 | `VideoSiteActivity` |
| `ringtone_categories.png` | screen-014 | `RingtoneMainActivity` |
| `ringtone_item_expanded.png` | screen-017 | `RingtoneMainActivity` |
| `ringtone_pro_unlock.png` | screen-015 | `RingtoneMainActivity` |
| `ringtone_dialog_purchase_unavailable.png` | screen-016 | `RingtoneMainActivity` |
| `ringtone_dialog_permission.png` | screen-019 | `RingtoneMainActivity` |
| `ringtone_dialog_info.png` | screen-020 | `RingtoneMainActivity` |

### 2.2 Lưu ý về 3 file `.jpg`

3 ảnh bổ sung là **JPEG 576×1280** (không phải PNG 720×1600 như bộ crawl) → **1dp = 1.5px**, và màu bị
nén lệch vài đơn vị. Giá trị màu đo từ 3 file này đã được **quy về bảng màu chuẩn** ở
[`03_STYLE_APP.md`](03_STYLE_APP.md) §2.5–2.7 (có cột "Đo được" và cột "Chốt dùng" — luôn dùng cột
"Chốt dùng"). Nếu chụp lại được bản PNG không nén thì đo lại và cập nhật §2.5.

### 2.3 State gốc KHÔNG copy (trùng lặp — đã mở từng ảnh ra xem, không đoán qua tên)

13/34 state gốc là **cùng một trạng thái, chỉ khác nội dung trang web đang mở**:

| State gốc | Trùng với | Khác biệt duy nhất |
|---|---|---|
| screen-005, 009, 010, 011, 012, 013 | `browser_page_loaded.png` | Đổi URL (Google / Instagram / Vimeo / Dailymotion / TikTok / X) |
| screen-006, 007, 029 | `browser_url_typing_suggestions.png` | Gợi ý là URL thay vì từ khoá |
| screen-030, 032 | `browser_tabs_panel*.png` | Đổi trang phía sau bảng |
| screen-023 | `history.png` | Chụp lại sau khi bấm `Copy link`, giao diện y hệt |
| screen-018 | `ringtone_item_expanded.png` | Giao diện y hệt screen-017 |

Bản gốc đầy đủ 34 ảnh vẫn giữ ở
[`../video.downloader.videodownloader/screenshots/`](../video.downloader.videodownloader/screenshots/).

## 3. Ranh giới: chép lại app gốc vs. tự thiết kế

Khi dựng UI, cần biết chỗ nào phải **bám sát ảnh** và chỗ nào **được tự do** — dùng sai loại sẽ tốn
công vô ích (cố bám sát một thứ không có ảnh) hoặc lệch app gốc (tự chế ở chỗ có ảnh).

| Loại | Cách làm | Áp dụng cho |
|---|---|---|
| **Có ảnh** | Bám sát tuyệt đối. Dùng vòng lặp render-so-ảnh ở [`UI_FIDELITY_WORKFLOW.md`](UI_FIDELITY_WORKFLOW.md) cho tới khi khớp | 24 mục ✅ ở §1 |
| 🎨 **Thiết kế mới** | Không có ảnh để so. Bám **Style Guide** ([`03_STYLE_APP.md`](03_STYLE_APP.md)) và **tái dùng khuôn có sẵn** (§7.9 thẻ mục tải, §7.10 dòng Cài đặt, §7.11 chọn nhiều, §11.3 trạng thái rỗng) thay vì tự chế thành phần mới | 12 mục 🎨 ở §1 |

**Nguyên tắc cho phần thiết kế mới:** mọi màn mới phải dựng từ các khuôn đã có trong Style Guide. Nếu
buộc phải thêm một thành phần chưa từng có (vd thanh tua của trình phát video), **thêm nó vào
`03_STYLE_APP.md` §7 trước**, rồi mới code — để màn sau còn dùng lại được.

## 4. Các quyết định đã chốt

Toàn bộ 18 mục `[ASSUMPTION]` / `[CẦN XÁC NHẬN]` của bản trước đã được người giao việc quyết. Không
còn mục nào treo.

### 4.1 Quyết định về phạm vi và thiết kế

| # | Vấn đề | Quyết định |
|---|---|---|
| A1 | Ngôn ngữ hiển thị | **Giữ nguyên kiểu lẫn Anh–Việt như app gốc** ở bản đầu cho đơn giản; chuỗi vẫn tách ra file tài nguyên để dịch lại sau |
| A2 | Chế độ tối | **Không làm** |
| A3 | Nhúng font riêng | **Không nhúng** — dùng font hệ thống |
| A4 | Điểm chèn quảng cáo | **Bản đầu không làm cái nào**. Bỏ: huy hiệu `AD`, `Chia sẻ với bạn bè`, `Scan (AD)`, `Ứng dụng Gia đình (AD)`. **Giữ chỗ trong bố cục** (không code) cho: dải `XÓA QUẢNG CÁO` ở Cài đặt, thẻ quảng cáo trong danh sách tải |
| A5 | 8 ô lối tắt trang chủ | Cố định, **còn 7 ô** (bỏ `Nhạc chuông`) |
| A6 | Danh sách `Trang web gợi ý` | Cố định trong app, không cấu hình từ xa |
| A7 | Khôi phục mã PIN | **Không có luồng khôi phục** — PIN chỉ là lớp che mắt để vào khu Video riêng tư, không phải bảo mật thật |
| A8 | Giới hạn số lần nhập sai PIN | **Không giới hạn**, chỉ rung + báo lỗi |
| A9 | Module Nhạc chuông | **Hoãn** — giữ ảnh + tóm tắt trong `01` §10, bản đầu không có lối vào |

### 4.2 Quyết định về hành vi

| # | Vấn đề | Quyết định | Ghi ở |
|---|---|---|---|
| B1 | Back ở tab `Progress`/`Finished` | **Về tab `Thẻ`**, không thoát app | `02` §3.1 |
| B2 | Back khi thẻ hết lịch sử web | Về trang chủ của thẻ; đang ở trang chủ rồi thì mới thoát app | `02` §3.1 |
| B3 | Back sau khi mở khoá PIN | Về thẳng màn Tải xuống, **không** quay lại màn nhập mã | `02` §3.4 |
| B4 | Bấm 1 ô ở `Trang web gợi ý` | Mở trong **thẻ hiện tại** — nhất quán với 7 ô lối tắt ở trang chủ | `01` §4.3 |
| B5 | Đóng thẻ cuối cùng | **Tạo ngay thẻ trống mới**, không thoát app | `02` §3.3 |
| B6 | Bấm thùng rác ở màn Lịch sử | **Có hộp thoại xác nhận** | `01` §3.3 |
| B7 | Dấu hỏi ở trang chủ vs `Cách tải về` | **Cùng một màn đích** (mục 9), 3 lối vào | `02` §3.6 |
| B8 | Biểu tượng **ba gạch ngang** | **Bật chế độ chọn nhiều**: nút ⋮/✕ của mỗi mục đổi thành ô đánh dấu, chọn nhiều rồi **Xoá** hoặc **Chuyển vào riêng tư** | `01` §2.2, `03` §7.11 |
| B9 | Bấm 1 mục **đang tải** | Không làm gì — mọi hành động nằm ở các biểu tượng trên thẻ (✕ huỷ, 🌐 mở trang nguồn, ⬇/⏸ tiếp tục/tạm dừng) | `01` §2.3 |
| B10 | Hộp thoại `Link you copied` bật khi nào | Khi vào app **và** đường dẫn trong bộ nhớ tạm khác lần cuối đã hỏi — mỗi đường dẫn chỉ hỏi 1 lần | `01` §1.4 |

### 4.3 Điểm phát hiện thêm từ 3 ảnh bổ sung

| Phát hiện | Xử lý |
|---|---|
| Mục đáy `Finished` có **huy hiệu tròn đỏ** đếm số (ảnh ghi `5`) | Đưa vào đặc tả: đếm video vừa tải xong chưa xem, mở tab đó là xoá về 0 (`01` §2.2, §2.4) |
| Biểu tượng **ba gạch ngang** cũng có ở tab `PROGRESS` (ảnh rỗng trước đó không có) | Chốt: chỉ hiện khi danh sách có ≥1 mục (`01` §2.2) |
| Tiêu đề màn Cài đặt là `Settings` **chữ thường**, khác `PROGRESS`/`RECOMMENDED SITES` viết hoa | Ghi rõ trong `01` §6.2 — giữ nguyên như app gốc |
| Mục tải có **2 biểu tượng hành động** (quả địa cầu + tải xuống) chứ không chỉ 1 | Đưa vào khuôn thẻ mục tải (`03` §7.9) |
| Trạng thái mục tải hiển thị bằng **chữ tiếng Anh thường** (`paused`) | Giữ nguyên theo quyết định A1 |

## 5. ⚠️ Cảnh báo về tài sản thương hiệu

Ảnh trong thư mục này chứa **biểu trưng và tên thương hiệu của bên thứ ba** (Facebook, Instagram,
TikTok, X, Vimeo, Dailymotion, WhatsApp, Tubidy, Google) và **ảnh bìa nhạc chuông có bản quyền**.

- Dựng lại **bố cục và luồng màn hình** là bình thường — đó là mục đích của bộ tài liệu này.
- **Không** sao chép biểu trưng, ảnh bìa, tên app, biểu tượng app của app gốc vào gói cài của app mới.
- 7 ô lối tắt và 5 ô `Trang web gợi ý` cần **tự vẽ lại biểu tượng** hoặc tải favicon tại thời điểm
  chạy, không nhúng sẵn logo cắt từ ảnh.
- Ảnh trong `screenshot/` **chưa che dữ liệu cá nhân** (lịch sử duyệt web thật, đường dẫn thật, tên
  tệp thật) — rà lại trước khi chia sẻ ra ngoài nhóm phát triển.
- Thẻ quảng cáo trong `downloads_progress_active.jpg` quảng bá một app cạnh tranh — đây chỉ là ảnh
  tham chiếu, không phải nội dung cần dựng lại.

## 6. Trạng thái checklist trước khi code

| Mục | Trạng thái |
|---|---|
| Screenshot đủ mọi màn + mọi trạng thái đáng chú ý | ✅ ĐỦ — 24 file; 12 mục còn lại chuyển sang 🎨 thiết kế mới có đặc tả đầy đủ |
| Tên file đúng quy tắc, không còn tên máy sinh | ✅ ĐỦ — 24/24 file |
| Mỗi màn trong `01_...md` có đủ 5 mục | ✅ ĐỦ — 9/9 màn đang làm (mục 10 Nhạc chuông hoãn, ghi rõ) |
| `02_...md` có sơ đồ + bảng chi tiết + back-stack | ✅ ĐỦ — thêm quy tắc Back 6 bước đã chốt |
| `03_...md` có đủ 8 mục, giá trị cụ thể | ✅ ĐỦ — màu đo bằng công cụ trên cả 24 ảnh |
| `04_...md` đối chiếu đủ 2 chiều | ✅ ĐỦ |
| Mọi chỗ suy đoán đã được quyết | ✅ ĐỦ — 18/18 mục đã chốt ở §4, không còn mục treo |
| Không tài liệu nào nhắc tên công nghệ/thư viện | ✅ ĐỦ |

**→ Đã đủ điều kiện chuyển sang giai đoạn dựng app.** Bước kế tiếp theo
[`README.md`](README.md) mục B: [`integration/`](integration/) → [`QUY_CHUAN_DESIGN.md`](QUY_CHUAN_DESIGN.md)
→ [`UI_FIDELITY_WORKFLOW.md`](UI_FIDELITY_WORKFLOW.md).
