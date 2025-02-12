/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.model

import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.constants.UserStatusConstants
import com.windscribe.vpn.model.User.AccountStatus.*
import com.windscribe.vpn.model.User.EmailStatus.*
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class User(private val sessionResponse: UserSessionResponse) {

    val alcList: String?
        get() {
            val alcListString = StringBuilder()
            val alcList = sessionResponse.alcList
            if (alcList != null) {
                for (alc in alcList) {
                    alcListString.append(alc).append(",")
                }
                return alcListString.toString()
            }
            return null
        }
    val isOurIp: Boolean
        get() = sessionResponse.ourIp != null && sessionResponse.ourIp == 0
    val locationRevision: String
        get() = sessionResponse.locationRevision
    val locationHash: String
        get() = sessionResponse.locationHash
    val email: String?
        get() = sessionResponse.userEmail
    val userName: String
        get() = if (sessionResponse.userName == null) "na" else sessionResponse.userName
    val sipCount: Int
        get() {
            return if (sessionResponse.sip != null) {
                sessionResponse.sip.count
            } else {
                0
            }
        }
    val dataUsed: Long
        get() {
            return sessionResponse.trafficUsed.toLong()
        }
    val maxData: Long
        get() = sessionResponse.trafficMax.toLong()
    val isPro: Boolean
        get() = sessionResponse.isPremium == 1
    val userStatusInt: Int
        get() = sessionResponse.isPremium
    val dataLeft: Float
        get() {
            if (dataUsed > maxData) {
                return 0F
            }
            return (maxData - dataUsed) / UserStatusConstants.GB_DATA.toFloat()
        }

    enum class AccountStatus {
        Okay, Expired, Banned
    }

    val accountStatus: AccountStatus
        get() {
            return when (sessionResponse.userAccountStatus) {
                1 -> Okay
                2 -> Expired
                3 -> Banned
                else -> Okay
            }
        }
    val accountStatusToInt: Int
        get() {
            return when (accountStatus) {
                Okay -> 1
                Expired -> 2
                Banned -> 3
            }
        }

    enum class EmailStatus {
        NoEmail, EmailProvided, Confirmed
    }

    val emailStatus: EmailStatus
        get() {
            return email?.let {
                if (sessionResponse.emailStatus == 1) {
                    return@let Confirmed
                } else {
                    return@let EmailProvided
                }
            } ?: kotlin.run { return NoEmail }
        }

    val isGhost: Boolean
        get() = userName == "na"

    val expiryDate: String?
        get() = sessionResponse.premiumExpiryDate
    val resetDate: String?
        get() = sessionResponse.lastResetDate

    val isAlaCarteUnlimitedPlan: Boolean
        get() = sessionResponse.billingPlanID == -9

    val daysRegisteredSince: Long
        get() {
            val registrationDate = sessionResponse.registrationDate
            val difference = Date().time - registrationDate.toLong() * 1000L
            return TimeUnit.DAYS.convert(difference, TimeUnit.MILLISECONDS)
        }

    fun nextResetDate(): String? {
        return if (resetDate != null) {
            try {
                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val lastResetDate = formatter.parse(resetDate)
                val c = Calendar.getInstance()
                c.time = Objects.requireNonNull(lastResetDate)
                c.add(Calendar.MONTH, 1)
                val nextResetDate = c.time
                formatter.format(nextResetDate)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    override fun toString(): String {
        return "Account Status: $accountStatus | User Status: $userStatusInt | Ghost $isGhost | Email Status: $emailStatus | Sip count $sipCount"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        if (alcList != other.alcList) return false
        if (isOurIp != other.isOurIp) return false
        if (locationRevision != other.locationRevision) return false
        if (locationHash != other.locationHash) return false
        if (email != other.email) return false
        if (userName != other.userName) return false
        if (sipCount != other.sipCount) return false
        if (dataUsed != other.dataUsed) return false
        if (maxData != other.maxData) return false
        if (isPro != other.isPro) return false
        if (userStatusInt != other.userStatusInt) return false
        if (dataLeft != other.dataLeft) return false
        if (accountStatus != other.accountStatus) return false
        if (accountStatusToInt != other.accountStatusToInt) return false
        if (emailStatus != other.emailStatus) return false
        if (isGhost != other.isGhost) return false
        if (expiryDate != other.expiryDate) return false
        if (resetDate != other.resetDate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = alcList?.hashCode() ?: 0
        result = 31 * result + isOurIp.hashCode()
        result = 31 * result + locationRevision.hashCode()
        result = 31 * result + locationHash.hashCode()
        result = 31 * result + (email?.hashCode() ?: 0)
        result = 31 * result + userName.hashCode()
        result = 31 * result + sipCount
        result = 31 * result + (dataUsed?.hashCode() ?: 0)
        result = 31 * result + maxData.hashCode()
        result = 31 * result + isPro.hashCode()
        result = 31 * result + userStatusInt
        result = 31 * result + (dataLeft?.hashCode() ?: 0)
        result = 31 * result + accountStatus.hashCode()
        result = 31 * result + accountStatusToInt
        result = 31 * result + emailStatus.hashCode()
        result = 31 * result + isGhost.hashCode()
        result = 31 * result + (expiryDate?.hashCode() ?: 0)
        result = 31 * result + (resetDate?.hashCode() ?: 0)
        return result
    }

}
