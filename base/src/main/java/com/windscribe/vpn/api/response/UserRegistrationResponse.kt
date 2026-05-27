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
class UserRegistrationResponse {
    @SerializedName("billing_plan_id")
    @Expose
    val billingPlanId: Int? = null

    @SerializedName("is_premium")
    @Expose
    val isPremium: Int? = null

    @SerializedName("loc_rev")
    @Expose
    val locRev: String? = null

    @SerializedName("loc_hash")
    @Expose
    val locationHash: String? = null

    @SerializedName("reg_date")
    @Expose
    val regDate: String? = null

    @SerializedName("secure_links_secret")
    @Expose
    val secureLinksSecret: String? = null

    @SerializedName("session_auth_hash")
    @Expose
    val sessionAuthHash: String? = null

    @SerializedName("traffic_max")
    @Expose
    val trafficMax: String? = null

    @SerializedName("traffic_used")
    @Expose
    val trafficUsed: String? = null

    @SerializedName("status")
    @Expose
    val userAccountStatus: Int? = null

    @SerializedName("user_id")
    @Expose
    val userId: String? = null

    @SerializedName("username")
    @Expose
    private val userNameValue: String? = null

    val username: String
        get() = userNameValue ?: "na"

    val isInGhostMode: Boolean
        get() = username == "na"

    override fun toString(): String =
        "UserRegistrationResponse{" +
            "session_auth_hash=[REDACTED]" +
            ", username='" + userNameValue + '\'' +
            ", user_id='" + userId + '\'' +
            ", traffic_used='" + trafficUsed + '\'' +
            ", traffic_max='" + trafficMax + '\'' +
            ", status='" + userAccountStatus + '\'' +
            ", billing_plan_id='" + billingPlanId + '\'' +
            ", is_premium='" + isPremium + '\'' +
            ", reg_date='" + regDate + '\'' +
            ", loc_rev='" + locRev + '\'' +
            ", loc_hash='" + locationHash + '\'' +
            ", secure_links_secret=[REDACTED]" +
            '}'
}
