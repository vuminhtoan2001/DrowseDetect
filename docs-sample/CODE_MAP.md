# Bản đồ code — Video Downloader

> Ghi lại **chỗ nào làm gì** và **những quyết định kỹ thuật không đọc ra được từ code**. Đặc tả chức
> năng/hình thức vẫn ở [`01_...md`](01_MO_TA_CHUC_NANG_MAN_HINH.md) →
> [`04_...md`](04_SCREENSHOT_MAPPING.md); file này chỉ nói về **implement**.
>
> Định danh app: `rootProject.name = "VideoDownloader"`, package/applicationId
> `com.video.smartdownload.downloadervideo`.

## 1. Cấu trúc

```
app/src/main/java/com/video/smartdownload/downloadervideo/
├── HostApplication.kt          Kế thừa BaseApplication của AAR (SDK là launcher). Khoá MODE_NIGHT_NO.
├── MainActivity.kt             Home trả qua getHomeActivity(). Nhận EXTRA_OPEN_PROGRESS.
├── ads/                        Khung sườn dùng chung — KHÔNG sửa khi làm tính năng
├── browser/VideoSniffer.kt     Dò video: chặn request + quét DOM, gộp theo URL
├── data/local/                 Room: Entities / Daos / AppDatabase
├── data/repository/            SettingsRepository (DataStore) · DownloadRepository (Room + tệp)
├── di/DataModule.kt            Cung cấp DataStore, AppDatabase, 3 DAO
├── domain/Models.kt            Model + 2 danh sách cố định (7 lối tắt, 5 trang gợi ý)
├── download/DownloadService.kt Foreground service tải nền, hỗ trợ tải tiếp bằng header Range
├── ui/browser/                 Màn Trình duyệt (màn lớn nhất): Screen + Components + ViewModel
├── ui/downloads/               2 tab Progress/Finished + khung danh sách dùng chung
├── ui/weblist/                 Lịch sử + Dấu trang (chung 1 composable)
├── ui/privatevideo/            Màn nhập PIN + danh sách riêng tư
├── ui/settings/ player/ howto/ sites/
├── ui/components/              Khuôn dùng lại: EmptyState · WebListRow · DownloadCard · Dialogs
├── ui/nav/                     AppNav (điều hướng) + BrowserRequests (kênh "mở URL này")
└── ui/theme/                   Color · Type · Theme(+AppShapes, AppDimens) — token, không hardcode
```

## 2. Quyết định kỹ thuật đáng lưu ý

### 2.1 Một `WebView` cho mọi thẻ
Trạng thái từng thẻ lưu/khôi phục bằng `saveState`/`restoreState` (Bundle giữ trong
`BrowserViewModel.tabStates`). Giữ nhiều `WebView` sống song song tốn bộ nhớ và dễ rò rỉ, mà người
dùng chỉ xem được 1 thẻ tại một thời điểm.

### 2.2 ⚠️ `WebView` xoá nội dung Compose vẽ TRƯỚC nó
`WebView` trong `AndroidView` vẽ bằng hardware layer riêng và **làm mất mọi nội dung Compose được vẽ
trước nó trong cùng cha**. Thanh công cụ nằm trên `WebView` trong `Column` nên bị mất trắng — đã gặp
thật (thanh công cụ trống trơn, thanh đáy vẫn hiện vì vẽ sau). Khắc phục: `Modifier.zIndex(1f)` cho
thanh công cụ. **Thêm bất kỳ thành phần nào phải nằm trên WebView đều cần nhớ điều này.**

### 2.2b ⚠️⚠️ `WebView` PHẢI có `layoutParams` MATCH_PARENT — thiếu là sập bố cục cả trang
Tạo `WebView(context)` rồi đưa vào `AndroidView` mà **không đặt `layoutParams`** thì chiều cao layout
của tài liệu bằng **0**, nên mọi `height: %` trong CSS của trang quy chiếu về 0 và sập — dù
`window.innerHeight` vẫn báo đúng (652), nên nhìn từ JS tưởng mọi thứ bình thường.

Đo thật trên trang đăng nhập `m.facebook.com` (33 tầng từ ô nhập lên `<html>`):

```
19) DIV.wbloks_113   h=600px                    ← nội dung
20) DIV.wbloks_112   h=0px  overflow=scroll     ← SẬP VỀ 0, cắt sạch phần dưới
27..33) #viewport, body, html  h=18px           ← chỉ còn 1 dòng chữ trôi nổi
```

Đặt `layoutParams = MATCH_PARENT/MATCH_PARENT` → `html/body/#viewport` thành **670px**, trang hiện
đầy đủ.

