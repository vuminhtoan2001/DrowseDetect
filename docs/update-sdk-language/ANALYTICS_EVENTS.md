# Tổng quan tracking Firebase / Google Analytics — app Vexaro (prefix `VD`)

Tài liệu này dành cho người **đọc và phân tích số liệu** trên Google Analytics 4 (GA4). Nó liệt kê
mọi tên màn và event app đang gửi, ý nghĩa của từng cái, và cách đọc chúng để phân tích.

- Cách code tracking hoạt động (dành cho dev): [SCREEN_TRACKING.md](SCREEN_TRACKING.md).
- Code nằm ở: `app/.../platform/ScreenTracker.kt` (`TrackScreen`, `AppEvents`), `platform/OnboardingExit.kt`.
- Bản đã điền cho app hiện tại: [../ANALYTICS_EVENTS.md](../ANALYTICS_EVENTS.md).

> **Đây là bản mẫu của app Vexaro.** Khi ghép sang app khác (bước 5 trong [README.md](README.md)),
> copy file này, đổi prefix `VD_`, danh sách màn ở mục 2 và các chi tiết riêng của Vexaro (Intro theo
> số lần mở, lần đầu bỏ IAP, 3 tab Browser / Progress / Finished) cho đúng app đó, rồi gửi cho người
> phân tích số liệu. Cấu trúc mục 1, 3 – 7 dùng lại được gần như nguyên vẹn.

---

## 1. Hai loại dữ liệu app gửi lên

| Loại | Là gì | Tên trên GA4 |
|---|---|---|
| **Màn hình** | Mỗi lần người dùng thực sự nhìn thấy 1 màn | Event `screen_view`, tên màn nằm ở param `screen_name` (luôn bắt đầu bằng `VD_`) |
| **Event nghiệp vụ** | Hành động / quyết định mà `screen_view` không trả lời được (người dùng tự chọn hay app tự chọn, vì sao có / không thấy Intro…) | Tên event viết thường, có gạch dưới: `language_select`, `intro_next`… |

### 1.1 Quy tắc của `screen_view` — cần nắm để đọc số cho đúng

- **1 lần nhìn thấy = 1 `screen_view`.** Quay lại đúng màn đang đứng thì **không** log lại. Ví dụ:
  sau quảng cáo inter/app-open, sau màn IAP, sau khi mở CH Play, Settings hệ thống, bấm Home rồi mở
  lại app.
- **Ngoại lệ:** nếu app nằm ở nền **quá 30 phút** rồi mở lại thì màn hiện tại được log lại (tính là
  phiên mới, giống cách GA4 tính phiên).
- **Back về màn trước** (tên khác màn vừa log) thì **có** log.
- **Dialog, bottom sheet, menu, quảng cáo toàn màn không phải là màn**, nên không có `screen_view`.
- **GA4 vẫn tự gửi thêm `screen_view` của riêng nó**, với tên là tên Activity (`MainActivity`,
  `SplashActivity`…). Khi phân tích, **luôn lọc `screen_name` bắt đầu bằng `VD_`** để chỉ lấy số
  liệu của app.

### 1.2 Kiểu giá trị param

- Chữ (`code = "vi"`) giữ nguyên.
- Đúng/sai được gửi dạng **chữ**: `"true"` / `"false"`, không phải boolean.
- Số nguyên (`open_count`, `page`, `from_page`) gửi dạng số.

---

## 2. Danh sách tên màn (`screen_name`)

### 2.1 Luồng mở app lần đầu (onboarding)

| `screen_name` | Màn | Ghi chú |
|---|---|---|
| `VD_Splash` | Splash của SDK | Mỗi lần mở app đều có. |
| `VD_First_Language` | Màn chọn ngôn ngữ, **lần đầu**, **chưa** chọn ngôn ngữ nào | SDK chỉ mở màn này ở lần đầu cài app. |
| `VD_First_LanguageSelect` | Màn chọn ngôn ngữ lần đầu, **đã** chọn 1 ngôn ngữ (tay hoặc tự động) | Chỉ log 1 lần, lúc ngôn ngữ đầu tiên được chọn. Đổi sang ngôn ngữ khác **không** log thêm. |
| `VD_First_NotiPermission` | Màn xin quyền thông báo của SDK, lần đầu | Không phải ai cũng thấy (tuỳ SDK / phiên bản Android). |
| `VD_First_Iap` | Màn mua Premium của SDK, lần đầu | **Hiện không xuất hiện:** lần đầu app cố ý bỏ màn IAP, đi thẳng Language → (xin quyền thông báo) → màn chính. Tên này giữ sẵn, phòng khi bật lại IAP. |
| `VD_Intro1` … `VD_Intro4` | 4 slide Intro | Đi kèm param `ads_variant` (xem §3). |
| `VD_Permission` | Màn xin quyền của app (sau Intro) | Chỉ hiện khi người dùng **chưa** bật thông báo. |

