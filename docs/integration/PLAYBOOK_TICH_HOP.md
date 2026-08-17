# Build / Release / Push — checklist thực chiến (lib `base-application`)

> File này TRƯỚC LÀ playbook migrate lib `base-application` vào 1 app đã có sẵn splash/ads riêng —
> phần đó (Phase 1-4 cũ, gỡ vỏ trùng, đổi startDestination...) đã xoá vì **base skeleton của dự án
> giờ tích hợp lib sẵn từ đầu**, app mới clone từ skeleton không cần làm lại. Còn lại ở đây là phần
> **vẫn lặp lại ở MỌI lần build/release**, không phụ thuộc có migrate hay không: build R8/release,
> catalog lỗi build chung, test bằng adb, và cách đẩy code qua `push.sh`. Cách gọi ads xem `ADS.md`.

---

## A. Bản chất lib (ngắn — để hiểu vì sao checklist dưới ra như vậy)

Lib là **launcher** (`SplashActivity`), chạy `Splash → Interstitial → Language → IAP →
getHomeActivity()`, tự lo ads + consent (UMP) + init SDK + FCM + notification + foreground service +
app-open/resume, đọc mọi ad-unit từ **một** key Remote Config `ads_config`. App không khai
`MAIN/LAUNCHER`, không tự init ads/consent/FCM — xem `ADS.md` cho cách gọi ads.

---

## B. Phase R8 / Release (bắt buộc mỗi lần build bản phát hành)

- `buildTypes.release`: `isMinifyEnabled = true` **và** `isShrinkResources = true` (thiếu cái 2 → AGP báo lỗi).
- `manifestPlaceholders["crashlyticsCollectionEnabled"]` = `"false"` (debug) / `"true"` (release) — **xem lỗi 4 ở mục C**.
- `proguard-rules.pro`: keep `com.nlbn.**`, `com.brian.**`, `com.google.ads.mediation.**`, `com.facebook.ads.**`,
  Gson `TypeToken`/`Signature`/`@SerializedName`, **model app** (Room entity + DTO Gson/Retrofit), Crashlytics attrs.
- Build `assembleRelease` + **cài máy test runtime** (R8 chỉ lộ lỗi lúc chạy release, không lộ ở debug).

---

## C. Catalog lỗi build/R8 đã gặp (đọc trước khi build release lần đầu cho app mới)

