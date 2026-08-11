# Common Actions — pattern dùng chung mọi app (update/language/IAP/rate/share)

> Khác `ADS.md` (ads) — đây là 5 hành vi **không phải ads**, nhưng lặp lại giống nhau ở mọi app trong
> dự án: check bản cập nhật, mở màn Language, mở màn IAP, rate app, share app. Khi build app mới, copy
> đúng khuôn dưới đây, không cần tự nghĩ lại cách làm.

## 1. Check bản cập nhật (chỉ gọi ở Home)

Dùng Play Core **In-App Update API** để biết có bản mới trên Play Store, tự vẽ dialog riêng (không
dùng flow update in-place chuẩn của Play Core) — bấm "Update" thì mở thẳng trang app trên Play Store.

**Dependency** (`app/build.gradle`):
```groovy
implementation 'com.google.android.play:app-update:2.1.0'
```

**Gọi 1 lần duy nhất ở màn Home** (ví dụ thật `ScanScreen.kt:76-77`):
```kotlin
// Check bản cập nhật trên Play (chỉ ở Home)
CheckForAppUpdateWithDialog()
```
- Composable `CheckForAppUpdateWithDialog()` (đặt ở `ui/common/AppUpdateDialog.kt`) tự dùng
  `AppUpdateManagerFactory.create(context)` → check `appUpdateInfo.updateAvailability()` → hiện dialog
  nếu `UPDATE_AVAILABLE`. Bấm Update → mở `Intent.ACTION_VIEW` tới
  `https://play.google.com/store/apps/details?id=<packageName>` (`setPackage("com.android.vending")`,
  fallback mở browser nếu Play Store app lỗi).
- **KHÔNG gọi lại ở màn khác** — chỉ đặt 1 lần ở màn gốc (Home), tránh hiện dialog trùng lặp nhiều nơi.
- **Không có config bật/tắt** — hoàn toàn tự động theo phía Play Store, không có Remote Config key nào
  chi phối. Nếu cần tắt tạm, gỡ thẳng dòng gọi Composable này.

## 2. Mở màn Language (đặt ở Settings)

Màn Language do lib `base-application` sở hữu (dùng lại đúng màn chọn ngôn ngữ ở luồng khởi động
Splash→Inter→**Language**→IAP→Home) — Settings chỉ gọi lại màn đó, không tự vẽ UI Language riêng.

Ví dụ thật (`SettingScreen.kt`):
```kotlin
SettingRow(
    Icons.Filled.Language,
    stringResource(R.string.LANGUAGE_OPTIONS),
    onClick = {
        (context as? androidx.fragment.app.FragmentActivity)?.let {
            com.brian.base_application.language.LanguageActivity.start(
                it,
                com.camera.qrscanner.barcode.MainActivity::class.java,
            )
        }
    },
)
```
- `LanguageActivity.start(activity, targetActivityClass)` — hàm tĩnh của lib. Tham số 2 là Activity sẽ
  được mở lại sau khi user xác nhận đổi ngôn ngữ — **luôn truyền `MainActivity::class.java`** (activity
  gốc của app), không phải activity hiện tại.
- `context` phải ép được về `FragmentActivity` (Compose `LocalContext.current` trong 1 Activity thường
  đã thoả điều kiện này).

## 3. Mở màn IAP/Premium (gọi được ở bất cứ đâu trong app)

Màn mua Premium do lib cung cấp, gọi trực tiếp 1 hàm tĩnh, không cần Activity cụ thể (nhận `Context`
thường):
```kotlin
com.brian.base_iap.utils.NativeCodecSnowFlakeCortexAI.nativeAiStartIapActivity(context)
```
- Đặt ở bất kỳ `onClick`/hành động nào muốn dẫn user tới màn mua Premium (nút icon Premium ở
  Settings/TopBar, nút "Remove ads", CTA sau khi bị chặn tính năng premium...).
- Không có helper wrapper riêng trong app — gọi thẳng full-qualified tên hàm tại chỗ dùng (không cần
  tạo thêm 1 hàm `openPremium(context)` trung gian, trừ khi có >1 chỗ gọi và muốn tránh lặp import dài).

## 4. Rate app

Dùng **In-App Review API** của Google Play — hiện popup đánh giá NGAY trong app (không rời app), nếu
lỗi (quá số lần Google cho phép trong ngày, network...) thì fallback mở trang Play Store.

**Dependency** (`app/build.gradle`):
```groovy
implementation 'com.google.android.play:review-ktx:2.0.2'
```

Code app tự viết (ví dụ thật `SettingScreen.kt`):
```kotlin
private fun requestReview(context: Context) {
    val activity = context as? Activity ?: return
    val manager = ReviewManagerFactory.create(context)
    manager.requestReviewFlow().addOnCompleteListener { task ->
        if (task.isSuccessful) {
            manager.launchReviewFlow(activity, task.result)
        } else {
            try {
                activity.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")),
                )
            } catch (_: Exception) {
            }
        }
    }
}
```
Gọi: `onClick = { requestReview(context) }` ở dòng "Rate app" trong Settings.

⚠️ In-App Review API **không đảm bảo popup luôn hiện** (Google giới hạn số lần/ngày theo tài khoản) —
đây là hành vi CHUẨN của Google, không phải bug. Không cố "ép" hiện lại nhiều lần trong 1 session.

## 5. Share app

Chuẩn `Intent.ACTION_SEND` chia sẻ text kèm link Play Store — không cần lib gì thêm.

Code app tự viết (ví dụ thật `SettingScreen.kt` + `platform/QrActions.kt`):
```kotlin
private fun shareApp(context: Context) {
    val text = "${context.getString(R.string.app_name)} - ${context.getString(R.string.share_message)}\n" +
        "android: https://play.google.com/store/apps/details?id=${context.packageName}"
    QrActions.share(context, text)
}

// platform/QrActions.kt
fun share(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    start(context, Intent.createChooser(intent, null))
}
```
Gọi: `onClick = { shareApp(context) }` ở dòng "Share app" trong Settings. Cần có string
`R.string.share_message` (nội dung mời dùng app, tự viết theo app) — `app_name` và `packageName` lấy
tự động, không cần điền tay.

## 6. Checklist khi clone sang app mới

- [ ] Đã có dependency `com.google.android.play:app-update:2.1.0` (mục 1) và
      `com.google.android.play:review-ktx:2.0.2` (mục 4) trong `app/build.gradle`.
- [ ] `CheckForAppUpdateWithDialog()` đặt đúng 1 lần ở màn Home/gốc — không đặt thêm ở màn khác.
- [ ] Settings có đủ 4 dòng: Language (`LanguageActivity.start`), Premium
      (`nativeAiStartIapActivity`), Rate app (`requestReview`), Share app (`shareApp`) — copy đúng
      khuôn ở mục 2-5, không cần viết lại logic.
- [ ] `MainActivity::class.java` truyền đúng vào `LanguageActivity.start` (đổi theo package app mới).
- [ ] String `share_message` đã điền nội dung thật cho app (không để trống/placeholder).
