# Video Downloader — Style Guide / Design System

> **Nguồn giá trị:** đo trực tiếp bằng công cụ trên ảnh chụp thật trong [`screenshot/`](screenshot/)
> (720×1600px, density 300 → **1dp = 1.875px**), đối chiếu với cây bố cục dp của app tham chiếu.
> Mã màu là màu điểm ảnh thật, không phải ước lượng bằng mắt.
>
> Chỗ nào không đo được (chưa có ảnh của trạng thái đó) được ghi rõ **[ASSUMPTION]** /
> **[CHỜ ẢNH]**.
>
> Bước tiếp theo bắt buộc sau khi đọc tài liệu này: map giá trị vào 3 file token theo
> [`QUY_CHUAN_DESIGN.md`](QUY_CHUAN_DESIGN.md) — không hardcode màu/cỡ chữ tại từng màn hình.

## 0. Tổng quan phong cách

Phẳng, sáng, gọn — phong cách "trình duyệt sạch": nền **trắng/gần trắng**, không đổ bóng nặng, không
viền, phân tách khối bằng **màu nền xám-xanh rất nhạt** thay vì đường kẻ. Điểm nhấn duy nhất là **màu
xanh dương** dùng cho mọi thứ đang được chọn hoặc bấm được. Chữ đen đậm cho tiêu đề, chữ **xám-xanh**
(không phải xám trung tính) cho nội dung phụ — đây là nét nhận dạng của bảng màu này.

Ba ngoại lệ có chủ đích, đi ngược tông chung:
1. **Màn Video riêng tư** dùng **nền tối** — nhấn mạnh tính chất riêng tư/khoá.
2. **Màn Trang web gợi ý** dùng các ô **màu thương hiệu đặc kín**, dính liền không khe hở.
3. **Minh hoạ trạng thái rỗng** là hình hộp giấy **màu cam** — màu nóng duy nhất trong app.

## 1. Font

| Hạng mục | Giá trị |
|---|---|
| Font | **Font hệ thống mặc định** — app gốc không nhúng font riêng. Ảnh chụp trên máy Samsung nên hiển thị bằng SamsungOne; trên máy khác sẽ ra Roboto |
| Weight dùng | Regular (400) · Medium (500) · SemiBold (600) · Bold (700) |

**[ĐÃ CHỐT] app mới KHÔNG nhúng font riêng** — dùng font hệ thống cho đơn giản và nhẹ gói cài. Hệ quả
phải chấp nhận: giao diện sẽ khác nhẹ giữa các hãng máy (Samsung ra SamsungOne, máy khác ra Roboto),
và ảnh render khi so với screenshot gốc sẽ lệch nhẹ về bề rộng chữ — **đừng chỉnh khoảng cách để bù
sai lệch này**, chỉ so bố cục và màu.

## 2. Bảng màu

### 2.1 Nền / bề mặt

| Tên | Mã | Dùng ở đâu |
|---|---|---|
| Nền chính | `#FFFFFF` | Nền màn Trình duyệt, thanh công cụ, thanh điều hướng đáy, hộp thoại, bảng menu, bảng danh sách thẻ |
| Nền phụ | `#FAFAFA` | Nền màn Lịch sử, màn Trang web gợi ý |
| Bề mặt nhấn (surface variant) | `#EEF1FB` | **Màu nhận dạng của app** — ô tìm kiếm, ô địa chỉ, nút `Cách tải về`, tiêu đề bảng thẻ, dòng thẻ đang chọn, thanh 4 nút điều hướng trong bảng thẻ, ô chứa đường dẫn trong hộp thoại (`#EEF0FB` — chênh 1 đơn vị, coi như cùng màu) |
| Nền tối (màn riêng tư) | `#2A2A2B` | Nền màn nhập mã PIN |
| Nền tối phụ (bàn phím số) | `#222222` | Nửa dưới màn nhập mã PIN |
| Lớp phủ tối sau hộp thoại/bảng | `#666666` (~40% đen) | Nền phía sau hộp thoại, phía sau bảng danh sách thẻ |

