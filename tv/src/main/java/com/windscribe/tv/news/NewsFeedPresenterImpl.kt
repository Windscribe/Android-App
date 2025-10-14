/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.news

import com.windscribe.tv.adapter.NewsFeedAdapter
import com.windscribe.vpn.api.response.PushNotificationAction
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.errormodel.WindError.Companion.instance
import com.windscribe.vpn.localdatabase.tables.NewsfeedAction
import com.windscribe.vpn.localdatabase.tables.WindNotification
import com.windscribe.vpn.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import javax.inject.Inject

class NewsFeedPresenterImpl @Inject constructor(
    private val newsFeedView: NewsFeedView,
    private val activityScope: CoroutineScope,
    private val preferencesHelper: PreferencesHelper,
    private val notificationRepository: NotificationRepository
) : NewsFeedPresenter, NewsFeedAdapter.NewsFeedListener {
    private var newsFeedAdapter: NewsFeedAdapter? = null
    private var notificationList: List<WindNotification>? = null
    private val logger = LoggerFactory.getLogger("basic")
    override fun onDestroy() {
        // Coroutine scope will be cancelled by the activity
    }

    override fun init(showPopUp: Boolean, popUpId: Int) {
        activityScope.launch(Dispatchers.IO) {
            preferencesHelper.setShowNewsFeedAlert(false)
        }

        activityScope.launch(Dispatchers.IO) {
            try {
                // Get notifications (this will update from API if needed)
                val notifications = notificationRepository.getNotifications()

                withContext(Dispatchers.Main) {
                    logger.info("Received notification data successfully...")
                    notificationList = notifications
                    newsFeedAdapter = NewsFeedAdapter(notificationList)
                    newsFeedAdapter?.let {
                        it.setListener(this@NewsFeedPresenterImpl)
                        newsFeedView.setNewsFeedAdapter(it)
                    }
                    notificationList?.let {
                        newsFeedView.setItemSelected(if (showPopUp) popUpId else it[0].notificationId)
                        newsFeedView.setNewsFeedContentText(it[0].notificationMessage)
                        it[0].action?.let { action ->
                            logger.debug("Action: {}", action)
                            newsFeedView.setActionLabel(action)
                        }
                    }
                }
            } catch (e: Throwable) {
                logger.debug(
                    "Error getting notification data. Error: " + instance.convertThrowableToString(e)
                )
                withContext(Dispatchers.Main) {
                    newsFeedView.showLoadingError("Error loading news feed data...")
                }
            }
        }
    }

    override fun onActionClick(action: NewsfeedAction) {
        val pushNotificationAction = PushNotificationAction(
            action.pcpID,
            action.promoCode, action.type
        )
        if (pushNotificationAction.type == "promo") {
            newsFeedView.startUpgradeActivity(pushNotificationAction)
        }
    }

    override fun onNewsFeedItemClick(windNotification: WindNotification) {
        newsFeedView.setNewsFeedContentText(windNotification.notificationMessage)
        windNotification.action?.let {
            newsFeedView.setActionLabel(it)
        } ?: kotlin.run {
            newsFeedView.hideActionLabel()
        }
    }
}
