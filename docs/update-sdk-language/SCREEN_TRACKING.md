# Bước 2 — Ghép tracking màn hình Firebase (screen_view) — hướng dẫn mẫu

> **Code đã có sẵn trong core:** `platform/ScreenTracker.kt` (`ScreenTracker`, `AppEvents`, `TrackScreen`) và
> `platform/OnboardingExit.kt` (`LogOnboardingExit`, `HomeReached`). App mới chỉ cần đổi prefix ở `object Dest`
> (`ui/nav/AppNav.kt`) và gắn `TrackScreen` cho màn mới (mục 2, 3.5). Các mục 3.1 – 3.3 bên dưới giữ lại để
> hiểu code, không phải làm lại.

Tài liệu cho **dev/AI ghép tracking màn hình vào một app khác cùng kiến trúc**: Kotlin + Jetpack
Compose + Navigation Compose + SDK `base-application` (Splash / Language / IAP do lib mở). Làm lần
lượt mục 3 là xong, code ở đây là **bản cuối đã sửa hết lỗi** gặp ở app Loan Calculator (prefix `LC`),
và đã ghép thành công lần 2 vào app Vexaro (`vga58a-download-video-app`, prefix `VD`). Những chỗ app
Vexaro khác Loan Calculator (không có cờ onboarding, Home là 3 tab trong 1 route, modal ad không có
`onShown`…) được ghi chú **"Vexaro:"** ngay tại mục tương ứng.

- Màn Language custom (dùng `TrackScreen` / `AppEvents` ở đây): [LANGUAGE_SCREEN_CUSTOM.md](LANGUAGE_SCREEN_CUSTOM.md).
- Giải thích event cho người đọc số trên GA4 (mẫu cho app Vexaro): [ANALYTICS_EVENTS.md](ANALYTICS_EVENTS.md).
  Ghép xong app mới thì viết 1 bản tương tự cho app đó.
- Trong tài liệu, `<PREFIX>` là mã viết tắt của app (Loan Calculator dùng `LC`, Vexaro dùng `VD`),
  `com.yourapp` là package của app.

## 1. Kết quả cần đạt

- Mỗi màn của app có đúng một tên `<PREFIX>_<PascalCase>` trên Firebase, kể cả màn của lib (Splash,
  IAP, xin quyền thông báo).
- Mỗi lần user thật sự nhìn thấy một màn → đúng **một** `screen_view`. Quay về màn đang đứng (sau ad,
  IAP, share, CH Play, recreate) **không** log lại.
- Timer tự chuyển màn (Intro, Language) không chạy khi màn bị che → không có màn "được đi qua" mà
  không log.
- Có event nghiệp vụ cho onboarding để tách drop thật khỏi lỗi tracking.
- Mọi thứ vẫn đúng trên **bản đã chạy script encode** (`encode-scripts/`).

## 2. Quy ước tên

| Quy tắc | Ví dụ |
|---|---|
| Format `<PREFIX>_<PascalCase>`, string literal cố định | `LC_Home` |
| Route Navigation dùng **luôn** tên màn (1 chuỗi, 2 việc) | `Home("LC_Home")` |
| Màn không phải route (Activity riêng, màn lib, tab trong 1 route) → hằng riêng cạnh route | `const val SPLASH = "LC_Splash"` |
| Cùng UI ở 2 luồng → 2 tên. Bản ở luồng first-open thêm `First_` (không dùng `Onb`) | `LC_First_Currency` / `LC_Currency` |
| Màn chỉ có 1 luồng → không cần `First_` | `LC_Intro1` |
| Pager nhiều slide → mỗi slide 1 tên = tên route + số thứ tự (từ 1), route tổng không log | `LC_Intro1`…`LC_Intro4` |
| Cùng route nhưng 2 UI khác hẳn nhau → 2 tên | `LC_CurrencyConverter` / `LC_UnitConverter` |
| Cùng UI, khác tham số (loại máy tính…) → **1 tên + param**, không nhét tham số vào tên | `LC_Calculator` + `calculator_type=loan` |
| Màn đổi trạng thái có ý nghĩa cho funnel → tên theo trạng thái | `LC_Language` → `LC_LanguageSelect` |
| Dialog, bottom sheet, dropdown, modal ad → **không** phải màn | — |
| Event nghiệp vụ: snake_case, không prefix | `intro_next`, `language_confirm` |

Giữ **tên hằng Kotlin** ổn định (`Screen.Home`), chỉ đổi **giá trị chuỗi** → các chỗ gọi
`navigate(Screen.X.route)` không phải sửa.

## 3. Các bước ghép

### 3.1 Dependency

`firebase-analytics` thường đã có qua `base-application`. Thêm:

```kotlin
// gradle/libs.versions.toml
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleRuntimeKtx" } // ≥ 2.8
androidx-lifecycle-process = { group = "androidx.lifecycle", name = "lifecycle-process", version.ref = "lifecycleRuntimeKtx" }

// app/build.gradle.kts
implementation(libs.androidx.lifecycle.runtime.compose)   // LifecycleResumeEffect, LocalLifecycleOwner
implementation(libs.androidx.lifecycle.process)           // ProcessLifecycleOwner
```

App dùng Groovy `app/build.gradle` (Vexaro) thì khai thẳng, cùng version với các lib lifecycle đang có:

```groovy
implementation 'androidx.lifecycle:lifecycle-runtime-compose:2.9.4'
implementation 'androidx.lifecycle:lifecycle-process:2.9.4'
```

Kiểm tra trước: `lifecycle-runtime-compose` có thể đã có sẵn; `firebase-analytics` đến từ
`base-application` (`grep firebase base-application/build.gradle`).

### 3.2 Helper `platform/ScreenTracker.kt` (đã có trong core)