### 2.2 Nhấn / trạng thái

| Tên | Mã | Dùng ở đâu |
|---|---|---|
| **Xanh chính** | `#067EFF` | Mục đáy đang chọn, nút chính trong hộp thoại, ô đánh dấu đã bật |
| Xanh liên kết | `#127AFF` | Đường dẫn trong hộp thoại `Link you copied` |
| Xanh chữ nút phụ | `#1974FF` | Chữ `Cách tải về` |
| Cam minh hoạ (đậm) | `#FF7F00` | Thân hộp giấy — trạng thái rỗng |
| Cam minh hoạ (nhạt) | `#FFA300` | Nắp hộp giấy — trạng thái rỗng |
| Gradient gói trả phí ⏸ | `#FF2680` → `#FF8B4E` (trái→phải) | Viên thuốc đang chọn ở màn Nhạc chuông (đang hoãn) |

> 3 sắc xanh `#067EFF` / `#127AFF` / `#1974FF` chênh nhau không đáng kể — app gốc dùng lẫn lộn.
> **App mới chỉ dùng đúng 1 màu `#067EFF`** cho toàn bộ, tránh lặp lại sự thiếu nhất quán này.

### 2.3 Màu chữ

| Tên | Mã | Dùng ở đâu |
|---|---|---|
| Chữ chính (tiêu đề màn) | `#212121` | `PROGRESS`, `FINISHED`, `RECOMMENDED SITES`, `History` (`#202020` — cùng một màu) |
| Chữ chính (nội dung) | `#141518` | Tiêu đề dòng lịch sử, nhãn mục trong menu |
| Chữ nhập/gợi ý trong ô địa chỉ | `#48494F` | Chữ mờ `Search or Type URL` |
| **Chữ phụ (xám-xanh)** | `#7D8EAF` | Nhãn lối tắt ở trang chủ, nhãn mục đáy KHÔNG chọn, liên kết phản hồi |
| Chữ phụ đậm hơn | `#565C6E` | URL ở dòng thứ 2 trong màn Lịch sử |
| Chữ trạng thái rỗng | `#989DAF` | `No history` |
| Chữ nút phụ (huỷ) | `#7A8194` | `Hủy bỏ` trong hộp thoại |
| Chữ trên nền tối — tiêu đề | `#E4E4E4` | `Set PIN` |
| Chữ trên nền tối — chữ số | `#D4D4D4` | Bàn phím số |
| Chấm PIN chưa nhập | `#6A6A6B` | 4 chấm tròn |
| Chữ trên nền màu | `#FFFFFF` | Nhãn trong ô màn Trang web gợi ý, chữ nút chính |

### 2.4 Màu thương hiệu các ô ở màn Trang web gợi ý

`Facebook` `#333333` · `Instagram` `#54B0E3` · `Vimeo` `#888888` · `Dailymotion` `#035FC1` ·
`Tubidy` `#28CAF1`.

> ⚠️ Đây là màu nền của app gốc, **không** phải mã màu thương hiệu chính thức. App mới **không được
> dùng lại biểu trưng/tên thương hiệu của các dịch vụ này làm tài sản riêng** — xem
> [`04_SCREENSHOT_MAPPING.md`](04_SCREENSHOT_MAPPING.md) §5.

### 2.5 Màu ở màn Cài đặt và thẻ mục tải

Đo từ 3 ảnh bổ sung (`settings.jpg`, `settings_scrolled.jpg`, `downloads_progress_active.jpg`). 3 file
này là **JPEG** nên màu bị nén lệch vài đơn vị — cột "Chốt dùng" là giá trị đã quy về bảng màu chuẩn ở
§2.1–2.3, **dùng cột này khi code**.

