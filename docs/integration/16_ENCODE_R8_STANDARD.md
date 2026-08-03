# 16 — Chuẩn encode-scripts + proguard-rules (mô hình "2 file per app")

> Kết quả rà soát 16 app Kotlin trong `vga49a` (2026-08-04) + chuẩn hoá lại bộ `encode-scripts/` và
> `proguard-rules.pro` của **vga52a-kotlin** làm bản gốc tham chiếu. Đọc file này **trước khi đụng
> vào encode/R8** — phần lớn lỗi "chỉ crash ở bản release" đến từ 2 mảng này.

## 1. Mô hình chuẩn: chỉ 2 file cần sửa khi clone app mới

```
encode-scripts/
├── rename-config-global.js   ⚙️ SỬA FILE NÀY — namespace/salt/pipeline + protected riêng app
├── rename-config-key.js      ⚙️ SỬA FILE NÀY — bật/tắt hash config key (xem mục 4)
├── safe-skip-base.js         🔒 LÕI — không sửa (danh sách bảo vệ dùng chung mọi app)
├── rename-file.js            🔒 LÕI — không sửa (đổi tên folder/file/package)
├── rename-code-symbols.js    🔒 LÕI — không sửa (đổi tên class/object/hàm)
├── rename-drawables.js       🔒 LÕI — không sửa
├── rename-languages.js       🔒 LÕI — không sửa
├── rename-all.js             🔒 LÕI — orchestrator, chạy toàn bộ pipeline bằng 1 lệnh
└── reverse.js                🔒 LÕI — không sửa (khôi phục code gốc)

app/
├── proguard-rules-base.pro   🔒 LÕI — không sửa (rule dùng chung: AAR/Billing/AdMob/Gson/Crashlytics)
└── proguard-rules.pro        ⚙️ SỬA FILE NÀY — rule riêng app (SDK đặc thù: CameraX/WebRTC/libpag/...)
```

**Tổng cộng 3 file cần sửa khi clone 1 app mới** (không phải 2 như dự tính ban đầu — thêm
`proguard-rules.pro` vì rule riêng SDK là bất khả kháng, mỗi app dùng SDK khác nhau):
`rename-config-global.js`, `rename-config-key.js`, `proguard-rules.pro`. Mọi file `.js` khác trong
`encode-scripts/` và `proguard-rules-base.pro` là **lõi dùng chung, copy nguyên, không sửa**.

### Cách hoạt động
- `safe-skip-base.js` export `SAFE_SKIP_BASE` / `PROTECTED_FOLDER_NAMES_BASE` /
  `PROTECTED_FOLDER_AND_FILE_BASE` — danh sách tên/folder **không bao giờ rename ở bất kỳ app nào**
  (keyword Kotlin, API lib ads/IAP dùng chung, tên Compose hay bị đặt trùng...).
- `rename-config-global.js` export thêm `SAFE_SKIP_APP_SPECIFIC` /
  `PROTECTED_FOLDER_NAMES_APP_SPECIFIC` / `PROTECTED_FOLDER_AND_FILE_APP_SPECIFIC` — **rỗng mặc
  định**, chỉ điền khi build R8 thật phát hiện 1 tên riêng của app đó bị rename nhầm.
- `rename-file.js`/`rename-code-symbols.js` require cả 2 file, gộp
  `[...safeSkipBase.X, ...config.X_APP_SPECIFIC]` — sửa 1 trong 2 chỗ đều có tác dụng, không cần sửa
  logic lõi.

## 2. Quy trình chạy

```bash
cd encode-scripts && npm install        # 1 lần
# copy toàn bộ *.js sang REPO_ROOT (push.sh làm tự động — xem push.sh: copy_scripts_to_root)
node rename-all.js                       # chạy cả pipeline, dừng ngay nếu 1 bước lỗi
./gradlew assembleRelease                 # build test
node reverse.js                           # khôi phục code gốc sau khi build xong / khi lỗi
```

`push.sh build_tool` đã được cập nhật để gọi `node rename-all.js` (không còn mảng `ENCODE_SCRIPTS`
hardcode) — tự dừng + tự reverse nếu 1 bước lỗi giữa chừng (trước đây `|| warn ... tiếp tục` sẽ chạy
tiếp các bước sau dù 1 bước đã lỗi, để lại code rename dở dang).

