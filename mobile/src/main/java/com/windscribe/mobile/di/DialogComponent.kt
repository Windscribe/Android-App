package com.windscribe.mobile.di

import com.windscribe.mobile.dialogs.ShareAppLinkDialog
import com.windscribe.vpn.di.ApplicationComponent
import dagger.Component

@PerDialog
@Component(dependencies = [ApplicationComponent::class], modules = [DialogModule::class])
interface DialogComponent {
    fun inject(shareAppLinkDialog: ShareAppLinkDialog)
}