```kotlin
package com.yourapp.platform

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.bundleOf
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Tự log `screen_view` với tên màn cố định.
 *
 * `screen_class` = chính tên màn (string literal): tên class thật thì mọi màn Compose đều là
 * `MainActivity`, và class của app có thể bị script encode đổi thành chuỗi ngẫu nhiên.
 *
 * Chặn log trùng liên tiếp: cùng tên với lần log trước thì bỏ qua. Quay về màn đang đứng từ bất
 * kỳ đâu (interstitial/app-open ad, IAP, recreate(), share chooser, CH Play, Settings hệ thống,
 * bấm Home rồi mở lại) đều KHÔNG log lại. Ngoại lệ: app ở background lâu hơn
 * [RELOG_AFTER_BACKGROUND_MS] (= timeout phiên GA4 mặc định) thì log lại cho phiên mới.
 */
object ScreenTracker {
    private const val TAG = "ScreenTracker"
    private const val RELOG_AFTER_BACKGROUND_MS = 30 * 60 * 1000L

    @Volatile
    private var lastScreen: String? = null
    private var backgroundAt = 0L

    /** Gọi 1 lần trong Application.onCreate (main thread). */
    fun init() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                backgroundAt = SystemClock.elapsedRealtime()
            }

            override fun onStart(owner: LifecycleOwner) {
                if (backgroundAt != 0L && SystemClock.elapsedRealtime() - backgroundAt > RELOG_AFTER_BACKGROUND_MS) {
                    lastScreen = null
                }
                backgroundAt = 0L
            }
        })
    }

    fun log(context: Context, screenName: String, params: Map<String, Any?> = emptyMap()) {
        if (screenName == lastScreen) {
            Log.d(TAG, "screen_view: $screenName (bỏ qua, trùng lần trước)")
            return
        }
        lastScreen = screenName
        Log.d(TAG, "screen_view: $screenName ${if (params.isEmpty()) "" else params}")
        val bundle = params.toBundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
        }
        FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }
}

/**
 * Event nghiệp vụ (snake_case, không prefix). Gọi thẳng trong callback, KHÔNG phụ thuộc
 * lifecycle — vẫn gửi được khi app ở background.
 */
object AppEvents {
    private const val TAG = "AppEvents"

    fun log(context: Context, name: String, vararg params: Pair<String, Any?>) {
        val map = params.toMap()
        Log.d(TAG, "$name $map")
        FirebaseAnalytics.getInstance(context).logEvent(name, map.toBundle())
    }
}

/** String / Boolean ("true"/"false") / số nguyên / số thực; null bị bỏ qua. */
private fun Map<String, Any?>.toBundle(): Bundle = bundleOf().also { b ->
    forEach { (k, v) ->
        when (v) {
            null -> Unit
            is String -> b.putString(k, v)
            is Boolean -> b.putString(k, v.toString())
            is Int -> b.putLong(k, v.toLong())
            is Long -> b.putLong(k, v)
            is Number -> b.putDouble(k, v.toDouble())
            else -> b.putString(k, v.toString())
        }
    }
}

/**
 * Log mỗi lần lifecycle owner hiện tại (NavBackStackEntry / Activity) vào RESUMED, và khi
 * [screenName] đổi. Không dùng LaunchedEffect: sẽ không log khi back về màn cũ.
 * [params] đi kèm screen_view — đổi params không tự log lại.
 */
@Composable
fun TrackScreen(screenName: String, params: Map<String, Any?> = emptyMap()) {
    val context = LocalContext.current
    LifecycleResumeEffect(screenName) {
        ScreenTracker.log(context, screenName, params)
        onPauseOrDispose { }
    }
}
```

Hành vi của `TrackScreen` + `ScreenTracker`:

| Tình huống | Có log? |
|---|---|
| Màn hiện lần đầu | Có |
| Back về màn trước (tên khác lần log trước) | Có |
| Đổi `screenName` (sang slide, đổi trạng thái) | Có |
| Quay về màn đang đứng: sau inter / app-open ad, IAP, `recreate()`, share, CH Play (kể cả click native ad), Settings hệ thống, bấm Home rồi mở lại | **Không** |
| App ở background > 30 phút rồi mở lại | Có |
| Recompose | Không |
| Màn đổi tên khi **không** RESUMED (bị che) | Không — xem 3.8 |

### 3.3 Helper `platform/OnboardingExit.kt` (đã có trong core)

Log khi user bấm Back để thoát ở màn onboarding, **không đổi** hành vi thoát app.

```kotlin
package com.yourapp.platform

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Không dùng BackHandler: muốn chuyển tiếp Back phải tắt callback của chính mình NGAY rồi mới gọi
 * lại dispatcher; BackHandler chỉ cập nhật isEnabled ở lần compose sau → lặp vô hạn.
 */
@Composable
fun LogOnboardingExit(screen: String) {
    val context = LocalContext.current
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher ?: return
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentScreen = rememberUpdatedState(screen)
    DisposableEffect(dispatcher, lifecycleOwner) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AppEvents.log(context, "onboarding_exit", "screen" to currentScreen.value)
                isEnabled = false
                dispatcher.onBackPressed()
                isEnabled = true
            }
        }
        dispatcher.addCallback(lifecycleOwner, callback)
        onDispose { callback.remove() }
    }
}
```

### 3.4 Đặt tên màn trong `ui/nav/Screen.kt`

```kotlin
enum class Screen(val route: String) {
    Intro("<PREFIX>_Intro"),        // pager: log theo slide, route tổng không log
    Home("<PREFIX>_Home"),
    Calculator("<PREFIX>_Calculator"),
    Converter("<PREFIX>_Converter"), // route; tên màn tách ở dưới
    CurrencySetting("<PREFIX>_Currency");
    // ...

    companion object {
        // Chỉ là tên màn Firebase (không phải route)
        const val FIRST_CURRENCY = "<PREFIX>_First_Currency"
        const val FIRST_LANGUAGE = "<PREFIX>_First_Language"
        const val FIRST_LANGUAGE_SELECT = "<PREFIX>_First_LanguageSelect"
        const val LANGUAGE = "<PREFIX>_Language"
        const val LANGUAGE_SELECT = "<PREFIX>_LanguageSelect"
        const val CURRENCY_CONVERTER = "<PREFIX>_CurrencyConverter"
        const val UNIT_CONVERTER = "<PREFIX>_UnitConverter"
        // Activity của lib
        const val SPLASH = "<PREFIX>_Splash"
        const val FIRST_NOTI_PERMISSION = "<PREFIX>_First_NotiPermission"
        const val NOTI_PERMISSION = "<PREFIX>_NotiPermission"
        const val FIRST_IAP = "<PREFIX>_First_Iap"
        const val IAP = "<PREFIX>_Iap"
    }
}
```

**Bắt buộc sau khi đổi giá trị route:** tìm mọi chỗ còn viết cứng chuỗi route cũ và đổi sang
`Screen.X.route` / `Screen.result(id)`. Loan Calculator từng còn `onNavigate("result/${id}")` ở
Home — đổi route mà sót chỗ này là bấm vào crash.

```
grep -rnE '"(home|tools|result|history|setting|currency_setting)(/|")' app/src/main/java --include=*.kt
grep -rn 'navigate("\|onNavigate("' app/src/main/java --include=*.kt
```

### 3.5 Gắn `TrackScreen` vào từng loại màn

**a) Route thường** — dòng đầu trong `composable {}`:

```kotlin
composable(Screen.Home.route) {
    TrackScreen(Screen.Home.route)
    HomeScreen(...)
}
```

