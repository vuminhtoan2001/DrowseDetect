# Bước 3 — Màn Language custom (không dùng màn dựng sẵn của lib) + kịch bản ads

Tài liệu này dành cho **dev/AI ghép màn Language custom vào một app khác cùng kiến trúc**: Kotlin +
Jetpack Compose + SDK `base-application` (Splash / Language / IAP do lib mở). Làm lần lượt mục 9
("Checklist ghép") là xong. Code trong tài liệu là **bản cuối đã chạy ở app Vexaro**
(`vga58a-download-video-app`, package `com.video.smartdownload.downloadervideo`, prefix tracking
`VD`). Khi ghép sang app khác, thay `com.yourapp`, prefix `VD_` và token màu cho đúng app đó.

**Trước khi làm:** đã xong [UPDATE_SDK.md](UPDATE_SDK.md) (bước 1) và phần helper tracking của
[SCREEN_TRACKING.md](SCREEN_TRACKING.md) (bước 2), vì màn này gọi `TrackScreen` / `AppEvents`.

Tài liệu liên quan:

- [SCREEN_TRACKING.md](SCREEN_TRACKING.md): tracking `screen_view` + event. Màn Language dùng
  `TrackScreen` / `AppEvents` của tài liệu đó, nên **ghép tracking trước** (ít nhất mục 3.1 – 3.2).
- [ANALYTICS_EVENTS.md](ANALYTICS_EVENTS.md): giải thích event cho người đọc số trên GA4.
- Khái niệm `LanguageRouter`, `customActivityClass`, `confirmLanguageSelection` của SDK: xem mục 3 – 4 dưới đây
  (spec đầy đủ của lib đã gỡ khỏi repo).

## 1. Vì sao dùng màn Language của app thay vì của lib

Thư viện `base-application` có sẵn 1 màn chọn ngôn ngữ (`LanguageActivity`) tự chạy trong luồng
Splash → Inter → **Language** → IAP, kèm native ad `native_language` preload sẵn. App dùng UI riêng
(`LanguageScreen` + `MyLanguageActivity`) để:

- Kiểm soát toàn bộ UI/UX (header, danh sách ngôn ngữ, cờ, auto-select ngôn ngữ máy…).
- Tự làm kịch bản 2 native ad tuần tự (`native_language` → `native_language_select`) mà màn mặc
  định của lib không hỗ trợ.
- Tuỳ chọn **bỏ màn IAP** sau Language ở lần đầu (mục 4.1).

## 2. Sơ đồ file

| File | Vai trò |
|---|---|
| `<Application>.kt` (Vexaro: `HostApplication.kt`) | Gán `LanguageRouter.customActivityClass = MyLanguageActivity::class.java` **trước** `super.onCreate()`. Khai default Remote Config `native_language_enable`, `native_language_select_enable`. |
| `ui/language/MyLanguageActivity.kt` | `AppCompatActivity`. Lib tự `startActivity` vào đây (first-run) hoặc app tự mở (từ Settings). Cầu nối `LanguageRouter` ↔ Compose UI. |
| `ui/language/LanguageScreen.kt` | Toàn bộ UI + state + kịch bản ads / auto + tracking. |
| `ui/language/LanguageModel.kt` | `SupportedLanguages.list` + `detectDeviceLanguageCode()`. Cờ là **emoji text**, không dùng drawable. |
| `ui/settings/SettingsScreen.kt` | Dòng "Language" mở `MyLanguageActivity` với `EXTRA_FROM_SETTINGS = true`. |
| `ui/common/Immersive.kt` | `Activity.enableImmersiveFullScreen()` dùng chung cho `MainActivity` và `MyLanguageActivity`. Nếu app đã có hàm tương tự thì dùng lại, đừng copy 2 bản. |
| `AndroidManifest.xml` | Khai `<activity android:name=".ui.language.MyLanguageActivity" …/>`, **đúng tên class thật** (mục 8.2, ý 4). |
| `ads/NativeAdSlot.kt` | Component native ad dùng chung của app. `LanguageScreen` gọi qua wrapper `LanguageAdStage` (mục 6.1), không tự viết lại `Admob.loadNativeAd`. |
| `res/values*/strings.xml` | 3 key: `language_title`, `language_screen_subtitle`, `language_done` (mục 5.4). |
| `encode-scripts/safe-skip-base.js` | Thêm `"MyLanguageActivity"` vào `PROTECTED_FOLDER_AND_FILE_BASE` (bảo vệ file), cạnh `MainActivity` — để script encode không mã hoá màn này (mục 8.2, ý 4). |

