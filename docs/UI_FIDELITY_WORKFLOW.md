# Quy trình dựng UI bám sát screenshot — Roborazzi (render Compose ra PNG trên JVM)

> Giải quyết vấn đề: giao diện Compose do AI/dev dựng lại từ screenshot + mô tả chức năng thường bị
> lệch (màu, khoảng cách, chi tiết nhỏ) so với ảnh gốc, và vòng lặp "sửa code → build app → cài máy →
> chụp màn hình → so sánh" quá chậm để lặp nhiều lần. Roborazzi cho phép **render 1 Composable ra file
> PNG trong vài giây, ngay trên máy build (JVM), không cần emulator/thiết bị thật** — AI có thể tự đọc
> ảnh PNG đó (bằng công cụ đọc ảnh) và so trực tiếp với screenshot gốc trong `docs-sample/screenshot/`
> (hoặc `docs/screenshot/` khi làm app mới), tự sửa code, lặp lại tới khi khớp — chỉ khi nào
> giao diện đã khớp mới cần build/cài app thật (để kiểm tra hành vi/tương tác, không phải để soi UI).

## 1. Đã cài đặt sẵn trong project

- Plugin `io.github.takahirom.roborazzi` (khai ở `build.gradle.kts` root + áp dụng ở `app/build.gradle.kts`).
- Thư viện: `roborazzi`, `roborazzi-compose`, `roborazzi-junit-rule`, `robolectric` (khai ở
  `gradle/libs.versions.toml`, nhóm version `roborazzi`/`robolectric`).
- `android.testOptions.unitTests.isIncludeAndroidResources = true` trong `app/build.gradle.kts` (bắt
  buộc để Robolectric đọc được resource của app).
- Test mẫu đã chạy được:
  [`app/src/test/java/.../screenshot/ComingSoonScreenshotTest.kt`](../app/src/test/java/com/camera/studio/photoeditor/screenshot/ComingSoonScreenshotTest.kt).

## 2. ⚠️ 3 lỗi thực tế đã gặp khi cài đặt lần đầu (đọc trước khi đụng vào phần này)

### 2.1 Roborazzi bản mới nhất không tương thích Kotlin của project
Bản `1.70.0` (mới nhất tại thời điểm cài) được build bằng **Kotlin 2.3.21**, trong khi project dùng
Kotlin **2.1.20** → lỗi biên dịch:
```
Module was compiled with an incompatible version of Kotlin.
The binary version of its metadata is 2.3.0, expected version is 2.1.0.
```
**Đã pin về `1.60.0`** (bản mới nhất còn build bằng Kotlin 2.0.21, tương thích ngược với 2.1.20) —
xem chú thích ở `gradle/libs.versions.toml`. Khi nào project bump Kotlin lên ≥ 2.3.x mới nên gỡ pin,
đổi lại bản mới nhất.

### 2.2 Robolectric mặc định khởi tạo `MyApplication` thật → crash
Nếu không chỉ định, Robolectric tự lấy `android:name` trong `AndroidManifest.xml` (`.MyApplication`,
kế thừa `BaseApplication` của lib — tự init Firebase/Ads/Hilt trong `onCreate()`) làm Application cho
test → crash `IllegalStateException` từ `FirebaseApp` (Firebase không bootstrap được trên JVM giả
lập). **Bắt buộc** với mọi test loại "chỉ render UI thuần" (không cần data/DI thật):
```kotlin
@Config(application = android.app.Application::class)
```
Xem ví dụ trong `ComingSoonScreenshotTest.kt`.

### 2.3 `./gradlew testDebugUnitTest` KHÔNG thực sự ghi ảnh ra đĩa
Chạy task unit test thường (`testDebugUnitTest`) khiến test **PASS** nhưng **không sinh file PNG nào**
— `captureRoboImage(...)` chỉ thực sự ghi/so sánh ảnh khi chạy qua **task riêng do plugin Roborazzi
sinh ra**. Luôn dùng 1 trong các task sau (xem mục 4):
```bash
./gradlew :app:recordRoborazziDebug --tests "<FQCN test>"
```

## 3. Giới hạn thực tế — composable nào render được, composable nào KHÔNG

