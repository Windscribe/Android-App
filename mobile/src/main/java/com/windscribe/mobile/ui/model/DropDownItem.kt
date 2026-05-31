package com.windscribe.mobile.ui.model

import androidx.annotation.StringRes

data class DropDownItem(
    val id: Int,
    @param:StringRes val title: Int = 0,
    val label: String = "",
)

data class ThemeItem(
    val id: String,
    @param:StringRes val title: Int,
)

data class DropDownStringItem(
    val key: String,
    val label: String? = key,
    val description: String? = null,
)