**b) Route có tham số** — tên không kèm tham số, tham số đi bằng param:

```kotlin
composable("${Screen.Calculator.route}/{${Screen.ARG_TYPE}}", ...) { entry ->
    val type = CalculatorType.fromKey(entry.arguments?.getString(Screen.ARG_TYPE).orEmpty())
    TrackScreen(Screen.Calculator.route, mapOf("calculator_type" to type.key))
    CalculatorScreen(type, ...)
}
```

**c) Cùng route, 2 UI khác nhau:**

```kotlin
val kind = entry.arguments?.getString(Screen.ARG_TYPE).orEmpty()
TrackScreen(if (kind == "currency") Screen.CURRENCY_CONVERTER else Screen.UNIT_CONVERTER)
if (kind == "currency") ApiConverterScreen(back) else ConverterScreen(kind, back)
```

**d) Cùng route dùng ở 2 luồng** — đọc cờ tại chỗ, không hoist; first-run không có nút back thì log
Back thoát app:

```kotlin
composable(Screen.CurrencySetting.route) {
    val firstRun = !AppStorage.isOnboardingDone(context)
    TrackScreen(if (firstRun) Screen.FIRST_CURRENCY else Screen.CurrencySetting.route)
    if (firstRun) LogOnboardingExit(Screen.FIRST_CURRENCY)
    CurrencyScreen(...)
}
```

**e) Route chứa nhiều tab tự quản bằng state** (không phải route con) — không track route cha, track
theo tab đang chọn: `TrackScreen(when (tab) { 1 -> Screen.TAB_B; else -> Screen.TAB_A })`. Nếu mỗi
tab là 1 route riêng (như Loan Calculator) thì làm như a).

Vexaro: route `MAIN` chứa 3 tab bằng state, cộng thêm 1 lớp phủ xem media toàn màn (không phải
route). Lớp phủ che kín cả màn nên tính là 1 màn riêng; dialog / bottom sheet thì vẫn không tính:

```kotlin
TrackScreen(
    when {
        viewing != null -> Dest.MEDIA_VIEWER        // lớp phủ toàn màn
        selectedTab == TAB_PROGRESS -> Dest.PROGRESS
        selectedTab == TAB_FINISHED -> Dest.FINISHED
        else -> Dest.BROWSER
    },
)
```

`TrackScreen` gắn với lifecycle của `NavBackStackEntry` của `MAIN`, nên đi sang màn phụ rồi Back về
vẫn log lại tab đang mở.

**f) Pager (Intro)** — route tổng **không** gọi `TrackScreen`; log theo slide bằng `settledPage`
(không dùng `currentPage`: vuốt dở rồi kéo về sẽ log nhầm slide kế):

```kotlin
val pager = rememberPagerState(pageCount = { SLIDES.size })
val selectedAds = remember { AdPositions.selected() }
val adsVariant = remember(selectedAds) { selectedAds.joinToString(",") }   // "1,22,3,4"
val screenName = "${Screen.Intro.route}${pager.settledPage + 1}"
TrackScreen(screenName, mapOf("ads_variant" to adsVariant))
LogOnboardingExit(screenName)
```

**g) Activity riêng dùng Compose (Language)** — gọi `TrackScreen` ở **một** chỗ (trong composable
màn, không đặt thêm ở `setContent`). Màn tách trạng thái + 2 luồng → 4 tên. Trạng thái phải
`rememberSaveable` (đổi locale / xoay màn làm Activity tạo lại):

```kotlin
@Composable
fun LanguageScreen(onDone: (code: String, method: String) -> Unit, fromSettings: Boolean = false) {
    val context = LocalContext.current
    var selectedCode by rememberSaveable { mutableStateOf<String?>(null) }
    val hasSelected = selectedCode != null
    TrackScreen(
        when {
            fromSettings && hasSelected -> Screen.LANGUAGE_SELECT
            fromSettings -> Screen.LANGUAGE
            hasSelected -> Screen.FIRST_LANGUAGE_SELECT
            else -> Screen.FIRST_LANGUAGE
        },
    )
    // language_select chỉ lần ĐẦU null → có giá trị; gửi thẳng, không qua lifecycle.
    // from_settings giống language_confirm — để funnel first-run không lẫn lượt đổi từ Cài đặt.
    fun select(code: String, method: String) {
        if (selectedCode == null) {
            AppEvents.log(
                context, "language_select",
                "code" to code, "method" to method, "from_settings" to fromSettings,
            )
        }
        selectedCode = code
    }
    // ... row click → select(code, "click"); auto-select → select(code, "auto") (xem 3.8)
}

// MyLanguageActivity: truyền fromSettings từ Intent extra; log language_confirm trước khi confirm
LanguageScreen(
    fromSettings = isFromSettings,
    onDone = { code, method ->
        AppEvents.log(this, "language_confirm", "code" to code, "method" to method, "from_settings" to isFromSettings)
        LanguageRouter.confirmLanguageSelection(...)
    },
)
```

### 3.6 Màn của lib — `MyApplication`

Không sửa được code lib → log ở `onActivityResumed`, so tên class bằng chuỗi (tên Activity khai
trong manifest AAR được R8 giữ nguyên). Lấy danh sách Activity của lib:

```
unzip -p base-application/base-application-*.aar AndroidManifest.xml | grep -o 'android:name="[^"]*Activity[^"]*"'
```

```kotlin
override fun onCreate() {
    LanguageRouter.customActivityClass = MyLanguageActivity::class.java
    super.onCreate()
    // ...
    ScreenTracker.init()
    trackLibScreens()
}

private fun trackLibScreens() {
    registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            val firstRun = !AppStorage.isOnboardingDone(activity)   // chưa từng vào Home
            val name = when (activity.javaClass.name) {
                LIB_SPLASH_ACTIVITY -> Screen.SPLASH
                LIB_NOTI_PERMISSION_ACTIVITY -> if (firstRun) Screen.FIRST_NOTI_PERMISSION else Screen.NOTI_PERMISSION
                LIB_IAP_ACTIVITY -> if (firstRun) Screen.FIRST_IAP else Screen.IAP
                else -> return   // AdActivity (quảng cáo) cố ý bỏ qua
            }
            ScreenTracker.log(activity, name)
        }
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    })
}

private companion object {
    const val LIB_SPLASH_ACTIVITY = "com.brian.base_application.start.SplashActivity"
    const val LIB_NOTI_PERMISSION_ACTIVITY = "com.brian.base_application.start.RequestNotificationPermissionActivity"
    const val LIB_IAP_ACTIVITY = "com.brian.base_iap.iap.IapActivity"
}
```