**Render được ngay** (composable THUẦN — chỉ nhận state/callback qua tham số, không tự gọi
`hiltViewModel()`/Firebase/ads bên trong): phần lớn các "tool panel" dùng chung
(`FilterPicker`, `StickerPicker`, `FramePicker`, `RatioPicker`, `BackgroundPicker`, `TextToolbar`,
`DrawToolPanel`, `PillTabs`, `AppHeader`...) ở `ui/shared/`, `ui/components/`. Đây là nhóm nên ưu tiên
viết Roborazzi test — chiếm phần lớn diện tích UI thật sự cần bám sát screenshot (panel công cụ trong
Edit Photo/Collage Maker/Camera).

**KHÔNG render trực tiếp được** — 9 màn hình cấp route hiện tại tự gọi `hiltViewModel()` và/hoặc chạm
Firebase Remote Config/ads ngay trong composable: `HomeScreen`, `CameraScreen`, `EditPhotoScreen`,
`CollageMakerScreen`, `WallpaperEditorScreen`, `GalleryScreen`, `ImportPhotoResultScreen`,
`MediaPickerScreen`, `VideoResultScreen`. Test các màn này theo cách ở mục 5 sẽ crash giống mục 2.2
(hoặc cần Hilt test rule đầy đủ, tốn công hơn nhiều).

**Hướng xử lý khuyến nghị (khi cần bám sát 1 trong 9 màn này):** tách phần **UI thuần** ra khỏi phần
**lấy dữ liệu**, theo đúng pattern "state hoisting" chuẩn của Compose — không phải hack riêng cho
testing, mà là kiến trúc tốt nói chung:
```kotlin
// Wrapper mỏng — KHÔNG test bằng Roborazzi (cần Hilt thật)
@Composable
fun HomeScreen(..., viewModel: HomeViewModel = hiltViewModel()) {
    val recentMedia by viewModel.recentMedia.collectAsState()
    HomeScreenContent(recentMedia = recentMedia, onOpenCamera = ..., ...)
}

// Phần UI thật — THUẦN, test/render được bằng Roborazzi với data giả
@Composable
fun HomeScreenContent(recentMedia: List<SavedMedia>, onOpenCamera: () -> Unit, ...) { ... }
```
Việc tách này KHÔNG bắt buộc phải làm ngay cho cả 9 màn — chỉ tách khi thực sự cần bám sát screenshot
chi tiết của đúng màn đó bằng vòng lặp nhanh. Không cần Roborazzi, vẫn build/cài app thật để soi UI của
9 màn này như trước (chậm hơn nhưng không mất công refactor).

## 4. Các lệnh Gradle hay dùng (cheat sheet)

| Lệnh | Tác dụng |
|---|---|
| `./gradlew :app:recordRoborazziDebug --tests "<FQCN>"` | Ghi/ghi đè ảnh PNG mới — dùng khi đang lặp sửa UI, luôn muốn thấy bản mới nhất |
| `./gradlew :app:recordRoborazziDebug` | Ghi ảnh cho **toàn bộ** test Roborazzi trong module (không lọc `--tests`) |
| `./gradlew :app:compareRoborazziDebug --tests "<FQCN>"` | So ảnh mới render với ảnh đã ghi trước đó (golden), xuất report HTML diff, KHÔNG fail build |
| `./gradlew :app:verifyRoborazziDebug --tests "<FQCN>"` | Giống compare nhưng **FAIL build** nếu khác — dùng khi muốn chặn regression UI ngoài ý muốn (chưa cần dùng ở giai đoạn "đang dựng UI mới") |
| `./gradlew :app:clearRoborazziDebug` | Xoá toàn bộ ảnh đã ghi |

Ảnh xuất ra nằm ở `app/build/outputs/roborazzi/<tên>.png` (đường dẫn đặt trong tham số của
`captureRoboImage("...")` trong code test) — thư mục này nằm trong `app/build/`, đã được `.gitignore`,
không cần lo commit nhầm.

## 5. Quy trình dùng hằng ngày (cho AI hoặc dev)

1. Xác định composable cần dựng/sửa khớp 1 file trong `docs-sample/screenshot/` (hoặc
   `docs/screenshot/` nếu đang làm app mới) + mô tả tương ứng trong
   `01_MO_TA_CHUC_NANG_MAN_HINH.md`.
