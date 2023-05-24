/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.customviews

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.Nullable
import androidx.constraintlayout.widget.ConstraintLayout
import com.windscribe.tv.R

class SettingFocusAware : ConstraintLayout {
    private val contentIds = intArrayOf(
        R.id.label, R.id.auto_connection, R.id.planContainer,
        R.id.emailContainer,
        R.id.confirmContainer, R.id.sortList, R.id.languageList
    )
    private var currentFragment = 0
    private val headerIds = intArrayOf(R.id.general, R.id.account, R.id.connection, R.id.debug_view)

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(
        context, attrs
    )

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    )

    override fun focusSearch(focused: View, direction: Int): View? {
        return if (direction == FOCUS_LEFT && contentIds.contains(focused.id)) {
            findViewById(headerIds[currentFragment])
        } else {
            return super.focusSearch(focused, direction) ?: focused
        }
    }

    fun setCurrentFragment(currentFragment: Int) {
        this.currentFragment = currentFragment
    }
}