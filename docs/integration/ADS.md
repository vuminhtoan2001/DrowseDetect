# Cách dùng Ads (native / interstitial) trong app này

> Tài liệu này KHÔNG dạy cách tích hợp lib ads từ đầu — chỉ dạy **cách gọi** hệ thống ads đã có sẵn
> trong app (`AdManager`, `NativeAdSlot`, `InterFlow`) khi thêm 1 vị trí quảng cáo mới. Khi cần thêm
> ads ở 1 màn/vị trí mới, đọc file này rồi làm theo đúng 1 trong 2 mẫu ở mục 2/3, KHÔNG cần đọc code
> lib `Admob`/`base-application`.

## 1. Quy ước key (nắm cái này trước, mọi thứ suy ra từ đây)

Mỗi vị trí quảng cáo có 1 **placement name** (đặt tự do, vd `native_setting`, `inter_scan`), từ đó suy
ra các key Remote Config đi kèm:

| Key Remote Config | Dùng cho | Ý nghĩa |
|---|---|---|
| `<placement>` | native + inter | Ad Unit ID thật, đọc qua remote key tổng `ads_config` (fallback asset `default_ads_config.json` nếu remote chưa có) |
| `<placement>_enable` | native + inter | bật/tắt vị trí này (boolean), default `true` nếu chưa set |
| `<placement>_showRatio` | **chỉ inter/native-sau-inter** | cứ N lần gọi mới đủ điều kiện hiện 1 lần (N = giá trị này), default `1` |
| `<placement>_maxShowPerDay` | **chỉ inter/native-sau-inter** | số lần tối đa hiện trong 1 ngày, default `0` |

⚠️ **`_showRatio`/`_maxShowPerDay` CHỈ áp dụng cho inter và native-sau-inter** (đi qua
`AdManager.shouldShowInter`). Native ad thường (đặt cố định trong 1 màn, không đi kèm inter) **không**
có 2 key này — chỉ cần `_enable`.

⚠️ **Nếu quên khai báo `<placement>_maxShowPerDay` → mặc định là `0` → ads KHÔNG BAO GIỜ hiện**
(điều kiện là `số lần đã hiện >= maxShowPerDay`, `0 >= 0` luôn đúng). Đây là lỗi hay gặp nhất khi thêm
1 kịch bản inter mới mà quên khai báo default — nhớ khai `maxShowPerDay >= 1` ở
`HostApplication.setAppDefaults` (mục 0.2 dưới đây) trước khi test.

## 0.1 Setup lần đầu khi clone sang app mới — test mode + key bắt buộc

Làm 1 lần khi mới clone project sang app mới, TRƯỚC khi có unit id thật từ AdMob console.

**`admob_app_id`** ở
[`sdk_strings.xml`](../../app/src/main/res/values/sdk_strings.xml) — sửa thành **App ID test chính
thức của Google** (dùng chung cho mọi app khi chưa có app thật trên AdMob console):
```xml
<string name="admob_app_id" translatable="false">ca-app-pub-3940256099942544~3347511713</string>
```

**Unit id trong `default_ads_config.json`** — khi build app mới, mọi entry dùng **unit id test chính
thức của Google theo đúng LOẠI ads** (native/inter/open), KHÔNG dùng unit id thật cho tới khi app có
tài khoản AdMob riêng và đã tạo unit thật:

| Loại ads | Unit id test (Google chính thức) |
|---|---|
| Native (mọi key `native_*`) | `ca-app-pub-3940256099942544/2247696110` |
| Interstitial (mọi key `inter_*`) | `ca-app-pub-3940256099942544/1033173712` |
| Open/App-open (`open_splash`, `open_all`) | `ca-app-pub-3940256099942544/9257395921` |

⚠️ Cùng 1 loại (vd tất cả `native_*`) dùng CHUNG 1 unit id test — không cần mỗi placement 1 unit id
test riêng, vì unit test không phân biệt vị trí, chỉ để kiểm tra pipeline load/hiện ads chạy đúng.