**Đây là lỗi gốc của một loạt "trang trắng" tưởng là lỗi riêng của từng trang:** Facebook đăng nhập
trắng, `x.com` chỉ hiện `#react-root` cao 0px, và nhiều khả năng cả trang chia sẻ TikTok tính
`height: 72px` (§2.3d) cũng bắt nguồn từ đây. Trước khi viết bản vá riêng cho một trang "tự tính sai
chiều cao", hãy kiểm chiều cao layout của chính WebView trước.

Cách loại trừ đã dùng (giữ lại để khỏi dò lại): **không phải** link `m.`/`www.`, **không phải**
User-Agent (thử 4 biến thể, cả phía máy chủ lẫn phía JS), **không phải** deep link bị chặn,
**không phải** `useWideViewPort`/`loadWithOverviewMode`, **không phải** JS app tiêm vào trang,
**không phải** 4 cài đặt cửa sổ/cookie, **không phải** CSS hỏng (7 stylesheet tải đủ).

> 💡 Bản debug đã bật `WebView.setWebContentsDebuggingEnabled(true)` — mở `chrome://inspect` trên máy
> tính là soi được cây layout và console của trang đang mở, nhanh hơn hẳn dò bằng `evaluateJavascript`.

### 2.3 Dò video bằng 2 đường song song
Trang web không khai báo video ở một chỗ cố định:
1. `shouldInterceptRequest` — lọc theo đuôi tệp/kiểu nội dung; bắt được HLS/DASH mà thẻ `<video>`
   không lộ đường dẫn.
2. Quét DOM bằng JS sau `onPageFinished` — bắt được video đã nằm sẵn trong trang.

Thiếu đường nào cũng sót một nhóm trang. Kết quả gộp theo URL để không hiện trùng.

⚠️ **Danh sách sniff phải gắn với video ĐANG xem, không phải với cả trang.** Feed cuộn vô hạn kiểu
TikTok chỉ có một thẻ `<video>` đổi nội dung theo slide, mà `blob:` thì không gọi API được (feed
không có liên kết bài viết nào quanh video — đã đo) nên buộc dùng địa chỉ sniff. Không dọn theo từng
video thì danh sách **tích luỹ**: video 1 hiện 1 lựa chọn, video 2 hiện 2, và mục chọn sẵn lại là
địa chỉ của video trước → **tải sai video**. `VideoSniffer.onSessionStart()` (gọi từ sự kiện
`loadstart` trong [MediaOverlay]) xử lý việc này, và **hoãn** xoá tới lúc có địa chỉ mới để không bao
giờ để danh sách rỗng — xoá ngay thì trang dựng lại thẻ video là mất luôn đường dự phòng.

Hỏi dung lượng **phải kèm `Referer`**, kể cả ở bước dựng lựa chọn: thiếu nó CDN trả 403 → dung lượng
ra 0 → bước sắp xếp theo dung lượng giảm dần mất tác dụng, bảng chọn giữ nguyên thứ tự chèn nên mục
chọn sẵn không phải mục nét nhất.

### 2.3b ⚠️ Địa chỉ media KHÔNG dùng làm khoá `LazyColumn/LazyRow`
API rất hay trả **cùng một địa chỉ cho nhiều trường**: TikTok `url` == `urlWatermark` khi video không
có bản gỡ logo, Facebook `hdUrl` == `sdUrl` khi chỉ có một mức. Khoá `LazyRow` trùng không phải hiện
2 ô giống nhau mà là **sập app** (`key ... was already used`) — đã gặp thật khi bấm tải video TikTok
qua API.

Chặn ở 2 lớp: `MediaResolver.resolve` lọc `distinctBy { it.url }` tại điểm trả về duy nhất (phủ cả
nhánh API, không chỉ nhánh dự phòng), và bảng chọn dùng khoá kèm vị trí. Thêm danh sách mới thì nhớ:
chỉ khoá theo `id` của Room hoặc theo giá trị đã lọc trùng.

### 2.3c ⚠️ Trang bật Trusted Types làm chết nút tải — KHÔNG dùng `innerHTML` trong JS chèn vào trang
YouTube (và nhiều trang của Google) đặt CSP `require-trusted-types-for 'script'`. Gán `innerHTML`
bằng chuỗi ở trang đó ném thẳng `TypeError: This document requires 'TrustedHTML' assignment`. Lỗi rơi
vào **giữa** `attach()` — sau khi đã đánh dấu thẻ là "đã xử lý" nhưng trước khi chèn nút — nên trang
đó **không có nút tải nào cả**, và vì thẻ đã mang dấu nên cũng không bao giờ được quét lại.

