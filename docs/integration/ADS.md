# Quảng cáo — tích hợp `com.nlbn.ads` (Admob wrapper của AAR `base-application`)

> Gộp từ 5 tài liệu con cũ (kiến trúc/API gọi/component tái dùng/pipeline firebase key/bài học vận
> hành) thành 1 file duy nhất, dễ đọc trọn vẹn 1 lượt khi bắt đầu phần ads của 1 app mới.

## 1. Nguyên tắc & kiến trúc

Lib tự lo **init SDK + consent(UMP) + App Open/Resume ad**; app **không** gọi
`MobileAds.initialize`, không tự viết consent manager, không tự viết App Open ad. App chỉ:

| Việc                    | Gọi gì                                                                                                                                          |
| ----------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| Tải + hiện interstitial | `Admob.getInstance().loadAndShowInter(activity, unit, true, AdCallback())`                                                                      |
| Tải native              | `Admob.getInstance().loadNativeAd(ctx, unit, NativeCallback())`                                                                                 |
| Bind native vào view    | Inflate layout **của lib** (`ads_native_bot_2`/`..._no_media_short`) thành `NativeAdView` → `Admob.getInstance().pushAdsToViewCustom(ad, view)` |
| Banner                  | `Admob.getInstance().loadBanner(activity, unit, container, BannerCallBack())`                                                                   |
| Đọc ad unit id          | `FirebaseRemoteConfigUtil.getInstance().getAdsConfigValue(placement)` (đọc key `ads_config`, fallback `assets/default_ads_config.json`)         |
| Premium tắt ads         | `IAPUtils.isPremium()`                                                                                                                          |

**Ad unit KHÔNG nằm rải rác theo từng key riêng** — tất cả gộp trong **một** key Remote Config
`ads_config` (JSON `{placement: unit_id}`), default lấy từ `assets/default_ads_config.json`. Xem
mục 5 (pipeline `firebase_key.json`).

## 2. Cách gọi ads đúng chuẩn (đừng tự gọi GMA SDK trực tiếp)

Google Mobile Ads SDK (`AdLoader`, `InterstitialAd.load`, `AppOpenAd`, `MobileAds.initialize`...)
**không được gọi trực tiếp** ở bất kỳ đâu trong code app — mọi load/show phải đi qua
`Admob.getInstance()` của lib; consent/open/resume/khởi tạo SDK do lib tự lo.

### Native — load

```kotlin
import com.nlbn.ads.callback.NativeCallback
import com.nlbn.ads.util.Admob
import com.google.android.gms.ads.nativead.NativeAd

Admob.getInstance().loadNativeAd(context, unitId, object : NativeCallback() {
    override fun onNativeAdLoaded(ad: NativeAd) { /* cache ad, set loaded */ }
    override fun onAdFailedToLoad() { /* set error → ẩn slot, xem mục 6 */ }
    override fun onAdImpression() { /* optional */ }
})
```

### Native — show (bind vào view)

```kotlin
import com.google.android.gms.ads.nativead.NativeAdView

val layoutRes = if (isSmall)
    com.brian.base_application.R.layout.ads_native_bot_no_media_short
else
    com.brian.base_application.R.layout.ads_native_bot_2

val view = LayoutInflater.from(ctx).inflate(layoutRes, null) as NativeAdView
Admob.getInstance().pushAdsToViewCustom(ad, view)   // lib tự map + bind
```

**Khi lib CHƯA có layout phù hợp** (vd native full-screen dạng modal): giữ nguyên giao diện custom
tự dựng (`NativeAdView` + `ConstraintLayout/MediaView/...`), chỉ đổi phần LOAD sang
`Admob.getInstance().loadNativeAd(...)`, bind bằng `nativeAdView.setNativeAd(ad)` (KHÔNG
`pushAdsToViewCustom` — hàm đó cần đúng layout của lib). Quy tắc chung: **lib có layout sẵn →
`pushAdsToViewCustom` + layout lib; lib chưa có → giữ view custom + `setNativeAd`, chỉ đổi loader**.

### Interstitial