### ⚠️ Thứ tự bước KHÔNG được đảo
`rename-file.js` **phải chạy trước** `rename-code-symbols.js`. Lý do: `rename-file.js` có bước
`updateClassNamesInFiles()` tìm class theo **tên cũ** để đồng bộ lại tên class cho khớp tên file mới
— nếu code-symbols chạy trước và đã đổi tên class thành hash, bước này sẽ không tìm thấy tên cũ nữa.
`rename-all.js` đã cố định đúng thứ tự này, kèm comment giải thích ngay trong file.

## 3. Catalog bug thật đã gặp (rà soát toàn bộ 16 app + docs)

### 3.1 Namespace/salt cấu hình sai — lớp bug nghiêm trọng nhất
- **[vga52a-kotlin] Đã xảy ra + đã sửa.** `rename-config-global.js` copy nguyên từ `vga34b-kotlin`,
  sót `BASE_NAMESPACE: "com.camera.geostamp"` (namespace app CŨ). Hậu quả: `PROTECTED_PACKAGE_ROOT`
  không khớp package gốc thật → cơ chế bảo vệ `import R`/`BuildConfig` + không đổi tên package gốc
  **mất tác dụng hoàn toàn** → file ở package gốc bị đổi tên/dời package → `R`/`BuildConfig`
  "Unresolved reference" khi build release.
- **[vga32a-kotlin] ĐANG TỒN TẠI, CHƯA SỬA** (phát hiện qua khảo sát này, chưa có ghi chú trong docs
  của app đó). `encode-scripts/rename-config-global.js` khai `BASE_NAMESPACE:
  "com.supportsoftware.checkerpro"` (copy nguyên từ vga31b) nhưng namespace thật trong
  `app/build.gradle.kts` là `com.smartapp.waterremover`. **Cần sửa trước khi build release app này.**
- **Quy tắc phòng ngừa**: LUÔN đối chiếu `BASE_NAMESPACE` trong `rename-config-global.js` với
  `namespace`/`applicationId` trong `app/build.gradle.kts` TRƯỚC LẦN ĐẦU chạy encode cho 1 app —
  không tin tưởng file copy từ app trước, dù trông "có vẻ đã điền đúng".

### 3.2 Manifest không theo kịp rename package/class
- **[vga24c-kotlin] Bug gốc đã tìm ra + sửa**: `updateManifest()` bản đầu chỉ patch tên CLASS, không
  patch đường dẫn PACKAGE trong `android:name="..."` → `lintVitalRelease` báo `MissingClass`.
- **Fix chuẩn hiện tại (đã verify bằng dry-run thật — xem mục 5)**: `rename-file.js` của
  vga52a-kotlin dùng `ownNameAttrRegex` bắt MỌI `android:name="..."` bắt đầu bằng `.` (dạng rút gọn)
  hoặc bằng `BASE_NAMESPACE` (dạng đầy đủ), rồi thay từng SEGMENT folder đã đổi tên bên trong — xử lý
  đúng cả 2 dạng, kể cả khi nhiều segment lồng nhau bị đổi tên cùng lúc. **Không** đụng tới
  `android:name` của thư viện ngoài (vd `com.google.android.gms.ads.AdActivity`) nhờ regex chỉ khớp
  giá trị bắt đầu bằng `.` hoặc đúng `BASE_NAMESPACE`.

### 3.3 FQN inline trong code không được patch
- **[vga24c-kotlin]** `new com.controlcenter.SquareLayout(this)` (gọi thẳng bằng đường dẫn đầy đủ
  giữa thân code, không qua `import`) → encode chỉ patch dòng `package`/`import`, không patch FQN
  literal giữa code → `javac: package com.controlcenter does not exist`. Sửa: bỏ FQN, chỉ dùng tên
  ngắn qua `import`. Cùng bản chất với gotcha "gọi `NativeInterHost()` phải qua import, không FQN
  inline" đã ghi ở `14_ADS_RUNTIME_LESSONS.md`.
