/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.api

import com.windscribe.vpn.api.response.*

interface IApiCallManager {

    suspend fun addUserEmailAddress(email: String): GenericResponseClass<AddEmailResponse?, ApiErrorResponse?>
    suspend fun getIp(): GenericResponseClass<String?, ApiErrorResponse?>
    suspend fun claimAccount(
        username: String,
        password: String,
        email: String,
        voucherCode: String?
    ): GenericResponseClass<ClaimAccountResponse?, ApiErrorResponse?>

    suspend fun getBillingPlans(promo: String?): GenericResponseClass<BillingPlanResponse?, ApiErrorResponse?>
    suspend fun getNotifications(pcpID: String?): GenericResponseClass<NewsFeedNotification?, ApiErrorResponse?>
    suspend fun getPortMap(): GenericResponseClass<PortMapResponse?, ApiErrorResponse?>
    suspend fun getServerConfig(): GenericResponseClass<String?, ApiErrorResponse?>
    suspend fun getServerCredentials(extraParams: Map<String, String>? = null): GenericResponseClass<ServerCredentialsResponse?, ApiErrorResponse?>
    suspend fun getServerCredentialsForIKev2(extraParams: Map<String, String>? = null): GenericResponseClass<ServerCredentialsResponse?, ApiErrorResponse?>
    suspend fun getServerList(
        isPro: Boolean,
        locHash: String,
        alcList: Array<String>,
        overriddenCountryCode: String?
    ): GenericResponseClass<String?, ApiErrorResponse?>

    suspend fun getSessionGeneric(firebaseToken: String?): GenericResponseClass<UserSessionResponse?, ApiErrorResponse?>
    suspend fun getStaticIpList(deviceID: String?): GenericResponseClass<StaticIPResponse?, ApiErrorResponse?>
    suspend fun logUserIn(
        username: String,
        password: String,
        twoFa: String?,
        secureToken: String?,
        captchaSolution: String?,
        captchaTrailX: FloatArray,
        captchaTrailY: FloatArray
    ): GenericResponseClass<UserLoginResponse?, ApiErrorResponse?>

    suspend fun getWebSession(): GenericResponseClass<WebSession?, ApiErrorResponse?>
    suspend fun recordAppInstall(): GenericResponseClass<String?, ApiErrorResponse?>
    suspend fun resendUserEmailAddress(extraParams: Map<String, String>? = null): GenericResponseClass<AddEmailResponse?, ApiErrorResponse?>
    suspend fun sendTicket(
        supportEmail: String,
        supportName: String,
        supportSubject: String,
        supportMessage: String,
        supportCategory: String,
        type: String,
        channel: String
    ): GenericResponseClass<TicketResponse?, ApiErrorResponse?>

    suspend fun signUserIn(
        username: String,
        password: String,
        referringUsername: String?,
        email: String?,
        voucherCode: String?,
        secureToken: String?,
        captchaSolution: String?,
        captchaTrailX: FloatArray,
        captchaTrailY: FloatArray
    ): GenericResponseClass<UserRegistrationResponse?, ApiErrorResponse?>

    suspend fun claimVoucherCode(voucherCode: String): GenericResponseClass<ClaimVoucherCodeResponse?, ApiErrorResponse?>
    suspend fun signUpUsingToken(token: String): GenericResponseClass<UserRegistrationResponse?, ApiErrorResponse?>
    suspend fun verifyPurchaseReceipt(
        purchaseToken: String,
        gpPackageName: String,
        gpProductId: String,
        type: String,
        amazonUserId: String
    ): GenericResponseClass<GenericSuccess?, ApiErrorResponse?>

    suspend fun verifyExpressLoginCode(loginCode: String): GenericResponseClass<VerifyExpressLoginResponse?, ApiErrorResponse?>
    suspend fun generateXPressLoginCode(): GenericResponseClass<XPressLoginCodeResponse?, ApiErrorResponse?>
    suspend fun verifyXPressLoginCode(
        loginCode: String,
        signature: String
    ): GenericResponseClass<XPressLoginVerifyResponse?, ApiErrorResponse?>

    suspend fun postDebugLog(
        username: String,
        log: String
    ): GenericResponseClass<GenericSuccess?, ApiErrorResponse?>

    suspend fun postPromoPaymentConfirmation(pcpID: String): GenericResponseClass<GenericSuccess?, ApiErrorResponse?>
    suspend fun getRobertFilters(): GenericResponseClass<RobertFilterResponse?, ApiErrorResponse?>
    suspend fun updateRobertSettings(
        id: String,
        status: Int
    ): GenericResponseClass<GenericSuccess?, ApiErrorResponse?>

    suspend fun syncRobert(): GenericResponseClass<GenericSuccess?, ApiErrorResponse?>
    suspend fun deleteSession(): GenericResponseClass<GenericSuccess?, ApiErrorResponse?>
    suspend fun wgInit(
        clientPublicKey: String,
        deleteOldestKey: Boolean
    ): GenericResponseClass<WgInitResponse?, ApiErrorResponse?>

    suspend fun wgConnect(
        clientPublicKey: String,
        hostname: String,
        deviceId: String
    ): GenericResponseClass<WgConnectResponse?, ApiErrorResponse?>
    suspend fun sendDecoyTraffic(
        url: String,
        data: String,
        sizeToReceive: String?
    ): GenericResponseClass<String?, ApiErrorResponse?>

    suspend fun sso(
        provider: String,
        token: String
    ): GenericResponseClass<SsoResponse?, ApiErrorResponse?>

    suspend fun authTokenSignup(useAsciiCaptcha: Boolean): GenericResponseClass<AuthToken?, ApiErrorResponse?>
    suspend fun authTokenLogin(useAsciiCaptcha: Boolean): GenericResponseClass<AuthToken?, ApiErrorResponse?>
    suspend fun rotateIp(): GenericResponseClass<String?, ApiErrorResponse?>
    suspend fun pinIp(ip: String?): GenericResponseClass<String?, ApiErrorResponse?>
}
