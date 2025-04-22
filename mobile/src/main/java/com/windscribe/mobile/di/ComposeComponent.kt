package com.windscribe.mobile.di

import androidx.lifecycle.ViewModelProvider
import com.windscribe.vpn.di.ApplicationComponent
import dagger.Component

@PerCompose
@Component(dependencies = [ApplicationComponent::class], modules = [ComposeModule::class])
interface ComposeComponent {
    @Component.Builder
    interface Builder {
        fun applicationComponent(component: ApplicationComponent): Builder
        fun build(): ComposeComponent
    }
    fun getViewModelFactory() : ViewModelProvider.Factory
}