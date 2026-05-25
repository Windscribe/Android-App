package com.windscribe.vpn.debug

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

/**
 * Periodically logs how many workers in the shared kotlinx-coroutines scheduler
 * (used by Dispatchers.IO and Dispatchers.Default) are currently busy, sleeping,
 * or parked. Also dumps the top of the stack for any worker that looks pinned
 * inside app code, so leaked Thread.sleep / blocking JNI calls are visible.
 *
 * Default pool size is max(64, nCpus). One stuck worker is fine; many stuck
 * workers means a thread leak).
 */
class DispatcherUsageMonitor(
    private val scope: CoroutineScope,
    private val intervalMs: Long = 10_000L,
    private val pinnedThresholdMs: Long = 5_000L
) {
    private val logger = LoggerFactory.getLogger("DispatcherUsageMonitor")
    private var job: Job? = null

    private data class Sample(val state: Thread.State, val top: StackTraceElement?)
    private val lastSample = mutableMapOf<String, Sample>()
    private val pinnedSince = mutableMapOf<String, Long>()

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            withContext(NonCancellable) {
                while (true) {
                    runCatching { sampleAndLog() }
                        .onFailure { logger.warn("DispatcherUsageMonitor sample failed: ${it.message}") }
                    delay(intervalMs)
                }
            }
        }
        logger.info("DispatcherUsageMonitor started (interval=${intervalMs}ms)")
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private fun sampleAndLog() {
        val selfThread = Thread.currentThread()
        val all = Thread.getAllStackTraces()
        val workers = all.entries.filter {
            val n = it.key.name
            it.key !== selfThread && (
                n.startsWith("DefaultDispatcher-worker-") ||
                    n.startsWith("CommonPool-worker-") ||
                    n.startsWith("kotlinx.coroutines")
                )
        }

        if (workers.isEmpty()) {
            logger.debug("DispatcherUsage: no scheduler workers found yet")
            return
        }

        var runnable = 0
        var timedWaiting = 0
        var waiting = 0
        var blocked = 0
        val now = System.currentTimeMillis()
        val pinnedReports = mutableListOf<PinnedReport>()
        val knownLongRunningCounts = mutableMapOf<String, Int>()

        for ((thread, stack) in workers) {
            when (thread.state) {
                Thread.State.RUNNABLE -> runnable++
                Thread.State.TIMED_WAITING -> timedWaiting++
                Thread.State.WAITING -> waiting++
                Thread.State.BLOCKED -> blocked++
                else -> Unit
            }

            val top = stack.firstOrNull { it.className.startsWith("com.windscribe") }
                ?: stack.firstOrNull()
            val current = Sample(thread.state, top)
            val previous = lastSample[thread.name]

            // A worker is "pinned" if it stays in the same non-parked frame across samples.
            val isParked = top?.className?.contains("park", ignoreCase = true) == true ||
                top?.methodName?.contains("park", ignoreCase = true) == true ||
                top?.className?.endsWith("CoroutineScheduler\$Worker") == true
            val sameAsBefore = previous != null &&
                previous.state == current.state &&
                previous.top?.className == current.top?.className &&
                previous.top?.methodName == current.top?.methodName

            val isSelfMonitor = stack.any {
                it.className == "com.windscribe.vpn.debug.DispatcherUsageMonitor" ||
                    it.className.startsWith("com.windscribe.vpn.debug.DispatcherUsageMonitor$")
            }
            // Long-blocking JNI calls that are expected to pin one worker each for the
            // duration of a VPN session. They're not leaks — the Go binaries embedded
            // via JNI block until stop() is called from elsewhere. Tracked separately
            // below so we can still detect duplicates (= real leaks).
            val knownFrame = stack.firstNotNullOfOrNull { frame ->
                KNOWN_LONG_RUNNING_FRAMES.firstOrNull { (cls, method) ->
                    frame.className == cls && frame.methodName == method
                }?.let { "${it.first.substringAfterLast('.')}.${it.second}" }
            }
            val isKnownLongRunning = knownFrame != null
            if (knownFrame != null) {
                knownLongRunningCounts[knownFrame] = (knownLongRunningCounts[knownFrame] ?: 0) + 1
            }
            if (!isParked && !isSelfMonitor && !isKnownLongRunning &&
                sameAsBefore && thread.state != Thread.State.WAITING) {
                val since = pinnedSince.getOrPut(thread.name) { now }
                val pinnedMs = now - since
                if (pinnedMs >= pinnedThresholdMs) {
                    pinnedReports += PinnedReport(thread.name, thread.state, pinnedMs, stack)
                }
            } else {
                pinnedSince.remove(thread.name)
            }
            lastSample[thread.name] = current
        }

        val knownSummary = if (knownLongRunningCounts.isEmpty()) "" else
            " known=" + knownLongRunningCounts.entries.joinToString(",") { "${it.key}:${it.value}" }
        logger.debug(
            "DispatcherUsage: workers=${workers.size} runnable=$runnable " +
                "timedWaiting=$timedWaiting waiting=$waiting blocked=$blocked$knownSummary"
        )
        // Detect leaks of supposedly-known-good long-running calls: more than one
        // worker pinned in the same JNI call means stop() wasn't called.
        val duplicates = knownLongRunningCounts.filter { it.value > 1 }
        if (duplicates.isNotEmpty()) {
            logger.warn(
                "DispatcherUsage: known long-running call(s) duplicated (possible leak): " +
                    duplicates.entries.joinToString(", ") { "${it.key}x${it.value}" }
            )
        }
        if (pinnedReports.isNotEmpty()) {
            // Bucket by caller signature so 49 identical runBlocking parks collapse to one entry.
            val buckets = pinnedReports.groupBy { callerSignature(it.stack) }
            val summary = buckets.entries
                .sortedByDescending { it.value.size }
                .joinToString("\n") { (sig, reports) ->
                    val sample = reports.maxByOrNull { r: PinnedReport -> r.pinnedMs } ?: reports.first()
                    val maxMs = sample.pinnedMs
                    val snippet = formatStack(sample.stack)
                    "    [${reports.size}x] caller=$sig maxPinnedMs=$maxMs (sample: ${sample.thread})\n$snippet"
                }
            logger.warn(
                "DispatcherUsage: ${pinnedReports.size} worker(s) pinned >= ${pinnedThresholdMs}ms in " +
                    "${buckets.size} bucket(s):\n$summary"
            )
        }
    }

    private class PinnedReport(
        val thread: String,
        val state: Thread.State,
        val pinnedMs: Long,
        val stack: Array<StackTraceElement>
    )

    /**
     * Identifies the actual caller below kotlinx/JDK plumbing. For a `runBlocking`
     * park stack we want the first com.windscribe frame, not Unsafe.park.
     */
    private fun callerSignature(stack: Array<StackTraceElement>): String {
        val appFrame = stack.firstOrNull { it.className.startsWith("com.windscribe") }
        if (appFrame != null) {
            val cls = appFrame.className.substringAfterLast('.')
            return "$cls.${appFrame.methodName}:${appFrame.lineNumber}"
        }
        // No app frame at all — fall back to top non-park frame.
        val nonPark = stack.firstOrNull {
            !it.methodName.contains("park", ignoreCase = true) &&
                !it.className.contains("LockSupport") &&
                !it.className.contains("Unsafe")
        } ?: stack.firstOrNull()
        return nonPark?.let { "${it.className.substringAfterLast('.')}.${it.methodName}" } ?: "unknown"
    }

    private fun formatStack(stack: Array<StackTraceElement>): String {
        // Show 4 framework frames + first 3 app frames so we always see the caller.
        val firstApp = stack.indexOfFirst { it.className.startsWith("com.windscribe") }
        val end = if (firstApp >= 0) (firstApp + 3).coerceAtMost(stack.size) else 6.coerceAtMost(stack.size)
        return stack.take(end).joinToString("\n") { "      at $it" }
    }

    companion object {
        // (className, methodName) pairs for JNI calls that are expected to block one
        // IO worker for the duration of a VPN session. Exactly one occurrence is normal;
        // duplicates indicate a missing stop() call.
        private val KNOWN_LONG_RUNNING_FRAMES: List<Pair<String, String>> = listOf(
            "com.windscribe.vpn.backend.CdLib" to "startCd",
            "com.windscribe.vpn.backend.openvpn.WSTunnelLib" to "startProxy"
        )
    }
}