- `rename-file.js` hiện tại (vga52a) đã có PASS 1b xử lý tham chiếu full-path giữa dòng code (không
  chỉ dòng `import`) — nhưng **vẫn nên tránh viết FQN inline** ngay từ đầu, an toàn hơn là dựa vào
  encode script vá kịp mọi trường hợp.

### 3.4 Tên tự đặt trùng tên hàm/class hệ thống hoặc Compose built-in
- **[vga24c-kotlin]** `data class Row` trùng `Row(...)` composable của Jetpack Compose → rename toàn
  cục phá luôn mọi `Row(...)` hợp lệ khác → `Unresolved reference`.
- **[vga52a-kotlin]** `data class SolidColor` trùng `androidx.compose.ui.graphics.SolidColor`; hàm tự
  viết `insert()`/`save()`/`decode()` trùng API `ContentResolver.insert()`/`URLDecoder.decode()`
  dùng ở nơi khác trong code.
- **[toàn hệ]** Hàm `parse()` tự viết trong `AdPositions` trùng `Uri.parse`/`SimpleDateFormat.parse`.
- **Đã đưa vào `safe-skip-base.js` dùng chung** (không cần từng app tự vấp lại): `Row`, `Column`,
  `Box`, `Card`, `Icon`, `Image`, `Button`, `Text`, `Brush`, `Color`, `Shape`, `Modifier`,
  `SolidColor`, `insert`, `save`, `decode`. **Quy tắc chung**: tránh đặt tên hàm/class trùng tên
  hàm/class dựng sẵn của Compose hoặc API Android/Java chuẩn (`parse`, `insert`, `save`, `decode`,
  `Row`, `Text`...), dù có `private` — encode script rename theo text toàn cục, không hiểu scope.

### 3.5 `R.string`/`R.drawable` bị đổi nhầm tài nguyên hệ thống
- Nếu app đặt tên string/drawable trùng tên resource Android hệ thống (vd `cancel`, `ok`) và code có
  gọi `android.R.string.cancel`/`android.R.drawable.ic_menu_...`, script cũ sẽ rename nhầm cả tham
  chiếu `android.R.*` (không phải resource của app). **Đã sửa** trong `rename-drawables.js`/
  `rename-languages.js` (thêm negative lookbehind `(?<!android\.)` trước `R\.drawable\.`/`R\.string\.`
  — đã verify bằng test độc lập, xem mục 5).

### 3.6 Log bị strip ở release
- **[toàn hệ]** Consumer proguard của `base-application-1.0.0.aar` có
  `-assumenosideeffects class android.util.Log {...}` → mọi `Log.*` biến mất ở bản release. Dùng
  `println(...)` để log ở production, đọc qua `adb logcat -s System.out`.

### 3.7 Config key hash lệch với code đọc key động → tắt hết ads
- App dòng "-kotlin" (từ vga31a trở đi) đọc Remote Config key **ĐỘNG** bằng ghép chuỗi
  (`remote.getBoolean("${placement}_enable")`) → `rename-config-key.js` chỉ tìm được chuỗi LITERAL
  trong code, không sửa được các chỗ gọi động → hash key trong `config.xml` nhưng code vẫn đọc tên
  gốc → lệch key → đọc default → **tắt hết ads/sai cấu hình**, khó phát hiện vì debug vẫn chạy đúng
  (unit test ads). Xem mục 4 để biết app nào cần bật/tắt.

### 3.8 R8 fullMode xoá generic signature → Gson trả sai kiểu
- **[vga_48]** `suspend fun getX(): Response<List<Category>>` bị R8-fullMode erase generic → Gson
  parse ra `LinkedTreeMap` thay vì model thật → `ClassCastException`, danh sách rỗng. Fix: tắt hẳn
  `android.enableR8.fullMode=false` trong `gradle.properties` (giữ shrink/obfuscate, chỉ tắt phần
  optimize làm mất generic) + đủ keep-rule Retrofit/Gson. **vga52a-kotlin đã tắt đúng** (`false`).