| # | Lỗi / triệu chứng | Nguyên nhân | Cách sửa |
|---|---|---|---|
| 1 | `Could not find com.google.firebase:firebase-analytics-ktx:` (version rỗng) | Firebase **BoM 34.x đã bỏ artifact `-ktx`** | Đổi `firebase-analytics-ktx`/`-crashlytics-ktx`/`-config-ktx`/`-messaging-ktx` → bản **không `-ktx`** (`firebase-analytics`, …). KTX API nằm trong artifact chính. |
| 2 | R8 release: `Type com.github.ybq.android.spinkit... is defined multiple times` | **fat-AAR đã gói sẵn** SpinKit, mà module `:base-application` **khai lại** `Android-SpinKit` → trùng class | **Gỡ** dep bị trùng khỏi `base-application/build.gradle`. ⚠️ Kiểm tra dummy-vs-real: nếu lib THẬT không gói (NoClassDefFound runtime) → khai lại. Soi class bundled: `unzip -l classes.jar` trong AAR. |
| 3 | Server build: `Dangerous code: Hardcoded absolute path (starts with /)` | Linter server chặn **mọi string mở đầu `/`** trong `build.gradle.kts` — kể cả `excludes += "/META-INF/..."` (mẫu Android Studio) | Bỏ `/` đầu: `excludes += "META-INF/{AL2.0,LGPL2.1}"` + `"META-INF/atomicfu.kotlin_module"`. AGP vẫn loại đúng. |
| 4 | Build lib thật: lỗi merge manifest thiếu placeholder | Lib thật có `<meta-data ...="${crashlyticsCollectionEnabled}">`; app không định nghĩa placeholder | Thêm `manifestPlaceholders["crashlyticsCollectionEnabled"]` (debug `"false"`, release `"true"`) trong buildTypes. |
| 5 | Build fail: `R8 strip` / `ClassNotFoundException` mediation lúc chạy release | Adapter mediation (Meta/Facebook) **nạp bằng reflection** → R8 xoá | proguard keep `com.google.ads.mediation.**` + `com.facebook.ads.**` (xem mục B). |
| 6 | `okhttp`/`retrofit` mismatch | Lib kéo okhttp 5.x, app pin 4.x | Để Gradle resolve hoặc đồng bộ version; build report lỗi cụ thể thì xử lý. |
| 7 | (bẫy quy trình) background build báo **exit 0 nhưng thực ra FAILED** | Lệnh `./gradlew ... ; echo done` → exit code là của `echo` | **Luôn grep `BUILD SUCCESSFUL`/`BUILD FAILED` trong log**, đừng tin exit code của lệnh có `;`/`| tail`. |
| 8 | `minifyReleaseWithR8` chết giữa task: `Could not receive a message from the daemon` | `org.gradle.jvmargs=-Xmx2048m` KHÔNG đủ để R8 minify cùng fat-AAR `base-application` (rất nhiều SDK) — daemon bị OOM và chết, không có stacktrace Java nào | Nâng `-Xmx4096m` trong `gradle.properties`. Gặp thật ở app Video Downloader. |
| 9 | Room: `error: illegal start of expression` trong `*_Impl.java` do KSP sinh | Tham số của hàm DAO đặt tên là **từ khoá Java** (`private`, `class`, `new`…). Room sinh code JAVA nên tên hợp lệ trong Kotlin vẫn có thể vỡ | Đổi tên tham số (`private` → `isPrivate`). Lỗi chỉ lộ ở bước KSP/javac, biên dịch Kotlin vẫn xanh. |
| 10 | Thanh công cụ (hoặc bất kỳ nội dung Compose nào) **mất trắng** khi cùng màn có `WebView` | `WebView` trong `AndroidView` vẽ bằng hardware layer riêng, **xoá mọi nội dung Compose được vẽ TRƯỚC nó** trong cùng cha. Nội dung vẽ SAU vẫn hiện → rất dễ nhầm là lỗi màu/theme | Thêm `Modifier.zIndex(1f)` cho phần phải nằm trên (vị trí layout không đổi, chỉ đẩy xuống vẽ sau). Gặp thật ở màn Trình duyệt. |
| 11 | Toàn bộ màn có **nền đen** dù mọi màu đều là màu sáng | `themes.xml` kế thừa `Theme.Material3.DayNight.*` mà máy đang bật chế độ tối → `windowBackground` thành tối, lộ ra sau nội dung Compose | App không làm chế độ tối thì kế thừa `Theme.Material3.Light.*` + `AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)`. |

### Mẹo: lấy chữ ký chính xác hàm abstract / API lib (khi lib bump version)
`javap` trong JDK (vd `C:/Program Files/Java/jdk-21/bin/javap.exe`):
```bash
unzip -o classes.jar -d jardir   # classes.jar lấy từ trong AAR (giải nén AAR như zip)
javap -p -classpath jardir com.brian.base_application.BaseApplication   # liệt kê hàm abstract
javap -p -classpath jardir com.nlbn.ads.util.Admob                      # loadNativeAd/loadAndShowInter/...
javap -p -classpath jardir com.nlbn.ads.callback.AdCallback             # onNextAction/onAdFailedToLoad/...
```

---

## D. Build & test trên máy (adb)

```bash
adb devices                                    # xác nhận có device
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -c
adb shell monkey -p <applicationId> -c android.intent.category.LAUNCHER 1   # mở launcher (Splash lib)
adb shell pm grant <applicationId> android.permission.POST_NOTIFICATIONS    # cấp quyền cho luồng splash đi tiếp
# kiểm tra activity hiện tại + còn sống + crash:
adb shell dumpsys activity activities | grep -i ResumedActivity
adb shell pidof <applicationId>
adb logcat -d | grep -iE "FATAL EXCEPTION|AndroidRuntime|NoClassDefFound|ClassNotFound"   # lọc crash THẬT của app (theo pid)
```
- Nghiệm thu đúng: luồng `SplashActivity (lib) → Inter ad → LanguageActivity → IAP → MainActivity`, process sống, không FATAL.
- Test **cả debug lẫn release (R8)** (R8 chỉ lộ lỗi lúc chạy). Cài release phải `adb uninstall` bản debug trước (khác chữ ký).

---

## E. Cách dùng `./push.sh` (đẩy 2 remote)

