# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.grimsley.claudefriends.data.model.**$$serializer { *; }
-keepclassmembers class com.grimsley.claudefriends.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.grimsley.claudefriends.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
