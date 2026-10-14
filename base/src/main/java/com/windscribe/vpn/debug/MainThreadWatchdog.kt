package com.windscribe.vpn.debug

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.windscribe.vpn.BuildConfig

/**
 * Watchdog that monitors the main thread for blocking operations.
 *
 * Detects when the main thread is blocked for more than the threshold duration
 * and crashes the app with a detailed stack trace showing what was blocking.
 *
 * Only detects actual app code blocking (runBlocking, Thread.sleep, sync I/O),
 * ignoring normal Android framework operations.
 */
class MainThreadWatchdog(
    private val thresholdMs: Long = 300,
    private val sampleIntervalMs: Long = 50,
    private val delayStartMs: Long = 2000,
) {
    /**
     * logcat, not the app log. This is a development tool, and the app log is what users attach to a
     * bug report — where a stall trace is noise nobody asked for.
     */
    private val tag = "main-thread-watchdog"
    private var watchdogThread: Thread? = null
    private var isRunning = false

    /** Stops one long stall from producing a report every sample interval. */
    private val quietPeriodMs = 2_000L

    @Volatile
    private var lastReportAt = 0L

    fun start() {
        // Belt and braces: this holds a thread and posts a 16ms ticker to the main looper, so it
        // must not run in a shipped build even if some future call site forgets to guard. Note
        // BuildConfig.DEV is NOT sufficient here - CI builds a signed release with DEV=true.
        if (!BuildConfig.DEBUG) {
            return
        }
        if (isRunning) {
            Log.w(tag, "Watchdog already running")
            return
        }

        isRunning = true
        val mainThread = Looper.getMainLooper().thread
        var lastTickTime = System.currentTimeMillis()
        var capturedStackTrace: Array<StackTraceElement>? = null

        // Watchdog thread that samples main thread stack
        watchdogThread =
            Thread {
                Thread.sleep(delayStartMs) // Wait for app initialization

                while (isRunning) {
                    Thread.sleep(sampleIntervalMs) // Check every 50ms

                    val timeSinceLastTick = System.currentTimeMillis() - lastTickTime

                    // If main thread hasn't ticked in threshold time, capture its stack
                    if (timeSinceLastTick > thresholdMs) {
                        val stackTrace = mainThread.stackTrace

                        // Filter out framework/system code we can't control
                        val filteredStack = filterFrameworkCode(stackTrace)

                        // Only crash if there's actual blocking app code (not just framework)
                        val hasBlockingAppCode = detectBlockingAppCode(filteredStack)
                        val hasComposeFramework = detectComposeFramework(filteredStack)

                        // Needs a frame we can act on: ours, or Compose. Anything else is almost
                        // always the device asleep — the ticker stops running, so the gap grows
                        // without bound while the main thread's stack is the bare looper. Reporting
                        // those buried the real stalls 80 to 1 and cost 90% of the log file.
                        val actionable =
                            filteredStack.any { it.className.startsWith("com.windscribe") } ||
                                hasComposeFramework
                        if (actionable && System.currentTimeMillis() - lastReportAt > quietPeriodMs) {
                            lastReportAt = System.currentTimeMillis()
                            capturedStackTrace = filteredStack.toTypedArray()
                            val kind =
                                when {
                                    hasBlockingAppCode -> "blocking app code"
                                    hasComposeFramework -> "Compose framework"
                                    else -> "unclassified"
                                }
                            Log.w(
                                tag,
                                "MAIN THREAD STALLED ~${timeSinceLastTick}ms ($kind):\n" +
                                    filteredStack.take(12).joinToString("\n") { "  at $it" },
                            )
                        }
                    }
                }
            }.apply {
                name = "MainThreadWatchdog"
                isDaemon = true
                start()
            }

        // Ticker on main thread
        val ticker =
            object : Runnable {
                override fun run() {
                    lastTickTime = System.currentTimeMillis()
                    if (isRunning) {
                        Handler(Looper.getMainLooper()).postDelayed(this, 16) // Tick every frame
                    }
                }
            }

        Handler(Looper.getMainLooper()).postDelayed({
            ticker.run()
        }, delayStartMs)

        Log.i(tag, "MainThreadWatchdog started (threshold: ${thresholdMs}ms)")
    }

    fun stop() {
        isRunning = false
        watchdogThread?.interrupt()
        watchdogThread = null
        Log.i(tag, "MainThreadWatchdog stopped")
    }

    private fun filterFrameworkCode(stackTrace: Array<StackTraceElement>): List<StackTraceElement> =
        stackTrace.filter {
            !it.className.contains("Choreographer") &&
                !it.className.contains("Handler") &&
                !it.className.contains("Looper") &&
                !it.className.contains("MessageQueue") &&
                !it.className.contains("ViewRootImpl") &&
                !it.className.contains("HardwareRenderer") &&
                !it.className.contains("ThreadedRenderer") &&
                !it.className.contains("SurfaceView") &&
                !it.className.contains("ViewTreeObserver") &&
                !it.className.startsWith("android.graphics") &&
                !it.className.startsWith("android.view.View\$") &&
                !it.className.startsWith("android.view.Surface") &&
                !it.methodName.contains("nSetStopped") &&
                !it.methodName.contains("updateSurface") &&
                !it.methodName.contains("createBlastSurfaceControls")
        }

    private fun detectBlockingAppCode(stackTrace: List<StackTraceElement>): Boolean =
        stackTrace.any {
            // Our app code doing blocking operations
            (
                it.className.startsWith("com.windscribe") &&
                    (
                        it.methodName.contains("Blocking") ||
                            it.methodName.contains("sleep") ||
                            it.className.contains("TrayProviderHelper") ||
                            it.className.contains("ContentProvider") ||
                            it.className.contains("SharedPreferences")
                    )
            ) ||
                // Explicit blocking constructs
                it.methodName.contains("runBlocking") ||
                it.methodName == "sleep"
        }

    private fun detectComposeFramework(stackTrace: List<StackTraceElement>): Boolean =
        stackTrace.any {
            it.className.contains("androidx.compose") ||
                it.className.contains("StateFlow") ||
                it.className.contains("SnapshotState") ||
                it.className.contains("AndroidUiDispatcher")
        }
}
