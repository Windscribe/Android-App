package com.windscribe.mobile.ui.model

data class AccountStatusDialogData(
    val title: String,
    val icon: Int,
    val description: String,
    val showSecondaryButton: Boolean,
    val secondaryText: String,
    val showPrimaryButton: Boolean,
    val primaryText: String
) : java.io.Serializable