```kotlin
import com.nlbn.ads.callback.AdCallback
Admob.getInstance().loadAndShowInter(activity, unitId, true, object : AdCallback() {
    override fun onNextAction() { /* điều hướng tiếp sau khi ad đóng/không có ad */ }
    override fun onAdFailedToLoad(e: LoadAdError?) { /* fallback: đi tiếp */ }
})
```

`loadAndShowInter(...)` **tự hiện loading** trong lúc nạp ad — đừng giữ thêm loading dialog riêng
cho inter (sẽ chồng 2 lớp loading). `onNextAction()`/`onNext()` **luôn phải được gọi đúng 1 lần**
(dù ads lỗi/không hiện) — thiếu sẽ kẹt điều hướng.

### Banner

```kotlin
import com.nlbn.ads.callback.BannerCallBack
Admob.getInstance().loadBanner(activity, unitId, bannerContainer, object : BannerCallBack() {
    override fun onBannerAdLoaded() {}
    override fun onBannerAdFailed() {}
})
```

### App Open / Resume / Consent / Init SDK — KHÔNG viết, lib đã lo

- **App Open & Resume ad**: lib tự hiển thị; app chỉ override
  `enableAdsResume(): Boolean = !IAPUtils.isPremium()` trong `HostApplication`.
- **Consent (UMP)**: lib lo ở splash.
- **`MobileAds.initialize(...)`**: lib lo.

### Premium — đã trả phí → ẩn TẤT CẢ ads

Native/banner **chiếm diện tích** → gate ngay ở cờ `enable`, để CẢ block (kể cả Spacer/padding/
container bao quanh) bị bỏ, không chỉ ẩn nội dung ad:

```kotlin
fun isAdEnabled(key: String): Boolean {
    if (IAPUtils.isPremium()) return false   // premium → false NGAY, chưa trả phí mới đọc cờ Firebase
    return FirebaseRemoteConfigUtil.getInstance().getBoolean(key)
}
```

```kotlin
if (isAdEnabled("native_setting_enable")) {   // premium → false → CẢ BLOCK bị bỏ
    NativeAdSlot(slotName = "native_setting", unitId = AdManager.getAdUnitId(context, "native_setting"), isSmall = true)
}
```

## 3. Kịch bản tần suất (enable/ratio/max) + native xen kẽ sau inter

Mỗi placement có 3 tham số cấu hình trong `res/xml/config.xml`:

- **enable** — bật/tắt: `{placement}_enable` (bool).
- **ratio (tỉ lệ xuất hiện)** — `{placement}_ratio`: chỉ hiện khi `showCount % ratio == 0`.
- **max/ngày (cap)** — `{placement}_max`: chỉ hiện khi `showCountAd < max`. Đếm theo **ngày** (reset
  mỗi ngày).

Cộng thêm **kịch bản native xen kẽ sau inter** (2 quảng cáo liên tiếp): khi inter đóng, nếu cấu hình
`native_inter_*` bật thì hiện thêm 1 native full-screen ngay sau đó.

### Kiến trúc chuẩn (6 điểm)

| #   | Điểm                                                                                                                                                | File                                                      |
| --- | --------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------- |
| 1   | **Cache native theo Activity** (`AdsViewModel` + `slot.prepareForEntry(interval)`) — không reload mỗi lần vào lại composition (đổi tab/back/scroll) | `advertisement/AdsViewModel.kt`, `NativeAdSlot.kt`        |
| 2   | **Bind native 1 lần** `key(ad)` chỉ bind trong `factory` (KHÔNG cả `update`) → hết warning "bind 2 lần"                                             | `NativeAdSlot.kt`                                         |
| 3   | **AdScenario dọn key đếm >7 ngày** (SharedPreferences không phình)                                                                                  | `AdScenario.kt`                                           |
| 4   | **Native-inter tính tần suất SAU khi inter đóng** — đếm đúng maxPerDay                                                                              | `AdManager.kt`                                            |
| 5   | **Preload native-inter** lúc inter đang hiển thị → đóng inter hiện gần tức thì                                                                      | `AdManager.kt` + `NativeInter.kt` (preload/takePreloaded) |
| 6   | **positionIntrol A/B ở Intro** (nếu có slide onboarding): `N`=native inline slide N, `NN` (11/22/33/44)=native MODAL toàn màn khi rời slide         | `AdPositions.kt`, `NativeAdsFull.kt`                      |

