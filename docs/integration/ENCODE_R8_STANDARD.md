# 16 — Chuẩn encode-scripts + proguard-rules (mô hình "2 file per app")

> Chuẩn hoá bộ `encode-scripts/` và `proguard-rules.pro` của **vga52a-kotlin** làm bản gốc tham
> chiếu, kèm catalog lỗi thực chiến đã gặp khi áp dụng cho nhiều app khác nhau. Đọc file này
> **trước khi đụng vào encode/R8** — phần lớn lỗi "chỉ crash ở bản release" đến từ 2 mảng này.

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
cd encode-scripts && npm install         # 1 lần
node rename-all.js                       # chạy cả pipeline, dừng ngay nếu 1 bước lỗi
(cd .. && ./gradlew assembleRelease)     # build test
node reverse.js                          # khôi phục code gốc sau khi build xong / khi lỗi
```

### ⚠️ Chạy TRONG `encode-scripts/`, KHÔNG copy script ra repo root
Cả bộ script giải đường dẫn theo **vị trí của chính nó**, nên phải chạy tại chỗ:

| Chỗ | Cách giải đường dẫn | Chỉ đúng khi ở |
|---|---|---|
| `rename-*.js`, `reverse.js` | `path.resolve(__dirname, config.srcCodePath)` với `srcCodePath: "../app/src/main"` | `encode-scripts/` |
| `firebase-merge.js` | `ROOT = process.cwd()` rồi `path.join(ROOT, config.srcCodePath, …)` | `encode-scripts/` |
| `keychange*.json` | ghi cạnh script | encode và `reverse.js` phải cùng chỗ |
| `rename-all.js` | `execSync(…, { cwd: __dirname })` | tự truyền, chạy đâu cũng được |

Bản `push.sh` cũ copy 10 script ra root rồi chạy ở đó (`copy_scripts_to_root`) — xem mục 3.12 để biết
vì sao đó là bug, không phải lựa chọn. `push.sh` hiện tại chạy tại chỗ qua `run_encode_in_place`.

### `reverse.js` khôi phục ĐÚNG NỘI DUNG, không byte-exact
Đã đo trên vga58a (encode → reverse, không build): 4 bước chạy sạch, 24 drawable + 20 file ngôn ngữ +
282 symbol đổi tên, reverse xong **21 file `strings.xml` vẫn hiện `M` trong `git status`**. So sánh
ngữ nghĩa từng cặp `name → text` bằng parser XML: **giống hệt 21/21** — `rename-languages.js` ghi lại
file nên đổi thụt lề (6 → 4 space) và bỏ dòng trống, reverse trả đúng key nhưng không trả whitespace.
Vì vậy: **đừng dùng "working tree sạch" làm bằng chứng reverse thành công** — phải so nội dung. Sau
một lần `push.sh build_tool` thất bại, chạy `git checkout -- app/src/main/res` để dọn phần dư này
(chỉ an toàn khi đã commit hết trước lúc chạy, đúng như §E.5 #2 yêu cầu).

`push.sh build_tool` đã được cập nhật để gọi `node rename-all.js` (không còn mảng `ENCODE_SCRIPTS`
hardcode) — tự dừng + tự reverse nếu 1 bước lỗi giữa chừng (trước đây `|| warn ... tiếp tục` sẽ chạy
tiếp các bước sau dù 1 bước đã lỗi, để lại code rename dở dang).

### ⚠️ Thứ tự bước KHÔNG được đảo
`rename-file.js` **phải chạy trước** `rename-code-symbols.js`. Lý do: `rename-file.js` có bước
`updateClassNamesInFiles()` tìm class theo **tên cũ** để đồng bộ lại tên class cho khớp tên file mới
— nếu code-symbols chạy trước và đã đổi tên class thành hash, bước này sẽ không tìm thấy tên cũ nữa.
`rename-all.js` đã cố định đúng thứ tự này, kèm comment giải thích ngay trong file.

## 3. Catalog bug thật đã gặp

### 3.1 Namespace/salt cấu hình sai — lớp bug nghiêm trọng nhất
- **Đã xảy ra + đã sửa (ở chính vga52a-kotlin).** `rename-config-global.js` copy nguyên từ app mẫu
  trước đó, sót `BASE_NAMESPACE` = namespace app CŨ. Hậu quả: `PROTECTED_PACKAGE_ROOT` không khớp
  package gốc thật → cơ chế bảo vệ `import R`/`BuildConfig` + không đổi tên package gốc **mất tác
  dụng hoàn toàn** → file ở package gốc bị đổi tên/dời package → `R`/`BuildConfig` "Unresolved
  reference" khi build release.
- **Quy tắc phòng ngừa**: LUÔN đối chiếu `BASE_NAMESPACE` trong `rename-config-global.js` với
  `namespace`/`applicationId` trong `app/build.gradle.kts` TRƯỚC LẦN ĐẦU chạy encode cho 1 app —
  không tin tưởng file copy từ app trước, dù trông "có vẻ đã điền đúng". Đây là bug **dễ xảy ra
  nhất và hậu quả nặng nhất** khi clone bộ script này sang app mới, vì file trông giống hệt file gốc
  và build DEBUG vẫn chạy bình thường (chỉ lộ ra ở release/R8).

### 3.2 Manifest không theo kịp rename package/class
- **Bug gốc đã từng gặp + đã sửa**: `updateManifest()` bản đầu chỉ patch tên CLASS, không patch
  đường dẫn PACKAGE trong `android:name="..."` → `lintVitalRelease` báo `MissingClass`.
- **Fix chuẩn hiện tại (đã verify bằng dry-run thật — xem mục 5)**: `rename-file.js` dùng
  `ownNameAttrRegex` bắt MỌI `android:name="..."` bắt đầu bằng `.` (dạng rút gọn) hoặc bằng
  `BASE_NAMESPACE` (dạng đầy đủ), rồi thay từng SEGMENT folder đã đổi tên bên trong — xử lý đúng cả
  2 dạng, kể cả khi nhiều segment lồng nhau bị đổi tên cùng lúc. **Không** đụng tới `android:name`
  của thư viện ngoài (vd `com.google.android.gms.ads.AdActivity`) nhờ regex chỉ khớp giá trị bắt đầu
  bằng `.` hoặc đúng `BASE_NAMESPACE`.

### 3.3 FQN inline trong code không được patch
- `new com.example.package.SquareLayout(this)` (gọi thẳng bằng đường dẫn đầy đủ giữa thân code,
  không qua `import`) → encode chỉ patch dòng `package`/`import`, không patch FQN literal giữa code
  → `javac: package com.example.package does not exist`. Sửa: bỏ FQN, chỉ dùng tên ngắn qua `import`.
  Cùng bản chất với gotcha "gọi `NativeInterHost()` phải qua import, không FQN inline" đã ghi ở
  `ADS.md` §6.5.
- `rename-file.js` hiện tại đã có PASS 1b xử lý tham chiếu full-path giữa dòng code (không chỉ dòng
  `import`) — nhưng **vẫn nên tránh viết FQN inline** ngay từ đầu, an toàn hơn là dựa vào encode
  script vá kịp mọi trường hợp.

### 3.4 Tên tự đặt trùng tên hàm/class hệ thống hoặc Compose built-in
- `data class Row` trùng `Row(...)` composable của Jetpack Compose → rename toàn cục phá luôn mọi
  `Row(...)` hợp lệ khác → `Unresolved reference`.
- `data class SolidColor` trùng `androidx.compose.ui.graphics.SolidColor`; hàm tự viết
  `insert()`/`save()`/`decode()` trùng API `ContentResolver.insert()`/`URLDecoder.decode()` dùng ở
  nơi khác trong code.
- Hàm `parse()` tự viết (vd trong 1 class kiểu `AdPositions`) trùng `Uri.parse`/
  `SimpleDateFormat.parse`.
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
- Nhiều app đọc Remote Config key **ĐỘNG** bằng ghép chuỗi
  (`remote.getBoolean("${placement}_enable")`) → `rename-config-key.js` chỉ tìm được chuỗi LITERAL
  trong code, không sửa được các chỗ gọi động → hash key trong `config.xml` nhưng code vẫn đọc tên
  gốc → lệch key → đọc default → **tắt hết ads/sai cấu hình**, khó phát hiện vì debug vẫn chạy đúng
  (unit test ads). Xem mục 4 để biết app nào cần bật/tắt.

### 3.8 R8 fullMode xoá generic signature → Gson trả sai kiểu
- `suspend fun getX(): Response<List<Category>>` bị R8-fullMode erase generic → Gson parse ra
  `LinkedTreeMap` thay vì model thật → `ClassCastException`, danh sách rỗng. Fix: tắt hẳn
  `android.enableR8.fullMode=false` trong `gradle.properties` (giữ shrink/obfuscate, chỉ tắt phần
  optimize làm mất generic) + đủ keep-rule Retrofit/Gson. **vga52a-kotlin đã tắt đúng** (`false`).

### 3.9 JNI/reflection bị R8 rename mà thiếu keep-rule → crash chỉ ở release
- SDK JNI/native (vd `libpag` của Tencent) crash `SIGABRT: java_class == null` vì 1 class được nó
  gọi bằng reflection (vd `androidx.exifinterface.media.ExifInterface`) bị R8 rename (AAR không kèm
  consumer-proguard đủ). Fix: keep đúng package gốc của SDK + `androidx.exifinterface.**` +
  `-keepclasseswithmembernames class * { native <methods>; }`.
- WebRTC (`org.webrtc.*`) cũng cùng lớp bug — crash dưới R8 do thiếu keep-rule, chỉ lộ ở release.
- OpenCV (`org.opencv.**`, dùng cho tính năng xử lý ảnh JNI như phát hiện góc tài liệu) cùng lớp bug
  — cần `-keep class org.opencv.** { *; }` + `-keepclasseswithmembernames class * { native <methods>; }`.
  **Quy tắc chung**: bất kỳ SDK nào dùng JNI/native method hoặc tra class bằng reflection đều cần
  keep-rule tương ứng, kiểm bằng cách build-test đúng tính năng dùng SDK đó ở bản R8 thật trước khi
  phát hành — thiếu rule này KHÔNG lộ lỗi ở build debug.
- **Bug khác (không phải proguard) cùng nhóm "chỉ lộ ở release"**: 1 module đọc file tự viết (vd
  parser OOXML `.xlsx/.docx/.pptx`) dùng reflection tra class bằng tên, bị R8 rename dù
  `encode-scripts/` không đụng tới (encode-scripts chỉ quét `app/src/main`, không đụng module Gradle
  riêng). **Bài học quy trình quan trọng**: khi nghi ngờ do encode, luôn test tách biệt 2 biến "có
  encode-scripts chạy hay không" và "có bật R8 minify hay không" (4 tổ hợp) — đừng gộp chung kết
  luận, dễ đổ oan cho sai cái này trong khi lỗi thật ở cái kia.

### 3.10 Trùng class do fat-AAR đã gói sẵn lib
- `Type com.github.ybq.android.spinkit... is defined multiple times` — module `:base-application`
  khai lại dependency mà AAR (fat-AAR) đã gói sẵn → trùng class lúc R8. Kiểm tra bằng
  `unzip -l classes.jar` (giải nén AAR) xem lib đã bundle chưa trước khi khai lại dependency.

### 3.11 `push.sh` — vận hành
- `_cleanup()` (trap EXIT) chạy sau khi hàm cha unwind → biến local có thể "unbound" dưới `set -u` →
  dùng `${var:-default}` thay vì tham chiếu trực tiếp.
- PHẢI commit hết trước khi chạy `push.sh` — script tự tạo branch tạm rồi `git checkout`, uncommitted
  changes sẽ mất. Guard đầu script chỉ bắt **staged** changes, file sửa-mà-chưa-`add` lọt qua — xem
  `PLAYBOOK_TICH_HOP.md` §E.5 #2.
- **Bất cứ thứ gì chặn `git checkout` lúc dọn dẹp = bị bỏ lại trên nhánh tạm.** Đã xảy ra 2 lần với 2
  nguyên nhân khác nhau: (1) `WORK_BRANCH` khai tên nhánh không tồn tại; (2) một file **untracked** chặn
  checkout — bản thử `git rm --cached push.sh` (để repo build không chứa file quy trình dev) làm
  `push.sh` thành untracked trên nhánh tạm, git từ chối ghi đè nó khi quay về `main`. Bài học: đừng đánh
  đổi 12 KB "sạch repo" bằng rủi ro làm hỏng đường về; và `_cleanup` phải **báo to** khi checkout thất
  bại thay vì `|| true` (bản hiện tại in hướng dẫn khắc phục kèm tên nhánh tạm).
- **`WORK_BRANCH` khai sai tên = bị bỏ lại trên nhánh tạm.** `_cleanup()` chạy
  `git checkout "$WORK_BRANCH" 2>/dev/null || true`: nếu nhánh đó không tồn tại (hay gặp khi copy
  `push.sh` từ app trước — file trông "đã điền đúng"), checkout thất bại và bị `|| true` **che im
  lặng**, rồi `git branch -D "$tb"` cũng thất bại vì không xoá được nhánh đang đứng. Kết quả: bạn còn
  ở nhánh tạm, mà ở luồng `build_tool` nhánh tạm là bản **đã xoá sạch `*.md` + `encode-scripts/`** →
  trông y như mất file, dễ tưởng encode script phá hỏng repo. Đối chiếu `WORK_BRANCH` với
  `git branch --show-current` trước lần đầu chạy cho một app mới. Đã gặp ở vga58a (khai
  `androidsdkads` trong khi repo chỉ có `main`) — cùng lớp lỗi "copy config từ app trước mà quên sửa"
  với `BASE_NAMESPACE` ở §3.1.

### 3.12 Copy script ra repo root → `srcCodePath` trỏ lên trên repo một cấp
- **Đã xảy ra ở vga58a + đã sửa.** `push.sh` cũ copy 10 script ra `$REPO_ROOT` rồi chạy `node
  rename-all.js` ở đó. Nhưng `srcCodePath: "../app/src/main"` được giải bằng
  `path.resolve(__dirname, …)`, mà `__dirname` giờ là repo root → `..` thành **thư mục cha của repo** →
  `ENOENT: scandir 'C:\workspace\java_app\app\src\main'`.
- **Hai điểm khiến nó khó lần ra:**
  1. Bước 1 (`rename-drawables.js`) **không chết ngay**: nó quét thư mục không tồn tại, in
     `✅ Đã đổi tên 0 file drawable`, ghi mapping rỗng, rồi mới chết ở bước cập nhật tham chiếu — đọc
     log tưởng "encode chạy được một phần".
  2. `reverse.js` dùng đúng cùng `srcCodePath` nên **cũng chết cùng lý do** → `trap` gọi reverse nhưng
     reverse không làm gì. Ở vga58a may là chưa file nào bị sửa (0 file rename) nên repo còn nguyên;
     nếu lỗi rơi vào bước 3 (`rename-file.js`) thì đã mất khả năng khôi phục tự động.
- **Bài học tổng quát**: cơ chế an toàn (`trap` + `reverse.js`) chỉ có giá trị khi nó chạy trong CÙNG
  điều kiện đường dẫn với encode. Cấu hình sai chỗ chạy làm hỏng CẢ HAI cùng lúc — chạy encode và
  reverse ở đúng một chỗ (`encode-scripts/`) là điều kiện để lưới an toàn còn tác dụng.
- **Cách kiểm nhanh cho app mới**, trước khi tin vào `push.sh`: chạy encode rồi reverse ngay, KHÔNG
  build (~10 giây), rồi so nội dung với `HEAD` bằng parser XML thay vì đọc `git status` (xem lý do ở
  mục 2, phần "không byte-exact").

## 4. `rename-config-key.js` — khi nào bật, khi nào tắt

File thân **giống hệt nhau 100%** ở mọi app đã khảo sát — không phải lõi có bug, mà là **cấu hình
bật/tắt** (qua `pipeline.renameConfigKey` trong `rename-config-global.js`) tuỳ cách app đọc Remote
Config key:

| App đọc key kiểu... | `renameConfigKey` |
|---|---|
| **Literal** cố định (`getString("some_key")`, key không ghép chuỗi runtime) | **BẬT** (`true`) — script quét được toàn bộ tham chiếu literal, đổi đồng bộ `config.xml` + code |
| **Động** (ghép chuỗi runtime: `getBoolean("${placement}_enable")`) — **đây là trường hợp của vga52a-kotlin** | **TẮT** (`false`, mặc định) — script không tìm được chuỗi động, chỉ hash `config.xml` → lệch key → tắt hết ads |

**Cách kiểm tra app của bạn thuộc nhóm nào**: `grep -rE 'getBoolean\(|getString\(|getInt\(' app/src`
— nếu thấy pattern `"${...}_..."` (ghép chuỗi/template) trong tham số key → nhóm ĐỘNG, giữ tắt.

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

## 6. "Công thức" proguard cho SDK phổ biến

Xem **[`PROGUARD_SDK_RECIPES.md`](PROGUARD_SDK_RECIPES.md)** — tách riêng thành file khác để khi có
SDK mới cần thêm rule, chỉ sửa file đó, không phải sửa file này.

## 7. Checklist khi migrate chuẩn này sang 1 app KHÁC

1. **Đối chiếu `BASE_NAMESPACE`** trong `rename-config-global.js` hiện tại của app đó với
   `namespace`/`applicationId` thật trong `app/build.gradle.kts` — sửa ngay nếu lệch (xem mục 3.1 —
   đây là lớp bug hay gặp nhất khi copy file cấu hình từ app trước sang mà quên sửa).
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
6. Cập nhật `push.sh` của app đó: thay mảng `ENCODE_SCRIPTS` hardcode bằng `node rename-all.js`, chạy
   **trong `encode-scripts/`** chứ không copy script ra root (mục 3.12), và đối chiếu `WORK_BRANCH`
   với `git branch --show-current` (mục 3.11).
7. Tách `proguard-rules.pro` hiện tại của app đó: phần trùng với `proguard-rules-base.pro` (Billing/
   AAR core/AdMob mediation/Gson/Crashlytics) → xoá khỏi file riêng, thêm `proguardFiles(...,
   "proguard-rules-base.pro", "proguard-rules.pro")` vào `build.gradle.kts`; phần còn lại (SDK riêng)
   giữ nguyên trong `proguard-rules.pro`.
8. **Dọn cruft**: với mọi tên trong `PROTECTED_FOLDER_NAMES`/`SAFE_SKIP_NAMES` cũ, chạy
   `grep -rln "\btên\b" app/src/main --include="*.kt"` — nếu 0 kết quả, đó là cruft (tên của 1 app
   TỔ TIÊN khác, copy nhầm qua nhiều lần clone — dễ tích tụ dần qua mỗi lần clone nếu không ai dọn) —
   xoá khỏi danh sách khi migrate, đừng copy tiếp sang app tiếp theo.
9. Build debug + `assembleRelease` (R8) thật, cài máy test đầy đủ tính năng — encode/proguard là loại
   lỗi **chỉ lộ ra ở bản release thật**, không phát hiện được bằng đọc code hay build debug.
