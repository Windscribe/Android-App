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
        int logLevel, struct go_string logPath);

extern int StopCd(bool restart, int pin);

extern bool IsCdRunning();

JNIEXPORT void JNICALL
Java_com_windscribe_vpn_backend_CdLib_startCd(JNIEnv *env, jobject thiz, jstring cuid,
                                              jstring homeDir, jstring proto, jstring logPath) {

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

    StartCd(cuid_go_str, homeDir_go_str, proto_go_str, 3, logPath_go_str);

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

JavaVM *jvm;
jclass wsLibClass;
jobject wsLibInstance;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    jvm = vm;
    JNIEnv *env;
    if ((*jvm)->AttachCurrentThread(jvm, (JNIEnv **) &env, NULL) != JNI_OK) {
        return JNI_ERR;
    }
    jclass cls = (*env)->FindClass(env, "com/windscribe/vpn/backend/CdLib");
    if (cls == NULL) {
        return JNI_ERR;
    }

    wsLibClass = (jclass) (*env)->NewGlobalRef(env, cls);
    if (wsLibClass == NULL) {
        return JNI_ERR;
    }
    jmethodID constructor = (*env)->GetMethodID(env, wsLibClass, "<init>", "()V");
    if (constructor == NULL) {
        return JNI_ERR;
    }

    jobject instance = (*env)->NewObject(env, wsLibClass, constructor);
    if (instance == NULL) {
        return JNI_ERR;
    }

    wsLibInstance = (*env)->NewGlobalRef(env, instance);
    (*env)->DeleteLocalRef(env, instance);
    (*env)->DeleteLocalRef(env, cls);

    return JNI_VERSION_1_6;
}

const char *getMetaData(const char *key) {
    JNIEnv *env;
    if ((*jvm)->AttachCurrentThread(jvm, (JNIEnv **) &env, NULL) != JNI_OK) {
        return "";
    }
    if (wsLibInstance == NULL) {
        (*jvm)->DetachCurrentThread(jvm);
        return "";
    }
    jmethodID mid = (*env)->GetMethodID(env, wsLibClass, key, "()Ljava/lang/String;");
    if (mid == NULL) {
        (*jvm)->DetachCurrentThread(jvm);
        return "";
    }
    jstring jresult = (jstring) (*env)->CallObjectMethod(env, wsLibInstance, mid);

    if (jresult == NULL) {
        (*jvm)->DetachCurrentThread(jvm);
        return "";
    }
    const char *c_str = (*env)->GetStringUTFChars(env, jresult, NULL);
    if (c_str == NULL) {
        (*env)->DeleteLocalRef(env, jresult);
        (*jvm)->DetachCurrentThread(jvm);
        return "";
    }
    char *result = strdup(c_str);
    (*env)->ReleaseStringUTFChars(env, jresult, c_str);
    (*env)->DeleteLocalRef(env, jresult);
    (*jvm)->DetachCurrentThread(jvm);

    return result;
}