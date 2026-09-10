/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.commonutils

import androidx.annotation.Keep
import ch.qos.logback.classic.pattern.ThrowableHandlingConverter
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxyUtil

/**
 * Renders the message as a JSON string body. A [ThrowableHandlingConverter] so logback does not
 * append its own after the pattern's newline, writing a bare stack trace between records.
 */
@Keep
class JsonMessageConverter : ThrowableHandlingConverter() {
    override fun convert(event: ILoggingEvent): String {
        val message = event.formattedMessage ?: ""
        val throwable = event.throwableProxy?.let { "\n" + ThrowableProxyUtil.asString(it) } ?: ""
        return escape(message + throwable)
    }

    private fun escape(raw: String): String {
        val out = StringBuilder(raw.length)
        for (c in raw) {
            when {
                c == '"' -> out.append("\\\"")
                c == '\\' -> out.append("\\\\")
                c == '\n' -> out.append("\\n")
                c == '\r' -> out.append("\\r")
                c == '\t' -> out.append("\\t")
                c == '\b' -> out.append("\\b")
                c == '\u000C' -> out.append("\\f")
                c < ' ' -> out.append("\\u").append(c.code.toString(16).padStart(4, '0'))
                else -> out.append(c)
            }
        }
        return out.toString()
    }
}
