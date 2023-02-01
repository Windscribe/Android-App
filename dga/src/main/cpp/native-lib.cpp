#include <jni.h>
#include <string>
#include <dlfcn.h>
#include <jni.h>

extern "C" JNIEXPORT jstring JNICALL
Java_com_windscribe_dga_Dga_getDomain(JNIEnv *env, jobject jb, jobject ctx) {
    typedef void (*FuncPrototype)(char *, int, void *, void *, void *);
    std::string retStr;

    void *handle = dlopen("libdga.so", RTLD_LAZY);
    if (handle) {
        auto func = (FuncPrototype) dlsym(handle, "d1");
        if (!func) {
            retStr = "Function loading error";
        } else {
            char domain[1024] = {0};
            func(domain, 1024, env, jb, ctx);
            retStr = domain;
        }
        dlclose(handle);
    } else {
        retStr = "Library not loaded!";
    }
    return env->NewStringUTF(retStr.c_str());
}