### 2.1 Code đã có sẵn trong core

`ui/language/` (`LanguageModel`, `LanguageScreen`, `MyLanguageActivity`), `ui/common/Immersive.kt`,
`platform/` và 3 string `language_*` (đã dịch 21 ngôn ngữ) đã nằm trong project. Màu màn Language khai ở đầu
`LanguageScreen.kt` (`BrandBgTop`, `BrandPrimary`… ánh xạ sang `Bg2`, `Theme`, `Border`): đổi ở đó khi app mới
đổi bảng màu. Màn tự vẽ nền vì `MyLanguageActivity` không nằm dưới `AppRoot`.

Khi đổi package / app mới, chỉ cần kiểm: `MyLanguageActivity` import đúng Activity Home (class mà
`getHomeActivity()` trả về), `Dest` có đủ 4 hằng `FIRST_LANGUAGE`, `FIRST_LANGUAGE_SELECT`, `LANGUAGE`,
`LANGUAGE_SELECT`, và `SupportedLanguages.list` khớp thư mục `values-xx` (mục 5.3).

## 3. Đăng ký customActivityClass

```kotlin
// <Application>.kt
override fun onCreate() {
    // PHẢI gán TRƯỚC super.onCreate() — SDK đọc giá trị này khi dựng luồng Splash.
    LanguageRouter.customActivityClass = MyLanguageActivity::class.java
    super.onCreate()
    ScreenTracker.init()          // SCREEN_TRACKING.md
    trackLibScreens()             // SCREEN_TRACKING.md §3.6
    FirebaseRemoteConfigUtil.getInstance().setAppDefaults(
        mapOf(
            // ...
            "native_language_enable" to true,
            "native_language_select_enable" to true,
        ),
    )
}
```

Gán `customActivityClass` thì lib tự `startActivity(MyLanguageActivity)` từ Splash khi tới lượt
Language trong luồng first-run, và **gắn sẵn "màn kế tiếp = IAP"** vào intent extras của
`MyLanguageActivity`.

Nhớ thêm ad unit cho cả 2 placement trong `app/src/main/assets/default_ads_config.json`:

```json
"native_language": "ca-app-pub-xxx/yyy",
"native_language_select": "ca-app-pub-xxx/zzz",
```

## 4. `MyLanguageActivity` — 2 luồng vào màn

Phân biệt bằng extra `EXTRA_FROM_SETTINGS`.

```kotlin
class MyLanguageActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FROM_SETTINGS = "extra_from_settings"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveFullScreen()

        val isFromSettings = intent.getBooleanExtra(EXTRA_FROM_SETTINGS, false)

        // First-run: disable back hẳn. Từ Settings: callback đã chặn default → phải tự finish().
        onBackPressedDispatcher.addCallback(this) {
            if (isFromSettings) finish()
        }

        setContent {
            AppTheme {
                LanguageScreen(
                    fromSettings = isFromSettings,
                    onDone = { code, method ->
                        AppEvents.log(
                            this, "language_confirm",
                            "code" to code, "method" to method, "from_settings" to isFromSettings,
                        )
                        if (isFromSettings) {
                            LanguageRouter.confirmLanguageSelection(this, code, navigate = false)
                            finish()
                        } else {
                            // Bỏ màn IAP — xem §4.1. Muốn GIỮ IAP thì gọi confirmLanguageSelection(this, code)
                            LanguageRouter.confirmLanguageSelection(this, code, MainActivity::class.java)
                        }
                    },
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableImmersiveFullScreen()
    }
}
```

### 4.1 Luồng first-run (`isFromSettings = false`) — chọn giữ hay bỏ màn IAP

