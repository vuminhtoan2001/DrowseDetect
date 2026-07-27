# Ghi chú tích hợp & sửa lỗi — vga_48 (Happy Coloring Art Game, `com.colorgame.artfunny`)

> App tô màu (Compose + Hilt + Room + Retrofit/Gson + libpag). Đã tích hợp thư viện
> `base-application` (ads/IAP/splash/language/notification). Tài liệu quy trình chung xem
> `PLAYBOOK_TICH_HOP.md`. File này ghi **các lỗi RIÊNG của vga_48 khi build R8/obfuscate** và
> **logic lưu/khôi phục tô màu** — vì debug chạy OK nhưng bản release/build server mới lộ lỗi.

---

## A. TÓM TẮT TÍCH HỢP
- Branch làm việc: `tich_hop_ads`. Remote: `origin` (bản gốc đầy đủ, có docs) + `build_tool`
  (bản encoded+strip, đẩy nhánh **`main`** — xem §E).
- Đã đổi: minSdk 24→28, Java/Kotlin 8→17, AGP 8.6→8.12, Gradle 8.7→8.13, Kotlin→2.1.20,
  Firebase BoM 32.8.1→34.1.0 (bỏ `-ktx`). `MyApplication : BaseApplication` (`core/Hilt.kt`).
- Vỏ khởi động/monetize dùng lib; gỡ splash/uninstall/notification của app; ads GMA→`com.nlbn.ads`;
  RemoteConfig (`fr/Remote.kt`) ủy quyền `FirebaseRemoteConfigUtil`; premium = `IAPUtils.isPremium()`.
- Bỏ màn xin quyền: Intro → thẳng Home (ListImage). Nút crown ở Home mở màn IAP của lib
  (`IapActivity.start(activity, "home")`).
- `rememberRipple` bị deprecation-ERROR (lib ép Compose BoM 2024.12): thêm
  `@file:Suppress("DEPRECATION_ERROR","DEPRECATION")` đầu 8 file UI.
- Giá trị thật: admob `~6774875100`, fb `1344206207171292` / token `d8f6…`, appsflyer `Qhno4yJY6KHmZp9uS9DRe4`,
  `public_license_key` của com.colorgame.artfunny (Play Console). Keystore: `app/keystore/colorfun.jks`
  (copy từ `app/src/main/java/.../count/colorfun.jks`, pass `123456`, alias `key0`).

---

## B. ⚠️ LỖI BUILD R8 / RELEASE (debug OK, release lỗi) — QUAN TRỌNG NHẤT

### B.1 Home không load danh sách ảnh ("Chưa có ảnh… / Thử lại")
- **Nguyên nhân:** **R8 FULL MODE** (mặc định AGP 8) XOÁ generic signature của Retrofit
  `suspend fun getPaintingCategories(): Response<List<Category>>`. Retrofit **2.9.0 KHÔNG kèm**
  rule R8-full-mode → GsonConverter nhận kiểu bị erase → parse JSON thành `LinkedTreeMap` thay vì
  `Category` → ClassCastException → list rỗng.
- **Fix:** (proguard) giữ interface service + generic:
  ```
  -keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault, Exceptions
  -keep,allowobfuscation,allowshrinking class retrofit2.Response
  -keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
  -if interface * { @retrofit2.http.* <methods>; }
  -keep,allowoptimization,allowobfuscation,allowshrinking interface <1>
  -keep interface com.colorgame.artfunny.data.net.ApiService { *; }
  ```

### B.2 Palette "select color" rỗng + không tô được
- **Nguyên nhân:** CÙNG GỐC B.1 — `Gson().fromJson(json, object: TypeToken<List<ColorGroup>>(){}.type)`
  (và `TypeToken<List<Area>>`) bị R8-full-mode erase generic → parse ra LinkedTreeMap →
  ClassCastException **bị `catch{}` nuốt lặng** (trong `Color.kt`/`Home`). colors.json + area đọc OK
  nhưng parse hỏng → palette rỗng → chọn màu không được → tô không được.
- **Fix:** hard-keep Gson TypeToken + (quan trọng) **tắt R8 full mode** ở §B.4.

### B.3 Click item → CRASH `SIGABRT: JNI DETECTED ERROR: java_class == null` (libpag / PAGView)
- **Nguyên nhân:** libpag (`com.tencent.tav:libpag`) load `libpag.so`; `JNI_OnLoad` tra class Java
  **bằng tên** (`FindClass`/`GetMethodID`). AAR libpag **KHÔNG kèm consumer-proguard** → R8 shrink/rename
  xoá hoặc đổi tên các class chỉ được tham chiếu từ native → `java_class==null` → abort khi mở canvas
  (PAGView.<init>). Class then chốt bị đổi tên: **`androidx.exifinterface.media.ExifInterface`** (→ `T1.g`).