> **"Lần đầu" được xác định thế nào:** app đánh dấu khi người dùng vào màn chính (`MainActivity`)
> lần đầu tiên. Trước thời điểm đó, màn IAP và màn xin quyền thông báo mang tên `VD_First_*`. Sau đó
> thì mang tên không có `First_` (`VD_Iap`, `VD_NotiPermission`).

### 2.2 Màn chính (3 tab)

| `screen_name` | Màn |
|---|---|
| `VD_Browser` | Tab trình duyệt (tab mặc định khi vào app) |
| `VD_Progress` | Tab đang tải |
| `VD_Finished` | Tab đã tải xong |
| `VD_MediaViewer` | Xem ảnh / nghe nhạc / xem video toàn màn từ danh sách đã tải |

### 2.3 Các màn phụ

| `screen_name` | Màn |
|---|---|
| `VD_History` | Lịch sử duyệt web |
| `VD_Bookmarks` | Dấu trang |
| `VD_Sites` | Trang web gợi ý |
| `VD_HowTo` | Hướng dẫn cách tải |
| `VD_Setting` | Cài đặt |
| `VD_Pin` | Nhập mã PIN vào thư mục riêng tư |
| `VD_PrivateVideo` | Thư mục video riêng tư |
| `VD_Player` | Trình phát video (tên màn **không** kèm tên file) |

### 2.4 Mở từ Cài đặt / sau lần đầu

| `screen_name` | Màn |
|---|---|
| `VD_Language` | Màn chọn ngôn ngữ mở từ Cài đặt, chưa chọn |
| `VD_LanguageSelect` | Màn chọn ngôn ngữ mở từ Cài đặt, đã chọn 1 ngôn ngữ |
| `VD_Iap` | Màn Premium (không phải lần đầu) |
| `VD_NotiPermission` | Màn xin quyền thông báo của SDK (không phải lần đầu) |

---

## 3. Danh sách event nghiệp vụ

### 3.1 `onboarding_route` — app quyết định có cho xem Intro hay không

**Khi nào gửi:** mỗi lần `MainActivity` được mở (sau Splash / Language / xin quyền thông báo).

| Param | Giá trị | Ý nghĩa |
|---|---|---|
| `intro_shown` | `"true"` / `"false"` | `true` = lần mở này đi vào Intro, `false` = vào thẳng màn chính |
| `is_ads_campaign` | `"true"` / `"false"` | Người dùng cài từ quảng cáo (`true`) hay tự nhiên / organic (`false`) |
| `open_count` | số | Đây là lần mở app thứ mấy |

**Trả lời câu hỏi:** vì sao người này có / không thấy Intro.

> ⚠️ **Rất quan trọng khi làm funnel:** Intro **không** chỉ hiện ở lần mở đầu tiên. App quyết định
> theo số lần mở và nguồn cài, qua 2 key Remote Config `count_app_open` (mặc định 3) và
> `organic_number_not_guide` (mặc định 1):
>
> - **Cài từ quảng cáo:** lần mở **1 → 3** vào Intro, từ lần 4 vào thẳng màn chính.
> - **Organic:** lần mở **1** vào thẳng màn chính, lần **2 → 4** vào Intro, từ lần 5 vào thẳng màn
>   chính.
>
> Hệ quả:
>
> - Người organic sẽ **không** có `VD_Intro1` ngay sau `language_confirm` ở lần đầu. Đó **không phải**
>   drop.
> - Cùng 1 người có thể xem Intro **nhiều lần**, và có nhiều `onboarding_complete`.
>
> Khi phân tích luôn tách theo `is_ads_campaign` và `open_count`.

