/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.newsfeedactivity

import com.windscribe.mobile.adapter.NewsFeedAdapter
import com.windscribe.vpn.ActivityInteractor
import com.windscribe.vpn.api.response.PushNotificationAction
import com.windscribe.vpn.errormodel.WindError.Companion.instance
import com.windscribe.vpn.localdatabase.tables.WindNotification
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.slf4j.LoggerFactory
import javax.inject.Inject

class NewsFeedPresenterImpl @Inject constructor(
    private val newsFeedView: NewsFeedView,
    private val interactor: ActivityInteractor
) : NewsFeedPresenter, NewsFeedListener {
    private var adapter: NewsFeedAdapter? = null
    private val logger = LoggerFactory.getLogger("news_feed_p")
    override fun onDestroy() {
        interactor.getCompositeDisposable()
        if (!interactor.getCompositeDisposable().isDisposed) {
            interactor.getCompositeDisposable().dispose()
        }
    }

    override fun init(showPopUp: Boolean, popUpId: Int) {
        //Set news feed alert to false
        newsFeedView.showProgress("Loading")
        interactor.getAppPreferenceInterface().setShowNewsFeedAlert(false)
        interactor.getCompositeDisposable().add(
            interactor.getNotifications()
                .onErrorResumeNext(
                    interactor.getNotificationUpdater().update()
                        .andThen(interactor.getNotifications())
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ notifications: List<WindNotification> ->
                    onNotificationResponse(
                        showPopUp,
                        popUpId,
                        notifications
                    )
                }) { throwable: Throwable -> onNotificationResponseError(throwable) })
    }

    override fun onNotificationActionClick(windNotification: WindNotification) {
        val newsfeedAction = windNotification.action
        if (newsfeedAction != null) {
            val pushNotificationAction = PushNotificationAction(
                newsfeedAction.pcpID,
                newsfeedAction.promoCode, newsfeedAction.type
            )
            if (pushNotificationAction.type == "promo") {
                newsFeedView.startUpgradeActivity(pushNotificationAction)
            }
        }
    }

    override fun onNotificationExpand(windNotification: WindNotification) {
        interactor.getAppPreferenceInterface()
            .saveNotificationId(windNotification.notificationId.toString())
    }

    private fun onNotificationResponse(
        showPopUp: Boolean,
        popUpId: Int,
        mNotificationList: List<WindNotification>
    ) {
        logger.info("Loaded notification data successfully...")
        var firstItemToOpen = -1
        for (wn in mNotificationList) {
            val read = interactor.getAppPreferenceInterface()
                .isNotificationAlreadyShown(wn.notificationId.toString())
            if (!read && firstItemToOpen == -1) {
                firstItemToOpen = wn.notificationId
            }
            wn.isRead = read
        }
        if (showPopUp) {
            logger.debug("Showing pop up message with Id: $popUpId")
            firstItemToOpen = popUpId
        } else if (firstItemToOpen != -1) {
            logger.debug("Showing unread message with Id: $firstItemToOpen")
        } else {
            logger.debug("No pop up or unread message to show")
        }
        adapter = NewsFeedAdapter(
            mNotificationList, firstItemToOpen,
            this@NewsFeedPresenterImpl
        )
        newsFeedView.setNewsFeedAdapter(adapter!!)
        newsFeedView.hideProgress()
    }

    private fun onNotificationResponseError(throwable: Throwable) {
        logger.debug(
            "Error getting notification data. Error: " +
                    instance.convertThrowableToString(throwable)
        )
        newsFeedView.showLoadingError("Error loading news feed data...")
        newsFeedView.hideProgress()
    }
}