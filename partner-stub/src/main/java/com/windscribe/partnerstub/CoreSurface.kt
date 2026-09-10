/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.partnerstub

import com.google.gson.Gson
import com.windscribe.vpn.api.response.GenericResponseClass
import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.commonutils.Ext
import com.windscribe.vpn.repository.CallResult
import org.slf4j.LoggerFactory

/**
 * Roots for R8, so shrinking this module exercises the paths a partner reaches.
 *
 * Referenced from nowhere: `-keep` in the AAR's contract is what has to hold these together, and
 * anything R8 strips or cannot resolve fails the build rather than the partner's runtime.
 */
@Suppress("unused")
object CoreSurface {
    /** inet.ipaddr, whose `<clinit>` needs its package left intact. */
    fun addressCheck(candidate: String): Boolean = Util.validIpAddress(candidate)

    /** gson over a generic response: needs Signature, *Annotation* and the model keeps. */
    fun parseSession(json: String): UserSessionResponse? =
        Gson()
            .fromJson(json, GenericResponseClass::class.java)
            ?.dataClass as? UserSessionResponse

    /** The reflection [Ext] performs over [CallResult]. */
    fun resultShape(): String = CallResult.Error(code = 0).toString()

    /** logback resolves its appenders, encoders and converters from assets/logback.xml by name. */
    fun log(): Unit = LoggerFactory.getLogger("partner-stub").info("core surface referenced")
}
