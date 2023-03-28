package com.windscribe.vpn.api

import com.windscribe.vpn.api.response.*

interface IApiCallManagerV2 {
    suspend fun addUserEmailAddress(extraParams: Map<String, String>? = null): Result<GenericResponseClass<AddEmailResponse?, ApiErrorResponse?>>
    suspend fun checkConnectivityAndIpAddress(extraParams: Map<String, String>? = null): Result<GenericResponseClass<String?, ApiErrorResponse?>>
    suspend fun getConnectedIp(): Result<GenericResponseClass<String?, ApiErrorResponse?>>
    suspend fun claimAccount(extraParams: Map<String, String>? = null): Result<GenericResponseClass<ClaimAccountResponse?, ApiErrorResponse?>>
    suspend fun getBestLocation(extraParams: Map<String, String>? = null): Result<GenericResponseClass<BestLocationResponse?, ApiErrorResponse?>>
    suspend fun getBillingPlans(extraParams: Map<String, String>? = null): Result<GenericResponseClass<BillingPlanResponse?, ApiErrorResponse?>>
    suspend fun getMyIp(extraParams: Map<String, String>? = null): Result<GenericResponseClass<GetMyIpResponse?, ApiErrorResponse?>>
    suspend fun getNotifications(extraParams: Map<String, String>? = null): Result<GenericResponseClass<NewsFeedNotification?, ApiErrorResponse?>>
    suspend fun getPortMap(extraParams: Map<String, String>? = null): Result<GenericResponseClass<PortMapResponse?, ApiErrorResponse?>>
    suspend fun getReg(extraParams: Map<String, String>? = null): Result<GenericResponseClass<RegToken?, ApiErrorResponse?>>
    suspend fun getServerConfig(extraParams: Map<String, String>? = null): Result<GenericResponseClass<String?, ApiErrorResponse?>>
    suspend fun getServerCredentials(extraParams: Map<String, String>? = null): Result<GenericResponseClass<ServerCredentialsResponse?, ApiErrorResponse?>>
    suspend fun getServerCredentialsForIKev2(extraParams: Map<String, String>? = null): Result<GenericResponseClass<ServerCredentialsResponse?, ApiErrorResponse?>>
    suspend fun getServerList(
        extraParams: Map<String, String>? = null,
        billingPlan: String?,
        locHash: String?,
        alcList: String?,
        overriddenCountryCode: String?
    ): Result<GenericResponseClass<String?, ApiErrorResponse?>>

    suspend fun getSessionGeneric(
        extraParams: Map<String, String>? = null, protect: Boolean = false
    ): Result<GenericResponseClass<UserSessionResponse?, ApiErrorResponse?>>

    suspend fun getSessionGeneric(extraParams: Map<String, String>? = null): Result<GenericResponseClass<UserSessionResponse?, ApiErrorResponse?>>
    suspend fun getSessionGenericInConnectedState(extraParams: Map<String, String>? = null): Result<GenericResponseClass<UserSessionResponse?, ApiErrorResponse?>>
    suspend fun getStaticIpList(extraParams: Map<String, String>? = null): Result<GenericResponseClass<StaticIPResponse?, ApiErrorResponse?>>
    suspend fun logUserIn(extraParams: Map<String, String>? = null): Result<GenericResponseClass<UserLoginResponse?, ApiErrorResponse?>>
    suspend fun getWebSession(extraParams: Map<String, String>? = null): Result<GenericResponseClass<WebSession?, ApiErrorResponse?>>
    suspend fun recordAppInstall(extraParams: Map<String, String>? = null): Result<GenericResponseClass<String?, ApiErrorResponse?>>
    suspend fun resendUserEmailAddress(extraParams: Map<String, String>? = null): Result<GenericResponseClass<AddEmailResponse?, ApiErrorResponse?>>
    suspend fun sendTicket(map: Map<String, String>? = null): Result<GenericResponseClass<TicketResponse?, ApiErrorResponse?>>
    suspend fun signUserIn(extraParams: Map<String, String>? = null): Result<GenericResponseClass<UserRegistrationResponse?, ApiErrorResponse?>>
    suspend fun verifyPurchaseReceipt(extraParams: Map<String, String>? = null): Result<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>>
    suspend fun verifyExpressLoginCode(extraParams: Map<String, String>? = null): Result<GenericResponseClass<VerifyExpressLoginResponse?, ApiErrorResponse?>>
    suspend fun generateXPressLoginCode(extraParams: Map<String, String>? = null): Result<GenericResponseClass<XPressLoginCodeResponse?, ApiErrorResponse?>>
    suspend fun verifyXPressLoginCode(extraParams: Map<String, String>? = null): Result<GenericResponseClass<XPressLoginVerifyResponse?, ApiErrorResponse?>>
    suspend fun postDebugLog(extraParams: Map<String, String>? = null): Result<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>>
    suspend fun postPromoPaymentConfirmation(extraParams: Map<String, String>? = null): Result<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>>
    suspend fun getRobertSettings(extraParams: Map<String, String>? = null): Result<GenericResponseClass<RobertSettingsResponse?, ApiErrorResponse?>>
    suspend fun getRobertFilters(extraParams: Map<String, String>? = null): Result<GenericResponseClass<RobertFilterResponse?, ApiErrorResponse?>>
    suspend fun updateRobertSettings(extraParams: Map<String, String>? = null): Result<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>>
    suspend fun syncRobert(extraParams: Map<String, String>? = null): Result<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>>
    suspend fun deleteSession(extraParams: Map<String, String>? = null): Result<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>>
    suspend fun wgInit(
        extraParams: Map<String, String>? = null, protect: Boolean
    ): Result<GenericResponseClass<WgInitResponse?, ApiErrorResponse?>>

    suspend fun wgConnect(
        extraParams: Map<String, String>? = null, protect: Boolean
    ): Result<GenericResponseClass<WgConnectResponse?, ApiErrorResponse?>>

    suspend fun sendDecoyTraffic(
        url: String, data: String, sizeToReceive: String?
    ): Result<GenericResponseClass<String?, ApiErrorResponse?>>
}