Đo thật trên `m.youtube.com/watch`: `video=yes mark=1 btn=no btns=0`. Sửa bằng `createElementNS`
(không đi qua bộ lọc Trusted Types), và cho `scan()` gắn lại khi `!el.__vdBtn` chứ không chỉ khi nút
bị gỡ — để một lần hỏng không giết thẻ vĩnh viễn.

**Bài học chung: mọi JS app chèn vào trang người khác phải dựng DOM bằng `createElement`, không bao
giờ bằng `innerHTML`.**

### 2.3d ⚠️ Vá bố cục phải so với TỈ LỆ KHUNG HÌNH và phải xác nhận lại, đừng tin một lần đo
Bản đầu của [CollapsedVideoFix] nhận diện "trang sập" chỉ bằng `body.scrollHeight < innerHeight*0.6`
rồi ép chiều cao video bằng `innerHeight`. Hai sai lầm, lộ ra trên YouTube:

1. **Tin một lần đo.** YouTube sập trong *chốc lát* lúc đang dựng; `ResizeObserver` chộp đúng khoảnh
   khắc đó rồi ép cứng vĩnh viễn. Đo sau khi trang ổn định thì `bodyH=2280` — không hề sập. Sửa:
   `scheduleFix()` chờ 600ms rồi **kiểm lại** mới vá; bố cục hỏng thật thì vẫn hỏng, chờ không mất gì.
2. **Ép bằng `innerHeight`.** Video 16:9 (640×360) bị kéo thành 375×641 → méo hình, thừa dải đen.
   Sửa: tính chiều cao theo tỉ lệ khung hình của chính video (`expectedHeight`), và chỉ vá khi chiều
   cao thật **nhỏ hơn một nửa** mức tỉ lệ đòi hỏi (`heightIsWrong`).

Nhờ tỉ lệ khung hình mà phân biệt được đúng/sai: YouTube dựng 211px = đúng 16:9 → không đụng; trang
chia sẻ TikTok dựng 72px trong khi tỉ lệ đòi 641px → mới là hỏng thật.

### 2.3e Hai cách phát hiện video, chọn theo dạng trang
| | Trang **bảng tin** (Facebook, TikTok feed, Instagram) | Trang **một-video-một-địa-chỉ** (YouTube) |
|---|---|---|
| Cách làm | [MediaOverlay] chèn nút đỏ lên từng ảnh/video | [SingleVideoSites] → nút tải **nổi** + địa chỉ trang |
| Căn cứ | thẻ media người dùng bấm + liên kết bài viết quanh nó | chính địa chỉ trên thanh URL |

Vì sao YouTube không dùng nút đè lên từng item:
- **Địa chỉ trang là căn cứ chắc hơn DOM.** `youtube.com/watch?v=…` nói rõ đang xem video nào; dò DOM
  phải đoán qua liên kết quanh thẻ media — cách đó từng lấy nhầm video của bài bên cạnh trên Facebook.
- **Ở trang chủ/kết quả tìm kiếm, thứ dưới ngón tay chỉ là ảnh thumbnail**, bấm vào sẽ tải tấm ảnh
  chứ không phải video.
- YouTube bật Trusted Types nên mọi thứ chèn vào trang đều dễ vỡ (§2.3c) — càng ít can thiệp càng bền.

⚠️ Chỉ thêm tên miền vào [SingleVideoSites] khi trang đó THẬT SỰ một-video-một-địa-chỉ. Trang bảng
tin mà đưa vào đây là hỏng: một địa chỉ ứng với hàng chục video, API không biết bóc cái nào.

**Giới hạn của YouTube: hiện đủ mức, nhưng phải đánh dấu mức không tiếng.** Đo thật một video: API
trả 8 mức nhưng CHỈ `360p` có `hasAudio=true`; 144p → 2160p60 đều là luồng DASH **chỉ có hình**. Giấu
đi thì mất hẳn lựa chọn nét; đưa vào mà im lặng thì người dùng bấm 1080p rồi nhận video câm. Làm theo
trang web gốc: liệt kê hết, gắn dấu 🔇 đỏ lên ô không tiếng ([DownloadOption.hasAudio]).

**API ghép luồng — có tồn tại nhưng CHƯA dùng được.** Trang web gốc ghép hình+tiếng ở máy chủ:
`POST downloadvideo.vn/api/mergeMedia` → `taskId`, rồi `POST /api/progress` mỗi 3s tới khi
`COMPLETED` thì tải `downloadUrl`. Project `alltoll` đã có sẵn `MergeApiClient` + `downloadMerge()`,
**nhưng là code chết**: không chỗ nào đặt `isMerge = true` nên nhánh đó chưa từng chạy.

