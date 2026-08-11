# Checklist proguard khi thêm SDK mới / build bản có minify

> `proguard-rules-base.pro` đã có rule dùng chung (AAR core, Billing, AdMob mediation, Gson,
> Crashlytics) — KHÔNG cần đụng khi chỉ thêm màn/tính năng mới không kèm SDK mới.

Chỉ cần đọc file này khi: (a) thêm 1 SDK/lib mới vào app, hoặc (b) sắp build bản `assembleRelease`
(minify+shrink) lần đầu cho 1 SDK mới thêm.

## Khi thêm 1 SDK mới — tự hỏi 3 câu

1. **SDK có dùng JNI/native method không?** (file `.so`, hàm `external fun`...) → cần:
   ```proguard
   -keep class <package_goc_cua_sdk>.** { *; }
   -keepclasseswithmembernames class * { native <methods>; }
   ```
2. **SDK có nạp class bằng reflection không?** (`Class.forName`, DI tự chế, parser tự dò annotation...)
   → cần `-keep` cho package gốc + `-dontwarn <package_goc>.**`.
3. **SDK có model serialize (Gson/Retrofit/Room entity) không?** → cần giữ field không bị đổi tên/xoá:
   ```proguard
   -keepattributes Signature, RuntimeVisibleAnnotations, AnnotationDefault
   -keep class <package_model_cua_app>.** { *; }
   ```

Nếu cả 3 câu đều "không" — thường không cần thêm rule (SDK Java thuần, không reflection).

## Vì sao phải check trước build release

R8 chỉ xoá/rename class **lúc build `release` có `isMinifyEnabled = true`** — build `debug` không lộ
lỗi này. Thiếu rule đúng chỗ → crash `ClassNotFoundException`/`NoSuchMethodError` chỉ xảy ra ở bản
release, không tái hiện được lúc debug. Luôn `assembleRelease` + cài máy test thật sau khi thêm SDK
mới, không chỉ tin vào build debug chạy được.

## Không tìm được rule đúng?

Tra theo tên SDK trên trang chính thức của SDK đó (mục "Proguard"/"R8 rules") — hầu hết SDK phổ biến
đã tự publish rule chuẩn qua `consumer-rules.pro` đóng gói sẵn trong AAR của họ (không cần app tự khai
lại) — chỉ cần tự khai thêm khi SDK **không** đóng gói `consumer-rules.pro` (kiểm tra: giải nén AAR,
tìm file này bên trong).
