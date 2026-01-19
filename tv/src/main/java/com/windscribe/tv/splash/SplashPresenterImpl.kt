/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.splash

import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.errormodel.WindError.Companion.instance
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.tables.ServerStatusUpdateTable
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.repository.ServerListRepository
import com.windscribe.vpn.repository.StaticIpRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.services.ReceiptValidator
import com.windscribe.vpn.workers.WindScribeWorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.Date
import javax.inject.Inject

class SplashPresenterImpl @Inject constructor(
    private var splashView: SplashView,
    private var activityScope: CoroutineScope,
    private var preferencesHelper: PreferencesHelper,
    private var apiCallManager: IApiCallManager,
    private var localDbInterface: LocalDbInterface,
    private var autoConnectionManager: AutoConnectionManager,
    private var workManager: WindScribeWorkManager,
    private var receiptValidator: ReceiptValidator,
    private var userRepository: UserRepository,
    private var serverListRepository: ServerListRepository,
    private var staticIpRepository: StaticIpRepository
) : SplashPresenter {
    private val logger = LoggerFactory.getLogger("basic")

    override fun onDestroy() {
        // Coroutine scope will be cancelled by the activity
    }

    fun checkApplicationInstanceAndDecideActivity() {
        if (preferencesHelper.isNewApplicationInstance) {
            activityScope.launch(Dispatchers.IO) {
                preferencesHelper.isNewApplicationInstance = false
            }
            val installation = preferencesHelper.newInstallation
            if (PreferencesKeyConstants.I_NEW == installation) {
                // Record new install
                preferencesHelper.newInstallation = PreferencesKeyConstants.I_OLD
                activityScope.launch(Dispatchers.IO) {
                    try {
                        val result = result<String> {
                            apiCallManager.recordAppInstall()
                        }
                        withContext(Dispatchers.Main) {
                            when (result) {
                                is CallResult.Success -> {
                                    logger.info("Recording app install success. ${result.data}")
                                }
                                is CallResult.Error -> {
                                    logger.debug("Recording app install failed. ${result.errorMessage}")
                                }
                            }
                            decideActivity()
                        }
                    } catch (e: Throwable) {
                        logger.debug("Error: ${instance.convertThrowableToString(e)}")
                        withContext(Dispatchers.Main) {
                            decideActivity()
                        }
                    }
                }
            } else {
                // Not a new install, decide activity
                decideActivity()
            }
        } else {
            // Decide which activity to goto
            decideActivity()
        }
    }

    override fun checkNewMigration() {
        autoConnectionManager.reset()
        migrateSessionAuthIfRequired()
        val userLoggedIn = preferencesHelper.sessionHash != null
        if (userLoggedIn) {
            if (preferencesHelper.loginTime == null){
                preferencesHelper.loginTime = Date()
            }
            activityScope.launch(Dispatchers.IO) {
                try {
                    // Check if server data is available (check if any cities exist)
                    val serverListAvailable = try {
                        localDbInterface.getCitiesAsync().count() > 0
                    } catch (_: Exception) {
                        false
                    }

                    withContext(Dispatchers.Main) {
                        if (serverListAvailable) {
                            logger.info("Migration not required.")
                            checkApplicationInstanceAndDecideActivity()
                        } else {
                            logger.info("Migration required. updating server list.")
                            updateDataFromApiAndOldStorage()
                        }
                    }
                } catch (_: Throwable) {
                    withContext(Dispatchers.Main) {
                        checkApplicationInstanceAndDecideActivity()
                    }
                }
            }
        } else {
            checkApplicationInstanceAndDecideActivity()
        }
    }

    fun decideActivity() {
        val sessionHash = preferencesHelper.sessionHash
        if (sessionHash != null) {
            logger.info("Session auth hash present. User is already logged in...")
            if (WindUtilities.isOnline()) {
                workManager.updateNodeLatencies()
                receiptValidator.checkPendingAccountUpgrades()
            }
            splashView.navigateToHome()
        } else {
            splashView.navigateToLogin()
        }
    }

    // Move SessionAuth to secure preferences
    private fun migrateSessionAuthIfRequired() {
        val oldSessionAuth = preferencesHelper.oldSessionAuth
        val newSessionAuth = preferencesHelper.sessionHash
        if (oldSessionAuth != null && newSessionAuth == null) {
            logger.debug("Migrating session auth to secure preferences")
            preferencesHelper.sessionHash = oldSessionAuth
            preferencesHelper.clearOldSessionAuth()
        }
    }

    private fun updateDataFromApiAndOldStorage() {
        activityScope.launch(Dispatchers.IO) {
            try {
                // Try to update server list
                try {
                    serverListRepository.update()
                } catch (e: Exception) {
                    logger.info("Failed to download server list.")
                    throw e
                }

                // Update static IPs
                staticIpRepository.updateFromApi()

                // Update user data
                userRepository.reload()

                // Update server data
                localDbInterface.insertOrUpdateStatusAsync(
                    ServerStatusUpdateTable(
                        preferencesHelper.userName,
                        preferencesHelper.userStatus
                    )
                )
            } catch (throwable: Throwable) {
                // Fallback: try to update user and server data even if server list update failed
                logger.info(
                    "*********Preparing dashboard failed: $throwable" +
                        " Use reload button in server list in home activity.*******"
                )
                try {
                    userRepository.reload()
                    localDbInterface.insertOrUpdateStatusAsync(
                        ServerStatusUpdateTable(
                            preferencesHelper.userName,
                            preferencesHelper.userStatus
                        )
                    )
                } catch (_: Exception) {
                    // Ignore errors in fallback
                }
            }

            withContext(Dispatchers.Main) {
                checkApplicationInstanceAndDecideActivity()
            }
        }
    }
}
