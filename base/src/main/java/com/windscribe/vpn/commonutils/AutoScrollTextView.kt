/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.commonutils

import android.content.Context
import android.text.Selection
import android.text.Spannable
import android.text.method.MovementMethod
import android.text.method.ScrollingMovementMethod
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import com.windscribe.vpn.commonutils.AutoScrollTextView.CursorScrollingMovementMethod

class AutoScrollTextView : AppCompatEditText {
    /**
     * Moves cursor when scrolled so it doesn't auto-scroll on configuration changes.
     */
    private class CursorScrollingMovementMethod : ScrollingMovementMethod() {
        override fun onTouchEvent(
            widget: TextView,
            buffer: Spannable,
            event: MotionEvent
        ): Boolean {
            widget.moveCursorToVisibleOffset()
            return super.onTouchEvent(widget, buffer, event)
        }
    }

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null) : super(
        context, attrs
    )

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context, attrs, defStyle
    )

    override fun append(text: CharSequence, start: Int, end: Int) {
        super.append(text, start, end)
        scrollToEnd()
    }

    override fun onInitializeAccessibilityEvent(event: AccessibilityEvent) {
        super.onInitializeAccessibilityEvent(event)
        event.className = AutoScrollTextView::class.java.name
    }

    private fun scrollToEnd() {
        val editable = text
        editable?.let {
            Selection.setSelection(it, it.length)
        }
    }

    override fun setText(text: CharSequence, type: BufferType) {
        super.setText(text, type)
        scrollToEnd()
    }

    override fun getDefaultEditable(): Boolean {
        return false
    }

    override fun getDefaultMovementMethod(): MovementMethod {
        return CursorScrollingMovementMethod()
    }
}