/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Keep
class UserSessionResponse {
    @SerializedName("alc")
    @Expose
    var alcList: List<String>? = null

    @SerializedName("billing_plan_id")
    @Expose
    var billingPlanID: Int? = null

    @SerializedName("email_status")
    @Expose
    var emailStatus: Int? = null

    @SerializedName("ignore_udp_tests")
    @Expose
    var ignoreUdpTest: Int? = null

    @SerializedName("is_premium")
    @Expose
    var isPremium: Int? = null

    @SerializedName("last_reset")
    @Expose
    var lastResetDate: String? = null

    @SerializedName("loc_hash")
    @Expose
    var locationHash: String? = null

    @SerializedName("loc_rev")
    @Expose
    var locationRevision: String? = null

    @SerializedName("our_addr")
    @Expose
    var ourAddress: String? = null

    @SerializedName("our_dc")
    @Expose
    var ourDc: Int? = null

    @SerializedName("our_ip")
    @Expose
    var ourIp: Int? = null

    @SerializedName("our_location")
    @Expose
    var ourLocation: String? = null

    @SerializedName("premium_expiry_date")
    @Expose
    var premiumExpiryDate: String? = null

    @SerializedName("rebill")
    @Expose
    var reBill: Int? = null

    @SerializedName("reg_date")
    @Expose
    var registrationDate: String? = null

    @SerializedName("sip")
    @Expose
    var sip: Sip? = null

    @SerializedName("traffic_max")
    @Expose
    var trafficMax: String? = null

    @SerializedName("traffic_used")
    @Expose
    var trafficUsed: String? = null

    @SerializedName("status")
    @Expose
    var userAccountStatus: Int? = null

    @SerializedName("email")
    @Expose
    var userEmail: String? = null

    @SerializedName("user_id")
    @Expose
    var userID: String? = null

    @SerializedName("username")
    @Expose
    var userName: String? = null

    @SerializedName("inventory_namespace")
    @Expose
    var inventoryNamespace: String? = null

    @SerializedName("user_class")
    @Expose
    var userClass: Int? = null

    @SerializedName("server_inventory")
    @Expose
    var serverInventory: ServerInventory? = null

    fun sipCount(): Int = sip?.count ?: 0

    override fun toString(): String =
        "UserSessionResponse{" +
            "ourIp=" + ourIp +
            ", ourLocation='" + ourLocation + '\'' +
            ", ourDc=" + ourDc +
            ", ourAddress='" + ourAddress + '\'' +
            ", reBill=" + reBill +
            ", ignoreUdpTest=" + ignoreUdpTest +
            ", trafficUsed='" + trafficUsed + '\'' +
            ", trafficMax='" + trafficMax + '\'' +
            ", userAccountStatus=" + userAccountStatus +
            ", emailStatus=" + emailStatus +
            ", billingPlanID=" + billingPlanID +
            ", isPremium=" + isPremium +
            ", registrationDate='" + registrationDate + '\'' +
            ", lastResetDate='" + lastResetDate + '\'' +
            ", locationRevision='" + locationRevision + '\'' +
            ", locationHash='" + locationHash + '\'' +
            ", premiumExpiryDate='" + premiumExpiryDate + '\'' +
            ", alcList=" + alcList +
            ", sip=" + sip +
            ", serverInventory=" + serverInventory +
            '}'
}
