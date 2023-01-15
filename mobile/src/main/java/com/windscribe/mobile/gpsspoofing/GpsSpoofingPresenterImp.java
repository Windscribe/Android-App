/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.gpsspoofing;

import com.windscribe.vpn.ActivityInteractor;

import javax.inject.Inject;

public class GpsSpoofingPresenterImp implements GpsSpoofingPresenter {

    private final ActivityInteractor gpsSpoofingInteractor;

    private final GpsSpoofingSettingView gpsSpoofingSettingView;

    @Inject
    public GpsSpoofingPresenterImp(GpsSpoofingSettingView gpsSpoofingSettingView,
            ActivityInteractor activityInteractor) {
        this.gpsSpoofingInteractor = activityInteractor;
        this.gpsSpoofingSettingView = gpsSpoofingSettingView;
    }

    @Override
    public void onError() {
        gpsSpoofingInteractor.getAppPreferenceInterface().setGpsSpoofing(true);
        gpsSpoofingSettingView.onError();
    }

    @Override
    public void onSuccess() {
        gpsSpoofingInteractor.getAppPreferenceInterface().setGpsSpoofing(true);
        gpsSpoofingSettingView.onSuccess();
    }
}
