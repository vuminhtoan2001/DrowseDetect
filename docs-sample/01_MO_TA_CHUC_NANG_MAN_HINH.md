# Video Downloader — Mô tả chức năng chi tiết từng màn hình

> App cho phép người dùng duyệt web bằng trình duyệt tích hợp sẵn, phát hiện video trên trang đang xem
> và tải về máy; các video đã tải được quản lý ở khu vực riêng, có thể khoá bằng mã PIN.
>
> **Nguồn viết tài liệu:**
> 1. Ảnh chụp thật của app tham chiếu `video.downloader.videodownloader` v2.6.9 (bản crawl trên
>    samsung SM-A055F, Android 15, ngôn ngữ hệ thống: Tiếng Việt).
> 2. Ảnh bổ sung do người giao việc cung cấp: màn Cài đặt (2 phần) và tab `PROGRESS` khi có dữ liệu.
> 3. **[THIẾT KẾ MỚI]** — các màn app gốc có nhưng không có ảnh: tự thiết kế theo đúng Style Guide ở
>    [`03_STYLE_APP.md`](03_STYLE_APP.md), không phải mô tả app gốc. Đánh dấu rõ ở từng chỗ.
>
> **[ASSUMPTION]** = suy đoán về hành vi app gốc, chưa kiểm chứng.
> **[ĐÃ CHỐT]** = quyết định của người giao việc, không phải mô tả app gốc.
>
> **Ngôn ngữ hiển thị — [ĐÃ CHỐT]:** giữ nguyên kiểu lẫn Anh–Việt như app gốc ở bản đầu cho đơn giản
> (`Thẻ`/`Progress`/`Finished`/`History`/`Settings`/`No history`...), chưa làm bộ dịch đồng nhất.
> Chuỗi hiển thị vẫn tách ra file tài nguyên để dịch lại sau mà không phải sửa màn hình.

## Mục lục