- **Fix (proguard):**
  ```
  -keep class org.libpag.** { *; }
  -keep class org.extra.tools.** { *; }
  -keep class org.extra.relinker.** { *; }
  -keep class org.ffavc.** { *; }
  -keep class com.tencent.tav.** { *; }
  -keep class androidx.exifinterface.** { *; }
  -keepclasseswithmembernames class * { native <methods>; }
  -dontwarn org.libpag.**, org.extra.**, org.ffavc.**, androidx.exifinterface.**
  ```
- **Cách soi class libpag JNI cần:** `strings lib/arm64-v8a/libpag.so | grep -E '^(org|androidx)/'`.
  Check bị rename chưa: `grep '^androidx.exifinterface' app/build/outputs/mapping/release/mapping.txt`.

### B.4 QUYẾT ĐỊNH THEN CHỐT — tắt R8 full mode
`gradle.properties`:
```
android.enableR8.fullMode=false
```
→ diệt DỨT ĐIỂM cả lớp lỗi erase-generic (B.1 + B.2). Vẫn giữ shrink/obfuscate; chỉ tắt phần
tối ưu quá tay làm mất generic signature (Retrofit/Gson reflection). Các keep-rule libpag/exif (B.3)
vẫn cần trong cả 2 chế độ.

---

## C. LOG TRONG BẢN RELEASE BỊ NUỐT
- Consumer `proguard.txt` của **base-application AAR** có
  `-assumenosideeffects class android.util.Log { v; d; i; w; e; wtf; println; }`
  → **MỌI `Log.*` (kể cả `Log.e`) bị xoá ở release**. Đặt `Log.d/e` để debug release là VÔ ÍCH.
- **Cách debug release:** dùng `System.out.println("PREFIX | …")` (class `PrintStream`, KHÔNG bị
  assumenosideeffects) → hiện trong logcat tag `System.out`. Hoặc thêm `-dontoptimize` (assumenosideeffects
  chỉ tác dụng khi optimize bật) — nhưng mất tối ưu.
- Repo build là **release-not-debuggable** → KHÔNG `adb run-as` pull được Room DB.

---

## D. LỊCH SỬ / LƯU & KHÔI PHỤC TÔ MÀU (logic app)
Cơ chế: tô 1 level → nhấn next → dialog "Xem quảng cáo để next level" → xem ad → **lưu** phần đã tô →
sang level kế. Mở lại level → **khôi phục** phần đã tô. `ColoringVersion` (Room, `imageId = imageClassify`).

**2 lỗi đã sửa (trong `com/ui/theme/lay/Color.kt` = màn Home, package `com.ui.theme.src`):**
1. **Mở lại KHÔNG khôi phục:** mở item từ home không truyền `versionId` → `loadColoringVersion` không
   được gọi. FIX: khi mở item mà `versionId==null` → tự tra `getRecentVersionsByImageId(imageClassify, 1)`,
   nếu có bản lưu thì `currentEditingVersionId = it.id` + `colorArea.loadColoringVersion(it.id)`.
2. **Next-level (xem ad) KHÔNG lưu:** `proceedToNextLevel` trước đây chỉ điều hướng, không lưu.
   FIX: lưu trước khi sang level (update nếu có `currentEditingVersionId`, else tạo mới) rồi mới navigate.
   (Icon con mắt vốn đã lưu — chỉ luồng next-level thiếu.)

**Ghi chú kỹ thuật:** `applyColoringVersion` cần `regionColorMap` (từ `setAreas`) + `colorLayer`/`colorBitmap`
sẵn sàng mới tô lại được → luồng load bitmap+area chạy TRƯỚC rồi mới `loadColoringVersion` (đã đảm bảo
trong LaunchedEffect load ảnh). Nếu vùng lưu không khớp `regionColorMap` sẽ không tô lại (kiểm bằng
`applyColoringVersion FILLED=N`).

---

## E. PUSH — `./push.sh`
- `origin` (dev, đầy đủ, có docs) đẩy nhánh **`tich_hop_ads`**.
- `build_tool` (build server) đẩy nhánh **`main`** với **`--force`** (biến `BUILD_PUSH_BRANCH="main"` trong
  `push.sh`). Lý do: nhánh trên build_tool bị **protected** → force-push nhánh cũ bị chặn; đội build đặt
  `main` là default + cho phép force. build_tool tự **strip `.md`/encode-scripts/drive-download** →
  **docs KHÔNG lên build_tool** (đúng thiết kế; docs chỉ ở origin).
- Luồng build_tool: encode (rename symbol/drawable/file/chuỗi) → `./gradlew assembleRelease` (BUILD TEST) →
  nếu OK: strip + `git push build_tool tb:main --force`; nếu FAIL: `node reverse.js`, không push.

---

## F. CHECKLIST NGHIỆM THU (đã đạt trên bản encoded+R8, máy R92X7128G2Y)
- [x] Home load danh sách ảnh (Retrofit/Gson OK).
- [x] Mở item → canvas không SIGABRT (libpag/exif keep).
- [x] Palette "select color" có màu; tô màu được.
- [x] Next-level (xem ad) → lưu; mở lại item → khôi phục màu đã tô.
- [x] Nút crown → màn IAP của lib.
- [x] Bản encoded (build server) chạy đúng như bản release local.