| Vai trò | Đo được | Chốt dùng |
|---|---|---|
| Nền thẻ 1 mục tải | `#F4F5FA` | `#F4F5FA` |
| Nền thẻ quảng cáo | `#EEF1FA` | `#EEF1FB` (= bề mặt nhấn) |
| Rãnh thanh tiến trình | `#D1D4E5` | `#D1D4E5` |
| Nền ảnh thu nhỏ chưa có ảnh thật | `#8BB1FA` | `#8BB1FA` |
| Chữ trạng thái mục tải (`paused`, dung lượng) | `#7D8089` | `#7D8089` |
| Biểu tượng hành động trên thẻ tải | `#277CF9` | `#067EFF` |
| Tiêu đề nhóm ở màn Cài đặt | ~`#4A8AD8` (nét mảnh, lệch nhiều do nén) | `#067EFF` |
| Chữ mục Cài đặt | `#131313` | `#141518` |
| Mô tả phụ mục Cài đặt | `#494A4E` | `#48494F` |
| Kẻ ngăn cách giữa các mục | `#E7E7E7` | `#E7E7E7` |
| Công tắc **bật** | `#297FF8` | `#067EFF` |
| Công tắc **tắt** | — | `#D1D4E5` (rãnh), thumb trắng |
| Giá trị bên phải mục (`Mặc định ▼`) | `#2C71EA` | `#067EFF` |

### 2.6 Huy hiệu đếm (badge)

| Vai trò | Đo được | Chốt dùng |
|---|---|---|
| Nền huy hiệu đỏ trên mục đáy `Finished` | `#DF5643` | `#E5484D` |
| Chữ trong huy hiệu | — | `#FFFFFF`, 10sp Bold |

Dùng cho: số video vừa tải xong chưa xem (mục đáy `Finished`), số video tìm thấy trên trang (nút tải
nổi ở màn Trình duyệt).

### 2.7 Gradient thương mại — [ĐÃ CHỐT: giữ chỗ, bản đầu chưa dùng]

| Vai trò | Giá trị |
|---|---|
| Dải `XÓA QUẢNG CÁO` ở màn Cài đặt | Gradient ngang `#EE8038` (trái) → `#D95551` (giữa) → `#CE3A6E` (phải) |
| Huy hiệu `Ưu đãi Đặc biệt` | Nền `#F2D369`, chữ nâu đậm `#5A4A20` [ước lượng] |
| Viên thuốc lọc ở màn Nhạc chuông ⏸ | Gradient `#FF2680` → `#FF8B4E` |

### 2.8 Màu trạng thái

App gốc **không có** màu success / warning / error riêng. **[THIẾT KẾ MỚI]** cho app mới:

| Vai trò | Giá trị |
|---|---|
| Lỗi (tải thất bại, sai mã PIN) | `#E5484D` — dùng chung với màu huy hiệu đếm |
| Cảnh báo (chờ Wi-Fi) | `#F5A524` |
| Thành công (tải xong) | `#067EFF` — dùng luôn màu xanh chính, giữ bảng màu gọn |

### 2.9 Chế độ tối — [ĐÃ CHỐT: không làm]

App gốc không có chế độ tối, app mới cũng không làm ở bản đầu. Màn Video riêng tư nền tối là **thiết
kế riêng của màn đó**, không phải theme — không được lấy làm cơ sở cho chế độ tối sau này.

## 3. Typography

Giá trị đo từ ảnh (chiều cao chữ thực tế → cỡ chữ, sai số ±1sp):

