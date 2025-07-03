/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.repository

import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.tables.PopupNotificationTable
import com.windscribe.vpn.localdatabase.tables.WindNotification
import io.reactivex.Completable
import kotlinx.coroutines.rx2.await
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
    fun update(): Completable {
        logger.debug("Starting notification data update.")
        return apiCallManager.getNotifications(appContext.appLifeCycleObserver.pushNotificationAction?.pcpID)
            .flatMapCompletable { response ->
                response.dataClass?.let { newsfeed ->
                    newsfeed.notifications?.let {
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
                            return@flatMapCompletable localDbInterface.insertWindNotifications(it)
                        }
                    }
                }
                return@flatMapCompletable response.errorClass?.let {
                    logger.debug("Error updating notifications ")
                    Completable.fromAction {}
                }
            }
    }

    suspend fun getNotifications(): List<WindNotification> {
        update().result()
        return localDbInterface.windNotifications.await() ?: emptyList()
    }
}
