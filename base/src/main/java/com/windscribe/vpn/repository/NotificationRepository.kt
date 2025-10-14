/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.repository

import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.NewsFeedNotification
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.tables.PopupNotificationTable
import com.windscribe.vpn.localdatabase.tables.WindNotification
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val preferencesHelper: PreferencesHelper,
    private val apiCallManager: IApiCallManager,
    private val localDbInterface: LocalDbInterface
) {
    private val logger = LoggerFactory.getLogger("data")
    suspend fun update() {
        logger.debug("Starting notification data update.")
        val result = result<NewsFeedNotification> {
            apiCallManager.getNotifications(appContext.appLifeCycleObserver.pushNotificationAction?.pcpID)
        }
        when(result) {
            is CallResult.Error -> {
                logger.debug("Error updating notifications ")
            }
            is CallResult.Success -> {
                result.data.notifications?.let {
                    if (it.isNotEmpty()) {
                        try {
                            localDbInterface.addToPopupNotification(
                                PopupNotificationTable(
                                    it[0].notificationId,
                                    preferencesHelper.userName,
                                    if (it[0].popUp == 1) 1 else 0
                                )
                            )
                        } catch (e: Exception) {
                            logger.debug("Failed add pop notification. $e")
                        }
                        localDbInterface.insertWindNotifications(it)
                    }
                }
            }
        }
    }

    suspend fun getNotifications(): List<WindNotification> {
        update()
        return localDbInterface.getWindNotifications()
    }
}