- [1. TRÌNH DUYỆT](#1-trình-duyệt)
- [2. QUẢN LÝ TẢI XUỐNG](#2-quản-lý-tải-xuống)
- [3. LỊCH SỬ DUYỆT WEB](#3-lịch-sử-duyệt-web)
- [4. TRANG WEB GỢI Ý](#4-trang-web-gợi-ý)
- [5. VIDEO RIÊNG TƯ](#5-video-riêng-tư)
- [6. CÀI ĐẶT](#6-cài-đặt)
- [7. TRÌNH PHÁT VIDEO](#7-trình-phát-video) — [THIẾT KẾ MỚI]
- [8. DẤU TRANG](#8-dấu-trang) — [THIẾT KẾ MỚI]
- [9. CÁCH TẢI VỀ](#9-cách-tải-về) — [THIẾT KẾ MỚI]
- [10. NHẠC CHUÔNG](#10-nhạc-chuông) — ⏸ HOÃN (làm sau)
- [11. Ghi chú chung (áp dụng nhiều màn)](#11-ghi-chú-chung-áp-dụng-nhiều-màn)

---

## 1. TRÌNH DUYỆT

> Ảnh: [`browser_home.png`](screenshot/browser_home.png),
> [`browser_page_loaded.png`](screenshot/browser_page_loaded.png),
> [`browser_url_focus_suggestions.png`](screenshot/browser_url_focus_suggestions.png),
> [`browser_url_typing_suggestions.png`](screenshot/browser_url_typing_suggestions.png),
> [`browser_menu_popup.png`](screenshot/browser_menu_popup.png),
> [`browser_tabs_panel.png`](screenshot/browser_tabs_panel.png),
> [`browser_tabs_panel_over_page.png`](screenshot/browser_tabs_panel_over_page.png),
> [`browser_dialog_link_copied.png`](screenshot/browser_dialog_link_copied.png),
> [`browser_dialog_close_browser.png`](screenshot/browser_dialog_close_browser.png)

### 1.1 Mục đích màn hình

Là màn hình chính, mở ra ngay khi vào app. Người dùng vào các trang web có video (mạng xã hội, trang
chia sẻ video) bằng trình duyệt tích hợp sẵn; khi app phát hiện có video trên trang, một nút tải nổi
sẽ đổi trạng thái để người dùng tải video đó về máy.

Đây là **một màn hình duy nhất có nhiều trạng thái**, không phải nhiều màn tách rời: trang chủ (tab
trống), trang web đã tải, đang nhập địa chỉ, danh sách thẻ, menu tuỳ chọn, các hộp thoại — tất cả đều
thuộc màn này.

### 1.2 Các thành phần UI và nội dung hiển thị

**Thanh công cụ trên cùng** (luôn hiện, cao 56dp, nền trắng, sát dưới thanh trạng thái):

| Vị trí | Thành phần | Ghi chú |
|---|---|---|
| Trái | Ô vuông viền đen chứa **số thẻ đang mở** (vd `1`) | Bấm mở danh sách thẻ |
| Trái (chỉ khi đã tải trang) | Biểu tượng **nhà** | Bấm quay về trang chủ của thẻ |
| Giữa | **Ô địa chỉ** bo tròn, nền `#EEF1FB` | Trang chủ hiện chữ mờ `Search or Type URL` + biểu tượng mũi tên xuống-dòng ở cuối ô; khi đã tải trang thì hiện URL thật + biểu tượng **tải lại** |
| Phải (chỉ ở trang chủ) | **Dấu hỏi trong hình tròn** (trợ giúp) · **bánh răng** (cài đặt) | **[ĐÃ CHỐT]** bỏ huy hiệu `AD` gỡ quảng cáo của app gốc — bản đầu chưa làm điểm chèn quảng cáo |
| Phải (chỉ khi đã tải trang) | Biểu tượng **bong bóng chat có dấu chấm than** (gửi phản hồi) | Thay chỗ 2 biểu tượng trên |
| Ngoài cùng phải | Biểu tượng **ba chấm dọc** | Bấm mở menu tuỳ chọn |

Ngay dưới thanh công cụ có **thanh tiến trình tải trang** mảnh (2dp), chỉ hiện khi trang đang tải.

**Trạng thái A — Trang chủ (thẻ trống)** — ảnh `browser_home.png`:
- **Ô tìm kiếm lớn** bo tròn (cao 52dp, lề ngang 24dp, nền `#EEF1FB`), chữ mờ `Search or Type URL`,
  biểu tượng mũi tên xuống-dòng bên phải.
- **Lưới lối tắt 4 cột × 2 hàng**, mỗi ô là biểu tượng tròn lớn + nhãn bên dưới:
  `Facebook`, `Instagram`, `Vimeo`, `Dailymotion`, `Tiktok`, `Twitter`, `WhatsApp` — **7 ô**.
  **[ĐÃ CHỐT]** ô `Nhạc chuông` của app gốc **bỏ ở bản đầu** (module đang hoãn); lưới lấp từ trái sang,
  ô thứ 8 để trống.
- **Dòng liên kết phản hồi**: biểu tượng bong bóng chat + chữ gạch chân
  `Bạn có phản hồi hoặc câu hỏi? Hãy cho chúng tôi biết`.
- Phần còn lại của màn để trống.

**Trạng thái B — Đã tải trang web** — ảnh `browser_page_loaded.png`:
- Toàn bộ vùng giữa màn là **nội dung trang web thật**, cuộn được.
- **Nút tải nổi hình tròn** ở góc dưới-phải (vùng chạm 123dp, biểu tượng 55dp), 3 trạng thái:

| Trạng thái | Hình thức |
|---|---|
| Chưa tìm thấy video | Nền xám `#A3A5A9` bán trong suốt + mũi tên tải trắng (đo được từ ảnh gốc) |
| **Đã tìm thấy video** | **[THIẾT KẾ MỚI]** Nền `#067EFF` đặc + mũi tên trắng + **huy hiệu tròn đỏ** ở góc trên-phải hiện **số video tìm được**; có hiệu ứng nảy nhẹ 1 lần khi vừa chuyển sang trạng thái này |
| Đang tải | **[THIẾT KẾ MỚI]** Vòng tiến trình xanh chạy quanh viền nút, giữa là **%** |

**Trạng thái C — Đang nhập địa chỉ / tìm kiếm** — ảnh `browser_url_focus_suggestions.png`,
`browser_url_typing_suggestions.png`:
- Thanh công cụ thu lại chỉ còn **ô nhập** bo tròn chiếm gần hết chiều ngang (rộng 327dp, cao 48.5dp)
  + biểu tượng mũi tên xuống-dòng bên phải; con trỏ nhấp nháy, bàn phím hệ thống bật lên.
- **Danh sách gợi ý** bên dưới, mỗi dòng cao 64dp gồm biểu tượng bên trái + 2 dòng chữ:
  - Ô nhập **rỗng** → 5 trang truy cập gần đây; biểu tượng **đồng hồ**, hoặc favicon thật nếu có.
  - **Đang gõ chữ** → 5 gợi ý tìm kiếm; biểu tượng **kính lúp**, dòng dưới `Search for "<từ khoá>"`.
  - **Đang gõ địa chỉ** → các URL đầy đủ khớp tiền tố.
- **Thanh phím tắt nhập nhanh** ngay trên bàn phím, nội dung đổi theo ngữ cảnh:
  ô rỗng → `www.` `m.` `wap.` `.` `/` ; đã có nội dung → `.` `/` `.com` `.net` `.org`.

**Trạng thái D — Menu tuỳ chọn** — ảnh `browser_menu_popup.png`:
Bảng nổi trắng bo 12dp, thả từ biểu tượng ba chấm, phủ lên nội dung trang. Mỗi mục cao 48dp gồm biểu
tượng + nhãn. **8 mục** (app gốc có 11):

`New tab` · `Chia sẻ` · `History` · `Copy link` · `Bookmarks` · `Add bookmark` · `Desktop site`
(có **ô đánh dấu** bên phải) · `Settings`.

> **[ĐÃ CHỐT]** bỏ 3 mục của app gốc: `Chia sẻ với bạn bè`, `Scan (AD)`, `Ứng dụng Gia đình (AD)` —
> đều là điểm chèn quảng cáo chéo, bản đầu chưa làm.

**Trạng thái E — Danh sách thẻ (Tabs)** — ảnh `browser_tabs_panel.png`,
`browser_tabs_panel_over_page.png`:
Bảng trượt từ **trái** sang, rộng 300dp (~78% màn), phần còn lại phủ `#666666` và vẫn nhìn xuyên thấy.
Cấu trúc từ trên xuống:
- **Tiêu đề** `Tabs` (nền `#EEF1FB`) + biểu tượng **ba chấm dọc** bên phải.
- **Danh sách thẻ đang mở**, mỗi dòng cao ~110dp: favicon + tiêu đề trang + **dấu X** đóng thẻ. Thẻ
  **đang xem** có nền `#EEF1FB` và chữ đậm; thẻ khác nền trắng. Thẻ trống hiện tên `Homepage` với
  biểu tượng quả địa cầu.
- **Dòng `Favorite sites`** (biểu tượng quà xanh), ngăn cách bằng đường kẻ mảnh phía trên.
- **Thanh 4 nút điều hướng** dưới cùng (nền `#EEF1FB`): **lùi** ← · **nhà** ⌂ · **tiến** → ·
  **thêm thẻ** +.

**Thanh điều hướng đáy** (luôn hiện, 3 mục chia đều, cao 56dp): `Thẻ` · `Progress` · `Finished`. Mục
đang chọn: biểu tượng + chữ `#067EFF`, chữ to hơn 2sp; mục không chọn `#7D8EAF`. Mục `Finished` có
**huy hiệu tròn đỏ** hiện **số video đã tải xong mà người dùng chưa mở** (thấy trong ảnh
`downloads_progress_active.png`, huy hiệu ghi `5`). Ở màn này mục `Thẻ` luôn được chọn.

### 1.3 Tương tác người dùng

| Thao tác | Kết quả |
|---|---|
| Bấm ô tìm kiếm / ô địa chỉ | Chuyển sang **trạng thái C** |
| Gõ chữ trong ô nhập | Danh sách gợi ý cập nhật theo từng ký tự |
| Bấm 1 dòng gợi ý | Tải trang/tìm kiếm tương ứng → **trạng thái B** |
| Bấm 1 nút trên thanh phím tắt | Chèn chuỗi đó vào ô nhập tại vị trí con trỏ, KHÔNG tải trang |
| Xác nhận trên bàn phím | Tải trang/tìm kiếm với nội dung đang nhập → **trạng thái B** |
| Bấm 1 ô lối tắt | Tải trang chủ của dịch vụ đó trong **thẻ hiện tại** → **trạng thái B** |
| Bấm dòng phản hồi | Mở luồng gửi phản hồi (thư/biểu mẫu ngoài app) |
| Bấm biểu tượng **nhà** | Thẻ hiện tại quay về trang chủ → **trạng thái A** |
| Bấm biểu tượng **tải lại** | Tải lại trang hiện tại; khi đang tải thì biểu tượng đổi thành **X** để dừng |
| Bấm biểu tượng **dấu hỏi** | Mở màn **Cách tải về** (mục 9) — **[ĐÃ CHỐT]** cùng một đích với nút `Cách tải về` ở màn Tải xuống |
| Bấm biểu tượng **bánh răng** | Mở màn **Cài đặt** (mục 6) |
| Bấm biểu tượng **bong bóng chat** | Mở luồng báo lỗi trang |
| Bấm **ba chấm dọc** | Mở **trạng thái D** |
| Bấm ô vuông số thẻ | Mở **trạng thái E** |
| Bấm **nút tải nổi** khi trang CÓ video | Mở **bảng chọn video** (xem 1.4) |
| Bấm **nút tải nổi** khi trang KHÔNG có video | Hiện thông báo ngắn `Không tìm thấy video trên trang này` **[THIẾT KẾ MỚI]** |
| Bấm mục đáy `Progress` / `Finished` | Chuyển sang màn **Quản lý tải xuống** (mục 2), tab tương ứng |

**Trong menu tuỳ chọn (trạng thái D):**

| Mục | Kết quả |
|---|---|
| `New tab` | Tạo thẻ mới, mở trang chủ → **trạng thái A**, số thẻ +1 |
| `Chia sẻ` | Mở bảng chia sẻ của hệ thống với URL trang hiện tại |
| `History` | Mở màn **Lịch sử duyệt web** (mục 3) |
| `Copy link` | Chép URL vào bộ nhớ tạm + thông báo ngắn xác nhận |
| `Bookmarks` | Mở màn **Dấu trang** (mục 8) |
| `Add bookmark` | Lưu trang hiện tại vào dấu trang + thông báo ngắn xác nhận; nếu đã có thì đổi thành `Đã lưu` và bấm lại là bỏ lưu **[THIẾT KẾ MỚI]** |
| `Desktop site` | Bật/tắt chế độ xem bản máy tính; **ô đánh dấu đổi tại chỗ**, trang tải lại, menu tự đóng |
| `Settings` | Mở màn **Cài đặt** (mục 6) |

**Trong danh sách thẻ (trạng thái E):**

| Thao tác | Kết quả |
|---|---|
| Bấm 1 thẻ | Chuyển sang thẻ đó, đóng bảng |
| Bấm **X** trên 1 thẻ | Đóng thẻ đó |
| Bấm **X** trên thẻ **cuối cùng** | **[ĐÃ CHỐT]** KHÔNG thoát app: đóng thẻ đó rồi **tạo ngay 1 thẻ trống mới** ở trang chủ. Lý do: thanh điều hướng đáy vẫn cần nội dung cho mục `Thẻ` — để trắng sẽ thành màn cụt |
| Bấm **ba chấm** ở tiêu đề `Tabs` | Mở hộp thoại `Close browser` (xem 1.4) |
| Bấm `Favorite sites` | Mở màn **Trang web gợi ý** (mục 4) |
| Bấm **←** / **→** | Lùi/tiến trong lịch sử của thẻ đang xem; nút mờ đi khi không lùi/tiến được **[THIẾT KẾ MỚI]** |
| Bấm **⌂** | Thẻ đang xem về trang chủ |
| Bấm **+** | Tạo thẻ mới |
| Chạm vùng tối / vuốt sang trái / Back | Đóng bảng, giữ nguyên thẻ đang xem |

### 1.4 Trạng thái/case đặc biệt

**Bảng chọn video** — **[THIẾT KẾ MỚI]**, mở từ nút tải nổi khi trang có video:
Bảng trượt lên từ đáy màn, bo góc trên 12dp, nền trắng:
- Thanh nắm nhỏ màu xám ở giữa mép trên.
- Tiêu đề `Video tìm thấy` (20sp Bold) + số lượng.
- **Danh sách video**, mỗi dòng cao 68dp: ảnh thu nhỏ 56×56dp bo 8dp bên trái · tên video (2 dòng,
  cắt bằng `…`) · dòng phụ ghi `<độ phân giải> · <dung lượng ước tính> · <định dạng>` · nút **Tải**
  bo tròn hết cạnh, nền `#067EFF`, chữ trắng 14sp ở bên phải.
- Nếu 1 video có nhiều chất lượng: dòng đó mở rộng được, lộ danh sách các mức
  (`1080p` / `720p` / `480p` / `Chỉ âm thanh`), mỗi mức 1 nút tải riêng.
- Bấm `Tải` → đóng bảng, hiện thông báo ngắn `Đã thêm vào danh sách tải`, huy hiệu trên mục đáy
  `Progress` tăng lên.

**Hộp thoại `Link you copied`** — ảnh `browser_dialog_link_copied.png`:
Hiện **khi mở app / khi app quay lại tiền cảnh** nếu bộ nhớ tạm đang chứa một đường dẫn web **và
đường dẫn đó khác lần cuối đã hỏi** (**[ĐÃ CHỐT]** — mỗi đường dẫn chỉ hỏi 1 lần, tránh làm phiền).
Thẻ trắng bo 12dp ở giữa màn, nền sau phủ tối:
- Tiêu đề đậm `Link you copied` (24sp Bold, căn giữa).
- Ô nền `#EEF1FB` chứa **đường dẫn** dạng liên kết xanh gạch chân.
- Nút chính bo tròn hết cạnh, nền `#067EFF`, chữ trắng `Mở`.
- Bên dưới là chữ xám `Hủy bỏ`.

Bấm `Mở` → tải đường dẫn đó trong thẻ hiện tại. Bấm `Hủy bỏ` → đóng, không làm gì.

**Hộp thoại `Close browser`** — ảnh `browser_dialog_close_browser.png`:
Mở từ nút ba chấm trong bảng danh sách thẻ. Thẻ trắng bo góc, tiêu đề `Close browser` + 2 lựa chọn
dạng chữ: `Close current tab` · `Close other tabs`. Không có nút Huỷ — chạm ra ngoài để đóng.
Nếu `Close current tab` làm hết thẻ → áp dụng quy tắc "tạo lại thẻ trống" ở trên.

**Các case khác:**

| Case | Hành vi |
|---|---|
| Không có mạng | Trang hiện lỗi của trình duyệt; lịch sử vẫn ghi nhận với tiêu đề `Trang web hiện không khả dụng` |
| Trang đang tải | Thanh tiến trình mảnh chạy; biểu tượng tải lại đổi thành **X** dừng tải |
| Chặn quảng cáo đang bật | Không hiện gì thêm trên giao diện; xem mục 6 (`Block ads`) |
| Chỉ tải qua Wi-Fi, đang dùng dữ liệu di động | Bấm tải → hiện thông báo ngắn `Chỉ tải qua Wi-Fi đang bật — bật lại trong Cài đặt` **[THIẾT KẾ MỚI]**, không thêm vào danh sách tải |
| Bấm Back khi đang xem trang | Lùi 1 bước trong lịch sử của thẻ; hết lịch sử thì về trang chủ của thẻ; đang ở trang chủ thì thoát app |
| Bấm Back khi đang mở bảng thẻ/menu/hộp thoại | Đóng lớp phủ đó, KHÔNG thoát app |

### 1.5 Điều hướng vào/ra màn hình này

**Vào từ:** Mở app (màn mặc định, sau luồng khởi động dùng chung) · mục đáy `Thẻ` từ màn Tải xuống ·
nút Back từ các màn Lịch sử / Trang web gợi ý / Cài đặt / Dấu trang / Cách tải về.

**Ra tới:** Quản lý tải xuống · Lịch sử duyệt web · Trang web gợi ý · Cài đặt · Dấu trang · Cách tải
về · bảng chia sẻ của hệ thống.

---

## 2. QUẢN LÝ TẢI XUỐNG

> Ảnh: [`downloads_progress_empty.png`](screenshot/downloads_progress_empty.png),
> [`downloads_finished_empty.png`](screenshot/downloads_finished_empty.png),
> `downloads_progress_active.png` (ảnh bổ sung do người giao việc cung cấp)

### 2.1 Mục đích màn hình

Cho người dùng theo dõi các video **đang tải** và mở/quản lý các video **đã tải xong**.

Là **một màn hình có 2 tab**, chuyển qua lại bằng chính thanh điều hướng đáy (`Progress` và
`Finished`) hoặc vuốt ngang giữa 2 tab.

### 2.2 Các thành phần UI và nội dung hiển thị

**Thanh công cụ** — tiêu đề chữ hoa căn trái (`PROGRESS` hoặc `FINISHED`), 20sp Bold; bên phải là các
biểu tượng, khác nhau theo tab:

| Tab | Biểu tượng bên phải (trái → phải) |
|---|---|
| `PROGRESS` | Bong bóng chat có chấm than (phản hồi) · **Ba gạch ngang** (bật chế độ chọn nhiều) |
| `FINISHED` | Bong bóng chat có chấm than · **Thư mục có ổ khoá** (video riêng tư) · **Ba gạch ngang** (bật chế độ chọn nhiều) |

> Ảnh trạng thái rỗng của app gốc chỉ có biểu tượng phản hồi ở tab `PROGRESS`; ảnh có dữ liệu cho thấy
> **ba gạch ngang cũng xuất hiện ở tab này** khi danh sách không rỗng. **[ĐÃ CHỐT]** app mới: biểu
> tượng ba gạch **chỉ hiện khi danh sách có ít nhất 1 mục**.

**Vùng nội dung — khi rỗng:** minh hoạ **hộp giấy mở nắp màu cam** (106×106dp) căn giữa + chữ
`No history` (16sp `#989DAF`) bên dưới.

**Vùng nội dung — tab `PROGRESS` khi có dữ liệu** (theo ảnh bổ sung):
Mỗi mục là một **thẻ** nền `#EEF1FB` (hoặc `#F5F6FA`), bo 12dp, lề ngang 16dp, cách nhau 8dp:

| Vùng | Nội dung |
|---|---|
| Trái | **Ảnh thu nhỏ** vuông ~88dp bo 8dp. Chưa lấy được ảnh thật thì hiện nền xanh nhạt + biểu tượng **bảng phim** trắng ở giữa |
| Phải, dòng 1 | **Tên tệp** đậm, cắt bằng `…`; ngoài cùng phải là **dấu ✕** để huỷ mục này |
| Phải, dòng 2 | **Thanh tiến trình** mảnh chạy hết chiều ngang phần phải (rãnh xám nhạt, phần đã tải màu xanh) |
| Phải, dòng 3 | Bên trái: `<đã tải>/<tổng>` (vd `0 B/1,72 MB`) rồi xuống dòng là **trạng thái** (`paused`, `downloading`, `error`). Bên phải: 2 biểu tượng xanh — **quả địa cầu** (mở lại trang nguồn) và **mũi tên tải xuống / tạm dừng** (tiếp tục hoặc tạm dừng) |

**Vùng nội dung — tab `FINISHED` khi có dữ liệu** — **[THIẾT KẾ MỚI]**, dùng lại đúng khuôn thẻ trên:

| Vùng | Nội dung |
|---|---|
| Trái | Ảnh thu nhỏ thật của video (khung hình đầu), bo 8dp, có **nhãn thời lượng** nền đen mờ ở góc dưới-phải |
| Phải, dòng 1 | Tên tệp đậm + **ba chấm dọc** ngoài cùng phải |
| Phải, dòng 2 | `<dung lượng> · <độ phân giải> · <ngày tải>` |

**Ô quảng cáo** (theo ảnh bổ sung): app gốc chèn 1 thẻ quảng cáo ngay dưới mục tải đầu tiên (biểu
tượng app + tiêu đề + nhãn `AD` + mô tả + nút viền `Cài đặt ngay` + chữ `Quảng cáo` ở góc).
**[ĐÃ CHỐT]** bản đầu **không làm** — nhưng **giữ chỗ trong bố cục** (vị trí sau mục thứ nhất) để bật
sau mà không phải sửa lại giao diện.

**Nút `Cách tải về`** — dải bo tròn hết cạnh, nền `#EEF1FB`, lề ngang 16dp, cao 44dp, sát trên thanh
điều hướng đáy: biểu tượng dấu hỏi tròn xanh + chữ `Cách tải về`. **[THIẾT KẾ MỚI]** chỉ hiện khi
danh sách **rỗng** — khi có dữ liệu thì ẩn đi để không che mục cuối.

**Thanh điều hướng đáy** — giống mục 1; mục `Finished` có huy hiệu đỏ đếm số video đã tải xong chưa xem.

**Chế độ chọn nhiều** — **[ĐÃ CHỐT]**, bật bằng biểu tượng **ba gạch ngang**:
- Thanh công cụ đổi thành: dấu **✕** (thoát chế độ) · tiêu đề `Đã chọn N` · biểu tượng **chọn tất cả**.
- Mỗi mục trong danh sách: nút **ba chấm dọc** (hoặc **✕** ở tab Progress) **đổi thành ô đánh dấu**;
  bấm bất kỳ đâu trên mục là chọn/bỏ chọn, không mở video.
- **Thanh hành động** trượt lên từ đáy (đè lên thanh điều hướng đáy), chứa 2 hành động:
  - **Xoá** (biểu tượng thùng rác + nhãn)
  - **Chuyển vào riêng tư** (biểu tượng thư mục có ổ khoá + nhãn) — chỉ có ở tab `FINISHED`
- Khi chưa chọn mục nào: 2 hành động mờ đi, không bấm được.
- Thoát chế độ bằng **✕** hoặc nút Back của máy.

### 2.3 Tương tác người dùng

| Thao tác | Kết quả |
|---|---|
| Bấm mục đáy `Progress` / `Finished` | Chuyển tab tương ứng (đổi tiêu đề, đổi bộ biểu tượng bên phải) |
| Vuốt ngang vùng nội dung | Chuyển qua lại giữa 2 tab |
| Bấm mục đáy `Thẻ` | Về màn **Trình duyệt** (mục 1) |
| Bấm `Cách tải về` | Mở màn **Cách tải về** (mục 9) |
| Bấm biểu tượng **bong bóng chat** | Mở luồng gửi phản hồi |
| Bấm biểu tượng **thư mục có ổ khoá** (tab Finished) | Mở màn **Video riêng tư** (mục 5) |
| Bấm biểu tượng **ba gạch ngang** | Bật **chế độ chọn nhiều** |
| **Tab Progress** — bấm ✕ trên 1 mục | Huỷ mục đó, xoá khỏi danh sách (có hộp thoại xác nhận **[THIẾT KẾ MỚI]**) |
| **Tab Progress** — bấm biểu tượng tải/tạm dừng | Tiếp tục hoặc tạm dừng riêng mục đó |
| **Tab Progress** — bấm biểu tượng quả địa cầu | Về màn Trình duyệt, mở lại trang nguồn của video đó |
| **Tab Progress** — bấm thân mục | Không làm gì (mọi hành động đều nằm ở các biểu tượng) |
| **Tab Finished** — bấm 1 mục | Mở **Trình phát video** (mục 7) |
| **Tab Finished** — bấm ba chấm của 1 mục | Mở menu 5 mục: `Phát` · `Chia sẻ` · `Đổi tên` · `Chuyển vào riêng tư` · `Xoá` **[THIẾT KẾ MỚI]** |
| Nhấn giữ 1 mục (cả 2 tab) | Bật **chế độ chọn nhiều** và chọn sẵn mục đó **[THIẾT KẾ MỚI]** |

### 2.4 Trạng thái/case đặc biệt

| Case | Hành vi |
|---|---|
| Chưa tải gì | Hộp giấy cam + `No history` + nút `Cách tải về` |
| Đang tải | Thanh tiến trình chạy, trạng thái `downloading`, kèm thông báo hệ thống có % và nút tạm dừng/huỷ |
| Tạm dừng | Trạng thái `paused`, thanh tiến trình giữ nguyên, biểu tượng đổi thành mũi tên tải (tiếp tục) |
| Tải lỗi | **[THIẾT KẾ MỚI]** Trạng thái đổi thành chữ đỏ `Lỗi — chạm để thử lại`, biểu tượng đổi thành mũi tên xoay vòng |
| Mất mạng giữa chừng | Tự chuyển sang `paused`, tự tiếp tục khi có mạng lại **[THIẾT KẾ MỚI]** |
| Đang bật "chỉ tải qua Wi-Fi" và rời Wi-Fi | Tự tạm dừng, trạng thái ghi `Chờ Wi-Fi` **[THIẾT KẾ MỚI]** |
| Tệp đã bị xoá ngoài app | Mục hiện ảnh thu nhỏ hỏng; bấm vào báo `Tệp không còn tồn tại` và cho xoá mục khỏi danh sách |
| Chưa cấp quyền thông báo | Vẫn tải được, chỉ không hiện tiến trình ngoài app |
| Tải xong 1 video | Mục chuyển từ tab `Progress` sang `Finished`, huy hiệu đỏ trên mục đáy `Finished` +1 |
| Mở tab `Finished` | Huy hiệu đỏ được xoá về 0 |

### 2.5 Điều hướng vào/ra màn hình này

**Vào từ:** Màn Trình duyệt (mục đáy `Progress`/`Finished`) · chạm thông báo tiến trình tải ngoài app
(mở thẳng tab `Progress`).

**Ra tới:** Trình duyệt (mục đáy `Thẻ`, hoặc biểu tượng quả địa cầu của 1 mục) · Video riêng tư ·
Trình phát video · Cách tải về.

---

## 3. LỊCH SỬ DUYỆT WEB

> Ảnh: [`history.png`](screenshot/history.png),
> [`history_item_menu.png`](screenshot/history_item_menu.png)

### 3.1 Mục đích màn hình

Xem lại các trang đã truy cập trong trình duyệt của app, mở lại nhanh hoặc xoá đi.

### 3.2 Các thành phần UI và nội dung hiển thị

- **Thanh công cụ**: mũi tên **quay lại** · tiêu đề `History` · biểu tượng **thùng rác** bên phải.
- **Danh sách** chiếm toàn màn (nền `#FAFAFA`), mỗi dòng cao 68dp, ngăn cách bằng đường kẻ mảnh chạy
  hết chiều ngang:
  - Dòng trên: **tiêu đề trang** 16sp `#141518`, cắt bằng `…`.
  - Dòng dưới: **favicon nhỏ** (~20dp) + **URL** 12sp `#565C6E`, cắt nếu dài.
  - Bên phải: biểu tượng **ba chấm dọc**.
- Xếp theo **thời gian truy cập, mới nhất trên cùng**.
- Không có thanh điều hướng đáy.

**Menu thao tác của 1 mục** — ảnh `history_item_menu.png`: bảng nổi trắng bo 12dp, 4 mục:
`Open in new tab` · `Chia sẻ` · `Copy link` · `Delete`.

### 3.3 Tương tác người dùng

| Thao tác | Kết quả |
|---|---|
| Bấm 1 dòng | Đóng màn Lịch sử, tải URL đó trong **thẻ hiện tại** |
| Bấm **ba chấm** của 1 dòng | Mở menu 4 mục (tại chỗ, không đổi màn) |
| Menu → `Open in new tab` | Đóng màn Lịch sử, mở URL đó trong **thẻ mới** |
| Menu → `Chia sẻ` | Mở bảng chia sẻ của hệ thống |
| Menu → `Copy link` | Chép URL + thông báo ngắn xác nhận |
| Menu → `Delete` | Xoá **riêng mục đó**, danh sách cập nhật ngay |
| Bấm **thùng rác** | **[ĐÃ CHỐT]** Hiện hộp thoại xác nhận `Xoá toàn bộ lịch sử?` với 2 nút `Xoá` / `Huỷ` — xoá hết là hành động không hoàn tác được nên phải hỏi |
| Bấm quay lại / Back | Về màn Trình duyệt, giữ nguyên trang đang xem |

### 3.4 Trạng thái/case đặc biệt

| Case | Hành vi |
|---|---|
| Lịch sử rỗng | **[THIẾT KẾ MỚI]** Hộp giấy cam + chữ `No history` (dùng chung khuôn ở mục 11.3); biểu tượng thùng rác ẩn đi |
| Trang không tải được | Vẫn ghi vào lịch sử, tiêu đề `Trang web hiện không khả dụng` |
| Trang không có favicon | Dùng biểu tượng **quả địa cầu** mặc định |
| Tắt `Recently used websites` trong Cài đặt | **[THIẾT KẾ MỚI]** Ngừng ghi lịch sử mới; các mục cũ vẫn còn cho tới khi người dùng tự xoá |
| Danh sách dài | Cuộn dọc, không phân nhóm theo ngày |

### 3.5 Điều hướng vào/ra màn hình này

**Vào từ:** Trình duyệt → menu ba chấm → `History`.
**Ra tới:** Trình duyệt (Back, hoặc bấm 1 mục / `Open in new tab`) · bảng chia sẻ hệ thống.

---

## 4. TRANG WEB GỢI Ý

> Ảnh: [`recommended_sites.png`](screenshot/recommended_sites.png)

### 4.1 Mục đích màn hình

Danh sách các trang web được gợi ý (những trang mà app tải video tốt), để người dùng vào nhanh mà
không phải gõ địa chỉ.

### 4.2 Các thành phần UI và nội dung hiển thị

- **Thanh công cụ**: mũi tên **quay lại** + tiêu đề `RECOMMENDED SITES`.
- **Lưới 3 cột**, mỗi ô **vuông 128×128dp**, **không khe hở** (dính liền thành mảng màu):
  - Nền là **màu thương hiệu riêng của trang**.
  - Giữa ô là **biểu trưng/chữ cái đại diện** cỡ lớn màu trắng.
  - Sát đáy ô là **tên trang** chữ trắng 12sp, căn giữa.
- 5 ô: `Facebook`, `Instagram`, `Vimeo`, `Dailymotion`, `Tubidy`. Lưới **lấp từ trái sang phải, trên
  xuống dưới**, hàng cuối không căn giữa.
- Phần màn còn lại để trống (nền `#FAFAFA`). Không có thanh điều hướng đáy.

### 4.3 Tương tác người dùng

| Thao tác | Kết quả |
|---|---|
| Bấm 1 ô | **[ĐÃ CHỐT]** Đóng màn này, tải trang chủ của trang đó trong **thẻ hiện tại** — giống hệt cách 8 ô lối tắt ở trang chủ hoạt động, giữ cho 2 chỗ nhất quán |
| Bấm quay lại / Back | Về màn Trình duyệt |

### 4.4 Trạng thái/case đặc biệt

**[ĐÃ CHỐT]** Danh sách **cố định, đóng gói sẵn trong app** ở bản đầu (đơn giản, không cần hạ tầng cấu
hình từ xa). Không có trạng thái rỗng.

### 4.5 Điều hướng vào/ra màn hình này

**Vào từ:** Trình duyệt → bảng danh sách thẻ → `Favorite sites`.
**Ra tới:** Trình duyệt (Back hoặc bấm 1 ô).

---

## 5. VIDEO RIÊNG TƯ

> Ảnh: [`private_video_set_pin.png`](screenshot/private_video_set_pin.png)

### 5.1 Mục đích màn hình

Khu vực cất riêng các video người dùng không muốn ai thấy, khoá bằng **mã PIN 4 chữ số**.

**[ĐÃ CHỐT]** Mã PIN chỉ có **một mục đích duy nhất: mở cửa vào khu vực Video riêng tư**. Không dùng
để khoá app, không mã hoá tệp, không có luồng khôi phục mã, không giới hạn số lần nhập sai.

### 5.2 Các thành phần UI và nội dung hiển thị

**Bước nhập mã** (ảnh hiện có) — **nền tối** (`#2A2A2B`), khác hẳn phần còn lại của app:
- **Thanh công cụ** tối: mũi tên **quay lại** + tiêu đề (`Set PIN` lần đầu, `Enter PIN` các lần sau).
- Giữa màn trên: chữ 24sp Bold `#E4E4E4` — nội dung đổi theo bước:
  `Set PIN` → `Nhập lại PIN` → `Enter PIN`.
- Bên dưới: **4 chấm tròn** `#6A6A6B` xếp ngang, cách đều; chấm được **tô đầy màu sáng** dần theo số
  chữ số đã nhập.
- Nửa dưới màn là **bàn phím số riêng của app** (nền `#222222`), lưới 3 cột × 4 hàng ô 128×61dp:
  `1 2 3` / `4 5 6` / `7 8 9` / (trống) `0` **⌫**. Chữ số 32sp `#D4D4D4`, không viền ô, không nền nút.

**Bước xem danh sách** (sau khi mở khoá) — **[THIẾT KẾ MỚI]**, quay lại **nền sáng** như phần còn lại
của app:
- Thanh công cụ: quay lại · tiêu đề `Private videos` · biểu tượng **ba gạch ngang** (chọn nhiều).
- Danh sách dùng **đúng khuôn thẻ của tab `FINISHED`** (mục 2.2) — ảnh thu nhỏ + tên + dung lượng +
  ba chấm.
- Menu ba chấm của mỗi mục: `Phát` · `Đưa ra ngoài` · `Xoá`.
- Chế độ chọn nhiều: hành động `Xoá` và `Đưa ra ngoài`.
- Rỗng: hộp giấy cam + chữ `Chưa có video riêng tư` + dòng phụ
  `Chuyển video từ tab Finished vào đây`.

### 5.3 Tương tác người dùng

| Thao tác | Kết quả |
|---|---|
| Bấm 1 chữ số | Điền 1 chấm; đủ 4 chữ số thì xử lý ngay, không cần nút xác nhận |
| Bấm **⌫** | Xoá chữ số cuối |
| Nhập đủ 4 số **lần đầu tiên** | Chuyển sang bước `Nhập lại PIN` để xác nhận; khớp → lưu mã và vào danh sách; không khớp → báo `Hai lần nhập không khớp`, quay về bước đầu |
| Nhập đủ 4 số **các lần sau** | Đúng → vào danh sách. Sai → rung nhẹ, 4 chấm xoá trắng, hiện chữ đỏ `Sai mã PIN` |
| Bấm quay lại / Back (chưa mở khoá) | Về màn Tải xuống tab `Finished` |
| Bấm quay lại / Back (đã mở khoá) | **[ĐÃ CHỐT]** Về thẳng màn Tải xuống, **KHÔNG** quay lại màn nhập mã |

### 5.4 Trạng thái/case đặc biệt

| Case | Hành vi |
|---|---|
| Chưa từng đặt mã | Tiêu đề `Set PIN`, phải nhập 2 lần |
| Đã đặt mã | Tiêu đề `Enter PIN`, nhập 1 lần |
| Nhập sai | Rung + xoá trắng + báo lỗi; **không giới hạn số lần thử** |
| Quên mã | **[ĐÃ CHỐT]** Không có luồng khôi phục. Gỡ app rồi cài lại sẽ mất cả mã lẫn danh sách riêng tư — chấp nhận, vì đây chỉ là lớp che mắt chứ không phải bảo mật thật |
| Danh sách riêng tư rỗng | Trạng thái rỗng như mô tả ở 5.2 |
| Thoát app rồi vào lại | Phải nhập mã lại từ đầu |

### 5.5 Điều hướng vào/ra màn hình này

**Vào từ:** Màn Tải xuống, tab `Finished` → biểu tượng **thư mục có ổ khoá**.
**Ra tới:** Màn Tải xuống (Back) · Trình phát video (bấm 1 video trong danh sách riêng tư).

---

## 6. CÀI ĐẶT

> Ảnh: `settings.png`, `settings_scrolled.png` (ảnh bổ sung do người giao việc cung cấp)

### 6.1 Mục đích màn hình

Chỉnh nơi lưu video, điều kiện mạng khi tải, các tuỳ chọn của trình duyệt, và mở các nội dung trợ giúp.

### 6.2 Các thành phần UI và nội dung hiển thị

- **Thanh công cụ**: mũi tên **quay lại** + tiêu đề `Settings` (chữ thường, không viết hoa toàn bộ —
  khác với `PROGRESS`/`RECOMMENDED SITES`).
- **Dải mua gói gỡ quảng cáo** (app gốc): dải bo tròn hết cạnh, nền **gradient cam → hồng** (trái →
  phải), chữ trắng đậm in hoa `XÓA QUẢNG CÁO` căn giữa; đè lên góc trên-phải là **huy hiệu vàng bo
  tròn** chữ đen nhỏ `Ưu đãi Đặc biệt`.
  **[ĐÃ CHỐT]** bản đầu **không làm** — nhưng **giữ chỗ trong bố cục** để bật lại sau.
- **Danh sách tuỳ chọn** chia thành **4 nhóm**, mỗi nhóm có **tiêu đề nhóm màu xanh** (14sp, căn
  trái); mỗi dòng cao ~56dp, ngăn cách bằng kẻ mảnh; dòng có mô tả phụ thì cao hơn:

| Nhóm | Dòng | Kiểu | Giá trị trong ảnh |
|---|---|---|---|
| `Tải về` | `Download location` | Chữ + **mô tả phụ** là đường dẫn | `/storage/emulated/0/Download/VideoDownloader` |
| | `Download with Wi-Fi Only` | **Công tắc** | Đang bật |
| `Browser` | `Block ads` | **Công tắc** + mô tả phụ | `Đang bật` |
| | `Recently used websites` | **Công tắc** + mô tả phụ ẩn | Đang bật |
| | `Search engine` | Chữ + mô tả phụ là công cụ đang chọn | `Bing` |
| | `Clear cache` | Chữ, hành động | — |
| | `Clear browser history` | Chữ, hành động | — |
| | `Clear cookies` | Chữ, hành động | — |
| `General Settings` | `Language options` | Chữ + **giá trị bên phải + mũi tên xuống** | `Mặc định ▼` |
| | `Sync to gallery` | **Công tắc** + mô tả phụ | `Đang bật` |
| `Help` | `Cách tải về` | Chữ, mở màn khác | — |
| | `Phản hồi` | Chữ, mở luồng ngoài | — |
| | `Chính sách bảo mật` | Chữ, mở trang web | — |
| | (số phiên bản) | **Chữ trơn, không bấm được** | `2.6.9A` |

- Công tắc bật có màu xanh; công tắc tắt màu xám nhạt.
- Toàn màn cuộn được; không có thanh điều hướng đáy.

### 6.3 Tương tác người dùng

| Thao tác | Kết quả |
|---|---|
| Bấm `Download location` | Mở bộ chọn thư mục của hệ thống; chọn xong thì mô tả phụ đổi theo **[THIẾT KẾ MỚI]** |
| Gạt `Download with Wi-Fi Only` | Bật/tắt tại chỗ. Bật → mọi mục tải đang chạy trên dữ liệu di động chuyển sang `Chờ Wi-Fi` |
| Gạt `Block ads` | Bật/tắt chặn quảng cáo trong trình duyệt; mô tả phụ đổi giữa `Đang bật`/`Đang tắt`; trang đang mở tải lại **[THIẾT KẾ MỚI]** |
| Gạt `Recently used websites` | Bật/tắt việc ghi lịch sử duyệt web và gợi ý trang gần đây |
| Bấm `Search engine` | Mở hộp thoại chọn 1 trong các công cụ tìm kiếm (`Google` · `Bing` · `DuckDuckGo` · `Yahoo`) **[THIẾT KẾ MỚI]**; chọn xong đóng ngay, mô tả phụ đổi theo |
| Bấm `Clear cache` | Hộp thoại xác nhận → xoá bộ nhớ đệm trình duyệt + thông báo ngắn `Đã xoá` **[THIẾT KẾ MỚI]** |
| Bấm `Clear browser history` | Hộp thoại xác nhận → xoá toàn bộ lịch sử (cùng tác dụng với thùng rác ở màn Lịch sử) |
| Bấm `Clear cookies` | Hộp thoại xác nhận → xoá cookie, người dùng bị đăng xuất khỏi các trang đã đăng nhập |
| Bấm `Language options` | Mở màn chọn ngôn ngữ của lớp dùng chung |
| Gạt `Sync to gallery` | Bật → video tải xong hiện trong ứng dụng Thư viện của máy; tắt → chỉ thấy trong app **[THIẾT KẾ MỚI]** |
| Bấm `Cách tải về` | Mở màn **Cách tải về** (mục 9) |
| Bấm `Phản hồi` | Mở luồng gửi phản hồi (thư/biểu mẫu ngoài app) |
| Bấm `Chính sách bảo mật` | Mở trang chính sách trong trình duyệt của app **[THIẾT KẾ MỚI]** |
| Bấm quay lại / Back | Về màn Trình duyệt |

### 6.4 Trạng thái/case đặc biệt

| Case | Hành vi |
|---|---|
| Chưa cấp quyền ghi bộ nhớ | Bấm `Download location` → xin quyền trước; từ chối thì giữ nguyên thư mục cũ **[THIẾT KẾ MỚI]** |
| Thư mục đã chọn không còn (thẻ nhớ tháo ra) | Tự quay về thư mục mặc định + thông báo ngắn **[THIẾT KẾ MỚI]** |
| Đang có mục tải dở khi bật `Wi-Fi Only` | Các mục đang chạy bằng dữ liệu di động chuyển sang `Chờ Wi-Fi`, không huỷ |
| Xoá cookie khi đang mở trang đã đăng nhập | Trang tải lại ở trạng thái chưa đăng nhập |
| Bật `Sync to gallery` lần đầu | Đồng bộ cả các video đã tải trước đó **[THIẾT KẾ MỚI]** |

### 6.5 Điều hướng vào/ra màn hình này

**Vào từ:** Trình duyệt → biểu tượng **bánh răng** ở trang chủ, hoặc menu ⋮ → `Settings`.
**Ra tới:** Trình duyệt (Back) · Cách tải về · màn chọn ngôn ngữ dùng chung · bộ chọn thư mục của hệ
thống · trang chính sách bảo mật · luồng phản hồi.

---

## 7. TRÌNH PHÁT VIDEO

> **[THIẾT KẾ MỚI]** — app gốc có màn này nhưng không có ảnh chụp. Toàn bộ mục 7 là thiết kế mới, bám
> Style Guide, không phải mô tả app gốc.

### 7.1 Mục đích màn hình

Phát video đã tải về, ngay trong app, không cần mở ứng dụng ngoài.

### 7.2 Các thành phần UI và nội dung hiển thị

Nền **đen tuyền**, video căn giữa theo tỉ lệ gốc. Giao diện điều khiển **tự ẩn sau 3 giây** không
chạm, chạm 1 lần vào màn để hiện lại.

**Khi giao diện điều khiển đang hiện:**
- **Lớp trên**: mũi tên quay lại · tên tệp (1 dòng, cắt bằng `…`) · biểu tượng **chia sẻ** · biểu
  tượng **ba chấm dọc**. Nền gradient đen mờ từ trên xuống.
- **Giữa màn**: nút **lùi 10 giây** · nút **phát/tạm dừng** (tròn lớn 64dp) · nút **tiến 10 giây**.
- **Lớp dưới**: thời gian đã phát · **thanh tua** (rãnh xám mờ, phần đã phát `#067EFF`, nút kéo tròn
  trắng) · tổng thời lượng · biểu tượng **xoay ngang/dọc màn hình**. Nền gradient đen mờ từ dưới lên.

**Menu ba chấm**: `Tốc độ phát` (0.5× / 1× / 1.5× / 2×) · `Chuyển vào riêng tư` · `Xoá`.

### 7.3 Tương tác người dùng

| Thao tác | Kết quả |
|---|---|
| Chạm 1 lần vào màn | Hiện/ẩn giao diện điều khiển |
| Bấm phát/tạm dừng | Phát hoặc dừng |
| Kéo thanh tua | Nhảy tới vị trí tương ứng; hiện nhãn thời gian khi đang kéo |
| Bấm lùi/tiến 10 giây | Nhảy 10 giây |
| Nhấn đúp nửa trái / nửa phải màn | Lùi / tiến 10 giây (phím tắt) |
| Vuốt dọc nửa trái | Chỉnh độ sáng màn hình |
| Vuốt dọc nửa phải | Chỉnh âm lượng |
| Bấm biểu tượng xoay | Chuyển giữa dọc và ngang |
| Bấm chia sẻ | Mở bảng chia sẻ của hệ thống với tệp video |
| Bấm quay lại / Back | Về màn trước (Tải xuống hoặc Video riêng tư), dừng phát |

### 7.4 Trạng thái/case đặc biệt

| Case | Hành vi |
|---|---|
| Đang tải đệm | Vòng quay ở giữa màn, ẩn nút phát |
| Tệp hỏng / không phát được | Nền đen + biểu tượng cảnh báo + chữ `Không phát được video này` + nút `Xoá tệp` |
| Tệp đã bị xoá ngoài app | Đóng màn ngay + thông báo ngắn `Tệp không còn tồn tại` |
| Phát xong | Dừng ở khung cuối, hiện lại nút phát (không tự phát video kế tiếp) |
| Có cuộc gọi / app khác chiếm âm thanh | Tự tạm dừng, không tự phát lại |
| Xoay ngang | Video phủ toàn màn, ẩn thanh trạng thái |

### 7.5 Điều hướng vào/ra màn hình này

**Vào từ:** Màn Tải xuống tab `Finished` (bấm 1 mục hoặc menu → `Phát`) · Danh sách video riêng tư.
**Ra tới:** Màn trước đó (Back) · bảng chia sẻ hệ thống.

---

## 8. DẤU TRANG

> **[THIẾT KẾ MỚI]** — app gốc có mục `Bookmarks` trong menu nhưng không có ảnh chụp.

### 8.1 Mục đích màn hình

Xem lại các trang đã lưu, mở nhanh hoặc bỏ lưu.

### 8.2 Các thành phần UI và nội dung hiển thị

Dùng **đúng khuôn màn Lịch sử duyệt web** (mục 3) để 2 màn nhất quán:
- Thanh công cụ: quay lại · tiêu đề `Bookmarks` · biểu tượng **ba gạch ngang** (chọn nhiều).
- Danh sách dòng 68dp: tiêu đề trang + favicon + URL + ba chấm dọc.
- Menu ba chấm: `Open in new tab` · `Chia sẻ` · `Copy link` · `Đổi tên` · `Delete`.
- Rỗng: hộp giấy cam + chữ `Chưa có dấu trang` + dòng phụ
  `Lưu trang bằng menu ⋮ → Add bookmark`.

### 8.3 Tương tác người dùng

Giống mục 3.3, thay `Delete` bằng bỏ lưu; thêm `Đổi tên` mở hộp thoại sửa tiêu đề hiển thị.

### 8.4 Trạng thái/case đặc biệt

Rỗng (xem trên) · trùng URL (lưu lần 2 thì báo `Trang này đã được lưu`, không tạo mục trùng).

### 8.5 Điều hướng vào/ra màn hình này

**Vào từ:** Trình duyệt → menu ⋮ → `Bookmarks`.
**Ra tới:** Trình duyệt (Back, hoặc bấm 1 mục).

---

## 9. CÁCH TẢI VỀ

> **[THIẾT KẾ MỚI]** — app gốc có 2 lối vào (`Cách tải về` ở màn Tải xuống, dấu hỏi ở trang chủ,
> `Cách tải về` trong Cài đặt) nhưng không có ảnh chụp. **[ĐÃ CHỐT]** cả 3 lối vào ra **cùng một màn**.

### 9.1 Mục đích màn hình

Dạy người dùng mới cách tải video: vào trang → mở video → bấm nút tải nổi.

### 9.2 Các thành phần UI và nội dung hiển thị

- Thanh công cụ: quay lại + tiêu đề `Cách tải về`.
- **3 bước** xếp dọc, mỗi bước là 1 khối gồm: **số thứ tự** trong hình tròn `#067EFF` chữ trắng · **ảnh
  minh hoạ** (ảnh chụp giao diện chính app này, bo 12dp, viền `#EEF1FB`) · **tiêu đề bước** 16sp
  Medium · **mô tả** 14sp `#7D8EAF`:
  1. `Mở trang có video` — *Nhập địa chỉ hoặc bấm một lối tắt ở trang chủ.*
  2. `Phát video muốn tải` — *Nút tải ở góc dưới-phải sẽ chuyển sang màu xanh khi tìm thấy video.*
  3. `Bấm nút tải và chọn chất lượng` — *Theo dõi tiến trình ở tab Progress, xem lại ở tab Finished.*
- Dưới cùng: nút chính bo tròn hết cạnh `Bắt đầu tải video` — bấm là về màn Trình duyệt.

### 9.3 Tương tác người dùng

Bấm `Bắt đầu tải video` hoặc quay lại / Back → về màn Trình duyệt.

### 9.4 Trạng thái/case đặc biệt

Không có — màn tĩnh, không phụ thuộc dữ liệu.

### 9.5 Điều hướng vào/ra màn hình này

**Vào từ:** Trang chủ trình duyệt (dấu hỏi) · Màn Tải xuống (nút `Cách tải về`) · Cài đặt (`Cách tải về`).
**Ra tới:** Màn trước đó (Back) hoặc màn Trình duyệt (nút chính).

---

## 10. NHẠC CHUÔNG

> ⏸ **HOÃN** — đã có đủ 6 ảnh trong [`screenshot/`](screenshot/) (tên bắt đầu bằng `ringtone_`) nhưng
> **giai đoạn này chưa làm**. Giữ mục này để làm sau, không xoá ảnh. Ô lối tắt `Nhạc chuông` ở trang
> chủ đã bỏ khỏi bản đầu (xem mục 1.2).

Tóm tắt để không mất thông tin (chưa phải đặc tả đầy đủ 5 mục):

- Thanh công cụ riêng, nền `#F5F5FA` bo góc dưới, có **viên kim cương tím** bên phải (mở gói trả phí).
  3 thẻ lọc dạng viên thuốc: `Thể loại` (đang chọn — **nền gradient `#FF2680`→`#FF8B4E`, chữ trắng**) ·
  `Phổ biến` · `Yêu thích` (không chọn — nền trắng, viền xám).
- Danh sách thể loại: ảnh bìa vuông bo góc + tên + số lượng (`39 nhạc chuông`) + biểu tượng tải.
- Bấm 1 mục **mở rộng tại chỗ**, lộ 4 hành động: `Nhạc chờ` · `Báo thức` · `Thông báo` · `Thông tin`.
- 3 hành động đầu cần quyền ghi cài đặt hệ thống → hộp thoại
  `Để đặt nhạc chuông, Video Downloader cần quyền cho phép này để quản lý nhạc chuông.` + `OK`.
- `Thông tin` → hộp thoại `Thông tin` với `Tác giả` + `Giấy phép` (vd `HerbertBoland` / `(CC BY 3.0)`).
- Màn mở khoá gói: `Mở khóa các nhạc chuông cao cấp miễn phí` / `Xem một video ngắn để mở khóa bộ sưu
  tập nhạc chuông miễn phí.` / `hoặc` / `Tham gia Pro`.
- Hộp thoại lỗi thanh toán: `Can't make purchase` + `The Google Paid service isn't available at the
  moment or in your country, you can continue to use the application except paying for removing ads.`
  + `OK`.

---

## 11. Ghi chú chung (áp dụng nhiều màn)

### 11.1 Thanh điều hướng đáy

Chỉ xuất hiện ở **2 màn**: Trình duyệt (mục `Thẻ`) và Quản lý tải xuống (mục `Progress`/`Finished`).
Các màn Lịch sử / Trang web gợi ý / Video riêng tư / Cài đặt / Dấu trang / Cách tải về / Trình phát
video **không có** thanh này — chúng mở chồng lên và thoát bằng nút quay lại.

Mục `Finished` có **huy hiệu tròn đỏ** đếm số video vừa tải xong chưa xem; mở tab đó là huy hiệu biến
mất.

### 11.2 Quy tắc nút Back — [ĐÃ CHỐT]

1. Đang mở lớp phủ (bảng thẻ / menu / hộp thoại / chế độ chọn nhiều) → **đóng lớp phủ**, không đổi màn.
2. Đang ở tab `Progress` hoặc `Finished` → **về tab `Thẻ`** (không thoát app). Đây là hành vi người
   dùng mong đợi và tránh việc bấm nhầm là mất app.
3. Đang ở tab `Thẻ`, thẻ còn lịch sử web → **lùi 1 bước trong trang**.
4. Đang ở tab `Thẻ`, thẻ hết lịch sử nhưng chưa ở trang chủ → **về trang chủ của thẻ**.
5. Đang ở tab `Thẻ`, đang ở trang chủ → **thoát app**.

### 11.3 Trạng thái rỗng

Dùng chung 1 khuôn ở mọi màn: minh hoạ **hộp giấy mở nắp màu cam** 106×106dp căn giữa + 1 dòng chữ
16sp `#989DAF` bên dưới; nếu cần hướng dẫn thì thêm 1 dòng phụ 14sp mờ hơn.

Áp dụng cho: Tải xuống (2 tab) · Lịch sử · Dấu trang · Video riêng tư.

### 11.4 Quyền cần xin

| Quyền | Dùng để | Xin khi nào |
|---|---|---|
| Truy cập mạng | Duyệt web + tải video | Không cần hỏi |
| Ghi bộ nhớ ngoài | Lưu video đã tải | Khi tải video **đầu tiên**, hoặc khi đổi `Download location` |
| Thông báo | Hiện tiến trình tải | Ở màn xin quyền lúc mở app lần đầu |
| Chạy nền / giữ máy thức | Tải tiếp khi tắt màn hình | Không cần hỏi |
| Ghi cài đặt hệ thống | Đặt nhạc chuông | ⏸ chưa cần (module đang hoãn) |

### 11.5 Những gì đã bỏ so với app gốc — [ĐÃ CHỐT]

| Bỏ | Lý do |
|---|---|
| Huy hiệu `AD` gỡ quảng cáo trên thanh công cụ | Bản đầu chưa làm điểm chèn quảng cáo |
| Dải `XÓA QUẢNG CÁO` ở màn Cài đặt | Như trên — nhưng **giữ chỗ** trong bố cục |
| Thẻ quảng cáo trong danh sách tải | Như trên — nhưng **giữ chỗ** trong bố cục |
| Menu ⋮: `Chia sẻ với bạn bè`, `Scan (AD)`, `Ứng dụng Gia đình (AD)` | Quảng cáo chéo |
| Ô lối tắt `Nhạc chuông` ở trang chủ | Module Nhạc chuông đang hoãn |
| Chế độ tối | App gốc không có; bản đầu không làm |
| Nhúng font riêng | Dùng font hệ thống cho đơn giản |
