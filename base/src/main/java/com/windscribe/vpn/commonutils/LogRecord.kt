package com.windscribe.vpn.commonutils

import com.google.gson.JsonParser

data class LogRecord(
    val time: String = "",
    val date: String = "",
    val level: String = "",
    val module: String = "",
    val thread: String = "",
    val connectionId: String? = null,
    val message: String = "",
    val raw: String? = null,
)

private const val DATE_LENGTH = 10

fun parseLogLine(line: String): LogRecord {
    val record =
        runCatching {
            val json = JsonParser.parseString(line).asJsonObject
            val stamp = json["tm"].asString
            LogRecord(
                date = stamp.take(DATE_LENGTH),
                time = stamp.drop(DATE_LENGTH + 1),
                level = json["lvl"].asString,
                module = json["mod"].asString,
                thread = json["thr"].asString,
                connectionId = json["cid"]?.asString,
                message = json["msg"].asString,
            )
        }.getOrNull()
    return record ?: LogRecord(raw = line)
}

enum class LogLevel(
    val token: String,
) {
    DEBUG("debug"),
    INFO("info"),
    WARN("warn"),
    ERROR("error"),
}

fun LogRecord.matches(
    minLevel: LogLevel,
    query: String,
): Boolean {
    if (raw != null) {
        return minLevel == LogLevel.DEBUG && raw.contains(query, ignoreCase = true)
    }
    val own = LogLevel.entries.firstOrNull { it.token == level }
    if (own != null && own < minLevel) {
        return false
    }
    return query.isEmpty() || shows(query)
}

private fun LogRecord.shows(query: String): Boolean =
    time.contains(query, ignoreCase = true) ||
        module.contains(query, ignoreCase = true) ||
        thread.contains(query, ignoreCase = true) ||
        connectionId?.contains(query, ignoreCase = true) == true ||
        message.contains(query, ignoreCase = true) ||
        date.contains(query, ignoreCase = true)