### E.1 Mục đích — vì sao cần 2 remote?
Project được đẩy lên **2 remote khác nhau cho 2 mục đích khác nhau**:

| Remote | Mục đích | Nội dung đẩy |
|---|---|---|
| **`origin`** | Repo **dev/lưu trữ** của bạn (cộng tác, backup). | **Snapshot ĐẦY ĐỦ**, code **gốc (chưa obfuscate)**, kèm `encode-scripts/`, `docs/*.md`, `drive-download/`. |
| **`build_tool`** | Remote dành cho **HỆ THỐNG BUILD tự động** (server build AAB/APK để phát hành). | Code đã **OBFUSCATE** (đổi tên symbol/drawable/file/chuỗi…), đã **BUILD TEST** đảm bảo build được, đã **STRIP** mọi thứ không phải đầu vào build (`.md`, `encode-scripts/`, `docs/`, `docs-sample/`, `_ref/`, `drive-download/`, `yarn.lock`, `debug.log` — xem `EXCLUDED_PATHS` trong `push.sh`; riêng `push.sh` thì KHÔNG strip, lý do ở §E.5b). |

> Tóm lại: **`origin` = bản gốc để dev; `build_tool` = bản sạch + bảo vệ (obfuscate) để build/phát hành.**
> Tách 2 remote để code phát hành được bảo vệ mà bản dev vẫn đọc được bình thường.

### E.2 ⚠️ BẮT BUỘC: phải có remote thứ 2 (`build_tool` — Remote để BUILD)
`push.sh` cần **đủ 2 remote**. Kiểm tra:
```bash
git remote -v          # phải thấy CẢ 'origin' VÀ 'build_tool'
```
- Nếu **thiếu `build_tool`** (remote để build) → **PHẢI add trước khi chạy.** ⚠️ **Khi áp dụng cho app mới,
  hãy YÊU CẦU người dùng cung cấp URL remote build** (đừng tự đoán), rồi:
  ```bash
  git remote add build_tool <URL_REMOTE_BUILD>
  # ví dụ: git remote add build_tool http://gitlab.vgasoft.vn/vga-trip-build/<app>.git
  ```
- `build_tool` là remote do **đội build** quản (server tự pull + build). Không có nó thì không thể phát hành.

### E.3 Cách dùng
```bash
./push.sh             # đẩy CẢ 2 (origin trước, rồi build_tool)
./push.sh origin      # chỉ đẩy bản gốc (an toàn, không động build)
./push.sh build_tool  # chỉ obfuscate + build test + đẩy bản phát hành
```
Khuyến nghị lần đầu: chạy `./push.sh origin` trước (an toàn), kiểm tra origin OK, rồi `./push.sh build_tool`.

Mức kiểm tra code đã mã hoá (chỉ ảnh hưởng luồng `build_tool`, thứ tự tham số tự do):
```bash
./push.sh build_tool              # = --full, assembleRelease (mặc định)
./push.sh build_tool --quick      # compileReleaseKotlin + lintVitalRelease
./push.sh build_tool --no-build   # không kiểm gì
```
**Đọc §E.7 trước khi dùng `--quick`/`--no-build`** — mỗi mức bỏ sót một lớp lỗi khác nhau.

### E.4 Quy trình bên trong (tóm tắt từ script)
- **origin flow:** tạo temp branch → `git add --all` → commit `"sync: full snapshot for origin"` →
  `git push origin <tmp>:main --force-with-lease` → cleanup về branch làm việc.
- **build_tool flow:** tạo temp branch → `npm install` (encode-scripts) → **chạy obfuscate NGAY TRONG
  `encode-scripts/`** (không copy script ra root — xem `ENCODE_R8_STANDARD.md` §3.12) →
  **chạy obfuscate** (`rename-drawables/languages/config-key/file/code-symbols.js`) →
  **`./gradlew assembleRelease` (BUILD TEST)**:
  - **Build FAIL** → tự `node reverse.js` (khôi phục code gốc) → **KHÔNG push**. (an toàn)
  - **Build OK** → xóa script + `keychange*.json` → strip `encode-scripts/` + `*.md` + mọi mục trong
    `EXCLUDED_PATHS` → commit `"sync: encoded build snapshot"` →
    `git push build_tool <tmp>:main --force`.