**App không có cờ "onboarding xong" (Vexaro):** Vexaro quyết định vào Intro theo **số lần mở app**
(Remote Config `count_app_open` / `organic_number_not_guide`), không có cờ một-lần kiểu
`AppStorage.isOnboardingDone`. Khi đó dùng cờ "đã từng vào Home", bật ngay khi `MainActivity` được
tạo lần đầu:

```kotlin
// platform/OnboardingExit.kt (đặt chung file cho gọn)
object HomeReached {
    private const val KEY = "home_reached"
    private fun prefs(context: Context) =
        context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
    fun isReached(context: Context): Boolean = prefs(context).getBoolean(KEY, false)
    fun mark(context: Context) {
        if (!isReached(context)) prefs(context).edit().putBoolean(KEY, true).apply()
    }
}

// MainActivity.onCreate, sau super.onCreate()
HomeReached.mark(this)

// trackLibScreens
val firstRun = !HomeReached.isReached(activity)
```

Lấy danh sách Activity của lib: giải nén AAR rồi `grep` manifest như trên. Trên Windows không có
`unzip` thì dùng Git Bash, hoặc đổi đuôi `.aar` → `.zip` rồi mở `AndroidManifest.xml`.

### 3.7 `navigateSafely` phải trả về `Boolean`

> Chỉ áp dụng nếu app có pattern `navigateSafely`. Vexaro không có (navigate thẳng
> `navController.navigate(...)`) nên bỏ qua mục này và không có event `nav_blocked`.

Pattern chặn crash "State must be at least CREATED" bỏ qua navigate khi entry không RESUMED. Nếu
caller đã bật cờ "đã xong" trước khi biết navigate có chạy không → **kẹt vĩnh viễn** (lỗi Intro4 của
Loan Calculator). Cờ chỉ được bật khi navigate thật sự xảy ra:

```kotlin
private fun NavHostController.navigateSafely(
    context: Context, route: String, builder: NavOptionsBuilder.() -> Unit = {},
): Boolean {
    val state = currentBackStackEntry?.lifecycle?.currentState
    if (state == Lifecycle.State.RESUMED) {
        navigate(route, builder)
        return true
    }
    AppEvents.log(context, "nav_blocked", "route" to route.substringBefore('/'), "state" to state?.name)
    return false
}

// Onboarding xong chỉ khi navigate thành công
fun toHome(): Boolean {
    val firstTime = !AppStorage.isOnboardingDone(context)
    val navigated = navController.navigateSafely(context, Screen.Home.route) { popUpTo(0) { inclusive = true } }
    if (navigated) {
        AppStorage.setOnboardingDone(context)
        if (firstTime) AppEvents.log(context, "onboarding_complete", "intro_shown" to introShown)
    }
    return navigated
}

// IntroScreen(onFinish: () -> Boolean) — trang cuối:
} else if (!finished) {
    finished = onFinish()   // KHÔNG: finished = true; onFinish()
}
```

### 3.8 Timer tự chuyển màn chỉ đếm khi RESUMED

`LaunchedEffect` + `delay` vẫn chạy khi màn bị che (click native ad mở **overlay CH Play bán trong
suốt**, app-open ad, bấm Home). Màn tự chuyển nhưng `TrackScreen` chỉ log khi RESUMED → mất
`<PREFIX>_Intro2..4`, funnel báo drop giả. Áp dụng cho **mọi** timer auto-next / auto-select /
auto-confirm:

```kotlin
val lifecycle = LocalLifecycleOwner.current.lifecycle   // androidx.lifecycle.compose

// Intro auto-next: dừng khi bị che, quay lại đếm lại từ đầu; mỗi trang chỉ auto 1 lần.
LaunchedEffect(page, lifecycle) {
    var fired = false
    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        if (fired) return@repeatOnLifecycle
        delay(if (hasAd) AUTO_NEXT_WITH_AD_MS else AUTO_NEXT_PLAIN_MS)
        fired = true
        onButtonClick("auto")
    }
}

// Language auto-confirm: như trên, fired chặn gọi onDone 2 lần.
// AUTO_CONFIRM_DELAY_MS = 3s, rồi đợi 2 ad done (LANGUAGE_SCREEN_CUSTOM.md §7.2).
LaunchedEffect(selectedCode, scrollSignal, lifecycle) {
    val code = selectedCode ?: return@LaunchedEffect
    var fired = false
    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        if (fired) return@repeatOnLifecycle
        delay(AUTO_CONFIRM_DELAY_MS)
        snapshotFlow { languageDone && selectDone }.filter { it }.first()
        fired = true
        confirm(code, "auto")   // confirm() có guard `confirmed`, chặn trùng với lúc user bấm ✓
    }
}
```

Intro của Vexaro trước đó dùng `LaunchedEffect(page) { delay(...); onButtonClick() }` không gate. Khi
ghép phải đổi sang dạng trên, đồng thời đổi `onButtonClick: () -> Unit` thành
`onButtonClick: (method: String) -> Unit` để mỗi chỗ gọi truyền `"click"` / `"auto"` /
`"modal_close"` / `"modal_error"` cho event `intro_next`.

Timer fallback chỉ mở khoá nút khi ad load chậm (không tự chuyển màn) thì không cần gate.

**Trường hợp dễ sót: timer fallback gián tiếp làm chuyển màn.** Timer không gọi navigate trực tiếp,
nhưng bật một cờ mà effect khác dùng để tự chuyển màn. Vexaro gặp ở màn Permission: fallback 6s bật
`adSettled = true`, rồi `LaunchedEffect(perNoti, adSettled) { if (perNoti && adSettled) onDone() }` tự
sang Home ngay cả khi màn đang bị che. Cách sửa: gate chính chỗ **chuyển màn**, không phải chỗ timer:

```kotlin
LaunchedEffect(perNoti, adSettled, owner) {
    if (!(perNoti && adSettled)) return@LaunchedEffect
    var fired = false
    owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        if (fired) return@repeatOnLifecycle
        fired = true
        onDone()
    }
}
```

Cách rà khi ghép app mới: `grep -rn "delay(" app/src/main/java`, rồi với **mỗi** timer hỏi "giá trị nó
bật có dẫn tới navigate / onDone / onFinish không?". Có thì gate chỗ navigate như trên. Các timer chỉ
đếm ngược nút đóng ad, hay cập nhật thanh tua video, thì không cần.

### 3.9 Modal ad lỗi phải tự đi tiếp

Modal native toàn màn giữa các slide: nếu đang được yêu cầu hiện mà lỗi / quá hạn → tự sang trang.
Không thì user kẹt (timer auto-next của trang đã dùng hết lượt). Lỗi lúc preload thì chỉ tắt modal.

