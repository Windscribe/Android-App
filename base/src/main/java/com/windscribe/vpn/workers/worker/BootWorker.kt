package com.windscribe.vpn.workers.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.state.ShortcutStateManager
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
    ) : CoroutineWorker(appContext, params) {
        private val logger = LoggerFactory.getLogger("worker")

        override suspend fun doWork(): Result {
            if (preferencesHelper.autoStartOnBoot) {
                logger.debug("Device rebooted and Auto start on boot is true, attempting to connect.")
                shortcutStateManager.connect()
            }
            return Result.success()
        }
    }