### 3.2 `language_select` — lần đầu một ngôn ngữ được chọn

**Khi nào gửi:** lần **đầu tiên** một ngôn ngữ được chọn trên màn Language. Chỉ 1 lần mỗi lần vào
màn; đổi qua lại giữa các ngôn ngữ không gửi thêm.

| Param | Giá trị | Ý nghĩa |
|---|---|---|
| `code` | `en`, `vi`, `zh-CN`… | Ngôn ngữ được chọn |
| `method` | `click` | Người dùng tự bấm chọn |
| | `auto` | Người dùng không chọn gì trong 8 giây, app **tự chọn** theo ngôn ngữ của máy (không có trong danh sách thì chọn `en`) |
| `from_settings` | `"true"` / `"false"` | `true` = mở từ Cài đặt, `false` = luồng lần đầu |

**Trả lời câu hỏi:** người dùng có chủ động chọn ngôn ngữ hay để app tự chọn.

### 3.3 `language_confirm` — ngôn ngữ được xác nhận, rời màn Language

**Khi nào gửi:** ngay trước khi lưu ngôn ngữ và rời màn.

| Param | Giá trị | Ý nghĩa |
|---|---|---|
| `code` | mã ngôn ngữ | Ngôn ngữ **cuối cùng** được lưu (có thể khác `code` của `language_select` nếu người dùng đổi) |
| `method` | `click` | Người dùng tự bấm nút ✓ |
| | `auto` | App tự xác nhận: đã chọn ngôn ngữ, 3 giây không đổi, **và** cả 2 quảng cáo native đã xong (impression / lỗi / quá 8 giây). Quảng cáo xong sớm thì xác nhận ở giây thứ 3; quảng cáo chậm thì đợi quảng cáo |
| `from_settings` | `"true"` / `"false"` | `true` = mở từ Cài đặt, `false` = luồng lần đầu |

**Trả lời câu hỏi:**

- Người dùng đã qua được màn Language chưa.
- Bao nhiêu % tự bấm, bao nhiêu % do app tự đi tiếp.

### 3.4 `intro_next` — chuyển slide ở Intro

**Khi nào gửi:** mỗi lần app chuyển slide, hoặc chuyển từ slide cuối sang màn kế.

| Param | Giá trị | Ý nghĩa |
|---|---|---|
| `from_page` | 1 – 4 | Đang ở slide mấy |
| `method` | `click` | Người dùng bấm nút Tiếp tục / Bắt đầu |
| | `auto` | Hết thời gian chờ, app tự chuyển (5 giây nếu slide không có quảng cáo, 15 giây nếu có). Bộ đếm **dừng** khi màn bị che (bấm quảng cáo, bấm Home) |
| | `modal_close` | Người dùng đóng quảng cáo toàn màn giữa các slide |
| | `modal_error` | Quảng cáo toàn màn lỗi đúng lúc đang được yêu cầu hiện, app tự chuyển để không bị kẹt |
| `ads_variant` | vd `"1,22,3,4"` | Biến thể vị trí quảng cáo ở Intro (xem bên dưới) |

> **Cách đọc `ads_variant`:** biến thể lấy từ Remote Config `positionIntrol`. Số `N` = slide N có
> native ad nhỏ trong slide. Số `NN` (`22`, `33`…) = sau slide N có quảng cáo native **toàn màn**.
> Ví dụ `"1,22,3,4"`: slide 1, 3, 4 có native nhỏ; slide 2 có quảng cáo toàn màn.

> **Lưu ý:** khi bấm nút ở slide có quảng cáo toàn màn, `intro_next` được gửi nhưng app **chưa**
> chuyển slide, mà bật quảng cáo toàn màn trước. Đóng quảng cáo thì có thêm 1 `intro_next` với
> `method = modal_close`, lúc đó mới chuyển slide. Vì vậy số `intro_next` của slide có quảng cáo toàn
> màn sẽ **cao hơn** số lần chuyển slide thật.

**Trả lời câu hỏi:** người dùng tự đọc Intro hay để tự chạy. Ngoài ra, nếu có `intro_next` mà
**không** có `screen_view` của slide kế tiếp, thì điều hướng có vấn đề.