Chữ ký của SDK: `confirmLanguageSelection(activity, code, nextScreen: Class? = null, navigate = true)`.

Hành vi đã đọc từ bytecode `LanguageRouter` (bản AAR `base-application-1.0.0`):

1. Lưu mã ngôn ngữ vào prefs của lib, áp locale, gọi `Application.notifyLanguageSaved(code)`, xoá cờ
   `IS_FIRST_TIME_LANGUAGE` / `FIRST_TIME_OPEN_APP` (lần sau không mở lại màn Language).
2. Nếu `navigate = true` → `navigateToNext(activity, nextScreen)`:
   - `nextScreen != null` → **ghi đè** màn kế tiếp trong intent bằng class truyền vào. Màn IAP mà
     Splash gắn sẵn bị thay.
   - Chưa có quyền `POST_NOTIFICATIONS` **và** Remote Config bật màn xin quyền
     (`isRequestNotiActivityOnOff`) → mở `RequestNotificationPermissionActivity`, xong thì đi tiếp
     tới màn kế tiếp.
   - Ngược lại → mở thẳng màn kế tiếp.
   - Cuối cùng **tự `finish()`** màn Language.

| Gọi | Luồng sau Language |
|---|---|
| `confirmLanguageSelection(this, code)` | (xin quyền thông báo) → **IAP** → Home |
| `confirmLanguageSelection(this, code, MainActivity::class.java)` | (xin quyền thông báo) → **Home**, **bỏ qua IAP** |

Vexaro dùng **cách 2 (cố ý bỏ IAP ở lần đầu)**. App khác chọn theo yêu cầu sản phẩm. Nếu bỏ IAP thì
trên GA4 sẽ **không** có `VD_First_Iap` trong funnel lần đầu, đừng hiểu nhầm là drop.

Cả 2 cách đều **không** tự gọi `finish()`: lib đã tự finish, gọi thêm là dư/sai luồng.

**Back-press khi first-run:** cố ý không xử lý gì (disable back hẳn). Nếu user thoát màn mà không
gọi `confirmLanguageSelection`, lib coi như chưa chọn ngôn ngữ lần đầu → lần mở app sau lại chạy lại
Splash → Language.

### 4.2 Luồng mở từ Settings (`isFromSettings = true`)

```kotlin
// SettingsScreen.kt
SettingRow(
    icon = Icons.Filled.Language,
    title = stringResource(R.string.setting_language),
    onClick = {
        context.startActivity(
            Intent(context, MyLanguageActivity::class.java)
                .putExtra(MyLanguageActivity.EXTRA_FROM_SETTINGS, true),
        )
    },
)
```

Confirm xong gọi `confirmLanguageSelection(this, code, navigate = false)` (lib **chỉ** lưu + áp
locale + `notifyLanguageSaved`, không điều hướng) rồi app **tự `finish()`** để quay lại Settings.

**Back-press khi mở từ Settings:** gọi `finish()`. Vì đã đăng ký `onBackPressedDispatcher` nên
default behavior bị chặn; không gọi tay thì user bị kẹt.

## 5. UI (`LanguageScreen.kt`)

### 5.1 Header

- Title `R.string.language_title`, subtitle `R.string.language_screen_subtitle`.
- **Không có thanh search.**
- Icon check ở góc phải, 3 trạng thái (mục 6.3).

### 5.2 Danh sách ngôn ngữ

- 1 dòng/ngôn ngữ: emoji cờ (`item.flagEmoji`) + tên bản ngữ (`FontWeight.Medium`) + `RadioButton`.
- **Không tự active dòng nào khi vào màn**, kể cả khi đã có ngôn ngữ đang dùng (`selectedCode` khởi
  tạo `null`). Áp dụng cho cả 2 luồng.
- `selectedCode` dùng `rememberSaveable` (đổi locale / xoay màn làm Activity tạo lại, mất trạng thái
  sẽ log lại màn).
- Màu lấy theo token của app. Vexaro: hàng thường = `BrandCard` + viền `BrandCardAlt`; hàng đang
  chọn = `BrandPrimary.copy(alpha = 0.08f)` + viền `BrandPrimary`; radio chưa chọn = `BrandTextMuted`.