| Vai trò | Cỡ + weight | Màu | Chỗ dùng (đã đo) |
|---|---|---|---|
| Tiêu đề màn | **20sp Bold** | `#212121` | `PROGRESS` / `FINISHED` / `RECOMMENDED SITES` / `History` / `Tabs` |
| Tiêu đề hộp thoại | **24sp Bold** | `#141518` | `Link you copied`, `Set PIN` (chữ lớn giữa màn) |
| Nội dung chính | **16sp Medium** | `#141518` | Tiêu đề dòng lịch sử, nhãn mục trong menu ⋮ (48dp/dòng), chữ mờ ô địa chỉ, `No history` |
| Nội dung phụ | **14sp Medium** | `#1974FF` / `#7D8EAF` | Chữ `Cách tải về` |
| Phụ chú / URL | **12sp Regular** | `#565C6E` | Dòng URL thứ 2 trong màn Lịch sử |
| Nhãn nhỏ | **12sp Regular** | `#7D8EAF` | Nhãn ô lối tắt (`Facebook`...), nhãn mục đáy **không chọn** |
| Nhãn mục đáy **đang chọn** | **14sp Medium** | `#067EFF` | `Thẻ` / `Progress` / `Finished` khi được chọn |
| Chữ nút chính | **18sp Bold** | `#FFFFFF` | `Mở` |
| Chữ nút phụ | **16sp Regular** | `#7A8194` | `Hủy bỏ` |
| Chữ số bàn phím PIN | **32sp Regular** | `#D4D4D4` | `0`–`9` |

Bảng 10 vai trò trên là **danh sách đầy đủ** — không tạo thêm cỡ chữ nào ngoài bảng này. Việc quy đổi
sang tên vai trò trong mã nguồn làm ở bước sau, xem [`QUY_CHUAN_DESIGN.md`](QUY_CHUAN_DESIGN.md) §1.

## 4. Bo góc (Corner Radius)

| Thành phần | Bán kính | Ghi chú |
|---|---|---|
| Ô tìm kiếm / ô địa chỉ | **Bo tròn hết** (= ½ chiều cao ≈ 25dp) | Đo: cao 50.7dp |
| Nút `Cách tải về` | **Bo tròn hết** (≈ 22dp) | Đo: cao 44.3dp |
| Nút chính trong hộp thoại (`Mở`) | **Bo tròn hết** (≈ 24dp) | Đo: cao 48.5dp |
| Thẻ hộp thoại | **12dp** | Đo được ~9–12dp |
| Bảng menu ⋮ nổi | **12dp** | [ASSUMPTION] — chưa đo chính xác được |
| Ô chứa đường dẫn trong hộp thoại | **8dp** | [ASSUMPTION] |
| Ảnh bìa danh sách nhạc chuông ⏸ | **12dp** | Ước lượng từ ảnh |
| Ô ở màn Trang web gợi ý | **0dp** | Vuông tuyệt đối, dính liền nhau |
| Dòng danh sách (lịch sử, thẻ, gợi ý) | **0dp** | Không bo, không thẻ nổi |

**Quy tắc rút ra:** mọi thứ **bấm được và nổi lên** thì bo tròn hết cạnh; mọi thứ **là dòng trong danh
sách** thì vuông; thẻ/bảng nổi bo 12dp.

## 5. Khoảng cách (Spacing)

Đơn vị nền tảng: **lưới 4dp**. Khung màn tham chiếu: **384dp** rộng.

