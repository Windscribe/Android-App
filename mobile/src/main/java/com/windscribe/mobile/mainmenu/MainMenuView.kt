/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.mainmenu

interface MainMenuView {
    fun goRobertSettingsActivity()
    fun gotToHelp()
    fun gotoAboutActivity()
    fun gotoAccountActivity()
    fun gotoConnectionSettingsActivity()
    fun gotoGeneralSettingsActivity()
    fun gotoLoginRegistrationActivity()
    fun openConfirmEmailActivity()
    fun openUpgradeActivity()
    fun resetAllTextResources(
        activityTitle: String,
        general: String,
        account: String,
        connection: String,
        helpMe: String,
        signOut: String,
        about: String,
        robert: String
    )

    fun setActionButtonVisibility(
        loginButtonVisibility: Int,
        addEmailButtonVisibility: Int,
        setUpAccountButtonVisibility: Int,
        confirmEmailButtonVisibility: Int
    )

    fun setActivityTitle(title: String)
    fun setLoginButtonVisibility(visibility: Int)
    fun setupLayoutForFreeUser(dataLeft: String, upgradeLabel: String, color: Int)
    fun setupLayoutForPremiumUser()
    fun showLogoutAlert()
    fun startAccountSetUpActivity()
    fun startAddEmailActivity()
    fun startLoginActivity()
    fun showShareLinkDialog()
    fun showShareLinkOption()
    fun showAdvanceParamsActivity()
}