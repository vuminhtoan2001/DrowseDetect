# 11 — Pipeline `firebase_key.json` (gộp + mã hoá key Remote Config)

> Mục tiêu: tạo **1 file** `firebase_key.json` để upload thủ công lên Firebase Remote Config, gồm **đủ** key của:
> 1. **RN cũ** (đã mã hoá kiểu RN) — giữ để bản RN cũ ngoài thị trường vẫn chạy.
> 2. **Kotlin mới** (key cấu hình của app Kotlin) — có ghi chú `[KOTLIN] <tên thật>`.
> 3. **Lib** (`base-application`) — gồm cả key `ads_config` = nội dung `default_ads_config.json`.

## 1. Vì sao phức tạp: 2 hệ mã hoá song song

- **RN** có cơ chế encode riêng (đổi tên key → hash) qua `keychange.json`. File export Remote Config của RN (`firebase_old_key.json`) chứa key **đã hash** + `description` = tên thật.
- **Kotlin** có encode riêng (`encode-scripts/`). Key cấu hình app Kotlin nằm ở `res/xml/config.xml`.

→ Phải hoà 2 hệ vào 1 file, **không trùng key**, và mỗi phần mã hoá đúng theo hệ của nó.

## 2. Các file nguồn (đặt ở root repo, đều **gitignore**)

| File | Là gì |
|---|---|
| `keychange.json` | Map RN `{tên thật: hash}` (export từ auto-rename RN) |
| `firebase_old_key.json` | Export Remote Config RN (parameterGroups, mỗi param có `description`=tên thật) |
| `fire_base_key_lib_arr.json` | Danh sách param của lib (interval_auto_open_app, type_of_start_up, …) |
| `app/src/main/assets/default_ads_config.json` | Ad unit thật `{placement: unit}` → nội dung cho key `ads_config` |
| `app/src/main/res/xml/config.xml` | Key cấu hình app Kotlin (enable/ratio/max, count_app_open, …) |

## 3. `encode-scripts/firebase-merge.js` — gộp

Chạy **sau** bước encode trong `push.sh`. Sản phẩm: `firebase_key.json` = union (không trùng) của:

1. **`collectParams(old, dropLogCustom=true)`** — đọc `firebase_old_key.json`, **bỏ**:
   - nhóm `log_item_ad` + param có `description` chứa "hiển thị log".
   - param custom-text (`description` chứa "custom text"/"customtext").
2. **lib** — toàn bộ `fire_base_key_lib_arr.json`.
3. **`buildKotlinParams()`** — duyệt **mọi entry** trong `config.xml`:
   - key = hash nếu có trong `keychangeConfigFirebase.json`, **else giữ readable** (xem §4).
   - `description = "[KOTLIN] <tên thật>"` (ghi chú mục 2).
   - `valueType` suy ra từ giá trị (BOOLEAN/NUMBER/JSON/STRING).
4. **`ads_config`** — inject thêm:
   ```js
   merged["ads_config"] = {
     defaultValue: { value: JSON.stringify(default_ads_config.json) },
     description: "[LIB] ads_config — ad unit ids",
     valueType: "JSON",
   };
   ```
   > Lib mặc định `ads_config = {useInAppDefault:true}`; ta **thay** bằng nội dung `default_ads_config.json` để Remote Config có unit thật.

Kết quả mẫu (vga31a): **126 params** = 36 KOTLIN (readable) + 43 lib + ~47 RN (đã lọc) + `ads_config`.

## 4. ⚠️ QUY TẮC SỐNG CÒN: KHÔNG hash config key của app Kotlin

Bản RN encode được cả key vì code RN truy cập key bằng **tên đã hash trực tiếp** (`dataRemoteConfig.g6da18b68`).
Code Kotlin thường đọc key **động** bằng ghép chuỗi:

```kotlin
remote.getBoolean("${placement}_enable")   // "native_home_enable"
remote.getInt("${interPlacement}_ratio")
```

→ Chuỗi literal `"native_home_enable"` **không xuất hiện** trong source (được ghép lúc chạy) → `rename-config-key` **không thể** đổi tham chiếu trong code, nhưng **vẫn** hash key trong `config.xml`.
Hậu quả: bản **release** có `config.xml` key đã hash, còn code hỏi key readable → **lệch → đọc default → tắt hết ads/sai cấu hình**. Debug không lộ (đọc readable).

**Cách xử lý (đã chốt):**
- **Bỏ `rename-config-key.js` khỏi `ENCODE_SCRIPTS` trong `push.sh`.** Config key giữ **readable** ở cả `config.xml` + code + `firebase_key.json` → release đọc đúng.
- `buildKotlinParams()` do đó dùng key readable (map hash rỗng). Vẫn có ghi chú `[KOTLIN]`.
- Đánh đổi: key cấu hình app không bị obfuscate. Phần nhạy cảm (ad unit) vẫn nằm trong `ads_config`. Nếu **bắt buộc** hash, phải refactor toàn bộ truy cập config sang **literal** (vd `when(placement){"native_home"->"native_home_enable"...}`) để encode đổi đồng bộ — tốn công, chưa làm.

## 5. Vị trí trong `push.sh`

- `FIREBASE_SCRIPTS=(firebase-merge.js)` — chạy ở **bước 3.5**, ngay sau encode, trước `assembleRelease`.
- **KHÔNG** thêm `rename-config-global.js` vào `FIREBASE_SCRIPTS` (nó đã ở root nhờ `TEMP_FILES`; thêm lại sẽ bị `rm` sau merge → `reverse.js require('./rename-config-global')` chết).
- `firebase_key.json` được **gitignore** + không push lên remote nào (chỉ để upload tay).

## 6. Quy trình chạy

```bash
# firebase_key.json tạo tự động trong bước build_tool của push.sh:
./push.sh build_tool     # encode → firebase-merge → assembleRelease → (push)
# → lấy firebase_key.json ở root, upload thủ công lên Firebase Console → Remote Config → Import.
```

## 7. Checklist

- [ ] `firebase_key.json` có `ads_config` = nội dung `default_ads_config.json` (không phải `{useInAppDefault:true}`).
- [ ] Có đủ 3 nhóm: RN (đã lọc log/customtext), lib, KOTLIN (ghi chú `[KOTLIN]`).
- [ ] Không key trùng.
- [ ] Config key **readable** (không hash) → khớp cách code đọc động.
- [ ] Đã bỏ key log + customtext của RN.