`advertisement/AdScenario.kt` — `shouldShow(context, type, ratio, maxPerDay, noCount)`:
lưu đếm theo ngày trong SharedPreferences (key = `date + type`, reset khi sang ngày mới);
`ratio <= 0` coi như `1` (luôn qua cửa tỉ lệ); pass = `(showCount % ratio == 0) && (showCountAd < maxPerDay)`.

`advertisement/AdManager.kt` — `showInter(activity, interPlacement, onNext)`:

1. Nếu `!enable(interPlacement)` hoặc không qua ratio/max → gọi `onNext()` luôn (không ads).
2. Nếu qua → `Admob.loadAndShowInter(...)`; trong callback → xét `native_{interPlacement}`
   (native-inter). Nếu bật → `NativeInterController.show(...)`; đóng xong mới `onNext()`.
3. `onNext()` luôn được gọi đúng 1 lần.

`advertisement/NativeInter.kt` — `NativeInterController` (object, state) +
`NativeInterHost()` (composable đặt ở **root** `MainActivity`): khi có request → `Admob.loadNativeAd`
→ hiện `Dialog` fullscreen dùng layout lib + `pushAdsToViewCustom`; nút ✕ bật sau 3s (giống inter).
Lỗi load → bỏ qua, gọi tiếp điều hướng.

**Layout native theo vị trí:**

- Slot thường (Home/Intro/Permission/detail): **full** `ads_native_bot_2` + shimmer `ads_native_bot_loading_2`.
- Slot màn LIST: **gọn** `ads_native_bot_no_media_short_main` + shimmer tương ứng
  (`NativeAdSlot(..., isSmall = true)`).
- Native-inter / modal Intro: **layout tự dựng** `res/layout/ad_native_full.xml` + bind thủ công
  `NativeAdView` API (`setNativeAd`), KHÔNG `pushAdsToViewCustom`.

## 4. Component tái dùng khi làm app mới

"Lõi ads" dưới đây gần như KHÔNG đổi giữa các app. Khi làm app mới: **copy nguyên các file "core"**,
chỉ **đổi `package`** (sed) + import `R`/`BuildConfig` cho đúng, rồi chỉnh **cấu hình + giao diện màn
hình** riêng app. Nguyên tắc: \*\*sửa core-logic → hầu như không; chỉ sửa config (placement/ratio/unit)

- UI màn hình.\*\*

### A. File copy nguyên (chỉ đổi package + R/BuildConfig)

Thư mục `app/src/main/java/<pkg>/advertisement/` (+ vài file kèm):

| File                            | Vai trò                                                                | Đổi gì khi copy                                                   |
| ------------------------------- | ---------------------------------------------------------------------- | ----------------------------------------------------------------- |
| `AdsViewModel.kt`               | Cache native theo Activity                                             | chỉ `package`                                                     |
| `NativeAdSlot.kt`               | Slot native (cache + shimmer + bind 1 lần + tự ẩn khi fail + isSmall)  | `package`; layout lib giữ nguyên                                  |
| `AdManager.kt`                  | Điều phối inter + chain native-inter + tần suất                        | chỉ `package`                                                     |
| `AdScenario.kt`                 | Tần suất `showCount % ratio` + `maxPerDay` + dọn key >7 ngày           | chỉ `package`                                                     |
| `NativeInter.kt`                | `NativeInterController` + `NativeInterHost` + bind `ad_native_full`    | `package` + import `R`                                            |
| `NativeAdsFull.kt`              | Composable native fullscreen (MODAL Intro)                             | `package` + import `R`                                            |
| `AdPositions.kt`                | Parse `positionIntrol` (A/B: N inline, NN modal)                       | chỉ `package` (đổi tên hàm parse → KHÔNG trùng stdlib, xem mục 6) |
| `firebase/Remote.kt`            | Facade Remote Config (adUnit/getBoolean/isAdEnabled) + debug test-unit | `package` + import `BuildConfig`                                  |
| `core/InstallReferrerHelper.kt` | isAdsCampaign (gate Intro, xem `INTRO_CHECK.md`) — cache 3 tầng        | chỉ `package`                                                     |
| `res/layout/ad_native_full.xml` | Layout native fullscreen tự dựng                                       | đổi màu `backgroundTint` CTA theo primary app                     |

