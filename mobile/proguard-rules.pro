# Retrofit
-dontnote retrofit2.Platform
-dontwarn retrofit2.Platform,Java8
-keepattributes Signature
-keepattributes Exceptions
-dontwarn okio.**

# Butter-knife
-keep public class * implements butterknife.Unbinder { public <init>(**, android.view.View); }
-keep class butterknife.*
-keepclasseswithmembernames class * { @butterknife.* <methods>; }
-keepclasseswithmembernames class * { @butterknife.* <fields>; }

# Dagger
-dontwarn com.google.errorprone.annotations.**

# Strongswan
-keep public class org.strongswan.android.** {
  public protected private *;
}

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

#Logger
-keep class ch.qos.** { *; }
-keep class ch.qos.logback.classic.db.SQLBuilder
-keep class org.slf4j.** { *; }
-keepattributes *Annotation*
-dontwarn ch.qos.logback.core.net.*

#Lifecycle
-keep class androidx.lifecycle.** {*;}