### 3.9 JNI/reflection bị R8 rename mà thiếu keep-rule → crash chỉ ở release
- **[vga_48]** `libpag` (Tencent) native JNI crash `SIGABRT: java_class == null` vì
  `androidx.exifinterface.media.ExifInterface` bị R8 rename (AAR không kèm consumer-proguard đủ).
  Fix: keep `org.libpag.**`, `androidx.exifinterface.**`,
  `-keepclasseswithmembernames class * { native <methods>; }`.
- **[vga15b/15c-kotlin]** WebRTC (`org.webrtc.*`) crash dưới R8 do thiếu keep-rule, chỉ lộ ở release.
- **[vga40a-kotlin] ĐANG THIẾU (phát hiện qua khảo sát này) — `OpenCV`**
  (`com.websitebeaver:opencv:4.1.0`, dùng cho tính năng Scan PDF phát hiện góc tài liệu) không có bất
  kỳ `-keep class org.opencv.**`/`-keepclasseswithmembernames class * { native <methods>; }` nào
  trong `proguard-rules.pro`. Cùng lớp bug với libpag ở trên. **Cần build-test tính năng Scan PDF ở
  bản R8 thật trước khi phát hành app này.**
- **[vga40a-kotlin] Bug đã gặp + đã sửa (bug khác, không phải proguard)**: module đọc OOXML tự viết
  (`.xlsx/.docx/.pptx` → màn trắng) dùng reflection tra class bằng tên, bị R8 rename dù
  `encode-scripts/` không đụng tới (encode-scripts chỉ quét `app/src/main`, không đụng module Gradle
  riêng `:documentreader`). **Bài học quy trình quan trọng**: khi nghi ngờ do encode, luôn test tách
  biệt 2 biến "có encode-scripts chạy hay không" và "có bật R8 minify hay không" (4 tổ hợp) — đừng gộp
  chung kết luận, dễ đổ oan cho sai cái này trong khi lỗi thật ở cái kia.

### 3.10 Trùng class do fat-AAR đã gói sẵn lib
- `Type com.github.ybq.android.spinkit... is defined multiple times` — module `:base-application`
  khai lại dependency mà AAR (fat-AAR) đã gói sẵn → trùng class lúc R8. Kiểm tra bằng
  `unzip -l classes.jar` (giải nén AAR) xem lib đã bundle chưa trước khi khai lại dependency.

### 3.11 `push.sh` — vận hành
- `_cleanup()` (trap EXIT) chạy sau khi hàm cha unwind → biến local có thể "unbound" dưới `set -u` →
  dùng `${var:-default}` thay vì tham chiếu trực tiếp.
- PHẢI commit hết trước khi chạy `push.sh` — script tự tạo branch tạm rồi `git checkout`, uncommitted
  changes sẽ mất.

## 4. `rename-config-key.js` — khi nào bật, khi nào tắt

File thân **giống hệt nhau 100%** ở mọi app đã khảo sát — không phải lõi có bug, mà là **cấu hình
bật/tắt** (qua `pipeline.renameConfigKey` trong `rename-config-global.js`) tuỳ cách app đọc Remote
Config key:

| App đọc key kiểu... | Ví dụ | `renameConfigKey` |
|---|---|---|
| **Literal** cố định (`getString("some_key")`, key không ghép chuỗi runtime) | `vga_48`, `vga_49` | **BẬT** (`true`) — script quét được toàn bộ tham chiếu literal, đổi đồng bộ `config.xml` + code |
| **Động** (ghép chuỗi runtime: `getBoolean("${placement}_enable")`) | Mọi app "-kotlin" từ vga31a trở đi, **kể cả vga52a-kotlin** | **TẮT** (`false`, mặc định) — script không tìm được chuỗi động, chỉ hash `config.xml` → lệch key → tắt hết ads |

**Cách kiểm tra app của bạn thuộc nhóm nào**: `grep -rE 'getBoolean\(|getString\(|getInt\(' app/src`
— nếu thấy pattern `"${...}_..."` (ghép chuỗi/template) trong tham số key → nhóm ĐỘNG, giữ tắt.

Hướng "chuẩn mới" cho app **sau này** (ghi trong `HANDOFF_NEXT_APP.md`): vẫn muốn mã hoá key Firebase
nhưng bằng cách tái dùng key đã hash sẵn từ bản RN cũ (map readable→encoded qua `keychange.json` của
RN) thay vì tự hash lại bằng `rename-config-key.js` — tránh đúng lớp lỗi "key động" ở trên.

