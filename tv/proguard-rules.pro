# Keep source file names and line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Retrofit
-dontnote retrofit2.Platform
-dontwarn retrofit2.Platform,Java8
-keepattributes Signature
-keepattributes Exceptions
-dontwarn okio.**

# RxJava2 Call Adapter Factory for Retrofit
-keep class retrofit2.adapter.rxjava2.** { *; }
-keep class io.reactivex.** { *; }
-dontwarn io.reactivex.**

# Dagger
-dontwarn com.google.errorprone.annotations.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.examples.android.model.** { <fields>; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# Amazon payments
-dontwarn com.amazon.**
-keep class com.amazon.** {*;}
-keepattributes *Annotation*

# Okhttp
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.*
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE

#Lifecycle
-keep class androidx.lifecycle.** {*;}

# Fire OS (API 22) compatibility - Prevent VerifyError with AutofillManager
# AutofillManager was added in API 26 but Fire OS runs on API 22
-dontwarn android.view.autofill.**
-dontwarn android.service.autofill.**

# Keep entire AppCompat library - prevent R8 from obfuscating/optimizing
-keep class androidx.appcompat.** { *; }
-keepclassmembers class androidx.appcompat.** { *; }

# Keep Material Design components
-keep class com.google.android.material.** { *; }
-keepclassmembers class com.google.android.material.** { *; }

# inet.ipaddr and the :base reflection keeps now travel with the module, in base/consumer-rules.pro.