2. Nếu composable đó THUẦN (mục 3) → viết 1 test Roborazzi mới trong
   `app/src/test/java/.../screenshot/`, theo khuôn `ComingSoonScreenshotTest.kt`:
   - Bọc trong `AppTheme { ... }`.
   - Nền đen (`Modifier.background(Color.Black)`) nếu panel gốc nằm trên nền tối, để so màu chính xác.
   - Dữ liệu mẫu truyền vào NÊN mô phỏng đúng trạng thái trong screenshot gốc (vd: nếu ảnh gốc đang
     chọn sẵn 1 mục, truyền `selected = <mục đó>` thay vì `null`) — so sánh với state khác ảnh gốc sẽ
     luôns lệch dù code đúng.
   - Đặt tên file PNG output theo cùng quy tắc đặt tên screenshot đã thống nhất (xem
     `docs-sample/04_SCREENSHOT_MAPPING.md` mục 1) để dễ đối chiếu.
3. Chạy `./gradlew :app:recordRoborazziDebug --tests "<FQCN test>"` (vài giây tới vài chục giây sau
   lần chạy đầu — lần đầu tải Robolectric jar có thể mất vài phút, các lần sau nhanh vì đã cache).
4. Đọc file PNG vừa sinh ra bằng công cụ đọc ảnh, đặt cạnh screenshot gốc, so trực tiếp: màu, khoảng
   cách, cỡ chữ, vị trí, trạng thái chọn/active...
5. Sửa code Compose theo chỗ lệch phát hiện được, quay lại bước 3 — lặp tới khi khớp. KHÔNG cần build
   cả app hay chạm tới emulator/thiết bị trong suốt quá trình này.
6. Khi giao diện đã khớp: build/cài app thật lên máy để kiểm tra **hành vi** (bấm có đúng chức năng
   không, chuyển màn có đúng không...) — lúc này không cần soi UI nữa vì đã chốt ở bước 3-5.

## 6. Ví dụ thật đã chạy (bằng chứng, không phải giả định)

`ComingSoonScreenshotTest.comingSoon_rendersTitleAndPlaceholderText` render composable
[`ComingSoon`](../app/src/main/java/com/camera/studio/photoeditor/ui/components/Common.kt) — bằng
chứng pipeline Roborazzi chạy được trên bản "example lõi" này (không có ảnh gốc để so sánh vì
`ComingSoon` là placeholder mới thêm, không phải màn thiết kế thật).

**Ví dụ trước đó (đã áp dụng đúng quy trình, phát hiện lỗi thật):** khi app này còn đầy đủ tính năng
(trước khi rút gọn thành example), một test tương tự render `FilterPicker` (panel Filter của Edit
Photo/Collage Maker) và so với `edit_image_filter.jpg` đã phát hiện: tab "Popular" đang chọn thiếu
gạch chân so với ảnh gốc (lỗi UI thật), và phát hiện dữ liệu test (`selected = null`,
`previewBitmap = null`) chưa mô phỏng đúng trạng thái ảnh gốc (lỗi ở test, không phải ở code) — đúng
2 loại lỗi mà quy trình này nhắm tới. Khi bắt đầu 1 tính năng thật trong app mới, dùng lại đúng cách
làm này (test composable thuần → so từng điểm với ảnh gốc → phân loại "lỗi code" vs "lỗi test").

## 7. Nâng cấp về sau (chưa cần làm ngay)

- **Golden-image regression**: dùng `verifyRoborazziDebug` trong CI, lưu ảnh "chuẩn" đã chốt vào
  `app/src/test/screenshots/` (commit vào git, KHÔNG để trong `build/`) để tự động chặn PR nào làm
  trôi UI ngoài ý muốn — khác mục đích với quy trình ở tài liệu này (đang DỰNG UI mới, chưa cần chặn).
- **`RoborazziRule`** (thay vì gọi `captureRoboImage` tay): tự động sinh task record/compare/verify
  gọn hơn, hỗ trợ capture GIF cho tương tác nhiều bước (vd kéo-thả 1 lớp chữ).
- Tách "stateful wrapper" khỏi "stateless content" cho 9 màn hình ở mục 3 khi thực sự cần bám sát
  screenshot chi tiết của chúng.
