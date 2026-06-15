/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.api

import com.windscribe.vpn.api.response.AddEmailResponse
import com.windscribe.vpn.api.response.ApiErrorResponse
import com.windscribe.vpn.api.response.AuthToken
import com.windscribe.vpn.api.response.BillingPlanResponse
import com.windscribe.vpn.api.response.CheckUpdateResponse
import com.windscribe.vpn.api.response.ClaimAccountResponse
import com.windscribe.vpn.api.response.ClaimVoucherCodeResponse
import com.windscribe.vpn.api.response.GeneratePasswordResponse
import com.windscribe.vpn.api.response.GenerateUsernameResponse
import com.windscribe.vpn.api.response.GenericResponseClass
import com.windscribe.vpn.api.response.GenericSuccess
import com.windscribe.vpn.api.response.GetMyIpResponse
import com.windscribe.vpn.api.response.LocationResponse
import com.windscribe.vpn.api.response.NewsFeedNotification
import com.windscribe.vpn.api.response.PortMapResponse
import com.windscribe.vpn.api.response.RobertFilterResponse
import com.windscribe.vpn.api.response.ServerCredentialsResponse
import com.windscribe.vpn.api.response.ServerResponse
import com.windscribe.vpn.api.response.SsoResponse
import com.windscribe.vpn.api.response.StaticIPResponse
import com.windscribe.vpn.api.response.TicketResponse
import com.windscribe.vpn.api.response.UnblockWgResponse
import com.windscribe.vpn.api.response.UserLoginResponse
import com.windscribe.vpn.api.response.UserRegistrationResponse
import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.api.response.VerifyExpressLoginResponse
import com.windscribe.vpn.api.response.WebSession
import com.windscribe.vpn.api.response.WgInitResponse
import com.windscribe.vpn.api.response.XPressLoginCodeResponse
import com.windscribe.vpn.api.response.XPressLoginVerifyResponse

interface IApiCallManager {
    suspend fun addUserEmailAddress(email: String): GenericResponseClass<AddEmailResponse?, ApiErrorResponse?>

    suspend fun getApiIp(): GenericResponseClass<GetMyIpResponse?, ApiErrorResponse?>

    suspend fun getIp(): GenericResponseClass<String?, ApiErrorResponse?>

    suspend fun claimAccount(
        username: String,
        password: String,
        email: String,
        voucherCode: String?,
    ): GenericResponseClass<ClaimAccountResponse?, ApiErrorResponse?>

    suspend fun getBillingPlans(promo: String?): GenericResponseClass<BillingPlanResponse?, ApiErrorResponse?>

    suspend fun getNotifications(pcpID: String?): GenericResponseClass<NewsFeedNotification?, ApiErrorResponse?>

    suspend fun getPortMap(): GenericResponseClass<PortMapResponse?, ApiErrorResponse?>

    suspend fun getServerConfig(): GenericResponseClass<String?, ApiErrorResponse?>

    suspend fun getServerCredentials(
        extraParams: Map<String, String>? = null,
    ): GenericResponseClass<ServerCredentialsResponse?, ApiErrorResponse?>

    suspend fun getServerCredentialsForIKev2(
        extraParams: Map<String, String>? = null,
    ): GenericResponseClass<ServerCredentialsResponse?, ApiErrorResponse?>

    suspend fun getServerList(
        isPro: Boolean,
        locHash: String,
        alcList: Array<String>,
        overriddenCountryCode: String?,
    ): GenericResponseClass<String?, ApiErrorResponse?>

    suspend fun getSessionGeneric(
        firebaseToken: String?,
        backup: Int = -1,
    ): GenericResponseClass<UserSessionResponse?, ApiErrorResponse?>

    suspend fun getStaticIpList(deviceID: String?): GenericResponseClass<StaticIPResponse?, ApiErrorResponse?>

    suspend fun logUserIn(
        username: String,
        password: String,
        twoFa: String?,
        secureToken: String?,
        captchaSolution: String?,
        captchaTrailX: FloatArray,
        captchaTrailY: FloatArray,
        installer: String? = null,
    ): GenericResponseClass<UserLoginResponse?, ApiErrorResponse?>

    suspend fun getWebSession(): GenericResponseClass<WebSession?, ApiErrorResponse?>

    suspend fun checkUpdate(
        appVersion: String,
        appBuild: String,
        osVersion: String,
    ): GenericResponseClass<CheckUpdateResponse?, ApiErrorResponse?>

