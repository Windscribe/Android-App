#include <jni.h>
#include <string.h>
#include <stdbool.h>
struct go_string { const char *str; long n; };

/* Go's `int` is pointer-width. cgo's real prototypes are not visible here, so
 * these declarations must match GoInt by hand or the callee reads a register the
 * caller never set. */
#if defined(__LP64__) || defined(_LP64)
typedef long long go_int;
#else
typedef int go_int;
#endif
extern void Initialise(bool development, struct go_string logPath);
extern bool StartProxy(struct go_string listenAddress, struct go_string remoteAddress, go_int tunnelType, go_int mtu, bool extraPadding, struct go_string tlsServerName);
extern go_int GetPrimaryListenerSocketFd();
extern void Stop();

/* The Go dialer calls ws_protect_socket between socket() and connect(), from a thread the JVM
 * has never seen. FindClass resolves app classes only through the caller's class loader, and a
 * Go thread has none, so the class and method are looked up here — on the loading thread — and
 * kept as a global ref. */
static JavaVM *ws_vm;
static jclass ws_class;
static jmethodID ws_protect_mid;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    ws_vm = vm;
    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6) != JNI_OK)
        return JNI_VERSION_1_6;
    jclass local = (*env)->FindClass(env, "com/windscribe/vpn/backend/openvpn/WSTunnelLib");
    if (local) {
        ws_class = (*env)->NewGlobalRef(env, local);
        (*env)->DeleteLocalRef(env, local);
        ws_protect_mid = (*env)->GetStaticMethodID(env, ws_class, "protectSocket", "(I)Z");
    }
    /* WireGuard shares this library and does not need any of the above, so a failed lookup must
     * not fail the load. */
    if ((*env)->ExceptionCheck(env))
        (*env)->ExceptionClear(env);
    return JNI_VERSION_1_6;
}

int ws_protect_socket(int fd) {
    JNIEnv *env;
    jboolean protectedOk;
    int attached = 0;

    if (!ws_vm || !ws_class || !ws_protect_mid)
        return 0;
    if ((*ws_vm)->GetEnv(ws_vm, (void **)&env, JNI_VERSION_1_6) != JNI_OK) {
        if ((*ws_vm)->AttachCurrentThread(ws_vm, &env, NULL) != JNI_OK)
            return 0;
        attached = 1;
    }
    protectedOk = (*env)->CallStaticBooleanMethod(env, ws_class, ws_protect_mid, (jint)fd);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
        protectedOk = JNI_FALSE;
    }
    if (attached)
        (*ws_vm)->DetachCurrentThread(ws_vm);
    return protectedOk == JNI_TRUE;
}

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
    return (jint)GetPrimaryListenerSocketFd();
}

JNIEXPORT void JNICALL
Java_com_windscribe_vpn_backend_openvpn_WSTunnelLib_startProxy(JNIEnv* env, jobject thiz, jstring listenAddress, jstring remoteAddress, jint tunnelType, jlong mtu, jboolean extraPadding, jstring tlsServerName) {
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
    const char *tls_server_name_str = (*env)->GetStringUTFChars(env, tlsServerName, 0);
    int const tls_server_name_size = (*env)->GetStringUTFLength(env, tlsServerName);
    struct go_string sni_go_str =  {
            .str = tls_server_name_str,
            .n = tls_server_name_size
    };
    StartProxy(listen_go_str, remote_go_str, (go_int)tunnelType, (go_int)mtu, extraPadding, sni_go_str);
    (*env)->ReleaseStringUTFChars(env, listenAddress, listen_str);
    (*env)->ReleaseStringUTFChars(env, remoteAddress, remote_str);
    (*env)->ReleaseStringUTFChars(env, tlsServerName, tls_server_name_str);
}