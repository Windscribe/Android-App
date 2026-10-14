/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.commonutils

import androidx.annotation.Keep
import ch.qos.logback.classic.pattern.ClassicConverter
import ch.qos.logback.classic.spi.ILoggingEvent
import java.util.UUID

/**
 * The connection attempt a log line belongs to, stamped on every record.
 *
 * Process-wide rather than MDC: the lines worth correlating come from threads the attempt never runs
 * on — native OpenVPN, wsnet, the mux pumps — where a thread-local is always empty. Only one attempt
 * is live at a time. Shortened to the first UUID block; the full id is not written anywhere.
 */
object ConnectionLogContext {
    @Volatile
    private var id: String = ""

    fun set(connectionId: UUID?) {
        id = shorten(connectionId)
    }

    fun clear() {
        id = ""
    }

    fun current(): String = id

    /** The attempt [connectionId] names, or null when every line already carries it. */
    fun differing(connectionId: UUID?): String? = shorten(connectionId).takeIf { it.isNotEmpty() && it != id }

    private fun shorten(connectionId: UUID?): String = connectionId?.toString()?.substringBefore('-') ?: ""
}

/** The whole `#id` field, leading space and all, so a line outside an attempt carries no empty one. */
@Keep
class ConnectionIdConverter : ClassicConverter() {
    override fun convert(event: ILoggingEvent): String {
        val id = ConnectionLogContext.current()
        return if (id.isEmpty()) "" else " #$id"
    }
}