| Hạng mục | Giá trị đo |
|---|---|
| Padding ngang màn (mặc định) | **16dp** — tiêu đề `PROGRESS` ở x=16; nút `Cách tải về` ở x=17.1 |
| Padding ngang ô tìm kiếm trang chủ | **24dp** — đo x=23.5..360.5 (rộng 337dp) |
| Chiều cao thanh công cụ | **56dp** (dưới thanh trạng thái 31.5dp) |
| Chiều cao thanh điều hướng đáy | **56dp**, chia đều 3 mục × **128dp** |
| Biểu tượng trong mục đáy | **24×24dp**, cách nhãn ~2dp |
| Chiều cao ô tìm kiếm trang chủ | **50.7dp** → dùng **52dp** |
| Chiều cao nút `Cách tải về` | **44.3dp** → dùng **44dp**, cách đáy nội dung 34dp |
| Chiều cao nút chính hộp thoại | **48.5dp** → dùng **48dp** |
| Ô lối tắt trang chủ | ô **96×70.9dp**, lưới 4 cột × 2 hàng, không khe hở; hàng 1 bắt đầu y=182.4dp |
| Khoảng cách ô tìm kiếm → lưới lối tắt | **26dp** |
| Dòng menu ⋮ | cao **48dp**, bảng rộng **244.8dp** |
| Dòng danh sách lịch sử | cao **68.3dp**, có đường kẻ mảnh ngăn cách hết chiều ngang |
| Dòng gợi ý khi nhập địa chỉ | cao **64dp** |
| Dòng thẻ trong bảng danh sách thẻ | cao **~110dp** |
| Bảng danh sách thẻ | rộng **300.3dp** (≈78% màn), trượt từ **trái** |
| Thẻ hộp thoại | rộng **330dp**, lề ngang **27dp**, padding trong ~13dp |
| Ô màn Trang web gợi ý | **128×128dp**, khe hở **0**, lưới 3 cột |
| Bàn phím PIN | lưới 3 cột × 4 hàng, ô **128×61dp** |
| Nút tải nổi | vùng chạm **123×123dp**, biểu tượng **55×55dp**, đặt góc dưới-phải, cách đáy nội dung ~6dp |
| **Thẻ 1 mục tải** | cao **~84dp** (±4), lề ngang **14dp**, cách nhau **8dp**, bo 12dp |
| **Ảnh thu nhỏ trong thẻ tải** | **72×72dp** bo 8dp, cách mép trái thẻ 6dp |
| **Thẻ quảng cáo trong danh sách tải** | cao **~64dp**, cùng lề với thẻ mục tải |
| **Dòng ở màn Cài đặt** | cao **56dp** (không có mô tả phụ) / **72dp** (có mô tả phụ), lề ngang 16dp |
| **Tiêu đề nhóm ở màn Cài đặt** | cao 40dp, chữ 14sp, lề ngang 16dp, cách nhóm trên 8dp |
| **Công tắc** | 52×32dp, đặt sát lề phải 16dp |
| **Dải `XÓA QUẢNG CÁO`** ⏸ | rộng **330dp** (lề ngang 32dp), cao **~52dp**, bo tròn hết cạnh; huy hiệu ưu đãi **nhô lên trên** góc phải |
| **Huy hiệu đếm** | đường kính **16dp**, đè lên góc trên-phải của biểu tượng |

## 6. Nút hành động chính (Primary Button)

Chỉ có **một** kiểu nút chính trong toàn app (thấy ở hộp thoại `Link you copied`):

| Thuộc tính | Giá trị |
|---|---|
| Chiều cao | **48dp** |
| Chiều rộng | Chiếm hết chiều ngang khả dụng của thẻ chứa (đo 250dp trong thẻ rộng 330dp) |
| Nền | `#067EFF` |
| Chữ | **18sp Bold**, `#FFFFFF`, căn giữa |
| Bo góc | Bo tròn hết cạnh (24dp) |
| Đổ bóng | Không |
| Trạng thái nhấn | **[ASSUMPTION]** tối màu nhẹ (gợn nước theo mặc định hệ thống) |
| Trạng thái tắt | **[ASSUMPTION]** nền `#EEF1FB`, chữ `#7D8EAF` |
| Trạng thái đang xử lý | **[ASSUMPTION]** vòng quay trắng thay chữ, giữ nguyên kích thước |

**Nút phụ** đi kèm luôn là **chữ trơn** (không nền, không viền): 16sp Regular `#7A8194`, đặt ngay dưới
nút chính, căn giữa — không dùng nút viền.

**Nút cấp 3** (`Cách tải về`): nền `#EEF1FB`, chữ 14sp Medium `#1974FF`, cao 44dp, bo tròn hết, có
biểu tượng tròn xanh 25dp bên trái chữ.

## 7. Thành phần đặc thù của app

### 7.1 Quy ước "đang chọn"

App **không dùng gạch chân, không dùng viền** để thể hiện trạng thái chọn. Chỉ 2 cách:

| Nơi | Cách thể hiện |
|---|---|
| Mục thanh điều hướng đáy | Biểu tượng + chữ đổi từ `#7D8EAF` sang `#067EFF`, chữ **to hơn 2sp và đậm hơn** (12sp → 14sp) |
| Dòng thẻ đang xem trong bảng thẻ | **Đổi màu nền dòng** sang `#EEF1FB`, chữ chuyển đậm; các dòng khác nền trắng |
| Viên thuốc lọc ở màn Nhạc chuông ⏸ | Nền gradient `#FF2680`→`#FF8B4E` + chữ trắng; chưa chọn thì nền trắng viền xám |

### 7.2 Ô nhập địa chỉ 2 hình thái

Cùng một ô nhưng đổi hẳn hình dạng theo trạng thái — đây là chi tiết dễ làm sai nhất của app:

| Trạng thái | Hình thái |
|---|---|
| **Nghỉ, trang chủ** | Ô lớn cao 52dp, lề ngang 24dp, nằm **trong thân màn** (không phải trên thanh công cụ), chữ mờ `Search or Type URL` |
| **Nghỉ, đã tải trang** | Ô nhỏ nằm **trên thanh công cụ**, cao ~37dp, chỉ chiếm phần giữa, hiện URL + nút tải lại |
| **Đang nhập** | Ô chiếm gần hết thanh công cụ (rộng 327dp, cao 48.5dp); mọi biểu tượng khác biến mất, chỉ còn nút mũi tên xuống-dòng bên phải |

### 7.3 Thanh phím tắt nhập nhanh

Dải nằm **ngay trên bàn phím hệ thống**, nền `#F5F5FA` **[ASSUMPTION]**, cao 42dp, chứa 5 chuỗi
chèn nhanh chia đều theo chiều ngang, chữ đen ~16sp Medium. Nội dung đổi theo ngữ cảnh: ô rỗng →
`www.` `m.` `wap.` `.` `/` ; đã có nội dung → `.` `/` `.com` `.net` `.org`.

### 7.4 Nút tải nổi

Hình tròn đè lên nội dung trang web, góc dưới-phải, ngay trên thanh điều hướng đáy. 3 trạng thái:

| Trạng thái | Hình thức |
|---|---|
| Chưa tìm thấy video | Nền `#A3A5A9` **bán trong suốt** + mũi tên tải trắng — đo được từ ảnh gốc |
| Đã tìm thấy video **[THIẾT KẾ MỚI]** | Nền `#067EFF` **đặc** + mũi tên trắng + huy hiệu đếm `#E5484D` (16dp) ở góc trên-phải ghi số video; nảy nhẹ 1 lần khi vừa chuyển trạng thái |
| Đang tải **[THIẾT KẾ MỚI]** | Vòng tiến trình `#067EFF` dày 3dp chạy quanh viền, giữa là **%** chữ trắng 14sp Bold |

### 7.5 Khuôn trạng thái rỗng

Dùng chung mọi màn: minh hoạ **hộp giấy mở nắp** (`#FF7F00` thân / `#FFA300` nắp, kèm bóng đổ elip
màu be nhạt), kích thước **106×106dp**, căn giữa theo chiều ngang, đặt ở khoảng **55% chiều cao** vùng
nội dung; bên dưới cách **10dp** là 1 dòng chữ 16sp `#989DAF`.

### 7.6 Bảng nổi (menu ⋮ và menu của dòng lịch sử)

Nền trắng, bo 12dp, **không viền**, bóng đổ nhẹ; mỗi dòng cao 48dp gồm biểu tượng 24dp + nhãn 16sp
`#141518`, padding ngang trong bảng ~16dp. Bảng thả ra **bám mép phải** của nút vừa bấm, phủ lên nội
dung, không làm tối nền phía sau.

### 7.7 Bảng danh sách thẻ

