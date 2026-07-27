# ✅ CHECKLIST — vga34b "GeoStamp Camera" (`com.camera.geostamp`)

> Danh sách việc chuyển RN→Kotlin + phát hành. **Làm xong hạng mục nào thì đổi `[ ]` → `[x]`.**
> Trước khi coi là "hoàn thành", rà lại toàn bộ file này 1 lượt.
>
> Cập nhật gần nhất: 2026-07-06 (viết lại HOÀN TOÀN — file cũ là bản sao nguyên của `vga34d`,
> không hề khớp với vga34b, sai cả package/ads/checklist item). Xem [[port-vga34b-rn-to-kotlin]].

## A. Nền tảng & rebrand
- [x] Clone `vga34d-kotlin` (không phải vga31b-kotlin — cùng dòng camera GPS-stamp, tái dùng ~80%)
- [x] package/namespace/applicationId = `com.camera.geostamp`
- [x] `google-services.json` (project `geostamp-camera-gps`, khớp package)
- [x] admob app id `ca-app-pub-9405071173323068~7449875879`
- [x] app_name = "GeoStamp Camera" + rootProject.name
- [x] Google Maps key (giống vga34d, cùng project Google Cloud)
- [x] `app_flyer_id` = `Qhno4yJY6KHmZp9uS9DRe4`
- [ ] logo/icon thật — đang dùng placeholder `logo.png` (xem mục H)

## B. Giao diện (bám RN) — đã test máy thật 2026-07-04/06, sửa nhiều vòng
- [x] Theme `#0C87FF` (Primary) + `#ed9a16` (ThemeSecond)
- [x] Overlay GPS-stamp 4-layout (id 2 mặc định có lat/long, 3/5/6 không) — `TemplateOverlay.kt`
- [x] **Bug lớn đã sửa**: overlay bake vào ảnh/video lưu bị lệch lên góc trên-trái so với preview
      (thiếu đúng modifier khớp Box BottomCenter ở cả 3 nơi bake: Camera/Video/ImportPhotoEdit) —
      đã sửa + verify bằng pull file thật (không phải screenshot). **`vga34d-kotlin` bị lỗi y hệt,
      CHƯA sửa (ngoài phạm vi, cần hỏi user).**
- [x] Home: Camera banner + Data map/Stamp 2 card + Gallery (3 mới nhất + Show more), gate quyền/vị trí
- [x] DataMap: form địa chỉ chi tiết, map luôn kéo được (đã sửa — trước bị Column cha cướp gesture),
      auto-fetch location 2 bước (lastKnown nhanh + fresh fix), fallback Village từ
      subLocality→locality→subAdminArea (Geocoder VN không ổn định field nào chứa cấp phường/xã),
      banner nhắc bật Location hệ thống khi tắt (khác quyền runtime app)
- [x] Gallery: đổi tên từ Library, thêm 2 tab Image/Video trong màn (yêu cầu user 2026-07-06)
- [x] ImportPhotoResult (Share/Delete): sửa nút bị "dính" — nguyên nhân Material3 `IconButton` tự
      nới vùng chạm tối thiểu 48dp dù đã set `.size(32.dp)` — đổi sang `Box.clickable` thường
- [x] DrawStyle (route mồ côi RN — đã bổ khuyết 1 icon Palette ở header Home)
- [ ] Rà lại thêm 1 vòng Template/Settings/EditTime/EditFormatTime vs RN — chưa test kỹ riêng từng màn này

## C. Quảng cáo
- [x] `default_ads_config.json` — unit THẬT vga34b (publisher `9405071173323068`), đủ placement
      (xem danh sách đầy đủ trong file)
- [x] **Bổ sung 5 key mới do AutoBuild API báo thiếu (2026-07-06, BuildNumber 328)**: `open_splash`,
      `open_all`, `native_keep_user`, `native_survey_user`, `native_exit_app` — schema mới của pipeline
      build yêu cầu, KHÔNG tồn tại ở bất kỳ đâu cho vga34b trước đó (đã tự tìm + dùng agent xác nhận
      kỹ, không có trong RN source/Remote Config export/workspace). Xử lý:
      - `open_splash`/`open_all` = tái dùng ĐÚNG unit `app_open` thật (`9094130032`) — an toàn, khớp
        cách vga34d-kotlin làm (2 key này cùng trỏ 1 unit App-Open thật, RN chỉ có 1 unit duy nhất).
      - `native_keep_user`/`native_survey_user`/`native_exit_app` = **TẠM dùng lại unit
        `native_settings` (`5171789799`)** — CHƯA có unit Native riêng nào được tạo cho 3 placement
        này trên AdMob Console. Ads vẫn hiển thị đúng (cùng format Native), chỉ gộp chung số liệu
        thống kê/report 3 placement này với native_settings tạm thời.
      - **[ ] TODO thật — cần làm sau**: tạo 3 Native ad unit riêng trên AdMob Console (publisher
        `9405071173323068`, app `com.camera.geostamp`) cho `native_keep_user`/`native_survey_user`/
        `native_exit_app`, rồi thay 3 giá trị tạm trên bằng unit thật riêng.
