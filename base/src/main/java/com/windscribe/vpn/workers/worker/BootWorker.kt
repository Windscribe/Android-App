package com.windscribe.vpn.workers.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.VPNState
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
        private val preferencesHelper: PreferencesHelper,
        private val shortcutStateManager: ShortcutStateManager,
        private val vpnConnectionStateManager: VPNConnectionStateManager,
    ) : CoroutineWorker(appContext, params) {
        private val logger = LoggerFactory.getLogger("worker")

        override suspend fun doWork(): Result {
            if (vpnConnectionStateManager.state.value.status != VPNState.Status.Disconnected) {
                return Result.success()
            }
            // Check if this is a boot or app upgrade reconnection
            if (preferencesHelper.autoStartOnBoot) {
                logger.debug("Device rebooted and Auto start on boot is true, attempting to connect.")
                shortcutStateManager.connect()
            } else if (preferencesHelper.globalUserConnectionPreference) {
                logger.debug("App updated and user was connected, attempting to reconnect.")
                shortcutStateManager.connect()
            }
            return Result.success()
        }
    }