Đã thử gọi trực tiếp 6 kiểu — payload của `alltoll`, 5 biến thể tên trường, payload sao y hệt web
(kèm `type`/`idLoading`/`id`, cả 2 mức tiếng), link lấy từ đúng host của web (`download.vgasoft.vn`
chứ không phải `api.downloadvideo.vn` app đang dùng), header `Origin`/`Referer` như trình duyệt, và
session cookie. **Tất cả đều `HTTP 422 {"status":false,"message":"No valid media links found"}`.**
Link googlevideo tự nó tải được (đã thử: `HTTP 206`, 245 MB) nên không phải link hỏng — nhiều khả
năng máy chủ ghép tự đi lấy link và bị YouTube chặn theo IP máy chủ. Muốn mở khoá mức nét có tiếng
thì phải hỏi đội backend, hoặc nhúng ffmpeg ghép ngay trong app.

### 2.3f ⚠️ SPA đổi địa chỉ bằng `pushState` — `onPageFinished` KHÔNG bắn
Trang dạng một-ứng-dụng (YouTube, Facebook, TikTok) chuyển nội dung bằng `history.pushState`, **không
tải lại tài liệu**, nên `onPageStarted`/`onPageFinished` đều im. Chỉ dựa vào 2 hàm đó thì địa chỉ app
giữ đứng nguyên ở trang đầu tiên.

Đo thật trên YouTube sau khi chuyển video: trang đã ở `watch?v=0mQn-pPDLuM` còn app vẫn giữ
`watch?v=G4Ma7v16LVE` — **hai video khác hẳn nhau**. Mà nút tải nổi (§2.3e) lấy đúng địa chỉ này để
gọi API, nên hậu quả là tải nhầm video người dùng xem lúc đầu. Thanh địa chỉ cũng hiện sai theo.

Vá bằng `WebViewClient.doUpdateVisitedHistory()` — callback này CÓ bắn khi `pushState`. Trong đó phải
bỏ qua khi địa chỉ không đổi, vì nó còn bắn ngay sau mỗi lần tải trang xong, không chặn thì mỗi trang
bị ghi vào lịch sử hai lần. Nhân tiện gọi `sniffer.onSessionStart()`: sang nội dung khác thì địa chỉ
media bắt được của nội dung trước không còn liên quan (§2.3).

### 2.4 Tải nền tuần tự, có tải tiếp
`DownloadService` chạy **một mục một lúc** (tải song song trên mạng di động chỉ làm mọi mục cùng chậm
và dễ đứt). Dùng header `Range` để tạm dừng/mất mạng không mất phần đã tải; máy chủ không trả
`206 Partial` thì tải lại từ đầu, nếu không tệp sẽ hỏng. Ghi tiến trình vào Room mỗi **256KB** thay vì
mỗi vòng lặp đọc — ghi liên tục làm chậm hẳn tốc độ tải.

Gửi request kèm `User-Agent` của trình duyệt và `Referer` là trang nguồn: nhiều CDN chặn request thiếu
2 header này.

### 2.5 Thư mục lưu mặc định nằm trong vùng riêng của app
`getExternalFilesDir(DIRECTORY_MOVIES)` — không cần xin quyền ghi bộ nhớ trên mọi bản Android. Muốn
video hiện trong ứng dụng Thư viện của máy thì bật `Sync to gallery`, lúc đó app chép thêm 1 bản qua
`MediaStore`.

### 2.6 Kênh `BrowserRequests` cho việc "mở URL này"
Lịch sử / Dấu trang / Trang web gợi ý / nút 🌐 trên thẻ mục tải **đóng màn hiện tại rồi trang mới
được tải** (`docs/02` §3.5) — không truyền tham số trực tiếp cho màn đích như điều hướng thường được,
nên cần một `@Singleton` trung gian.

### 2.7 Một khuôn thẻ cho 3 danh sách
`DownloadCard` + `DownloadListScaffold` dùng cho tab `Progress`, tab `Finished`, và Video riêng tư.
Phân biệt bằng `status`/`isPrivate` chứ không phải 3 composable riêng — chế độ chọn nhiều nhờ vậy viết
một lần.

### 2.8 ⚠️ Lớp phần cứng của `<video>` phủ lên nút cùng cây DOM
Đo thật trên trang chia sẻ TikTok: nút tải là con cùng khối với `<video>`, `z-index: 2147483647`,
`elementFromPoint` ngay tâm nút **vẫn trả về chính nút** (tức nút nằm trên, bấm được) nhưng trên màn
hình **không thấy gì** — WebView đưa `<video>` lên lớp vẽ riêng của phần cứng và lớp đó không tuân
theo `z-index` của DOM. `translateZ(0)` và `will-change: transform` đều **không** cứu được (đã thử).

