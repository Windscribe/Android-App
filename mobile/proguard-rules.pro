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
#Common module dependecies.
-keep class com.scapix.** { *; }
-keep class com.wsnet.** { *; }
-keep class com.windscribe.vpn.backend.CdLib { *; }
# Keep classes from pcap4j-core
-keep class org.pcap4j.** { *; }
# Keep classes from pcap4j-packetfactory-static
-keep class org.pcap4j.packet.factory.** { *; }
# Keep classes from minidns-client
-keep class org.minidns.** { *; }
-keep class com.windscribe.vpn.commonutils.LowerCaseLevelConverter { *; }

# Keep Ext.result function and CallResult classes for reflection
-keep class com.windscribe.vpn.commonutils.Ext { *; }
-keep class com.windscribe.vpn.repository.CallResult { *; }
-keep class com.windscribe.vpn.repository.CallResult$* { *; }
-keep class com.windscribe.vpn.api.response.GenericResponseClass { *; }
-keep class com.windscribe.vpn.serverlist.entity.** { *; }
-keep class com.windscribe.vpn.api.response.** { *; }
