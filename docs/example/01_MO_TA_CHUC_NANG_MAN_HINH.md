# vga52a-kotlin — Mô tả chức năng chi tiết từng màn hình (ví dụ mẫu)

> **Đây là tài liệu ví dụ (`docs/example/`)** — được sinh ra bằng cách đọc lại toàn bộ code thật của
> app **vga52a-kotlin** (Camera Studio: chụp ảnh/video + chỉnh sửa ảnh + ghép collage + tạo hình nền +
> thư viện) sau khi app đã build xong, để làm mẫu cho quy trình viết đặc tả chức năng cho một app MỚI
> trong tương lai. Xem tài liệu quy trình tổng quát ở [`../APP_SPEC_PROCESS.md`](../APP_SPEC_PROCESS.md).
>
> Nguyên tắc khi đọc/viết loại tài liệu này: **chỉ mô tả CHỨC NĂNG** (người dùng thấy gì, bấm vào làm
> gì, có những trạng thái/case đặc biệt nào) — **không nói về công nghệ/thư viện implement**
> (Compose, ViewModel, CameraX, Room...). Một app mới đọc xong tài liệu này phải hình dung đủ để tự
> chọn công nghệ khác và làm ra chức năng tương đương.
>
> Ảnh chụp màn hình đối chiếu: xem thư mục [`screenshot/`](screenshot/) (lấy từ `vga52a/screenshot`,
> là app tham chiếu gốc trước khi viết lại bằng Kotlin).

## Mục lục

