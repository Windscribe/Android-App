# Consumed by anything that embeds this module, including the fused windscribe-core AAR.
# These cover this module's own dependencies, which resolve types reflectively.

# pcap4j builds packet factories by reflection from a properties file.
-keep class org.pcap4j.** { *; }
-keep class org.pcap4j.packet.factory.** { *; }

# minidns resolves record types by class name.
-keep class org.minidns.** { *; }

# PacketMux's O_NONBLOCK helper is resolved by JNI name.
-keepclasseswithmembernames class com.windscribe.common.tunnel.PacketMux {
    native <methods>;
}