### 3.5 `intro_modal` — quảng cáo toàn màn giữa các slide

| Param | Giá trị | Ý nghĩa |
|---|---|---|
| `page` | 1 – 4 | Quảng cáo toàn màn của slide mấy |
| `result` | `shown` | App yêu cầu hiện quảng cáo (người dùng vừa bấm nút / hết giờ) |
| | `closed` | Người dùng đóng quảng cáo |
| | `error` | Quảng cáo lỗi (lúc tải trước hoặc lúc đang hiện) |
| `ads_variant` | như trên | |

**Trả lời câu hỏi:** người dùng có rời app vì quảng cáo toàn màn không. Nếu `shown` nhiều hơn hẳn
`closed + error`, người dùng thoát app ngay khi gặp quảng cáo.

> `shown` được gửi lúc app **yêu cầu** hiện. Nếu quảng cáo chưa tải xong thì chưa chắc người dùng đã
> thấy nó.

### 3.6 `onboarding_exit` — bấm Back thoát app giữa onboarding

| Param | Giá trị | Ý nghĩa |
|---|---|---|
| `screen` | `VD_Intro1`…`VD_Intro4`, `VD_Permission` | Thoát ở màn nào |

**Trả lời câu hỏi:** người dùng **chủ động** bỏ đi ở đâu. Drop có `onboarding_exit` là người dùng tự
thoát. Drop không có là người dùng tắt app / bấm Home / app lỗi.

> Màn Language lần đầu đã **khoá nút Back**, nên không có `onboarding_exit` ở màn đó.

### 3.7 `onboarding_complete` — xong Intro, vào màn chính

| Param | Giá trị | Ý nghĩa |
|---|---|---|
| `intro_shown` | luôn `"true"` | Event này chỉ gửi ở cuối luồng Intro → Permission |

**Lưu ý:** người vào thẳng màn chính (không qua Intro) **không** có event này. Người xem Intro nhiều
lần (xem §3.1) sẽ có nhiều `onboarding_complete`.

---

## 4. Các funnel nên dựng

Dựng trong GA4 → **Explore** → **Funnel exploration**. Bật **"Make open funnel" = tắt** (bắt buộc đi
từ bước 1), và **lọc theo `app_version`** từ bản có tracking này trở lên. Nếu không lọc, người dùng
bản cũ sẽ bị tính là drop.

### 4.1 Funnel lần đầu cài app

| Bước | Điều kiện |
|---|---|
| 1 | `screen_view`, `screen_name = VD_Splash` |
| 2 | `screen_view`, `screen_name = VD_First_Language` |
| 3 | `language_select`, `from_settings = false` |
| 4 | `language_confirm`, `from_settings = false` |
| 5 | `onboarding_route` |

Không có bước `VD_First_Iap`: lần đầu app bỏ màn IAP. Màn xin quyền thông báo của SDK
(`VD_First_NotiPermission`) chỉ hiện với một phần người dùng nên **không** đưa vào làm bước bắt buộc.

**Breakdown** theo `is_ads_campaign` để so sánh 2 nhóm người dùng.

### 4.2 Funnel Intro (chỉ người dùng được cho xem Intro)

| Bước | Điều kiện |
|---|---|
| 1 | `onboarding_route`, `intro_shown = true` |
| 2 | `screen_view`, `screen_name = VD_Intro1` |
| 3 | `screen_view`, `screen_name = VD_Intro2` |
| 4 | `screen_view`, `screen_name = VD_Intro3` |
| 5 | `screen_view`, `screen_name = VD_Intro4` |
| 6 | `onboarding_complete` |
| 7 | `screen_view`, `screen_name = VD_Browser` |

**Breakdown** theo `ads_variant` để xem biến thể quảng cáo nào làm rơi người dùng nhiều nhất.

### 4.3 Đọc nguyên nhân drop

