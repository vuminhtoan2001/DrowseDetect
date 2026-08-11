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
- `signingConfig` release (keystore app).
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
| **`build_tool`** | Remote dành cho **HỆ THỐNG BUILD tự động** (server build AAB/APK để phát hành). | Code đã **OBFUSCATE** (đổi tên symbol/drawable/file/chuỗi…), đã **BUILD TEST** đảm bảo build được, đã **STRIP** file thừa (`.md`, `encode-scripts/`, `drive-download/`, `yarn.lock`, `debug.log`). |

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

### E.4 Quy trình bên trong (tóm tắt từ script)
- **origin flow:** tạo temp branch → `git add --all` → commit `"sync: full snapshot for origin"` →
  `git push origin <tmp>:tich_hop_ads --force-with-lease` → cleanup về branch làm việc.
- **build_tool flow:** tạo temp branch → `npm install` (encode-scripts) → copy script ra root →
  **chạy obfuscate** (`rename-drawables/languages/config-key/file/code-symbols.js`) →
  **`./gradlew assembleRelease` (BUILD TEST)**:
  - **Build FAIL** → tự `node reverse.js` (khôi phục code gốc) → **KHÔNG push**. (an toàn)
  - **Build OK** → xóa script + `keychange*.json` → strip `encode-scripts/` + `*.md` + `drive-download/` +
    `yarn.lock` + `debug.log` → commit `"sync: encoded build snapshot"` →
    `git push build_tool <tmp>:tich_hop_ads --force-with-lease`.

### E.5 Điều kiện tiên quyết (trước khi chạy)
1. **Đủ 2 remote** (E.2) — thiếu `build_tool` thì yêu cầu URL & add.
2. **Working tree sạch** — commit hết; script `die` nếu còn staged changes.
3. Branch làm việc/đẩy = `tich_hop_ads` (sửa biến `WORK_BRANCH`/`PUSH_BRANCH` trong `push.sh` nếu app khác branch).
4. Có `encode-scripts/` (các `rename-*.js`, `reverse.js`, `package.json`) + **Node.js** cài sẵn.
5. **Phase R8 (mục B) đã xanh local** — vì `build_tool` tự chạy `assembleRelease` (coi chừng lỗi 2 SpinKit,
   lỗi 5 mediation... ở mục C nếu dùng lib thật).

### E.6 Khi build_tool FAIL
Script tự reverse code về bản gốc và **không push** → bạn xem log lỗi build (thường là R8/lib thật ở
mục C), sửa, commit, rồi chạy lại `./push.sh build_tool`. `origin` không bị ảnh hưởng.

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
