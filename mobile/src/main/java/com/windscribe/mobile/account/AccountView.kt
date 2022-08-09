/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.account

interface AccountView {
    fun goToConfirmEmailActivity()
    fun goToEmailActivity()
    fun hideProgress()
    fun openEditAccountInBrowser(url: String)
    fun openUpgradeActivity()
    fun setActivityTitle(title: String)
    fun setEmail(email: String, textColor: Int, labelColor: Int)
    fun setEmailConfirm(
        emailConfirm: String,
        warningText: String,
        emailColor: Int,
        emailLabelColor: Int,
        infoIcon: Int,
        containerBackground: Int
    )

    fun setEmailConfirmed(
        emailConfirm: String,
        warningText: String,
        emailColor: Int,
        emailLabelColor: Int,
        infoIcon: Int,
        containerBackground: Int
    )

    fun setPlanName(planName: String)
    fun setDataLeft(dataLeft: String)
    fun setResetDate(resetDateLabel: String, resetDate: String)
    fun setUsername(username: String)
    fun setWebSessionLoading(show: Boolean)
    fun setupLayoutForFreeUser(upgradeText: String)
    fun setupLayoutForGhostMode(proUser: Boolean)
    fun setupLayoutForPremiumUser(upgradeText: String)
    fun showEnterCodeDialog()
    fun showErrorDialog(error: String)
    fun showErrorMessage(errorMessage: String)
    fun showProgress(progressText: String)
    fun showSuccessDialog(message: String)
}