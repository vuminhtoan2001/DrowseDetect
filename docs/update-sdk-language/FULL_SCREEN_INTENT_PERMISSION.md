# Bước 4 — Dialog mời bật quyền Full-screen intent (Android 14+)

Ở màn Home, nếu người dùng chưa bật quyền "hiển thị thông báo toàn màn hình", app hiện 1 dialog mời
bật, bấm "Cho phép" thì mở đúng màn Settings của hệ thống. Mỗi lượt app ở foreground hỏi tối đa 1 lần.

Code đã có sẵn trong core: `ui/common/FullScreenIntentPermissionDialog.kt`, icon `drawable/notification_bell.png`,
4 string `full_screen_intent_permission_*` (21 ngôn ngữ), và `HomeScreen` đã gọi `CheckFullScreenIntentPermission()`.
App mới chỉ cần đổi màu (`Theme`/`ThemeSecond`) nếu bảng màu khác, và **viết lại câu `message`** cho đúng app.

## 1. Cách ghép (tham khảo — core đã làm sẵn)

| Việc | Chi tiết |
|---|---|
| Copy file | `common/FullScreenIntentPermissionDialog.kt` → `<app>/ui/common/` (hoặc package UI dùng chung của app) |
| Sửa import | Dòng `package …`, `import <package app>.R` và 2 import màu `…ui.theme.Theme`, `…ui.theme.ThemeSecond` (đổi sang 2 màu chủ đạo của app đích: gradient nút, bóng, hào quang icon; app không có thì khai `Color(0x…)` tại chỗ). Tên file ở bản mẫu Vexaro là mẫu, màu thật theo app |
| Icon | Copy `res/drawable/notification_bell.png` → `<app>/src/main/res/drawable/` (dialog dùng `R.drawable.notification_bell`; tên resource chỉ được chữ thường, số, `_`). Muốn icon riêng thì thay file, giữ tên |
| Dependency | `implementation 'androidx.lifecycle:lifecycle-process:2.9.4'` (đã thêm ở [UPDATE_SDK.md](UPDATE_SDK.md)) |
| String | 4 key `full_screen_intent_permission_*` (mục 5): đã gộp vào `strings.xml` cùng lúc với 3 key màn Language khi chạy `res/merge_strings.py`. **Sau đó sửa câu `message` cho đúng app ở mọi ngôn ngữ** |
| Gọi ở Home | `CheckFullScreenIntentPermission()` — mục 3 |
| Manifest | **Không** cần khai: SDK đã khai `USE_FULL_SCREEN_INTENT` trong manifest của AAR |

## 2. Quyền `USE_FULL_SCREEN_INTENT` là gì, vì sao cần dialog riêng

- **Android < 14:** là quyền thường, SDK đã khai sẵn → coi như **luôn có**, không cần xin.
- **Android 14+ (API 34, `UPSIDE_DOWN_CAKE`):** người dùng phải **tự bật** trong Settings. App không xin
  được qua hộp thoại runtime permission, chỉ có thể mở đúng màn Settings cho người dùng bật.

```kotlin
private fun isFullScreenIntentGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    return nm.canUseFullScreenIntent()
}

private fun openFullScreenIntentSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
    AppOpenManager.getInstance().disableAdResumeByClickAction()   // mục 4
    context.startActivity(
        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}")),
    )
}
```

`isFullScreenIntentGranted` trả `true` trên Android < 14 không có nghĩa app đã xin được quyền, mà là
quyền đó **không cần xin** ở các bản Android này.

## 3. Gọi ở đâu — chỉ ở màn Home

```kotlin
@Composable
fun HomeScreen(...) {
    CheckFullScreenIntentPermission()
    // ... nội dung Home
}
```

- Đặt **cạnh** `CheckForAppUpdateWithDialog()` nếu app có; 2 composable chạy độc lập.
- **Chỉ đặt ở Home**, không đặt ở `NavHost` / `AppNav` chung: đặt ở đó thì dialog có thể hiện đè lên
  Intro / onboarding.
- Home gồm nhiều tab trong 1 route (Vexaro: 3 tab Browser / Progress / Finished) thì đặt trong
  composable chứa các tab (Vexaro: `MainTabsScreen` trong `ui/nav/AppNav.kt`). Đổi tab hay vào màn phụ
  rồi Back về đều **không** hỏi lại, nhờ cờ ở mục 3.1.

