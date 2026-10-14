/*
 * Copyright (c) 2026 Windscribe Limited.
 */

#include <errno.h>
#include <fcntl.h>
#include <jni.h>

/*
 * android.system.Os.fcntlInt is public SDK only from API 30; below that the hidden-API check
 * denies the call outright, so the descriptor flags have to be set through libc.
 *
 * Returns errno, or 0 on success.
 */
JNIEXPORT jint JNICALL
Java_com_windscribe_common_tunnel_PacketMux_nativeSetNonBlocking(JNIEnv *env, jclass clazz, jint fd) {
    (void) env;
    (void) clazz;

    int flags = fcntl(fd, F_GETFL, 0);
    if (flags == -1) {
        return errno;
    }
    if (fcntl(fd, F_SETFL, flags | O_NONBLOCK) == -1) {
        return errno;
    }
    return 0;
}