### E.5 Điều kiện tiên quyết (trước khi chạy)
1. **Đủ 2 remote** (E.2) — thiếu `build_tool` thì yêu cầu URL & add.
2. **Working tree sạch — commit hết, kể cả file chưa `git add`.** Guard trong script chỉ `die` khi có
   **staged** changes (`git status --porcelain | grep -qE '^[MADRC]'` chỉ đọc cột INDEX), nên file
   sửa-mà-chưa-add vẫn lọt qua. Chúng sẽ được `git add --all` + commit lên **nhánh tạm**, push lên
   origin, rồi nhánh tạm bị `git branch -D`. Đã MẤT THẬT một lần như vậy — người dùng mở IDE ra thấy
   file trở về như cũ, tưởng bị linter revert. `push_origin` giờ `git merge --ff-only` nhánh tạm về
   nhánh làm việc trước khi xoá, nên thay đổi không mất nữa; nhưng **vẫn nên tự commit trước** để commit
   có thông điệp thật thay vì `"sync: full snapshot for origin [auto]"`.
3. **Branch làm việc/đẩy = `main` cho CẢ 2 remote** — `origin/main` giữ code gốc đầy đủ,
   `build_tool/main` giữ code đã mã hoá. Nếu app dùng tên nhánh khác thì sửa `WORK_BRANCH`/
   `PUSH_BRANCH`/`BUILD_PUSH_BRANCH` trong `push.sh`. ⚠️ `WORK_BRANCH` phải là nhánh **đang tồn tại
   và bạn đang đứng trên đó** — xem cảnh báo ở `ENCODE_R8_STANDARD.md` §3.11.
4. Có `encode-scripts/` (các `rename-*.js`, `reverse.js`, `package.json`) + **Node.js** cài sẵn.
5. **Phase R8 (mục B) đã xanh local** — chạy `./gradlew assembleRelease` trên code **GỐC (chưa
   encode)** trước. Không phải thủ tục cho vui: `build_tool` tự chạy `assembleRelease` **sau** khi
   obfuscate, nên nếu chưa biết R8 có xanh sẵn hay không thì lúc build fail bạn không tách được
   nguyên nhân là **R8/lib** hay **encode** — đúng bài học ở `ENCODE_R8_STANDARD.md` §3.9 (luôn tách
   riêng 2 biến "có encode" và "có minify"). Coi chừng lỗi 2 SpinKit, lỗi 5 mediation... ở mục C nếu
   dùng lib thật.

### E.5b Danh sách strip — `EXCLUDED_PATHS` trong `push.sh`
Repo build **chỉ cần thứ Gradle đọc**. Bước lọc cũ chỉ xoá `*.md`, nên ở lần push đầu của vga58a có
**104 MB** lọt sang `build_tool`: `docs/screenshot/*.png` (2.4 MB — file `.md` bị xoá nhưng ảnh thì
không), `docs-sample/` (100.6 MB, ảnh chụp app khác + file `.fig` Figma), `_ref/` (0.8 MB). Khi clone
sang app mới, mở `EXCLUDED_PATHS` và **rà lại theo cây thư mục thật của app đó** — mỗi app có tên
folder tài liệu/ảnh mẫu riêng, danh sách này không tự đúng.

Kiểm sau khi push (đừng đoán):
```bash
git ls-tree -r --name-only build_tool/main | awk -F/ '{print $1}' | sort | uniq -c | sort -rn
```
Chỉ nên còn `app/`, `gradle/`, `base-application/`, `gradlew*`, `*.gradle*`, `.gitignore`, **`push.sh`**.

⚠️ **`push.sh` cố tình KHÔNG strip** — đã thử và đã bỏ. Không thể `rm` nó như các mục khác vì bash đọc
file script theo từng chunk trong lúc chạy, xoá giữa đường thì các bước sau có thể âm thầm không chạy.
Cách thay thế là `git rm --cached` **sau** `git add --all` (làm trước thì chính lệnh đó add lại ngay) —
nhưng cách đó biến `push.sh` thành file **untracked** trên nhánh tạm, nên `git checkout main` lúc dọn dẹp
bị git từ chối ("would be overwritten by checkout") → người dùng bị bỏ lại trên nhánh tạm với code đã mã
hoá và `.md`/`encode-scripts/` đã xoá. **Đổi 12 KB "sạch repo" lấy rủi ro làm hỏng đường về là không
đáng** — đây là lý do `push.sh` vẫn nằm trên `build_tool/main`.