- [x] `config.xml` ratios (`inter_home` 4/20, `native_inter_home` 2/20, tất cả `_enable=true`)
- [x] Log ads dùng `println` (lib strip `android.util.Log` ở release)
- [x] Đã cài máy test thật nhiều vòng (2026-07-04, 2026-07-06) — không crash, nhiều native ad slot
      load OK (`ADSLOT loaded ... ad=true`): `native_home/intro1-4/permission/takepho_sucess/gallery`,
      interstitial test ad render/đóng bình thường, AdMob native ad validator "No implementation
      issues found" liên tục.
- [x] **Bug quan trọng đã sửa (2026-07-06, audit agent phát hiện)**: `AdManager.showInter()`
      (chain inter_home → native_inter_home) được định nghĩa đầy đủ nhưng **KHÔNG có nơi nào gọi**
      trong code sản xuất — RN gốc gọi `showad()` ở **MỌI** lần `handleNavigate` (Settings/DataMap/
      Camera/Template), Kotlin port chỉ có gate quyền/vị trí, thiếu hẳn bước ads. Đã wire lại đúng
      RN (`HomeScreen.kt handleNavigate` gọi `AdManager.showInter(activity, "inter_home") { proceed() }`
      trước khi proceed, khớp `HomeLayout.js:177-192`). **`vga34d-kotlin` có khả năng bị lỗi tương tự
      — CHƯA kiểm tra riêng, cần rà lại nếu có thời gian.**
- [x] **Bug đã sửa**: `native_gallery` có trong config nhưng `GalleryScreen.kt` chưa từng gọi
      `NativeAdPlaceholder` — đã thêm dưới lưới ảnh, khớp `screens/Gallery.js:60-67` RN.
- [ ] **`inter_uninstall`/`native_inter_uninstall` — CHƯA QUYẾT ĐỊNH**: RN vga34b CÓ tính năng
      Uninstall thật (long-press icon launcher → app shortcut → màn khảo sát → 2 ad placement này) —
      KHÔNG phải config dư như từng nghĩ. Bản Kotlin (cả vga34b và vga34d trước đó) đã xoá
      `ShortcutHelper.kt` nhầm thành "dead code" nên tính năng này chưa được port. 2 hướng: (a) port
      đầy đủ UninstallScreen + Android app-shortcut, hoặc (b) xoá 2 key này khỏi
      `default_ads_config.json`/`config.xml` nếu chủ động bỏ tính năng. **User chưa chọn hướng nào
      — hỏi lại trước khi publish.**
- [ ] Xác nhận `native_language`/`native_language_select` (đang định nghĩa trong config nhưng
      Kotlin app không có Language screen riêng — do lib `base_application` tự quản lý màn Language)
      thực sự được lib đọc đúng 2 key này — chưa tự kiểm tra sâu vào lib.
- [ ] **Publish Play → xác nhận ads THẬT fill** (chưa publish → NO_FILL là bình thường)

## D. IAP
- [x] `iapPremiumKey` = `remove_ads_premium` (đúng RN `SUBSCRIPTION_IDS[0]`)
- [x] `monthly`/`yearly` = đúng RN `BASE_PLAN_MONTH`/`BASE_PLAN_YEAR`
- [x] **Xác nhận qua audit agent (đọc trực tiếp RN `envApp.js`)**: RN vga34b **KHÔNG có** base-plan
      "weekly" — chỉ 1 product (`remove_ads_premium`) với 2 base-plan monthly/yearly. Giá trị
      `iapPremiumWeeklyKey() = "weekly"` trong Kotlin là **placeholder bắt buộc** (lib yêu cầu 1 key
      weekly dù RN không dùng) — KHÔNG phải thiếu sót, không cần sửa.
- [x] `public_license_key` — đã điền key THẬT (RSA Licensing), lấy từ `keystore_2.zip/VGA34B/public_license_key.txt`

