/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.commonutils

import android.text.InputFilter
import android.text.Spanned
import java.lang.NumberFormatException

class InputFilterMinMax(min: String, max: String) : InputFilter {
    private val max: Int = max.toInt()
    private val min: Int = min.toInt()
    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dStart: Int,
        dEnd: Int
    ): CharSequence? {
        try {
            val input = (dest.toString() + source.toString()).toInt()
            if (isInRange(min, max, input)) {
                return null
            }
        } catch (ignored: NumberFormatException) {
        }
        return ""
    }

    private fun isInRange(a: Int, b: Int, c: Int): Boolean {
        return if (b > a) c in a..b else c in b..a
    }
}