### E.5c `main` trên repo build là PROTECTED BRANCH — không force push được
```
remote: GitLab: You are not allowed to force push code to a protected branch on this project.
 ! [remote rejected] temp/build-… -> main (pre-receive hook declined)
```
Lần push **đầu tiên** qua được vì repo build còn RỖNG (tạo nhánh mới không tính là force). Từ lần 2 trở
đi `git push --force` luôn bị `pre-receive hook` chặn — mà nhánh tạm thì không thể fast-forward, vì nó
là con của `main` LOCAL nên không chứa commit đang là tip của `build_tool/main`.

`push.sh` giải bằng cách dựng commit đè lên đúng tip hiện tại của remote, không dùng force:
```bash
git fetch build_tool main
tree=$(git rev-parse "<nhánh-tạm>^{tree}")        # snapshot đã mã hoá + đã strip
parent=$(git rev-parse build_tool/main)            # tip hiện tại của remote
commit=$(git commit-tree "$tree" -p "$parent" -m "sync: encoded build snapshot [auto]")
git push build_tool "$commit:refs/heads/main"      # fast-forward → protected branch chấp nhận
```
Cách này còn **giữ được history bên repo build** thay vì ghi đè mất mỗi lần push. Nếu vẫn bị chặn thì
tài khoản của bạn không có quyền push vào nhánh protected — nhờ đội build cấp quyền Maintainer hoặc
mở `Settings → Repository → Protected branches`.

### E.6 Khi build_tool FAIL
Script tự reverse code về bản gốc và **không push** → bạn xem log lỗi build (thường là R8/lib thật ở
mục C), sửa, commit, rồi chạy lại `./push.sh build_tool`. `origin` không bị ảnh hưởng.

### E.7 Ba mức kiểm tra — mức nào BẮT ĐƯỢC lỗi gì

Đo thật trên vga58a: `npm install` ~2s, encode toàn bộ **4-7 giây** (24 drawable + 20 file ngôn ngữ +
282 symbol), strip + commit + push ~10s, còn bước kiểm tra chiếm gần 99% thời gian — nên chỉ bước đó
đáng để có tuỳ chọn.

| Mức | Thời gian đo được | Task Gradle | Chạy lại |
|---|---|---|---|
| `--full` | **11m 16s** | 66 | 48 (4 UP-TO-DATE, 13 FROM-CACHE) |
| `--quick` | **1m 23s** | 35 | 9 (25 UP-TO-DATE), **không chạy R8** |
| `--no-build` | ~0s | 0 | — |

**Gradle build tăng dần KHÔNG cứu được luồng có encode.** Phép đo `--full` ở trên chạy khi chỉ sửa
**đúng 1 file** so với lần build full trước đó, vậy mà 48/66 task vẫn phải chạy lại và vẫn mất 11 phút.
Lý do: encode đổi tên toàn bộ file/folder/class/hàm **và cả key trong `strings.xml`**, nên với Gradle đây
là một cây source khác → `parseReleaseLocalResources`, `processReleaseResources`, `kspReleaseKotlin`
(Room + Hilt sinh lại code), `compileReleaseKotlin`, R8, lint đều mất trạng thái. 13 task lấy được từ
cache đều là loại không phụ thuộc source (merge JNI/shader/asset). Đừng mất thời gian tinh chỉnh Gradle
cho nhanh hơn — cách duy nhất là **không chạy `--full` mỗi lần**.

⚠️ Con số `1m 23s` của `--quick` đo NGAY SAU một lần `--full` xanh, nên 25 task đã ấm sẵn. Chạy `--quick`
trên máy mới clone hoặc sau `./gradlew clean` sẽ lâu hơn đáng kể (phải biên dịch lại từ đầu).