- `MyLanguageActivity` **không nằm dưới `AppRoot`** của app, nên màn phải **tự vẽ nền** (Vexaro: cùng
  gradient `BrandBgTop → BrandBgBottom` với `AppRoot`). Quên bước này là màn trong suốt / sai nền.

### 5.3 Danh sách ngôn ngữ hỗ trợ

`SupportedLanguages.list` phải khớp **chính xác** các thư mục `values-xx` trong `app/src/main/res`.
Vexaro: `ar, de, es, fa, fr, hi, id, it, iw, ja, km, ko, nl, pt, ru, th, tr, uk, vi, zh-rCN` + `en`
mặc định (21 ngôn ngữ). Thêm/bớt ở đây thì **phải** thêm/bớt `values-<code>/strings.xml` tương ứng,
nếu không UI vẫn hiện tiếng Anh do thiếu bản dịch.

`detectDeviceLanguageCode()` xử lý quirk `Locale.getDefault().language` trả `"in"` (Indonesia) /
`"he"` (Hebrew) và `zh` → `zh-CN`. Không khớp gì → `"en"`.

### 5.4 String resources

| Key | Dùng ở đâu | Text tiếng Anh |
|---|---|---|
| `language_title` | Title header | Language |
| `language_screen_subtitle` | Subtitle header | Choose the language you want to use in the app |
| `language_done` | `contentDescription` của icon check | Done |

Phải dịch đủ sang **mọi** locale app hỗ trợ. Chuỗi có dấu nháy đơn (tiếng Pháp/Ý, `don't`…) phải
escape thành `\'`, không thì build lỗi resource.

## 6. Kịch bản ads (`native_language` + `native_language_select`)

2 placement, bật/tắt qua Remote Config `<placement>_enable`, ad unit trong
`default_ads_config.json`.

### 6.1 `LanguageAdStage` — tín hiệu "done" của 1 placement

```kotlin
/** 1 placement native ad — tự "done" ngay nếu bị disable (Remote Config / thiếu ad unit / premium). */
@Composable
private fun LanguageAdStage(placement: String, onStageDone: () -> Unit) {
    val context = LocalContext.current
    val unitId = remember(placement) { AdManager.getAdUnitId(context, placement) }
    val enabled = remember(placement) { AdManager.isAdEnabled("${placement}_enable") && unitId.isNotBlank() }
    if (enabled) {
        NativeAdSlot(
            slotName = placement,
            unitId = unitId,
            modifier = Modifier.fillMaxWidth(),
            onImpressionOrError = onStageDone,
        )
    } else {
        LaunchedEffect(placement) { onStageDone() }
    }
}
```

1 placement được coi là **done** khi: ad ghi nhận **impression**, hoặc **lỗi load**, hoặc
placement **bị tắt** (premium / Remote Config / thiếu ad unit), hoặc **quá `AD_LOAD_TIMEOUT_MS`**
(mục 6.5).

> **Callback phải bắn lúc impression, KHÔNG phải lúc load xong.** Ad load xong chưa chắc user đã
> thấy. Kiểm tra component native ad của app đích: Vexaro có sẵn `onImpressionOrError` bắn ở
> `onAdImpression` / `onAdFailedToLoad`. App nào chỉ có `onLoaded` thì phải sửa component trước, và
> rà mọi màn khác đang dùng callback đó (Intro, Permission…) vì thay đổi ảnh hưởng chung.

### 6.2 Trình tự — `native_language_select` PHẢI chờ `native_language` done

```kotlin
// Chỉ MOUNT ad select sau khi đã active ngôn ngữ VÀ native_language đã done.
LaunchedEffect(hasSelected, languageDone) {
    if (hasSelected && languageDone) selectStarted = true
}
```

Mount component là thứ kích hoạt nó tự load, nên `selectStarted` là guard để ad select chỉ được
mount đúng 1 lần, đúng lúc.

Trace ví dụ (`native_language` đang load, user chọn ngôn ngữ ngay lúc đó):

