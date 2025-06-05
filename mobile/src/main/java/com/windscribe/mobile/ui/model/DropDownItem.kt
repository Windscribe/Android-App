package com.windscribe.mobile.ui.model

import androidx.annotation.StringRes

data class DropDownItem(val id: Int, @StringRes val title: Int = 0, val label: String = "")
data class ThemeItem(val id: String, @StringRes val title: Int)
data class DropDownStringItem(val key: String, val label: String? = key)