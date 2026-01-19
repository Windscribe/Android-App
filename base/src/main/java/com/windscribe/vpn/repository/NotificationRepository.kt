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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val scope: CoroutineScope,
    private val preferencesHelper: PreferencesHelper,
    private val apiCallManager: IApiCallManager,
    private val localDbInterface: LocalDbInterface
) {
    private val logger = LoggerFactory.getLogger("migration")

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount

    init {
        observeNotifications()
    }

    private fun observeNotifications() {
        scope.launch {
            localDbInterface.observeNotifications()
                .map { notifications -> notifications.count { !it.isRead } }
                .collect { count ->
                    _unreadCount.value = count
                }
        }
    }
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
                            // Only insert popup notification if it doesn't exist yet
                            // This preserves the user's local state (whether they've seen it)
                            if (!localDbInterface.popupNotificationExists(it[0].notificationId)) {
                                localDbInterface.addToPopupNotification(
                                    PopupNotificationTable(
                                        it[0].notificationId,
                                        preferencesHelper.userName,
                                        if (it[0].popUp == 1) 1 else 0
                                    )
                                )
                                logger.debug("Added new popup notification with id ${it[0].notificationId}")
                            } else {
                                logger.debug("Popup notification ${it[0].notificationId} already exists, preserving local state")
                            }
                        } catch (e: Exception) {
                            logger.debug("Failed add pop notification. $e")
                        }

                        // Preserve isRead status before clearing
                        val existingNotifications = localDbInterface.getWindNotifications()
                        val readStatusMap = existingNotifications.associate { it.notificationId to it.isRead }

                        // Clear old notifications before inserting new ones to avoid orphaned data
                        localDbInterface.clearWindNotifications()

                        // Restore isRead status for notifications that existed before
                        it.forEach { notification ->
                            notification.isRead = readStatusMap[notification.notificationId] ?: false
                        }

                        localDbInterface.insertWindNotifications(it)
                        logger.debug("Replaced notifications cache with ${it.size} items from API")
                    }
                }
            }
        }
    }

    suspend fun getNotifications(): List<WindNotification> {
        update()
        return localDbInterface.getWindNotifications()
    }

    suspend fun markNotificationAsRead(notificationId: Int) {
        localDbInterface.markNotificationAsRead(notificationId)
    }
}
