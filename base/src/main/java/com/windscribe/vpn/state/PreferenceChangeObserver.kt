/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.state

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import javax.inject.Singleton

@Singleton
class PreferenceChangeObserver {

    private val emailStatusChange = MutableLiveData<Boolean>()
    private val languageChange = MutableLiveData<String>()
    private val showLocationHealthChange = MutableLiveData<Boolean>()
    private val cityServerListChange = MutableLiveData<Boolean>()
    private val configListChange = MutableLiveData<Boolean>()
    private val locationSettingsChange = MutableLiveData<Boolean>()
    private val antiCenterShipStatus = MutableLiveData<Boolean>()

    fun addConfigListObserver(owner: LifecycleOwner, observer: Observer<Boolean>) {
        configListChange.observe(owner, observer)
    }

    fun addLanguageChangeObserver(owner: LifecycleOwner, observer: Observer<String>) {
        languageChange.observe(owner, observer)
    }

    fun addShowLocationHealthChangeObserver(owner: LifecycleOwner?, observer: Observer<Boolean>?) {
        showLocationHealthChange.observe(owner!!, observer!!)
    }

    fun addLocationSettingsChangeObserver(owner: LifecycleOwner, observer: Observer<Boolean>) {
        locationSettingsChange.observe(owner, observer)
    }

    fun addAntiCensorShipStatusChangeObserver(owner: LifecycleOwner, observer: Observer<Boolean>) {
        antiCenterShipStatus.observe(owner, observer)
    }

    fun postLocationSettingsChange() {
        locationSettingsChange.postValue(true)
    }

    fun postCityServerChange() {
        cityServerListChange.postValue(true)
    }

    fun postConfigListChange() {
        configListChange.postValue(true)
    }

    fun postEmailStatusChange() {
        emailStatusChange.postValue(true)
    }

    fun postLanguageChange(language: String) {
        languageChange.postValue(language)
    }

    fun postShowLocationHealthChange() {
        showLocationHealthChange.postValue(true)
    }
    fun postAntiCensorShipStatusChange() {
        antiCenterShipStatus.postValue(true)
    }
}