### 3.1 "Mỗi phiên chỉ hỏi 1 lần" — vì sao KHÔNG dùng biến static thường

```kotlin
private object FullScreenIntentPromptState {
    var askedThisSession = false

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                askedThisSession = false
            }
        })
    }
}
```

Biến static thường là **sai**: Android không kill process khi người dùng bấm Home / Back thoát app
(giữ lại để mở lại nhanh). Biến static sống theo process, nên thoát ra vào lại vẫn giữ `true` →
không bao giờ hỏi lại.

`ProcessLifecycleOwner.onStop` chỉ chạy khi **toàn bộ app** rời foreground (không tính chuyển
Activity trong app hay xoay màn), nên đây là mốc "hết phiên" đúng. Kết quả:

- Trong 1 phiên: hỏi tối đa 1 lần, dù Home bị dựng lại nhiều lần.
- Thoát app rồi mở lại: hỏi lại nếu quyền vẫn chưa bật.

## 4. Chặn quảng cáo app-open khi quay lại từ Settings

Bấm "Cho phép" → app mở Settings → người dùng quay lại app. SDK coi đây là "quay lại foreground" và
bắn quảng cáo app-open, đè lên ngay sau khi người dùng vừa bật quyền. Gọi
`AppOpenManager.getInstance().disableAdResumeByClickAction()` **ngay trước** `startActivity` để SDK
bỏ qua lần đó.

> Áp dụng pattern này ở **mọi** chỗ app tự mở màn bên ngoài rồi quay lại (Settings, trình duyệt, Play
> Store, chia sẻ…), không riêng màn này.

## 5. String resources

| Key | Ý nghĩa | Tiếng Anh (mẫu) |
|---|---|---|
| `full_screen_intent_permission_title` | Tiêu đề dialog | Allow full-screen alerts |
| `full_screen_intent_permission_message` | Lý do cần quyền | Allow the app to show full-screen notifications so you don't miss important updates, such as when a download finishes. |
| `full_screen_intent_permission_grant` | Nút chính, mở Settings | Allow |
| `full_screen_intent_permission_later` | Nút phụ, chỉ đóng dialog | Not now |

> **Sửa câu `message` cho đúng app đích.** Bản mẫu nhắc tới "tải xuống hoàn tất" vì Vexaro là app tải
> video. App khác phải đổi ví dụ cho khớp tính năng của app đó, ở **mọi** ngôn ngữ.

**Không** dùng lại `baseapp_fullscreen_1` / `baseapp_fullscreen_2` (hay `sdk_fullscreen_1/2`): đó là nội
dung **bên trong** thông báo toàn màn của SDK, khác ngữ cảnh với dialog xin quyền.

Chuỗi có dấu `'` (tiếng Anh `don't`, tiếng Pháp `l'application`…) phải viết `\'`. File mẫu trong
`res/` đã escape sẵn.

## 6. Log để debug

```
adb logcat -s FullScreenIntentPerm
```

Log đủ các bước: giá trị `askedThisSession`, `SDK_INT` + kết quả `canUseFullScreenIntent()`, quyết định
hiện dialog, người dùng bấm Allow / Not now, mở Settings thành công / lỗi, và lúc reset cờ ở
`onStop`.

## 7. Test

1. Máy **Android 14+**: vào `Settings > Apps > <app> > Full screen notifications`, **tắt** quyền.
2. Mở app tới Home → dialog hiện.
3. Bấm "Not now" → đổi tab, vào màn phụ rồi Back → **không** hỏi lại.
4. Bấm Home thoát app, mở lại → hỏi lại (không cần force-stop).
5. Bấm "Allow" → mở đúng màn Settings của app; bật quyền rồi quay lại → **không** có quảng cáo
   app-open đè lên, và lần mở sau không hỏi nữa.
6. Máy Android 13 trở xuống → không bao giờ hiện dialog.
7. App vào Intro (onboarding) trước → dialog **không** hiện ở Intro, chỉ hiện khi tới Home.

## 8. Pitfall

1. **Đừng đổi `askedThisSession` về biến static / `remember` thường** — mục 3.1.
2. **Đừng quên `disableAdResumeByClickAction()`** ở chỗ khác tự mở màn ngoài app — mục 4.
3. **Đừng đặt `CheckFullScreenIntentPermission()` ở NavHost chung** — dialog hiện đè lên onboarding.