## E. Firebase Remote Config
- [x] `fire_base_key_lib_arr.json` — copy từ `vga34d-kotlin` (xác nhận **giống hệt byte-for-byte**
      vga31a-kotlin/vga31b-kotlin → đúng schema chung lib, dùng lại an toàn cho mọi app).
- [ ] **`firebase_old_key.json` — VẪN THIẾU**, cần user tự export từ Firebase Console project
      **`geostamp-camera-gps`** (Remote Config → ⋮ → Tải xuống dưới dạng JSON) hoặc cấp file — không
      thể tự tạo được (đây là dữ liệu Remote Config THẬT của app RN đang chạy ngoài thị trường).
- [x] Đã chạy thử `firebase-merge.js` (2026-07-06) — **thành công nhưng THIẾU phần RN cũ** vì chưa có
      `firebase_old_key.json` (script tự bỏ qua phần thiếu, không lỗi) → file kết quả CHỈ có phần
      KOTLIN + `ads_config`, thiếu toàn bộ tham số RN cũ (~40-50 param tuỳ app khác). **Phải chạy lại
      sau khi có `firebase_old_key.json` mới coi là bản cuối để upload.**
- [ ] Upload `firebase_key.json` (bản ĐẦY ĐỦ, sau khi có old key) lên Firebase Console → Remote
      Config → Import

## F. Encode / Build / Ký
- [x] **Bug thật đã gặp + sửa (2026-07-06) — ĐÚNG lỗi vga34d từng gặp với vga31b, lặp lại y hệt
      khi clone vga34d→vga34b**: `encode-scripts/rename-config-global.js` vẫn giữ nguyên
      `RANDOM_SALT`/`BASE_NAMESPACE` của **vga34d** (`com-mapcamera-location-slt`/
      `com.mapcamera.location`) — không ai cập nhật khi clone. Hậu quả: `rename-file.js` KHÔNG nhận
      diện được `com.camera.geostamp` là package gốc cần bảo vệ → rename lẫn cả `import
      com.camera.geostamp.R`/package gốc của `MyApplication.kt` → `assembleRelease` lỗi
      `Unresolved reference 'R'`/`'BuildConfig'` toàn bộ 19 file dùng R. Đã sửa: đổi lại đúng
      `com-camera-geostamp-slt`/`com.camera.geostamp`. **Bài học lặp lại lần 3 (vga31b→vga34d→vga34b)
      — LUÔN kiểm tra + tự thử build_tool 1 lần MỚI tin, đừng chỉ đọc lại config bằng mắt.**
- [x] **Bug thứ 2 đã sửa (cùng lúc, phòng hờ)**: `rename-file.js` blanket string-replace package cũ
      → mới trong MỌI dòng `import`, không loại trừ `import <BASE_NAMESPACE>.R` — dù đã sửa bug #1
      ở trên, vẫn thêm 1 lớp bảo vệ riêng cho dòng `import ...R` (R là class AGP sinh dưới
      `namespace` khai báo ở `build.gradle.kts`, không đổi theo rename source) để tránh lặp lại nếu
      BASE_NAMESPACE sai lần nữa trong tương lai.
- [x] Config key KHÔNG hash (giữ readable — tránh tắt ads ở release, xem `11_FIREBASE_KEY.md` §4)
- [x] Keystore THẬT VGA34B (`keystore/VGA34B/keystore.jks` + `pass.txt`) — khớp `app/build.gradle.kts`
- [x] Build debug XANH + `assembleRelease` (R8) XANH qua `push.sh build_tool` thật (2026-07-06, sau
      khi sửa 2 bug trên) — đã push thành công lên `build_tool/main`.
- [x] Đã cài máy test thật nhiều vòng debug — CHƯA cài bản release/encode lên máy test riêng (build
      test của `push.sh` chỉ compile, không tự cài).
- [x] **Bài học quy trình quan trọng (git)**: `push.sh` (cả `origin` và `build_tool`) luôn tạo
      NHÁNH TẠM từ `androidsdkads`, commit + push NHÁNH TẠM lên remote, rồi `git checkout
      androidsdkads` — nhưng branch `androidsdkads` **local** KHÔNG được fast-forward theo nhánh tạm
      đã push! Kết quả: mọi commit mới (kể cả fix vừa làm) chỉ có trên REMOTE, còn working
      directory sau mỗi lần `push.sh` chạy xong sẽ tự "lùi lại" đúng commit local cũ của
      `androidsdkads` — làm mất trắng bất kỳ sửa nào chưa kịp thấy trong log. **Bắt buộc sau MỖI
      lần `push.sh` chạy: `git merge --ff-only origin/androidsdkads`** để đồng bộ local khớp remote,
      nếu không sẽ lặp lại đúng bug đã mất fix 1 lần trong phiên này (phải phát hiện qua system note
      báo file "bị sửa lại" mới biết).