**Copy nhanh (ví dụ):**

```bash
SA=<app-nguồn-đã-có-sẵn-lõi-ads>/app/src/main/java/<pkg-path-cũ>   # vd chính app này nếu đang nhân bản
SB=<app-mới>/app/src/main/java/<pkg-path-mới>
for f in advertisement/*.kt firebase/Remote.kt core/InstallReferrerHelper.kt; do
  sed 's/<pkg\.cũ>/<pkg.mới>/g' "$SA/$f" > "$SB/$f"
done
cp .../res/layout/ad_native_full.xml <app-mới>/.../res/layout/   # rồi đổi màu CTA
```

### B. File copy rồi chỉnh theo app (cấu hình + màn hình)

| File                                | Chỉnh gì                                                                                                                        |
| ----------------------------------- | ------------------------------------------------------------------------------------------------------------------------------- |
| `res/xml/config.xml`                | Danh sách placement `*_enable` + `inter_*_ratio/max` + `native_inter_*` + `positionIntrol` + `count_app_open`/`organic`         |
| `assets/default_ads_config.json`    | **Ad unit THẬT** của app (8 placement lib bắt buộc + placement riêng app)                                                       |
| `ui/theme/Theme.kt`                 | Màu Primary/Secondary theo thiết kế app                                                                                         |
| Màn Intro (nếu có slide onboarding) | Chỉ đổi nội dung slide (ảnh/emoji/chuỗi) + danh sách `native_introN`; logic positionIntrol (inline/modal) + nút Next GIỮ NGUYÊN |
| `core/MainActivity.kt`              | Giữ `NativeInterHost()` ở root + `resolveStartRoute` gate Intro; chỉ đổi route nếu app khác                                     |
| `MyApplication.kt`                  | IAP keys, test device id, notification/IAP resources                                                                            |

### C. Nơi gọi (điểm ráp vào màn hình)

- **Native slot**: `NativeAdSlot("native_home")` / `NativeAdSlot("native_x", isSmall = true)` (màn list).
- **Inter + native-inter (ads kép)**: `AdManager.showInter(activity, "inter_home") { onNavigate() }` tại điểm chuyển màn.
- **Native-inter host**: đặt `NativeInterHost()` ở root `setContent` của MainActivity (BẮT BUỘC, gọi qua import — KHÔNG FQN inline, xem mục 6).
- **Intro modal** (nếu có): `NativeAdsFull(unitId, onClose, onError)` overlay trong màn Intro.

## 5. Pipeline `firebase_key.json` (gộp + mã hoá key Remote Config)

Mục tiêu: tạo **1 file** `firebase_key.json` để upload thủ công lên Firebase Remote Config, gồm đủ
key của:

1. **Kotlin** (key cấu hình của app, từ `res/xml/config.xml`) — có ghi chú `[KOTLIN] <tên thật>`.
2. **Lib** (`base-application`) — từ `fire_base_key_lib_arr.json`, gồm cả key `ads_config` = nội
   dung `assets/default_ads_config.json`.

### Các file nguồn (đặt ở root repo, đều **gitignore**)

| File                                          | Là gì                                                                 |
| --------------------------------------------- | --------------------------------------------------------------------- |
| `fire_base_key_lib_arr.json`                  | Danh sách param của lib (interval_auto_open_app, type_of_start_up, …) |
| `app/src/main/assets/default_ads_config.json` | Ad unit thật `{placement: unit}` → nội dung cho key `ads_config`      |
| `app/src/main/res/xml/config.xml`             | Key cấu hình app Kotlin (enable/ratio/max, count_app_open, …)         |