## 5. Đã kiểm chứng thật (không chỉ đọc code) — xem chi tiết cách test

Đã dựng 1 project Kotlin tối giản trong scratchpad (không đụng source thật) để chạy thật
`rename-file.js` → `rename-code-symbols.js` → `reverse.js`, xác nhận:
- `MainActivity`, folder `dao`, hàm `insert()` **không bị đổi tên** (protected list qua config hoạt
  động đúng sau khi tách khỏi hardcode).
- `android:name=".feature.intro.IntroActivity"` (dạng rút gọn, nhiều segment lồng nhau) sau khi
  folder `feature`/`intro` bị rename → Manifest tự cập nhật đúng thành
  `android:name=".d2e32a7.c0c1c2.F512715"` — khớp chính xác file/folder thật sau rename.
  `android:name="com.google.android.gms.ads.AdActivity"` (thư viện ngoài) **không bị đụng tới**.
- `data class Row` / `fun Row(...)` **không bị đổi tên** (defensive protection mới thêm hoạt động).
- `reverse.js` khôi phục **chính xác 100%** cấu trúc folder/file/nội dung ban đầu.
- Regex `(?<!android\.)R\.drawable\.cancel\b` match đúng `R.drawable.cancel`, **không** match
  `android.R.drawable.cancel`.

## 6. "Công thức" proguard cho SDK phổ biến (thêm vào `proguard-rules.pro` RIÊNG app khi cần)

`proguard-rules-base.pro` chỉ chứa rule THỰC SỰ dùng chung (AAR core, Billing, AdMob mediation, Gson,
Crashlytics). SDK dưới đây **tuỳ app có dùng hay không** — copy đúng khối cần, đừng copy thừa (dễ tạo
lại đúng loại "cruft" đã dọn ở bản base):

```proguard
# --- CameraX / Media3 / Room (app quay/chụp, lưu overlay/lịch sử) ---
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.**

# --- Google Maps ---
-keep class com.google.android.gms.maps.** { *; }
-keep class com.google.maps.android.** { *; }
-dontwarn com.google.android.gms.maps.**

# --- Retrofit (chỉ thêm nếu app dùng Retrofit, KHÔNG mặc định) ---
-keepattributes Exceptions, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault
-dontwarn retrofit2.**
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowoptimization,allowobfuscation,allowshrinking interface <1>

# --- jsoup (chỉ thêm nếu app gọi trực tiếp — phần lớn app trong dự án KHÔNG gọi trực tiếp,
#     jsoup chỉ là dependency gián tiếp của base-application AAR, không cần khai lại) ---
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# --- WebRTC (app video-call/P2P) ---
-keep class org.webrtc.** { *; }
-keepclassmembers class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# --- libpag / native JNI theo tên lớp cứng (app dùng animation Tencent libpag) ---
-keep class org.libpag.** { *; }
-keep class org.extra.tools.** { *; }
-keep class org.ffavc.** { *; }
-keep class com.tencent.tav.** { *; }
-keep class androidx.exifinterface.** { *; }
-keepclasseswithmembernames class * { native <methods>; }

# --- OpenCV (app xử lý ảnh bằng JNI, vd Scan PDF phát hiện góc tài liệu) ---
-keep class org.opencv.** { *; }
-dontwarn org.opencv.**
-keepclasseswithmembernames class * { native <methods>; }
```

## 7. Checklist khi migrate chuẩn này sang 1 app KHÁC trong số 15 app còn lại

Chuẩn này hiện **mới áp dụng cho vga52a-kotlin**. Khi migrate sang app khác, làm theo thứ tự:

1. **Đối chiếu `BASE_NAMESPACE`** trong `rename-config-global.js` hiện tại của app đó với
   `namespace`/`applicationId` thật trong `app/build.gradle.kts` — sửa ngay nếu lệch (xem mục 3.1,
   **vga32a-kotlin đang lệch, cần sửa trước tiên**).
