#include <jni.h>
#include <string.h>
#include <stdbool.h>
#include <android/log.h>

struct go_string {
    const char *str;
    long n;
};

extern void
StartCd(struct go_string cdUID, struct go_string homeDir, struct go_string upstreamProto,
        int logLevel, struct go_string logPath, struct go_string hostName,
        struct go_string lanIp, struct go_string macAddress);

extern int StopCd(bool restart, int pin);

extern bool IsCdRunning();

JNIEXPORT void JNICALL
Java_com_windscribe_vpn_backend_CdLib_startCd(JNIEnv *env, jobject thiz, jstring cuid,
                                              jstring homeDir, jstring proto, jstring logPath,
                                              jstring hostName, jstring lanIp, jstring macAddress) {

    const char *proto_str = (*env)->GetStringUTFChars(env, proto, 0);
    int const proto_size = (*env)->GetStringUTFLength(env, proto);
    struct go_string proto_go_str = {
            .str = proto_str,
            .n = proto_size
    };

    const char *cuid_str = (*env)->GetStringUTFChars(env, cuid, 0);
    int const cuid_size = (*env)->GetStringUTFLength(env, cuid);
    struct go_string cuid_go_str = {
            .str = cuid_str,
            .n = cuid_size
    };

    const char *homeDir_str = (*env)->GetStringUTFChars(env, homeDir, 0);
    int const homeDir_size = (*env)->GetStringUTFLength(env, homeDir);
    struct go_string homeDir_go_str = {
            .str = homeDir_str,
            .n = homeDir_size
    };

    const char *logPath_str = (*env)->GetStringUTFChars(env, logPath, 0);
    int const logPath_size = (*env)->GetStringUTFLength(env, logPath);
    struct go_string logPath_go_str = {
            .str = logPath_str,
            .n = logPath_size
    };

    const char *hostName_str = (*env)->GetStringUTFChars(env, hostName, 0);
    int const hostName_size = (*env)->GetStringUTFLength(env, hostName);
    struct go_string hostName_go_str = {
            .str = hostName_str,
            .n = hostName_size
    };

    const char *lanIp_str = (*env)->GetStringUTFChars(env, lanIp, 0);
    int const lanIp_size = (*env)->GetStringUTFLength(env, lanIp);
    struct go_string lanIp_go_str = {
            .str = lanIp_str,
            .n = lanIp_size
    };

    const char *macAddress_str = (*env)->GetStringUTFChars(env, macAddress, 0);
    int const macAddress_size = (*env)->GetStringUTFLength(env, macAddress);
    struct go_string macAddress_go_str = {
            .str = macAddress_str,
            .n = macAddress_size
    };

    StartCd(cuid_go_str, homeDir_go_str, proto_go_str, 3, logPath_go_str,
            hostName_go_str, lanIp_go_str, macAddress_go_str);

    (*env)->ReleaseStringUTFChars(env, macAddress, macAddress_str);
    (*env)->ReleaseStringUTFChars(env, lanIp, lanIp_str);
    (*env)->ReleaseStringUTFChars(env, hostName, hostName_str);
    (*env)->ReleaseStringUTFChars(env, logPath, logPath_str);
    (*env)->ReleaseStringUTFChars(env, homeDir, homeDir_str);
    (*env)->ReleaseStringUTFChars(env, cuid, cuid_str);
    (*env)->ReleaseStringUTFChars(env, proto, proto_str);
}

JNIEXPORT int JNICALL
Java_com_windscribe_vpn_backend_CdLib_stopCd(JNIEnv *env, jobject thiz, jboolean isRestart,
                                             jint pin) {
    return StopCd(isRestart, pin);
}

JNIEXPORT jboolean JNICALL
Java_com_windscribe_vpn_backend_CdLib_isCdRunning(JNIEnv *env, jobject thiz) {
    return IsCdRunning();
}