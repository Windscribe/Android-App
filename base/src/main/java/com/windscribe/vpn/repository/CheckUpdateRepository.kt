/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.repository

import android.os.Build
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.CheckUpdateResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.commonutils.Ext.result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckUpdateRepository
    @Inject
    constructor(
        private val scope: CoroutineScope,
        private val apiCallManager: IApiCallManager,
        private val preferencesHelper: PreferencesHelper,
    ) {
        private val logger = LoggerFactory.getLogger("update_check")

        private val _updateAvailable = MutableStateFlow<CheckUpdateResponse?>(null)
        val updateAvailable: StateFlow<CheckUpdateResponse?> = _updateAvailable

        private companion object {
            const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours
            const val PROMPT_INTERVAL_MS = 72 * 60 * 60 * 1000L // 72 hours
        }

        fun checkForUpdate() {
            if (!shouldCheck()) {
                logger.debug("Skipping update check — last check was within 24 hours.")
                return
            }
            scope.launch {
                preferencesHelper.lastUpdateCheckTimestamp = System.currentTimeMillis()
                val appVersion =
                    try {
                        val context = com.windscribe.vpn.Windscribe.Companion.appContext
                        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                        pInfo.versionName ?: ""
                    } catch (e: Exception) {
                        ""
                    }
                val appBuild =
                    try {
                        val context = com.windscribe.vpn.Windscribe.Companion.appContext
                        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            pInfo.longVersionCode.toString()
                        } else {
                            @Suppress("DEPRECATION")
                            pInfo.versionCode.toString()
                        }
                    } catch (e: Exception) {
                        ""
                    }
                val osVersion = Build.VERSION.RELEASE
                val result =
                    result<CheckUpdateResponse> {
                        apiCallManager.checkUpdate(appVersion, appBuild, osVersion)
                    }
                when (result) {
                    is CallResult.Success -> {
                        logger.info(
                            "Update check result: available=${result.data.isUpdateAvailable}, force=${result.data.isForceUpgrade}, version=${result.data.latestVersion}",
                        )
                        // On force, clear the 24h cooldown so the next cold launch
                        // re-arms the gate even if the user force-quit the app.
                        if (result.data.isUpdateAvailable && result.data.isForceUpgrade) {
                            preferencesHelper.lastUpdateCheckTimestamp = 0L
                        }
                        _updateAvailable.value = result.data
                    }

                    is CallResult.Error -> {
                        logger.error("Update check failed: ${result.errorMessage}")
                    }
                }
            }
        }

        fun shouldShowPrompt(): Boolean {
            val lastPrompt = preferencesHelper.lastUpdatePromptTimestamp
            if (lastPrompt == 0L) return true
            return System.currentTimeMillis() - lastPrompt >= PROMPT_INTERVAL_MS
        }

        fun recordPromptShown() {
            preferencesHelper.lastUpdatePromptTimestamp = System.currentTimeMillis()
        }

        private fun shouldCheck(): Boolean {
            val lastCheck = preferencesHelper.lastUpdateCheckTimestamp
            if (lastCheck == 0L) return true
            return System.currentTimeMillis() - lastCheck >= CHECK_INTERVAL_MS
        }
    }
