package com.windscribe.vpn.commonutils

import ch.qos.logback.classic.pattern.ClassicConverter
import ch.qos.logback.classic.spi.ILoggingEvent

class LowerCaseLevelConverter: ClassicConverter() {
    override fun convert(event: ILoggingEvent): String {
        return event.level.toString().lowercase()
    }
}