**Các key CỐ ĐỊNH đã có trong `default_ads_config.json`** — thuộc lib `base-application` (vỏ khởi
động) hoặc thuộc khung sườn onboarding (Intro/Permission) đã baked sẵn trong skeleton, KHÔNG gắn với
màn tính năng riêng của từng app → **không xoá, không cần đụng khi build app mới**, chỉ đổi ảnh/title
tương ứng nếu Intro/Permission đổi nội dung:
```
open_splash, inter_splash, splash_uninstall, open_all,
native_language, native_keep_user, native_survey_user, native_exit_app,
native_permission, native_intro1, native_intro2, native_intro3, native_intro4
```
Khi thêm màn tính năng mới cho app (Home trở đi), KHÔNG thêm key vào nhóm này — làm theo mục 2/3 để
tạo placement riêng cho màn đó.

## 0.2 Khai báo default cho 3 key `_enable/_showRatio/_maxShowPerDay` — làm TRƯỚC TIÊN, ở code

3 key `_enable`/`_showRatio`/`_maxShowPerDay` đọc từ Firebase Remote Config lúc runtime, nhưng **phải
khai giá trị default ngay trong code**, ở
[`HostApplication.kt`](../../app/src/main/java/com/camera/qrscanner/barcode/HostApplication.kt) —
trong `onCreate()`, tham số truyền cho `FirebaseRemoteConfigUtil.getInstance().setAppDefaults(mapOf(...))`.
Đây là **nguồn giá trị dùng ngay khi app chưa fetch được Remote Config lần nào** (cài mới, mất mạng...)
— không khai ở đây thì phải chờ set đúng trên Remote Config console app mới chạy ads đúng ý, dễ quên.

Thêm placement mới → thêm dòng tương ứng vào map đó, CÙNG LÚC với việc thêm entry ở
`default_ads_config.json` (bước 1 dưới đây) — 2 file này luôn sửa cùng nhau. Remote Config console chỉ
dùng để **ghi đè** giá trị default này về sau (đổi tần suất/tắt ads không cần build lại app), không
phải bước bắt buộc để ads chạy được lần đầu.

## 2. Thêm 1 native ad thường (không đi kèm inter)

Dùng khi: muốn có 1 khối native ad cố định trong 1 màn hình (settings, list, kết quả...).

**Bước 1** — thêm entry vào `app/src/main/assets/default_ads_config.json` (unit id thật lấy từ AdMob
console, hoặc để tạm unit id test khi chưa có thật):
```json
"native_ten_man_hinh": "ca-app-pub-xxxxxxxxxxxxxxxx/yyyyyyyyyy"
```

**Bước 2** — thêm 1 dòng vào map `setAppDefaults` trong `HostApplication.kt` (mục 0.2):
```kotlin
"native_ten_man_hinh_enable" to true,
```

**Bước 3** — gọi trong Composable, đúng khuôn (ví dụ thật ở `SettingScreen.kt`):
```kotlin
if (AdManager.isAdEnabled("native_ten_man_hinh_enable")) {
    NativeAdSlot(
        slotName = "native_ten_man_hinh",
        unitId = AdManager.getAdUnitId(context, "native_ten_man_hinh"),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    )
}
```
- `slotName` và key trong `isAdEnabled`/`getAdUnitId` **phải cùng gốc tên** (`native_ten_man_hinh`) —
  chỉ khác nhau ở suffix `_enable`.
- Không cần tự kiểm tra premium/unitId rỗng — `AdManager` và `NativeAdSlot` tự lo (premium ⇒
  `getAdUnitId` trả `""` ⇒ `NativeAdSlot` tự ẩn khi `unitId.isBlank()`).
- Ví dụ thật khác cùng khuôn: `native_camera` (`ScanScreen.kt`, thêm `isSmall = true` cho slot nhỏ),
  `native_create` (`CreateItemScreen.kt`, `CreateScreen.kt`).

## 3. Thêm 1 kịch bản inter + native-xen-kẽ-sau-inter

Dùng khi: có 1 hành động chuyển màn/hoàn tất tác vụ (scan xong, tạo xong, mở tab...) muốn chèn quảng
cáo toàn màn theo tần suất — hiện interstitial thật, NGAY SAU ĐÓ (dù inter lỗi hay không lỗi, hay
không đủ điều kiện hiện) thử hiện thêm 1 native full-màn dàn dựng giống interstitial.

