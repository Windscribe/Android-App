/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.api

import com.windscribe.vpn.api.response.AddEmailResponse
import com.windscribe.vpn.api.response.ApiErrorResponse
import com.windscribe.vpn.api.response.BestLocationResponse
import com.windscribe.vpn.api.response.BillingPlanResponse
import com.windscribe.vpn.api.response.ClaimAccountResponse
import com.windscribe.vpn.api.response.GenericResponseClass
import com.windscribe.vpn.api.response.GenericSuccess
import com.windscribe.vpn.api.response.GetMyIpResponse
import com.windscribe.vpn.api.response.NewsFeedNotification
import com.windscribe.vpn.api.response.PortMapResponse
import com.windscribe.vpn.api.response.RegToken
import com.windscribe.vpn.api.response.RobertSettingsResponse
import com.windscribe.vpn.api.response.ServerCredentialsResponse
import com.windscribe.vpn.api.response.StaticIPResponse
import com.windscribe.vpn.api.response.TicketResponse
import com.windscribe.vpn.api.response.UserLoginResponse
import com.windscribe.vpn.api.response.UserRegistrationResponse
import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.api.response.VerifyExpressLoginResponse
import com.windscribe.vpn.api.response.WebSession
import com.windscribe.vpn.api.response.WgConnectResponse
import com.windscribe.vpn.api.response.WgInitResponse
import com.windscribe.vpn.api.response.XPressLoginCodeResponse
import com.windscribe.vpn.api.response.XPressLoginVerifyResponse
import io.reactivex.Single

interface IApiCallManager {

    fun addUserEmailAddress(extraParams: Map<String, String>? = null): Single<GenericResponseClass<AddEmailResponse?, ApiErrorResponse?>>
    fun checkConnectivityAndIpAddress(extraParams: Map<String, String>? = null): Single<GenericResponseClass<String?, ApiErrorResponse?>>
    fun claimAccount(extraParams: Map<String, String>? = null): Single<GenericResponseClass<ClaimAccountResponse?, ApiErrorResponse?>>

    //    fun getAccessIp(accessIpMap: Map<String, String>? = null): Single<GenericResponseClass<AccessIpResponse?, ApiErrorResponse?>>
    fun getBestLocation(extraParams: Map<String, String>? = null): Single<GenericResponseClass<BestLocationResponse?, ApiErrorResponse?>>
    fun getBillingPlans(extraParams: Map<String, String>? = null): Single<GenericResponseClass<BillingPlanResponse?, ApiErrorResponse?>>
    fun getMyIp(extraParams: Map<String, String>? = null): Single<GenericResponseClass<GetMyIpResponse?, ApiErrorResponse?>>
    fun getNotifications(extraParams: Map<String, String>? = null): Single<GenericResponseClass<NewsFeedNotification?, ApiErrorResponse?>>
    fun getPortMap(extraParams: Map<String, String>? = null): Single<GenericResponseClass<PortMapResponse?, ApiErrorResponse?>>
    fun getReg(extraParams: Map<String, String>? = null): Single<GenericResponseClass<RegToken?, ApiErrorResponse?>>
    fun getServerConfig(extraParams: Map<String, String>? = null): Single<GenericResponseClass<String?, ApiErrorResponse?>>
    fun getServerCredentials(extraParams: Map<String, String>? = null): Single<GenericResponseClass<ServerCredentialsResponse?, ApiErrorResponse?>>
    fun getServerCredentialsForIKev2(extraParams: Map<String, String>? = null): Single<GenericResponseClass<ServerCredentialsResponse?, ApiErrorResponse?>>
    fun getServerList(
            extraParams: Map<String, String>? = null,
            billingPlan: String?,
            locHash: String?,
            alcList: String?,
            overriddenCountryCode: String?
    ): Single<GenericResponseClass<String?, ApiErrorResponse?>>

    fun getSessionGeneric(extraParams: Map<String, String>? = null, protect: Boolean = false): Single<GenericResponseClass<UserSessionResponse?, ApiErrorResponse?>>
    fun getSessionGeneric(extraParams: Map<String, String>? = null): Single<GenericResponseClass<UserSessionResponse?, ApiErrorResponse?>>
    fun getSessionGenericInConnectedState(extraParams: Map<String, String>? = null): Single<GenericResponseClass<UserSessionResponse?, ApiErrorResponse?>>
    fun getStaticIpList(extraParams: Map<String, String>? = null): Single<GenericResponseClass<StaticIPResponse?, ApiErrorResponse?>>
    fun logUserIn(extraParams: Map<String, String>? = null): Single<GenericResponseClass<UserLoginResponse?, ApiErrorResponse?>>
    fun getWebSession(extraParams: Map<String, String>? = null): Single<GenericResponseClass<WebSession?, ApiErrorResponse?>>
    fun recordAppInstall(extraParams: Map<String, String>? = null): Single<GenericResponseClass<String?, ApiErrorResponse?>>
    fun resendUserEmailAddress(extraParams: Map<String, String>? = null): Single<GenericResponseClass<AddEmailResponse?, ApiErrorResponse?>>
    fun sendTicket(map: Map<String, String>? = null): Single<GenericResponseClass<TicketResponse?, ApiErrorResponse?>>
    fun signUserIn(extraParams: Map<String, String>? = null): Single<GenericResponseClass<UserRegistrationResponse?, ApiErrorResponse?>>
    fun verifyPayment(extraParams: Map<String, String>? = null): Single<GenericResponseClass<String?, ApiErrorResponse?>>
    fun verifyExpressLoginCode(extraParams: Map<String, String>? = null): Single<GenericResponseClass<VerifyExpressLoginResponse?, ApiErrorResponse?>>
    fun generateXPressLoginCode(extraParams: Map<String, String>? = null): Single<GenericResponseClass<XPressLoginCodeResponse?, ApiErrorResponse?>>
    fun verifyXPressLoginCode(extraParams: Map<String, String>? = null): Single<GenericResponseClass<XPressLoginVerifyResponse?, ApiErrorResponse?>>
    fun postDebugLog(extraParams: Map<String, String>? = null): Single<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>>
    fun postPromoPaymentConfirmation(extraParams: Map<String, String>? = null): Single<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>>
    fun getRobertSettings(extraParams: Map<String, String>? = null): Single<GenericResponseClass<RobertSettingsResponse?, ApiErrorResponse?>>
    fun updateRobertSettings(extraParams: Map<String, String>? = null): Single<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>>
    fun syncRobert(extraParams: Map<String, String>? = null): Single<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>>
    fun deleteSession(extraParams: Map<String, String>? = null): Single<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>>
    fun wgInit(extraParams: Map<String, String>? = null, protect: Boolean): Single<GenericResponseClass<WgInitResponse?, ApiErrorResponse?>>
    fun wgConnect(extraParams: Map<String, String>? = null, protect: Boolean): Single<GenericResponseClass<WgConnectResponse?, ApiErrorResponse?>>
    fun sendDecoyTraffic(url: String, data: String, sizeToReceive: String?): Single<GenericResponseClass<String?, ApiErrorResponse?>>
}