### `encode-scripts/firebase-merge.js` — gộp

Chạy **sau** bước encode trong `push.sh`. Sản phẩm: `firebase_key.json` = union (không trùng) của:

1. **lib** — toàn bộ `fire_base_key_lib_arr.json`.
2. **`buildKotlinParams()`** — duyệt **mọi entry** trong `config.xml`:
   - key = hash nếu có trong `keychangeConfigFirebase.json`, **else giữ readable** (xem mục
     "Quy tắc sống còn" dưới).
   - `description = "[KOTLIN] <tên thật>"`.
   - `valueType` suy ra từ giá trị (BOOLEAN/NUMBER/JSON/STRING).
3. **`ads_config`** — inject thêm:
   ```js
   merged["ads_config"] = {
     defaultValue: { value: JSON.stringify(default_ads_config.json) },
     description: "[LIB] ads_config — ad unit ids",
     valueType: "JSON",
   };
   ```
   > Lib mặc định `ads_config = {useInAppDefault:true}`; ta **thay** bằng nội dung
   > `default_ads_config.json` để Remote Config có unit thật.

Tổng số param = (số key Kotlin readable trong `config.xml`) + (số param cố định trong
`fire_base_key_lib_arr.json`) + 1 (`ads_config`) — số lượng thay đổi tuỳ app.

### ⚠️ Quy tắc sống còn: KHÔNG hash config key của app Kotlin

Code Kotlin thường đọc key **động** bằng ghép chuỗi:

```kotlin
remote.getBoolean("${placement}_enable")   // "native_home_enable"
remote.getInt("${interPlacement}_ratio")
```

→ Chuỗi literal `"native_home_enable"` **không xuất hiện** trong source (được ghép lúc chạy) →
`rename-config-key` **không thể** đổi tham chiếu trong code, nhưng **vẫn** hash key trong
`config.xml`. Hậu quả: bản **release** có `config.xml` key đã hash, còn code hỏi key readable →
**lệch → đọc default → tắt hết ads/sai cấu hình**. Debug không lộ (đọc readable).

**Cách xử lý (mặc định):**

- **Bỏ `rename-config-key.js` khỏi `ENCODE_SCRIPTS` trong `push.sh`.** Config key giữ **readable** ở
  cả `config.xml` + code + `firebase_key.json` → release đọc đúng.
- Đánh đổi: key cấu hình app không bị obfuscate. Phần nhạy cảm (ad unit) vẫn nằm trong `ads_config`.
- Nếu app đọc key kiểu **LITERAL cố định** (không ghép chuỗi runtime, vd `getBoolean("some_key")`) —
  trường hợp này CÓ THỂ bật hash an toàn, vì script quét được toàn bộ tham chiếu literal và đổi đồng
  bộ `config.xml` + code.

### Vị trí trong `push.sh` + quy trình chạy

- `FIREBASE_SCRIPTS=(firebase-merge.js)` — chạy ở **bước 3.5**, ngay sau encode, trước
  `assembleRelease`.
- **KHÔNG** thêm `rename-config-global.js` vào `FIREBASE_SCRIPTS` (nó đã ở root nhờ `TEMP_FILES`;
  thêm lại sẽ bị `rm` sau merge → `reverse.js require('./rename-config-global')` chết).
- `firebase_key.json` được **gitignore** + không push lên remote nào (chỉ để upload tay).

```bash
./push.sh build_tool     # encode → firebase-merge → assembleRelease → (push)
# → lấy firebase_key.json ở root, upload thủ công lên Firebase Console → Remote Config → Import.
```

## 6. Bẫy & bài học vận hành (BẮT BUỘC đọc trước khi đụng ads)

Đây là những thứ **KHÔNG lộ ra lúc code/compile** — chỉ hiện khi chạy debug/release trên máy.

### 6.1 LOG: lib STRIP `android.util.Log` ở release → dùng `println`

`base-application-1.0.0.aar` có proguard consumer:

