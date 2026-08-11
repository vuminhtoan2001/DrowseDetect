# Standard Android rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# AAR base library classes
-keep class com.brian.** { *; }
-keep class com.nlbn.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}

# Room — thêm lại khi app thật dùng Room (khung sườn hiện không có Entity/Dao/Database nào).

# Gson serialization
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }

# --- Fix: "TypeToken must be created with a type argument" (Gson + R8) ---
# Giữ generic signature + thông tin inner/anonymous class để TypeToken<...>(){} đọc được type.
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
# Giữ TypeToken và MỌI lớp con ẩn danh của nó (không bị R8 gộp/strip → còn type argument).
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken
# Field map theo @SerializedName vẫn giữ tên khi class bị obfuscate.
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Retrofit — thêm lại `-keep interface <package model app>.** { *; }` khi app thật dùng Retrofit.
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Kotlin serialization
-keepattributes RuntimeVisibleAnnotations
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# ExoPlayer / Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { *; }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# AdMob mediation adapters — nạp bằng reflection (Class.forName), R8 không thấy caller
# → phải giữ, nếu không sẽ ClassNotFoundException khi GMA khởi tạo adapter.
-keep class com.google.ads.mediation.** { *; }

# Facebook SDK / Meta Audience Network (mediation)
-keep class com.facebook.** { *; }
-keep class com.facebook.ads.** { *; }
-dontwarn com.facebook.**
-dontwarn com.facebook.ads.internal.**

# Cảnh báo lành tính (chỉ cần khi build fail vì nó)
-dontwarn de.hdodenhof.circleimageview.**

# AppsFlyer
-keep class com.appsflyer.** { *; }
-dontwarn com.appsflyer.**

# Adjust
-keep class com.adjust.sdk.** { *; }
-dontwarn com.adjust.sdk.**

# Lottie
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# Billing
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# General
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
