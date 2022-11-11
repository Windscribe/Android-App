/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.news

import com.google.gson.Gson
import com.windscribe.tv.adapter.NewsFeedAdapter
import com.windscribe.vpn.ActivityInteractor
import com.windscribe.vpn.Windscribe.Companion.getExecutorService
import com.windscribe.vpn.api.response.NewsFeedNotification
import com.windscribe.vpn.api.response.PushNotificationAction
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.errormodel.WindError.Companion.instance
import com.windscribe.vpn.localdatabase.tables.NewsfeedAction
import com.windscribe.vpn.localdatabase.tables.WindNotification
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import org.slf4j.LoggerFactory
import javax.inject.Inject

class NewsFeedPresenterImpl @Inject constructor(
    private val newsFeedView: NewsFeedView,
    private val interactor: ActivityInteractor
) : NewsFeedPresenter, NewsFeedAdapter.NewsFeedListener {
    private var newsFeedAdapter: NewsFeedAdapter? = null
    private var notificationList: List<WindNotification>? = null
    private val logger = LoggerFactory.getLogger("news_feed_p")
    override fun onDestroy() {
        if (!interactor.getCompositeDisposable().isDisposed) {
            interactor.getCompositeDisposable().dispose()
        }
    }

    override fun init(showPopUp: Boolean, popUpId: Int) {
        getExecutorService().submit {
            interactor.getAppPreferenceInterface().setShowNewsFeedAlert(false)
        }
        interactor.getCompositeDisposable().add(
            interactor.getAppPreferenceInterface().notifications
                .onErrorResumeNext {
                    interactor.getApiCallManager().getNotifications()
                        .flatMap {
                            Single
                                .fromCallable {
                                    it.dataClass?.let {
                                        logger.info("Received data from api call. Saving in storage...")
                                        interactor.getAppPreferenceInterface()
                                            .saveResponseStringData(
                                                PreferencesKeyConstants.NEWS_FEED_RESPONSE,
                                                Gson().toJson(it)
                                            )
                                        return@fromCallable it
                                    } ?: kotlin.run {
                                        logger.debug("Server returned null response!")
                                        it.errorClass?.let {
                                            throw Exception(it.errorMessage)
                                        }
                                    }
                                }
                        }.doOnError {
                            logger.debug(
                                "Error getting notification data. Error: " + instance.convertThrowableToString(
                                    it
                                )
                            )
                            newsFeedView.showLoadingError("Error loading news feed data...")
                        }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<NewsFeedNotification?>() {
                    override fun onError(e: Throwable) {
                        logger.debug(
                            "Error getting notification data. Error: " + instance.convertThrowableToString(
                                e
                            )
                        )
                        newsFeedView.showLoadingError("Error loading news feed data...")
                    }

                    override fun onSuccess(newsFeedNotification: NewsFeedNotification) {
                        logger.info("Received notification data successfully...")
                        notificationList = newsFeedNotification.notifications
                        newsFeedAdapter =
                            NewsFeedAdapter(
                                notificationList
                            )
                        newsFeedAdapter?.let {
                            it.setListener(this@NewsFeedPresenterImpl)
                            newsFeedView.setNewsFeedAdapter(it)
                        }
                        notificationList?.let {
                            newsFeedView.setItemSelected(if (showPopUp) popUpId else it[0].notificationId)
                            newsFeedView.setNewsFeedContentText(it[0].notificationMessage)
                            it[0].action?.let { action ->
                                newsFeedView.setActionLabel(action)
                            }
                        }
                    }
                })
        )
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
        }
    }
}
