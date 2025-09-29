/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.api

import com.windscribe.vpn.api.response.*
import io.reactivex.Single

interface IApiCallManager {

    fun addUserEmailAddress(email: String): Single<GenericResponseClass<AddEmailResponse?, ApiErrorResponse?>>
    fun checkConnectivityAndIpAddress(): Single<GenericResponseClass<GetMyIpResponse?, ApiErrorResponse?>>
    fun claimAccount(
        username: String,
        password: String,
        email: String,
        voucherCode: String?
    ): Single<GenericResponseClass<ClaimAccountResponse?, ApiErrorResponse?>>

    fun getBillingPlans(promo: String?): Single<GenericResponseClass<BillingPlanResponse?, ApiErrorResponse?>>
    fun getNotifications(pcpID: String?): Single<GenericResponseClass<NewsFeedNotification?, ApiErrorResponse?>>
    fun getPortMap(): Single<GenericResponseClass<PortMapResponse?, ApiErrorResponse?>>
    fun getServerConfig(): Single<GenericResponseClass<String?, ApiErrorResponse?>>
    fun getServerCredentials(extraParams: Map<String, String>? = null): Single<GenericResponseClass<ServerCredentialsResponse?, ApiErrorResponse?>>
    fun getServerCredentialsForIKev2(extraParams: Map<String, String>? = null): Single<GenericResponseClass<ServerCredentialsResponse?, ApiErrorResponse?>>
    fun getServerList(
        isPro: Boolean,
        locHash: String,
        alcList: Array<String>,
        overriddenCountryCode: String?
    ): Single<GenericResponseClass<String?, ApiErrorResponse?>>

    fun getSessionGeneric(firebaseToken: String?): Single<GenericResponseClass<UserSessionResponse?, ApiErrorResponse?>>
    fun getStaticIpList(deviceID: String?): Single<GenericResponseClass<StaticIPResponse?, ApiErrorResponse?>>
    fun logUserIn(
        username: String,
        password: String,
        twoFa: String?,
        secureToken: String?,
        captchaSolution: String?,
        captchaTrailX: FloatArray,
        captchaTrailY: FloatArray
    ): Single<GenericResponseClass<UserLoginResponse?, ApiErrorResponse?>>

    fun getWebSession(): Single<GenericResponseClass<WebSession?, ApiErrorResponse?>>
    fun recordAppInstall(): Single<GenericResponseClass<String?, ApiErrorResponse?>>
    fun resendUserEmailAddress(extraParams: Map<String, String>? = null): Single<GenericResponseClass<AddEmailResponse?, ApiErrorResponse?>>
    fun sendTicket(
        supportEmail: String,
        supportName: String,
        supportSubject: String,
        supportMessage: String,
        supportCategory: String,
        type: String,
        channel: String
    ): Single<GenericResponseClass<TicketResponse?, ApiErrorResponse?>>

    fun signUserIn(
        username: String,
        password: String,
        referringUsername: String?,
        email: String?,
        voucherCode: String?,
        secureToken: String?,
        captchaSolution: String?,
        captchaTrailX: FloatArray,
        captchaTrailY: FloatArray
    ): Single<GenericResponseClass<UserRegistrationResponse?, ApiErrorResponse?>>

    fun claimVoucherCode(voucherCode: String): Single<GenericResponseClass<ClaimVoucherCodeResponse?, ApiErrorResponse?>>
    fun signUpUsingToken(token: String): Single<GenericResponseClass<UserRegistrationResponse?, ApiErrorResponse?>>
    fun verifyPurchaseReceipt(
        purchaseToken: String,
        gpPackageName: String,
        gpProductId: String,
        type: String,
        amazonUserId: String
    ): Single<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>>

    fun verifyExpressLoginCode(loginCode: String): Single<GenericResponseClass<VerifyExpressLoginResponse?, ApiErrorResponse?>>
    fun generateXPressLoginCode(): Single<GenericResponseClass<XPressLoginCodeResponse?, ApiErrorResponse?>>
    fun verifyXPressLoginCode(
        loginCode: String,
        signature: String
    ): Single<GenericResponseClass<XPressLoginVerifyResponse?, ApiErrorResponse?>>

    fun postDebugLog(
        username: String,
        log: String
    ): Single<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>>

    fun postPromoPaymentConfirmation(pcpID: String): Single<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>>
    fun getRobertFilters(): Single<GenericResponseClass<RobertFilterResponse?, ApiErrorResponse?>>
    fun updateRobertSettings(
        id: String,
        status: Int
    ): Single<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>>

    fun syncRobert(): Single<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>>
    fun deleteSession(): Single<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>>
    fun wgInit(
        clientPublicKey: String,
        deleteOldestKey: Boolean
    ): Single<GenericResponseClass<WgInitResponse?, ApiErrorResponse?>>

    fun wgConnect(
        clientPublicKey: String,
        hostname: String,
        deviceId: String
    ): Single<GenericResponseClass<WgConnectResponse?, ApiErrorResponse?>>

    fun wgRekey(clientPublicKey: String): Single<GenericResponseClass<WgRekeyResponse?, ApiErrorResponse?>>

    fun sendDecoyTraffic(
        url: String,
        data: String,
        sizeToReceive: String?
    ): Single<GenericResponseClass<String?, ApiErrorResponse?>>

    fun sso(
        provider: String,
        token: String
    ): Single<GenericResponseClass<SsoResponse?, ApiErrorResponse?>>

    fun authTokenSignup(useAsciiCaptcha: Boolean): Single<GenericResponseClass<AuthToken?, ApiErrorResponse?>>
    fun authTokenLogin(useAsciiCaptcha: Boolean): Single<GenericResponseClass<AuthToken?, ApiErrorResponse?>>
}
