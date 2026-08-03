# vga52a-kotlin — Mục lục tài liệu

> Thư mục này gồm 3 loại tài liệu hoàn toàn khác nhau về mục đích — đọc đúng nhóm cho đúng việc đang
> làm, đừng đọc lẫn:
>
> 1. **Quy trình đặc tả app** (mục A) — dùng khi bắt đầu 1 app MỚI, TRƯỚC khi chọn công nghệ/code.
> 2. **Kỹ thuật tích hợp lib** (mục B) — dùng khi ĐÃ có đặc tả và đang code, cần tích hợp
>    `base-application` AAR (splash/ads/IAP/language/notification) — playbook này **tái dùng cho mọi
>    app** trong dự án, không riêng vga52a-kotlin.
> 3. **Kỹ thuật dựng UI bám sát screenshot** (mục B2) — dùng khi ĐÃ có đặc tả và đang viết Compose UI,
>    cần vòng lặp nhanh để kiểm tra giao diện có khớp screenshot gốc không, không cần build cả
>    app/emulator mỗi lần sửa — cũng **tái dùng cho mọi app**.

## A. Tổng quát — đọc đầu tiên khi bắt đầu 1 app mới

| Tài liệu | Khi nào đọc |
|---|---|
| **[`APP_SPEC_PROCESS.md`](APP_SPEC_PROCESS.md)** | Trước khi viết bất kỳ dòng code nào của app mới — định nghĩa 4 tài liệu đặc tả bắt buộc (screenshot/mô tả chức năng/luồng màn hình/style) + quy trình từng bước để tạo ra chúng. |
| **[`example/`](example/)** | Bộ ví dụ ĐẦY ĐỦ — 4 tài liệu đặc tả + 1 tài liệu đối chiếu chéo màn hình↔screenshot, đã viết sẵn cho chính app vga52a-kotlin (viết lại từ code thật sau khi app đã build xong), dùng làm khuôn mẫu về độ chi tiết/cách trình bày. Đọc [`example/README.md`](example/README.md) trước. |
| **[`template/`](template/)** | Bản **trắng** để điền khi bắt đầu đặc tả 1 app khác — copy thư mục này, mỗi file có comment trỏ đúng file ví dụ tương ứng trong `example/`. Bao gồm cả quy tắc đặt tên screenshot + bảng đối chiếu chéo (`04_SCREENSHOT_MAPPING.md`). |
| **`HANDOFF_NEXT_APP.md`** (ở `vga49a/` — thư mục cha) | Sau khi đã có đủ đặc tả: chọn app mẫu gần giống nhất trong dự án để clone làm khung. |

## B. Đọc theo từng bước — khi tích hợp lib `base-application` (ads/IAP/splash/language)

Toàn bộ nhóm này nằm trong [`integration/`](integration/) — xem
**[`integration/README.md`](integration/README.md)** để có thứ tự đọc đầy đủ (11 file, từ playbook
tổng quát → catalog lỗi → từng chủ đề con: ads/firebase/intro-check/UI-giống-RN/bài học vận hành/
**chuẩn encode+R8**). Đây là bộ tài liệu **dùng chung cho mọi app** trong dự án (copy nguyên cả thư
mục khi làm app mới), KHÔNG mô tả riêng tính năng Camera/Edit Photo/Collage/Wallpaper của app này.
Đặc biệt: **[`integration/16_ENCODE_R8_STANDARD.md`](integration/16_ENCODE_R8_STANDARD.md)** — bắt
buộc đọc trước khi chạy `push.sh build_tool` lần đầu, mô hình chuẩn "2-3 file cần sửa khi clone app
mới" cho `encode-scripts/`+`proguard-rules.pro` + catalog bug thật đã gặp qua khảo sát 16 app.

## B2. Kỹ thuật dựng UI bám sát screenshot (khi viết Compose UI)

**[`UI_FIDELITY_WORKFLOW.md`](UI_FIDELITY_WORKFLOW.md)** — dùng **Roborazzi** để render 1 Composable
ra file PNG trên JVM trong vài giây (không cần emulator/thiết bị thật), so trực tiếp với screenshot
gốc trong `example/screenshot/`, sửa code, lặp lại tới khi khớp. Giải quyết đúng vấn đề "AI dựng UI
xong bị lệch so với ảnh gốc, mà build lại cả app để kiểm tra thì quá chậm". Tài liệu có kèm 3 lỗi thực
tế đã gặp khi cài đặt (bắt buộc đọc trước khi thêm test Roborazzi mới) + ví dụ thật đã chạy
(`FilterPickerScreenshotTest.kt`) + giới hạn thực tế (màn hình dùng `hiltViewModel()`/ads/Firebase
không render trực tiếp được).

## C. Tài liệu chuyên biệt của app vga52a-kotlin

Mô tả chức năng/luồng/style **thật** của chính app này (Camera Studio: chụp ảnh + video, chỉnh sửa
ảnh, ghép collage, tạo wallpaper, thư viện) nằm trong **[`example/`](example/)** — vì file
`01_MO_TA_CHUC_NANG_MAN_HINH.md`/`02_LUONG_DI_CHUYEN_MAN_HINH.md`/`03_STYLE_APP.md` ở đó được viết
bằng cách đọc trực tiếp code của chính app này (không phải app giả định), nên đồng thời đóng 2 vai
trò: vừa là **ví dụ mẫu** cho quy trình ở mục A, vừa là **tài liệu tham chiếu thật** khi cần tra lại
"màn X hoạt động đúng như thế nào" mà không muốn đọc lại toàn bộ source Kotlin.

## D. Đã dọn khỏi thư mục này (2026-08-03)

Khi rà soát lại để phân loại, phát hiện 2 file **không liên quan tới vga52a-kotlin** — tài liệu của
app **vga34b (GeoStamp Camera, `com.camera.geostamp`)**, một app camera GPS-stamp hoàn toàn khác, lạc
vào `docs/` của app này (khả năng cao do copy nhầm thư mục `docs/` mẫu lúc khởi tạo project):

- `CHECKLIST.md` — checklist port RN→Kotlin của vga34b (DataMap/GPS overlay/Uninstall shortcut — các
  tính năng không tồn tại ở app camera/photo-editor này).
- `KE_HOACH_CHUYEN_KOTLIN_vga34b.md` — kế hoạch chuyển đổi riêng của vga34b.

Đã **xoá** (lịch sử vẫn còn trong `git log` nếu cần tra lại) để tránh gây nhầm lẫn cho người đọc sau.
`KE_HOACH_TICH_HOP.md` (case ví dụ app `vga_49`/DetectorSpy) được **giữ lại** trong `integration/` vì
được `PLAYBOOK_TICH_HOP.md` trích dẫn làm minh hoạ thực tế cho quy trình tích hợp — đọc với tinh thần
"đây là 1 ví dụ đã áp dụng playbook cho app khác", không phải tài liệu của app hiện tại.
