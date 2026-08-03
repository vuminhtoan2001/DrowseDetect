# Công thức proguard cho SDK phổ biến (thêm vào `proguard-rules.pro` RIÊNG app khi cần)

> File này tách riêng khỏi `ENCODE_R8_STANDARD.md` để khi phát hiện SDK mới cần rule, hoặc rule
> có SDK nào đó cần cập nhật, chỉ sửa **file này** — không đụng vào `ENCODE_R8_STANDARD.md`.

`proguard-rules-base.pro` chỉ chứa rule THỰC SỰ dùng chung (AAR core, Billing, AdMob mediation, Gson,
Crashlytics). SDK dưới đây **tuỳ app có dùng hay không** — copy đúng khối cần vào `proguard-rules.pro`
riêng của app, đừng copy thừa (dễ tạo cruft: rule cho SDK app không dùng, nằm vô hại nhưng gây khó đọc).

```proguard
# --- CameraX / Media3 / Room (app quay/chụp, lưu overlay/lịch sử) ---
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.**

# --- Google Maps ---
-keep class com.google.android.gms.maps.** { *; }
-keep class com.google.maps.android.** { *; }
-dontwarn com.google.android.gms.maps.**

# --- Retrofit (chỉ thêm nếu app dùng Retrofit, KHÔNG mặc định) ---
-keepattributes Exceptions, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault
-dontwarn retrofit2.**
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowoptimization,allowobfuscation,allowshrinking interface <1>

# --- jsoup (chỉ thêm nếu app gọi trực tiếp — jsoup thường chỉ là dependency gián tiếp của
#     base-application AAR, không cần khai lại nếu app không tự import) ---
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# --- WebRTC (app video-call/P2P) ---
-keep class org.webrtc.** { *; }
-keepclassmembers class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# --- libpag / native JNI theo tên lớp cứng (app dùng animation Tencent libpag) ---
-keep class org.libpag.** { *; }
-keep class org.extra.tools.** { *; }
-keep class org.ffavc.** { *; }
-keep class com.tencent.tav.** { *; }
-keep class androidx.exifinterface.** { *; }
-keepclasseswithmembernames class * { native <methods>; }

# --- OpenCV (app xử lý ảnh bằng JNI, vd Scan PDF phát hiện góc tài liệu) ---
-keep class org.opencv.** { *; }
-dontwarn org.opencv.**
-keepclasseswithmembernames class * { native <methods>; }
```

**Quy tắc chung khi thêm SDK mới không có trong danh sách trên**: nếu SDK dùng JNI/native method
hoặc bị nạp bằng reflection (`Class.forName`), luôn cần `-keep` cho package gốc của nó +
`-keepclasseswithmembernames class * { native <methods>; }` (nếu có JNI) — xem
`ENCODE_R8_STANDARD.md` §3.9 để hiểu vì sao (bug loại này chỉ lộ ở bản R8 thật, không lộ lúc code/
build debug).