2. Copy `safe-skip-base.js` + `rename-all.js` (LÕI, không sửa) vào `encode-scripts/` của app đó.
3. Trong `rename-file.js`/`rename-code-symbols.js` của app đó: thay đoạn khai `PROTECTED_FOLDER_NAMES`
   / `PROTECTED_FOLDER_AND_FILE` / `IGNORE_NAMES` / `SAFE_SKIP_NAMES` hardcode bằng cách `require`
   `safe-skip-base.js` + merge với `config.*_APP_SPECIFIC` (xem code hiện tại của vga52a-kotlin làm
   mẫu) — **trước khi xoá**, đối chiếu từng tên hardcode cũ của app đó với `safe-skip-base.js`: tên
   nào không có trong danh sách chung mà app đó thực sự dùng (kiểm bằng `grep -rn "\btên\b"
   app/src/main` — xem cách kiểm tra ở mục "dọn cruft" bên dưới) thì chuyển vào
   `SAFE_SKIP_APP_SPECIFIC` thay vì xoá mất.
4. Thêm `(?<!android\.)` vào `rename-drawables.js`/`rename-languages.js` của app đó (mục 3.5).
5. Thêm `pipeline: {...}` vào `rename-config-global.js`, xác định `renameConfigKey` bật/tắt theo mục 4.
6. Cập nhật `push.sh` của app đó: thay mảng `ENCODE_SCRIPTS` hardcode bằng `node rename-all.js`.
7. Tách `proguard-rules.pro` hiện tại của app đó: phần trùng với `proguard-rules-base.pro` (Billing/
   AAR core/AdMob mediation/Gson/Crashlytics) → xoá khỏi file riêng, thêm `proguardFiles(...,
   "proguard-rules-base.pro", "proguard-rules.pro")` vào `build.gradle.kts`; phần còn lại (SDK riêng)
   giữ nguyên trong `proguard-rules.pro`.
8. **Dọn cruft**: với mọi tên trong `PROTECTED_FOLDER_NAMES`/`SAFE_SKIP_NAMES` cũ, chạy
   `grep -rln "\btên\b" app/src/main --include="*.kt"` — nếu 0 kết quả, đó là cruft (tên của 1 app
   TỔ TIÊN khác, copy nhầm qua nhiều lần clone, xem ví dụ thật `smartbudget`/`trackermoney` từng lọt
   vào 15/16 app) — xoá khỏi danh sách khi migrate, đừng copy tiếp sang app tiếp theo.
9. Build debug + `assembleRelease` (R8) thật, cài máy test đầy đủ tính năng — encode/proguard là loại
   lỗi **chỉ lộ ra ở bản release thật**, không phát hiện được bằng đọc code hay build debug.

## 8. Rủi ro khác phát hiện qua khảo sát — cần xử lý riêng (không thuộc phạm vi chuẩn hoá này)

- **vga50a, vga51b**: `proguard-rules.pro` thiếu rule cho CameraX (dùng thật, comment "for spy camera
  capture") + Biometric, trong khi R8 fullMode đang mặc định `true` (chưa tắt như 11 app khác) — rủi
  ro cao hơn bình thường, nên build-test kỹ + cân nhắc tắt fullMode trước khi phát hành.
- **vga51b**: `proguard-rules.pro` có khối rule `-keep class
  com.voice.lock.ai.screen.ui.a74b0a.Theme.ThemeItem/...` — cấu trúc gói con giống hệt model của
  vga50a (app "look.sreen" — theme picker), nhiều khả năng copy nhầm nguyên khối, chỉ đổi tên package
  đầu. App vga51b (voice lock) khả năng không có class `ThemeItem/ThemeCategory` thật → rule chết vô
  hại, nhưng nên dọn lại cho đúng model Gson thật của app này (nếu có).
- **vga28b, vga_49, vga50a, vga51b**: đều dùng Billing (`com.android.billingclient:billing-ktx`)
  nhưng **thiếu rule Billing** trong `proguard-rules.pro` — rủi ro thấp-trung bình (Billing 7.x ít
  dùng enum/reflection nặng), nhưng không nhất quán với 12/16 app còn lại. Nếu áp dụng chuẩn này
  (mục 7), rule Billing sẽ tự có sẵn trong `proguard-rules-base.pro`.
