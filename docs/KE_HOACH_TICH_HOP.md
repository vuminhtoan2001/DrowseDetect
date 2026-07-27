# Kế hoạch tích hợp `base-application-1.0.0.aar` vào app `vga_49` (DetectorSpy)

> Tài liệu phân tích & lập kế hoạch. Nguồn: `drive-download-.../HUONG_DAN_TICH_HOP.md` +
> `MIGRATE_ADS_TO_LIB.md` + đọc trực tiếp source `vga_49`. (Đã thực thi xong — xem trạng thái cuối tài liệu.)

---

## 0. Bản chất vấn đề

App hiện tại là **Jetpack Compose single-Activity (`MainActivity`) + Hilt + Room**, tự xây toàn bộ
lớp vỏ khởi động/monetize: Splash/Language/Intro/Uninstall/Permission (Composable trong MainActivity),
hệ ads riêng (GMA+UMP+AppsFlyer), `RemoteConfigManager` riêng, billing/premium riêng, FCM + hệ
notification hành vi riêng.

Thư viện muốn **sở hữu chính lớp vỏ đó**: là **launcher** (Splash Activity), chạy
`Splash → Inter → Language → IAP → getHomeActivity()`, tự init ads + consent + FCM + notification, đọc
ad-unit từ **một key Remote Config `ads_config`** qua `FirebaseRemoteConfigUtil`.

➡️ Tích hợp = **thay lớp vỏ khởi động/monetize của app bằng vỏ của lib, giữ các màn tính năng**.

---

## 1. So sánh môi trường (phải nâng toolchain)

| Mục | App cũ | Lib yêu cầu |
|---|---|---|
| `minSdk` | 24 | **28** (mất Android 7.0/7.1/8.0) |
| Java / Kotlin JVM | 1.8 | **17** |
| AGP | 8.6.0 | **8.12.0** (cần Gradle ≥ 8.13) |
| Kotlin / KSP | 2.1.10 | **2.1.20** / `2.1.20-1.0.32` |
| google-services / crashlytics | 4.4.2 / 3.0.3 | **4.4.3** / **3.0.6** |
| Firebase BoM | 32.8.1 | **34.1.0** (BoM 34 đã bỏ artifact `-ktx`) |
| play-services-ads / ump / fb-mediation / appsflyer | 24.6.0 / 3.2.0 / 6.20.0.1 / 6.17.5 | **25.2.0 / 4.0.0 / 6.21.0.2 / 6.18.0** (do module AAR cấp — gỡ khai báo trùng ở app) |

---

## 2. Quyết định đã chốt

- **QĐ-A — minSdk 24→28**: ✅ chấp nhận.
- **QĐ-B — Ads**: ✅ bỏ init/consent/app-open của app, dùng `Admob`/`AppOpenManager` của lib; **giữ tầng điều phối** (AdsViewModel/NativeAdSlot/AdHelper/AdFrequencyManager/AdConfig), chỉ thay primitive (xem `MIGRATE_ADS_TO_LIB.md`).
- **QĐ-C — Language đầu vào**: dùng màn Language của lib; bỏ màn startup của app; giữ `LanguageSetting`.
- **QĐ-D — Billing/Premium**: ✅ `IAPUtils.isPremium()` là nguồn sự thật.
- **QĐ-E — Notification**: 🗑️ **BỎ TOÀN BỘ hệ notification của app, chỉ dùng lib** (chốt cuối).

---

## 3. Tài nguyên bắt buộc chuẩn bị (guide §5)

- Strings: `app_name`, `app_flyer_id`, `admob_app_id`, `facebook_app_id`, `facebook_client_token`, `public_license_key` + ~20 chuỗi noti/IAP.
- String-array (mỗi mảng **đúng 5**): `notification_title2`, `notification_message2`, `notification_button2`.
- Colors (`values/` + `values-night/`): `primaryColor` == `accentTone`; `text1` giống nhau 2 theme.
- Drawable: `icon_app`, `icon_notification` (5 mật độ, alpha 24dp), `img_document_preview` (365×174), `icon_1_iap`…`5`, `icon_noti_1`…`5`, `btn_background_primary`.
- `res/raw/splash_loading.json` (Lottie — có file mẫu kèm bộ download).
- `assets/default_ads_config.json` (8 key lib + placement app).
- Theme: thêm `tools:replace="android:theme"` nếu cần.
- `google-services.json` khớp `applicationId`.

