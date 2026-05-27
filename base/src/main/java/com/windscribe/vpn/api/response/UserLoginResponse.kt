/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Created by Mustafizur on 2017-09-08.
 */
@Keep
class UserLoginResponse {

    @SerializedName("alc")
    @Expose
    val alcList: List<String>? = null

    @SerializedName("billing_plan_id")
    @Expose
    val billingPlanID: Int? = null

    @SerializedName("email_status")
    @Expose
    val emailStatus: String? = null

    @SerializedName("is_premium")
    @Expose
    val isPremium: Int? = null

    @SerializedName("loc_hash")
    @Expose
    val locationHash: String? = null

    @SerializedName("loc_rev")
    @Expose
    val locationRevision: String? = null

    @SerializedName("reg_date")
    @Expose
    val registrationDate: String? = null

    @SerializedName("session_auth_hash")
    @Expose
    val sessionAuthHash: String? = null

    @SerializedName("sip")
    @Expose
    val sip: Sip? = null

    @SerializedName("traffic_max")
    @Expose
    val trafficMax: String? = null

    @SerializedName("traffic_used")
    @Expose
    val trafficUsed: String? = null

    @SerializedName("status")
    @Expose
    val userAccountStatus: Int? = null

    @SerializedName("email")
    @Expose
    val userEmail: String? = null

    @SerializedName("user_id")
    @Expose
    val userID: String? = null

    @SerializedName("username")
    @Expose
    private val userNameValue: String? = null

    val userName: String
        get() = userNameValue ?: "na"

    val isInGhostMode: Boolean
        get() = userName == "na"

    override fun toString(): String {
        return "UserLoginResponse{" +
                "session_auth_hash=[REDACTED]" +
                ", username='" + userNameValue + '\'' +
                ", user_id='" + userID + '\'' +
                ", traffic_used='" + trafficUsed + '\'' +
                ", traffic_max='" + trafficMax + '\'' +
                ", status='" + userAccountStatus + '\'' +
                ", email='" + userEmail + '\'' +
                ", email_status='" + emailStatus + '\'' +
                ", billing_plan_id='" + billingPlanID + '\'' +
                ", is_premium='" + isPremium + '\'' +
                ", reg_date='" + registrationDate + '\'' +
                ", loc_rev='" + locationRevision + '\'' +
                ", loc_hash='" + locationHash + '\'' +
                '}'
    }
}
