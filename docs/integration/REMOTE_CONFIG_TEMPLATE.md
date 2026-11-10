# 🧩 Dựng template Firebase Remote Config cho app mới

> **Mục tiêu:** đưa file này cho AI, AI **tự viết ra template Remote Config hoàn chỉnh** để import lên
> Firebase — không cần script.
>
> **Mặc định là TẠO MỚI, không phải gộp.** App clone từ skeleton là **project Firebase mới, Remote Config
> đang rỗng** — không có gì để gộp vào. Chỉ khi bạn dựng lại một app ĐÃ PHÁT HÀNH (đã có template live,
> user cũ đang đọc key cũ) thì mới cần bước gộp ở §5.

---

## 1. Ba nguồn AI cần đọc

| Nguồn | Là gì | Lấy ở đâu |
|---|---|---|
| **RC của SDK** | Key thư viện `base-application` cần: 5 param top-level + 3 group `noti` (21 key) / `ads` (7 key) / `iap` (10 key) | [`remote_config_setup.json`](remote_config_setup.json) — có sẵn trong kit |
| **`default_ads_config.json`** | Ad unit id thật theo placement (tạo ở GĐ4) | `app/src/main/assets/default_ads_config.json` |
| **`HostApplication.setAppDefaults(...)`** | Danh sách key RIÊNG của app + giá trị mặc định | Đọc thẳng trong code app |

Không có nguồn thứ tư. **Đừng đi tìm "file RC cũ"** — app mới không có.

## 2. Quy tắc dựng (BẮT BUỘC)

1. **Chép nguyên xi phần SDK** từ `remote_config_setup.json`: giữ đủ 5 `parameters` top-level và cả 3
   group `noti`, `ads`, `iap`. Không bớt key nào — key thiếu thì SDK đọc default nội bộ, nhưng bạn mất
   khả năng chỉnh từ console.

2. **`ads_config` giữ nguyên vị trí trong group `ads`** — KHÔNG di chuyển, KHÔNG xoá, KHÔNG nhân bản sang
   group khác. **Nhưng thay giá trị**: template SDK để `useInAppDefault: true`, phải đổi thành `value` =
   **chuỗi JSON escape của toàn bộ `default_ads_config.json`**:
   ```json
   "ads_config": {
     "defaultValue": { "value": "{\"open_splash\":\"ca-app-pub-.../...\",\"native_home\":\"...\"}" },
     "valueType": "JSON"
   }
   ```
   App đọc id qua `getAdsConfigValue("<placement>")`; có trên console thì đổi id không cần build lại,
   không có thì fallback về asset trong app.

3. **Gom key riêng của app vào ĐÚNG 1 group tên `new_sdk_ads_<tên app>`** — lấy đúng danh sách trong
   `setAppDefaults(...)`, value = default khai trong đó. Ba loại:
   - **Cờ bật/tắt** `<placement>_enable` (BOOLEAN) — native thuần **chỉ cần key này**; thêm
     `showRatio`/`maxShowPerDay` cho native thường là gây hiểu nhầm là có chặn tần suất (`ADS.md` §1).
   - **Tần suất** `<placement>_showRatio`, `<placement>_maxShowPerDay` (NUMBER) — **chỉ cho inter và
     native-xen-kẽ-sau-inter**, mỗi placement là một bộ đếm độc lập nên phải đủ cả 3 key.
   - **Onboarding/gate**: `positionIntrol` (JSON/STRING), `count_app_open`, `organic_number_not_guide` (NUMBER).

   ⚠️ `maxShowPerDay` để trống = 0 nghĩa là **ads không bao giờ hiện** — đây là lỗi hay gặp nhất, và nó
   không báo gì cả (`ADS.md` §1).

4. **KHÔNG đưa `ads_config` vào group của app.** Key Remote Config là **duy nhất toàn cục** —
   `parameterGroups` chỉ để nhóm hiển thị trên console, không tạo namespace. Trùng key giữa top-level và
   group, hoặc giữa 2 group, đều là lỗi. Rà lại: không key nào xuất hiện 2 lần.

5. **Import thay TOÀN BỘ template**, không phải thêm vào — nên file xuất ra phải đầy đủ, thiếu gì mất nấy.

