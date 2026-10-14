package com.windscribe.vpn.commonutils

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

/**
 * One line of the pattern in assets/logback.xml, kept in step by hand:
 * `2026-09-21 12:54:26.608 warn  [Thread-23] wsnet-wrapper #e52bf6d5 - Curl request failed`
 */
private val RECORD =
    Regex(
        """(\d{4}-\d{2}-\d{2}) (\d{2}:\d{2}:\d{2}\.\d{3}) (\w+) +\[([^\]]*)\] (\S+?)(?: #(\S+))? - (.*)""",
    )

fun parseLogLine(line: String): LogRecord {
    val fields = RECORD.matchEntire(line)?.groupValues ?: return LogRecord(raw = line)
    return LogRecord(
        date = fields[1],
        time = fields[2],
        level = fields[3],
        thread = fields[4],
        module = fields[5],
        connectionId = fields[6].takeIf { it.isNotEmpty() },
        message = fields[7],
    )
}

/**
 * A stack trace lands on its own lines after the record it belongs to, so a line that parses as
 * nothing gets folded back into the record above it. Only a log that never parsed at all - charon's
 * or wireguard's, which the debug tab shows in place of ours - stays a run of raw lines.
 */
fun parseLogLines(lines: List<String>): List<LogRecord> {
    val records = ArrayList<LogRecord>(lines.size)
    for (line in lines) {
        val record = parseLogLine(line)
        val previous = records.lastOrNull()
        if (record.raw != null && previous != null && previous.raw == null) {
            records[records.lastIndex] = previous.copy(message = previous.message + "\n" + line)
        } else {
            records.add(record)
        }
    }
    return records
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