    suspend fun recordAppInstall(): GenericResponseClass<String?, ApiErrorResponse?>

    suspend fun resendUserEmailAddress(extraParams: Map<String, String>? = null): GenericResponseClass<AddEmailResponse?, ApiErrorResponse?>

    suspend fun sendTicket(
        supportEmail: String,
        supportName: String,
        supportSubject: String,
        supportMessage: String,
        supportCategory: String,
        type: String,
        channel: String,
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
        captchaTrailY: FloatArray,
        integrityToken: String? = null,
        installer: String? = null,
    ): GenericResponseClass<UserRegistrationResponse?, ApiErrorResponse?>

    suspend fun claimVoucherCode(voucherCode: String): GenericResponseClass<ClaimVoucherCodeResponse?, ApiErrorResponse?>

    suspend fun signUpUsingToken(
        token: String,
        integrityToken: String? = null,
    ): GenericResponseClass<UserRegistrationResponse?, ApiErrorResponse?>

    suspend fun verifyPurchaseReceipt(
        purchaseToken: String,
        gpPackageName: String,
        gpProductId: String,
        type: String,
        amazonUserId: String,
    ): GenericResponseClass<GenericSuccess?, ApiErrorResponse?>

    suspend fun verifyExpressLoginCode(loginCode: String): GenericResponseClass<VerifyExpressLoginResponse?, ApiErrorResponse?>

    suspend fun generateXPressLoginCode(): GenericResponseClass<XPressLoginCodeResponse?, ApiErrorResponse?>

    suspend fun verifyXPressLoginCode(
        loginCode: String,
        signature: String,
    ): GenericResponseClass<XPressLoginVerifyResponse?, ApiErrorResponse?>

    suspend fun postDebugLog(
        username: String,
        log: String,
    ): GenericResponseClass<GenericSuccess?, ApiErrorResponse?>

    suspend fun postPromoPaymentConfirmation(pcpID: String): GenericResponseClass<GenericSuccess?, ApiErrorResponse?>

    suspend fun getRobertFilters(): GenericResponseClass<RobertFilterResponse?, ApiErrorResponse?>

    suspend fun updateRobertSettings(
        id: String,
        status: Int,
    ): GenericResponseClass<GenericSuccess?, ApiErrorResponse?>

    suspend fun syncRobert(): GenericResponseClass<GenericSuccess?, ApiErrorResponse?>

    suspend fun deleteSession(): GenericResponseClass<GenericSuccess?, ApiErrorResponse?>

    suspend fun wgInit(
        clientPublicKey: String,
        deleteOldestKey: Boolean,
    ): GenericResponseClass<WgInitResponse?, ApiErrorResponse?>

    suspend fun sendDecoyTraffic(
        url: String,
        data: String,
        sizeToReceive: String?,
    ): GenericResponseClass<String?, ApiErrorResponse?>

    suspend fun sso(
        provider: String,
        token: String,
        integrityToken: String? = null,
        installer: String? = null,
    ): GenericResponseClass<SsoResponse?, ApiErrorResponse?>

    suspend fun authTokenSignup(
        username: String,
        useAsciiCaptcha: Boolean,
    ): GenericResponseClass<AuthToken?, ApiErrorResponse?>

    suspend fun authTokenLogin(
        username: String,
        useAsciiCaptcha: Boolean,
    ): GenericResponseClass<AuthToken?, ApiErrorResponse?>

    suspend fun rotateIp(): GenericResponseClass<String?, ApiErrorResponse?>

    suspend fun pinIp(ip: String?): GenericResponseClass<String?, ApiErrorResponse?>

    suspend fun passwordRecovery(email: String?): GenericResponseClass<GenericSuccess?, ApiErrorResponse?>

    suspend fun unblockWgParams(): GenericResponseClass<UnblockWgResponse?, ApiErrorResponse?>

    suspend fun getLocations(): GenericResponseClass<LocationResponse?, ApiErrorResponse?>

    suspend fun getServers(backup: Int): GenericResponseClass<ServerResponse?, ApiErrorResponse?>

    suspend fun generateUsername(): GenericResponseClass<GenerateUsernameResponse?, ApiErrorResponse?>

    suspend fun generatePassword(): GenericResponseClass<GeneratePasswordResponse?, ApiErrorResponse?>
}