```proguard
-assumenosideeffects class android.util.Log { v(...); d(...); i(...); w(...); e(...); wtf(...); println(...); }
```

→ Bản **release (R8)** **XÓA SẠCH mọi `android.util.Log`** của app. Debug thì còn, release thì mất →
tưởng "không có log / app không chạy ads".

**Quy tắc:** log ads (và log cần thấy ở production) phải dùng **`println(...)`** (Kotlin →
`System.out`, KHÔNG bị strip). KHÔNG dùng `android.util.Log`.

```bash
adb logcat -s System.out | grep -E "ADSLOT|ADS"
# ADSLOT load START native_home unit=...   (đang request)
# ADSLOT loaded native_home ad=true         (fill OK)
# ADSLOT ERR failed native_home (...) → ẩn slot   (no-fill → ẩn)
```

### 6.2 NativeAdSlot: PHẢI ẩn slot khi fail (nếu không shimmer KẸT mãi)

Lúc native fail/no-fill **PHẢI** gọi `slot.onError()` + `suppressedAfterError=true` để **ẩn slot**.
Thiếu → `isLoading` kẹt `true` → **shimmer đứng vĩnh viễn (>30s)**, chiếm chỗ.

```kotlin
onResolved = { loaded ->
    if (loaded) println("ADSLOT loaded $placement ad=true")
    else { println("ADSLOT ERR failed $placement → ẩn slot")
           slot.onError(); suppressedAfterError = true }   // <-- BẮT BUỘC
}
```

### 6.3 "Bản release/production KHÔNG có ads" = NO_FILL, KHÔNG phải lỗi code

App **chưa publish Play** + unit **THẬT** → AdMob không phục vụ (log `Too many recently failed
requests`/`TimeoutException`/HTTP 400) → không có ads (native slot tự ẩn, inter/open bỏ qua).
**Bình thường.** Chỉ hết sau khi app lên Play Store (đúng package + ký đúng keystore) và ad unit có
traffic thật (vài giờ–vài ngày sau request đầu).

Kiểm chứng code ĐÚNG: bản **debug** dùng **unit test** Google → `ad=true` + Google báo
"AdMob native ad validator: No implementation issues found". Cơ chế:

```kotlin
fun adUnit(p) = if (BuildConfig.DEBUG) debugTestUnit(p) else frc.getAdsConfigValue(p)
// test: native 2247696110, inter 1033173712, open 9257395921
```

Ads của LIB (open_splash/inter_splash/native_language…) đọc từ `ads_config` (unit thật) nên vẫn
NO_FILL trong debug — chỉ placement do APP điều khiển (native slot, inter_home…) mới dùng test unit.

### 6.4 Bẫy khác thường gặp

| Triệu chứng                                                                | Nguyên nhân                                              | Sửa                                                                |
| -------------------------------------------------------------------------- | -------------------------------------------------------- | ------------------------------------------------------------------ |
| Native trong `LazyColumn` load rồi biến mất, load lại 3 lần (rate-limited) | Item ads không có key ổn định → recompose dispose/reload | `item(key = "ad_$placement")` cho slot ads                         |
| Native hiện ở Home nhưng KHÔNG hiện ở màn list                             | Cùng lỗi key ở trên + đo sai enable                      | Key ổn định + kiểm tra `isAdEnabled` đọc đúng `{placement}_enable` |
| `native_home` no-fill lúc test                                             | Hành vi ad-network tạm thời (không phải bug)             | Thử lại/đợi; kiểm tra bằng test unit id                            |

### 6.5 Encode (obfuscate) — bẫy đã gặp

- **KHÔNG hash config key** (xem mục 5 "Quy tắc sống còn").
- **Không đặt tên hàm trùng stdlib**: hàm `parse()` tự viết trùng `Uri.parse`/`SimpleDateFormat.parse`
  → encode đổi mọi `.parse` → `Unresolved reference`. Đặt tên riêng (vd `parsePositions()`).
- **`NativeInterHost()` gọi qua import** (KHÔNG FQN inline `com.x.advertisement.NativeInterHost()`)
  — FQN inline không được encode đổi package → `Unresolved reference` khi build release.
