package com.windscribe.mobile.base

import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import com.windscribe.mobile.R

open class BaseDialogFragment : DialogFragment() {

    fun setViewWithCutout(view: View) {
        val window = activity?.window
        window?.setFormat(PixelFormat.RGBA_8888)
        var boundingRect: List<Rect> = ArrayList()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val displayCutout = window?.decorView?.rootWindowInsets?.displayCutout
            if (displayCutout != null) {
                boundingRect = displayCutout.boundingRects
            }
        }
        if (boundingRect.isNotEmpty()) {
            val boundingRectHeight = boundingRect[0].height()
            val backButton = view.findViewById<ConstraintLayout>(R.id.nav_bar)
            backButton?.setPaddingRelative(
                backButton.paddingStart,
                backButton.paddingTop + boundingRectHeight / 2, backButton.paddingEnd,
                backButton.paddingBottom
            )
        }
    }
}