#include <jni.h>
#include <string.h>
#include <stdbool.h>
struct go_string { const char *str; long n; };
extern void Initialise(bool development, struct go_string logPath);
extern bool StartProxy(struct go_string listenAddress, struct go_string remoteAddress, int tunnelType, long mtu , bool extraPadding);
extern int GetPrimaryListenerSocketFd();
extern void Stop();

JNIEXPORT void JNICALL
Java_com_windscribe_vpn_backend_openvpn_WSTunnelLib_initialise(JNIEnv* env, jobject thiz, jboolean development, jstring logPath) {
    const char *logPath_str = (*env)->GetStringUTFChars(env, logPath, 0);
    int const size = (*env)->GetStringUTFLength(env, logPath);
    struct go_string logPath_go_str =  {
            .str = logPath_str,
            .n = size
    };
    Initialise(development, logPath_go_str);
    (*env)->ReleaseStringUTFChars(env, logPath, logPath_str);
}

JNIEXPORT void JNICALL
Java_com_windscribe_vpn_backend_openvpn_WSTunnelLib_stop(JNIEnv* env, jobject thiz) {
   Stop();
}

JNIEXPORT int JNICALL
Java_com_windscribe_vpn_backend_openvpn_WSTunnelLib_socketFd(JNIEnv* env, jobject thiz) {
    return GetPrimaryListenerSocketFd();
}

JNIEXPORT void JNICALL
Java_com_windscribe_vpn_backend_openvpn_WSTunnelLib_startProxy(JNIEnv* env, jobject thiz, jstring listenAddress, jstring remoteAddress, jint tunnelType, jlong mtu, jboolean extraPadding) {
    const char *listen_str = (*env)->GetStringUTFChars(env, listenAddress, 0);
    int const listen_size = (*env)->GetStringUTFLength(env, listenAddress);
    struct go_string listen_go_str =  {
            .str = listen_str,
            .n = listen_size
    };
    const char *remote_str = (*env)->GetStringUTFChars(env, remoteAddress, 0);
    int const remote_size = (*env)->GetStringUTFLength(env, remoteAddress);
    struct go_string remote_go_str =  {
            .str = remote_str,
            .n = remote_size
    };
    StartProxy(listen_go_str, remote_go_str, tunnelType, mtu, extraPadding);
    (*env)->ReleaseStringUTFChars(env, listenAddress, listen_str);
    (*env)->ReleaseStringUTFChars(env, remoteAddress, remote_str);
}