1. User tap 1 dòng → `hasSelected = true`.
2. Effect chạy lại nhưng `languageDone` vẫn `false` → chưa mount ad select.
3. `native_language` impression / lỗi → `languageDone = true`.
4. Effect chạy lại → `selectStarted = true` → slot đổi sang `native_language_select` → tự load.

### 6.3 Icon check ở header — 3 trạng thái

```kotlin
when {
    !hasSelected -> Icon(Check, tint = muted.copy(alpha = 0.35f))         // mờ, KHÔNG gắn clickable
    !isReady     -> CircularProgressIndicator(...)                          // chờ ad, không bấm được
    else         -> Icon(Check, modifier = Modifier.clickable(onConfirm))   // sẵn sàng
}
// isReady = languageDone && selectDone
```

### 6.4 1 slot ad duy nhất, full width

Chỉ 1 vị trí ad ở cuối màn (dưới `LazyColumn`, ngoài phần padding ngang 20dp để chạy full width).
Ngay khi `selectStarted = true`, slot **đổi ngay** sang `native_language_select`;
`native_language` bị unmount kể cả khi chưa kịp impression (quyết định thiết kế đã chốt).

```kotlin
if (!selectStarted) {
    LanguageAdStage(PLACEMENT_LANGUAGE, onStageDone = { languageDone = true })
} else {
    LanguageAdStage(PLACEMENT_LANGUAGE_SELECT, onStageDone = { selectDone = true })
}
```

### 6.5 Timeout chống treo

```kotlin
LaunchedEffect(Unit) {
    delay(AD_LOAD_TIMEOUT_MS)
    if (!languageDone) languageDone = true
}
LaunchedEffect(selectStarted) {
    if (!selectStarted) return@LaunchedEffect
    delay(AD_LOAD_TIMEOUT_MS)
    if (!selectDone) selectDone = true
}
```

Màn Language là **gate bắt buộc** ở lần đầu (đã khoá Back). Nếu SDK treo, không bao giờ gọi
callback, mà không có timeout thì user kẹt vĩnh viễn. Timeout này chỉ **mở khoá**, không tự chuyển
màn, nên không cần gate lifecycle.

## 7. Kịch bản auto — 3 hằng số thời gian

Toàn màn chỉ có **3 hằng số**, áp dụng giống nhau cho cả first-run và mở từ Settings:

```kotlin
private const val AD_LOAD_TIMEOUT_MS = 8_000L                    // ad không phản hồi → coi như done
private const val AUTO_SELECT_DEVICE_LANGUAGE_DELAY_MS = 8_000L  // chưa chọn gì → tự chọn ngôn ngữ máy
private const val AUTO_CONFIRM_DELAY_MS = 3_000L                 // đã chọn, không đổi → tự confirm
```

| Hằng số | Giá trị | Vì sao |
|---|---|---|
| `AD_LOAD_TIMEOUT_MS` | 8s | Đủ cho native load + impression ở mạng chậm; không để user chờ quá lâu nếu ad treo. |
| `AUTO_SELECT_DEVICE_LANGUAGE_DELAY_MS` | 8s | Ad đầu thường impression sau 2 – 4s, nên vẫn được hiển thị 4 – 6s. Người đang đọc danh sách không bị ảnh hưởng vì scroll reset timer. 12s (bản cũ) làm người không thao tác phải chờ tới 24s. Không nên dưới 5s. |
| `AUTO_CONFIRM_DELAY_MS` | 3s | Đủ để user thấy mình chọn nhầm và chọn lại; ad select hiển thị được ~2 – 3s trước khi màn đóng. |

### 7.1 Auto-select ngôn ngữ máy

```kotlin
LaunchedEffect(scrollSignal, lifecycle) {
    var fired = false
    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        if (fired) return@repeatOnLifecycle
        delay(AUTO_SELECT_DEVICE_LANGUAGE_DELAY_MS)
        fired = true
        if (selectedCode == null) select(detectDeviceLanguageCode(), "auto")
    }
}
```

### 7.2 Auto-confirm — đợi 3s VÀ đợi ad done

