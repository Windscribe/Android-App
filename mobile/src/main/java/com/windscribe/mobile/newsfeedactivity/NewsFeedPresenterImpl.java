/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.newsfeedactivity;


import static com.windscribe.vpn.Windscribe.appContext;

import com.windscribe.mobile.adapter.NewsFeedAdapter;
import com.windscribe.vpn.ActivityInteractor;
import com.windscribe.vpn.api.response.PushNotificationAction;
import com.windscribe.vpn.errormodel.WindError;
import com.windscribe.vpn.localdatabase.tables.NewsfeedAction;
import com.windscribe.vpn.localdatabase.tables.WindNotification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class NewsFeedPresenterImpl implements NewsFeedPresenter, NewsFeedListener {

    private final String TAG = "news_feed_p";

    private NewsFeedAdapter mAdapter;

    private final ActivityInteractor mFeedInteractor;

    private final NewsFeedView mNewsFeedView;

    private final Logger mPresenterLog = LoggerFactory.getLogger(TAG);

    @Inject
    public NewsFeedPresenterImpl(NewsFeedView mNewsFeedView, ActivityInteractor activityInteractor) {
        this.mNewsFeedView = mNewsFeedView;
        this.mFeedInteractor = activityInteractor;
    }

    @Override
    public void onDestroy() {
        mFeedInteractor.getCompositeDisposable();
        if (!mFeedInteractor.getCompositeDisposable().isDisposed()) {
            mFeedInteractor.getCompositeDisposable().dispose();
        }
    }

    @Override
    public void init(boolean showPopUp, int popUpId) {
        //Set news feed alert to false
        mNewsFeedView.showProgress("Loading");
        mFeedInteractor.getAppPreferenceInterface().setShowNewsFeedAlert(false);
        mFeedInteractor.getCompositeDisposable().add(
                mFeedInteractor.getNotifications()
                        .onErrorResumeNext(mFeedInteractor.getNotificationUpdater().update().andThen(mFeedInteractor.getNotifications()))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(notifications -> onNotificationResponse(showPopUp, popUpId, notifications),
                                this::onNotificationResponseError));
    }

    @Override
    public void onNotificationActionClick(final WindNotification windNotification) {
        NewsfeedAction newsfeedAction = windNotification.getAction();
        if (newsfeedAction != null) {
            PushNotificationAction pushNotificationAction = new PushNotificationAction(newsfeedAction.getPcpID(),
                    newsfeedAction.getPromoCode(), newsfeedAction.getType());
            if (pushNotificationAction.getType().equals("promo")) {
                mNewsFeedView.startUpgradeActivity(pushNotificationAction);
            }
        }
    }

    @Override
    public void onNotificationExpand(WindNotification windNotification) {
        mFeedInteractor.getAppPreferenceInterface()
                .saveNotificationId(String.valueOf(windNotification.getNotificationId()));
    }

    private void onNotificationResponse(boolean showPopUp, int popUpId, List<WindNotification> mNotificationList) {
        mPresenterLog.info("Loaded notification data successfully...");
        int firstItemToOpen = -1;
        for (WindNotification wn : mNotificationList) {
            boolean read = mFeedInteractor.getAppPreferenceInterface()
                    .isNotificationAlreadyShown(String.valueOf(wn.getNotificationId()));
            if (!read && firstItemToOpen == -1) {
                firstItemToOpen = wn.getNotificationId();
            }
            wn.setRead(read);
        }
        if (showPopUp) {
            mPresenterLog.debug("Showing pop up message with Id: " + popUpId);
            firstItemToOpen = popUpId;
        } else if (firstItemToOpen != -1) {
            mPresenterLog.debug("Showing unread message with Id: " + firstItemToOpen);
        } else {
            mPresenterLog.debug("No pop up or unread message to show");
        }
        mAdapter = new NewsFeedAdapter(mNotificationList, firstItemToOpen,
                NewsFeedPresenterImpl.this);
        mNewsFeedView.setNewsFeedAdapter(mAdapter);
        mNewsFeedView.hideProgress();
    }

    private void onNotificationResponseError(Throwable throwable) {
        mPresenterLog.debug("Error getting notification data. Error: " +
                WindError.getInstance().convertThrowableToString(throwable));
        mNewsFeedView.showLoadingError("Error loading news feed data...");
        mNewsFeedView.hideProgress();
    }
}
