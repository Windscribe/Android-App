package com.windscribe.vpn.debug

import android.os.Handler
import android.os.Looper
import org.slf4j.LoggerFactory

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
    private val delayStartMs: Long = 2000
) {
    private val logger = LoggerFactory.getLogger("app")
    private var watchdogThread: Thread? = null
    private var isRunning = false

    fun start() {
        if (isRunning) {
            logger.warn("Watchdog already running")
            return
        }

        isRunning = true
        val mainThread = Looper.getMainLooper().thread
        var lastTickTime = System.currentTimeMillis()
        var capturedStackTrace: Array<StackTraceElement>? = null

        // Watchdog thread that samples main thread stack
        watchdogThread = Thread {
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

                    if (filteredStack.isNotEmpty() && hasBlockingAppCode && !hasComposeFramework) {
                        capturedStackTrace = filteredStack.toTypedArray()

                        val stackTraceString = filteredStack.joinToString("\n") { "  at $it" }
                        logger.error("Main thread BLOCKED for ${timeSinceLastTick}ms! Stack trace:\n$stackTraceString")

                        // Post crash to main thread
                        Handler(Looper.getMainLooper()).post {
                            throw RuntimeException(
                                "Main thread blocked for ${timeSinceLastTick}ms!\n" +
                                "Stack trace shows what was blocking:\n" +
                                capturedStackTrace?.take(10)?.joinToString("\n") { "  at $it" }
                            )
                        }
                        break
                    }
                }
            }
        }.apply {
            name = "MainThreadWatchdog"
            isDaemon = true
            start()
        }

        // Ticker on main thread
        val ticker = object : Runnable {
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

        logger.info("MainThreadWatchdog started (threshold: ${thresholdMs}ms)")
    }

    fun stop() {
        isRunning = false
        watchdogThread?.interrupt()
        watchdogThread = null
        logger.info("MainThreadWatchdog stopped")
    }

    private fun filterFrameworkCode(stackTrace: Array<StackTraceElement>): List<StackTraceElement> {
        return stackTrace.filter {
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
    }

    private fun detectBlockingAppCode(stackTrace: List<StackTraceElement>): Boolean {
        return stackTrace.any {
            // Our app code doing blocking operations
            (it.className.startsWith("com.windscribe") &&
             (it.methodName.contains("Blocking") ||
              it.methodName.contains("sleep") ||
              it.className.contains("TrayProviderHelper") ||
              it.className.contains("ContentProvider") ||
              it.className.contains("SharedPreferences"))) ||
            // Explicit blocking constructs
            it.methodName.contains("runBlocking") ||
            it.methodName == "sleep"
        }
    }

    private fun detectComposeFramework(stackTrace: List<StackTraceElement>): Boolean {
        return stackTrace.any {
            it.className.contains("androidx.compose") ||
            it.className.contains("StateFlow") ||
            it.className.contains("SnapshotState") ||
            it.className.contains("AndroidUiDispatcher")
        }
    }
}