Trượt từ **trái** (khác thói quen thường thấy là trượt từ phải), rộng 78% màn; phần màn còn lại phủ
`#666666` và **vẫn nhìn xuyên thấy** nội dung phía sau. Bảng có 3 vùng cố định: tiêu đề trên cùng
(nền `#EEF1FB`) — danh sách thẻ cuộn ở giữa (nền trắng) — `Favorite sites` + thanh 4 nút điều hướng
dưới cùng (nền `#EEF1FB`).

### 7.8 Màn nền tối (Video riêng tư)

Ngoại lệ duy nhất về nền: `#2A2A2B` cho nửa trên, `#222222` cho vùng bàn phím số — ranh giới giữa 2
vùng là đường chuyển màu **thẳng, không có kẻ**. Bàn phím số **không có viền ô, không có nền nút**,
chỉ có chữ số cỡ 32sp căn giữa từng ô 128×61dp.

### 7.9 Thẻ 1 mục tải

Khuôn dùng chung cho **cả 3 nơi**: tab `PROGRESS`, tab `FINISHED`, danh sách Video riêng tư — chỉ khác
phần thông tin ở dòng 3 và biểu tượng hành động bên phải.

```
┌──────────────────────────────────────────────┐  nền #F4F5FA, bo 12dp, cao 84dp
│ ┌────────┐  Tên tệp (16sp Medium, 1 dòng, …)  ✕│  lề ngang 14dp
│ │ 72×72  │  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬  │  thanh tiến trình cao 3dp, bo tròn
│ │ bo 8dp │  0 B/1,72 MB            🌐   ⬇     │  12sp #7D8089 · biểu tượng 24dp #067EFF
│ └────────┘  paused                            │
└──────────────────────────────────────────────┘
```

| Vùng | Tab `PROGRESS` | Tab `FINISHED` / Riêng tư |
|---|---|---|
| Ảnh thu nhỏ | Nền `#8BB1FA` + biểu tượng bảng phim trắng khi chưa có ảnh thật | Khung hình đầu của video + **nhãn thời lượng** nền đen 60% ở góc dưới-phải |
| Góc trên-phải | **✕** huỷ | **⋮** menu |
| Dòng 2 | Thanh tiến trình (rãnh `#D1D4E5`, phần đã tải `#067EFF`) | Không có |
| Dòng 3 trái | `<đã tải>/<tổng>` rồi xuống dòng là trạng thái | `<dung lượng> · <độ phân giải> · <ngày tải>` |
| Dòng 3 phải | 🌐 mở lại trang nguồn · ⬇/⏸ tiếp tục hoặc tạm dừng | Không có |

Trạng thái lỗi: chữ dòng 3 đổi sang `#E5484D`, biểu tượng phải đổi thành mũi tên xoay vòng.
Trạng thái chờ Wi-Fi: chữ dòng 3 đổi sang `#F5A524`.

### 7.10 Dòng ở màn Cài đặt

4 kiểu dòng, dùng chung 1 khuôn cao 56dp (72dp nếu có mô tả phụ), lề ngang 16dp, kẻ ngăn `#E7E7E7`:

| Kiểu | Bố cục | Ví dụ |
|---|---|---|
| Chỉ nhãn | Nhãn 16sp `#141518` căn trái | `Clear cache` |
| Nhãn + mô tả phụ | Nhãn ở trên, mô tả 12sp `#48494F` ở dưới | `Download location` + đường dẫn |
| Nhãn + công tắc | Nhãn trái, công tắc 52×32dp sát phải | `Download with Wi-Fi Only` |
| Nhãn + giá trị | Nhãn trái, giá trị 14sp `#067EFF` + mũi tên xuống sát phải | `Language options` + `Mặc định ▼` |

**Tiêu đề nhóm**: 14sp `#067EFF`, căn trái lề 16dp, cao 40dp, **không có** kẻ ngăn phía trên.
Kẻ ngăn giữa các dòng chạy **hết chiều ngang màn**, không thụt lề.