```kotlin
NativeAdsFull(
    start = showFull[i],
    onShown = { logModal(i, "shown") },          // overlay thật sự hiện (≠ onLoaded = preload xong)
    onClose = { logModal(i, "closed"); onButtonClick("modal_close") },
    onError = {
        val requested = showFull[i]
        modalStatus[i] = false
        showFull[i] = false
        logModal(i, "error")
        if (requested) onButtonClick("modal_error")
    },
    ...
)
```

Vexaro: `NativeAdsFull` **không có** callback `onShown`. Khi đó log `shown` ngay lúc app bật modal
(trong `onButtonClick`, chỗ set `showFull[p] = true`). Lưu ý cho người đọc số: `shown` = app **yêu
cầu** hiện, chưa chắc ad đã render.

```kotlin
if (modalEnabled(p) && !showFull[p] && modalStatus[p]) {
    showFull[p] = true
    logModal(p, "shown")
}
```

### 3.10 Event nghiệp vụ

Chỉ thêm event ở chỗ `screen_view` không trả lời được câu hỏi. Gọi `AppEvents.log` thẳng trong
callback.

| Event | Param | Gọi ở đâu | Trả lời câu hỏi |
|---|---|---|---|
| `onboarding_route` | `intro_shown`, `is_ads_campaign`, `open_count` | `MainActivity.resolveStartRoute`, chỉ khi `savedInstanceState == null` | Vì sao user có / không thấy Intro |
| `language_select` | `code`, `method=click\|auto`, `from_settings` | Lần đầu chọn ngôn ngữ | User tự chọn hay app tự chọn |
| `language_confirm` | `code`, `method=click\|auto`, `from_settings` | Trước `confirmLanguageSelection` | User đã xác nhận chưa |
| `intro_next` | `from_page`, `method=click\|auto\|modal_close\|modal_error`, `ads_variant` | Đầu `onButtonClick` của Intro | Có `intro_next` mà không có screen_view kế → navigation hỏng |
| `intro_modal` | `page`, `result=shown\|closed\|error`, `ads_variant` | Callback của modal ad | Drop có do modal ad không |
| `nav_blocked` | `route` (bỏ tham số), `state` | Nhánh bị chặn của `navigateSafely` | User bấm nhưng navigate thất bại |
| `onboarding_exit` | `screen` | `LogOnboardingExit` ở Intro và `First_Currency` | User bấm Back thoát |
| `onboarding_complete` | `intro_shown` | `toHome()` lần đầu | Hoàn thành onboarding |
| `iap_open` | `source` | Hàm mở paywall của app (`IapOpener.open`) | Paywall mở từ đâu |
| `calculate` / `compare_add` / `share_pdf` / `currency_select` | `calculator_type` / `calculator_type` / — / `code` | Callback tương ứng | Hành động chính của app |

Ghi chú khi ghép (rút ra từ Vexaro):

- **`onboarding_route`:** app không có `resolveStartRoute` trong `MainActivity` mà quyết định trong
  ViewModel (Vexaro: `AppViewModel.init`) thì gửi ở đó. ViewModel sống qua `recreate()` nên tự đúng
  điều kiện "chỉ 1 lần mỗi lần mở".
- **`onboarding_complete`:** gửi ở hàm kết thúc onboarding (Vexaro: `finishOnboarding` trong
  `AppNav.kt`). Nếu Intro được quyết định theo **số lần mở app** thì cùng 1 user có thể xem Intro
  nhiều lần, nên event này xuất hiện nhiều lần. Ghi rõ điều đó vào tài liệu cho người đọc số.
- **`onboarding_exit`:** đặt `LogOnboardingExit` ở mọi màn onboarding có thể Back thoát app (Vexaro:
  `VD_Intro1..4` và `VD_Permission`). Màn Language first-run đã khoá Back nên không cần.
- **`nav_blocked` / `iap_open` / event hành động chính:** chỉ thêm nếu app có chỗ tương ứng. Vexaro
  chưa thêm event hành động chính (tải video…); nếu cần thì thêm sau, nhớ dò trùng tên ở §3.11.

### 3.11 Script encode — cập nhật skip list

`encode-scripts/rename-code-symbols.js` thay `\b<tên hàm/class/object>\b` **toàn cục, kể cả bên trong
string literal**. Hệ quả:

- Tên có `_` (`LC_Home`, `intro_next`, `ads_variant`) an toàn — `_` là ký tự của từ nên `\b` không
  khớp vào giữa.
- Tên param / giá trị event là **từ đơn** (`page`, `result`, `code`, `click`…) trùng tên một hàm /
  class của app → bị thay bằng hash. Build vẫn xanh, không có log, custom dimension trên GA4 không
  nhận nữa.
- Activity của app không nằm trong skip list → auto `screen_view` của Firebase báo `screen_class`
  là chuỗi ngẫu nhiên.

#### a) Không mã hoá `MyLanguageActivity` — BẮT BUỘC

Thêm `"MyLanguageActivity"` vào **`PROTECTED_FOLDER_AND_FILE_BASE`** trong
`encode-scripts/safe-skip-base.js`, ngay cạnh `MainActivity`. Danh sách này **bảo vệ file**: giữ
nguyên tên file `MyLanguageActivity.kt` và class bên trong.

```js
PROTECTED_FOLDER_AND_FILE_BASE: [
  "dao", "database", "core", "data", "Color", "Type", "Success", "Theme", "MainActivity",
  "AdActivity", "MyLanguageActivity"
],
```

**Vì sao đúng chỗ này** (đọc từ code của 2 script đổi tên trong `encode-scripts/`):

| Script | Làm gì với `MyLanguageActivity` |
|---|---|
| `rename-file.js` | Đổi tên **file** `.kt` sang chuỗi hash, class và manifest đổi theo. Chỉ bỏ qua tên nằm trong `PROTECTED_FOLDER_AND_FILE_BASE` (+ `PROTECTED_FOLDER_AND_FILE_APP_SPECIFIC`). **Đây là script cần chặn.** |
| `rename-code-symbols.js` | Đổi tên class / hàm trong code. Script này **tự bỏ qua** mọi class trùng tên với file chứa nó (`buildFileNameBlacklist`), nên class `MyLanguageActivity` nằm trong `MyLanguageActivity.kt` không bao giờ bị đổi ở đây. **Không cần** thêm vào `SAFE_SKIP_BASE`. |

Vì sao thêm vào **file lõi** `safe-skip-base.js` mà không vào file config riêng của app: đầu file đó
ghi "chỉ thêm khi tên đó thực sự chung cho mọi app". Mọi app dùng gói cập nhật này đều có màn
`MyLanguageActivity`, nên nó cùng loại với `MainActivity` / `AdActivity` đang nằm sẵn ở đó.

