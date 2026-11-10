# Bước 1 — Cập nhật SDK `base-application` + nâng compileSdk / targetSdk 36

Làm bước này **trước** mọi bước khác trong folder (xem [README.md](README.md)). Màn Language custom
cần API `LanguageRouter` của SDK, và các thư viện SDK kéo theo đòi version build mới hơn.

## 1. Thay 2 file SDK

Khi có bản SDK mới, chép **đè** `base-application-<ver>.aar` và `build.gradle` do đội SDK cung cấp vào
`<project>/base-application/`. Nếu tên file AAR không đổi thì không phải sửa `settings.gradle(.kts)` hay
`app/build.gradle` (app khai `api project(':base-application')`). Sau khi chép: build debug + release và làm
checklist mục 6.

### Bản SDK mới đổi gì

So sánh với bản SDK cũ (đọc trực tiếp nội dung AAR):

- **Không đổi:** API `LanguageRouter` (`customActivityClass`, `confirmLanguageSelection`…), danh sách
  Activity và quyền khai trong manifest của SDK. Code màn Language custom không phải sửa theo.
- **Có đổi bên trong:** Splash, IAP / billing, dialog inter ads, adapter của màn Language mặc định
  của SDK, một số layout/drawable của SDK.
- **`build.gradle` của SDK:** Facebook mediation `6.21.0.2` → **`6.22.0.0`**. Chính dòng này làm phải
  nâng compileSdk và Kotlin (mục 2, 3).

## 2. compileSdk và targetSdk lên 36

```groovy
// app/build.gradle
android {
    compileSdk 36
    defaultConfig {
        targetSdk 36
        // minSdk giữ nguyên (SDK yêu cầu tối thiểu 28)
    }
}
```

**Vì sao compileSdk bắt buộc lên 36:** Facebook mediation `6.22.0.0` kéo `androidx.browser:1.9.0`,
và thư viện này yêu cầu compileSdk ≥ 36. Để compileSdk 35 sẽ lỗi ngay ở bước kiểm tra AAR:

```
Dependency 'androidx.browser:browser:1.9.0' requires libraries and applications that
depend on it to compile against version 36 or later of the Android APIs.
:app is currently compiled against android-35.
```

(Đã kiểm bằng cách tạm hạ compileSdk xuống 35 rồi chạy `./gradlew :app:checkDebugAarMetadata`: đây là
**thư viện duy nhất** đòi 36.)

**Vì sao targetSdk lên 36:** không phải do thư viện, mà do Google Play. Từ **31/08/2026**, app mới và
bản cập nhật phải target Android 16 (API 36) mới được đẩy lên Play (có thể xin gia hạn tới
01/11/2026). Nguồn: developer.android.com/google/play/requirements/target-sdk.

> Nâng targetSdk là đổi hành vi app trên Android 16. Đọc mục 5 và test lại trước khi phát hành.

## 3. Bảng version tham khảo

Project SDK 35 cũ mà build lỗi sau khi thay SDK thì so với bảng này. Cột "Sau" là bộ version đã chạy
`compileDebugKotlin` thành công với SDK mới ở app Vexaro (`vga58a-download-video-app`).

| Thành phần | Khai ở đâu | Trước (SDK cũ) | Sau (SDK mới) | Bắt buộc? |
|---|---|---|---|---|
| compileSdk | `app/build.gradle` | 35 | **36** | Bắt buộc (mục 2) |
| targetSdk | `app/build.gradle` | 35 | **36** | Bắt buộc theo Google Play |
| Android Gradle Plugin (`com.android.application`) | `build.gradle(.kts)` gốc | 8.12.0 | 8.12.0 | Cần **≥ 8.10** để hỗ trợ API 36 (AGP 8.9 chỉ tới API 35) |
| Gradle wrapper | `gradle/wrapper/gradle-wrapper.properties` | 8.13 | 8.13 | Theo AGP (AGP 8.12 cần Gradle ≥ 8.13) |
| Kotlin (`org.jetbrains.kotlin.android`) | `build.gradle(.kts)` gốc | 2.1.20 | **2.2.21** | Bắt buộc ≥ 2.2 (mục 4, lỗi 2) |
| Kotlin Compose plugin (`org.jetbrains.kotlin.plugin.compose`) | `build.gradle(.kts)` gốc | 2.1.20 | **2.2.21** | Luôn bằng version Kotlin |
| KSP (`com.google.devtools.ksp`) | `build.gradle(.kts)` gốc | 2.1.20-1.0.31 | **2.2.21-2.0.5** | Phần trước dấu `-` phải bằng version Kotlin |
| Hilt plugin + `hilt-android` + `hilt-compiler` | gốc + `app/build.gradle` | 2.56.2 | **2.57.2** | Nâng kèm Kotlin 2.2 (3 chỗ cùng 1 số) |
| Room (`room-runtime`, `room-ktx`, `room-compiler`) | `app/build.gradle` | 2.6.1 | **2.7.2** | Bắt buộc nếu app dùng Room (mục 4, lỗi 3) |
| `androidx.lifecycle:lifecycle-process` | `app/build.gradle` | (không có) | 2.9.4 | Thêm cho Screen tracking + Full-screen intent |
| JDK | Android Studio / `JAVA_HOME` | 17 | 17 | AGP 8.x cần JDK 17 |
| Compose BOM | `app/build.gradle` | 2024.12.01 | 2024.12.01 | Không cần đổi |
| `google-services` / `firebase-crashlytics` plugin | `build.gradle(.kts)` gốc | 4.4.3 / 3.0.6 | 4.4.3 / 3.0.6 | Không cần đổi |