## G. Đẩy git
- [x] Repo có sẵn, KHỞI TẠO branch `androidsdkads` (2026-07-06), commit đầu tiên toàn bộ source.
- [x] Remote đã thêm (2026-07-06, theo user cấp):
  - `origin` = `http://gitlab.vgasoft.vn/camera-location/vga34c-camera-location.git`
    (⚠️ tên repo là **vga34c**, không phải vga34b — đã hỏi/ghi chú lại, dùng đúng URL user cấp)
  - `build_tool` = `http://gitlab.vgasoft.vn/vga-trip-build/vga34b.git`
- [x] Đã chạy `./push.sh` (cả origin + build_tool) — xem log chạy thật trong session, kết quả ghi ở
      cuối file này/báo cáo chat.

## H. Còn thiếu trước phát hành (TODO)
- [ ] `facebook_app_id` + `facebook_client_token` — **còn placeholder `TODO_FACEBOOK_APP_ID`/
      `TODO_FACEBOOK_CLIENT_TOKEN`**, không tìm thấy nguồn nào cấp giá trị thật (đã tìm trong
      `keystore/`, `keystore_2/` — không có).
- [ ] Branding: icon notification (5 density)/icon splash/ảnh preview IAP 365×174 — đang placeholder
      `logo.png`
- [ ] Quyết định hướng tính năng Uninstall (xem mục C)
- [ ] Publish Play Store (đúng package `com.camera.geostamp` + keystore VGA34B)
- [ ] `firebase_old_key.json` từ user → hoàn thiện `firebase_key.json` → upload Remote Config

## I. ⚠️ MỤC HAY QUÊN — KIỂM MỖI LẦN BUILD (đặc biệt ADS)
- [x] **Ads kép (chain)**: `inter_home` → (đóng) → `native_inter_home` fallback, ratio 4/20 và 2/20.
      Đã sửa lỗi wiring thiếu — xem mục C.
- [x] **Log ads + test bản THẬT**: `println` (lib strip Log ở release). Đã cài máy test lại nhiều lần.
- [x] **IAP key** lấy đúng từ RN `utils_app/envApp.js` — đã audit xác nhận không thiếu SKU nào.
- [x] Slot ad tự ẩn khi fail (không kẹt shimmer) — kế thừa từ vga34d, không đổi logic.
- [x] Namespace/salt encode đúng app hiện tại trước khi chạy `push.sh build_tool` — đã tự kiểm tra
      `rename-config-global.js` trước khi chạy lần này (bài học từ vga34d từng dính salt sai app cũ).
- [x] **Pull file thật để verify khi nghi ngờ UI/overlay** — `adb shell run-as <pkg> cat <file> >
      local` LÀM HỎNG dữ liệu nhị phân trên Windows/Git Bash → dùng `adb exec-out run-as <pkg> cat
      <file> > local` (binary-safe). Từng tưởng nhầm ảnh hỏng là bug overlay.

## J. i18n
- [x] Kế thừa từ vga34d-kotlin (`strings_app_i18n.xml` tách riêng UI text khỏi `strings.xml`
      config/baseapp) — đã cập nhật thêm `theme`→"Stamp", `grant_permission_sub` mention "GeoStamp
      Camera", `saved_images`→"Saved" (yêu cầu user 2026-07-06).
- [ ] Chưa dịch lại 16 ngôn ngữ cho các key MỚI riêng vga34b (nếu có key UI mới không có trong
      vga34d) — cần rà lại khi rảnh.

## K. Đã gỡ (kế thừa từ vga34d, XEM LẠI mục C — có thể là gỡ NHẦM)
- [x] `ShortcutHelper.kt`/`UninstallScreen.kt`/route `Screen.Uninstall` — đã xoá lúc clone, LÚC ĐẦU
      tưởng là dead code của vga34d, nhưng audit agent (2026-07-06) xác nhận RN vga34b **CÓ** tính
      năng Uninstall thật (khác giả định trước) → xem mục C, chưa quyết định port lại hay bỏ hẳn.
