# ============================================================================
# proguard-rules-base.pro — LÕI DÙNG CHUNG cho mọi app tích hợp base-application AAR.
#
# File này KHÔNG sửa khi clone sang app mới — copy nguyên. Rule đặc thù của TỪNG app (SDK riêng như
# CameraX/Media3/Room/WebRTC/Retrofit/jsoup/libpag/OpenCV...) khai ở `proguard-rules.pro` (file cạnh
# file này), KHÔNG khai ở đây. Xem quy trình đầy đủ + rule "công thức" cho SDK phổ biến ở
# docs/integration/16_ENCODE_R8_STANDARD.md.
#
# Rule ở đây được chọn vì THỰC SỰ dùng chung cho MỌI app trong dòng này (khảo sát 16 app, xem tài
# liệu trên) — không phải "thêm cho chắc" kiểu paranoia (đã bỏ hẳn rule jsoup/Retrofit generic ở bản
# base vì phần lớn app KHÔNG dùng trực tiếp — xem "công thức" riêng trong docs nếu app nào cần).
# ============================================================================

# --- Google Play Billing — mọi app dùng base-application AAR đều tích hợp IAP qua lib này ---
-keep class com.android.billingclient.api.** { *; }
-keep class com.android.billingclient.** { *; }
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- base-application AAR (đã có consumer rules trong AAR — giữ thêm cho chắc, vô hại nếu dư) ---
-keep class com.nlbn.** { *; }
-keep class com.brian.** { *; }
-keep class com.google.android.gms.ads.nativead.** { *; }

# --- AdMob mediation (Meta/Facebook Audience Network) — SDK bên thứ 3, KHÔNG nằm trong AAR, nạp
#     bằng reflection nên R8 không tự thấy ai gọi → phải tự giữ ---
-keep class com.google.ads.mediation.** { *; }
-keep class com.facebook.ads.** { *; }
-dontwarn com.facebook.ads.**
-dontwarn com.facebook.ads.internal.**

# --- Gson: giữ generic signature + TypeToken + field @SerializedName — hầu hết app đều dùng Gson
#     (catalog asset JSON, model config...) dù không phải qua Retrofit ---
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# --- Crashlytics: giữ stack trace đọc được (chỉ ảnh hưởng deobfuscation, không phải chức năng) ---
-keepattributes SourceFile,LineNumberTable

# --- Cảnh báo lành tính (dependency gián tiếp của base-application AAR) ---
-dontwarn de.hdodenhof.circleimageview.**