Không sửa được file lõi (repo dùng chung khoá file đó) thì khai ở file config riêng của app,
`encode-scripts/rename-config-global.js`, mảng tương ứng:

```js
PROTECTED_FOLDER_AND_FILE_APP_SPECIFIC: [ "MyLanguageActivity" ],
```

(`rename-file.js` gộp danh sách lõi + danh sách config, khai ở đâu cũng có tác dụng.)

**Thiếu thì hỏng gì:** file và class bị đổi tên sang hash. Nếu chạy `reverse.js` sót hoặc lệch,
manifest và code không khớp → **`ActivityNotFoundException`** khi SDK mở màn Language, tức crash ngay
lần mở app đầu tiên (lỗi đã xảy ra thật ở app Loan Calculator). Kể cả không crash, GA4 tự log
`screen_view` với `screen_class` là chuỗi hash.

Kiểm tra sau khi encode (chạy ở root project):

```
grep -rn "MyLanguageActivity" app/src/main/AndroidManifest.xml
find app/src/main/java -name "MyLanguageActivity.kt"
```

Cả 2 lệnh phải ra kết quả. Ra rỗng nghĩa là tên đã bị mã hoá.

> Tên trùng tên file chỉ an toàn ở `rename-code-symbols.js` khi **file đó không bị đổi tên**. Đừng đổi
> tên file `MyLanguageActivity.kt` hay tách class sang file khác tên.

#### b) Tên param / giá trị event trùng tên hàm

Thêm vào `SAFE_SKIP_APP_SPECIFIC` trong `encode-scripts/rename-config-global.js`, kèm comment lý do.
Những tên này là riêng từng app nên **không** đưa vào file lõi:

```js
"page",                 // param intro_modal, trùng `fun page(...)` của app
"result",               // param intro_modal, trùng `fun result(id)` trong Screen.kt
```

Chỉ cần thêm **khi script dò bên dưới báo trùng**. Vexaro chạy script ra `TRÙNG: []` nên không thêm
gì ở mục này.

Dò trùng mỗi khi thêm event / param mới (chạy ở root project):

```python
import re, glob
names = set()
for f in glob.glob('app/src/main/java/**/*.kt', recursive=True):
    s = open(f, encoding='utf-8').read()
    for m in re.finditer(r'\b(?:(?:class|object)\s+([A-Za-z_]\w*)|fun\s+(?:[\w.<>?, ]+\.)?([A-Za-z_]\w*)\s*\()', s):
        names.add(m.group(1) or m.group(2))
lits = set()
for f in glob.glob('app/src/main/java/**/*.kt', recursive=True):
    s = open(f, encoding='utf-8').read()
    lits |= set(re.findall(r'"([A-Za-z]\w*)"\s+to\b', s))                  # key của param
    lits |= set(re.findall(r'AppEvents\.log\([^"]*"([a-z_]+)"', s))        # tên event
lits |= {"click", "auto", "shown", "closed", "error", "home", "settings"}  # thêm giá trị event từ đơn
print("TRÙNG:", sorted(l for l in lits if l in names))
```