Cách duy nhất chạy được: gắn nút **ra ngoài cây của video**, vào `document.body` với `position:
fixed`. Chỉ áp cho video chiếm ≥80% khung nhìn (`MediaOverlay.isFullscreenVideo`) — video nhỏ trong
bảng tin vẫn gắn vào khối cha, vì `fixed` buộc phải đo lại vị trí mỗi lần cuộn, đúng nguyên nhân gây
khựng đã gặp. Nút nằm trong `body` không bị gỡ theo khi trang xoá thẻ video nên phải tự dọn nút mồ
côi (`__vdOwner`).

### 2.9 Trang tính chiều cao MỘT LẦN lúc WebView chưa có kích cỡ thật
Trang chia sẻ TikTok ghi thẳng `style="height: 72px"` vào cả chuỗi ô chứa video trong khi khung nhìn
là 375×641, và `#app` (dùng `min-height: -webkit-fill-available`) chỉ ra 144px → video còn một dải
mỏng, phần còn lại đen. Bắn sự kiện `resize` **không** làm trang tính lại (đã đo). Xoá chiều cao đi
cũng không xong: chuỗi đó là flex không có chiều cao tự nhiên nên video tụt về 0 (đã đo).

`CollapsedVideoFix` vá bằng cách ghi đè lại chuỗi theo `window.innerHeight`. Nhận diện theo *triệu
chứng cả trang sập* (`body.scrollHeight < innerHeight * 0.6`) chứ không theo tên class — ngưỡng này
không thể chạm nhầm video nhỏ hợp lệ vì trang bình thường luôn cao ít nhất bằng khung nhìn. Phải theo
dõi bằng `ResizeObserver` chứ không chỉ xét ở sự kiện `playing`: lúc `playing` trang **vẫn còn cao
bình thường**, một lúc sau mới bị ghi chiều cao sai.

### 2.10 Bẫy đã gặp khi build (đã ghi vào catalog chung)
Xem [`integration/PLAYBOOK_TICH_HOP.md`](integration/PLAYBOOK_TICH_HOP.md) §C lỗi 8–11:
heap Gradle cho R8, tham số DAO trùng từ khoá Java, WebView xoá nội dung Compose, theme `DayNight`
gây nền đen.

## 3. Việc còn phải làm trước khi phát hành

| # | Việc | Vì sao chưa làm được ở đây |
|---|---|---|
| 1 | `app/google-services.json` | Đang là bản của app cũ, chỉ đổi `package_name` cho khớp `applicationId`. Cần tải bản THẬT sau khi thêm app `com.video.smartdownload.downloadervideo` vào Firebase Console — thiếu thì Crashlytics/Remote Config/FCM không hoạt động |
| 2 | `admob_app_id` + `native_permission` + khoá IAP/AppsFlyer/license | 24 unit id ads đã là **id thật**. Còn thiếu: `admob_app_id` (giá trị được cấp sai định dạng — xem ghi chú dưới bảng), `native_permission` (chưa được cấp), và khoá IAP/AppsFlyer/license ở `res/values/sdk_strings.xml` |
| 3 | Ảnh Intro (`intro1..4`) | Chữ đã viết lại cho Video Downloader; **ảnh vẫn là của app QR Scanner**, cần bộ ảnh mới |
| 4 | Biểu tượng app (`icon_app.png`) | Vẫn là icon app cũ |
| 5 | Remote `build_tool` | `git remote -v` chưa có → chưa chạy được `./push.sh`. Xem `integration/PLAYBOOK_TICH_HOP.md` §E.2 |
| 6 | Chạy thử tính năng tải trên bản **release** | R8 chỉ lộ lỗi lúc chạy; đặc biệt Room + Media3 vừa thêm keep-rule mới |

> ⚠️ **`admob_app_id` phải dùng dấu `~`, không phải `/`.** App ID của AdMob có dạng
> `ca-app-pub-XXXXXXXX~YYYYYYYY`; dạng có dấu `/` là **ad unit id**, không phải app id. Giá trị này đi
> vào `AndroidManifest.xml` dưới `com.google.android.gms.ads.APPLICATION_ID`, và SDK Google Mobile Ads
> **kiểm tra ngay lúc khởi động** — sai định dạng là app chết ngay khi mở, chứ không phải chỉ mất ads.
> Vì vậy đang cố ý để nguyên App ID test của Google cho tới khi có giá trị dạng `~`.

> Không còn mục `signingConfig release`: quy trình build của dự án tự ký, app không cần khai keystore.