```kotlin
LaunchedEffect(selectedCode, scrollSignal, lifecycle) {
    val code = selectedCode ?: return@LaunchedEffect
    var fired = false
    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        if (fired) return@repeatOnLifecycle
        delay(AUTO_CONFIRM_DELAY_MS)
        snapshotFlow { languageDone && selectDone }.filter { it }.first()   // chờ 2 ad done
        fired = true
        confirm(code, "auto")
    }
}
```

Thời điểm tự confirm = **max(lần chọn/đổi gần nhất + 3s, lúc cả 2 ad done)**:

| Tình huống | Confirm lúc |
|---|---|
| Ad select impression sớm (giây 1,5) | Giây 3 — user vẫn có 3s để đổi ý |
| Ad select impression muộn (giây 5) | Giây 5 — đợi ad |
| Ad lỗi / bị tắt | Giây 3 |
| Ad treo | Tối đa `AD_LOAD_TIMEOUT_MS` sau khi ad select bắt đầu mount |

**Không cần** một hằng số / effect riêng kiểu "impression xong thì confirm ngay": `AUTO_CONFIRM_DELAY_MS`
ngắn + chờ ad done đã ra đúng hành vi. (Bản thử "confirm ngay khi impression" đã bỏ: ad chỉ hiện
< 1s và user không kịp đổi ý.)

### 7.3 Quy tắc chung của 2 timer

1. **Scroll danh sách → reset cả 2 timer.** `rememberLazyListState()` + `snapshotFlow` theo dõi
   `firstVisibleItemIndex` / `firstVisibleItemScrollOffset`, tăng `scrollSignal` mỗi lần đổi;
   `scrollSignal` nằm trong **key** của 2 effect nên Compose huỷ + chạy lại (đếm lại từ đầu).
2. **Đổi ngôn ngữ → reset auto-confirm** (`selectedCode` nằm trong key).
3. **Chỉ đếm khi RESUMED** (`repeatOnLifecycle`): bấm vào ad mở CH Play, app-open ad, bấm Home thì
   dừng; quay lại thì đếm lại từ đầu. Không gate thì màn tự chuyển lúc bị che và mất `screen_view`
   (SCREEN_TRACKING.md §3.8). `fired` chặn chạy 2 lần khi resume lại.
4. **`confirmed` guard** chặn `onDone` bị gọi 2 lần (user bấm ✓ trùng lúc auto-confirm):

```kotlin
var confirmed by remember { mutableStateOf(false) }
fun confirm(code: String, method: String) {
    if (confirmed) return
    confirmed = true
    onDone(code, method)
}
```

## 8. Tracking + pitfall

### 8.1 Tracking (chi tiết ở SCREEN_TRACKING.md §3.5g)

| Gì | Ở đâu | Giá trị |
|---|---|---|
| `screen_view` | `TrackScreen(...)` đầu `LanguageScreen` (đúng 1 chỗ, **không** đặt thêm ở `setContent`) | `<P>_First_Language` → `<P>_First_LanguageSelect` (first-run), `<P>_Language` → `<P>_LanguageSelect` (từ Settings) |
| `language_select` | Hàm `select()`, chỉ lần đầu `selectedCode` từ `null` → có giá trị | `code`, `method = click \| auto`, `from_settings` |
| `language_confirm` | `MyLanguageActivity.onDone`, **trước** `confirmLanguageSelection` | `code`, `method = click \| auto`, `from_settings` |

### 8.2 Pitfall khi sửa lại

1. **Đừng bỏ `onBackPressedDispatcher` custom** — back first-run sẽ tự `finish()` mà không confirm →
   lặp lại màn Language mỗi lần mở app.
2. **Đừng gọi `finish()` ở nhánh first-run** — lib tự finish. Ngược lại **PHẢI** `finish()` ở nhánh
   Settings vì dùng `navigate = false`.
3. **Truyền `nextScreen` ở first-run là bỏ IAP** (mục 4.1). Đổi qua lại 2 cách là đổi luồng kinh
   doanh, báo cho người phân tích số liệu biết.