---

## 4. Kế hoạch theo giai đoạn (đã cập nhật cách build)

> **Cách build cuối cùng (theo yêu cầu):** Phase 1→4 làm **một thể, KHÔNG build từng phase**;
> xong rồi **build debug test + sửa lỗi** cho tới xanh, sau đó mới sang **Phase 5 (R8/release)**.
> (Lần đầu thực thi đã build từng chunk để dễ truy lỗi; lần sau gộp.)

- **Phase 1 — Toolchain + module AAR**: nâng wrapper/AGP/Kotlin/KSP; tạo module `:base-application` (AAR + Groovy `build.gradle` khai transitive deps); app `minSdk 28` + Java/Kotlin 17 + multidex + viewBinding + buildConfig + `api(project(":base-application"))` + Firebase BoM 34.1.0; gỡ dep ads trùng.
- **Phase 2 — HostApplication + Manifest + Resources**: `MyApplication : BaseApplication()` (giữ `@HiltAndroidApp`) implement đủ hàm abstract; gỡ `MAIN/LAUNCHER` khỏi MainActivity; thêm meta-data admob/fb + 4 quyền; thêm assets/raw/strings/colors.
- **Phase 3 — Gỡ vỏ trùng + Ads migration + RemoteConfig + Premium**: gỡ Splash/Uninstall/Language startup; `RemoteConfigManager` → `FirebaseRemoteConfigUtil`; ads → `Admob.getInstance()`; `GlobalAds.isPremiumUser` → `IAPUtils.isPremium()`.
- **Phase 4 — Xóa notification app**: xóa toàn bộ `nofi/*` + FCM service app + CustomNotificationBuilder; dọn manifest.
- **→ BUILD DEBUG TEST + sửa lỗi** (xem §6 danh mục lỗi).
- **Phase 5 — R8/release**: bật `isMinifyEnabled=true` + `isShrinkResources=true` + signingConfig + proguard rules (xem MIGRATE §8).

---

## 5. DANH MỤC RỦI RO (phần quan trọng nhất)

| # | Rủi ro | Phòng ngừa |
|---|---|---|
| R1 | `setDefaultsAsync` trực tiếp **xoá `ads_config`** của lib → ads lib chết | RemoteConfigManager ủy quyền `FirebaseRemoteConfigUtil`, KHÔNG gọi setDefaultsAsync trực tiếp |
| R2 | Hai luồng consent (UMP) + 2 lần init MobileAds | Gỡ hẳn consent + MobileAds.initialize của app |
| R3 | Hai App-Open/resume ad | Gỡ khối App Open trong MainActivity.onResume |
| R4 | Hilt + BaseApplication không init graph | Giữ `@HiltAndroidApp`; init prefs trước super.onCreate nếu getHomeActivity cần |
| R5 | R8 lần đầu strip class (Gson/Retrofit/Facebook reflection) → crash chỉ ở release | Keep-rules đầy đủ; build+test release thật trước khi phát hành |
| R6 | minSdk 24→28 mất user | Xác nhận với product |
| R7 | Hai BillingClient acknowledge tranh nhau | Một nguồn sự thật IAPUtils |
| R8/R9 | (ĐÃ TAN BIẾN vì QĐ-E bỏ hết notification app) Hai FCM / hai FGS | — |
| R11 | Bỏ MAIN/LAUNCHER nhưng quên đổi startDestination → màn trắng | Đổi startDestination sang Home; test cold start |
| R13 | String-array noti < 5 / icon thiếu → ArrayIndexOutOfBounds | Đủ đúng 5 phần tử |

### 5b. NOTIFICATION — bỏ sạch của app, chỉ dùng lib (đã chốt)

**XÓA:** `firebase/FirebaseNotification.kt`; `nofi/{BehaviorNotificationManager, NotificationScheduler,
NotificationReceiver, NotificationHelper, LocalizedNotificationHelper, ScreenStateMonitorService,
DeviceUnlockReceiver, VolumeChangeObserver}`; `ui/theme/screen/CustomNotificationBuilder.kt`.

