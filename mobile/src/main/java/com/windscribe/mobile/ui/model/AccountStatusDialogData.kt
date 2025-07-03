package com.windscribe.mobile.ui.model

data class AccountStatusDialogData(
    val title: String,
    val icon: Int,
    val description: String,
    val showSkipButton: Boolean,
    val skipText: String,
    val showUpgradeButton: Boolean,
    val upgradeText: String,
    val bannedLayout: Boolean = false
) : java.io.Serializable