> **Quy ước đặt tên khi thêm mới:** dùng `native_inter_<x>` (không có "fake"). Code hiện tại còn 3 chỗ
> cũ đặt tên `native_fake_inter_<x>` (scan/create/menu) — giữ nguyên các chỗ cũ đó, chỉ áp dụng tên
> KHÔNG "fake" cho placement MỚI thêm sau này, để tên nhất quán và dễ hiểu hơn (đây vẫn là native ad
> dàn dựng giống inter, không phải native chờ load thật — bản chất kỹ thuật giống hệt "fake-inter" cũ).

**Bước 1** — thêm 2 entry vào `default_ads_config.json`:
```json
"inter_ten_hanh_dong": "ca-app-pub-xxxxxxxxxxxxxxxx/aaaaaaaaaa",
"native_inter_ten_hanh_dong": "ca-app-pub-xxxxxxxxxxxxxxxx/bbbbbbbbbb"
```

**Bước 2** — thêm 6 dòng vào map `setAppDefaults` trong `HostApplication.kt` (mục 0.2) — **cả 2
placement riêng biệt** (2 bộ đếm tần suất độc lập), nhớ `maxShowPerDay >= 1`:
```kotlin
"inter_ten_hanh_dong_enable" to true,
"inter_ten_hanh_dong_showRatio" to 3L,
"inter_ten_hanh_dong_maxShowPerDay" to 10L,
"native_inter_ten_hanh_dong_enable" to true,
"native_inter_ten_hanh_dong_showRatio" to 3L,
"native_inter_ten_hanh_dong_maxShowPerDay" to 10L,
```

**Bước 3** — trong Composable của màn chứa hành động đó:
```kotlin
val interFlow = rememberInterFlow()

fun onHoanTatHanhDong() {
    val activity = context as? android.app.Activity
    interFlow.run(context, activity, "inter_ten_hanh_dong", "native_inter_ten_hanh_dong") {
        // onDone — code chạy SAU khi cả inter + native-sau-inter đã xử lý xong (hiện hoặc bị bỏ qua
        // vì không đủ điều kiện tần suất) — vd điều hướng sang màn Result.
    }
}

// đặt 1 lần trong Composable, phủ toàn màn khi cần hiện:
InterFlowOverlay(interFlow)
```
- `interFlow.run(context, activity, interPlacement, fakePlacement, onDone)`: tự kiểm tra
  `AdManager.shouldShowInter` cho từng placement, tự quyết định hiện inter thật hay bỏ qua, rồi luôn
  thử tiếp placement thứ 2, cuối cùng gọi `onDone()`.
- Ví dụ thật cùng khuôn: `inter_scan`/`native_fake_inter_scan` (`ScanScreen.kt`),
  `inter_create`/`native_fake_inter_create` (`CreateItemScreen.kt`),
  `inter_menu`/`native_fake_inter_menu` (`CreateScreen.kt`) — chỉ khác tên "fake" vì thêm trước khi có
  quy ước đặt tên mới ở trên.

## 4. Checklist khi thêm 1 placement mới (native thường HOẶC inter+native-sau-inter)

- [ ] Đã thêm unit id thật (hoặc unit id test) vào `default_ads_config.json`.
- [ ] Đã thêm `_enable` vào `setAppDefaults` (`HostApplication.kt`) — native thường: 1 key;
      inter+native-sau-inter: 2 key riêng.
- [ ] Nếu là inter/native-sau-inter: đã thêm `_showRatio` **và** `_maxShowPerDay` vào `setAppDefaults`
      (nhớ `maxShowPerDay >= 1`, không để trống/0 nếu muốn ads thực sự hiện).
- [ ] Tên `slotName` (native thường) hoặc tên truyền vào `interFlow.run` khớp đúng gốc tên đã dùng ở
      `default_ads_config.json` + Remote Config.
- [ ] Native-sau-inter mới thêm: đặt tên `native_inter_<x>` (không "fake").
