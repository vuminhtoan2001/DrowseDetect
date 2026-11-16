-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature, InnerClasses, EnclosingMethod

# MediaPipe Tasks (JNI + protobuf)
-keep class com.google.mediapipe.** { *; }
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.mediapipe.proto.**

# R8: các lớp chỉ có ở compile-time (annotation processor) mà MediaPipe tham chiếu
-dontwarn javax.lang.model.**
-dontwarn javax.annotation.**
-dontwarn com.google.auto.value.**
-dontwarn autovalue.shaded.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn org.checkerframework.**
-dontwarn com.google.mediapipe.**
