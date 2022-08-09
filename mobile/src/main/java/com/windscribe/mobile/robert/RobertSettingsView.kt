/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.robert

import com.windscribe.mobile.adapter.RobertSettingsAdapter

interface RobertSettingsView {
    fun hideProgress()
    fun openUrl(url: String)
    fun setAdapter(robertSettingsAdapter: RobertSettingsAdapter)
    fun setTitle(title: String)
    fun setWebSessionLoading(loading: Boolean)
    fun showError(error: String)
    fun showErrorDialog(error: String)
    fun showProgress()
    fun showToast(message: String)
}