package com.windscribe.tv.di

import android.animation.ArgbEvaluator
import android.app.Activity
import com.windscribe.tv.confirmemail.ConfirmEmailPresenter
import com.windscribe.tv.confirmemail.ConfirmEmailPresenterImp
import com.windscribe.tv.customview.CustomDialog
import com.windscribe.tv.email.AddEmailPresenter
import com.windscribe.tv.email.AddEmailPresenterImpl
import com.windscribe.tv.news.NewsFeedPresenter
import com.windscribe.tv.news.NewsFeedPresenterImpl
import com.windscribe.tv.rate.RateMyAppPresenter
import com.windscribe.tv.rate.RateMyAppPresenterImp
import com.windscribe.tv.serverlist.detail.DetailPresenter
import com.windscribe.tv.serverlist.detail.DetailsPresenterImp
import com.windscribe.tv.serverlist.overlay.OverlayPresenter
import com.windscribe.tv.serverlist.overlay.OverlayPresenterImp
import com.windscribe.tv.settings.SettingsPresenter
import com.windscribe.tv.settings.SettingsPresenterImp
import com.windscribe.tv.splash.SplashPresenter
import com.windscribe.tv.splash.SplashPresenterImpl
import com.windscribe.tv.welcome.WelcomePresenter
import com.windscribe.tv.welcome.WelcomePresenterImpl
import com.windscribe.tv.windscribe.WindscribePresenter
import com.windscribe.tv.windscribe.WindscribePresenterImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
abstract class TvActivityModule {
    @Binds
    @ActivityScoped
    abstract fun bindConfirmEmailPresenter(impl: ConfirmEmailPresenterImp): ConfirmEmailPresenter

    @Binds
    @ActivityScoped
    abstract fun bindAddEmailPresenter(impl: AddEmailPresenterImpl): AddEmailPresenter

    @Binds
    @ActivityScoped
    abstract fun bindNewsFeedPresenter(impl: NewsFeedPresenterImpl): NewsFeedPresenter

    @Binds
    @ActivityScoped
    abstract fun bindRateMyAppPresenter(impl: RateMyAppPresenterImp): RateMyAppPresenter

    @Binds
    @ActivityScoped
    abstract fun bindDetailPresenter(impl: DetailsPresenterImp): DetailPresenter

    @Binds
    @ActivityScoped
    abstract fun bindOverlayPresenter(impl: OverlayPresenterImp): OverlayPresenter

    @Binds
    @ActivityScoped
    abstract fun bindSettingsPresenter(impl: SettingsPresenterImp): SettingsPresenter

    @Binds
    @ActivityScoped
    abstract fun bindSplashPresenter(impl: SplashPresenterImpl): SplashPresenter

    @Binds
    @ActivityScoped
    abstract fun bindWelcomePresenter(impl: WelcomePresenterImpl): WelcomePresenter

    @Binds
    @ActivityScoped
    abstract fun bindWindscribePresenter(impl: WindscribePresenterImpl): WindscribePresenter

    companion object {
        @Provides
        @ActivityScoped
        @JvmStatic
        fun provideCustomDialog(activity: Activity): CustomDialog = CustomDialog(activity)

        @Provides
        @JvmStatic
        fun provideArgbEvaluator(): ArgbEvaluator = ArgbEvaluator()
    }
}