- **push.sh** dùng `${encode_done:-false}` + `${tb:-}` trong `_cleanup` (set -u — trap EXIT chạy sau
  khi hàm unwind → biến local unbound → cleanup chết → kẹt temp branch + code encode).

### 6.6 Ký & remote (phát hành)

- **Keystore THẬT**: đặt theo quy ước `keystore/<TÊN_APP>/keystore.jks` + `pass.txt`
  (storePassword/keyAlias/keyPassword). Điền vào `app/build.gradle.kts` signingConfig. Copy jks →
  `app/keystore/update.jks` (gitignore).
- **Remote**: `origin` = repo dev/lưu trữ (snapshot đầy đủ, chưa obfuscate); `build_tool` = repo dành
  cho hệ thống build tự động (code đã obfuscate) — xem `PLAYBOOK_TICH_HOP.md` §E để biết vì sao cần
  tách 2 remote và URL cụ thể lấy ở đâu (hỏi người phụ trách build của app đó, đừng tự đoán).
- **`build_tool`/nhánh chính protected**: nếu remote build có bật branch-protection chặn force-push
  → phải nhờ admin **mở khoá force-push** cho nhánh đó trước khi chạy `push.sh build_tool`.

## 7. Test ads trên máy (adb) — cheat-sheet

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm clear <applicationId>          # reset onboarding/counter
adb logcat -c
adb shell monkey -p <applicationId> -c android.intent.category.LAUNCHER 1
adb shell input keyevent KEYCODE_BACK       # đóng inter (AdActivity) để đi tiếp
adb logcat -s System.out | grep -E "ADSLOT|ADS"     # log ads (println)
adb logcat -d | grep -E " Ads +:" | grep -iE "no fill|too many|400|timeout"  # log SDK GMS
adb shell dumpsys activity activities | grep topResumedActivity   # màn hiện tại
```

Muốn xem test-ad trên **release** (chưa publish): thêm test device id vào `getListTestDeviceId()`
của `HostApplication` (id lấy từ log `Use RequestConfiguration...setTestDeviceIds("<id>")`).

## 8. ProGuard / R8

`proguard-rules-base.pro` (dùng chung mọi app) đã có sẵn keep-rule cho `com.nlbn.**`, `com.brian.**`,
AdMob mediation (Meta/Facebook), Gson generic — **không** cần khai lại trong `proguard-rules.pro`
riêng của app. Xem `ENCODE_R8_STANDARD.md` + `PROGUARD_SDK_RECIPES.md` nếu cần thêm rule cho SDK
khác app đang dùng.

## 9. Checklist tổng hợp

- [ ] Không còn `import` GMA (`AdLoader`/`InterstitialAd`/`AppOpenAd`/`MobileAds.initialize`) trong
      code app — mọi load/show qua `Admob.getInstance()`.
- [ ] Đã xóa mọi consent manager / App Open manager tự viết — lib lo hết.
- [ ] Mỗi placement có đủ `{placement}_enable` (+ `_ratio`/`_max` cho inter) trong `res/xml/config.xml`.
- [ ] `default_ads_config.json` đủ **8 placement lib bắt buộc** (open_splash/inter_splash/
      splash_uninstall/native_language/open_all/native_keep_user/native_survey_user/native_exit_app) + placement riêng app, **dùng unit THẬT**.
- [ ] `NativeInterHost()` đặt ở root `MainActivity` (qua **import**, không FQN inline).
- [ ] `onNext()`/`onNextAction()` của `showInter` luôn gọi đúng 1 lần.
- [ ] NativeAdSlot ẩn đúng khi fail (không shimmer kẹt).
- [ ] Log ads dùng `println`, không dùng `android.util.Log`.
- [ ] Config key giữ readable (không hash) nếu code đọc key động (mục 5).
- [ ] Premium → ẩn hết ads (cả block, không chỉ nội dung).
- [ ] Đã build + test **cả debug lẫn release (R8)** trên máy thật — nhiều bug ở trên chỉ lộ ở release.
