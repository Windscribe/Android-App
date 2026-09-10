package com.windscribe.vpn.workers.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.tunnel.VpnServiceGate
import com.windscribe.vpn.backend.utils.ConnectSource
import com.windscribe.vpn.state.ConnectionPolicy
import com.windscribe.vpn.state.ConnectionPolicyInputs
import com.windscribe.vpn.state.ShortcutStateManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.slf4j.LoggerFactory

@HiltWorker
class BootWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted params: WorkerParameters,
        private val policyInputs: ConnectionPolicyInputs,
        private val shortcutStateManager: ShortcutStateManager,
        private val vpnConnectionStateManager: VPNConnectionStateManager,
        private val serviceGate: VpnServiceGate,
    ) : CoroutineWorker(appContext, params) {
        private val logger = LoggerFactory.getLogger("worker")

        override suspend fun doWork(): Result {
            if (vpnConnectionStateManager.state.value.status != VPNState.Status.Disconnected) {
                return Result.success()
            }
            val reason = inputData.getString(KEY_REASON) ?: REASON_UNKNOWN
            val startReason =
                if (reason == REASON_APP_UPDATED) {
                    ConnectionPolicy.StartReason.AppUpdate
                } else {
                    ConnectionPolicy.StartReason.DeviceBoot
                }
            if (!ConnectionPolicy.shouldConnectOnStart(policyInputs.current(), startReason)) {
                logger.debug("Nothing to do after $reason: nothing asks us to connect.")
                return Result.success()
            }
            logger.debug("Connecting after $reason.")
            // Held until the service has attached: WorkManager may release the process the moment
            // this returns, and a success recorded against a lost connection is never retried.
            if (!shortcutStateManager.connectNow(ConnectSource.App)) {
                logger.error("Nothing was dispatched after $reason; no session to connect with.")
                return Result.failure()
            }
            if (serviceGate.await(HANDOFF_TIMEOUT_MS) == null) {
                logger.error("Connect after $reason never reached the VPN service; retrying.")
                return Result.retry()
            }
            logger.debug("Connect after $reason reached the service; it owns the attempt now.")
            return Result.success()
        }

        companion object {
            /** Longer than [VpnServiceGate]'s own wait: a cold start does more than start a service. */
            private const val HANDOFF_TIMEOUT_MS = 30_000L

            const val KEY_REASON = "reason"
            const val REASON_BOOT = "device boot"
            const val REASON_APP_UPDATED = "app update"
            private const val REASON_UNKNOWN = "an unknown trigger"
        }
    }