| Lỗi encode (mã tham chiếu ở `ENCODE_R8_STANDARD.md`) | `--quick` | `--full` |
|---|---|---|
| §3.1 `Unresolved reference` R/BuildConfig (hay gặp nhất khi clone app mới) | ✅ | ✅ |
| §3.3 FQN inline giữa thân code không được patch | ✅ | ✅ |
| §3.4 Tên tự đặt trùng Compose/API hệ thống bị rename | ✅ | ✅ |
| §3.2 `MissingClass` — manifest không theo kịp rename package | ✅ *(chỉ `lintVitalRelease` bắt)* | ✅ |
| §3.10 Trùng class do fat-AAR đã gói sẵn lib | ❌ | ✅ *(chỉ R8 bắt)* |
| §3.8 R8 erase generic → Gson trả sai kiểu | ❌ | ⚠️ chỉ lộ lúc **chạy** |
| §3.9 Thiếu keep-rule JNI/reflection | ❌ | ⚠️ chỉ lộ lúc **chạy** |
| §3.7 Lệch key Remote Config → tắt hết ads | ❌ | ❌ |
| §3.12b Hash đập vào chuỗi JS/CSS *(xem dưới)* | ❌ | ❌ |

Hai điều rút ra từ bảng:
- **Phần lớn lỗi encode bị bắt ngay ở `compileReleaseKotlin`**, không cần tới R8 — nên `--quick` là lựa
  chọn có cơ sở, không phải làm tắt cho nhanh.
- **`--no-build` bỏ sót cả những lỗi biên dịch tầm thường.** Chỉ dùng khi VỪA chạy `--full` xanh và sau
  đó chỉ sửa thứ không đổi cấu trúc (text, màu, con số). Đổi tên/thêm/xoá file, thêm thư viện, sửa
  manifest → phải `--full`.

**Không mức nào thay được việc cài lên máy thật.** Ba lớp lỗi ở cuối bảng đều **build xanh, push thành
công, không sinh log nào** và chỉ sai khi chạy. Riêng lớp cuối đã gặp thật ở vga58a: `rename-code-symbols.js`
thay tên theo text toàn cục nên hash đập vào **bên trong chuỗi JS** — `fun absolute()` làm
`'position:absolute'` thành `'position:f6f4047'` → nút tải trong WebView lệch vị trí; `fun slot()` và
`fun download()` giết 4 selector của `AppPromptRemover`. Cách phát hiện duy nhất là **diff file gốc với
file đã encode**, hoặc:
```bash
grep -rnE "'[^']*(f|F|C|O)[0-9a-f]{5,7}[^']*'" app/src/main/java --include=*.kt
```
Hash nằm trong dấu nháy = đã đập vào nội dung chuỗi (bỏ qua chỗ hash nằm trong `${...}` — đó là nội suy,
tức code — và `const val TAG = "<tên class>"`). Tên gây lỗi thì thêm vào `SAFE_SKIP_APP_SPECIFIC`.

---

## F. Checklist QA trước phát hành

- [ ] Build **release** xanh với `minifyEnabled=true` + `shrinkResources=true`, không cảnh báo R8 strip lib.
- [ ] `app/build.gradle.kts` không có string mở đầu `/` (lỗi 3); có `crashlyticsCollectionEnabled` placeholder (lỗi 4).
- [ ] `android:name=".MyApplication"`/`HostApplication` khai đúng trong manifest + đủ 4 quyền FGS/POST_NOTIFICATIONS/AD_ID.
- [ ] Luồng `Splash → Inter → Language → IAP → Home` chạy đúng trên máy thật (debug + release).
- [ ] Màn IAP mở được từ Home & Settings; mua xong premium tắt ads.
- [ ] `assets/default_ads_config.json` đủ 13 key core (xem `ADS.md` mục 0.1) + placement app tự thêm.
- [ ] Thay drawable placeholder bằng ảnh thật đúng kích thước; điền giá trị thật (admob/fb/appsflyer/license/IAP)
      trước khi phát hành thật (xem `ADS.md` mục 0.1 — đang dùng id test).
- [ ] `res/values/sdk_strings.xml` (5 tính năng màn IAP, dialog xin quyền noti, nội dung notification...)
      đã viết lại đúng tính năng/tên app THẬT — đây là text mẫu của khung sườn, KHÔNG dùng nguyên khi
      build app mới. Giữ text ngắn gọn (nhiều ngôn ngữ dịch dài hơn tiếng Anh, view có giới hạn).
- [ ] `git remote -v` đủ 2 remote trước khi `./push.sh`.

---

## G. File tham chiếu
1. **PLAYBOOK_TICH_HOP.md** (file này) — build/release/push + catalog lỗi build.
2. **ADS.md** — cách gọi ads, setup lần đầu (test mode + key core), quy ước key tần suất.
