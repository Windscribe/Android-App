package com.windscribe.vpn.commonutils

import androidx.annotation.Keep
import ch.qos.logback.classic.pattern.ClassicConverter
import ch.qos.logback.classic.spi.ILoggingEvent

@Keep
class LowerCaseLevelConverter : ClassicConverter() {
    override fun convert(event: ILoggingEvent): String = event.level.toString().lowercase()
}
