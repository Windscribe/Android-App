/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.errormodel

import java.io.PrintWriter
import java.io.StringWriter

class WindError private constructor() {
    private val sw = StringWriter()
    fun convertErrorToString(e: Exception): String {
        e.printStackTrace(PrintWriter(sw))
        return if (sw.toString().length > 2000) {
            sw.toString().substring(0, 1999)
        } else {
            sw.toString()
        }
    }

    fun convertThrowableToString(e: Throwable?): String {
        e?.printStackTrace(PrintWriter(sw))
        return if (sw.toString().length > 2000) {
            sw.toString().substring(0, 1999)
        } else {
            sw.toString()
        }
    }

    companion object {
        @JvmStatic
        val instance = WindError()
    }
}
