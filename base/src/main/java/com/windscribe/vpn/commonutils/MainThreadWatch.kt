/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.vpn.commonutils

import android.os.Looper
import android.util.Log
import com.windscribe.vpn.BuildConfig

/**
 * Diagnostic for "the UI froze for a second" reports.
 *
 * StrictMode cannot see these: `setupStrictMode()` permits disk reads and writes, and every getter
 * on `DataStorePreferenceHelper` is `runBlocking { }` over DataStore, which is a disk read.
 *
 * Log-only and rate-limited, so it never changes the path it measures. Logcat rather than the app
 * log, which is what users attach to a bug report.
 */
object MainThreadWatch {
    /** Distinct call sites already reported, so a hot path logs once rather than thousands of times. */
    private val reported = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    /**
     * Logs a stack trace if called on the main thread.
     *
     * @param what a short label for the blocking operation, e.g. the preference key being read.
     */
    fun warnIfMainThread(what: String) {
        // Debug builds only. Gated here rather than at each of the call sites in
        // DataStorePreferenceHelper so they stay readable, and so a release build pays nothing
        // beyond a constant-folded branch R8 can remove.
        if (!BuildConfig.DEBUG) {
            return
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            return
        }
        // Skip this frame and the immediate caller to land on the interesting one.
        val stack = Throwable().stackTrace
        val site =
            stack
                .drop(1)
                .firstOrNull { !it.className.contains("MainThreadWatch") }
                ?.let { "${it.className}.${it.methodName}:${it.lineNumber}" }
                ?: "unknown"
        if (!reported.add(site)) {
            return
        }
        Log.w(
            "main-thread-watch",
            "BLOCKING ON MAIN THREAD: $what from $site\n" +
                stack.drop(1).take(14).joinToString("\n") { "    at $it" },
        )
    }
}
