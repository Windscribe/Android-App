package com.windscribe.mobile.dialogs

import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import com.windscribe.mobile.R

open class FullScreenDialog : DialogFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenWithNoStatusBar)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

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
            val backButton = view.findViewById<ConstraintLayout>(R.id.nav_button)
            backButton?.setPaddingRelative(
                backButton.paddingStart,
                backButton.paddingTop + boundingRectHeight / 2,
                backButton.paddingEnd,
                backButton.paddingBottom
            )
        }
    }
}