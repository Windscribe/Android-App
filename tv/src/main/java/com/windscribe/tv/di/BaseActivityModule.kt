package com.windscribe.tv.di

import android.animation.ArgbEvaluator
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.windscribe.tv.confirmemail.ConfirmEmailView
import com.windscribe.tv.customview.CustomDialog
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
import dagger.Provides

@Module
open class BaseActivityModule {
    lateinit var confirmEmailView: ConfirmEmailView
    lateinit var activity: AppCompatActivity
    lateinit var emailView: AddEmailView
    lateinit var newsFeedView: NewsFeedView
    lateinit var splashView: SplashView
    lateinit var windscribeView: WindscribeView
    lateinit var detailView: DetailView
    lateinit var overlayView: OverlayView
    lateinit var rateView: RateView
    lateinit var settingView: SettingView
    lateinit var welcomeView: WelcomeView

    @Provides
    fun provideActivity(): AppCompatActivity {
        return activity
    }

    @Provides
    fun provideConfirmEmailView(): ConfirmEmailView {
        return confirmEmailView
    }

    @Provides
    fun provideDetailView(): DetailView {
        return detailView
    }

    @Provides
    fun provideOverlayView(): OverlayView {
        return overlayView
    }

    @Provides
    fun provideRateView(): RateView {
        return rateView
    }

    @Provides
    fun provideSettingView(): SettingView {
        return settingView
    }

    @Provides
    @PerActivity
    fun provideCustomDialog(): CustomDialog {
        return CustomDialog(activity)
    }

    @Provides
    fun provideEmailView(): AddEmailView {
        return emailView
    }

    @Provides
    fun provideSplashView(): SplashView {
        return splashView
    }

    @Provides
    fun provideWelcomeView(): WelcomeView {
        return welcomeView
    }

    @Provides
    fun provideWindscribeView(): WindscribeView {
        return windscribeView
    }

    @Provides
    fun provideNewsFeedView(): NewsFeedView {
        return newsFeedView
    }

    @Provides
    @PerActivity
    fun providesArgbEvaluator(): ArgbEvaluator {
        return ArgbEvaluator()
    }

    @Provides
    fun providesActivityScope(): LifecycleCoroutineScope {
        return activity.lifecycleScope
    }
}