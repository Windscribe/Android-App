# Consumed by anything that embeds this module, including the fused windscribe-core AAR. They
# belong here rather than in an app's own file: the app modules are not fused, so a partner build
# shrinking this code would otherwise have no rules for it at all.
#
# Classes this module owns carry @Keep, which androidx.annotation's own rules act on. What is left
# here is what an annotation cannot express: third-party code, attribute directives, and the two
# gson model packages that would need 58 of them.

# wsnet and its scapix bindings are reached from native code.
-keep class com.wsnet.** { *; }
-keep class com.scapix.** { *; }

# logback and slf4j: assets/logback.xml names appenders, encoders and policies by string, so
# nothing in the bytecode refers to them. The converters this module owns carry @Keep instead.
-keep class ch.qos.** { *; }
-keep class ch.qos.logback.classic.db.SQLBuilder
-keep class org.slf4j.** { *; }
-dontwarn ch.qos.logback.core.net.*

# IPAddress (inet.ipaddr) — its static initializer builds a ResourceBundle name via
# HostIdentifierException.class.getPackage().getName(). R8's repackaging/flattening can
# move these classes to the root package, making getPackage() return null -> NPE in
# <clinit> (ExceptionInInitializerError) on release builds only. Keep the package intact
# and preserve the IPAddressResources bundle so the lookup succeeds.
# This module owns the only use of it (Util.validIpAddress, on every connect).
-keep class inet.ipaddr.** { *; }
-keeppackagenames inet.ipaddr.**
-keepclassmembers class inet.ipaddr.** { *; }

# gson is `api` here and 59 files map API responses with it, 48 of them by @SerializedName.
# Signature carries the generic types `TypeToken` and `GenericResponseClass<T>` resolve through.
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers class * extends com.google.gson.reflect.TypeToken {
    <fields>;
    <methods>;
}

# gson maps these two packages whole.
-keep class com.windscribe.vpn.serverlist.entity.** { *; }
-keep class com.windscribe.vpn.api.response.** { *; }