Mẫu khai ở `build.gradle.kts` gốc:

```kotlin
plugins {
    id("com.android.application") version "8.12.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
    id("com.google.devtools.ksp") version "2.2.21-2.0.5" apply false
    id("com.google.dagger.hilt.android") version "2.57.2" apply false
    id("com.google.gms.google-services") version "4.4.3" apply false
    id("com.google.firebase.crashlytics") version "3.0.6" apply false
}
```

Project dùng version catalog (`gradle/libs.versions.toml`) thì sửa các dòng tương ứng:

```toml
[versions]
agp = "8.12.0"
kotlin = "2.2.21"
ksp = "2.2.21-2.0.5"
hilt = "2.57.2"
room = "2.7.2"
lifecycle = "2.9.4"
```

**Không cần** lên AGP 9.x: AGP 9 đổi cách cấu hình Kotlin/DSL khá nhiều, là một đợt nâng cấp riêng.

## 4. Lỗi build đã gặp khi thay SDK và cách sửa

Gặp theo đúng thứ tự dưới đây: sửa xong lỗi trên mới lộ lỗi dưới.

**Lỗi 1 — compileSdk 35**

```
Dependency 'androidx.browser:browser:1.9.0' requires ... compile against version 36 or later
```

Sửa: compileSdk 36 (mục 2).

**Lỗi 2 — Kotlin quá cũ**

```
e: .../play-services-ads-25.4.0-api.jar!/META-INF/...kotlin_module Module was compiled with an
incompatible version of Kotlin. The binary version of its metadata is 2.3.0, expected version is 2.1.0.
```

Nguyên nhân: Facebook mediation `6.22.0.0` kéo `play-services-ads` lên `25.4.0` và
`kotlin-stdlib 2.3.0`, được biên dịch bằng Kotlin 2.3. Trình biên dịch Kotlin chỉ đọc được metadata
mới hơn nó **1 bản**: Kotlin 2.1 đọc tới 2.2, nên cần **Kotlin ≥ 2.2**.

Sửa: Kotlin + Compose plugin `2.2.21`, KSP `2.2.21-2.0.5`, Hilt `2.57.2` (bảng mục 3).

**Lỗi 3 — Room cũ không chạy với KSP2**

```
e: [ksp] java.lang.IllegalStateException: unexpected jvm signature V
```

Nguyên nhân: KSP bản `2.x` (đi với Kotlin 2.2) mặc định chạy **KSP2**; Room 2.6.x chỉ chạy với KSP1.

Sửa: Room `2.7.2` (cả 3 dòng `room-runtime`, `room-ktx`, `room-compiler`).

**Thư viện dùng KSP khác** (Moshi codegen, Glide KSP, Room…) ở app khác cũng có thể báo lỗi tương tự
lỗi 3 → nâng thư viện đó lên bản có hỗ trợ KSP2. App dùng `kapt` thay vì KSP thì không bị lỗi 3.

## 5. Hành vi đổi khi targetSdk 36 (Android 16) — phải test lại

Không làm build lỗi, nhưng chạy trên máy Android 16 sẽ khác. Kiểm tra trên **máy / emulator Android
16**:

| Thay đổi | Ảnh hưởng | Kiểm tra |
|---|---|---|
| **Không tắt được edge-to-edge nữa** (`android:windowOptOutEdgeToEdgeEnforcement` bị bỏ qua) | Màn nào chưa xử lý inset sẽ bị status bar / navigation bar đè lên nội dung | Mở mọi màn, xem header và nút đáy có bị che không |
| **Predictive back bật mặc định**: `Activity.onBackPressed()` không còn được gọi, `KEYCODE_BACK` không còn được gửi | Code back viết kiểu cũ không chạy. Code dùng `OnBackPressedDispatcher` / `BackHandler` (như màn Language mẫu) vẫn chạy | Bấm Back ở **mọi** màn, đặc biệt màn của SDK (IAP, Language mặc định có tham chiếu `onBackPressed` bên trong). Nếu lỗi và chưa sửa kịp: thêm `android:enableOnBackInvokedCallback="false"` vào `<application>` để tạm tắt |
| **Màn hình lớn (tablet, máy gập ≥ 600dp)**: bỏ qua `screenOrientation`, `resizeableActivity`, giới hạn tỉ lệ | App khoá dọc (`portrait`) vẫn bị xoay ngang trên tablet | Mở app trên emulator tablet, xoay màn |

## 6. Checklist bước 1

1. [ ] Chép đè 2 file `base-application/` (mục 1), `md5sum` khớp.
2. [ ] `compileSdk 36`, `targetSdk 36`.
3. [ ] AGP ≥ 8.10, Gradle wrapper đúng theo AGP, JDK 17.
4. [ ] Kotlin + Compose plugin ≥ 2.2, KSP cùng version Kotlin, Hilt nâng kèm.
5. [ ] Room ≥ 2.7 (nếu dùng), các thư viện KSP khác hỗ trợ KSP2.
6. [ ] Thêm `androidx.lifecycle:lifecycle-process:2.9.4` (dùng ở bước 2 và 4 của README).
7. [ ] `./gradlew :app:assembleDebug` và `:app:assembleRelease` đều xanh (release có R8, hay lộ lỗi
   thiếu class khi đổi thư viện).
8. [ ] Test trên máy Android 16 theo mục 5.