Trên Windows nên lưu đoạn trên thành file `.py` rồi chạy `python file.py`, đừng dán vào heredoc của
Git Bash: dấu `\` trong regex có thể bị shell nuốt mất.

### 3.12 GA4

- Đăng ký custom dimension (event-scoped) cho mọi param cần lọc: `method`, `ads_variant`, `page`,
  `calculator_type`, `source`, `code`…
- **Không** tắt `google_analytics_automatic_screen_reporting_enabled`: tắt sẽ mất số liệu các
  Activity của lib chưa được map. Auto `screen_view` (không có tên `<PREFIX>_`) vẫn chạy song song →
  khi phân tích lọc `screen_name` bắt đầu bằng `<PREFIX>_`.
- Báo cáo **Pages and screens** mặc định nhóm theo *screen class* → đổi dimension sang
  **"Page title and screen name"**.
- Số liệu có sau 24–48 giờ; xem ngay bằng Realtime / DebugView.
- Funnel: lọc theo `app_version` từ build có tracking mới, không thì user build cũ bị tính là drop.

## 4. Không được làm

| Không | Vì sao |
|---|---|
| `LaunchedEffect(Unit)` / `DisposableEffect` để log màn | Không log khi back về màn cũ |
| Tên màn từ `javaClass.simpleName` / `::class.simpleName` | Bị R8 / script encode đổi |
| Nhét tham số route vào tên màn (`LC_Result/12`) | Mỗi giá trị thành 1 màn, phá báo cáo |
| Gọi `TrackScreen` trong dialog / bottom sheet / modal ad | Không phải màn |
| Đặt `TrackScreen` ở cả `setContent` lẫn composable màn | Log 2 lần |
| Bật cờ "đã xong" trước khi biết navigate có chạy | Kẹt màn vĩnh viễn |
| Timer tự chuyển màn bằng `delay` không gate lifecycle | Màn tự chạy khi bị che, mất screen_view |
| Cờ `skipNextResume` bật trước khi hiện ad để chặn log trùng | Không chặn được app-open ad (lib tự hiện) và click ad; dùng cơ chế trùng tên của `ScreenTracker` |
| `BackHandler` rồi gọi lại `onBackPressed()` để chuyển tiếp Back | Lặp vô hạn; dùng `LogOnboardingExit` |
| Param / giá trị event là từ đơn mà không dò trùng tên hàm | Bị script encode hash |
| `remember` cho trạng thái của màn có thể bị tạo lại Activity | Mất trạng thái, log lại màn |

## 5. Checklist review

1. [ ] `ScreenTracker.kt`, `OnboardingExit.kt` giống mục 3.2 / 3.3; `ScreenTracker.init()` được gọi
   trong `Application.onCreate`.
2. [ ] Mọi `composable(...)` gọi `TrackScreen`, trừ route pager / route chứa tab (track theo slide /
   tab). `grep -c "composable("` ≈ `grep -c "TrackScreen("` + số route pager/tab.
3. [ ] Không còn chuỗi route cũ viết cứng (3.4).
4. [ ] Activity riêng của app: `TrackScreen` ở đúng 1 chỗ; trạng thái dùng `rememberSaveable`.
5. [ ] Màn của lib có trong `trackLibScreens`.
6. [ ] Không có 2 màn khác nhau trùng tên; cùng UI ở 2 luồng có 2 tên (`First_`).
7. [ ] Route có tham số: tên không kèm tham số.
8. [ ] Mọi caller của `navigateSafely` có cờ "đã xong" chỉ bật khi trả về `true`.
9. [ ] Mọi timer tự chuyển màn dùng `repeatOnLifecycle(RESUMED)` + `fired`, kể cả timer fallback
   **gián tiếp** dẫn tới chuyển màn (3.8, ví dụ màn Permission).
10. [ ] Modal ad lỗi lúc được yêu cầu hiện → tự đi tiếp.
11. [ ] Script dò trùng ở 3.11 không báo tên nào ngoài skip list.
12. [ ] `MyLanguageActivity` có trong `PROTECTED_FOLDER_AND_FILE_BASE` của
    `encode-scripts/safe-skip-base.js` (3.11a).
13. [ ] Custom dimension đã đăng ký trên GA4.

## 6. Test / nghiệm thu

```
adb logcat -s ScreenTracker AppEvents
adb shell setprop debug.firebase.analytics.app <applicationId>     # Firebase Console → DebugView
adb shell setprop debug.firebase.analytics.app .none.              # tắt
```

Chạy trên bản cài mới (clear data). Mỗi bước phải thấy **đúng 1** `screen_view` với tên đúng:

1. First-open đủ luồng: `<P>_Splash` → `<P>_First_Language` → `language_select` →
   `<P>_First_LanguageSelect` → `language_confirm` → `<P>_First_NotiPermission` (nếu có) →
   `<P>_First_Iap` → `onboarding_route` → `<P>_Intro1` + `intro_next` … `<P>_Intro4` →
   `<P>_First_Currency` → `onboarding_complete` → `<P>_Home`.
   Vexaro: **không có** `VD_First_Iap` (first-run cố ý bỏ IAP, LANGUAGE_SCREEN_CUSTOM.md §4.1); user
   organic ở lần mở 1 vào thẳng `VD_Browser`, không qua Intro (`onboarding_route intro_shown=false`).
2. Ở Intro1 bấm native ad (overlay CH Play), chờ > 15s → Intro **đứng yên**. Đóng overlay → không
   log lại `<P>_Intro1`, sau đó auto-next ra `<P>_Intro2`.
3. Ở Intro4 bấm Home, chờ > 15s, mở lại, bấm Start → sang màn kế.
4. Ép biến thể có modal (RC `positionIntrol`), tắt mạng, bấm Continue → sau ≤ 6s tự sang trang,
   có `intro_modal result=error`.
5. Ở Language bấm Home, chờ > 30s, mở lại → vẫn ở màn Language, chưa tự chọn.
6. Chọn 1 ngôn ngữ → `…LanguageSelect`; chọn ngôn ngữ khác → **không** log thêm.
7. Ở Home: mở máy tính có interstitial → đóng ad → chỉ thấy `<P>_Calculator`, **không** có
   `<P>_Home` lần 2.
8. Result → Share → quay về: **không** log lại `<P>_Result`.
9. Cài đặt → đổi ngôn ngữ → quay về: `<P>_Setting` chỉ 1 lần.
10. Đổi tab qua lại, vào màn chi tiết, Back: mỗi lần chuyển đúng 1 dòng.
11. **Lặp lại bước 1, 2, 4 trên bản đã chạy script encode**: tên màn, tên event, tên param
    (`page`, `result`…) phải giữ nguyên chữ.

Bước nào sai thì tra mục 7.

## 7. Bắt lỗi log sai — triệu chứng → nguyên nhân → cách sửa

Tra theo **chỗ phát hiện ra lỗi**. Mỗi dòng: thấy gì, vì sao, kiểm bằng cách nào, sửa ở đâu.

Lệnh kiểm dùng chung:

```
adb logcat -s ScreenTracker AppEvents          # log app gửi đi (có cả dòng "bỏ qua, trùng lần trước")
adb shell setprop debug.firebase.analytics.app <applicationId>   # xem trên GA4 → Admin → DebugView
```

### 7.1 Thấy trong logcat lúc test

| Triệu chứng | Nguyên nhân hay gặp | Kiểm | Sửa |
|---|---|---|---|
| **1 màn log 2 dòng** `screen_view` liền nhau, khác tên | `TrackScreen` đặt ở 2 chỗ (vd cả `setContent` lẫn composable màn), hoặc route cha và màn con đều track | `grep -rn "TrackScreen(" app/src/main/java` — mỗi màn đúng 1 chỗ | Bỏ bớt, giữ 1 chỗ trong composable màn (§3.5g) |
| **Back về màn trước mà không log** | Log bằng `LaunchedEffect(Unit)` / `DisposableEffect` thay vì `TrackScreen` | Tìm `logEvent(` / `ScreenTracker.log(` ngoài `ScreenTracker.kt` và `trackLibScreens` | Đổi sang `TrackScreen(...)` (§3.2) |
| **Quay về sau quảng cáo / IAP / CH Play mà log lại** màn đang đứng | Có code log thẳng, không qua `ScreenTracker.log` (mất cơ chế chặn trùng), hoặc tự viết cờ `skipNextResume` | Log phải có dòng `(bỏ qua, trùng lần trước)` khi quay về | Mọi log màn đi qua `ScreenTracker.log`; xoá cờ tự chế (§4) |
| **Có `intro_next` nhưng không có `screen_view` của slide kế** | Slide tự chuyển lúc màn bị che (timer không gate), hoặc navigate bị chặn mà vẫn bật cờ "đã xong" | Ở slide có ad, bấm vào ad (sang CH Play), chờ quá thời gian auto rồi quay lại: slide phải đứng yên | Gate `repeatOnLifecycle(RESUMED)` + `fired` (§3.8); cờ chỉ bật khi navigate trả `true` (§3.7) |
| **Màn tự sang màn khác trong lúc đang bị che**, mất `screen_view` | Timer fallback **gián tiếp** bật cờ dẫn tới chuyển màn (Vexaro: màn Permission) | `grep -rn "delay(" app/src/main/java`, xét từng timer | Gate chỗ navigate, không phải chỗ timer (§3.8) |
| **Màn của SDK (Splash, IAP, xin quyền) không có log** | Tên class trong `trackLibScreens` sai, hoặc thiếu `registerActivityLifecycleCallbacks` | So tên class với manifest trong AAR (§3.6) | Sửa hằng `LIB_*_ACTIVITY` cho đúng |
| **Màn Language log lại mỗi lần đổi ngôn ngữ / xoay màn** | Trạng thái màn dùng `remember` thay vì `rememberSaveable`, Activity tạo lại thì mất | Đổi ngôn ngữ từ Cài đặt, xem log | `rememberSaveable` cho `selectedCode` (§3.5g) |
| **`language_select` gửi nhiều lần** trong 1 lần vào màn | Bỏ mất điều kiện `if (selectedCode == null)` | Chọn 2 ngôn ngữ liên tiếp → chỉ được 1 event | Giữ điều kiện "chỉ lần đầu" (§3.5g) |
| **Bấm Back ở onboarding bị lặp vô hạn / treo** | Chuyển tiếp Back bằng `BackHandler` rồi gọi lại `onBackPressed()` | — | Dùng `LogOnboardingExit` (§3.3) |
| **Không thấy log gì cả** | Chưa gọi `ScreenTracker.init()`, hoặc filter logcat sai tag | Log có tag `ScreenTracker` / `AppEvents` | `ScreenTracker.init()` trong `Application.onCreate` |

### 7.2 Thấy trên GA4

| Triệu chứng | Nguyên nhân hay gặp | Kiểm | Sửa |
|---|---|---|---|
| Báo cáo toàn tên **`MainActivity`**, không thấy `<P>_…` | Báo cáo đang nhóm theo *screen class* | — | Đổi dimension sang "Page title and screen name", lọc bắt đầu bằng `<P>_` (§3.12) |
| **Param không hiện** trong báo cáo / Explore | Chưa đăng ký custom dimension, hoặc mới đăng ký (không áp dụng ngược) | Admin → Custom definitions | Đăng ký Scope = Event (§3.12); chờ số liệu mới |
| **Funnel rơi giả** ở 1 bước | Bước đó không phải ai cũng đi qua (Intro theo số lần mở, màn xin quyền chỉ hiện với một số máy, IAP bị bỏ ở lần đầu…) | Breakdown theo `is_ads_campaign`, `open_count` | Bỏ bước không bắt buộc khỏi funnel; ghi rõ trong tài liệu event của app |
| **Người dùng bản cũ bị tính là drop** | Funnel không lọc version | — | Lọc `app_version` ≥ bản có tracking (§3.12) |
| **`<P>_First_*` xuất hiện ở người dùng cũ** (hoặc người mới không có) | Cờ "lần đầu" sai: đánh dấu quá sớm / quá muộn, hoặc cờ của app khác nghĩa | Cài mới, xem màn IAP / xin quyền mang tên `First_` trước khi vào Home | Cờ phải bật đúng lúc vào Home lần đầu (`HomeReached`, §3.6) |
| **Tên màn bị nhân bản theo tham số** (`<P>_Player/abc.mp4`…) | Nhét tham số route vào tên màn | — | Tên màn cố định, tham số đi bằng param (§3.5b) |
| **Số `screen_view` tăng vọt** ở 1 màn | Tên màn đổi theo state liên tục (vd tên tính từ `currentPage` khi đang vuốt) | Xem log lúc thao tác màn đó | Tên màn phải ổn định; pager dùng `settledPage` (§3.5f) |

### 7.3 Chỉ xảy ra ở bản đã chạy script encode

| Triệu chứng | Nguyên nhân | Kiểm | Sửa |
|---|---|---|---|
| **Crash `ActivityNotFoundException` khi mở màn Language** | `MyLanguageActivity` bị mã hoá: manifest và class lệch tên | `grep MyLanguageActivity app/src/main/AndroidManifest.xml` và `find … -name MyLanguageActivity.kt` sau khi encode | Thêm vào `PROTECTED_FOLDER_AND_FILE_BASE` (§3.11a) |
| `screen_class` trên GA4 là **chuỗi hash** | File Activity của app bị `rename-file.js` đổi tên: không nằm trong `PROTECTED_FOLDER_AND_FILE_*` | DebugView, xem param `screen_class` của auto `screen_view` | Thêm tên Activity vào `PROTECTED_FOLDER_AND_FILE_BASE` (§3.11a) |
| **Tên param / giá trị event thành hash** (`page` → `f3a9c1d`) | Tên đó trùng tên 1 hàm / class của app, script encode thay luôn trong chuỗi | Chạy script dò trùng (§3.11) | Thêm vào `SAFE_SKIP_APP_SPECIFIC` (§3.11b) |
| Tên màn `<P>_…` hoặc tên event bị đổi | Hiếm: tên **không có** dấu `_` | — | Mọi tên màn / event phải có `_` (§2) |

> Bản encode không có log lỗi nào báo các trường hợp ở 7.3: build vẫn xanh, chỉ thấy khi chạy app
> hoặc xem GA4. Luôn chạy lại test §6 bước 11 trên bản encode trước khi phát hành.

## Phụ lục — tên màn và event của Loan Calculator (ví dụ, `PREFIX = LC`)

| Nhóm | screen_name |
|---|---|
| Lib | `LC_Splash`, `LC_First_NotiPermission` / `LC_NotiPermission`, `LC_First_Iap` / `LC_Iap` |
| Onboarding | `LC_First_Language` → `LC_First_LanguageSelect`, `LC_Intro1`…`LC_Intro4` (+ `ads_variant`), `LC_First_Currency` |
| Tab | `LC_Home`, `LC_Tools`, `LC_Compare`, `LC_Setting` |
| Màn con | `LC_Calculator` (+ `calculator_type`), `LC_Result`, `LC_CompareDetail` (+ `calculator_type`), `LC_CurrencyConverter`, `LC_UnitConverter`, `LC_History`, `LC_WorldClock`, `LC_AddClock` |
| Cài đặt | `LC_Currency`, `LC_Language` → `LC_LanguageSelect` |

Event: xem bảng 3.10.

## Phụ lục — tên màn của Vexaro (`PREFIX = VD`)

| Nhóm | screen_name |
|---|---|
| Lib | `VD_Splash`, `VD_First_NotiPermission` / `VD_NotiPermission`, `VD_First_Iap` / `VD_Iap` |
| Onboarding | `VD_First_Language` → `VD_First_LanguageSelect`, `VD_Intro1`…`VD_Intro4` (+ `ads_variant`), `VD_Permission` |
| Tab trong route `VD_Main` (route không log) | `VD_Browser`, `VD_Progress`, `VD_Finished`, lớp phủ `VD_MediaViewer` |
| Màn con | `VD_History`, `VD_Bookmarks`, `VD_Sites`, `VD_HowTo`, `VD_Setting`, `VD_Pin`, `VD_PrivateVideo`, `VD_Player` (route `VD_Player/{path}/{name}`, tên màn không kèm tham số) |
| Cài đặt | `VD_Language` → `VD_LanguageSelect` |

Route trong `object Dest` (file `ui/nav/AppNav.kt`) đã đổi giá trị sang đúng tên màn; tên chỉ dùng cho
Firebase (tab, Language, màn của lib) là hằng riêng trong cùng `Dest`. Giải thích từng event cho người
đọc số: [ANALYTICS_EVENTS.md](ANALYTICS_EVENTS.md).
