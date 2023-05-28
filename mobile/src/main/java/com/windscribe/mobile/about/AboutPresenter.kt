/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.about

interface AboutPresenter {
    fun init()
    val isHapticFeedbackEnabled: Boolean
    fun onAboutClick()
    fun onBlogClick()
    fun onJobsClick()
    fun onPrivacyClick()
    fun onStatusClick()
    fun onTermsClick()
    fun onViewLicenceClick()
}