4. **Không để script encode mã hoá `MyLanguageActivity`.** Script `encode-scripts/rename-all.js` đổi
   tên file, package, class. `MyLanguageActivity` bị đổi tên mà manifest lệch (chạy `reverse.js` sót
   hoặc lệch) → **`ActivityNotFoundException`** ngay lần mở app đầu tiên, vì SDK mở màn này từ Splash.

   Thêm vào **`PROTECTED_FOLDER_AND_FILE_BASE`** trong **`encode-scripts/safe-skip-base.js`** (bảo vệ
   file), cạnh `MainActivity`:

   ```js
   PROTECTED_FOLDER_AND_FILE_BASE: [ /* ... */ "MainActivity", "AdActivity", "MyLanguageActivity" ],
   ```

   - `rename-file.js` là script đổi tên file `MyLanguageActivity.kt` (kéo theo class và manifest), và
     chỉ bỏ qua tên nằm trong danh sách này.
   - **Không cần** thêm vào `SAFE_SKIP_BASE`: `rename-code-symbols.js` tự bỏ qua class trùng tên với
     file chứa nó, nên khi file đã được bảo vệ thì class cũng giữ nguyên.

   Đặt ở file lõi vì mọi app dùng gói này đều có `MyLanguageActivity`, giống `MainActivity`. Chi tiết,
   cách dùng file config riêng của app thay thế, và lệnh kiểm sau khi encode: SCREEN_TRACKING.md
   §3.11a. Luôn đối chiếu tên trong manifest với file `.kt` trước khi build bản phát hành.
5. **Callback "done" của ad phải bắn lúc impression** (mục 6.1). Sửa component native ad chung là
   ảnh hưởng mọi màn đang dùng callback đó.
6. **Thêm/bớt ngôn ngữ** → thêm/bớt `values-<code>/strings.xml` và dịch đủ 3 key ở mục 5.4.
7. **Đừng tăng lại `AUTO_CONFIRM_DELAY_MS` lên 8 – 12s** hoặc thêm timer riêng cho impression mà
   không xem lại mục 7: cộng dồn với auto-select, người không thao tác sẽ phải chờ rất lâu.

## 9. Checklist khi clone core sang app mới

1. [ ] `SupportedLanguages.list` khớp đúng thư mục `values-xx` (mục 5.3); app thêm ngôn ngữ thì dịch thêm 3 key ở mục 5.4.
2. [ ] Import Activity Home trong `MyLanguageActivity`; màu ở đầu `LanguageScreen.kt` đúng bảng màu app.
3. [ ] `default_ads_config.json` có `native_language` và `native_language_select` (unit thật riêng).
4. [ ] Application: `customActivityClass` trước `super.onCreate()` + default RC 2 placement (mục 3).
5. [ ] Manifest khai `MyLanguageActivity`; `safe-skip-base.js` có `"MyLanguageActivity"` (mục 8.2, ý 4).
6. [ ] First-run **bỏ IAP** (đã chốt, mục 4.1): không đổi `MainActivity::class.java` ở nhánh first-run.

## 10. Test

```
adb logcat -s ScreenTracker AppEvents
```

1. Cài mới (clear data): Splash → `<P>_First_Language`. Không chạm gì: sau 8s tự chọn ngôn ngữ máy
   (`language_select method=auto`, `<P>_First_LanguageSelect`), rồi ~3s sau (hoặc khi ad select
   done) tự confirm (`language_confirm method=auto`) và sang màn kế (IAP hoặc Home tuỳ mục 4.1).
2. Chọn 1 ngôn ngữ, trong 3s đổi sang ngôn ngữ khác → timer đếm lại, confirm đúng ngôn ngữ sau.
3. Cuộn danh sách liên tục > 8s → không bị tự chọn.
4. Bấm vào native ad (mở CH Play), chờ > 10s, quay lại → màn vẫn đứng yên, rồi mới đếm lại.
5. Tắt mạng → icon check chuyển loading rồi mở khoá sau tối đa 8s, không kẹt.
6. Bấm Back ở first-run → không thoát. Từ Settings → Back đóng màn, quay lại Settings.
7. Từ Settings đổi ngôn ngữ → UI app đổi ngôn ngữ; log `<P>_Language` → `<P>_LanguageSelect`,
   `from_settings=true`.