### 7.11 Chế độ chọn nhiều

Bật từ biểu tượng ba gạch ngang hoặc nhấn giữ 1 mục. Áp dụng cho: 2 tab Tải xuống, Video riêng tư,
Dấu trang.

| Thành phần | Hình thức |
|---|---|
| Thanh công cụ | **✕** thoát · tiêu đề `Đã chọn N` (20sp Bold) · biểu tượng **chọn tất cả** bên phải |
| Mục trong danh sách | Nút **✕**/**⋮** đổi thành **ô đánh dấu tròn** 24dp; đã chọn = nền `#067EFF` + dấu tích trắng, chưa chọn = viền `#D1D4E5` |
| Mục đã chọn | Nền thẻ đổi từ `#F4F5FA` sang `#E3EBFB` **[THIẾT KẾ MỚI]** |
| Thanh hành động | Trượt lên từ đáy, **đè lên** thanh điều hướng đáy, cao 56dp, nền trắng, kẻ ngăn phía trên; các hành động chia đều, mỗi hành động = biểu tượng 24dp + nhãn 12sp |
| Khi chưa chọn gì | Hành động mờ 38%, không bấm được |

## 8. Giá trị mặc định khuyến nghị (Recommended Defaults)

Dựng 1 màn mới đúng phong cách mà không cần đọc lại toàn bộ:

```
Nền màn                   #FFFFFF   (danh sách: #FAFAFA)
Bề mặt nhấn / ô nhập      #EEF1FB
Nền thẻ mục tải           #F4F5FA
Màu nhấn                  #067EFF
Màu lỗi / huy hiệu đếm    #E5484D
Chữ chính                 #141518
Chữ phụ                   #7D8EAF   (trong thẻ mục tải: #7D8089)
Kẻ ngăn cách              #E7E7E7

Padding ngang màn         16dp   (thẻ mục tải: 14dp — giữ nguyên, không làm tròn)
Thanh công cụ             56dp, nền trắng, tiêu đề 20sp Bold căn TRÁI (không căn giữa)
Thanh điều hướng đáy      56dp, 3 mục chia đều, chọn = xanh + chữ to hơn 2sp
Dòng danh sách            68dp, có kẻ ngăn cách, KHÔNG bo góc, KHÔNG thẻ nổi
Dòng Cài đặt              56dp (72dp nếu có mô tả phụ)
Thẻ mục tải               84dp, bo 12dp, ảnh thu nhỏ 72dp bo 8dp
Dòng menu nổi             48dp
Nút chính                 48dp cao, bo tròn hết, nền #067EFF, chữ 18sp Bold trắng
Nút phụ                   chữ trơn 16sp #7A8194, đặt dưới nút chính
Bảng/hộp thoại nổi        bo 12dp, nền trắng, không viền
Trạng thái rỗng           hộp giấy cam 106dp + chữ 16sp #989DAF
Huy hiệu đếm              16dp tròn #E5484D, chữ 10sp Bold trắng
Lưới                      bội số 4dp
```

**Hai khuôn dùng lại nhiều nhất** — dựng 1 lần rồi tái sử dụng, đừng viết lại ở từng màn:
1. **Thẻ 1 mục tải** (§7.9) — dùng ở 3 nơi: tab Progress, tab Finished, Video riêng tư.
2. **Dòng danh sách web** (68dp, tiêu đề + favicon + URL + ⋮) — dùng ở 3 nơi: Lịch sử, Dấu trang, gợi
   ý khi nhập địa chỉ.

**Ba điều KHÔNG làm** (để không lệch phong cách app gốc):
1. Không dùng thẻ nổi có bóng cho từng dòng danh sách — app này dùng danh sách phẳng có kẻ ngăn.
2. Không căn giữa tiêu đề thanh công cụ — luôn căn trái, sát lề 16dp.
3. Không dùng viền/gạch chân để đánh dấu "đang chọn" — chỉ đổi màu và đổi nền.
