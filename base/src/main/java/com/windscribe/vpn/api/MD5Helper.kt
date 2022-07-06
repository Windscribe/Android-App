/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.api

import com.windscribe.vpn.exceptions.WindScribeException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object MD5Helper {

    @JvmStatic
    fun md5(rawPassword: CharSequence): String {
        val hexString = StringBuilder()
        return try {
            val md = MessageDigest.getInstance("MD5")
            md.update(rawPassword.toString().toByteArray())
            val byteData = md.digest()
            for (aByteData in byteData) {
                val hex = Integer.toHexString(0xff and aByteData.toInt())
                if (hex.length == 1) {
                    hexString.append("0")
                }
                hexString.append(hex)
            }
            hexString.toString()
        } catch (e: NoSuchAlgorithmException) {
            throw WindScribeException("Failed to create md5 hash.")
        }
    }
}
