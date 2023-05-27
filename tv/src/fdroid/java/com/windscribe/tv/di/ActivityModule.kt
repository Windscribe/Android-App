/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.di

import androidx.appcompat.app.AppCompatActivity
import com.windscribe.tv.confirmemail.ConfirmEmailView
import com.windscribe.tv.email.AddEmailView
import com.windscribe.tv.news.NewsFeedView
import com.windscribe.tv.rate.RateView
import com.windscribe.tv.serverlist.detail.DetailView
import com.windscribe.tv.serverlist.overlay.OverlayView
import com.windscribe.tv.settings.SettingView
import com.windscribe.tv.splash.SplashView
import com.windscribe.tv.welcome.WelcomeView
import com.windscribe.tv.windscribe.WindscribeView
import dagger.Module

@Module
class ActivityModule : BaseActivityModule {
    constructor(activity: AppCompatActivity, confirmEmailView: ConfirmEmailView) {
        this.activity = activity
        this.confirmEmailView = confirmEmailView
    }

    constructor(activity: AppCompatActivity, rateView: RateView) {
        this.activity = activity
        this.rateView = rateView
    }

    constructor(activity: AppCompatActivity, welcomeView: WelcomeView) {
        this.activity = activity
        this.welcomeView = welcomeView
    }

    constructor(activity: AppCompatActivity) {
        this.activity = activity
    }

    constructor(activity: AppCompatActivity, splashView: SplashView) {
        this.activity = activity
        this.splashView = splashView
    }

    constructor(activity: AppCompatActivity, overlayView: OverlayView) {
        this.activity = activity
        this.overlayView = overlayView
    }

    constructor(activity: AppCompatActivity, settingView: SettingView) {
        this.activity = activity
        this.settingView = settingView
    }

    constructor(activity: AppCompatActivity, windscribeView: WindscribeView) {
        this.activity = activity
        this.windscribeView = windscribeView
    }

    constructor(activity: AppCompatActivity, newsFeedView: NewsFeedView) {
        this.activity = activity
        this.newsFeedView = newsFeedView
    }

    constructor(activity: AppCompatActivity, emailView: AddEmailView) {
        this.activity = activity
        this.emailView = emailView
    }

    constructor(activity: AppCompatActivity, detailView: DetailView) {
        this.activity = activity
        this.detailView = detailView
    }
}