| Thấy gì | Nghĩa là |
|---|---|
| Drop giữa 2 slide **có** `onboarding_exit` ở slide trước | Người dùng chủ động bấm Back thoát |
| Drop giữa 2 slide, **có** `intro_modal result=shown` nhưng không có `closed`/`error` | Người dùng bỏ đi khi gặp quảng cáo toàn màn |
| Có `intro_next` nhưng không có `screen_view` của slide kế | Lỗi điều hướng, báo dev |
| `language_confirm` → không có `onboarding_route` | Người dùng thoát ở màn xin quyền thông báo, hoặc lỗi chuyển màn (báo dev nếu tỉ lệ cao) |
| `VD_First_Language` cao nhưng `language_confirm` thấp | Người dùng thoát ở màn Language. Xem tiếp tỉ lệ `method` |

---

## 5. Cài đặt bắt buộc trên GA4 (làm 1 lần)

### 5.1 Đăng ký custom dimension

GA4 **không** hiển thị param của event trong báo cáo nếu chưa đăng ký. Vào **Admin → Data display →
Custom definitions → Create custom dimension**, chọn **Scope = Event** rồi tạo cho từng param:

| Param | Tên gợi ý |
|---|---|
| `method` | Method |
| `code` | Language code |
| `from_settings` | From settings |
| `intro_shown` | Intro shown |
| `is_ads_campaign` | Is ads campaign |
| `open_count` | Open count |
| `from_page` | From page |
| `page` | Intro modal page |
| `result` | Intro modal result |
| `ads_variant` | Ads variant |
| `screen` | Exit screen |

> Custom dimension chỉ có số liệu **từ thời điểm tạo trở đi**, không áp dụng ngược cho dữ liệu cũ.
> Nên tạo ngay khi phát hành bản có tracking.

### 5.2 Xem đúng tên màn trong báo cáo "Pages and screens"

**Reports → Engagement → Pages and screens**. Báo cáo này mặc định nhóm theo *screen class* (sẽ chỉ
thấy toàn `MainActivity`). Bấm vào tên cột và đổi dimension sang **"Page title and screen name"**.
Thêm bộ lọc `Page title and screen name` **bắt đầu bằng** `VD_`.

### 5.3 Thời gian có số liệu

- Báo cáo thường: **24 – 48 giờ** sau khi người dùng dùng app.
- **Realtime** (Reports → Realtime): thấy trong vài phút.
- **DebugView**: thấy ngay, dùng khi test, xem §6.

---

## 6. Kiểm tra tracking khi test

```
# Xem log trên máy
adb logcat -s ScreenTracker AppEvents

# Bật DebugView cho máy test (Firebase Console / GA4 → Admin → DebugView)
adb shell setprop debug.firebase.analytics.app com.video.smartdownload.downloadervideo

# Tắt DebugView
adb shell setprop debug.firebase.analytics.app .none.
```

Trong log `ScreenTracker`:

- Dòng `screen_view: VD_xxx` nghĩa là đã gửi.
- Dòng `(bỏ qua, trùng lần trước)` nghĩa là quay lại đúng màn đang đứng nên không gửi. Đây là hành vi
  **đúng**.

Mỗi bước phải thấy **đúng 1** `screen_view` với tên đúng. Nếu thấy 2 lần liên tiếp cùng tên, hoặc
thiếu tên ở một bước, thì báo dev.

---

## 7. Những điều dễ hiểu sai

1. **"Người organic không xem Intro"** → sai. Họ xem Intro ở lần mở 2 – 4, không phải lần 1 (§3.1).
2. **"`screen_view` của `VD_Browser` = số lần mở app"** → sai. Quay lại app trong vòng 30 phút không
   log lại. Dùng event `session_start` / `first_open` có sẵn của GA4 để đếm phiên và cài mới.
3. **"`intro_next` = số lần chuyển slide"** → không chính xác ở slide có quảng cáo toàn màn (§3.4).
4. **"Tổng số màn `VD_*` = tổng `screen_view`"** → sai. GA4 còn tự log `screen_view` theo tên
   Activity. Luôn lọc prefix `VD_`.
5. **"`language_select.code` = ngôn ngữ người dùng dùng"** → không chắc. Ngôn ngữ cuối cùng nằm ở
   `language_confirm.code`.
6. **Số liệu của bản đã chạy script encode** vẫn giữ nguyên tên màn / event / param (đã kiểm tra
   không bị đổi tên). Nếu thấy tên lạ dạng chuỗi hash trên GA4, báo dev kiểm tra lại script encode.
