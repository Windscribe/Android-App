/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.bootreceiver

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentWorkAroundService
import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.state.ShortcutStateManager
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class BootSessionService : JobIntentWorkAroundService() {

    @Inject
    lateinit var interactor: ServiceInteractor

    @Inject
    lateinit var vpnController: WindVpnController

    @Inject
    lateinit var shortcutStateManager: ShortcutStateManager

    private val logger = LoggerFactory.getLogger("boot_session_s")
    private val stateBoolean = AtomicBoolean()
    override fun onCreate() {
        super.onCreate()
        stateBoolean.set(true)
        appContext.serviceComponent.inject(this)
    }

    override fun onHandleWork(intent: Intent) {
        if (stateBoolean.getAndSet(false)) {
            if (interactor.preferenceHelper.autoStartOnBoot) {
                logger.debug("Device rebooted and Auto start on boot is true, attempting to connect.")
                shortcutStateManager.connect()
            }
        }
    }

    companion object {

        private const val BOOT_JOB_ID = 9192

        @JvmStatic
        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(context, BootSessionService::class.java, BOOT_JOB_ID, intent)
        }
    }
}
