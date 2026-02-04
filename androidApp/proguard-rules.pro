# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in $ANDROID_HOME/tools/proguard/proguard-android.txt

# Keep Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items)
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Keep Ktor classes
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }

# Keep VwaTek models
-keep class com.vwatek.apply.domain.model.** { *; }

# Keep ViewModels
-keep class com.vwatek.apply.presentation.** { *; }

# Koin
-keepclassmembers class * { public <init>(...); }