1. [Intro (Onboarding)](#1-màn-hình-intro-onboarding--4-slide-giới-thiệu)
2. [Permission](#2-màn-hình-permission-xin-quyền-cameramicro)
3. [Home](#3-màn-hình-home-trang-chủ)
4. [Setting](#4-màn-hình-setting-cài-đặt)
5. [TakePhotoSuccessfully](#5-màn-hình-takephotosuccessfully-chụp-ảnhquay-video-thành-công)
6. [Camera — chế độ Ảnh](#6-màn-hình-camera--chế-độ-ảnh-photo-mode)
7. [Camera — chế độ Video](#7-màn-hình-camera--chế-độ-video)
8. [Camera — menu "More"](#8-camera--menu-more-menu-tuỳ-chọn-mở-rộng)
9. [VideoResult](#9-màn-hình-xem-lại-video-videoresult)
10. [ImportPhotoResult](#10-màn-hình-chi-tiết-ảnhvideo-importphotoresult)
11. [MediaPickerScreen (chọn ảnh dùng chung)](#11-mediapickerscreen--màn-chọn-ảnh-dùng-chung-edit-photo--collage-maker)
12. [EditPhotoScreen (editor chính)](#12-editphotoscreen--màn-editor-chỉnh-ảnh-chính)
13. [Tool: Frame / Text / Filter / Sticker / RemoveBG / Draw / Rotate / Save](#13-các-công-cụ-trong-edit-photo-editor)
14. [Collage Picker](#14-collage-picker-màn-chọn-ảnh-trước-khi-vào-collage-editor)
15. [Collage Maker Editor](#15-collage-maker--màn-editor-chính-collagemakerscreen)
16. [Wallpaper Templates](#16-wallpaper-templates--trang-chọn-mẫu-hình-nền-wallpapertemplatesscreen)
17. [Wallpaper Editor](#17-wallpaper-editor-wallpapereditorscreen)
18. [Gallery](#18-gallery--thư-viện-đã-lưu-galleryscreen-3-tab-photo--video--templates)
19. [Ghi chú chung / quy tắc dùng chung](#19-ghi-chú-chung-áp-dụng-nhiều-màn)

---

# PHẦN A — ONBOARDING, HOME, SETTING, SUCCESS

## 1. Màn hình Intro (Onboarding — 4 slide giới thiệu)

### 1.1 Mục đích màn hình
Giới thiệu nhanh 4 nhóm tính năng chính của app cho người dùng lần đầu mở app (chụp ảnh thông minh, chỉnh sửa ảnh, ghép ảnh collage, và tổng kết "sẵn sàng bắt đầu"), trước khi vào xin quyền và sử dụng app.

### 1.2 Các thành phần UI và nội dung hiển thị
- Toàn màn hình là một chuỗi 4 trang có thể vuốt ngang qua lại (dạng carousel/pager), mỗi trang gồm:
  - Phần trên: một ảnh minh hoạ lớn chiếm phần lớn chiều cao màn hình (ảnh cắt vừa khung).
  - Phần dưới ảnh: tiêu đề in đậm cỡ lớn, và một dòng phụ đề cỡ nhỏ màu xám mô tả thêm.
  - Nội dung 4 trang:
    1. "Smart Camera Tools" — "Zoom, grid, flash & pro settings to snap the perfect shot anytime."
    2. "Edit Made Simple" — "Add frames, filters, stickers & text — edit fast, fun, and easy."
    3. "Collage Maker" — "Mix photos, layouts to create memories that shine."
    4. "Get Started" — "Everything you need to capture, edit, and share — all in one app."
  - Có thể có một khối quảng cáo dạng native chèn ngay dưới phần tiêu đề/phụ đề của từng trang (chỉ hiện khi cấu hình từ xa bật quảng cáo cho vị trí đó và người dùng chưa mua gói premium).
- Bên dưới khu vực trang là dải chấm tròn chỉ số trang (dot indicator): chấm của trang đang xem dài hơn và có màu nhấn, các chấm còn lại nhỏ và màu xám nhạt.
- Dưới cùng là một nút bấm dạng viên thuốc (pill), rộng gần hết chiều ngang màn hình:
  - Ở 3 trang đầu: nút ghi "Continue".
  - Ở trang cuối (trang 4): nút đổi thành "Start".

### 1.3 Tương tác người dùng
- Vuốt ngang: chuyển qua lại giữa 4 trang.
- Bấm nút "Continue" (khi chưa ở trang cuối): tự động cuộn sang trang kế tiếp (không rời màn hình).
- Bấm nút "Start" (ở trang cuối): kết thúc Onboarding, điều hướng sang **màn Permission**. Sau khi rời Intro, người dùng không thể quay lại Intro bằng nút back nữa.

### 1.4 Trạng thái/case đặc biệt
- Nếu trang hiện tại có quảng cáo native inline và quảng cáo đó chưa tải xong: nút "Continue/Start" bị vô hiệu hoá (mờ) và thay bằng biểu tượng loading, tối đa 5 giây thì tự coi như sẵn sàng.
- Không bật quảng cáo cho trang đó: nút luôn sẵn sàng ngay lập tức.
- Người dùng đã mua Premium: không hiện quảng cáo nào trong Intro.

### 1.5 Điều hướng vào/ra
- Vào: màn khởi đầu cho người dùng cần xem Intro (dựa theo số lần mở app / nguồn cài đặt). Người dùng cũ không bị đưa vào màn này nữa.
- Ra: Intro → Permission (luôn luôn), khi bấm "Start" ở trang cuối.

---

## 2. Màn hình Permission (Xin quyền Camera/Micro)

### 2.1 Mục đích màn hình
Giải thích vì sao app cần quyền Camera và Micro, và tuần tự xin từng quyền một trước khi cho phép sử dụng tính năng chụp ảnh/quay video.

### 2.2 Các thành phần UI và nội dung hiển thị
- Tiêu đề lớn: "Permission".
- Ảnh minh hoạ tròn + đoạn mô tả căn giữa giải thích lý do cần quyền camera/thư mục thiết bị.
- Danh sách 2 dòng quyền dạng thanh bo tròn (viên thuốc), nền xám nhạt: tên quyền bên trái, trạng thái bên phải (công tắc gạt nếu chưa cấp / dấu tích tròn màu nhấn nếu đã cấp):
  1. "Camera"
  2. "Audio"
- Dòng chữ "Skip" màu xám, căn giữa, bấm được, nằm dưới danh sách quyền.
- Khối quảng cáo native ở cuối trang.

### 2.3 Tương tác người dùng
- Tự động xin quyền tuần tự khi vào màn: Camera trước, sau khi có phản hồi mới xin Audio (không hỏi cùng lúc).
- Bấm công tắc dòng "Camera"/"Audio" (khi chưa cấp) → mở hộp thoại xin quyền hệ thống tương ứng.
- Bấm "Skip" → bỏ qua, vào thẳng **Home** dù quyền có thể chưa được cấp.
- Khi CẢ HAI quyền đã cấp → tự động điều hướng sang **Home**, không cần bấm gì thêm.
- Nếu người dùng rời app vào Cài đặt hệ thống cấp quyền thủ công rồi quay lại (resume): trạng thái 2 dòng quyền tự cập nhật lại theo thực tế.

### 2.4 Trạng thái/case đặc biệt
- Quyền đã cấp: dấu tích tròn màu nhấn. Chưa cấp: công tắc gạt OFF.
- Đã cấp đủ 2 quyền: tự rời sang Home không cần bấm Skip.
- Từ chối quyền: dòng quyền vẫn ở trạng thái "chưa cấp", có thể bấm lại để xin lại hoặc Skip để bỏ qua.

### 2.5 Điều hướng vào/ra
- Vào: từ Intro (sau khi bấm Start) — luôn đi qua Permission trước Home. Cũng có thể là màn khởi đầu trực tiếp cho người dùng cũ chưa cấp đủ quyền camera/micro dù đã qua Intro.
- Ra: Permission → Home, khi (a) cấp đủ 2 quyền, hoặc (b) bấm "Skip". Không có đường quay lại Intro.

---

## 3. Màn hình Home (Trang chủ)

### 3.1 Mục đích màn hình
Trung tâm điều hướng của app — chọn chụp ảnh/quay video mới, vào các công cụ sáng tạo (chỉnh sửa ảnh, ghép ảnh, hình nền, thư viện), truy cập Cài đặt hoặc gói Premium.

### 3.2 Các thành phần UI và nội dung hiển thị
- Thanh trên cùng: tên app bên trái (đậm), bên phải 2 icon: vương miện (Premium, màu vàng) + bánh răng (Settings, màu xám).
- Khối "Hero" nổi bật ngay dưới thanh trên: nền ảnh tối (mô phỏng ống kính), phủ gradient tối dần từ trong suốt (trên) sang đen (dưới):
  - Nhãn nhỏ "PRO MODE" màu vàng góc trên-trái.
  - Tiêu đề lớn trắng: "Take Photo".
  - Phụ đề nhỏ trắng mờ: "Capture high-resolution professional shots".
  - Góc dưới-phải: nút tròn vàng chứa icon máy ảnh (shutter). Toàn bộ khối Hero bấm được, không chỉ riêng nút tròn.
- Tiêu đề nhóm "CREATIVE STUDIO" (nhỏ, đậm, xám) ngay dưới Hero.
- Lưới 2x2 gồm 4 thẻ (icon tròn nền vàng nhạt + tên chức năng dưới icon, nền thẻ xám nhạt bo góc, bấm được):
  1. "Edit Photo" (icon đũa phép)
  2. "Collage Maker" (icon lưới/grid)
  3. "Wallpaper" (icon hình nền)
  4. "My Gallery" (icon chồng ảnh)
- Cuối trang: khối quảng cáo native nhỏ.

### 3.3 Tương tác người dùng
- Bấm icon vương miện → mở luồng mua Premium (paywall IAP).
- Bấm icon bánh răng → **Setting**.
- Bấm khối Hero "Take Photo" (bất kỳ đâu trong khối) → có thể hiện quảng cáo interstitial trước → **Camera**.
- Bấm thẻ "Edit Photo" → (có thể interstitial trước) → màn **chọn ảnh** cho Edit Photo → editor.
- Bấm thẻ "Collage Maker" → (có thể interstitial trước) → màn **chọn nhiều ảnh** → editor ghép ảnh.
- Bấm thẻ "Wallpaper" → (có thể interstitial trước) → **danh sách mẫu Wallpaper**.
- Bấm thẻ "My Gallery" → (có thể interstitial trước) → **Gallery**.
- Quy tắc quảng cáo chung khi bấm bất kỳ mục nào ở trên: nếu interstitial thường hiện lên, sau khi đóng mới xét thêm 1 lớp "native toàn màn hình" dự phòng; nếu interstitial không hiện thì xét ngay lớp dự phòng. Điều hướng thật luôn xảy ra đúng 1 lần sau khi (các) quảng cáo xong/bị bỏ qua — không bao giờ bị chặn vĩnh viễn.

### 3.4 Trạng thái/case đặc biệt
- Đã mua Premium: không hiện quảng cáo nào (interstitial lẫn native), điều hướng ngay lập tức.
- Quảng cáo lỗi tải/bị tắt cấu hình: bỏ qua, không treo màn hình.
- Khối quảng cáo native cuối trang chỉ hiện khi được bật cấu hình + chưa mua Premium; mặc định không chiếm chỗ nếu không có quảng cáo.

### 3.5 Điều hướng vào/ra
- Vào từ: Permission (cấp đủ quyền/Skip), hoặc khởi đầu trực tiếp cho người dùng cũ; cũng là nơi quay về sau khi: chụp ảnh xong (qua màn Successfully), lưu xong ảnh chỉnh sửa, lưu xong ảnh ghép.
- Ra tới: Camera, Setting, Gallery, Edit Photo (qua Media Picker), Collage Maker (qua Media Picker), Wallpaper (danh sách mẫu), màn Premium (paywall).

---

## 4. Màn hình Setting (Cài đặt)

### 4.1 Mục đích màn hình
Đổi ngôn ngữ hiển thị, chia sẻ app, đánh giá app, mở gói Premium.

### 4.2 Các thành phần UI và nội dung hiển thị
- Thanh tiêu đề: nút back (trái) + "Settings" (căn trái cạnh back) + số phiên bản "v-<số>" (mép phải).
- Danh sách dòng cài đặt xếp dọc, mỗi dòng: thẻ bo góc, đổ bóng nhẹ, icon tròn nền vàng nhạt bên trái + tên mục giữa (đậm vừa) + mũi tên ">" bên phải:
  1. "Language" (icon quả địa cầu)
  2. "Share app" (icon chia sẻ)
  3. "Rate app" (icon ngôi sao)
  4. "Premium Package" (icon vương miện)
- Khối quảng cáo native dưới danh sách.

### 4.3 Tương tác người dùng
- Back → quay lại Home.
- "Language" → mở màn chọn ngôn ngữ dùng chung (cùng màn lúc chọn ngôn ngữ lần đầu mở app), áp dụng lại cho toàn app.
- "Share app" → mở share sheet hệ thống với nội dung soạn sẵn (tên app + link Play Store).
- "Rate app" → mở hộp thoại đánh giá NGAY trong app (không mở thẳng Play Store).
- "Premium Package" → mở luồng mua Premium.

**Hộp thoại "Rate app":**
- Tiêu đề dạng câu hỏi "Do you like it <tên app>?" căn giữa, đậm.
- Hàng 5 sao chọn được (1–5); sao chọn tô vàng, chưa chọn viền xám.
- Nút "Send" nền màu nhấn, chữ trắng đậm — chỉ bấm được khi đã chọn ≥1 sao.
- Bấm "Send": nếu ≥4 sao → đóng dialog + mở trang app trên Google Play (market://) để đánh giá công khai; nếu <4 sao → chỉ đóng dialog, KHÔNG chuyển Play Store.
- Có thể đóng dialog bằng bấm ra ngoài, không gửi gì.

### 4.4 Trạng thái/case đặc biệt
- Không lấy được số phiên bản: hiển thị "v-" thay vì crash.
- Context không hỗ trợ (hiếm): bấm "Language" không có tác dụng.
- Rate dialog: chưa chọn sao thì nút "Send" vô hiệu hoá.

### 4.5 Điều hướng vào/ra
- Vào: từ Home (icon bánh răng).
- Ra: về Home (back), hoặc mở overlay/màn phụ: màn chọn ngôn ngữ, share sheet hệ thống, Rate dialog (ở lại Setting), màn Premium, trang Play Store (rate ≥4 sao).

---

## 5. Màn hình TakePhotoSuccessfully (Chụp ảnh/quay video thành công)

### 5.1 Mục đích màn hình
Xác nhận ảnh/video vừa chụp đã lưu thành công, trước khi quay lại trang chủ.

### 5.2 Các thành phần UI và nội dung hiển thị
- Nội dung căn giữa dọc + ngang: hình minh hoạ "thành công" lớn, tiêu đề đậm màu xanh lá "Success!", mô tả nhỏ xám căn giữa "Congratulations! Your image has been successfully modified", khối quảng cáo native, dòng "Back to home" màu nhấn đậm bấm được ở dưới cùng.

### 5.3 Tương tác người dùng
- Bấm "Back to home" → về **Home**, xoá sạch lịch sử điều hướng phía sau (không back lại được Camera/màn này nữa).
- Không có nút back cứng nào khác.

### 5.4 Trạng thái/case đặc biệt
- Màn tĩnh, không loading/lỗi/rỗng. Dùng chung 1 nội dung cho cả chụp ảnh và quay video (không phân biệt).

### 5.5 Điều hướng vào/ra
- Vào từ: Camera (chụp ảnh xong), hoặc VideoResult (lưu video xong) — cả hai đều xoá bớt lịch sử về tới Home trước khi vào màn này.
- Ra: duy nhất 1 lối, "Back to home" → Home, xoá sạch lịch sử trước đó.

---

# PHẦN B — CAMERA, VIDEO RESULT, CHI TIẾT ẢNH/VIDEO

## 6. Màn hình Camera — chế độ Ảnh (Photo Mode)

### 6.1 Mục đích màn hình
Màn chụp ảnh chính: ngắm khung hình qua camera thật, tuỳ chỉnh màu sắc/độ sáng/zoom/tỉ lệ ngay trên khung ngắm, bấm nút chụp để lưu ảnh — chụp liên tục nhiều tấm không cần rời màn hình.

### 6.2 Các thành phần UI và nội dung hiển thị

**Khung ngắm (toàn màn hình, nền đen):**
- Camera thật theo thời gian thực (camera sau mặc định).
- Chưa cấp quyền Camera: nền đen + dòng chữ trắng căn giữa xin quyền; hệ thống tự bật hộp thoại xin quyền ngay khi vào màn.
- Đã chọn tỉ lệ khung hình cụ thể (không phải "Full"): khung ngắm co lại đúng tỉ lệ, phần còn lại là dải đen — đúng phần hiển thị sẽ là phần được lưu vào ảnh.
- Có thể bật lưới caro 3x3 phủ lên khung ngắm (bật/tắt qua menu More).

**Thanh trên cùng** (nền đen mờ, 6 nút tròn/nhãn từ trái sang phải):
1. Nút "X" — đóng màn Camera, quay lại màn trước.
2. Chip tỉ lệ khung hình (mặc định "Full") — mở/đóng bảng chọn tỉ lệ.
3. Nút Flash — bật/tắt đèn flash (vàng khi bật).
4. Nút hẹn giờ — icon đồng hồ (tắt) hoặc số giây 3/5/10 màu vàng (bật).
5. Nút "More" (ba chấm) — mở/đóng menu mở rộng.

**Bảng chọn tỉ lệ** (xổ dưới thanh trên): "Full", "16:9", "4:3", "1:1" — tỉ lệ đang chọn tô vàng, chọn xong tự đóng.

**Menu "More"** (chi tiết mục 8): chế độ Ảnh gồm 4 mục Grid/Focus/Resolution/Collage.

**Thanh công cụ chỉnh màu** (trên thanh dưới cùng, chỉ 1 bảng mở tại 1 thời điểm) — 5 nút: "AMP" (tăng sáng tự động), "WB" (cân bằng trắng), mức zoom hiện tại (vd "1x"), phơi sáng thủ công, bộ lọc màu (Filter). Nút đang mở bảng tô vàng.
- Vào màn Camera: bảng **Filter** tự mở sẵn.
- **AMP**: thanh trượt 0–100%, bong bóng % nổi trên con trượt, nút "X" đóng + nút "A" reset về 0. Kéo làm ảnh sáng lên + tăng nhẹ bão hoà màu.
- **WB**: lưới 7 ô cố định 2 hàng (4+3): Auto, Sunny, Cloudy, Shade, Tungsten, Fluorescent, Flash.
- **Filter**: nhóm theo chủ đề — "Popular" (None/Warm/Cool/Vivid/B&W), "Adventure" (None/Forest/Desert/Arctic), "Blue Shadow" (None/Midnight/Ocean/Steel), "Retro" (None/Sepia/Faded/Vintage). Mỗi filter hiển thị ảnh mẫu thu nhỏ cố định (không phải chụp trực tiếp khung hình).
- **Exposure**: thanh trượt -100% đến 100% (0 = giữ nguyên), đóng + reset về 0. Dương sáng lên, âm tối đi (tuyến tính, không đổi bão hoà).
- **Zoom**: thanh trượt 1–100 (thang riêng, quy đổi ra mức zoom thật tối đa máy hỗ trợ), nhãn "Nx", đóng + reset về 1x.
- 4 hiệu ứng màu (WB + Filter + AMP + Exposure) cộng dồn thành 1 hiệu ứng duy nhất, áp cả khung ngắm lẫn ảnh chụp — màu khung ngắm khớp chính xác ảnh đã lưu.

**Hàng chuyển chế độ:** "PHOTO"/"VIDEO" cạnh nhau; chế độ chọn trắng đậm, còn lại mờ. Ẩn khi đang ở chế độ Collage live-shoot.

**Hàng dưới cùng (nút chụp):**
- Trái: ô vuông nhỏ hiển thị ảnh/khung hình đại diện lần chụp gần nhất (chỉ hiện nếu đã từng chụp).
- Giữa: nút chụp tròn lớn, viền trắng nền trắng (đỏ khi đang quay video).
- Phải: nút đảo camera trước/sau.

**Vòng xoay loading:** hiện giữa màn khi ảnh vừa chụp đang xử lý/lưu; nút chụp vô hiệu hoá tạm thời.

**Đếm ngược:** khi hẹn giờ bật, sau khi bấm chụp toàn màn tối mờ, số lớn trắng đếm ngược về 1 rồi mới thực chụp/quay.

### 6.3 Tương tác người dùng
- "X" → quay lại màn trước.
- Chip tỉ lệ → mở/đóng bảng tỉ lệ (tự đóng menu More nếu đang mở).
- Chọn tỉ lệ → khung ngắm/ảnh chụp co theo; bảng tự đóng.
- Flash → bật/tắt cho lần chụp tiếp theo.
- Hẹn giờ → chuyển tuần tự Tắt → 3s → 5s → 10s → Tắt.
- "More" → mở/đóng menu mở rộng (tự đóng bảng tỉ lệ nếu đang mở).
- 1 trong 5 nút chỉnh màu → mở bảng tương ứng; bấm lại → đóng; mở bảng khác tự đóng bảng cũ và cả bảng tỉ lệ/menu More (chỉ 1 nhóm mở tại 1 thời điểm toàn màn).
- Kéo/chạm thanh trượt (AMP/Exposure/Zoom) → cập nhật tức thời.
- Nút "A" trên thanh trượt → về mặc định.
- "PHOTO"/"VIDEO" → chuyển chế độ, tự đóng mọi bảng/menu.
- Ô thumbnail gần nhất (nếu có) → mở **ImportPhotoResult** đúng file đó.
- Nút chụp: hẹn giờ bật → đếm ngược rồi chụp; tắt → chụp ngay. Lưu xong, ô thumbnail cập nhật, ở lại Camera để chụp tiếp. Vô hiệu hoá tạm thời trong lúc xử lý.
- Đảo camera → chuyển trước/sau, khung ngắm khởi động lại chốc lát.

### 6.4 Trạng thái/case đặc biệt
- **Chưa cấp quyền:** không hiện khung ngắm, chỉ thông báo xin quyền; tự bật hộp thoại ngay khi vào.
- **Đổi tỉ lệ:** khung ngắm co theo tỉ lệ, khớp đúng phần cắt vào ảnh lưu.
- **Bật lưới caro:** 2 đường dọc + 2 đường ngang chia 9 ô.
- **Chế độ Collage live-shoot** (vào từ More → Collage → chọn 1/7 bố cục: 1 ô, 2 cột dọc, 2 hàng ngang, lưới 4 ô, 3 cột dọc, 3 hàng ngang, lưới 9 ô):
  - Khung ngắm chia theo bố cục; ô ĐANG CHỜ lộ camera thật; ô ĐÃ CHỤP hiển thị phần đã chụp (tĩnh); ô CHƯA TỚI LƯỢT che đen; viền trắng mỏng phân tách các ô.
  - Mỗi lần chụp: chụp 1 tấm full khung, tự cắt đúng phần ứng với ô đang chờ, điền vào ô đó.
  - Đủ số ô → tự ghép thành 1 ảnh vuông hoàn chỉnh, lưu như ảnh chụp bình thường (không phải mục "Templates" riêng).
  - Hàng "PHOTO/VIDEO" bị ẩn trong chế độ này.
- **Focus (menu More):** Auto/Center có tác dụng lấy nét/đo sáng thật; Detect/Infinity/Macro chỉ đổi icon hiển thị, chưa có tác dụng quang học thật (giới hạn camera thiết bị).
- **Resolution (menu More):** FHD (1920x1080), 2K (2560x1440), 4K (3840x2160, có icon khoá premium). Đổi mức làm khung ngắm khởi động lại chốc lát; áp cho cả ảnh lẫn video.
- **Vào từ luồng chọn ảnh** (Edit Photo/Collage Maker/Wallpaper, nút "+"): chụp xong 1 tấm → Camera tự đóng ngay, quay lại đúng màn chọn ảnh (không ở lại chụp liên tục). Chuyển sang Video hoặc bấm "gần nhất" trong luồng này cũng chỉ đóng Camera quay lại màn chọn ảnh, không cho kết quả dùng được.
- **Ghi chú kỹ thuật-chức năng quan trọng:** hiệu ứng màu (AMP/WB/Filter/Exposure) chỉ áp thật cho ẢNH CHỤP, KHÔNG ghi vào file VIDEO (video giữ màu gốc cảm biến, chỉ khung ngắm lúc quay có hiệu ứng). Flash cũng chỉ có tác dụng khi chụp ảnh, không bật khi quay video.

### 6.5 Điều hướng vào/ra
- Vào từ: Home (bấm "Camera", có thể interstitial trước) → chế độ Ảnh, chụp liên tục không tự thoát.
- Vào từ (luồng phụ): màn chọn ảnh Edit Photo/Collage Maker/Wallpaper (nút "+") → mở Camera ngay trong màn đó, chụp xong 1 tấm tự đóng quay lại màn chọn ảnh.
- Ra: "X" → quay lại màn trước (Home hoặc màn chọn ảnh).
- Ra (hoàn tất ghép Collage): tự chuyển sang "Chụp ảnh thành công", xoá Home khỏi lịch sử trước nó.
- Ra (chụp ảnh đơn từ Home): không điều hướng đi đâu — ở lại Camera.
- Ra (bấm thumbnail gần nhất): sang ImportPhotoResult với file vừa chụp.

---

## 7. Màn hình Camera — chế độ Video

### 7.1 Mục đích màn hình
Cùng 1 màn Camera, chuyển sang quay video: ngắm khung hình, chỉnh màu/zoom/hẹn giờ như chế độ ảnh, bấm nút bắt đầu/dừng quay 1 đoạn video, quay liên tục nhiều đoạn không rời màn.

### 7.2 Khác biệt so với chế độ Ảnh
- **Bảng chọn tỉ lệ:** chỉ còn "Full" và "16:9" — video luôn giữ tỉ lệ gốc 16:9 cảm biến, không cắt/chuyển đổi pixel thật như ảnh.
- **Menu "More":** chỉ còn 2 mục Focus và Resolution — không có Grid, không có Collage ở chế độ Video.
- **Nút chụp:** đổi màu đỏ đặc khi đang quay (thay vì trắng khi nghỉ).
- **Ô thumbnail gần nhất:** hiển thị 1 khung hình trích từ video gần nhất.

### 7.3 Tương tác người dùng
- Bấm "VIDEO" → chuyển chế độ quay video; nếu đang chọn 4:3/1:1 thì tự đưa về "Full".
- Bấm nút chụp lần đầu: hẹn giờ bật → đếm ngược rồi quay; tắt → quay ngay, nút chuyển đỏ.
- Bấm lại (đang quay) → dừng ngay lập tức (không áp dụng đếm ngược cho thao tác dừng).
- Flash/Hẹn giờ/AMP/WB/Zoom/Exposure/Filter/Focus/Resolution/đảo camera hoạt động y hệt chế độ Ảnh.

### 7.4 Trạng thái/case đặc biệt
- Đang quay: nút chụp đỏ; các tuỳ chỉnh khác vẫn thao tác được.
- Đổi tỉ lệ khi ở Video: chỉ Full hoặc 16:9.
- Dừng quay: video tự lưu vào bộ sưu tập, ở lại Camera để quay tiếp; thumbnail cập nhật khung hình mới nhất.
- Flash không có tác dụng khi quay video.
- Hiệu ứng màu chỉ hiện trên khung ngắm lúc quay, KHÔNG ghi/bake vào file video đã lưu (video giữ màu gốc thật).
- Đổi độ phân giải áp dụng luôn cho chất lượng video quay.
- Focus Detect/Infinity/Macro cũng chỉ là lựa chọn hiển thị, tương tự chế độ Ảnh.
- **Ghi chú quan trọng (khác thiết kế ban đầu):** hệ thống điều hướng có sẵn đường dẫn để mở "VideoResult" (xem lại trước khi lưu) ngay sau khi dừng quay — nhưng luồng thực tế hiện tại LUÔN tự lưu thẳng video và ở lại Camera, KHÔNG gọi tới VideoResult. Màn VideoResult vẫn hoạt động đúng chức năng nếu được điều hướng tới trực tiếp bằng route của nó, nhưng hiện đang "mồ côi" với luồng quay-video-trong-Camera.

### 7.5 Điều hướng vào/ra
- Giống hệt chế độ Ảnh (cùng 1 màn, chỉ đổi trạng thái nội bộ) — xem mục 6.5.
- Route `Camera → VideoResult` tồn tại trong hệ thống điều hướng nhưng không được kích hoạt từ thao tác dừng quay hiện tại (xem ghi chú 7.4).

---

## 8. Camera — Menu "More" (menu tuỳ chọn mở rộng)

### 8.1 Mục đích
Gom các tuỳ chọn nâng cao ít dùng hơn (lưới bố cục, cách lấy nét, độ phân giải, chụp ghép ảnh) để không làm rối thanh công cụ chính.

### 8.2 Các thành phần UI và nội dung hiển thị
- Hàng icon + nhãn nhỏ, xổ dưới thanh trên khi bấm "More":
  - Chế độ Ảnh: **Grid, Focus, Resolution, Collage** (4 mục).
  - Chế độ Video: chỉ **Focus, Resolution** (2 mục).
- Bấm 1 mục (trừ Grid) xổ thêm 1 hàng lựa chọn con:
  - **Focus:** 5 icon Auto, Center, Detect, Infinity, Macro.
  - **Resolution:** 3 thẻ FHD/2K/4K (kèm kích thước pixel; 4K có icon vương miện).
  - **Collage:** lưới 7 icon bố cục (5 hàng trên + 2 hàng dưới căn giữa).

### 8.3 Tương tác người dùng
- "More" → mở/đóng menu, tự đóng bảng tỉ lệ + bảng chỉnh màu nếu đang mở.
- "Grid" → bật/tắt lưới caro ngay, không mở hàng con, tự thu gọn.
- "Focus/Resolution/Collage" → mở hàng con tương ứng; bấm lại đóng.
- Chọn 1 focus mode → đổi ngay (tô vàng); Auto/Center có tác dụng thật, còn lại chỉ đổi hiển thị.
- Chọn 1 resolution → khung ngắm khởi động lại ngắn.
- Chọn 1 bố cục Collage → menu tự đóng, Camera chuyển "Collage live-shoot".

### 8.4 Trạng thái/case đặc biệt
- Chỉ 1 trong 2 nhóm (bảng tỉ lệ+More ở trên) HOẶC (5 bảng chỉnh màu ở dưới) mở tại 1 thời điểm.
- Nội dung More đổi theo chế độ Ảnh/Video.

### 8.5 Điều hướng vào/ra
- Không phải màn riêng — khối UI xổ/thu ngay trong Camera, không rời màn.

---

## 9. Màn hình Xem lại Video (VideoResult)

### 9.1 Mục đích màn hình
Xem lại đoạn video vừa quay trước khi quyết định giữ (xác nhận) hay huỷ.

### 9.2 Các thành phần UI và nội dung hiển thị
- Thanh trên: "X" (huỷ) trái, "RESULT" giữa, "✓" (xác nhận) phải.
- Vùng giữa (nền đen): trình phát video hiển thị đúng đoạn vừa quay, không có thanh điều khiển hệ thống đè lên.
- Nút tròn Play/Pause lớn nổi giữa vùng phát, đổi icon theo trạng thái.

### 9.3 Tương tác người dùng
- "X" → dừng phát, quay lại màn trước (huỷ).
- "✓" → dừng phát, xác nhận → sang "Chụp ảnh thành công".
- Nút Play/Pause giữa màn → phát tiếp/tạm dừng.
- Video tự chuẩn bị và có thể phát ngay khi vào màn.

### 9.4 Trạng thái/case đặc biệt
- Trình phát tự giải phóng tài nguyên khi rời màn (xác nhận hay huỷ).
- **Quan trọng:** hiện KHÔNG được gọi tới từ luồng quay video thực tế trong Camera (xem mục 7.4) — vẫn hoạt động đầy đủ nếu điều hướng tới trực tiếp.

### 9.5 Điều hướng vào/ra
- Vào: thiết kế để vào từ Camera sau khi dừng quay (kèm đường dẫn video) — hiện chưa kích hoạt trong luồng thực tế.
- Ra (huỷ): quay lại Camera.
- Ra (xác nhận): sang "Chụp ảnh thành công", xoá các màn từ Home tới đó khỏi lịch sử.

---

## 10. Màn hình Chi tiết Ảnh/Video (ImportPhotoResult)

### 10.1 Mục đích màn hình
Xem đầy đủ 1 ảnh/video đã lưu, cùng thao tác: chia sẻ, yêu thích, xem thông tin file, chỉnh sửa (chỉ ảnh), tải về bộ nhớ công khai máy, hoặc xoá.

### 10.2 Các thành phần UI và nội dung hiển thị
- Thanh tiêu đề: back trái, tiêu đề "Video"/"Ảnh" giữa tuỳ loại, nút tròn nền xanh dương icon Chia sẻ phải.
- Vùng xem nội dung: ảnh co giãn vừa khung không cắt; video có đầy đủ thanh điều khiển hệ thống.
- Hàng nút thao tác dưới cùng (icon + nhãn nhỏ, dàn đều):
  - **Delete** (thùng rác)
  - **Favorite** (trái tim rỗng/đặc đỏ tuỳ trạng thái)
  - **Info** (chữ "i")
  - **Edit** (bút chì) — CHỈ hiện với ẢNH
  - **Download** (mũi tên xuống)

### 10.3 Tương tác người dùng
- Back → đóng màn, quay lại nơi đã mở tới.
- Chia sẻ → mở share sheet hệ thống kèm file.
- Play/Pause/tua (video) → điều khiển phát lại thông thường.
- **Delete** → dialog xác nhận (tiêu đề/nội dung đổi theo ảnh/video); Xoá → xoá khỏi bộ sưu tập + tự quay lại màn trước; Huỷ → đóng dialog, không xoá.
- **Favorite** → bật/tắt ngay, không cần xác nhận.
- **Info** → dialog 3 dòng: Tên file, Dung lượng (KB), Ngày giờ sửa gần nhất; nút "Cancel" đóng.
- **Edit** (chỉ ảnh) → mở Edit Photo Editor với ảnh này làm nguồn.
- **Download** → lưu thêm 1 bản sao vào bộ nhớ Ảnh/Video công khai của máy.

### 10.4 Trạng thái/case đặc biệt
- Trạng thái yêu thích lấy từ danh sách media đã lưu, khớp theo đường dẫn file.
- Xoá thành công: màn tự đóng, không còn cách xem lại file đã xoá.
- Download: app ghi nhớ liên kết bản sao công khai với file gốc — nếu file gốc bị xoá trong app, bản sao công khai cũng được dọn theo (tránh file "mồ côi").
- Video: không có nút Edit.
- Info: đọc dữ liệu trực tiếp từ file thật tại thời điểm mở dialog.

### 10.5 Điều hướng vào/ra
- Vào từ: ô thumbnail "gần nhất" trong Camera; 1 mục trong Home; 1 mục trong Gallery.
- Ra: back/xoá thành công → quay lại đúng màn đã mở tới (Camera/Home/Gallery); Edit (chỉ ảnh) → Edit Photo Editor; Chia sẻ → share sheet hệ thống (không rời app).

---

# PHẦN C — EDIT PHOTO VÀ CÁC CÔNG CỤ DÙNG CHUNG

## 11. MediaPickerScreen — Màn chọn ảnh dùng chung (Edit Photo & Collage Maker)

### 11.1 Mục đích
Chọn 1 ảnh (Edit Photo) hoặc nhiều ảnh (Collage Maker) làm nguồn đầu vào, lấy từ ảnh do chính app tạo ra, ảnh có sẵn trên máy, hoặc chụp ảnh mới ngay tại chỗ.

### 11.2 Các thành phần UI và nội dung hiển thị
- **Thanh tiêu đề:** back trái, tên màn giữa ("Edit Photo" hoặc "Collage Maker" tuỳ nơi gọi); chế độ chọn nhiều ảnh có thêm icon dấu tick góc phải (vàng khi đã chọn ≥1 ảnh, xám/không bấm được khi chưa chọn).
- **Mục "ẢNH TỪ ỨNG DỤNG":** tiêu đề hoa đậm + icon dấu cộng tròn (mở Camera chụp mới) cùng hàng bên phải; lưới 3 cột ảnh do app tạo trước đó. Rỗng: chữ nghiêng giữa "Chưa có ảnh nào do ứng dụng tạo".
- **Mục "ẢNH TRONG MÁY":** tiêu đề hoa đậm; đang cấp quyền một phần có thêm icon dấu cộng xin thêm quyền. Theo trạng thái quyền:
  - Chưa có quyền: khối bo góc nền xám "Cho phép truy cập ảnh trên máy", chạm để xin quyền.
  - Có quyền nhưng máy không có ảnh: chữ nghiêng "Không có ảnh nào trên máy".
  - Có ảnh: lưới 3 cột từ thư viện thiết bị.
- **Ô ảnh:** vuông, bo góc nhẹ; đang chọn có chấm tròn vàng + dấu tick góc trên-phải.
- Khối quảng cáo native cuối danh sách (sau cả 2 mục).
- **"Selected: N"** (chỉ chế độ nhiều ảnh): chữ vàng đậm, giữa, cố định đáy màn, đếm số ảnh đang chọn.
- **Chụp ảnh mới:** bấm dấu cộng ở "Ảnh từ ứng dụng" → toàn bộ nội dung màn thay bằng Camera đầy đủ (không mở route mới); chụp xong/back tự đóng Camera, hiện lại đúng màn chọn ảnh với trạng thái chọn cũ còn nguyên.

### 11.3 Tương tác người dùng
- **Chọn 1 ảnh (Edit Photo):** chạm bất kỳ ảnh nào → xác nhận ngay, sang Editor với ảnh đó, không cần bước xác nhận riêng.
- **Chọn nhiều ảnh (Collage Maker):** chạm 1 ảnh để bật/tắt chọn (không điều hướng ngay); chạm icon tick ở tiêu đề để xác nhận toàn bộ danh sách.
- Dấu cộng ở "Ảnh từ ứng dụng" → mở Camera chụp mới, ảnh chụp xuất hiện lại ở mục này.
- Khối "Cho phép truy cập ảnh" → dialog quyền hệ thống.
- Dấu cộng cạnh "Ảnh trong máy" (quyền một phần) → mở lại dialog quyền để cấp thêm/cấp toàn bộ.
- Back ở tiêu đề → thoát, về màn trước (Home).

### 11.4 Trạng thái/case đặc biệt
- Quyền truy cập ảnh máy có 3 mức: chưa cấp, cấp một phần (Android 14+), cấp toàn bộ — giao diện tự đổi theo đúng mức.
- Danh sách ảnh máy chỉ truy vấn lại khi quyền/lựa chọn ảnh thay đổi.
- Không có mục "Ảnh mẫu" (Sample photo) — chỉ còn 2 mục "Ảnh từ ứng dụng" và "Ảnh trong máy".
- Nút xác nhận (chọn nhiều) luôn hiển thị nhưng mờ khi chưa chọn ảnh nào.

### 11.5 Điều hướng vào/ra
- Vào: Home → "Edit Photo" (có thể interstitial) → MediaPickerScreen(title="Edit Photo", multiSelect=false). Hoặc Home → "Collage Maker" → MediaPickerScreen(title="Collage Maker", multiSelect=true).
- Ra (Edit Photo): chọn 1 ảnh → EditPhotoScreen — màn chọn ảnh KHÔNG bị gỡ khỏi lịch sử (đóng Editor bằng X sẽ quay lại đúng màn chọn ảnh này, không rơi thẳng về Home).
- Ra (Collage Maker): xác nhận danh sách → Collage Editor với toàn bộ ảnh đã chọn.
- Ra (chung): back ở tiêu đề → Home.

---

## 12. EditPhotoScreen — Màn Editor chỉnh ảnh chính

### 12.1 Mục đích
Chỉnh sửa 1 ảnh đã chọn: thêm khung viền, chữ, hiệu ứng màu, nhãn dán, xoá nền, vẽ tay, xoay ảnh — rồi lưu kết quả.

### 12.2 Các thành phần UI và nội dung hiển thị
- Nền màn: đen tuyền.
- Thanh trên cùng: icon đóng "X" trái; nút "Save" viên thuốc nền vàng chữ đen đậm, phải.
- **Vùng canvas** (gần hết màn), các lớp chồng từ dưới lên: ảnh gốc (co giãn giữ tỉ lệ, không cắt) → hiệu ứng màu (nếu có) → nét vẽ tay đã vẽ → khung viền (nếu có, phủ kín ảnh) → các lớp chữ/nhãn dán kéo-thả (đang chọn: viền vàng mảnh + 2 tay cầm tròn — trên-trái dấu X để xoá, dưới-phải icon sao chép để nhân bản).
- **Thanh công cụ dưới cùng** (chỉ hiện khi chưa mở tool nào): nền đen, 7 nút cách đều, mỗi nút icon nhỏ + nhãn chữ rất nhỏ: **Frame, Text, Filter, Sticker, Rotate, RemoveBG, Draw**. Nút đang bật màu vàng, còn lại trắng.
- **Panel công cụ:** khi mở 1 tool, panel thay thế vị trí thanh công cụ, đè lên phía dưới ảnh (ảnh không bị co/đẩy lên).
- **Lớp phủ xử lý:** đang xoá nền hoặc đang lưu → toàn màn tối ~50% + vòng xoay loading trắng giữa màn.

### 12.3 Tương tác người dùng
- **X (đóng):** đang mở 1 tool → chỉ đóng tool đó (bỏ chọn lớp, về thanh công cụ chính), KHÔNG thoát màn; không có tool nào mở → thoát hẳn Editor, về màn trước.
- **Save:** chụp lại đúng những gì hiển thị trên canvas (ảnh + hiệu ứng + khung + toàn bộ chữ/nhãn dán + nét vẽ, không kèm viền chọn) → lưu JPEG chất lượng cao vào bộ nhớ riêng app + 1 bản vào thư viện ảnh máy + ghi vào danh sách "ảnh do app tạo" → thông báo "Đã lưu vào Gallery" → điều hướng tiếp. Đang lưu: Save vô hiệu hoá.
- **Frame/Filter/Sticker/Draw:** mở panel tương ứng (chi tiết mục 13).
- **Text:** chưa có lớp chữ nào chọn → tạo lớp mới "Text" giữa ảnh + mở luôn thanh công cụ chữ; đã chọn 1 lớp → chỉ mở lại thanh công cụ cho lớp đó.
- **Rotate:** xoay ngay ảnh gốc thêm 90° mỗi lần chạm, tức thì, không cần xác nhận.
- **RemoveBG:** gửi ảnh hiện tại cho dịch vụ xoá nền, hiện loading; xong thì thay ảnh nền bằng kết quả.
- **Chạm/kéo/chụm 2 ngón** trên lớp chữ/nhãn dán: chọn (viền vàng + 2 tay cầm); kéo di chuyển; chụm/xoè+xoay để phóng to-thu nhỏ (0.3×–4×) và xoay.
- Tay cầm trên-trái (X): xoá lớp. Tay cầm dưới-phải (sao chép): nhân bản lớp, đặt lệch nhẹ.

### 12.4 Trạng thái/case đặc biệt
- Ảnh nguồn có thể là: ảnh mẫu đóng gói sẵn, ảnh từ thư viện thiết bị, hoặc ảnh đã lưu trước đó bởi chính app — màn tự nhận diện đúng nguồn. Đọc ảnh thất bại: canvas trống, thao tác vẫn hiện nhưng không có ảnh nền.
- Đang xoá nền HOẶC đang lưu: khoá tương tác toàn màn bằng lớp phủ + loading.
- Lưu thất bại: thông báo "Lưu ảnh thất bại, thử lại", tắt trạng thái lưu, ở lại Editor để thử lại.
- Rotate chỉ xoay ảnh nền — các lớp chữ/nhãn dán/nét vẽ giữ nguyên vị trí/góc riêng, KHÔNG xoay theo.
- **Không có công cụ Crop (cắt ảnh)** — chỉ có Rotate (xoay 90°).
- **RemoveBG hiện là bản giả lập tạm thời:** trả về nguyên ảnh gốc sau ~1.2 giây trễ giả lập, chưa nối dịch vụ xoá nền thật — chỉ mới có luồng giao diện (loading, thay ảnh khi xong). Thất bại: ảnh giữ nguyên, không có thông báo lỗi.

### 12.5 Điều hướng vào/ra
- Vào: MediaPickerScreen (chọn 1 ảnh, Edit Photo) → EditPhotoScreen(sourcePath). Lối vào khác: màn "Kết quả ảnh" (ImportPhotoResult) qua nút Edit → cũng mở EditPhotoScreen(sourcePath) với đúng ảnh đó.
- Ra (huỷ): X khi không mở tool nào → quay lại màn trước trong lịch sử (thường MediaPickerScreen vì route không bị gỡ).
- Ra (lưu thành công): thẳng về Home, xoá sạch lịch sử phía sau (kể cả màn chọn ảnh) — back sau đó không quay lại Editor/màn chọn ảnh được nữa.

---

## 13. Các công cụ trong Edit Photo Editor

### 13.1 Tool: Frame (Khung viền)
**Mục đích:** phủ khung trang trí (hoạ tiết, ruy băng, bóng bay, hoa văn...) quanh mép ảnh.
**UI:** tiêu đề panel "Frame" + icon X đóng. Tab thể loại cuộn ngang: **Party, Travel, Floral, Cute, Vintage** (chọn tô vàng). Lưới thumbnail 5 cột theo thể loại; ô đầu tiên cố định = icon "cấm" (không dùng khung).
**Tương tác:** chạm tab → đổi thể loại; chạm ô "cấm" → bỏ khung; chạm 1 mẫu → áp ngay lên canvas thật, panel vẫn mở để đổi tiếp; X → đóng panel, giữ khung đã chọn.
**Đặc biệt:** thể loại không có mẫu nào → lưới chỉ còn ô "cấm".
**Điều hướng:** vào từ thanh công cụ chính (chạm "Frame"); ra bằng X → về thanh công cụ chính.

### 13.2 Tool: Text (Chữ)
**Mục đích:** thêm dòng chữ tuỳ chỉnh, kéo-thả tự do, đổi nội dung/font/cỡ/màu/đậm-nghiêng/căn lề.
**UI:** không có nút đóng/tiêu đề panel riêng (đóng qua X ở thanh trên Editor). 2 tầng:
- Tầng phụ (hiện khi 1 mục tầng chính bật, có X riêng đóng tầng phụ):
  - **Edit:** ô nhập văn bản trực tiếp, hiện sẵn khi vừa tạo lớp mới.
  - **Font:** danh sách tên font cuộn ngang, mỗi tên hiển thị đúng bằng font đó.
  - **Size:** thanh trượt 12–72.
  - **Colors:** 8 ô màu tròn (trắng, đen, đỏ cam, vàng, xanh lá, xanh dương, tím, hồng).
  - **Style:** 2 icon Đậm/Nghiêng, bật/tắt độc lập.
  - **Align:** 3 icon trái/giữa/phải.
  - **ARC:** thanh trượt bẻ cong chữ -100%–100%. **Chỉ dùng ở Collage Maker — Edit Photo KHÔNG bật ARC** (`showArc = false`).
- Tầng chính (luôn hiện): hàng icon nhỏ + nhãn — **Edit, Font, Size, Colors, Style, Align, (ARC nếu bật), Delete**.
**Tương tác:** chạm icon tầng chính → mở/đóng tầng phụ tương ứng; gõ/chọn → áp dụng tức thời lên canvas; Delete → xoá lớp, tự đóng Text; chạm lớp chữ khác trên canvas → chuyển chỉnh lớp đó.
**Đặc biệt:** nội dung để trống vẫn hiển thị (dùng khoảng trắng, không ràng buộc bắt buộc nhập); không có cách "bỏ chọn" mà vẫn ở lại Text (ngoài xoá lớp hoặc X ở thanh trên).
**Điều hướng:** vào từ "Text" (tạo mới hoặc chạm lớp có sẵn); ra bằng X ở thanh trên Editor hoặc Delete.

### 13.3 Tool: Filter (Hiệu ứng màu)
**Mục đích:** áp 1 phối màu tổng thể (như filter máy ảnh) lên toàn ảnh.
**UI:** tiêu đề "Filter" + X. Tab 4 thể loại: **Popular, Adventure, Blue Shadow, Retro**. Thumbnail tròn cuộn ngang = bản xem trước áp trực tiếp lên chính ảnh đang chỉnh (không phải ảnh minh hoạ chung), kèm tên bên dưới; đang chọn có nền vàng nhạt bao quanh.
- Popular: None, Warm, Cool, Vivid, B&W. Adventure: None, Forest, Desert, Arctic. Blue Shadow: None, Midnight, Ocean, Steel. Retro: None, Sepia, Faded, Vintage.
**Tương tác:** chạm tab → đổi danh sách; chạm thumbnail → áp ngay, thấy tức thì; "None" → bỏ hiệu ứng; X → đóng, giữ hiệu ứng.
**Đặc biệt:** ảnh gốc chưa load → thumbnail hiện icon "cấm" thay preview màu. Bộ hiệu ứng này dùng chung với Camera và Collage Maker (cùng danh sách).
**Điều hướng:** vào từ "Filter"; ra bằng X.

### 13.4 Tool: Sticker (Nhãn dán)
**Mục đích:** thêm hình nhãn dán trang trí dạng đối tượng kéo-thả.
**UI:** tiêu đề "Stickers" + X. Tab 5 thể loại: **Cute, Kawaii, Party, Vintage, Minimal** (Cute mặc định). Icon mũi tên phóng to/thu nhỏ chiều cao lưới, sát mép phải cùng hàng tab. Lưới thumbnail 5 cột.
**Tương tác:** chạm tab → đổi thể loại; mũi tên → phóng to/thu nhỏ vùng lưới; chạm 1 sticker → thêm ngay vào giữa ảnh (panel KHÔNG tự đóng, thêm liên tiếp được); X → đóng, giữ sticker đã thêm.
**Đặc biệt:** sticker dùng chung cơ chế kéo-thả/xoay/phóng to-nhỏ/xoá/nhân bản như lớp chữ. Thể loại không có sticker → lưới trống.
**Điều hướng:** vào từ "Sticker"; ra bằng X.

### 13.5 Tool: Remove Background (Xoá phông nền)
**Mục đích:** tự động xoá nền, chỉ giữ chủ thể chính, tạo nền trong suốt.
**UI:** không panel riêng — chỉ 1 nút trên thanh công cụ chính. Đang xử lý: nút đổi trạng thái "đang chạy" + toàn màn tối ~50% + loading giữa màn.
**Tương tác:** chạm RemoveBG → gửi ảnh hiện tại (kèm mọi chỉnh sửa hiện có) cho dịch vụ xử lý; xong thì thay ảnh nền bằng kết quả.
**Đặc biệt:** **bản giả lập tạm thời** — trễ ~1.2s rồi trả về NGUYÊN ảnh gốc (chưa xoá nền thật, dự kiến nối API ngoài sau). Xử lý thất bại: ảnh giữ nguyên, không có thông báo lỗi. Chưa có ảnh gốc: chạm không có tác dụng.
**Điều hướng:** không mở màn/panel mới — xử lý ngay tại chỗ trên EditPhotoScreen.

### 13.6 Tool: Draw (Vẽ tay)
**Mục đích:** vẽ nét tự do trực tiếp lên ảnh, tuỳ chỉnh màu/độ dày/độ mờ.
**UI:** tiêu đề "Draw" + X. Hàng đầu: Undo/Redo (trái), "Brush" (giữa), dấu tick vàng xác nhận (phải). Hàng 7 màu tròn (trắng, tím, cam vàng, xám, xanh lá, xanh dương, đen; đang chọn có nền vàng nhạt). Thanh trượt "Size" (2–40). Thanh trượt "Opacity" (10%–100%).
**Tương tác:** kéo trên canvas (phía trên panel) → vẽ nét theo màu/dày/mờ hiện tại (đổi thông số chỉ ảnh hưởng nét tiếp theo); Undo/Redo; chạm màu/thanh trượt → đổi cho nét sắp tới; tick hoặc X → đóng, giữ nét đã vẽ.
**Đặc biệt:** Undo/Redo tự mờ khi hết tác vụ; vẽ nét mới sau khi Undo sẽ xoá vĩnh viễn hàng đợi Redo; chỉ nhận thao tác vẽ khi panel đang mở (đóng panel thì canvas chỉ hiển thị tĩnh, không cản trở kéo-thả lớp khác).
**Điều hướng:** vào từ "Draw"; ra bằng tick/X → về thanh công cụ chính.

### 13.7 Tool: Rotate (Xoay ảnh)
**Mục đích:** xoay ảnh 90°/lần để đổi chiều dọc↔ngang.
**UI:** chỉ 1 nút trên thanh công cụ chính, không panel riêng.
**Tương tác:** mỗi chạm xoay thêm 90° theo chiều kim đồng hồ, tức thì, không xác nhận; 4 lần liên tiếp = 360° (về góc ban đầu).
**Đặc biệt:** chưa có ảnh gốc → không tác dụng. Các lớp chữ/nhãn dán/nét vẽ **không xoay theo** ảnh nền — có thể gây lệch bố cục nếu xoay sau khi đã đặt chữ/sticker (điểm cần cân nhắc khi làm lại ở app mới).
**Điều hướng:** không mở màn/panel — xử lý ngay tại chỗ.

### 13.8 Ghi chú — Crop (Cắt ảnh)
**Không tồn tại** trong Edit Photo hiện tại. 7 công cụ chính không có tính năng cắt/khoanh vùng riêng — nếu app mới muốn có Crop, cần thiết kế mới hoàn toàn.

### 13.9 Ghi chú — thành phần dùng chung khác (không thuộc Edit Photo)
- **BackgroundPicker:** panel chọn nền (Blur/Upload/Colors/Template) — dùng ở Collage Maker & Wallpaper.
- **RatioPicker:** chọn 1/6 tỉ lệ khung — dùng ở Collage Maker/Wallpaper, KHÔNG có ở Edit Photo (Edit Photo giữ tỉ lệ ảnh gốc).
- **TemplateFrame/TemplateFrameGeometry:** đọc toạ độ khe hở ảnh của mẫu nền dựng sẵn — chỉ Wallpaper Creator dùng.
- **AssetCatalog:** liệt kê khung/nhãn dán/mẫu nền từ tài nguyên — Edit Photo dùng phần khung+nhãn dán; phần mẫu nền/template chỉ Collage Maker/Wallpaper dùng.
- **ToolPanelScaffold:** khung tiêu đề dùng chung "tiêu đề giữa + X trái" cho các panel (trừ Text, không dùng khung này).

---

# PHẦN D — COLLAGE MAKER, WALLPAPER CREATOR, GALLERY

## 14. Collage Picker (màn chọn ảnh trước khi vào Collage Editor)

### Mục đích
Chọn một hoặc nhiều ảnh (từ ảnh do app tạo hoặc có sẵn trên máy) làm nguyên liệu ghép collage.

### UI/Tương tác
Dùng lại đúng MediaPickerScreen dùng chung (mục 11), chỉ khác tiêu đề ("Collage Maker") và cho phép chọn NHIỀU ảnh (ô tick + đếm "Selected: N", nút xác nhận góc phải trên chỉ bật vàng khi đã chọn ≥1 ảnh).

### Trạng thái/case đặc biệt
- Chọn nhiều hơn số ô layout mặc định hỗ trợ (vd 12 ảnh): chỉ 9 ảnh đầu có chỗ trong lưới ghép mặc định — ảnh dư không tự hiện thêm khi vào Editor.
- Back → về Home.

### Điều hướng vào/ra
- Home → "Collage Maker" (có thể interstitial) → Collage Picker.
- Collage Picker → xác nhận đã chọn → Collage Editor (mang theo danh sách đường dẫn ảnh).
- Collage Picker → back → Home.

---

## 15. Collage Maker — Màn Editor chính (CollageMakerScreen)

### 15.1 Mục đích
Ghép nhiều ảnh đã chọn thành 1 ảnh collage theo bố cục tuỳ chỉnh, trang trí thêm nền/khung/chữ/sticker/nét vẽ, lưu vào thư viện.

### 15.2 Các thành phần UI và nội dung hiển thị
- Thanh trên: X trái, tiêu đề "Collage Maker" giữa, "Save" viên thuốc vàng phải.
- Canvas giữa: lưới ảnh ghép theo bố cục + tỉ lệ đang chọn. Lớp chồng: nền dưới cùng (mờ/màu/mẫu/ảnh tải lên) → lưới ô ảnh (khoảng cách/viền theo mức đã chọn) → khung trang trí phủ toàn canvas (kéo giãn vừa khít) → nét vẽ tay → đối tượng chữ/sticker kéo-thả.
- Mỗi ô ảnh: có ảnh → hiển thị (cắt vừa khung, áp filter nếu chọn); trống → icon "+" mờ.
- Thanh công cụ dưới (cuộn ngang): **Layout, Ratio, Margin, Background, Frame, Text, Filter, Sticker, Add, Draw**.
- Mở 1 tool → thanh chính biến mất, panel tương ứng đè phía dưới canvas (canvas không co/đẩy). Mỗi panel (trừ Text) có tiêu đề giữa + X trái.
- Đang lưu: toàn màn phủ tối mờ + loading giữa.

### 15.3 Tương tác người dùng — từng công cụ
- **Layout:** 7 bố cục (1 ô lớn; 2 ô dọc; 2 ô ngang; 3 ô dọc; 3 ô ngang; lưới 2x2; lưới 3x3), 5 ô hàng đầu + 2 ô hàng hai căn giữa; đang chọn nền cam nhạt + viền vàng. Đổi layout vẽ lại lưới ngay, không mất ảnh đã gán.
- **Ratio:** 6 tỉ lệ tổng thể (1:1, 4:3, 3:4, 16:9, 9:16, 3:2), mỗi lựa chọn có hình minh hoạ tỉ lệ.
- **Margin:** 3 mức khoảng cách — "Borderless" (sát nhau), "Thin" (mặc định), "Thick".
- **Background:** 4 nguồn — "Blur" (làm mờ ảnh đầu tiên đã chọn để lấp nền), "Upload" (dự kiến chọn ảnh nền tuỳ ý — **hiện chỉ đổi icon đang chọn, KHÔNG thực sự mở màn chọn ảnh nào**), "Colors" (8 màu đơn sắc), "Template" (mẫu nền dựng sẵn cuộn ngang, dùng chung bộ mẫu với Wallpaper).
- **Frame:** khung trang trí phủ toàn canvas, 5 danh mục (Party/Travel/Floral/Cute/Vintage) qua tab, lưới 5 cột (ô đầu = không dùng khung). Khung kéo giãn (không cắt) vừa khung hình canvas.
- **Text:** bấm "Text" tự thêm 1 lớp chữ mới ("Text") giữa canvas + mở thanh công cụ chữ: Edit, Font, Size (12–72), Colors (8 màu), Style (đậm/nghiêng), Align (trái/giữa/phải), **ARC** (-100%–100%, bẻ cong dòng chữ — **chỉ có ở Collage Maker, không có ở Wallpaper**), Delete.
- **Filter:** 4 danh mục qua tab (Popular/Adventure/Blue Shadow/Retro), preview ngay trên ảnh đầu tiên đã chọn (trong hình tròn nhỏ). Chọn 1 filter áp đồng loạt cho TOÀN BỘ ô ảnh trong lưới cùng lúc (không áp riêng từng ô).
- **Sticker:** 5 danh mục (Cute/Kawaii/Minimal/Party/Vintage) qua tab, mũi tên phóng to/thu nhỏ lưới. Chọn 1 sticker → thêm vào giữa canvas dạng lớp kéo-thả.
- **Add:** mở màn chọn ảnh toàn màn hình (không qua panel) để thêm 1 ảnh mới vào cuối danh sách hiện có (thêm mới, không thay ảnh cũ).
- **Draw:** vẽ tay tự do trên canvas — Undo/Redo (mờ khi hết tác vụ), nhãn "Brush", tick đóng giữ nét, 7 màu, thanh trượt Size (2–40) và Opacity (10%–100%). Panel mở thì chạm-kéo trên canvas sẽ vẽ theo thông số đang chọn.
- **Chạm trực tiếp 1 ô ảnh trong lưới:** mở màn chọn ảnh để gán/THAY ảnh cho đúng ô đó (dù trống hay đã có ảnh).
- **Đối tượng chữ/sticker trên canvas:** chạm chọn (viền vàng + 2 tay cầm: trên-trái xoá, dưới-phải nhân bản lệch nhẹ); kéo 1 ngón di chuyển; chụm/xoay 2 ngón phóng to-nhỏ+xoay.
- **X (đóng):** đang mở panel → chỉ đóng panel, về thanh chính; không panel nào mở → thoát Editor, về Collage Picker (ảnh đã chọn còn nguyên).
- **Save:** đóng băng canvas hiện tại (bố cục+nền+khung+filter+chữ+sticker+nét vẽ) thành 1 ảnh, lưu vào thư viện app + máy, thông báo "Đã lưu vào Gallery", tự rời toàn bộ luồng về thẳng Home. Thất bại: thông báo "Lưu collage thất bại, thử lại", ở lại Editor.

### 15.4 Trạng thái/case đặc biệt
- Bố cục mặc định tự chọn theo số ảnh mang vào: 0–1 ảnh → 1 ô; 2 ảnh → 2 ô dọc; 3 ảnh → 3 ô dọc; 4 ảnh → lưới 2x2; ≥5 ảnh → lưới 3x3. Ảnh dư (nhiều hơn số ô) không có chỗ hiển thị, layout tối đa hiện có chỉ 9 ô.
- Đang lưu (isSaving): khoá toàn bộ thao tác bằng lớp phủ + loading.
- "Upload" trong Background **không thực sự mở màn chọn ảnh** — hành vi thực tế hiện tại, không phải giả định.
- Tỉ lệ và mức khoảng cách/viền chỉ có tác dụng trong phiên hiện tại, KHÔNG lưu làm mặc định cho lần sau (mỗi lần vào lại Editor bắt đầu lại từ tỉ lệ 4:3, viền "Thin").

### 15.5 Điều hướng vào/ra
- Collage Picker → xác nhận ảnh → Collage Editor.
- Collage Editor → "Add" hoặc chạm 1 ô ảnh → màn chọn ảnh con → chọn xong quay lại đúng Editor với ảnh vừa gán.
- Collage Editor → X (không panel mở) → về Collage Picker.
- Collage Editor → Save thành công → Home (thoát hẳn luồng, back không quay lại Collage Picker được nữa).

---

## 16. Wallpaper Templates — Trang chọn mẫu hình nền (WallpaperTemplatesScreen)

### Mục đích
Duyệt và chọn nhanh 1 mẫu hình nền dựng sẵn để bắt đầu chỉnh sửa thành hình nền riêng.

### UI/Tương tác
- Thanh trên: back + tiêu đề "Wallpaper Creator".
- Lưới 2 cột ảnh xem trước từng mẫu, tỉ lệ dọc kiểu màn hình điện thoại.
- Mẫu trả phí (từ mẫu thứ 3 trở đi): huy hiệu tròn nền đen mờ + icon vương miện vàng góc trên-phải.
- Chạm bất kỳ mẫu nào (kể cả có vương miện) → mở ngay Wallpaper Editor — **hiện KHÔNG có bước chặn/khoá nào** khi chạm mẫu trả phí; huy hiệu chỉ mang tính hiển thị/gợi ý, chưa gắn với mở khoá/thanh toán thực sự.
- Back → về Home.

### Trạng thái/case đặc biệt
- Danh sách mẫu liệt kê tự động từ thư mục tài nguyên (không cố định cứng số lượng); 2 mẫu đầu luôn miễn phí, còn lại luôn có huy hiệu premium.
- Không xử lý riêng trạng thái "danh sách rỗng" — nếu không liệt kê được mẫu nào, lưới chỉ trống.

### Điều hướng vào/ra
- Home → "Wallpaper" (có thể interstitial) → Wallpaper Templates.
- Wallpaper Templates → chạm 1 mẫu → Wallpaper Editor (mang theo mã mẫu).
- Wallpaper Templates → back → Home.

---

## 17. Wallpaper Editor (WallpaperEditorScreen)

### 17.1 Mục đích
Chỉnh sửa mẫu hình nền đã chọn (hoặc bắt đầu từ canvas trống) bằng ảnh, chữ, sticker, nét vẽ, rồi lưu và tuỳ chọn đặt luôn làm hình nền máy.

### 17.2 Các thành phần UI và nội dung hiển thị
- Thanh trên: X trái, "Save" viên thuốc vàng phải (không có tiêu đề chữ giữa như Collage Maker).
- Canvas giữa, tỉ lệ dọc cố định 9:16.
- Vào từ mẫu có sẵn (mã > 0): canvas hiện ảnh nền trang trí mẫu, tự dò các "khe hở" trong suốt thiết kế sẵn trên khung mẫu (vd hình trái tim, khung ảnh) — mỗi khe hở là 1 "ô ảnh" để lấp ảnh cá nhân; ô trống hiện icon "+" tròn nổi trên nền mờ trắng nhạt, đúng vị trí/hình dạng khe hở.
- Canvas trống (mã = 0): nền màu xám-xanh đậm đơn sắc, không khung, không ô mẫu.
- Thanh công cụ dưới (cố định, không cuộn, giãn đều): **Add, Text, Background, Sticker, Draw** — chỉ 5 công cụ (ít hơn Collage Maker: không Layout/Ratio/Margin/Frame/Filter).
- Hộp thoại "Set as Wallpaper?" sau khi lưu thành công: tiêu đề "Set as Wallpaper?", nội dung "Đã lưu vào Gallery. Bạn có muốn đặt làm hình nền không?", 3 nút "Home"/"Lock"/"Both" + nút "Để sau".

### 17.3 Tương tác người dùng
- **Chạm khe hở ảnh của mẫu** (nếu có): mở màn chọn ảnh, ảnh chọn tự động biến dạng (bo góc/phối cảnh) để lấp vừa khít hình dạng khe hở.
- **Add:** mở màn chọn ảnh toàn màn; ảnh thêm vào dạng lớp tự do kéo-thả, KHÔNG gắn vào khe hở nào.
- **Text:** giống Collage Maker (Edit/Font/Size/Colors/Style/Align/Delete) nhưng **KHÔNG có ARC**.
- **Background:** cùng 4 nguồn Blur/Upload/Colors/Template, nhưng ở đây "Blur" không có ảnh gốc để làm mờ nên chỉ hiện màu nền đơn sắc; "Upload" cũng chỉ mở màn chọn ảnh để thêm lớp tự do (như Add), không đặt làm nền thật.
- **Sticker/Draw:** giống hệt Collage Maker.
- **X:** đang mở panel → chỉ đóng panel; không panel nào → thoát Editor về Wallpaper Templates.
- **Save:** đóng băng canvas (nền/khung mẫu + khe hở đã lấp + lớp tự do + nét vẽ) thành 1 ảnh, lưu thư viện app + máy, thông báo "Đã lưu vào Gallery", hiện dialog "Set as Wallpaper?". Thất bại: thông báo "Lưu wallpaper thất bại, thử lại", không hiện dialog.
- Dialog: "Home" đặt hình nền màn chính; "Lock" đặt màn khoá; "Both" cả hai; "Để sau" chỉ đóng, không đặt gì. Sau khi đóng dialog (bằng cách nào), **vẫn ở nguyên Wallpaper Editor** — khác Collage Maker (không tự về Home).

### 17.4 Trạng thái/case đặc biệt
- Mẫu không có khe hở nào: mọi thao tác thêm ảnh phải qua "Add", không có ô "+" nào có sẵn trên canvas.
- Lưu thành công KHÔNG tự điều hướng đi đâu — phải tự bấm X để rời màn sau khi xử lý dialog đặt hình nền.
- Đang lưu: canvas phủ tối mờ + loading, chặn thao tác.

### 17.5 Điều hướng vào/ra
- Wallpaper Templates → chọn mẫu → Wallpaper Editor.
- Wallpaper Editor → chạm khe hở/Add/Upload (Background) → màn chọn ảnh con → chọn xong quay lại đúng Editor.
- Wallpaper Editor → X (không panel mở) → về Wallpaper Templates.
- Wallpaper Editor → Save thành công → ở lại Wallpaper Editor (chỉ hiện dialog đặt hình nền, không rời màn).

---

## 18. Gallery — Thư viện đã lưu (GalleryScreen, 3 tab Photo / Video / Templates)

### 18.1 Mục đích
Xem lại ảnh/video đã chụp/chỉnh sửa bằng app (tuỳ chọn xem luôn toàn bộ ảnh/video có sẵn trên máy), lọc theo yêu thích, xem riêng sản phẩm collage/wallpaper đã tạo.

### 18.2 Các thành phần UI và nội dung hiển thị
- Thanh trên: tiêu đề "Saved items", icon trái tim góc phải bật/tắt "chỉ hiện mục yêu thích" (đặc đỏ khi bật, viền rỗng khi tắt).
- Dưới tiêu đề: 3 nút viên thuốc bằng nhau — "Photo", "Video", "Templates".
- Tab Photo/Video (không có ở Templates): hàng "Xem tất cả ảnh/video trong máy" + công tắc bật/tắt bên phải. Công tắc bật + quyền "một phần" (Android 14+): thêm icon "+" nhỏ trước công tắc để xin thêm quyền.
- Nội dung chính: lưới ảnh 3 cột, chia nhóm theo thời điểm (tiêu đề nhỏ trên mỗi nhóm: "Hôm nay", "Hôm qua", tên tháng nếu cùng năm hiện tại, "tháng/năm" nếu năm cũ hơn) — nhóm mới nhất lên trước.
- Mỗi ô: ảnh vuông; video có thêm icon play trắng nổi giữa ô.
- Danh sách rỗng (sau lọc): chữ xám giữa màn mời "lưu lại khoảnh khắc của bạn", thay lưới ảnh.
- Cuối màn: dải quảng cáo banner, hiển thị ở mọi tab.

### 18.3 Tương tác người dùng
- Icon trái tim → bật/tắt lọc chỉ-yêu-thích.
- 3 tab → chuyển Photo (app tạo) / Video (app tạo) / Templates (collage/wallpaper đã tạo).
- Bật công tắc "Xem tất cả ảnh/video trong máy" (chỉ Photo/Video): chưa từng cấp quyền → bật ngay dialog quyền hệ thống. Có quyền (toàn phần/một phần) + công tắc bật: lưới hiển thị TOÀN BỘ ảnh/video trên máy (không chỉ của app), nhóm theo ngày thêm vào máy.
- Icon "+" xin thêm quyền (chỉ hiện khi quyền một phần + công tắc bật) → mở lại dialog quyền để chọn thêm.
- Chạm 1 ảnh/video trong lưới (app tạo hay lấy từ máy) → mở màn xem chi tiết (ImportPhotoResult).
- Back → về màn trước (thường Home).

### 18.4 Trạng thái/case đặc biệt
- Mỗi lần vào lại Gallery, công tắc "xem tất cả media máy" LUÔN tự reset về tắt — không ghi nhớ cho lần sau.
- Công tắc "xem tất cả" đang bật: bộ lọc yêu thích KHÔNG có tác dụng (ảnh máy không có khái niệm "yêu thích" gắn với ngoài app).
- Tab "Templates": không có công tắc "xem tất cả", không đọc dữ liệu máy — chỉ hiện collage/wallpaper do chính app tạo.
- Chưa từng cấp quyền ảnh máy + công tắc tắt: tab Photo/Video vẫn hoạt động bình thường (chỉ hiện ảnh/video do app tạo) — quyền chỉ cần khi chủ động bật công tắc.
- Danh sách "mục do app tạo" ở Photo/Video KHÔNG bao gồm collage/wallpaper (những thứ đó chỉ ở tab Templates), dù lưu chung 1 chỗ trong dữ liệu app.

### 18.5 Điều hướng vào/ra
- Home → "Gallery" (có thể interstitial) → Gallery.
- Gallery → chạm 1 ảnh/video (tab bất kỳ) → ImportPhotoResult (kèm đường dẫn + cờ video).
- Gallery → back → màn trước (thường Home).

---

## 19. Ghi chú chung (áp dụng nhiều màn)

- **Quy tắc quảng cáo native:** chỉ hiện khi (a) chưa mua Premium, (b) cấu hình từ xa bật cho đúng vị trí, (c) có ID quảng cáo hợp lệ. Không thoả → ẩn hoàn toàn, không chiếm chỗ trống. Các vị trí: `native_intro1..4`, `native_permission`, `native_home`, `native_settings`, `native_takepho_sucess`, và các vị trí tương tự trong màn chọn ảnh/Gallery.
- **Quy tắc quảng cáo interstitial:** chỉ xuất hiện tại các điểm chuyển từ Home sang Camera/Gallery/Edit Photo/Collage Maker/Wallpaper, dùng chung vị trí `inter_home`. Nếu interstitial thường hiện, đóng xong mới xét thêm 1 lớp "native toàn màn hình" dự phòng; điều hướng thật luôn xảy ra đúng 1 lần, không bao giờ bị chặn vĩnh viễn.
- **Thư mục `ui/shared/media/`:** chứa logic xin quyền truy cập Ảnh/Video máy — dùng cho Gallery và các màn chọn ảnh (Edit Photo/Collage Maker/Wallpaper). Camera/VideoResult/ImportPhotoResult KHÔNG dùng logic này (Camera tự xin quyền Camera riêng; 2 màn kia chỉ thao tác trên file đã có sẵn trong bộ sưu tập nội bộ).
- **Route "mồ côi" cần lưu ý khi tham khảo app này:** `VideoResult` được khai báo đầy đủ trong hệ thống điều hướng nhưng KHÔNG được gọi tới từ luồng quay-video-trong-Camera thực tế (Camera tự lưu thẳng). Khi viết đặc tả cho app mới dựa theo mẫu này, cần xác nhận lại có muốn giữ bước "xem lại video trước khi lưu" hay bỏ hẳn.
- **Điểm chưa hoàn thiện cần ghi chú khi copy làm mẫu:** nút "Upload" trong Background Picker (Collage Maker & Wallpaper) hiện không thực sự mở màn chọn ảnh; Remove Background hiện là bản giả lập (chưa nối dịch vụ thật); Focus mode Detect/Infinity/Macro chỉ đổi UI, chưa có tác dụng quang học thật. Đây đều là hành vi THẬT của code hiện tại, không phải giả định — ghi lại để app mới biết chỗ nào cần làm đầy đủ hơn nếu muốn.