**MainActivity:** gỡ `initializeNotificationSystem`/`startScreenMonitorService`/`trackAppOpenBehavior`/
`trackNotificationClickIfNeeded`/`dismissNotificationIfNeeded`. Giữ `logNotificationClick` (analytics).

**Manifest:** gỡ service FCM app + ScreenStateMonitorService + receiver NotificationReceiver + meta-data
FCM icon; bỏ `SCHEDULE_EXACT_ALARM`. → còn **1 FCM + 1 FGS** của lib.

**Lợi ích:** R8/R9 (trùng FCM/FGS) biến mất; không cần subclass `BaseFirebaseMessagingService`.

---

## 6. Đối chiếu `app/build.gradle.kts` với guide §3.4 (đã đạt)

minSdk 28 ✓ · Java/Kotlin 17 ✓ · multiDexEnabled ✓ · debug `crashlyticsCollectionEnabled="false"` ✓ ·
release `isMinifyEnabled=true` + `isShrinkResources=true` ✓ · release `crashlyticsCollectionEnabled="true"` ✓ ·
viewBinding + buildConfig ✓ · `packaging` exclude atomicfu ✓ · `api(project(":base-application"))` ✓.

---

## 7. (chuyển sang §2 — Quyết định đã chốt)

---

## 8. Bổ sung — cơ học chuyển Ads sang lib

Xem chi tiết ở **`MIGRATE_ADS_TO_LIB.md`**. Tóm tắt:
- GIỮ: `AdsViewModel`, `NativeAdSlot`, `AdFrequencyManager`, `AdHelper`, `AdConfig`.
- XÓA: `GoogleMobileAdsConsentManager`, `AppOpenAdManager` (app), `MobileAds.initialize`, `RemoteConfigManager`(→FRC), `GlobalAds.isPremiumUser`(→IAPUtils).
- THAY primitive: native→`Admob.loadNativeAd`; inter→`Admob.loadAndShowInter`; show native dùng layout lib `ads_native_bot_2`/`ads_native_bot_no_media_short` + `pushAdsToViewCustom` (native full-screen không có layout lib → giữ view custom + `setNativeAd`, chỉ đổi loader).
- **`default_ads_config.json` phải chứa CẢ 8 placement lib LẪN mọi placement app** (lấy ad-unit thật từ `res/xml/config.xml`).

---

## 9. Giá trị thật đã thu thập (app cũ đã config sẵn)

| String | Giá trị / nơi |
|---|---|
| `admob_app_id` | `ca-app-pub-9405071173323068~8317174641` (AndroidManifest cũ) |
| `facebook_app_id` / `client_token` | `24765881169734891` / `661be10ae72ede8245c85c790a03a777` (manifest) |
| `app_flyer_id` | `Qhno4yJY6KHmZp9uS9DRe4` (MainActivity cũ) |
| `public_license_key` | `MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtTuKJD8HwpOzEMVugEDYZrYDEhw45kbx+b5PmlgdFomPTvApvXq7EoTCZEN2J/CHIS1jurQcfm5pdHxz8yFBXxlk6FwvBWWTI5LDdSqFA1UwXfClEBtdfuJi/tCnNPvaSopMP1kUVP8/K9JZIpZkRdZ3xCViPRBeeygH4JVM9xtKP48T1/pQgzxGAne0gVpbIoneswQvYnzNhro2+2SJWcMVBOdjRVBpkp7WljVVeggDuCwBB6FOXgYoZVwOUryXKsli+JlTEDjY5EDqUuHIC0cHaC6o/hZDzYIpd81ib19doNkcHUVaNLtPO6wRLq9NsFmlYwD/HCFha9NPuCs2pQIDAQAB` (user cấp) |
| IAP | base plan `features`; offers `free-trial`/`monthly`/`yearly` (ProductIds.kt) |
| Ad-unit | toàn bộ trong `res/xml/config.xml` (publisher `ca-app-pub-9405071173323068`) |

---

## 10. Trạng thái thực thi (nhánh `tich_hop_ads`)

Phase 1→5 đã xong, build debug + release (R8) xanh, nghiệm thu runtime trên device thật
(Splash lib → Inter ad AdMob → Language → MainActivity, không crash). Chi tiết lỗi đã gặp +
quy trình tái dùng: xem **`PLAYBOOK_TICH_HOP.md`**.