## 3. Cấu trúc file kết quả

```
{
  "parameters":      { <5 param top-level của SDK> },
  "parameterGroups": {
    "noti": {...SDK, 21 key...},
    "ads":  {...SDK, 7 key; ads_config = nội dung default_ads_config.json (§2.2)...},
    "iap":  {...SDK, 10 key...},
    "new_sdk_ads_<tên app>": { "parameters": { <key trong setAppDefaults, KHÔNG có ads_config> } }
  }
}
```

Không cần khối `version` — Firebase tự sinh khi publish. (Chỉ giữ `version` khi gộp vào template cũ, §5.)

## 4. Cách AI làm (không script)

Đọc 3 nguồn ở §1 → dựng object theo §2–§3 → xuất ra `remote_config_new.json`. Value của `ads_config` là
chuỗi JSON escape copy đúng từ `default_ads_config.json`. Rà key trùng trước khi xuất.

**Đối chiếu cuối cùng — đây là bước hay bị bỏ:** mọi key trong `setAppDefaults(...)` phải có mặt trong
group của app, và mọi `<placement>_enable` của app phải có unit id tương ứng trong `default_ads_config.json`.
Có `_enable` mà thiếu unit id → ads im lặng không hiện, không báo gì.

**Nhưng chiều ngược lại KHÔNG phải lỗi.** Placement của **luồng launcher do SDK quản** chỉ có unit id
trong `ads_config`, app **không** khai `_enable` cho chúng — SDK tự bật/tắt bằng key riêng của nó (group
`ads`/`noti` ở trên). Đo trên vga58a: 25 placement trong `ads_config`, app khai 17 `_enable`, 8 cái còn
lại đều thuộc nhóm này:

```
open_splash   inter_splash   banner_splash   open_all        splash_uninstall
native_language*  native_keep_user   native_survey_user   native_exit_app
```

Thấy 8 cái "thiếu" này thì **đừng thêm key vào `setAppDefaults`** — app không có màn nào gọi chúng, thêm
vào chỉ tạo key chết trên console. Ranh giới: app chỉ khai `_enable` cho placement mà **chính code app**
gọi (xem `ADS.md`), phần launcher `Splash → Inter → Language → IAP` là của SDK.

> `*` **Ngoại lệ `native_language`**: màn Language giờ là màn của app (`LanguageScreen`) nên app TỰ gọi
> `native_language` + `native_language_select` và đã khai `native_language_enable` /
> `native_language_select_enable` trong `HostApplication.setAppDefaults`. Khi dựng template RC phải có
> 2 key `_enable` này (cùng nhóm key riêng của app) và đủ unit id `native_language_select` trong `ads_config`.

## 5. Trường hợp NGOẠI LỆ: app đã phát hành, đã có template live

Chỉ khi đó mới gộp, và quy tắc thêm là:

1. **Giữ NGUYÊN toàn bộ config cũ** — `parameters` top-level + mọi `parameterGroups` cũ + khối `version`.
   **Không xoá key cũ**: user chưa update app vẫn đang đọc chúng; app mới chỉ đơn giản không dùng tới.
2. Thêm phần SDK + group app mới theo §2, đặt cạnh chứ không đè lên.
3. Rà trùng key **giữa cũ và mới** — app cũ (thường là scheme RN, key đã obfuscate, nhóm theo placement)
   có thể đã dùng đúng tên key mà SDK cần. Trùng thì phải xử lý thủ công, không được để 2 bản.

## 6. Import lên Firebase

Console → Remote Config → ⋮ → *Publish changes from file*. Hoặc REST:
```
PUT https://firebaseremoteconfig.googleapis.com/v1/projects/<project-id>/remoteConfig
Header: If-Match: *
```

> ⚠️ Nếu bật rename định danh chống trùng (`renameConfigKey` trong `encode-scripts/rename-config-global.js`)
> làm đổi key Firebase → **dựng template SAU khi rename** để khớp key thật. App này đang **TẮT**
> `renameConfigKey` vì đọc key động bằng ghép chuỗi (`"${placement}_enable"`) — xem
> `ENCODE_R8_STANDARD.md` §3.7 và §4.
